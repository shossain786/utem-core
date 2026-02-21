package com.utem.utem_core.service;

import com.utem.utem_core.dto.FailureClusterDTO;
import com.utem.utem_core.dto.FailureHotspotDTO;
import com.utem.utem_core.dto.FailureInsightsDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import com.utem.utem_core.repository.TestStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service providing failure hotspot detection (#32) and error clustering (#33).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FailureInsightsService {

    private static final Set<TestRun.RunStatus> FINISHED_STATUSES =
            Set.of(TestRun.RunStatus.PASSED, TestRun.RunStatus.FAILED);

    private static final Set<TestNode.NodeType> TEST_CASE_TYPES =
            Set.of(TestNode.NodeType.SCENARIO, TestNode.NodeType.STEP);

    // Patterns to strip from error messages before clustering
    private static final Pattern HEX_ADDRESS = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern LINE_REF = Pattern.compile(":(\\d+)");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+\\b");

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final TestStepRepository testStepRepository;

    // ============ #32: Failure Hotspots ============

    /**
     * Returns tests with the highest failure rate across recent finished runs.
     */
    public List<FailureHotspotDTO> getFailureHotspots(int limit, int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        if (runs.isEmpty()) return List.of();

        // runId → run map for lookup
        Map<String, TestRun> runById = runs.stream()
                .collect(Collectors.toMap(TestRun::getId, r -> r));

        // Group: "name|nodeType" → {runId → node}
        Map<String, Map<String, TestNode>> nodesByKeyAndRun = new LinkedHashMap<>();

        for (TestRun run : runs) {
            List<TestNode> failedNodes = testNodeRepository
                    .findByTestRunIdAndStatus(run.getId(), TestNode.NodeStatus.FAILED);
            for (TestNode node : failedNodes) {
                if (!TEST_CASE_TYPES.contains(node.getNodeType())) continue;
                String key = node.getName() + "|" + node.getNodeType();
                nodesByKeyAndRun.computeIfAbsent(key, k -> new LinkedHashMap<>())
                        .put(run.getId(), node);
            }
        }

        // Total runs each test appeared in (any status)
        Map<String, Set<String>> testRunAppearances = new HashMap<>();
        for (TestRun run : runs) {
            List<TestNode> allNodes = testNodeRepository.findByTestRunIdAndNodeTypeIn(run.getId(), TEST_CASE_TYPES);
            for (TestNode node : allNodes) {
                String key = node.getName() + "|" + node.getNodeType();
                testRunAppearances.computeIfAbsent(key, k -> new HashSet<>()).add(run.getId());
            }
        }

        List<FailureHotspotDTO> hotspots = new ArrayList<>();

        for (Map.Entry<String, Map<String, TestNode>> entry : nodesByKeyAndRun.entrySet()) {
            String key = entry.getKey();
            Map<String, TestNode> failsByRun = entry.getValue();
            int failCount = failsByRun.size();
            int totalRuns = testRunAppearances.getOrDefault(key, Set.of()).size();
            double failRate = totalRuns > 0 ? (failCount * 100.0) / totalRuns : 100.0;

            // Find the most recent failure for error context
            TestRun latestRun = failsByRun.keySet().stream()
                    .map(runById::get)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(TestRun::getStartTime))
                    .orElse(null);

            String lastRunId = latestRun != null ? latestRun.getId() : null;
            String lastError = null;
            if (lastRunId != null) {
                TestNode lastFailedNode = failsByRun.get(lastRunId);
                if (lastFailedNode != null) {
                    lastError = findFirstStepError(lastFailedNode.getId());
                }
            }

            String[] parts = key.split("\\|", 2);
            String testName = parts[0];
            TestNode.NodeType nodeType = TestNode.NodeType.valueOf(parts[1]);

            hotspots.add(new FailureHotspotDTO(testName, nodeType, failCount, totalRuns, failRate, lastRunId, lastError));
        }

        hotspots.sort(Comparator.comparingDouble(FailureHotspotDTO::failRate).reversed());
        return hotspots.size() > limit ? hotspots.subList(0, limit) : hotspots;
    }

    // ============ #33: Failure Clustering ============

    /**
     * Clusters error messages from failed steps across recent finished runs.
     * Uses normalized exact-match grouping.
     */
    public List<FailureClusterDTO> getFailureClusters(int limit, int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        if (runs.isEmpty()) return List.of();

        // Collect all failed step error messages with context
        record ErrorEntry(String normalizedMsg, String rawMsg, String testName, String runId) {}
        List<ErrorEntry> entries = new ArrayList<>();

        for (TestRun run : runs) {
            List<TestNode> failedNodes = testNodeRepository
                    .findByTestRunIdAndStatus(run.getId(), TestNode.NodeStatus.FAILED);
            if (failedNodes.isEmpty()) continue;

            List<String> nodeIds = failedNodes.stream().map(TestNode::getId).toList();
            List<TestStep> steps = testStepRepository.findByTestNodeIdIn(nodeIds);

            Map<String, String> nodeIdToName = failedNodes.stream()
                    .collect(Collectors.toMap(TestNode::getId, TestNode::getName));

            for (TestStep step : steps) {
                if (step.getErrorMessage() == null || step.getErrorMessage().isBlank()) continue;
                String normalized = normalizeError(step.getErrorMessage());
                if (normalized.isBlank()) continue;
                String testName = nodeIdToName.getOrDefault(
                        step.getTestNode() != null ? step.getTestNode().getId() : "", "unknown");
                entries.add(new ErrorEntry(normalized, step.getErrorMessage(), testName, run.getId()));
            }
        }

        // Group by normalized message
        Map<String, List<ErrorEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(ErrorEntry::normalizedMsg));

        List<FailureClusterDTO> clusters = new ArrayList<>();
        int clusterId = 1;

        for (Map.Entry<String, List<ErrorEntry>> entry : grouped.entrySet()) {
            List<ErrorEntry> group = entry.getValue();
            // Use the shortest raw message as the representative (closest to original)
            String representative = group.stream()
                    .map(ErrorEntry::rawMsg)
                    .min(Comparator.comparingInt(String::length))
                    .orElse(entry.getKey());
            // Truncate for display
            if (representative.length() > 500) {
                representative = representative.substring(0, 500) + "…";
            }

            List<String> affectedTests = group.stream()
                    .map(ErrorEntry::testName)
                    .distinct()
                    .sorted()
                    .toList();

            List<String> runIds = group.stream()
                    .map(ErrorEntry::runId)
                    .distinct()
                    .toList();

            clusters.add(new FailureClusterDTO(
                    clusterId++, representative, group.size(), affectedTests, runIds));
        }

        clusters.sort(Comparator.comparingInt(FailureClusterDTO::occurrences).reversed());
        return clusters.size() > limit ? clusters.subList(0, limit) : clusters;
    }

    // ============ Combined ============

    public FailureInsightsDTO getInsights(int limit, int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        return new FailureInsightsDTO(
                runs.size(),
                getFailureHotspots(limit, recentRuns),
                getFailureClusters(limit, recentRuns)
        );
    }

    // ============ Private Helpers ============

    private List<TestRun> getRecentFinishedRuns(int limit) {
        int fetchSize = Math.min(limit * 2, 500);
        List<TestRun> recent = testRunRepository
                .findByStatusInOrderByStartTimeDesc(FINISHED_STATUSES, PageRequest.of(0, fetchSize));
        return recent.size() > limit ? recent.subList(0, limit) : recent;
    }

    private String findFirstStepError(String nodeId) {
        List<TestStep> steps = testStepRepository.findByTestNodeIdOrderByStepOrderAsc(nodeId);
        return steps.stream()
                .filter(s -> s.getErrorMessage() != null && !s.getErrorMessage().isBlank())
                .map(TestStep::getErrorMessage)
                .findFirst()
                .orElse(null);
    }

    private static String normalizeError(String raw) {
        String normalized = raw;
        normalized = HEX_ADDRESS.matcher(normalized).replaceAll("<hex>");
        normalized = UUID_PATTERN.matcher(normalized).replaceAll("<uuid>");
        normalized = LINE_REF.matcher(normalized).replaceAll(":<N>");
        normalized = NUMBERS.matcher(normalized).replaceAll("<N>");
        normalized = normalized.toLowerCase(java.util.Locale.ROOT).trim();
        // Keep first 300 chars as the grouping key
        if (normalized.length() > 300) {
            normalized = normalized.substring(0, 300);
        }
        return normalized;
    }
}

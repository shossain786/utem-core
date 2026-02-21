package com.utem.utem_core.service;

import com.utem.utem_core.dto.DurationStatsDTO;
import com.utem.utem_core.dto.PerformanceReportDTO;
import com.utem.utem_core.dto.SlowTestDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for performance analysis: slowest tests and duration breakdown by node type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PerformanceAnalysisService {

    private static final Set<TestRun.RunStatus> FINISHED_STATUSES =
            Set.of(TestRun.RunStatus.PASSED, TestRun.RunStatus.FAILED);

    private static final Set<TestNode.NodeType> TEST_CASE_TYPES =
            Set.of(TestNode.NodeType.SCENARIO, TestNode.NodeType.STEP);

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;

    /**
     * Returns the slowest tests by average duration across recent finished runs.
     */
    public List<SlowTestDTO> getSlowestTests(int limit, int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        if (runs.isEmpty()) return List.of();

        // key: "name|nodeType" → list of (duration, runId)
        record NodeSample(long duration, String runId) {}
        Map<String, List<NodeSample>> samplesByKey = new LinkedHashMap<>();

        for (TestRun run : runs) {
            List<TestNode> nodes = testNodeRepository.findByTestRunIdAndNodeTypeIn(run.getId(), TEST_CASE_TYPES);
            for (TestNode node : nodes) {
                if (node.getDuration() == null || node.getDuration() <= 0) continue;
                String key = node.getName() + "|" + node.getNodeType();
                samplesByKey.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new NodeSample(node.getDuration(), run.getId()));
            }
        }

        List<SlowTestDTO> results = new ArrayList<>();

        for (Map.Entry<String, List<NodeSample>> entry : samplesByKey.entrySet()) {
            List<NodeSample> samples = entry.getValue();
            long total = 0;
            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;
            String slowestRunId = null;

            for (NodeSample s : samples) {
                total += s.duration();
                if (s.duration() > max) { max = s.duration(); slowestRunId = s.runId(); }
                if (s.duration() < min) min = s.duration();
            }

            long avg = total / samples.size();
            String[] parts = entry.getKey().split("\\|", 2);
            TestNode.NodeType nodeType = TestNode.NodeType.valueOf(parts[1]);

            results.add(new SlowTestDTO(parts[0], nodeType, avg, max, min, samples.size(), slowestRunId));
        }

        results.sort(Comparator.comparingLong(SlowTestDTO::avgDurationMs).reversed());
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    /**
     * Returns average/max/min/total duration broken down by node type.
     */
    public Map<String, DurationStatsDTO> getDurationByNodeType(int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        if (runs.isEmpty()) return Map.of();

        // nodeType → list of durations
        Map<TestNode.NodeType, List<Long>> durationsByType = new EnumMap<>(TestNode.NodeType.class);

        for (TestRun run : runs) {
            List<TestNode> nodes = testNodeRepository.findByTestRunId(run.getId());
            for (TestNode node : nodes) {
                if (node.getDuration() == null || node.getDuration() <= 0) continue;
                durationsByType.computeIfAbsent(node.getNodeType(), k -> new ArrayList<>())
                        .add(node.getDuration());
            }
        }

        Map<String, DurationStatsDTO> result = new LinkedHashMap<>();
        for (Map.Entry<TestNode.NodeType, List<Long>> entry : durationsByType.entrySet()) {
            List<Long> durations = entry.getValue();
            long total = durations.stream().mapToLong(Long::longValue).sum();
            long max = durations.stream().mapToLong(Long::longValue).max().orElse(0);
            long min = durations.stream().mapToLong(Long::longValue).min().orElse(0);
            long avg = total / durations.size();
            result.put(entry.getKey().name(), new DurationStatsDTO(avg, max, min, total, durations.size()));
        }

        return result;
    }

    /**
     * Combined performance report.
     */
    public PerformanceReportDTO getPerformanceReport(int limit, int recentRuns) {
        List<TestRun> runs = getRecentFinishedRuns(recentRuns);
        return new PerformanceReportDTO(
                runs.size(),
                getSlowestTests(limit, recentRuns),
                getDurationByNodeType(recentRuns)
        );
    }

    // ============ Private Helpers ============

    private List<TestRun> getRecentFinishedRuns(int limit) {
        int fetchSize = Math.min(limit * 2, 500);
        List<TestRun> recent = testRunRepository
                .findByStatusInOrderByStartTimeDesc(FINISHED_STATUSES, PageRequest.of(0, fetchSize));
        return recent.size() > limit ? recent.subList(0, limit) : recent;
    }
}

package com.utem.utem_core.service;

import com.utem.utem_core.dto.FlakinessReportDTO;
import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and analyzing flaky tests across runs.
 * <p>
 * A test is considered flaky if it has inconsistent status (PASSED/FAILED)
 * across different runs, or if it was marked flaky by the test framework.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlakinessDetectionService {

    private static final Set<TestNode.NodeType> TEST_CASE_TYPES = Set.of(
            TestNode.NodeType.SCENARIO, TestNode.NodeType.STEP
    );

    private final TestNodeRepository testNodeRepository;
    private final TestRunRepository testRunRepository;

    /**
     * Get tests marked flaky within a specific run (by framework or by retry).
     */
    @Transactional(readOnly = true)
    public List<FlakyTestDTO> getFlakyTestsForRun(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> nodes = testNodeRepository.findByTestRunIdAndNodeTypeIn(runId, TEST_CASE_TYPES);

        return nodes.stream()
                .filter(n -> Boolean.TRUE.equals(n.getFlaky()) || (n.getRetryCount() != null && n.getRetryCount() > 0))
                .map(n -> new FlakyTestDTO(
                        n.getName(),
                        n.getNodeType(),
                        100.0, // Within a single run, framework-marked = 100% flaky
                        1,
                        n.getStatus() == TestNode.NodeStatus.PASSED ? 1 : 0,
                        n.getStatus() == TestNode.NodeStatus.FAILED ? 1 : 0,
                        n.getStatus() == TestNode.NodeStatus.SKIPPED ? 1 : 0,
                        Boolean.TRUE.equals(n.getFlaky()),
                        n.getRetryCount(),
                        n.getStatus(),
                        runId
                ))
                .toList();
    }

    /**
     * Analyze all tests across runs to detect flaky ones.
     *
     * @param minRuns   Minimum number of runs a test must appear in (default 2)
     * @param threshold Minimum flakiness rate to be considered flaky (0-100, default 0)
     * @return List of flaky tests sorted by flakiness rate descending
     */
    @Transactional(readOnly = true)
    public List<FlakyTestDTO> detectFlakyTests(int minRuns, double threshold) {
        // Get all distinct test names from scenario-level nodes
        List<TestNode> allTestCases = testNodeRepository.findAll().stream()
                .filter(n -> TEST_CASE_TYPES.contains(n.getNodeType()))
                .toList();

        // Group by test name + node type
        Map<String, List<TestNode>> grouped = allTestCases.stream()
                .collect(Collectors.groupingBy(n -> n.getName() + "|" + n.getNodeType()));

        List<FlakyTestDTO> flakyTests = new ArrayList<>();

        for (Map.Entry<String, List<TestNode>> entry : grouped.entrySet()) {
            List<TestNode> instances = entry.getValue();

            if (instances.size() < minRuns) {
                continue;
            }

            FlakyTestDTO dto = analyzeTestInstances(instances);

            if ((dto.flakinessRate() > 0 || dto.frameworkMarked()) && dto.flakinessRate() >= threshold) {
                flakyTests.add(dto);
            }
        }

        flakyTests.sort(Comparator.comparingDouble(FlakyTestDTO::flakinessRate).reversed());
        return flakyTests;
    }

    /**
     * Get flakiness details for a specific test across all runs.
     */
    @Transactional(readOnly = true)
    public FlakyTestDTO getTestFlakinessHistory(String testName, TestNode.NodeType nodeType) {
        List<TestNode> instances = testNodeRepository
                .findByNameAndNodeTypeOrderByTestRunStartTimeDesc(testName, nodeType);

        if (instances.isEmpty()) {
            return new FlakyTestDTO(testName, nodeType, 0, 0, 0, 0, 0, false, null, null, null);
        }

        return analyzeTestInstances(instances);
    }

    /**
     * Get a flakiness report for a specific run.
     * Includes tests marked flaky by framework AND tests detected as flaky across runs.
     */
    @Transactional(readOnly = true)
    public FlakinessReportDTO getFlakinessReport(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> testCases = testNodeRepository.findByTestRunIdAndNodeTypeIn(runId, TEST_CASE_TYPES);
        int totalTests = testCases.size();

        // Framework-marked flaky tests in this run
        List<FlakyTestDTO> flakyInRun = getFlakyTestsForRun(runId);

        // Also check cross-run flakiness for tests in this run
        Set<String> alreadyFlaky = flakyInRun.stream()
                .map(FlakyTestDTO::testName)
                .collect(Collectors.toSet());

        List<FlakyTestDTO> crossRunFlaky = new ArrayList<>(flakyInRun);

        for (TestNode testCase : testCases) {
            if (alreadyFlaky.contains(testCase.getName())) {
                continue;
            }

            List<TestNode> instances = testNodeRepository
                    .findByNameAndNodeTypeOrderByTestRunStartTimeDesc(testCase.getName(), testCase.getNodeType());

            if (instances.size() >= 2) {
                FlakyTestDTO dto = analyzeTestInstances(instances);
                if (dto.flakinessRate() > 0) {
                    crossRunFlaky.add(dto);
                    alreadyFlaky.add(testCase.getName());
                }
            }
        }

        crossRunFlaky.sort(Comparator.comparingDouble(FlakyTestDTO::flakinessRate).reversed());

        double flakinessPercentage = totalTests > 0
                ? (crossRunFlaky.size() * 100.0) / totalTests
                : 0;

        return new FlakinessReportDTO(totalTests, crossRunFlaky.size(), flakinessPercentage, crossRunFlaky);
    }

    /**
     * Get overall flakiness report across recent N runs.
     */
    @Transactional(readOnly = true)
    public FlakinessReportDTO getOverallFlakinessReport(int recentRunCount) {
        List<TestRun> recentRuns = testRunRepository.findAllByOrderByStartTimeDesc();

        if (recentRuns.size() > recentRunCount) {
            recentRuns = recentRuns.subList(0, recentRunCount);
        }

        Set<String> runIds = recentRuns.stream().map(TestRun::getId).collect(Collectors.toSet());

        // Get all test cases from recent runs
        List<TestNode> allTestCases = new ArrayList<>();
        for (String runId : runIds) {
            allTestCases.addAll(testNodeRepository.findByTestRunIdAndNodeTypeIn(runId, TEST_CASE_TYPES));
        }

        // Get distinct test names
        Set<String> distinctTestNames = allTestCases.stream()
                .map(TestNode::getName)
                .collect(Collectors.toSet());

        int totalTests = distinctTestNames.size();

        // Detect flaky tests across these runs
        Map<String, List<TestNode>> grouped = allTestCases.stream()
                .collect(Collectors.groupingBy(n -> n.getName() + "|" + n.getNodeType()));

        List<FlakyTestDTO> flakyTests = new ArrayList<>();

        for (List<TestNode> instances : grouped.values()) {
            if (instances.size() < 2) {
                continue;
            }
            FlakyTestDTO dto = analyzeTestInstances(instances);
            if (dto.flakinessRate() > 0 || dto.frameworkMarked()) {
                flakyTests.add(dto);
            }
        }

        flakyTests.sort(Comparator.comparingDouble(FlakyTestDTO::flakinessRate).reversed());

        double flakinessPercentage = totalTests > 0
                ? (flakyTests.size() * 100.0) / totalTests
                : 0;

        return new FlakinessReportDTO(totalTests, flakyTests.size(), flakinessPercentage, flakyTests);
    }

    /**
     * Get the top N most flaky tests sorted by flakiness rate.
     */
    @Transactional(readOnly = true)
    public List<FlakyTestDTO> getMostFlakyTests(int limit) {
        List<FlakyTestDTO> all = detectFlakyTests(2, 0);
        return all.stream().limit(limit).toList();
    }

    // ============ Private Helper Methods ============

    /**
     * Analyze a list of test instances (same test across runs) to compute flakiness.
     */
    private FlakyTestDTO analyzeTestInstances(List<TestNode> instances) {
        String testName = instances.get(0).getName();
        TestNode.NodeType nodeType = instances.get(0).getNodeType();

        int passCount = 0;
        int failCount = 0;
        int skipCount = 0;
        boolean frameworkMarked = false;

        for (TestNode node : instances) {
            switch (node.getStatus()) {
                case PASSED -> passCount++;
                case FAILED -> failCount++;
                case SKIPPED -> skipCount++;
                default -> {} // PENDING, RUNNING - ignore
            }
            if (Boolean.TRUE.equals(node.getFlaky())) {
                frameworkMarked = true;
            }
        }

        // Flakiness rate: if a test has both passes and failures, it's flaky
        // Rate = number of status transitions / (total decisive runs - 1)
        int decisiveRuns = passCount + failCount; // Exclude skipped
        double flakinessRate = 0;

        if (decisiveRuns >= 2 && passCount > 0 && failCount > 0) {
            // Count status transitions (consecutive status changes)
            List<TestNode.NodeStatus> statusHistory = instances.stream()
                    .filter(n -> n.getStatus() == TestNode.NodeStatus.PASSED || n.getStatus() == TestNode.NodeStatus.FAILED)
                    .map(TestNode::getStatus)
                    .toList();

            int transitions = 0;
            for (int i = 1; i < statusHistory.size(); i++) {
                if (statusHistory.get(i) != statusHistory.get(i - 1)) {
                    transitions++;
                }
            }

            flakinessRate = (transitions * 100.0) / (statusHistory.size() - 1);
        } else if (frameworkMarked) {
            // Framework marked it flaky even without cross-run status changes
            flakinessRate = 100.0;
        }

        // Most recent instance (first in list since ordered by startTime desc)
        TestNode latest = instances.get(0);

        return new FlakyTestDTO(
                testName,
                nodeType,
                flakinessRate,
                instances.size(),
                passCount,
                failCount,
                skipCount,
                frameworkMarked,
                latest.getRetryCount(),
                latest.getStatus(),
                latest.getTestRun() != null ? latest.getTestRun().getId() : null
        );
    }
}

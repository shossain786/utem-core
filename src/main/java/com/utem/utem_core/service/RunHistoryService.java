package com.utem.utem_core.service;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.AttachmentRepository;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import com.utem.utem_core.repository.TestStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for querying, listing, comparing, and managing test run history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunHistoryService {

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final TestStepRepository testStepRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final HierarchyReconstructionService hierarchyReconstructionService;

    /**
     * Get active (non-archived) runs with pagination, newest first.
     */
    @Transactional(readOnly = true)
    public Page<TestRunSummaryDTO> getAllRuns(int page, int size) {
        return testRunRepository.findByArchivedFalseOrderByStartTimeDesc(PageRequest.of(page, size))
                .map(TestRunSummaryDTO::from);
    }

    /**
     * Get active runs filtered by status with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TestRunSummaryDTO> getRunsByStatus(TestRun.RunStatus status, int page, int size) {
        return testRunRepository.findByArchivedFalseAndStatusOrderByStartTimeDesc(status, PageRequest.of(page, size))
                .map(TestRunSummaryDTO::from);
    }

    /**
     * Search active runs by name with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TestRunSummaryDTO> searchRuns(String name, int page, int size) {
        return testRunRepository.findByArchivedFalseAndNameContainingIgnoreCaseOrderByStartTimeDesc(name, PageRequest.of(page, size))
                .map(TestRunSummaryDTO::from);
    }

    /**
     * Get archived runs with pagination, newest first.
     */
    @Transactional(readOnly = true)
    public Page<TestRunSummaryDTO> getArchivedRuns(int page, int size) {
        return testRunRepository.findByArchivedTrueOrderByStartTimeDesc(PageRequest.of(page, size))
                .map(TestRunSummaryDTO::from);
    }

    /**
     * Archive a set of runs by ID.
     */
    @Transactional
    public void archiveRuns(List<String> ids) {
        List<TestRun> runs = testRunRepository.findAllById(ids);
        runs.forEach(r -> r.setArchived(true));
        testRunRepository.saveAll(runs);
        log.info("Archived {} runs: {}", runs.size(), ids);
    }

    /**
     * Restore a single archived run back to the active list.
     */
    @Transactional
    public TestRunSummaryDTO unarchiveRun(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));
        run.setArchived(false);
        TestRun saved = testRunRepository.save(run);
        log.info("Unarchived run {} ('{}')", runId, run.getName());
        return TestRunSummaryDTO.from(saved);
    }

    /**
     * Get a single run summary by ID.
     */
    @Transactional(readOnly = true)
    public TestRunSummaryDTO getRunById(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));
        return TestRunSummaryDTO.from(run);
    }

    /**
     * Get full run detail with hierarchy tree.
     * Delegates to HierarchyReconstructionService.
     */
    @Transactional(readOnly = true)
    public TestRunHierarchyDTO getRunDetail(String runId) {
        return hierarchyReconstructionService.getFullHierarchy(runId);
    }

    /**
     * Get run statistics (node counts, duration).
     * Delegates to HierarchyReconstructionService.
     */
    @Transactional(readOnly = true)
    public NodeStatistics getRunStatistics(String runId) {
        return hierarchyReconstructionService.calculateRunStatistics(runId);
    }

    /**
     * Compare two runs side by side, including node-level diff matched by name.
     */
    @Transactional(readOnly = true)
    public RunComparisonDTO compareRuns(String baseRunId, String compareRunId) {
        TestRunSummaryDTO baseRun = getRunById(baseRunId);
        TestRunSummaryDTO compareRun = getRunById(compareRunId);

        // Build node-level diff (SCENARIO and SUITE types, matched by name)
        List<TestNode> baseNodes = testNodeRepository.findByTestRunId(baseRunId);
        List<TestNode> compareNodes = testNodeRepository.findByTestRunId(compareRunId);

        Map<String, TestNode> baseByName = baseNodes.stream()
                .collect(Collectors.toMap(TestNode::getName, n -> n, (a, b) -> a));
        Map<String, TestNode> compareByName = compareNodes.stream()
                .collect(Collectors.toMap(TestNode::getName, n -> n, (a, b) -> a));

        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(baseByName.keySet());
        allNames.addAll(compareByName.keySet());

        List<RunComparisonDTO.NodeDiffEntry> diffs = new ArrayList<>();
        for (String name : allNames) {
            TestNode base = baseByName.get(name);
            TestNode compare = compareByName.get(name);

            RunComparisonDTO.DiffType diffType;
            if (base == null) {
                diffType = RunComparisonDTO.DiffType.NEW;
            } else if (compare == null) {
                diffType = RunComparisonDTO.DiffType.REMOVED;
            } else if (base.getStatus() == TestNode.NodeStatus.PASSED
                    && compare.getStatus() == TestNode.NodeStatus.FAILED) {
                diffType = RunComparisonDTO.DiffType.REGRESSION;
            } else if (base.getStatus() == TestNode.NodeStatus.FAILED
                    && compare.getStatus() == TestNode.NodeStatus.PASSED) {
                diffType = RunComparisonDTO.DiffType.FIX;
            } else {
                diffType = RunComparisonDTO.DiffType.UNCHANGED;
            }

            diffs.add(new RunComparisonDTO.NodeDiffEntry(
                    name,
                    compare != null ? compare.getNodeType().name() : base.getNodeType().name(),
                    diffType,
                    base != null ? base.getStatus().name() : null,
                    compare != null ? compare.getStatus().name() : null,
                    base != null ? base.getDuration() : null,
                    compare != null ? compare.getDuration() : null
            ));
        }

        return RunComparisonDTO.from(baseRun, compareRun, diffs);
    }

    /**
     * Manually abort a RUNNING run. Throws {@link IllegalStateException} if run is not RUNNING.
     */
    @Transactional
    public TestRunSummaryDTO abortRun(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));
        if (run.getStatus() != TestRun.RunStatus.RUNNING) {
            throw new IllegalStateException(
                    "Run " + runId + " cannot be aborted — current status: " + run.getStatus());
        }
        run.setStatus(TestRun.RunStatus.ABORTED);
        run.setEndTime(Instant.now());
        TestRun saved = testRunRepository.save(run);
        log.info("Manually aborted run {} ('{}')", runId, run.getName());
        return TestRunSummaryDTO.from(saved);
    }

    /**
     * Delete a run and all associated data (nodes, steps, attachments, files).
     */
    @Transactional
    public void deleteRun(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        log.info("Deleting run {} and all associated data", runId);

        // Delete files from disk
        attachmentStorageService.deleteFilesForRun(runId);

        // Find all nodes for this run
        List<TestNode> nodes = testNodeRepository.findByTestRunId(runId);
        List<String> nodeIds = nodes.stream().map(TestNode::getId).toList();

        if (!nodeIds.isEmpty()) {
            // Find all steps for these nodes
            List<TestStep> steps = testStepRepository.findByTestNodeIdIn(nodeIds);
            List<String> stepIds = steps.stream().map(TestStep::getId).toList();

            // Delete attachments (for both nodes and steps)
            if (!stepIds.isEmpty()) {
                attachmentRepository.deleteAll(attachmentRepository.findByTestStepIdIn(stepIds));
            }
            attachmentRepository.deleteAll(attachmentRepository.findByTestNodeIdIn(nodeIds));

            // Delete steps
            testStepRepository.deleteAll(steps);

            // Delete nodes
            testNodeRepository.deleteAll(nodes);
        }

        // Delete the run itself
        testRunRepository.deleteById(runId);

        log.info("Deleted run {} with {} nodes", runId, nodeIds.size());
    }

    /**
     * Delete all runs started before the given cutoff time.
     *
     * @param cutoff Delete runs with startTime before this instant
     * @return Number of runs deleted
     */
    @Transactional
    public int deleteRunsBefore(Instant cutoff) {
        List<TestRun> oldRuns = testRunRepository.findByStartTimeBefore(cutoff);

        if (oldRuns.isEmpty()) {
            log.info("No runs found before {}", cutoff);
            return 0;
        }

        log.info("Deleting {} runs before {}", oldRuns.size(), cutoff);

        for (TestRun run : oldRuns) {
            deleteRun(run.getId());
        }

        return oldRuns.size();
    }

    /**
     * Get aggregate summary stats across all runs.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRunSummaryStats() {
        long total = testRunRepository.count();
        long running = testRunRepository.countByStatus(TestRun.RunStatus.RUNNING);
        long passed = testRunRepository.countByStatus(TestRun.RunStatus.PASSED);
        long failed = testRunRepository.countByStatus(TestRun.RunStatus.FAILED);
        long aborted = testRunRepository.countByStatus(TestRun.RunStatus.ABORTED);

        return Map.of(
                "totalRuns", total,
                "runningRuns", running,
                "passedRuns", passed,
                "failedRuns", failed,
                "abortedRuns", aborted
        );
    }
}

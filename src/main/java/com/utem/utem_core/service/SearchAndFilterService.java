package com.utem.utem_core.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utem.utem_core.dto.AttachmentSummaryDTO;
import com.utem.utem_core.dto.SearchResultDTO;
import com.utem.utem_core.dto.TestNodeSummaryDTO;
import com.utem.utem_core.dto.TestRunSummaryDTO;
import com.utem.utem_core.dto.TestStepSummaryDTO;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.repository.AttachmentRepository;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import com.utem.utem_core.repository.TestStepRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified search and filter service supporting combined filters across all entity types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAndFilterService {

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final TestStepRepository testStepRepository;
    private final AttachmentRepository attachmentRepository;

    /**
     * Search runs with combined filters (all optional).
     *
     * @param status Filter by run status (nullable)
     * @param name   Filter by name containing (nullable, case-insensitive)
     * @param from   Filter by start time >= from (nullable)
     * @param to     Filter by start time <= to (nullable)
     * @param page   Page number (0-based)
     * @param size   Page size
     */
    @Transactional(readOnly = true)
    public Page<TestRunSummaryDTO> searchRuns(TestRun.RunStatus status, String name,
                                              Instant from, Instant to,
                                              int page, int size) {
        log.debug("Searching runs: status={}, name={}, from={}, to={}, page={}, size={}",
                status, name, from, to, page, size);

        return testRunRepository.searchRuns(status, name, from, to, PageRequest.of(page, size))
                .map(TestRunSummaryDTO::from);
    }

    /**
     * Search nodes with combined filters and pagination.
     *
     * @param runId    Filter within a specific run (nullable for cross-run search)
     * @param name     Filter by name containing (nullable, case-insensitive)
     * @param status   Filter by node status (nullable)
     * @param nodeType Filter by node type (nullable)
     * @param page     Page number (0-based)
     * @param size     Page size
     */
    @Transactional(readOnly = true)
    public Page<TestNodeSummaryDTO> searchNodes(String runId, String name,
                                                 TestNode.NodeStatus status,
                                                 TestNode.NodeType nodeType,
                                                 int page, int size) {
        log.debug("Searching nodes: runId={}, name={}, status={}, nodeType={}, page={}, size={}",
                runId, name, status, nodeType, page, size);

        PageRequest pageable = PageRequest.of(page, size);

        // Use the most specific query available
        if (runId != null && name != null) {
            return testNodeRepository
                    .findByTestRunIdAndNameContainingIgnoreCaseOrderByStartTimeDesc(runId, name, pageable)
                    .map(TestNodeSummaryDTO::from);
        } else if (name != null) {
            return testNodeRepository
                    .findByNameContainingIgnoreCaseOrderByStartTimeDesc(name, pageable)
                    .map(TestNodeSummaryDTO::from);
        } else if (status != null) {
            return testNodeRepository
                    .findByStatusOrderByStartTimeDesc(status, pageable)
                    .map(TestNodeSummaryDTO::from);
        } else if (nodeType != null) {
            return testNodeRepository
                    .findByNodeTypeOrderByStartTimeDesc(nodeType, pageable)
                    .map(TestNodeSummaryDTO::from);
        }

        // No filters - return all nodes paginated
        return testNodeRepository.findAll(pageable).map(TestNodeSummaryDTO::from);
    }

    /**
     * Search steps with combined filters.
     *
     * @param nodeId    Filter within a specific node (nullable)
     * @param name      Filter by name containing (nullable, case-insensitive)
     * @param status    Filter by step status (nullable)
     * @param errorText Filter by error message containing (nullable, case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<TestStepSummaryDTO> searchSteps(String nodeId, String name,
                                                 TestStep.StepStatus status,
                                                 String errorText) {
        log.debug("Searching steps: nodeId={}, name={}, status={}, errorText={}",
                nodeId, name, status, errorText);

        // Start with the most specific filter
        if (nodeId != null && status != null) {
            return testStepRepository.findByTestNodeIdAndStatus(nodeId, status).stream()
                    .map(TestStepSummaryDTO::from)
                    .toList();
        } else if (nodeId != null) {
            return testStepRepository.findByTestNodeIdOrderByStepOrderAsc(nodeId).stream()
                    .map(TestStepSummaryDTO::from)
                    .toList();
        } else if (errorText != null) {
            return testStepRepository.findByErrorMessageContainingIgnoreCase(errorText).stream()
                    .map(TestStepSummaryDTO::from)
                    .toList();
        } else if (name != null) {
            return testStepRepository.findByNameContainingIgnoreCase(name).stream()
                    .map(TestStepSummaryDTO::from)
                    .toList();
        } else if (status != null) {
            return testStepRepository.findByStatusOrderByTimestampDesc(status).stream()
                    .map(TestStepSummaryDTO::from)
                    .toList();
        }

        // No filters - return all (limited for safety)
        return testStepRepository.findAll().stream()
                .limit(100)
                .map(TestStepSummaryDTO::from)
                .toList();
    }

    /**
     * Search attachments with combined filters.
     *
     * @param nodeId Filter within a specific node (nullable)
     * @param type   Filter by attachment type (nullable)
     * @param name   Filter by name containing (nullable, case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<AttachmentSummaryDTO> searchAttachments(String nodeId,
                                                        Attachment.AttachmentType type,
                                                        String name) {
        log.debug("Searching attachments: nodeId={}, type={}, name={}", nodeId, type, name);

        if (nodeId != null && type != null) {
            return attachmentRepository.findByTestNodeIdAndType(nodeId, type).stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        } else if (nodeId != null) {
            return attachmentRepository.findByTestNodeId(nodeId).stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        } else if (type != null && name != null) {
            return attachmentRepository.findByTypeAndNameContainingIgnoreCase(type, name).stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        } else if (name != null) {
            return attachmentRepository.findByNameContainingIgnoreCase(name).stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        } else if (type != null) {
            return attachmentRepository.findByType(type).stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        }

        // No filters - return all (limited for safety)
        return attachmentRepository.findAll().stream()
                .limit(100)
                .map(AttachmentSummaryDTO::from)
                .toList();
    }

    /**
     * Global search across all entity types by keyword.
     *
     * @param query Keyword to search for
     * @param limit Max results per entity type
     */
    @Transactional(readOnly = true)
    public SearchResultDTO globalSearch(String query, int limit) {
        log.debug("Global search: query={}, limit={}", query, limit);

        List<TestRunSummaryDTO> runs = testRunRepository
                .findByNameContainingIgnoreCase(query).stream()
                .limit(limit)
                .map(TestRunSummaryDTO::from)
                .toList();

        List<TestNodeSummaryDTO> nodes = testNodeRepository
                .findByNameContainingIgnoreCase(query).stream()
                .limit(limit)
                .map(TestNodeSummaryDTO::from)
                .toList();

        List<TestStepSummaryDTO> steps = testStepRepository
                .findByNameContainingIgnoreCase(query).stream()
                .limit(limit)
                .map(TestStepSummaryDTO::from)
                .toList();

        List<AttachmentSummaryDTO> attachments = attachmentRepository
                .findByNameContainingIgnoreCase(query).stream()
                .limit(limit)
                .map(AttachmentSummaryDTO::from)
                .toList();

        int totalResults = runs.size() + nodes.size() + steps.size() + attachments.size();

        return new SearchResultDTO(query, runs, nodes, steps, attachments, totalResults);
    }

    /**
     * Get all failed steps across a run.
     * Useful for quickly identifying failures.
     *
     * @param runId The run ID
     * @return List of failed step summaries
     */
    @Transactional(readOnly = true)
    public List<TestStepSummaryDTO> searchFailedSteps(String runId) {
        log.debug("Searching failed steps for run: {}", runId);

        List<TestNode> nodes = testNodeRepository.findByTestRunId(runId);
        List<String> nodeIds = nodes.stream().map(TestNode::getId).toList();

        if (nodeIds.isEmpty()) {
            return List.of();
        }

        return testStepRepository.findByTestNodeIdIn(nodeIds).stream()
                .filter(s -> s.getStatus() == TestStep.StepStatus.FAILED)
                .map(TestStepSummaryDTO::from)
                .toList();
    }
}

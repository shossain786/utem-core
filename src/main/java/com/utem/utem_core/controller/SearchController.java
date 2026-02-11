package com.utem.utem_core.controller;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.service.SearchAndFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for unified search and filter operations.
 */
@RestController
@RequestMapping("/utem/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchAndFilterService searchAndFilterService;

    /**
     * Global search across all entity types by keyword.
     */
    @GetMapping
    public ResponseEntity<SearchResultDTO> globalSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchAndFilterService.globalSearch(q, limit));
    }

    /**
     * Search runs with combined filters.
     */
    @GetMapping("/runs")
    public ResponseEntity<Page<TestRunSummaryDTO>> searchRuns(
            @RequestParam(required = false) TestRun.RunStatus status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchAndFilterService.searchRuns(status, name, from, to, page, size));
    }

    /**
     * Search nodes with combined filters and pagination.
     */
    @GetMapping("/nodes")
    public ResponseEntity<Page<TestNodeSummaryDTO>> searchNodes(
            @RequestParam(required = false) String runId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TestNode.NodeStatus status,
            @RequestParam(required = false) TestNode.NodeType nodeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchAndFilterService.searchNodes(runId, name, status, nodeType, page, size));
    }

    /**
     * Search steps with combined filters.
     */
    @GetMapping("/steps")
    public ResponseEntity<List<TestStepSummaryDTO>> searchSteps(
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TestStep.StepStatus status,
            @RequestParam(required = false) String errorText) {
        return ResponseEntity.ok(searchAndFilterService.searchSteps(nodeId, name, status, errorText));
    }

    /**
     * Search attachments with combined filters.
     */
    @GetMapping("/attachments")
    public ResponseEntity<List<AttachmentSummaryDTO>> searchAttachments(
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) Attachment.AttachmentType type,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(searchAndFilterService.searchAttachments(nodeId, type, name));
    }

    /**
     * Get all failed steps for a specific run.
     */
    @GetMapping("/runs/{runId}/failed-steps")
    public ResponseEntity<List<TestStepSummaryDTO>> getFailedSteps(@PathVariable String runId) {
        return ResponseEntity.ok(searchAndFilterService.searchFailedSteps(runId));
    }
}

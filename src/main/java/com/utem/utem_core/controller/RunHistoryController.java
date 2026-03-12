package com.utem.utem_core.controller;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.service.RunHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for test run history operations.
 */
@RestController
@RequestMapping("/utem/runs")
@RequiredArgsConstructor
@Slf4j
public class RunHistoryController {

    private final RunHistoryService runHistoryService;

    /**
     * List all runs with pagination, newest first.
     * Optionally filter by status, label, or search by name.
     */
    @GetMapping
    public ResponseEntity<Page<TestRunSummaryDTO>> getRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TestRun.RunStatus status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String label) {

        Page<TestRunSummaryDTO> result;

        if (name != null && !name.isBlank()) {
            result = runHistoryService.searchRuns(name, page, size);
        } else if (label != null && !label.isBlank()) {
            result = runHistoryService.getRunsByLabel(label, page, size);
        } else if (status != null) {
            result = runHistoryService.getRunsByStatus(status, page, size);
        } else {
            result = runHistoryService.getAllRuns(page, size);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get all distinct labels used on active runs.
     */
    @GetMapping("/labels")
    public ResponseEntity<List<String>> getRunLabels() {
        return ResponseEntity.ok(runHistoryService.getDistinctLabels());
    }

    /**
     * Get a single run summary by ID.
     */
    @GetMapping("/{runId}")
    public ResponseEntity<TestRunSummaryDTO> getRunById(@PathVariable String runId) {
        return ResponseEntity.ok(runHistoryService.getRunById(runId));
    }

    /**
     * Get full run detail with hierarchy tree.
     */
    @GetMapping("/{runId}/detail")
    public ResponseEntity<TestRunHierarchyDTO> getRunDetail(@PathVariable String runId) {
        return ResponseEntity.ok(runHistoryService.getRunDetail(runId));
    }

    /**
     * Get run statistics (node counts, duration).
     */
    @GetMapping("/{runId}/stats")
    public ResponseEntity<NodeStatistics> getRunStatistics(@PathVariable String runId) {
        return ResponseEntity.ok(runHistoryService.getRunStatistics(runId));
    }

    /**
     * Compare two runs side by side.
     */
    @GetMapping("/{runId}/compare")
    public ResponseEntity<RunComparisonDTO> compareRuns(
            @PathVariable String runId,
            @RequestParam String with) {
        return ResponseEntity.ok(runHistoryService.compareRuns(runId, with));
    }

    /**
     * Get aggregate summary stats across all runs.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getRunSummaryStats() {
        return ResponseEntity.ok(runHistoryService.getRunSummaryStats());
    }

    /**
     * Get archived runs with pagination.
     */
    @GetMapping("/archived")
    public ResponseEntity<Page<TestRunSummaryDTO>> getArchivedRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(runHistoryService.getArchivedRuns(page, size));
    }

    /**
     * Archive one or more runs. Body: { "ids": ["id1", "id2", ...] }
     */
    @PostMapping("/archive/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveRuns(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            log.info("Archiving {} run(s): {}", ids.size(), ids);
            runHistoryService.archiveRuns(ids);
        }
    }

    /**
     * Restore an archived run back to the active list.
     */
    @PostMapping("/{runId}/unarchive")
    public ResponseEntity<TestRunSummaryDTO> unarchiveRun(@PathVariable String runId) {
        log.info("Unarchiving run {}", runId);
        return ResponseEntity.ok(runHistoryService.unarchiveRun(runId));
    }

    /**
     * Update run metadata (label and/or name) from the dashboard UI.
     * Pass null or omit a field to leave it unchanged. Pass empty string to clear the label.
     */
    @PatchMapping("/{runId}")
    public ResponseEntity<TestRunSummaryDTO> updateRun(
            @PathVariable String runId,
            @RequestBody Map<String, String> body) {
        String label = body.containsKey("label") ? body.get("label") : null;
        String name  = body.get("name");
        log.info("Updating run {} metadata — label={} name={}", runId, label, name);
        return ResponseEntity.ok(runHistoryService.updateRunMetadata(runId, label, name));
    }

    /**
     * Manually abort a run stuck in RUNNING status (e.g. after CI pipeline crash).
     * Returns 409 if the run is not currently RUNNING.
     */
    @PostMapping("/{runId}/abort")
    public ResponseEntity<TestRunSummaryDTO> abortRun(@PathVariable String runId) {
        log.info("Manual abort requested for run {}", runId);
        return ResponseEntity.ok(runHistoryService.abortRun(runId));
    }

    /**
     * Delete a run and all associated data.
     */
    @DeleteMapping("/{runId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRun(@PathVariable String runId) {
        log.info("Deleting run {}", runId);
        runHistoryService.deleteRun(runId);
    }

    /**
     * Delete all runs started before the given cutoff time.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteRunsBefore(@RequestParam Instant before) {
        log.info("Deleting runs before {}", before);
        int deleted = runHistoryService.deleteRunsBefore(before);
        return ResponseEntity.ok(Map.of("deletedCount", deleted));
    }
}

package com.utem.utem_core.controller;

import com.utem.utem_core.dto.JobSummaryDTO;
import com.utem.utem_core.dto.TestRunSummaryDTO;
import com.utem.utem_core.service.RunHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for job-level views (runs grouped by job name).
 */
@RestController
@RequestMapping("/utem/jobs")
@RequiredArgsConstructor
public class JobController {

    private final RunHistoryService runHistoryService;

    /**
     * List all distinct jobs with their latest run status and total run count.
     */
    @GetMapping
    public ResponseEntity<List<JobSummaryDTO>> getJobs() {
        return ResponseEntity.ok(runHistoryService.getJobList());
    }

    /**
     * Get paginated run history for a specific job name.
     */
    @GetMapping("/{jobName}/runs")
    public ResponseEntity<Page<TestRunSummaryDTO>> getJobRuns(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(runHistoryService.getRunsByJob(jobName, page, size));
    }
}

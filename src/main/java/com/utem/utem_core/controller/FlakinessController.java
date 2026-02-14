package com.utem.utem_core.controller;

import com.utem.utem_core.dto.FlakinessReportDTO;
import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.service.FlakinessDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for flakiness detection and reporting.
 */
@RestController
@RequestMapping("/utem/flakiness")
@RequiredArgsConstructor
@Slf4j
public class FlakinessController {

    private final FlakinessDetectionService flakinessDetectionService;

    /**
     * Get overall flakiness report across recent runs.
     */
    @GetMapping("/report")
    public ResponseEntity<FlakinessReportDTO> getOverallReport() {
        return ResponseEntity.ok(flakinessDetectionService.getOverallFlakinessReport(20));
    }

    /**
     * Get flakiness report for a specific run.
     */
    @GetMapping("/report/{runId}")
    public ResponseEntity<FlakinessReportDTO> getRunReport(@PathVariable String runId) {
        return ResponseEntity.ok(flakinessDetectionService.getFlakinessReport(runId));
    }

    /**
     * Get top N most flaky tests.
     */
    @GetMapping("/top")
    public ResponseEntity<List<FlakyTestDTO>> getTopFlakyTests(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(flakinessDetectionService.getMostFlakyTests(limit));
    }
}

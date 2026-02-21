package com.utem.utem_core.controller;

import com.utem.utem_core.dto.FailureClusterDTO;
import com.utem.utem_core.dto.FailureHotspotDTO;
import com.utem.utem_core.dto.FailureInsightsDTO;
import com.utem.utem_core.service.FailureInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utem/failure-insights")
@RequiredArgsConstructor
@Slf4j
public class FailureInsightsController {

    private final FailureInsightsService failureInsightsService;

    @GetMapping("/hotspots")
    public ResponseEntity<List<FailureHotspotDTO>> getHotspots(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getFailureHotspots(clamp(limit, 1, 100), clamp(recentRuns, 1, 200)));
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<FailureClusterDTO>> getClusters(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getFailureClusters(clamp(limit, 1, 100), clamp(recentRuns, 1, 200)));
    }

    @GetMapping("/summary")
    public ResponseEntity<FailureInsightsDTO> getSummary(
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getInsights(20, clamp(recentRuns, 1, 200)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}

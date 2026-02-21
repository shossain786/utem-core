package com.utem.utem_core.controller;

import com.utem.utem_core.dto.PerformanceReportDTO;
import com.utem.utem_core.dto.SlowTestDTO;
import com.utem.utem_core.service.PerformanceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utem/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final PerformanceAnalysisService performanceAnalysisService;

    @GetMapping("/report")
    public ResponseEntity<PerformanceReportDTO> getReport(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(performanceAnalysisService.getPerformanceReport(clamp(limit, 1, 100), clamp(recentRuns, 1, 200)));
    }

    @GetMapping("/slowest")
    public ResponseEntity<List<SlowTestDTO>> getSlowest(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(performanceAnalysisService.getSlowestTests(clamp(limit, 1, 100), clamp(recentRuns, 1, 200)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}

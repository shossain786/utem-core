package com.utem.utem_core.controller;

import com.utem.utem_core.dto.TrendDataDTO;
import com.utem.utem_core.service.TrendAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utem/trends")
@RequiredArgsConstructor
@Slf4j
public class TrendController {

    private final TrendAnalysisService trendAnalysisService;

    @GetMapping("/pass-rate")
    public ResponseEntity<TrendDataDTO> getPassRateTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getPassRateTrend(clamp(limit)));
    }

    @GetMapping("/duration")
    public ResponseEntity<TrendDataDTO> getDurationTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getDurationTrend(clamp(limit)));
    }

    @GetMapping("/test-count")
    public ResponseEntity<TrendDataDTO> getTestCountTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getTestCountTrend(clamp(limit)));
    }

    @GetMapping("/flakiness")
    public ResponseEntity<TrendDataDTO> getFlakinessTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getFlakinessTrend(clamp(limit)));
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }
}

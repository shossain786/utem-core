package com.utem.utem_core.controller;

import com.utem.utem_core.dto.TrendDataDTO;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.TrendAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utem/trends")
@RequiredArgsConstructor
@Slf4j
public class TrendController {

    private final TrendAnalysisService trendAnalysisService;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @GetMapping("/pass-rate")
    public ResponseEntity<TrendDataDTO> getPassRateTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getPassRateTrend(clamp(limit), resolveProjectIds()));
    }

    @GetMapping("/duration")
    public ResponseEntity<TrendDataDTO> getDurationTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getDurationTrend(clamp(limit), resolveProjectIds()));
    }

    @GetMapping("/test-count")
    public ResponseEntity<TrendDataDTO> getTestCountTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getTestCountTrend(clamp(limit), resolveProjectIds()));
    }

    @GetMapping("/flakiness")
    public ResponseEntity<TrendDataDTO> getFlakinessTrend(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(trendAnalysisService.getFlakinessTrend(clamp(limit), resolveProjectIds()));
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }

    private List<String> resolveProjectIds() {
        if (!securityEnabled) return null;
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) throw new UnauthorizedException("Authentication required");
        return user.isSuperAdmin() ? null : user.projectIds();
    }
}

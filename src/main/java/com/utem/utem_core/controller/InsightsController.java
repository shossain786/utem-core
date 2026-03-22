package com.utem.utem_core.controller;

import com.utem.utem_core.dto.InsightsSummaryDTO;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.InsightsService;
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
@RequestMapping("/utem/insights")
@RequiredArgsConstructor
@Slf4j
public class InsightsController {

    private final InsightsService insightsService;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @GetMapping("/summary")
    public ResponseEntity<InsightsSummaryDTO> getSummary(
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(insightsService.getSummary(Math.min(Math.max(recentRuns, 1), 200), resolveProjectIds()));
    }

    private List<String> resolveProjectIds() {
        if (!securityEnabled) return null;
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) throw new UnauthorizedException("Authentication required");
        return user.isSuperAdmin() ? null : user.projectIds();
    }
}

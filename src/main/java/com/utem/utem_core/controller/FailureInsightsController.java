package com.utem.utem_core.controller;

import com.utem.utem_core.dto.DiagnosisDTO;
import com.utem.utem_core.dto.FailureClusterDTO;
import com.utem.utem_core.dto.FailureHotspotDTO;
import com.utem.utem_core.dto.FailureInsightsDTO;
import com.utem.utem_core.exception.UnauthorizedException;
import com.utem.utem_core.security.AuthenticatedUser;
import com.utem.utem_core.security.UserContextHolder;
import com.utem.utem_core.service.FailureDiagnosisService;
import com.utem.utem_core.service.FailureInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final FailureDiagnosisService failureDiagnosisService;

    @Value("${utem.security.enabled:false}")
    private boolean securityEnabled;

    @GetMapping("/hotspots")
    public ResponseEntity<List<FailureHotspotDTO>> getHotspots(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getFailureHotspots(clamp(limit, 1, 100), clamp(recentRuns, 1, 200), resolveProjectIds()));
    }

    @GetMapping("/clusters")
    public ResponseEntity<List<FailureClusterDTO>> getClusters(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getFailureClusters(clamp(limit, 1, 100), clamp(recentRuns, 1, 200), resolveProjectIds()));
    }

    @GetMapping("/summary")
    public ResponseEntity<FailureInsightsDTO> getSummary(
            @RequestParam(defaultValue = "30") int recentRuns) {
        return ResponseEntity.ok(failureInsightsService.getInsights(20, clamp(recentRuns, 1, 200), resolveProjectIds()));
    }

    @GetMapping("/steps/{stepId}/diagnosis")
    public ResponseEntity<DiagnosisDTO> getStepDiagnosis(@PathVariable String stepId) {
        return ResponseEntity.ok(failureDiagnosisService.diagnoseStep(stepId));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private List<String> resolveProjectIds() {
        if (!securityEnabled) return null;
        AuthenticatedUser user = UserContextHolder.get();
        if (user == null) throw new UnauthorizedException("Authentication required");
        return user.isSuperAdmin() ? null : user.projectIds();
    }
}

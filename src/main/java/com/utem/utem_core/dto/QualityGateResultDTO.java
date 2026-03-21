package com.utem.utem_core.dto;

import java.util.List;

public record QualityGateResultDTO(
        String runId,
        boolean passed,
        String overallStatus,
        List<GateViolation> violations,
        GateMetrics metrics
) {

    public record GateViolation(
            String rule,
            String message,
            double actual,
            double threshold
    ) {}

    public record GateMetrics(
            double failRate,
            double avgFlakinessScore,
            int newFailures,
            int totalTests,
            int failedTests,
            int flakyTestCount
    ) {}
}

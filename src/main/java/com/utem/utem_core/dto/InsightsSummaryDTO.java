package com.utem.utem_core.dto;

import java.util.List;

public record InsightsSummaryDTO(
        int recentRunsAnalyzed,
        double overallHealthScore,       // 0–100 composite metric
        List<FailureHotspotDTO> topFailures,
        List<FailureClusterDTO> topClusters,
        List<SlowTestDTO> topSlowTests,
        int totalFailureClusters,
        int totalHotspots
) {}

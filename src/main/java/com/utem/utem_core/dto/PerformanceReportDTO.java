package com.utem.utem_core.dto;

import java.util.List;
import java.util.Map;

public record PerformanceReportDTO(
        int recentRunsAnalyzed,
        List<SlowTestDTO> slowestTests,
        Map<String, DurationStatsDTO> durationByNodeType  // key = "SUITE", "SCENARIO", etc.
) {}

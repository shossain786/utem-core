package com.utem.utem_core.dto;

import java.util.List;

public record FailureInsightsDTO(
        int recentRunsAnalyzed,
        List<FailureHotspotDTO> hotspots,
        List<FailureClusterDTO> clusters
) {}

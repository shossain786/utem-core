package com.utem.utem_core.dto;

import java.time.Instant;
import java.util.List;

public record TrendDataDTO(
        String metric,
        int limit,
        List<TrendPoint> points
) {
    public record TrendPoint(
            String runId,
            String runName,
            Instant startTime,
            Double value
    ) {}
}

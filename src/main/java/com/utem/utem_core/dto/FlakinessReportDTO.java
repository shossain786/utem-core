package com.utem.utem_core.dto;

import java.util.List;

/**
 * Aggregate flakiness report for a run or overall.
 */
public record FlakinessReportDTO(
        int totalTests,
        int flakyTests,
        double flakinessPercentage,
        List<FlakyTestDTO> topFlakyTests
) {}

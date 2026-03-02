package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestRun;

import java.time.Instant;

/**
 * Summary of a named recurring test job (grouped by TestRun.jobName).
 */
public record JobSummaryDTO(
        String jobName,
        TestRun.RunStatus latestStatus,
        String latestRunId,
        Instant lastRunAt,
        long totalRuns
) {
    public static JobSummaryDTO from(String jobName, TestRun latestRun, long totalRuns) {
        return new JobSummaryDTO(
                jobName,
                latestRun.getStatus(),
                latestRun.getId(),
                latestRun.getStartTime(),
                totalRuns
        );
    }
}

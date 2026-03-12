package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestRun;

import java.time.Duration;
import java.time.Instant;

/**
 * Lightweight test run summary for list views.
 */
public record TestRunSummaryDTO(
        String id,
        String name,
        TestRun.RunStatus status,
        Instant startTime,
        Instant endTime,
        Long duration,
        Integer totalTests,
        Integer passedTests,
        Integer failedTests,
        Integer skippedTests,
        Double passRate,
        boolean archived,
        boolean pinned,
        String label,
        String jobName
) {
    public static TestRunSummaryDTO from(TestRun run) {
        Long duration = null;
        if (run.getStartTime() != null && run.getEndTime() != null) {
            duration = Duration.between(run.getStartTime(), run.getEndTime()).toMillis();
        }

        Double passRate = null;
        if (run.getTotalTests() != null && run.getTotalTests() > 0 && run.getPassedTests() != null) {
            passRate = (run.getPassedTests() * 100.0) / run.getTotalTests();
        }

        return new TestRunSummaryDTO(
                run.getId(),
                run.getName(),
                run.getStatus(),
                run.getStartTime(),
                run.getEndTime(),
                duration,
                run.getTotalTests(),
                run.getPassedTests(),
                run.getFailedTests(),
                run.getSkippedTests(),
                passRate,
                run.isArchived(),
                run.isPinned(),
                run.getLabel(),
                run.getJobName()
        );
    }
}

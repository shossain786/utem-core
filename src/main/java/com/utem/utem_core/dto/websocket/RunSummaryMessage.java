package com.utem.utem_core.dto.websocket;

import com.utem.utem_core.entity.TestRun;

import java.time.Instant;

/**
 * WebSocket message for test run summary/statistics updates.
 */
public record RunSummaryMessage(
    String runId,
    String testRunId,
    TestRun.RunStatus status,
    Integer totalTests,
    Integer passedTests,
    Integer failedTests,
    Integer skippedTests,
    Instant lastUpdated
) {
    public static RunSummaryMessage from(String runId, TestRun testRun) {
        return new RunSummaryMessage(
            runId,
            testRun.getId(),
            testRun.getStatus(),
            testRun.getTotalTests(),
            testRun.getPassedTests(),
            testRun.getFailedTests(),
            testRun.getSkippedTests(),
            Instant.now()
        );
    }
}

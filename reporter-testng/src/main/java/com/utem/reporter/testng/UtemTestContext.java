package com.utem.reporter.testng;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds the UTEM context for the currently executing test on this thread.
 * Set by {@link UtemTestNGListener#onTestStart} and cleared after the test finishes.
 * Used by {@link UtemSteps} to report steps without needing a direct reference to the listener.
 */
final class UtemTestContext {

    static final ThreadLocal<UtemTestContext> current = new ThreadLocal<>();

    final String runId;
    final String caseEventId;
    final EventQueue eventQueue;
    final EventBuilder builder;
    final AtomicInteger stepCounter = new AtomicInteger(0);

    UtemTestContext(String runId, String caseEventId,
                    EventQueue eventQueue, EventBuilder builder) {
        this.runId = runId;
        this.caseEventId = caseEventId;
        this.eventQueue = eventQueue;
        this.builder = builder;
    }
}

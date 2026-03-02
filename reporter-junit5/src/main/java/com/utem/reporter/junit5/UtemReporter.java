package com.utem.reporter.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUnit 5 TestExecutionListener that reports test lifecycle events to UTEM Core.
 * <p>
 * Auto-discovered via SPI (META-INF/services). Configure the server URL with:
 * <ul>
 *   <li>System property: {@code -Dutem.server.url=http://host:port/utem}</li>
 *   <li>Environment variable: {@code UTEM_SERVER_URL}</li>
 *   <li>Default: {@code http://localhost:8080/utem}</li>
 * </ul>
 */
public class UtemReporter implements TestExecutionListener {

    private final String runId = UUID.randomUUID().toString();
    private final UtemConfig config = new UtemConfig();
    private final UtemHttpClient httpClient = new UtemHttpClient(config);
    private final EventQueue eventQueue = new EventQueue(httpClient);
    private final EventBuilder builder = new EventBuilder();

    // Maps TestIdentifier.uniqueId -> generated eventId for the START event
    private final Map<String, String> identifierToEventId = new ConcurrentHashMap<>();
    // Maps TestIdentifier.uniqueId -> start time in millis
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();

    private String runEventId;
    private final AtomicInteger totalTests = new AtomicInteger();
    private final AtomicInteger passedTests = new AtomicInteger();
    private final AtomicInteger failedTests = new AtomicInteger();
    private final AtomicInteger skippedTests = new AtomicInteger();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        runEventId = UUID.randomUUID().toString();
        long count = testPlan.countTestIdentifiers(TestIdentifier::isTest);
        totalTests.set((int) count);

        String json = builder.buildRunStarted(runEventId, runId, "JUnit 5 Test Run", config.getRunLabel());
        eventQueue.enqueue(json);

        System.out.println("[UTEM] Test run started: " + runId);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        String eventId = UUID.randomUUID().toString();
        String json = builder.buildRunFinished(eventId, runId, runEventId,
                totalTests.get(), passedTests.get(), failedTests.get(), skippedTests.get());
        eventQueue.enqueue(json);

        eventQueue.flush();
        System.out.println("[UTEM] Test run finished: " + passedTests.get() + " passed, "
                + failedTests.get() + " failed, " + skippedTests.get() + " skipped");
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        String eventId = UUID.randomUUID().toString();
        identifierToEventId.put(testIdentifier.getUniqueId(), eventId);
        startTimes.put(testIdentifier.getUniqueId(), System.currentTimeMillis());

        String parentEventId = resolveParentEventId(testIdentifier);

        if (testIdentifier.isTest()) {
            String json = builder.buildCaseStarted(eventId, runId, parentEventId,
                    testIdentifier.getDisplayName());
            eventQueue.enqueue(json);
        } else if (isReportableContainer(testIdentifier)) {
            String json = builder.buildSuiteStarted(eventId, runId, parentEventId,
                    testIdentifier.getDisplayName());
            eventQueue.enqueue(json);
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        String startEventId = identifierToEventId.get(testIdentifier.getUniqueId());
        if (startEventId == null) return;

        Long startTime = startTimes.remove(testIdentifier.getUniqueId());
        Long duration = startTime != null ? System.currentTimeMillis() - startTime : null;

        if (testIdentifier.isTest()) {
            handleTestFinished(startEventId, result, duration);
        } else if (isReportableContainer(testIdentifier)) {
            String nodeStatus = deriveContainerStatus(result);
            String json = builder.buildSuiteFinished(
                    UUID.randomUUID().toString(), runId, startEventId, nodeStatus, duration);
            eventQueue.enqueue(json);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        // For skipped tests, we need to send START + SKIPPED + FINISHED
        String eventId = UUID.randomUUID().toString();
        identifierToEventId.put(testIdentifier.getUniqueId(), eventId);
        String parentEventId = resolveParentEventId(testIdentifier);

        if (testIdentifier.isTest()) {
            skippedTests.incrementAndGet();

            String startJson = builder.buildCaseStarted(eventId, runId, parentEventId,
                    testIdentifier.getDisplayName());
            eventQueue.enqueue(startJson);

            String skipJson = builder.buildTestSkipped(
                    UUID.randomUUID().toString(), runId, eventId, reason);
            eventQueue.enqueue(skipJson);

            String finishJson = builder.buildCaseFinished(
                    UUID.randomUUID().toString(), runId, eventId, "SKIPPED", 0L);
            eventQueue.enqueue(finishJson);
        }
    }

    // ── Private helpers ─────────────────────────────────────────────

    private void handleTestFinished(String startEventId, TestExecutionResult result, Long duration) {
        switch (result.getStatus()) {
            case SUCCESSFUL -> {
                passedTests.incrementAndGet();
                String passJson = builder.buildTestPassed(
                        UUID.randomUUID().toString(), runId, startEventId, duration);
                eventQueue.enqueue(passJson);
                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, startEventId, "PASSED", duration);
                eventQueue.enqueue(finishJson);
            }
            case FAILED -> {
                failedTests.incrementAndGet();
                Throwable throwable = result.getThrowable().orElse(null);
                String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
                String stackTrace = throwable != null ? stackTraceToString(throwable) : null;

                String failJson = builder.buildTestFailed(
                        UUID.randomUUID().toString(), runId, startEventId,
                        duration, errorMessage, stackTrace);
                eventQueue.enqueue(failJson);

                captureScreenshotIfAvailable(startEventId);

                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, startEventId, "FAILED", duration);
                eventQueue.enqueue(finishJson);
            }
            case ABORTED -> {
                skippedTests.incrementAndGet();
                String skipJson = builder.buildTestSkipped(
                        UUID.randomUUID().toString(), runId, startEventId, "Aborted");
                eventQueue.enqueue(skipJson);
                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, startEventId, "SKIPPED", duration);
                eventQueue.enqueue(finishJson);
            }
        }
    }

    private String resolveParentEventId(TestIdentifier identifier) {
        return identifier.getParentId()
                .map(identifierToEventId::get)
                .orElse(runEventId);
    }

    /**
     * Skip root engine containers (e.g., "[engine:junit-jupiter]") that have no parent.
     */
    private boolean isReportableContainer(TestIdentifier identifier) {
        return !identifier.isTest() && identifier.getParentId().isPresent();
    }

    private String deriveContainerStatus(TestExecutionResult result) {
        return switch (result.getStatus()) {
            case SUCCESSFUL -> "PASSED";
            case FAILED -> "FAILED";
            case ABORTED -> "SKIPPED";
        };
    }

    private void captureScreenshotIfAvailable(String testCaseEventId) {
        try {
            Class.forName("org.openqa.selenium.TakesScreenshot");
        } catch (ClassNotFoundException e) {
            return; // Selenium not on classpath
        }

        Object driver = WebDriverRegistry.get();
        if (driver == null) return;

        try {
            org.openqa.selenium.TakesScreenshot screenshotter =
                    (org.openqa.selenium.TakesScreenshot) driver;
            File screenshot = screenshotter.getScreenshotAs(org.openqa.selenium.OutputType.FILE);

            String attachmentEventId = UUID.randomUUID().toString();
            String json = builder.buildAttachment(attachmentEventId, runId, testCaseEventId,
                    "failure-screenshot.png", "image/png", screenshot.length(), true);
            eventQueue.enqueue(json);
            httpClient.uploadFile(attachmentEventId, screenshot.toPath(), "failure-screenshot.png");
        } catch (Exception e) {
            System.err.println("[UTEM] Screenshot capture failed: " + e.getMessage());
        }
    }

    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

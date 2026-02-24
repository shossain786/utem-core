package com.utem.reporter.junit5;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cucumber plugin that reports test lifecycle events to UTEM Core.
 * <p>
 * Usage in {@code @CucumberOptions}:
 * <pre>
 * &#64;CucumberOptions(plugin = {"com.utem.reporter.junit5.UtemCucumberPlugin"})
 * </pre>
 * <p>
 * Configure the server URL with:
 * <ul>
 *   <li>System property: {@code -Dutem.server.url=http://host:port/utem}</li>
 *   <li>Environment variable: {@code UTEM_SERVER_URL}</li>
 *   <li>Default: {@code http://localhost:8080/utem}</li>
 * </ul>
 */
public class UtemCucumberPlugin implements ConcurrentEventListener {

    private final String runId = UUID.randomUUID().toString();
    private final UtemConfig config = new UtemConfig();
    private final UtemHttpClient httpClient = new UtemHttpClient(config);
    private final EventQueue eventQueue = new EventQueue(httpClient);
    private final EventBuilder builder = new EventBuilder();

    private String runEventId;

    // Maps feature URI -> eventId for the feature's SUITE_STARTED event
    private final Map<URI, String> featureEventIds = new ConcurrentHashMap<>();
    // Maps TestCase id -> eventId for CASE_STARTED event
    private final Map<String, String> caseEventIds = new ConcurrentHashMap<>();
    // Maps TestCase id -> start time
    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    // Maps TestCase id -> step counter for ordering steps
    private final Map<String, AtomicInteger> stepCounters = new ConcurrentHashMap<>();

    private final AtomicInteger totalTests = new AtomicInteger();
    private final AtomicInteger passedTests = new AtomicInteger();
    private final AtomicInteger failedTests = new AtomicInteger();
    private final AtomicInteger skippedTests = new AtomicInteger();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onRunStarted);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onCaseStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private void onRunStarted(TestRunStarted event) {
        runEventId = UUID.randomUUID().toString();
        String json = builder.buildRunStarted(runEventId, runId, "Cucumber Test Run");
        eventQueue.enqueue(json);
        System.out.println("[UTEM] Cucumber test run started: " + runId);
    }

    private void onCaseStarted(TestCaseStarted event) {
        totalTests.incrementAndGet();
        TestCase testCase = event.getTestCase();
        String caseKey = testCase.getId().toString();

        // Ensure feature-level suite event is sent (once per feature file)
        String featureEventId = featureEventIds.computeIfAbsent(testCase.getUri(), uri -> {
            String fEventId = UUID.randomUUID().toString();
            String featureName = extractFeatureName(uri);
            String json = builder.buildSuiteStarted(fEventId, runId, runEventId, featureName);
            eventQueue.enqueue(json);
            return fEventId;
        });

        // Send case started
        String caseEventId = UUID.randomUUID().toString();
        caseEventIds.put(caseKey, caseEventId);
        startTimes.put(caseKey, System.currentTimeMillis());

        String json = builder.buildCaseStarted(caseEventId, runId, featureEventId,
                testCase.getName());
        eventQueue.enqueue(json);
    }

    private void onStepFinished(TestStepFinished event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep pickleStep)) return;

        String caseKey = event.getTestCase().getId().toString();
        String caseEventId = caseEventIds.get(caseKey);
        if (caseEventId == null) return;

        int order = stepCounters.computeIfAbsent(caseKey, k -> new AtomicInteger(0))
                .incrementAndGet();

        Result result = event.getResult();
        long durationMs = result.getDuration() != null ? result.getDuration().toMillis() : 0L;

        String stepStatus = switch (result.getStatus()) {
            case PASSED -> "PASSED";
            case FAILED -> "FAILED";
            default     -> "SKIPPED";
        };

        String errorMessage = null;
        String stackTrace = null;
        if (result.getError() != null) {
            errorMessage = result.getError().getMessage() != null
                    ? result.getError().getMessage()
                    : result.getError().getClass().getName();
            stackTrace = stackTraceToString(result.getError());
        }

        String stepName = pickleStep.getStep().getKeyword().trim()
                + " " + pickleStep.getStep().getText();

        String json = builder.buildTestStep(
                UUID.randomUUID().toString(), runId, caseEventId,
                stepName, stepStatus, order, durationMs, errorMessage, stackTrace);
        eventQueue.enqueue(json);

        // Capture screenshot here (before @After hooks close the browser)
        if (result.getStatus() == Status.FAILED) {
            captureScreenshotIfAvailable(caseEventId);
        }
    }

    private void onCaseFinished(TestCaseFinished event) {
        TestCase testCase = event.getTestCase();
        String caseKey = testCase.getId().toString();
        stepCounters.remove(caseKey);
        String caseEventId = caseEventIds.remove(caseKey);
        if (caseEventId == null) return;

        Long startTime = startTimes.remove(caseKey);
        Long duration = startTime != null ? System.currentTimeMillis() - startTime : null;

        Result result = event.getResult();
        Status status = result.getStatus();

        switch (status) {
            case PASSED -> {
                passedTests.incrementAndGet();
                String passJson = builder.buildTestPassed(
                        UUID.randomUUID().toString(), runId, caseEventId, duration);
                eventQueue.enqueue(passJson);
                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, caseEventId, "PASSED", duration);
                eventQueue.enqueue(finishJson);
            }
            case FAILED -> {
                failedTests.incrementAndGet();
                Throwable error = result.getError();
                String errorMessage = error != null ? error.getMessage() : "Unknown error";
                String stackTrace = error != null ? stackTraceToString(error) : null;

                String failJson = builder.buildTestFailed(
                        UUID.randomUUID().toString(), runId, caseEventId,
                        duration, errorMessage, stackTrace);
                eventQueue.enqueue(failJson);

                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, caseEventId, "FAILED", duration);
                eventQueue.enqueue(finishJson);
            }
            case SKIPPED, PENDING, UNDEFINED, AMBIGUOUS -> {
                skippedTests.incrementAndGet();
                String reason = status.name();
                if (result.getError() != null) {
                    reason = result.getError().getMessage();
                }
                String skipJson = builder.buildTestSkipped(
                        UUID.randomUUID().toString(), runId, caseEventId, reason);
                eventQueue.enqueue(skipJson);
                String finishJson = builder.buildCaseFinished(
                        UUID.randomUUID().toString(), runId, caseEventId, "SKIPPED", duration);
                eventQueue.enqueue(finishJson);
            }
        }
    }

    private void onRunFinished(TestRunFinished event) {
        // Finish all feature suites
        for (Map.Entry<URI, String> entry : featureEventIds.entrySet()) {
            String nodeStatus = failedTests.get() > 0 ? "FAILED" : "PASSED";
            String json = builder.buildSuiteFinished(
                    UUID.randomUUID().toString(), runId, entry.getValue(), nodeStatus, null);
            eventQueue.enqueue(json);
        }

        // Finish the run
        String eventId = UUID.randomUUID().toString();
        String json = builder.buildRunFinished(eventId, runId, runEventId,
                totalTests.get(), passedTests.get(), failedTests.get(), skippedTests.get());
        eventQueue.enqueue(json);

        eventQueue.flush();
        System.out.println("[UTEM] Cucumber test run finished: " + passedTests.get() + " passed, "
                + failedTests.get() + " failed, " + skippedTests.get() + " skipped");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String extractFeatureName(URI uri) {
        String path = uri.getSchemeSpecificPart();
        if (path == null) path = uri.toString();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) path = path.substring(lastSlash + 1);
        // Remove .feature extension
        if (path.endsWith(".feature")) path = path.substring(0, path.length() - 8);
        return path;
    }

    private void captureScreenshotIfAvailable(String testCaseEventId) {
        try {
            Class.forName("org.openqa.selenium.TakesScreenshot");
        } catch (ClassNotFoundException e) {
            return;
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
            // Send synchronously so the DB record exists before the file upload arrives
            httpClient.sendEvent(json);
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

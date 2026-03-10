package com.utem.reporter.testng;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UTEM reporter for TestNG. Implements {@link ISuiteListener} and {@link ITestListener}
 * to stream test events to UTEM Core in real time.
 *
 * <p>Registration options:
 * <ol>
 *   <li>Auto-discovery (recommended): Add the JAR to the test classpath — discovered via
 *       {@code META-INF/services/org.testng.ITestNGListener}.</li>
 *   <li>testng.xml: {@code <listener class-name="com.utem.reporter.testng.UtemTestNGListener"/>}</li>
 * </ol>
 *
 * <p>Configuration: {@code -Dutem.server.url=http://host:8080/utem} or
 * {@code UTEM_SERVER_URL} environment variable.
 */
public class UtemTestNGListener implements ISuiteListener, ITestListener {

    private final UtemConfig config = new UtemConfig();
    private final UtemHttpClient httpClient = new UtemHttpClient(config);
    private final EventQueue eventQueue = new EventQueue(httpClient);
    private final EventBuilder builder = new EventBuilder();

    private volatile boolean disabled = false;

    /** runId shared across all events in one suite execution. */
    private volatile String runId;
    /** eventId of the TEST_RUN_STARTED event. */
    private volatile String runEventId;

    /** testContext name → eventId of TEST_SUITE_STARTED. */
    private final ConcurrentHashMap<String, String> contextToEventId = new ConcurrentHashMap<>();
    /** ITestResult identity hash → eventId of TEST_CASE_STARTED. */
    private final ConcurrentHashMap<Integer, String> resultToEventId = new ConcurrentHashMap<>();

    /** Accumulated run-level counts (updated in onFinish(ITestContext)). */
    private final AtomicInteger totalTests   = new AtomicInteger(0);
    private final AtomicInteger passedTests  = new AtomicInteger(0);
    private final AtomicInteger failedTests  = new AtomicInteger(0);
    private final AtomicInteger skippedTests = new AtomicInteger(0);

    // ── ISuiteListener ───────────────────────────────────────────────

    @Override
    public void onStart(ISuite suite) {
        if (config.isDisabled()) {
            disabled = true;
            System.out.println("[UTEM] Reporter disabled via utem.disabled=true — no events will be sent");
            return;
        }
        runId = UUID.randomUUID().toString();
        runEventId = UUID.randomUUID().toString();
        String runName = config.getRunName(suite.getName());
        eventQueue.enqueue(builder.buildRunStarted(runEventId, runId, runName,
                config.getRunLabel(), config.getJobName()));
    }

    @Override
    public void onFinish(ISuite suite) {
        if (disabled) return;
        String finishId = UUID.randomUUID().toString();
        eventQueue.enqueue(builder.buildRunFinished(
                finishId, runId, runEventId,
                totalTests.get(), passedTests.get(), failedTests.get(), skippedTests.get()));
        eventQueue.flush();
    }

    // ── ITestListener ────────────────────────────────────────────────

    @Override
    public void onStart(ITestContext context) {
        if (disabled) return;
        String suiteEventId = UUID.randomUUID().toString();
        contextToEventId.put(context.getName(), suiteEventId);
        eventQueue.enqueue(builder.buildSuiteStarted(suiteEventId, runId, runEventId, context.getName()));
    }

    @Override
    public void onFinish(ITestContext context) {
        if (disabled) return;
        String suiteEventId = contextToEventId.remove(context.getName());
        if (suiteEventId == null) return;

        int ctxPassed  = context.getPassedTests().size();
        int ctxFailed  = context.getFailedTests().size();
        int ctxSkipped = context.getSkippedTests().size();

        passedTests.addAndGet(ctxPassed);
        failedTests.addAndGet(ctxFailed);
        skippedTests.addAndGet(ctxSkipped);
        totalTests.addAndGet(ctxPassed + ctxFailed + ctxSkipped);

        String status = ctxFailed > 0 ? "FAILED" : "PASSED";
        long duration = context.getEndDate().getTime() - context.getStartDate().getTime();
        eventQueue.enqueue(builder.buildSuiteFinished(UUID.randomUUID().toString(), runId, suiteEventId, status, duration));
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (disabled) return;
        String caseEventId = UUID.randomUUID().toString();
        resultToEventId.put(System.identityHashCode(result), caseEventId);
        String parentId = contextToEventId.getOrDefault(result.getTestContext().getName(), runEventId);
        eventQueue.enqueue(builder.buildCaseStarted(caseEventId, runId, parentId, buildTestName(result)));
        UtemTestContext.current.set(new UtemTestContext(runId, caseEventId, eventQueue, builder));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (disabled) return;
        UtemTestContext.current.remove();
        String caseEventId = resultToEventId.remove(System.identityHashCode(result));
        if (caseEventId == null) return;
        long duration = result.getEndMillis() - result.getStartMillis();
        eventQueue.enqueue(builder.buildTestPassed(UUID.randomUUID().toString(), runId, caseEventId, duration));
        eventQueue.enqueue(builder.buildCaseFinished(UUID.randomUUID().toString(), runId, caseEventId, "PASSED", duration));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (disabled) return;
        UtemTestContext.current.remove();
        String caseEventId = resultToEventId.remove(System.identityHashCode(result));
        if (caseEventId == null) return;
        long duration = result.getEndMillis() - result.getStartMillis();
        String error = extractMessage(result.getThrowable());
        String stack = extractStack(result.getThrowable());
        eventQueue.enqueue(builder.buildTestFailed(UUID.randomUUID().toString(), runId, caseEventId, duration, error, stack));
        captureScreenshot(caseEventId);
        eventQueue.enqueue(builder.buildCaseFinished(UUID.randomUUID().toString(), runId, caseEventId, "FAILED", duration));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (disabled) return;
        UtemTestContext.current.remove();
        String caseEventId = resultToEventId.remove(System.identityHashCode(result));
        if (caseEventId == null) return;
        long duration = result.getEndMillis() - result.getStartMillis();
        String reason = extractMessage(result.getThrowable());
        eventQueue.enqueue(builder.buildTestSkipped(UUID.randomUUID().toString(), runId, caseEventId, reason));
        eventQueue.enqueue(builder.buildCaseFinished(UUID.randomUUID().toString(), runId, caseEventId, "SKIPPED", duration));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String buildTestName(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            StringBuilder sb = new StringBuilder(methodName).append("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i]);
            }
            sb.append(")");
            return sb.toString();
        }
        return methodName;
    }

    private String extractMessage(Throwable t) {
        if (t == null) return null;
        return t.getMessage() != null ? t.getMessage() : t.getClass().getName();
    }

    private String extractStack(Throwable t) {
        if (t == null) return null;
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Captures a Selenium screenshot on the current thread, if a WebDriver is registered. */
    private void captureScreenshot(String parentEventId) {
        try {
            Class<?> takesScreenshotClass = Class.forName("org.openqa.selenium.TakesScreenshot");
            Object driver = WebDriverRegistry.get();
            if (driver == null || !takesScreenshotClass.isInstance(driver)) return;

            Class<?> outputTypeClass = Class.forName("org.openqa.selenium.OutputType");
            Object fileType = outputTypeClass.getField("FILE").get(null);
            Object screenshotObj = takesScreenshotClass.getMethod("getScreenshotAs", outputTypeClass).invoke(driver, fileType);
            java.io.File screenshotFile = (java.io.File) screenshotObj;
            Path screenshotPath = screenshotFile.toPath();
            long fileSize = Files.size(screenshotPath);

            String attachEventId = UUID.randomUUID().toString();
            String attachJson = builder.buildAttachment(
                    attachEventId, runId, parentEventId,
                    "failure-screenshot.png", "image/png", fileSize, true);
            httpClient.sendEvent(attachJson);
            httpClient.uploadFile(attachEventId, screenshotPath, "failure-screenshot.png");
        } catch (ClassNotFoundException ignored) {
            // Selenium not on classpath — skip screenshot
        } catch (Exception e) {
            System.err.println("[UTEM] Screenshot capture failed: " + e.getMessage());
        }
    }
}

package com.utem.reporter.testng;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Static helper for reporting named test steps to UTEM from within TestNG tests.
 *
 * <p>Works automatically when {@link UtemTestNGListener} is registered — no extra setup needed.
 *
 * <h3>Usage — step label only (reported as PASSED immediately):</h3>
 * <pre>{@code
 * UtemSteps.step("Enter username 'admin'");
 * usernameField.sendKeys("admin");
 *
 * UtemSteps.step("Click Login button");
 * loginBtn.click();
 * }</pre>
 *
 * <h3>Usage — step with action (auto-timed, PASSED/FAILED based on outcome):</h3>
 * <pre>{@code
 * UtemSteps.step("Enter username 'admin'",   () -> usernameField.sendKeys("admin"));
 * UtemSteps.step("Enter password",            () -> passwordField.sendKeys("password"));
 * UtemSteps.step("Click Login button",        () -> loginBtn.click());
 * UtemSteps.step("Verify success message",    () -> Assert.assertTrue(banner.isDisplayed()));
 * }</pre>
 *
 * <p>If called outside a UTEM-managed test the {@code step(name)} form is a no-op and
 * {@code step(name, action)} simply runs the action unchanged.
 */
public final class UtemSteps {

    private UtemSteps() {}

    /**
     * Reports a step as PASSED immediately. The step name is logged before you perform the
     * actual action so it appears in UTEM even if the subsequent code throws.
     *
     * @param name Human-readable step description
     */
    public static void step(String name) {
        UtemTestContext ctx = UtemTestContext.current.get();
        if (ctx == null) return;

        int order = ctx.stepCounter.incrementAndGet();
        String json = ctx.builder.buildTestStep(
                UUID.randomUUID().toString(), ctx.runId, ctx.caseEventId,
                name, "PASSED", order, null, null, null);
        ctx.eventQueue.enqueue(json);
    }

    /**
     * Executes {@code action}, timing it and reporting the step as PASSED or FAILED.
     * Any exception thrown by the action is re-thrown so TestNG still sees the failure.
     *
     * @param name   Human-readable step description
     * @param action Code to execute for this step
     */
    public static void step(String name, Runnable action) {
        UtemTestContext ctx = UtemTestContext.current.get();
        if (ctx == null) {
            action.run(); // no UTEM context — just run the action
            return;
        }

        int order = ctx.stepCounter.incrementAndGet();
        long start = System.currentTimeMillis();
        try {
            action.run();
            long duration = System.currentTimeMillis() - start;
            String json = ctx.builder.buildTestStep(
                    UUID.randomUUID().toString(), ctx.runId, ctx.caseEventId,
                    name, "PASSED", order, duration, null, null);
            ctx.eventQueue.enqueue(json);
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            String json = ctx.builder.buildTestStep(
                    UUID.randomUUID().toString(), ctx.runId, ctx.caseEventId,
                    name, "FAILED", order, duration,
                    t.getMessage() != null ? t.getMessage() : t.getClass().getName(),
                    extractStack(t));
            ctx.eventQueue.enqueue(json);
            rethrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    private static String extractStack(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

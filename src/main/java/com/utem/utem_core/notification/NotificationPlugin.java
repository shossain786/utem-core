package com.utem.utem_core.notification;

import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.entity.TestRun;

import java.util.List;

/**
 * SPI for UTEM notification plugins.
 * <p>
 * Implement this interface and annotate with {@code @Component} to register a plugin.
 * All enabled plugins are called automatically when a test run finishes.
 */
public interface NotificationPlugin {

    /** Short name used in logs (e.g. "Jenkins", "Teams"). */
    String getName();

    /** Whether this plugin should fire. Typically reads a config property. */
    boolean isEnabled();

    /** Called once after a test run transitions to PASSED, FAILED, or ABORTED. */
    void onRunCompleted(TestRun run);

    /**
     * Called when one or more tests in a completed run exceed the flakiness threshold.
     * Default is a no-op — override to send flakiness alerts.
     */
    default void onFlakinessThresholdBreached(TestRun run, List<FlakyTestDTO> flakyTests, double threshold) {}
}

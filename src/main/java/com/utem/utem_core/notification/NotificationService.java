package com.utem.utem_core.notification;

import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.entity.TestRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatches run-completion events to all registered {@link NotificationPlugin} beans.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /** Spring auto-collects every @Component implementing NotificationPlugin. */
    private final List<NotificationPlugin> plugins;

    /**
     * Notify all enabled plugins that a test run has completed.
     * Exceptions in individual plugins are caught and logged so others still run.
     */
    public void notifyRunCompleted(TestRun run) {
        for (NotificationPlugin plugin : plugins) {
            if (!plugin.isEnabled()) continue;
            try {
                plugin.onRunCompleted(run);
                log.debug("Notification plugin '{}' fired for run {}", plugin.getName(), run.getId());
            } catch (Exception e) {
                log.error("Notification plugin '{}' failed for run {}: {}", plugin.getName(), run.getId(), e.getMessage(), e);
            }
        }
    }

    public void notifyFlakinessThresholdBreached(TestRun run, List<FlakyTestDTO> flakyTests, double threshold) {
        for (NotificationPlugin plugin : plugins) {
            if (!plugin.isEnabled()) continue;
            try {
                plugin.onFlakinessThresholdBreached(run, flakyTests, threshold);
                log.debug("Flakiness alert plugin '{}' fired for run {}", plugin.getName(), run.getId());
            } catch (Exception e) {
                log.error("Flakiness alert plugin '{}' failed for run {}: {}", plugin.getName(), run.getId(), e.getMessage(), e);
            }
        }
    }
}

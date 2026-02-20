package com.utem.utem_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for stale run recovery.
 * Runs that remain in RUNNING status longer than {@link #staleThresholdMinutes} are
 * automatically marked as ABORTED by {@code RunRecoveryService}.
 */
@ConfigurationProperties(prefix = "utem.recovery")
public record RecoveryProperties(boolean enabled, long checkIntervalMs, int staleThresholdMinutes) {

    public RecoveryProperties {
        if (checkIntervalMs <= 0) checkIntervalMs = 300_000L;   // 5 minutes
        if (staleThresholdMinutes <= 0) staleThresholdMinutes = 60;
    }
}

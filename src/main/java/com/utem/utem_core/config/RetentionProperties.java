package com.utem.utem_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for data retention policy.
 * Controls automatic cleanup of old test run data.
 */
@ConfigurationProperties(prefix = "utem.retention")
public record RetentionProperties(
        boolean enabled,
        int retentionDays,
        String cronExpression
) {
    public RetentionProperties {
        if (retentionDays <= 0) retentionDays = 30;
        if (cronExpression == null || cronExpression.isBlank()) cronExpression = "0 0 2 * * *";
    }
}

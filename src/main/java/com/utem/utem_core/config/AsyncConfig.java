package com.utem.utem_core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable asynchronous method execution and scheduled tasks.
 * Used for non-blocking WebSocket broadcasts and data retention cleanup.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}

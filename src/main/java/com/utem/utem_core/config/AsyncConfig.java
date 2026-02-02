package com.utem.utem_core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration to enable asynchronous method execution.
 * Used for non-blocking WebSocket broadcasts.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

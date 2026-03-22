package com.utem.utem_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utem.jwt")
public record JwtProperties(String secret, int expiryHours) {
}

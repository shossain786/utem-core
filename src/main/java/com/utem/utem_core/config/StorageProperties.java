package com.utem.utem_core.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * Configuration properties for attachment file storage.
 */
@ConfigurationProperties(prefix = "utem.storage")
@Validated
public record StorageProperties(
        @NotBlank String basePath,
        long maxFileSizeMb,
        Set<String> allowedMimeTypes,
        boolean organizeByRunId
) {
    public StorageProperties {
        if (basePath == null || basePath.isBlank()) {
            basePath = "./utem-attachments";
        }
        if (maxFileSizeMb <= 0) {
            maxFileSizeMb = 50L;
        }
    }

    /**
     * Get maximum file size in bytes.
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }
}

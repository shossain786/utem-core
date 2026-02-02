package com.utem.utem_core.dto;

import com.utem.utem_core.entity.Attachment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Metadata about a stored file.
 */
public record FileMetadata(
        String attachmentId,
        String filename,
        String mimeType,
        long fileSize,
        Instant lastModified,
        boolean exists
) {
    /**
     * Create FileMetadata from an Attachment entity and its file path.
     */
    public static FileMetadata from(Attachment attachment, Path filePath) {
        boolean fileExists = filePath != null && Files.exists(filePath);
        Instant modified = null;

        if (fileExists) {
            try {
                modified = Files.getLastModifiedTime(filePath).toInstant();
            } catch (Exception e) {
                // Ignore - modified will be null
            }
        }

        return new FileMetadata(
                attachment.getId(),
                attachment.getName(),
                attachment.getMimeType(),
                attachment.getFileSize() != null ? attachment.getFileSize() : 0L,
                modified,
                fileExists
        );
    }

    /**
     * Create FileMetadata for a non-existent attachment.
     */
    public static FileMetadata notFound(String attachmentId) {
        return new FileMetadata(attachmentId, null, null, 0, null, false);
    }
}

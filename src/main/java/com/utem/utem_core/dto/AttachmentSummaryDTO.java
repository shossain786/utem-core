package com.utem.utem_core.dto;

import com.utem.utem_core.entity.Attachment;

import java.time.Instant;

/**
 * Lightweight summary of an attachment for hierarchy and search responses.
 */
public record AttachmentSummaryDTO(
        String id,
        String nodeId,
        String name,
        Attachment.AttachmentType type,
        String mimeType,
        Long fileSize,
        Instant timestamp,
        Boolean isFailureScreenshot
) {
    public static AttachmentSummaryDTO from(Attachment attachment) {
        return new AttachmentSummaryDTO(
                attachment.getId(),
                attachment.getTestNode() != null ? attachment.getTestNode().getId() : null,
                attachment.getName(),
                attachment.getType(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getTimestamp(),
                attachment.getIsFailureScreenshot()
        );
    }
}

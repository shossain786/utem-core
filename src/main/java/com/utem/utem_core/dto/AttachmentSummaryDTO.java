package com.utem.utem_core.dto;

import com.utem.utem_core.entity.Attachment;

/**
 * Lightweight summary of an attachment for hierarchy responses.
 */
public record AttachmentSummaryDTO(
    String id,
    String name,
    Attachment.AttachmentType type,
    String mimeType,
    Long fileSize,
    Boolean isFailureScreenshot
) {
    public static AttachmentSummaryDTO from(Attachment attachment) {
        return new AttachmentSummaryDTO(
            attachment.getId(),
            attachment.getName(),
            attachment.getType(),
            attachment.getMimeType(),
            attachment.getFileSize(),
            attachment.getIsFailureScreenshot()
        );
    }
}

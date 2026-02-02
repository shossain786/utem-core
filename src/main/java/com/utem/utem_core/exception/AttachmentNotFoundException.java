package com.utem.utem_core.exception;

/**
 * Exception thrown when an attachment is not found.
 */
public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(String attachmentId) {
        super("Attachment not found with id: " + attachmentId);
    }
}

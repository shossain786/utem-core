package com.utem.utem_core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Centralized exception handler for REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAttachmentNotFound(AttachmentNotFoundException ex) {
        log.warn("Attachment not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "AttachmentNotFound", ex.getMessage());
    }

    @ExceptionHandler(TestRunNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTestRunNotFound(TestRunNotFoundException ex) {
        log.warn("Test run not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "TestRunNotFound", ex.getMessage());
    }

    @ExceptionHandler(TestNodeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTestNodeNotFound(TestNodeNotFoundException ex) {
        log.warn("Test node not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "TestNodeNotFound", ex.getMessage());
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFileValidation(FileValidationException ex) {
        log.warn("File validation failed [{}]: {}", ex.getValidationType(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "FileValidation", ex.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>> handleFileStorage(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "FileStorageError", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = Map.of(
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}

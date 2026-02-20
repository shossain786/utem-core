package com.utem.utem_core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "IllegalState", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", fieldErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ValidationError", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "MalformedRequest", "Request body is not valid JSON");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "InternalError", "An unexpected error occurred");
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

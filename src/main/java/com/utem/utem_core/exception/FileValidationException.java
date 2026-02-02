package com.utem.utem_core.exception;

/**
 * Exception thrown when file validation fails.
 */
public class FileValidationException extends RuntimeException {

    private final String validationType;

    public FileValidationException(String validationType, String message) {
        super(message);
        this.validationType = validationType;
    }

    public String getValidationType() {
        return validationType;
    }
}

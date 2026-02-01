package com.utem.utem_core.exception;

/**
 * Exception thrown when a test run is not found.
 */
public class TestRunNotFoundException extends RuntimeException {

    public TestRunNotFoundException(String runId) {
        super("Test run not found with id: " + runId);
    }
}

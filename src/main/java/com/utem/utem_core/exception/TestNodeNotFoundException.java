package com.utem.utem_core.exception;

/**
 * Exception thrown when a test node is not found.
 */
public class TestNodeNotFoundException extends RuntimeException {

    public TestNodeNotFoundException(String nodeId) {
        super("Test node not found with id: " + nodeId);
    }
}

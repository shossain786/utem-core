package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestStep;

/**
 * Lightweight test step summary for search results.
 */
public record TestStepSummaryDTO(
        String id,
        String nodeId,
        String nodeName,
        String name,
        TestStep.StepStatus status,
        Long duration,
        String errorMessage
) {
    public static TestStepSummaryDTO from(TestStep step) {
        return new TestStepSummaryDTO(
                step.getId(),
                step.getTestNode() != null ? step.getTestNode().getId() : null,
                step.getTestNode() != null ? step.getTestNode().getName() : null,
                step.getName(),
                step.getStatus(),
                step.getDuration(),
                step.getErrorMessage()
        );
    }
}

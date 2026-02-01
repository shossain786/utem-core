package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestStep;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Test step representation for hierarchy API responses.
 */
public record TestStepDTO(
    String id,
    String name,
    TestStep.StepStatus status,
    Instant timestamp,
    Long duration,
    Integer stepOrder,
    String errorMessage,
    String stackTrace,
    List<AttachmentSummaryDTO> attachments
) {
    public static TestStepDTO from(TestStep step) {
        return new TestStepDTO(
            step.getId(),
            step.getName(),
            step.getStatus(),
            step.getTimestamp(),
            step.getDuration(),
            step.getStepOrder(),
            step.getErrorMessage(),
            step.getStackTrace(),
            new ArrayList<>()
        );
    }

    public static TestStepDTO from(TestStep step, List<AttachmentSummaryDTO> attachments) {
        return new TestStepDTO(
            step.getId(),
            step.getName(),
            step.getStatus(),
            step.getTimestamp(),
            step.getDuration(),
            step.getStepOrder(),
            step.getErrorMessage(),
            step.getStackTrace(),
            attachments != null ? attachments : new ArrayList<>()
        );
    }
}

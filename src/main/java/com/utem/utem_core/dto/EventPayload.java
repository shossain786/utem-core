package com.utem.utem_core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.utem.utem_core.entity.Attachment.AttachmentType;
import com.utem.utem_core.entity.TestNode.NodeStatus;
import com.utem.utem_core.entity.TestRun.RunStatus;
import com.utem.utem_core.entity.TestStep.StepStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventPayload(
    // Common fields
    String name,
    String metadata,

    // TestRun specific
    String label,
    String jobName,
    Integer totalTests,
    Integer passedTests,
    Integer failedTests,
    Integer skippedTests,
    RunStatus runStatus,

    // TestNode specific
    NodeStatus nodeStatus,
    Boolean flaky,
    Integer retryCount,
    Long duration,

    // TestStep specific
    Integer stepOrder,
    StepStatus stepStatus,

    // Error info (for TEST_FAILED)
    String errorMessage,
    String stackTrace,

    // Attachment specific
    String filePath,
    String mimeType,
    Long fileSize,
    AttachmentType attachmentType,
    Boolean isFailureScreenshot
) {}

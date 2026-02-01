package com.utem.utem_core.dto;

import com.utem.utem_core.entity.EventLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record EventRequest(
    @NotBlank String eventId,
    @NotBlank String runId,
    @NotNull EventLog.EventType eventType,
    String parentId,
    @NotNull Instant timestamp,
    @NotBlank String payload
) {}

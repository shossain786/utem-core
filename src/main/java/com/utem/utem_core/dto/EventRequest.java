package com.utem.utem_core.dto;

import com.utem.utem_core.entity.EventLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventRequest(
    @NotBlank @Size(max = 100) String eventId,
    @NotBlank @Size(max = 100) String runId,
    @NotNull EventLog.EventType eventType,
    @Size(max = 100) String parentId,
    @NotNull Instant timestamp,
    @NotBlank @Size(max = 1_000_000) String payload
) {}

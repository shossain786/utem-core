package com.utem.utem_core.dto;

import com.utem.utem_core.entity.EventLog;

import java.time.Instant;

public record EventResponse(
    String id,
    String eventId,
    String runId,
    EventLog.EventType eventType,
    String parentId,
    Instant timestamp,
    String payload,
    Instant receivedAt
) {
    public static EventResponse from(EventLog eventLog) {
        return new EventResponse(
            eventLog.getId(),
            eventLog.getEventId(),
            eventLog.getRunId(),
            eventLog.getEventType(),
            eventLog.getParentId(),
            eventLog.getTimestamp(),
            eventLog.getPayload(),
            eventLog.getReceivedAt()
        );
    }
}

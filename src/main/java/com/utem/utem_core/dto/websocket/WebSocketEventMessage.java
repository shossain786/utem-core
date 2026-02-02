package com.utem.utem_core.dto.websocket;

import com.utem.utem_core.entity.EventLog;

import java.time.Instant;

/**
 * WebSocket message payload for real-time event broadcasting.
 */
public record WebSocketEventMessage(
    String eventId,
    String runId,
    EventLog.EventType eventType,
    String parentId,
    Instant timestamp,
    String entityId,
    String entityType,
    String name,
    String status
) {
    public static WebSocketEventMessage from(
            EventLog eventLog,
            String entityId,
            String entityType,
            String name,
            String status
    ) {
        return new WebSocketEventMessage(
            eventLog.getEventId(),
            eventLog.getRunId(),
            eventLog.getEventType(),
            eventLog.getParentId(),
            eventLog.getTimestamp(),
            entityId,
            entityType,
            name,
            status
        );
    }
}

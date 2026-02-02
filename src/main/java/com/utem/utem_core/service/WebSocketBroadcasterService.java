package com.utem.utem_core.service;

import com.utem.utem_core.dto.websocket.RunSummaryMessage;
import com.utem.utem_core.dto.websocket.WebSocketEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting test events to WebSocket subscribers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketBroadcasterService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts an event to subscribers of a specific test run.
     *
     * @param runId The run ID (from EventLog.runId)
     * @param message The event message to broadcast
     */
    @Async
    public void broadcastEvent(String runId, WebSocketEventMessage message) {
        String destination = "/topic/runs/" + runId + "/events";
        log.debug("Broadcasting event {} to {}", message.eventType(), destination);
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("Failed to broadcast event to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Broadcasts run summary/statistics update to subscribers.
     *
     * @param runId The run ID
     * @param summary The updated run summary
     */
    @Async
    public void broadcastSummary(String runId, RunSummaryMessage summary) {
        String destination = "/topic/runs/" + runId + "/summary";
        log.debug("Broadcasting summary update to {}", destination);
        try {
            messagingTemplate.convertAndSend(destination, summary);
        } catch (Exception e) {
            log.error("Failed to broadcast summary to {}: {}", destination, e.getMessage());
        }
    }
}

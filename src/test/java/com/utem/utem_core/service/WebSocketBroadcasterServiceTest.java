package com.utem.utem_core.service;

import com.utem.utem_core.dto.websocket.RunSummaryMessage;
import com.utem.utem_core.dto.websocket.WebSocketEventMessage;
import com.utem.utem_core.entity.EventLog;
import com.utem.utem_core.entity.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketBroadcasterServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketBroadcasterService broadcasterService;

    private String runId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString();
        timestamp = Instant.now();
        broadcasterService = new WebSocketBroadcasterService(messagingTemplate);
    }

    @Test
    @DisplayName("Should broadcast event to correct topic")
    void shouldBroadcastEventToCorrectTopic() {
        WebSocketEventMessage message = createTestEventMessage();

        broadcasterService.broadcastEvent(runId, message);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/runs/" + runId + "/events"),
                eq(message)
        );
    }

    @Test
    @DisplayName("Should broadcast summary to correct topic")
    void shouldBroadcastSummaryToCorrectTopic() {
        RunSummaryMessage summary = createTestSummaryMessage();

        broadcasterService.broadcastSummary(runId, summary);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/runs/" + runId + "/summary"),
                eq(summary)
        );
    }

    @Test
    @DisplayName("Should handle messaging exceptions gracefully for events")
    void shouldHandleMessagingExceptionsGracefullyForEvents() {
        WebSocketEventMessage message = createTestEventMessage();

        doThrow(new MessagingException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> broadcasterService.broadcastEvent(runId, message));
    }

    @Test
    @DisplayName("Should handle messaging exceptions gracefully for summary")
    void shouldHandleMessagingExceptionsGracefullyForSummary() {
        RunSummaryMessage summary = createTestSummaryMessage();

        doThrow(new MessagingException("Connection lost"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> broadcasterService.broadcastSummary(runId, summary));
    }

    @Test
    @DisplayName("Should include run ID in event topic path")
    void shouldIncludeRunIdInEventTopicPath() {
        String specificRunId = "specific-run-123";
        WebSocketEventMessage message = createTestEventMessage();

        broadcasterService.broadcastEvent(specificRunId, message);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/runs/specific-run-123/events"),
                any(WebSocketEventMessage.class)
        );
    }

    @Test
    @DisplayName("Should include run ID in summary topic path")
    void shouldIncludeRunIdInSummaryTopicPath() {
        String specificRunId = "specific-run-456";
        RunSummaryMessage summary = createTestSummaryMessage();

        broadcasterService.broadcastSummary(specificRunId, summary);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/runs/specific-run-456/summary"),
                any(RunSummaryMessage.class)
        );
    }

    private WebSocketEventMessage createTestEventMessage() {
        return new WebSocketEventMessage(
                UUID.randomUUID().toString(),
                runId,
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                timestamp,
                UUID.randomUUID().toString(),
                "TEST_RUN",
                "Test Run",
                "RUNNING"
        );
    }

    private RunSummaryMessage createTestSummaryMessage() {
        return new RunSummaryMessage(
                runId,
                UUID.randomUUID().toString(),
                TestRun.RunStatus.RUNNING,
                10,
                5,
                2,
                1,
                timestamp
        );
    }
}

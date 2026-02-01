package com.utem.utem_core.controller;

import com.utem.utem_core.dto.EventRequest;
import com.utem.utem_core.dto.EventResponse;
import com.utem.utem_core.entity.EventLog;
import com.utem.utem_core.repository.EventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @InjectMocks
    private EventController eventController;

    private String runId;
    private String eventId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString();
        eventId = UUID.randomUUID().toString();
        timestamp = Instant.now();
    }

    @Test
    @DisplayName("Should ingest single event successfully")
    void shouldIngestEventSuccessfully() {
        EventRequest request = new EventRequest(
            eventId,
            runId,
            EventLog.EventType.TEST_RUN_STARTED,
            null,
            timestamp,
            "{\"name\": \"Test Run\"}"
        );

        when(eventLogRepository.existsByEventId(eventId)).thenReturn(false);
        when(eventLogRepository.save(any(EventLog.class))).thenAnswer(invocation -> {
            EventLog event = invocation.getArgument(0);
            event.setId(UUID.randomUUID().toString());
            return event;
        });

        ResponseEntity<EventResponse> response = eventController.ingestEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().eventId()).isEqualTo(eventId);
        assertThat(response.getBody().runId()).isEqualTo(runId);
        assertThat(response.getBody().eventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        assertThat(response.getBody().receivedAt()).isNotNull();

        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"name\": \"Test Run\"}");
    }

    @Test
    @DisplayName("Should reject duplicate event ID")
    void shouldRejectDuplicateEventId() {
        EventRequest request = new EventRequest(
            eventId,
            runId,
            EventLog.EventType.TEST_RUN_STARTED,
            null,
            timestamp,
            "{}"
        );

        when(eventLogRepository.existsByEventId(eventId)).thenReturn(true);

        ResponseEntity<EventResponse> response = eventController.ingestEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNull();
        verify(eventLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should ingest batch events successfully")
    void shouldIngestBatchEventsSuccessfully() {
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();

        List<EventRequest> requests = List.of(
            new EventRequest(
                eventId1,
                runId,
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                timestamp,
                "{\"name\": \"Test Run\"}"
            ),
            new EventRequest(
                eventId2,
                runId,
                EventLog.EventType.TEST_SUITE_STARTED,
                runId,
                timestamp,
                "{\"name\": \"Suite 1\"}"
            )
        );

        when(eventLogRepository.existsByEventId(eventId1)).thenReturn(false);
        when(eventLogRepository.existsByEventId(eventId2)).thenReturn(false);
        when(eventLogRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<EventLog> events = invocation.getArgument(0);
            events.forEach(e -> e.setId(UUID.randomUUID().toString()));
            return events;
        });

        ResponseEntity<List<EventResponse>> response = eventController.ingestEvents(requests);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).eventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        assertThat(response.getBody().get(1).eventType()).isEqualTo(EventLog.EventType.TEST_SUITE_STARTED);
    }

    @Test
    @DisplayName("Should skip duplicate events in batch")
    void shouldSkipDuplicatesInBatch() {
        String existingEventId = UUID.randomUUID().toString();
        String newEventId = UUID.randomUUID().toString();

        List<EventRequest> requests = List.of(
            new EventRequest(
                existingEventId,
                runId,
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                timestamp,
                "{}"
            ),
            new EventRequest(
                newEventId,
                runId,
                EventLog.EventType.TEST_SUITE_STARTED,
                runId,
                timestamp,
                "{}"
            )
        );

        when(eventLogRepository.existsByEventId(existingEventId)).thenReturn(true);
        when(eventLogRepository.existsByEventId(newEventId)).thenReturn(false);
        when(eventLogRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<EventLog> events = invocation.getArgument(0);
            events.forEach(e -> e.setId(UUID.randomUUID().toString()));
            return events;
        });

        ResponseEntity<List<EventResponse>> response = eventController.ingestEvents(requests);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).eventType()).isEqualTo(EventLog.EventType.TEST_SUITE_STARTED);
    }

    @Test
    @DisplayName("Should get event by ID")
    void shouldGetEventById() {
        String id = UUID.randomUUID().toString();
        EventLog event = EventLog.builder()
            .id(id)
            .eventId(eventId)
            .runId(runId)
            .eventType(EventLog.EventType.TEST_RUN_STARTED)
            .timestamp(timestamp)
            .payload("{\"name\": \"Test\"}")
            .receivedAt(Instant.now())
            .build();

        when(eventLogRepository.findById(id)).thenReturn(Optional.of(event));

        ResponseEntity<EventResponse> response = eventController.getEventById(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(id);
        assertThat(response.getBody().eventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
    }

    @Test
    @DisplayName("Should return 404 for non-existent event")
    void shouldReturn404ForNonExistentEvent() {
        String id = UUID.randomUUID().toString();
        when(eventLogRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<EventResponse> response = eventController.getEventById(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should get events by run ID")
    void shouldGetEventsByRunId() {
        EventLog event1 = EventLog.builder()
            .id(UUID.randomUUID().toString())
            .eventId(UUID.randomUUID().toString())
            .runId(runId)
            .eventType(EventLog.EventType.TEST_RUN_STARTED)
            .timestamp(timestamp)
            .payload("{}")
            .receivedAt(Instant.now())
            .build();

        EventLog event2 = EventLog.builder()
            .id(UUID.randomUUID().toString())
            .eventId(UUID.randomUUID().toString())
            .runId(runId)
            .eventType(EventLog.EventType.TEST_SUITE_STARTED)
            .parentId(runId)
            .timestamp(timestamp.plusSeconds(1))
            .payload("{}")
            .receivedAt(Instant.now())
            .build();

        when(eventLogRepository.findByRunIdOrderByTimestampAsc(runId))
            .thenReturn(List.of(event1, event2));

        ResponseEntity<List<EventResponse>> response = eventController.getEventsByRunId(runId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).eventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        assertThat(response.getBody().get(1).eventType()).isEqualTo(EventLog.EventType.TEST_SUITE_STARTED);
    }

    @Test
    @DisplayName("Should get events by event type")
    void shouldGetEventsByEventType() {
        EventLog event = EventLog.builder()
            .id(UUID.randomUUID().toString())
            .eventId(UUID.randomUUID().toString())
            .runId(runId)
            .eventType(EventLog.EventType.TEST_RUN_STARTED)
            .timestamp(timestamp)
            .payload("{}")
            .receivedAt(Instant.now())
            .build();

        when(eventLogRepository.findByEventType(EventLog.EventType.TEST_RUN_STARTED))
            .thenReturn(List.of(event));

        ResponseEntity<List<EventResponse>> response = eventController.getEventsByType(
            EventLog.EventType.TEST_RUN_STARTED,
            null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).eventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
    }

    @Test
    @DisplayName("Should get events by event type and run ID")
    void shouldGetEventsByEventTypeAndRunId() {
        EventLog event = EventLog.builder()
            .id(UUID.randomUUID().toString())
            .eventId(UUID.randomUUID().toString())
            .runId(runId)
            .eventType(EventLog.EventType.TEST_RUN_STARTED)
            .timestamp(timestamp)
            .payload("{}")
            .receivedAt(Instant.now())
            .build();

        when(eventLogRepository.findByRunIdAndEventType(runId, EventLog.EventType.TEST_RUN_STARTED))
            .thenReturn(List.of(event));

        ResponseEntity<List<EventResponse>> response = eventController.getEventsByType(
            EventLog.EventType.TEST_RUN_STARTED,
            runId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).runId()).isEqualTo(runId);
    }

    @Test
    @DisplayName("Should handle event with parent ID")
    void shouldHandleEventWithParentId() {
        String parentId = UUID.randomUUID().toString();
        EventRequest request = new EventRequest(
            eventId,
            runId,
            EventLog.EventType.TEST_SUITE_STARTED,
            parentId,
            timestamp,
            "{\"name\": \"Suite\"}"
        );

        when(eventLogRepository.existsByEventId(eventId)).thenReturn(false);
        when(eventLogRepository.save(any(EventLog.class))).thenAnswer(invocation -> {
            EventLog event = invocation.getArgument(0);
            event.setId(UUID.randomUUID().toString());
            return event;
        });

        ResponseEntity<EventResponse> response = eventController.ingestEvent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().parentId()).isEqualTo(parentId);

        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        verify(eventLogRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("Should handle all event types")
    void shouldHandleAllEventTypes() {
        for (EventLog.EventType eventType : EventLog.EventType.values()) {
            String testEventId = UUID.randomUUID().toString();
            EventRequest request = new EventRequest(
                testEventId,
                runId,
                eventType,
                null,
                timestamp,
                "{\"type\": \"" + eventType.name() + "\"}"
            );

            when(eventLogRepository.existsByEventId(testEventId)).thenReturn(false);
            when(eventLogRepository.save(any(EventLog.class))).thenAnswer(invocation -> {
                EventLog event = invocation.getArgument(0);
                event.setId(UUID.randomUUID().toString());
                return event;
            });

            ResponseEntity<EventResponse> response = eventController.ingestEvent(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().eventType()).isEqualTo(eventType);
        }
    }
}

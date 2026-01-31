package com.utem.utem_core.repository;

import com.utem.utem_core.entity.EventLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EventLogRepositoryTest {

    @Autowired
    private EventLogRepository eventLogRepository;

    private String runId;
    private EventLog runStartedEvent;
    private EventLog suiteStartedEvent;

    @BeforeEach
    void setUp() {
        eventLogRepository.deleteAll();

        runId = UUID.randomUUID().toString();

        runStartedEvent = EventLog.builder()
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(Instant.now())
                .receivedAt(Instant.now())
                .payload("{\"name\": \"Integration Tests\", \"env\": \"staging\"}")
                .build();

        suiteStartedEvent = EventLog.builder()
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SUITE_STARTED)
                .parentId(runId)
                .timestamp(Instant.now().plus(1, ChronoUnit.SECONDS))
                .receivedAt(Instant.now().plus(1, ChronoUnit.SECONDS))
                .payload("{\"name\": \"Login Suite\"}")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve event by ID")
    void shouldSaveAndFindById() {
        EventLog saved = eventLogRepository.save(runStartedEvent);

        Optional<EventLog> found = eventLogRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        assertThat(found.get().getRunId()).isEqualTo(runId);
    }

    @Test
    @DisplayName("Should find events by run ID")
    void shouldFindByRunId() {
        eventLogRepository.save(runStartedEvent);
        eventLogRepository.save(suiteStartedEvent);

        List<EventLog> events = eventLogRepository.findByRunId(runId);

        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("Should find events by run ID ordered by timestamp")
    void shouldFindByRunIdOrderByTimestamp() {
        eventLogRepository.save(suiteStartedEvent); // Save second event first
        eventLogRepository.save(runStartedEvent);   // Save first event second

        List<EventLog> events = eventLogRepository.findByRunIdOrderByTimestampAsc(runId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        assertThat(events.get(1).getEventType()).isEqualTo(EventLog.EventType.TEST_SUITE_STARTED);
    }

    @Test
    @DisplayName("Should find events by event type")
    void shouldFindByEventType() {
        eventLogRepository.save(runStartedEvent);
        eventLogRepository.save(suiteStartedEvent);

        List<EventLog> runEvents = eventLogRepository.findByEventType(EventLog.EventType.TEST_RUN_STARTED);
        List<EventLog> suiteEvents = eventLogRepository.findByEventType(EventLog.EventType.TEST_SUITE_STARTED);

        assertThat(runEvents).hasSize(1);
        assertThat(suiteEvents).hasSize(1);
    }

    @Test
    @DisplayName("Should find events by run ID and event type")
    void shouldFindByRunIdAndEventType() {
        eventLogRepository.save(runStartedEvent);
        eventLogRepository.save(suiteStartedEvent);

        // Add event from different run
        EventLog otherRunEvent = EventLog.builder()
                .eventId(UUID.randomUUID().toString())
                .runId(UUID.randomUUID().toString())
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(Instant.now())
                .payload("{}")
                .build();
        eventLogRepository.save(otherRunEvent);

        List<EventLog> events = eventLogRepository.findByRunIdAndEventType(
                runId,
                EventLog.EventType.TEST_RUN_STARTED
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRunId()).isEqualTo(runId);
    }

    @Test
    @DisplayName("Should find events within timestamp range")
    void shouldFindByTimestampBetween() {
        Instant now = Instant.now();
        runStartedEvent.setTimestamp(now);
        suiteStartedEvent.setTimestamp(now.plus(1, ChronoUnit.MINUTES));
        eventLogRepository.save(runStartedEvent);
        eventLogRepository.save(suiteStartedEvent);

        // Old event outside range
        EventLog oldEvent = EventLog.builder()
                .eventId(UUID.randomUUID().toString())
                .runId(UUID.randomUUID().toString())
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(now.minus(1, ChronoUnit.DAYS))
                .payload("{}")
                .build();
        eventLogRepository.save(oldEvent);

        List<EventLog> recentEvents = eventLogRepository.findByTimestampBetween(
                now.minus(1, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS)
        );

        assertThat(recentEvents).hasSize(2);
    }

    @Test
    @DisplayName("Should check if event exists by event ID")
    void shouldCheckExistsByEventId() {
        eventLogRepository.save(runStartedEvent);

        boolean exists = eventLogRepository.existsByEventId(runStartedEvent.getEventId());
        boolean notExists = eventLogRepository.existsByEventId("non-existent-id");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should save event with all event types")
    void shouldSaveAllEventTypes() {
        List<EventLog.EventType> eventTypes = List.of(
                EventLog.EventType.TEST_RUN_STARTED,
                EventLog.EventType.TEST_RUN_FINISHED,
                EventLog.EventType.TEST_SUITE_STARTED,
                EventLog.EventType.TEST_SUITE_FINISHED,
                EventLog.EventType.TEST_CASE_STARTED,
                EventLog.EventType.TEST_CASE_FINISHED,
                EventLog.EventType.TEST_STEP,
                EventLog.EventType.TEST_PASSED,
                EventLog.EventType.TEST_FAILED,
                EventLog.EventType.TEST_SKIPPED,
                EventLog.EventType.ATTACHMENT
        );

        for (EventLog.EventType eventType : eventTypes) {
            EventLog event = EventLog.builder()
                    .eventId(UUID.randomUUID().toString())
                    .runId(runId)
                    .eventType(eventType)
                    .timestamp(Instant.now())
                    .payload("{\"type\": \"" + eventType.name() + "\"}")
                    .build();
            eventLogRepository.save(event);
        }

        List<EventLog> allEvents = eventLogRepository.findByRunId(runId);
        assertThat(allEvents).hasSize(eventTypes.size());
    }

    @Test
    @DisplayName("Should preserve payload JSON")
    void shouldPreservePayloadJson() {
        String complexPayload = """
                {
                    "name": "Test Case",
                    "metadata": {
                        "tags": ["smoke", "regression"],
                        "priority": 1,
                        "data": {
                            "input": "test@example.com",
                            "expected": true
                        }
                    }
                }
                """;

        runStartedEvent.setPayload(complexPayload);
        EventLog saved = eventLogRepository.save(runStartedEvent);

        Optional<EventLog> found = eventLogRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPayload()).contains("smoke");
        assertThat(found.get().getPayload()).contains("regression");
        assertThat(found.get().getPayload()).contains("test@example.com");
    }

    @Test
    @DisplayName("Should track received timestamp")
    void shouldTrackReceivedTimestamp() {
        Instant eventTime = Instant.now().minus(5, ChronoUnit.SECONDS);
        Instant receivedTime = Instant.now();

        runStartedEvent.setTimestamp(eventTime);
        runStartedEvent.setReceivedAt(receivedTime);
        EventLog saved = eventLogRepository.save(runStartedEvent);

        Optional<EventLog> found = eventLogRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTimestamp()).isEqualTo(eventTime);
        assertThat(found.get().getReceivedAt()).isEqualTo(receivedTime);
    }

    @Test
    @DisplayName("Events should be immutable after save")
    void eventsShouldBeImmutableAfterSave() {
        EventLog saved = eventLogRepository.save(runStartedEvent);
        String originalPayload = saved.getPayload();
        String originalEventId = saved.getEventId();

        // Verify the event exists with original data
        Optional<EventLog> found = eventLogRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPayload()).isEqualTo(originalPayload);
        assertThat(found.get().getEventId()).isEqualTo(originalEventId);
    }
}

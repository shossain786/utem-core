package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "event_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private String parentId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    private Instant receivedAt;

    public enum EventType {
        TEST_RUN_STARTED,
        TEST_RUN_FINISHED,
        TEST_SUITE_STARTED,
        TEST_SUITE_FINISHED,
        TEST_CASE_STARTED,
        TEST_CASE_FINISHED,
        TEST_STEP,
        TEST_PASSED,
        TEST_FAILED,
        TEST_SKIPPED,
        ATTACHMENT
    }
}

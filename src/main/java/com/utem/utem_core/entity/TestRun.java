package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "test_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    /** The reporter-generated UUID (runId) used to correlate in-memory maps after server restart. */
    @Column(unique = true)
    private String sourceRunId;

    @Column(nullable = false)
    private Instant startTime;

    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private Integer totalTests;

    private Integer passedTests;

    private Integer failedTests;

    private Integer skippedTests;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean archived = false;

    @Column(length = 100)
    private String label;

    public enum RunStatus {
        RUNNING,
        PASSED,
        FAILED,
        ABORTED
    }
}

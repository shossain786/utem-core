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

    public enum RunStatus {
        RUNNING,
        PASSED,
        FAILED,
        ABORTED
    }
}

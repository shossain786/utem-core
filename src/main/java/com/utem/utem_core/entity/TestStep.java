package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "test_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private TestNode testNode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    @Column(nullable = false)
    private Instant timestamp;

    private Long duration;

    private Integer stepOrder;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public enum StepStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        SKIPPED
    }
}

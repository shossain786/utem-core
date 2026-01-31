package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "test_node")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TestNode parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType nodeType;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    @Column(nullable = false)
    private Instant startTime;

    private Instant endTime;

    private Long duration;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private Boolean flaky;

    private Integer retryCount;

    public enum NodeType {
        SUITE,
        FEATURE,
        SCENARIO,
        STEP
    }

    public enum NodeStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        SKIPPED
    }
}

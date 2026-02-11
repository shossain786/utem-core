package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

import java.time.Instant;

/**
 * Lightweight test node summary for search results.
 */
public record TestNodeSummaryDTO(
        String id,
        String runId,
        String runName,
        TestNode.NodeType nodeType,
        String name,
        TestNode.NodeStatus status,
        Instant startTime,
        Long duration,
        Boolean flaky
) {
    public static TestNodeSummaryDTO from(TestNode node) {
        return new TestNodeSummaryDTO(
                node.getId(),
                node.getTestRun() != null ? node.getTestRun().getId() : null,
                node.getTestRun() != null ? node.getTestRun().getName() : null,
                node.getNodeType(),
                node.getName(),
                node.getStatus(),
                node.getStartTime(),
                node.getDuration(),
                node.getFlaky()
        );
    }
}

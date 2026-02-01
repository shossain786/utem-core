package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical tree node representation with children, steps, and statistics.
 */
public record HierarchyNodeDTO(
    String id,
    String parentId,
    TestNode.NodeType nodeType,
    String name,
    TestNode.NodeStatus status,
    Instant startTime,
    Instant endTime,
    Long duration,
    Boolean flaky,
    Integer retryCount,
    NodeStatistics statistics,
    List<HierarchyNodeDTO> children,
    List<TestStepDTO> steps,
    List<AttachmentSummaryDTO> attachments
) {
    public static HierarchyNodeDTO from(TestNode node) {
        return new HierarchyNodeDTO(
            node.getId(),
            node.getParent() != null ? node.getParent().getId() : null,
            node.getNodeType(),
            node.getName(),
            node.getStatus(),
            node.getStartTime(),
            node.getEndTime(),
            node.getDuration(),
            node.getFlaky(),
            node.getRetryCount(),
            null,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }

    public static HierarchyNodeDTO from(
            TestNode node,
            NodeStatistics statistics,
            List<HierarchyNodeDTO> children,
            List<TestStepDTO> steps,
            List<AttachmentSummaryDTO> attachments
    ) {
        return new HierarchyNodeDTO(
            node.getId(),
            node.getParent() != null ? node.getParent().getId() : null,
            node.getNodeType(),
            node.getName(),
            node.getStatus(),
            node.getStartTime(),
            node.getEndTime(),
            node.getDuration(),
            node.getFlaky(),
            node.getRetryCount(),
            statistics,
            children != null ? children : new ArrayList<>(),
            steps != null ? steps : new ArrayList<>(),
            attachments != null ? attachments : new ArrayList<>()
        );
    }
}

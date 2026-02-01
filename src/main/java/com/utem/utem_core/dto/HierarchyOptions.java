package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

import java.util.Set;

/**
 * Options for customizing hierarchy retrieval behavior.
 */
public record HierarchyOptions(
    int maxDepth,
    boolean includeSteps,
    boolean includeAttachments,
    boolean calculateStats,
    Set<TestNode.NodeType> nodeTypes,
    Set<TestNode.NodeStatus> statuses
) {
    /**
     * Default options: full tree with all details.
     */
    public static HierarchyOptions defaults() {
        return new HierarchyOptions(-1, true, true, true, null, null);
    }

    /**
     * Shallow options: immediate children only, no details.
     */
    public static HierarchyOptions shallow() {
        return new HierarchyOptions(1, false, false, false, null, null);
    }

    /**
     * Stats only: full tree structure with statistics but no steps/attachments.
     */
    public static HierarchyOptions statsOnly() {
        return new HierarchyOptions(-1, false, false, true, null, null);
    }

    /**
     * Check if depth limit has been reached.
     */
    public boolean isDepthExceeded(int currentDepth) {
        return maxDepth >= 0 && currentDepth >= maxDepth;
    }
}

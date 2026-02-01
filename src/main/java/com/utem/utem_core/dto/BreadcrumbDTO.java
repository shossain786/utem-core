package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

/**
 * Single breadcrumb entry for ancestor path navigation.
 */
public record BreadcrumbDTO(
    String id,
    String name,
    TestNode.NodeType nodeType,
    TestNode.NodeStatus status
) {
    public static BreadcrumbDTO from(TestNode node) {
        return new BreadcrumbDTO(
            node.getId(),
            node.getName(),
            node.getNodeType(),
            node.getStatus()
        );
    }
}

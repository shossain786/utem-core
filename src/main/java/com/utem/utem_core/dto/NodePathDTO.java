package com.utem.utem_core.dto;

import java.util.List;

/**
 * Node with its ancestor path (breadcrumb trail from root to node).
 */
public record NodePathDTO(
    String nodeId,
    List<BreadcrumbDTO> path
) {
    public static NodePathDTO of(String nodeId, List<BreadcrumbDTO> path) {
        return new NodePathDTO(nodeId, path);
    }
}

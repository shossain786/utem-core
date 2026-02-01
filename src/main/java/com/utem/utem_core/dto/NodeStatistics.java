package com.utem.utem_core.dto;

/**
 * Statistics aggregation for test nodes within a hierarchy.
 */
public record NodeStatistics(
    int totalNodes,
    int passedNodes,
    int failedNodes,
    int skippedNodes,
    int runningNodes,
    int pendingNodes,
    long totalDuration
) {
    public static NodeStatistics empty() {
        return new NodeStatistics(0, 0, 0, 0, 0, 0, 0L);
    }

    public static NodeStatistics single(String status, Long duration) {
        long dur = duration != null ? duration : 0L;
        return switch (status) {
            case "PASSED" -> new NodeStatistics(1, 1, 0, 0, 0, 0, dur);
            case "FAILED" -> new NodeStatistics(1, 0, 1, 0, 0, 0, dur);
            case "SKIPPED" -> new NodeStatistics(1, 0, 0, 1, 0, 0, dur);
            case "RUNNING" -> new NodeStatistics(1, 0, 0, 0, 1, 0, dur);
            case "PENDING" -> new NodeStatistics(1, 0, 0, 0, 0, 1, dur);
            default -> new NodeStatistics(1, 0, 0, 0, 0, 0, dur);
        };
    }

    public NodeStatistics merge(NodeStatistics other) {
        return new NodeStatistics(
            this.totalNodes + other.totalNodes,
            this.passedNodes + other.passedNodes,
            this.failedNodes + other.failedNodes,
            this.skippedNodes + other.skippedNodes,
            this.runningNodes + other.runningNodes,
            this.pendingNodes + other.pendingNodes,
            this.totalDuration + other.totalDuration
        );
    }
}

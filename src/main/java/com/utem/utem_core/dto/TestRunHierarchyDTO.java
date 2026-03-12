package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestRun;

import java.time.Instant;
import java.util.List;

/**
 * Complete test run hierarchy with root nodes and aggregated statistics.
 */
public record TestRunHierarchyDTO(
    String runId,
    String name,
    String label,
    TestRun.RunStatus status,
    Instant startTime,
    Instant endTime,
    NodeStatistics statistics,
    List<HierarchyNodeDTO> rootNodes
) {
    public static TestRunHierarchyDTO from(
            TestRun run,
            List<HierarchyNodeDTO> rootNodes,
            NodeStatistics statistics
    ) {
        return new TestRunHierarchyDTO(
            run.getId(),
            run.getName(),
            run.getLabel(),
            run.getStatus(),
            run.getStartTime(),
            run.getEndTime(),
            statistics,
            rootNodes
        );
    }
}

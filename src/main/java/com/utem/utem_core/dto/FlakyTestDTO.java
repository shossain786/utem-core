package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

/**
 * Flakiness information for a specific test across runs.
 */
public record FlakyTestDTO(
        String testName,
        TestNode.NodeType nodeType,
        double flakinessRate,
        int totalRuns,
        int passCount,
        int failCount,
        int skipCount,
        boolean frameworkMarked,
        Integer lastRetryCount,
        TestNode.NodeStatus lastStatus,
        String lastRunId
) {}

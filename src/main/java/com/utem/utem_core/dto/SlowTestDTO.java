package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

public record SlowTestDTO(
        String testName,
        TestNode.NodeType nodeType,
        long avgDurationMs,
        long maxDurationMs,
        long minDurationMs,
        int runCount,
        String slowestRunId    // run ID where maxDuration was observed
) {}

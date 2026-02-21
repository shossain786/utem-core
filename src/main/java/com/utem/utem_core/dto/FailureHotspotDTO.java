package com.utem.utem_core.dto;

import com.utem.utem_core.entity.TestNode;

public record FailureHotspotDTO(
        String testName,
        TestNode.NodeType nodeType,
        int failCount,
        int totalRuns,
        double failRate,           // percentage 0–100
        String lastRunId,
        String lastErrorMessage    // nullable — from latest failed step
) {}

package com.utem.utem_core.dto;

public record DurationStatsDTO(
        long avgMs,
        long maxMs,
        long minMs,
        long totalMs,
        int count
) {}

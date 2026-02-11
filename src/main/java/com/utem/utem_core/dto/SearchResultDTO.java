package com.utem.utem_core.dto;

import java.util.List;

/**
 * Unified search result wrapper for global search across all entity types.
 */
public record SearchResultDTO(
        String query,
        List<TestRunSummaryDTO> runs,
        List<TestNodeSummaryDTO> nodes,
        List<TestStepSummaryDTO> steps,
        List<AttachmentSummaryDTO> attachments,
        int totalResults
) {}

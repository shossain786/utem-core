package com.utem.utem_core.dto;

/**
 * Rule-based failure diagnosis for a single failed test step.
 *
 * @param category   Short label, e.g. "Null Reference", "Timeout", "Assertion Failure"
 * @param explanation Plain-English description of what went wrong
 * @param suggestion  Recommended next step to investigate or fix the failure
 * @param confidence  How confident the rule engine is: HIGH, MEDIUM, or LOW
 */
public record DiagnosisDTO(
        String category,
        String explanation,
        String suggestion,
        String confidence
) {}

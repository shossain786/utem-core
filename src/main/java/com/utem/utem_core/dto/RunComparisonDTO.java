package com.utem.utem_core.dto;

import java.util.List;

/**
 * Side-by-side comparison of two test runs, including node-level diff.
 */
public record RunComparisonDTO(
        TestRunSummaryDTO baseRun,
        TestRunSummaryDTO compareRun,
        int totalTestsDiff,
        int passedTestsDiff,
        int failedTestsDiff,
        int skippedTestsDiff,
        Double passRateDiff,
        Long durationDiff,
        List<NodeDiffEntry> nodeDiffs
) {

    public enum DiffType {
        /** Test was passing in base but failing in compare. */
        REGRESSION,
        /** Test was failing in base but passing in compare. */
        FIX,
        /** Test exists in compare but not in base. */
        NEW,
        /** Test exists in base but not in compare. */
        REMOVED,
        /** Test result is the same in both runs. */
        UNCHANGED
    }

    public record NodeDiffEntry(
            String name,
            String nodeType,
            DiffType diffType,
            String baseStatus,
            String compareStatus,
            Long baseDuration,
            Long compareDuration
    ) {}

    /** Factory used by tests and backward-compatible callers (no node diffs). */
    public static RunComparisonDTO from(TestRunSummaryDTO base, TestRunSummaryDTO compare) {
        return from(base, compare, List.of());
    }

    /** Full factory including node-level diffs, called by RunHistoryService. */
    public static RunComparisonDTO from(TestRunSummaryDTO base, TestRunSummaryDTO compare,
                                        List<NodeDiffEntry> nodeDiffs) {
        int totalDiff = safeIntDiff(compare.totalTests(), base.totalTests());
        int passedDiff = safeIntDiff(compare.passedTests(), base.passedTests());
        int failedDiff = safeIntDiff(compare.failedTests(), base.failedTests());
        int skippedDiff = safeIntDiff(compare.skippedTests(), base.skippedTests());

        Double passRateDiff = null;
        if (compare.passRate() != null && base.passRate() != null) {
            passRateDiff = compare.passRate() - base.passRate();
        }

        Long durationDiff = null;
        if (compare.duration() != null && base.duration() != null) {
            durationDiff = compare.duration() - base.duration();
        }

        return new RunComparisonDTO(
                base, compare,
                totalDiff, passedDiff, failedDiff, skippedDiff,
                passRateDiff, durationDiff,
                nodeDiffs
        );
    }

    private static int safeIntDiff(Integer a, Integer b) {
        return (a != null ? a : 0) - (b != null ? b : 0);
    }
}

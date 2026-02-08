package com.utem.utem_core.dto;

/**
 * Side-by-side comparison of two test runs.
 */
public record RunComparisonDTO(
        TestRunSummaryDTO baseRun,
        TestRunSummaryDTO compareRun,
        int totalTestsDiff,
        int passedTestsDiff,
        int failedTestsDiff,
        int skippedTestsDiff,
        Double passRateDiff,
        Long durationDiff
) {
    public static RunComparisonDTO from(TestRunSummaryDTO base, TestRunSummaryDTO compare) {
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
                passRateDiff, durationDiff
        );
    }

    private static int safeIntDiff(Integer a, Integer b) {
        return (a != null ? a : 0) - (b != null ? b : 0);
    }
}

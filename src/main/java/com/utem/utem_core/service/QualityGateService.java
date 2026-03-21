package com.utem.utem_core.service;

import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.dto.QualityGateResultDTO;
import com.utem.utem_core.dto.RunComparisonDTO;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QualityGateService {

    private final TestRunRepository testRunRepository;
    private final FlakinessDetectionService flakinessDetectionService;
    private final RunHistoryService runHistoryService;

    @Value("${utem.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Evaluate quality gates for a finished run.
     *
     * @param runId              The run to evaluate
     * @param maxFailRate        Max allowed fail rate (0-100). Use 100 to disable.
     * @param maxFlakinessScore  Max allowed avg flakiness score (0-100). Use 100 to disable.
     * @param maxNewFailures     Max allowed regressions vs baseline. Use -1 to disable.
     * @param baselineRunId      Baseline run for regression comparison (required for new-failures gate).
     */
    @Transactional(readOnly = true)
    public QualityGateResultDTO evaluate(String runId, double maxFailRate, double maxFlakinessScore,
                                          int maxNewFailures, String baselineRunId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));

        if (run.getStatus() == TestRun.RunStatus.RUNNING) {
            throw new IllegalStateException("Run " + runId + " is still in progress — wait for it to finish before evaluating quality gates");
        }

        List<QualityGateResultDTO.GateViolation> violations = new ArrayList<>();

        int total = run.getTotalTests() != null ? run.getTotalTests() : 0;
        int failed = run.getFailedTests() != null ? run.getFailedTests() : 0;

        // ── Fail rate gate ───────────────────────────────────────────────────
        double failRate = total > 0 ? (failed * 100.0) / total : 0.0;
        if (maxFailRate < 100.0 && failRate > maxFailRate) {
            violations.add(new QualityGateResultDTO.GateViolation(
                    "FAIL_RATE",
                    String.format("Fail rate %.1f%% exceeds maximum allowed %.1f%%", failRate, maxFailRate),
                    failRate,
                    maxFailRate
            ));
        }

        // ── Flakiness gate ───────────────────────────────────────────────────
        List<FlakyTestDTO> flakyTests = flakinessDetectionService.getFlakyTestsForRun(runId);
        double avgFlakiness = flakyTests.isEmpty() ? 0.0
                : flakyTests.stream().mapToDouble(FlakyTestDTO::flakinessRate).average().orElse(0.0);

        if (maxFlakinessScore < 100.0 && avgFlakiness > maxFlakinessScore) {
            violations.add(new QualityGateResultDTO.GateViolation(
                    "FLAKINESS",
                    String.format("Average flakiness score %.1f exceeds maximum allowed %.1f", avgFlakiness, maxFlakinessScore),
                    avgFlakiness,
                    maxFlakinessScore
            ));
        }

        // ── New failures gate ────────────────────────────────────────────────
        int newFailures = 0;
        if (maxNewFailures >= 0 && baselineRunId != null && !baselineRunId.isBlank()) {
            RunComparisonDTO comparison = runHistoryService.compareRuns(baselineRunId, runId);
            newFailures = (int) comparison.nodeDiffs().stream()
                    .filter(d -> d.diffType() == RunComparisonDTO.DiffType.REGRESSION)
                    .count();

            if (newFailures > maxNewFailures) {
                violations.add(new QualityGateResultDTO.GateViolation(
                        "NEW_FAILURES",
                        String.format("%d new failure(s) vs baseline (max allowed: %d)", newFailures, maxNewFailures),
                        newFailures,
                        maxNewFailures
                ));
            }
        }

        boolean passed = violations.isEmpty();
        QualityGateResultDTO.GateMetrics metrics = new QualityGateResultDTO.GateMetrics(
                failRate, avgFlakiness, newFailures, total, failed, flakyTests.size()
        );

        log.info("Quality gate for run {}: {} — {} violation(s)", runId, passed ? "PASSED" : "FAILED", violations.size());
        return new QualityGateResultDTO(runId, passed, passed ? "PASSED" : "FAILED", violations, metrics);
    }

    /**
     * Generate a markdown CI summary for posting in PR comments.
     */
    @Transactional(readOnly = true)
    public String generateCiSummary(String runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));

        int total   = run.getTotalTests()  != null ? run.getTotalTests()  : 0;
        int passed  = run.getPassedTests() != null ? run.getPassedTests() : 0;
        int failed  = run.getFailedTests() != null ? run.getFailedTests() : 0;
        int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;
        double passRate = total > 0 ? (passed * 100.0) / total : 0.0;

        String duration = formatDuration(run);
        String statusEmoji = switch (run.getStatus()) {
            case PASSED  -> "✅";
            case FAILED  -> "❌";
            case ABORTED -> "⚠️";
            default      -> "🔄";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(statusEmoji).append(" UTEM Test Run Summary\n\n");
        sb.append("**Run:** ").append(run.getName()).append("  \n");
        sb.append("**Status:** ").append(run.getStatus()).append("  \n");
        if (run.getLabel() != null) {
            sb.append("**Label:** ").append(run.getLabel()).append("  \n");
        }
        sb.append("**Duration:** ").append(duration).append("  \n\n");

        sb.append("| Total | Passed | Failed | Skipped | Pass Rate |\n");
        sb.append("|-------|--------|--------|---------|----------|\n");
        sb.append(String.format("| %d | %d | %d | %d | %.1f%% |\n\n",
                total, passed, failed, skipped, passRate));

        sb.append("_[Full report](").append(baseUrl).append("/runs/").append(runId).append(")_\n");

        return sb.toString();
    }

    private String formatDuration(TestRun run) {
        if (run.getStartTime() == null || run.getEndTime() == null) return "—";
        Duration d = Duration.between(run.getStartTime(), run.getEndTime());
        long mins = d.toMinutes();
        long secs = d.toSecondsPart();
        if (mins > 0) return mins + "m " + secs + "s";
        return secs + "s";
    }
}

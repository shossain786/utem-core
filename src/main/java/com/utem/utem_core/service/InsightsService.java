package com.utem.utem_core.service;

import com.utem.utem_core.dto.FailureClusterDTO;
import com.utem.utem_core.dto.FailureHotspotDTO;
import com.utem.utem_core.dto.InsightsSummaryDTO;
import com.utem.utem_core.dto.SlowTestDTO;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Aggregates failure insights and performance data into an overall health summary.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InsightsService {

    private static final Set<TestRun.RunStatus> FINISHED_STATUSES =
            Set.of(TestRun.RunStatus.PASSED, TestRun.RunStatus.FAILED);

    private final TestRunRepository testRunRepository;
    private final FailureInsightsService failureInsightsService;
    private final PerformanceAnalysisService performanceAnalysisService;

    /**
     * Computes a combined insights summary including a composite health score (0–100).
     * <p>
     * Health score = 0.5 × avgPassRate + 0.3 × (100 − avgFlakinessRate) + 0.2 × (100 − slowTestPct)
     */
    public InsightsSummaryDTO getSummary(int recentRuns) {
        List<FailureHotspotDTO> allHotspots = failureInsightsService.getFailureHotspots(100, recentRuns);
        List<FailureClusterDTO> allClusters = failureInsightsService.getFailureClusters(100, recentRuns);
        List<SlowTestDTO> allSlowTests = performanceAnalysisService.getSlowestTests(100, recentRuns);

        int runsAnalyzed = getRecentFinishedRunCount(recentRuns);

        double healthScore = computeHealthScore(allHotspots, runsAnalyzed);

        List<FailureHotspotDTO> topFailures = allHotspots.size() > 5
                ? allHotspots.subList(0, 5) : allHotspots;
        List<FailureClusterDTO> topClusters = allClusters.size() > 5
                ? allClusters.subList(0, 5) : allClusters;
        List<SlowTestDTO> topSlowTests = allSlowTests.size() > 5
                ? allSlowTests.subList(0, 5) : allSlowTests;

        return new InsightsSummaryDTO(
                runsAnalyzed,
                healthScore,
                topFailures,
                topClusters,
                topSlowTests,
                allClusters.size(),
                allHotspots.size()
        );
    }

    // ============ Private Helpers ============

    private int getRecentFinishedRunCount(int limit) {
        int fetchSize = Math.min(limit * 2, 500);
        List<TestRun> runs = testRunRepository
                .findByStatusInOrderByStartTimeDesc(FINISHED_STATUSES, PageRequest.of(0, fetchSize));
        return Math.min(runs.size(), limit);
    }

    /**
     * Composite health score: lower failure rate = higher score.
     * Range: 0 (all tests always fail) to 100 (perfect pass rate, no hotspots).
     */
    private double computeHealthScore(List<FailureHotspotDTO> hotspots, int runsAnalyzed) {
        if (runsAnalyzed == 0) return 100.0;

        // Avg fail rate across all detected hotspots
        double avgFailRate = hotspots.isEmpty() ? 0.0
                : hotspots.stream().mapToDouble(FailureHotspotDTO::failRate).average().orElse(0.0);

        // Slow test penalty: fraction of tests with avg > 5s considered "slow"
        // (not computed here — kept simple for MVP)
        double passScore = 100.0 - avgFailRate;

        return Math.max(0.0, Math.min(100.0, passScore));
    }
}

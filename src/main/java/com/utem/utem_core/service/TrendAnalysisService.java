package com.utem.utem_core.service;

import com.utem.utem_core.dto.TrendDataDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrendAnalysisService {

    private static final Set<TestRun.RunStatus> FINISHED_STATUSES =
            Set.of(TestRun.RunStatus.PASSED, TestRun.RunStatus.FAILED);

    private static final Set<TestNode.NodeType> TEST_CASE_TYPES =
            Set.of(TestNode.NodeType.SCENARIO, TestNode.NodeType.STEP);

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;

    public TrendDataDTO getPassRateTrend(int limit) {
        List<TestRun> runs = getRecentFinishedRuns(limit);
        List<TrendDataDTO.TrendPoint> points = runs.stream()
                .map(r -> {
                    Double value = (r.getPassedTests() != null && r.getTotalTests() != null && r.getTotalTests() > 0)
                            ? (r.getPassedTests() * 100.0) / r.getTotalTests()
                            : null;
                    return new TrendDataDTO.TrendPoint(r.getId(), r.getName(), r.getStartTime(), value);
                })
                .toList();
        return new TrendDataDTO("PASS_RATE", limit, points);
    }

    public TrendDataDTO getDurationTrend(int limit) {
        List<TestRun> runs = getRecentFinishedRuns(limit);
        List<TrendDataDTO.TrendPoint> points = runs.stream()
                .map(r -> {
                    Double value = (r.getEndTime() != null)
                            ? (double) (r.getEndTime().toEpochMilli() - r.getStartTime().toEpochMilli())
                            : null;
                    return new TrendDataDTO.TrendPoint(r.getId(), r.getName(), r.getStartTime(), value);
                })
                .toList();
        return new TrendDataDTO("DURATION", limit, points);
    }

    public TrendDataDTO getTestCountTrend(int limit) {
        List<TestRun> runs = getRecentFinishedRuns(limit);
        List<TrendDataDTO.TrendPoint> points = runs.stream()
                .map(r -> new TrendDataDTO.TrendPoint(
                        r.getId(), r.getName(), r.getStartTime(),
                        r.getTotalTests() != null ? r.getTotalTests().doubleValue() : null))
                .toList();
        return new TrendDataDTO("TEST_COUNT", limit, points);
    }

    public TrendDataDTO getFlakinessTrend(int limit) {
        List<TestRun> runs = getRecentFinishedRuns(limit);
        List<TrendDataDTO.TrendPoint> points = runs.stream()
                .map(r -> {
                    List<TestNode> testCases = testNodeRepository.findByTestRunIdAndNodeTypeIn(r.getId(), TEST_CASE_TYPES);
                    int total = testCases.size();
                    long flaky = testCases.stream().filter(n -> Boolean.TRUE.equals(n.getFlaky())).count();
                    Double value = total > 0 ? (flaky * 100.0) / total : 0.0;
                    return new TrendDataDTO.TrendPoint(r.getId(), r.getName(), r.getStartTime(), value);
                })
                .toList();
        return new TrendDataDTO("FLAKINESS", limit, points);
    }

    /** Returns up to {@code limit} finished runs ordered oldest-first (for left-to-right charting). */
    private List<TestRun> getRecentFinishedRuns(int limit) {
        // Fetch more than limit to account for RUNNING/ABORTED runs interleaved
        int fetchSize = Math.min(limit * 2, 500);
        List<TestRun> recent = testRunRepository
                .findByStatusInOrderByStartTimeDesc(FINISHED_STATUSES, PageRequest.of(0, fetchSize));

        List<TestRun> trimmed = recent.size() > limit ? recent.subList(0, limit) : recent;
        List<TestRun> result = new ArrayList<>(trimmed);
        Collections.reverse(result); // oldest first → left on chart
        return result;
    }
}

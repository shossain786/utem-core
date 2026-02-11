package com.utem.utem_core.service;

import com.utem.utem_core.dto.FlakinessReportDTO;
import com.utem.utem_core.dto.FlakyTestDTO;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class FlakinessDetectionServiceTest {

    @Mock
    private TestNodeRepository testNodeRepository;

    @Mock
    private TestRunRepository testRunRepository;

    private FlakinessDetectionService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        service = new FlakinessDetectionService(testNodeRepository, testRunRepository);
    }

    // ============ Helpers ============

    private TestRun createRun(String id, String name, int daysAgo) {
        return TestRun.builder()
                .id(id).name(name)
                .status(TestRun.RunStatus.PASSED)
                .startTime(now.minus(daysAgo, ChronoUnit.DAYS))
                .build();
    }

    private TestNode createNode(String id, String name, TestNode.NodeStatus status,
                                 TestRun run, Boolean flaky, Integer retryCount) {
        return TestNode.builder()
                .id(id).name(name)
                .nodeType(TestNode.NodeType.SCENARIO)
                .status(status)
                .testRun(run)
                .startTime(run.getStartTime())
                .flaky(flaky)
                .retryCount(retryCount)
                .build();
    }

    // ============ Tests ============

    @Nested
    @DisplayName("getFlakyTestsForRun tests")
    class GetFlakyTestsForRunTests {

        @Test
        @DisplayName("Should return framework-marked flaky tests in a run")
        void shouldReturnFrameworkMarkedFlakyTests() {
            String runId = "run-1";
            TestRun run = createRun(runId, "Run 1", 0);

            TestNode flaky1 = createNode("n1", "Login Test", TestNode.NodeStatus.PASSED, run, true, 2);
            TestNode normal = createNode("n2", "Logout Test", TestNode.NodeStatus.PASSED, run, false, null);
            TestNode flaky2 = createNode("n3", "Cart Test", TestNode.NodeStatus.FAILED, run, true, 1);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq(runId), anyCollection()))
                    .thenReturn(List.of(flaky1, normal, flaky2));

            List<FlakyTestDTO> result = service.getFlakyTestsForRun(runId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(FlakyTestDTO::testName)
                    .containsExactlyInAnyOrder("Login Test", "Cart Test");
        }

        @Test
        @DisplayName("Should include tests with retryCount > 0")
        void shouldIncludeRetriedTests() {
            String runId = "run-1";
            TestRun run = createRun(runId, "Run 1", 0);

            TestNode retried = createNode("n1", "Retry Test", TestNode.NodeStatus.PASSED, run, false, 3);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq(runId), anyCollection()))
                    .thenReturn(List.of(retried));

            List<FlakyTestDTO> result = service.getFlakyTestsForRun(runId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).lastRetryCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run not found")
        void shouldThrowWhenRunNotFound() {
            when(testRunRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> service.getFlakyTestsForRun("missing"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should return empty list when no flaky tests")
        void shouldReturnEmptyWhenNoFlakyTests() {
            String runId = "run-1";
            TestRun run = createRun(runId, "Run 1", 0);

            TestNode stable = createNode("n1", "Stable Test", TestNode.NodeStatus.PASSED, run, false, null);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq(runId), anyCollection()))
                    .thenReturn(List.of(stable));

            List<FlakyTestDTO> result = service.getFlakyTestsForRun(runId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("detectFlakyTests tests")
    class DetectFlakyTestsTests {

        @Test
        @DisplayName("Should detect test with inconsistent status across runs")
        void shouldDetectInconsistentStatus() {
            TestRun run1 = createRun("run-1", "Run 1", 3);
            TestRun run2 = createRun("run-2", "Run 2", 2);
            TestRun run3 = createRun("run-3", "Run 3", 1);

            // Login Test: PASSED -> FAILED -> PASSED (flaky)
            TestNode n1 = createNode("n1", "Login Test", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n2 = createNode("n2", "Login Test", TestNode.NodeStatus.FAILED, run2, false, null);
            TestNode n3 = createNode("n3", "Login Test", TestNode.NodeStatus.PASSED, run3, false, null);

            // Stable Test: PASSED -> PASSED -> PASSED (not flaky)
            TestNode s1 = createNode("s1", "Stable Test", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode s2 = createNode("s2", "Stable Test", TestNode.NodeStatus.PASSED, run2, false, null);
            TestNode s3 = createNode("s3", "Stable Test", TestNode.NodeStatus.PASSED, run3, false, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(n1, n2, n3, s1, s2, s3));

            List<FlakyTestDTO> result = service.detectFlakyTests(2, 0);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).testName()).isEqualTo("Login Test");
            assertThat(result.get(0).passCount()).isEqualTo(2);
            assertThat(result.get(0).failCount()).isEqualTo(1);
            assertThat(result.get(0).flakinessRate()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should respect minRuns parameter")
        void shouldRespectMinRuns() {
            TestRun run1 = createRun("run-1", "Run 1", 1);

            // Only one run - below minRuns=2
            TestNode n1 = createNode("n1", "One Run Test", TestNode.NodeStatus.PASSED, run1, false, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(n1));

            List<FlakyTestDTO> result = service.detectFlakyTests(2, 0);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should include framework-marked flaky tests even without status change")
        void shouldIncludeFrameworkMarkedFlaky() {
            TestRun run1 = createRun("run-1", "Run 1", 2);
            TestRun run2 = createRun("run-2", "Run 2", 1);

            // Framework marked flaky but always passes
            TestNode n1 = createNode("n1", "Flaky Test", TestNode.NodeStatus.PASSED, run1, true, null);
            TestNode n2 = createNode("n2", "Flaky Test", TestNode.NodeStatus.PASSED, run2, true, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(n1, n2));

            List<FlakyTestDTO> result = service.detectFlakyTests(2, 0);

            // Framework marked = 100% flakiness rate
            assertThat(result).hasSize(1);
            assertThat(result.get(0).frameworkMarked()).isTrue();
            assertThat(result.get(0).flakinessRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should sort by flakiness rate descending")
        void shouldSortByFlakinessRate() {
            TestRun run1 = createRun("run-1", "Run 1", 3);
            TestRun run2 = createRun("run-2", "Run 2", 2);
            TestRun run3 = createRun("run-3", "Run 3", 1);

            // Test A: always flips (high flakiness)
            TestNode a1 = createNode("a1", "Test A", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode a2 = createNode("a2", "Test A", TestNode.NodeStatus.FAILED, run2, false, null);
            TestNode a3 = createNode("a3", "Test A", TestNode.NodeStatus.PASSED, run3, false, null);

            // Test B: framework marked but stable
            TestNode b1 = createNode("b1", "Test B", TestNode.NodeStatus.PASSED, run1, true, null);
            TestNode b2 = createNode("b2", "Test B", TestNode.NodeStatus.PASSED, run2, false, null);
            TestNode b3 = createNode("b3", "Test B", TestNode.NodeStatus.FAILED, run3, false, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(a1, a2, a3, b1, b2, b3));

            List<FlakyTestDTO> result = service.detectFlakyTests(2, 0);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).flakinessRate()).isGreaterThanOrEqualTo(result.get(1).flakinessRate());
        }
    }

    @Nested
    @DisplayName("getTestFlakinessHistory tests")
    class GetTestFlakinessHistoryTests {

        @Test
        @DisplayName("Should return history for a specific test")
        void shouldReturnHistory() {
            TestRun run1 = createRun("run-1", "Run 1", 2);
            TestRun run2 = createRun("run-2", "Run 2", 1);

            TestNode n1 = createNode("n1", "Login Test", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n2 = createNode("n2", "Login Test", TestNode.NodeStatus.FAILED, run2, false, null);

            when(testNodeRepository.findByNameAndNodeTypeOrderByTestRunStartTimeDesc(
                    "Login Test", TestNode.NodeType.SCENARIO))
                    .thenReturn(List.of(n2, n1)); // Ordered by startTime desc

            FlakyTestDTO result = service.getTestFlakinessHistory("Login Test", TestNode.NodeType.SCENARIO);

            assertThat(result.testName()).isEqualTo("Login Test");
            assertThat(result.totalRuns()).isEqualTo(2);
            assertThat(result.passCount()).isEqualTo(1);
            assertThat(result.failCount()).isEqualTo(1);
            assertThat(result.flakinessRate()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should return empty result when test not found")
        void shouldReturnEmptyWhenNotFound() {
            when(testNodeRepository.findByNameAndNodeTypeOrderByTestRunStartTimeDesc(
                    "Unknown", TestNode.NodeType.SCENARIO))
                    .thenReturn(Collections.emptyList());

            FlakyTestDTO result = service.getTestFlakinessHistory("Unknown", TestNode.NodeType.SCENARIO);

            assertThat(result.totalRuns()).isEqualTo(0);
            assertThat(result.flakinessRate()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getFlakinessReport tests")
    class GetFlakinessReportTests {

        @Test
        @DisplayName("Should return report with flaky tests for a run")
        void shouldReturnReport() {
            String runId = "run-1";
            TestRun run = createRun(runId, "Run 1", 0);

            TestNode flaky = createNode("n1", "Flaky Test", TestNode.NodeStatus.PASSED, run, true, 1);
            TestNode stable = createNode("n2", "Stable Test", TestNode.NodeStatus.PASSED, run, false, null);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq(runId), anyCollection()))
                    .thenReturn(List.of(flaky, stable));
            when(testNodeRepository.findByNameAndNodeTypeOrderByTestRunStartTimeDesc(
                    "Stable Test", TestNode.NodeType.SCENARIO))
                    .thenReturn(List.of(stable));

            FlakinessReportDTO report = service.getFlakinessReport(runId);

            assertThat(report.totalTests()).isEqualTo(2);
            assertThat(report.flakyTests()).isEqualTo(1);
            assertThat(report.flakinessPercentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should throw when run not found")
        void shouldThrowWhenRunNotFound() {
            when(testRunRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> service.getFlakinessReport("missing"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getOverallFlakinessReport tests")
    class GetOverallFlakinessReportTests {

        @Test
        @DisplayName("Should return overall report for recent runs")
        void shouldReturnOverallReport() {
            TestRun run1 = createRun("run-1", "Run 1", 2);
            TestRun run2 = createRun("run-2", "Run 2", 1);

            TestNode n1 = createNode("n1", "Login Test", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n2 = createNode("n2", "Login Test", TestNode.NodeStatus.FAILED, run2, false, null);
            TestNode n3 = createNode("n3", "Stable Test", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n4 = createNode("n4", "Stable Test", TestNode.NodeStatus.PASSED, run2, false, null);

            when(testRunRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(run2, run1));
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq("run-1"), anyCollection()))
                    .thenReturn(List.of(n1, n3));
            when(testNodeRepository.findByTestRunIdAndNodeTypeIn(eq("run-2"), anyCollection()))
                    .thenReturn(List.of(n2, n4));

            FlakinessReportDTO report = service.getOverallFlakinessReport(10);

            assertThat(report.totalTests()).isEqualTo(2); // 2 distinct tests
            assertThat(report.flakyTests()).isEqualTo(1); // Login Test is flaky
            assertThat(report.flakinessPercentage()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("getMostFlakyTests tests")
    class GetMostFlakyTestsTests {

        @Test
        @DisplayName("Should return top N flaky tests")
        void shouldReturnTopN() {
            TestRun run1 = createRun("run-1", "Run 1", 2);
            TestRun run2 = createRun("run-2", "Run 2", 1);

            TestNode n1 = createNode("n1", "Flaky A", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n2 = createNode("n2", "Flaky A", TestNode.NodeStatus.FAILED, run2, false, null);
            TestNode n3 = createNode("n3", "Flaky B", TestNode.NodeStatus.FAILED, run1, false, null);
            TestNode n4 = createNode("n4", "Flaky B", TestNode.NodeStatus.PASSED, run2, false, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(n1, n2, n3, n4));

            List<FlakyTestDTO> result = service.getMostFlakyTests(1);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no flaky tests")
        void shouldReturnEmptyWhenNoFlaky() {
            TestRun run1 = createRun("run-1", "Run 1", 2);
            TestRun run2 = createRun("run-2", "Run 2", 1);

            TestNode n1 = createNode("n1", "Stable", TestNode.NodeStatus.PASSED, run1, false, null);
            TestNode n2 = createNode("n2", "Stable", TestNode.NodeStatus.PASSED, run2, false, null);

            when(testNodeRepository.findAll()).thenReturn(List.of(n1, n2));

            List<FlakyTestDTO> result = service.getMostFlakyTests(5);

            assertThat(result).isEmpty();
        }
    }
}

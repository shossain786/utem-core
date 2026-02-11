package com.utem.utem_core.service;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.repository.AttachmentRepository;
import com.utem.utem_core.repository.TestNodeRepository;
import com.utem.utem_core.repository.TestRunRepository;
import com.utem.utem_core.repository.TestStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class RunHistoryServiceTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestNodeRepository testNodeRepository;

    @Mock
    private TestStepRepository testStepRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AttachmentStorageService attachmentStorageService;

    @Mock
    private HierarchyReconstructionService hierarchyReconstructionService;

    private RunHistoryService service;

    private Instant timestamp;

    @BeforeEach
    void setUp() {
        timestamp = Instant.now();
        service = new RunHistoryService(
                testRunRepository,
                testNodeRepository,
                testStepRepository,
                attachmentRepository,
                attachmentStorageService,
                hierarchyReconstructionService
        );
    }

    // ============ Helper Methods ============

    private TestRun createTestRun(String id, String name, TestRun.RunStatus status,
                                   int total, int passed, int failed, int skipped) {
        return TestRun.builder()
                .id(id)
                .name(name)
                .status(status)
                .startTime(timestamp.minus(1, ChronoUnit.HOURS))
                .endTime(timestamp)
                .totalTests(total)
                .passedTests(passed)
                .failedTests(failed)
                .skippedTests(skipped)
                .build();
    }

    private TestRun createRunningTestRun(String id, String name) {
        return TestRun.builder()
                .id(id)
                .name(name)
                .status(TestRun.RunStatus.RUNNING)
                .startTime(timestamp)
                .totalTests(10)
                .passedTests(0)
                .failedTests(0)
                .skippedTests(0)
                .build();
    }

    // ============ Test Classes ============

    @Nested
    @DisplayName("getAllRuns tests")
    class GetAllRunsTests {

        @Test
        @DisplayName("Should return paginated runs newest first")
        void shouldReturnPaginatedRuns() {
            TestRun run1 = createTestRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 10, 8, 1, 1);
            TestRun run2 = createTestRun("run-2", "Run 2", TestRun.RunStatus.FAILED, 10, 5, 4, 1);
            Page<TestRun> page = new PageImpl<>(List.of(run1, run2), PageRequest.of(0, 10), 2);

            when(testRunRepository.findAllByOrderByStartTimeDesc(any(PageRequest.class))).thenReturn(page);

            Page<TestRunSummaryDTO> result = service.getAllRuns(0, 10);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("Run 1");
            assertThat(result.getContent().get(1).name()).isEqualTo("Run 2");
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page when no runs exist")
        void shouldReturnEmptyPage() {
            Page<TestRun> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(testRunRepository.findAllByOrderByStartTimeDesc(any(PageRequest.class))).thenReturn(emptyPage);

            Page<TestRunSummaryDTO> result = service.getAllRuns(0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getRunsByStatus tests")
    class GetRunsByStatusTests {

        @Test
        @DisplayName("Should filter runs by status")
        void shouldFilterByStatus() {
            TestRun run = createTestRun("run-1", "Failed Run", TestRun.RunStatus.FAILED, 10, 5, 4, 1);
            Page<TestRun> page = new PageImpl<>(List.of(run), PageRequest.of(0, 10), 1);

            when(testRunRepository.findByStatusOrderByStartTimeDesc(
                    eq(TestRun.RunStatus.FAILED), any(PageRequest.class))).thenReturn(page);

            Page<TestRunSummaryDTO> result = service.getRunsByStatus(TestRun.RunStatus.FAILED, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(TestRun.RunStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("searchRuns tests")
    class SearchRunsTests {

        @Test
        @DisplayName("Should search runs by name")
        void shouldSearchByName() {
            TestRun run = createTestRun("run-1", "Login Tests", TestRun.RunStatus.PASSED, 5, 5, 0, 0);
            Page<TestRun> page = new PageImpl<>(List.of(run), PageRequest.of(0, 10), 1);

            when(testRunRepository.findByNameContainingIgnoreCaseOrderByStartTimeDesc(
                    eq("login"), any(PageRequest.class))).thenReturn(page);

            Page<TestRunSummaryDTO> result = service.searchRuns("login", 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Login Tests");
        }
    }

    @Nested
    @DisplayName("getRunById tests")
    class GetRunByIdTests {

        @Test
        @DisplayName("Should return run summary by ID")
        void shouldReturnRunSummary() {
            TestRun run = createTestRun("run-1", "Test Run", TestRun.RunStatus.PASSED, 10, 8, 1, 1);
            when(testRunRepository.findById("run-1")).thenReturn(Optional.of(run));

            TestRunSummaryDTO result = service.getRunById("run-1");

            assertThat(result.id()).isEqualTo("run-1");
            assertThat(result.name()).isEqualTo("Test Run");
            assertThat(result.passRate()).isCloseTo(80.0, within(0.01));
            assertThat(result.duration()).isNotNull();
        }

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run not found")
        void shouldThrowWhenNotFound() {
            when(testRunRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRunById("missing"))
                    .isInstanceOf(TestRunNotFoundException.class)
                    .hasMessageContaining("missing");
        }
    }

    @Nested
    @DisplayName("getRunDetail tests")
    class GetRunDetailTests {

        @Test
        @DisplayName("Should delegate to HierarchyReconstructionService")
        void shouldDelegateToHierarchyService() {
            TestRunHierarchyDTO hierarchy = new TestRunHierarchyDTO(
                    "run-1", "Test Run", TestRun.RunStatus.PASSED,
                    timestamp, timestamp, NodeStatistics.empty(), Collections.emptyList());

            when(hierarchyReconstructionService.getFullHierarchy("run-1")).thenReturn(hierarchy);

            TestRunHierarchyDTO result = service.getRunDetail("run-1");

            assertThat(result.runId()).isEqualTo("run-1");
            verify(hierarchyReconstructionService).getFullHierarchy("run-1");
        }
    }

    @Nested
    @DisplayName("getRunStatistics tests")
    class GetRunStatisticsTests {

        @Test
        @DisplayName("Should delegate to HierarchyReconstructionService")
        void shouldDelegateToHierarchyService() {
            NodeStatistics stats = new NodeStatistics(10, 8, 1, 1, 0, 0, 5000L);
            when(hierarchyReconstructionService.calculateRunStatistics("run-1")).thenReturn(stats);

            NodeStatistics result = service.getRunStatistics("run-1");

            assertThat(result.totalNodes()).isEqualTo(10);
            assertThat(result.passedNodes()).isEqualTo(8);
            verify(hierarchyReconstructionService).calculateRunStatistics("run-1");
        }
    }

    @Nested
    @DisplayName("compareRuns tests")
    class CompareRunsTests {

        @Test
        @DisplayName("Should compare two runs and compute differences")
        void shouldCompareTwoRuns() {
            TestRun base = createTestRun("base", "Base Run", TestRun.RunStatus.PASSED, 10, 8, 1, 1);
            TestRun compare = createTestRun("compare", "Compare Run", TestRun.RunStatus.FAILED, 10, 6, 3, 1);

            when(testRunRepository.findById("base")).thenReturn(Optional.of(base));
            when(testRunRepository.findById("compare")).thenReturn(Optional.of(compare));

            RunComparisonDTO result = service.compareRuns("base", "compare");

            assertThat(result.baseRun().id()).isEqualTo("base");
            assertThat(result.compareRun().id()).isEqualTo("compare");
            assertThat(result.totalTestsDiff()).isEqualTo(0);
            assertThat(result.passedTestsDiff()).isEqualTo(-2);
            assertThat(result.failedTestsDiff()).isEqualTo(2);
            assertThat(result.passRateDiff()).isCloseTo(-20.0, within(0.01));
        }

        @Test
        @DisplayName("Should throw when base run not found")
        void shouldThrowWhenBaseNotFound() {
            when(testRunRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.compareRuns("missing", "other"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteRun tests")
    class DeleteRunTests {

        @Test
        @DisplayName("Should delete run and cascade to all associated data")
        void shouldDeleteRunWithCascade() {
            String runId = "run-1";
            TestRun run = createTestRun(runId, "Run", TestRun.RunStatus.PASSED, 2, 2, 0, 0);

            TestNode node1 = TestNode.builder().id("node-1").testRun(run)
                    .nodeType(TestNode.NodeType.SCENARIO).status(TestNode.NodeStatus.PASSED)
                    .name("Test 1").startTime(timestamp).build();
            TestNode node2 = TestNode.builder().id("node-2").testRun(run)
                    .nodeType(TestNode.NodeType.SCENARIO).status(TestNode.NodeStatus.PASSED)
                    .name("Test 2").startTime(timestamp).build();

            TestStep step1 = TestStep.builder().id("step-1").testNode(node1)
                    .name("Step 1").status(TestStep.StepStatus.PASSED).timestamp(timestamp).build();

            Attachment att1 = Attachment.builder().id("att-1").testNode(node1)
                    .name("screenshot.png").type(Attachment.AttachmentType.SCREENSHOT)
                    .filePath("/test").timestamp(timestamp).build();

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(List.of(node1, node2));
            when(testStepRepository.findByTestNodeIdIn(List.of("node-1", "node-2"))).thenReturn(List.of(step1));
            when(attachmentRepository.findByTestStepIdIn(List.of("step-1"))).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestNodeIdIn(List.of("node-1", "node-2"))).thenReturn(List.of(att1));

            service.deleteRun(runId);

            verify(attachmentStorageService).deleteFilesForRun(runId);
            verify(attachmentRepository).deleteAll(Collections.emptyList()); // step attachments
            verify(attachmentRepository).deleteAll(List.of(att1)); // node attachments
            verify(testStepRepository).deleteAll(List.of(step1));
            verify(testNodeRepository).deleteAll(List.of(node1, node2));
            verify(testRunRepository).deleteById(runId);
        }

        @Test
        @DisplayName("Should throw when run not found")
        void shouldThrowWhenRunNotFound() {
            when(testRunRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> service.deleteRun("missing"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should handle run with no nodes gracefully")
        void shouldHandleRunWithNoNodes() {
            String runId = "empty-run";
            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Collections.emptyList());

            service.deleteRun(runId);

            verify(attachmentStorageService).deleteFilesForRun(runId);
            verify(testRunRepository).deleteById(runId);
            verifyNoInteractions(testStepRepository);
        }
    }

    @Nested
    @DisplayName("deleteRunsBefore tests")
    class DeleteRunsBeforeTests {

        @Test
        @DisplayName("Should delete old runs and return count")
        void shouldDeleteOldRuns() {
            Instant cutoff = timestamp.minus(30, ChronoUnit.DAYS);
            TestRun oldRun = createTestRun("old-run", "Old Run", TestRun.RunStatus.PASSED, 5, 5, 0, 0);

            when(testRunRepository.findByStartTimeBefore(cutoff)).thenReturn(List.of(oldRun));
            when(testRunRepository.existsById("old-run")).thenReturn(true);
            when(testNodeRepository.findByTestRunId("old-run")).thenReturn(Collections.emptyList());

            int deleted = service.deleteRunsBefore(cutoff);

            assertThat(deleted).isEqualTo(1);
            verify(testRunRepository).deleteById("old-run");
        }

        @Test
        @DisplayName("Should return 0 when no old runs exist")
        void shouldReturnZeroWhenNoOldRuns() {
            Instant cutoff = timestamp.minus(30, ChronoUnit.DAYS);
            when(testRunRepository.findByStartTimeBefore(cutoff)).thenReturn(Collections.emptyList());

            int deleted = service.deleteRunsBefore(cutoff);

            assertThat(deleted).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getRunSummaryStats tests")
    class GetRunSummaryStatsTests {

        @Test
        @DisplayName("Should return aggregate stats across all runs")
        void shouldReturnAggregateStats() {
            when(testRunRepository.count()).thenReturn(15L);
            when(testRunRepository.countByStatus(TestRun.RunStatus.RUNNING)).thenReturn(2L);
            when(testRunRepository.countByStatus(TestRun.RunStatus.PASSED)).thenReturn(10L);
            when(testRunRepository.countByStatus(TestRun.RunStatus.FAILED)).thenReturn(2L);
            when(testRunRepository.countByStatus(TestRun.RunStatus.ABORTED)).thenReturn(1L);

            Map<String, Object> stats = service.getRunSummaryStats();

            assertThat(stats.get("totalRuns")).isEqualTo(15L);
            assertThat(stats.get("runningRuns")).isEqualTo(2L);
            assertThat(stats.get("passedRuns")).isEqualTo(10L);
            assertThat(stats.get("failedRuns")).isEqualTo(2L);
            assertThat(stats.get("abortedRuns")).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("TestRunSummaryDTO tests")
    class TestRunSummaryDTOTests {

        @Test
        @DisplayName("Should compute duration from start and end time")
        void shouldComputeDuration() {
            TestRun run = createTestRun("run-1", "Run", TestRun.RunStatus.PASSED, 10, 10, 0, 0);

            TestRunSummaryDTO dto = TestRunSummaryDTO.from(run);

            assertThat(dto.duration()).isNotNull();
            assertThat(dto.duration()).isEqualTo(3600000L); // 1 hour in ms
        }

        @Test
        @DisplayName("Should compute pass rate correctly")
        void shouldComputePassRate() {
            TestRun run = createTestRun("run-1", "Run", TestRun.RunStatus.PASSED, 10, 7, 2, 1);

            TestRunSummaryDTO dto = TestRunSummaryDTO.from(run);

            assertThat(dto.passRate()).isCloseTo(70.0, within(0.01));
        }

        @Test
        @DisplayName("Should handle null end time for running tests")
        void shouldHandleNullEndTime() {
            TestRun run = createRunningTestRun("run-1", "Running");

            TestRunSummaryDTO dto = TestRunSummaryDTO.from(run);

            assertThat(dto.duration()).isNull();
        }

        @Test
        @DisplayName("Should handle zero total tests")
        void shouldHandleZeroTotalTests() {
            TestRun run = TestRun.builder()
                    .id("run-1").name("Empty").status(TestRun.RunStatus.PASSED)
                    .startTime(timestamp).totalTests(0).passedTests(0)
                    .failedTests(0).skippedTests(0).build();

            TestRunSummaryDTO dto = TestRunSummaryDTO.from(run);

            assertThat(dto.passRate()).isNull();
        }
    }

    @Nested
    @DisplayName("RunComparisonDTO tests")
    class RunComparisonDTOTests {

        @Test
        @DisplayName("Should compute differences correctly")
        void shouldComputeDifferences() {
            TestRun base = createTestRun("base", "Base", TestRun.RunStatus.PASSED, 10, 8, 1, 1);
            TestRun compare = createTestRun("compare", "Compare", TestRun.RunStatus.PASSED, 12, 10, 1, 1);

            TestRunSummaryDTO baseSummary = TestRunSummaryDTO.from(base);
            TestRunSummaryDTO compareSummary = TestRunSummaryDTO.from(compare);

            RunComparisonDTO comparison = RunComparisonDTO.from(baseSummary, compareSummary);

            assertThat(comparison.totalTestsDiff()).isEqualTo(2);
            assertThat(comparison.passedTestsDiff()).isEqualTo(2);
            assertThat(comparison.failedTestsDiff()).isEqualTo(0);
            // compare passRate (10/12*100=83.33) - base passRate (8/10*100=80.0) ≈ 3.33
            assertThat(comparison.passRateDiff()).isCloseTo(3.33, within(0.1));
            // Same duration (1 hour each), so diff is 0
            assertThat(comparison.durationDiff()).isEqualTo(0L);
        }
    }
}

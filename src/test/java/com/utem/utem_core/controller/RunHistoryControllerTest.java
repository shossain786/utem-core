package com.utem.utem_core.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.utem.utem_core.dto.NodeStatistics;
import com.utem.utem_core.dto.RunComparisonDTO;
import com.utem.utem_core.dto.TestRunHierarchyDTO;
import com.utem.utem_core.dto.TestRunSummaryDTO;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.exception.TestRunNotFoundException;
import com.utem.utem_core.service.RunHistoryService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class RunHistoryControllerTest {

    @Mock
    private RunHistoryService runHistoryService;

    @InjectMocks
    private RunHistoryController controller;

    private final Instant timestamp = Instant.now();

    private TestRunSummaryDTO createSummary(String id, String name, TestRun.RunStatus status) {
        return new TestRunSummaryDTO(
                id, name, status,
                timestamp.minus(1, ChronoUnit.HOURS), timestamp,
                3600000L, 10, 8, 1, 1, 80.0, false, null, null
        );
    }

    @Nested
    @DisplayName("GET /utem/runs tests")
    class GetRunsTests {

        @Test
        @DisplayName("Should return all runs when no filters")
        void shouldReturnAllRuns() {
            TestRunSummaryDTO run = createSummary("run-1", "Run 1", TestRun.RunStatus.PASSED);
            Page<TestRunSummaryDTO> page = new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1);

            when(runHistoryService.getAllRuns(0, 20)).thenReturn(page);

            ResponseEntity<Page<TestRunSummaryDTO>> response = controller.getRuns(0, 20, null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).name()).isEqualTo("Run 1");
        }

        @Test
        @DisplayName("Should filter by status when provided")
        void shouldFilterByStatus() {
            TestRunSummaryDTO run = createSummary("run-1", "Failed Run", TestRun.RunStatus.FAILED);
            Page<TestRunSummaryDTO> page = new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1);

            when(runHistoryService.getRunsByStatus(TestRun.RunStatus.FAILED, 0, 20)).thenReturn(page);

            ResponseEntity<Page<TestRunSummaryDTO>> response =
                    controller.getRuns(0, 20, TestRun.RunStatus.FAILED, null, null);

            assertThat(response.getBody().getContent().get(0).status()).isEqualTo(TestRun.RunStatus.FAILED);
            verify(runHistoryService).getRunsByStatus(TestRun.RunStatus.FAILED, 0, 20);
        }

        @Test
        @DisplayName("Should search by name when provided (takes priority over status)")
        void shouldSearchByName() {
            TestRunSummaryDTO run = createSummary("run-1", "Login Tests", TestRun.RunStatus.PASSED);
            Page<TestRunSummaryDTO> page = new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1);

            when(runHistoryService.searchRuns("Login", 0, 20)).thenReturn(page);

            ResponseEntity<Page<TestRunSummaryDTO>> response =
                    controller.getRuns(0, 20, TestRun.RunStatus.FAILED, "Login", null);

            verify(runHistoryService).searchRuns("Login", 0, 20);
            verifyNoMoreInteractions(runHistoryService);
        }

        @Test
        @DisplayName("Should return empty page when no runs match")
        void shouldReturnEmptyPage() {
            Page<TestRunSummaryDTO> emptyPage = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(runHistoryService.getAllRuns(0, 20)).thenReturn(emptyPage);

            ResponseEntity<Page<TestRunSummaryDTO>> response = controller.getRuns(0, 20, null, null, null);

            assertThat(response.getBody().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /utem/runs/{runId} tests")
    class GetRunByIdTests {

        @Test
        @DisplayName("Should return run summary")
        void shouldReturnRunSummary() {
            TestRunSummaryDTO run = createSummary("run-1", "Test Run", TestRun.RunStatus.PASSED);
            when(runHistoryService.getRunById("run-1")).thenReturn(run);

            ResponseEntity<TestRunSummaryDTO> response = controller.getRunById("run-1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().id()).isEqualTo("run-1");
            assertThat(response.getBody().passRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should propagate TestRunNotFoundException")
        void shouldPropagateNotFound() {
            when(runHistoryService.getRunById("missing")).thenThrow(new TestRunNotFoundException("missing"));

            assertThatThrownBy(() -> controller.getRunById("missing"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /utem/runs/{runId}/detail tests")
    class GetRunDetailTests {

        @Test
        @DisplayName("Should return full hierarchy")
        void shouldReturnHierarchy() {
            TestRunHierarchyDTO hierarchy = new TestRunHierarchyDTO(
                    "run-1", "Test Run", TestRun.RunStatus.PASSED,
                    timestamp, timestamp, NodeStatistics.empty(), Collections.emptyList());

            when(runHistoryService.getRunDetail("run-1")).thenReturn(hierarchy);

            ResponseEntity<TestRunHierarchyDTO> response = controller.getRunDetail("run-1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().runId()).isEqualTo("run-1");
        }
    }

    @Nested
    @DisplayName("GET /utem/runs/{runId}/stats tests")
    class GetRunStatisticsTests {

        @Test
        @DisplayName("Should return node statistics")
        void shouldReturnStatistics() {
            NodeStatistics stats = new NodeStatistics(10, 8, 1, 1, 0, 0, 5000L);
            when(runHistoryService.getRunStatistics("run-1")).thenReturn(stats);

            ResponseEntity<NodeStatistics> response = controller.getRunStatistics("run-1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().totalNodes()).isEqualTo(10);
            assertThat(response.getBody().passedNodes()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("GET /utem/runs/{runId}/compare tests")
    class CompareRunsTests {

        @Test
        @DisplayName("Should return comparison between two runs")
        void shouldReturnComparison() {
            TestRunSummaryDTO base = createSummary("run-1", "Base", TestRun.RunStatus.PASSED);
            TestRunSummaryDTO compare = createSummary("run-2", "Compare", TestRun.RunStatus.FAILED);
            RunComparisonDTO comparison = new RunComparisonDTO(
                    base, compare, 0, -2, 2, 0, -20.0, 0L, java.util.List.of());

            when(runHistoryService.compareRuns("run-1", "run-2")).thenReturn(comparison);

            ResponseEntity<RunComparisonDTO> response = controller.compareRuns("run-1", "run-2");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().baseRun().id()).isEqualTo("run-1");
            assertThat(response.getBody().compareRun().id()).isEqualTo("run-2");
            assertThat(response.getBody().passedTestsDiff()).isEqualTo(-2);
        }
    }

    @Nested
    @DisplayName("GET /utem/runs/summary tests")
    class GetSummaryStatsTests {

        @Test
        @DisplayName("Should return aggregate stats")
        void shouldReturnAggregateStats() {
            Map<String, Object> stats = Map.of(
                    "totalRuns", 15L,
                    "runningRuns", 2L,
                    "passedRuns", 10L,
                    "failedRuns", 2L,
                    "abortedRuns", 1L
            );
            when(runHistoryService.getRunSummaryStats()).thenReturn(stats);

            ResponseEntity<Map<String, Object>> response = controller.getRunSummaryStats();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("totalRuns")).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("DELETE /utem/runs/{runId} tests")
    class DeleteRunTests {

        @Test
        @DisplayName("Should delete run successfully")
        void shouldDeleteRun() {
            doNothing().when(runHistoryService).deleteRun("run-1");

            controller.deleteRun("run-1");

            verify(runHistoryService).deleteRun("run-1");
        }

        @Test
        @DisplayName("Should propagate TestRunNotFoundException on delete")
        void shouldPropagateNotFoundOnDelete() {
            doThrow(new TestRunNotFoundException("missing")).when(runHistoryService).deleteRun("missing");

            assertThatThrownBy(() -> controller.deleteRun("missing"))
                    .isInstanceOf(TestRunNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DELETE /utem/runs?before=... tests")
    class DeleteRunsBeforeTests {

        @Test
        @DisplayName("Should delete old runs and return count")
        void shouldDeleteOldRuns() {
            Instant cutoff = timestamp.minus(30, ChronoUnit.DAYS);
            when(runHistoryService.deleteRunsBefore(cutoff)).thenReturn(5);

            ResponseEntity<Map<String, Object>> response = controller.deleteRunsBefore(cutoff);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("deletedCount")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return 0 when no runs to delete")
        void shouldReturnZero() {
            Instant cutoff = timestamp.minus(30, ChronoUnit.DAYS);
            when(runHistoryService.deleteRunsBefore(cutoff)).thenReturn(0);

            ResponseEntity<Map<String, Object>> response = controller.deleteRunsBefore(cutoff);

            assertThat(response.getBody().get("deletedCount")).isEqualTo(0);
        }
    }
}

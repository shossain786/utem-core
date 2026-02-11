package com.utem.utem_core.controller;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.service.SearchAndFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchAndFilterService searchAndFilterService;

    @InjectMocks
    private SearchController controller;

    private final Instant now = Instant.now();

    @Nested
    @DisplayName("GET /utem/search tests")
    class GlobalSearchTests {

        @Test
        @DisplayName("Should return global search results")
        void shouldReturnGlobalSearchResults() {
            SearchResultDTO result = new SearchResultDTO(
                    "Login", Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 0);

            when(searchAndFilterService.globalSearch("Login", 10)).thenReturn(result);

            ResponseEntity<SearchResultDTO> response = controller.globalSearch("Login", 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().query()).isEqualTo("Login");
            verify(searchAndFilterService).globalSearch("Login", 10);
        }

        @Test
        @DisplayName("Should use default limit of 10")
        void shouldUseDefaultLimit() {
            SearchResultDTO result = new SearchResultDTO(
                    "Test", Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), 0);

            when(searchAndFilterService.globalSearch("Test", 10)).thenReturn(result);

            ResponseEntity<SearchResultDTO> response = controller.globalSearch("Test", 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /utem/search/runs tests")
    class SearchRunsTests {

        @Test
        @DisplayName("Should search runs with all filters")
        void shouldSearchRunsWithFilters() {
            TestRunSummaryDTO run = new TestRunSummaryDTO(
                    "run-1", "Regression", TestRun.RunStatus.PASSED,
                    now.minus(1, ChronoUnit.HOURS), now, 3600000L,
                    10, 8, 1, 1, 80.0);
            Page<TestRunSummaryDTO> page = new PageImpl<>(List.of(run));

            Instant from = now.minus(7, ChronoUnit.DAYS);
            Instant to = now;

            when(searchAndFilterService.searchRuns(TestRun.RunStatus.PASSED, "Regression", from, to, 0, 20))
                    .thenReturn(page);

            ResponseEntity<Page<TestRunSummaryDTO>> response =
                    controller.searchRuns(TestRun.RunStatus.PASSED, "Regression", from, to, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).name()).isEqualTo("Regression");
        }

        @Test
        @DisplayName("Should search runs with no filters")
        void shouldSearchRunsWithNoFilters() {
            Page<TestRunSummaryDTO> emptyPage = Page.empty();

            when(searchAndFilterService.searchRuns(null, null, null, null, 0, 20))
                    .thenReturn(emptyPage);

            ResponseEntity<Page<TestRunSummaryDTO>> response =
                    controller.searchRuns(null, null, null, null, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("GET /utem/search/nodes tests")
    class SearchNodesTests {

        @Test
        @DisplayName("Should search nodes with filters")
        void shouldSearchNodesWithFilters() {
            TestNodeSummaryDTO node = new TestNodeSummaryDTO(
                    "n1", "run-1", "Run 1", TestNode.NodeType.SCENARIO,
                    "Login Test", TestNode.NodeStatus.PASSED, now, 1000L, false);
            Page<TestNodeSummaryDTO> page = new PageImpl<>(List.of(node));

            when(searchAndFilterService.searchNodes("run-1", "Login", null, null, 0, 20))
                    .thenReturn(page);

            ResponseEntity<Page<TestNodeSummaryDTO>> response =
                    controller.searchNodes("run-1", "Login", null, null, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).name()).isEqualTo("Login Test");
        }

        @Test
        @DisplayName("Should search nodes by status")
        void shouldSearchNodesByStatus() {
            Page<TestNodeSummaryDTO> page = Page.empty();

            when(searchAndFilterService.searchNodes(null, null, TestNode.NodeStatus.FAILED, null, 0, 20))
                    .thenReturn(page);

            ResponseEntity<Page<TestNodeSummaryDTO>> response =
                    controller.searchNodes(null, null, TestNode.NodeStatus.FAILED, null, 0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(searchAndFilterService).searchNodes(null, null, TestNode.NodeStatus.FAILED, null, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /utem/search/steps tests")
    class SearchStepsTests {

        @Test
        @DisplayName("Should search steps by error text")
        void shouldSearchStepsByErrorText() {
            TestStepSummaryDTO step = new TestStepSummaryDTO(
                    "s1", "n1", "Test Node", "Click button",
                    TestStep.StepStatus.FAILED, 500L, "Element not found");

            when(searchAndFilterService.searchSteps(null, null, null, "Element not found"))
                    .thenReturn(List.of(step));

            ResponseEntity<List<TestStepSummaryDTO>> response =
                    controller.searchSteps(null, null, null, "Element not found");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).errorMessage()).isEqualTo("Element not found");
        }

        @Test
        @DisplayName("Should search steps by node ID and status")
        void shouldSearchStepsByNodeIdAndStatus() {
            when(searchAndFilterService.searchSteps("n1", null, TestStep.StepStatus.FAILED, null))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<List<TestStepSummaryDTO>> response =
                    controller.searchSteps("n1", null, TestStep.StepStatus.FAILED, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(searchAndFilterService).searchSteps("n1", null, TestStep.StepStatus.FAILED, null);
        }
    }

    @Nested
    @DisplayName("GET /utem/search/attachments tests")
    class SearchAttachmentsTests {

        @Test
        @DisplayName("Should search attachments by type and name")
        void shouldSearchAttachmentsByTypeAndName() {
            AttachmentSummaryDTO attachment = new AttachmentSummaryDTO(
                    "a1", "n1", "screenshot.png", Attachment.AttachmentType.SCREENSHOT,
                    "image/png", 1024L, now, false);

            when(searchAndFilterService.searchAttachments(null, Attachment.AttachmentType.SCREENSHOT, "screenshot"))
                    .thenReturn(List.of(attachment));

            ResponseEntity<List<AttachmentSummaryDTO>> response =
                    controller.searchAttachments(null, Attachment.AttachmentType.SCREENSHOT, "screenshot");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("screenshot.png");
        }
    }

    @Nested
    @DisplayName("GET /utem/search/runs/{runId}/failed-steps tests")
    class FailedStepsTests {

        @Test
        @DisplayName("Should return failed steps for a run")
        void shouldReturnFailedSteps() {
            TestStepSummaryDTO step = new TestStepSummaryDTO(
                    "s1", "n1", "Test Node", "Assert title",
                    TestStep.StepStatus.FAILED, 200L, "Expected 'Home' but got 'Login'");

            when(searchAndFilterService.searchFailedSteps("run-1")).thenReturn(List.of(step));

            ResponseEntity<List<TestStepSummaryDTO>> response = controller.getFailedSteps("run-1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).status()).isEqualTo(TestStep.StepStatus.FAILED);
        }

        @Test
        @DisplayName("Should return empty list when no failed steps")
        void shouldReturnEmptyWhenNoFailedSteps() {
            when(searchAndFilterService.searchFailedSteps("run-1")).thenReturn(Collections.emptyList());

            ResponseEntity<List<TestStepSummaryDTO>> response = controller.getFailedSteps("run-1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }
}

package com.utem.utem_core.service;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAndFilterServiceTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestNodeRepository testNodeRepository;

    @Mock
    private TestStepRepository testStepRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    private SearchAndFilterService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        service = new SearchAndFilterService(
                testRunRepository, testNodeRepository, testStepRepository, attachmentRepository);
    }

    // ============ Helpers ============

    private TestRun createRun(String id, String name, TestRun.RunStatus status, int daysAgo) {
        return TestRun.builder()
                .id(id).name(name)
                .status(status)
                .startTime(now.minus(daysAgo, ChronoUnit.DAYS))
                .endTime(now.minus(daysAgo, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .totalTests(10).passedTests(8).failedTests(2).skippedTests(0)
                .build();
    }

    private TestNode createNode(String id, String name, TestNode.NodeType nodeType,
                                 TestNode.NodeStatus status, TestRun run) {
        return TestNode.builder()
                .id(id).name(name)
                .nodeType(nodeType)
                .status(status)
                .testRun(run)
                .startTime(run.getStartTime())
                .duration(1000L)
                .flaky(false)
                .build();
    }

    private TestStep createStep(String id, String name, TestStep.StepStatus status,
                                 TestNode node, String errorMessage) {
        return TestStep.builder()
                .id(id).name(name)
                .status(status)
                .testNode(node)
                .timestamp(now)
                .duration(500L)
                .stepOrder(1)
                .errorMessage(errorMessage)
                .build();
    }

    private Attachment createAttachment(String id, String name, Attachment.AttachmentType type,
                                         TestNode node) {
        return Attachment.builder()
                .id(id).name(name)
                .type(type)
                .testNode(node)
                .filePath("/path/" + name)
                .mimeType("image/png")
                .fileSize(1024L)
                .timestamp(now)
                .build();
    }

    // ============ Tests ============

    @Nested
    @DisplayName("searchRuns tests")
    class SearchRunsTests {

        @Test
        @DisplayName("Should search runs with all filters")
        void shouldSearchWithAllFilters() {
            TestRun run = createRun("run-1", "Regression Suite", TestRun.RunStatus.PASSED, 1);
            Page<TestRun> page = new PageImpl<>(List.of(run));

            when(testRunRepository.searchRuns(
                    eq(TestRun.RunStatus.PASSED), eq("Regression"), any(Instant.class), any(Instant.class),
                    any(PageRequest.class)))
                    .thenReturn(page);

            Instant from = now.minus(7, ChronoUnit.DAYS);
            Instant to = now;

            Page<TestRunSummaryDTO> result = service.searchRuns(
                    TestRun.RunStatus.PASSED, "Regression", from, to, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Regression Suite");
        }

        @Test
        @DisplayName("Should search runs with null filters")
        void shouldSearchWithNullFilters() {
            TestRun run = createRun("run-1", "Any Run", TestRun.RunStatus.FAILED, 0);
            Page<TestRun> page = new PageImpl<>(List.of(run));

            when(testRunRepository.searchRuns(isNull(), isNull(), isNull(), isNull(),
                    any(PageRequest.class)))
                    .thenReturn(page);

            Page<TestRunSummaryDTO> result = service.searchRuns(null, null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            when(testRunRepository.searchRuns(any(), any(), any(), any(), any(PageRequest.class)))
                    .thenReturn(Page.empty());

            Page<TestRunSummaryDTO> result = service.searchRuns(
                    TestRun.RunStatus.ABORTED, "nonexistent", null, null, 0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchNodes tests")
    class SearchNodesTests {

        @Test
        @DisplayName("Should search nodes by name within a run")
        void shouldSearchByNameWithinRun() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Login Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            Page<TestNode> page = new PageImpl<>(List.of(node));

            when(testNodeRepository.findByTestRunIdAndNameContainingIgnoreCaseOrderByStartTimeDesc(
                    eq("run-1"), eq("Login"), any(PageRequest.class)))
                    .thenReturn(page);

            Page<TestNodeSummaryDTO> result = service.searchNodes("run-1", "Login", null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Login Test");
            assertThat(result.getContent().get(0).runId()).isEqualTo("run-1");
        }

        @Test
        @DisplayName("Should search nodes by name across all runs")
        void shouldSearchByNameAcrossRuns() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Login Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            Page<TestNode> page = new PageImpl<>(List.of(node));

            when(testNodeRepository.findByNameContainingIgnoreCaseOrderByStartTimeDesc(
                    eq("Login"), any(PageRequest.class)))
                    .thenReturn(page);

            Page<TestNodeSummaryDTO> result = service.searchNodes(null, "Login", null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search nodes by status")
        void shouldSearchByStatus() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.FAILED, 0);
            TestNode node = createNode("n1", "Failing Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, run);
            Page<TestNode> page = new PageImpl<>(List.of(node));

            when(testNodeRepository.findByStatusOrderByStartTimeDesc(
                    eq(TestNode.NodeStatus.FAILED), any(PageRequest.class)))
                    .thenReturn(page);

            Page<TestNodeSummaryDTO> result = service.searchNodes(null, null, TestNode.NodeStatus.FAILED, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(TestNode.NodeStatus.FAILED);
        }

        @Test
        @DisplayName("Should search nodes by node type")
        void shouldSearchByNodeType() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, run);
            Page<TestNode> page = new PageImpl<>(List.of(node));

            when(testNodeRepository.findByNodeTypeOrderByStartTimeDesc(
                    eq(TestNode.NodeType.SUITE), any(PageRequest.class)))
                    .thenReturn(page);

            Page<TestNodeSummaryDTO> result = service.searchNodes(null, null, null, TestNode.NodeType.SUITE, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).nodeType()).isEqualTo(TestNode.NodeType.SUITE);
        }
    }

    @Nested
    @DisplayName("searchSteps tests")
    class SearchStepsTests {

        @Test
        @DisplayName("Should search steps by node ID and status")
        void shouldSearchByNodeIdAndStatus() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.FAILED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, run);
            TestStep step = createStep("s1", "Click button", TestStep.StepStatus.FAILED,
                    node, "Element not found");

            when(testStepRepository.findByTestNodeIdAndStatus("n1", TestStep.StepStatus.FAILED))
                    .thenReturn(List.of(step));

            List<TestStepSummaryDTO> result = service.searchSteps("n1", null, TestStep.StepStatus.FAILED, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).errorMessage()).isEqualTo("Element not found");
        }

        @Test
        @DisplayName("Should search steps by error message text")
        void shouldSearchByErrorMessage() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.FAILED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, run);
            TestStep step = createStep("s1", "Click button", TestStep.StepStatus.FAILED,
                    node, "TimeoutException: Element not found after 30s");

            when(testStepRepository.findByErrorMessageContainingIgnoreCase("TimeoutException"))
                    .thenReturn(List.of(step));

            List<TestStepSummaryDTO> result = service.searchSteps(null, null, null, "TimeoutException");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Click button");
        }

        @Test
        @DisplayName("Should search steps by name")
        void shouldSearchByName() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            TestStep step = createStep("s1", "Navigate to login", TestStep.StepStatus.PASSED, node, null);

            when(testStepRepository.findByNameContainingIgnoreCase("login"))
                    .thenReturn(List.of(step));

            List<TestStepSummaryDTO> result = service.searchSteps(null, "login", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Navigate to login");
        }
    }

    @Nested
    @DisplayName("searchAttachments tests")
    class SearchAttachmentsTests {

        @Test
        @DisplayName("Should search attachments by node ID and type")
        void shouldSearchByNodeIdAndType() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            Attachment attachment = createAttachment("a1", "screenshot.png",
                    Attachment.AttachmentType.SCREENSHOT, node);

            when(attachmentRepository.findByTestNodeIdAndType("n1", Attachment.AttachmentType.SCREENSHOT))
                    .thenReturn(List.of(attachment));

            List<AttachmentSummaryDTO> result = service.searchAttachments(
                    "n1", Attachment.AttachmentType.SCREENSHOT, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("screenshot.png");
        }

        @Test
        @DisplayName("Should search attachments by name")
        void shouldSearchByName() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            Attachment attachment = createAttachment("a1", "failure-screenshot.png",
                    Attachment.AttachmentType.SCREENSHOT, node);

            when(attachmentRepository.findByNameContainingIgnoreCase("failure"))
                    .thenReturn(List.of(attachment));

            List<AttachmentSummaryDTO> result = service.searchAttachments(null, null, "failure");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("failure-screenshot.png");
        }

        @Test
        @DisplayName("Should search attachments by type and name")
        void shouldSearchByTypeAndName() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            Attachment attachment = createAttachment("a1", "test.log",
                    Attachment.AttachmentType.LOG, node);

            when(attachmentRepository.findByTypeAndNameContainingIgnoreCase(
                    Attachment.AttachmentType.LOG, "test"))
                    .thenReturn(List.of(attachment));

            List<AttachmentSummaryDTO> result = service.searchAttachments(
                    null, Attachment.AttachmentType.LOG, "test");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("globalSearch tests")
    class GlobalSearchTests {

        @Test
        @DisplayName("Should search across all entity types")
        void shouldSearchAcrossAllTypes() {
            TestRun run = createRun("run-1", "Login Tests", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Login Test Case", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);
            TestStep step = createStep("s1", "Login step", TestStep.StepStatus.PASSED, node, null);
            Attachment attachment = createAttachment("a1", "login-screenshot.png",
                    Attachment.AttachmentType.SCREENSHOT, node);

            when(testRunRepository.findByNameContainingIgnoreCase("Login"))
                    .thenReturn(List.of(run));
            when(testNodeRepository.findByNameContainingIgnoreCase("Login"))
                    .thenReturn(List.of(node));
            when(testStepRepository.findByNameContainingIgnoreCase("Login"))
                    .thenReturn(List.of(step));
            when(attachmentRepository.findByNameContainingIgnoreCase("Login"))
                    .thenReturn(List.of(attachment));

            SearchResultDTO result = service.globalSearch("Login", 10);

            assertThat(result.query()).isEqualTo("Login");
            assertThat(result.runs()).hasSize(1);
            assertThat(result.nodes()).hasSize(1);
            assertThat(result.steps()).hasSize(1);
            assertThat(result.attachments()).hasSize(1);
            assertThat(result.totalResults()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should respect limit per entity type")
        void shouldRespectLimit() {
            TestRun run1 = createRun("run-1", "Test Run A", TestRun.RunStatus.PASSED, 2);
            TestRun run2 = createRun("run-2", "Test Run B", TestRun.RunStatus.PASSED, 1);
            TestRun run3 = createRun("run-3", "Test Run C", TestRun.RunStatus.PASSED, 0);

            when(testRunRepository.findByNameContainingIgnoreCase("Test"))
                    .thenReturn(List.of(run1, run2, run3));
            when(testNodeRepository.findByNameContainingIgnoreCase("Test"))
                    .thenReturn(Collections.emptyList());
            when(testStepRepository.findByNameContainingIgnoreCase("Test"))
                    .thenReturn(Collections.emptyList());
            when(attachmentRepository.findByNameContainingIgnoreCase("Test"))
                    .thenReturn(Collections.emptyList());

            SearchResultDTO result = service.globalSearch("Test", 2);

            assertThat(result.runs()).hasSize(2); // Limited to 2
            assertThat(result.totalResults()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty results when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            when(testRunRepository.findByNameContainingIgnoreCase("xyz"))
                    .thenReturn(Collections.emptyList());
            when(testNodeRepository.findByNameContainingIgnoreCase("xyz"))
                    .thenReturn(Collections.emptyList());
            when(testStepRepository.findByNameContainingIgnoreCase("xyz"))
                    .thenReturn(Collections.emptyList());
            when(attachmentRepository.findByNameContainingIgnoreCase("xyz"))
                    .thenReturn(Collections.emptyList());

            SearchResultDTO result = service.globalSearch("xyz", 10);

            assertThat(result.totalResults()).isEqualTo(0);
            assertThat(result.runs()).isEmpty();
            assertThat(result.nodes()).isEmpty();
            assertThat(result.steps()).isEmpty();
            assertThat(result.attachments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchFailedSteps tests")
    class SearchFailedStepsTests {

        @Test
        @DisplayName("Should return failed steps for a run")
        void shouldReturnFailedSteps() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.FAILED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, run);

            TestStep passedStep = createStep("s1", "Step 1", TestStep.StepStatus.PASSED, node, null);
            TestStep failedStep = createStep("s2", "Step 2", TestStep.StepStatus.FAILED,
                    node, "Assertion failed");

            when(testNodeRepository.findByTestRunId("run-1")).thenReturn(List.of(node));
            when(testStepRepository.findByTestNodeIdIn(List.of("n1")))
                    .thenReturn(List.of(passedStep, failedStep));

            List<TestStepSummaryDTO> result = service.searchFailedSteps("run-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Step 2");
            assertThat(result.get(0).errorMessage()).isEqualTo("Assertion failed");
        }

        @Test
        @DisplayName("Should return empty list when run has no nodes")
        void shouldReturnEmptyWhenNoNodes() {
            when(testNodeRepository.findByTestRunId("run-1")).thenReturn(Collections.emptyList());

            List<TestStepSummaryDTO> result = service.searchFailedSteps("run-1");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when no failed steps")
        void shouldReturnEmptyWhenNoFailedSteps() {
            TestRun run = createRun("run-1", "Run 1", TestRun.RunStatus.PASSED, 0);
            TestNode node = createNode("n1", "Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, run);

            TestStep passedStep = createStep("s1", "Step 1", TestStep.StepStatus.PASSED, node, null);

            when(testNodeRepository.findByTestRunId("run-1")).thenReturn(List.of(node));
            when(testStepRepository.findByTestNodeIdIn(List.of("n1")))
                    .thenReturn(List.of(passedStep));

            List<TestStepSummaryDTO> result = service.searchFailedSteps("run-1");

            assertThat(result).isEmpty();
        }
    }
}

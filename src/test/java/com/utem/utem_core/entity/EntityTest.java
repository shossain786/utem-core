package com.utem.utem_core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
class EntityTest {

    @Nested
    @DisplayName("TestRun Entity Tests")
    class TestRunEntityTests {

        @Test
        @DisplayName("Should create TestRun using builder")
        void shouldCreateUsingBuilder() {
            Instant now = Instant.now();

            TestRun testRun = TestRun.builder()
                    .name("Integration Tests")
                    .startTime(now)
                    .status(TestRun.RunStatus.RUNNING)
                    .totalTests(50)
                    .passedTests(0)
                    .failedTests(0)
                    .skippedTests(0)
                    .metadata("{\"env\": \"production\"}")
                    .build();

            assertThat(testRun.getName()).isEqualTo("Integration Tests");
            assertThat(testRun.getStartTime()).isEqualTo(now);
            assertThat(testRun.getStatus()).isEqualTo(TestRun.RunStatus.RUNNING);
            assertThat(testRun.getTotalTests()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should create TestRun using no-args constructor")
        void shouldCreateUsingNoArgsConstructor() {
            TestRun testRun = new TestRun();
            testRun.setName("Unit Tests");
            testRun.setStatus(TestRun.RunStatus.PASSED);

            assertThat(testRun.getName()).isEqualTo("Unit Tests");
            assertThat(testRun.getStatus()).isEqualTo(TestRun.RunStatus.PASSED);
        }

        @Test
        @DisplayName("Should have all RunStatus values")
        void shouldHaveAllStatusValues() {
            assertThat(TestRun.RunStatus.values()).containsExactlyInAnyOrder(
                    TestRun.RunStatus.RUNNING,
                    TestRun.RunStatus.PASSED,
                    TestRun.RunStatus.FAILED,
                    TestRun.RunStatus.ABORTED
            );
        }
    }

    @Nested
    @DisplayName("TestNode Entity Tests")
    class TestNodeEntityTests {

        @Test
        @DisplayName("Should create TestNode using builder")
        void shouldCreateUsingBuilder() {
            TestRun testRun = TestRun.builder()
                    .name("Test Run")
                    .startTime(Instant.now())
                    .status(TestRun.RunStatus.RUNNING)
                    .build();

            TestNode testNode = TestNode.builder()
                    .testRun(testRun)
                    .nodeType(TestNode.NodeType.SUITE)
                    .name("Login Suite")
                    .status(TestNode.NodeStatus.RUNNING)
                    .startTime(Instant.now())
                    .flaky(false)
                    .retryCount(0)
                    .build();

            assertThat(testNode.getName()).isEqualTo("Login Suite");
            assertThat(testNode.getNodeType()).isEqualTo(TestNode.NodeType.SUITE);
            assertThat(testNode.getTestRun()).isEqualTo(testRun);
        }

        @Test
        @DisplayName("Should have all NodeType values")
        void shouldHaveAllNodeTypeValues() {
            assertThat(TestNode.NodeType.values()).containsExactlyInAnyOrder(
                    TestNode.NodeType.SUITE,
                    TestNode.NodeType.FEATURE,
                    TestNode.NodeType.SCENARIO,
                    TestNode.NodeType.STEP
            );
        }

        @Test
        @DisplayName("Should have all NodeStatus values")
        void shouldHaveAllNodeStatusValues() {
            assertThat(TestNode.NodeStatus.values()).containsExactlyInAnyOrder(
                    TestNode.NodeStatus.PENDING,
                    TestNode.NodeStatus.RUNNING,
                    TestNode.NodeStatus.PASSED,
                    TestNode.NodeStatus.FAILED,
                    TestNode.NodeStatus.SKIPPED
            );
        }

        @Test
        @DisplayName("Should support parent-child relationship")
        void shouldSupportParentChildRelationship() {
            TestRun testRun = TestRun.builder()
                    .name("Test Run")
                    .startTime(Instant.now())
                    .status(TestRun.RunStatus.RUNNING)
                    .build();

            TestNode parent = TestNode.builder()
                    .testRun(testRun)
                    .nodeType(TestNode.NodeType.SUITE)
                    .name("Parent Suite")
                    .status(TestNode.NodeStatus.RUNNING)
                    .startTime(Instant.now())
                    .build();

            TestNode child = TestNode.builder()
                    .testRun(testRun)
                    .parent(parent)
                    .nodeType(TestNode.NodeType.FEATURE)
                    .name("Child Feature")
                    .status(TestNode.NodeStatus.PENDING)
                    .startTime(Instant.now())
                    .build();

            assertThat(child.getParent()).isEqualTo(parent);
            assertThat(child.getParent().getName()).isEqualTo("Parent Suite");
        }
    }

    @Nested
    @DisplayName("TestStep Entity Tests")
    class TestStepEntityTests {

        @Test
        @DisplayName("Should create TestStep using builder")
        void shouldCreateUsingBuilder() {
            TestRun testRun = TestRun.builder()
                    .name("Test Run")
                    .startTime(Instant.now())
                    .status(TestRun.RunStatus.RUNNING)
                    .build();

            TestNode testNode = TestNode.builder()
                    .testRun(testRun)
                    .nodeType(TestNode.NodeType.SCENARIO)
                    .name("Login Scenario")
                    .status(TestNode.NodeStatus.RUNNING)
                    .startTime(Instant.now())
                    .build();

            TestStep step = TestStep.builder()
                    .testNode(testNode)
                    .name("Click login button")
                    .status(TestStep.StepStatus.PASSED)
                    .timestamp(Instant.now())
                    .duration(1500L)
                    .stepOrder(1)
                    .build();

            assertThat(step.getName()).isEqualTo("Click login button");
            assertThat(step.getStatus()).isEqualTo(TestStep.StepStatus.PASSED);
            assertThat(step.getDuration()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("Should have all StepStatus values")
        void shouldHaveAllStepStatusValues() {
            assertThat(TestStep.StepStatus.values()).containsExactlyInAnyOrder(
                    TestStep.StepStatus.PENDING,
                    TestStep.StepStatus.RUNNING,
                    TestStep.StepStatus.PASSED,
                    TestStep.StepStatus.FAILED,
                    TestStep.StepStatus.SKIPPED
            );
        }

        @Test
        @DisplayName("Should store error details")
        void shouldStoreErrorDetails() {
            TestStep step = TestStep.builder()
                    .name("Failed Step")
                    .status(TestStep.StepStatus.FAILED)
                    .timestamp(Instant.now())
                    .errorMessage("Element not found")
                    .stackTrace("org.example.Exception: Element not found\n\tat Test.java:10")
                    .build();

            assertThat(step.getErrorMessage()).isEqualTo("Element not found");
            assertThat(step.getStackTrace()).contains("Exception");
        }
    }

    @Nested
    @DisplayName("Attachment Entity Tests")
    class AttachmentEntityTests {

        @Test
        @DisplayName("Should create Attachment using builder")
        void shouldCreateUsingBuilder() {
            Attachment attachment = Attachment.builder()
                    .name("screenshot.png")
                    .type(Attachment.AttachmentType.SCREENSHOT)
                    .filePath("/attachments/screenshot.png")
                    .mimeType("image/png")
                    .fileSize(102400L)
                    .timestamp(Instant.now())
                    .isFailureScreenshot(true)
                    .build();

            assertThat(attachment.getName()).isEqualTo("screenshot.png");
            assertThat(attachment.getType()).isEqualTo(Attachment.AttachmentType.SCREENSHOT);
            assertThat(attachment.getIsFailureScreenshot()).isTrue();
        }

        @Test
        @DisplayName("Should have all AttachmentType values")
        void shouldHaveAllAttachmentTypeValues() {
            assertThat(Attachment.AttachmentType.values()).containsExactlyInAnyOrder(
                    Attachment.AttachmentType.SCREENSHOT,
                    Attachment.AttachmentType.LOG,
                    Attachment.AttachmentType.VIDEO,
                    Attachment.AttachmentType.FILE
            );
        }
    }

    @Nested
    @DisplayName("EventLog Entity Tests")
    class EventLogEntityTests {

        @Test
        @DisplayName("Should create EventLog using builder")
        void shouldCreateUsingBuilder() {
            Instant now = Instant.now();

            EventLog event = EventLog.builder()
                    .eventId("evt-123")
                    .runId("run-456")
                    .eventType(EventLog.EventType.TEST_RUN_STARTED)
                    .timestamp(now)
                    .receivedAt(now)
                    .payload("{\"name\": \"Test Suite\"}")
                    .build();

            assertThat(event.getEventId()).isEqualTo("evt-123");
            assertThat(event.getRunId()).isEqualTo("run-456");
            assertThat(event.getEventType()).isEqualTo(EventLog.EventType.TEST_RUN_STARTED);
        }

        @Test
        @DisplayName("Should have all EventType values")
        void shouldHaveAllEventTypeValues() {
            assertThat(EventLog.EventType.values()).containsExactlyInAnyOrder(
                    EventLog.EventType.TEST_RUN_STARTED,
                    EventLog.EventType.TEST_RUN_FINISHED,
                    EventLog.EventType.TEST_SUITE_STARTED,
                    EventLog.EventType.TEST_SUITE_FINISHED,
                    EventLog.EventType.TEST_CASE_STARTED,
                    EventLog.EventType.TEST_CASE_FINISHED,
                    EventLog.EventType.TEST_STEP,
                    EventLog.EventType.TEST_PASSED,
                    EventLog.EventType.TEST_FAILED,
                    EventLog.EventType.TEST_SKIPPED,
                    EventLog.EventType.ATTACHMENT
            );
        }

        @Test
        @DisplayName("Should support optional parentId")
        void shouldSupportOptionalParentId() {
            EventLog eventWithParent = EventLog.builder()
                    .eventId("evt-child")
                    .runId("run-1")
                    .eventType(EventLog.EventType.TEST_SUITE_STARTED)
                    .parentId("parent-id")
                    .timestamp(Instant.now())
                    .payload("{}")
                    .build();

            EventLog eventWithoutParent = EventLog.builder()
                    .eventId("evt-root")
                    .runId("run-1")
                    .eventType(EventLog.EventType.TEST_RUN_STARTED)
                    .timestamp(Instant.now())
                    .payload("{}")
                    .build();

            assertThat(eventWithParent.getParentId()).isEqualTo("parent-id");
            assertThat(eventWithoutParent.getParentId()).isNull();
        }
    }
}

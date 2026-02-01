package com.utem.utem_core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.utem.utem_core.entity.*;
import com.utem.utem_core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessingServiceTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestNodeRepository testNodeRepository;

    @Mock
    private TestStepRepository testStepRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    private ObjectMapper objectMapper;
    private EventProcessingService eventProcessingService;

    private String runId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString();
        timestamp = Instant.now();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        eventProcessingService = new EventProcessingService(
                testRunRepository,
                testNodeRepository,
                testStepRepository,
                attachmentRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should create TestRun when processing TEST_RUN_STARTED event")
    void shouldCreateTestRunOnStartedEvent() {
        EventLog eventLog = createEventLog(
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                "{\"name\": \"My Test Run\", \"metadata\": \"{\\\"env\\\": \\\"test\\\"}\"}"
        );

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(UUID.randomUUID().toString());
            return tr;
        });

        eventProcessingService.processEvent(eventLog);

        ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());

        TestRun saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("My Test Run");
        assertThat(saved.getStatus()).isEqualTo(TestRun.RunStatus.RUNNING);
        assertThat(saved.getStartTime()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should update TestRun when processing TEST_RUN_FINISHED event")
    void shouldUpdateTestRunOnFinishedEvent() {
        String startEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();

        EventLog startEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(startEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .parentId(null)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });

        eventProcessingService.processEvent(startEvent);

        EventLog finishEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_FINISHED)
                .parentId(startEventId)
                .timestamp(timestamp.plusSeconds(10))
                .payload("{\"totalTests\": 10, \"passedTests\": 8, \"failedTests\": 2, \"skippedTests\": 0}")
                .receivedAt(Instant.now())
                .build();

        TestRun existingRun = TestRun.builder()
                .id(testRunId)
                .name("Test Run")
                .status(TestRun.RunStatus.RUNNING)
                .startTime(timestamp)
                .build();

        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(existingRun));

        eventProcessingService.processEvent(finishEvent);

        verify(testRunRepository, times(2)).save(any(TestRun.class));
        assertThat(existingRun.getStatus()).isEqualTo(TestRun.RunStatus.FAILED);
        assertThat(existingRun.getTotalTests()).isEqualTo(10);
        assertThat(existingRun.getPassedTests()).isEqualTo(8);
        assertThat(existingRun.getFailedTests()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create TestNode with SUITE type when processing TEST_SUITE_STARTED event")
    void shouldCreateSuiteNodeOnSuiteStartedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Now create suite
        EventLog suiteEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SUITE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Login Suite\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(UUID.randomUUID().toString());
            return node;
        });

        eventProcessingService.processEvent(suiteEvent);

        ArgumentCaptor<TestNode> captor = ArgumentCaptor.forClass(TestNode.class);
        verify(testNodeRepository).save(captor.capture());

        TestNode saved = captor.getValue();
        assertThat(saved.getNodeType()).isEqualTo(TestNode.NodeType.SUITE);
        assertThat(saved.getName()).isEqualTo("Login Suite");
        assertThat(saved.getStatus()).isEqualTo(TestNode.NodeStatus.RUNNING);
    }

    @Test
    @DisplayName("Should update TestNode when processing TEST_SUITE_FINISHED event")
    void shouldUpdateSuiteNodeOnSuiteFinishedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String suiteEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create suite
        EventLog suiteStartEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(suiteEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SUITE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Login Suite\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(nodeId);
            return node;
        });

        eventProcessingService.processEvent(suiteStartEvent);

        // Now finish suite
        TestNode existingNode = TestNode.builder()
                .id(nodeId)
                .nodeType(TestNode.NodeType.SUITE)
                .name("Login Suite")
                .status(TestNode.NodeStatus.RUNNING)
                .build();

        when(testNodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));

        EventLog finishEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SUITE_FINISHED)
                .parentId(suiteEventId)
                .timestamp(timestamp.plusSeconds(5))
                .payload("{\"nodeStatus\": \"PASSED\", \"duration\": 5000}")
                .receivedAt(Instant.now())
                .build();

        eventProcessingService.processEvent(finishEvent);

        assertThat(existingNode.getStatus()).isEqualTo(TestNode.NodeStatus.PASSED);
        assertThat(existingNode.getDuration()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Should create TestNode with SCENARIO type when processing TEST_CASE_STARTED event")
    void shouldCreateScenarioNodeOnCaseStartedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String suiteEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String suiteNodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create suite
        EventLog suiteEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(suiteEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SUITE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Suite\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            if (node.getId() == null) {
                node.setId(node.getNodeType() == TestNode.NodeType.SUITE ? suiteNodeId : UUID.randomUUID().toString());
            }
            return node;
        });
        when(testNodeRepository.findById(suiteNodeId)).thenReturn(Optional.of(
                TestNode.builder().id(suiteNodeId).nodeType(TestNode.NodeType.SUITE).build()
        ));

        eventProcessingService.processEvent(suiteEvent);

        // Now process case started
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(suiteEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Login with valid credentials\"}")
                .receivedAt(Instant.now())
                .build();

        eventProcessingService.processEvent(caseEvent);

        ArgumentCaptor<TestNode> captor = ArgumentCaptor.forClass(TestNode.class);
        verify(testNodeRepository, times(2)).save(captor.capture());

        TestNode lastSaved = captor.getAllValues().get(1);
        assertThat(lastSaved.getNodeType()).isEqualTo(TestNode.NodeType.SCENARIO);
        assertThat(lastSaved.getName()).isEqualTo("Login with valid credentials");
    }

    @Test
    @DisplayName("Should create TestStep when processing TEST_STEP event")
    void shouldCreateTestStepOnStepEvent() {
        String runEventId = UUID.randomUUID().toString();
        String caseEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String caseNodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create case
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(caseEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Case\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(caseNodeId);
            return node;
        });
        when(testNodeRepository.findById(caseNodeId)).thenReturn(Optional.of(
                TestNode.builder().id(caseNodeId).nodeType(TestNode.NodeType.SCENARIO).build()
        ));

        eventProcessingService.processEvent(caseEvent);

        // Now process step
        EventLog stepEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_STEP)
                .parentId(caseEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Click login button\", \"stepOrder\": 1, \"duration\": 150}")
                .receivedAt(Instant.now())
                .build();

        when(testStepRepository.save(any(TestStep.class))).thenAnswer(inv -> {
            TestStep step = inv.getArgument(0);
            step.setId(UUID.randomUUID().toString());
            return step;
        });

        eventProcessingService.processEvent(stepEvent);

        ArgumentCaptor<TestStep> captor = ArgumentCaptor.forClass(TestStep.class);
        verify(testStepRepository).save(captor.capture());

        TestStep saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Click login button");
        assertThat(saved.getStepOrder()).isEqualTo(1);
        assertThat(saved.getDuration()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Should update TestNode status to PASSED when processing TEST_PASSED event")
    void shouldUpdateNodeStatusOnPassedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String caseEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create case
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(caseEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Case\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(nodeId);
            return node;
        });

        eventProcessingService.processEvent(caseEvent);

        // Now process passed event
        TestNode existingNode = TestNode.builder()
                .id(nodeId)
                .status(TestNode.NodeStatus.RUNNING)
                .build();

        when(testNodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));

        EventLog passedEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_PASSED)
                .parentId(caseEventId)
                .timestamp(timestamp.plusSeconds(1))
                .payload("{}")
                .receivedAt(Instant.now())
                .build();

        eventProcessingService.processEvent(passedEvent);

        assertThat(existingNode.getStatus()).isEqualTo(TestNode.NodeStatus.PASSED);
    }

    @Test
    @DisplayName("Should update TestNode status to FAILED when processing TEST_FAILED event")
    void shouldUpdateNodeStatusOnFailedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String caseEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create case
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(caseEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Case\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(nodeId);
            return node;
        });

        eventProcessingService.processEvent(caseEvent);

        // Now process failed event
        TestNode existingNode = TestNode.builder()
                .id(nodeId)
                .status(TestNode.NodeStatus.RUNNING)
                .build();

        when(testNodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));

        EventLog failedEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_FAILED)
                .parentId(caseEventId)
                .timestamp(timestamp.plusSeconds(1))
                .payload("{\"errorMessage\": \"Element not found\"}")
                .receivedAt(Instant.now())
                .build();

        eventProcessingService.processEvent(failedEvent);

        assertThat(existingNode.getStatus()).isEqualTo(TestNode.NodeStatus.FAILED);
    }

    @Test
    @DisplayName("Should update TestNode status to SKIPPED when processing TEST_SKIPPED event")
    void shouldUpdateNodeStatusOnSkippedEvent() {
        String runEventId = UUID.randomUUID().toString();
        String caseEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create case
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(caseEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Case\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(nodeId);
            return node;
        });

        eventProcessingService.processEvent(caseEvent);

        // Now process skipped event
        TestNode existingNode = TestNode.builder()
                .id(nodeId)
                .status(TestNode.NodeStatus.PENDING)
                .build();

        when(testNodeRepository.findById(nodeId)).thenReturn(Optional.of(existingNode));

        EventLog skippedEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_SKIPPED)
                .parentId(caseEventId)
                .timestamp(timestamp.plusSeconds(1))
                .payload("{}")
                .receivedAt(Instant.now())
                .build();

        eventProcessingService.processEvent(skippedEvent);

        assertThat(existingNode.getStatus()).isEqualTo(TestNode.NodeStatus.SKIPPED);
    }

    @Test
    @DisplayName("Should create Attachment when processing ATTACHMENT event")
    void shouldCreateAttachmentOnAttachmentEvent() {
        String runEventId = UUID.randomUUID().toString();
        String caseEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();
        String caseNodeId = UUID.randomUUID().toString();

        // First create TestRun
        EventLog runEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(runEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });
        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(
                TestRun.builder().id(testRunId).name("Test Run").status(TestRun.RunStatus.RUNNING).build()
        ));

        eventProcessingService.processEvent(runEvent);

        // Create case
        EventLog caseEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(caseEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_CASE_STARTED)
                .parentId(runEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Case\"}")
                .receivedAt(Instant.now())
                .build();

        when(testNodeRepository.save(any(TestNode.class))).thenAnswer(inv -> {
            TestNode node = inv.getArgument(0);
            node.setId(caseNodeId);
            return node;
        });
        when(testNodeRepository.findById(caseNodeId)).thenReturn(Optional.of(
                TestNode.builder().id(caseNodeId).nodeType(TestNode.NodeType.SCENARIO).build()
        ));

        eventProcessingService.processEvent(caseEvent);

        // Now process attachment
        EventLog attachmentEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.ATTACHMENT)
                .parentId(caseEventId)
                .timestamp(timestamp)
                .payload("{\"name\": \"failure-screenshot.png\", \"attachmentType\": \"SCREENSHOT\", " +
                        "\"filePath\": \"/screenshots/failure-screenshot.png\", \"mimeType\": \"image/png\", " +
                        "\"fileSize\": 12345, \"isFailureScreenshot\": true}")
                .receivedAt(Instant.now())
                .build();

        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
            Attachment att = inv.getArgument(0);
            att.setId(UUID.randomUUID().toString());
            return att;
        });

        eventProcessingService.processEvent(attachmentEvent);

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());

        Attachment saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("failure-screenshot.png");
        assertThat(saved.getType()).isEqualTo(Attachment.AttachmentType.SCREENSHOT);
        assertThat(saved.getFilePath()).isEqualTo("/screenshots/failure-screenshot.png");
        assertThat(saved.getIsFailureScreenshot()).isTrue();
    }

    @Test
    @DisplayName("Should handle invalid JSON payload gracefully")
    void shouldHandleInvalidJsonPayload() {
        EventLog eventLog = createEventLog(
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                "{ invalid json }"
        );

        eventProcessingService.processEvent(eventLog);

        verify(testRunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle missing parent gracefully for TEST_SUITE_FINISHED")
    void shouldHandleMissingParentGracefully() {
        EventLog eventLog = createEventLog(
                EventLog.EventType.TEST_SUITE_FINISHED,
                "non-existent-parent-id",
                "{}"
        );

        eventProcessingService.processEvent(eventLog);

        verify(testNodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should use default name when name is not provided in payload")
    void shouldUseDefaultNameWhenNotProvided() {
        EventLog eventLog = createEventLog(
                EventLog.EventType.TEST_RUN_STARTED,
                null,
                "{}"
        );

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(UUID.randomUUID().toString());
            return tr;
        });

        eventProcessingService.processEvent(eventLog);

        ArgumentCaptor<TestRun> captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());

        assertThat(captor.getValue().getName()).isEqualTo("Unnamed Test Run");
    }

    @Test
    @DisplayName("Should determine PASSED status when no failed tests")
    void shouldDeterminePassedStatusWhenNoFailedTests() {
        String startEventId = UUID.randomUUID().toString();
        String testRunId = UUID.randomUUID().toString();

        EventLog startEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(startEventId)
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_STARTED)
                .timestamp(timestamp)
                .payload("{\"name\": \"Test Run\"}")
                .receivedAt(Instant.now())
                .build();

        when(testRunRepository.save(any(TestRun.class))).thenAnswer(inv -> {
            TestRun tr = inv.getArgument(0);
            tr.setId(testRunId);
            return tr;
        });

        eventProcessingService.processEvent(startEvent);

        EventLog finishEvent = EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(EventLog.EventType.TEST_RUN_FINISHED)
                .parentId(startEventId)
                .timestamp(timestamp.plusSeconds(10))
                .payload("{\"totalTests\": 10, \"passedTests\": 10, \"failedTests\": 0, \"skippedTests\": 0}")
                .receivedAt(Instant.now())
                .build();

        TestRun existingRun = TestRun.builder()
                .id(testRunId)
                .name("Test Run")
                .status(TestRun.RunStatus.RUNNING)
                .startTime(timestamp)
                .build();

        when(testRunRepository.findById(testRunId)).thenReturn(Optional.of(existingRun));

        eventProcessingService.processEvent(finishEvent);

        assertThat(existingRun.getStatus()).isEqualTo(TestRun.RunStatus.PASSED);
    }

    private EventLog createEventLog(EventLog.EventType eventType, String parentId, String payload) {
        return EventLog.builder()
                .id(UUID.randomUUID().toString())
                .eventId(UUID.randomUUID().toString())
                .runId(runId)
                .eventType(eventType)
                .parentId(parentId)
                .timestamp(timestamp)
                .payload(payload)
                .receivedAt(Instant.now())
                .build();
    }
}

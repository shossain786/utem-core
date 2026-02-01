package com.utem.utem_core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utem.utem_core.dto.EventPayload;
import com.utem.utem_core.entity.*;
import com.utem.utem_core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final TestStepRepository testStepRepository;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, String> eventToTestRunMap = new ConcurrentHashMap<>();
    private final Map<String, String> eventToTestNodeMap = new ConcurrentHashMap<>();
    private final Map<String, String> eventToTestStepMap = new ConcurrentHashMap<>();

    @Transactional
    public void processEvent(EventLog eventLog) {
        try {
            EventPayload payload = parsePayload(eventLog.getPayload());

            switch (eventLog.getEventType()) {
                case TEST_RUN_STARTED -> handleTestRunStarted(eventLog, payload);
                case TEST_RUN_FINISHED -> handleTestRunFinished(eventLog, payload);
                case TEST_SUITE_STARTED -> handleTestSuiteStarted(eventLog, payload);
                case TEST_SUITE_FINISHED -> handleTestSuiteFinished(eventLog, payload);
                case TEST_CASE_STARTED -> handleTestCaseStarted(eventLog, payload);
                case TEST_CASE_FINISHED -> handleTestCaseFinished(eventLog, payload);
                case TEST_STEP -> handleTestStep(eventLog, payload);
                case TEST_PASSED -> handleTestPassed(eventLog, payload);
                case TEST_FAILED -> handleTestFailed(eventLog, payload);
                case TEST_SKIPPED -> handleTestSkipped(eventLog, payload);
                case ATTACHMENT -> handleAttachment(eventLog, payload);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse payload for event {}: {}", eventLog.getEventId(), e.getMessage());
        }
    }

    private EventPayload parsePayload(String payloadJson) throws JsonProcessingException {
        return objectMapper.readValue(payloadJson, EventPayload.class);
    }

    private void handleTestRunStarted(EventLog eventLog, EventPayload payload) {
        TestRun testRun = TestRun.builder()
                .name(payload.name() != null ? payload.name() : "Unnamed Test Run")
                .startTime(eventLog.getTimestamp())
                .status(TestRun.RunStatus.RUNNING)
                .metadata(payload.metadata())
                .totalTests(payload.totalTests())
                .passedTests(0)
                .failedTests(0)
                .skippedTests(0)
                .build();

        TestRun saved = testRunRepository.save(testRun);
        eventToTestRunMap.put(eventLog.getEventId(), saved.getId());
        eventToTestRunMap.put(eventLog.getRunId(), saved.getId());
        log.debug("Created TestRun {} for event {}", saved.getId(), eventLog.getEventId());
    }

    private void handleTestRunFinished(EventLog eventLog, EventPayload payload) {
        String testRunId = resolveTestRunId(eventLog);
        if (testRunId == null) {
            log.warn("No TestRun found for TEST_RUN_FINISHED event {}", eventLog.getEventId());
            return;
        }

        testRunRepository.findById(testRunId).ifPresent(testRun -> {
            testRun.setEndTime(eventLog.getTimestamp());
            testRun.setStatus(determineRunStatus(payload));
            if (payload.totalTests() != null) testRun.setTotalTests(payload.totalTests());
            if (payload.passedTests() != null) testRun.setPassedTests(payload.passedTests());
            if (payload.failedTests() != null) testRun.setFailedTests(payload.failedTests());
            if (payload.skippedTests() != null) testRun.setSkippedTests(payload.skippedTests());
            testRunRepository.save(testRun);
            log.debug("Updated TestRun {} for event {}", testRunId, eventLog.getEventId());
        });
    }

    private TestRun.RunStatus determineRunStatus(EventPayload payload) {
        if (payload.runStatus() != null) {
            return payload.runStatus();
        }
        if (payload.failedTests() != null && payload.failedTests() > 0) {
            return TestRun.RunStatus.FAILED;
        }
        return TestRun.RunStatus.PASSED;
    }

    private void handleTestSuiteStarted(EventLog eventLog, EventPayload payload) {
        TestRun testRun = resolveTestRun(eventLog);
        if (testRun == null) {
            log.warn("No TestRun found for TEST_SUITE_STARTED event {}", eventLog.getEventId());
            return;
        }

        TestNode parentNode = resolveParentNode(eventLog.getParentId());

        TestNode suite = TestNode.builder()
                .testRun(testRun)
                .parent(parentNode)
                .nodeType(TestNode.NodeType.SUITE)
                .name(payload.name() != null ? payload.name() : "Unnamed Suite")
                .status(TestNode.NodeStatus.RUNNING)
                .startTime(eventLog.getTimestamp())
                .metadata(payload.metadata())
                .flaky(payload.flaky())
                .retryCount(payload.retryCount())
                .build();

        TestNode saved = testNodeRepository.save(suite);
        eventToTestNodeMap.put(eventLog.getEventId(), saved.getId());
        log.debug("Created TestNode (SUITE) {} for event {}", saved.getId(), eventLog.getEventId());
    }

    private void handleTestSuiteFinished(EventLog eventLog, EventPayload payload) {
        String nodeId = eventToTestNodeMap.get(eventLog.getParentId());
        if (nodeId == null) {
            log.warn("No TestNode found for TEST_SUITE_FINISHED event {}", eventLog.getEventId());
            return;
        }

        testNodeRepository.findById(nodeId).ifPresent(node -> {
            node.setEndTime(eventLog.getTimestamp());
            node.setStatus(payload.nodeStatus() != null ? payload.nodeStatus() : TestNode.NodeStatus.PASSED);
            if (payload.duration() != null) node.setDuration(payload.duration());
            testNodeRepository.save(node);
            log.debug("Updated TestNode (SUITE) {} for event {}", nodeId, eventLog.getEventId());
        });
    }

    private void handleTestCaseStarted(EventLog eventLog, EventPayload payload) {
        TestRun testRun = resolveTestRun(eventLog);
        if (testRun == null) {
            log.warn("No TestRun found for TEST_CASE_STARTED event {}", eventLog.getEventId());
            return;
        }

        TestNode parentNode = resolveParentNode(eventLog.getParentId());

        TestNode testCase = TestNode.builder()
                .testRun(testRun)
                .parent(parentNode)
                .nodeType(TestNode.NodeType.SCENARIO)
                .name(payload.name() != null ? payload.name() : "Unnamed Test Case")
                .status(TestNode.NodeStatus.RUNNING)
                .startTime(eventLog.getTimestamp())
                .metadata(payload.metadata())
                .flaky(payload.flaky())
                .retryCount(payload.retryCount())
                .build();

        TestNode saved = testNodeRepository.save(testCase);
        eventToTestNodeMap.put(eventLog.getEventId(), saved.getId());
        log.debug("Created TestNode (SCENARIO) {} for event {}", saved.getId(), eventLog.getEventId());
    }

    private void handleTestCaseFinished(EventLog eventLog, EventPayload payload) {
        String nodeId = eventToTestNodeMap.get(eventLog.getParentId());
        if (nodeId == null) {
            log.warn("No TestNode found for TEST_CASE_FINISHED event {}", eventLog.getEventId());
            return;
        }

        testNodeRepository.findById(nodeId).ifPresent(node -> {
            node.setEndTime(eventLog.getTimestamp());
            node.setStatus(payload.nodeStatus() != null ? payload.nodeStatus() : TestNode.NodeStatus.PASSED);
            if (payload.duration() != null) node.setDuration(payload.duration());
            testNodeRepository.save(node);
            log.debug("Updated TestNode (SCENARIO) {} for event {}", nodeId, eventLog.getEventId());
        });
    }

    private void handleTestStep(EventLog eventLog, EventPayload payload) {
        TestNode parentNode = resolveParentNode(eventLog.getParentId());
        if (parentNode == null) {
            log.warn("No TestNode found for TEST_STEP event {}", eventLog.getEventId());
            return;
        }

        TestStep step = TestStep.builder()
                .testNode(parentNode)
                .name(payload.name() != null ? payload.name() : "Unnamed Step")
                .status(payload.stepStatus() != null ? payload.stepStatus() : TestStep.StepStatus.PASSED)
                .timestamp(eventLog.getTimestamp())
                .duration(payload.duration())
                .stepOrder(payload.stepOrder())
                .errorMessage(payload.errorMessage())
                .stackTrace(payload.stackTrace())
                .metadata(payload.metadata())
                .build();

        TestStep saved = testStepRepository.save(step);
        eventToTestStepMap.put(eventLog.getEventId(), saved.getId());
        log.debug("Created TestStep {} for event {}", saved.getId(), eventLog.getEventId());
    }

    private void handleTestPassed(EventLog eventLog, EventPayload payload) {
        updateNodeStatus(eventLog, TestNode.NodeStatus.PASSED, payload);
    }

    private void handleTestFailed(EventLog eventLog, EventPayload payload) {
        updateNodeStatus(eventLog, TestNode.NodeStatus.FAILED, payload);
    }

    private void handleTestSkipped(EventLog eventLog, EventPayload payload) {
        updateNodeStatus(eventLog, TestNode.NodeStatus.SKIPPED, payload);
    }

    private void updateNodeStatus(EventLog eventLog, TestNode.NodeStatus status, EventPayload payload) {
        String nodeId = eventToTestNodeMap.get(eventLog.getParentId());
        if (nodeId == null) {
            log.warn("No TestNode found for status update event {}", eventLog.getEventId());
            return;
        }

        testNodeRepository.findById(nodeId).ifPresent(node -> {
            node.setStatus(status);
            node.setEndTime(eventLog.getTimestamp());
            if (payload.duration() != null) node.setDuration(payload.duration());
            testNodeRepository.save(node);
            log.debug("Updated TestNode {} status to {} for event {}", nodeId, status, eventLog.getEventId());
        });
    }

    private void handleAttachment(EventLog eventLog, EventPayload payload) {
        TestNode testNode = resolveParentNode(eventLog.getParentId());
        TestStep testStep = resolveParentStep(eventLog.getParentId());

        if (testNode == null && testStep == null) {
            log.warn("No parent found for ATTACHMENT event {}", eventLog.getEventId());
            return;
        }

        Attachment attachment = Attachment.builder()
                .testNode(testNode)
                .testStep(testStep)
                .name(payload.name() != null ? payload.name() : "Unnamed Attachment")
                .type(payload.attachmentType() != null ? payload.attachmentType() : Attachment.AttachmentType.FILE)
                .filePath(payload.filePath() != null ? payload.filePath() : "")
                .mimeType(payload.mimeType())
                .fileSize(payload.fileSize())
                .timestamp(eventLog.getTimestamp())
                .isFailureScreenshot(payload.isFailureScreenshot())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.debug("Created Attachment {} for event {}", saved.getId(), eventLog.getEventId());
    }

    private String resolveTestRunId(EventLog eventLog) {
        String testRunId = null;
        if (eventLog.getParentId() != null) {
            testRunId = eventToTestRunMap.get(eventLog.getParentId());
        }
        if (testRunId == null && eventLog.getRunId() != null) {
            testRunId = eventToTestRunMap.get(eventLog.getRunId());
        }
        return testRunId;
    }

    private TestRun resolveTestRun(EventLog eventLog) {
        String testRunId = resolveTestRunId(eventLog);
        if (testRunId != null) {
            return testRunRepository.findById(testRunId).orElse(null);
        }
        return null;
    }

    private TestNode resolveParentNode(String parentEventId) {
        if (parentEventId == null) return null;

        String nodeId = eventToTestNodeMap.get(parentEventId);
        if (nodeId != null) {
            return testNodeRepository.findById(nodeId).orElse(null);
        }
        return null;
    }

    private TestStep resolveParentStep(String parentEventId) {
        if (parentEventId == null) return null;

        String stepId = eventToTestStepMap.get(parentEventId);
        if (stepId != null) {
            return testStepRepository.findById(stepId).orElse(null);
        }
        return null;
    }
}

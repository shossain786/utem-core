package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TestStepRepositoryTest {

    @Autowired
    private TestStepRepository testStepRepository;

    @Autowired
    private TestNodeRepository testNodeRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    private TestRun testRun;
    private TestNode testNode;
    private TestStep step1;
    private TestStep step2;

    @BeforeEach
    void setUp() {
        testStepRepository.deleteAll();
        testNodeRepository.deleteAll();
        testRunRepository.deleteAll();

        testRun = TestRun.builder()
                .name("Test Suite")
                .startTime(Instant.now())
                .status(TestRun.RunStatus.RUNNING)
                .build();
        testRun = testRunRepository.save(testRun);

        testNode = TestNode.builder()
                .testRun(testRun)
                .nodeType(TestNode.NodeType.SCENARIO)
                .name("Login Scenario")
                .status(TestNode.NodeStatus.RUNNING)
                .startTime(Instant.now())
                .build();
        testNode = testNodeRepository.save(testNode);

        step1 = TestStep.builder()
                .testNode(testNode)
                .name("Navigate to login page")
                .status(TestStep.StepStatus.PASSED)
                .timestamp(Instant.now())
                .duration(1000L)
                .stepOrder(1)
                .build();

        step2 = TestStep.builder()
                .testNode(testNode)
                .name("Enter credentials")
                .status(TestStep.StepStatus.RUNNING)
                .timestamp(Instant.now())
                .stepOrder(2)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve step by ID")
    void shouldSaveAndFindById() {
        TestStep saved = testStepRepository.save(step1);

        Optional<TestStep> found = testStepRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Navigate to login page");
        assertThat(found.get().getStatus()).isEqualTo(TestStep.StepStatus.PASSED);
    }

    @Test
    @DisplayName("Should find steps by test node")
    void shouldFindByTestNode() {
        testStepRepository.save(step1);
        testStepRepository.save(step2);

        List<TestStep> steps = testStepRepository.findByTestNodeId(testNode.getId());

        assertThat(steps).hasSize(2);
    }

    @Test
    @DisplayName("Should find steps ordered by step order")
    void shouldFindByNodeIdOrderByStepOrder() {
        testStepRepository.save(step2); // Save step2 first
        testStepRepository.save(step1); // Save step1 second

        List<TestStep> steps = testStepRepository.findByTestNodeIdOrderByStepOrderAsc(testNode.getId());

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).getStepOrder()).isEqualTo(1);
        assertThat(steps.get(1).getStepOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find steps by status within a node")
    void shouldFindByNodeIdAndStatus() {
        testStepRepository.save(step1);
        testStepRepository.save(step2);

        List<TestStep> passedSteps = testStepRepository.findByTestNodeIdAndStatus(
                testNode.getId(),
                TestStep.StepStatus.PASSED
        );
        List<TestStep> runningSteps = testStepRepository.findByTestNodeIdAndStatus(
                testNode.getId(),
                TestStep.StepStatus.RUNNING
        );

        assertThat(passedSteps).hasSize(1);
        assertThat(runningSteps).hasSize(1);
    }

    @Test
    @DisplayName("Should find all steps by status")
    void shouldFindByStatus() {
        testStepRepository.save(step1);
        testStepRepository.save(step2);

        List<TestStep> passedSteps = testStepRepository.findByStatus(TestStep.StepStatus.PASSED);

        assertThat(passedSteps).hasSize(1);
        assertThat(passedSteps.get(0).getName()).isEqualTo("Navigate to login page");
    }

    @Test
    @DisplayName("Should save step with error details")
    void shouldSaveStepWithErrorDetails() {
        TestStep failedStep = TestStep.builder()
                .testNode(testNode)
                .name("Click login button")
                .status(TestStep.StepStatus.FAILED)
                .timestamp(Instant.now())
                .stepOrder(3)
                .errorMessage("Element not found: #login-button")
                .stackTrace("org.openqa.selenium.NoSuchElementException: ...")
                .build();

        TestStep saved = testStepRepository.save(failedStep);

        Optional<TestStep> found = testStepRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TestStep.StepStatus.FAILED);
        assertThat(found.get().getErrorMessage()).contains("Element not found");
        assertThat(found.get().getStackTrace()).contains("NoSuchElementException");
    }

    @Test
    @DisplayName("Should update step status and duration")
    void shouldUpdateStepStatusAndDuration() {
        TestStep saved = testStepRepository.save(step2);

        saved.setStatus(TestStep.StepStatus.PASSED);
        saved.setDuration(2500L);
        testStepRepository.save(saved);

        Optional<TestStep> updated = testStepRepository.findById(saved.getId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(TestStep.StepStatus.PASSED);
        assertThat(updated.get().getDuration()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("Should save step with metadata")
    void shouldSaveStepWithMetadata() {
        step1.setMetadata("{\"screenshot\": \"step1.png\", \"data\": {\"username\": \"testuser\"}}");
        TestStep saved = testStepRepository.save(step1);

        Optional<TestStep> found = testStepRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMetadata()).contains("screenshot");
        assertThat(found.get().getMetadata()).contains("testuser");
    }

    @Test
    @DisplayName("Should delete step")
    void shouldDeleteStep() {
        TestStep saved = testStepRepository.save(step1);
        String id = saved.getId();

        testStepRepository.deleteById(id);

        Optional<TestStep> deleted = testStepRepository.findById(id);
        assertThat(deleted).isEmpty();
    }
}

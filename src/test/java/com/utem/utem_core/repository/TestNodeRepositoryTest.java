package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
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
class TestNodeRepositoryTest {

    @Autowired
    private TestNodeRepository testNodeRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    private TestRun testRun;
    private TestNode suiteNode;
    private TestNode featureNode;

    @BeforeEach
    void setUp() {
        testNodeRepository.deleteAll();
        testRunRepository.deleteAll();

        testRun = TestRun.builder()
                .name("Test Suite")
                .startTime(Instant.now())
                .status(TestRun.RunStatus.RUNNING)
                .build();
        testRun = testRunRepository.save(testRun);

        suiteNode = TestNode.builder()
                .testRun(testRun)
                .nodeType(TestNode.NodeType.SUITE)
                .name("Login Suite")
                .status(TestNode.NodeStatus.RUNNING)
                .startTime(Instant.now())
                .flaky(false)
                .build();

        featureNode = TestNode.builder()
                .testRun(testRun)
                .nodeType(TestNode.NodeType.FEATURE)
                .name("Login Feature")
                .status(TestNode.NodeStatus.PENDING)
                .startTime(Instant.now())
                .flaky(false)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve test node by ID")
    void shouldSaveAndFindById() {
        TestNode saved = testNodeRepository.save(suiteNode);

        Optional<TestNode> found = testNodeRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Login Suite");
        assertThat(found.get().getNodeType()).isEqualTo(TestNode.NodeType.SUITE);
    }

    @Test
    @DisplayName("Should find nodes by test run ID")
    void shouldFindByTestRunId() {
        testNodeRepository.save(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> nodes = testNodeRepository.findByTestRunId(testRun.getId());

        assertThat(nodes).hasSize(2);
    }

    @Test
    @DisplayName("Should find child nodes by parent")
    void shouldFindByParent() {
        TestNode savedSuite = testNodeRepository.save(suiteNode);

        featureNode.setParent(savedSuite);
        testNodeRepository.save(featureNode);

        TestNode scenarioNode = TestNode.builder()
                .testRun(testRun)
                .parent(savedSuite)
                .nodeType(TestNode.NodeType.SCENARIO)
                .name("Valid Login Scenario")
                .status(TestNode.NodeStatus.PENDING)
                .startTime(Instant.now())
                .build();
        testNodeRepository.save(scenarioNode);

        List<TestNode> children = testNodeRepository.findByParentId(savedSuite.getId());

        assertThat(children).hasSize(2);
    }

    @Test
    @DisplayName("Should find root nodes (nodes without parent)")
    void shouldFindRootNodes() {
        testNodeRepository.save(suiteNode);

        featureNode.setParent(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> rootNodes = testNodeRepository.findByTestRunIdAndParentIsNull(testRun.getId());

        assertThat(rootNodes).hasSize(1);
        assertThat(rootNodes.get(0).getName()).isEqualTo("Login Suite");
    }

    @Test
    @DisplayName("Should find nodes by type")
    void shouldFindByNodeType() {
        testNodeRepository.save(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> suites = testNodeRepository.findByTestRunIdAndNodeType(
                testRun.getId(),
                TestNode.NodeType.SUITE
        );
        List<TestNode> features = testNodeRepository.findByTestRunIdAndNodeType(
                testRun.getId(),
                TestNode.NodeType.FEATURE
        );

        assertThat(suites).hasSize(1);
        assertThat(features).hasSize(1);
    }

    @Test
    @DisplayName("Should find nodes by status")
    void shouldFindByStatus() {
        testNodeRepository.save(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> runningNodes = testNodeRepository.findByTestRunIdAndStatus(
                testRun.getId(),
                TestNode.NodeStatus.RUNNING
        );
        List<TestNode> pendingNodes = testNodeRepository.findByTestRunIdAndStatus(
                testRun.getId(),
                TestNode.NodeStatus.PENDING
        );

        assertThat(runningNodes).hasSize(1);
        assertThat(pendingNodes).hasSize(1);
    }

    @Test
    @DisplayName("Should find flaky nodes")
    void shouldFindFlakyNodes() {
        suiteNode.setFlaky(true);
        testNodeRepository.save(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> flakyNodes = testNodeRepository.findByFlakyTrue();

        assertThat(flakyNodes).hasSize(1);
        assertThat(flakyNodes.get(0).getName()).isEqualTo("Login Suite");
    }

    @Test
    @DisplayName("Should find nodes by name containing string")
    void shouldFindByNameContaining() {
        testNodeRepository.save(suiteNode);
        testNodeRepository.save(featureNode);

        List<TestNode> results = testNodeRepository.findByNameContainingIgnoreCase("login");

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should update node status and duration")
    void shouldUpdateNodeStatusAndDuration() {
        TestNode saved = testNodeRepository.save(suiteNode);

        saved.setStatus(TestNode.NodeStatus.PASSED);
        saved.setEndTime(Instant.now());
        saved.setDuration(5000L);
        testNodeRepository.save(saved);

        Optional<TestNode> updated = testNodeRepository.findById(saved.getId());

        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(TestNode.NodeStatus.PASSED);
        assertThat(updated.get().getDuration()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Should build hierarchical structure")
    void shouldBuildHierarchy() {
        TestNode savedSuite = testNodeRepository.save(suiteNode);

        featureNode.setParent(savedSuite);
        TestNode savedFeature = testNodeRepository.save(featureNode);

        TestNode scenarioNode = TestNode.builder()
                .testRun(testRun)
                .parent(savedFeature)
                .nodeType(TestNode.NodeType.SCENARIO)
                .name("Valid Login Scenario")
                .status(TestNode.NodeStatus.PENDING)
                .startTime(Instant.now())
                .build();
        TestNode savedScenario = testNodeRepository.save(scenarioNode);

        // Verify hierarchy
        List<TestNode> rootNodes = testNodeRepository.findByTestRunIdAndParentIsNull(testRun.getId());
        assertThat(rootNodes).hasSize(1);

        List<TestNode> suiteChildren = testNodeRepository.findByParentId(savedSuite.getId());
        assertThat(suiteChildren).hasSize(1);
        assertThat(suiteChildren.get(0).getName()).isEqualTo("Login Feature");

        List<TestNode> featureChildren = testNodeRepository.findByParentId(savedFeature.getId());
        assertThat(featureChildren).hasSize(1);
        assertThat(featureChildren.get(0).getName()).isEqualTo("Valid Login Scenario");
    }
}

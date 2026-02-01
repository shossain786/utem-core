package com.utem.utem_core.service;

import com.utem.utem_core.dto.*;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.exception.TestNodeNotFoundException;
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

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HierarchyReconstructionServiceTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestNodeRepository testNodeRepository;

    @Mock
    private TestStepRepository testStepRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    private HierarchyReconstructionService service;

    private String runId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString();
        timestamp = Instant.now();
        service = new HierarchyReconstructionService(
                testRunRepository,
                testNodeRepository,
                testStepRepository,
                attachmentRepository
        );
    }

    @Nested
    @DisplayName("getFullHierarchy tests")
    class GetFullHierarchyTests {

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run does not exist")
        void shouldThrowExceptionWhenRunNotFound() {
            when(testRunRepository.findById(runId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFullHierarchy(runId))
                    .isInstanceOf(TestRunNotFoundException.class)
                    .hasMessageContaining(runId);
        }

        @Test
        @DisplayName("Should return empty hierarchy for run with no nodes")
        void shouldReturnEmptyHierarchyForRunWithNoNodes() {
            TestRun testRun = createTestRun(runId, "Empty Run", TestRun.RunStatus.PASSED);
            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Collections.emptyList());

            TestRunHierarchyDTO result = service.getFullHierarchy(runId);

            assertThat(result.runId()).isEqualTo(runId);
            assertThat(result.name()).isEqualTo("Empty Run");
            assertThat(result.rootNodes()).isEmpty();
            assertThat(result.statistics().totalNodes()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should build flat hierarchy with only root nodes")
        void shouldBuildFlatHierarchyWithOnlyRootNodes() {
            TestRun testRun = createTestRun(runId, "Flat Run", TestRun.RunStatus.PASSED);
            TestNode suite1 = createTestNode("suite1", "Suite 1", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode suite2 = createTestNode("suite2", "Suite 2", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.FAILED, null, testRun);

            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Arrays.asList(suite1, suite2));
            when(testStepRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());

            TestRunHierarchyDTO result = service.getFullHierarchy(runId);

            assertThat(result.rootNodes()).hasSize(2);
            assertThat(result.statistics().totalNodes()).isEqualTo(2);
            assertThat(result.statistics().passedNodes()).isEqualTo(1);
            assertThat(result.statistics().failedNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should build deep hierarchy with multiple levels")
        void shouldBuildDeepHierarchyWithMultipleLevels() {
            TestRun testRun = createTestRun(runId, "Deep Run", TestRun.RunStatus.PASSED);
            TestNode suite = createTestNode("suite", "Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode scenario1 = createTestNode("scenario1", "Scenario 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, suite, testRun);
            TestNode scenario2 = createTestNode("scenario2", "Scenario 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, suite, testRun);

            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Arrays.asList(suite, scenario1, scenario2));
            when(testStepRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());

            TestRunHierarchyDTO result = service.getFullHierarchy(runId);

            assertThat(result.rootNodes()).hasSize(1);
            HierarchyNodeDTO root = result.rootNodes().get(0);
            assertThat(root.name()).isEqualTo("Suite");
            assertThat(root.children()).hasSize(2);
            assertThat(result.statistics().totalNodes()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should include steps when option is enabled")
        void shouldIncludeStepsWhenOptionEnabled() {
            TestRun testRun = createTestRun(runId, "Run with Steps", TestRun.RunStatus.PASSED);
            TestNode scenario = createTestNode("scenario", "Scenario", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestStep step1 = createTestStep("step1", "Step 1", TestStep.StepStatus.PASSED, scenario);
            TestStep step2 = createTestStep("step2", "Step 2", TestStep.StepStatus.PASSED, scenario);

            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Collections.singletonList(scenario));
            when(testStepRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Arrays.asList(step1, step2));
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestStepIdIn(anyCollection())).thenReturn(Collections.emptyList());

            TestRunHierarchyDTO result = service.getFullHierarchy(runId, HierarchyOptions.defaults());

            assertThat(result.rootNodes().get(0).steps()).hasSize(2);
        }

        @Test
        @DisplayName("Should exclude steps when option is disabled")
        void shouldExcludeStepsWhenOptionDisabled() {
            TestRun testRun = createTestRun(runId, "Run without Steps", TestRun.RunStatus.PASSED);
            TestNode scenario = createTestNode("scenario", "Scenario", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);

            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Collections.singletonList(scenario));
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());

            HierarchyOptions options = new HierarchyOptions(-1, false, true, true, null, null);
            TestRunHierarchyDTO result = service.getFullHierarchy(runId, options);

            assertThat(result.rootNodes().get(0).steps()).isEmpty();
        }

        @Test
        @DisplayName("Should include attachments when option is enabled")
        void shouldIncludeAttachmentsWhenOptionEnabled() {
            TestRun testRun = createTestRun(runId, "Run with Attachments", TestRun.RunStatus.PASSED);
            TestNode scenario = createTestNode("scenario", "Scenario", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            Attachment attachment = createAttachment("att1", "screenshot.png",
                    Attachment.AttachmentType.SCREENSHOT, scenario, null);

            when(testRunRepository.findById(runId)).thenReturn(Optional.of(testRun));
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Collections.singletonList(scenario));
            when(testStepRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.singletonList(attachment));

            TestRunHierarchyDTO result = service.getFullHierarchy(runId, HierarchyOptions.defaults());

            assertThat(result.rootNodes().get(0).attachments()).hasSize(1);
            assertThat(result.rootNodes().get(0).attachments().get(0).name()).isEqualTo("screenshot.png");
        }
    }

    @Nested
    @DisplayName("getNode tests")
    class GetNodeTests {

        @Test
        @DisplayName("Should throw TestNodeNotFoundException when node does not exist")
        void shouldThrowExceptionWhenNodeNotFound() {
            String nodeId = "non-existent";
            when(testNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getNode(nodeId))
                    .isInstanceOf(TestNodeNotFoundException.class)
                    .hasMessageContaining(nodeId);
        }

        @Test
        @DisplayName("Should return node with immediate children")
        void shouldReturnNodeWithImmediateChildren() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode suite = createTestNode("suite", "Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode scenario = createTestNode("scenario", "Scenario", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, suite, testRun);

            when(testNodeRepository.findById("suite")).thenReturn(Optional.of(suite));
            when(testNodeRepository.findByParentId("suite")).thenReturn(Collections.singletonList(scenario));
            when(testStepRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());
            when(attachmentRepository.findByTestNodeIdIn(anyCollection())).thenReturn(Collections.emptyList());

            HierarchyNodeDTO result = service.getNode("suite");

            assertThat(result.id()).isEqualTo("suite");
            assertThat(result.name()).isEqualTo("Suite");
            assertThat(result.children()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getChildren tests")
    class GetChildrenTests {

        @Test
        @DisplayName("Should throw TestNodeNotFoundException when node does not exist")
        void shouldThrowExceptionWhenNodeNotFound() {
            String nodeId = "non-existent";
            when(testNodeRepository.existsById(nodeId)).thenReturn(false);

            assertThatThrownBy(() -> service.getChildren(nodeId))
                    .isInstanceOf(TestNodeNotFoundException.class);
        }

        @Test
        @DisplayName("Should return immediate children of a node")
        void shouldReturnImmediateChildren() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode suite = createTestNode("suite", "Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode scenario1 = createTestNode("scenario1", "Scenario 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, suite, testRun);
            TestNode scenario2 = createTestNode("scenario2", "Scenario 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, suite, testRun);

            when(testNodeRepository.existsById("suite")).thenReturn(true);
            when(testNodeRepository.findByParentId("suite")).thenReturn(Arrays.asList(scenario1, scenario2));

            List<HierarchyNodeDTO> result = service.getChildren("suite");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Scenario 1");
            assertThat(result.get(1).name()).isEqualTo("Scenario 2");
        }

        @Test
        @DisplayName("Should return empty list when node has no children")
        void shouldReturnEmptyListWhenNoChildren() {
            when(testNodeRepository.existsById("leaf")).thenReturn(true);
            when(testNodeRepository.findByParentId("leaf")).thenReturn(Collections.emptyList());

            List<HierarchyNodeDTO> result = service.getChildren("leaf");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAncestors tests")
    class GetAncestorsTests {

        @Test
        @DisplayName("Should throw TestNodeNotFoundException when node does not exist")
        void shouldThrowExceptionWhenNodeNotFound() {
            String nodeId = "non-existent";
            when(testNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAncestors(nodeId))
                    .isInstanceOf(TestNodeNotFoundException.class);
        }

        @Test
        @DisplayName("Should return single element path for root node")
        void shouldReturnSingleElementPathForRootNode() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode root = createTestNode("root", "Root Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);

            when(testNodeRepository.findById("root")).thenReturn(Optional.of(root));

            NodePathDTO result = service.getAncestors("root");

            assertThat(result.nodeId()).isEqualTo("root");
            assertThat(result.path()).hasSize(1);
            assertThat(result.path().get(0).name()).isEqualTo("Root Suite");
        }

        @Test
        @DisplayName("Should return correct ancestor path from root to node")
        void shouldReturnCorrectAncestorPath() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode root = createTestNode("root", "Root", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode child = createTestNode("child", "Child", TestNode.NodeType.FEATURE,
                    TestNode.NodeStatus.PASSED, root, testRun);
            TestNode grandchild = createTestNode("grandchild", "Grandchild", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, child, testRun);

            when(testNodeRepository.findById("grandchild")).thenReturn(Optional.of(grandchild));

            NodePathDTO result = service.getAncestors("grandchild");

            assertThat(result.nodeId()).isEqualTo("grandchild");
            assertThat(result.path()).hasSize(3);
            assertThat(result.path().get(0).name()).isEqualTo("Root");
            assertThat(result.path().get(1).name()).isEqualTo("Child");
            assertThat(result.path().get(2).name()).isEqualTo("Grandchild");
        }
    }

    @Nested
    @DisplayName("getSiblings tests")
    class GetSiblingsTests {

        @Test
        @DisplayName("Should throw TestNodeNotFoundException when node does not exist")
        void shouldThrowExceptionWhenNodeNotFound() {
            String nodeId = "non-existent";
            when(testNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSiblings(nodeId))
                    .isInstanceOf(TestNodeNotFoundException.class);
        }

        @Test
        @DisplayName("Should return siblings excluding the current node")
        void shouldReturnSiblingsExcludingSelf() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode parent = createTestNode("parent", "Parent", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode sibling1 = createTestNode("sibling1", "Sibling 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, parent, testRun);
            TestNode sibling2 = createTestNode("sibling2", "Sibling 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, parent, testRun);
            TestNode sibling3 = createTestNode("sibling3", "Sibling 3", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, parent, testRun);

            when(testNodeRepository.findById("sibling2")).thenReturn(Optional.of(sibling2));
            when(testNodeRepository.findByParentId("parent")).thenReturn(Arrays.asList(sibling1, sibling2, sibling3));

            List<HierarchyNodeDTO> result = service.getSiblings("sibling2");

            assertThat(result).hasSize(2);
            assertThat(result).extracting(HierarchyNodeDTO::id)
                    .containsExactlyInAnyOrder("sibling1", "sibling3");
        }

        @Test
        @DisplayName("Should return empty list when no siblings")
        void shouldReturnEmptyListWhenNoSiblings() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode parent = createTestNode("parent", "Parent", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode onlyChild = createTestNode("only", "Only Child", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, parent, testRun);

            when(testNodeRepository.findById("only")).thenReturn(Optional.of(onlyChild));
            when(testNodeRepository.findByParentId("parent")).thenReturn(Collections.singletonList(onlyChild));

            List<HierarchyNodeDTO> result = service.getSiblings("only");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return root-level siblings for root nodes")
        void shouldReturnRootLevelSiblingsForRootNodes() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode root1 = createTestNode("root1", "Root 1", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode root2 = createTestNode("root2", "Root 2", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);

            when(testNodeRepository.findById("root1")).thenReturn(Optional.of(root1));
            when(testNodeRepository.findByTestRunIdAndParentIsNull(runId)).thenReturn(Arrays.asList(root1, root2));

            List<HierarchyNodeDTO> result = service.getSiblings("root1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("root2");
        }
    }

    @Nested
    @DisplayName("getRootNodes tests")
    class GetRootNodesTests {

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run does not exist")
        void shouldThrowExceptionWhenRunNotFound() {
            when(testRunRepository.existsById(runId)).thenReturn(false);

            assertThatThrownBy(() -> service.getRootNodes(runId))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should return root nodes of a run")
        void shouldReturnRootNodes() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode root1 = createTestNode("root1", "Root 1", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode root2 = createTestNode("root2", "Root 2", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndParentIsNull(runId)).thenReturn(Arrays.asList(root1, root2));

            List<HierarchyNodeDTO> result = service.getRootNodes(runId);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getNodesByStatus tests")
    class GetNodesByStatusTests {

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run does not exist")
        void shouldThrowExceptionWhenRunNotFound() {
            when(testRunRepository.existsById(runId)).thenReturn(false);

            assertThatThrownBy(() -> service.getNodesByStatus(runId, TestNode.NodeStatus.FAILED))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should return nodes with matching status")
        void shouldReturnNodesWithMatchingStatus() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.FAILED);
            TestNode passed = createTestNode("passed", "Passed", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode failed1 = createTestNode("failed1", "Failed 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, null, testRun);
            TestNode failed2 = createTestNode("failed2", "Failed 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, null, testRun);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndStatus(runId, TestNode.NodeStatus.FAILED))
                    .thenReturn(Arrays.asList(failed1, failed2));

            List<HierarchyNodeDTO> result = service.getNodesByStatus(runId, TestNode.NodeStatus.FAILED);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(node -> node.status() == TestNode.NodeStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("getNodesByType tests")
    class GetNodesByTypeTests {

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run does not exist")
        void shouldThrowExceptionWhenRunNotFound() {
            when(testRunRepository.existsById(runId)).thenReturn(false);

            assertThatThrownBy(() -> service.getNodesByType(runId, TestNode.NodeType.SCENARIO))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should return nodes with matching type")
        void shouldReturnNodesWithMatchingType() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode suite = createTestNode("suite", "Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode scenario1 = createTestNode("scenario1", "Scenario 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode scenario2 = createTestNode("scenario2", "Scenario 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndNodeType(runId, TestNode.NodeType.SCENARIO))
                    .thenReturn(Arrays.asList(scenario1, scenario2));

            List<HierarchyNodeDTO> result = service.getNodesByType(runId, TestNode.NodeType.SCENARIO);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(node -> node.nodeType() == TestNode.NodeType.SCENARIO);
        }
    }

    @Nested
    @DisplayName("getFailedNodesWithPaths tests")
    class GetFailedNodesWithPathsTests {

        @Test
        @DisplayName("Should throw TestRunNotFoundException when run does not exist")
        void shouldThrowExceptionWhenRunNotFound() {
            when(testRunRepository.existsById(runId)).thenReturn(false);

            assertThatThrownBy(() -> service.getFailedNodesWithPaths(runId))
                    .isInstanceOf(TestRunNotFoundException.class);
        }

        @Test
        @DisplayName("Should return failed nodes with their paths")
        void shouldReturnFailedNodesWithPaths() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.FAILED);
            TestNode suite = createTestNode("suite", "Suite", TestNode.NodeType.SUITE,
                    TestNode.NodeStatus.FAILED, null, testRun);
            TestNode failed = createTestNode("failed", "Failed Test", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, suite, testRun);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunIdAndStatus(runId, TestNode.NodeStatus.FAILED))
                    .thenReturn(Collections.singletonList(failed));
            when(testNodeRepository.findById("failed")).thenReturn(Optional.of(failed));

            List<NodePathDTO> result = service.getFailedNodesWithPaths(runId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nodeId()).isEqualTo("failed");
            assertThat(result.get(0).path()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Statistics tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate correct statistics for run")
        void shouldCalculateCorrectStatisticsForRun() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.FAILED);
            TestNode passed1 = createTestNode("passed1", "Passed 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode passed2 = createTestNode("passed2", "Passed 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            TestNode failed = createTestNode("failed", "Failed", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.FAILED, null, testRun);
            TestNode skipped = createTestNode("skipped", "Skipped", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.SKIPPED, null, testRun);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunId(runId))
                    .thenReturn(Arrays.asList(passed1, passed2, failed, skipped));

            NodeStatistics result = service.calculateRunStatistics(runId);

            assertThat(result.totalNodes()).isEqualTo(4);
            assertThat(result.passedNodes()).isEqualTo(2);
            assertThat(result.failedNodes()).isEqualTo(1);
            assertThat(result.skippedNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should aggregate duration in statistics")
        void shouldAggregateDurationInStatistics() {
            TestRun testRun = createTestRun(runId, "Run", TestRun.RunStatus.PASSED);
            TestNode node1 = createTestNode("node1", "Node 1", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            node1.setDuration(1000L);
            TestNode node2 = createTestNode("node2", "Node 2", TestNode.NodeType.SCENARIO,
                    TestNode.NodeStatus.PASSED, null, testRun);
            node2.setDuration(2000L);

            when(testRunRepository.existsById(runId)).thenReturn(true);
            when(testNodeRepository.findByTestRunId(runId)).thenReturn(Arrays.asList(node1, node2));

            NodeStatistics result = service.calculateRunStatistics(runId);

            assertThat(result.totalDuration()).isEqualTo(3000L);
        }
    }

    // Helper methods for creating test entities

    private TestRun createTestRun(String id, String name, TestRun.RunStatus status) {
        return TestRun.builder()
                .id(id)
                .name(name)
                .status(status)
                .startTime(timestamp)
                .build();
    }

    private TestNode createTestNode(String id, String name, TestNode.NodeType nodeType,
                                    TestNode.NodeStatus status, TestNode parent, TestRun testRun) {
        return TestNode.builder()
                .id(id)
                .name(name)
                .nodeType(nodeType)
                .status(status)
                .parent(parent)
                .testRun(testRun)
                .startTime(timestamp)
                .build();
    }

    private TestStep createTestStep(String id, String name, TestStep.StepStatus status, TestNode testNode) {
        return TestStep.builder()
                .id(id)
                .name(name)
                .status(status)
                .testNode(testNode)
                .timestamp(timestamp)
                .build();
    }

    private Attachment createAttachment(String id, String name, Attachment.AttachmentType type,
                                        TestNode testNode, TestStep testStep) {
        return Attachment.builder()
                .id(id)
                .name(name)
                .type(type)
                .testNode(testNode)
                .testStep(testStep)
                .filePath("/path/to/" + name)
                .timestamp(timestamp)
                .build();
    }
}

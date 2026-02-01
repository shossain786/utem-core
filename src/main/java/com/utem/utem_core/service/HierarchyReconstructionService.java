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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for reconstructing and navigating test execution hierarchies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyReconstructionService {

    private final TestRunRepository testRunRepository;
    private final TestNodeRepository testNodeRepository;
    private final TestStepRepository testStepRepository;
    private final AttachmentRepository attachmentRepository;

    /**
     * Reconstructs the complete hierarchy for a test run with default options.
     */
    @Transactional(readOnly = true)
    public TestRunHierarchyDTO getFullHierarchy(String runId) {
        return getFullHierarchy(runId, HierarchyOptions.defaults());
    }

    /**
     * Reconstructs the complete hierarchy for a test run with custom options.
     */
    @Transactional(readOnly = true)
    public TestRunHierarchyDTO getFullHierarchy(String runId, HierarchyOptions options) {
        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new TestRunNotFoundException(runId));

        List<TestNode> allNodes = testNodeRepository.findByTestRunId(runId);

        if (allNodes.isEmpty()) {
            return TestRunHierarchyDTO.from(testRun, Collections.emptyList(), NodeStatistics.empty());
        }

        Map<String, List<TestNode>> childrenMap = groupNodesByParentId(allNodes);
        List<TestNode> rootNodes = allNodes.stream()
                .filter(node -> node.getParent() == null)
                .toList();

        Map<String, List<TestStep>> stepsMap = Collections.emptyMap();
        Map<String, List<Attachment>> nodeAttachmentsMap = Collections.emptyMap();
        Map<String, List<Attachment>> stepAttachmentsMap = Collections.emptyMap();

        Set<String> allNodeIds = allNodes.stream()
                .map(TestNode::getId)
                .collect(Collectors.toSet());

        if (options.includeSteps()) {
            List<TestStep> allSteps = testStepRepository.findByTestNodeIdIn(allNodeIds);
            stepsMap = allSteps.stream()
                    .collect(Collectors.groupingBy(step -> step.getTestNode().getId()));

            if (options.includeAttachments()) {
                Set<String> allStepIds = allSteps.stream()
                        .map(TestStep::getId)
                        .collect(Collectors.toSet());
                if (!allStepIds.isEmpty()) {
                    stepAttachmentsMap = attachmentRepository.findByTestStepIdIn(allStepIds).stream()
                            .collect(Collectors.groupingBy(att -> att.getTestStep().getId()));
                }
            }
        }

        if (options.includeAttachments()) {
            nodeAttachmentsMap = attachmentRepository.findByTestNodeIdIn(allNodeIds).stream()
                    .collect(Collectors.groupingBy(att -> att.getTestNode().getId()));
        }

        List<HierarchyNodeDTO> rootDTOs = new ArrayList<>();
        NodeStatistics totalStats = NodeStatistics.empty();

        for (TestNode root : rootNodes) {
            HierarchyNodeDTO rootDTO = buildNodeTree(
                    root, childrenMap, stepsMap, nodeAttachmentsMap, stepAttachmentsMap,
                    0, options
            );
            rootDTOs.add(rootDTO);
            if (options.calculateStats() && rootDTO.statistics() != null) {
                totalStats = totalStats.merge(rootDTO.statistics());
            }
        }

        return TestRunHierarchyDTO.from(testRun, rootDTOs, totalStats);
    }

    /**
     * Gets a single node with its immediate children, steps, and attachments.
     */
    @Transactional(readOnly = true)
    public HierarchyNodeDTO getNode(String nodeId) {
        return getNodeWithSubtree(nodeId, 1);
    }

    /**
     * Gets a single node with its subtree to specified depth.
     */
    @Transactional(readOnly = true)
    public HierarchyNodeDTO getNodeWithSubtree(String nodeId, int maxDepth) {
        TestNode node = testNodeRepository.findById(nodeId)
                .orElseThrow(() -> new TestNodeNotFoundException(nodeId));

        HierarchyOptions options = new HierarchyOptions(
                maxDepth, true, true, true, null, null
        );

        List<TestNode> subtreeNodes = collectSubtreeNodes(node, maxDepth);
        Set<String> nodeIds = subtreeNodes.stream()
                .map(TestNode::getId)
                .collect(Collectors.toSet());

        Map<String, List<TestNode>> childrenMap = groupNodesByParentId(subtreeNodes);
        Map<String, List<TestStep>> stepsMap = testStepRepository.findByTestNodeIdIn(nodeIds).stream()
                .collect(Collectors.groupingBy(step -> step.getTestNode().getId()));
        Map<String, List<Attachment>> nodeAttachmentsMap = attachmentRepository.findByTestNodeIdIn(nodeIds).stream()
                .collect(Collectors.groupingBy(att -> att.getTestNode().getId()));

        Set<String> stepIds = stepsMap.values().stream()
                .flatMap(List::stream)
                .map(TestStep::getId)
                .collect(Collectors.toSet());
        Map<String, List<Attachment>> stepAttachmentsMap = stepIds.isEmpty()
                ? Collections.emptyMap()
                : attachmentRepository.findByTestStepIdIn(stepIds).stream()
                        .collect(Collectors.groupingBy(att -> att.getTestStep().getId()));

        return buildNodeTree(node, childrenMap, stepsMap, nodeAttachmentsMap, stepAttachmentsMap, 0, options);
    }

    /**
     * Gets immediate children of a node (single level).
     */
    @Transactional(readOnly = true)
    public List<HierarchyNodeDTO> getChildren(String nodeId) {
        if (!testNodeRepository.existsById(nodeId)) {
            throw new TestNodeNotFoundException(nodeId);
        }

        List<TestNode> children = testNodeRepository.findByParentId(nodeId);
        return children.stream()
                .map(HierarchyNodeDTO::from)
                .toList();
    }

    /**
     * Gets the path from root to specified node (breadcrumb trail).
     */
    @Transactional(readOnly = true)
    public NodePathDTO getAncestors(String nodeId) {
        TestNode node = testNodeRepository.findById(nodeId)
                .orElseThrow(() -> new TestNodeNotFoundException(nodeId));

        List<BreadcrumbDTO> path = new ArrayList<>();
        TestNode current = node;

        while (current != null) {
            path.add(BreadcrumbDTO.from(current));
            current = current.getParent();
        }

        Collections.reverse(path);
        return NodePathDTO.of(nodeId, path);
    }

    /**
     * Gets sibling nodes (same parent, excluding the specified node).
     */
    @Transactional(readOnly = true)
    public List<HierarchyNodeDTO> getSiblings(String nodeId) {
        TestNode node = testNodeRepository.findById(nodeId)
                .orElseThrow(() -> new TestNodeNotFoundException(nodeId));

        List<TestNode> siblings;
        if (node.getParent() == null) {
            siblings = testNodeRepository.findByTestRunIdAndParentIsNull(node.getTestRun().getId());
        } else {
            siblings = testNodeRepository.findByParentId(node.getParent().getId());
        }

        return siblings.stream()
                .filter(sibling -> !sibling.getId().equals(nodeId))
                .map(HierarchyNodeDTO::from)
                .toList();
    }

    /**
     * Gets the root nodes for a test run (nodes with no parent).
     */
    @Transactional(readOnly = true)
    public List<HierarchyNodeDTO> getRootNodes(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> rootNodes = testNodeRepository.findByTestRunIdAndParentIsNull(runId);
        return rootNodes.stream()
                .map(HierarchyNodeDTO::from)
                .toList();
    }

    /**
     * Gets nodes matching a specific status within a run.
     */
    @Transactional(readOnly = true)
    public List<HierarchyNodeDTO> getNodesByStatus(String runId, TestNode.NodeStatus status) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> nodes = testNodeRepository.findByTestRunIdAndStatus(runId, status);
        return nodes.stream()
                .map(HierarchyNodeDTO::from)
                .toList();
    }

    /**
     * Gets nodes matching a specific type within a run.
     */
    @Transactional(readOnly = true)
    public List<HierarchyNodeDTO> getNodesByType(String runId, TestNode.NodeType nodeType) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> nodes = testNodeRepository.findByTestRunIdAndNodeType(runId, nodeType);
        return nodes.stream()
                .map(HierarchyNodeDTO::from)
                .toList();
    }

    /**
     * Gets all failed nodes with their ancestor paths.
     */
    @Transactional(readOnly = true)
    public List<NodePathDTO> getFailedNodesWithPaths(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> failedNodes = testNodeRepository.findByTestRunIdAndStatus(
                runId, TestNode.NodeStatus.FAILED
        );

        return failedNodes.stream()
                .map(node -> getAncestors(node.getId()))
                .toList();
    }

    /**
     * Calculates statistics for a specific node and its subtree.
     */
    @Transactional(readOnly = true)
    public NodeStatistics calculateSubtreeStatistics(String nodeId) {
        TestNode node = testNodeRepository.findById(nodeId)
                .orElseThrow(() -> new TestNodeNotFoundException(nodeId));

        List<TestNode> subtreeNodes = collectSubtreeNodes(node, -1);
        return aggregateStatistics(subtreeNodes);
    }

    /**
     * Calculates overall statistics for a test run.
     */
    @Transactional(readOnly = true)
    public NodeStatistics calculateRunStatistics(String runId) {
        if (!testRunRepository.existsById(runId)) {
            throw new TestRunNotFoundException(runId);
        }

        List<TestNode> allNodes = testNodeRepository.findByTestRunId(runId);
        return aggregateStatistics(allNodes);
    }

    // ============ Internal Helper Methods ============

    private Map<String, List<TestNode>> groupNodesByParentId(List<TestNode> nodes) {
        Map<String, List<TestNode>> childrenMap = new HashMap<>();
        for (TestNode node : nodes) {
            String parentId = node.getParent() != null ? node.getParent().getId() : null;
            if (parentId != null) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node);
            }
        }
        return childrenMap;
    }

    private HierarchyNodeDTO buildNodeTree(
            TestNode node,
            Map<String, List<TestNode>> childrenMap,
            Map<String, List<TestStep>> stepsMap,
            Map<String, List<Attachment>> nodeAttachmentsMap,
            Map<String, List<Attachment>> stepAttachmentsMap,
            int currentDepth,
            HierarchyOptions options
    ) {
        List<HierarchyNodeDTO> childDTOs = new ArrayList<>();
        NodeStatistics stats = NodeStatistics.single(
                node.getStatus().name(),
                node.getDuration()
        );

        if (!options.isDepthExceeded(currentDepth)) {
            List<TestNode> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
            for (TestNode child : children) {
                HierarchyNodeDTO childDTO = buildNodeTree(
                        child, childrenMap, stepsMap, nodeAttachmentsMap, stepAttachmentsMap,
                        currentDepth + 1, options
                );
                childDTOs.add(childDTO);
                if (options.calculateStats() && childDTO.statistics() != null) {
                    stats = stats.merge(childDTO.statistics());
                }
            }
        }

        List<TestStepDTO> stepDTOs = new ArrayList<>();
        if (options.includeSteps()) {
            List<TestStep> steps = stepsMap.getOrDefault(node.getId(), Collections.emptyList());
            for (TestStep step : steps) {
                List<AttachmentSummaryDTO> stepAttachments = new ArrayList<>();
                if (options.includeAttachments()) {
                    stepAttachments = stepAttachmentsMap.getOrDefault(step.getId(), Collections.emptyList())
                            .stream()
                            .map(AttachmentSummaryDTO::from)
                            .toList();
                }
                stepDTOs.add(TestStepDTO.from(step, stepAttachments));
            }
        }

        List<AttachmentSummaryDTO> attachmentDTOs = new ArrayList<>();
        if (options.includeAttachments()) {
            attachmentDTOs = nodeAttachmentsMap.getOrDefault(node.getId(), Collections.emptyList())
                    .stream()
                    .map(AttachmentSummaryDTO::from)
                    .toList();
        }

        return HierarchyNodeDTO.from(
                node,
                options.calculateStats() ? stats : null,
                childDTOs,
                stepDTOs,
                attachmentDTOs
        );
    }

    private List<TestNode> collectSubtreeNodes(TestNode root, int maxDepth) {
        List<TestNode> result = new ArrayList<>();
        collectSubtreeNodesRecursive(root, result, 0, maxDepth);
        return result;
    }

    private void collectSubtreeNodesRecursive(TestNode node, List<TestNode> result, int currentDepth, int maxDepth) {
        result.add(node);
        if (maxDepth >= 0 && currentDepth >= maxDepth) {
            return;
        }
        List<TestNode> children = testNodeRepository.findByParentId(node.getId());
        for (TestNode child : children) {
            collectSubtreeNodesRecursive(child, result, currentDepth + 1, maxDepth);
        }
    }

    private NodeStatistics aggregateStatistics(List<TestNode> nodes) {
        NodeStatistics stats = NodeStatistics.empty();
        for (TestNode node : nodes) {
            stats = stats.merge(NodeStatistics.single(
                    node.getStatus().name(),
                    node.getDuration()
            ));
        }
        return stats;
    }
}

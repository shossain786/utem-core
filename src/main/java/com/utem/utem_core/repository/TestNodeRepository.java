package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TestNodeRepository extends JpaRepository<TestNode, String> {

    List<TestNode> findByTestRun(TestRun testRun);

    List<TestNode> findByTestRunId(String runId);

    List<TestNode> findByParent(TestNode parent);

    List<TestNode> findByParentId(String parentId);

    List<TestNode> findByTestRunIdAndParentIsNull(String runId);

    List<TestNode> findByTestRunIdAndNodeType(String runId, TestNode.NodeType nodeType);

    List<TestNode> findByTestRunIdAndStatus(String runId, TestNode.NodeStatus status);

    List<TestNode> findByFlakyTrue();

    List<TestNode> findByNameContainingIgnoreCase(String name);

    long countByTestRunIdAndStatus(String runId, TestNode.NodeStatus status);

    long countByTestRunIdAndNodeType(String runId, TestNode.NodeType nodeType);

    List<TestNode> findByNameAndNodeTypeOrderByTestRunStartTimeDesc(String name, TestNode.NodeType nodeType);

    @Query("SELECT DISTINCT n.name FROM TestNode n WHERE n.flaky = true")
    List<String> findDistinctFlakyTestNames();

    List<TestNode> findByTestRunIdAndNodeTypeIn(String runId, Collection<TestNode.NodeType> nodeTypes);

    Page<TestNode> findByNameContainingIgnoreCaseOrderByStartTimeDesc(String name, Pageable pageable);

    Page<TestNode> findByStatusOrderByStartTimeDesc(TestNode.NodeStatus status, Pageable pageable);

    Page<TestNode> findByNodeTypeOrderByStartTimeDesc(TestNode.NodeType nodeType, Pageable pageable);

    Page<TestNode> findByTestRunIdAndNameContainingIgnoreCaseOrderByStartTimeDesc(
            String runId, String name, Pageable pageable);
}

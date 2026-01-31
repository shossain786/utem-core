package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

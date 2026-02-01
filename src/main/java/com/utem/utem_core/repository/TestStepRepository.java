package com.utem.utem_core.repository;

import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, String> {

    List<TestStep> findByTestNode(TestNode testNode);

    List<TestStep> findByTestNodeId(String nodeId);

    List<TestStep> findByTestNodeIdOrderByStepOrderAsc(String nodeId);

    List<TestStep> findByTestNodeIdAndStatus(String nodeId, TestStep.StepStatus status);

    List<TestStep> findByStatus(TestStep.StepStatus status);

    @Query("SELECT s FROM TestStep s WHERE s.testNode.id IN :nodeIds ORDER BY s.testNode.id, s.stepOrder ASC")
    List<TestStep> findByTestNodeIdIn(@Param("nodeIds") Collection<String> nodeIds);
}

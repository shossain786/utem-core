package com.utem.utem_core.repository;

import com.utem.utem_core.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    List<Attachment> findByTestNodeId(String nodeId);

    List<Attachment> findByTestStepId(String stepId);

    List<Attachment> findByType(Attachment.AttachmentType type);

    List<Attachment> findByIsFailureScreenshotTrue();

    List<Attachment> findByTestNodeIdAndType(String nodeId, Attachment.AttachmentType type);

    @Query("SELECT a FROM Attachment a WHERE a.testNode.id IN :nodeIds")
    List<Attachment> findByTestNodeIdIn(@Param("nodeIds") Collection<String> nodeIds);

    @Query("SELECT a FROM Attachment a WHERE a.testStep.id IN :stepIds")
    List<Attachment> findByTestStepIdIn(@Param("stepIds") Collection<String> stepIds);
}

package com.utem.utem_core.repository;

import com.utem.utem_core.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    List<Attachment> findByTestNodeId(String nodeId);

    List<Attachment> findByTestStepId(String stepId);

    List<Attachment> findByType(Attachment.AttachmentType type);

    List<Attachment> findByIsFailureScreenshotTrue();

    List<Attachment> findByTestNodeIdAndType(String nodeId, Attachment.AttachmentType type);
}

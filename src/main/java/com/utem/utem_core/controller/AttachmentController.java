package com.utem.utem_core.controller;

import com.utem.utem_core.dto.AttachmentSummaryDTO;
import com.utem.utem_core.dto.FileMetadata;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.exception.AttachmentNotFoundException;
import com.utem.utem_core.repository.AttachmentRepository;
import com.utem.utem_core.service.AttachmentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for attachment file operations.
 */
@RestController
@RequestMapping("/utem/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentStorageService attachmentStorageService;
    private final AttachmentRepository attachmentRepository;

    /**
     * Upload a file for an existing attachment record.
     */
    @PostMapping("/{attachmentId}/upload")
    public ResponseEntity<AttachmentSummaryDTO> uploadFile(
            @PathVariable String attachmentId,
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Uploading file for attachment {}: {} ({} bytes)",
                attachmentId, file.getOriginalFilename(), file.getSize());

        Attachment attachment = attachmentStorageService.storeFile(
                attachmentId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize()
        );

        return ResponseEntity.ok(AttachmentSummaryDTO.from(attachment));
    }

    /**
     * Download the file for an attachment.
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        Resource resource = attachmentStorageService.loadFile(attachmentId);

        String contentType = attachment.getMimeType() != null
                ? attachment.getMimeType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getName() + "\"")
                .body(resource);
    }

    /**
     * Delete the file for an attachment.
     */
    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable String attachmentId) {
        log.info("Deleting file for attachment {}", attachmentId);
        attachmentStorageService.deleteFile(attachmentId);
    }

    /**
     * Get file metadata for an attachment.
     */
    @GetMapping("/{attachmentId}/metadata")
    public ResponseEntity<FileMetadata> getMetadata(@PathVariable String attachmentId) {
        FileMetadata metadata = attachmentStorageService.getFileMetadata(attachmentId);
        return ResponseEntity.ok(metadata);
    }

    /**
     * List attachments for a test node.
     */
    @GetMapping("/node/{nodeId}")
    public ResponseEntity<List<AttachmentSummaryDTO>> getAttachmentsByNode(@PathVariable String nodeId) {
        List<AttachmentSummaryDTO> attachments = attachmentRepository.findByTestNodeId(nodeId)
                .stream()
                .map(AttachmentSummaryDTO::from)
                .toList();
        return ResponseEntity.ok(attachments);
    }

    /**
     * List attachments for a test step.
     */
    @GetMapping("/step/{stepId}")
    public ResponseEntity<List<AttachmentSummaryDTO>> getAttachmentsByStep(@PathVariable String stepId) {
        List<AttachmentSummaryDTO> attachments = attachmentRepository.findByTestStepId(stepId)
                .stream()
                .map(AttachmentSummaryDTO::from)
                .toList();
        return ResponseEntity.ok(attachments);
    }
}

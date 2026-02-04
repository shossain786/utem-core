package com.utem.utem_core.controller;

import com.utem.utem_core.dto.AttachmentSummaryDTO;
import com.utem.utem_core.dto.FileMetadata;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.exception.AttachmentNotFoundException;
import com.utem.utem_core.exception.FileStorageException;
import com.utem.utem_core.exception.FileValidationException;
import com.utem.utem_core.repository.AttachmentRepository;
import com.utem.utem_core.service.AttachmentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentStorageService attachmentStorageService;

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private AttachmentController attachmentController;

    private String attachmentId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID().toString();
        timestamp = Instant.now();
    }

    private Attachment createAttachment(String id, String name, Attachment.AttachmentType type) {
        return Attachment.builder()
                .id(id)
                .name(name)
                .type(type)
                .filePath("/test/path/" + name)
                .mimeType("image/png")
                .fileSize(1024L)
                .timestamp(timestamp)
                .isFailureScreenshot(false)
                .build();
    }

    @Nested
    @DisplayName("Upload file tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload file successfully")
        void shouldUploadFileSuccessfully() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "screenshot.png", "image/png", "test content".getBytes());

            Attachment attachment = createAttachment(attachmentId, "screenshot.png", Attachment.AttachmentType.SCREENSHOT);

            when(attachmentStorageService.storeFile(
                    eq(attachmentId), any(InputStream.class), eq("screenshot.png"), eq(12L)))
                    .thenReturn(attachment);

            ResponseEntity<AttachmentSummaryDTO> response = attachmentController.uploadFile(attachmentId, file);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isEqualTo(attachmentId);
            assertThat(response.getBody().name()).isEqualTo("screenshot.png");
            assertThat(response.getBody().type()).isEqualTo(Attachment.AttachmentType.SCREENSHOT);
            assertThat(response.getBody().mimeType()).isEqualTo("image/png");
            assertThat(response.getBody().fileSize()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("Should propagate AttachmentNotFoundException on upload")
        void shouldPropagateNotFoundOnUpload() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", "content".getBytes());

            when(attachmentStorageService.storeFile(
                    eq(attachmentId), any(InputStream.class), anyString(), anyLong()))
                    .thenThrow(new AttachmentNotFoundException(attachmentId));

            assertThatThrownBy(() -> attachmentController.uploadFile(attachmentId, file))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }

        @Test
        @DisplayName("Should propagate FileValidationException on upload")
        void shouldPropagateValidationErrorOnUpload() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "huge.bin", "application/octet-stream", "content".getBytes());

            when(attachmentStorageService.storeFile(
                    eq(attachmentId), any(InputStream.class), anyString(), anyLong()))
                    .thenThrow(new FileValidationException("SIZE", "File too large"));

            assertThatThrownBy(() -> attachmentController.uploadFile(attachmentId, file))
                    .isInstanceOf(FileValidationException.class);
        }
    }

    @Nested
    @DisplayName("Download file tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download file successfully with correct headers")
        void shouldDownloadFileSuccessfully() {
            Attachment attachment = createAttachment(attachmentId, "screenshot.png", Attachment.AttachmentType.SCREENSHOT);
            Resource resource = new ByteArrayResource("file content".getBytes());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentStorageService.loadFile(attachmentId)).thenReturn(resource);

            ResponseEntity<Resource> response = attachmentController.downloadFile(attachmentId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
            assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .contains("screenshot.png");
        }

        @Test
        @DisplayName("Should throw AttachmentNotFoundException when attachment not found")
        void shouldThrowWhenAttachmentNotFound() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attachmentController.downloadFile(attachmentId))
                    .isInstanceOf(AttachmentNotFoundException.class)
                    .hasMessageContaining(attachmentId);
        }

        @Test
        @DisplayName("Should use octet-stream when MIME type is null")
        void shouldUseDefaultMimeTypeWhenNull() {
            Attachment attachment = createAttachment(attachmentId, "data.bin", Attachment.AttachmentType.FILE);
            attachment.setMimeType(null);
            Resource resource = new ByteArrayResource("binary data".getBytes());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentStorageService.loadFile(attachmentId)).thenReturn(resource);

            ResponseEntity<Resource> response = attachmentController.downloadFile(attachmentId);

            assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Should propagate FileStorageException when file not on disk")
        void shouldPropagateStorageError() {
            Attachment attachment = createAttachment(attachmentId, "missing.png", Attachment.AttachmentType.SCREENSHOT);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentStorageService.loadFile(attachmentId))
                    .thenThrow(new FileStorageException("File not found on disk"));

            assertThatThrownBy(() -> attachmentController.downloadFile(attachmentId))
                    .isInstanceOf(FileStorageException.class);
        }
    }

    @Nested
    @DisplayName("Delete file tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() {
            doNothing().when(attachmentStorageService).deleteFile(attachmentId);

            attachmentController.deleteFile(attachmentId);

            verify(attachmentStorageService).deleteFile(attachmentId);
        }

        @Test
        @DisplayName("Should propagate AttachmentNotFoundException on delete")
        void shouldPropagateNotFoundOnDelete() {
            doThrow(new AttachmentNotFoundException(attachmentId))
                    .when(attachmentStorageService).deleteFile(attachmentId);

            assertThatThrownBy(() -> attachmentController.deleteFile(attachmentId))
                    .isInstanceOf(AttachmentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get metadata tests")
    class GetMetadataTests {

        @Test
        @DisplayName("Should return metadata successfully")
        void shouldReturnMetadata() {
            FileMetadata metadata = new FileMetadata(
                    attachmentId, "screenshot.png", "image/png", 1024L, timestamp, true);

            when(attachmentStorageService.getFileMetadata(attachmentId)).thenReturn(metadata);

            ResponseEntity<FileMetadata> response = attachmentController.getMetadata(attachmentId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().attachmentId()).isEqualTo(attachmentId);
            assertThat(response.getBody().filename()).isEqualTo("screenshot.png");
            assertThat(response.getBody().mimeType()).isEqualTo("image/png");
            assertThat(response.getBody().fileSize()).isEqualTo(1024L);
            assertThat(response.getBody().exists()).isTrue();
        }

        @Test
        @DisplayName("Should return notFound metadata when attachment does not exist")
        void shouldReturnNotFoundMetadata() {
            FileMetadata metadata = FileMetadata.notFound(attachmentId);

            when(attachmentStorageService.getFileMetadata(attachmentId)).thenReturn(metadata);

            ResponseEntity<FileMetadata> response = attachmentController.getMetadata(attachmentId);

            assertThat(response.getBody().exists()).isFalse();
        }
    }

    @Nested
    @DisplayName("List attachments by node tests")
    class ListByNodeTests {

        @Test
        @DisplayName("Should return attachments for a node")
        void shouldReturnAttachmentsForNode() {
            String nodeId = UUID.randomUUID().toString();
            Attachment a1 = createAttachment(UUID.randomUUID().toString(), "screenshot.png", Attachment.AttachmentType.SCREENSHOT);
            Attachment a2 = createAttachment(UUID.randomUUID().toString(), "log.txt", Attachment.AttachmentType.LOG);

            when(attachmentRepository.findByTestNodeId(nodeId)).thenReturn(List.of(a1, a2));

            ResponseEntity<List<AttachmentSummaryDTO>> response = attachmentController.getAttachmentsByNode(nodeId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).name()).isEqualTo("screenshot.png");
            assertThat(response.getBody().get(1).name()).isEqualTo("log.txt");
        }

        @Test
        @DisplayName("Should return empty list when node has no attachments")
        void shouldReturnEmptyListWhenNoAttachments() {
            String nodeId = UUID.randomUUID().toString();

            when(attachmentRepository.findByTestNodeId(nodeId)).thenReturn(Collections.emptyList());

            ResponseEntity<List<AttachmentSummaryDTO>> response = attachmentController.getAttachmentsByNode(nodeId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("List attachments by step tests")
    class ListByStepTests {

        @Test
        @DisplayName("Should return attachments for a step")
        void shouldReturnAttachmentsForStep() {
            String stepId = UUID.randomUUID().toString();
            Attachment a1 = createAttachment(UUID.randomUUID().toString(), "failure.png", Attachment.AttachmentType.SCREENSHOT);

            when(attachmentRepository.findByTestStepId(stepId)).thenReturn(List.of(a1));

            ResponseEntity<List<AttachmentSummaryDTO>> response = attachmentController.getAttachmentsByStep(stepId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).name()).isEqualTo("failure.png");
        }

        @Test
        @DisplayName("Should return empty list when step has no attachments")
        void shouldReturnEmptyListWhenNoAttachments() {
            String stepId = UUID.randomUUID().toString();

            when(attachmentRepository.findByTestStepId(stepId)).thenReturn(Collections.emptyList());

            ResponseEntity<List<AttachmentSummaryDTO>> response = attachmentController.getAttachmentsByStep(stepId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }
}

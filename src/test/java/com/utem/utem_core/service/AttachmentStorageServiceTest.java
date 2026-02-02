package com.utem.utem_core.service;

import com.utem.utem_core.config.StorageProperties;
import com.utem.utem_core.dto.FileMetadata;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import com.utem.utem_core.exception.AttachmentNotFoundException;
import com.utem.utem_core.exception.FileStorageException;
import com.utem.utem_core.exception.FileValidationException;
import com.utem.utem_core.repository.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentStorageServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @TempDir
    Path tempDir;

    private StorageProperties storageProperties;
    private AttachmentStorageService service;

    private String attachmentId;
    private String runId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID().toString();
        runId = UUID.randomUUID().toString();
        timestamp = Instant.now();

        storageProperties = new StorageProperties(
                tempDir.toString(),
                10L,  // 10 MB max
                null, // Allow all MIME types
                true  // Organize by run ID
        );

        service = new AttachmentStorageService(attachmentRepository, storageProperties);
    }

    // ============ Helper Methods ============

    private TestRun createTestRun(String id, String name) {
        return TestRun.builder()
                .id(id)
                .name(name)
                .status(TestRun.RunStatus.RUNNING)
                .startTime(timestamp)
                .build();
    }

    private TestNode createTestNode(String id, String name, TestRun testRun) {
        return TestNode.builder()
                .id(id)
                .name(name)
                .testRun(testRun)
                .nodeType(TestNode.NodeType.SCENARIO)
                .status(TestNode.NodeStatus.RUNNING)
                .startTime(timestamp)
                .build();
    }

    private TestStep createTestStep(String id, String name, TestNode testNode) {
        return TestStep.builder()
                .id(id)
                .name(name)
                .testNode(testNode)
                .status(TestStep.StepStatus.RUNNING)
                .timestamp(timestamp)
                .build();
    }

    private Attachment createAttachment(String id, TestNode testNode, TestStep testStep) {
        return Attachment.builder()
                .id(id)
                .name("test-file.png")
                .type(Attachment.AttachmentType.SCREENSHOT)
                .filePath("")
                .timestamp(timestamp)
                .testNode(testNode)
                .testStep(testStep)
                .build();
    }

    // ============ Test Classes ============

    @Nested
    @DisplayName("storeFile (byte array) tests")
    class StoreFileByteArrayTests {

        @Test
        @DisplayName("Should store file successfully and update attachment record")
        void shouldStoreFileSuccessfully() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test content".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "screenshot.png");

            assertThat(result.getFilePath()).isNotNull();
            assertThat(result.getFilePath()).contains(runId);
            assertThat(result.getMimeType()).isEqualTo("image/png");
            assertThat(result.getFileSize()).isEqualTo(content.length);

            // Verify file was actually written
            Path filePath = Path.of(result.getFilePath());
            assertThat(Files.exists(filePath)).isTrue();
        }

        @Test
        @DisplayName("Should throw AttachmentNotFoundException when attachment does not exist")
        void shouldThrowWhenAttachmentNotFound() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            byte[] content = "test content".getBytes();

            assertThatThrownBy(() -> service.storeFile(attachmentId, content, "test.png"))
                    .isInstanceOf(AttachmentNotFoundException.class)
                    .hasMessageContaining(attachmentId);
        }

        @Test
        @DisplayName("Should reject file exceeding max size")
        void shouldRejectOversizedFile() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            // Create content larger than 10MB limit
            byte[] content = new byte[11 * 1024 * 1024];

            assertThatThrownBy(() -> service.storeFile(attachmentId, content, "large-file.bin"))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            byte[] content = new byte[0];

            assertThatThrownBy(() -> service.storeFile(attachmentId, content, "empty.txt"))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("Should detect MIME type from file extension")
        void shouldDetectMimeType() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "log.json");

            assertThat(result.getMimeType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("Should sanitize filename with special characters")
        void shouldSanitizeFilename() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "File With Spaces & Special!@#.txt");

            assertThat(result.getFilePath()).doesNotContain(" ");
            assertThat(result.getFilePath()).doesNotContain("@");
            assertThat(result.getFilePath()).doesNotContain("#");
            assertThat(result.getFilePath()).contains(".txt");
        }

        @Test
        @DisplayName("Should organize files by run ID")
        void shouldOrganizeByRunId() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "test.png");

            assertThat(result.getFilePath()).contains(runId);
        }

        @Test
        @DisplayName("Should resolve run ID from TestStep when TestNode is null")
        void shouldResolveRunIdFromTestStep() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            TestStep testStep = createTestStep("step-1", "Test Step", testNode);
            Attachment attachment = createAttachment(attachmentId, null, testStep);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "test.png");

            assertThat(result.getFilePath()).contains(runId);
        }

        @Test
        @DisplayName("Should use 'unassigned' folder when no run ID can be resolved")
        void shouldUseUnassignedFolderWhenNoRunId() {
            Attachment attachment = createAttachment(attachmentId, null, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();
            Attachment result = service.storeFile(attachmentId, content, "test.png");

            assertThat(result.getFilePath()).contains("unassigned");
        }
    }

    @Nested
    @DisplayName("storeFile (InputStream) tests")
    class StoreFileInputStreamTests {

        @Test
        @DisplayName("Should store file from InputStream successfully")
        void shouldStoreFileFromInputStream() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "stream content".getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

            Attachment result = service.storeFile(attachmentId, inputStream, "stream.txt", content.length);

            assertThat(result.getFilePath()).isNotNull();
            assertThat(result.getMimeType()).isEqualTo("text/plain");
            assertThat(result.getFileSize()).isEqualTo(content.length);

            // Verify file content
            Path filePath = Path.of(result.getFilePath());
            assertThat(Files.readAllBytes(filePath)).isEqualTo(content);
        }
    }

    @Nested
    @DisplayName("loadFile tests")
    class LoadFileTests {

        @Test
        @DisplayName("Should load file successfully as Resource")
        void shouldLoadFileSuccessfully() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            // Create actual file
            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Path filePath = runDir.resolve("test-file.txt");
            Files.write(filePath, "file content".getBytes());

            attachment.setFilePath(filePath.toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            Resource resource = service.loadFile(attachmentId);

            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }

        @Test
        @DisplayName("Should throw FileStorageException when file path is empty")
        void shouldThrowWhenFilePathEmpty() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);
            attachment.setFilePath("");

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> service.loadFile(attachmentId))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("No file stored");
        }

        @Test
        @DisplayName("Should throw FileStorageException when file not found on disk")
        void shouldThrowWhenFileNotOnDisk() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);
            attachment.setFilePath(tempDir.resolve("non-existent.txt").toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> service.loadFile(attachmentId))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("not found on disk");
        }
    }

    @Nested
    @DisplayName("loadFileAsBytes tests")
    class LoadFileAsBytesTests {

        @Test
        @DisplayName("Should load file as bytes successfully")
        void shouldLoadFileAsBytes() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            // Create actual file
            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Path filePath = runDir.resolve("test-file.txt");
            byte[] expectedContent = "file content bytes".getBytes();
            Files.write(filePath, expectedContent);

            attachment.setFilePath(filePath.toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            byte[] result = service.loadFileAsBytes(attachmentId);

            assertThat(result).isEqualTo(expectedContent);
        }
    }

    @Nested
    @DisplayName("deleteFile tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file and clear file path")
        void shouldDeleteFile() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            // Create actual file
            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Path filePath = runDir.resolve("to-delete.txt");
            Files.write(filePath, "delete me".getBytes());

            attachment.setFilePath(filePath.toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteFile(attachmentId);

            assertThat(Files.exists(filePath)).isFalse();

            ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
            verify(attachmentRepository).save(captor.capture());
            assertThat(captor.getValue().getFilePath()).isNull();
            assertThat(captor.getValue().getFileSize()).isNull();
        }

        @Test
        @DisplayName("Should handle delete of non-existent file gracefully")
        void shouldHandleDeleteNonExistent() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);
            attachment.setFilePath(tempDir.resolve("non-existent.txt").toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw
            assertThatCode(() -> service.deleteFile(attachmentId)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deleteFilesForRun tests")
    class DeleteFilesForRunTests {

        @Test
        @DisplayName("Should delete all files in run directory")
        void shouldDeleteFilesForRun() throws IOException {
            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Files.write(runDir.resolve("file1.txt"), "content1".getBytes());
            Files.write(runDir.resolve("file2.txt"), "content2".getBytes());

            service.deleteFilesForRun(runId);

            assertThat(Files.exists(runDir)).isFalse();
        }

        @Test
        @DisplayName("Should handle non-existent run directory gracefully")
        void shouldHandleNonExistentRunDirectory() {
            // Should not throw
            assertThatCode(() -> service.deleteFilesForRun("non-existent-run"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("fileExists tests")
    class FileExistsTests {

        @Test
        @DisplayName("Should return true when file exists")
        void shouldReturnTrueWhenFileExists() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Path filePath = runDir.resolve("exists.txt");
            Files.write(filePath, "content".getBytes());

            attachment.setFilePath(filePath.toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThat(service.fileExists(attachmentId)).isTrue();
        }

        @Test
        @DisplayName("Should return false when file does not exist")
        void shouldReturnFalseWhenFileNotExists() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);
            attachment.setFilePath(tempDir.resolve("non-existent.txt").toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThat(service.fileExists(attachmentId)).isFalse();
        }

        @Test
        @DisplayName("Should return false when attachment not found")
        void shouldReturnFalseWhenAttachmentNotFound() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            assertThat(service.fileExists(attachmentId)).isFalse();
        }
    }

    @Nested
    @DisplayName("getFileMetadata tests")
    class GetFileMetadataTests {

        @Test
        @DisplayName("Should return metadata for existing file")
        void shouldReturnMetadataForExistingFile() throws IOException {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);
            attachment.setMimeType("image/png");
            attachment.setFileSize(1024L);

            Path runDir = tempDir.resolve(runId);
            Files.createDirectories(runDir);
            Path filePath = runDir.resolve("metadata.txt");
            Files.write(filePath, "content".getBytes());

            attachment.setFilePath(filePath.toString());

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            FileMetadata metadata = service.getFileMetadata(attachmentId);

            assertThat(metadata.attachmentId()).isEqualTo(attachmentId);
            assertThat(metadata.filename()).isEqualTo("test-file.png");
            assertThat(metadata.mimeType()).isEqualTo("image/png");
            assertThat(metadata.fileSize()).isEqualTo(1024L);
            assertThat(metadata.exists()).isTrue();
        }

        @Test
        @DisplayName("Should return notFound metadata when attachment not found")
        void shouldReturnNotFoundMetadata() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            FileMetadata metadata = service.getFileMetadata(attachmentId);

            assertThat(metadata.attachmentId()).isEqualTo(attachmentId);
            assertThat(metadata.exists()).isFalse();
        }
    }

    @Nested
    @DisplayName("MIME type validation tests")
    class MimeTypeValidationTests {

        @Test
        @DisplayName("Should reject disallowed MIME type when restrictions configured")
        void shouldRejectDisallowedMimeType() {
            // Create service with restricted MIME types
            StorageProperties restrictedProps = new StorageProperties(
                    tempDir.toString(),
                    10L,
                    Set.of("image/png", "image/jpeg"),
                    true
            );
            AttachmentStorageService restrictedService = new AttachmentStorageService(
                    attachmentRepository, restrictedProps
            );

            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            byte[] content = "test".getBytes();

            assertThatThrownBy(() -> restrictedService.storeFile(attachmentId, content, "script.js"))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("Should accept allowed MIME type when restrictions configured")
        void shouldAcceptAllowedMimeType() {
            StorageProperties restrictedProps = new StorageProperties(
                    tempDir.toString(),
                    10L,
                    Set.of("image/png", "image/jpeg"),
                    true
            );
            AttachmentStorageService restrictedService = new AttachmentStorageService(
                    attachmentRepository, restrictedProps
            );

            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();

            assertThatCode(() -> restrictedService.storeFile(attachmentId, content, "image.png"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MIME type detection tests")
    class MimeTypeDetectionTests {

        @Test
        @DisplayName("Should detect common MIME types correctly")
        void shouldDetectCommonMimeTypes() {
            TestRun testRun = createTestRun(runId, "Test Run");
            TestNode testNode = createTestNode("node-1", "Test Node", testRun);
            Attachment attachment = createAttachment(attachmentId, testNode, null);

            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

            byte[] content = "test".getBytes();

            // Test PNG
            Attachment png = service.storeFile(attachmentId, content, "image.png");
            assertThat(png.getMimeType()).isEqualTo("image/png");

            // Test JPEG
            Attachment jpg = service.storeFile(attachmentId, content, "photo.jpg");
            assertThat(jpg.getMimeType()).isEqualTo("image/jpeg");

            // Test JSON
            Attachment json = service.storeFile(attachmentId, content, "data.json");
            assertThat(json.getMimeType()).isEqualTo("application/json");

            // Test text
            Attachment txt = service.storeFile(attachmentId, content, "readme.txt");
            assertThat(txt.getMimeType()).isEqualTo("text/plain");

            // Test log
            Attachment log = service.storeFile(attachmentId, content, "console.log");
            assertThat(log.getMimeType()).isEqualTo("text/plain");

            // Test video
            Attachment mp4 = service.storeFile(attachmentId, content, "recording.mp4");
            assertThat(mp4.getMimeType()).isEqualTo("video/mp4");

            // Test unknown
            Attachment unknown = service.storeFile(attachmentId, content, "binary.xyz");
            assertThat(unknown.getMimeType()).isEqualTo("application/octet-stream");
        }
    }
}

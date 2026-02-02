package com.utem.utem_core.service;

import com.utem.utem_core.config.StorageProperties;
import com.utem.utem_core.dto.FileMetadata;
import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.exception.AttachmentNotFoundException;
import com.utem.utem_core.exception.FileStorageException;
import com.utem.utem_core.exception.FileValidationException;
import com.utem.utem_core.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;

/**
 * Service for storing, retrieving, and deleting attachment files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentStorageService {

    private final AttachmentRepository attachmentRepository;
    private final StorageProperties storageProperties;

    /**
     * Stores a file and updates the Attachment record.
     *
     * @param attachmentId    The ID of the existing Attachment record
     * @param content         File content as byte array
     * @param originalFilename Original filename for extension detection
     * @return Updated Attachment with file path populated
     */
    @Transactional
    public Attachment storeFile(String attachmentId, byte[] content, String originalFilename) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        String mimeType = detectMimeType(originalFilename);
        validateFile(content, mimeType);

        Path filePath = generateFilePath(attachment, originalFilename);
        ensureDirectoryExists(filePath.getParent());

        try {
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            attachment.setFilePath(filePath.toString());
            attachment.setMimeType(mimeType);
            attachment.setFileSize((long) content.length);

            Attachment saved = attachmentRepository.save(attachment);
            log.info("Stored file for attachment {}: {} ({} bytes)", attachmentId, filePath, content.length);

            return saved;
        } catch (IOException e) {
            log.error("Failed to store file for attachment {}: {}", attachmentId, e.getMessage());
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Stores a file from an InputStream.
     *
     * @param attachmentId   The ID of the existing Attachment record
     * @param inputStream    File input stream
     * @param originalFilename Original filename for extension detection
     * @param contentLength  Expected content length for validation
     * @return Updated Attachment with file path populated
     */
    @Transactional
    public Attachment storeFile(String attachmentId, InputStream inputStream, String originalFilename, long contentLength) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        String mimeType = detectMimeType(originalFilename);
        validateFileSize(contentLength);

        Path filePath = generateFilePath(attachment, originalFilename);
        ensureDirectoryExists(filePath.getParent());

        try {
            long bytesWritten = Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            attachment.setFilePath(filePath.toString());
            attachment.setMimeType(mimeType);
            attachment.setFileSize(bytesWritten);

            Attachment saved = attachmentRepository.save(attachment);
            log.info("Stored file for attachment {}: {} ({} bytes)", attachmentId, filePath, bytesWritten);

            return saved;
        } catch (IOException e) {
            log.error("Failed to store file for attachment {}: {}", attachmentId, e.getMessage());
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves file content for an attachment as a Resource.
     *
     * @param attachmentId The attachment ID
     * @return Resource pointing to the file
     */
    @Transactional(readOnly = true)
    public Resource loadFile(String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            throw new FileStorageException("No file stored for attachment: " + attachmentId);
        }

        Path filePath = Path.of(attachment.getFilePath());

        if (!Files.exists(filePath)) {
            throw new FileStorageException("File not found on disk: " + filePath);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File is not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Invalid file path: " + filePath, e);
        }
    }

    /**
     * Retrieves file content as bytes.
     *
     * @param attachmentId The attachment ID
     * @return File content as byte array
     */
    @Transactional(readOnly = true)
    public byte[] loadFileAsBytes(String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            throw new FileStorageException("No file stored for attachment: " + attachmentId);
        }

        Path filePath = Path.of(attachment.getFilePath());

        if (!Files.exists(filePath)) {
            throw new FileStorageException("File not found on disk: " + filePath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the file associated with an attachment.
     *
     * @param attachmentId The attachment ID
     */
    @Transactional
    public void deleteFile(String attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        if (attachment.getFilePath() != null && !attachment.getFilePath().isBlank()) {
            Path filePath = Path.of(attachment.getFilePath());

            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted file for attachment {}: {}", attachmentId, filePath);
                }

                attachment.setFilePath(null);
                attachment.setFileSize(null);
                attachmentRepository.save(attachment);
            } catch (IOException e) {
                log.error("Failed to delete file for attachment {}: {}", attachmentId, e.getMessage());
                throw new FileStorageException("Failed to delete file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Deletes all files for a test run.
     *
     * @param runId The test run ID
     */
    @Transactional
    public void deleteFilesForRun(String runId) {
        if (!storageProperties.organizeByRunId()) {
            log.warn("Cannot delete files by run ID when organize-by-run-id is disabled");
            return;
        }

        Path runDirectory = Path.of(storageProperties.basePath(), runId);

        if (Files.exists(runDirectory)) {
            try {
                // Delete all files in the directory
                Files.walk(runDirectory)
                        .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
                log.info("Deleted files for run {}: {}", runId, runDirectory);
            } catch (IOException e) {
                log.error("Failed to delete files for run {}: {}", runId, e.getMessage());
                throw new FileStorageException("Failed to delete files for run: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Checks if file exists for an attachment.
     *
     * @param attachmentId The attachment ID
     * @return true if file exists on disk
     */
    public boolean fileExists(String attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .map(attachment -> {
                    if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
                        return false;
                    }
                    return Files.exists(Path.of(attachment.getFilePath()));
                })
                .orElse(false);
    }

    /**
     * Gets file metadata without loading content.
     *
     * @param attachmentId The attachment ID
     * @return FileMetadata record
     */
    @Transactional(readOnly = true)
    public FileMetadata getFileMetadata(String attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .map(attachment -> {
                    Path filePath = attachment.getFilePath() != null
                            ? Path.of(attachment.getFilePath())
                            : null;
                    return FileMetadata.from(attachment, filePath);
                })
                .orElse(FileMetadata.notFound(attachmentId));
    }

    /**
     * Gets attachments for a list of node IDs that have files stored.
     *
     * @param nodeIds Collection of node IDs
     * @return List of attachments with files
     */
    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsWithFiles(List<String> nodeIds) {
        return attachmentRepository.findByTestNodeIdIn(nodeIds).stream()
                .filter(a -> a.getFilePath() != null && !a.getFilePath().isBlank())
                .filter(a -> Files.exists(Path.of(a.getFilePath())))
                .toList();
    }

    // ============ Private Helper Methods ============

    private Path generateFilePath(Attachment attachment, String originalFilename) {
        String sanitizedName = sanitizeFilename(originalFilename);
        String prefix = attachment.getId().substring(0, 8);
        String fileName = prefix + "_" + sanitizedName;

        if (storageProperties.organizeByRunId()) {
            String runId = resolveRunId(attachment);
            return Path.of(storageProperties.basePath(), runId, fileName);
        } else {
            return Path.of(storageProperties.basePath(), fileName);
        }
    }

    private String resolveRunId(Attachment attachment) {
        if (attachment.getTestNode() != null && attachment.getTestNode().getTestRun() != null) {
            return attachment.getTestNode().getTestRun().getId();
        } else if (attachment.getTestStep() != null
                && attachment.getTestStep().getTestNode() != null
                && attachment.getTestStep().getTestNode().getTestRun() != null) {
            return attachment.getTestStep().getTestNode().getTestRun().getId();
        }
        return "unassigned";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename
                .replaceAll("[^a-zA-Z0-9.\\-_]", "_")
                .replaceAll("_+", "_")
                .toLowerCase();
    }

    private String detectMimeType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String ext = getFileExtension(filename).toLowerCase();
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "txt", "log" -> "text/plain";
            case "json" -> "application/json";
            case "html", "htm" -> "text/html";
            case "xml" -> "application/xml";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void validateFile(byte[] content, String mimeType) {
        if (content == null || content.length == 0) {
            throw new FileValidationException("EMPTY", "File is empty");
        }

        validateFileSize(content.length);
        validateMimeType(mimeType);
    }

    private void validateFileSize(long size) {
        if (size > storageProperties.getMaxFileSizeBytes()) {
            throw new FileValidationException("SIZE",
                    String.format("File size %d bytes exceeds maximum allowed %d bytes",
                            size, storageProperties.getMaxFileSizeBytes()));
        }
    }

    private void validateMimeType(String mimeType) {
        Set<String> allowed = storageProperties.allowedMimeTypes();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(mimeType)) {
            throw new FileValidationException("MIME_TYPE",
                    String.format("MIME type '%s' is not allowed. Allowed types: %s", mimeType, allowed));
        }
    }

    private void ensureDirectoryExists(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                log.debug("Created directory: {}", directory);
            } catch (IOException e) {
                throw new FileStorageException("Failed to create directory: " + directory, e);
            }
        }
    }
}

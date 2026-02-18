package com.utem.reporter.junit5;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Thin HTTP client wrapper for sending events and uploading files to UTEM Core.
 * Uses java.net.http.HttpClient — zero external dependencies.
 * All methods silently catch exceptions to never disrupt test execution.
 */
public final class UtemHttpClient {

    private final HttpClient client;
    private final UtemConfig config;

    public UtemHttpClient(UtemConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * POST a JSON event to /utem/events.
     */
    public void sendEvent(String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getEventsUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400 && response.statusCode() != 409) {
                System.err.println("[UTEM] Event rejected (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[UTEM] Failed to send event: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Upload a file as multipart/form-data to /utem/attachments/{id}/upload.
     */
    public void uploadFile(String attachmentId, Path filePath, String filename) {
        try {
            String boundary = "----UtemBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] fileBytes = Files.readAllBytes(filePath);

            String prefix = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            String suffix = "\r\n--" + boundary + "--\r\n";

            byte[] prefixBytes = prefix.getBytes();
            byte[] suffixBytes = suffix.getBytes();
            byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
            System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
            System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
            System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getAttachmentUploadUrl(attachmentId)))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("[UTEM] File upload failed (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[UTEM] Failed to upload file: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

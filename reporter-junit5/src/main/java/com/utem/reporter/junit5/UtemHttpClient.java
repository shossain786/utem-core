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

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {100, 500, 2000};

    /**
     * POST a JSON event to /utem/events with retry on failure.
     */
    public void sendEvent(String json) {
        sendWithRetry(config.getEventsUrl(), json);
    }

    /**
     * POST a JSON array of events to /utem/events/batch.
     * Returns true if all events were accepted.
     */
    public boolean sendBatch(java.util.List<String> jsonEvents) {
        if (jsonEvents.isEmpty()) return true;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < jsonEvents.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonEvents.get(i));
        }
        sb.append("]");
        return sendWithRetry(config.getBatchEventsUrl(), sb.toString());
    }

    /**
     * POST JSON to a URL with exponential backoff retry. Returns true on success.
     */
    private boolean sendWithRetry(String url, String json) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 409) return true; // duplicate, ignore
                if (response.statusCode() < 400) return true;  // success

                System.err.println("[UTEM] Event rejected (HTTP " + response.statusCode()
                        + "), attempt " + (attempt + 1) + ": " + response.body());
            } catch (IOException e) {
                System.err.println("[UTEM] Send failed (attempt " + (attempt + 1) + "): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        System.err.println("[UTEM] Gave up after " + (MAX_RETRIES + 1) + " attempts.");
        return false;
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

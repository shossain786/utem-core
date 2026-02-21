package com.utem.utem_core.notification;

import com.utem.utem_core.entity.TestRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends a JSON webhook notification to a Jenkins Generic Webhook Trigger endpoint
 * (or any generic HTTP endpoint) when a test run completes.
 *
 * <p>Enable in {@code application.properties}:
 * <pre>
 * utem.notification.jenkins.enabled=true
 * utem.notification.jenkins.url=http://jenkins-host/generic-webhook-trigger/invoke?token=MY_TOKEN
 * </pre>
 */
@Component
@Slf4j
public class JenkinsNotificationPlugin implements NotificationPlugin {

    @Value("${utem.notification.jenkins.enabled:false}")
    private boolean enabled;

    @Value("${utem.notification.jenkins.url:}")
    private String webhookUrl;

    @Value("${utem.notification.dashboard-base-url:http://localhost:5173}")
    private String dashboardBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String getName() {
        return "Jenkins";
    }

    @Override
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void onRunCompleted(TestRun run) {
        String payload = buildPayload(run);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("Jenkins webhook returned HTTP {} for run {}: {}", response.statusCode(), run.getId(), response.body());
            } else {
                log.info("Jenkins webhook notified for run {} (status={})", run.getId(), run.getStatus());
            }
        } catch (IOException e) {
            log.error("Jenkins webhook failed for run {}: {}", run.getId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Jenkins webhook interrupted for run {}", run.getId());
        }
    }

    private String buildPayload(TestRun run) {
        String dashboardUrl = dashboardBaseUrl.replaceAll("/+$", "") + "/runs/" + run.getId();
        int total = run.getTotalTests() != null ? run.getTotalTests() : 0;
        int passed = run.getPassedTests() != null ? run.getPassedTests() : 0;
        int failed = run.getFailedTests() != null ? run.getFailedTests() : 0;
        int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;

        return "{"
                + "\"runId\":\"" + run.getId() + "\","
                + "\"runName\":\"" + escape(run.getName()) + "\","
                + "\"status\":\"" + run.getStatus().name() + "\","
                + "\"total\":" + total + ","
                + "\"passed\":" + passed + ","
                + "\"failed\":" + failed + ","
                + "\"skipped\":" + skipped + ","
                + "\"dashboardUrl\":\"" + dashboardUrl + "\""
                + "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

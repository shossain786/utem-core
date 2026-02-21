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
 * Sends a Microsoft Teams MessageCard notification via an incoming webhook
 * when a test run completes.
 *
 * <p>Enable in {@code application.properties}:
 * <pre>
 * utem.notification.teams.enabled=true
 * utem.notification.teams.webhook-url=https://outlook.office.com/webhook/...
 * </pre>
 *
 * <p>To create an incoming webhook in Teams: channel settings → Connectors →
 * "Incoming Webhook" → Configure → copy the URL.
 */
@Component
@Slf4j
public class TeamsNotificationPlugin implements NotificationPlugin {

    @Value("${utem.notification.teams.enabled:false}")
    private boolean enabled;

    @Value("${utem.notification.teams.webhook-url:}")
    private String webhookUrl;

    @Value("${utem.notification.dashboard-base-url:http://localhost:5173}")
    private String dashboardBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String getName() {
        return "Teams";
    }

    @Override
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void onRunCompleted(TestRun run) {
        String card = buildMessageCard(run);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(card))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("Teams webhook returned HTTP {} for run {}: {}", response.statusCode(), run.getId(), response.body());
            } else {
                log.info("Teams notified for run {} (status={})", run.getId(), run.getStatus());
            }
        } catch (IOException e) {
            log.error("Teams notification failed for run {}: {}", run.getId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Teams notification interrupted for run {}", run.getId());
        }
    }

    private String buildMessageCard(TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String color = passed ? "00C851" : "FF4444";
        String statusLabel = passed ? "✅ PASSED" : "❌ FAILED";
        String dashboardUrl = dashboardBaseUrl.replaceAll("/+$", "") + "/runs/" + run.getId();

        int total   = run.getTotalTests()   != null ? run.getTotalTests()   : 0;
        int passedN = run.getPassedTests()  != null ? run.getPassedTests()  : 0;
        int failedN = run.getFailedTests()  != null ? run.getFailedTests()  : 0;
        int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;

        String results = passedN + " passed / " + failedN + " failed / " + total + " total";
        if (skipped > 0) results += " / " + skipped + " skipped";

        return "{"
                + "\"@type\":\"MessageCard\","
                + "\"@context\":\"http://schema.org/extensions\","
                + "\"themeColor\":\"" + color + "\","
                + "\"summary\":\"UTEM: " + escape(run.getName()) + " - " + run.getStatus().name() + "\","
                + "\"sections\":[{"
                +   "\"activityTitle\":\"Test Run: " + escape(run.getName()) + "\","
                +   "\"facts\":["
                +     "{\"name\":\"Status\",\"value\":\"" + statusLabel + "\"},"
                +     "{\"name\":\"Results\",\"value\":\"" + results + "\"}"
                +   "],"
                +   "\"potentialAction\":[{"
                +     "\"@type\":\"OpenUri\","
                +     "\"name\":\"View in UTEM\","
                +     "\"targets\":[{\"os\":\"default\",\"uri\":\"" + dashboardUrl + "\"}]"
                +   "}]"
                + "}]}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

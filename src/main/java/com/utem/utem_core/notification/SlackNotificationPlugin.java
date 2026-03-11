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
 * Sends a Slack Block Kit notification via an incoming webhook when a test run completes.
 *
 * <p>Enable in {@code application.properties}:
 * <pre>
 * utem.notification.slack.enabled=true
 * utem.notification.slack.webhook-url=https://hooks.slack.com/services/T.../B.../...
 * </pre>
 *
 * <p>To create an incoming webhook: Slack App settings → Incoming Webhooks → Add New Webhook to Workspace.
 */
@Component
@Slf4j
public class SlackNotificationPlugin implements NotificationPlugin {

    @Value("${utem.notification.slack.enabled:false}")
    private boolean enabled;

    @Value("${utem.notification.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${utem.notification.dashboard-base-url:http://localhost:8080}")
    private String dashboardBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String getName() {
        return "Slack";
    }

    @Override
    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void onRunCompleted(TestRun run) {
        String body = buildBlocks(run);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("Slack webhook returned HTTP {} for run {}: {}", response.statusCode(), run.getId(), response.body());
            } else {
                log.info("Slack notified for run {} (status={})", run.getId(), run.getStatus());
            }
        } catch (IOException e) {
            log.error("Slack notification failed for run {}: {}", run.getId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Slack notification interrupted for run {}", run.getId());
        }
    }

    private String buildBlocks(TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String emoji  = passed ? ":white_check_mark:" : ":x:";
        String status = passed ? "PASSED" : run.getStatus().name();
        String url    = dashboardBaseUrl.replaceAll("/+$", "") + "/#/runs/" + run.getId();

        int total   = run.getTotalTests()   != null ? run.getTotalTests()   : 0;
        int passedN = run.getPassedTests()  != null ? run.getPassedTests()  : 0;
        int failedN = run.getFailedTests()  != null ? run.getFailedTests()  : 0;
        int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;
        double rate = total > 0 ? (passedN * 100.0 / total) : 0.0;

        String results = passedN + " passed / " + failedN + " failed / " + total + " total"
                + (skipped > 0 ? " / " + skipped + " skipped" : "");

        return "{"
                + "\"blocks\":["
                + "{\"type\":\"header\",\"text\":{\"type\":\"plain_text\","
                +   "\"text\":\"" + emoji + " " + escape(run.getName()) + " — " + status + "\"}},"
                + "{\"type\":\"section\",\"fields\":["
                +   "{\"type\":\"mrkdwn\",\"text\":\"*Results*\\n" + results + "\"},"
                +   "{\"type\":\"mrkdwn\",\"text\":\"*Pass Rate*\\n" + String.format("%.1f%%", rate) + "\"}"
                + "]},"
                + "{\"type\":\"actions\",\"elements\":[{"
                +   "\"type\":\"button\","
                +   "\"text\":{\"type\":\"plain_text\",\"text\":\"View in UTEM\"},"
                +   "\"url\":\"" + url + "\""
                + "}]}"
                + "]}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

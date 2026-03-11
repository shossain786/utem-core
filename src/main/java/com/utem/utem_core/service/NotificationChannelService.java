package com.utem.utem_core.service;

import com.utem.utem_core.dto.NotificationChannelDTO;
import com.utem.utem_core.entity.NotificationChannel;
import com.utem.utem_core.entity.NotificationChannel.ChannelType;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.repository.NotificationChannelRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationChannelService {

    private final NotificationChannelRepository repository;

    @Value("${utem.notification.dashboard-base-url:http://localhost:8080}")
    private String dashboardBaseUrl;

    @Value("${utem.notification.email.from:utem@localhost}")
    private String emailFrom;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<NotificationChannelDTO> findAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public NotificationChannelDTO create(NotificationChannelDTO dto) {
        NotificationChannel channel = NotificationChannel.builder()
                .name(dto.getName())
                .type(ChannelType.valueOf(dto.getType()))
                .webhookUrl(dto.getWebhookUrl())
                .emailTo(dto.getEmailTo())
                .enabled(dto.isEnabled())
                .notifyOnFailureOnly(dto.isNotifyOnFailureOnly())
                .createdAt(Instant.now())
                .build();
        return toDTO(repository.save(channel));
    }

    public NotificationChannelDTO update(Long id, NotificationChannelDTO dto) {
        NotificationChannel channel = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification channel not found: " + id));
        channel.setName(dto.getName());
        channel.setType(ChannelType.valueOf(dto.getType()));
        channel.setWebhookUrl(dto.getWebhookUrl());
        channel.setEmailTo(dto.getEmailTo());
        channel.setEnabled(dto.isEnabled());
        channel.setNotifyOnFailureOnly(dto.isNotifyOnFailureOnly());
        return toDTO(repository.save(channel));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    public void notifyRunCompleted(TestRun run) {
        List<NotificationChannel> channels = repository.findAllByEnabledTrueOrderByCreatedAtAsc();
        for (NotificationChannel ch : channels) {
            if (ch.isNotifyOnFailureOnly() && run.getStatus() == TestRun.RunStatus.PASSED) {
                continue;
            }
            try {
                dispatch(ch, run);
                log.debug("[UTEM] Notification sent via '{}' ({}) for run {}", ch.getName(), ch.getType(), run.getId());
            } catch (Exception e) {
                log.error("[UTEM] Notification failed via '{}' for run {}: {}", ch.getName(), run.getId(), e.getMessage());
            }
        }
    }

    public void sendTest(Long channelId) {
        NotificationChannel ch = repository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Notification channel not found: " + channelId));

        TestRun dummy = TestRun.builder()
                .id("test-run-id")
                .name("Test Notification")
                .status(TestRun.RunStatus.PASSED)
                .totalTests(10)
                .passedTests(9)
                .failedTests(1)
                .skippedTests(0)
                .startTime(Instant.now().minusSeconds(30))
                .endTime(Instant.now())
                .build();

        dispatch(ch, dummy);
        log.info("[UTEM] Test notification sent via '{}' ({})", ch.getName(), ch.getType());
    }

    // ── Internal dispatch ─────────────────────────────────────────────────────

    private void dispatch(NotificationChannel ch, TestRun run) {
        switch (ch.getType()) {
            case SLACK   -> sendSlack(ch.getWebhookUrl(), run);
            case TEAMS   -> sendTeams(ch.getWebhookUrl(), run);
            case WEBHOOK -> sendWebhook(ch.getWebhookUrl(), run);
            case EMAIL   -> sendEmail(ch.getEmailTo(), run);
        }
    }

    private void sendSlack(String webhookUrl, TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String emoji  = passed ? ":white_check_mark:" : ":x:";
        String status = passed ? "PASSED" : run.getStatus().name();
        String url    = dashboardUrl(run);

        int total   = nullSafe(run.getTotalTests());
        int passedN = nullSafe(run.getPassedTests());
        int failedN = nullSafe(run.getFailedTests());
        int skipped = nullSafe(run.getSkippedTests());

        String results = passedN + " passed / " + failedN + " failed / " + total + " total"
                + (skipped > 0 ? " / " + skipped + " skipped" : "");

        String body = "{"
                + "\"blocks\":["
                + "{\"type\":\"header\",\"text\":{\"type\":\"plain_text\","
                +   "\"text\":\"" + emoji + " " + escape(run.getName()) + " — " + status + "\"}},"
                + "{\"type\":\"section\",\"fields\":["
                +   "{\"type\":\"mrkdwn\",\"text\":\"*Results*\\n" + results + "\"},"
                +   "{\"type\":\"mrkdwn\",\"text\":\"*Pass Rate*\\n" + passRate(run) + "\"}"
                + "]},"
                + "{\"type\":\"actions\",\"elements\":[{"
                +   "\"type\":\"button\","
                +   "\"text\":{\"type\":\"plain_text\",\"text\":\"View in UTEM\"},"
                +   "\"url\":\"" + url + "\""
                + "}]}"
                + "]}";

        post(webhookUrl, body, "Slack");
    }

    private void sendTeams(String webhookUrl, TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String color  = passed ? "00C851" : "FF4444";
        String status = passed ? "✅ PASSED" : "❌ " + run.getStatus().name();
        String url    = dashboardUrl(run);

        int total   = nullSafe(run.getTotalTests());
        int passedN = nullSafe(run.getPassedTests());
        int failedN = nullSafe(run.getFailedTests());
        int skipped = nullSafe(run.getSkippedTests());
        String results = passedN + " passed / " + failedN + " failed / " + total + " total"
                + (skipped > 0 ? " / " + skipped + " skipped" : "");

        String body = "{"
                + "\"@type\":\"MessageCard\","
                + "\"@context\":\"http://schema.org/extensions\","
                + "\"themeColor\":\"" + color + "\","
                + "\"summary\":\"UTEM: " + escape(run.getName()) + " - " + run.getStatus().name() + "\","
                + "\"sections\":[{"
                +   "\"activityTitle\":\"Test Run: " + escape(run.getName()) + "\","
                +   "\"facts\":["
                +     "{\"name\":\"Status\",\"value\":\"" + status + "\"},"
                +     "{\"name\":\"Results\",\"value\":\"" + results + "\"},"
                +     "{\"name\":\"Pass Rate\",\"value\":\"" + passRate(run) + "\"}"
                +   "],"
                +   "\"potentialAction\":[{"
                +     "\"@type\":\"OpenUri\","
                +     "\"name\":\"View in UTEM\","
                +     "\"targets\":[{\"os\":\"default\",\"uri\":\"" + url + "\"}]"
                +   "}]"
                + "}]}";

        post(webhookUrl, body, "Teams");
    }

    private void sendWebhook(String webhookUrl, TestRun run) {
        int total   = nullSafe(run.getTotalTests());
        int passedN = nullSafe(run.getPassedTests());
        int failedN = nullSafe(run.getFailedTests());
        int skipped = nullSafe(run.getSkippedTests());

        String body = "{"
                + "\"runId\":\"" + run.getId() + "\","
                + "\"runName\":\"" + escape(run.getName()) + "\","
                + "\"status\":\"" + run.getStatus().name() + "\","
                + "\"total\":" + total + ","
                + "\"passed\":" + passedN + ","
                + "\"failed\":" + failedN + ","
                + "\"skipped\":" + skipped + ","
                + "\"passRate\":\"" + passRate(run) + "\","
                + "\"dashboardUrl\":\"" + dashboardUrl(run) + "\""
                + "}";

        post(webhookUrl, body, "Webhook");
    }

    private void sendEmail(String emailTo, TestRun run) {
        if (mailSender == null) {
            log.warn("[UTEM] Email channel skipped — spring.mail is not configured");
            return;
        }
        if (emailTo == null || emailTo.isBlank()) {
            log.warn("[UTEM] Email channel has no recipients configured");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailFrom);
            helper.setTo(emailTo.split(","));
            helper.setSubject("[UTEM] " + run.getName() + " — " + run.getStatus().name());
            helper.setText(buildEmailHtml(run), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("[UTEM] Email send failed: {}", e.getMessage());
        }
    }

    private void post(String url, String body, String channelName) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("[UTEM] {} webhook returned HTTP {}: {}", channelName, resp.statusCode(), resp.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("[UTEM] {} webhook failed: {}", channelName, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String dashboardUrl(TestRun run) {
        return dashboardBaseUrl.replaceAll("/+$", "") + "/#/runs/" + run.getId();
    }

    private String passRate(TestRun run) {
        int total   = nullSafe(run.getTotalTests());
        int passedN = nullSafe(run.getPassedTests());
        double rate = total > 0 ? (passedN * 100.0 / total) : 0.0;
        return String.format("%.1f%%", rate);
    }

    private int nullSafe(Integer v) {
        return v != null ? v : 0;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildEmailHtml(TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String statusColor = passed ? "#00C851" : "#FF4444";
        String statusLabel = passed ? "PASSED" : run.getStatus().name();
        String url = dashboardUrl(run);

        int total   = nullSafe(run.getTotalTests());
        int passedN = nullSafe(run.getPassedTests());
        int failedN = nullSafe(run.getFailedTests());
        int skipped = nullSafe(run.getSkippedTests());

        return "<!DOCTYPE html><html><body style='font-family:sans-serif;background:#f9fafb;margin:0;padding:24px;'>"
                + "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:8px;border:1px solid #e5e7eb;overflow:hidden;'>"
                + "<div style='background:" + statusColor + ";padding:16px 24px;'>"
                + "<h2 style='margin:0;color:#fff;font-size:18px;'>" + htmlEscape(run.getName()) + "</h2>"
                + "<p style='margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:13px;'>Test Run Completed — " + statusLabel + "</p>"
                + "</div>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + row("Status", "<span style='font-weight:600;color:" + statusColor + ";'>" + statusLabel + "</span>")
                + row("Results",
                        "<span style='color:#16a34a;'>" + passedN + " passed</span> / "
                        + "<span style='color:#dc2626;'>" + failedN + " failed</span>"
                        + (skipped > 0 ? " / <span style='color:#9ca3af;'>" + skipped + " skipped</span>" : "")
                        + " / " + total + " total")
                + row("Pass Rate", passRate(run))
                + "</table>"
                + "<div style='padding:20px 24px;text-align:center;'>"
                + "<a href='" + url + "' style='display:inline-block;background:#2563eb;color:#fff;"
                + "text-decoration:none;padding:10px 24px;border-radius:6px;font-size:14px;font-weight:500;'>"
                + "View in UTEM Dashboard</a>"
                + "</div>"
                + "<div style='padding:12px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;"
                + "font-size:11px;color:#9ca3af;text-align:center;'>UTEM — Universal Test Execution Monitor</div>"
                + "</div></body></html>";
    }

    private String row(String label, String value) {
        return "<tr style='border-bottom:1px solid #f3f4f6;'>"
                + "<td style='padding:8px 12px;color:#6b7280;width:100px;'>" + label + "</td>"
                + "<td style='padding:8px 12px;'>" + value + "</td>"
                + "</tr>";
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private NotificationChannelDTO toDTO(NotificationChannel ch) {
        return NotificationChannelDTO.builder()
                .id(ch.getId())
                .name(ch.getName())
                .type(ch.getType().name())
                .webhookUrl(ch.getWebhookUrl())
                .emailTo(ch.getEmailTo())
                .enabled(ch.isEnabled())
                .notifyOnFailureOnly(ch.isNotifyOnFailureOnly())
                .createdAt(ch.getCreatedAt().toString())
                .build();
    }
}

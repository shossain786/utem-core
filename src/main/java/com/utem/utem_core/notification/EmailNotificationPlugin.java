package com.utem.utem_core.notification;

import com.utem.utem_core.entity.TestRun;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends an HTML email summary when a test run completes.
 *
 * <p>Enable in {@code application.properties}:
 * <pre>
 * utem.notification.email.enabled=true
 * utem.notification.email.to=team@example.com
 *
 * # Standard Spring Mail SMTP config:
 * spring.mail.host=smtp.gmail.com
 * spring.mail.port=587
 * spring.mail.username=sender@example.com
 * spring.mail.password=app-password
 * spring.mail.properties.mail.smtp.auth=true
 * spring.mail.properties.mail.smtp.starttls.enable=true
 * </pre>
 */
@Component
@Slf4j
public class EmailNotificationPlugin implements NotificationPlugin {

    @Value("${utem.notification.email.enabled:false}")
    private boolean enabled;

    @Value("${utem.notification.email.to:}")
    private String toAddress;

    @Value("${utem.notification.email.from:utem@localhost}")
    private String fromAddress;

    @Value("${utem.notification.dashboard-base-url:http://localhost:5173}")
    private String dashboardBaseUrl;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public String getName() {
        return "Email";
    }

    @Override
    public boolean isEnabled() {
        return enabled && mailSender != null && toAddress != null && !toAddress.isBlank();
    }

    @Override
    public void onRunCompleted(TestRun run) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toAddress.split(","));
            helper.setSubject(buildSubject(run));
            helper.setText(buildHtmlBody(run), true);
            mailSender.send(message);
            log.info("Email notification sent for run {} to {}", run.getId(), toAddress);
        } catch (MessagingException e) {
            log.error("Email notification failed for run {}: {}", run.getId(), e.getMessage());
        }
    }

    private String buildSubject(TestRun run) {
        String status = run.getStatus().name();
        return "[UTEM] " + run.getName() + " — " + status;
    }

    private String buildHtmlBody(TestRun run) {
        boolean passed = run.getStatus() == TestRun.RunStatus.PASSED;
        String statusColor = passed ? "#00C851" : "#FF4444";
        String statusLabel = passed ? "PASSED" : run.getStatus().name();
        String dashboardUrl = dashboardBaseUrl.replaceAll("/+$", "") + "/runs/" + run.getId();

        int total   = run.getTotalTests()   != null ? run.getTotalTests()   : 0;
        int passedN = run.getPassedTests()  != null ? run.getPassedTests()  : 0;
        int failedN = run.getFailedTests()  != null ? run.getFailedTests()  : 0;
        int skipped = run.getSkippedTests() != null ? run.getSkippedTests() : 0;

        double passRate = total > 0 ? (passedN * 100.0 / total) : 0.0;
        String passRateStr = String.format("%.1f%%", passRate);

        String durationStr = "--";
        if (run.getStartTime() != null && run.getEndTime() != null) {
            long secs = java.time.Duration.between(run.getStartTime(), run.getEndTime()).toSeconds();
            durationStr = secs >= 60
                    ? (secs / 60) + "m " + (secs % 60) + "s"
                    : secs + "s";
        }

        String jobRow = run.getJobName() != null
                ? "<tr><td style='padding:6px 12px;color:#6b7280;'>Job</td>"
                + "<td style='padding:6px 12px;font-weight:500;'>" + escape(run.getJobName()) + "</td></tr>"
                : "";
        String labelRow = run.getLabel() != null
                ? "<tr><td style='padding:6px 12px;color:#6b7280;'>Label</td>"
                + "<td style='padding:6px 12px;'><span style='background:#e0e7ff;color:#4338ca;padding:2px 8px;border-radius:4px;font-size:12px;'>"
                + escape(run.getLabel()) + "</span></td></tr>"
                : "";

        return "<!DOCTYPE html><html><body style='font-family:sans-serif;background:#f9fafb;margin:0;padding:24px;'>"
                + "<div style='max-width:560px;margin:0 auto;background:#fff;border-radius:8px;border:1px solid #e5e7eb;overflow:hidden;'>"

                // Header bar
                + "<div style='background:" + statusColor + ";padding:16px 24px;'>"
                + "<h2 style='margin:0;color:#fff;font-size:18px;'>" + escape(run.getName()) + "</h2>"
                + "<p style='margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:13px;'>Test Run Completed — " + statusLabel + "</p>"
                + "</div>"

                // Stats table
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + "<tr style='border-bottom:1px solid #f3f4f6;'>"
                + "<td style='padding:6px 12px;color:#6b7280;'>Status</td>"
                + "<td style='padding:6px 12px;font-weight:600;color:" + statusColor + ";'>" + statusLabel + "</td>"
                + "</tr>"
                + "<tr style='border-bottom:1px solid #f3f4f6;'>"
                + "<td style='padding:6px 12px;color:#6b7280;'>Results</td>"
                + "<td style='padding:6px 12px;'>"
                + "<span style='color:#16a34a;'>" + passedN + " passed</span> / "
                + "<span style='color:#dc2626;'>" + failedN + " failed</span>"
                + (skipped > 0 ? " / <span style='color:#9ca3af;'>" + skipped + " skipped</span>" : "")
                + " / " + total + " total"
                + "</td></tr>"
                + "<tr style='border-bottom:1px solid #f3f4f6;'>"
                + "<td style='padding:6px 12px;color:#6b7280;'>Pass Rate</td>"
                + "<td style='padding:6px 12px;font-weight:500;'>" + passRateStr + "</td>"
                + "</tr>"
                + "<tr style='border-bottom:1px solid #f3f4f6;'>"
                + "<td style='padding:6px 12px;color:#6b7280;'>Duration</td>"
                + "<td style='padding:6px 12px;'>" + durationStr + "</td>"
                + "</tr>"
                + jobRow
                + labelRow
                + "</table>"

                // CTA button
                + "<div style='padding:20px 24px;text-align:center;'>"
                + "<a href='" + dashboardUrl + "' style='display:inline-block;background:#2563eb;color:#fff;"
                + "text-decoration:none;padding:10px 24px;border-radius:6px;font-size:14px;font-weight:500;'>"
                + "View in UTEM Dashboard</a>"
                + "</div>"

                + "<div style='padding:12px 24px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:11px;color:#9ca3af;text-align:center;'>"
                + "UTEM — Universal Test Execution Monitor"
                + "</div>"
                + "</div></body></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

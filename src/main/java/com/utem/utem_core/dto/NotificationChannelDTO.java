package com.utem.utem_core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelDTO {

    private Long id;

    /** Display name (e.g. "QA Slack Channel"). */
    private String name;

    /** Channel type: SLACK | TEAMS | EMAIL | WEBHOOK */
    private String type;

    /** Webhook URL — required for SLACK, TEAMS, WEBHOOK. */
    private String webhookUrl;

    /** Comma-separated recipients — required for EMAIL. */
    private String emailTo;

    private boolean enabled;

    /** When true, only fire when the run status is FAILED. */
    private boolean notifyOnFailureOnly;

    private String createdAt;
}

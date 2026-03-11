package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    /** Webhook URL — used for SLACK, TEAMS, WEBHOOK channel types. */
    @Column(name = "webhook_url", length = 1024)
    private String webhookUrl;

    /** Comma-separated recipient list — used for EMAIL channel type. */
    @Column(name = "email_to", length = 512)
    private String emailTo;

    @Column(columnDefinition = "boolean default true")
    private boolean enabled = true;

    /** When true, only send a notification if the run FAILED. */
    @Column(name = "notify_on_failure_only", columnDefinition = "boolean default false")
    private boolean notifyOnFailureOnly = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public enum ChannelType {
        SLACK, TEAMS, EMAIL, WEBHOOK
    }
}

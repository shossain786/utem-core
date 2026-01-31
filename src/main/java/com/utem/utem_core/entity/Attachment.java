package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "attachment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private TestNode testNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private TestStep testStep;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentType type;

    @Column(nullable = false)
    private String filePath;

    private String mimeType;

    private Long fileSize;

    @Column(nullable = false)
    private Instant timestamp;

    private Boolean isFailureScreenshot;

    public enum AttachmentType {
        SCREENSHOT,
        LOG,
        VIDEO,
        FILE
    }
}

package com.utem.utem_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

package com.webar.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "three_d_generation_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreeDGenerationJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerTaskId;

    @Column(nullable = false)
    private String generationType;

    private Integer progress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreeDGenerationJobStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer sourceMediaCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ThreeDGenerationJobStatus.PENDING;
        if (progress == null) progress = 0;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

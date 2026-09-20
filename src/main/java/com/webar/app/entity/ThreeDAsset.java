package com.webar.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "three_d_assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreeDAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String glbUrl;

    @Column(columnDefinition = "TEXT")
    private String usdzUrl;

    private String glbFileName;

    private String usdzFileName;

    @Enumerated(EnumType.STRING)
    private ThreeDStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "menu_item_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private MenuItem menuItem;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ThreeDStatus.NOT_STARTED;
        }
    }

    @PreUpdate
    public void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
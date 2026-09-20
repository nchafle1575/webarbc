package com.webar.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_images")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private Integer sortOrder;

    private Boolean primaryImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "menu_item_id",
            nullable = false
    )
    @JsonIgnore
    private MenuItem menuItem;
}
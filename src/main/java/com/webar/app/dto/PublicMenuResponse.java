package com.webar.app.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicMenuResponse(
        Long restaurantId,
        String restaurantName,
        String logoUrl,
        String description,
        String address,
        String city,
        String state,
        String country,
        String phone,
        String email,
        List<CategoryResponse> categories
) {
    public record CategoryResponse(
            Long id,
            String name,
            String slug,
            String imageUrl,
            Integer sortOrder,
            List<MenuItemResponse> items
    ) {}

    public record MenuItemResponse(
            Long id,
            String name,
            String sku,
            BigDecimal price,
            String currency,
            String shortDescription,
            String description,
            String foodType,
            String spiceLevel,
            String servingSize,
            Integer preparationTimeMinutes,
            String ingredients,
            String allergens,
            Integer calories,
            List<String> tags,
            List<ImageResponse> images,
            List<MediaResponse> media,
            ThreeDResponse threeD
    ) {}

    public record ImageResponse(
            Long id,
            String fileName,
            String imageUrl,
            Integer sortOrder,
            Boolean primaryImage
    ) {}

    public record MediaResponse(
            Long id,
            String fileName,
            String mediaUrl,
            String mediaType
    ) {}

    public record ThreeDResponse(
            Long id,
            String glbUrl,
            String usdzUrl,
            String glbFileName,
            String usdzFileName,
            String status,
            String errorMessage
    ) {}
}

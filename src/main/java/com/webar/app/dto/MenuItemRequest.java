package com.webar.app.dto;

import com.webar.app.entity.FoodType;
import com.webar.app.entity.SpiceLevel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(

        @NotBlank(message = "Menu item name is required")
        String name,

        String sku,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.0",
                message = "Price cannot be negative"
        )
        BigDecimal price,

        String currency,

        String shortDescription,

        String description,

        FoodType foodType,

        SpiceLevel spiceLevel,

        String servingSize,

        Integer preparationTimeMinutes,

        String ingredients,

        String allergens,

        Integer calories,

        Boolean available,

        Boolean published,

        @NotNull(message = "Restaurant id is required")
        Long restaurantId,

        @NotNull(message = "Category id is required")
        Long categoryId,

        List<String> tags
) {
}
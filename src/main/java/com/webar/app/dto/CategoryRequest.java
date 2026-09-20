package com.webar.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        String name,

        String slug,

        Integer sortOrder,

        Boolean active,

        @NotNull(message = "Restaurant id is required")
        Long restaurantId
) {
}
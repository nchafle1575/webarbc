package com.webar.app.dto;

import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(

        @NotBlank(message = "Restaurant name is required")
        String name,

        String phone,

        String email,

        String address,

        String city,

        String state,

        String country,

        String website,

        String description
) {
}
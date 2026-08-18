package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a new restaurant")
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Schema(description = "Name of the restaurant", example = "FoodFlow Kitchen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String restaurantName;

    @Schema(description = "Location or address of the restaurant", example = "Delhi")
    private String location;

    @Schema(description = "Cuisine type offered", example = "North Indian")
    private String cuisine;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    @Schema(description = "Initial restaurant rating (0.0 - 5.0)", example = "4.5")
    private Double rating;
}

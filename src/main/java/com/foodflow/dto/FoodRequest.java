package com.foodflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRequest {

    private String itemId;

    @NotBlank(message = "Food name is required")
    private String name;

    private String description;

    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private Double price;

    @Builder.Default
    private Boolean isVeg = true;

    private String spiceLevel;

    private String dietaryTags;

    private Long restaurantId;
}

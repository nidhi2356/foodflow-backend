package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for creating a new food menu item")
public class FoodRequest {

    @Schema(description = "External or AI dataset item identifier (e.g. m001, m004)", example = "m004")
    private String itemId;

    @NotBlank(message = "Food name is required")
    @Schema(description = "Name of the food item", example = "Paneer Tikka Protein Bowl", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed description of the food item", example = "High protein healthy meal with grilled paneer and salad")
    private String description;

    @Schema(description = "Category of the food", example = "Healthy")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    @Schema(description = "Price of the food item in INR", example = "350.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double price;

    @Builder.Default
    @Schema(description = "Indicates if the item is vegetarian", example = "true")
    private Boolean isVeg = true;

    @Schema(description = "Spice level (e.g. Mild, Medium, Spicy)", example = "Mild")
    private String spiceLevel;

    @Schema(description = "Dietary tags (comma-separated)", example = "Vegetarian, High Protein, Healthy")
    private String dietaryTags;

    @NotNull(message = "Restaurant ID is required")
    @Schema(description = "Database ID of the restaurant offering this food item", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long restaurantId;
}

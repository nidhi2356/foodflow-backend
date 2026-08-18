package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Food item details response")
public class FoodResponse {

    @Schema(description = "Database primary key ID", example = "10")
    private Long id;

    @Schema(description = "External or AI dataset item identifier (e.g. m001, m004)", example = "m004")
    private String itemId;

    @Schema(description = "Name of the food item", example = "Paneer Tikka Protein Bowl")
    private String name;

    @Schema(description = "Description of the food item", example = "High protein healthy meal with grilled paneer")
    private String description;

    @Schema(description = "Food category", example = "Healthy Bowl")
    private String category;

    @Schema(description = "Price in INR", example = "350.0")
    private Double price;

    @Schema(description = "Vegetarian flag", example = "true")
    private Boolean isVeg;

    @Schema(description = "Spice level", example = "Mild")
    private String spiceLevel;

    @Schema(description = "Dietary tags", example = "Vegetarian, High Protein, Healthy")
    private String dietaryTags;

    @Schema(description = "Database ID of the associated restaurant", example = "1")
    private Long restaurantId;

    @Schema(description = "Name of the associated restaurant", example = "FoodFlow Kitchen")
    private String restaurantName;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}

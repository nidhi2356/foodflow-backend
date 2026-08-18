package com.foodflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for partially updating a food menu item")
public class FoodPatchRequest {

    @Schema(description = "External or AI dataset item identifier (e.g. m001, m004)", example = "m004")
    private String itemId;

    @Pattern(regexp = "^(?!\\s*$).+", message = "Food name cannot be blank if provided")
    @Schema(description = "Updated name of the food item", example = "Paneer Tikka Special")
    private String name;

    @Schema(description = "Updated description", example = "Updated high protein healthy meal")
    private String description;

    @Schema(description = "Updated category", example = "Healthy Bowl")
    private String category;

    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0 if provided")
    @Schema(description = "Updated price in INR", example = "550.0")
    private Double price;

    @Schema(description = "Updated vegetarian flag", example = "true")
    private Boolean isVeg;

    @Schema(description = "Updated spice level", example = "Medium")
    private String spiceLevel;

    @Schema(description = "Updated dietary tags", example = "Vegetarian, High Protein")
    private String dietaryTags;

    @JsonIgnore
    public boolean hasUpdates() {
        return itemId != null || name != null || description != null || category != null
                || price != null || isVeg != null || spiceLevel != null || dietaryTags != null;
    }
}

package com.foodflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
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
@Schema(description = "Request body for partially updating a restaurant")
public class RestaurantPatchRequest {

    @Pattern(regexp = "^(?!\\s*$).+", message = "Restaurant name cannot be blank if provided")
    @Schema(description = "Updated name of the restaurant", example = "FoodFlow Gourmet Kitchen")
    private String restaurantName;

    @Schema(description = "Updated location of the restaurant", example = "Connaught Place, Delhi")
    private String location;

    @Schema(description = "Updated cuisine type", example = "North Indian, Mughlai")
    private String cuisine;

    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    @Schema(description = "Updated rating (0.0 - 5.0)", example = "4.8")
    private Double rating;

    @JsonIgnore
    public boolean hasUpdates() {
        return restaurantName != null || location != null || cuisine != null || rating != null;
    }
}

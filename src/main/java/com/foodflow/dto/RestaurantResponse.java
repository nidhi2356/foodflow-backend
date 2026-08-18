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
@Schema(description = "Restaurant details response")
public class RestaurantResponse {

    @Schema(description = "Database primary key ID", example = "1")
    private Long id;

    @Schema(description = "Name of the restaurant", example = "FoodFlow Kitchen")
    private String restaurantName;

    @Schema(description = "Location or address", example = "Delhi")
    private String location;

    @Schema(description = "Cuisine type", example = "North Indian")
    private String cuisine;

    @Schema(description = "Restaurant rating (0.0 - 5.0)", example = "4.5")
    private Double rating;

    @Schema(description = "ID of the user who owns this restaurant", example = "1")
    private Long ownerId;

    @Schema(description = "Username of the restaurant owner", example = "chef_john")
    private String ownerUsername;

    @Schema(description = "Number of food menu items offered", example = "12")
    private Integer foodItemCount;

    @Schema(description = "Timestamp when restaurant was created")
    private LocalDateTime createdAt;
}

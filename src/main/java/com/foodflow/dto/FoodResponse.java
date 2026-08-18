package com.foodflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {

    private Long id;
    private String itemId;
    private String name;
    private String description;
    private String category;
    private Double price;
    private Boolean isVeg;
    private String spiceLevel;
    private String dietaryTags;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantExternalId;
    private LocalDateTime createdAt;
}

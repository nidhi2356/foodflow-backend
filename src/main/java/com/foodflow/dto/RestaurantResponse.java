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
public class RestaurantResponse {

    private Long id;
    private String restaurantId;
    private String restaurantName;
    private String location;
    private String cuisine;
    private Double rating;
    private String priceRange;
    private Integer foodItemCount;
    private LocalDateTime createdAt;
}

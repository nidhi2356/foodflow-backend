package com.foodflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    private String itemName;
    private String itemId;
    private String restaurantId;
    private String restaurantName;
    private String location;
    private String cuisine;
    private Double rating;
    private String category;
    private Double price;
    private Boolean isVeg;
    private String spiceLevel;
    private String dietaryTags;
    private String description;
    private Double crossEncoderScore;
    private Double normalizedCrossScore;
    private Double metadataScore;
    private Double normalizedMetadataScore;
    private Double finalScore;
}

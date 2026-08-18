package com.foodflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFoodMetadata {

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("restaurant_id")
    private String restaurantId;

    @JsonProperty("restaurant_name")
    private String restaurantName;

    @JsonProperty("location")
    private String location;

    @JsonProperty("cuisine")
    private String cuisine;

    @JsonProperty("rating")
    private Double rating;

    @JsonProperty("category")
    private String category;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("is_veg")
    private Boolean isVeg;

    @JsonProperty("spice_level")
    private String spiceLevel;

    @JsonProperty("dietary_tags")
    private String dietaryTags;
}

package com.foodflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSearchResponse {

    @JsonProperty("results")
    @Builder.Default
    private List<AiSearchResult> results = new ArrayList<>();

    @JsonProperty("recommendation")
    private String recommendation;
}

package com.foodflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchRequest {

    @JsonProperty("query")
    private String query;

    @JsonProperty("top_k")
    private Integer topK;
}

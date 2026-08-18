package com.foodflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Search query is required and cannot be blank")
    private String query;

    @Min(value = 1, message = "top_k must be at least 1")
    @Max(value = 50, message = "top_k must not exceed 50")
    @Builder.Default
    private Integer topK = 5;
}

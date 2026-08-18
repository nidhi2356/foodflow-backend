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
public class AiSearchResult {

    @JsonProperty("text")
    private String text;

    @JsonProperty("metadata")
    private AiFoodMetadata metadata;

    @JsonProperty("cross_encoder_score")
    private Double crossEncoderScore;

    @JsonProperty("normalized_cross_score")
    private Double normalizedCrossScore;

    @JsonProperty("metadata_score")
    private Double metadataScore;

    @JsonProperty("normalized_metadata_score")
    private Double normalizedMetadataScore;

    @JsonProperty("final_score")
    private Double finalScore;
}

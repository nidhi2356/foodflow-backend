package com.foodflow.service;

import com.foodflow.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final AiService aiService;

    public SearchService(AiService aiService) {
        this.aiService = aiService;
    }

    public SearchResponse search(SearchRequest request) {
        log.info("Processing search query: '{}', top_k={}", request.getQuery(), request.getTopK());

        AiSearchResponse aiResponse = aiService.searchFood(request.getQuery(), request.getTopK());

        List<SearchResultResponse> searchResults = new ArrayList<>();
        if (aiResponse.getResults() != null) {
            searchResults = aiResponse.getResults().stream()
                    .map(this::mapToSearchResultResponse)
                    .collect(Collectors.toList());
        }

        return SearchResponse.builder()
                .results(searchResults)
                .recommendation(aiResponse.getRecommendation())
                .build();
    }

    private SearchResultResponse mapToSearchResultResponse(AiSearchResult aiResult) {
        AiFoodMetadata meta = aiResult.getMetadata();

        SearchResultResponse.SearchResultResponseBuilder builder = SearchResultResponse.builder()
                .description(aiResult.getText())
                .crossEncoderScore(aiResult.getCrossEncoderScore())
                .normalizedCrossScore(aiResult.getNormalizedCrossScore())
                .metadataScore(aiResult.getMetadataScore())
                .normalizedMetadataScore(aiResult.getNormalizedMetadataScore())
                .finalScore(aiResult.getFinalScore());

        if (meta != null) {
            builder.itemName(meta.getItemName())
                    .itemId(meta.getItemId())
                    .restaurantId(meta.getRestaurantId())
                    .restaurantName(meta.getRestaurantName())
                    .location(meta.getLocation())
                    .cuisine(meta.getCuisine())
                    .rating(meta.getRating())
                    .category(meta.getCategory())
                    .price(meta.getPrice())
                    .isVeg(meta.getIsVeg())
                    .spiceLevel(meta.getSpiceLevel())
                    .dietaryTags(meta.getDietaryTags());
        }

        return builder.build();
    }
}

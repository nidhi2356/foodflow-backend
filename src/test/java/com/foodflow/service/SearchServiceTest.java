package com.foodflow.service;

import com.foodflow.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private SearchService searchService;

    private AiSearchResponse mockAiResponse;

    @BeforeEach
    void setUp() {
        AiFoodMetadata metadata = AiFoodMetadata.builder()
                .itemName("Grilled Paneer Protein Bowl")
                .itemId("m004")
                .restaurantId("r002")
                .restaurantName("Green Bowl")
                .location("Saket, Delhi")
                .cuisine("Healthy, Continental")
                .rating(4.6)
                .category("Healthy Bowl")
                .price(350.0)
                .isVeg(true)
                .spiceLevel("Mild")
                .dietaryTags("Vegetarian, High Protein, Healthy")
                .build();

        AiSearchResult result = AiSearchResult.builder()
                .text("Delicious high protein vegetarian bowl with grilled paneer and fresh veggies")
                .metadata(metadata)
                .crossEncoderScore(3.5675)
                .normalizedCrossScore(1.0)
                .metadataScore(2.0)
                .normalizedMetadataScore(1.0)
                .finalScore(1.0)
                .build();

        mockAiResponse = AiSearchResponse.builder()
                .results(List.of(result))
                .recommendation("Based on your preference for high protein vegetarian dinner, I recommend the Grilled Paneer Protein Bowl.")
                .build();
    }

    @Test
    @DisplayName("Should successfully coordinate search and map AI response to frontend DTO")
    void shouldSearchSuccessfully() {
        SearchRequest request = SearchRequest.builder()
                .query("healthy high protein vegetarian dinner under 400")
                .topK(5)
                .build();

        when(aiService.searchFood("healthy high protein vegetarian dinner under 400", 5))
                .thenReturn(mockAiResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getRecommendation()).contains("Grilled Paneer Protein Bowl");

        SearchResultResponse firstResult = response.getResults().get(0);
        assertThat(firstResult.getItemName()).isEqualTo("Grilled Paneer Protein Bowl");
        assertThat(firstResult.getItemId()).isEqualTo("m004");
        assertThat(firstResult.getRestaurantName()).isEqualTo("Green Bowl");
        assertThat(firstResult.getPrice()).isEqualTo(350.0);
        assertThat(firstResult.getIsVeg()).isTrue();
        assertThat(firstResult.getFinalScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle empty results gracefully without error")
    void shouldHandleEmptyAiResultsGracefully() {
        SearchRequest request = SearchRequest.builder()
                .query("nonexistent unusual food xyz")
                .topK(5)
                .build();

        AiSearchResponse emptyAiResponse = AiSearchResponse.builder()
                .results(Collections.emptyList())
                .recommendation("Sorry, I couldn't find any food items matching your requirements.")
                .build();

        when(aiService.searchFood("nonexistent unusual food xyz", 5))
                .thenReturn(emptyAiResponse);

        SearchResponse response = searchService.search(request);

        assertThat(response).isNotNull();
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getRecommendation()).isEqualTo("Sorry, I couldn't find any food items matching your requirements.");
    }
}

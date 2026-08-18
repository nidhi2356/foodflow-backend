package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.SearchRequest;
import com.foodflow.dto.SearchResponse;
import com.foodflow.dto.SearchResultResponse;
import com.foodflow.exception.AiServiceUnavailableException;
import com.foodflow.security.JwtService;
import com.foodflow.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/search should return 200 OK with AI ranked results and grounded recommendation")
    void searchShouldReturnOkWithResults() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("healthy high protein vegetarian dinner under ₹400")
                .topK(5)
                .build();

        SearchResultResponse result = SearchResultResponse.builder()
                .itemName("Grilled Paneer Protein Bowl")
                .itemId("m004")
                .restaurantName("Green Bowl")
                .location("Saket, Delhi")
                .cuisine("Healthy, Continental")
                .rating(4.6)
                .category("Healthy Bowl")
                .price(350.0)
                .isVeg(true)
                .spiceLevel("Mild")
                .dietaryTags("Vegetarian, High Protein, Healthy")
                .description("Grilled Paneer Protein Bowl with fresh veggies")
                .crossEncoderScore(3.5675)
                .metadataScore(2.0)
                .finalScore(1.0)
                .build();

        SearchResponse response = SearchResponse.builder()
                .results(List.of(result))
                .recommendation("Based on your preference for high protein vegetarian dinner, I recommend the Grilled Paneer Protein Bowl from Green Bowl.")
                .build();

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].itemName").value("Grilled Paneer Protein Bowl"))
                .andExpect(jsonPath("$.results[0].restaurantName").value("Green Bowl"))
                .andExpect(jsonPath("$.results[0].price").value(350.0))
                .andExpect(jsonPath("$.results[0].isVeg").value(true))
                .andExpect(jsonPath("$.results[0].crossEncoderScore").value(3.5675))
                .andExpect(jsonPath("$.recommendation").value(containsString("Grilled Paneer Protein Bowl")));
    }

    @Test
    @DisplayName("POST /api/search should return 200 OK with graceful message when zero results found")
    void searchShouldReturnOkWhenZeroResults() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("alien dish with zero matches")
                .topK(5)
                .build();

        SearchResponse response = SearchResponse.builder()
                .results(Collections.emptyList())
                .recommendation("Sorry, I couldn't find any food items matching your requirements.")
                .build();

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty())
                .andExpect(jsonPath("$.recommendation").value("Sorry, I couldn't find any food items matching your requirements."));
    }

    @Test
    @DisplayName("POST /api/search should return 503 Service Unavailable when FastAPI is down")
    void searchShouldReturn503WhenAiServiceUnavailable() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("healthy food")
                .topK(5)
                .build();

        when(searchService.search(any(SearchRequest.class)))
                .thenThrow(new AiServiceUnavailableException("AI search service is currently unavailable"));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI search service is currently unavailable"));
    }

    @Test
    @DisplayName("POST /api/search should return 400 Bad Request on blank query")
    void searchShouldReturn400OnBlankQuery() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("")
                .topK(5)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("POST /api/search should return 400 Bad Request on invalid top_k")
    void searchShouldReturn400OnInvalidTopK() throws Exception {
        SearchRequest request = SearchRequest.builder()
                .query("pizza")
                .topK(0)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}

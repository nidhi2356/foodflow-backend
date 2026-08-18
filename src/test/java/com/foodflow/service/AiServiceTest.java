package com.foodflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.AiFoodMetadata;
import com.foodflow.dto.AiSearchResponse;
import com.foodflow.dto.AiSearchResult;
import com.foodflow.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiServiceTest {

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        aiService = new AiService(restClient);
    }

    @Test
    @DisplayName("Should successfully call FastAPI and parse AI response")
    void shouldCallFastApiSuccessfully() throws Exception {
        AiFoodMetadata metadata = AiFoodMetadata.builder()
                .itemName("Grilled Paneer Protein Bowl")
                .itemId("m004")
                .restaurantName("Green Bowl")
                .price(350.0)
                .isVeg(true)
                .build();

        AiSearchResult result = AiSearchResult.builder()
                .text("High protein bowl")
                .metadata(metadata)
                .crossEncoderScore(3.56)
                .finalScore(1.0)
                .build();

        AiSearchResponse mockResponse = AiSearchResponse.builder()
                .results(List.of(result))
                .recommendation("Try the Grilled Paneer Protein Bowl")
                .build();

        mockServer.expect(requestTo("http://localhost:8000/api/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiSearchResponse response = aiService.searchFood("healthy high protein vegetarian", 5);

        mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getMetadata().getItemName()).isEqualTo("Grilled Paneer Protein Bowl");
        assertThat(response.getRecommendation()).isEqualTo("Try the Grilled Paneer Protein Bowl");
    }

    @Test
    @DisplayName("Should handle empty search results from FastAPI gracefully")
    void shouldHandleEmptyResultsFromFastApi() throws Exception {
        AiSearchResponse mockResponse = AiSearchResponse.builder()
                .results(Collections.emptyList())
                .recommendation("Sorry, I couldn't find any food items matching your requirements.")
                .build();

        mockServer.expect(requestTo("http://localhost:8000/api/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        AiSearchResponse response = aiService.searchFood("unknown dish", 5);

        mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getRecommendation()).contains("couldn't find any food items");
    }

    @Test
    @DisplayName("Should throw AiServiceUnavailableException when FastAPI returns 500 error")
    void shouldThrowWhenFastApiReturns500() {
        mockServer.expect(requestTo("http://localhost:8000/api/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> aiService.searchFood("healthy dinner", 5))
                .isInstanceOf(AiServiceUnavailableException.class);

        mockServer.verify();
    }
}

package com.foodflow.service;

import com.foodflow.dto.AiSearchRequest;
import com.foodflow.dto.AiSearchResponse;
import com.foodflow.exception.AiServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestClient aiRestClient;

    public AiService(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public AiSearchResponse searchFood(String query, Integer topK) {
        log.info("Sending search request to FastAPI AI service: query='{}', top_k={}", query, topK);

        AiSearchRequest request = AiSearchRequest.builder()
                .query(query)
                .topK(topK != null ? topK : 5)
                .build();

        try {
            AiSearchResponse response = aiRestClient.post()
                    .uri("/api/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("AI service returned 5xx server error: status={}", resp.getStatusCode());
                        throw new AiServiceUnavailableException("AI search service returned server error: " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("AI service returned 4xx client error: status={}", resp.getStatusCode());
                        throw new AiServiceUnavailableException("AI search service rejected request with status: " + resp.getStatusCode());
                    })
                    .body(AiSearchResponse.class);

            if (response == null) {
                log.warn("AI service returned null response body");
                return AiSearchResponse.builder()
                        .results(new ArrayList<>())
                        .recommendation("Sorry, I couldn't find any food items matching your requirements.")
                        .build();
            }

            if (response.getResults() == null) {
                response.setResults(new ArrayList<>());
            }

            if (response.getResults().isEmpty() && (response.getRecommendation() == null || response.getRecommendation().isBlank())) {
                response.setRecommendation("Sorry, I couldn't find any food items matching your requirements.");
            }

            log.info("AI service returned {} results", response.getResults().size());
            return response;

        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Failed to connect to AI service: {}", e.getMessage());
            throw new AiServiceUnavailableException("AI search service is currently unavailable", e);
        } catch (RestClientResponseException e) {
            log.error("REST client error communicating with AI service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceUnavailableException("AI search service error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during AI service communication: {}", e.getMessage(), e);
            throw new AiServiceUnavailableException("AI search service is currently unavailable", e);
        }
    }
}

package com.foodflow.controller;

import com.foodflow.dto.SearchRequest;
import com.foodflow.dto.SearchResponse;
import com.foodflow.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Tag(name = "AI Search & Recommendation", description = "AI-powered food search and personalized recommendation gateway")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    @Operation(
            summary = "Search food items using AI",
            description = "Delegates semantic natural language queries to the Python FastAPI AI pipeline (Hybrid BM25 + Vector Search + RRF + Cross-Encoder Ranking) and returns ranked results and grounded recommendations."
    )
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
}

package com.foodflow.controller;

import com.foodflow.dto.FavoriteResponse;
import com.foodflow.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "Favorites", description = "Endpoints for managing user favorite food items")
@SecurityRequirement(name = "Bearer Authentication")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "Get user favorites", description = "Retrieves all favorite food items for the authenticated user")
    public ResponseEntity<List<FavoriteResponse>> getFavorites() {
        List<FavoriteResponse> favorites = favoriteService.getUserFavorites();
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/{foodId}")
    @Operation(summary = "Add food to favorites", description = "Adds a food item to the authenticated user's favorites list")
    public ResponseEntity<FavoriteResponse> addFavorite(@PathVariable Long foodId) {
        FavoriteResponse response = favoriteService.addFavorite(foodId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{foodId}")
    @Operation(summary = "Remove food from favorites", description = "Removes a food item from the authenticated user's favorites list")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long foodId) {
        favoriteService.removeFavorite(foodId);
        return ResponseEntity.noContent().build();
    }
}

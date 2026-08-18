package com.foodflow.controller;

import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@Tag(name = "Foods", description = "Endpoints for viewing and managing food menu items")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    @Operation(summary = "Get all food items", description = "Retrieves a list of all food items")
    public ResponseEntity<List<FoodResponse>> getAllFoods() {
        List<FoodResponse> foods = foodService.getAllFoods();
        return ResponseEntity.ok(foods);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID", description = "Retrieves food item details by its database ID")
    public ResponseEntity<FoodResponse> getFoodById(@PathVariable Long id) {
        FoodResponse food = foodService.getFoodById(id);
        return ResponseEntity.ok(food);
    }

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create food item", description = "Creates a new food menu item")
    public ResponseEntity<FoodResponse> createFood(@Valid @RequestBody FoodRequest request) {
        FoodResponse response = foodService.createFood(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update food item", description = "Updates an existing food menu item by ID")
    public ResponseEntity<FoodResponse> updateFood(@PathVariable Long id, @Valid @RequestBody FoodRequest request) {
        FoodResponse response = foodService.updateFood(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete food item", description = "Deletes a food menu item by ID")
    public ResponseEntity<Void> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }
}

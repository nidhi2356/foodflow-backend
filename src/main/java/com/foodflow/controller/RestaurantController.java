package com.foodflow.controller;

import com.foodflow.dto.FoodResponse;
import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.service.FoodService;
import com.foodflow.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@Tag(name = "Restaurants", description = "Endpoints for viewing and managing restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final FoodService foodService;

    public RestaurantController(RestaurantService restaurantService, FoodService foodService) {
        this.restaurantService = restaurantService;
        this.foodService = foodService;
    }

    @GetMapping
    @Operation(summary = "Get all restaurants", description = "Retrieves a list of all registered restaurants")
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Retrieves details of a restaurant by its database ID")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurant);
    }

    @GetMapping("/{id}/foods")
    @Operation(summary = "Get foods by restaurant ID", description = "Retrieves all menu food items for a specific restaurant")
    public ResponseEntity<List<FoodResponse>> getFoodsByRestaurantId(@PathVariable Long id) {
        List<FoodResponse> foods = foodService.getFoodsByRestaurant(id);
        return ResponseEntity.ok(foods);
    }

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a new restaurant", description = "Creates a new restaurant entry")
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        RestaurantResponse response = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update restaurant", description = "Updates an existing restaurant entry by ID")
    public ResponseEntity<RestaurantResponse> updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantRequest request) {
        RestaurantResponse response = restaurantService.updateRestaurant(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete restaurant", description = "Deletes a restaurant by its database ID")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}

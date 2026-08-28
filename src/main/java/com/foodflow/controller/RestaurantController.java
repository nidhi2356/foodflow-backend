package com.foodflow.controller;

import com.foodflow.dto.FoodResponse;
import com.foodflow.dto.OrderResponse;
import com.foodflow.dto.RestaurantPatchRequest;
import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ErrorResponse;
import com.foodflow.service.FoodService;
import com.foodflow.service.OrderService;
import com.foodflow.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@Tag(name = "Restaurants", description = "Endpoints for viewing, creating, and managing restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final FoodService foodService;
    private final OrderService orderService;

    public RestaurantController(
            RestaurantService restaurantService,
            FoodService foodService,
            OrderService orderService
    ) {
        this.restaurantService = restaurantService;
        this.foodService = foodService;
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Get all restaurants", description = "Retrieves a list of all registered restaurants")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of restaurants",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestaurantResponse.class))))
    })
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Retrieves details of a restaurant by its database primary key ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved restaurant",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurant);
    }

    @GetMapping("/{id}/foods")
    @Operation(summary = "Get foods by restaurant ID", description = "Retrieves all menu food items for a specific restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved restaurant food items",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FoodResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<FoodResponse>> getFoodsByRestaurantId(@PathVariable Long id) {
        List<FoodResponse> foods = foodService.getFoodsByRestaurant(id);
        return ResponseEntity.ok(foods);
    }

    @GetMapping("/{id}/orders")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get incoming orders for a restaurant", description = "Retrieves all incoming orders for a restaurant. Accessible only by the authenticated restaurant owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved restaurant orders",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own the restaurant",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<OrderResponse>> getRestaurantOrders(@PathVariable Long id) {
        List<OrderResponse> orders = orderService.getRestaurantOrders(id);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create a new restaurant", description = "Creates a new restaurant with auto-generated ID, assigning the authenticated user as owner")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurant created successfully",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        RestaurantResponse response = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Partially update restaurant", description = "Flexible partial update supporting one, multiple, or all editable fields. Empty request body is rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant updated successfully",
                    content = @Content(schema = @Schema(implementation = RestaurantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Empty body or invalid field values provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RestaurantResponse> patchRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RestaurantPatchRequest request
    ) {
        if (request == null) {
            throw new ApiException("Patch request body cannot be empty", HttpStatus.BAD_REQUEST);
        }
        RestaurantResponse response = restaurantService.patchRestaurant(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete restaurant", description = "Deletes a restaurant by its database ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Restaurant deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}

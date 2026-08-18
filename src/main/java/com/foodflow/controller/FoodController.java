package com.foodflow.controller;

import com.foodflow.dto.FoodPatchRequest;
import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ErrorResponse;
import com.foodflow.service.FoodService;
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
@RequestMapping("/api/foods")
@Tag(name = "Foods", description = "Endpoints for viewing, creating, and managing food menu items")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    @Operation(summary = "Get all food items", description = "Retrieves a list of all food items")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved food items",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FoodResponse.class))))
    })
    public ResponseEntity<List<FoodResponse>> getAllFoods() {
        List<FoodResponse> foods = foodService.getAllFoods();
        return ResponseEntity.ok(foods);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID", description = "Retrieves food item details by its database ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved food item",
                    content = @Content(schema = @Schema(implementation = FoodResponse.class))),
            @ApiResponse(responseCode = "404", description = "Food item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FoodResponse> getFoodById(@PathVariable Long id) {
        FoodResponse food = foodService.getFoodById(id);
        return ResponseEntity.ok(food);
    }

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create food item", description = "Creates a new food menu item associated with an existing restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Food item created successfully",
                    content = @Content(schema = @Schema(implementation = FoodResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Restaurant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FoodResponse> createFood(@Valid @RequestBody FoodRequest request) {
        FoodResponse response = foodService.createFood(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Partially update food item", description = "Flexible partial update supporting one, multiple, or all editable fields. Database ID and restaurant relationship cannot be modified via PATCH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Food item updated successfully",
                    content = @Content(schema = @Schema(implementation = FoodResponse.class))),
            @ApiResponse(responseCode = "400", description = "Empty body or invalid field values provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Food item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FoodResponse> patchFood(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) FoodPatchRequest request
    ) {
        if (request == null) {
            throw new ApiException("Patch request body cannot be empty", HttpStatus.BAD_REQUEST);
        }
        FoodResponse response = foodService.patchFood(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Delete food item", description = "Deletes a food menu item by its database ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Food item deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Food item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
        return ResponseEntity.noContent().build();
    }
}

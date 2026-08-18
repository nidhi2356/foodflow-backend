package com.foodflow.service;

import com.foodflow.dto.FoodPatchRequest;
import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.entity.FoodItem;
import com.foodflow.entity.Restaurant;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private FoodService foodService;

    private Restaurant sampleRestaurant;
    private FoodItem sampleFood;

    @BeforeEach
    void setUp() {
        sampleRestaurant = Restaurant.builder()
                .id(1L)
                .restaurantName("Green Bowl")
                .build();

        sampleFood = FoodItem.builder()
                .id(10L)
                .itemId("m004")
                .name("Grilled Paneer Protein Bowl")
                .description("Healthy bowl")
                .category("Bowl")
                .price(350.0)
                .isVeg(true)
                .spiceLevel("Mild")
                .dietaryTags("High Protein")
                .restaurant(sampleRestaurant)
                .build();
    }

    @Test
    @DisplayName("Should retrieve all food items")
    void shouldGetAllFoods() {
        when(foodItemRepository.findAll()).thenReturn(List.of(sampleFood));

        List<FoodResponse> result = foodService.getAllFoods();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Grilled Paneer Protein Bowl");
        assertThat(result.get(0).getRestaurantName()).isEqualTo("Green Bowl");
    }

    @Test
    @DisplayName("Should retrieve food by id")
    void shouldGetFoodById() {
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));

        FoodResponse result = foodService.getFoodById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPrice()).isEqualTo(350.0);
        assertThat(result.getItemId()).isEqualTo("m004");
    }

    @Test
    @DisplayName("Should create food item successfully with restaurant relationship")
    void shouldCreateFood() {
        FoodRequest request = FoodRequest.builder()
                .itemId("m005")
                .name("Tofu Stir Fry")
                .price(299.0)
                .isVeg(true)
                .restaurantId(1L)
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(sampleFood);

        FoodResponse response = foodService.createFood(request);

        assertThat(response).isNotNull();
        verify(foodItemRepository, times(1)).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should PATCH single field (price: 600)")
    void shouldPatchSingleFieldPrice() {
        FoodPatchRequest patch = FoodPatchRequest.builder()
                .price(600.0)
                .build();

        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(sampleFood);

        FoodResponse response = foodService.patchFood(10L, patch);

        assertThat(response).isNotNull();
        assertThat(sampleFood.getPrice()).isEqualTo(600.0);
        assertThat(sampleFood.getName()).isEqualTo("Grilled Paneer Protein Bowl"); // unchanged
        assertThat(sampleFood.getRestaurant().getId()).isEqualTo(1L); // immutable restaurant relationship
    }

    @Test
    @DisplayName("Should PATCH multiple fields (name and price)")
    void shouldPatchMultipleFieldsNameAndPrice() {
        FoodPatchRequest patch = FoodPatchRequest.builder()
                .name("Paneer Tikka Special")
                .price(550.0)
                .build();

        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(sampleFood);

        FoodResponse response = foodService.patchFood(10L, patch);

        assertThat(response).isNotNull();
        assertThat(sampleFood.getName()).isEqualTo("Paneer Tikka Special");
        assertThat(sampleFood.getPrice()).isEqualTo(550.0);
        assertThat(sampleFood.getDescription()).isEqualTo("Healthy bowl"); // unchanged
    }

    @Test
    @DisplayName("Should PATCH all editable fields")
    void shouldPatchAllFields() {
        FoodPatchRequest patch = FoodPatchRequest.builder()
                .itemId("m004-v2")
                .name("Paneer Tikka Protein Bowl")
                .description("High protein healthy meal")
                .category("Healthy")
                .price(600.0)
                .isVeg(true)
                .spiceLevel("Medium")
                .dietaryTags("Vegetarian, High Protein")
                .build();

        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(sampleFood);

        FoodResponse response = foodService.patchFood(10L, patch);

        assertThat(response).isNotNull();
        assertThat(sampleFood.getItemId()).isEqualTo("m004-v2");
        assertThat(sampleFood.getName()).isEqualTo("Paneer Tikka Protein Bowl");
        assertThat(sampleFood.getDescription()).isEqualTo("High protein healthy meal");
        assertThat(sampleFood.getCategory()).isEqualTo("Healthy");
        assertThat(sampleFood.getPrice()).isEqualTo(600.0);
        assertThat(sampleFood.getIsVeg()).isTrue();
        assertThat(sampleFood.getSpiceLevel()).isEqualTo("Medium");
        assertThat(sampleFood.getDietaryTags()).isEqualTo("Vegetarian, High Protein");
    }

    @Test
    @DisplayName("Should throw ApiException 400 when PATCH payload is empty {}")
    void shouldThrow400OnEmptyPatch() {
        FoodPatchRequest emptyPatch = FoodPatchRequest.builder().build();

        assertThatThrownBy(() -> foodService.patchFood(10L, emptyPatch))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be empty");

        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should throw ApiException 400 when PATCH price is negative")
    void shouldThrow400OnNegativePrice() {
        FoodPatchRequest invalidPatch = FoodPatchRequest.builder()
                .price(-50.0)
                .build();

        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));

        assertThatThrownBy(() -> foodService.patchFood(10L, invalidPatch))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Price must be greater than or equal to 0");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patching nonexistent food")
    void shouldThrow404WhenPatchingNonexistent() {
        FoodPatchRequest patch = FoodPatchRequest.builder()
                .price(400.0)
                .build();

        when(foodItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> foodService.patchFood(999L, patch))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

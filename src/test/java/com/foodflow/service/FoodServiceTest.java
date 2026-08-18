package com.foodflow.service;

import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.entity.FoodItem;
import com.foodflow.entity.Restaurant;
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
                .restaurantId("r001")
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
    }

    @Test
    @DisplayName("Should create food item successfully")
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
    @DisplayName("Should throw ResourceNotFoundException when creating food with invalid restaurant id")
    void shouldThrowWhenCreatingFoodWithInvalidRestaurant() {
        FoodRequest request = FoodRequest.builder()
                .name("Tofu Stir Fry")
                .price(299.0)
                .restaurantId(99L)
                .build();

        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> foodService.createFood(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

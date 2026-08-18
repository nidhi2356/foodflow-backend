package com.foodflow.service;

import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.entity.FoodItem;
import com.foodflow.entity.Restaurant;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodService {

    private static final Logger log = LoggerFactory.getLogger(FoodService.class);

    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;

    public FoodService(FoodItemRepository foodItemRepository, RestaurantRepository restaurantRepository) {
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> getAllFoods() {
        return foodItemRepository.findAll().stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodResponse getFoodById(Long id) {
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
        return mapToFoodResponse(foodItem);
    }

    @Transactional(readOnly = true)
    public FoodItem getFoodEntityById(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> getFoodsByRestaurant(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", "id", restaurantId);
        }
        return foodItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> getFoodsByRestaurantExternalId(String restaurantExternalId) {
        return foodItemRepository.findByRestaurant_RestaurantId(restaurantExternalId).stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FoodResponse createFood(FoodRequest request) {
        log.info("Creating food item: {}", request.getName());

        Restaurant restaurant = null;
        if (request.getRestaurantId() != null) {
            restaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));
        }

        FoodItem foodItem = FoodItem.builder()
                .itemId(request.getItemId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .isVeg(request.getIsVeg() != null ? request.getIsVeg() : true)
                .spiceLevel(request.getSpiceLevel())
                .dietaryTags(request.getDietaryTags())
                .restaurant(restaurant)
                .build();

        FoodItem saved = foodItemRepository.save(foodItem);
        return mapToFoodResponse(saved);
    }

    @Transactional
    public FoodResponse updateFood(Long id, FoodRequest request) {
        log.info("Updating food item id: {}", id);

        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));

        Restaurant restaurant = foodItem.getRestaurant();
        if (request.getRestaurantId() != null) {
            if (restaurant == null || !request.getRestaurantId().equals(restaurant.getId())) {
                restaurant = restaurantRepository.findById(request.getRestaurantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));
            }
        }

        foodItem.setItemId(request.getItemId());
        foodItem.setName(request.getName().trim());
        foodItem.setDescription(request.getDescription());
        foodItem.setCategory(request.getCategory());
        foodItem.setPrice(request.getPrice());
        if (request.getIsVeg() != null) {
            foodItem.setIsVeg(request.getIsVeg());
        }
        foodItem.setSpiceLevel(request.getSpiceLevel());
        foodItem.setDietaryTags(request.getDietaryTags());
        foodItem.setRestaurant(restaurant);

        FoodItem updated = foodItemRepository.save(foodItem);
        return mapToFoodResponse(updated);
    }

    @Transactional
    public void deleteFood(Long id) {
        log.info("Deleting food item id: {}", id);
        if (!foodItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("FoodItem", "id", id);
        }
        foodItemRepository.deleteById(id);
    }

    public FoodResponse mapToFoodResponse(FoodItem foodItem) {
        Restaurant restaurant = foodItem.getRestaurant();
        return FoodResponse.builder()
                .id(foodItem.getId())
                .itemId(foodItem.getItemId())
                .name(foodItem.getName())
                .description(foodItem.getDescription())
                .category(foodItem.getCategory())
                .price(foodItem.getPrice())
                .isVeg(foodItem.getIsVeg())
                .spiceLevel(foodItem.getSpiceLevel())
                .dietaryTags(foodItem.getDietaryTags())
                .restaurantId(restaurant != null ? restaurant.getId() : null)
                .restaurantName(restaurant != null ? restaurant.getRestaurantName() : null)
                .restaurantExternalId(restaurant != null ? restaurant.getRestaurantId() : null)
                .createdAt(foodItem.getCreatedAt())
                .build();
    }
}

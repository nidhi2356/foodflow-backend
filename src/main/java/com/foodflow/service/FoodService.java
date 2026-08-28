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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.foodflow.config.RedisConfig.FOODS_CACHE;
import static com.foodflow.config.RedisConfig.RESTAURANT_FOODS_CACHE;

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
    @Cacheable(value = FOODS_CACHE, key = "'all'")
    public List<FoodResponse> getAllFoods() {
        return foodItemRepository.findAll().stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = FOODS_CACHE, key = "#id")
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
    @Cacheable(value = RESTAURANT_FOODS_CACHE, key = "#restaurantId")
    public List<FoodResponse> getFoodsByRestaurant(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", "id", restaurantId);
        }
        return foodItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = FOODS_CACHE, allEntries = true),
            @CacheEvict(value = RESTAURANT_FOODS_CACHE, allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = FOODS_CACHE, allEntries = true),
            @CacheEvict(value = RESTAURANT_FOODS_CACHE, allEntries = true)
    })
    public FoodResponse patchFood(Long id, FoodPatchRequest request) {
        log.info("Patching food item id: {}", id);

        if (request == null || !request.hasUpdates()) {
            throw new ApiException("Patch request body cannot be empty", HttpStatus.BAD_REQUEST);
        }

        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", id));

        if (request.getItemId() != null) {
            foodItem.setItemId(request.getItemId().trim());
        }

        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new ApiException("Food name cannot be blank", HttpStatus.BAD_REQUEST);
            }
            foodItem.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            foodItem.setDescription(request.getDescription().trim());
        }

        if (request.getCategory() != null) {
            foodItem.setCategory(request.getCategory().trim());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0.0) {
                throw new ApiException("Price must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
            }
            foodItem.setPrice(request.getPrice());
        }

        if (request.getIsVeg() != null) {
            foodItem.setIsVeg(request.getIsVeg());
        }

        if (request.getSpiceLevel() != null) {
            foodItem.setSpiceLevel(request.getSpiceLevel().trim());
        }

        if (request.getDietaryTags() != null) {
            foodItem.setDietaryTags(request.getDietaryTags().trim());
        }

        FoodItem updated = foodItemRepository.save(foodItem);
        return mapToFoodResponse(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = FOODS_CACHE, allEntries = true),
            @CacheEvict(value = RESTAURANT_FOODS_CACHE, allEntries = true)
    })
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
                .createdAt(foodItem.getCreatedAt())
                .build();
    }
}

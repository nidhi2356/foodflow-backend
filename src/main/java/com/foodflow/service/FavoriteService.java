package com.foodflow.service;

import com.foodflow.dto.FavoriteResponse;
import com.foodflow.dto.FoodResponse;
import com.foodflow.entity.Favorite;
import com.foodflow.entity.FoodItem;
import com.foodflow.entity.User;
import com.foodflow.exception.DuplicateResourceException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FavoriteRepository;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.UserRepository;
import com.foodflow.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

    private final FavoriteRepository favoriteRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;
    private final FoodService foodService;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            FoodItemRepository foodItemRepository,
            UserRepository userRepository,
            FoodService foodService
    ) {
        this.favoriteRepository = favoriteRepository;
        this.foodItemRepository = foodItemRepository;
        this.userRepository = userRepository;
        this.foodService = foodService;
    }

    @Transactional
    public FavoriteResponse addFavorite(Long foodItemId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("User '{}' adding favorite for food item id: {}", username, foodItemId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", foodItemId));

        if (favoriteRepository.existsByUserIdAndFoodItemId(user.getId(), foodItem.getId())) {
            throw new DuplicateResourceException("Food item '" + foodItem.getName() + "' is already in your favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .foodItem(foodItem)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        return mapToFavoriteResponse(saved);
    }

    @Transactional
    public void removeFavorite(Long foodItemId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("User '{}' removing favorite for food item id: {}", username, foodItemId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Favorite favorite = favoriteRepository.findByUserIdAndFoodItemId(user.getId(), foodItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found for food item id: " + foodItemId));

        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getUserFavorites() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Fetching favorites for user: {}", username);

        List<Favorite> favorites = favoriteRepository.findAllWithFoodDetailsByUsername(username);
        return favorites.stream()
                .map(this::mapToFavoriteResponse)
                .collect(Collectors.toList());
    }

    private FavoriteResponse mapToFavoriteResponse(Favorite favorite) {
        FoodResponse foodResponse = foodService.mapToFoodResponse(favorite.getFoodItem());
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .foodItem(foodResponse)
                .favoritedAt(favorite.getCreatedAt())
                .build();
    }
}

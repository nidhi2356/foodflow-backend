package com.foodflow.service;

import com.foodflow.dto.RestaurantPatchRequest;
import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.entity.Restaurant;
import com.foodflow.entity.User;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.RestaurantRepository;
import com.foodflow.repository.UserRepository;
import com.foodflow.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return mapToRestaurantResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public Restaurant getRestaurantEntityById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        log.info("Creating restaurant: {}", request.getRestaurantName());

        String username = SecurityUtils.getCurrentUsername();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Restaurant restaurant = Restaurant.builder()
                .restaurantName(request.getRestaurantName().trim())
                .location(request.getLocation())
                .cuisine(request.getCuisine())
                .rating(request.getRating())
                .owner(owner)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(saved);
    }

    @Transactional
    public RestaurantResponse patchRestaurant(Long id, RestaurantPatchRequest request) {
        log.info("Patching restaurant id: {}", id);

        if (request == null || !request.hasUpdates()) {
            throw new ApiException("Patch request body cannot be empty", HttpStatus.BAD_REQUEST);
        }

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));

        if (request.getRestaurantName() != null) {
            if (request.getRestaurantName().trim().isEmpty()) {
                throw new ApiException("Restaurant name cannot be blank", HttpStatus.BAD_REQUEST);
            }
            restaurant.setRestaurantName(request.getRestaurantName().trim());
        }

        if (request.getLocation() != null) {
            restaurant.setLocation(request.getLocation().trim());
        }

        if (request.getCuisine() != null) {
            restaurant.setCuisine(request.getCuisine().trim());
        }

        if (request.getRating() != null) {
            if (request.getRating() < 0.0 || request.getRating() > 5.0) {
                throw new ApiException("Rating must be between 0.0 and 5.0", HttpStatus.BAD_REQUEST);
            }
            restaurant.setRating(request.getRating());
        }

        Restaurant updated = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(updated);
    }

    @Transactional
    public void deleteRestaurant(Long id) {
        log.info("Deleting restaurant id: {}", id);
        if (!restaurantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant", "id", id);
        }
        restaurantRepository.deleteById(id);
    }

    public RestaurantResponse mapToRestaurantResponse(Restaurant restaurant) {
        User owner = restaurant.getOwner();
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .restaurantName(restaurant.getRestaurantName())
                .location(restaurant.getLocation())
                .cuisine(restaurant.getCuisine())
                .rating(restaurant.getRating())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerUsername(owner != null ? owner.getUsername() : null)
                .foodItemCount(restaurant.getFoodItems() != null ? restaurant.getFoodItems().size() : 0)
                .createdAt(restaurant.getCreatedAt())
                .build();
    }
}

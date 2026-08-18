package com.foodflow.service;

import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.entity.Restaurant;
import com.foodflow.exception.DuplicateResourceException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
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

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantByExternalId(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "restaurantId", restaurantId));
        return mapToRestaurantResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        log.info("Creating restaurant: {}", request.getRestaurantName());

        if (request.getRestaurantId() != null && !request.getRestaurantId().isBlank()) {
            if (restaurantRepository.existsByRestaurantId(request.getRestaurantId().trim())) {
                throw new DuplicateResourceException("Restaurant with external ID '" + request.getRestaurantId() + "' already exists");
            }
        }

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(request.getRestaurantId() != null ? request.getRestaurantId().trim() : null)
                .restaurantName(request.getRestaurantName().trim())
                .location(request.getLocation())
                .cuisine(request.getCuisine())
                .rating(request.getRating())
                .priceRange(request.getPriceRange())
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(saved);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(Long id, RestaurantRequest request) {
        log.info("Updating restaurant id: {}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));

        if (request.getRestaurantId() != null && !request.getRestaurantId().isBlank()) {
            if (!request.getRestaurantId().equals(restaurant.getRestaurantId())
                    && restaurantRepository.existsByRestaurantId(request.getRestaurantId())) {
                throw new DuplicateResourceException("Restaurant with external ID '" + request.getRestaurantId() + "' already exists");
            }
            restaurant.setRestaurantId(request.getRestaurantId().trim());
        }

        restaurant.setRestaurantName(request.getRestaurantName().trim());
        restaurant.setLocation(request.getLocation());
        restaurant.setCuisine(request.getCuisine());
        restaurant.setRating(request.getRating());
        restaurant.setPriceRange(request.getPriceRange());

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
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .restaurantId(restaurant.getRestaurantId())
                .restaurantName(restaurant.getRestaurantName())
                .location(restaurant.getLocation())
                .cuisine(restaurant.getCuisine())
                .rating(restaurant.getRating())
                .priceRange(restaurant.getPriceRange())
                .foodItemCount(restaurant.getFoodItems() != null ? restaurant.getFoodItems().size() : 0)
                .createdAt(restaurant.getCreatedAt())
                .build();
    }
}

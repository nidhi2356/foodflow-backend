package com.foodflow.service;

import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.entity.Restaurant;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    private Restaurant sampleRestaurant;

    @BeforeEach
    void setUp() {
        sampleRestaurant = Restaurant.builder()
                .id(1L)
                .restaurantId("r001")
                .restaurantName("Green Bowl")
                .location("Saket, Delhi")
                .cuisine("Healthy, Continental")
                .rating(4.6)
                .priceRange("₹₹")
                .foodItems(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should retrieve all restaurants")
    void shouldGetAllRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(List.of(sampleRestaurant));

        List<RestaurantResponse> result = restaurantService.getAllRestaurants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestaurantName()).isEqualTo("Green Bowl");
    }

    @Test
    @DisplayName("Should retrieve restaurant by id")
    void shouldGetRestaurantById() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));

        RestaurantResponse result = restaurantService.getRestaurantById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRestaurantName()).isEqualTo("Green Bowl");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant not found")
    void shouldThrowWhenRestaurantNotFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should create a restaurant successfully")
    void shouldCreateRestaurant() {
        RestaurantRequest request = RestaurantRequest.builder()
                .restaurantId("r002")
                .restaurantName("Green Bowl")
                .location("Saket, Delhi")
                .cuisine("Healthy")
                .rating(4.5)
                .build();

        when(restaurantRepository.existsByRestaurantId("r002")).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(sampleRestaurant);

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertThat(response).isNotNull();
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }
}

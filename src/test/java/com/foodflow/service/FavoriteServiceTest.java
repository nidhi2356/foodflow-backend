package com.foodflow.service;

import com.foodflow.dto.FavoriteResponse;
import com.foodflow.dto.FoodResponse;
import com.foodflow.entity.Favorite;
import com.foodflow.entity.FoodItem;
import com.foodflow.entity.Restaurant;
import com.foodflow.entity.Role;
import com.foodflow.entity.User;
import com.foodflow.exception.DuplicateResourceException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FavoriteRepository;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FoodService foodService;

    @InjectMocks
    private FavoriteService favoriteService;

    private User sampleUser;
    private FoodItem sampleFood;
    private Restaurant sampleRestaurant;
    private Favorite sampleFavorite;

    @BeforeEach
    void setUp() {
        // Set security context
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("testuser", "pass", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@foodflow.com")
                .role(Role.ROLE_USER)
                .build();

        sampleRestaurant = Restaurant.builder()
                .id(1L)
                .restaurantName("Green Bowl")
                .build();

        sampleFood = FoodItem.builder()
                .id(10L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .restaurant(sampleRestaurant)
                .build();

        sampleFavorite = Favorite.builder()
                .id(100L)
                .user(sampleUser)
                .foodItem(sampleFood)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully add a food item to user favorites")
    void shouldAddFavoriteSuccessfully() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));
        when(favoriteRepository.existsByUserIdAndFoodItemId(1L, 10L)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(sampleFavorite);
        when(foodService.mapToFoodResponse(sampleFood)).thenReturn(FoodResponse.builder().id(10L).name("Grilled Paneer Protein Bowl").build());

        FavoriteResponse response = favoriteService.addFavorite(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getFoodItem().getName()).isEqualTo("Grilled Paneer Protein Bowl");
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should prevent duplicate favorite and throw DuplicateResourceException")
    void shouldPreventDuplicateFavorite() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(sampleFood));
        when(favoriteRepository.existsByUserIdAndFoodItemId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(10L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in your favorites");

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should remove favorite successfully")
    void shouldRemoveFavoriteSuccessfully() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(favoriteRepository.findByUserIdAndFoodItemId(1L, 10L)).thenReturn(Optional.of(sampleFavorite));

        favoriteService.removeFavorite(10L);

        verify(favoriteRepository, times(1)).delete(sampleFavorite);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when removing nonexistent favorite")
    void shouldThrowWhenRemovingNonexistentFavorite() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(favoriteRepository.findByUserIdAndFoodItemId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    @DisplayName("Should return list of user favorites")
    void shouldReturnUserFavorites() {
        when(favoriteRepository.findAllWithFoodDetailsByUsername("testuser")).thenReturn(List.of(sampleFavorite));
        when(foodService.mapToFoodResponse(sampleFood)).thenReturn(FoodResponse.builder().id(10L).name("Grilled Paneer").build());

        List<FavoriteResponse> result = favoriteService.getUserFavorites();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }
}

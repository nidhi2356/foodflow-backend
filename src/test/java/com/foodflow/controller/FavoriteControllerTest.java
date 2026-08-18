package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.FavoriteResponse;
import com.foodflow.dto.FoodResponse;
import com.foodflow.exception.DuplicateResourceException;
import com.foodflow.security.JwtService;
import com.foodflow.service.FavoriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/favorites should return user's favorite foods")
    void getFavoritesShouldReturnList() throws Exception {
        FoodResponse food = FoodResponse.builder()
                .id(10L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .build();

        FavoriteResponse response = FavoriteResponse.builder()
                .id(1L)
                .foodItem(food)
                .favoritedAt(LocalDateTime.now())
                .build();

        when(favoriteService.getUserFavorites()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foodItem.name").value("Grilled Paneer Protein Bowl"));
    }

    @Test
    @DisplayName("POST /api/favorites/{foodId} should add favorite and return 201")
    void addFavoriteShouldReturnCreated() throws Exception {
        FoodResponse food = FoodResponse.builder()
                .id(10L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .build();

        FavoriteResponse response = FavoriteResponse.builder()
                .id(1L)
                .foodItem(food)
                .favoritedAt(LocalDateTime.now())
                .build();

        when(favoriteService.addFavorite(10L)).thenReturn(response);

        mockMvc.perform(post("/api/favorites/10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.foodItem.name").value("Grilled Paneer Protein Bowl"));
    }

    @Test
    @DisplayName("POST /api/favorites/{foodId} should return 409 Conflict when duplicate")
    void addFavoriteShouldReturnConflictWhenDuplicate() throws Exception {
        when(favoriteService.addFavorite(10L))
                .thenThrow(new DuplicateResourceException("Food item 'Grilled Paneer Protein Bowl' is already in your favorites"));

        mockMvc.perform(post("/api/favorites/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already in your favorites")));
    }

    @Test
    @DisplayName("DELETE /api/favorites/{foodId} should remove favorite and return 204")
    void removeFavoriteShouldReturn204() throws Exception {
        doNothing().when(favoriteService).removeFavorite(10L);

        mockMvc.perform(delete("/api/favorites/10"))
                .andExpect(status().isNoContent());
    }
}

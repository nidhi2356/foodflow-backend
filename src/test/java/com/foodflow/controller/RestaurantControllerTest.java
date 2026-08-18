package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.FoodResponse;
import com.foodflow.dto.RestaurantRequest;
import com.foodflow.dto.RestaurantResponse;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.security.JwtService;
import com.foodflow.service.FoodService;
import com.foodflow.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RestaurantController.class)
@AutoConfigureMockMvc(addFilters = false)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantService restaurantService;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/restaurants should return list of restaurants")
    void getAllRestaurantsShouldReturnList() throws Exception {
        RestaurantResponse response = RestaurantResponse.builder()
                .id(1L)
                .restaurantId("r001")
                .restaurantName("Green Bowl")
                .location("Saket, Delhi")
                .cuisine("Healthy")
                .rating(4.6)
                .build();

        when(restaurantService.getAllRestaurants()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantName").value("Green Bowl"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id} should return restaurant by id")
    void getRestaurantByIdShouldReturnRestaurant() throws Exception {
        RestaurantResponse response = RestaurantResponse.builder()
                .id(1L)
                .restaurantName("Green Bowl")
                .build();

        when(restaurantService.getRestaurantById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantName").value("Green Bowl"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id} should return 404 when not found")
    void getRestaurantByIdShouldReturn404WhenNotFound() throws Exception {
        when(restaurantService.getRestaurantById(99L))
                .thenThrow(new ResourceNotFoundException("Restaurant", "id", 99L));

        mockMvc.perform(get("/api/restaurants/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/foods should return restaurant menu items")
    void getFoodsByRestaurantShouldReturnList() throws Exception {
        FoodResponse food = FoodResponse.builder()
                .id(10L)
                .name("Paneer Bowl")
                .price(300.0)
                .build();

        when(foodService.getFoodsByRestaurant(1L)).thenReturn(List.of(food));

        mockMvc.perform(get("/api/restaurants/1/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Paneer Bowl"));
    }

    @Test
    @DisplayName("POST /api/restaurants should create restaurant and return 201")
    void createRestaurantShouldReturn201() throws Exception {
        RestaurantRequest request = RestaurantRequest.builder()
                .restaurantName("Green Bowl")
                .rating(4.5)
                .build();

        RestaurantResponse response = RestaurantResponse.builder()
                .id(1L)
                .restaurantName("Green Bowl")
                .rating(4.5)
                .build();

        when(restaurantService.createRestaurant(any(RestaurantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantName").value("Green Bowl"));
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{id} should return 204 No Content")
    void deleteRestaurantShouldReturn204() throws Exception {
        doNothing().when(restaurantService).deleteRestaurant(1L);

        mockMvc.perform(delete("/api/restaurants/1"))
                .andExpect(status().isNoContent());
    }
}

package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.FoodRequest;
import com.foodflow.dto.FoodResponse;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.security.JwtService;
import com.foodflow.service.FoodService;
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

@WebMvcTest(controllers = FoodController.class)
@AutoConfigureMockMvc(addFilters = false)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/foods should return list of food items")
    void getAllFoodsShouldReturnList() throws Exception {
        FoodResponse food = FoodResponse.builder()
                .id(1L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .isVeg(true)
                .build();

        when(foodService.getAllFoods()).thenReturn(List.of(food));

        mockMvc.perform(get("/api/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Grilled Paneer Protein Bowl"))
                .andExpect(jsonPath("$[0].price").value(350.0));
    }

    @Test
    @DisplayName("GET /api/foods/{id} should return food item by id")
    void getFoodByIdShouldReturnFood() throws Exception {
        FoodResponse food = FoodResponse.builder()
                .id(1L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .build();

        when(foodService.getFoodById(1L)).thenReturn(food);

        mockMvc.perform(get("/api/foods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grilled Paneer Protein Bowl"));
    }

    @Test
    @DisplayName("GET /api/foods/{id} should return 404 when not found")
    void getFoodByIdShouldReturn404WhenNotFound() throws Exception {
        when(foodService.getFoodById(99L))
                .thenThrow(new ResourceNotFoundException("FoodItem", "id", 99L));

        mockMvc.perform(get("/api/foods/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/foods should create food item and return 201")
    void createFoodShouldReturn201() throws Exception {
        FoodRequest request = FoodRequest.builder()
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .isVeg(true)
                .build();

        FoodResponse response = FoodResponse.builder()
                .id(1L)
                .name("Grilled Paneer Protein Bowl")
                .price(350.0)
                .isVeg(true)
                .build();

        when(foodService.createFood(any(FoodRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grilled Paneer Protein Bowl"));
    }

    @Test
    @DisplayName("POST /api/foods should return 400 when validation fails (negative price)")
    void createFoodShouldReturn400OnNegativePrice() throws Exception {
        FoodRequest request = FoodRequest.builder()
                .name("Grilled Paneer Protein Bowl")
                .price(-50.0)
                .build();

        mockMvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.price").exists());
    }

    @Test
    @DisplayName("DELETE /api/foods/{id} should return 204 No Content")
    void deleteFoodShouldReturn204() throws Exception {
        doNothing().when(foodService).deleteFood(1L);

        mockMvc.perform(delete("/api/foods/1"))
                .andExpect(status().isNoContent());
    }
}

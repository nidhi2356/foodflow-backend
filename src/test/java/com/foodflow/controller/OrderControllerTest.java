package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.OrderCreateRequest;
import com.foodflow.dto.OrderItemRequest;
import com.foodflow.dto.OrderItemResponse;
import com.foodflow.dto.OrderResponse;
import com.foodflow.entity.OrderStatus;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.security.JwtService;
import com.foodflow.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/orders should create order and return 201 Created")
    void createOrderShouldReturn201() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(1L)
                .items(List.of(OrderItemRequest.builder().foodId(5L).quantity(2).build()))
                .build();

        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(101L)
                .foodId(5L)
                .foodName("Paneer Tikka Bowl")
                .quantity(2)
                .priceAtOrderTime(BigDecimal.valueOf(280.00))
                .subtotal(BigDecimal.valueOf(560.00))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .userId(10L)
                .username("john_doe")
                .restaurantId(1L)
                .restaurantName("FoodFlow Kitchen")
                .orderItems(List.of(itemResponse))
                .totalAmount(BigDecimal.valueOf(560.00))
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(560.00))
                .andExpect(jsonPath("$.orderItems[0].subtotal").value(560.00));
    }

    @Test
    @DisplayName("POST /api/orders should return 400 when missing items")
    void createOrderShouldReturn400OnEmptyItems() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(1L)
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    @DisplayName("GET /api/orders should return list of customer orders")
    void getMyOrdersShouldReturnList() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .userId(10L)
                .username("john_doe")
                .restaurantName("Green Bowl")
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.getMyOrders()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].restaurantName").value("Green Bowl"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} should return order details")
    void getOrderByIdShouldReturnOrder() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .userId(10L)
                .username("john_doe")
                .restaurantName("Green Bowl")
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} should return 403 when unauthorized")
    void getOrderByIdShouldReturn403WhenUnauthorized() throws Exception {
        when(orderService.getOrderById(99L))
                .thenThrow(new ApiException("Access denied: You do not have permission to view this order", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/orders/{id} should return 404 when not found")
    void getOrderByIdShouldReturn404WhenNotFound() throws Exception {
        when(orderService.getOrderById(99L))
                .thenThrow(new ResourceNotFoundException("Order", "id", 99L));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel should return 200 on successful cancellation")
    void cancelOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel should return 400 when order is not PENDING")
    void cancelOrderShouldReturn400WhenNotPending() throws Exception {
        when(orderService.cancelOrder(1L))
                .thenThrow(new ApiException("Cannot cancel order with status ACCEPTED", HttpStatus.BAD_REQUEST));

        mockMvc.perform(patch("/api/orders/1/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/accept should return 200")
    void acceptOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.ACCEPTED)
                .build();

        when(orderService.acceptOrder(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/reject should return 200")
    void rejectOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.REJECTED)
                .build();

        when(orderService.rejectOrder(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/preparing should return 200")
    void preparingOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.PREPARING)
                .build();

        when(orderService.markPreparing(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/preparing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/ready should return 200")
    void readyOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.READY)
                .build();

        when(orderService.markReady(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/complete should return 200")
    void completeOrderShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderService.completeOrder(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}

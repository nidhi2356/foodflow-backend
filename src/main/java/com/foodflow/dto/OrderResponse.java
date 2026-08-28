package com.foodflow.dto;

import com.foodflow.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an order response")
public class OrderResponse {

    @Schema(description = "Database primary key ID of the order", example = "1")
    private Long id;

    @Schema(description = "Database ID of the customer who placed the order", example = "10")
    private Long userId;

    @Schema(description = "Username of the customer", example = "john_doe")
    private String username;

    @Schema(description = "Database ID of the restaurant", example = "1")
    private Long restaurantId;

    @Schema(description = "Name of the restaurant", example = "FoodFlow Kitchen")
    private String restaurantName;

    @Builder.Default
    @Schema(description = "List of ordered items with prices snapshot at creation")
    private List<OrderItemResponse> orderItems = new ArrayList<>();

    @Schema(description = "Total amount of the order in INR", example = "560.00")
    private BigDecimal totalAmount;

    @Schema(description = "Current lifecycle status of the order", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Timestamp when the order was placed")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the order was last updated")
    private LocalDateTime updatedAt;
}

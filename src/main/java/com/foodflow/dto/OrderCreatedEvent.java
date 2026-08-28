package com.foodflow.dto;

import com.foodflow.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private Long userId;
    private String username;
    private Long restaurantId;
    private String restaurantName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime timestamp;
}

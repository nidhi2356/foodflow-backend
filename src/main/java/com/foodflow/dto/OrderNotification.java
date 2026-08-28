package com.foodflow.dto;

import com.foodflow.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotification {

    private Long orderId;
    private Long restaurantId;
    private Long userId;
    private OrderStatus status;
    private String message;
    private LocalDateTime timestamp;
}

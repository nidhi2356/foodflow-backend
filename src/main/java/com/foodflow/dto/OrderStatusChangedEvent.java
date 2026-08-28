package com.foodflow.dto;

import com.foodflow.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent implements Serializable {

    private Long orderId;
    private Long restaurantId;
    private Long userId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private LocalDateTime timestamp;
}

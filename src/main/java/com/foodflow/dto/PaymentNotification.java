package com.foodflow.dto;

import com.foodflow.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotification {

    private Long orderId;
    private Long paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String message;
    private LocalDateTime timestamp;
}

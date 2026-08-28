package com.foodflow.dto;

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
public class PaymentFailedEvent implements Serializable {

    private Long paymentId;
    private Long orderId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime timestamp;
}

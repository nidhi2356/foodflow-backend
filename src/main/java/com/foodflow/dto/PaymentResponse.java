package com.foodflow.dto;

import com.foodflow.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Safe payment information details")
public class PaymentResponse {

    @Schema(description = "Database primary key ID of the payment record", example = "1")
    private Long id;

    @Schema(description = "Database ID of the associated order", example = "501")
    private Long orderId;

    @Schema(description = "Razorpay order ID", example = "order_N1234567890abc")
    private String razorpayOrderId;

    @Schema(description = "Razorpay payment ID if verified", example = "pay_N1234567890xyz")
    private String razorpayPaymentId;

    @Schema(description = "Payment amount in INR", example = "560.00")
    private BigDecimal amount;

    @Schema(description = "Current payment status", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Timestamp when the payment record was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the payment was last updated")
    private LocalDateTime updatedAt;
}

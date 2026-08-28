package com.foodflow.dto;

import com.foodflow.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned when a payment order is initiated with Razorpay")
public class PaymentCreateResponse {

    @Schema(description = "Database ID of the payment record", example = "1")
    private Long paymentId;

    @Schema(description = "Database ID of the associated order", example = "501")
    private Long orderId;

    @Schema(description = "Razorpay order ID for client checkout", example = "order_N1234567890abc")
    private String razorpayOrderId;

    @Schema(description = "Server-calculated payment amount in INR", example = "560.00")
    private BigDecimal amount;

    @Builder.Default
    @Schema(description = "Payment currency", example = "INR")
    private String currency = "INR";

    @Schema(description = "Razorpay public key ID for client checkout", example = "rzp_test_mock_key")
    private String keyId;

    @Schema(description = "Current payment status", example = "CREATED")
    private PaymentStatus status;
}

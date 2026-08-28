package com.foodflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for verifying a completed Razorpay payment")
public class PaymentVerifyRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Database ID of the order being paid", example = "501", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotBlank(message = "Razorpay order ID is required")
    @Schema(description = "Razorpay order ID received during checkout initiation", example = "order_N1234567890abc", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    @Schema(description = "Razorpay payment ID returned after checkout completion", example = "pay_N1234567890xyz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Schema(description = "HMAC SHA-256 signature returned by Razorpay checkout", example = "9ef2d8b4e7...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String razorpaySignature;
}

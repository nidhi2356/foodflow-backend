package com.foodflow.controller;

import com.foodflow.dto.PaymentCreateResponse;
import com.foodflow.dto.PaymentResponse;
import com.foodflow.dto.PaymentVerifyRequest;
import com.foodflow.exception.ErrorResponse;
import com.foodflow.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for Razorpay payment initiation, cryptographic verification, and status lookups")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/order/{orderId}")
    @Operation(summary = "Initiate payment for an ACCEPTED order", description = "Creates a Razorpay order with server-calculated amount from Order.totalAmount. Allowed only for ACCEPTED orders by the ordering customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment order initiated successfully",
                    content = @Content(schema = @Schema(implementation = PaymentCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Order is not in ACCEPTED state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Authenticated user does not own this order",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate payment already exists for this order",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentCreateResponse> createPayment(@PathVariable Long orderId) {
        PaymentCreateResponse response = paymentService.createPayment(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature", description = "Cryptographically verifies the Razorpay payment signature using HMAC SHA-256 with the server-side secret and transitions payment status to SUCCESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment verified successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid signature, order ID mismatch, or validation failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Authenticated user does not own this order",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order or Payment record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment status for an order", description = "Retrieves payment details for an order. Accessible by the customer who placed the order or the restaurant owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized to view this payment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order or Payment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}

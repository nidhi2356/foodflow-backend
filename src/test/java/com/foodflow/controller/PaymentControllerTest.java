package com.foodflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.dto.PaymentCreateResponse;
import com.foodflow.dto.PaymentResponse;
import com.foodflow.dto.PaymentVerifyRequest;
import com.foodflow.entity.PaymentStatus;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.security.JwtService;
import com.foodflow.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/payments/order/{orderId} should initiate payment and return 201 Created")
    void createPaymentShouldReturn201() throws Exception {
        PaymentCreateResponse response = PaymentCreateResponse.builder()
                .paymentId(1L)
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .amount(BigDecimal.valueOf(560.00))
                .currency("INR")
                .keyId("rzp_test_mock_key")
                .status(PaymentStatus.CREATED)
                .build();

        when(paymentService.createPayment(501L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/order/501"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1L))
                .andExpect(jsonPath("$.orderId").value(501L))
                .andExpect(jsonPath("$.razorpayOrderId").value("order_test_12345"))
                .andExpect(jsonPath("$.amount").value(560.00))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST /api/payments/order/{orderId} should return 400 when order is not ACCEPTED")
    void createPaymentShouldReturn400WhenOrderNotAccepted() throws Exception {
        when(paymentService.createPayment(501L))
                .thenThrow(new ApiException("Payment can only be initiated for ACCEPTED orders", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/payments/order/501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/payments/order/{orderId} should return 409 when payment already exists")
    void createPaymentShouldReturn409OnDuplicatePayment() throws Exception {
        when(paymentService.createPayment(501L))
                .thenThrow(new ApiException("Payment has already been completed for this order", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/payments/order/501"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/payments/verify should verify payment and return 200 OK")
    void verifyPaymentShouldReturn200() throws Exception {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("signature_hash")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .amount(BigDecimal.valueOf(560.00))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.verifyPayment(any(PaymentVerifyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_test_99999"));
    }

    @Test
    @DisplayName("POST /api/payments/verify should return 400 on invalid signature")
    void verifyPaymentShouldReturn400OnInvalidSignature() throws Exception {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("invalid_signature")
                .build();

        when(paymentService.verifyPayment(any(PaymentVerifyRequest.class)))
                .thenThrow(new ApiException("Payment verification failed: Invalid signature", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} should return payment details")
    void getPaymentByOrderIdShouldReturn200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .amount(BigDecimal.valueOf(560.00))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentByOrderId(501L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} should return 403 when unauthorized")
    void getPaymentByOrderIdShouldReturn403WhenUnauthorized() throws Exception {
        when(paymentService.getPaymentByOrderId(501L))
                .thenThrow(new ApiException("Access denied: You do not have permission to view this payment", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/payments/order/501"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/payments/order/{orderId} should return 404 when payment not found")
    void getPaymentByOrderIdShouldReturn404WhenNotFound() throws Exception {
        when(paymentService.getPaymentByOrderId(999L))
                .thenThrow(new ResourceNotFoundException("Payment", "orderId", 999L));

        mockMvc.perform(get("/api/payments/order/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

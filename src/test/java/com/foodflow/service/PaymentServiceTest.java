package com.foodflow.service;

import com.foodflow.dto.PaymentCreateResponse;
import com.foodflow.dto.PaymentResponse;
import com.foodflow.dto.PaymentVerifyRequest;
import com.foodflow.entity.*;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.OrderRepository;
import com.foodflow.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RazorpayService razorpayService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private User sampleCustomer;
    private User sampleOwner;
    private User otherCustomer;
    private Restaurant sampleRestaurant;
    private Order acceptedOrder;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        setAuthenticatedUser("john_doe");

        sampleCustomer = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .build();

        sampleOwner = User.builder()
                .id(2L)
                .username("chef_mario")
                .email("mario@example.com")
                .role(Role.ROLE_USER)
                .build();

        otherCustomer = User.builder()
                .id(3L)
                .username("alice_wonder")
                .email("alice@example.com")
                .role(Role.ROLE_USER)
                .build();

        sampleRestaurant = Restaurant.builder()
                .id(10L)
                .restaurantName("Mario Trattoria")
                .owner(sampleOwner)
                .build();

        acceptedOrder = Order.builder()
                .id(501L)
                .user(sampleCustomer)
                .restaurant(sampleRestaurant)
                .totalAmount(BigDecimal.valueOf(560.00))
                .status(OrderStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .build();

        samplePayment = Payment.builder()
                .id(1001L)
                .order(acceptedOrder)
                .razorpayOrderId("order_test_12345")
                .amount(BigDecimal.valueOf(560.00))
                .status(PaymentStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "pass", Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==========================================
    // 1. PAYMENT CREATION
    // ==========================================

    @Test
    @DisplayName("Should create payment for ACCEPTED order with server-side amount")
    void shouldCreatePaymentSuccessfully() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.empty());
        when(razorpayService.createRazorpayOrder(BigDecimal.valueOf(560.00), "rcpt_order_501"))
                .thenReturn("order_test_12345");
        when(razorpayService.getKeyId()).thenReturn("rzp_test_mock_key");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1001L);
            return p;
        });

        PaymentCreateResponse response = paymentService.createPayment(501L);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1001L);
        assertThat(response.getOrderId()).isEqualTo(501L);
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_test_12345");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(560.00));
        assertThat(response.getKeyId()).isEqualTo("rzp_test_mock_key");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    @DisplayName("Should throw 403 when creating payment for an order owned by someone else")
    void shouldThrow403WhenNonOwnerInitiatesPayment() {
        setAuthenticatedUser("alice_wonder");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder)); // owned by john_doe

        assertThatThrownBy(() -> paymentService.createPayment(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Should throw 400 when order is in PENDING status (not ACCEPTED)")
    void shouldThrow400WhenOrderIsNotAccepted() {
        acceptedOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));

        assertThatThrownBy(() -> paymentService.createPayment(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Payment can only be initiated for ACCEPTED orders");
    }

    @Test
    @DisplayName("Should throw 409 Conflict when payment is already SUCCESS for this order")
    void shouldThrow409WhenPaymentAlreadyCompleted() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.createPayment(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Payment has already been completed");
    }

    @Test
    @DisplayName("Should throw 409 Conflict when an active payment is already in progress")
    void shouldThrow409WhenActivePaymentInProgress() {
        samplePayment.setStatus(PaymentStatus.CREATED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.createPayment(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("An active payment is already in progress");
    }

    // ==========================================
    // 2. PAYMENT VERIFICATION
    // ==========================================

    @Test
    @DisplayName("Should verify valid Razorpay signature and transition status to SUCCESS")
    void shouldVerifyValidSignatureSuccessfully() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("valid_signature_hash")
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));
        when(razorpayService.verifySignature("order_test_12345", "pay_test_99999", "valid_signature_hash"))
                .thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.verifyPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getRazorpayPaymentId()).isEqualTo("pay_test_99999");

        verify(notificationService, times(1)).sendPaymentNotification(
                eq(501L), eq(1001L), eq(PaymentStatus.SUCCESS), any(BigDecimal.class), eq("Payment successful")
        );
        verify(eventPublisher, times(1)).publishPaymentSuccessful(any());
    }

    @Test
    @DisplayName("Should reject invalid signature and transition status to FAILED (throws 400)")
    void shouldFailVerificationOnInvalidSignature() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("tampered_signature")
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));
        when(razorpayService.verifySignature("order_test_12345", "pay_test_99999", "tampered_signature"))
                .thenReturn(false);

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Payment verification failed: Invalid signature");

        assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository, times(1)).save(samplePayment);
        verify(notificationService, times(1)).sendPaymentNotification(
                eq(501L), eq(1001L), eq(PaymentStatus.FAILED), any(BigDecimal.class), contains("failed")
        );
        verify(eventPublisher, times(1)).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("Should reject verification when client razorpayOrderId does not match server record")
    void shouldRejectOnRazorpayOrderIdMismatch() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_different_999") // mismatch!
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("signature")
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.verifyPayment(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Razorpay order ID mismatch");

        assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Verification is idempotent when payment is already SUCCESS with same payment ID")
    void shouldBeIdempotentForAlreadyVerifiedPayment() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        samplePayment.setRazorpayPaymentId("pay_test_99999");

        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .orderId(501L)
                .razorpayOrderId("order_test_12345")
                .razorpayPaymentId("pay_test_99999")
                .razorpaySignature("valid_signature_hash")
                .build();

        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.verifyPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(razorpayService, never()).verifySignature(any(), any(), any());
    }

    // ==========================================
    // 3. PAYMENT STATUS RETRIEVAL
    // ==========================================

    @Test
    @DisplayName("Customer can retrieve own order payment status")
    void shouldGetPaymentStatusForCustomer() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentByOrderId(501L);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(501L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(560.00));
    }

    @Test
    @DisplayName("Restaurant owner can retrieve restaurant order payment status")
    void shouldGetPaymentStatusForRestaurantOwner() {
        setAuthenticatedUser("chef_mario");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentByOrderId(501L);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("Unauthorized user cannot retrieve payment status (throws 403)")
    void shouldThrow403WhenUnauthorizedUserGetsPayment() {
        setAuthenticatedUser("alice_wonder");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(acceptedOrder));

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Access denied");
    }
}

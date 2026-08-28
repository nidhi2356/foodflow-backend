package com.foodflow.service;

import com.foodflow.dto.*;
import com.foodflow.entity.*;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.OrderRepository;
import com.foodflow.repository.PaymentRepository;
import com.foodflow.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final NotificationService notificationService;
    private final EventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            RazorpayService razorpayService,
            NotificationService notificationService,
            EventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentCreateResponse createPayment(Long orderId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Customer '{}' initiating payment for order id: {}", username, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // 1. Authorization: Only the ordering customer can initiate payment
        if (!order.getUser().getUsername().equals(username)) {
            log.warn("User '{}' unauthorized to pay for order id: {}", username, orderId);
            throw new ApiException("Access denied: You do not have permission to pay for this order", HttpStatus.FORBIDDEN);
        }

        // 2. Order status must be ACCEPTED
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            log.warn("Cannot create payment for order id: {} with status: {}", orderId, order.getStatus());
            throw new ApiException("Payment can only be initiated for ACCEPTED orders. Current status: " + order.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // 3. Duplicate payment prevention
        Optional<Payment> existingPaymentOpt = paymentRepository.findByOrderId(orderId);
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                log.warn("Order id: {} already has a successful payment", orderId);
                throw new ApiException("Payment has already been completed for this order", HttpStatus.CONFLICT);
            }
            if (existing.getStatus() == PaymentStatus.CREATED || existing.getStatus() == PaymentStatus.PENDING) {
                log.warn("Order id: {} already has an active payment initiated", orderId);
                throw new ApiException("An active payment is already in progress for this order", HttpStatus.CONFLICT);
            }
        }

        // 4. Create Razorpay order using server-side amount
        String receipt = "rcpt_order_" + order.getId();
        String razorpayOrderId = razorpayService.createRazorpayOrder(order.getTotalAmount(), receipt);

        Payment payment = existingPaymentOpt.orElseGet(() -> Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .build());

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setRazorpayPaymentId(null);
        payment.setRazorpaySignature(null);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with ID: {} for order: {}, Razorpay order ID: {}",
                savedPayment.getId(), order.getId(), razorpayOrderId);

        return PaymentCreateResponse.builder()
                .paymentId(savedPayment.getId())
                .orderId(order.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(savedPayment.getAmount())
                .currency("INR")
                .keyId(razorpayService.getKeyId())
                .status(savedPayment.getStatus())
                .build();
    }

    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Customer '{}' verifying payment for order id: {}", username, request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        // Authorization: Only customer who owns order can verify
        if (!order.getUser().getUsername().equals(username)) {
            log.warn("User '{}' unauthorized to verify payment for order id: {}", username, request.getOrderId());
            throw new ApiException("Access denied: You do not have permission to verify this payment", HttpStatus.FORBIDDEN);
        }

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", request.getOrderId()));

        // Idempotency: If already verified with same payment ID
        if (payment.getStatus() == PaymentStatus.SUCCESS &&
                request.getRazorpayPaymentId().equals(payment.getRazorpayPaymentId())) {
            log.info("Payment for order id: {} is already successfully verified (idempotent response)", request.getOrderId());
            return mapToPaymentResponse(payment);
        }

        // Server-side authoritative order ID check
        String serverOrderId = payment.getRazorpayOrderId();
        if (serverOrderId == null || !serverOrderId.equals(request.getRazorpayOrderId())) {
            log.warn("Razorpay order ID mismatch: expected {}, received {}", serverOrderId, request.getRazorpayOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException("Payment verification failed: Razorpay order ID mismatch", HttpStatus.BAD_REQUEST);
        }

        // Cryptographic verification with server secret
        boolean isValid = razorpayService.verifySignature(
                serverOrderId,
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // WebSocket & RabbitMQ notifications on failure
            notificationService.sendPaymentNotification(
                    order.getId(),
                    payment.getId(),
                    PaymentStatus.FAILED,
                    payment.getAmount(),
                    "Payment verification failed: Invalid signature"
            );

            eventPublisher.publishPaymentFailed(PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .razorpayOrderId(serverOrderId)
                    .amount(payment.getAmount())
                    .reason("Invalid signature")
                    .timestamp(LocalDateTime.now())
                    .build());

            throw new ApiException("Payment verification failed: Invalid signature", HttpStatus.BAD_REQUEST);
        }

        // Success transition
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment successfully verified for order id: {}", order.getId());

        // WebSocket real-time notification to customer
        notificationService.sendPaymentNotification(
                order.getId(),
                updatedPayment.getId(),
                PaymentStatus.SUCCESS,
                updatedPayment.getAmount(),
                "Payment successful"
        );

        // RabbitMQ asynchronous event
        eventPublisher.publishPaymentSuccessful(PaymentSuccessfulEvent.builder()
                .paymentId(updatedPayment.getId())
                .orderId(order.getId())
                .razorpayOrderId(serverOrderId)
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .amount(updatedPayment.getAmount())
                .timestamp(LocalDateTime.now())
                .build());

        return mapToPaymentResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Retrieving payment for order id: {} by user: {}", orderId, username);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        boolean isCustomer = order.getUser().getUsername().equals(username);
        boolean isRestaurantOwner = order.getRestaurant().getOwner() != null &&
                order.getRestaurant().getOwner().getUsername().equals(username);

        if (!isCustomer && !isRestaurantOwner) {
            log.warn("User '{}' unauthorized to view payment for order id: {}", username, orderId);
            throw new ApiException("Access denied: You do not have permission to view this payment", HttpStatus.FORBIDDEN);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        return mapToPaymentResponse(payment);
    }

    public PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

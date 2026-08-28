package com.foodflow.service;

import com.foodflow.dto.OrderCreatedEvent;
import com.foodflow.dto.OrderStatusChangedEvent;
import com.foodflow.dto.PaymentFailedEvent;
import com.foodflow.dto.PaymentSuccessfulEvent;
import com.foodflow.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "exchangeName", "foodflow.exchange");
    }

    @Test
    @DisplayName("Should publish OrderCreatedEvent to foodflow.exchange with order.created routing key")
    void shouldPublishOrderCreatedEvent() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(100L)
                .userId(1L)
                .username("john_doe")
                .restaurantId(10L)
                .restaurantName("Mario Trattoria")
                .totalAmount(BigDecimal.valueOf(560.00))
                .status(OrderStatus.PENDING)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishOrderCreated(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("foodflow.exchange"), eq("order.created"), eq(event));
    }

    @Test
    @DisplayName("Should publish OrderStatusChangedEvent with order.status.{status} routing key")
    void shouldPublishOrderStatusChangedEvent() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId(100L)
                .restaurantId(10L)
                .userId(1L)
                .previousStatus(OrderStatus.PENDING)
                .newStatus(OrderStatus.ACCEPTED)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishOrderStatusChanged(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("foodflow.exchange"), eq("order.status.accepted"), eq(event));
    }

    @Test
    @DisplayName("Should publish PaymentSuccessfulEvent with payment.success routing key")
    void shouldPublishPaymentSuccessfulEvent() {
        PaymentSuccessfulEvent event = PaymentSuccessfulEvent.builder()
                .paymentId(1L)
                .orderId(100L)
                .razorpayOrderId("order_test_123")
                .razorpayPaymentId("pay_test_456")
                .amount(BigDecimal.valueOf(560.00))
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPaymentSuccessful(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("foodflow.exchange"), eq("payment.success"), eq(event));
    }

    @Test
    @DisplayName("Should publish PaymentFailedEvent with payment.failed routing key")
    void shouldPublishPaymentFailedEvent() {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(1L)
                .orderId(100L)
                .razorpayOrderId("order_test_123")
                .amount(BigDecimal.valueOf(560.00))
                .reason("Invalid signature")
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishPaymentFailed(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("foodflow.exchange"), eq("payment.failed"), eq(event));
    }
}

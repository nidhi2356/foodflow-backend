package com.foodflow.service;

import com.foodflow.dto.OrderNotification;
import com.foodflow.dto.PaymentNotification;
import com.foodflow.entity.OrderStatus;
import com.foodflow.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should send order status notification to /topic/orders/{orderId}")
    void shouldSendOrderStatusNotification() {
        notificationService.sendOrderStatusNotification(100L, 10L, 1L, OrderStatus.ACCEPTED, "Order #100 accepted");

        ArgumentCaptor<OrderNotification> captor = ArgumentCaptor.forClass(OrderNotification.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/orders/100"), captor.capture());

        OrderNotification notification = captor.getValue();
        assertThat(notification.getOrderId()).isEqualTo(100L);
        assertThat(notification.getRestaurantId()).isEqualTo(10L);
        assertThat(notification.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(notification.getMessage()).isEqualTo("Order #100 accepted");
    }

    @Test
    @DisplayName("Should send new order notification to /topic/restaurants/{restaurantId}/orders")
    void shouldSendNewOrderNotification() {
        notificationService.sendNewOrderNotification(100L, 10L, 1L, "New order received: #100");

        ArgumentCaptor<OrderNotification> captor = ArgumentCaptor.forClass(OrderNotification.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/restaurants/10/orders"), captor.capture());

        OrderNotification notification = captor.getValue();
        assertThat(notification.getOrderId()).isEqualTo(100L);
        assertThat(notification.getRestaurantId()).isEqualTo(10L);
        assertThat(notification.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(notification.getMessage()).isEqualTo("New order received: #100");
    }

    @Test
    @DisplayName("Should send payment notification to /topic/orders/{orderId}/payment")
    void shouldSendPaymentNotification() {
        notificationService.sendPaymentNotification(100L, 50L, PaymentStatus.SUCCESS, BigDecimal.valueOf(560.00), "Payment successful");

        ArgumentCaptor<PaymentNotification> captor = ArgumentCaptor.forClass(PaymentNotification.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/orders/100/payment"), captor.capture());

        PaymentNotification notification = captor.getValue();
        assertThat(notification.getOrderId()).isEqualTo(100L);
        assertThat(notification.getPaymentId()).isEqualTo(50L);
        assertThat(notification.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(notification.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(560.00));
        assertThat(notification.getMessage()).isEqualTo("Payment successful");
    }
}

package com.foodflow.service;

import com.foodflow.dto.OrderNotification;
import com.foodflow.dto.PaymentNotification;
import com.foodflow.entity.OrderStatus;
import com.foodflow.entity.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendOrderStatusNotification(Long orderId, Long restaurantId, Long userId, OrderStatus status, String message) {
        try {
            OrderNotification notification = OrderNotification.builder()
                    .orderId(orderId)
                    .restaurantId(restaurantId)
                    .userId(userId)
                    .status(status)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String destination = "/topic/orders/" + orderId;
            log.info("Publishing WebSocket notification to {}: {}", destination, message);
            messagingTemplate.convertAndSend(destination, notification);
        } catch (Exception ex) {
            log.error("Failed to send WebSocket order status notification for order: {}", orderId, ex);
        }
    }

    public void sendNewOrderNotification(Long orderId, Long restaurantId, Long userId, String message) {
        try {
            OrderNotification notification = OrderNotification.builder()
                    .orderId(orderId)
                    .restaurantId(restaurantId)
                    .userId(userId)
                    .status(OrderStatus.PENDING)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String destination = "/topic/restaurants/" + restaurantId + "/orders";
            log.info("Publishing WebSocket notification to {}: {}", destination, message);
            messagingTemplate.convertAndSend(destination, notification);
        } catch (Exception ex) {
            log.error("Failed to send WebSocket new order notification for restaurant: {}", restaurantId, ex);
        }
    }

    public void sendPaymentNotification(Long orderId, Long paymentId, PaymentStatus status, BigDecimal amount, String message) {
        try {
            PaymentNotification notification = PaymentNotification.builder()
                    .orderId(orderId)
                    .paymentId(paymentId)
                    .status(status)
                    .amount(amount)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String destination = "/topic/orders/" + orderId + "/payment";
            log.info("Publishing WebSocket notification to {}: {}", destination, message);
            messagingTemplate.convertAndSend(destination, notification);
        } catch (Exception ex) {
            log.error("Failed to send WebSocket payment notification for order: {}", orderId, ex);
        }
    }
}

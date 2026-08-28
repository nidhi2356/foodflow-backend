package com.foodflow.service;

import com.foodflow.dto.OrderCreatedEvent;
import com.foodflow.dto.OrderStatusChangedEvent;
import com.foodflow.dto.PaymentFailedEvent;
import com.foodflow.dto.PaymentSuccessfulEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.foodflow.config.RabbitMQConfig.*;

@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${foodflow.rabbitmq.exchange:foodflow.exchange}")
    private String exchangeName;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("Publishing OrderCreatedEvent for order ID: {} to exchange: {} with routingKey: {}",
                    event.getOrderId(), exchangeName, ORDER_CREATED_ROUTING_KEY);
            rabbitTemplate.convertAndSend(exchangeName, ORDER_CREATED_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish OrderCreatedEvent for order: {}", event.getOrderId(), ex);
        }
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            String routingKey = "order.status." + event.getNewStatus().name().toLowerCase();
            log.info("Publishing OrderStatusChangedEvent for order ID: {} ({} -> {}) with routingKey: {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getNewStatus(), routingKey);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish OrderStatusChangedEvent for order: {}", event.getOrderId(), ex);
        }
    }

    public void publishPaymentSuccessful(PaymentSuccessfulEvent event) {
        try {
            log.info("Publishing PaymentSuccessfulEvent for payment ID: {}, order ID: {} with routingKey: {}",
                    event.getPaymentId(), event.getOrderId(), PAYMENT_SUCCESS_ROUTING_KEY);
            rabbitTemplate.convertAndSend(exchangeName, PAYMENT_SUCCESS_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish PaymentSuccessfulEvent for order: {}", event.getOrderId(), ex);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            log.info("Publishing PaymentFailedEvent for payment ID: {}, order ID: {} with routingKey: {}",
                    event.getPaymentId(), event.getOrderId(), PAYMENT_FAILED_ROUTING_KEY);
            rabbitTemplate.convertAndSend(exchangeName, PAYMENT_FAILED_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish PaymentFailedEvent for order: {}", event.getOrderId(), ex);
        }
    }
}

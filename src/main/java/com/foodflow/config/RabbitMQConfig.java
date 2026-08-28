package com.foodflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${foodflow.rabbitmq.exchange:foodflow.exchange}")
    private String exchangeName;

    public static final String ORDER_CREATED_QUEUE = "foodflow.order.created.queue";
    public static final String ORDER_STATUS_QUEUE = "foodflow.order.status.queue";
    public static final String PAYMENT_SUCCESS_QUEUE = "foodflow.payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE = "foodflow.payment.failed.queue";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_STATUS_ROUTING_KEY = "order.status.#";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public TopicExchange foodflowExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue orderStatusQueue() {
        return QueueBuilder.durable(ORDER_STATUS_QUEUE).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange foodflowExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(foodflowExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange foodflowExchange) {
        return BindingBuilder.bind(orderStatusQueue).to(foodflowExchange).with(ORDER_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange foodflowExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(foodflowExchange).with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange foodflowExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(foodflowExchange).with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

package com.foodflow.service;

import com.foodflow.dto.OrderCreateRequest;
import com.foodflow.dto.OrderItemRequest;
import com.foodflow.dto.OrderItemResponse;
import com.foodflow.dto.OrderResponse;
import com.foodflow.entity.*;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.OrderRepository;
import com.foodflow.repository.RestaurantRepository;
import com.foodflow.repository.UserRepository;
import com.foodflow.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            FoodItemRepository foodItemRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Creating order for customer '{}' at restaurant id: {}", username, request.getRestaurantId());

        User customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", request.getRestaurantId()));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException("Order must contain at least one item", HttpStatus.BAD_REQUEST);
        }

        Order order = Order.builder()
                .user(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PENDING)
                .orderItems(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new ApiException("Item quantity must be greater than 0", HttpStatus.BAD_REQUEST);
            }

            FoodItem foodItem = foodItemRepository.findById(itemReq.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("FoodItem", "id", itemReq.getFoodId()));

            if (foodItem.getRestaurant() == null || !foodItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new ApiException("Food item '" + foodItem.getName() + "' does not belong to restaurant '" + restaurant.getRestaurantName() + "'", HttpStatus.BAD_REQUEST);
            }

            BigDecimal priceAtOrderTime = BigDecimal.valueOf(foodItem.getPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = priceAtOrderTime.multiply(BigDecimal.valueOf(itemReq.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .foodItem(foodItem)
                    .quantity(itemReq.getQuantity())
                    .priceAtOrderTime(priceAtOrderTime)
                    .subtotal(subtotal)
                    .build();

            order.addOrderItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} and totalAmount: ₹{}", savedOrder.getId(), savedOrder.getTotalAmount());

        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Retrieving orders for customer: {}", username);

        List<Order> orders = orderRepository.findByUser_UsernameOrderByCreatedAtDesc(username);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Retrieving order id: {} by user: {}", id, username);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        boolean isCustomer = order.getUser().getUsername().equals(username);
        boolean isRestaurantOwner = order.getRestaurant().getOwner() != null &&
                order.getRestaurant().getOwner().getUsername().equals(username);

        if (!isCustomer && !isRestaurantOwner) {
            log.warn("User '{}' unauthorized to view order id: {}", username, id);
            throw new ApiException("Access denied: You do not have permission to view this order", HttpStatus.FORBIDDEN);
        }

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Retrieving orders for restaurant id: {} by user: {}", restaurantId, username);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getUsername().equals(username)) {
            log.warn("User '{}' does not own restaurant id: {}", username, restaurantId);
            throw new ApiException("Access denied: You do not own this restaurant", HttpStatus.FORBIDDEN);
        }

        List<Order> orders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Customer '{}' cancelling order id: {}", username, id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (!order.getUser().getUsername().equals(username)) {
            log.warn("User '{}' unauthorized to cancel order id: {}", username, id);
            throw new ApiException("Access denied: Only the customer who placed the order can cancel it", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException("Cannot cancel order with status " + order.getStatus() + ". Only PENDING orders can be cancelled.", HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToOrderResponse(updated);
    }

    @Transactional
    public OrderResponse acceptOrder(Long id) {
        return transitionRestaurantOrder(id, OrderStatus.PENDING, OrderStatus.ACCEPTED);
    }

    @Transactional
    public OrderResponse rejectOrder(Long id) {
        return transitionRestaurantOrder(id, OrderStatus.PENDING, OrderStatus.REJECTED);
    }

    @Transactional
    public OrderResponse markPreparing(Long id) {
        return transitionRestaurantOrder(id, OrderStatus.ACCEPTED, OrderStatus.PREPARING);
    }

    @Transactional
    public OrderResponse markReady(Long id) {
        return transitionRestaurantOrder(id, OrderStatus.PREPARING, OrderStatus.READY);
    }

    @Transactional
    public OrderResponse completeOrder(Long id) {
        return transitionRestaurantOrder(id, OrderStatus.READY, OrderStatus.COMPLETED);
    }

    private OrderResponse transitionRestaurantOrder(Long id, OrderStatus expectedStatus, OrderStatus targetStatus) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Restaurant owner '{}' transitioning order id: {} from {} to {}", username, id, expectedStatus, targetStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        Restaurant restaurant = order.getRestaurant();
        if (restaurant.getOwner() == null || !restaurant.getOwner().getUsername().equals(username)) {
            log.warn("User '{}' does not own restaurant for order id: {}", username, id);
            throw new ApiException("Access denied: You do not own the restaurant for this order", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() != expectedStatus) {
            throw new ApiException("Invalid status transition: Cannot transition order from " + order.getStatus() + " to " + targetStatus + ". Expected status: " + expectedStatus, HttpStatus.BAD_REQUEST);
        }

        order.setStatus(targetStatus);
        Order updated = orderRepository.save(order);
        return mapToOrderResponse(updated);
    }

    public OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems() != null ?
                order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList()) : new ArrayList<>();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .username(order.getUser() != null ? order.getUser().getUsername() : null)
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .restaurantName(order.getRestaurant() != null ? order.getRestaurant().getRestaurantName() : null)
                .orderItems(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        FoodItem foodItem = item.getFoodItem();
        return OrderItemResponse.builder()
                .id(item.getId())
                .foodId(foodItem != null ? foodItem.getId() : null)
                .foodName(foodItem != null ? foodItem.getName() : null)
                .quantity(item.getQuantity())
                .priceAtOrderTime(item.getPriceAtOrderTime())
                .subtotal(item.getSubtotal())
                .build();
    }
}

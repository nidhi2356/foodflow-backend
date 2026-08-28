package com.foodflow.service;

import com.foodflow.dto.OrderCreateRequest;
import com.foodflow.dto.OrderItemRequest;
import com.foodflow.dto.OrderResponse;
import com.foodflow.entity.*;
import com.foodflow.exception.ApiException;
import com.foodflow.exception.ResourceNotFoundException;
import com.foodflow.repository.FoodItemRepository;
import com.foodflow.repository.OrderRepository;
import com.foodflow.repository.RestaurantRepository;
import com.foodflow.repository.UserRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User sampleCustomer;
    private User sampleRestaurantOwner;
    private User otherCustomer;
    private Restaurant sampleRestaurant;
    private Restaurant otherRestaurant;
    private FoodItem sampleFood1;
    private FoodItem sampleFood2;
    private FoodItem otherRestaurantFood;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        setAuthenticatedUser("john_doe");

        sampleCustomer = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .build();

        sampleRestaurantOwner = User.builder()
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
                .location("Delhi")
                .owner(sampleRestaurantOwner)
                .build();

        otherRestaurant = Restaurant.builder()
                .id(20L)
                .restaurantName("Sushi Zen")
                .location("Saket, Delhi")
                .owner(otherCustomer)
                .build();

        sampleFood1 = FoodItem.builder()
                .id(101L)
                .name("Margherita Pizza")
                .price(280.0)
                .restaurant(sampleRestaurant)
                .build();

        sampleFood2 = FoodItem.builder()
                .id(102L)
                .name("Garlic Bread")
                .price(120.0)
                .restaurant(sampleRestaurant)
                .build();

        otherRestaurantFood = FoodItem.builder()
                .id(201L)
                .name("Salmon Nigiri")
                .price(450.0)
                .restaurant(otherRestaurant)
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(1001L)
                .foodItem(sampleFood1)
                .quantity(2)
                .priceAtOrderTime(BigDecimal.valueOf(280.00).setScale(2, RoundingMode.HALF_UP))
                .subtotal(BigDecimal.valueOf(560.00).setScale(2, RoundingMode.HALF_UP))
                .build();

        sampleOrder = Order.builder()
                .id(501L)
                .user(sampleCustomer)
                .restaurant(sampleRestaurant)
                .orderItems(new ArrayList<>(List.of(item1)))
                .totalAmount(BigDecimal.valueOf(560.00).setScale(2, RoundingMode.HALF_UP))
                .status(OrderStatus.PENDING)
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
    // 1. ORDER CREATION & CALCULATIONS
    // ==========================================

    @Test
    @DisplayName("Should create order successfully with auto ID, price snapshotting, and initial PENDING status")
    void shouldCreateOrderSuccessfully() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(10L)
                .items(List.of(
                        OrderItemRequest.builder().foodId(101L).quantity(2).build(), // 280 * 2 = 560
                        OrderItemRequest.builder().foodId(102L).quantity(1).build()  // 120 * 1 = 120 -> Total = 680
                ))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));
        when(foodItemRepository.findById(101L)).thenReturn(Optional.of(sampleFood1));
        when(foodItemRepository.findById(102L)).thenReturn(Optional.of(sampleFood2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(501L);
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(501L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getRestaurantId()).isEqualTo(10L);
        assertThat(response.getRestaurantName()).isEqualTo("Mario Trattoria");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(680.00));
        assertThat(response.getOrderItems()).hasSize(2);

        // Verify item 1
        assertThat(response.getOrderItems().get(0).getFoodId()).isEqualTo(101L);
        assertThat(response.getOrderItems().get(0).getPriceAtOrderTime()).isEqualByComparingTo(BigDecimal.valueOf(280.00));
        assertThat(response.getOrderItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(560.00));

        // Verify item 2
        assertThat(response.getOrderItems().get(1).getFoodId()).isEqualTo(102L);
        assertThat(response.getOrderItems().get(1).getPriceAtOrderTime()).isEqualByComparingTo(BigDecimal.valueOf(120.00));
        assertThat(response.getOrderItems().get(1).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(120.00));
    }

    @Test
    @DisplayName("Price snapshot permanently preserved when food menu price changes later")
    void priceSnapshotPermanentlyPreserved() {
        // Order placed with sampleFood1 at ₹280
        assertThat(sampleOrder.getOrderItems().get(0).getPriceAtOrderTime()).isEqualByComparingTo(BigDecimal.valueOf(280.00));
        assertThat(sampleOrder.getOrderItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(560.00));

        // Restaurant owner modifies menu price of sampleFood1 to ₹350
        sampleFood1.setPrice(350.0);

        // Historical order still retains snapshot ₹280 and ₹560 subtotal
        assertThat(sampleOrder.getOrderItems().get(0).getPriceAtOrderTime()).isEqualByComparingTo(BigDecimal.valueOf(280.00));
        assertThat(sampleOrder.getOrderItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(560.00));
        assertThat(sampleOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(560.00));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant does not exist")
    void shouldThrowWhenRestaurantNotFound() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(999L)
                .items(List.of(OrderItemRequest.builder().foodId(101L).quantity(1).build()))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when food item does not exist")
    void shouldThrowWhenFoodItemNotFound() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(10L)
                .items(List.of(OrderItemRequest.builder().foodId(999L).quantity(1).build()))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));
        when(foodItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw 400 when food item belongs to another restaurant")
    void shouldThrowWhenFoodBelongsToAnotherRestaurant() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(10L)
                .items(List.of(
                        OrderItemRequest.builder().foodId(101L).quantity(1).build(),
                        OrderItemRequest.builder().foodId(201L).quantity(1).build() // from otherRestaurant (id 20)
                ))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));
        when(foodItemRepository.findById(101L)).thenReturn(Optional.of(sampleFood1));
        when(foodItemRepository.findById(201L)).thenReturn(Optional.of(otherRestaurantFood));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not belong to restaurant");
    }

    @Test
    @DisplayName("Should throw 400 when quantity is 0 or negative")
    void shouldThrowWhenQuantityIsInvalid() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(10L)
                .items(List.of(OrderItemRequest.builder().foodId(101L).quantity(0).build()))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("quantity must be greater than 0");
    }

    @Test
    @DisplayName("Should throw 400 when items list is empty")
    void shouldThrowWhenItemsListIsEmpty() {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .restaurantId(10L)
                .items(Collections.emptyList())
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleCustomer));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least one item");
    }

    // ==========================================
    // 2. CUSTOMER ORDERS & CANCELLATION
    // ==========================================

    @Test
    @DisplayName("Customer can retrieve own orders")
    void shouldGetCustomerOrders() {
        when(orderRepository.findByUser_UsernameOrderByCreatedAtDesc("john_doe")).thenReturn(List.of(sampleOrder));

        List<OrderResponse> result = orderService.getMyOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(501L);
        assertThat(result.get(0).getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Customer can retrieve own order by ID")
    void shouldGetOrderByIdForCustomer() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        OrderResponse response = orderService.getOrderById(501L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(501L);
    }

    @Test
    @DisplayName("Customer cannot retrieve another customer's order (throws 403)")
    void shouldThrow403WhenCustomerAccessesOtherOrder() {
        setAuthenticatedUser("alice_wonder");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder)); // belongs to john_doe

        assertThatThrownBy(() -> orderService.getOrderById(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Customer can cancel own PENDING order (PENDING -> CANCELLED)")
    void shouldCancelPendingOrder() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.cancelOrder(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Customer cannot cancel another customer's order (throws 403)")
    void shouldThrow403WhenCancellingOtherCustomerOrder() {
        setAuthenticatedUser("alice_wonder");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Access denied: Only the customer who placed the order can cancel it");
    }

    @Test
    @DisplayName("Customer cannot cancel ACCEPTED order (throws 400)")
    void shouldThrow400WhenCancellingAcceptedOrder() {
        sampleOrder.setStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot cancel order with status ACCEPTED");
    }

    // ==========================================
    // 3. RESTAURANT OWNER ORDER HANDLING & LIFECYCLE
    // ==========================================

    @Test
    @DisplayName("Restaurant owner can retrieve their restaurant orders")
    void shouldGetRestaurantOrdersForOwner() {
        setAuthenticatedUser("chef_mario");
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant));
        when(orderRepository.findByRestaurantIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(sampleOrder));

        List<OrderResponse> result = orderService.getRestaurantOrders(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestaurantName()).isEqualTo("Mario Trattoria");
    }

    @Test
    @DisplayName("Non-owner cannot retrieve restaurant orders (throws 403)")
    void shouldThrow403WhenNonOwnerGetsRestaurantOrders() {
        setAuthenticatedUser("alice_wonder");
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(sampleRestaurant)); // owner is chef_mario

        assertThatThrownBy(() -> orderService.getRestaurantOrders(10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Access denied: You do not own this restaurant");
    }

    @Test
    @DisplayName("Restaurant owner can ACCEPT a PENDING order (PENDING -> ACCEPTED)")
    void shouldAcceptPendingOrder() {
        setAuthenticatedUser("chef_mario");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.acceptOrder(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Restaurant owner can REJECT a PENDING order (PENDING -> REJECTED)")
    void shouldRejectPendingOrder() {
        setAuthenticatedUser("chef_mario");
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.rejectOrder(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    @DisplayName("Restaurant owner can mark ACCEPTED order as PREPARING (ACCEPTED -> PREPARING)")
    void shouldMarkOrderPreparing() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.markPreparing(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    @DisplayName("Restaurant owner can mark PREPARING order as READY (PREPARING -> READY)")
    void shouldMarkOrderReady() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.markReady(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    @DisplayName("Restaurant owner can COMPLETE a READY order (READY -> COMPLETED)")
    void shouldCompleteOrder() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.READY);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderResponse response = orderService.completeOrder(501L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    // ==========================================
    // 4. INVALID STATUS TRANSITIONS (400 BAD REQUEST)
    // ==========================================

    @Test
    @DisplayName("Invalid transition: PENDING -> PREPARING throws 400")
    void shouldThrow400OnPendingToPreparing() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.PENDING); // expects ACCEPTED for markPreparing
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.markPreparing(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition: PENDING -> READY throws 400")
    void shouldThrow400OnPendingToReady() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.PENDING); // expects PREPARING for markReady
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.markReady(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition: PENDING -> COMPLETED throws 400")
    void shouldThrow400OnPendingToCompleted() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.PENDING); // expects READY for completeOrder
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.completeOrder(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition: ACCEPTED -> READY throws 400")
    void shouldThrow400OnAcceptedToReady() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.markReady(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition: PREPARING -> COMPLETED throws 400")
    void shouldThrow400OnPreparingToCompleted() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.completeOrder(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition from terminal state: COMPLETED -> PREPARING throws 400")
    void shouldThrow400OnCompletedToPreparing() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.markPreparing(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Invalid transition from terminal state: REJECTED -> ACCEPTED throws 400")
    void shouldThrow400OnRejectedToAccepted() {
        setAuthenticatedUser("chef_mario");
        sampleOrder.setStatus(OrderStatus.REJECTED);
        when(orderRepository.findById(501L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.acceptOrder(501L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid status transition");
    }
}

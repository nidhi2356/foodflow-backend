package com.foodflow.repository;

import com.foodflow.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUser_UsernameOrderByCreatedAtDesc(String username);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    Optional<Order> findByIdAndUser_Username(Long id, String username);
}

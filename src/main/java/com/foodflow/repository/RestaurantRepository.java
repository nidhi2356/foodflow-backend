package com.foodflow.repository;

import com.foodflow.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByRestaurantId(String restaurantId);

    boolean existsByRestaurantId(String restaurantId);
}

package com.foodflow.repository;

import com.foodflow.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    Optional<FoodItem> findByItemId(String itemId);

    List<FoodItem> findByRestaurantId(Long restaurantId);

    List<FoodItem> findByRestaurant_RestaurantId(String restaurantId);

    List<FoodItem> findByCategoryIgnoreCase(String category);

    List<FoodItem> findByIsVeg(Boolean isVeg);
}

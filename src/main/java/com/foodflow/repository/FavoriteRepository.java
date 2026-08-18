package com.foodflow.repository;

import com.foodflow.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser_Username(String username);

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUser_UsernameAndFoodItemId(String username, Long foodItemId);

    Optional<Favorite> findByUserIdAndFoodItemId(Long userId, Long foodItemId);

    boolean existsByUser_UsernameAndFoodItemId(String username, Long foodItemId);

    boolean existsByUserIdAndFoodItemId(Long userId, Long foodItemId);

    void deleteByUser_UsernameAndFoodItemId(String username, Long foodItemId);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.foodItem fi LEFT JOIN FETCH fi.restaurant WHERE f.user.username = :username ORDER BY f.createdAt DESC")
    List<Favorite> findAllWithFoodDetailsByUsername(@Param("username") String username);
}

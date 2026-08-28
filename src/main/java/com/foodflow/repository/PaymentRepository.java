package com.foodflow.repository;

import com.foodflow.entity.Payment;
import com.foodflow.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}

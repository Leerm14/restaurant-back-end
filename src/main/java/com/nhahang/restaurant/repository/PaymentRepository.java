package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.PaymentStatus;
import com.nhahang.restaurant.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    
    Optional<Payment> findByOrderId(Integer orderId);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.paymentTime BETWEEN :fromDate AND :toDate")
    List<Payment> findByStatusAndPaymentTimeBetween(
            @Param("status") PaymentStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
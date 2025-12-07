package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserId(Integer userId);
    List<Order> findByStatus(String status);
    List<Order> findByTableId(Integer tableId);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT o FROM Order o WHERE o.user.email = :email")
    List<Order> findByUserEmail(@Param("email") String email);

    @Query("SELECT o FROM Order o WHERE o.user.phoneNumber = :phoneNumber")
    List<Order> findByUserPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o " +
           "WHERE o.table.id = :tableId " +
           "AND o.status IN (:statuses)")
    boolean existsActiveOrderAtTable(
            @Param("tableId") Integer tableId, 
            @Param("statuses") List<OrderStatus> statuses
    );
}
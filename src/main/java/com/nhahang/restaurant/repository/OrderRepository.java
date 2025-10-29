package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserId(Integer userId);

    List<Order> findByStatus(String status);
    
    List<Order> findByTableId(Integer tableId);
}
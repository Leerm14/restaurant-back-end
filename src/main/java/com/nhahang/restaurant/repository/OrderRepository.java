package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Lấy tất cả đơn hàng của một user (dựa vào user.id)
    List<Order> findByUserId(Integer userId);

    // Lấy tất cả đơn hàng theo trạng thái
    List<Order> findByStatus(String status);
    
    // Lấy tất cả đơn hàng của một bàn (dựa vào table.id)
    List<Order> findByTableId(Integer tableId);
}
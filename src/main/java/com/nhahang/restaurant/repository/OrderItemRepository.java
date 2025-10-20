package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    // Thường thì OrderItem sẽ được truy vấn thông qua OrderRepository,
    // nhưng vẫn tạo sẵn repository này để quản lý.
}
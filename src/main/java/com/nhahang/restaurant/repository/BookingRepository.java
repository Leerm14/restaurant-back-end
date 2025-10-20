package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    
    // Lấy lịch sử đặt bàn của một user (dựa vào user.id)
    List<Booking> findByUserId(Integer userId);
    
    // Lấy các lịch đặt của một bàn (dựa vào table.id)
    List<Booking> findByTableId(Integer tableId);
}
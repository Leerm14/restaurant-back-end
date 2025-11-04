package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    
    List<Booking> findByUserId(Integer userId);
    
    List<Booking> findByTableId(Integer tableId);

    List<Booking> findByUserPhoneNumber(String phoneNumber);
}
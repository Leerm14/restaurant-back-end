package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {  
    List<Booking> findByUserId(Integer userId);
    List<Booking> findByTableId(Integer tableId);
    List<Booking> findByUserPhoneNumber(String phoneNumber);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.table.id = :tableId " +
           "AND b.status IN (com.nhahang.restaurant.model.BookingStatus.Confirmed, com.nhahang.restaurant.model.BookingStatus.Pending) " +
           "AND b.bookingTime > :startTime " +
           "AND b.bookingTime < :endTime")
    boolean existsConflictingBooking(
            @Param("tableId") Integer tableId, 
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT b FROM Booking b " +
           "WHERE b.status IN (com.nhahang.restaurant.model.BookingStatus.Confirmed, com.nhahang.restaurant.model.BookingStatus.Pending) " +
           "AND b.bookingTime > :startTime " +
           "AND b.bookingTime < :endTime")
    List<Booking> findConflictingBookings(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT b FROM Booking b " +
           "WHERE b.user.id = :userId " +
           "AND b.table.id = :tableId " +
           "AND b.bookingTime BETWEEN :startTime AND :endTime " +
           "ORDER BY b.bookingTime DESC")
    List<Booking> findBookingsForOrder(
            @Param("userId") Integer userId,
            @Param("tableId") Integer tableId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
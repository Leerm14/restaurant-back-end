package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.BookingCreateRequest;
import com.nhahang.restaurant.dto.BookingDTO;
import com.nhahang.restaurant.model.entity.Booking;
import com.nhahang.restaurant.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // --- API 1: TẠO ĐẶT BÀN MỚI ---
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingCreateRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- API 2: LẤY TẤT CẢ ĐẶT BÀN ---
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    // --- API 3: LẤY ĐẶT BÀN THEO ID ---
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- API 4: LẤY ĐẶT BÀN THEO USER ID ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUserId(@PathVariable Integer userId) {
        List<Booking> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    // --- API 5: LẤY ĐẶT BÀN THEO TABLE ID ---
    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<Booking>> getBookingsByTableId(@PathVariable Integer tableId) {
        List<Booking> bookings = bookingService.getBookingsByTableId(tableId);
        return ResponseEntity.ok(bookings);
    }
     
    // --- API 6: LẤY ĐẶT BÀN THEO PHONE NUMBER ---
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<List<Booking>> getBookingsByPhoneNumber(@PathVariable String phoneNumber) {
        List<Booking> bookings = bookingService.getBookingsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(bookings);
    }

    /// --- API 7: CẬP NHẬT ĐẶT BÀN ---
    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(
            @PathVariable Integer id, 
            @RequestBody BookingCreateRequest request) {
        try {
            Booking booking = bookingService.updateBooking(id, request);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// --- API 7: HỦY ĐẶT BÀN ---
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.cancelBooking(id);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// --- API 8: HOÀN THÀNH ĐẶT BÀN ---
    @PutMapping("/{id}/complete")
    public ResponseEntity<Booking> completeBooking(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.completeBooking(id);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// --- API 9: XÓA ĐẶT BÀN ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Integer id) {
        try {
            bookingService.deleteBooking(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

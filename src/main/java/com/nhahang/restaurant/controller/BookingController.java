package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.BookingCreateRequest;
import com.nhahang.restaurant.model.entity.Booking;
import com.nhahang.restaurant.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // --- API 1: TẠO ĐẶT BÀN MỚI ---
    @PostMapping
     @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingCreateRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING')") 
    public ResponseEntity<?> checkInBooking(@PathVariable Integer id) {
        try {
            Booking booking = bookingService.checkInBooking(id);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // --- API 2: LẤY TẤT CẢ ĐẶT BÀN ---
    @GetMapping
     @PreAuthorize("hasAuthority('READ_BOOKING')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    // --- API 3: LẤY ĐẶT BÀN THEO ID ---
    @GetMapping("/{id}")
     @PreAuthorize("hasAuthority('READ_BOOKING')")
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
     @PreAuthorize("hasAuthority('READ_BOOKING')")
    public ResponseEntity<List<Booking>> getBookingsByUserId(@PathVariable Integer userId) {
        List<Booking> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    // --- API 5: LẤY ĐẶT BÀN THEO TABLE ID ---
    @GetMapping("/table/{tableId}")
     @PreAuthorize("hasAuthority('READ_BOOKING')")
    public ResponseEntity<List<Booking>> getBookingsByTableId(@PathVariable Integer tableId) {
        List<Booking> bookings = bookingService.getBookingsByTableId(tableId);
        return ResponseEntity.ok(bookings);
    }
     
    // --- API 6: LẤY ĐẶT BÀN THEO PHONE NUMBER ---
    @GetMapping("/phone/{phoneNumber}")
     @PreAuthorize("hasAuthority('READ_BOOKING')")
    public ResponseEntity<List<Booking>> getBookingsByPhoneNumber(@PathVariable String phoneNumber) {
        List<Booking> bookings = bookingService.getBookingsByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(bookings);
    }

    /// --- API 7: CẬP NHẬT ĐẶT BÀN ---
    @PutMapping("/{id}")
     @PreAuthorize("hasAuthority('UPDATE_BOOKING')")
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

    /// --- API 7b: CẬP NHẬT TRẠNG THÁI ĐẶT BÀN ---
    @PatchMapping("/{id}/status")
     @PreAuthorize("hasAuthority('UPDATE_BOOKING')")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable Integer id, @RequestParam String status) {
        try {
            Booking booking = bookingService.updateBookingStatus(id, status);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// --- API 9: XÓA ĐẶT BÀN ---
    @DeleteMapping("/{id}")
     @PreAuthorize("hasAuthority('DELETE_BOOKING')")
    public ResponseEntity<Void> deleteBooking(@PathVariable Integer id) {
        try {
            bookingService.deleteBooking(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

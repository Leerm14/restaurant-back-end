package com.nhahang.restaurant.model.entity;

import com.nhahang.restaurant.model.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "num_guests", nullable = false)
    private int numGuests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // ENUM('Confirmed', 'Cancelled', 'Completed')
}
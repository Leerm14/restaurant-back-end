package com.nhahang.restaurant.model.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nhahang.restaurant.model.TableStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
@Entity
@Table(name = "tables")
@Data
public class RestaurantTable { // Đổi tên từ 'Table' để tránh xung đột

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "table_number", nullable = false, unique = true)
    private String tableNumber;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status; // Tương ứng với ENUM('Available', 'Occupied', 'Reserved', 'Cleaning')

    // ----- Quan hệ ngược -----
    
    @OneToMany(mappedBy = "table")
    @JsonIgnore
    private List<Order> orders;

    @OneToMany(mappedBy = "table")
    @JsonIgnore
    private List<Booking> bookings;
}
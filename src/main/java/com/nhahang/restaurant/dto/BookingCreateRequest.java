package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {
    private Integer userId;
    private Integer tableId;
    private LocalDateTime bookingTime;
    private int numGuests;
}

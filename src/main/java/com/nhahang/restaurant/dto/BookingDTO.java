package com.nhahang.restaurant.dto;

import com.nhahang.restaurant.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Integer id;
    private Integer userId;
    private String userName;
    private Integer tableId;
    private String tableName;
    private LocalDateTime bookingTime;
    private int numGuests;
    private BookingStatus status;
}

package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Integer id;
    private Integer userId;
    private String userFullName;
    private Integer tableId;
    private String tableName;
    private BigDecimal totalAmount;
    private String status;
    private String orderType;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> orderItems;
    private LocalDateTime bookingTime; // Thời gian đặt bàn (nếu có)
}

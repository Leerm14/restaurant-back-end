package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    private Integer userId;
    private Integer tableId;
    private String orderType; 
    private List<OrderItemRequest> orderItems;
}

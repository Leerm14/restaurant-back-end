package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Integer id;
    private Integer menuItemId;
    private String menuItemName;
    private Integer quantity;
    private BigDecimal priceAtOrder;
    private BigDecimal subtotal; // quantity * priceAtOrder
}

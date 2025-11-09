package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestSellingItemDTO {
    private Integer menuItemId;
    private String menuItemName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String categoryName;
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
}

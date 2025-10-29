package com.nhahang.restaurant.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data // Chỉ cần getter/setter để chứa dữ liệu
public class MenuItemDTO {

    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer categoryId; 
    private String status;
}
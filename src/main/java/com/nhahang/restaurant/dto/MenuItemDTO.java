package com.nhahang.restaurant.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data // Chỉ cần getter/setter để chứa dữ liệu
public class MenuItemDTO {
    // Đây là những thứ client được phép gửi lên
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private Integer categoryId; 
    private String status;
}
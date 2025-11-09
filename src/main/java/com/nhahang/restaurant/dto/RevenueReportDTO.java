package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    private BigDecimal totalRevenue;
    private Long totalTransactions;
    private BigDecimal averageTransactionValue;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    
    // Phân loại theo phương thức thanh toán
    private BigDecimal cashRevenue;
    private Long cashTransactions;
    private BigDecimal qrCodeRevenue;
    private Long qrCodeTransactions;
    private BigDecimal creditCardRevenue;
    private Long creditCardTransactions;
}

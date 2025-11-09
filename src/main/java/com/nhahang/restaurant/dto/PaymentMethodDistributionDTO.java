package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDistributionDTO {
    private String paymentMethod;
    private Long transactionCount;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
}

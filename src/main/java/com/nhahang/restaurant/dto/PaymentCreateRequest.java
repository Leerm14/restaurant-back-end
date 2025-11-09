package com.nhahang.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    private Integer orderId;
    private BigDecimal amount;
    private String paymentMethod; // "Cash", "QRCode", "CreditCard"
    private String transactionId; // Optional - for QR Code or Credit Card
}

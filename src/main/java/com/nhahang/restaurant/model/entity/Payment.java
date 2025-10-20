package com.nhahang.restaurant.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nhahang.restaurant.model.PaymentMethod;
import com.nhahang.restaurant.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true) // Mỗi thanh toán chỉ thuộc 1 đơn hàng
    @JsonIgnore
    private Order order;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod; // ENUM('Cash', 'QR Code', 'Credit Card')

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // ENUM('Pending', 'Successful', 'Failed')

    @Column(name = "transaction_id")
    private String transactionId;

    @CreationTimestamp
    @Column(name = "payment_time", updatable = false)
    private LocalDateTime paymentTime;
}
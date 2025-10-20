package com.nhahang.restaurant.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // ENUM('Cash', 'QR Code', 'Credit Card')

    @Column(nullable = false)
    private String status; // ENUM('Pending', 'Successful', 'Failed')

    @Column(name = "transaction_id")
    private String transactionId;

    @CreationTimestamp
    @Column(name = "payment_time", updatable = false)
    private LocalDateTime paymentTime;
}
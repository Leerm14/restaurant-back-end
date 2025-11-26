package com.nhahang.restaurant.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhahang.restaurant.dto.PaymentCreateRequest;
import com.nhahang.restaurant.dto.PaymentDTO;
import com.nhahang.restaurant.dto.PaymentMethodDistributionDTO;
import com.nhahang.restaurant.dto.RevenueReportDTO;
import com.nhahang.restaurant.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// IMPORT MỚI CHO PAYOS V2
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * API 1: TẠO LINK THANH TOÁN PAYOS
     * Thay đổi kiểu trả về thành CreatePaymentLinkResponse
     */
    @PostMapping("/payos/{orderId}")
    @PreAuthorize("hasAuthority('CREATE_PAYMENT')")
    public ResponseEntity<CreatePaymentLinkResponse> createPayOSLink(@PathVariable Integer orderId) {
        try {
            CreatePaymentLinkResponse data = paymentService.createPayOSLink(orderId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * API 2: WEBHOOK
     * PayOS gọi vào đây khi thanh toán thành công
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<String> handlePayOSWebhook(@RequestBody ObjectNode webhookBody) {
        try {
            paymentService.handlePayOSWebhook(webhookBody);
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- CÁC API KHÁC GIỮ NGUYÊN ---

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        try {
            List<PaymentDTO> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Integer id) {
        try {
            PaymentDTO payment = paymentService.getPaymentById(id);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<PaymentDTO> getPaymentByOrderId(@PathVariable Integer orderId) {
        try {
            PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByStatus(@PathVariable String status) {
        try {
            List<PaymentDTO> payments = paymentService.getPaymentsByStatus(status);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/method/{method}")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByMethod(@PathVariable String method) {
        try {
            List<PaymentDTO> payments = paymentService.getPaymentsByMethod(method);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PAYMENT')")
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody PaymentCreateRequest request) {
        try {
            PaymentDTO createdPayment = paymentService.createPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('UPDATE_PAYMENT')")
    public ResponseEntity<PaymentDTO> confirmPayment(@PathVariable Integer id) {
        try {
            PaymentDTO confirmedPayment = paymentService.confirmPayment(id);
            return ResponseEntity.ok(confirmedPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/fail")
    @PreAuthorize("hasAuthority('UPDATE_PAYMENT')")
    public ResponseEntity<PaymentDTO> failPayment(@PathVariable Integer id) {
        try {
            PaymentDTO failedPayment = paymentService.failPayment(id);
            return ResponseEntity.ok(failedPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('UPDATE_PAYMENT')")
    public ResponseEntity<PaymentDTO> updatePaymentStatus(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            PaymentDTO updatedPayment = paymentService.updatePaymentStatus(id, status);
            return ResponseEntity.ok(updatedPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_PAYMENT')")
    public ResponseEntity<Void> deletePayment(@PathVariable Integer id) {
        try {
            paymentService.deletePayment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/revenue-report")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            LocalDateTime fromDate = (from != null) ? from.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime toDate = (to != null) ? to.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            RevenueReportDTO report = paymentService.getRevenueReport(fromDate, toDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/payment-method-distribution")
    @PreAuthorize("hasAuthority('READ_PAYMENT')")
    public ResponseEntity<List<PaymentMethodDistributionDTO>> getPaymentMethodDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            LocalDateTime fromDate = (from != null) ? from.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime toDate = (to != null) ? to.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            List<PaymentMethodDistributionDTO> distribution = paymentService.getPaymentMethodDistribution(fromDate, toDate);
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.nhahang.restaurant.controller;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Lấy tất cả thanh toán
     */
    @GetMapping
    @PreAuthorize("haspermission('READ_PAYMENT')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        try {
            List<PaymentDTO> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thanh toán theo ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("haspermission('READ_PAYMENT')")
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

    /**
     * Lấy thanh toán theo order ID
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("haspermission('READ_PAYMENT')")
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

    /**
     * Lấy thanh toán theo trạng thái
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("haspermission('READ_PAYMENT')")
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

    /**
     * Lấy thanh toán theo phương thức thanh toán
     */
    @GetMapping("/method/{method}")
    @PreAuthorize("haspermission('READ_PAYMENT')")
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

    /**
     * Tạo thanh toán mới
     */
    @PostMapping
    @PreAuthorize("haspermission('CREATE_PAYMENT')")
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

    /**
     * Xác nhận thanh toán thành công
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("haspermission('UPDATE_PAYMENT')")
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

    /**
     * Đánh dấu thanh toán thất bại
     */
    @PatchMapping("/{id}/fail")
    @PreAuthorize("haspermission('UPDATE_PAYMENT')")
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

    /**
     * Cập nhật trạng thái thanh toán
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("haspermission('UPDATE_PAYMENT')")
    public ResponseEntity<PaymentDTO> updatePaymentStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
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

    /**
     * Xóa thanh toán
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("haspermission('DELETE_PAYMENT')")
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

    /**
     * Lấy báo cáo doanh thu theo khoảng thời gian
     * @param from Ngày bắt đầu (format: yyyy-MM-dd), mặc định là đầu tháng hiện tại
     * @param to Ngày kết thúc (format: yyyy-MM-dd), mặc định là cuối ngày hôm nay
     */
    @GetMapping("/revenue-report")
    @PreAuthorize("haspermission('READ_PAYMENT')")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Nếu không có tham số from, lấy từ đầu tháng hiện tại
            LocalDateTime fromDate = (from != null) 
                ? from.atStartOfDay() 
                : LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            // Nếu không có tham số to, lấy đến cuối ngày hôm nay
            LocalDateTime toDate = (to != null) 
                ? to.atTime(LocalTime.MAX) 
                : LocalDate.now().atTime(LocalTime.MAX);

            // Kiểm tra ngày hợp lệ
            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            RevenueReportDTO report = paymentService.getRevenueReport(fromDate, toDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy phân phối phương thức thanh toán
     * @param from Ngày bắt đầu (format: yyyy-MM-dd), mặc định là đầu tháng hiện tại
     * @param to Ngày kết thúc (format: yyyy-MM-dd), mặc định là cuối ngày hôm nay
     */
    @GetMapping("/payment-method-distribution")
    @PreAuthorize("haspermission('READ_PAYMENT')")
    public ResponseEntity<List<PaymentMethodDistributionDTO>> getPaymentMethodDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            // Nếu không có tham số from, lấy từ đầu tháng hiện tại
            LocalDateTime fromDate = (from != null) 
                ? from.atStartOfDay() 
                : LocalDate.now().withDayOfMonth(1).atStartOfDay();
            
            // Nếu không có tham số to, lấy đến cuối ngày hôm nay
            LocalDateTime toDate = (to != null) 
                ? to.atTime(LocalTime.MAX) 
                : LocalDate.now().atTime(LocalTime.MAX);

            // Kiểm tra ngày hợp lệ
            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            List<PaymentMethodDistributionDTO> distribution = 
                    paymentService.getPaymentMethodDistribution(fromDate, toDate);
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

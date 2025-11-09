package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.PaymentCreateRequest;
import com.nhahang.restaurant.dto.PaymentDTO;
import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.PaymentMethod;
import com.nhahang.restaurant.model.PaymentStatus;
import com.nhahang.restaurant.model.entity.Order;
import com.nhahang.restaurant.model.entity.Payment;
import com.nhahang.restaurant.repository.OrderRepository;
import com.nhahang.restaurant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Lấy tất cả thanh toán
     */
    @Transactional
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thanh toán theo ID
     */
    @Transactional
    public PaymentDTO getPaymentById(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));
        return convertToDTO(payment);
    }

    /**
     * Lấy thanh toán theo order ID
     */
    @Transactional
    public PaymentDTO getPaymentByOrderId(Integer orderId) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder() != null && p.getOrder().getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán cho đơn hàng với ID: " + orderId));
        return convertToDTO(payment);
    }

    /**
     * Lấy thanh toán theo trạng thái
     */
    @Transactional
    public List<PaymentDTO> getPaymentsByStatus(String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            List<Payment> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == paymentStatus)
                    .collect(Collectors.toList());
            return payments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái thanh toán không hợp lệ: " + status);
        }
    }

    /**
     * Lấy thanh toán theo phương thức thanh toán
     */
    @Transactional
    public List<PaymentDTO> getPaymentsByMethod(String method) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.valueOf(method);
            List<Payment> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getPaymentMethod() == paymentMethod)
                    .collect(Collectors.toList());
            return payments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + method);
        }
    }

    /**
     * Tạo thanh toán mới
     */
    @Transactional
    public PaymentDTO createPayment(PaymentCreateRequest request) {
        // Kiểm tra order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        // Kiểm tra order đã có payment chưa
        boolean hasPayment = paymentRepository.findAll().stream()
                .anyMatch(p -> p.getOrder() != null && p.getOrder().getId().equals(request.getOrderId()));
        if (hasPayment) {
            throw new RuntimeException("Đơn hàng này đã có thanh toán");
        }

        // Kiểm tra trạng thái order
        if (order.getStatus() == OrderStatus.Cancelled) {
            throw new RuntimeException("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        // Kiểm tra số tiền
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thanh toán phải lớn hơn 0");
        }

        // Kiểm tra số tiền khớp với order
        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với tổng tiền đơn hàng");
        }

        // Kiểm tra payment method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }

        // Tạo payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.Pending);
        payment.setTransactionId(request.getTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        return convertToDTO(savedPayment);
    }

    /**
     * Xác nhận thanh toán thành công
     */
    @Transactional
    public PaymentDTO confirmPayment(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        if (payment.getStatus() == PaymentStatus.Successful) {
            throw new RuntimeException("Thanh toán đã được xác nhận trước đó");
        }

        if (payment.getStatus() == PaymentStatus.Failed) {
            throw new RuntimeException("Không thể xác nhận thanh toán đã thất bại");
        }

        payment.setStatus(PaymentStatus.Successful);

        // Cập nhật trạng thái order thành Completed
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.Completed);
            orderRepository.save(order);
        }

        Payment updatedPayment = paymentRepository.save(payment);
        return convertToDTO(updatedPayment);
    }

    /**
     * Đánh dấu thanh toán thất bại
     */
    @Transactional
    public PaymentDTO failPayment(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        if (payment.getStatus() == PaymentStatus.Successful) {
            throw new RuntimeException("Không thể đánh dấu thất bại cho thanh toán đã thành công");
        }

        if (payment.getStatus() == PaymentStatus.Failed) {
            throw new RuntimeException("Thanh toán đã được đánh dấu thất bại trước đó");
        }

        payment.setStatus(PaymentStatus.Failed);
        Payment updatedPayment = paymentRepository.save(payment);
        return convertToDTO(updatedPayment);
    }

    /**
     * Cập nhật trạng thái thanh toán
     */
    @Transactional
    public PaymentDTO updatePaymentStatus(Integer id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        try {
            PaymentStatus newStatus = PaymentStatus.valueOf(status);

            // Kiểm tra logic chuyển trạng thái
            if (payment.getStatus() == PaymentStatus.Successful && newStatus != PaymentStatus.Successful) {
                throw new RuntimeException("Không thể thay đổi trạng thái thanh toán đã thành công");
            }

            payment.setStatus(newStatus);

            // Nếu chuyển sang Successful, cập nhật order
            if (newStatus == PaymentStatus.Successful && payment.getOrder() != null) {
                Order order = payment.getOrder();
                order.setStatus(OrderStatus.Completed);
                orderRepository.save(order);
            }

            Payment updatedPayment = paymentRepository.save(payment);
            return convertToDTO(updatedPayment);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái thanh toán không hợp lệ: " + status);
        }
    }

    /**
     * Xóa thanh toán
     */
    @Transactional
    public void deletePayment(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        if (payment.getStatus() == PaymentStatus.Successful) {
            throw new RuntimeException("Không thể xóa thanh toán đã thành công");
        }

        paymentRepository.delete(payment);
    }

    /**
     * Chuyển đổi Payment entity sang PaymentDTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod().name());
        dto.setStatus(payment.getStatus().name());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaymentTime(payment.getPaymentTime());
        return dto;
    }
}

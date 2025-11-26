package com.nhahang.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhahang.restaurant.dto.PaymentCreateRequest;
import com.nhahang.restaurant.dto.PaymentDTO;
import com.nhahang.restaurant.dto.PaymentMethodDistributionDTO;
import com.nhahang.restaurant.dto.RevenueReportDTO;
import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.PaymentMethod;
import com.nhahang.restaurant.model.PaymentStatus;
import com.nhahang.restaurant.model.entity.Order;
import com.nhahang.restaurant.model.entity.OrderItem;
import com.nhahang.restaurant.model.entity.Payment;
import com.nhahang.restaurant.repository.BookingRepository;
import com.nhahang.restaurant.repository.OrderRepository;
import com.nhahang.restaurant.repository.PaymentRepository;
import com.nhahang.restaurant.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// IMPORT MỚI CHÍNH XÁC CHO PAYOS V2
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final PayOS payOS;

    @Value("${payos.return-url}")
    private String returnUrl;
    @Value("${payos.cancel-url}")
    private String cancelUrl;

    /**
     * TẠO LINK THANH TOÁN PAYOS (V2)
     */
    @Transactional
    public CreatePaymentLinkResponse createPayOSLink(Integer orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getTotalAmount().longValue() <= 0) {
            throw new RuntimeException("Số tiền thanh toán không hợp lệ.");
        }

        // 1. Cập nhật hoặc tạo Payment trong Database
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(PaymentMethod.PayOS);
            payment.setStatus(PaymentStatus.Pending);
            paymentRepository.save(payment);
        } else {
            if (payment.getStatus() == PaymentStatus.Successful) {
                throw new RuntimeException("Đơn hàng này đã được thanh toán thành công.");
            }
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(PaymentMethod.PayOS);
            payment.setStatus(PaymentStatus.Pending);
            paymentRepository.save(payment);
        }

        // 2. Tạo danh sách sản phẩm (PaymentLinkItem)
        List<PaymentLinkItem> items = new ArrayList<>();
        for (OrderItem orderItem : order.getOrderItems()) {
            String itemName = orderItem.getMenuItem().getName();
            if (itemName.length() > 50) itemName = itemName.substring(0, 50);
            
            // SỬA LỖI Ở ĐÂY: Đổi .intValue() thành .longValue()
            items.add(PaymentLinkItem.builder()
                    .name(itemName)
                    .quantity(orderItem.getQuantity())
                    .price(orderItem.getPriceAtOrder().longValue()) // [Fix] Sử dụng longValue()
                    .build());
        }

        // 3. Tạo Request tạo link
        long expiredAt = (System.currentTimeMillis() / 1000) + (15 * 60); // Hết hạn sau 15 phút
        String finalReturnUrl = returnUrl + "?orderId=" + orderId;
        String finalCancelUrl = cancelUrl + "?orderId=" + orderId;
        String description = "Thanh toan don " + orderId;

        // SỬA LỖI Ở ĐÂY: Đổi .intValue() thành .longValue()
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(Long.valueOf(orderId))
                .amount(order.getTotalAmount().longValue()) // [Fix] Sử dụng longValue()
                .description(description)
                .items(items)
                .returnUrl(finalReturnUrl)
                .cancelUrl(finalCancelUrl)
                .expiredAt(expiredAt)
                .build();

        // Gọi API qua paymentRequests()
        return payOS.paymentRequests().create(request);
    }

    /**
     * XỬ LÝ WEBHOOK TỪ PAYOS (V2)
     */
    @Transactional
    public void handlePayOSWebhook(ObjectNode webhookBody) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Webhook webhook = objectMapper.treeToValue(webhookBody, Webhook.class);
        
        // Xác thực Webhook (V2)
        WebhookData data = payOS.webhooks().verify(webhook);

        Integer orderId = (int) data.getOrderCode().longValue(); // Ép kiểu về int cho phù hợp DB
        String transactionId = data.getReference();

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán cho Order ID: " + orderId));

        if (payment.getStatus() != PaymentStatus.Successful) {
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.Successful);
            paymentRepository.save(payment);

            // Cập nhật trạng thái đơn hàng và bàn
            confirmPaymentInternal(payment.getId());
        }
    }

    /**
     * Logic nội bộ xác nhận thanh toán
     */
    private PaymentDTO confirmPaymentInternal(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.Failed) {
            throw new RuntimeException("Không thể xác nhận thanh toán đã thất bại");
        }
        
        if (payment.getStatus() != PaymentStatus.Successful) {
            payment.setStatus(PaymentStatus.Successful);
            paymentRepository.save(payment);
        }

        Order order = payment.getOrder();
        if (order != null && order.getStatus() != OrderStatus.Completed) {
            order.setStatus(OrderStatus.Completed);
            orderRepository.save(order);

            // Logic giải phóng bàn nếu là Dine-in
            if (order.getOrderType() == com.nhahang.restaurant.model.OrderType.Dinein && order.getTable() != null) {
                List<com.nhahang.restaurant.model.entity.Booking> bookings = bookingRepository.findByTableId(order.getTable().getId());
                
                com.nhahang.restaurant.model.entity.Booking activeBooking = bookings.stream()
                    .filter(b -> b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed 
                              || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Pending)
                    .findFirst()
                    .orElse(null);

                if (activeBooking != null) {
                    activeBooking.setStatus(com.nhahang.restaurant.model.BookingStatus.Completed);
                    bookingRepository.save(activeBooking);
                }

                com.nhahang.restaurant.model.entity.RestaurantTable table = order.getTable();
                table.setStatus(com.nhahang.restaurant.model.TableStatus.Available); 
                restaurantTableRepository.save(table);
            }
        }
        return convertToDTO(payment);
    }

    // --- CÁC PHƯƠNG THỨC KHÁC GIỮ NGUYÊN ---

    @Transactional
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDTO getPaymentById(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));
        return convertToDTO(payment);
    }

    @Transactional
    public PaymentDTO getPaymentByOrderId(Integer orderId) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder() != null && p.getOrder().getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán cho đơn hàng với ID: " + orderId));
        return convertToDTO(payment);
    }

    @Transactional
    public List<PaymentDTO> getPaymentsByStatus(String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            return paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == paymentStatus)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái thanh toán không hợp lệ: " + status);
        }
    }

    @Transactional
    public List<PaymentDTO> getPaymentsByMethod(String method) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.valueOf(method);
            return paymentRepository.findAll().stream()
                    .filter(p -> p.getPaymentMethod() == paymentMethod)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + method);
        }
    }

    @Transactional
    public PaymentDTO createPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        boolean hasPayment = paymentRepository.findAll().stream()
                .anyMatch(p -> p.getOrder() != null && p.getOrder().getId().equals(request.getOrderId()));
        if (hasPayment) {
            throw new RuntimeException("Đơn hàng này đã có thanh toán");
        }

        if (order.getStatus() == OrderStatus.Cancelled) {
            throw new RuntimeException("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thanh toán phải lớn hơn 0");
        }

        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với tổng tiền đơn hàng");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.Pending);
        payment.setTransactionId(request.getTransactionId());

        Payment savedPayment = paymentRepository.save(payment);
        return convertToDTO(savedPayment);
    }

    @Transactional
    public PaymentDTO confirmPayment(Integer id) {
        return confirmPaymentInternal(id);
    }

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

    @Transactional
    public PaymentDTO updatePaymentStatus(Integer id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        try {
            PaymentStatus newStatus = PaymentStatus.valueOf(status);
            if (payment.getStatus() == PaymentStatus.Successful && newStatus != PaymentStatus.Successful) {
                throw new RuntimeException("Không thể thay đổi trạng thái thanh toán đã thành công");
            }

            payment.setStatus(newStatus);

            if (newStatus == PaymentStatus.Successful && payment.getOrder() != null) {
                confirmPaymentInternal(id); // Tái sử dụng logic confirm
            } else {
                paymentRepository.save(payment);
            }
            
            return convertToDTO(payment);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái thanh toán không hợp lệ: " + status);
        }
    }

    @Transactional
    public void deletePayment(Integer id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán với ID: " + id));

        if (payment.getStatus() == PaymentStatus.Successful) {
            throw new RuntimeException("Không thể xóa thanh toán đã thành công");
        }

        paymentRepository.delete(payment);
    }

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

    @Transactional
    public RevenueReportDTO getRevenueReport(LocalDateTime fromDate, LocalDateTime toDate) {
        List<Payment> payments = paymentRepository.findByStatusAndPaymentTimeBetween(
                PaymentStatus.Successful, fromDate, toDate);

        RevenueReportDTO report = new RevenueReportDTO();
        report.setFromDate(fromDate);
        report.setToDate(toDate);

        if (payments.isEmpty()) {
            report.setTotalRevenue(BigDecimal.ZERO);
            report.setTotalTransactions(0L);
            report.setAverageTransactionValue(BigDecimal.ZERO);
            report.setCashRevenue(BigDecimal.ZERO);
            report.setCashTransactions(0L);
            report.setQrCodeRevenue(BigDecimal.ZERO);
            report.setQrCodeTransactions(0L);
            report.setCreditCardRevenue(BigDecimal.ZERO);
            report.setCreditCardTransactions(0L);
            return report;
        }

        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransactions = payments.size();

        BigDecimal averageTransactionValue = totalRevenue.divide(
                BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);

        report.setTotalRevenue(totalRevenue);
        report.setTotalTransactions(totalTransactions);
        report.setAverageTransactionValue(averageTransactionValue);

        // Phân loại theo phương thức thanh toán
        // Cash
        List<Payment> cashPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.Cash)
                .collect(Collectors.toList());
        report.setCashRevenue(cashPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setCashTransactions((long) cashPayments.size());

        // PayOS (Tạm thời gộp vào QRCode hoặc CreditCard tùy logic hiển thị, ở đây để riêng hoặc map vào 1 loại)
        // Ví dụ: PayOS coi như QR Code
        List<Payment> payOSPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.PayOS)
                .collect(Collectors.toList());
        report.setQrCodeRevenue(payOSPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setQrCodeTransactions((long) payOSPayments.size());

        return report;
    }

    @Transactional
    public List<PaymentMethodDistributionDTO> getPaymentMethodDistribution(
            LocalDateTime fromDate, LocalDateTime toDate) {
        
        List<Payment> payments = paymentRepository.findByStatusAndPaymentTimeBetween(
                PaymentStatus.Successful, fromDate, toDate);

        if (payments.isEmpty()) {
            return new ArrayList<>();
        }

        BigDecimal grandTotal = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<PaymentMethod, List<Payment>> groupedByMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod));

        List<PaymentMethodDistributionDTO> distribution = new ArrayList<>();

        for (Map.Entry<PaymentMethod, List<Payment>> entry : groupedByMethod.entrySet()) {
            PaymentMethod method = entry.getKey();
            List<Payment> methodPayments = entry.getValue();

            PaymentMethodDistributionDTO dto = new PaymentMethodDistributionDTO();
            dto.setPaymentMethod(method.name());
            dto.setTransactionCount((long) methodPayments.size());

            BigDecimal totalAmount = methodPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalAmount(totalAmount);

            BigDecimal percentage = BigDecimal.ZERO;
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = totalAmount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 2, RoundingMode.HALF_UP);
            }
            dto.setPercentage(percentage);

            distribution.add(dto);
        }

        distribution.sort((a, b) -> b.getTransactionCount().compareTo(a.getTransactionCount()));

        return distribution;
    }
}
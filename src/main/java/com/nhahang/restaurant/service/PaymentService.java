package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.PaymentCreateRequest;
import com.nhahang.restaurant.dto.PaymentDTO;
import com.nhahang.restaurant.dto.PaymentMethodDistributionDTO;
import com.nhahang.restaurant.dto.RevenueReportDTO;
import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.PaymentMethod;
import com.nhahang.restaurant.model.PaymentStatus;
import com.nhahang.restaurant.model.entity.Order;
import com.nhahang.restaurant.model.entity.Payment;
import com.nhahang.restaurant.repository.OrderRepository;
import com.nhahang.restaurant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

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
    private final PayOS payOS;
    private final com.nhahang.restaurant.repository.BookingRepository bookingRepository;
    private final com.nhahang.restaurant.repository.RestaurantTableRepository restaurantTableRepository;
    @Value("${payos.return-url}")
    private String returnUrl;
    @Value("${payos.cancel-url}")
    private String cancelUrl;
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
     * Lấy tất cả thanh toán với phân trang
     */
    @Transactional
    public List<PaymentDTO> getAllPayments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> paymentPage = paymentRepository.findAll(pageable);
        return paymentPage.getContent().stream()
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
     * Lấy thanh toán theo trạng thái với phân trang
     */
    @Transactional
    public List<PaymentDTO> getPaymentsByStatus(String status, int page, int size) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
            Pageable pageable = PageRequest.of(page, size);
            Page<Payment> paymentPage = paymentRepository.findAll(pageable);
            
            return paymentPage.getContent().stream()
                    .filter(p -> p.getStatus() == paymentStatus)
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

        // 1. Cập nhật Order
        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.Completed);
            orderRepository.save(order);

            // 2. LOGIC MỚI: Cập nhật Booking và Table nếu là Dine-in
            if (order.getOrderType() == com.nhahang.restaurant.model.OrderType.Dinein
                && order.getTable() != null) {

                // Tìm booking đang active của bàn này
                java.util.List<com.nhahang.restaurant.model.entity.Booking> bookings = bookingRepository.findByTableId(order.getTable().getId());

                // Lấy booking Confirmed gần nhất (đang diễn ra)
                com.nhahang.restaurant.model.entity.Booking activeBooking = bookings.stream()
                    .filter(b -> b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed)
                    .findFirst()
                    .orElse(null);

                if (activeBooking != null) {
                    activeBooking.setStatus(com.nhahang.restaurant.model.BookingStatus.Completed);
                    bookingRepository.save(activeBooking);
                }

                // Giải phóng bàn (Chuyển về Available hoặc Cleaning)
                com.nhahang.restaurant.model.entity.RestaurantTable table = order.getTable();
                table.setStatus(com.nhahang.restaurant.model.TableStatus.Available); // Hoặc Cleaning
                restaurantTableRepository.save(table);
            }
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

    /**
     * Lấy báo cáo doanh thu theo khoảng thời gian
     */
    @Transactional
    public RevenueReportDTO getRevenueReport(LocalDateTime fromDate, LocalDateTime toDate) {
        // Lấy tất cả payment thành công trong khoảng thời gian
        List<Payment> payments = paymentRepository.findByStatusAndPaymentTimeBetween(
                PaymentStatus.Successful, fromDate, toDate);

        RevenueReportDTO report = new RevenueReportDTO();
        report.setFromDate(fromDate);
        report.setToDate(toDate);

        if (payments.isEmpty()) {
            // Nếu không có giao dịch nào, trả về report với giá trị 0
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

        // Tính tổng doanh thu
        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng số giao dịch
        long totalTransactions = payments.size();

        // Tính giá trị trung bình mỗi giao dịch
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

        return report;
    }

    /**
     * Lấy phân phối phương thức thanh toán
     */
    @Transactional
    public List<PaymentMethodDistributionDTO> getPaymentMethodDistribution(
            LocalDateTime fromDate, LocalDateTime toDate) {
        
        // Lấy tất cả payment thành công trong khoảng thời gian
        List<Payment> payments = paymentRepository.findByStatusAndPaymentTimeBetween(
                PaymentStatus.Successful, fromDate, toDate);

        if (payments.isEmpty()) {
            return new ArrayList<>();
        }

        // Tính tổng tiền
        BigDecimal grandTotal = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Nhóm theo phương thức thanh toán
        Map<PaymentMethod, List<Payment>> groupedByMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::getPaymentMethod));

        // Tạo danh sách kết quả
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

            // Tính phần trăm
            BigDecimal percentage = BigDecimal.ZERO;
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = totalAmount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 2, RoundingMode.HALF_UP);
            }
            dto.setPercentage(percentage);

            distribution.add(dto);
        }

        // Sắp xếp theo số lượng giao dịch giảm dần
        distribution.sort((a, b) -> b.getTransactionCount().compareTo(a.getTransactionCount()));

        return distribution;
    }
    /**
     * TẠO LINK THANH TOÁN PAYOS
     */
    @Transactional
    public CheckoutResponseData createPayOSLink(Integer orderId) {
        // 1. Lấy Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getTotalAmount().intValue() <= 0) {
            throw new RuntimeException("Số tiền phải lớn hơn 0");
        }

        // 2. Tạo ItemData cho PayOS (để hiển thị tên món ăn ở màn hình thanh toán)
        List<ItemData> items = order.getOrderItems().stream()
                .map(item -> ItemData.builder()
                        .name(item.getMenuItem().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPriceAtOrder().intValue())
                        .build())
                .collect(Collectors.toList());

        // 3. Tạo PaymentData
        // Lưu ý: orderCode của PayOS yêu cầu là số nguyên (long). Ta dùng orderId.
        // Nếu cần unique mỗi lần thử lại, có thể dùng System.currentTimeMillis() và lưu mapping lại.
        long orderCode = Long.valueOf(order.getId()); 
        
        String description = "Thanh toan don hang " + orderCode;
        // Cắt chuỗi description nếu quá dài (PayOS giới hạn 25 ký tự cho description khi chuyển khoản)
        if(description.length() > 25) description = description.substring(0, 25);

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(order.getTotalAmount().intValue())
                .description(description)
                .items(items)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        try {
            // 4. Gọi PayOS để lấy link
            CheckoutResponseData data = payOS.createPaymentLink(paymentData);

            // 5. Lưu thông tin Payment vào Database (Trạng thái Pending)
            // Kiểm tra xem payment cho order này đã tồn tại chưa
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
            
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(PaymentMethod.PayOS);
            payment.setStatus(PaymentStatus.Pending);
            payment.setTransactionId(String.valueOf(orderCode)); // Lưu orderCode làm transactionId
            
            paymentRepository.save(payment);

            return data;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo link PayOS: " + e.getMessage());
        }
    }

    /**
     * XỬ LÝ WEBHOOK TỪ PAYOS
     */
    @Transactional
    public void handlePayOSWebhook(Webhook webhookBody) {
        try {
            // 1. Xác thực dữ liệu Webhook (đảm bảo do PayOS gửi)
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            
            // 2. Lấy orderCode từ webhook (chính là orderId mình đã gửi đi)
            long orderCode = data.getOrderCode();
            Integer orderId = (int) orderCode;

            // 3. Tìm Payment trong DB
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán cho Order ID: " + orderId));

            // 4. Kiểm tra và cập nhật trạng thái
            // Nếu PayOS báo success (code == "00")
            if ("00".equals(data.getCode())) {
                if (payment.getStatus() != PaymentStatus.Successful) {
                    payment.setStatus(PaymentStatus.Successful);
                    payment.setTransactionId(data.getReference()); // Update mã tham chiếu thực tế từ ngân hàng nếu cần
                    paymentRepository.save(payment);

                    // Cập nhật trạng thái Order
                    Order order = payment.getOrder();
                    if (order != null) {
                        order.setStatus(OrderStatus.Completed);
                        orderRepository.save(order);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi xử lý Webhook: " + e.getMessage());
        }
    }
}

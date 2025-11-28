package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.*;
import com.nhahang.restaurant.model.MenuItemStatus;
import com.nhahang.restaurant.model.OrderStatus;
import com.nhahang.restaurant.model.OrderType;
import com.nhahang.restaurant.model.entity.*;
import com.nhahang.restaurant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final MenuItemRepository menuItemRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Integer userId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserEmail(String email) {
        List<Order> orders = orderRepository.findByUserEmail(email);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserPhoneNumber(String phoneNumber) {
        List<Order> orders = orderRepository.findByUserPhoneNumber(phoneNumber);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByTableId(Integer tableId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getTable() != null && order.getTable().getId().equals(tableId))
                .collect(Collectors.toList());
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            List<Order> orders = orderRepository.findByStatus(orderStatus);
            return orders.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
    }

    @Transactional
    public OrderDTO createOrder(OrderCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + request.getUserId()));

        OrderType orderType;
        try {
            orderType = OrderType.valueOf(request.getOrderType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại đơn hàng không hợp lệ: " + request.getOrderType());
        }

        RestaurantTable table = null;
        if (orderType == OrderType.Takeaway) {
            if (request.getTableId() != null) {
                throw new RuntimeException("Đơn hàng mang đi không cần bàn");
            }
        } else {
            if (request.getTableId() == null) {
                throw new RuntimeException("Đơn hàng tại chỗ phải có bàn");
            }
            table = restaurantTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));

            LocalDateTime now = LocalDateTime.now();
            List<Booking> bookings = bookingRepository.findByTableId(table.getId());
            boolean hasValidBooking = bookings.stream().anyMatch(b ->
                b.getUser() != null &&
                b.getUser().getId().equals(request.getUserId()) &&
                (b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Pending)  && 
                b.getBookingTime() != null &&
                (b.getBookingTime().isAfter(now.minusMinutes(30)))
            );
            if (!hasValidBooking) {
                throw new RuntimeException("Đơn tại chỗ phải có booking bàn hợp lệ và đúng thời gian đặt!");
            }
        }

        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một món");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTable(table);
        order.setOrderType(orderType);
        order.setStatus(OrderStatus.Pending);
        order.setTotalAmount(BigDecimal.ZERO);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        var mergedItems = request.getOrderItems().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::getMenuItemId,
                        Collectors.summingInt(OrderItemRequest::getQuantity)
                ));

        for (var entry : mergedItems.entrySet()) {
            Integer menuItemId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + menuItemId));
            if (menuItem.getStatus() != MenuItemStatus.Available) {
                throw new RuntimeException("Món ăn không khả dụng: " + menuItem.getName());
            }
            if (totalQuantity <= 0) {
                throw new RuntimeException("Số lượng món ăn phải lớn hơn 0");
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(totalQuantity);
            orderItem.setPriceAtOrder(menuItem.getPrice());

            orderItems.add(orderItem);
            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(totalQuantity));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        if (orderType == OrderType.Dinein && table != null && user != null) {
            List<Booking> bookings = bookingRepository.findByTableId(table.getId());
            LocalDateTime now = LocalDateTime.now();
            Booking matched = bookings.stream()
                .filter(b -> b.getUser() != null && b.getUser().getId().equals(user.getId())
                        && (b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Pending)
                        && b.getBookingTime() != null
                        && (b.getBookingTime().isAfter(now.minusDays(1)))
                )
                .sorted((b1, b2) -> b2.getBookingTime().compareTo(b1.getBookingTime()))
                .findFirst().orElse(null);
            if (matched != null) {
                matched.setStatus(com.nhahang.restaurant.model.BookingStatus.Confirmed);
                bookingRepository.save(matched);
            }
        }

        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO updateOrder(Integer id, OrderCreateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        if (order.getStatus() == OrderStatus.Cancelled) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã bị hủy");
        }

        if (order.getStatus() == OrderStatus.Completed) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã hoàn thành");
        }

        OrderType newOrderType = order.getOrderType();
        if (request.getOrderType() != null) {
            try {
                newOrderType = OrderType.valueOf(request.getOrderType());
                order.setOrderType(newOrderType);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Loại đơn hàng không hợp lệ: " + request.getOrderType());
            }
        }

        if (newOrderType == OrderType.Takeaway) {
            if (request.getTableId() != null) {
                throw new RuntimeException("Đơn hàng mang đi không cần bàn");
            }
            order.setTable(null);
        } else {
            if (request.getTableId() != null) {
                RestaurantTable newTable = restaurantTableRepository.findById(request.getTableId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));
                order.setTable(newTable);
            } else if (order.getTable() == null) {
                throw new RuntimeException("Đơn hàng tại chỗ phải có bàn");
            }
        }

        order.getOrderItems().clear();
        orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        
        var mergedItems = request.getOrderItems().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::getMenuItemId,
                        Collectors.summingInt(OrderItemRequest::getQuantity)
                ));

        for (var entry : mergedItems.entrySet()) {
            Integer menuItemId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + menuItemId));

            if (menuItem.getStatus() != MenuItemStatus.Available) {
                throw new RuntimeException("Món ăn '" + menuItem.getName() + "' hiện không khả dụng");
            }

            if (totalQuantity <= 0) {
                throw new RuntimeException("Số lượng món ăn phải lớn hơn 0");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(totalQuantity);
            orderItem.setPriceAtOrder(menuItem.getPrice());

            order.getOrderItems().add(orderItem);

            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(totalQuantity));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Integer id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status);
            
            if (order.getStatus() == OrderStatus.Cancelled) {
                throw new RuntimeException("Không thể cập nhật đơn hàng đã bị hủy");
            }

            if (order.getStatus() == OrderStatus.Completed) {
                throw new RuntimeException("Không thể cập nhật đơn hàng đã hoàn thành");
            }

            order.setStatus(newStatus);
            Order updatedOrder = orderRepository.save(order);
            return convertToDTO(updatedOrder);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
    }

    @Transactional
    public OrderDTO cancelOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        if (order.getStatus() == OrderStatus.Cancelled) {
            throw new RuntimeException("Đơn hàng đã được hủy trước đó");
        }

        if (order.getStatus() == OrderStatus.Completed) {
            throw new RuntimeException("Không thể hủy đơn hàng đã hoàn thành");
        }

        if (order.getOrderType() == OrderType.Dinein && order.getTable() != null && order.getUser() != null) {
            List<Booking> bookings = bookingRepository.findByTableId(order.getTable().getId());
            LocalDateTime now = LocalDateTime.now();
            Booking matched = bookings.stream()
                .filter(b -> b.getUser() != null && b.getUser().getId().equals(order.getUser().getId())
                        && (b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Pending)
                        && b.getBookingTime() != null
                        && (b.getBookingTime().isAfter(now.minusDays(1)) && b.getBookingTime().isBefore(now.plusDays(2)))
                )
                .sorted((b1, b2) -> b2.getBookingTime().compareTo(b1.getBookingTime()))
                .findFirst().orElse(null);
            if (matched != null) {
                matched.setStatus(com.nhahang.restaurant.model.BookingStatus.Cancelled);
                bookingRepository.save(matched);
            }
            RestaurantTable table = order.getTable();
            if (table != null) {
                table.setStatus(com.nhahang.restaurant.model.TableStatus.Available);
                restaurantTableRepository.save(table);
            }
        }

        order.setStatus(OrderStatus.Cancelled);
        Order cancelledOrder = orderRepository.save(order);
        return convertToDTO(cancelledOrder);
    }

    @Transactional
    public void deleteOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        orderRepository.delete(order);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        dto.setUserFullName(order.getUser() != null ? order.getUser().getFullName() : null);
        dto.setTableId(order.getTable() != null ? order.getTable().getId() : null);
        dto.setTableName(order.getTable() != null ? "Bàn " + order.getTable().getTableNumber() : null);
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus().name());
        dto.setOrderType(order.getOrderType().name());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getOrderItems() != null) {
            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                    .map(this::convertOrderItemToDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItemDTOs);
        }

        if (order.getOrderType() == OrderType.Dinein && order.getTable() != null && order.getUser() != null) {
            List<Booking> bookings = bookingRepository.findByTableId(order.getTable().getId());
            
            // Dùng thời gian tạo đơn hàng làm mốc tìm kiếm (không dùng LocalDateTime.now())
            LocalDateTime orderTime = order.getCreatedAt();
            
            Booking matched = bookings.stream()
                .filter(b -> b.getUser() != null && b.getUser().getId().equals(order.getUser().getId())
                        // Bao gồm cả trạng thái Completed vì đơn hàng đã thanh toán (Completed) thì booking cũng Completed
                        && (b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Confirmed 
                            || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Pending
                            || b.getStatus() == com.nhahang.restaurant.model.BookingStatus.Completed)
                        && b.getBookingTime() != null
                        // Tìm booking trong khoảng thời gian xung quanh lúc tạo đơn (ví dụ +/- 6 tiếng)
                        && b.getBookingTime().isAfter(orderTime.minusHours(6)) 
                        && b.getBookingTime().isBefore(orderTime.plusHours(6))
                )
                // Lấy booking gần nhất với thời điểm tạo đơn
                .sorted((b1, b2) -> b2.getBookingTime().compareTo(b1.getBookingTime()))
                .findFirst().orElse(null);
                
            if (matched != null) {
                dto.setBookingTime(matched.getBookingTime());
            }
        }

        if (order.getPayment() != null) {
            dto.setPaymentStatus(order.getPayment().getStatus().name());
        } else {
            dto.setPaymentStatus(null);
        }

        return dto;
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setMenuItemId(orderItem.getMenuItem() != null ? orderItem.getMenuItem().getId() : null);
        dto.setMenuItemName(orderItem.getMenuItem() != null ? orderItem.getMenuItem().getName() : null);
        dto.setQuantity(orderItem.getQuantity());
        dto.setPriceAtOrder(orderItem.getPriceAtOrder());
        dto.setSubtotal(orderItem.getPriceAtOrder().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        return dto;
    }

    @Transactional(readOnly = true)
    public MonthlyOrderStatsDTO getMonthlyOrderStats(Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);

        MonthlyOrderStatsDTO stats = new MonthlyOrderStatsDTO();
        stats.setYear(year);
        stats.setMonth(month);
        stats.setTotalOrders((long) orders.size());

        long completedOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.Completed)
                .count();
        stats.setCompletedOrders(completedOrders);

        long cancelledOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.Cancelled)
                .count();
        stats.setCancelledOrders(cancelledOrders);

        long pendingOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.Pending)
                .count();
        stats.setPendingOrders(pendingOrders);

        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.Completed)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<MonthlyOrderStatsDTO> getMonthlyOrderStatsRange(Integer year, Integer fromMonth, Integer toMonth) {
        List<MonthlyOrderStatsDTO> statsList = new ArrayList<>();
        
        for (int month = fromMonth; month <= toMonth; month++) {
            MonthlyOrderStatsDTO stats = getMonthlyOrderStats(year, month);
            statsList.add(stats);
        }
        
        return statsList;
    }
}
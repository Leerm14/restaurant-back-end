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

    /**
     * Lấy tất cả đơn hàng
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả đơn hàng với phân trang
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đơn hàng theo ID
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        return convertToDTO(order);
    }

    /**
     * Lấy đơn hàng theo user ID
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Integer userId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đơn hàng theo table ID
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByTableId(Integer tableId) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getTable() != null && order.getTable().getId().equals(tableId))
                .collect(Collectors.toList());
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy đơn hàng theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            List<Order> orders = orderRepository.findAll().stream()
                    .filter(order -> order.getStatus() == orderStatus)
                    .collect(Collectors.toList());
            return orders.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
    }

    /**
     * Lấy đơn hàng theo trạng thái với phân trang
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status, int page, int size) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orderPage = orderRepository.findAll(pageable);
            
            return orderPage.getContent().stream()
                    .filter(order -> order.getStatus() == orderStatus)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ: " + status);
        }
    }

    /**
     * Tạo đơn hàng mới
     */
    @Transactional
    public OrderDTO createOrder(OrderCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + request.getUserId()));

        RestaurantTable table = null;
        if (request.getTableId() != null) {
            table = restaurantTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));
        }

        // Kiểm tra orderType
        OrderType orderType;
        try {
            orderType = OrderType.valueOf(request.getOrderType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại đơn hàng không hợp lệ: " + request.getOrderType());
        }

        // Kiểm tra danh sách món ăn
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

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemRequest.getMenuItemId()));
            if (menuItem.getStatus() != MenuItemStatus.Available) {
                throw new RuntimeException("Món ăn không khả dụng: " + menuItem.getName());
            }
            if (itemRequest.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng món ăn phải lớn hơn 0");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPriceAtOrder(menuItem.getPrice());

            orderItems.add(orderItem);
            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        return convertToDTO(savedOrder);
    }

    /**
     * Cập nhật đơn hàng
     */
    @Transactional
    public OrderDTO updateOrder(Integer id, OrderCreateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        // Kiểm tra trạng thái
        if (order.getStatus() == OrderStatus.Cancelled) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã bị hủy");
        }

        if (order.getStatus() == OrderStatus.Completed) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã hoàn thành");
        }

        // Cập nhật table nếu thay đổi
        if (request.getTableId() != null && !order.getTable().getId().equals(request.getTableId())) {
            RestaurantTable newTable = restaurantTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + request.getTableId()));
            order.setTable(newTable);
        }

        // Cập nhật orderType nếu có
        if (request.getOrderType() != null) {
            try {
                OrderType orderType = OrderType.valueOf(request.getOrderType());
                order.setOrderType(orderType);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Loại đơn hàng không hợp lệ: " + request.getOrderType());
            }
        }

        order.getOrderItems().clear();
        orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemRequest.getMenuItemId()));

            if (menuItem.getStatus() != MenuItemStatus.Available) {
                throw new RuntimeException("Món ăn '" + menuItem.getName() + "' hiện không khả dụng");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPriceAtOrder(menuItem.getPrice());

            order.getOrderItems().add(orderItem);

            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    @Transactional
    public OrderDTO updateOrderStatus(Integer id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status);
            
            // Kiểm tra logic chuyển trạng thái
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

    /**
     * Hủy đơn hàng
     */
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

        order.setStatus(OrderStatus.Cancelled);
        Order cancelledOrder = orderRepository.save(order);
        return convertToDTO(cancelledOrder);
    }

    /**
     * Xóa đơn hàng
     */
    @Transactional
    public void deleteOrder(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
        orderRepository.delete(order);
    }

    /**
     * Chuyển đổi Order entity sang OrderDTO
     */
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

        // Chuyển đổi order items
        if (order.getOrderItems() != null) {
            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                    .map(this::convertOrderItemToDTO)
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItemDTOs);
        }

        return dto;
    }

    /**
     * Chuyển đổi OrderItem entity sang OrderItemDTO
     */
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

    /**
     * Lấy thống kê đơn hàng theo tháng
     */
    @Transactional(readOnly = true)
    public MonthlyOrderStatsDTO getMonthlyOrderStats(Integer year, Integer month) {
        // Tạo khoảng thời gian cho tháng
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Lấy tất cả đơn hàng trong tháng
        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);

        MonthlyOrderStatsDTO stats = new MonthlyOrderStatsDTO();
        stats.setYear(year);
        stats.setMonth(month);
        stats.setTotalOrders((long) orders.size());

        // Đếm số đơn hàng theo trạng thái
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

        // Tính tổng doanh thu từ các đơn hàng đã hoàn thành
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.Completed)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        return stats;
    }

    /**
     * Lấy thống kê đơn hàng cho nhiều tháng
     */
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

package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.MonthlyOrderStatsDTO;
import com.nhahang.restaurant.dto.OrderCreateRequest;
import com.nhahang.restaurant.dto.OrderDTO;
import com.nhahang.restaurant.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    /**
     * Lấy tất cả đơn hàng
     */
    @GetMapping
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<List<OrderDTO>> getAllOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validation
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100;
            
            List<OrderDTO> orders = orderService.getAllOrders(page, size);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy đơn hàng theo ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Integer id) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy đơn hàng theo user ID
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Integer userId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy đơn hàng theo table ID
     */
    @GetMapping("/table/{tableId}")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByTableId(@PathVariable Integer tableId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByTableId(tableId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy đơn hàng theo trạng thái
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validation
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100;
            
            List<OrderDTO> orders = orderService.getOrdersByStatus(status, page, size);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Tạo đơn hàng mới
     */
    @PostMapping
    @PreAuthorize("haspermission('CREATE_ORDER')")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderCreateRequest request) {
        try {
            OrderDTO createdOrder = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("haspermission('UPDATE_ORDER')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Hủy đơn hàng
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("haspermission('UPDATE_ORDER')")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Integer id) {
        try {
            OrderDTO cancelledOrder = orderService.cancelOrder(id);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Xóa đơn hàng
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("haspermission('DELETE_ORDER')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thống kê đơn hàng theo tháng
     * @param year Năm (mặc định: năm hiện tại)
     * @param month Tháng (mặc định: tháng hiện tại)
     */
    @GetMapping("/stats/monthly")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<MonthlyOrderStatsDTO> getMonthlyOrderStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            // Nếu không có tham số, dùng tháng hiện tại
            LocalDate now = LocalDate.now();
            int selectedYear = (year != null) ? year : now.getYear();
            int selectedMonth = (month != null) ? month : now.getMonthValue();

            // Validate tháng (1-12)
            if (selectedMonth < 1 || selectedMonth > 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Validate năm (phải > 2000 và <= năm hiện tại + 1)
            if (selectedYear < 2000 || selectedYear > now.getYear() + 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            MonthlyOrderStatsDTO stats = orderService.getMonthlyOrderStats(selectedYear, selectedMonth);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy thống kê đơn hàng cho nhiều tháng
     * @param year Năm
     * @param fromMonth Từ tháng (1-12)
     * @param toMonth Đến tháng (1-12)
     */
    @GetMapping("/stats/monthly-range")
    @PreAuthorize("haspermission('READ_ORDER')")
    public ResponseEntity<List<MonthlyOrderStatsDTO>> getMonthlyOrderStatsRange(
            @RequestParam Integer year,
            @RequestParam Integer fromMonth,
            @RequestParam Integer toMonth) {
        try {
            // Validate tháng
            if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            if (fromMonth > toMonth) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Validate năm
            LocalDate now = LocalDate.now();
            if (year < 2000 || year > now.getYear() + 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            List<MonthlyOrderStatsDTO> statsList = orderService.getMonthlyOrderStatsRange(year, fromMonth, toMonth);
            return ResponseEntity.ok(statsList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

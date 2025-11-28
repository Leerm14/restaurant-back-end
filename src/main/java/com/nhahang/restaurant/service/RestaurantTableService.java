package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.TableDTO;
import com.nhahang.restaurant.model.entity.Booking;
import com.nhahang.restaurant.repository.BookingRepository;
import com.nhahang.restaurant.repository.RestaurantTableRepository;
import com.nhahang.restaurant.model.entity.RestaurantTable;
import com.nhahang.restaurant.model.TableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {
    private final RestaurantTableRepository restaurantTableRepository;
    private final BookingRepository bookingRepository; // [MỚI] Inject thêm BookingRepository

    /**
     * Lấy thống kê số bàn (tổng số và theo từng trạng thái)
     */
    public java.util.Map<String, Object> getTablesCountStatistics() {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        
        // Tổng số bàn
        long totalCount = restaurantTableRepository.count();
        statistics.put("total", totalCount);
        
        // Số bàn theo từng trạng thái
        java.util.Map<String, Long> byStatus = new java.util.HashMap<>();
        for (TableStatus status : TableStatus.values()) {
            long count = restaurantTableRepository.countByStatus(status);
            byStatus.put(status.name(), count);
        }
        statistics.put("byStatus", byStatus);
        
        return statistics;
    }

    /**
     * Lấy tất cả bàn 
     */
    public List<RestaurantTable> getAllTables() {
        return restaurantTableRepository.findAll();
    }

    /**
     * Lấy bàn theo number
     */
    public Optional<RestaurantTable> getTableByNumber(int tableNumber) {
        return restaurantTableRepository.findByTableNumber(tableNumber);
    }

    /**
     * Lấy tất cả bàn đang available
     */
    public List<RestaurantTable> getAvailableTables() {
        return restaurantTableRepository.findByStatus(TableStatus.Available);
    }

    /**
     * Lấy tất cả bàn theo status
     */
    public List<RestaurantTable> getTablesByStatus(TableStatus status) {
        return restaurantTableRepository.findByStatus(status);
    }

    /**
     * [MỚI] Lấy trạng thái các bàn tại một thời điểm cụ thể
     * Logic: Tìm các booking trong khoảng [time - 2h, time + 2h].
     * Bàn nào có booking trong khoảng này sẽ bị đánh dấu là Booked.
     */
    public List<RestaurantTable> getTablesStatusAtTime(LocalDateTime checkTime) {
        // Lấy tất cả các bàn
        List<RestaurantTable> allTables = restaurantTableRepository.findAll();

        // Xác định khung giờ va chạm (2 tiếng trước và sau giờ ăn)
        LocalDateTime startCheck = checkTime.minusHours(2);
        LocalDateTime endCheck = checkTime.plusHours(2);

        // Tìm các booking đã tồn tại trong khung giờ này
        List<Booking> conflicts = bookingRepository.findConflictingBookings(startCheck, endCheck);
        
        // Lấy danh sách ID các bàn đã bị đặt
        Set<Integer> bookedTableIds = conflicts.stream()
                .map(b -> b.getTable().getId())
                .collect(Collectors.toSet());

        // Cập nhật trạng thái hiển thị (Chỉ update trên object trả về, không lưu DB)
        for (RestaurantTable table : allTables) {
            if (bookedTableIds.contains(table.getId())) {
                table.setStatus(TableStatus.Booked);
            } else {
                // Nếu không bị đặt, coi như là Trống để khách chọn
                table.setStatus(TableStatus.Available);
            }
        }
        return allTables;
    }

    /**
     * Thêm bàn mới
     */
    public RestaurantTable createTable(TableDTO tableDTO) {
        if (restaurantTableRepository.findByTableNumber(tableDTO.getTableNumber()).isPresent()) {
            throw new RuntimeException("Bàn với số '" + tableDTO.getTableNumber() + "' đã tồn tại");
        }

        RestaurantTable newTable = new RestaurantTable();
        newTable.setTableNumber(tableDTO.getTableNumber());
        newTable.setCapacity(tableDTO.getCapacity());
        newTable.setStatus(TableStatus.Available);

        return restaurantTableRepository.save(newTable);
    }

    /**
     * Cập nhật thông tin bàn
     */
    public RestaurantTable updateTable(Integer id, TableDTO tableDTO) {
        RestaurantTable existingTable = restaurantTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));
        TableStatus status;
        try {
            status = TableStatus.valueOf(tableDTO.getStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status không hợp lệ: " + tableDTO.getStatus());
        }

        existingTable.setTableNumber(tableDTO.getTableNumber());
        existingTable.setCapacity(tableDTO.getCapacity());
        existingTable.setStatus(status);

        return restaurantTableRepository.save(existingTable);
    }

    /**
     * Cập nhật trạng thái bàn
     */
    public RestaurantTable updateTableStatus(Integer id, String statusStr) {
        RestaurantTable existingTable = restaurantTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));
        TableStatus status;
        try {
            status = TableStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status không hợp lệ: " + statusStr);
        }

        existingTable.setStatus(status);
        return restaurantTableRepository.save(existingTable);
    }

    /**
     * Xóa bàn
     */
    public void deleteTable(Integer id) {
        RestaurantTable existingTable = restaurantTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));
        restaurantTableRepository.delete(existingTable);
    }
}
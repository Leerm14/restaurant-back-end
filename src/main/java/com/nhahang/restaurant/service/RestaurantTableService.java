package com.nhahang.restaurant.service;
import com.nhahang.restaurant.dto.TableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.nhahang.restaurant.repository.RestaurantTableRepository;
import com.nhahang.restaurant.model.entity.RestaurantTable;
import com.nhahang.restaurant.model.TableStatus;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class RestaurantTableService {
    private final RestaurantTableRepository restaurantTableRepository;

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
     * Lấy tất cả bàn đang booked
     */
    public List<RestaurantTable> getBookedTables() {
        return restaurantTableRepository.findByStatus(TableStatus.Booked);
    }
    /**
     * Lấy tất cả bàn đang cleaning
     */
    public List<RestaurantTable> getCleaningTables() {
        return restaurantTableRepository.findByStatus(TableStatus.Cleaning);
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
     * Xóa bàn
     */
    public void deleteTable(Integer id) {
        RestaurantTable existingTable = restaurantTableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));
        restaurantTableRepository.delete(existingTable);
    }
}

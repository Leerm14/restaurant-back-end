package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.TableDTO;
import com.nhahang.restaurant.model.entity.RestaurantTable;
import com.nhahang.restaurant.service.RestaurantTableService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class RestaurantTableController {
    private final RestaurantTableService restaurantTableService;
    // --- API 1: LẤY TẤT CẢ BÀN THEO STATUS ---
    @GetMapping
    // @PreAuthorize("hasAuthority('READ_TABLE')")
    public ResponseEntity<List<RestaurantTable>> getTablesByStatus(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        // Validation
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        List<RestaurantTable> tables;
        if (status == null) {
            tables = restaurantTableService.getAllTables();
        } else {
            com.nhahang.restaurant.model.TableStatus tableStatus;
            try {
                tableStatus = com.nhahang.restaurant.model.TableStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
            tables = restaurantTableService.getTablesByStatus(tableStatus, page, size);
        }
        return ResponseEntity.ok(tables);
    }
    // --- API 2: LẤY BÀN THEO SỐ BÀN ---
    @GetMapping("/{tableNumber}")
    // @PreAuthorize("hasAuthority('READ_TABLE')")
    public ResponseEntity<RestaurantTable> getTableByNumber(@PathVariable int tableNumber) {
        return restaurantTableService.getTableByNumber(tableNumber)
                .map(table -> ResponseEntity.ok(table))
                .orElse(ResponseEntity.notFound().build());
    }
    // --- API 3: THÊM BÀN MỚI ---
    @PostMapping
    // @PreAuthorize("hasAuthority('CREATE_TABLE')")
    public ResponseEntity<RestaurantTable> createTable(@RequestBody TableDTO tableDTO) {
        try{
            RestaurantTable createdTable = restaurantTableService.createTable(tableDTO);
        return new ResponseEntity<>(createdTable, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().build();
        }
    }
    // --- API 4: CẬP NHẬT THÔNG TIN BÀN ---
    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('UPDATE_TABLE')")
    public ResponseEntity<RestaurantTable> updateTable(@PathVariable Integer id, @RequestBody TableDTO tableDTO) {
        try {
            RestaurantTable updatedTable = restaurantTableService.updateTable(id, tableDTO);
            return ResponseEntity.ok(updatedTable);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // --- API 5: SỬA TRẠNG THÁI BÀN ---
    @PatchMapping("/{id}/status")
    // @PreAuthorize("hasAuthority('UPDATE_TABLE')")
    public ResponseEntity<RestaurantTable> updateTableStatus(@PathVariable Integer id, @RequestParam String status) {
        try {
            RestaurantTable updatedTable = restaurantTableService.updateTableStatus(id, status);
            return ResponseEntity.ok(updatedTable);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    

    // --- API 6: XÓA BÀN ---
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('DELETE_TABLE')")
    public ResponseEntity<Void> deleteTable(@PathVariable Integer id) {
        try {
            restaurantTableService.deleteTable(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.BestSellingItemDTO;
import com.nhahang.restaurant.dto.MenuItemDTO; // Import DTO ta đã tạo
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.web.multipart.MultipartFile;



@RestController 
@RequestMapping("/api/menu") 
@RequiredArgsConstructor 
public class MenuController {

    private final MenuService menuService;

    // --- API 0: LẤY TỔNG SỐ TRANG ---
    @GetMapping("/page-count")
    @PreAuthorize("hasAuthority('READ_MENU')")
    public ResponseEntity<Map<String, Object>> getPageCount(
            @RequestParam(value = "available", required = false) Boolean available,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            // Validation
            if (size <= 0) size = 10;
            if (size > 100) size = 100;
            
            Map<String, Object> result = menuService.getPageCount(available, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // --- API 1: LẤY TẤT CẢ MÓN ĂN (với filter available + pagination) ---
    @GetMapping 
    @PreAuthorize("hasAuthority('READ_MENU')")
    public ResponseEntity<List<MenuItem>> getMenuItems(
            @RequestParam(value = "available", required = false) Boolean available,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        // Validation: page >= 0, size > 0 và size <= 100
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100; 
        List<MenuItem> menuItems = menuService.getMenuItems(available, page, size);
        return ResponseEntity.ok(menuItems);
    }

    // --- API 2: LẤY MÓN ĂN THEO ID ---
    @GetMapping("/{id}") 
    @PreAuthorize("hasAuthority('READ_MENU')")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Integer id) {
        return menuService.getMenuItemById(id)
                .map(item -> ResponseEntity.ok(item)) 
                .orElse(ResponseEntity.notFound().build()); 
    }
    // --- API 2b: LẤY MÓN ĂN THEO CATEGORY ID (với pagination) ---
    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('READ_MENU')")
    public ResponseEntity<List<MenuItem>> getMenuItemsByCategoryId(
            @PathVariable Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        List<MenuItem> menuItems = menuService.getMenuItemsByCategoryId(categoryId, page, size);
        return ResponseEntity.ok(menuItems);
    }
    
    // --- API 3: TẠO MỘT MÓN ĂN MỚI (Dùng cho Admin) ---
    @PostMapping 
    @PreAuthorize("hasAuthority('CREATE_MENU')")
    public ResponseEntity<MenuItem> createMenuItem(
            @ModelAttribute MenuItemDTO menuItemDTO, 
            @RequestParam("file") MultipartFile file) { 
        try {

            MenuItem createdItem = menuService.createMenuItem(menuItemDTO, file);
            return new ResponseEntity<>(createdItem, HttpStatus.CREATED); 
        } catch (Exception e) { 
            System.err.println("Error creating menu item: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    // --- API 4: CẬP NHẬT MỘT MÓN ĂN (Dùng cho Admin) ---
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE_MENU')")
    public ResponseEntity<MenuItem> updateMenuItem(
            @PathVariable Integer id, 
            @ModelAttribute MenuItemDTO menuItemDTO, 
            @RequestParam(value = "file", required = false) MultipartFile file) { 
        try {
            MenuItem updatedItem = menuService.updateMenuItem(id, menuItemDTO, file);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            System.err.println("Error updating menu item: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    // --- API 5: XÓA MỘT MÓN ĂN (Dùng cho Admin) ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_MENU')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Integer id) {
        try {
            menuService.deleteMenuItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- API 6: LẤY DANH SÁCH MÓN ĂN BÁN CHẠY NHẤT ---
    @GetMapping("/best-selling")
    @PreAuthorize("hasAuthority('READ_MENU')")
    public ResponseEntity<List<BestSellingItemDTO>> getBestSellingItems(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            // Validate limit: phải > 0 và <= 100
            if (limit <= 0) limit = 10;
            if (limit > 100) limit = 100;
            
            List<BestSellingItemDTO> bestSellingItems = menuService.getBestSellingItems(limit);
            return ResponseEntity.ok(bestSellingItems);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
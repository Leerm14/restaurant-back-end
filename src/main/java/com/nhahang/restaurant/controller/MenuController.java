package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.MenuItemDTO; // Import DTO ta đã tạo
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;



@RestController 
@RequestMapping("/api/menu") 
@RequiredArgsConstructor 
public class MenuController {

    private final MenuService menuService;

    // --- API 1: LẤY TẤT CẢ MÓN ĂN (với filter available + pagination) ---
    @GetMapping 
    public ResponseEntity<List<MenuItem>> getMenuItems(
            @RequestParam(value = "available", required = true) Boolean available,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        // Validation: page >= 0, size > 0 và size <= 100
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100; // Giới hạn tối đa 100 items/page
        
        List<MenuItem> menuItems = menuService.getMenuItems(available, page, size);
        return ResponseEntity.ok(menuItems);
    }

    // --- API 2: LẤY MÓN ĂN THEO ID ---
    @GetMapping("/{id}") 
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Integer id) {
        return menuService.getMenuItemById(id)
                .map(item -> ResponseEntity.ok(item)) 
                .orElse(ResponseEntity.notFound().build()); 
    }
    // --- API 2b: LẤY MÓN ĂN THEO CATEGORY ID (với pagination) ---
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItem>> getMenuItemsByCategoryId(
            @PathVariable Integer categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        // Validation
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        List<MenuItem> menuItems = menuService.getMenuItemsByCategoryId(categoryId, page, size);
        return ResponseEntity.ok(menuItems);
    }
    
    // --- API 3: TẠO MỘT MÓN ĂN MỚI (Dùng cho Admin) ---
    @PostMapping 
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItemDTO menuItemDTO) {
        // @RequestBody: Spring tự động chuyển JSON client gửi lên thành đối tượng MenuItemDTO
        try {
            MenuItem createdItem = menuService.createMenuItem(menuItemDTO);
            return new ResponseEntity<>(createdItem, HttpStatus.CREATED); 
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- API 4: CẬP NHẬT MỘT MÓN ĂN (Dùng cho Admin) ---
    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable Integer id, @RequestBody MenuItemDTO menuItemDTO) {
        try {
            MenuItem updatedItem = menuService.updateMenuItem(id, menuItemDTO);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            // Log error for debugging
            System.err.println("Error updating menu item: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Integer id) {
        try {
            menuService.deleteMenuItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
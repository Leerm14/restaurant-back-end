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



@RestController // 1. Đánh dấu đây là một REST Controller (chuyên trả về JSON)
@RequestMapping("/api/menu") // 2. Đường dẫn cơ sở cho tất cả API trong class này
@RequiredArgsConstructor // 3. Tự động tiêm (inject) MenuService qua constructor
public class MenuController {

    // 4. Tiêm Service mà Controller này cần
    private final MenuService menuService;

    // --- API 1: LẤY TẤT CẢ MÓN ĂN ---
    @GetMapping 
    public ResponseEntity<List<MenuItem>> getAvailableMenuItems() {
        List<MenuItem> menuItems = menuService.getAvailableMenuItems();
        return ResponseEntity.ok(menuItems); 
    }

    // --- API 2: LẤY MÓN ĂN THEO ID ---
    @GetMapping("/{id}") 
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Integer id) {
        return menuService.getMenuItemById(id)
                .map(item -> ResponseEntity.ok(item)) 
                .orElse(ResponseEntity.notFound().build()); 
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItem>> getMenuItemsByCategoryId(@PathVariable Integer categoryId) {
        List<MenuItem> menuItems = menuService.getMenuItemsByCategoryId(categoryId);
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
package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.MenuItemDTO; // Import DTO ta đã tạo
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Đánh dấu đây là một REST Controller (chuyên trả về JSON)
@RequestMapping("/api/menu") // 2. Đường dẫn cơ sở cho tất cả API trong class này
@RequiredArgsConstructor // 3. Tự động tiêm (inject) MenuService qua constructor
public class MenuController {

    // 4. Tiêm Service mà Controller này cần
    private final MenuService menuService;

    // --- API 1: LẤY TẤT CẢ MÓN ĂN ---
    @GetMapping // 5. Xử lý request GET tới /api/menu
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        List<MenuItem> menuItems = menuService.getAllMenuItems();
        return ResponseEntity.ok(menuItems); // 6. Trả về ds món ăn với status 200 OK
    }

    // --- API 2: LẤY MÓN ĂN THEO ID ---
    @GetMapping("/{id}") // Xử lý GET tới /api/menu/1 (ví dụ)
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Integer id) {
        // .map(ResponseEntity::ok) là cách viết gọn của: .map(item -> ResponseEntity.ok(item))
        return menuService.getMenuItemById(id)
                .map(ResponseEntity::ok) 
                .orElse(ResponseEntity.notFound().build()); // 7. Nếu không tìm thấy, trả về 404 Not Found
    }

    // --- API 3: TẠO MỘT MÓN ĂN MỚI (Dùng cho Admin) ---
    @PostMapping // 8. Xử lý request POST tới /api/menu
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItemDTO menuItemDTO) {
        // 9. @RequestBody: Spring tự động chuyển JSON client gửi lên thành đối tượng MenuItemDTO
        try {
            MenuItem createdItem = menuService.createMenuItem(menuItemDTO);
            // Trả về đối tượng vừa tạo với status 201 Created
            return new ResponseEntity<>(createdItem, HttpStatus.CREATED); 
        } catch (RuntimeException e) {
            // Xử lý cơ bản nếu có lỗi (ví dụ: CategoryId không tồn tại)
            return ResponseEntity.badRequest().build();
        }
    }
}
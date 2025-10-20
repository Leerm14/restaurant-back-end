package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.MenuItemDTO;
import com.nhahang.restaurant.model.MenuItemStatus;
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.repository.MenuItemRepository;
import com.nhahang.restaurant.repository.CategoryRepository; // Giả sử bạn cần cả Category
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service 
@RequiredArgsConstructor 
public class MenuService {
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;


    /**
     * Logic: Lấy tất cả các món ăn
     */
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    /**
     * Logic: Lấy một món ăn theo ID
     */
    public Optional<MenuItem> getMenuItemById(Integer id) {
        return menuItemRepository.findById(id);
    }

    /**
     * Logic: Lấy các món ăn theo một Category cụ thể
     */
    public List<MenuItem> getMenuItemsByCategoryId(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    /**
     * Logic: Lấy các món ăn đang 'Available'
     */
    public List<MenuItem> getAvailableMenuItems() {
        return menuItemRepository.findByStatus(MenuItemStatus.Available);
    }
    public MenuItem createMenuItem(MenuItemDTO menuItemDTO) {
        var category = categoryRepository.findById(menuItemDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + menuItemDTO.getCategoryId()));

        // 2. Chuyển đổi từ DTO sang Entity
        MenuItem newMenuItem = new MenuItem();
        newMenuItem.setName(menuItemDTO.getName());
        newMenuItem.setDescription(menuItemDTO.getDescription());
        newMenuItem.setImageUrl(menuItemDTO.getImageUrl());
        newMenuItem.setPrice(menuItemDTO.getPrice());
        newMenuItem.setStatus(MenuItemStatus.valueOf(menuItemDTO.getStatus()));
        newMenuItem.setCategory(category); 

        // 3. Gọi Repository để lưu
        return menuItemRepository.save(newMenuItem);
    }

    /**
     * Logic: Cập nhật một món ăn
     */
    public MenuItem updateMenuItem(Integer id, MenuItemDTO menuItemDTO) {
        // 1. Kiểm tra nghiệp vụ: Món ăn có tồn tại không?
        MenuItem existingMenuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy MenuItem với ID: " + id));

        // 2. Kiểm tra nghiệp vụ: Category mới có tồn tại không?
        var category = categoryRepository.findById(menuItemDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + menuItemDTO.getCategoryId()));

        // 3. Validation status enum
        MenuItemStatus status;
        try {
            status = MenuItemStatus.valueOf(menuItemDTO.getStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status không hợp lệ: " + menuItemDTO.getStatus());
        }

        // 4. Cập nhật thông tin từ DTO vào Entity đã tồn tại
        existingMenuItem.setName(menuItemDTO.getName());
        existingMenuItem.setDescription(menuItemDTO.getDescription());
        existingMenuItem.setImageUrl(menuItemDTO.getImageUrl());
        existingMenuItem.setPrice(menuItemDTO.getPrice());
        existingMenuItem.setStatus(status);
        existingMenuItem.setCategory(category);

        // 5. Lưu lại (vì 'existingMenuItem' đã có ID, 'save' sẽ hiểu là UPDATE)
        return menuItemRepository.save(existingMenuItem);
    }
    /** 
     * Logic: Xóa một món ăn theo ID 
    */
    public void deleteMenuItem(Integer id) {
         MenuItem existingMenuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy MenuItem với ID: " + id));
         menuItemRepository.delete(existingMenuItem);
    }
}
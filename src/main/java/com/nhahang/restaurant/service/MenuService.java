package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.MenuItemDTO;
import com.nhahang.restaurant.model.MenuItemStatus;
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.repository.MenuItemRepository;
import com.nhahang.restaurant.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Logic: Lấy các món ăn đang 'Unavailable'
     */
    public List<MenuItem> getUnavailableMenuItems() {
        return menuItemRepository.findByStatus(MenuItemStatus.Unavailable);
    }

    /**
     * Logic: Lấy menu items với pagination và filter available
     */
    public List<MenuItem> getMenuItems(Boolean available, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (available != null && available) {
            Page<MenuItem> menuPage = menuItemRepository.findByStatus(MenuItemStatus.Available, pageable);
            return menuPage.getContent();
        } else if (available != null && !available) {
            Page<MenuItem> menuPage = menuItemRepository.findByStatus(MenuItemStatus.Unavailable, pageable);
            return menuPage.getContent();
        } else {
            Page<MenuItem> menuPage = menuItemRepository.findAll(pageable);
            return menuPage.getContent();
        }
    }

    /**
     * Logic: Lấy menu items theo category với pagination
     */
    public List<MenuItem> getMenuItemsByCategoryId(Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MenuItem> menuPage = menuItemRepository.findByCategoryId(categoryId, pageable);
        return menuPage.getContent();
    }
    public MenuItem createMenuItem(MenuItemDTO menuItemDTO) {
        var category = categoryRepository.findById(menuItemDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + menuItemDTO.getCategoryId()));

        MenuItem newMenuItem = new MenuItem();
        newMenuItem.setName(menuItemDTO.getName());
        newMenuItem.setDescription(menuItemDTO.getDescription());
        newMenuItem.setImageUrl(menuItemDTO.getImageUrl());
        newMenuItem.setPrice(menuItemDTO.getPrice());
        newMenuItem.setStatus(MenuItemStatus.valueOf(menuItemDTO.getStatus()));
        newMenuItem.setCategory(category); 

        return menuItemRepository.save(newMenuItem);
    }

    /**
     * Logic: Cập nhật một món ăn
     */
    public MenuItem updateMenuItem(Integer id, MenuItemDTO menuItemDTO) {
        MenuItem existingMenuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy MenuItem với ID: " + id));

        var category = categoryRepository.findById(menuItemDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + menuItemDTO.getCategoryId()));

        MenuItemStatus status;
        try {
            status = MenuItemStatus.valueOf(menuItemDTO.getStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Status không hợp lệ: " + menuItemDTO.getStatus());
        }

        existingMenuItem.setName(menuItemDTO.getName());
        existingMenuItem.setDescription(menuItemDTO.getDescription());
        existingMenuItem.setImageUrl(menuItemDTO.getImageUrl());
        existingMenuItem.setPrice(menuItemDTO.getPrice());
        existingMenuItem.setStatus(status);
        existingMenuItem.setCategory(category);

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

    private static final String UPLOAD_DIR = "uploads/images/";

    /**
     * Logic: Upload ảnh cho món ăn và cập nhật imageUrl
     */
    public String uploadImageForMenuItem(Integer menuItemId, MultipartFile file) {
        try {
            MenuItem menuItem = menuItemRepository.findById(menuItemId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy MenuItem với ID: " + menuItemId));

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/api/upload/images/" + uniqueFilename;
            menuItem.setImageUrl(imageUrl);
            menuItemRepository.save(menuItem);

            return imageUrl;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu file ảnh: " + e.getMessage());
        }
    }

    /**
     * Helper method: Lấy file extension từ filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
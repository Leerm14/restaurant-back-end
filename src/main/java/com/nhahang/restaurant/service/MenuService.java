package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.BestSellingItemDTO;
import com.nhahang.restaurant.dto.MenuItemDTO;
import com.nhahang.restaurant.model.MenuItemStatus;
import com.nhahang.restaurant.model.entity.MenuItem;
import com.nhahang.restaurant.repository.MenuItemRepository;
import com.nhahang.restaurant.repository.CategoryRepository;
import com.nhahang.restaurant.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

@Service 
@RequiredArgsConstructor 
public class MenuService {
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final Cloudinary cloudinary;


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

    /**
     * Logic: Tạo món ăn mới
    */
    public MenuItem createMenuItem(MenuItemDTO menuItemDTO, MultipartFile file) {
        // 1. Upload ảnh LÊN TRƯỚC
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Ảnh món ăn là bắt buộc khi tạo mới");
        }
        String imageUrl = uploadToCloudinary(file);
        menuItemDTO.setImageUrl(imageUrl); 
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
    public MenuItem updateMenuItem(Integer id, MenuItemDTO menuItemDTO, MultipartFile file) {
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

        if (file != null && !file.isEmpty()) {
            String newImageUrl = uploadToCloudinary(file);
            existingMenuItem.setImageUrl(newImageUrl); 
        }
        existingMenuItem.setName(menuItemDTO.getName());
        existingMenuItem.setDescription(menuItemDTO.getDescription());
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

    /**
     * Phương thức private để xử lý upload (TÁI SỬ DỤNG)
     */
    private String uploadToCloudinary(MultipartFile file) {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(), 
                ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "restaurant_menu" // Thư mục trên Cloudinary
                )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
    /**
     * Logic: Lấy danh sách món ăn bán chạy nhất
     */
    public List<BestSellingItemDTO> getBestSellingItems(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = orderItemRepository.findBestSellingItems(pageable);
        
        return results.stream().map(row -> {
            BestSellingItemDTO dto = new BestSellingItemDTO();
            dto.setMenuItemId((Integer) row[0]);
            dto.setMenuItemName((String) row[1]);
            dto.setDescription((String) row[2]);
            dto.setImageUrl((String) row[3]);
            dto.setPrice((BigDecimal) row[4]);
            dto.setCategoryName((String) row[5]);
            dto.setTotalQuantitySold((Long) row[6]);
            dto.setTotalRevenue((BigDecimal) row[7]);
            return dto;
        }).collect(Collectors.toList());
    }
}
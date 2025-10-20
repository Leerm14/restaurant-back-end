package com.nhahang.restaurant.service;

import com.nhahang.restaurant.dto.CategoryDTO;
import com.nhahang.restaurant.model.entity.Category;
import com.nhahang.restaurant.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;

    /**
     * Lấy tất cả categories
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Lấy category theo ID
     */
    public Optional<Category> getCategoryById(Integer id) {
        return categoryRepository.findById(id);
    }

    /**
     * Tạo category mới
     */
    public Category createCategory(CategoryDTO categoryDTO) {
        // Kiểm tra trùng tên
        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new RuntimeException("Category với tên '" + categoryDTO.getName() + "' đã tồn tại");
        }

        Category newCategory = new Category();
        newCategory.setName(categoryDTO.getName());
        
        return categoryRepository.save(newCategory);
    }

    /**
     * Cập nhật category
     */
    public Category updateCategory(Integer id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + id));

        // Kiểm tra trùng tên với category khác
        Optional<Category> duplicateCategory = categoryRepository.findByName(categoryDTO.getName());
        if (duplicateCategory.isPresent() && !duplicateCategory.get().getId().equals(id)) {
            throw new RuntimeException("Category với tên '" + categoryDTO.getName() + "' đã tồn tại");
        }

        existingCategory.setName(categoryDTO.getName());
        return categoryRepository.save(existingCategory);
    }

    /**
     * Xóa category
     */
    public void deleteCategory(Integer id) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với ID: " + id));
        
        categoryRepository.delete(existingCategory);
    }
}
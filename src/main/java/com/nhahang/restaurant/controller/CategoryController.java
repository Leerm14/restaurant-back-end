package com.nhahang.restaurant.controller;

import com.nhahang.restaurant.dto.CategoryDTO;
import com.nhahang.restaurant.model.entity.Category;
import com.nhahang.restaurant.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // --- API 1: LẤY TẤT CẢ CATEGORIES (với pagination) ---
    @GetMapping
    // @PreAuthorize("hasAuthority('READ_CATEGORY')")
    public ResponseEntity<List<Category>> getAllCategories(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        // Validation
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        List<Category> categories = categoryService.getAllCategories(page, size);
        return ResponseEntity.ok(categories);
    }

    // --- API 2: TẠO CATEGORY MỚI ---
    @PostMapping
    // @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryDTO categoryDTO) {
        try {
            Category createdCategory = categoryService.createCategory(categoryDTO);
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            System.err.println("Error creating category: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // --- API 3: CẬP NHẬT CATEGORY ---
    @PutMapping("/{id}")
    // @PreAuthorize("hasAuthority('UPDATE_CATEGORY')")
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody CategoryDTO categoryDTO) {
        try {
            Category updatedCategory = categoryService.updateCategory(id, categoryDTO);
            return ResponseEntity.ok(updatedCategory);
        } catch (RuntimeException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // --- API 5: XÓA CATEGORY ---
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
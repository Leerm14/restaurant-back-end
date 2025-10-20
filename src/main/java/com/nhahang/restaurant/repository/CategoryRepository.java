package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // Tìm category theo tên (để kiểm tra trùng lặp)
    java.util.Optional<Category> findByName(String name);
}
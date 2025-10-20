package com.nhahang.restaurant.repository;
import com.nhahang.restaurant.model.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    // Tìm danh sách món ăn theo Category (dựa vào category.id)
    List<MenuItem> findByCategoryId(Integer categoryId);

    // Tìm danh sách món ăn theo trạng thái (Available / Unavailable)
    List<MenuItem> findByStatus(String status);
}
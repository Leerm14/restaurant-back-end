package com.nhahang.restaurant.repository;
import com.nhahang.restaurant.model.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    List<MenuItem> findByCategoryId(Integer categoryId);

    List<MenuItem> findByStatus(com.nhahang.restaurant.model.MenuItemStatus status);

    Page<MenuItem> findByStatus(com.nhahang.restaurant.model.MenuItemStatus status, Pageable pageable);
    Page<MenuItem> findByCategoryId(Integer categoryId, Pageable pageable);
}
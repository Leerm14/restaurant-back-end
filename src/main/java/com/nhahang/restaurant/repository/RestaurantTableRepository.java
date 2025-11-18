package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.RestaurantTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {
    
    Optional<RestaurantTable> findByTableNumber(int tableNumber);

    List<RestaurantTable> findByStatus(com.nhahang.restaurant.model.TableStatus status);
    
    Page<RestaurantTable> findByStatus(com.nhahang.restaurant.model.TableStatus status, Pageable pageable);
    
    long countByStatus(com.nhahang.restaurant.model.TableStatus status);
}

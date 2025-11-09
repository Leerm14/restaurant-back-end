package com.nhahang.restaurant.repository;

import com.nhahang.restaurant.model.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("SELECT oi.menuItem.id as menuItemId, " +
           "oi.menuItem.name as menuItemName, " +
           "oi.menuItem.description as description, " +
           "oi.menuItem.imageUrl as imageUrl, " +
           "oi.menuItem.price as price, " +
           "oi.menuItem.category.name as categoryName, " +
           "SUM(oi.quantity) as totalQuantitySold, " +
           "SUM(oi.quantity * oi.priceAtOrder) as totalRevenue " +
           "FROM OrderItem oi " +
           "WHERE oi.order.status = 'Completed' " +
           "GROUP BY oi.menuItem.id, oi.menuItem.name, oi.menuItem.description, " +
           "oi.menuItem.imageUrl, oi.menuItem.price, oi.menuItem.category.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findBestSellingItems(Pageable pageable);
}
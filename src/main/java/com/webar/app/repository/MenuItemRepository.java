package com.webar.app.repository;

import com.webar.app.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantId(Long restaurantId);
    List<MenuItem> findByRestaurantIdAndPublishedTrueAndAvailableTrue(Long restaurantId);
    boolean existsByCategoryId(Long categoryId);
}

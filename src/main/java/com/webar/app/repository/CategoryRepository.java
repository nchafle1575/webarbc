package com.webar.app.repository;

import com.webar.app.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    List<Category>
    findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
}
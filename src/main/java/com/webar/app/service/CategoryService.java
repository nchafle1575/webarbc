package com.webar.app.service;

import com.webar.app.dto.CategoryRequest;
import com.webar.app.entity.Category;
import com.webar.app.entity.Restaurant;
import com.webar.app.repository.CategoryRepository;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           RestaurantRepository restaurantRepository,
                           MenuItemRepository menuItemRepository) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public Category create(CategoryRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .active(request.active() == null || request.active())
                .restaurant(restaurant)
                .build();

        return categoryRepository.save(category);
    }

    public List<Category> getByRestaurant(Long restaurantId) {
        return categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);

        if (request.restaurantId() != null && !category.getRestaurant().getId().equals(request.restaurantId())) {
            Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                    .orElseThrow(() -> new RuntimeException("Restaurant not found"));
            category.setRestaurant(restaurant);
        }

        category.setName(request.name());
        category.setSlug(request.slug());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return categoryRepository.save(category);
    }

    public Category setActive(Long id, boolean active) {
        Category category = getById(id);
        category.setActive(active);
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        Category category = getById(id);
        if (menuItemRepository.existsByCategoryId(id)) {
            throw new RuntimeException("Cannot delete this category because it has menu items. Deactivate it instead.");
        }
        categoryRepository.delete(category);
    }

    private Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }
}

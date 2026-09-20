package com.webar.app.service;

import com.webar.app.dto.MenuItemRequest;
import com.webar.app.entity.Category;
import com.webar.app.entity.MenuItem;
import com.webar.app.entity.Restaurant;


import com.webar.app.repository.CategoryRepository;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuImageRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import com.webar.app.repository.RestaurantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    private final RestaurantRepository restaurantRepository;

    private final CategoryRepository categoryRepository;

    private final MenuImageRepository menuImageRepository;

    private final MenuMediaRepository menuMediaRepository;

    private final ThreeDAssetRepository threeDAssetRepository;

    public MenuItemService(
            MenuItemRepository menuItemRepository,
            RestaurantRepository restaurantRepository,
            CategoryRepository categoryRepository,
            MenuImageRepository menuImageRepository,
            MenuMediaRepository menuMediaRepository,
            ThreeDAssetRepository threeDAssetRepository, MenuImageRepository menuImageRepository1, MenuMediaRepository menuMediaRepository1, ThreeDAssetRepository threeDAssetRepository1
    ) {

        this.menuItemRepository =
                menuItemRepository;

        this.restaurantRepository =
                restaurantRepository;

        this.categoryRepository =
                categoryRepository;
        this.menuImageRepository = menuImageRepository1;
        this.menuMediaRepository = menuMediaRepository1;
        this.threeDAssetRepository = threeDAssetRepository1;
    }

    public MenuItem create(
            MenuItemRequest request
    ) {

        Restaurant restaurant =
                restaurantRepository
                        .findById(request.restaurantId())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Restaurant not found"
                                )
                        );

        Category category =
                categoryRepository
                        .findById(request.categoryId())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Category not found"
                                )
                        );

        MenuItem menuItem =
                MenuItem.builder()
                        .name(request.name())
                        .sku(request.sku())
                        .price(request.price())
                        .currency(
                                request.currency() == null
                                        ? "INR"
                                        : request.currency()
                        )
                        .shortDescription(
                                request.shortDescription()
                        )
                        .description(
                                request.description()
                        )
                        .foodType(
                                request.foodType()
                        )
                        .spiceLevel(
                                request.spiceLevel()
                        )
                        .servingSize(
                                request.servingSize()
                        )
                        .preparationTimeMinutes(
                                request.preparationTimeMinutes()
                        )
                        .ingredients(
                                request.ingredients()
                        )
                        .allergens(
                                request.allergens()
                        )
                        .calories(
                                request.calories()
                        )
                        .available(
                                request.available() == null
                                        ? true
                                        : request.available()
                        )
                        .published(
                                request.published() == null
                                        ? false
                                        : request.published()
                        )
                        .restaurant(restaurant)
                        .category(category)
                        .tags(
                                request.tags() == null
                                        ? new ArrayList<>()
                                        : request.tags()
                        )
                        .build();

        return menuItemRepository.save(menuItem);
    }

    public List<MenuItem> getAll(
            Long restaurantId
    ) {

        return menuItemRepository
                .findByRestaurantId(restaurantId);
    }

    public List<MenuItem> getPublished(
            Long restaurantId
    ) {

        return menuItemRepository
                .findByRestaurantIdAndPublishedTrueAndAvailableTrue(
                        restaurantId
                );
    }

    public MenuItem getById(Long id) {

        return menuItemRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Menu item not found"
                        )
                );
    }

    public MenuItem update(
            Long id,
            MenuItemRequest request
    ) {

        MenuItem item =
                getById(id);

        Category category =
                categoryRepository
                        .findById(request.categoryId())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Category not found"
                                )
                        );

        item.setName(request.name());
        item.setSku(request.sku());
        item.setPrice(request.price());

        item.setCurrency(
                request.currency() == null
                        ? "INR"
                        : request.currency()
        );

        item.setShortDescription(
                request.shortDescription()
        );

        item.setDescription(
                request.description()
        );

        item.setFoodType(
                request.foodType()
        );

        item.setSpiceLevel(
                request.spiceLevel()
        );

        item.setServingSize(
                request.servingSize()
        );

        item.setPreparationTimeMinutes(
                request.preparationTimeMinutes()
        );

        item.setIngredients(
                request.ingredients()
        );

        item.setAllergens(
                request.allergens()
        );

        item.setCalories(
                request.calories()
        );

        item.setAvailable(
                request.available() == null
                        ? true
                        : request.available()
        );

        item.setPublished(
                request.published() == null
                        ? false
                        : request.published()
        );

        item.setCategory(category);

        item.setTags(
                request.tags() == null
                        ? new ArrayList<>()
                        : request.tags()
        );

        return menuItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {

        MenuItem item =
                getById(id);

        threeDAssetRepository
                .findByMenuItemId(id)
                .ifPresent(
                        threeDAssetRepository::delete
                );

        menuImageRepository
                .findByMenuItemIdOrderBySortOrderAscIdAsc(id)
                .forEach(
                        menuImageRepository::delete
                );

        menuMediaRepository
                .findByMenuItemIdOrderByIdAsc(id)
                .forEach(
                        menuMediaRepository::delete
                );

        menuItemRepository.delete(item);
    }

    public MenuItem publish(Long id) {

        MenuItem item =
                getById(id);

        item.setPublished(true);

        return menuItemRepository.save(item);
    }

    public MenuItem unpublish(Long id) {

        MenuItem item =
                getById(id);

        item.setPublished(false);

        return menuItemRepository.save(item);
    }
}
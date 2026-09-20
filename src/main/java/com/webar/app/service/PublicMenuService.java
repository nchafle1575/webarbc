package com.webar.app.service;

import com.webar.app.dto.PublicMenuResponse;
import com.webar.app.entity.Category;
import com.webar.app.entity.MenuItem;
import com.webar.app.entity.MenuImage;
import com.webar.app.entity.MenuMedia;
import com.webar.app.entity.Restaurant;
import com.webar.app.entity.ThreeDAsset;
import com.webar.app.repository.CategoryRepository;
import com.webar.app.repository.MenuImageRepository;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.RestaurantRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PublicMenuService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuImageRepository menuImageRepository;
    private final MenuMediaRepository menuMediaRepository;
    private final ThreeDAssetRepository threeDAssetRepository;

    public PublicMenuService(
            RestaurantRepository restaurantRepository,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            MenuImageRepository menuImageRepository,
            MenuMediaRepository menuMediaRepository,
            ThreeDAssetRepository threeDAssetRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuImageRepository = menuImageRepository;
        this.menuMediaRepository = menuMediaRepository;
        this.threeDAssetRepository = threeDAssetRepository;
    }

    @Transactional(readOnly = true)
    public PublicMenuResponse getMenu(String restaurantSlug) {
        String requestedSlug = slugify(restaurantSlug);

        Restaurant restaurant = restaurantRepository.findAll()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .filter(r -> slugify(r.getName()).equals(requestedSlug))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        List<Category> categories = categoryRepository
                .findByRestaurantIdOrderBySortOrderAsc(restaurant.getId())
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .toList();

        List<PublicMenuResponse.CategoryResponse> categoryResponses = categories.stream()
                .map(category -> new PublicMenuResponse.CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getImageUrl(),
                        category.getSortOrder(),
                        menuItemRepository
                                .findByRestaurantIdAndPublishedTrueAndAvailableTrue(restaurant.getId())
                                .stream()
                                .filter(item -> item.getCategory() != null)
                                .filter(item -> Objects.equals(item.getCategory().getId(), category.getId()))
                                .map(this::toMenuItem)
                                .toList()
                ))
                .toList();

        return new PublicMenuResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getLogoUrl(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getCity(),
                restaurant.getState(),
                restaurant.getCountry(),
                restaurant.getPhone(),
                restaurant.getEmail(),
                categoryResponses
        );
    }

    private PublicMenuResponse.MenuItemResponse toMenuItem(MenuItem item) {
        List<PublicMenuResponse.ImageResponse> images = menuImageRepository
                .findByMenuItemIdOrderBySortOrderAscIdAsc(item.getId())
                .stream()
                .map(this::toImage)
                .toList();

        List<PublicMenuResponse.MediaResponse> media = menuMediaRepository
                .findByMenuItemIdOrderByIdAsc(item.getId())
                .stream()
                .map(this::toMedia)
                .toList();

        PublicMenuResponse.ThreeDResponse threeD = threeDAssetRepository
                .findByMenuItemId(item.getId())
                .map(this::toThreeD)
                .orElse(null);

        return new PublicMenuResponse.MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getSku(),
                item.getPrice(),
                item.getCurrency(),
                item.getShortDescription(),
                item.getDescription(),
                item.getFoodType() == null ? null : item.getFoodType().name(),
                item.getSpiceLevel() == null ? null : item.getSpiceLevel().name(),
                item.getServingSize(),
                item.getPreparationTimeMinutes(),
                item.getIngredients(),
                item.getAllergens(),
                item.getCalories(),
                item.getTags(),
                images,
                media,
                threeD
        );
    }

    private PublicMenuResponse.ImageResponse toImage(MenuImage image) {
        return new PublicMenuResponse.ImageResponse(
                image.getId(), image.getFileName(), image.getImageUrl(),
                image.getSortOrder(), image.getPrimaryImage()
        );
    }

    private PublicMenuResponse.MediaResponse toMedia(MenuMedia media) {
        return new PublicMenuResponse.MediaResponse(
                media.getId(), media.getFileName(), media.getMediaUrl(),
                media.getMediaType() == null ? null : media.getMediaType().name()
        );
    }

    private PublicMenuResponse.ThreeDResponse toThreeD(ThreeDAsset asset) {
        return new PublicMenuResponse.ThreeDResponse(
                asset.getId(), asset.getGlbUrl(), asset.getUsdzUrl(),
                asset.getGlbFileName(), asset.getUsdzFileName(),
                asset.getStatus() == null ? null : asset.getStatus().name(),
                asset.getErrorMessage()
        );
    }

    private String slugify(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return Pattern.compile("-+").matcher(normalized).replaceAll("-");
    }
}

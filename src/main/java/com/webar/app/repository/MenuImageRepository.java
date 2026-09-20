package com.webar.app.repository;

import com.webar.app.entity.MenuImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuImageRepository extends JpaRepository<MenuImage, Long> {
    List<MenuImage> findByMenuItemIdOrderBySortOrderAscIdAsc(Long menuItemId);
}

package com.webar.app.repository;

import com.webar.app.entity.MenuMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuMediaRepository extends JpaRepository<MenuMedia, Long> {
    List<MenuMedia> findByMenuItemIdOrderByIdAsc(Long menuItemId);
}

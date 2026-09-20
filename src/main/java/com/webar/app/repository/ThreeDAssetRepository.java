package com.webar.app.repository;

import com.webar.app.entity.ThreeDAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThreeDAssetRepository
        extends JpaRepository<ThreeDAsset, Long> {

    Optional<ThreeDAsset>
    findByMenuItemId(Long menuItemId);
}
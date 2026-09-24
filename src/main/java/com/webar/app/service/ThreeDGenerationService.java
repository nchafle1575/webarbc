package com.webar.app.service;

import com.webar.app.dto.ThreeDGenerationJobResponse;
import com.webar.app.entity.*;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import com.webar.app.repository.ThreeDGenerationJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ThreeDGenerationService {
    private final MenuItemRepository menuItemRepository;
    private final MenuMediaRepository menuMediaRepository;
    private final ThreeDAssetRepository threeDAssetRepository;
    private final ThreeDGenerationJobRepository jobRepository;

    public ThreeDGenerationService(MenuItemRepository menuItemRepository, MenuMediaRepository menuMediaRepository,
                                   ThreeDAssetRepository threeDAssetRepository, ThreeDGenerationJobRepository jobRepository) {
        this.menuItemRepository=menuItemRepository;
        this.menuMediaRepository=menuMediaRepository;
        this.threeDAssetRepository=threeDAssetRepository;
        this.jobRepository=jobRepository;
    }

    @Transactional
    public ThreeDGenerationJobResponse generate(Long menuItemId) {
        MenuItem item=getItem(menuItemId);
        var active=jobRepository.findByStatusIn(List.of(ThreeDGenerationJobStatus.PENDING,ThreeDGenerationJobStatus.PROCESSING))
                .stream().filter(j->j.getMenuItem().getId().equals(menuItemId)).findFirst();
        if(active.isPresent()) return toResponse(active.get());

        List<MenuMedia> media=menuMediaRepository.findByMenuItemIdOrderByIdAsc(menuItemId);
        List<MenuMedia> source=media.stream().filter(m->m.getMediaUrl()!=null&&!m.getMediaUrl().isBlank()).toList();
        if(source.isEmpty()) throw new IllegalStateException("Upload multiple photos and/or one or more videos of the same food item before generating the 3D model");

        ThreeDGenerationJob job=ThreeDGenerationJob.builder().menuItem(item).provider("PHOTOGRAMMETRY")
                .providerTaskId("PENDING_WORKER").generationType("PHOTO_VIDEO_RECONSTRUCTION")
                .progress(0).sourceMediaCount(source.size()).status(ThreeDGenerationJobStatus.PENDING).build();

        ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(menuItemId)
                .orElseGet(()->ThreeDAsset.builder().menuItem(item).build());
        asset.setStatus(ThreeDStatus.SOURCE_UPLOADED); asset.setErrorMessage(null); threeDAssetRepository.save(asset);
        return toResponse(jobRepository.save(job));
    }

    public ThreeDGenerationJobResponse getLatestJob(Long menuItemId) {
        getItem(menuItemId);
        return jobRepository.findTopByMenuItemIdOrderByCreatedAtDesc(menuItemId).map(this::toResponse).orElse(null);
    }

    private MenuItem getItem(Long id){return menuItemRepository.findById(id).orElseThrow(()->new NoSuchElementException("Menu item not found"));}
    private ThreeDGenerationJobResponse toResponse(ThreeDGenerationJob j){
        return new ThreeDGenerationJobResponse(j.getId(),j.getMenuItem().getId(),j.getProvider(),j.getProviderTaskId(),
                j.getGenerationType(),j.getProgress(),j.getStatus(),j.getSourceMediaCount(),j.getErrorMessage(),
                j.getCreatedAt(),j.getUpdatedAt(),j.getCompletedAt());
    }
}

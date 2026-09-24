package com.webar.app.service;

import com.webar.app.dto.ThreeDGenerationJobResponse;
import com.webar.app.entity.*;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import com.webar.app.repository.ThreeDGenerationJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ThreeDGenerationService {
    private final MenuItemRepository menuItemRepository;
    private final MenuMediaRepository menuMediaRepository;
    private final ThreeDAssetRepository threeDAssetRepository;
    private final ThreeDGenerationJobRepository jobRepository;
    private final FileStorageService fileStorageService;
    private final PhotogrammetryClient workerClient;

    public ThreeDGenerationService(MenuItemRepository menuItemRepository,MenuMediaRepository menuMediaRepository,
                                   ThreeDAssetRepository threeDAssetRepository,ThreeDGenerationJobRepository jobRepository,
                                   FileStorageService fileStorageService,PhotogrammetryClient workerClient) {
        this.menuItemRepository=menuItemRepository;
        this.menuMediaRepository=menuMediaRepository;
        this.threeDAssetRepository=threeDAssetRepository;
        this.jobRepository=jobRepository;
        this.fileStorageService=fileStorageService;
        this.workerClient=workerClient;
    }

    @Transactional
    public ThreeDGenerationJobResponse generate(Long menuItemId) {
        MenuItem item=getItem(menuItemId);
        var active=jobRepository.findByStatusIn(List.of(ThreeDGenerationJobStatus.PENDING,ThreeDGenerationJobStatus.PROCESSING))
                .stream().filter(j->j.getMenuItem().getId().equals(menuItemId)).findFirst();
        if(active.isPresent()) return toResponse(active.get());

        List<MenuMedia> media=menuMediaRepository.findByMenuItemIdOrderByIdAsc(menuItemId).stream()
                .filter(m->m.getMediaUrl()!=null&&!m.getMediaUrl().isBlank()).toList();
        if(media.isEmpty()) throw new IllegalStateException("Upload photos and/or videos of the same food item first");

        List<PhotogrammetryClient.SourceFile> source=new ArrayList<>();
        for(MenuMedia m:media) source.add(new PhotogrammetryClient.SourceFile(m.getFileName(),fileStorageService.readFile(m.getMediaUrl())));

        PhotogrammetryClient.WorkerJobResponse worker=workerClient.createJob(source);
        if(worker==null||worker.id()==null||worker.id().isBlank()) throw new IllegalStateException("Photogrammetry worker did not return a job id");

        ThreeDGenerationJob job=ThreeDGenerationJob.builder().menuItem(item).provider("PHOTOGRAMMETRY")
                .providerTaskId(worker.id()).generationType("PHOTO_VIDEO_RECONSTRUCTION")
                .progress(worker.progress()==null?0:worker.progress()).sourceMediaCount(media.size())
                .status(ThreeDGenerationJobStatus.PROCESSING).build();

        ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(menuItemId)
                .orElseGet(()->ThreeDAsset.builder().menuItem(item).build());
        asset.setStatus(ThreeDStatus.PROCESSING); asset.setErrorMessage(null); threeDAssetRepository.save(asset);
        return toResponse(jobRepository.save(job));
    }

    @Scheduled(fixedDelay=15000)
    @Transactional
    public void pollRunningJobs() {
        for(ThreeDGenerationJob job:jobRepository.findByStatusIn(List.of(ThreeDGenerationJobStatus.PROCESSING))) {
            try {
                var worker=workerClient.getJob(job.getProviderTaskId());
                if(worker==null) continue;
                if(worker.progress()!=null) job.setProgress(worker.progress());
                if("SUCCEEDED".equalsIgnoreCase(worker.status())) completeJob(job);
                else if("FAILED".equalsIgnoreCase(worker.status())) failJob(job,worker.error());
                else jobRepository.save(job);
            } catch(Exception e) {
                job.setErrorMessage(safeMessage(e)); jobRepository.save(job);
            }
        }
    }

    private void completeJob(ThreeDGenerationJob job) {
        try {
            String base=sanitize(job.getMenuItem().getName());
            String glbName=base+"-"+job.getId()+".glb";
            String usdzName=base+"-"+job.getId()+".usdz";
            String glbUrl=fileStorageService.saveBytes(workerClient.downloadArtifact(job.getProviderTaskId(),"glb"),
                    "3d/"+job.getMenuItem().getId(),glbName);
            String usdzUrl=fileStorageService.saveBytes(workerClient.downloadArtifact(job.getProviderTaskId(),"usdz"),
                    "3d/"+job.getMenuItem().getId(),usdzName);

            ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(job.getMenuItem().getId())
                    .orElseGet(()->ThreeDAsset.builder().menuItem(job.getMenuItem()).build());
            asset.setGlbUrl(glbUrl); asset.setUsdzUrl(usdzUrl); asset.setGlbFileName(glbName);
            asset.setUsdzFileName(usdzName); asset.setStatus(ThreeDStatus.READY); asset.setErrorMessage(null);
            threeDAssetRepository.save(asset);

            job.setProgress(100); job.setStatus(ThreeDGenerationJobStatus.SUCCEEDED);
            job.setCompletedAt(LocalDateTime.now()); job.setErrorMessage(null); jobRepository.save(job);
        } catch(Exception e) { failJob(job,"Could not persist reconstruction artifacts: "+safeMessage(e)); }
    }

    private void failJob(ThreeDGenerationJob job,String message) {
        job.setStatus(ThreeDGenerationJobStatus.FAILED);
        job.setErrorMessage(message==null||message.isBlank()?"3D reconstruction failed":message);
        job.setCompletedAt(LocalDateTime.now()); jobRepository.save(job);
        ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(job.getMenuItem().getId())
                .orElseGet(()->ThreeDAsset.builder().menuItem(job.getMenuItem()).build());
        asset.setStatus(ThreeDStatus.FAILED); asset.setErrorMessage(job.getErrorMessage()); threeDAssetRepository.save(asset);
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
    private String sanitize(String v){return v==null||v.isBlank()?"menu-item":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-+|-+$","");}
    private String safeMessage(Exception e){return e.getMessage()==null||e.getMessage().isBlank()?e.getClass().getSimpleName():e.getMessage();}
}

package com.webar.app.service;

import com.webar.app.dto.ThreeDGenerationJobResponse;
import com.webar.app.dto.meshy.MeshyModelUrls;
import com.webar.app.dto.meshy.MeshyTaskResponse;
import com.webar.app.entity.*;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import com.webar.app.repository.ThreeDGenerationJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ThreeDGenerationService {
    private final MenuItemRepository menuItemRepository;
    private final MenuMediaRepository menuMediaRepository;
    private final ThreeDAssetRepository threeDAssetRepository;
    private final ThreeDGenerationJobRepository jobRepository;
    private final FileStorageService fileStorageService;
    private final MeshyClient meshyClient;

    public ThreeDGenerationService(MenuItemRepository menuItemRepository, MenuMediaRepository menuMediaRepository,
                                   ThreeDAssetRepository threeDAssetRepository, ThreeDGenerationJobRepository jobRepository,
                                   FileStorageService fileStorageService, MeshyClient meshyClient) {
        this.menuItemRepository=menuItemRepository; this.menuMediaRepository=menuMediaRepository;
        this.threeDAssetRepository=threeDAssetRepository; this.jobRepository=jobRepository;
        this.fileStorageService=fileStorageService; this.meshyClient=meshyClient;
    }

    @Transactional
    public ThreeDGenerationJobResponse generate(Long menuItemId) {
        MenuItem item=getItem(menuItemId);
        Optional<ThreeDGenerationJob> active=jobRepository.findByStatusIn(List.of(
                ThreeDGenerationJobStatus.PENDING, ThreeDGenerationJobStatus.PROCESSING)).stream()
                .filter(j->Objects.equals(j.getMenuItem().getId(),menuItemId)).findFirst();
        if(active.isPresent()) return toResponse(active.get());

        List<MenuMedia> media=menuMediaRepository.findByMenuItemIdOrderByIdAsc(menuItemId);
        List<MenuMedia> photos=media.stream().filter(m->m.getMediaType()==MediaType.PHOTO).limit(4).toList();
        List<MenuMedia> videos=media.stream().filter(m->m.getMediaType()==MediaType.VIDEO).toList();
        if(photos.isEmpty() && videos.isEmpty()) throw new IllegalStateException("Upload at least one food image or video before generating a 3D model");

        try {
            List<String> imageDataUris=new ArrayList<>();
            for(MenuMedia photo:photos) imageDataUris.add(fileStorageService.toDataUri(photo.getMediaUrl(),photo.getFileName()));
            if(imageDataUris.size()<4) for(MenuMedia video:videos) {
                imageDataUris.addAll(extractVideoFramesAsDataUris(video.getMediaUrl(),4-imageDataUris.size()));
                if(imageDataUris.size()>=4) break;
            }
            if(imageDataUris.isEmpty()) throw new IllegalStateException("No usable image frames were found");

            List<String> selected=imageDataUris.subList(0,Math.min(4,imageDataUris.size()));
            String type=selected.size()==1?"IMAGE_TO_3D":"MULTI_IMAGE_TO_3D";
            String taskId=selected.size()==1?meshyClient.createImageTo3DTask(selected.get(0)):meshyClient.createMultiImageTo3DTask(selected);

            ThreeDGenerationJob job=ThreeDGenerationJob.builder().menuItem(item).provider("MESHY")
                    .providerTaskId(taskId).generationType(type).progress(0)
                    .status(ThreeDGenerationJobStatus.PROCESSING).sourceMediaCount(selected.size()).build();

            ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(menuItemId)
                    .orElseGet(()->ThreeDAsset.builder().menuItem(item).build());
            asset.setStatus(ThreeDStatus.PROCESSING); asset.setErrorMessage(null); threeDAssetRepository.save(asset);
            return toResponse(jobRepository.save(job));
        } catch(Exception e) {
            String msg=safeMessage(e);
            jobRepository.save(ThreeDGenerationJob.builder().menuItem(item).provider("MESHY")
                    .providerTaskId("NOT_CREATED").generationType("FAILED_TO_START").progress(0)
                    .status(ThreeDGenerationJobStatus.FAILED).sourceMediaCount(0)
                    .errorMessage(msg).completedAt(LocalDateTime.now()).build());
            ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(menuItemId)
                    .orElseGet(()->ThreeDAsset.builder().menuItem(item).build());
            asset.setStatus(ThreeDStatus.FAILED); asset.setErrorMessage(msg); threeDAssetRepository.save(asset);
            throw new IllegalStateException("Could not start 3D generation: "+msg,e);
        }
    }

    @Scheduled(fixedDelayString="#{@meshyConfig.pollIntervalMs}")
    @Transactional
    public void pollRunningJobs() {
        for(ThreeDGenerationJob job:jobRepository.findByStatusIn(List.of(ThreeDGenerationJobStatus.PROCESSING))) {
            try {
                MeshyTaskResponse task="MULTI_IMAGE_TO_3D".equals(job.getGenerationType())
                        ?meshyClient.getMultiImageTask(job.getProviderTaskId()):meshyClient.getImageTask(job.getProviderTaskId());
                if(task==null) continue;
                if(task.progress()!=null) job.setProgress(task.progress());
                if("SUCCEEDED".equalsIgnoreCase(task.status())) completeJob(job,task);
                else if("FAILED".equalsIgnoreCase(task.status())||"CANCELED".equalsIgnoreCase(task.status()))
                    failJob(job,task.taskError()==null?"Meshy task "+task.status():task.taskError().message());
                else jobRepository.save(job);
            } catch(Exception e) { job.setErrorMessage(safeMessage(e)); jobRepository.save(job); }
        }
    }

    public ThreeDGenerationJobResponse getLatestJob(Long menuItemId) {
        getItem(menuItemId);
        return jobRepository.findTopByMenuItemIdOrderByCreatedAtDesc(menuItemId).map(this::toResponse).orElse(null);
    }

    private void completeJob(ThreeDGenerationJob job,MeshyTaskResponse task) {
        MeshyModelUrls urls=task.modelUrls();
        if(urls==null||urls.glb()==null||urls.usdz()==null){failJob(job,"Meshy completed but GLB or USDZ was not returned");return;}
        try {
            String base=sanitize(job.getMenuItem().getName());
            String glb=base+"-"+job.getId()+".glb", usdz=base+"-"+job.getId()+".usdz";
            String glbUrl=fileStorageService.saveBytes(download(urls.glb()),"3d/"+job.getMenuItem().getId(),glb);
            String usdzUrl=fileStorageService.saveBytes(download(urls.usdz()),"3d/"+job.getMenuItem().getId(),usdz);
            ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(job.getMenuItem().getId())
                    .orElseGet(()->ThreeDAsset.builder().menuItem(job.getMenuItem()).build());
            asset.setGlbUrl(glbUrl); asset.setUsdzUrl(usdzUrl); asset.setGlbFileName(glb); asset.setUsdzFileName(usdz);
            asset.setStatus(ThreeDStatus.READY); asset.setErrorMessage(null); threeDAssetRepository.save(asset);
            job.setProgress(100); job.setStatus(ThreeDGenerationJobStatus.SUCCEEDED); job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null); jobRepository.save(job);
        } catch(Exception e){failJob(job,"Could not persist generated 3D files: "+safeMessage(e));}
    }

    private void failJob(ThreeDGenerationJob job,String message){
        job.setStatus(ThreeDGenerationJobStatus.FAILED); job.setErrorMessage(message==null||message.isBlank()?"3D generation failed":message);
        job.setCompletedAt(LocalDateTime.now()); jobRepository.save(job);
        ThreeDAsset asset=threeDAssetRepository.findByMenuItemId(job.getMenuItem().getId())
                .orElseGet(()->ThreeDAsset.builder().menuItem(job.getMenuItem()).build());
        asset.setStatus(ThreeDStatus.FAILED); asset.setErrorMessage(job.getErrorMessage()); threeDAssetRepository.save(asset);
    }

    private byte[] download(String url){return RestClient.create().get().uri(url).retrieve().body(byte[].class);}

    private List<String> extractVideoFramesAsDataUris(String mediaUrl,int maxFrames){
        Path video=fileStorageService.resolveLocalPath(mediaUrl);
        if(video==null||!Files.exists(video)) throw new IllegalStateException("Video file is not available on the server: "+mediaUrl);
        Path dir=null;
        try{
            dir=Files.createTempDirectory("webar-3d-frames-");
            Process p=new ProcessBuilder("ffmpeg","-hide_banner","-loglevel","error","-y","-i",video.toString(),
                    "-vf","fps=1/2,scale=1024:-1","-frames:v",String.valueOf(maxFrames),dir.resolve("frame-%02d.jpg").toString())
                    .redirectErrorStream(true).start();
            String out=new String(p.getInputStream().readAllBytes());
            int exit=p.waitFor(); if(exit!=0) throw new IllegalStateException("ffmpeg failed: "+out);
            List<Path> frames;
            try(var s=Files.list(dir)){frames=s.filter(x->x.toString().endsWith(".jpg")).sorted().limit(maxFrames).toList();}
            List<String> result=new ArrayList<>();
            for(Path frame:frames) result.add("data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(Files.readAllBytes(frame)));
            return result;
        }catch(IOException e){throw new IllegalStateException("Could not extract video frames. Ensure ffmpeg is installed.",e);}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Video frame extraction was interrupted",e);}
        finally{if(dir!=null)try(var s=Files.walk(dir)){s.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}catch(IOException ignored){}}
    }

    private MenuItem getItem(Long id){return menuItemRepository.findById(id).orElseThrow(()->new NoSuchElementException("Menu item not found"));}
    private ThreeDGenerationJobResponse toResponse(ThreeDGenerationJob j){return new ThreeDGenerationJobResponse(j.getId(),j.getMenuItem().getId(),j.getProvider(),j.getProviderTaskId(),j.getGenerationType(),j.getProgress(),j.getStatus(),j.getSourceMediaCount(),j.getErrorMessage(),j.getCreatedAt(),j.getUpdatedAt(),j.getCompletedAt());}
    private String sanitize(String v){return v==null||v.isBlank()?"menu-item":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-+|-+$","");}
    private String safeMessage(Exception e){return e.getMessage()==null||e.getMessage().isBlank()?e.getClass().getSimpleName():e.getMessage();}
}
package com.webar.app.controller;

import com.webar.app.entity.MediaType;
import com.webar.app.entity.MenuImage;
import com.webar.app.entity.MenuItem;
import com.webar.app.entity.MenuMedia;
import com.webar.app.entity.ThreeDAsset;
import com.webar.app.entity.ThreeDStatus;
import com.webar.app.repository.MenuImageRepository;
import com.webar.app.repository.MenuItemRepository;
import com.webar.app.repository.MenuMediaRepository;
import com.webar.app.repository.ThreeDAssetRepository;
import com.webar.app.service.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu-items")
@CrossOrigin(origins = "*")
public class MenuItemMediaController {

    private final MenuItemRepository menuItemRepository;
    private final MenuImageRepository menuImageRepository;
    private final MenuMediaRepository menuMediaRepository;
    private final ThreeDAssetRepository threeDAssetRepository;
    private final FileStorageService fileStorageService;

    public MenuItemMediaController(
            MenuItemRepository menuItemRepository,
            MenuImageRepository menuImageRepository,
            MenuMediaRepository menuMediaRepository,
            ThreeDAssetRepository threeDAssetRepository,
            FileStorageService fileStorageService) {
        this.menuItemRepository = menuItemRepository;
        this.menuImageRepository = menuImageRepository;
        this.menuMediaRepository = menuMediaRepository;
        this.threeDAssetRepository = threeDAssetRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/{id}/images")
    public MenuImage uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MenuItem item = getItem(id);
        String url = fileStorageService.saveFile(file, "menu-images/" + id,
                new String[]{"jpg", "jpeg", "png", "webp"});

        MenuImage image = MenuImage.builder()
                .menuItem(item)
                .fileName(file.getOriginalFilename())
                .imageUrl(url)
                .sortOrder(nextImageOrder(id))
                .primaryImage(menuImageRepository.findByMenuItemIdOrderBySortOrderAscIdAsc(id).isEmpty())
                .build();
        return menuImageRepository.save(image);
    }

    @PostMapping("/{id}/media")
    public MenuMedia uploadSourceMedia(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MenuItem item = getItem(id);
        String url = fileStorageService.saveFile(file, "source-media/" + id,
                new String[]{"jpg", "jpeg", "png", "webp", "mp4", "mov"});
        String ext = extension(file.getOriginalFilename());
        MediaType type = (ext.equals("mp4") || ext.equals("mov")) ? MediaType.VIDEO : MediaType.PHOTO;

        MenuMedia media = MenuMedia.builder()
                .menuItem(item)
                .fileName(file.getOriginalFilename())
                .mediaUrl(url)
                .mediaType(type)
                .build();
        return menuMediaRepository.save(media);
    }

    @PostMapping("/{id}/glb")
    public ThreeDAsset uploadGlb(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MenuItem item = getItem(id);
        String url = fileStorageService.saveFile(file, "3d/" + id, new String[]{"glb"});
        ThreeDAsset asset = getOrCreateAsset(id, item);
        asset.setGlbUrl(url);
        asset.setGlbFileName(file.getOriginalFilename());
        asset.setStatus(asset.getUsdzUrl() != null ? ThreeDStatus.READY : ThreeDStatus.SOURCE_UPLOADED);
        return threeDAssetRepository.save(asset);
    }

    @PostMapping("/{id}/usdz")
    public ThreeDAsset uploadUsdz(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MenuItem item = getItem(id);
        String url = fileStorageService.saveFile(file, "3d/" + id, new String[]{"usdz"});
        ThreeDAsset asset = getOrCreateAsset(id, item);
        asset.setUsdzUrl(url);
        asset.setUsdzFileName(file.getOriginalFilename());
        asset.setStatus(asset.getGlbUrl() != null ? ThreeDStatus.READY : ThreeDStatus.SOURCE_UPLOADED);
        return threeDAssetRepository.save(asset);
    }

    @GetMapping("/{id}/images")
    public List<MenuImage> getImages(@PathVariable Long id) {
        getItem(id);
        return menuImageRepository.findByMenuItemIdOrderBySortOrderAscIdAsc(id);
    }

    @GetMapping("/{id}/media")
    public List<MenuMedia> getMedia(@PathVariable Long id) {
        getItem(id);
        return menuMediaRepository.findByMenuItemIdOrderByIdAsc(id);
    }

    @GetMapping("/{id}/3d")
    public ThreeDAsset getThreeD(@PathVariable Long id) {
        getItem(id);
        return threeDAssetRepository.findByMenuItemId(id).orElse(null);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        getItem(id);
        MenuImage image = menuImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (!image.getMenuItem().getId().equals(id)) throw new RuntimeException("Image does not belong to menu item");
        fileStorageService.deleteFile(image.getImageUrl());
        menuImageRepository.delete(image);
        return "Image deleted successfully";
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public String deleteMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        getItem(id);
        MenuMedia media = menuMediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        if (!media.getMenuItem().getId().equals(id)) throw new RuntimeException("Media does not belong to menu item");
        fileStorageService.deleteFile(media.getMediaUrl());
        menuMediaRepository.delete(media);
        return "Media deleted successfully";
    }

    @DeleteMapping("/{id}/3d/{format}")
    public String deleteThreeD(@PathVariable Long id, @PathVariable String format) {
        getItem(id);
        ThreeDAsset asset = threeDAssetRepository.findByMenuItemId(id)
                .orElseThrow(() -> new RuntimeException("3D asset not found"));
        if (format.equalsIgnoreCase("glb")) {
            fileStorageService.deleteFile(asset.getGlbUrl());
            asset.setGlbUrl(null);
            asset.setGlbFileName(null);
        } else if (format.equalsIgnoreCase("usdz")) {
            fileStorageService.deleteFile(asset.getUsdzUrl());
            asset.setUsdzUrl(null);
            asset.setUsdzFileName(null);
        } else {
            throw new RuntimeException("Format must be glb or usdz");
        }
        asset.setStatus(asset.getGlbUrl() != null && asset.getUsdzUrl() != null
                ? ThreeDStatus.READY
                : asset.getGlbUrl() != null || asset.getUsdzUrl() != null
                  ? ThreeDStatus.SOURCE_UPLOADED
                  : ThreeDStatus.NOT_STARTED);
        threeDAssetRepository.save(asset);
        return "3D asset deleted successfully";
    }

    private MenuItem getItem(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
    }

    private ThreeDAsset getOrCreateAsset(Long id, MenuItem item) {
        return threeDAssetRepository.findByMenuItemId(id)
                .orElse(ThreeDAsset.builder().menuItem(item).build());
    }

    private int nextImageOrder(Long id) {
        List<MenuImage> images = menuImageRepository.findByMenuItemIdOrderBySortOrderAscIdAsc(id);
        return images.stream().map(MenuImage::getSortOrder).filter(x -> x != null).max(Integer::compareTo).orElse(0) + 1;
    }

    private String extension(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }
}

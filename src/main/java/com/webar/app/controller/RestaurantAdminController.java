package com.webar.app.controller;

import com.webar.app.dto.RestaurantRequest;
import com.webar.app.entity.Restaurant;
import com.webar.app.service.FileStorageService;
import com.webar.app.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/restaurants")
@CrossOrigin(origins = "*")
public class RestaurantAdminController {

    private final RestaurantService service;
    private final FileStorageService fileStorageService;

    public RestaurantAdminController(RestaurantService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public Restaurant create(@Valid @RequestBody RestaurantRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Restaurant> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Restaurant getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Restaurant update(@PathVariable Long id, @Valid @RequestBody RestaurantRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public Restaurant activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public Restaurant deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }

    @PostMapping("/{id}/logo")
    public Restaurant uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Restaurant restaurant = service.getById(id);
        String url = fileStorageService.saveFile(file, "restaurant-logos/" + id,
                new String[]{"jpg", "jpeg", "png", "webp"});
        restaurant.setLogoUrl(url);
        return service.save(restaurant);
    }
}

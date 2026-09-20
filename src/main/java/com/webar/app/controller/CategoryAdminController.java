package com.webar.app.controller;

import com.webar.app.dto.CategoryRequest;
import com.webar.app.entity.Category;
import com.webar.app.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "*")
public class CategoryAdminController {

    private final CategoryService service;

    public CategoryAdminController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<Category> getByRestaurant(@PathVariable Long restaurantId) {
        return service.getByRestaurant(restaurantId);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/activate")
    public Category activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    public Category deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }
}

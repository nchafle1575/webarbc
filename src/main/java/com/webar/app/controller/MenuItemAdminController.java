package com.webar.app.controller;

import com.webar.app.dto.MenuItemRequest;
import com.webar.app.entity.MenuItem;
import com.webar.app.service.MenuItemService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu-items")
@CrossOrigin(origins = "*")
public class MenuItemAdminController {

    private final MenuItemService service;

    public MenuItemAdminController(
            MenuItemService service
    ) {

        this.service = service;
    }

    @PostMapping
    public MenuItem create(
            @Valid @RequestBody MenuItemRequest request
    ) {

        return service.create(request);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<MenuItem> getAll(
            @PathVariable Long restaurantId
    ) {

        return service.getAll(
                restaurantId
        );
    }

    @GetMapping(
            "/restaurant/{restaurantId}/published"
    )
    public List<MenuItem> getPublished(
            @PathVariable Long restaurantId
    ) {

        return service.getPublished(
                restaurantId
        );
    }

    @GetMapping("/{id}")
    public MenuItem getById(
            @PathVariable Long id
    ) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    public MenuItem update(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request
    ) {

        return service.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id
    ) {

        service.delete(id);

        return "Menu item deleted successfully";
    }

    @PostMapping("/{id}/publish")
    public MenuItem publish(
            @PathVariable Long id
    ) {

        return service.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    public MenuItem unpublish(
            @PathVariable Long id
    ) {

        return service.unpublish(id);
    }
}
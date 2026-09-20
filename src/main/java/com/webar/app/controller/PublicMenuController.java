package com.webar.app.controller;

import com.webar.app.dto.PublicMenuResponse;
import com.webar.app.service.PublicMenuService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class PublicMenuController {

    private final PublicMenuService publicMenuService;

    public PublicMenuController(PublicMenuService publicMenuService) {
        this.publicMenuService = publicMenuService;
    }

    @GetMapping("/{restaurantSlug}")
    public PublicMenuResponse getMenu(@PathVariable String restaurantSlug) {
        return publicMenuService.getMenu(restaurantSlug);
    }
}

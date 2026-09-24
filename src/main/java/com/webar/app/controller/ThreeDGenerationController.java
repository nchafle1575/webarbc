package com.webar.app.controller;

import com.webar.app.dto.ThreeDGenerationJobResponse;
import com.webar.app.service.ThreeDGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/menu-items")
@CrossOrigin(origins = "*")
public class ThreeDGenerationController {
    private final ThreeDGenerationService generationService;
    public ThreeDGenerationController(ThreeDGenerationService generationService){this.generationService=generationService;}

    @PostMapping("/{id}/3d-model/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ThreeDGenerationJobResponse generate(@PathVariable Long id){return generationService.generate(id);}

    @GetMapping("/{id}/3d-model/status")
    public ThreeDGenerationJobResponse status(@PathVariable Long id){return generationService.getLatestJob(id);}
}

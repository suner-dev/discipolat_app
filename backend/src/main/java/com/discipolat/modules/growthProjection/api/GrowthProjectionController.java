package com.discipolat.modules.growthProjection.api;

import com.discipolat.modules.growthProjection.domain.GrowthProjection;
import com.discipolat.modules.growthProjection.domain.GrowthProjectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/growth-projections")
@PreAuthorize("isAuthenticated()")
public class GrowthProjectionController {

    private final GrowthProjectionService service;
    public GrowthProjectionController(GrowthProjectionService service) { this.service = service; }

    @GetMapping
    public List<GrowthProjection> list() { return service.listAll(); }

    // P3 #103 — Prophétie de croissance (modèle prédictif sur données réelles)
    @GetMapping("/prophecy")
    public Map<String, Object> prophecy() { return service.prophesy(); }


    @GetMapping("/{id}")
    public GrowthProjection get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping("/simulate")
    public GrowthProjection simulate(@RequestBody GrowthProjection proj) {
        return service.simulate(proj);
    }

    @PostMapping
    public ResponseEntity<GrowthProjection> create(@RequestBody GrowthProjection proj) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(proj));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

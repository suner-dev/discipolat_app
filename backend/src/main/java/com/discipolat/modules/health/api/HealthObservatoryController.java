package com.discipolat.modules.health.api;

import com.discipolat.modules.health.domain.SpiritualHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health-observatory")
public class HealthObservatoryController {

    private final SpiritualHealthService service;

    public HealthObservatoryController(SpiritualHealthService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> observatory() {
        return ResponseEntity.ok(service.observatory());
    }
}

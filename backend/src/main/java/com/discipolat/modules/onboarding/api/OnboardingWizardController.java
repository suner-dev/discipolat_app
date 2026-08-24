package com.discipolat.modules.onboarding.api;

import com.discipolat.modules.onboarding.domain.OnboardingWizardService;
import com.discipolat.modules.onboarding.domain.OnboardingWizardStep;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding-wizard")
public class OnboardingWizardController {

    private final OnboardingWizardService service;
    public OnboardingWizardController(OnboardingWizardService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> getSteps() { return ResponseEntity.ok(service.getSteps()); }

    @GetMapping("/progress")
    public ResponseEntity<?> progress() { return ResponseEntity.ok(service.getProgress()); }

    @PostMapping("/initialize")
    public ResponseEntity<?> initialize() { return ResponseEntity.ok(service.initializeSteps()); }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable UUID id) { return ResponseEntity.ok(service.startStep(id)); }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.completeStep(id, body.get("data")));
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<?> skip(@PathVariable UUID id) { return ResponseEntity.ok(service.skipStep(id)); }
}

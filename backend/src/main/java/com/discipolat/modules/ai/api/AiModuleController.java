package com.discipolat.modules.ai.api;

import com.discipolat.modules.ai.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ai/module")
public class AiModuleController {

    private final AiModuleService moduleService;
    private final AiKpiNarrativeService kpiService;
    private final AiFamilyCohesionService familyService;
    private final SermonAssistantService sermonService;

    public AiModuleController(AiModuleService moduleService, AiKpiNarrativeService kpiService,
                              AiFamilyCohesionService familyService, SermonAssistantService sermonService) {
        this.moduleService = moduleService;
        this.kpiService = kpiService;
        this.familyService = familyService;
        this.sermonService = sermonService;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(moduleService.getTenantAiSummary());
    }

    @GetMapping("/soul/{soulId}/analyze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> analyzeSoul(@PathVariable UUID soulId) {
        return ResponseEntity.ok(moduleService.analyzeSoul(soulId));
    }

    @GetMapping("/soul/{soulId}/encouragement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> encouragement(@PathVariable UUID soulId) {
        return ResponseEntity.ok(Map.of("message", moduleService.generateEncouragement(soulId)));
    }

    @GetMapping("/kpi-narrative")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getKpiNarrative() {
        return ResponseEntity.ok(kpiService.generateNarrative());
    }

    @GetMapping("/family/{familyId}/cohesion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> analyzeFamily(@PathVariable UUID familyId) {
        return ResponseEntity.ok(familyService.analyzeFamilyCohesion(familyId));
    }

    @GetMapping("/families/cohesion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> analyzeAllFamilies() {
        return ResponseEntity.ok(familyService.analyzeAllFamilies());
    }

    @PostMapping("/sermon/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> generateSermon(@RequestBody Map<String, String> body) {
        String passage = body.getOrDefault("passage", "");
        String theme = body.getOrDefault("theme", "");
        String audience = body.getOrDefault("audience", "");
        return ResponseEntity.ok(sermonService.generateOutlines(passage, theme, audience, null));
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "services", Map.of(
                "module", true,
                "kpi", true,
                "family", true,
                "sermon", true
            )
        ));
    }
}

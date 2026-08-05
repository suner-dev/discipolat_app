package com.discipolat.modules.ai.api;

import com.discipolat.modules.ai.domain.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/analyze/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> analyze(@PathVariable UUID soulId) {
        return ResponseEntity.ok(aiAssistantService.analyze(soulId));
    }

    @GetMapping("/resume/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, String>> resume(@PathVariable UUID soulId) {
        return ResponseEntity.ok(Map.of("resume", aiAssistantService.resume(soulId)));
    }

    @GetMapping("/encouragement/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, String>> encouragement(@PathVariable UUID soulId) {
        return ResponseEntity.ok(aiAssistantService.encouragement(soulId));
    }
}

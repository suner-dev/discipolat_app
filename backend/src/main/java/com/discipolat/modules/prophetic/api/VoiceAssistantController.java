package com.discipolat.modules.prophetic.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.prophetic.domain.VoiceAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * P0 #5 — Assistant vocal conversationnel "PasteurBot".
 * API pour la transcription vocale et les commandes vocales.
 */
@RestController
@RequestMapping("/api/v1/voice")
@PreAuthorize("isAuthenticated()")
public class VoiceAssistantController {

    private final VoiceAssistantService voiceService;
    private final SecurityUtils securityUtils;

    public VoiceAssistantController(VoiceAssistantService voiceService, SecurityUtils securityUtils) {
        this.voiceService = voiceService;
        this.securityUtils = securityUtils;
    }

    /**
     * Traite un message vocal transcrit et retourne une réponse contextuelle.
     * POST /api/v1/voice/process
     * Body: { "transcription": "Montre-moi les familles en décrochement", "sessionId": "..." }
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processVoice(@RequestBody Map<String, String> body) {
        String transcription = body.getOrDefault("transcription", "");
        String sessionIdStr = body.get("sessionId");
        UUID sessionId = sessionIdStr != null ? UUID.fromString(sessionIdStr) : null;

        if (transcription.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcription requise"));
        }

        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(voiceService.processVoiceMessage(userId, transcription, sessionId));
    }

    /**
     * Liste les commandes vocales disponibles (pour le tutoriel).
     * GET /api/v1/voice/commands
     */
    @GetMapping("/commands")
    public ResponseEntity<List<Map<String, String>>> getCommands() {
        return ResponseEntity.ok(voiceService.getVoiceCommands());
    }

    /**
     * Health check de l'assistant vocal.
     * GET /api/v1/voice/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(voiceService.healthCheck());
    }
}

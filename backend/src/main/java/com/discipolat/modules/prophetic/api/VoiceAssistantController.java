package com.discipolat.modules.prophetic.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.prophetic.domain.SpeechToTextException;
import com.discipolat.modules.prophetic.domain.VoiceAssistantService;
import com.discipolat.modules.prophetic.domain.VoiceSttService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final VoiceSttService sttService;
    private final SecurityUtils securityUtils;

    public VoiceAssistantController(VoiceAssistantService voiceService,
                                    VoiceSttService sttService,
                                    SecurityUtils securityUtils) {
        this.voiceService = voiceService;
        this.sttService = sttService;
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
     * Transcrit un échantillon audio réel (multipart) puis le traite.
     * POST /api/v1/voice/transcribe
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribe(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier audio requis"));
        }
        try {
            byte[] audio = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.m4a";
            return ResponseEntity.ok(sttService.transcribeAndProcess(audio, filename, language, sessionId));
        } catch (SpeechToTextException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage(), "code", "STT_NOT_CONFIGURED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Transcription impossible : " + e.getMessage()));
        }
    }

    /**
     * Statut des fournisseurs Speech-to-Text configurés.
     * GET /api/v1/voice/stt-status
     */
    @GetMapping("/stt-status")
    public ResponseEntity<?> sttStatus() {
        return ResponseEntity.ok(sttService.status());
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

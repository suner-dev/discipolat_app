package com.discipolat.modules.ai.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.ai.domain.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private final AiAssistantService service;
    private final SecurityUtils securityUtils;

    public AiAssistantController(AiAssistantService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    /**
     * Chat avec l'assistant IA pastoral.
     * Envoie une question et reçoit une réponse contextuelle basée
     * sur les données de l'église.
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }
        return ResponseEntity.ok(service.chat(message, securityUtils.getCurrentUserId()));
    }

    /**
     * Récupère le contexte pertinent pour une question.
     * Utilisé quand Ollama n'est pas disponible — retourne les données
     * brutes que le frontend peut formater.
     */
    @GetMapping("/context")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> context(@RequestParam String query) {
        return ResponseEntity.ok(service.getContextForQuery(query));
    }

    /**
     * Historique du chat (pour la session utilisateur).
     */
    @GetMapping("/chat/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> chatHistory() {
        return ResponseEntity.ok(service.getChatHistory(securityUtils.getCurrentUserId()));
    }

    /**
     * Effacer l'historique du chat.
     */
    @DeleteMapping("/chat/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearChatHistory() {
        service.clearChatHistory(securityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Vérifie l'état d'Ollama (santé du service IA).
     */
    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(service.checkHealth());
    }
}

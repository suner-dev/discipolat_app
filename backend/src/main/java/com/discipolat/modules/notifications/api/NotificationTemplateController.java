package com.discipolat.modules.notifications.api;

import com.discipolat.modules.notifications.domain.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration des modèles de notification (centre de configuration admin).
 *
 * <p>Un modèle associe un événement ({@link com.discipolat.common.enums.TypeNotification})
 * à un titre/message rendus (variables {@code {{...}}}), des canaux de diffusion et des
 * rôles destinataires. Le {@code NotificationService} applique réellement ces modèles à
 * l'émission des notifications.
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    public NotificationTemplateController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    /** Catalogue des événements configurables (libellés + modèle suggéré). */
    @GetMapping("/events")
    public ResponseEntity<List<NotificationEventInfo>> events() {
        return ResponseEntity.ok(templateService.eventCatalog());
    }

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplateResponse>> list() {
        return ResponseEntity.ok(templateService.list());
    }

    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateResponse> create(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<NotificationTemplateResponse> update(
            @PathVariable UUID id, @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @PatchMapping("/templates/{id}/toggle")
    public ResponseEntity<NotificationTemplateResponse> toggle(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(templateService.toggle(id, body.getOrDefault("actif", true)));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Modèle supprimé"));
    }
}

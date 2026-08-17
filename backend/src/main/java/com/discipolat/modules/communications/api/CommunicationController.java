package com.discipolat.modules.communications.api;

import com.discipolat.modules.communications.domain.CommunicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API de l'outil métier COMMUNICATION (annonces ciblées).
 * Lecture : tout rôle authentifié (annonces publiées dans sa cible) ;
 * gestion : ADMIN / PASTEUR. Module activable (ModuleGateFilter → COMMUNICATION).
 */
@RestController
@RequestMapping("/api/v1/communications")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    /** Annonces publiées visibles par l'utilisateur courant. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listPublished() {
        return ResponseEntity.ok(communicationService.listForCurrentUser());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        return ResponseEntity.ok(communicationService.listAll());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CommunicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communicationService.create(request));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @Valid @RequestBody CommunicationRequest request) {
        return ResponseEntity.ok(communicationService.update(id, request));
    }

    @PostMapping("/admin/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(communicationService.publish(id));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        communicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

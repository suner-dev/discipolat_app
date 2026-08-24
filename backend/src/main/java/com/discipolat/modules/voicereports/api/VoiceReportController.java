package com.discipolat.modules.voicereports.api;

import com.discipolat.modules.voicereports.domain.VoiceReport;
import com.discipolat.modules.voicereports.domain.VoiceReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/voice-reports")
public class VoiceReportController {

    private final VoiceReportService service;

    public VoiceReportController(VoiceReportService service) {
        this.service = service;
    }

    /** Synchronisation d'un rapport dicté hors ligne. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<VoiceReport> create(@RequestBody VoiceReport report) {
        return ResponseEntity.ok(service.create(report));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<VoiceReport>> recent() {
        return ResponseEntity.ok(service.recent());
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VoiceReport>> mine() {
        return ResponseEntity.ok(service.mine());
    }

    /** P14 — Génère un rapport structuré Markdown à partir des entités extraites. */
    @GetMapping("/{id}/structured")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<java.util.Map<String, Object>> structured(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(service.generateStructuredReport(id));
    }

    /** P14 — Liste des actions à suivre (extraites de tous les rapports). */
    @GetMapping("/action-items")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> actionItems() {
        return ResponseEntity.ok(service.extractAllActionItems());
    }
}

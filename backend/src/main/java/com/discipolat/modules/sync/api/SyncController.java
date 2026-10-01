package com.discipolat.modules.sync.api;

import com.discipolat.modules.sync.domain.SyncBatchService;
import com.discipolat.modules.sync.domain.SyncConflict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * §G5.7 — Porte d'entrée de la file d'écriture hors-ligne du mobile.
 *
 * Le mobile envoie ses items par lot à la reconnexion ; chaque item porte un
 * {@code clientUuid} stable → le serveur est idempotent (rejeu = doublon
 * ignoré). Le dispatch applique les mêmes gardes {@code @PreAuthorize} que les
 * endpoints en ligne (appel des beans controlleurs via proxy AOP).
 */
@RestController
@RequestMapping("/api/v1/sync")
@PreAuthorize("isAuthenticated()")
public class SyncController {

    private final SyncBatchService syncBatchService;

    public SyncController(SyncBatchService syncBatchService) {
        this.syncBatchService = syncBatchService;
    }

    public record SyncOpDto(String clientUuid, String type, Map<String, Object> payload,
                            String at, String photoBase64, String photoMime, String photoName) {}

    public record BatchRequest(List<SyncOpDto> operations) {}

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batch(@RequestBody BatchRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<SyncBatchService.ClientOp> ops = request.operations() == null
                ? List.of() : request.operations().stream()
                .map(o -> new SyncBatchService.ClientOp(
                        o.clientUuid(), o.type(), o.payload(),
                        parseAt(o.at()),
                        o.photoBase64(), o.photoMime(), o.photoName()))
                .toList();
        SyncBatchService.BatchResult r = syncBatchService.applyBatch(ops, auth);
        return ResponseEntity.ok(Map.of(
                "results", r.results(),
                "applied", r.applied(),
                "duplicates", r.duplicates(),
                "failed", r.failed(),
                "conflicts", r.conflicts()));
    }

    /** Conflits LWW en attente de réconciliation (écran responsable web/mobile). */
    @GetMapping("/conflicts")
    public ResponseEntity<List<SyncConflict>> conflicts() {
        return ResponseEntity.ok(syncBatchService.openConflicts());
    }

    /** Réconciliation manuelle documentée (§1111) : trace la décision, ne réécrit pas. */
    @PostMapping("/conflicts/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<SyncConflict> resolve(@PathVariable UUID id,
                                                @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(syncBatchService.resolveConflict(id,
                body != null ? body.get("note") : null));
    }

    private static LocalDateTime parseAt(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(iso);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}

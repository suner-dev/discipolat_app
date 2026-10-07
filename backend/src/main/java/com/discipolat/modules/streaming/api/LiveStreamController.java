package com.discipolat.modules.streaming.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.streaming.domain.LiveStream;
import com.discipolat.modules.streaming.domain.LiveStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V240 : le tenant est TOUJOURS dérivé du contexte authentifié
 * (JWT via TenantContext), plus jamais d'un @RequestParam client —
 * l'ancienne signature list(live) acceptait un tenantId fourni par
 * l'appelant (IDOR en lecture). Toutes les opérations par id sont
 * scopées tenant côté service (anti-IDOR en écriture).
 */
@RestController
@RequestMapping("/api/v1/streams")
@PreAuthorize("isAuthenticated()")
public class LiveStreamController {

    private final LiveStreamService service;

    public LiveStreamController(LiveStreamService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LiveStream>> list() {
        return ResponseEntity.ok(service.listByTenant(TenantContext.requireTenantId()));
    }

    @GetMapping("/live")
    public ResponseEntity<List<LiveStream>> live() {
        return ResponseEntity.ok(service.listLive(TenantContext.requireTenantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveStream> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(TenantContext.requireTenantId(), id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping
    public ResponseEntity<LiveStream> create(@RequestBody LiveStream stream) {
        // tenantId et createdBy du corps sont ignorés : forcés serveur.
        return ResponseEntity.ok(service.create(stream, SecurityUtils.getCurrentUserId()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PutMapping("/{id}")
    public ResponseEntity<LiveStream> update(@PathVariable Long id, @RequestBody LiveStream stream) {
        // Seuls les champs éditables sont appliqués sur l'entité chargée,
        // scopée tenant : id/tenant/créateur du corps ne peuvent rien écraser.
        return ResponseEntity.ok(service.update(TenantContext.requireTenantId(), id, stream));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(TenantContext.requireTenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/go-live")
    public ResponseEntity<LiveStream> goLive(@PathVariable Long id) {
        return ResponseEntity.ok(service.goLive(TenantContext.requireTenantId(), id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/end")
    public ResponseEntity<LiveStream> endStream(@PathVariable Long id) {
        return ResponseEntity.ok(service.endStream(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/viewer")
    public ResponseEntity<LiveStream> addViewer(@PathVariable Long id) {
        return ResponseEntity.ok(service.incrementViewers(TenantContext.requireTenantId(), id));
    }
}

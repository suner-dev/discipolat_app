package com.discipolat.modules.audit.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditLog;
import com.discipolat.modules.audit.domain.AuditEvent;
import com.discipolat.modules.audit.domain.BusinessHistory;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.audit.service.AuditEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
public class AuditController {

    private final AuditService auditService;           // Legacy audit_logs (V1 schema)
    private final AuditEventService auditEventService; // New audit_event (G2.9 schema with hash chain)

    public AuditController(AuditService auditService, AuditEventService auditEventService) {
        this.auditService = auditService;
        this.auditEventService = auditEventService;
    }

    // ========== LEGACY AUDIT (audit_logs table from V1) ==========

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PageResponse<AuditLog>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String entiteType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditService.findFiltered(utilisateurId, entiteType, action, debut, fin, pageable);
        return ResponseEntity.ok(PageResponse.of(
                logs.getContent(), logs.getNumber(), logs.getSize(),
                logs.getTotalElements(), logs.getTotalPages()));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) UUID utilisateurId,
            @RequestParam(required = false) String entiteType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        byte[] csv = auditService.exportCsv(utilisateurId, entiteType, action, debut, fin);
        String filename = "journal-audit-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<AuditLog> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.findById(id));
    }

    /** Activité récente — fil d'activité du dashboard Pasteur. */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(auditService.getRecentActivity(limit));
    }

    /** Tendances d'audit : répartition des actions sur N jours. */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getAuditTrend(
            @RequestParam(defaultValue = "30") int jours) {
        return ResponseEntity.ok(auditService.getAuditTrend(jours));
    }

    // ========== G2.9 AUDIT ENGINE (audit_event + business_history) ==========

    /**
     * Recherche paginée des événements d'audit G2.9 (nouvelle table audit_event avec hash chain).
     */
    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PageResponse<AuditEvent>> findEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditEvent> events = auditEventService.findFiltered(tenantId, actorId, entity, action, from, to, pageable);
        return ResponseEntity.ok(PageResponse.of(
                events.getContent(), events.getNumber(), events.getSize(),
                events.getTotalElements(), events.getTotalPages()));
    }

    /**
     * Export CSV des événements d'audit G2.9 (audité : trace export).
     */
    @GetMapping(value = "/events/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<byte[]> exportEventsCsv(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID tenantId = TenantContext.requireTenantId();
        byte[] csv = auditEventService.exportCsv(tenantId, actorId, entity, action, from, to);
        String filename = "audit-events-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    /**
     * Vérifie l'intégrité de la chaîne de hachage (hash chain) du tenant courant.
     */
    @GetMapping("/events/verify-chain")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(auditEventService.verifyAuditChain(tenantId));
    }

    /**
     * Historique métier d'un objet (timeline : ce qui est arrivé à l'objet).
     * Distinct de l'audit technique : pas de hash chain, requêtable par objet métier.
     */
    @GetMapping("/history/{objectType}/{objectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<BusinessHistory>> getObjectHistory(
            @PathVariable String objectType,
            @PathVariable UUID objectId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(auditEventService.getObjectHistory(tenantId, objectType, objectId));
    }

    /**
     * Historique métier d'un espace (dashboard espace).
     */
    @GetMapping("/history/space/{spaceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<BusinessHistory>> getSpaceHistory(
            @PathVariable UUID spaceId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(auditEventService.getSpaceHistory(tenantId, spaceId));
    }
}

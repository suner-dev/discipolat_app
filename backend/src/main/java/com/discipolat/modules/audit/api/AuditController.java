package com.discipolat.modules.audit.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.audit.domain.AuditLog;
import com.discipolat.modules.audit.domain.AuditService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

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
}

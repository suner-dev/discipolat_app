package com.discipolat.modules.audit.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.audit.domain.AuditLog;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String entiteType) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs;
        if (utilisateurId != null) {
            logs = auditService.findByUtilisateur(utilisateurId, pageable);
        } else if (entiteType != null) {
            logs = auditService.findByEntite(entiteType, null, pageable);
        } else {
            logs = auditService.findAll(pageable);
        }
        return ResponseEntity.ok(PageResponse.of(
                logs.getContent(), logs.getNumber(), logs.getSize(),
                logs.getTotalElements(), logs.getTotalPages()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<AuditLog> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.findById(id));
    }
}

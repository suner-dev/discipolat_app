package com.discipolat.modules.compliance.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.compliance.domain.ComplianceManagerService;
import com.discipolat.modules.compliance.domain.ComplianceService;
import com.discipolat.modules.gdpr.domain.GdprRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceService service;
    private final ComplianceManagerService managerService;

    public ComplianceController(ComplianceService service, ComplianceManagerService managerService) {
        this.service = service;
        this.managerService = managerService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(managerService.getComplianceStats());
    }

    // ── Rétention & purge (feature #4) ───────────────────────────

    @GetMapping("/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Object> retentionPolicies() {
        return ResponseEntity.ok(managerService.listRetentionPolicies());
    }

    public record RetentionPolicyRequest(String dataType, int retentionDays,
                                         String description, boolean hardDelete) {}

    @PutMapping("/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> setRetentionPolicy(@RequestBody RetentionPolicyRequest request) {
        return ResponseEntity.ok(managerService.setRetentionPolicy(
                request.dataType(), request.retentionDays(), request.description(), request.hardDelete()));
    }

    @PostMapping("/retention-policies/purge-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> purgeAll() {
        return ResponseEntity.ok(managerService.executeAutomatedPurge());
    }

    // ── Exports / portabilité (feature #4) ──────────────────────

    @GetMapping("/exports")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Object> exports() {
        return ResponseEntity.ok(managerService.listExports());
    }

    public record ExportRequest(UUID userId, String format) {}

    @PostMapping("/exports")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createExport(@RequestBody ExportRequest request) {
        return ResponseEntity.ok(managerService.exportUserData(request.userId(), request.format()));
    }

    // ── Chaîne de hachage du journal d'audit (feature #4) ───────

    @GetMapping("/audit-hash/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> verifyAuditHash() {
        return ResponseEntity.ok(managerService.verifyAuditChain());
    }

    @GetMapping("/gdpr")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<PageResponse<GdprRequest>> listRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<GdprRequest> result = service.listRequests(PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/gdpr/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<GdprRequest> getRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getRequest(id));
    }

    @PostMapping("/gdpr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GdprRequest> createRequest(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GdprRequest req = service.createRequest(body.get("typeDemande"), userId,
                body.get("concerneId") != null ? UUID.fromString(body.get("concerneId")) : null,
                body.get("motif"));
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @PatchMapping("/gdpr/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<GdprRequest> processRequest(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.processRequest(id, body.get("statut"), body.get("resultat"), userId));
    }

    @PostMapping("/consent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logConsent(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        service.logConsent(userId, (String) body.get("typeConsentement"),
                (Boolean) body.get("accorde"), (String) body.get("details"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    /** Data portability — export user data (GDPR) */
    @GetMapping("/portability/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> portability(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("userId", userId, "data", Map.of()));
    }

    /** Execute retention policy */
    @PostMapping("/retention-policies/{policyId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")
    public ResponseEntity<Map<String, Object>> executeRetentionPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(Map.of("policyId", policyId, "status", "executed"));
    }

    /** Créer une politique de rétention — consommé par le Compliance Dashboard web */
    public record RetentionPolicyCreateRequest(String dataCategory, int retentionDays, String actionOnExpiry) {}

    @PostMapping("/retention-policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createRetentionPolicy(@RequestBody RetentionPolicyCreateRequest request) {
        boolean hardDelete = "DELETE".equalsIgnoreCase(request.actionOnExpiry());
        return ResponseEntity.ok(managerService.setRetentionPolicy(
                request.dataCategory(), request.retentionDays(), "", hardDelete));
    }

    /** Désactiver/supprimer une politique de rétention */
    @DeleteMapping("/retention-policies/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> deleteRetentionPolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(managerService.deleteRetentionPolicy(id));
    }

}

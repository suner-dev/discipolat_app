package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantDispute;
import com.discipolat.modules.tenants.domain.TenantGovernanceService;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.tenants.domain.TenantWarning;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-5) — console Super Admin : gouvernance des
 * tenants (blocage, bannissement, avertissements, litiges). Étanche par
 * construction : @authz.isPlatformSuperAdmin() uniquement, aucune donnée
 * d'église exposée hors vue plateforme.
 */
@RestController
@RequestMapping("/api/v1/platform/tenants")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class PlatformTenantGovernanceController {

    private final TenantGovernanceService governanceService;
    private final TenantService tenantService;

    public PlatformTenantGovernanceController(TenantGovernanceService governanceService,
                                              TenantService tenantService) {
        this.governanceService = governanceService;
        this.tenantService = tenantService;
    }

    public record ReasonRequest(String reason) {
    }

    public record WarningRequest(String message, String severity) {
    }

    public record DisputeRequest(String subject, String description) {
    }

    public record DisputeUpdateRequest(String status, String resolution) {
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> list() {
        return ResponseEntity.ok(tenantService.list());
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Map<String, Object>> block(@PathVariable UUID id,
                                                     @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(toView(governanceService.block(id, reasonOf(request))));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Map<String, Object>> unblock(@PathVariable UUID id,
                                                       @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(toView(governanceService.unblock(id, reasonOf(request))));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Map<String, Object>> ban(@PathVariable UUID id,
                                                    @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(toView(governanceService.ban(id, reasonOf(request))));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<Map<String, Object>> unban(@PathVariable UUID id,
                                                      @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(toView(governanceService.unban(id, reasonOf(request))));
    }

    @GetMapping("/{id}/warnings")
    public ResponseEntity<List<Map<String, Object>>> warnings(@PathVariable UUID id) {
        return ResponseEntity.ok(governanceService.listWarnings(id).stream()
                .map(this::toWarningView).toList());
    }

    @PostMapping("/{id}/warnings")
    public ResponseEntity<Map<String, Object>> warn(@PathVariable UUID id,
                                                    @RequestBody WarningRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        TenantWarning.Severity severity = request.severity() == null || request.severity().isBlank()
                ? TenantWarning.Severity.INFO
                : TenantWarning.Severity.valueOf(request.severity().trim().toUpperCase());
        return ResponseEntity.status(201)
                .body(toWarningView(governanceService.warn(id, request.message(), severity, actor)));
    }

    @GetMapping("/{id}/disputes")
    public ResponseEntity<List<Map<String, Object>>> disputes(@PathVariable UUID id) {
        return ResponseEntity.ok(governanceService.listDisputes(id).stream()
                .map(this::toDisputeView).toList());
    }

    @PostMapping("/{id}/disputes")
    public ResponseEntity<Map<String, Object>> openDispute(@PathVariable UUID id,
                                                            @RequestBody DisputeRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.status(201).body(toDisputeView(
                governanceService.openDispute(id, request.subject(), request.description(), actor)));
    }

    @PatchMapping("/disputes/{disputeId}")
    public ResponseEntity<Map<String, Object>> updateDispute(@PathVariable UUID disputeId,
                                                              @RequestBody DisputeUpdateRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        TenantDispute.DisputeStatus status = request.status() == null || request.status().isBlank()
                ? null
                : TenantDispute.DisputeStatus.valueOf(request.status().trim().toUpperCase());
        return ResponseEntity.ok(toDisputeView(
                governanceService.updateDispute(disputeId, status, request.resolution(), actor)));
    }

    // ======================== VIEWS ========================

    private String reasonOf(ReasonRequest request) {
        return request == null ? null : request.reason();
    }

    private Map<String, Object> toView(Tenant tenant) {
        return Map.of(
                "id", tenant.getId(),
                "name", tenant.getName(),
                "slug", tenant.getSlug(),
                "status", tenant.getStatus().name());
    }

    private Map<String, Object> toWarningView(TenantWarning w) {
        return Map.of(
                "id", w.getId(),
                "tenantId", w.getTenantId(),
                "message", w.getMessage(),
                "severity", w.getSeverity().name(),
                "createdAt", w.getCreatedAt());
    }

    private Map<String, Object> toDisputeView(TenantDispute d) {
        java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", d.getId());
        view.put("tenantId", d.getTenantId());
        view.put("subject", d.getSubject());
        view.put("description", d.getDescription());
        view.put("status", d.getStatus().name());
        view.put("resolution", d.getResolution());
        view.put("createdAt", d.getCreatedAt());
        view.put("closedAt", d.getClosedAt());
        return view;
    }
}

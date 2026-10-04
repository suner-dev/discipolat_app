package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantDispute;
import com.discipolat.modules.tenants.domain.TenantGovernanceService;
import com.discipolat.modules.tenants.domain.TenantStatus;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.tenants.domain.TenantWarning;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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

    /**
     * Liste paginée des églises de la plateforme.
     *
     * <p><b>F27.</b> La version initiale renvoyait <b>toute</b> la liste et ne
     * fournissait ni le {@code plan} (alors que l'IHM l'affichait — rendu
     * {@code slug · undefined}), ni les compteurs. Le tableau de gouvernance
     * doit rester operable sur une plateforme de plusieurs centaines
     * d'églises : sans pagination, le navigateur reçoit et rend tout.
     *
     * <p><b>D7 — agrégats seulement.</b> Aucun email, aucun nom de membre : ces
     * données passent par l'impersonation journalisée.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);

        List<TenantResponse> all = tenantService.list();
        List<TenantResponse> filtered = all.stream()
                .filter(t -> status == null || status.isBlank()
                        || t.status().name().equalsIgnoreCase(status.trim()))
                .filter(t -> search == null || search.isBlank()
                        || (t.name() != null && t.name().toLowerCase().contains(search.trim().toLowerCase()))
                        || (t.slug() != null && t.slug().toLowerCase().contains(search.trim().toLowerCase())))
                .toList();

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());

        // `TenantResponse` porte déjà le plan (champ `plan`) : on le lit
        // directement, sans lecture supplémentaire par tenant.
        List<Map<String, Object>> items = filtered.subList(from, to).stream()
                .map(t -> tenantSummary(t.id(), t.name(), t.slug(), t.status(), t.plan()))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", items);
        body.put("total", filtered.size());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", (int) Math.ceil(filtered.size() / (double) safeSize));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Map<String, Object>> block(@PathVariable UUID id,
                                                     @RequestBody(required = false) ReasonRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(governanceService.block(id, reasonOf(request), actor)));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Map<String, Object>> unblock(@PathVariable UUID id,
                                                       @RequestBody(required = false) ReasonRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(governanceService.unblock(id, reasonOf(request), actor)));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Map<String, Object>> ban(@PathVariable UUID id,
                                                    @RequestBody(required = false) ReasonRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(governanceService.ban(id, reasonOf(request), actor)));
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<Map<String, Object>> unban(@PathVariable UUID id,
                                                      @RequestBody(required = false) ReasonRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        return ResponseEntity.ok(toView(governanceService.unban(id, reasonOf(request), actor)));
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
        return tenantSummary(tenant.getId(), tenant.getName(), tenant.getSlug(),
                tenant.getStatus(), tenant.getPlan());
    }

    /**
     * Vignette d'une église pour la console plateforme.
     *
     * <p><b>F27.</b> Le {@code plan} manquait alors que l'IHM de gouvernance
     * l'affichait — l'écran rendait « slug · undefined ». Ajouté ici, sans
     * aucune donnée nominative (D7 : agrégats seulement).
     */
    private Map<String, Object> tenantSummary(UUID id, String name, String slug,
                                              TenantStatus status, String plan) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("name", name);
        view.put("slug", slug);
        view.put("status", status.name());
        view.put("plan", plan == null ? "" : plan);
        return view;
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

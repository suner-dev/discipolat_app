package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantJoinRequest;
import com.discipolat.modules.tenants.domain.TenantJoinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — rejointure côté membre et file d'approbation
 * côté admin tenant.
 */
/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — rejointure côté membre et file d'approbation
 * côté admin tenant.
 *
 * <p><b>Gardes d'autorisation (F10, SPEC §7.0 / T-B0).</b> Les endpoints de
 * file d'adhésion étaient en {@code hasAnyRole('TENANT_OWNER','TENANT_ADMIN')},
 * expression inatteignable puisque le JWT ne porte jamais ces autorités
 * (cf. {@code JwtAuthenticationFilter:59-64}). Elles basculent sur
 * {@code @authz.isTenantAdmin()} : membership ACTIVE de portée {@code TENANT}
 * <b>pour le tenant courant</b> — ce qui corrige au passage la fuite
 * inter-tenant décrite dans {@code InvitationController.java:38-47}.
 */
@RestController
@RequestMapping("/api/v1/tenant")
public class TenantJoinController {

    private final TenantJoinService tenantJoinService;

    public TenantJoinController(TenantJoinService tenantJoinService) {
        this.tenantJoinService = tenantJoinService;
    }

    public record JoinRequest(String code, String slug) {
    }

    /**
     * Un compte connecté rejoint l'église du code (OPEN direct, APPROVAL → demande).
     *
     * <p><b>T-B0bis (F7)</b> : quand l'adhésion est directe, la réponse porte
     * <b>une paire de jetons réémis</b> sur l'organisation rejointe. Sans eux, le
     * claim {@code tenantId} du jeton courant continuerait de désigner
     * l'ancienne église et le client y atterrirait malgré un message
     * « JOINED ». Ils sont absents lorsqu'aucune bascule n'a eu lieu (demande en
     * attente d'approbation).
     */
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> join(@RequestBody JoinRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        boolean hasCode = request.code() != null && !request.code().isBlank();
        boolean hasSlug = request.slug() != null && !request.slug().isBlank();
        if (!hasCode && !hasSlug) {
            throw new IllegalArgumentException("code ou slug requis");
        }
        TenantJoinService.JoinOutcome outcome = hasCode
                ? tenantJoinService.join(userId, request.code())
                : tenantJoinService.joinBySlug(userId, request.slug());
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("status", outcome.status());
        body.put("tenantName", outcome.tenantName() == null ? "" : outcome.tenantName());
        body.put("orgNodeLabel", outcome.orgNodeLabel() == null ? "" : outcome.orgNodeLabel());
        if (outcome.hasSession()) {
            body.put("accessToken", outcome.accessToken());
            body.put("refreshToken", outcome.refreshToken() == null ? "" : outcome.refreshToken());
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/join-requests")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<Map<String, Object>>> pendingRequests() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> body = tenantJoinService.pendingRequests(tenantId).stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "code", r.getCode(),
                        "email", r.getEmail() == null ? "" : r.getEmail(),
                        "userId", r.getUserId() == null ? "" : r.getUserId().toString(),
                        "status", r.getStatus().name(),
                        "createdAt", r.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/join-requests/{id}/approve")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        TenantJoinService.JoinOutcome outcome = tenantJoinService.approveRequest(id, tenantId, actor);
        return ResponseEntity.ok(Map.of("status", outcome.status()));
    }

    @PostMapping("/join-requests/{id}/reject")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Void> reject(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        tenantJoinService.rejectRequest(id, tenantId, actor);
        return ResponseEntity.noContent().build();
    }
}

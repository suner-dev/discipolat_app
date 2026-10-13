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

    /**
     * Demandes d'adhésion.
     *
     * <p>{@code ?status=} : absent ou {@code PENDING} = l'historique
     * (comportement inchangé), {@code APPROVED} = les demandes validées dont
     * <b>personne n'a encore ouvert le lien</b>, {@code ALL} = tout.
     *
     * <p><b>Pourquoi ce filtre existe.</b> Une demande approuvée sans compte ne
     * crée aucun membre : elle émet un lien d'activation valable 7 jours. Passé
     * ce délai la personne est coincée, et le responsable doit pouvoir la
     * retrouver pour lui renvoyer le lien. Or la liste ne renvoyait que les
     * PENDING : après approbation, la demande disparaissait de l'écran et
     * personne ne pouvait plus rien faire pour elle.
     */
    @GetMapping("/join-requests")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<Map<String, Object>>> pendingRequests(
            @RequestParam(required = false) String status) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantJoinRequest.Status filter = parseStatusFilter(status);
        List<Map<String, Object>> body = tenantJoinService.joinRequests(tenantId, filter).stream()
                .map(r -> {
                    Map<String, Object> view = new java.util.LinkedHashMap<>();
                    view.put("id", r.getId());
                    view.put("code", r.getCode());
                    view.put("email", r.getEmail() == null ? "" : r.getEmail());
                    view.put("userId", r.getUserId() == null ? "" : r.getUserId().toString());
                    view.put("status", r.getStatus().name());
                    view.put("createdAt", r.getCreatedAt());
                    // V253 : l'écran doit pouvoir dire si le lien est parti.
                    // Une chaîne vide = aucune invitation émise, jamais `null`
                    // (le front distingue l'absence d'une valeur vide).
                    view.put("invitationId", r.getInvitationId() == null ? "" : r.getInvitationId().toString());
                    view.put("invitationSentAt", r.getInvitationSentAt() == null ? "" : r.getInvitationSentAt().toString());
                    view.put("resendable", tenantJoinService.isResendable(r));
                    return view;
                })
                .toList();
        return ResponseEntity.ok(body);
    }

    /**
     * Un filtre inconnu vaut {@code PENDING} : l'écran d'administration ne doit
     * jamais se retrouver vide à cause d'un paramètre mal orthographié — un
     * responsable y lirait qu'il n'a aucune demande.
     */
    private static TenantJoinRequest.Status parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank() || "PENDING".equalsIgnoreCase(raw.trim())) {
            return TenantJoinRequest.Status.PENDING;
        }
        if ("ALL".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        for (TenantJoinRequest.Status candidate : TenantJoinRequest.Status.values()) {
            if (candidate.name().equalsIgnoreCase(raw.trim())) {
                return candidate;
            }
        }
        return TenantJoinRequest.Status.PENDING;
    }

    @PostMapping("/join-requests/{id}/approve")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        TenantJoinService.ApprovalOutcome outcome = tenantJoinService.approveRequest(id, tenantId, actor);
        return ResponseEntity.ok(approvalView(outcome));
    }

    /**
     * Renvoyer le lien d'activation d'une demande DÉJÀ approuvée.
     *
     * <p>Le token étant stocké haché, il n'est pas rejouable : on en génère un
     * nouveau. C'est la seule sortie pour la personne dont le lien a expiré, et
     * la seule façon pour l'église de ne pas laisser une adhésion qu'elle a
     * validée devenir inaccessible au bout de 7 jours.
     */
    @PostMapping("/join-requests/{id}/resend-invitation")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<Map<String, Object>> resendInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actor = TenantContext.getCurrentUserId();
        TenantJoinService.ApprovalOutcome outcome =
                tenantJoinService.resendInvitationForRequest(id, tenantId, actor);
        return ResponseEntity.ok(approvalView(outcome));
    }

    /**
     * Vue d'une approbation. Elle expose {@code emailSent} et l'échéance :
     * afficher « Demande approuvée » alors que le mail est parti dans le vide
     * serait un mensonge de plus — et le responsable n'aurait aucun moyen de
     * savoir qu'il doit relancer.
     */
    private static Map<String, Object> approvalView(TenantJoinService.ApprovalOutcome outcome) {
        Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("status", outcome.status());
        view.put("tenantName", outcome.tenantName() == null ? "" : outcome.tenantName());
        view.put("applicantEmail", outcome.applicantEmail() == null ? "" : outcome.applicantEmail());
        view.put("emailSent", outcome.emailSent());
        view.put("invitationExpiresAt", outcome.invitationExpiresAt() == null
                ? "" : outcome.invitationExpiresAt().toString());
        if (outcome.hasSession()) {
            view.put("accessToken", outcome.accessToken());
            view.put("refreshToken", outcome.refreshToken() == null ? "" : outcome.refreshToken());
        }
        return view;
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

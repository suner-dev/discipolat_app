package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantKind;
import com.discipolat.modules.tenants.domain.TenantOrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Réseau d'organisations d'une église — SPEC_ORGANISATION_DENOMINATION_V2 §6.
 *
 * <p><b>Garde d'accès (F10).</b> {@code hasAnyRole('TENANT_OWNER','TENANT_ADMIN')}
 * est inatteignable — le JWT ne porte que le rôle legacy. On utilise le garde
 * scopé tenant {@code @authz.isTenantAdmin()}, qui lit
 * {@code tenant_memberships} pour le tenant courant.
 *
 * <p><b>D7 — agrégats seulement.</b> Aucun email, aucun nom de membre dans ces
 * réponses : voir le contenu d'une église relève de l'impersonation
 * journalisée.
 */
@RestController
@RequestMapping("/api/v1/tenant/organization")
@PreAuthorize("@authz.isTenantAdmin()")
public class TenantOrganizationController {

    private final TenantOrganizationService organizationService;

    public TenantOrganizationController(TenantOrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    public record CreateDenominationRequest(String name, String kind, String plan) {
    }

    public record CreateSubChurchRequest(String name, String mode, String plan) {
    }

    /** Vue réseau de l'organisation courante : elle, ses enfants, sa racine. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> current() {
        UUID tenantId = TenantContext.requireTenantId();
        TenantOrganizationService.OrganizationView view = organizationService.view(tenantId);
        Map<String, Object> body = toMap(view);
        body.put("children", organizationService.childrenOf(tenantId).stream()
                .map(this::toMap)
                .toList());
        return ResponseEntity.ok(body);
    }

    /**
     * Toute la descendance de la racine — « la communauté qui accueille
     * plusieurs sous-tenant » du client (§1.3).
     */
    @GetMapping("/network")
    public ResponseEntity<List<Map<String, Object>>> network() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(organizationService.networkOf(tenantId).stream()
                .map(this::toMap)
                .toList());
    }

    /**
     * Crée une organisation <b>enfant autonome</b> (mode AUTONOME, §1.3) :
     * un vrai tenant distinct, avec son stock de membres et sa facturation.
     *
     * <p>Le mode LÉGER (sous-église = simple nœud) passe par la gestion des
     * codes d'entrée, qui accepte déjà un {@code orgNodeId} : créer un tenant
     * pour un simple campus serait plus coûteux que nécessaire.
     */
    @PostMapping("/sub-churches")
    public ResponseEntity<Map<String, Object>> createSubChurch(
            @RequestBody CreateSubChurchRequest request) {
        UUID actor = TenantContext.getCurrentUserId();
        UUID parentId = TenantContext.requireTenantId();
        String mode = request.mode() == null ? "autonomous" : request.mode().trim().toLowerCase();
        if (!"autonomous".equals(mode)) {
            // Le mode léger n'a pas sa place ici : il ne crée aucun tenant.
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SUB_CHURCH_MODE_NOT_SUPPORTED",
                    "message", "Le mode 'light' ne crée pas d'organisation : "
                            + "utilisez un code d'entrée rattaché à un nœud (POST /tenant/join-codes)"));
        }
        var created = organizationService.createChildChurch(
                parentId, request.name(), actor, request.plan());
        return ResponseEntity.status(201).body(toMap(organizationService.view(created.getId())));
    }

    private Map<String, Object> toMap(TenantOrganizationService.OrganizationView view) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", view.id());
        body.put("name", view.name());
        body.put("slug", view.slug());
        body.put("kind", view.kind());
        body.put("parentTenantId", view.parentTenantId());
        body.put("rootTenantId", view.rootTenantId());
        body.put("status", view.status());
        body.put("plan", view.plan());
        body.put("childCount", view.childCount());
        // Le lien d'invitation est un CONTRAT de la spec (§4.1) : c'est lui que
        // l'administrateur partage, pas le code à dicter.
        body.put("invitePath", view.slug() == null ? null : "/j/" + view.slug());
        return body;
    }
}

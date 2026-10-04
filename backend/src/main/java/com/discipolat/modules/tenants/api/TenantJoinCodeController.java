package com.discipolat.modules.tenants.api;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.JoinCodeService;
import com.discipolat.modules.tenants.domain.JoinMode;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.TenantJoinCode;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — gestion des codes de rejointure par
 * l'admin du tenant (église principale et sous-églises D3).
 *
 * <p><b>Garde d'autorisation (F10, SPEC §7.0 / T-B0).</b> Elle était
 * {@code hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')}, expression
 * <b>inatteignable</b> : {@code JwtAuthenticationFilter} ne construit les
 * autorités Spring qu'à partir du claim {@code role} du JWT, or ce claim ne
 * contient qu'un {@code UserRole} legacy ({@code ADMIN}, {@code PASTEUR},
 * {@code RESPONSABLE}, {@code CHEF_DE_FAMILLE}, {@code FAISEUR}, {@code MEMBRE}).
 * Un fondateur d'église — dont le rôle legacy est {@code ADMIN} et dont
 * l'appartenance est {@code TENANT_OWNER} — obtenait donc un <b>403</b> sur
 * cette page.
 *
 * <p>Le motif est déjà documenté comme fautif dans
 * {@code InvitationController.java:38-47} (fuite d'autorité inter-tenant,
 * SKIP E2E-9/E2E-10b) : l'expression n'évalue que l'autorité du JWT et ignore
 * le tenant réellement visé. On bascule sur le garde scopé tenant
 * {@code @authz.isTenantAdmin()}, qui lit {@code tenant_memberships}.
 */
@RestController
@RequestMapping("/api/v1/tenant/join-codes")
@PreAuthorize("@authz.isTenantAdmin()")
public class TenantJoinCodeController {

    private final JoinCodeService joinCodeService;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantJoinCodeController(JoinCodeService joinCodeService,
                                    OrganizationNodeRepository organizationNodeRepository,
                                    TenantRepository tenantRepository,
                                    AuditService auditService) {
        this.joinCodeService = joinCodeService;
        this.organizationNodeRepository = organizationNodeRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    public record CreateJoinCodeRequest(UUID orgNodeId, String label, String joinMode) {
    }

    /**
     * Sous-églises rattachables à un code — alimente le sélecteur de
     * l'écran « Codes d'entrée » (F14).
     *
     * <p>Sans cette liste, l'administrateur ne peut pas produire le code d'un
     * campus : le backend savait le faire, l'écran ne le pouvait pas.
     */
    public record SubChurchOption(UUID id, String name, String type, boolean hasActiveCode) {
    }

    @GetMapping("/sub-churches")
    public ResponseEntity<List<SubChurchOption>> subChurches() {
        UUID tenantId = TenantContext.requireTenantId();
        List<TenantJoinCode> activeCodes = joinCodeService.listActiveForTenant(tenantId);
        List<SubChurchOption> options = organizationNodeRepository
                .findByTenantIdOrderByNameAsc(tenantId).stream()
                .filter(node -> node.getType() != OrganizationNodeType.ROOT_CHURCH)
                .map(node -> new SubChurchOption(
                        node.getId(),
                        node.getName() != null && !node.getName().isBlank() ? node.getName() : node.getCode(),
                        node.getType().name(),
                        activeCodes.stream()
                                .anyMatch(c -> node.getId().equals(c.getOrgNodeId()))))
                .toList();
        return ResponseEntity.ok(options);
    }

    public record UpdateJoinCodeRequest(String label, String joinMode, Boolean isActive) {
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Map<String, Object>> body = joinCodeService.listForTenant(tenantId).stream()
                .map(this::toView)
                .toList();
        return ResponseEntity.ok(body);
    }

/**
     * Crée un code d'entrée — pour l'église racine ou une sous-église (F14).
     *
     * <p>{@code orgNodeId} absent = code principal de l'église. Présent = code
     * d'un campus ou d'une sous-église <b>de cette organisation</b> : le nœud
     * est validé comme appartenant au tenant courant, sinon un administrateur
     * pourrait rattacher son code au nœud d'une autre église et y faire
     * adhérer des membres hors de son périmètre.
     *
     * <p><b>F16.</b> Un nouveau code racine neutralise le précédent
     * ({@code JoinCodeService.generate}) : c'est l'invariant « un code actif
     * par cible » (D9), sans lequel la résolution publique par slug levait une
     * exception.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateJoinCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getCurrentUserId();
        JoinMode mode = parseMode(request.joinMode());
        UUID orgNodeId = requireNodeOfThisTenant(request.orgNodeId());
        TenantJoinCode code = joinCodeService.generate(
                tenantId, orgNodeId, normalizeLabel(request.label()), mode, userId);
        auditService.logSimple("JOIN_CODE_CREATED", "TENANT_JOIN_CODE", code.getId());
        return ResponseEntity.status(201).body(toView(code));
    }

    /**
     * Le nœud visé doit appartenir à CETTE organisation.
     *
     * <p>Sans cette vérification, un administrateur d'église A pouvait créer un
     * code rattaché à un nœud d'église B : les membres qui l'utiliseraient
     * seraient scopés dans une hiérarchie qui n'est pas la leur — un problème
     * de <b>cohérence de périmètre</b>, pas seulement de confort.
     */
    private UUID requireNodeOfThisTenant(UUID orgNodeId) {
        if (orgNodeId == null) {
            return null;
        }
        OrganizationNode node = organizationNodeRepository.findById(orgNodeId)
                .orElseThrow(() -> new DomainException("Sous-église introuvable",
                        HttpStatus.NOT_FOUND, "ORG_NODE_NOT_FOUND"));
        if (!node.getTenantId().equals(TenantContext.requireTenantId())) {
            throw new DomainException("Cette sous-église n'appartient pas à votre église",
                    HttpStatus.FORBIDDEN, "ORG_NODE_OTHER_TENANT");
        }
        return orgNodeId;
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<Map<String, Object>> rotate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getCurrentUserId();
        TenantJoinCode fresh = joinCodeService.rotate(id, tenantId, userId);
        auditService.logSimple("JOIN_CODE_ROTATED", "TENANT_JOIN_CODE", fresh.getId());
        return ResponseEntity.ok(toView(fresh));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody UpdateJoinCodeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantJoinCode updated = joinCodeService.update(id, tenantId,
                normalizeLabel(request.label()),
                request.joinMode() == null ? null : parseMode(request.joinMode()),
                request.isActive());
        auditService.logSimple("JOIN_CODE_UPDATED", "TENANT_JOIN_CODE", updated.getId());
        return ResponseEntity.ok(toView(updated));
    }

    /** Suppression douce : le code devient injoignable, l'historique reste. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        joinCodeService.deactivate(id, tenantId);
        auditService.logSimple("JOIN_CODE_DEACTIVATED", "TENANT_JOIN_CODE", id);
        return ResponseEntity.noContent().build();
    }

    private JoinMode parseMode(String raw) {
        if (raw == null || raw.isBlank()) return JoinMode.OPEN;
        try {
            return JoinMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("joinMode invalide : " + raw);
        }
    }

    private String normalizeLabel(String label) {
        return label == null || label.isBlank() ? null : label.trim();
    }

/**
     * Vue d'un code pour la console d'église.
     *
     * <p><b>F21.</b> Le {@code tenantSlug} est renvoyé : sans lui, le front ne
     * peut pas construire le lien d'invitation {@code /j/<slug>} — le founder
     * n'avait donc que le code à dicter, alors que la spec (§4.1) fait du
     * LIEN la porte principale.
     */
    private Map<String, Object> toView(TenantJoinCode code) {
        Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("id", code.getId());
        view.put("code", code.getCode());
        view.put("label", code.getLabel());
        view.put("joinMode", code.getJoinMode().name());
        view.put("isActive", code.isActive());
        view.put("orgNodeId", code.getOrgNodeId());
        view.put("createdAt", code.getCreatedAt());
        view.put("rotatedAt", code.getRotatedAt());
        view.put("tenantSlug", tenantRepository.findById(code.getTenantId())
                .map(com.discipolat.modules.tenants.domain.Tenant::getSlug)
                .orElse(null));
        return view;
    }
}

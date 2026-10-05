package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.OrganizationLevelRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantDispute;
import com.discipolat.modules.tenants.domain.TenantGovernanceService;
import com.discipolat.modules.tenants.domain.TenantKind;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.tenants.domain.TenantStatus;
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
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final OrganizationLevelRepository levelRepository;

    public PlatformTenantGovernanceController(TenantGovernanceService governanceService,
                                              TenantService tenantService,
                                              TenantRepository tenantRepository,
                                              TenantMembershipRepository membershipRepository,
                                              OrganizationLevelRepository levelRepository) {
        this.governanceService = governanceService;
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.levelRepository = levelRepository;
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
        //
        // T-B2 (SPEC_ORGANISATION_DENOMINATION_V2) : la console doit pouvoir
        // distinguer une dénomination d'une église et compter son réseau.
        // `childCount` et `memberCount` sont des AGRÉGATS (D7) — jamais une
        // liste de membres, jamais un email.
        List<Map<String, Object>> items = filtered.subList(from, to).stream()
                .map(t -> tenantSummary(t))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", items);
        body.put("total", filtered.size());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", (int) Math.ceil(filtered.size() / (double) safeSize));
        return ResponseEntity.ok(body);
    }

    /**
     * Détail d'une organisation + son réseau.
     *
     * <p><b>T-B2.</b> La console doit répondre à « cette dénomination contient
     * quelles églises, et combien de membres par église ? » sans jamais exposer
     * une identité (D7) : uniquement des agrégats et des métadonnées de
     * structure.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new com.discipolat.common.domain.BusinessRuleException(
                        "Église introuvable", "TENANT_NOT_FOUND"));

        TenantResponse response = TenantResponse.from(tenant);
        Map<String, Object> body = new LinkedHashMap<>(tenantSummary(response));
        body.put("country", tenant.getCountry());
        body.put("createdAt", tenant.getCreatedAt());

        // Descendance (enfants directs + réseau complet sous la même racine).
        List<Map<String, Object>> children = tenantRepository
                .findByParentTenantIdOrderByNameAsc(id).stream()
                .map(TenantResponse::from)
                .map(this::tenantSummary)
                .toList();
        body.put("children", children);

        List<Map<String, Object>> network = tenantRepository
                .findByRootTenantId(response.rootTenantId()).stream()
                .filter(t -> !t.getId().equals(response.id()))
                .map(TenantResponse::from)
                .map(this::tenantSummary)
                .toList();
        body.put("network", network);
        body.put("networkSize", network.size());

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
        return tenantSummary(TenantResponse.from(tenant));
    }

    /**
     * Vignette d'une église pour la console plateforme.
     *
     * <p><b>F27.</b> Le {@code plan} manquait alors que l'IHM de gouvernance
     * l'affichait — l'écran rendait « slug · undefined ». Ajouté ici, sans
     * aucune donnée nominative (D7 : agrégats seulement).
     *
     * <p><b>T-B2 — modèle d'organisation.</b> Expose la nature
     * ({@code kind}), le rattachement ({@code parentTenantId}), la racine
     * ({@code rootTenantId}) et deux compteurs : le nombre d'organisations
     * rattachées à la même racine ({@code childCount}) et le nombre de membres
     * actifs ({@code memberCount}).
     *
     * <p><b>D7 — agrégats seulement.</b> {@code memberCount} est un {@code COUNT} :
     * aucune identité, aucun email, aucun nom de membre ne franchit cette
     * frontière. Passer par une impersonation journalisée pour le nominatif.
     */
    private Map<String, Object> tenantSummary(TenantResponse t) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", t.id());
        view.put("name", t.name());
        view.put("slug", t.slug());
        view.put("status", t.status().name());
        view.put("plan", t.plan() == null ? "" : t.plan());

        // Modèle d'organisation (V222).
        view.put("kind", t.kind() == null ? TenantKind.CHURCH.name() : t.kind().name());
        view.put("parentTenantId", t.parentTenantId());
        view.put("rootTenantId", t.rootTenantId());
        view.put("isNetworkRoot", t.id() != null && t.id().equals(t.rootTenantId()));
        view.put("childCount", childCountOf(t));
        view.put("memberCount", memberCountOf(t.id()));
        // §6.2 V3 — « niveaux personnalisés » : COUNT structurel des niveaux de la
        // racine (dénomination). Un nombre uniquement — jamais de libellé de
        // niveau ni de PII (D7).
        view.put("customLevelCount", customLevelCountOf(t));
        return view;
    }

    /**
     * Nombre de niveaux configurés portés par la racine de {@code t} (V3-A).
     * Un simple {@code COUNT} scopé par racine : la console plateforme ne voit
     * jamais les libellés de niveaux, uniquement leur nombre (D7).
     */
    private long customLevelCountOf(TenantResponse t) {
        UUID root = t.rootTenantId() != null ? t.rootTenantId() : t.id();
        if (root == null) {
            return 0L;
        }
        return levelRepository.countByRootTenantId(root);
    }

    /**
     * Nombre d'organisations rattachées à la racine de {@code t} (la racine
     * elle-même exclue). Les organisations isolées valent 0.
     */
    private long childCountOf(TenantResponse t) {
        UUID root = t.rootTenantId();
        if (root == null || t.id() == null) {
            return 0L;
        }
        // `countByRootTenantId` inclut la racine : on la retire.
        long total = tenantRepository.countByRootTenantId(root);
        return total <= 0 ? 0L : total - 1;
    }

    /**
     * Nombre de membres ACTIFS d'une organisation — un simple COUNT.
     *
     * <p>Isolé dans sa propre méthode pour que la frontière de sécurité (D7)
     * soit explicite et vérifiable en un seul endroit du code.
     */
    private long memberCountOf(UUID tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        return membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
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

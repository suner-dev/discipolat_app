package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.security.domain.RefreshTokenSessionService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenant-switcher")
public class TenantSwitcherController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final OrganizationNodeService orgNodeService;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;
    private final TenantService tenantService;
    private final TenantStatusGuard tenantStatusGuard;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionService refreshTokenSessionService;
    /** Lecture cross-tenant déclarée : voir {@link CrossTenantScopeAccess} (constat H4). */
    private final CrossTenantScopeAccess crossTenantRead;

    public TenantSwitcherController(TenantRepository tenantRepository,
                                    TenantMembershipRepository membershipRepository,
                                    UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PermissionRepository permissionRepository,
                                    OrganizationNodeRepository orgNodeRepository,
                                    OrganizationNodeService orgNodeService,
                                    TenantSubscriptionRepository subscriptionRepository,
                                      SaasPlanRepository planRepository,
                                      TenantService tenantService,
                                      TenantStatusGuard tenantStatusGuard,
                                      JwtTokenProvider jwtTokenProvider,
                                      RefreshTokenSessionService refreshTokenSessionService,
                                      CrossTenantScopeAccess crossTenantRead) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.orgNodeService = orgNodeService;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.tenantService = tenantService;
        this.tenantStatusGuard = tenantStatusGuard;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenSessionService = refreshTokenSessionService;
        this.crossTenantRead = crossTenantRead;
    }

    // ==================== CONTEXTE COURANT ====================

    @GetMapping("/context")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCurrentContext() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // User has multiple tenants - return available tenants for selection.
            // H4 : la liste doit traverser le filtre multi-tenant, mais elle reste
            // bornée aux memberships de CET utilisateur (userId vient du principal
            // authentifié, jamais du client).
            List<TenantMembership> memberships = crossTenantRead.call(
                    () -> membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE));
            List<Map<String, Object>> availableTenants = memberships.stream()
                    .map(m -> {
                        Optional<Tenant> tenant = tenantRepository.findById(m.getTenantId());
                        return tenant.map(t -> MembershipView.of(m, t, false)).orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "requiresSelection", true,
                    "availableTenants", availableTenants,
                    "userId", userId.toString()
            ));
        }

        // Single tenant context - return full context
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);

        // Get active membership (the one matching current scope)
        TenantMembership activeMembership = memberships.stream()
                .filter(m -> m.getScopeType() == MembershipScopeType.TENANT || m.getScopeId() == null)
                .findFirst()
                .orElse(memberships.get(0));

        // Get roles and permissions for this membership
        Set<String> permissions = new HashSet<>();
        if (activeMembership.getRole() != null) {
            permissions = permissionRepository.findByRoleId(activeMembership.getRole().getId())
                    .stream().map(Permission::getKey).collect(Collectors.toSet());
        }

        // Get organization nodes user has access to
        List<OrganizationNode> accessibleNodes = getAccessibleNodes(userId, tenantId, activeMembership);

        // Get subscription
        Optional<TenantSubscription> subscription = subscriptionRepository.findCurrentByTenantId(tenantId);
        Optional<SaasPlan> plan = subscription.flatMap(s -> planRepository.findByKeyIgnoreCase(s.getPlanKey()));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userId", userId.toString());
        context.put("tenantId", tenantId.toString());
        context.put("tenantName", tenant.getName());
        context.put("tenantSlug", tenant.getSlug());
        context.put("tenantStatus", tenant.getStatus().name());
        context.put("plan", tenant.getPlan());
        context.put("role", activeMembership.getRole() != null ? activeMembership.getRole().getKey() : "UNKNOWN");
        context.put("scopeType", activeMembership.getScopeType().name());
        context.put("scopeId", activeMembership.getScopeId() != null ? activeMembership.getScopeId().toString() : null);
        context.put("permissions", permissions);
        context.put("accessibleNodes", accessibleNodes.stream().map(n -> Map.of(
                "id", n.getId().toString(),
                "name", n.getName(),
                "type", n.getType().name(),
                "path", n.getPath()
        )).toList());
        context.put("subscription", subscription.map(s -> Map.of(
                "planKey", s.getPlanKey(),
                "status", s.getStatus().name(),
                "currentPeriodEnd", s.getCurrentPeriodEnd(),
                "limits", s.getQuotasJson(),
                "plan", plan.map(p -> Map.of(
                        "name", p.getName(),
                        "features", p.getFeaturesJson()
                )).orElse(null)
        )).orElse(null));
        context.put("branding", parseJson(tenant.getBrandingJson()));
        context.put("features", parseJson(tenant.getFeaturesJson()));
        context.put("settings", parseJson(tenant.getSettingsJson()));

        return ResponseEntity.ok(context);
    }

    // ==================== MES TENANTS ====================

    @GetMapping("/my-tenants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getMyTenants() {
        UUID userId = SecurityUtils.getCurrentUserId();
        // H4 : lister les églises d'un utilisateur impose de traverser le filtre
        // multi-tenant, qui sinon ne renverrait que l'église courante. La requête
        // reste paramétrée par l'identifiant de l'utilisateur authentifié.
        List<TenantMembership> memberships = crossTenantRead.call(
                () -> membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE));

        List<Map<String, Object>> result = memberships.stream()
                .map(m -> {
                    Optional<Tenant> tenant = tenantRepository.findById(m.getTenantId());
                    return tenant.map(t -> MembershipView.of(m, t, true)).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(result);
    }

    // ==================== SWITCH TENANT ====================

    @PostMapping("/switch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> switchTenant(@RequestBody Map<String, String> request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        String tenantIdStr = request.get("tenantId");

        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "tenantId requis"));
        }

        UUID newTenantId = UUID.fromString(tenantIdStr);

        // Validate user has access to this tenant.
        // H4 : ce controle doit PAR TOI-MEME traverser le filtre multi-tenant —
        // c'est justement lui quisinon rendait la bascule impossible, puisque la
        // requête portait `tenant_id = tenant courant AND tenant_id = tenant
        // demandé`. Borné aux memberships de l'utilisateur authentifié.
        boolean hasAccess = crossTenantRead.call(
                () -> membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, newTenantId, MembershipStatus.ACTIVE));
        if (!hasAccess) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé à ce tenant"));
        }

        // B1 : un tenant suspendu/annule ne doit jamais delivrer de JWT.
        // Controleplace AVANT le changement de contexte et AVANT toute generation
        // de jeton : si le statut est interdit, le ThreadLocal n'est pas pollue et
        // aucun accessToken/refreshToken n'est produit.
        tenantStatusGuard.assertAccessible(newTenantId);

        // H4 (2e cran) : l'IDENTITE de l'utilisateur ne change pas lorsqu'on
        // change d'eglise. Il faut donc le lire AVANT de basculer le contexte :
        // `TenantAwareSimpleJpaRepository.findById` ajoute explicitement
        // `tenant_id = TenantContext.getTenantId()`, et apres la bascule on
        // chercherait un utilisateur dont `users.tenant_id` est encore son tenant
        // d'origine -> "Utilisateur introuvable", 500. Lire avant evite d'avoir a
        // contourner l'isolation pour un acces cross-tenant.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));

        // Set new tenant context
        TenantContext.setTenantId(newTenantId);

        Tenant tenant = tenantRepository.findById(newTenantId).orElseThrow();
        // H4 : le filtre porte encore l'ANCIEN tenant (TenantContext vient d'être
        // changé, le paramètre du filtre, non). Sans cette lecture cross-tenant,
        // la liste serait vide et `get(0)` lèverait un IndexOutOfBounds.
        List<TenantMembership> memberships = crossTenantRead.call(
                () -> membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, newTenantId, MembershipStatus.ACTIVE));
        if (memberships.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé à ce tenant"));
        }
        TenantMembership membership = memberships.get(0);
        String activeRole = user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name();
        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                : Set.of(activeRole);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRole, roles,
                user.isEstChefDeFamille(), newTenantId);
        UUID familyId = UUID.randomUUID();
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRole, roles, newTenantId, familyId);
        refreshTokenSessionService.register(
                refreshToken, user.getId(), familyId, jwtTokenProvider.getTokenExpiration(refreshToken));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("tenantId", newTenantId.toString());
        response.put("tenantName", tenant.getName());
        response.put("role", membership.getRole() != null ? membership.getRole().getKey() : "UNKNOWN");
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("message", "Contexte tenant changé avec succès");
        return ResponseEntity.ok(response);
    }

    // ==================== SWITCH ORGANIZATION NODE ====================

    @PostMapping("/switch-org")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> switchOrganization(@RequestBody Map<String, String> request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        String orgNodeIdStr = request.get("organizationId");

        if (orgNodeIdStr == null || orgNodeIdStr.isBlank()) {
            // Clear org node context
            return ResponseEntity.ok(Map.of("success", true, "message", "Contexte organisationnel réinitialisé"));
        }

        UUID newOrgNodeId = UUID.fromString(orgNodeIdStr);

        // Validate user has access to this org node via their memberships
        List<TenantMembership> memberships = membershipRepository.findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE);
        boolean hasAccess = memberships.stream().anyMatch(m ->
                m.getScopeType() == MembershipScopeType.TENANT ||
                (m.getScopeId() != null && m.getScopeId().equals(newOrgNodeId)) ||
                (m.getScopeId() != null && orgNodeRepository.isDescendantOf(newOrgNodeId, m.getScopeId()))
        );

        if (!hasAccess) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé à ce nœud organisationnel"));
        }

        // Note: In a real implementation, you'd store the active org node in session/context
        // For now, we return the node details for frontend to store
        Optional<OrganizationNode> node = orgNodeRepository.findById(newOrgNodeId);
        if (node.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "organizationNodeId", newOrgNodeId.toString(),
                "organizationNodeName", node.get().getName(),
                "organizationNodeType", node.get().getType().name(),
                "message", "Contexte organisationnel changé"
        ));
    }

    // ==================== REFRESH CONTEXT ====================

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> refreshContext() {
        return getCurrentContext();
    }

    // ==================== HELPERS ====================

    private List<OrganizationNode> getAccessibleNodes(UUID userId, UUID tenantId, TenantMembership membership) {
        if (membership.getScopeType() == MembershipScopeType.TENANT) {
            return orgNodeRepository.findByTenantId(tenantId);
        }
        if (membership.getScopeId() != null) {
            List<OrganizationNode> descendants = orgNodeRepository.findDescendants(tenantId, membership.getScopeId().toString() + ":*");
            Optional<OrganizationNode> self = orgNodeRepository.findById(membership.getScopeId());
            List<OrganizationNode> result = new ArrayList<>(descendants);
            self.ifPresent(result::add);
            return result;
        }
        return List.of();
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Super Admin Controller - Gestion plateforme SaaS
 * Rôle requis : PLATFORM_SUPER_ADMIN
 */
@RestController
@RequestMapping("/api/v1/platform/admin")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final SaasPlanService saasPlanService;
    private final AuditService auditService;

    public SuperAdminController(TenantRepository tenantRepository,
                                TenantMembershipRepository membershipRepository,
                                UserRepository userRepository,
                                TenantService tenantService,
                                SaasPlanService saasPlanService,
                                AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.saasPlanService = saasPlanService;
        this.auditService = auditService;
    }

    /**
     * Dashboard Super Admin - Métriques globales
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatut(com.discipolat.modules.users.domain.UserStatus.ACTIVE);
        long totalMemberships = membershipRepository.count();
        
        List<SaasPlan> plans = saasPlanService.getAllPlans();
        List<TenantSubscription> subscriptions = saasPlanService.getAllSubscriptions();
        
        Map<String, Long> tenantsByPlan = new HashMap<>();
        tenantRepository.findAll().forEach(t -> 
            tenantsByPlan.merge(t.getPlan(), 1L, Long::sum));

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalTenants", totalTenants);
        dashboard.put("activeTenants", activeTenants);
        dashboard.put("suspendedTenants", totalTenants - activeTenants);
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("activeUsers", activeUsers);
        dashboard.put("totalMemberships", totalMemberships);
        dashboard.put("tenantsByPlan", tenantsByPlan);
        dashboard.put("plans", plans.stream().map(p -> Map.of(
            "id", p.getId(),
            "key", p.getKey(),
            "name", p.getName(),
            "priceMonthly", p.getPriceMonthly(),
            "priceYearly", p.getPriceYearly(),
            "usersLimit", p.getUsersLimit(),
            "churchesLimit", p.getChurchesLimit()
        )).collect(Collectors.toList()));
        dashboard.put("subscriptions", subscriptions.stream().map(s -> Map.of(
            "id", s.getId(),
            "tenantId", s.getTenantId(),
            "planKey", s.getPlanKey(),
            "status", s.getStatus().name(),
            "currentPeriodEnd", s.getCurrentPeriodEnd()
        )).collect(Collectors.toList()));
        dashboard.put("generatedAt", Instant.now());
        
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Liste tous les tenants (Super Admin)
     */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<PageResponse<Map<String, Object>>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Tenant> tenants;
        
        if (status != null || plan != null) {
            tenants = tenantRepository.findAll(pageRequest).map(t -> {
                if (status != null && !t.getStatus().name().equalsIgnoreCase(status)) {
                    // filtered - handled via stream below
                }
                return t;
            });
            // Filter manually
            tenants = tenants.getContent().stream()
                .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                .filter(t -> plan == null || t.getPlan().equalsIgnoreCase(plan))
                .collect(Collectors.toList());
            // Convert to Page (simplified)
            List<Map<String, Object>> content = tenants.stream().map(this::toTenantMap).toList();
            return ResponseEntity.ok(PageResponse.of(content, page, size, content.size(), 1));
        }
        
        Page<Tenant> tenantPage = tenantRepository.findAll(pageRequest);
        List<Map<String, Object>> content = tenantPage.getContent().stream()
            .map(this::toTenantMap)
            .toList();
        
        return ResponseEntity.ok(PageResponse.of(content, page, size,
            tenantPage.getTotalElements(), tenantPage.getTotalPages()));
    }

    /**
     * Créer un nouveau tenant
     */
    @PostMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createTenant(
            @RequestBody Map<String, Object> request) {
        
        String name = (String) request.get("name");
        String slug = ((String) request.get("slug")).toLowerCase();
        String plan = request.get("plan") != null ? (String) request.get("plan") : "free";
        String country = request.get("country") != null ? (String) request.get("country") : "CM";
        String currency = request.get("currency") != null ? (String) request.get("currency") : "XAF";
        String timezone = request.get("timezone") != null ? (String) request.get("timezone") : "Africa/Douala";
        String locale = request.get("locale") != null ? (String) request.get("locale") : "fr";
        
        // Vérifier que le slug n'existe pas
        if (tenantRepository.existsBySlug(slug)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ce slug est déjà utilisé",
                "field", "slug"
            ));
        }
        
        // Créer le tenant
        Tenant tenant = Tenant.builder()
                .name(name)
                .slug(slug)
                .status(TenantStatus.ACTIVE)
                .plan(plan)
                .country(country)
                .currency(currency)
                .timezone(timezone)
                .locale(locale)
                .build();
        
        tenant = tenantRepository.save(tenant);
        
        // Créer l'abonnement
        if (!"free".equals(plan)) {
            saasPlanService.subscribe(tenant.getId(), plan, "monthly", null);
        }
        
        // Créer la structure organisationnelle de base
        OrganizationNode root = OrganizationNode.builder()
                .tenantId(tenant.getId())
                .type(OrganizationNodeType.ROOT_CHURCH)
                .name(name)
                .code("ROOT_" + tenant.getId().toString().substring(0, 8))
                .status(OrganizationNodeStatus.ACTIVE)
                .path(tenant.getId().toString() + ".")
                .level(0)
                .build();
        // orgNodeService.save(root); // Décommenter si le service existe
        
        // Logger l'action
        auditService.log(UUID.randomUUID(), tenant.getId(), "TENANT_CREATED",
            "TENANT", tenant.getId(), "SUCCESS", Map.of(
            "name", name, "slug", slug, "plan", plan
        ));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toTenantMap(tenant));
    }

    /**
     * Suspendre un tenant
     */
    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> suspendTenant(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Réactiver un tenant
     */
    @PostMapping("/tenants/{id}/reactivate")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> reactivateTenant(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Archiver un tenant
     */
    @PostMapping("/tenants/{id}/archive")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> archiveTenant(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenant.setStatus(TenantStatus.CANCELLED);
        tenantRepository.save(tenant);
        auditService.log(UUID.randomUUID(), id, "TENANT_ARCHIVED",
            "TENANT", id, "SUCCESS", Map.of("tenantId", id.toString()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Gestion des plans SaaS
     */
    @GetMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listPlans() {
        return ResponseEntity.ok(saasPlanService.getAllPlans().stream()
            .map(p -> Map.of(
                "id", p.getId(),
                "key", p.getKey(),
                "name", p.getName(),
                "description", p.getDescription(),
                "priceMonthly", p.getPriceMonthly(),
                "priceYearly", p.getPriceYearly(),
                "usersLimit", p.getUsersLimit(),
                "churchesLimit", p.getChurchesLimit(),
                "departmentsLimit", p.getDepartmentsLimit(),
                "features", p.getFeatures()
            )).collect(Collectors.toList()));
    }

    /**
     * Créer / Mettre à jour un plan SaaS
     */
    @PostMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createOrUpdatePlan(
            @RequestBody Map<String, Object> request) {
        
        String key = (String) request.get("key");
        SaasPlan existingPlan = saasPlanService.getPlan(key).orElse(null);
        
        SaasPlan plan;
        if (existingPlan != null) {
            // Mettre à jour
            if (request.get("name") != null) existingPlan.setName((String) request.get("name"));
            if (request.get("description") != null) existingPlan.setDescription((String) request.get("description"));
            if (request.get("priceMonthly") != null) existingPlan.setPriceMonthly((Double) request.get("priceMonthly"));
            if (request.get("priceYearly") != null) existingPlan.setPriceYearly((Double) request.get("priceYearly"));
            if (request.get("usersLimit") != null) existingPlan.setUsersLimit(((Number) request.get("usersLimit")).intValue());
            if (request.get("churchesLimit") != null) existingPlan.setChurchesLimit(((Number) request.get("churchesLimit")).intValue());
            existingPlan = saasPlanService.updatePlan(existingPlan);
        } else {
            // Créer
            plan = SaasPlan.builder()
                .key(key.toUpperCase())
                .name(key)
                .description(request.get("description") != null ? (String) request.get("description") : "")
                .priceMonthly(request.get("priceMonthly") != null ? (Double) request.get("priceMonthly") : 0.0)
                .priceYearly(request.get("priceYearly") != null ? (Double) request.get("priceYearly") : 0.0)
                .usersLimit(request.get("usersLimit") != null ? ((Number) request.get("usersLimit")).intValue() : 10)
                .churchesLimit(request.get("churchesLimit") != null ? ((Number) request.get("churchesLimit")).intValue() : 1)
                .build();
            plan = saasPlanService.createPlan(plan);
        }
        
        return ResponseEntity.ok(Map.of(
            "id", plan.getId(),
            "key", plan.getKey(),
            "name", plan.getName(),
            "description", plan.getDescription(),
            "priceMonthly", plan.getPriceMonthly(),
            "priceYearly", plan.getPriceYearly(),
            "usersLimit", plan.getUsersLimit(),
            "churchesLimit", plan.getChurchesLimit()
        ));
    }

    /**
     * Gérer les feature flags globaux
     */
    @GetMapping("/feature-flags")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        // Retourner les feature flags globaux
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("aiEnabled", true);
        flags.put("mobileMoneyEnabled", true);
        flags.put("whatsappEnabled", true);
        flags.put("analyticsEnabled", true);
        flags.put("docsEnabled", true);
        return ResponseEntity.ok(flags);
    }

    /**
     * Définir un feature flag global
     */
    @PutMapping("/feature-flags/{key}")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> setFeatureFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> request) {
        boolean value = request.get("enabled");
        // À implémenter avec un service FeatureFlagService
        return ResponseEntity.ok(Map.of("key", key, "enabled", value));
    }

    /**
     * Impersonation - Démarrer la session d'impersonation
     */
    @PostMapping("/impersonate")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> startImpersonation(
            @RequestBody Map<String, Object> request) {
        
        UUID tenantId = UUID.fromString((String) request.get("tenantId"));
        String reason = (String) request.get("reason");
        
        if (tenantId == null || reason == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "tenantId et reason sont requis"
            ));
        }
        
        // Vérifier que le tenant existe
        tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        // Créer l'entrée d'impersonation
        Instant startTime = Instant.now();
        auditService.log(UUID.randomUUID(), tenantId, "IMPERSONATION_START",
            "TENANT", tenantId, "SUCCESS", Map.of(
            "reason", reason
        ));
        
        // Retourner un token d'impersonation temporaire
        String impersonationToken = UUID.randomUUID().toString();
        
        return ResponseEntity.ok(Map.of(
            "impersonationToken", impersonationToken,
            "tenantId", tenantId.toString(),
            "tenantName", tenantRepository.findById(tenantId).get().getName(),
            "startTime", startTime.toString(),
            "expiresAt", Instant.now().plusSeconds(1800).toString()
        ));
    }

    /**
     * Impersonation - Arrêter la session
     */
    @PostMapping("/impersonate/stop")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> stopImpersonation() {
        auditService.log(UUID.randomUUID(), null, "IMPERSONATION_END",
            "PLATFORM", null, "SUCCESS", Map.of());
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtenir les détails d'un tenant spécifique
     */
    @GetMapping("/tenants/{id}")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTenantDetails(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Optional<TenantSubscription> subscription =
            saasPlanService.getTenantSubscription(id);
        
        long memberCount = membershipRepository.countByTenantId(id);
        
        Map<String, Object> details = new LinkedHashMap<>();
        details.putAll(toTenantMap(tenant));
        details.put("memberCount", memberCount);
        details.put("subscription", subscription.map(s -> Map.of(
            "planKey", s.getPlanKey(),
            "status", s.getStatus().name(),
            "currentPeriodEnd", s.getCurrentPeriodEnd(),
            "cancelAtPeriodEnd", s.getCancelAtPeriodEnd()
        )).orElse(null));
        
        return ResponseEntity.ok(details);
    }

    private Map<String, Object> toTenantMap(Tenant tenant) {
        return Map.of(
            "id", tenant.getId().toString(),
            "name", tenant.getName(),
            "slug", tenant.getSlug(),
            "status", tenant.getStatus().name(),
            "plan", tenant.getPlan(),
            "country", tenant.getCountry(),
            "currency", tenant.getCurrency(),
            "timezone", tenant.getTimezone(),
            "locale", tenant.getLocale(),
            "createdAt", tenant.getCreatedAt().toString(),
            "updatedAt", tenant.getUpdatedAt().toString()
        );
    }
}

package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.platform.domain.ImpersonationService;
import com.discipolat.modules.platform.domain.PlatformFeatureFlag;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.audit.domain.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/platform/admin")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantService tenantService;
    private final SaasPlanService saasPlanService;
    private final ImpersonationService impersonationService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final PlatformFeatureFlagService featureFlagService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SuperAdminController(TenantRepository tenantRepository,
                                TenantMembershipRepository membershipRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository,
                                SaasPlanRepository planRepository,
                                TenantSubscriptionRepository subscriptionRepository,
                                OrganizationNodeRepository orgNodeRepository,
                                TenantService tenantService,
                                SaasPlanService saasPlanService,
                                ImpersonationService impersonationService,
                                AuditService auditService,
                                AuditLogRepository auditLogRepository,
                                PlatformFeatureFlagService featureFlagService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.tenantService = tenantService;
        this.saasPlanService = saasPlanService;
        this.impersonationService = impersonationService;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long cancelledTenants = tenantRepository.countByStatus(TenantStatus.CANCELLED);
        long pendingSetupTenants = tenantRepository.countByStatus(TenantStatus.PENDING_SETUP);

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatut(UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatut(UserStatus.INACTIVE);

        long totalMemberships = membershipRepository.count();
        long activeMemberships = membershipRepository.countByStatus(MembershipStatus.ACTIVE);

        Map<String, Long> tenantsByPlan = tenantRepository.findAll().stream()
                .collect(Collectors.groupingBy(Tenant::getPlan, Collectors.counting()));

        Map<String, Long> tenantsByStatus = tenantRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long recentTenants = tenantRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long trialSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        long pastDueSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE);
        long canceledSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);

        long totalChurches = orgNodeRepository.countByType(OrganizationNodeType.ROOT_CHURCH);
        long totalCampuses = orgNodeRepository.countByType(OrganizationNodeType.CAMPUS);
        long totalSubChurches = orgNodeRepository.countByType(OrganizationNodeType.SUB_CHURCH);
        long totalDepartments = orgNodeRepository.countByType(OrganizationNodeType.DEPARTMENT);
        long totalGroups = orgNodeRepository.countByType(OrganizationNodeType.GROUP);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentAuditLogs = auditLogRepository.countByCreatedAtGreaterThan(weekAgo);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("generatedAt", Instant.now().toString());

        dashboard.put("tenants", Map.of(
                "total", totalTenants,
                "active", activeTenants,
                "suspended", suspendedTenants,
                "cancelled", cancelledTenants,
                "pendingSetup", pendingSetupTenants,
                "recent30Days", recentTenants,
                "byPlan", tenantsByPlan,
                "byStatus", tenantsByStatus
        ));

        dashboard.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "inactive", inactiveUsers,
                "totalMemberships", totalMemberships,
                "activeMemberships", activeMemberships
        ));

        dashboard.put("subscriptions", Map.of(
                "active", activeSubscriptions,
                "trial", trialSubscriptions,
                "pastDue", pastDueSubscriptions,
                "canceled", canceledSubscriptions
        ));

        dashboard.put("organizations", Map.of(
                "churches", totalChurches,
                "campuses", totalCampuses,
                "subChurches", totalSubChurches,
                "departments", totalDepartments,
                "groups", totalGroups
        ));

        dashboard.put("activity", Map.of(
                "auditLogsLast7Days", recentAuditLogs
        ));

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return getAdminDashboard();
    }

    @GetMapping("/tenants")
    public ResponseEntity<PageResponse<Map<String, Object>>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Tenant> tenantPage;

        if (status != null || plan != null || (search != null && !search.isBlank())) {
            List<Tenant> all = tenantRepository.findAll();
            all = all.stream()
                    .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                    .filter(t -> plan == null || t.getPlan().equalsIgnoreCase(plan))
                    .filter(t -> search == null || search.isBlank() ||
                            t.getName().toLowerCase().contains(search.toLowerCase()) ||
                            t.getSlug().toLowerCase().contains(search.toLowerCase()))
                    .toList();

            int start = (int) Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            List<Tenant> pageContent = all.subList(start, end);

            List<Map<String, Object>> content = pageContent.stream().map(this::toTenantMap).toList();
            return ResponseEntity.ok(PageResponse.of(content, page, size, all.size(), (int) Math.ceil((double) all.size() / size)));
        }

        tenantPage = tenantRepository.findAll(pageable);
        List<Map<String, Object>> content = tenantPage.getContent().stream()
                .map(this::toTenantMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                tenantPage.getTotalElements(), tenantPage.getTotalPages()));
    }

    @PostMapping("/tenants")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> createTenant(
            @RequestBody Map<String, Object> request) {

        String name = (String) request.get("name");
        String slug = ((String) request.get("slug")).toLowerCase();
        String plan = request.get("plan") != null ? (String) request.get("plan") : "free";
        String country = request.get("country") != null ? (String) request.get("country") : "CM";
        String currency = request.get("currency") != null ? (String) request.get("currency") : "XAF";
        String timezone = request.get("timezone") != null ? (String) request.get("timezone") : "Africa/Douala";
        String locale = request.get("locale") != null ? (String) request.get("locale") : "fr";

        if (tenantRepository.existsBySlug(slug)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ce slug est déjà utilisé",
                "field", "slug"
            ));
        }

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

        if (!"free".equals(plan)) {
            saasPlanService.subscribe(tenant.getId(), plan, "monthly", null);
        }

        auditService.log(UUID.randomUUID(), tenant.getId(), "TENANT_CREATED",
            "TENANT", tenant.getId(), "SUCCESS", Map.of(
            "name", name, "slug", slug, "plan", plan
        ), null, null, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(toTenantMap(tenant));
    }

    @PutMapping("/tenants/{id}")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> updateTenant(@PathVariable UUID id,
                                                           @RequestBody Map<String, Object> request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Tenant non trouvé: " + id));

        if (request.get("name") != null && !String.valueOf(request.get("name")).isBlank()) {
            tenant.setName(String.valueOf(request.get("name")).trim());
        }
        if (request.get("plan") != null && !String.valueOf(request.get("plan")).isBlank()) {
            tenant.setPlan(String.valueOf(request.get("plan")).trim());
        }
        if (request.get("locale") != null && !String.valueOf(request.get("locale")).isBlank()) {
            tenant.setLocale(String.valueOf(request.get("locale")).trim());
        }
        if (request.get("timezone") != null && !String.valueOf(request.get("timezone")).isBlank()) {
            tenant.setTimezone(String.valueOf(request.get("timezone")).trim());
        }
        tenant = tenantRepository.save(tenant);

        auditService.log(UUID.randomUUID(), tenant.getId(), "TENANT_UPDATED",
                "TENANT", tenant.getId(), "SUCCESS",
                Map.of("name", tenant.getName(), "plan", tenant.getPlan()),
                null, null, null);

        return ResponseEntity.ok(toTenantMap(tenant));
    }

    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Void> suspendTenant(@PathVariable UUID id) {
        tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{id}/reactivate")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Void> reactivateTenant(@PathVariable UUID id) {
        tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{id}/archive")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Void> archiveTenant(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenant.setStatus(TenantStatus.CANCELLED);
        tenantRepository.save(tenant);
        auditService.log(UUID.randomUUID(), id, "TENANT_ARCHIVED",
                "TENANT", id, "SUCCESS", Map.of("tenantId", id.toString()),
                null, null, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plans")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> createOrUpdatePlan(
            @RequestBody Map<String, Object> request) {

        String key = (String) request.get("key");
        SaasPlan existingPlan = saasPlanService.getPlan(key).orElse(null);

        SaasPlan plan;
        if (existingPlan != null) {
            if (request.get("name") != null) existingPlan.setName((String) request.get("name"));
            if (request.get("description") != null) existingPlan.setDescription((String) request.get("description"));
            if (request.get("priceMonthly") != null) existingPlan.setPriceMonthly(((Number) request.get("priceMonthly")).longValue());
            if (request.get("priceYearly") != null) existingPlan.setPriceYearly(((Number) request.get("priceYearly")).longValue());
            if (request.get("isActive") != null) existingPlan.setIsActive(Boolean.parseBoolean(String.valueOf(request.get("isActive"))));
            plan = saasPlanService.savePlan(existingPlan);
        } else {
            plan = SaasPlan.builder()
                .key(key.toUpperCase())
                .name(request.get("name") != null ? (String) request.get("name") : key)
                .description(request.get("description") != null ? (String) request.get("description") : "")
                .priceMonthly(request.get("priceMonthly") != null ? ((Number) request.get("priceMonthly")).longValue() : 0L)
                .priceYearly(request.get("priceYearly") != null ? ((Number) request.get("priceYearly")).longValue() : 0L)
                .limitsJson(request.get("limitsJson") != null ? (String) request.get("limitsJson") : "{}")
                .featuresJson(request.get("featuresJson") != null ? (String) request.get("featuresJson") : "{}")
                .build();
            plan = saasPlanService.savePlan(plan);
        }

        return ResponseEntity.ok(toPlanMap(plan));
    }

    @GetMapping("/feature-flags")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flags", featureFlagService.getAllAsMap());
        result.put("details", featureFlagService.getAll().stream().map(f -> Map.of(
                "key", f.getKey(),
                "name", f.getName(),
                "description", f.getDescription(),
                "enabled", f.isEnabled(),
                "category", f.getCategory()
        )).toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/feature-flags/{key}")
    public ResponseEntity<Map<String, Object>> setFeatureFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> request) {
        boolean value = request.get("enabled");
        PlatformFeatureFlag flag = featureFlagService.toggle(key, value);
        return ResponseEntity.ok(Map.of(
                "key", flag.getKey(),
                "name", flag.getName(),
                "description", flag.getDescription(),
                "enabled", flag.isEnabled(),
                "category", flag.getCategory()
        ));
    }

    @PostMapping("/feature-flags")
    public ResponseEntity<Map<String, Object>> createFeatureFlag(
            @RequestBody Map<String, Object> request) {
        String key = (String) request.get("key");
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Boolean enabled = (Boolean) request.getOrDefault("enabled", false);
        String category = (String) request.getOrDefault("category", "GENERAL");

        PlatformFeatureFlag flag = featureFlagService.createOrUpdate(key, name, description, enabled, category);
        return ResponseEntity.ok(Map.of(
                "key", flag.getKey(),
                "name", flag.getName(),
                "description", flag.getDescription(),
                "enabled", flag.isEnabled(),
                "category", flag.getCategory()
        ));
    }

    @DeleteMapping("/feature-flags/{key}")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable String key) {
        featureFlagService.delete(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/impersonate")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> startImpersonation(
            @RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // §G1.9 — Délégue au service canonique (validation super admin réel,
        // anti-escalade par rôle, JWT cible TTL court, journalisation complète).
        var session = impersonationService.start(
                com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId(),
                UUID.fromString((String) request.get("tenantId")),
                (String) request.get("targetUserEmail"),
                (String) request.get("reason"),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("impersonationToken", session.token());
        result.put("targetUserId", session.targetUserId().toString());
        result.put("tenantId", session.tenantId().toString());
        result.put("tenantName", session.tenantName());
        result.put("targetRole", session.targetRole());
        result.put("startTime", session.startTime().toString());
        result.put("expiresAt", session.expiresAt().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/impersonate/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> stopImpersonation(@RequestBody(required = false) Map<String, Object> request,
                                                  jakarta.servlet.http.HttpServletRequest httpRequest) {
        String token = request != null ? (String) request.get("impersonationToken") : null;
        if (token != null && !token.isBlank()) {
            impersonationService.stop(token, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        } else {
            auditService.log(com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId(), null,
                    "IMPERSONATION_END", "PLATFORM", null, "SUCCESS", Map.of(),
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), httpRequest);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<Map<String, Object>> getTenantDetails(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Optional<TenantSubscription> subscription = subscriptionRepository.findByTenantId(id);
        List<TenantMembership> memberships = membershipRepository.findByTenantIdAndStatus(id, MembershipStatus.ACTIVE);

        Map<String, Long> membershipsByRole = memberships.stream()
                .collect(Collectors.groupingBy(m -> m.getRole().getKey(), Collectors.counting()));

        long orgNodes = orgNodeRepository.countByTenantId(id);

        Map<String, Object> details = new LinkedHashMap<>(toTenantMap(tenant));
        details.put("subscription", subscription.map(s -> Map.of(
                "planKey", s.getPlanKey(),
                "status", s.getStatus().name(),
                "billingCycle", s.getBillingCycle(),
                "currentPeriodStart", s.getCurrentPeriodStart(),
                "currentPeriodEnd", s.getCurrentPeriodEnd(),
                "cancelAtPeriodEnd", s.getCancelAtPeriodEnd(),
                "canceledAt", s.getCanceledAt(),
                "trialEndsAt", s.getTrialEndsAt()
        )).orElse(null));
        details.put("membershipsCount", memberships.size());
        details.put("membershipsByRole", membershipsByRole);
        details.put("organizationNodesCount", orgNodes);
        details.put("branding", parseJson(tenant.getBrandingJson()));
        details.put("features", parseJson(tenant.getFeaturesJson()));
        details.put("settings", parseJson(tenant.getSettingsJson()));

        return ResponseEntity.ok(details);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<Map<String, Object>>> listPlans() {
        return ResponseEntity.ok(planRepository.findAll().stream()
                .map(this::toPlanSummary)
                .toList());
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<PageResponse<Map<String, Object>>> getSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TenantSubscription> subPage;

        if (status != null && !status.isBlank()) {
            subPage = subscriptionRepository.findByStatus(
                    SubscriptionStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            subPage = subscriptionRepository.findAll(pageable);
        }

        List<Map<String, Object>> content = subPage.getContent().stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId().toString());
            map.put("tenantId", s.getTenantId().toString());
            map.put("planKey", s.getPlanKey());
            map.put("status", s.getStatus().name());
            map.put("billingCycle", s.getBillingCycle());
            map.put("currentPeriodStart", s.getCurrentPeriodStart());
            map.put("currentPeriodEnd", s.getCurrentPeriodEnd());
            map.put("cancelAtPeriodEnd", s.getCancelAtPeriodEnd());
            map.put("canceledAt", s.getCanceledAt());
            return map;
        }).toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                subPage.getTotalElements(), subPage.getTotalPages()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        Map<String, Object> dbHealth = new LinkedHashMap<>();
        dbHealth.put("status", "UP");
        dbHealth.put("tenants", tenantRepository.count());
        health.put("database", dbHealth);
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> toTenantMap(Tenant tenant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tenant.getId().toString());
        map.put("name", tenant.getName());
        map.put("slug", tenant.getSlug());
        map.put("status", tenant.getStatus().name());
        map.put("plan", tenant.getPlan());
        map.put("country", tenant.getCountry());
        map.put("currency", tenant.getCurrency());
        map.put("timezone", tenant.getTimezone());
        map.put("locale", tenant.getLocale());
        map.put("createdAt", tenant.getCreatedAt().toString());
        map.put("updatedAt", tenant.getUpdatedAt().toString());
        return map;
    }

    private Map<String, Object> toPlanMap(SaasPlan p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", p.getKey());
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("priceMonthly", p.getPriceMonthly());
        map.put("priceYearly", p.getPriceYearly());
        map.put("currency", p.getCurrency());
        map.put("sortOrder", p.getSortOrder());
        map.put("isActive", p.getIsActive());
        map.put("limitsJson", p.getLimitsJson());
        map.put("featuresJson", p.getFeaturesJson());
        return map;
    }

    private Map<String, Object> toPlanSummary(SaasPlan plan) {
        Map<String, Object> limits = parseJson(plan.getLimitsJson());
        Map<String, Object> features = parseJson(plan.getFeaturesJson());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", plan.getKey());
        map.put("name", plan.getName());
        map.put("description", plan.getDescription());
        map.put("priceMonthly", plan.getPriceMonthly());
        map.put("priceYearly", plan.getPriceYearly());
        map.put("currency", plan.getCurrency());
        map.put("limits", limits);
        map.put("features", features);
        map.put("isActive", plan.getIsActive());
        map.put("sortOrder", plan.getSortOrder());
        return map;
    }

    private Map<String, Object> toSubscriptionMap(TenantSubscription s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId().toString());
        map.put("tenantId", s.getTenantId().toString());
        map.put("planKey", s.getPlanKey());
        map.put("status", s.getStatus().name());
        map.put("billingCycle", s.getBillingCycle());
        map.put("currentPeriodEnd", s.getCurrentPeriodEnd());
        return map;
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}

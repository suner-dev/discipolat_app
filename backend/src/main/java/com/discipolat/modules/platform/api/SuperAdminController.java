package com.discipolat.modules.platform.api;

import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.platform.domain.ImpersonationService;
import com.discipolat.modules.platform.domain.PlatformFeatureFlag;
import com.discipolat.modules.platform.domain.TenantRegistrationRequest;
import com.discipolat.modules.platform.domain.TenantRegistrationService;
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
    private final TenantPlanPolicy tenantPlanPolicy;
    private final ImpersonationService impersonationService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final PlatformFeatureFlagService featureFlagService;
    private final TenantRegistrationService registrationService;
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
                                 TenantPlanPolicy tenantPlanPolicy,
                                 ImpersonationService impersonationService,
                                 AuditService auditService,
                                 AuditLogRepository auditLogRepository,
                                 PlatformFeatureFlagService featureFlagService,
                                 TenantRegistrationService registrationService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.tenantService = tenantService;
        this.saasPlanService = saasPlanService;
        this.tenantPlanPolicy = tenantPlanPolicy;
        this.impersonationService = impersonationService;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.featureFlagService = featureFlagService;
        this.registrationService = registrationService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        List<Tenant> tenants = tenantRepository.findAll();
        long totalTenants = tenants.size();
        long activeTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.ACTIVE).count();
        long suspendedTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.SUSPENDED).count();
        long cancelledTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.CANCELLED).count();
        long pendingSetupTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.PENDING_SETUP).count();

        long totalUsers = 0;
        long activeUsers = 0;
        long inactiveUsers = 0;
        long totalMemberships = 0;
        long activeMemberships = 0;
        long totalChurches = 0;
        long totalCampuses = 0;
        long totalSubChurches = 0;
        long totalDepartments = 0;
        long totalGroups = 0;
        long[] subscriptionCounts = new long[4];
        long recentAuditLogs = 0;
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        for (Tenant tenant : tenants) {
            UUID tenantId = tenant.getId();
            totalUsers += userRepository.countByTenantId(tenantId);
            activeUsers += userRepository.countByTenantIdAndStatut(tenantId, UserStatus.ACTIVE);
            inactiveUsers += userRepository.countByTenantIdAndStatut(tenantId, UserStatus.INACTIVE);
            totalMemberships += membershipRepository.countByTenantId(tenantId);
            activeMemberships += membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
            totalChurches += orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
            totalCampuses += orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS);
            totalSubChurches += orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
            totalDepartments += orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
            totalGroups += orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP);
            recentAuditLogs += auditLogRepository.countByTenantIdAndCreatedAtGreaterThan(tenantId, weekAgo);
            subscriptionRepository.findCurrentByTenantId(tenantId).ifPresent(subscription -> {
                switch (subscription.getStatus()) {
                    case ACTIVE -> subscriptionCounts[0]++;
                    case TRIAL -> subscriptionCounts[1]++;
                    case PAST_DUE -> subscriptionCounts[2]++;
                    case CANCELED -> subscriptionCounts[3]++;
                    default -> {
                    }
                }
            });
        }

        Map<String, Long> tenantsByPlan = tenants.stream()
                .collect(Collectors.groupingBy(t -> tenantPlanPolicy.normalizePlanKey(t.getPlan()), Collectors.counting()));
        Map<String, Long> tenantsByStatus = tenants.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
        long recentTenants = tenantRepository.countByCreatedAtAfter(Instant.now().minus(30, ChronoUnit.DAYS));

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
                "active", subscriptionCounts[0],
                "trial", subscriptionCounts[1],
                "pastDue", subscriptionCounts[2],
                "canceled", subscriptionCounts[3]
        ));
        dashboard.put("organizations", Map.of(
                "churches", totalChurches,
                "campuses", totalCampuses,
                "subChurches", totalSubChurches,
                "departments", totalDepartments,
                "groups", totalGroups
        ));
        dashboard.put("activity", Map.of("auditLogsLast7Days", recentAuditLogs));
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return getAdminDashboard();
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<Map<String, Object>>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<com.discipolat.modules.audit.domain.AuditLog> auditPage =
                auditLogRepository.findPlatformAll(PageRequest.of(safePage, safeSize));
        List<Map<String, Object>> content = auditPage.getContent().stream()
                .map(this::toAuditLogMap)
                .toList();
        return ResponseEntity.ok(PageResponse.of(content, safePage, safeSize,
                auditPage.getTotalElements(), auditPage.getTotalPages()));
    }

    @GetMapping("/tenants")
    public ResponseEntity<PageResponse<Map<String, Object>>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String search) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
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

            int start = (int) Math.min((long) safePage * safeSize, all.size());
            int end = Math.min(start + safeSize, all.size());
            List<Tenant> pageContent = all.subList(start, end);

            List<Map<String, Object>> content = pageContent.stream().map(this::toTenantMap).toList();
            int totalPages = (int) Math.ceil((double) all.size() / safeSize);
            return ResponseEntity.ok(PageResponse.of(content, safePage, safeSize, all.size(), totalPages));
        }

        tenantPage = tenantRepository.findAll(pageable);
        List<Map<String, Object>> content = tenantPage.getContent().stream()
                .map(this::toTenantMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(PageResponse.of(content, safePage, safeSize,
                tenantPage.getTotalElements(), tenantPage.getTotalPages()));
    }

    @PostMapping("/tenants")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> createTenant(
            @RequestBody Map<String, Object> request) {

        String name = stringValue(request.get("name"));
        String slug = stringValue(request.get("slug")).toLowerCase(Locale.ROOT);
        String plan = tenantPlanPolicy.normalizePlanKey(stringValueOrDefault(request.get("plan"), "DISCOVERY"));
        if (plan == null) {
            plan = "DISCOVERY";
        }
        String country = stringValueOrDefault(request.get("country"), "CM");
        String currency = stringValueOrDefault(request.get("currency"), "XAF");
        String timezone = stringValueOrDefault(request.get("timezone"), "Africa/Douala");
        String locale = stringValueOrDefault(request.get("locale"), "fr");

        if (name.isBlank()) {
            return badRequest("Le nom de l'organisation est requis", "name");
        }
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            return badRequest("Le slug doit contenir uniquement des lettres minuscules, chiffres et tirets", "slug");
        }
        if (slug.length() > 50) {
            return badRequest("Le slug ne doit pas dépasser 50 caractères", "slug");
        }
        try {
            java.time.ZoneId.of(timezone);
        } catch (Exception e) {
            return badRequest("Le fuseau horaire est invalide", "timezone");
        }

        if (!"DISCOVERY".equals(plan)
                && planRepository.findByKeyIgnoreCaseAndIsActiveTrue(plan).isEmpty()) {
            return badRequest("Le plan sélectionné n'existe pas ou n'est pas actif", "plan");
        }

        if (tenantRepository.existsBySlug(slug)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ce slug est déjà utilisé",
                "field", "slug"
            ));
        }

        TenantResponse tenantResponse = tenantService.create(new CreateTenantRequest(
                name, slug, plan, country, currency, timezone, locale, null, null, null));
        Tenant tenant = tenantRepository.findById(tenantResponse.id())
                .orElseThrow(() -> new IllegalStateException("Tenant non trouvé après création"));

        if (!"DISCOVERY".equals(plan)) {
            saasPlanService.subscribe(tenant.getId(), plan, "monthly", null);
        }

        auditService.log(SecurityUtils.getCurrentUserId(), tenant.getId(), "TENANT_CREATED",
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
            String canonicalPlan = tenantPlanPolicy.normalizePlanKey(String.valueOf(request.get("plan")));
            if (canonicalPlan == null
                    || planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalPlan).isEmpty()) {
                return badRequest("Le plan sélectionné n'existe pas ou n'est pas actif", "plan");
            }
            String billingCycle = subscriptionRepository.findCurrentByTenantId(id)
                    .map(subscription -> subscription.getBillingCycle())
                    .orElse("monthly");
            saasPlanService.subscribe(id, canonicalPlan, billingCycle, SecurityUtils.getCurrentUserId());
            tenant = tenantRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Tenant non trouvé après mise à jour"));
        }
        if (request.get("locale") != null && !String.valueOf(request.get("locale")).isBlank()) {
            tenant.setLocale(String.valueOf(request.get("locale")).trim());
        }
        if (request.get("timezone") != null && !String.valueOf(request.get("timezone")).isBlank()) {
            tenant.setTimezone(String.valueOf(request.get("timezone")).trim());
        }
        tenant = tenantRepository.save(tenant);

        auditService.log(SecurityUtils.getCurrentUserId(), tenant.getId(), "TENANT_UPDATED",
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
        auditService.log(SecurityUtils.getCurrentUserId(), id, "TENANT_ARCHIVED",
                "TENANT", id, "SUCCESS", Map.of("tenantId", id.toString()),
                null, null, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plans")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> createOrUpdatePlan(
            @RequestBody Map<String, Object> request) {

        String key = stringValue(request.get("key"));
        SaasPlan existingPlan = planRepository.findById(key).orElse(null);

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
        result.put("details", featureFlagService.getAll().stream().map(this::toFeatureFlagMap).toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/feature-flags/{key}")
    public ResponseEntity<Map<String, Object>> setFeatureFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> request) {
        boolean value = request.get("enabled");
        PlatformFeatureFlag flag = featureFlagService.toggle(key, value);
        return ResponseEntity.ok(toFeatureFlagMap(flag));
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
        return ResponseEntity.ok(toFeatureFlagMap(flag));
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
    public ResponseEntity<?> stopImpersonation(@RequestBody Map<String, Object> request,
                                                jakarta.servlet.http.HttpServletRequest httpRequest) {
        String token = request == null ? null : (String) request.get("impersonationToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "impersonationToken est requis"));
        }
        impersonationService.stop(token, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<Map<String, Object>> getTenantDetails(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Optional<TenantSubscription> subscription = subscriptionRepository.findCurrentByTenantId(id);
        List<TenantMembership> memberships = membershipRepository.findByTenantIdAndStatus(id, MembershipStatus.ACTIVE);

        Map<String, Long> membershipsByRole = memberships.stream()
                .collect(Collectors.groupingBy(m -> m.getRole().getKey(), Collectors.counting()));

        long orgNodes = orgNodeRepository.countByTenantId(id);

        Map<String, Object> details = new LinkedHashMap<>(toTenantMap(tenant));
        details.put("subscription", subscription.map(this::toSubscriptionMap).orElse(null));
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

    @GetMapping("/registration-requests")
    public ResponseEntity<PageResponse<Map<String, Object>>> listRegistrationRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<TenantRegistrationRequest> requests = registrationService.listPending(
                PageRequest.of(safePage, safeSize));
        List<Map<String, Object>> content = requests.getContent().stream()
                .map(this::toRegistrationRequestMap)
                .toList();
        return ResponseEntity.ok(PageResponse.of(content, safePage, safeSize,
                requests.getTotalElements(), requests.getTotalPages()));
    }

    @PostMapping("/registration-requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveRegistrationRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        TenantRegistrationService.ApprovalResult result = registrationService.approve(
                id, body == null ? null : body.get("reason"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("request", toRegistrationRequestMap(result.request()));
        response.put("tenantId", result.tenant().id().toString());
        response.put("userId", result.owner().getId().toString());
        response.put("churchId", result.church().getId().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/registration-requests/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectRegistrationRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        TenantRegistrationRequest request = registrationService.reject(
                id, body == null ? null : body.get("reason"));
        return ResponseEntity.ok(toRegistrationRequestMap(request));
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

    private Map<String, Object> toRegistrationRequestMap(TenantRegistrationRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.getId().toString());
        map.put("email", request.getEmail());
        map.put("firstName", request.getFirstName());
        map.put("lastName", request.getLastName());
        map.put("phone", request.getPhone());
        map.put("organizationName", request.getOrganizationName());
        map.put("slug", request.getSlug());
        map.put("plan", request.getPlan());
        map.put("country", request.getCountry());
        map.put("currency", request.getCurrency());
        map.put("timezone", request.getTimezone());
        map.put("locale", request.getLocale());
        map.put("status", request.getStatus().name());
        map.put("createdAt", request.getCreatedAt());
        map.put("reviewedAt", request.getReviewedAt());
        return map;
    }

    private Map<String, Object> toAuditLogMap(com.discipolat.modules.audit.domain.AuditLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId().toString());
        map.put("tenantId", log.getTenantId().toString());
        map.put("userId", log.getUtilisateurId() != null ? log.getUtilisateurId().toString() : null);
        map.put("action", log.getAction());
        map.put("entityType", log.getEntiteType());
        map.put("entityId", log.getEntiteId() != null ? log.getEntiteId().toString() : null);
        map.put("oldValues", log.getAncienValeur());
        map.put("newValues", log.getNouvelleValeur());
        map.put("ipAddress", log.getAdresseIp());
        map.put("userAgent", log.getUserAgent());
        map.put("createdAt", log.getCreatedAt());
        return map;
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
        map.put("id", s.getId() != null ? s.getId().toString() : null);
        map.put("tenantId", s.getTenantId() != null ? s.getTenantId().toString() : null);
        map.put("planKey", s.getPlanKey());
        map.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        map.put("billingCycle", s.getBillingCycle());
        map.put("currentPeriodStart", s.getCurrentPeriodStart());
        map.put("currentPeriodEnd", s.getCurrentPeriodEnd());
        map.put("cancelAtPeriodEnd", s.getCancelAtPeriodEnd());
        map.put("canceledAt", s.getCanceledAt());
        map.put("trialEndsAt", s.getTrialEndsAt());
        return map;
    }

    private Map<String, Object> toFeatureFlagMap(PlatformFeatureFlag flag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", flag.getKey());
        map.put("name", flag.getName());
        map.put("description", flag.getDescription());
        map.put("enabled", flag.isEnabled());
        map.put("category", flag.getCategory());
        return map;
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private String stringValueOrDefault(Object value, String defaultValue) {
        String text = stringValue(value);
        return text.isBlank() ? defaultValue : text;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message, String field) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("field", field);
        return ResponseEntity.badRequest().body(body);
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

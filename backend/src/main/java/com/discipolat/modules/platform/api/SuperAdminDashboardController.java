package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/platform/admin/dashboard")
@PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
public class SuperAdminDashboardController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public SuperAdminDashboardController(TenantRepository tenantRepository,
                                         TenantMembershipRepository membershipRepository,
                                         UserRepository userRepository,
                                         RoleRepository roleRepository,
                                         SaasPlanRepository planRepository,
                                         TenantSubscriptionRepository subscriptionRepository,
                                         OrganizationNodeRepository orgNodeRepository,
                                         AuditService auditService,
                                         AuditLogRepository auditLogRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
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

        // Tenants by plan
        Map<String, Long> tenantsByPlan = tenantRepository.findAll().stream()
                .collect(Collectors.groupingBy(Tenant::getPlan, Collectors.counting()));

        // Tenants by status
        Map<String, Long> tenantsByStatus = tenantRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        // Recent tenants (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentTenants = tenantRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        // Active subscriptions
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long trialSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        long pastDueSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE);
        long canceledSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);

        // Organization hierarchy stats
        long totalChurches = orgNodeRepository.countByType(OrganizationNodeType.ROOT_CHURCH);
        long totalCampuses = orgNodeRepository.countByType(OrganizationNodeType.CAMPUS);
        long totalSubChurches = orgNodeRepository.countByType(OrganizationNodeType.SUB_CHURCH);
        long totalDepartments = orgNodeRepository.countByType(OrganizationNodeType.DEPARTMENT);
        long totalGroups = orgNodeRepository.countByType(OrganizationNodeType.GROUP);

        // Recent activity
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentAuditLogs = auditLogRepository.countByTimestampAfter(weekAgo);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("generatedAt", Instant.now().toString());

        // Tenant metrics
        overview.put("tenants", Map.of(
                "total", totalTenants,
                "active", activeTenants,
                "suspended", suspendedTenants,
                "cancelled", cancelledTenants,
                "pendingSetup", pendingSetupTenants,
                "recent30Days", recentTenants,
                "byPlan", tenantsByPlan,
                "byStatus", tenantsByStatus
        ));

        // User metrics
        overview.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "inactive", inactiveUsers,
                "totalMemberships", totalMemberships,
                "activeMemberships", activeMemberships
        ));

        // Subscription metrics
        overview.put("subscriptions", Map.of(
                "active", activeSubscriptions,
                "trial", trialSubscriptions,
                "pastDue", pastDueSubscriptions,
                "canceled", canceledSubscriptions
        ));

        // Organization metrics
        overview.put("organizations", Map.of(
                "churches", totalChurches,
                "campuses", totalCampuses,
                "subChurches", totalSubChurches,
                "departments", totalDepartments,
                "groups", totalGroups
        ));

        // Activity
        overview.put("activity", Map.of(
                "auditLogsLast7Days", recentAuditLogs
        ));

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/tenants")
    public ResponseEntity<PageResponse<Map<String, Object>>> getTenants(
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

            List<Map<String, Object>> content = pageContent.stream().map(this::toTenantSummary).toList();
            return ResponseEntity.ok(PageResponse.of(content, page, size, all.size(), (int) Math.ceil((double) all.size() / size)));
        }

        tenantPage = tenantRepository.findAll(pageable);
        List<Map<String, Object>> content = tenantPage.getContent().stream()
                .map(this::toTenantSummary)
                .toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                tenantPage.getTotalElements(), tenantPage.getTotalPages()));
    }

    @GetMapping("/tenants/{id}/details")
    public ResponseEntity<Map<String, Object>> getTenantDetails(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Optional<TenantSubscription> subscription = subscriptionRepository.findByTenantId(id);
        List<TenantMembership> memberships = membershipRepository.findByTenantIdAndStatus(id, MembershipStatus.ACTIVE);

        Map<String, Long> membershipsByRole = memberships.stream()
                .collect(Collectors.groupingBy(m -> m.getRole().getKey(), Collectors.counting()));

        long orgNodes = orgNodeRepository.countByTenantId(id);

        Map<String, Object> details = new LinkedHashMap<>();
        details.putAll(toTenantSummary(tenant));
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
    public ResponseEntity<List<Map<String, Object>>> getPlans() {
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

        List<Map<String, Object>> content = subPage.getContent().stream().map(s -> Map.of(
                "id", s.getId().toString(),
                "tenantId", s.getTenantId().toString(),
                "planKey", s.getPlanKey(),
                "status", s.getStatus().name(),
                "billingCycle", s.getBillingCycle(),
                "currentPeriodStart", s.getCurrentPeriodStart(),
                "currentPeriodEnd", s.getCurrentPeriodEnd(),
                "cancelAtPeriodEnd", s.getCancelAtPeriodEnd(),
                "canceledAt", s.getCanceledAt()
        )).toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                subPage.getTotalElements(), subPage.getTotalPages()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        // Basic health metrics
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("database", Map.of("status", "UP", "tenants", tenantRepository.count()));
        health.put("version", "1.0.0"); // Would come from build info
        return ResponseEntity.ok(health);
    }

    private Map<String, Object> toTenantSummary(Tenant tenant) {
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

    private Map<String, Object> toPlanSummary(SaasPlan plan) {
        Map<String, Object> limits = parseJson(plan.getLimitsJson());
        Map<String, Object> features = parseJson(plan.getFeaturesJson());

        return Map.of(
                "key", plan.getKey(),
                "name", plan.getName(),
                "description", plan.getDescription(),
                "priceMonthly", plan.getPriceMonthly(),
                "priceYearly", plan.getPriceYearly(),
                "currency", plan.getCurrency(),
                "limits", limits,
                "features", features,
                "isActive", plan.isActive(),
                "sortOrder", plan.getSortOrder()
        );
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
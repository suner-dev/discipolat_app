package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.audit.domain.AuditLogRepository;
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
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
public class TenantAdminDashboardController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;

    public TenantAdminDashboardController(TenantRepository tenantRepository,
                                          TenantMembershipRepository membershipRepository,
                                          UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          OrganizationNodeRepository orgNodeRepository,
                                          AuditService auditService,
                                          AuditLogRepository auditLogRepository,
                                          TenantSubscriptionRepository subscriptionRepository,
                                          SaasPlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    private UUID getCurrentTenantId() {
        return TenantContext.requireTenantId();
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        UUID tenantId = getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndStatut(tenantId, UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByTenantIdAndStatut(tenantId, UserStatus.INACTIVE);

        long totalMemberships = membershipRepository.countByTenantId(tenantId);
        long activeMemberships = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        long pendingMemberships = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.PENDING);

        Map<String, Long> membersByRole = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(m -> m.getRole().getKey(), Collectors.counting()));

        long churchCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        long campusCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS);
        long subChurchCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
        long departmentCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
        long groupCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP);

        Optional<TenantSubscription> subscription = subscriptionRepository.findByTenantId(tenantId);
        Optional<SaasPlan> plan = subscription.flatMap(s -> planRepository.findById(s.getPlanKey()));

        // Quota usage
        Map<String, Object> quotas = new HashMap<>();
        if (subscription.isPresent() && plan.isPresent()) {
            Map<String, Object> limits = parseJson(plan.get().getLimitsJson());
            quotas = calculateQuotaUsage(tenantId, limits);
        }

        // Recent activity
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long recentAuditLogs = auditLogRepository.countByTenantIdAndTimestampAfter(tenantId, weekAgo);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("generatedAt", Instant.now().toString());
        overview.put("tenantId", tenantId.toString());
        overview.put("tenantName", tenant.getName());
        overview.put("plan", tenant.getPlan());

        overview.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "inactive", inactiveUsers,
                "totalMemberships", totalMemberships,
                "activeMemberships", activeMemberships,
                "pendingMemberships", pendingMemberships,
                "membersByRole", membersByRole
        ));

        overview.put("organizations", Map.of(
                "churches", churchCount,
                "campuses", campusCount,
                "subChurches", subChurchCount,
                "departments", departmentCount,
                "groups", groupCount
        ));

        overview.put("subscription", subscription.map(s -> Map.of(
                "planKey", s.getPlanKey(),
                "status", s.getStatus().name(),
                "billingCycle", s.getBillingCycle(),
                "currentPeriodStart", s.getCurrentPeriodStart(),
                "currentPeriodEnd", s.getCurrentPeriodEnd(),
                "cancelAtPeriodEnd", s.getCancelAtPeriodEnd(),
                "trialEndsAt", s.getTrialEndsAt(),
                "plan", plan.map(p -> Map.of(
                        "name", p.getName(),
                        "features", parseJson(p.getFeaturesJson())
                )).orElse(null)
        )).orElse(null));

        overview.put("quotas", quotas);

        overview.put("activity", Map.of(
                "auditLogsLast7Days", recentAuditLogs
        ));

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/members")
    public ResponseEntity<PageResponse<Map<String, Object>>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<TenantMembership> memberships;
        if (role != null) {
            memberships = membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                    tenantId, MembershipStatus.ACTIVE, role.toUpperCase(), pageable).getContent();
        } else if (status != null) {
            memberships = membershipRepository.findByTenantIdAndStatus(
                    tenantId, MembershipStatus.valueOf(status.toUpperCase()), pageable).getContent();
        } else {
            memberships = membershipRepository.findByTenantIdAndStatus(
                    tenantId, MembershipStatus.ACTIVE, pageable).getContent();
        }

        // Apply search filter
        if (search != null && !search.isBlank()) {
            String lowerSearch = search.toLowerCase();
            List<User> users = userRepository.findByTenantId(getCurrentTenantId());
            Set<UUID> matchingUserIds = users.stream()
                    .filter(u -> (u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(lowerSearch) ||
                            u.getEmail().toLowerCase().contains(lowerSearch))
                    .map(User::getId)
                    .collect(Collectors.toSet());
            memberships = memberships.stream()
                    .filter(m -> matchingUserIds.contains(m.getUserId()))
                    .toList();
        }

        List<Map<String, Object>> content = memberships.stream().map(m -> {
            Optional<User> user = userRepository.findById(m.getUserId());
            return Map.<String, Object>of(
                    "membershipId", m.getId().toString(),
                    "userId", m.getUserId().toString(),
                    "email", user.map(User::getEmail).orElse("unknown"),
                    "firstName", user.map(User::getFirstName).orElse(""),
                    "lastName", user.map(User::getLastName).orElse(""),
                    "fullName", (user.map(User::getFirstName).orElse("") + " " + user.map(User::getLastName).orElse("")).trim(),
                    "role", m.getRole().getKey(),
                    "scopeType", m.getScopeType().name(),
                    "scopeId", m.getScopeId() != null ? m.getScopeId().toString() : null,
                    "status", m.getStatus().name(),
                    "joinedAt", m.getJoinedAt().toString(),
                    "photoUrl", user.map(User::getPhotoUrl).orElse(null)
            );
        }).toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size, content.size(), 1));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getRoles() {
        UUID tenantId = getCurrentTenantId();
        List<Role> roles = roleRepository.findByTenantId(tenantId);
        List<Role> systemRoles = roleRepository.findByTenantIdIsNull();

        List<Map<String, Object>> all = new ArrayList<>();
        for (Role r : systemRoles) {
            all.add(toRoleMap(r, true));
        }
        for (Role r : roles) {
            all.add(toRoleMap(r, false));
        }

        return ResponseEntity.ok(all);
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<Permission>>> getPermissionsByScope() {
        return ResponseEntity.ok(permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Permission::getScope)));
    }

    @GetMapping("/org/stats")
    public ResponseEntity<Map<String, Long>> getOrgStats() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(orgNodeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(OrganizationNode::getType, Collectors.counting())));
    }

    @GetMapping("/activity")
    public ResponseEntity<PageResponse<Map<String, Object>>> getActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<com.discipolat.modules.audit.domain.AuditLog> auditPage = auditLogRepository.findByTenantId(tenantId, pageable);

        List<Map<String, Object>> content = auditPage.getContent().stream().map(log -> Map.of(
                "id", log.getId().toString(),
                "action", log.getAction(),
                "resourceType", log.getEntiteType(),
                "resourceId", log.getEntiteId() != null ? log.getEntiteId().toString() : null,
                "result", log.getResult(),
                "timestamp", log.getTimestamp().toString()
        )).toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                auditPage.getTotalElements(), auditPage.getTotalPages()));
    }

    private Map<String, Object> toRoleMap(Role role, boolean isSystem) {
        return Map.of(
                "id", role.getId().toString(),
                "key", role.getKey(),
                "label", role.getLabel(),
                "description", role.getDescription(),
                "system", isSystem || role.getSystem(),
                "priority", role.getPriority(),
                "permissions", role.getPermissions().stream().map(Permission::getKey).toList()
        );
    }

    private Map<String, Object> calculateQuotaUsage(UUID tenantId, Map<String, Object> limits) {
        Map<String, Object> usage = new HashMap<>();

        // Current usage
        long users = userRepository.countByTenantId(tenantId);
        long churches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        long departments = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);

        if (limits.containsKey("max_users")) {
            usage.put("users", Map.of(
                    "used", users,
                    "limit", limits.get("max_users"),
                    "percent", limits.get("max_users") instanceof Number ?
                            (users * 100.0 / ((Number) limits.get("max_users")).doubleValue()) : 0
            ));
        }
        if (limits.containsKey("max_churches")) {
            usage.put("churches", Map.of(
                    "used", churches,
                    "limit", limits.get("max_churches"),
                    "percent", limits.get("max_churches") instanceof Number ?
                            (churches * 100.0 / ((Number) limits.get("max_churches")).doubleValue()) : 0
            ));
        }
        if (limits.containsKey("max_departments")) {
            usage.put("departments", Map.of(
                    "used", departments,
                    "limit", limits.get("max_departments"),
                    "percent", limits.get("max_departments") instanceof Number ?
                            (departments * 100.0 / ((Number) limits.get("max_departments")).doubleValue()) : 0
            ));
        }
        if (limits.containsKey("max_storage_mb")) {
            usage.put("storage", Map.of(
                    "usedMb", 0, // Would need actual storage calculation
                    "limitMb", limits.get("max_storage_mb"),
                    "percent", 0
            ));
        }

        return usage;
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
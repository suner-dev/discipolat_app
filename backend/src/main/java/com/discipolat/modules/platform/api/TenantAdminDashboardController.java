package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.audit.domain.AuditLog;
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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("@authz.isTenantAdmin()")
public class TenantAdminDashboardController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final AuditLogRepository auditLogRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SaasPlanRepository planRepository;

    public TenantAdminDashboardController(TenantRepository tenantRepository,
                                          TenantMembershipRepository membershipRepository,
                                          UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          PermissionRepository permissionRepository,
                                          OrganizationNodeRepository orgNodeRepository,
                                          AuditLogRepository auditLogRepository,
                                          TenantSubscriptionRepository subscriptionRepository,
                                          SaasPlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.orgNodeRepository = orgNodeRepository;
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
                .filter(m -> m.getRole() != null)
                .collect(Collectors.groupingBy(m -> m.getRole().getKey(), Collectors.counting()));

        long churchCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        long campusCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS);
        long subChurchCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH);
        long departmentCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);
        long groupCount = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.GROUP);

        Optional<TenantSubscription> subscription = subscriptionRepository.findCurrentByTenantId(tenantId);
        Optional<SaasPlan> plan = subscription.flatMap(s -> planRepository.findByKeyIgnoreCase(s.getPlanKey()));

        Map<String, Object> quotas = new HashMap<>();
        if (subscription.isPresent() && plan.isPresent()) {
            Map<String, Object> limits = parseJson(plan.get().getLimitsJson());
            quotas = calculateQuotaUsage(tenantId, limits);
        }

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<AuditLog> recentLogs = auditLogRepository.findSince(weekAgo);
        long recentAuditLogs = recentLogs.stream()
                .filter(l -> tenantId.equals(l.getTenantId()))
                .count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("generatedAt", Instant.now().toString());
        overview.put("tenantId", tenantId.toString());
        overview.put("tenantName", tenant.getName());
        overview.put("plan", tenant.getPlan());

        Map<String, Object> userMetrics = new LinkedHashMap<>();
        userMetrics.put("total", totalUsers);
        userMetrics.put("active", activeUsers);
        userMetrics.put("inactive", inactiveUsers);
        userMetrics.put("totalMemberships", totalMemberships);
        userMetrics.put("activeMemberships", activeMemberships);
        userMetrics.put("pendingMemberships", pendingMemberships);
        userMetrics.put("membersByRole", membersByRole);
        overview.put("users", userMetrics);

        Map<String, Long> orgStats = new LinkedHashMap<>();
        orgStats.put("churches", churchCount);
        orgStats.put("campuses", campusCount);
        orgStats.put("subChurches", subChurchCount);
        orgStats.put("departments", departmentCount);
        orgStats.put("groups", groupCount);
        overview.put("organizations", orgStats);

        overview.put("subscription", subscription.map(s -> {
            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("planKey", s.getPlanKey());
            sub.put("status", s.getStatus().name());
            sub.put("billingCycle", s.getBillingCycle());
            sub.put("currentPeriodStart", s.getCurrentPeriodStart());
            sub.put("currentPeriodEnd", s.getCurrentPeriodEnd());
            sub.put("cancelAtPeriodEnd", s.getCancelAtPeriodEnd());
            sub.put("trialEndsAt", s.getTrialEndsAt());
            sub.put("plan", plan.map(p -> {
                Map<String, Object> pMap = new LinkedHashMap<>();
                pMap.put("name", p.getName());
                pMap.put("features", parseJson(p.getFeaturesJson()));
                return pMap;
            }).orElse(null));
            return sub;
        }).orElse(null));

        overview.put("quotas", quotas);

        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("auditLogsLast7Days", recentAuditLogs);
        overview.put("activity", activity);

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
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("membershipId", m.getId().toString());
            map.put("userId", m.getUserId().toString());
            map.put("email", user.map(User::getEmail).orElse("unknown"));
            map.put("firstName", user.map(User::getFirstName).orElse(""));
            map.put("lastName", user.map(User::getLastName).orElse(""));
            map.put("fullName", (user.map(User::getFirstName).orElse("") + " " + user.map(User::getLastName).orElse("")).trim());
            map.put("role", m.getRole() != null ? m.getRole().getKey() : "UNKNOWN");
            map.put("scopeType", m.getScopeType() != null ? m.getScopeType().name() : null);
            map.put("scopeId", m.getScopeId() != null ? m.getScopeId().toString() : null);
            map.put("status", m.getStatus().name());
            map.put("joinedAt", m.getJoinedAt().toString());
            map.put("photoUrl", user.map(User::getPhotoUrl).orElse(null));
            return map;
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
                .collect(Collectors.groupingBy(p -> p.getScope().name())));
    }

    @GetMapping("/org/stats")
    public ResponseEntity<Map<String, Long>> getOrgStats() {
        UUID tenantId = getCurrentTenantId();
        Map<String, Long> stats = new LinkedHashMap<>();
        for (OrganizationNode node : orgNodeRepository.findByTenantId(tenantId)) {
            stats.merge(node.getType().name(), 1L, Long::sum);
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activity")
    public ResponseEntity<PageResponse<Map<String, Object>>> getActivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditPage = auditLogRepository.findFiltered(tenantId, null, null, null, null, pageable);

        List<Map<String, Object>> content = auditPage.getContent().stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", log.getId().toString());
            map.put("action", log.getAction());
            map.put("resourceType", log.getEntiteType());
            map.put("resourceId", log.getEntiteId() != null ? log.getEntiteId().toString() : null);
            map.put("timestamp", log.getCreatedAt().toString());
            return map;
        }).toList();

        return ResponseEntity.ok(PageResponse.of(content, page, size,
                auditPage.getTotalElements(), auditPage.getTotalPages()));
    }

    private Map<String, Object> toRoleMap(Role role, boolean isSystem) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", role.getId().toString());
        map.put("key", role.getKey());
        map.put("label", role.getLabel());
        map.put("description", role.getDescription());
        map.put("system", isSystem || role.getSystem());
        map.put("priority", role.getPriority());
        map.put("permissions", role.getPermissions() != null ? role.getPermissions().stream().map(Permission::getKey).toList() : List.of());
        return map;
    }

    private Map<String, Object> calculateQuotaUsage(UUID tenantId, Map<String, Object> limits) {
        Map<String, Object> usage = new LinkedHashMap<>();

        long users = userRepository.countByTenantId(tenantId);
        long churches = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        long departments = orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.DEPARTMENT);

        if (limits.containsKey("max_users")) {
            Map<String, Object> userQuota = new LinkedHashMap<>();
            userQuota.put("used", users);
            userQuota.put("limit", limits.get("max_users"));
            userQuota.put("percent", limits.get("max_users") instanceof Number ?
                    (users * 100.0 / ((Number) limits.get("max_users")).doubleValue()) : 0);
            usage.put("users", userQuota);
        }
        if (limits.containsKey("max_churches")) {
            Map<String, Object> churchQuota = new LinkedHashMap<>();
            churchQuota.put("used", churches);
            churchQuota.put("limit", limits.get("max_churches"));
            churchQuota.put("percent", limits.get("max_churches") instanceof Number ?
                    (churches * 100.0 / ((Number) limits.get("max_churches")).doubleValue()) : 0);
            usage.put("churches", churchQuota);
        }
        if (limits.containsKey("max_departments")) {
            Map<String, Object> deptQuota = new LinkedHashMap<>();
            deptQuota.put("used", departments);
            deptQuota.put("limit", limits.get("max_departments"));
            deptQuota.put("percent", limits.get("max_departments") instanceof Number ?
                    (departments * 100.0 / ((Number) limits.get("max_departments")).doubleValue()) : 0);
            usage.put("departments", deptQuota);
        }
        if (limits.containsKey("max_storage_mb")) {
            Map<String, Object> storageQuota = new LinkedHashMap<>();
            storageQuota.put("usedMb", 0);
            storageQuota.put("limitMb", limits.get("max_storage_mb"));
            storageQuota.put("percent", 0);
            usage.put("storage", storageQuota);
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

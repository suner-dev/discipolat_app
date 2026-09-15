package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.platform.domain.ImpersonationService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.audit.domain.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@RestController
@RequestMapping("/api/v1/platform/admin")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final SaasPlanService saasPlanService;
    private final ImpersonationService impersonationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SuperAdminController(TenantRepository tenantRepository,
                                TenantMembershipRepository membershipRepository,
                                UserRepository userRepository,
                                TenantService tenantService,
                                SaasPlanService saasPlanService,
                                ImpersonationService impersonationService,
                                AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.saasPlanService = saasPlanService;
        this.impersonationService = impersonationService;
        this.auditService = auditService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatut(UserStatus.ACTIVE);
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
        dashboard.put("plans", plans.stream().map(this::toPlanMap).collect(Collectors.toList()));
        dashboard.put("subscriptions", subscriptions.stream().map(this::toSubscriptionMap).collect(Collectors.toList()));
        dashboard.put("generatedAt", Instant.now().toString());

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<PageResponse<Map<String, Object>>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (status != null || plan != null) {
            List<Map<String, Object>> content = tenantRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus().name().equalsIgnoreCase(status))
                .filter(t -> plan == null || t.getPlan().equalsIgnoreCase(plan))
                .map(this::toTenantMap)
                .collect(Collectors.toList());
            return ResponseEntity.ok(PageResponse.of(content, page, size, content.size(), 1));
        }

        Page<Tenant> tenantPage = tenantRepository.findAll(pageRequest);
        List<Map<String, Object>> content = tenantPage.getContent().stream()
            .map(this::toTenantMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok(PageResponse.of(content, page, size,
            tenantPage.getTotalElements(), tenantPage.getTotalPages()));
    }

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

    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> suspendTenant(@PathVariable UUID id) {
        tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{id}/reactivate")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Void> reactivateTenant(@PathVariable UUID id) {
        tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        tenantService.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{id}/archive")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
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

    @GetMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listPlans() {
        return ResponseEntity.ok(saasPlanService.getAllPlans().stream()
            .map(this::toPlanMap).collect(Collectors.toList()));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
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
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("aiEnabled", true);
        flags.put("mobileMoneyEnabled", true);
        flags.put("whatsappEnabled", true);
        flags.put("analyticsEnabled", true);
        flags.put("docsEnabled", true);
        return ResponseEntity.ok(flags);
    }

    @PutMapping("/feature-flags/{key}")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> setFeatureFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> request) {
        boolean value = request.get("enabled");
        return ResponseEntity.ok(Map.of("key", key, "enabled", value));
    }

    @PostMapping("/impersonate")
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
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
    @PreAuthorize("hasRole('PLATFORM_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTenantDetails(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Optional<TenantSubscription> subscription = saasPlanService.getTenantSubscription(id);
        long memberCount = membershipRepository.countByTenantId(id);

        Map<String, Object> details = new LinkedHashMap<>(toTenantMap(tenant));
        details.put("memberCount", memberCount);
        details.put("subscription", subscription.map(s -> {
            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("planKey", s.getPlanKey());
            sub.put("status", s.getStatus().name());
            sub.put("currentPeriodEnd", s.getCurrentPeriodEnd());
            sub.put("cancelAtPeriodEnd", s.getCancelAtPeriodEnd());
            return sub;
        }).orElse(null));

        return ResponseEntity.ok(details);
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
}

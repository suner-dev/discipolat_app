package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Modules & Feature Flags (Section 29, 39-42 du prompt)
 * Gestion des modules activés par tenant, feature flags, plans et quotas
 */
@RestController
@RequestMapping("/api/v1/admin/features")
public class ModuleFeatureController {

    private final TenantRepository tenantRepository;
    private final SaasPlanRepository planRepository;
    private final SaasPlanService planService;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;

    public ModuleFeatureController(
            TenantRepository tenantRepository,
            SaasPlanRepository planRepository,
            SaasPlanService planService,
            TenantSubscriptionRepository subscriptionRepository,
            AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.planService = planService;
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant");
        return tenantId;
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    // ==================== LISTER LES MODULES ====================

    @GetMapping("/modules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ModulesConfigResponse> getModules() {
        UUID tenantId = getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Map<String, Object> featuresJson = parseJson(tenant.getFeaturesJson());
        Map<String, Object> settingsJson = parseJson(tenant.getSettingsJson());

        List<ModuleInfo> modules = new ArrayList<>();
        modules.add(new ModuleInfo("DASHBOARD", "Tableau de bord", true, featuresJson.containsKey("dashboard")));
        modules.add(new ModuleInfo("MEMBERS", "Membres", true, featuresJson.containsKey("members")));
        modules.add(new ModuleInfo("FAMILIES", "Familles", true, featuresJson.containsKey("families")));
        modules.add(new ModuleInfo("CHURCHES", "Églises", true, featuresJson.containsKey("churches")));
        modules.add(new ModuleInfo("EVENTS", "Événements", true, featuresJson.containsKey("events")));
        modules.add(new ModuleInfo("NOTIFICATIONS", "Notifications", true, featuresJson.containsKey("notifications")));
        modules.add(new ModuleInfo("MESSAGES", "Messagerie", true, featuresJson.containsKey("messages")));
        modules.add(new ModuleInfo("DOCUMENTS", "Documents", true, featuresJson.containsKey("documents")));
        modules.add(new ModuleInfo("REPORTS", "Rapports", true, featuresJson.containsKey("reports")));
        modules.add(new ModuleInfo("ANALYTICS", "Analytics", true, featuresJson.containsKey("analytics")));
        modules.add(new ModuleInfo("DISCIPLESHIP", "Discipleship", true, featuresJson.containsKey("discipleship")));
        modules.add(new ModuleInfo("ACADEMY", "Académie", true, featuresJson.containsKey("academy")));
        modules.add(new ModuleInfo("AI", "IA", true, featuresJson.containsKey("ai") && isAiEnabled(tenantId)));
        modules.add(new ModuleInfo("FINANCE", "Finances", true, featuresJson.containsKey("finance")));
        modules.add(new ModuleInfo("MOBILE_MONEY", "Mobile Money", true, featuresJson.containsKey("mobileMoney")));
        modules.add(new ModuleInfo("MARKETPLACE", "Marketplace", true, featuresJson.containsKey("marketplace")));
        modules.add(new ModuleInfo("WHATSLINK", "WhatsApp", true, featuresJson.containsKey("whatsapp")));
        modules.add(new ModuleInfo("COMMUNITY", "Communauté", true, featuresJson.containsKey("community")));
        modules.add(new ModuleInfo("FORMS", "Formulaires", true, featuresJson.containsKey("forms")));
        modules.add(new ModuleInfo("CALENDAR", "Calendrier", true, featuresJson.containsKey("calendar")));

        // Vérifier les quotas
        Map<String, QuotaInfo> quotas = getQuotas(tenantId);

        return ResponseEntity.ok(new ModulesConfigResponse(
                tenantId,
                modules,
                quotas,
                getSubscriptionInfo(tenantId)
        ));
    }

    // ==================== ACTIVER/DÉSACTIVER UN MODULE ====================

    @PutMapping("/modules/{moduleKey}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<ModuleInfo> toggleModule(
            @PathVariable String moduleKey,
            @RequestBody Map<String, Boolean> request) {
        
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();
        boolean enabled = request.getOrDefault("enabled", true);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        features.put(moduleKey.toLowerCase(), enabled);
        tenant.setFeaturesJson(toJson(features));
        tenantRepository.save(tenant);

        auditService.log(currentUserId, tenantId, "MODULE_TOGGLED", "TENANT",
                null, "SUCCESS",
                Map.of("module", moduleKey, "enabled", enabled),
                null, null, null);

        boolean isEnabled = features.containsKey(moduleKey.toLowerCase()) 
            && (Boolean) features.get(moduleKey.toLowerCase());
        
        return ResponseEntity.ok(new ModuleInfo(
                moduleKey,
                getModuleLabel(moduleKey),
                isEnabled,
                isEnabled
        ));
    }

    // ==================== FEATURE FLAGS ====================

    @GetMapping("/flags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFeatureFlags() {
        UUID tenantId = getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Map<String, Object> features = parseJson(tenant.getFeaturesJson());
        Map<String, Object> settings = parseJson(tenant.getSettingsJson());

        // Feature flags globaux (toujours activés sauf override)
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("darkMode", settings.getOrDefault("darkMode", true));
        flags.put("betaFeatures", settings.getOrDefault("betaFeatures", false));
        flags.put("newDashboard", settings.getOrDefault("newDashboard", false));
        flags.put("autoSync", settings.getOrDefault("autoSync", true));
        flags.put("twoFactorRequired", settings.getOrDefault("twoFactorRequired", false));
        flags.put("passwordPolicy", settings.getOrDefault("passwordPolicy", true));
        flags.put("sessionTimeout", settings.getOrDefault("sessionTimeout", 900));

        return ResponseEntity.ok(flags);
    }

    @PutMapping("/flags")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateFeatureFlags(@RequestBody Map<String, Object> flags) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Map<String, Object> settings = parseJson(tenant.getSettingsJson());
        settings.putAll(flags);
        tenant.setSettingsJson(toJson(settings));
        tenantRepository.save(tenant);

        auditService.log(currentUserId, tenantId, "FEATURE_FLAGS_UPDATED", "TENANT",
                tenantId, "SUCCESS",
                Map.of("flags", flags.keySet()),
                null, null, null);

        return ResponseEntity.ok(flags);
    }

    // ==================== PLANS ====================

    @GetMapping("/plans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlanInfo>> getPlans() {
        List<SaasPlan> plans = planService.getActivePlans();
        List<PlanInfo> result = new ArrayList<>();
        
        for (SaasPlan plan : plans) {
            result.add(new PlanInfo(
                    plan.getId(),
                    plan.getKey(),
                    plan.getName(),
                    plan.getDescription(),
                    plan.getPriceMonthly(),
                    plan.getPriceYearly(),
                    plan.getSeatsLimit(),
                    parseJson(plan.getLimitsJson()),
                    plan.getFeatures() != null ? plan.getFeatures() : List.of(),
                    plan.getIsPublic()
            ));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== QUOTAS ====================

    @GetMapping("/quotas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantQuotaResponse> getQuotas() {
        UUID tenantId = getCurrentTenantId();
        
        return ResponseEntity.ok(new TenantQuotaResponse(
                tenantId,
                getQuotas(tenantId),
                getSubscriptionInfo(tenantId)
        ));
    }

    // ==================== SERVICES PRIVÉS ====================

    private boolean isAiEnabled(UUID tenantId) {
        Optional<TenantSubscription> sub = subscriptionRepository.findByTenantId(tenantId);
        if (sub.isEmpty()) return false;
        
        try {
            Map<String, Object> limits = parseJson(sub.get().getQuotasJson());
            if (limits.containsKey("MAX_AI_REQUESTS")) {
                long remaining = getAiRequestRemaining(tenantId);
                return remaining > 0;
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private long getAiRequestRemaining(UUID tenantId) {
        // Simuler le compteur - dans un système réel, utiliser Redis
        return 100; // Valeur par défaut pour les tests
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, QuotaInfo> getQuotas(UUID tenantId) {
        Map<String, QuotaInfo> quotas = new LinkedHashMap<>();
        
        // Obtenir les limits du plan
        SaasPlan currentPlan = planService.getCurrentPlan(tenantId);
        Map<String, Object> limits = currentPlan != null 
            ? parseJson(currentPlan.getLimitsJson()) 
            : new HashMap<>();

        // Compter l'usage réel
        long userCount = getCurrentUserCount(tenantId);
        long churchCount = getCurrentChurchCount(tenantId);
        
        quotas.put("MAX_USERS", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_USERS", 100L),
                userCount,
                userCount < (Long) limits.getOrDefault("MAX_USERS", 100L)
        ));
        
        quotas.put("MAX_CHURCHES", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_CHURCHES", 10L),
                churchCount,
                churchCount < (Long) limits.getOrDefault("MAX_CHURCHES", 10L)
        ));
        
        quotas.put("MAX_SUB_CHURCHES", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_SUB_CHURCHES", 20L),
                0,
                true
        ));
        
        quotas.put("MAX_CAMPUSES", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_CAMPUSES", 5L),
                0,
                true
        ));
        
        quotas.put("MAX_STORAGE_MB", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_STORAGE_MB", 1024L),
                getCurrentStorageMB(tenantId),
                getCurrentStorageMB(tenantId) < (Long) limits.getOrDefault("MAX_STORAGE_MB", 1024L)
        ));
        
        quotas.put("MAX_AI_REQUESTS", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_AI_REQUESTS", 1000L),
                getAiRequestRemaining(tenantId),
                getAiRequestRemaining(tenantId) > 0
        ));
        
        quotas.put("MAX_ADMINS", new QuotaInfo(
                (Long) limits.getOrDefault("MAX_ADMINS", 10L),
                0,
                true
        ));

        return quotas;
    }

    private long getCurrentUserCount(UUID tenantId) {
        try {
            return org.springframework.data.jpa.repository.support.SimpleJpaRepository
                    .class.cast(null); // Placeholder - implémentation réelle via repository
        } catch (Exception e) {
            return 0;
        }
    }

    private long getCurrentChurchCount(UUID tenantId) {
        return 0; // À implémenter avec OrganizationNodeRepository
    }

    private long getCurrentStorageMB(UUID tenantId) {
        return 0; // À implémenter avec FileStorageService
    }

    private SubscriptionInfo getSubscriptionInfo(UUID tenantId) {
        Optional<TenantSubscription> sub = subscriptionRepository.findByTenantId(tenantId);
        if (sub.isEmpty()) {
            return new SubscriptionInfo("FREE", "Gratuit", null, null, null, null, false, false);
        }
        
        SaasPlan plan = planRepository.findById(sub.get().getPlanKey()).orElse(null);
        return new SubscriptionInfo(
                sub.get().getPlanKey(),
                plan != null ? plan.getName() : "Inconnu",
                plan != null ? plan.getPriceMonthly() : null,
                plan != null ? plan.getPriceYearly() : null,
                sub.get().getCurrentPeriodEnd(),
                sub.get().getCurrentPeriodStart(),
                sub.get().isCancelAtPeriodEnd(),
                sub.get().getStatus() == com.discipolat.modules.tenants.domain.SubscriptionStatus.ACTIVE
        );
    }

    private String getModuleLabel(String key) {
        return switch (key.toUpperCase()) {
            case "DASHBOARD" -> "Tableau de bord";
            case "MEMBERS" -> "Membres";
            case "FAMILIES" -> "Familles";
            case "CHURCHES" -> "Églises";
            case "EVENTS" -> "Événements";
            case "NOTIFICATIONS" -> "Notifications";
            case "MESSAGES" -> "Messagerie";
            case "DOCUMENTS" -> "Documents";
            case "REPORTS" -> "Rapports";
            case "ANALYTICS" -> "Analytics";
            case "DISCIPLESHIP" -> "Discipleship";
            case "ACADEMY" -> "Académie";
            case "AI" -> "IA";
            case "FINANCE" -> "Finances";
            case "MOBILE_MONEY" -> "Mobile Money";
            case "MARKETPLACE" -> "Marketplace";
            case "WHATSLINK" -> "WhatsApp";
            case "COMMUNITY" -> "Communauté";
            case "FORMS" -> "Formulaires";
            case "CALENDAR" -> "Calendrier";
            default -> key;
        };
    }

    // ==================== RECORDS ====================

    public record ModulesConfigResponse(
            UUID tenantId,
            List<ModuleInfo> modules,
            Map<String, QuotaInfo> quotas,
            SubscriptionInfo subscription
    ) {}

    public record ModuleInfo(
            String key,
            String label,
            boolean configured,
            boolean enabled
    ) {}

    public record QuotaInfo(
            long limit,
            long currentUsage,
            boolean available
    ) {}

    public record SubscriptionInfo(
            String planKey,
            String planName,
            Double priceMonthly,
            Double priceYearly,
            Instant periodEnd,
            Instant periodStart,
            boolean cancelAtPeriodEnd,
            boolean active
    ) {}

    public record TenantQuotaResponse(
            UUID tenantId,
            Map<String, QuotaInfo> quotas,
            SubscriptionInfo subscription
    ) {}

    public record PlanInfo(
            UUID id,
            String key,
            String name,
            String description,
            Double priceMonthly,
            Double priceYearly,
            Integer seatsLimit,
            Map<String, Object> limits,
            List<String> features,
            boolean isPublic
    ) {}
}

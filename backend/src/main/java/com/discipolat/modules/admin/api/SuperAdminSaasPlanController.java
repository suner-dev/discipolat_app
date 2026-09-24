package com.discipolat.modules.admin.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/saas/plans")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class SuperAdminSaasPlanController {

    private final SaasPlanRepository planRepository;
    private final SaasPlanService planService;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final AuditService auditService;

    public SuperAdminSaasPlanController(SaasPlanRepository planRepository,
                                         SaasPlanService planService,
                                         TenantSubscriptionRepository subscriptionRepository,
                                         AuditService auditService) {
        this.planRepository = planRepository;
        this.planService = planService;
        this.subscriptionRepository = subscriptionRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<SaasPlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans().stream().map(this::toResponse).toList());
    }

    @GetMapping("/active")
    public ResponseEntity<List<SaasPlanResponse>> getActivePlans() {
        return ResponseEntity.ok(planService.getActivePlans().stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<SaasPlanResponse> createPlan(@RequestBody CreatePlanRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        SaasPlan plan = SaasPlan.builder()
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .priceMonthly(request.priceMonthly())
                .priceYearly(request.priceYearly())
                .currency(request.currency())
                .limitsJson(request.limitsJson())
                .featuresJson(request.featuresJson())
                .isPublic(request.isPublic())
                .seatsLimit(request.seatsLimit())
                .storageLimitMb(request.storageLimitMb())
                .aiCreditsLimit(request.aiCreditsLimit())
                .priceEur(request.priceEur())
                .priceXaf(request.priceXaf())
                .priceUsd(request.priceUsd())
                .billingPeriod(request.billingPeriod())
                .trialDays(request.trialDays())
                .annualDiscountPct(request.annualDiscountPct())
                .regionsJson(request.regionsJson())
                .status(request.status())
                .sortOrder(request.sortOrder())
                .build();
        SaasPlan saved = planRepository.save(plan);
        auditService.logSimpleExternalKey("SAAS_PLAN_CREATED", "SAAS_PLAN", saved.getKey());
        return ResponseEntity.status(201).body(toResponse(saved));
    }

    @PutMapping("/{key}")
    public ResponseEntity<SaasPlanResponse> updatePlan(@PathVariable String key, @RequestBody UpdatePlanRequest request) {
        SaasPlan plan = planRepository.findById(key).orElseThrow(() -> new RuntimeException("Plan not found: " + key));
        if (request.name() != null) plan.setName(request.name());
        if (request.description() != null) plan.setDescription(request.description());
        if (request.priceMonthly() != null) plan.setPriceMonthly(request.priceMonthly());
        if (request.priceYearly() != null) plan.setPriceYearly(request.priceYearly());
        if (request.currency() != null) plan.setCurrency(request.currency());
        if (request.limitsJson() != null) plan.setLimitsJson(request.limitsJson());
        if (request.featuresJson() != null) plan.setFeaturesJson(request.featuresJson());
        if (request.isPublic() != null) plan.setIsPublic(request.isPublic());
        if (request.seatsLimit() != null) plan.setSeatsLimit(request.seatsLimit());
        if (request.storageLimitMb() != null) plan.setStorageLimitMb(request.storageLimitMb());
        if (request.aiCreditsLimit() != null) plan.setAiCreditsLimit(request.aiCreditsLimit());
        if (request.priceEur() != null) plan.setPriceEur(request.priceEur());
        if (request.priceXaf() != null) plan.setPriceXaf(request.priceXaf());
        if (request.priceUsd() != null) plan.setPriceUsd(request.priceUsd());
        if (request.billingPeriod() != null) plan.setBillingPeriod(request.billingPeriod());
        if (request.trialDays() != null) plan.setTrialDays(request.trialDays());
        if (request.annualDiscountPct() != null) plan.setAnnualDiscountPct(request.annualDiscountPct());
        if (request.regionsJson() != null) plan.setRegionsJson(request.regionsJson());
        if (request.status() != null) plan.setStatus(request.status());
        if (request.sortOrder() != null) plan.setSortOrder(request.sortOrder());
        SaasPlan saved = planRepository.save(plan);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deactivatePlan(@PathVariable String key) {
        SaasPlan plan = planRepository.findById(key).orElseThrow(() -> new RuntimeException("Plan not found: " + key));
        plan.setIsActive(false);
        plan.setStatus("INACTIVE");
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(planService.getAllSubscriptions().stream().map(this::toSubscriptionResponse).toList());
    }

    @PostMapping("/{key}/subscribe/{tenantId}")
    public ResponseEntity<SubscriptionResponse> subscribe(@PathVariable String key, @PathVariable UUID tenantId, @RequestParam(defaultValue = "monthly") String billingCycle) {
        UUID userId = TenantContext.getCurrentUserId();
        TenantSubscription sub = planService.subscribe(tenantId, key, billingCycle, userId);
        return ResponseEntity.status(201).body(toSubscriptionResponse(sub));
    }

    @GetMapping("/usage/{tenantId}")
    public ResponseEntity<Map<String, Object>> getUsage(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId,
                "limits", planService.getCurrentPlan(tenantId) != null ? planService.getCurrentPlan(tenantId).getLimitsJson() : null,
                "plan", planService.getCurrentPlan(tenantId) != null ? planService.getCurrentPlan(tenantId).getName() : "Aucun"
        ));
    }

    // DTOs
    public record SaasPlanResponse(String id, String key, String name, String description, Long priceMonthly, Long priceYearly, String currency, String limitsJson, String featuresJson, Boolean isPublic, Integer seatsLimit, Integer storageLimitMb, String modulesIncludedJson, Integer aiCreditsLimit, Long priceEur, Long priceXaf, Long priceUsd, String billingPeriod, Integer trialDays, Integer annualDiscountPct, String regionsJson, String status, Integer sortOrder, Boolean isActive) {}
    public record CreatePlanRequest(String key, String name, String description, Long priceMonthly, Long priceYearly, String currency, String limitsJson, String featuresJson, Boolean isPublic, Integer seatsLimit, Integer storageLimitMb, String modulesIncludedJson, Integer aiCreditsLimit, Long priceEur, Long priceXaf, Long priceUsd, String billingPeriod, Integer trialDays, Integer annualDiscountPct, String regionsJson, String status, Integer sortOrder) {}
    public record UpdatePlanRequest(String name, String description, Long priceMonthly, Long priceYearly, String currency, String limitsJson, String featuresJson, Boolean isPublic, Integer seatsLimit, Integer storageLimitMb, String modulesIncludedJson, Integer aiCreditsLimit, Long priceEur, Long priceXaf, Long priceUsd, String billingPeriod, Integer trialDays, Integer annualDiscountPct, String regionsJson, String status, Integer sortOrder) {}
    public record SubscriptionResponse(String id, UUID tenantId, String planKey, String planName, String billingCycle, String status, Boolean cancelAtPeriodEnd, String periodStart, String periodEnd) {}

    private SaasPlanResponse toResponse(SaasPlan p) {
        return new SaasPlanResponse(p.getKey(), p.getKey(), p.getName(), p.getDescription(), p.getPriceMonthly(), p.getPriceYearly(), p.getCurrency(), p.getLimitsJson(), p.getFeaturesJson(), p.getIsPublic(), p.getSeatsLimit(), p.getStorageLimitMb(), p.getModulesIncludedJson(), p.getAiCreditsLimit(), p.getPriceEur(), p.getPriceXaf(), p.getPriceUsd(), p.getBillingPeriod(), p.getTrialDays(), p.getAnnualDiscountPct(), p.getRegionsJson(), p.getStatus(), p.getSortOrder(), p.getIsActive());
    }

    private SubscriptionResponse toSubscriptionResponse(TenantSubscription s) {
        SaasPlan p = planRepository.findById(s.getPlanKey()).orElse(null);
        return new SubscriptionResponse(s.getId() != null ? s.getId().toString() : null, s.getTenantId(), s.getPlanKey(), p != null ? p.getName() : "Unknown", s.getBillingCycle(), s.getStatus() != null ? s.getStatus().name() : "UNKNOWN", s.getCancelAtPeriodEnd(), s.getCurrentPeriodStart() != null ? s.getCurrentPeriodStart().toString() : null, s.getCurrentPeriodEnd() != null ? s.getCurrentPeriodEnd().toString() : null);
    }
}
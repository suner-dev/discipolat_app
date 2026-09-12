package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/admin/subscription")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  SaasPlanRepository planRepository,
                                  TenantSubscriptionRepository subscriptionRepository) {
        this.subscriptionService = subscriptionService;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentSubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(subscriptionService.getSubscriptionDetails(tenantId));
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailablePlans() {
        List<Map<String, Object>> plans = planRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("key", p.getKey());
                    map.put("name", p.getName());
                    map.put("description", p.getDescription());
                    map.put("priceMonthly", p.getPriceMonthly());
                    map.put("priceYearly", p.getPriceYearly());
                    map.put("currency", p.getCurrency());
                    map.put("limits", parseJson(p.getLimitsJson()));
                    map.put("features", parseJson(p.getFeaturesJson()));
                    map.put("isActive", p.getIsActive());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @RequestBody Map<String, String> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        String planKey = request.get("planKey");
        String billingCycle = request.getOrDefault("billingCycle", "monthly");

        TenantSubscription subscription = subscriptionService.subscribe(
                tenantId, planKey, billingCycle, currentUserId);

        return ResponseEntity.ok(Map.of(
                "subscriptionId", subscription.getId().toString(),
                "planKey", subscription.getPlanKey(),
                "status", subscription.getStatus().name(),
                "message", "Abonnement créé avec succès"
        ));
    }

    @PostMapping("/change-plan")
    public ResponseEntity<Map<String, Object>> changePlan(
            @RequestBody Map<String, String> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        String newPlanKey = request.get("planKey");

        TenantSubscription subscription = subscriptionService.changePlan(
                tenantId, newPlanKey, currentUserId);

        return ResponseEntity.ok(Map.of(
                "subscriptionId", subscription.getId().toString(),
                "planKey", subscription.getPlanKey(),
                "status", subscription.getStatus().name(),
                "message", "Changement de plan planifié pour la prochaine période"
        ));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelSubscription(
            @RequestBody(required = false) Map<String, Boolean> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();
        boolean atPeriodEnd = request != null && Boolean.TRUE.equals(request.get("atPeriodEnd"));

        if (atPeriodEnd) {
            subscriptionService.cancelAtPeriodEnd(tenantId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "Abonnement annulé à la fin de la période"));
        } else {
            subscriptionService.cancelImmediately(tenantId, currentUserId);
            return ResponseEntity.ok(Map.of("message", "Abonnement annulé immédiatement"));
        }
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateSubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = com.discipolat.common.infrastructure.security.SecurityUtils.getCurrentUserId();

        subscriptionService.reactivate(tenantId, currentUserId);
        
        return ResponseEntity.ok(Map.of("message", "Abonnement réactivé"));
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsage() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(subscriptionService.getSubscriptionDetails(tenantId));
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
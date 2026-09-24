package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/** Catalogue SaaS minimal nécessaire à la page de tarifs publique. */
@RestController
@RequestMapping("/api/v1/public/plans")
public class PublicSaasPlanController {

    private final SaasPlanService planService;

    public PublicSaasPlanController(SaasPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PublicPlanResponse>> getPublicPlans() {
        List<SaasPlan> plans = planService.getPublicPlans().stream()
                .filter(plan -> SaasPlanService.PUBLIC_PLAN_KEYS.contains(normalize(plan.getKey())))
                .sorted(Comparator.comparingInt(plan -> SaasPlanService.PUBLIC_PLAN_KEYS.indexOf(normalize(plan.getKey()))))
                .toList();
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic().mustRevalidate();
        return ResponseEntity.ok().cacheControl(cacheControl).body(plans.stream().map(this::toResponse).toList());
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private PublicPlanResponse toResponse(SaasPlan plan) {
        return new PublicPlanResponse(
                plan.getKey(), plan.getName(), plan.getDescription(), plan.getPriceMonthly(),
                plan.getPriceYearly(), plan.getCurrency(), plan.getPriceEur(), plan.getPriceXaf(),
                plan.getPriceUsd(), plan.getSeatsLimit(), plan.getStorageLimitMb(),
                plan.getAiCreditsLimit(), plan.getBillingPeriod(), plan.getTrialDays(),
                plan.getAnnualDiscountPct(), plan.getSortOrder()
        );
    }

    public record PublicPlanResponse(
            String key,
            String name,
            String description,
            Long priceMonthly,
            Long priceYearly,
            String currency,
            Long priceEur,
            Long priceXaf,
            Long priceUsd,
            Integer seatsLimit,
            Integer storageLimitMb,
            Integer aiCreditsLimit,
            String billingPeriod,
            Integer trialDays,
            Integer annualDiscountPct,
            Integer sortOrder
    ) { }
}

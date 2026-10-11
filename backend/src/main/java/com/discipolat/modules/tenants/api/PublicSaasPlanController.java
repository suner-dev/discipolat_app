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

/**
 * Catalogue SaaS minimal nécessaire à la page de tarifs publique.
 *
 * <p><b>Endpoint conservé tel quel</b> pour ne casser aucun client existant : les
 * champs d'origine ({@code priceMonthly}, {@code priceEur}, {@code priceXaf},
 * {@code priceUsd}…) répondent toujours, avec leur sémantique historique de
 * montants <b>majeurs</b>. Les prix résolus en unités mineures, multi-devises et
 * multi-régions sont servis par {@link PublicPricingController}.</p>
 *
 * <p><b>Correction de la faille F3</b> : le filtre sur
 * {@code SaasPlanService.PUBLIC_PLAN_KEYS} était appliqué ici <em>et</em> dans
 * {@code SaasPlanService.getPublicPlans()}. Conséquence : tout plan créé par le
 * super-admin avec {@code is_public = true} était invisible de la page publique,
 * quelle que soit sa configuration. Le seul critère retenu est désormais
 * « publié, actif, statut non déprécié » ; le tri conserve la position des plans
 * canoniques et place les plans ajoutés ensuite.</p>
 */
@RestController
@RequestMapping("/api/v1/public/plans")
public class PublicSaasPlanController {

    private final SaasPlanService planService;

    public PublicSaasPlanController(SaasPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<PublicPlanResponse>> getPublicPlans() {
        // Les plans canoniques gardent leur position historique ; les plans ajoutés
        // par le super-admin viennent ensuite, classés par ordre puis par clé.
        Comparator<SaasPlan> order = Comparator
                .comparingInt((SaasPlan plan) -> {
                    int index = SaasPlanService.PUBLIC_PLAN_KEYS.indexOf(normalize(plan.getKey()));
                    return index >= 0 ? index : SaasPlanService.PUBLIC_PLAN_KEYS.size();
                })
                .thenComparingInt(plan -> plan.getSortOrder() == null ? 0 : plan.getSortOrder())
                .thenComparing(plan -> normalize(plan.getKey()));

        List<SaasPlan> plans = planService.getPublicPlans().stream()
                .sorted(order)
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

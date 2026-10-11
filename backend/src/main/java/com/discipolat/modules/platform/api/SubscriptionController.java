package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.currency.domain.CurrencyConfig;
import com.discipolat.modules.currency.domain.CurrencyService;
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
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/v1/admin/subscription")
@PreAuthorize("@authz.isTenantAdmin()")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final PricingCatalogService catalogService;
    private final CurrencyService currencyService;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  SaasPlanRepository planRepository,
                                  TenantSubscriptionRepository subscriptionRepository,
                                  PricingCatalogService catalogService,
                                  CurrencyService currencyService) {
        this.subscriptionService = subscriptionService;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.catalogService = catalogService;
        this.currencyService = currencyService;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentSubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(subscriptionService.getSubscriptionDetails(tenantId));
    }

    /**
     * Catalogue des plans pour l'admin de l'église.
     *
     * <p>Les montants historiques ({@code priceMonthly}, {@code priceYearly},
     * {@code currency}) restent là pour compatibilité, mais deux champs
     * <b>complétés</b> sont ajoutés : {@code prices} (catalogue résolu, unités
     * mineures, multi-devises) et {@code display} (le prix applicable à ce
     * tenant). Sans cela, l'admin verrait un montant différent du visiteur —
     * deux vérités sur le même tarif, ce qui est la pire façon de traiter de
     * l'argent.</p>
     *
     * <p>La devise d'affichage suit la devise primaire du tenant quand elle est
     * autorisée, sinon celle du catalogue : un administrateur camerounais doit
     * lire « en FCFA », pas « en euros ».</p>
     */
    @GetMapping("/plans")
    @PreAuthorize("@authz.isTenantAdmin()")
    public ResponseEntity<List<Map<String, Object>>> getAvailablePlans() {
        String displayCurrency = resolveTenantDisplayCurrency();

        PricingCatalogService.PublicCatalog catalog =
                catalogService.publicCatalog(displayCurrency, null, null);

        List<Map<String, Object>> plans = catalog.plans().stream().map(publicPlan -> {
            SaasPlan p = publicPlan.plan();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", p.getKey());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            // Champs historiques conservés (montants MAJEURS).
            map.put("priceMonthly", p.getPriceMonthly());
            map.put("priceYearly", p.getPriceYearly());
            map.put("currency", p.getCurrency());
            // Catalogue résolu : la source de vérité des montants.
            map.put("prices", publicPlan.prices().stream().map(price -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("currency", price.currency());
                entry.put("cycle", price.cycle().name());
                entry.put("amountMinor", price.amountMinor());
                entry.put("amount", price.amount());
                entry.put("decimals", price.decimals());
                entry.put("region", price.regionCode());
                entry.put("selectable", price.selectable());
                entry.put("channels", price.channels().stream().map(Enum::name).toList());
                entry.put("checkoutCurrency", price.checkoutCurrency());
                return entry;
            }).toList());
            map.put("display", publicPlan.display() == null ? null
                    : Map.of(
                    "available", true,
                    "amountMinor", publicPlan.display().amountMinor(),
                    "amount", publicPlan.display().amount(),
                    "currency", publicPlan.display().currency(),
                    "decimals", publicPlan.display().decimals(),
                    "cycle", publicPlan.display().cycle().name(),
                    "channels", publicPlan.display().channels().stream().map(Enum::name).toList()));
            map.put("limits", parseJson(p.getLimitsJson()));
            map.put("features", parseJson(p.getFeaturesJson()));
            map.put("isActive", p.getIsActive());
            return map;
        }).toList();
        return ResponseEntity.ok(plans);
    }

    /**
     * Devise d'affichage de l'admin : la devise primaire du tenant si le
     * catalogue l'autorise, sinon la devise par défaut de la plateforme.
     *
     * <p>Un administrateur camerounais doit lire « en FCFA ». Lui montrer « en
     * euros » serait exact mais trompeur, et l'exactitude perçue compte autant
     * que l'exactitude réelle quand on parle d'argent.</p>
     */
    private String resolveTenantDisplayCurrency() {
        try {
            CurrencyConfig primary = currencyService.getPrimaryCurrency();
            return catalogService.resolveDisplayCurrency(primary.getCurrencyCode());
        } catch (RuntimeException unavailable) {
            // Devise du tenant illisible : on ne bloque pas l'écran pour autant.
            return catalogService.resolveDisplayCurrency(null);
        }
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
package com.discipolat.modules.payments.stripe;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.domain.PricingCatalogService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Facturation SaaS Stripe : création de sessions Checkout (abonnement) et
 * Portail client (moyens de paiement, factures, changement de plan).
 *
 * <p>Service fail-closed : sans clé configurée, chaque appel renvoie une
 * erreur métier explicite (jamais un appel réseau silencieux).</p>
 *
 * <p><b>Les prix sont lus par {@link PricingCatalogService} uniquement.</b> Cette
 * classe ne lit plus {@code SaasPlan.priceMonthly}/{@code priceYearly} : ces
 * colonnes sont des montants <em>majeurs</em> hérités d'une époque sans contrat
 * d'unité, et les passer telles quelles à {@code unit_amount} — qui attend des
 * centimes — faisait débiter 0,29 € au lieu de 29 € (faille F1). Le catalogue
 * {@code saas_plan_prices} stocke la valeur exacte en unités mineures.</p>
 *
 * <p>Deux modes de prix, dans cet ordre :</p>
 * <ol>
 *   <li>un {@code stripe_price_id} rattaché au prix de catalogue (Price créé par
 *       {@link StripePriceSyncService} ou saisi manuellement) ;</li>
 *   <li>sinon un prix inline, <b>uniquement</b> si la devise de débit accepte les
 *       centimes — Stripe refuse les devises sans unité mineure.</li>
 * </ol>
 */
@Service
public class StripeBillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    /** Devises à 2 décimales autorisées pour les prix en ligne (unit_amount en centimes). */
    private static final Set<String> INLINE_PRICE_CURRENCIES = Set.of("eur", "usd", "cad", "gbp", "chf", "cny", "jpy");

    private final StripeBillingProperties props;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final PricingCatalogService pricingCatalogService;
    private final Iso4217CurrencyValidator currencyValidator;

    public StripeBillingService(StripeBillingProperties props,
                                SaasPlanRepository planRepository,
                                TenantSubscriptionRepository subscriptionRepository,
                                ObjectProvider<PricingCatalogService> pricingCatalogService,
                                Iso4217CurrencyValidator currencyValidator) {
        this.props = props;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pricingCatalogService = pricingCatalogService.getIfAvailable();
        this.currencyValidator = currencyValidator;
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    /**
     * Crée une session Checkout « subscription » pour le tenant.
     *
     * @return {url, sessionId} — url = redirection HTTPS vers Stripe Checkout.
     */
    public Map<String, Object> createCheckoutSession(UUID tenantId, UUID userId,
                                                     String planKey, String billingCycle,
                                                     String displayCurrency, String regionCode) {
        requireEnabled();
        String cycle = "yearly".equalsIgnoreCase(billingCycle) ? "yearly" : "monthly";
        String canonicalKey = TenantPlanPolicy.canonicalizePlanKey(planKey);
        if (canonicalKey == null) {
            throw new BusinessRuleException("Plan inconnu: " + planKey, "PLAN_NOT_FOUND");
        }
        SaasPlan plan = planRepository.findByKeyIgnoreCaseAndIsActiveTrue(canonicalKey)
                .orElseThrow(() -> new BusinessRuleException("Plan inconnu: " + planKey, "PLAN_NOT_FOUND"));

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(tenantId.toString())
                .setSuccessUrl(props.getSuccessUrl())
                .setCancelUrl(props.getCancelUrl())
                .putAllMetadata(metadata(tenantId, userId, plan.getKey(), cycle));

        String legacyPriceId = "yearly".equals(cycle)
                ? plan.getStripePriceIdYearly() : plan.getStripePriceIdMonthly();

        if (legacyPriceId != null && !legacyPriceId.isBlank()) {
            // Price hérité posé sur le plan : il porte son propre montant. On ne
            // le recalcule surtout pas ici, au risque de facturer autre chose que
            // ce que Stripe a enregistré à l'abonnement.
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(legacyPriceId)
                    .setQuantity(1L)
                    .build());
        } else {
            // ── Faille F1 : lecture du catalogue unitaire, en unités mineures ──
            PricingCatalogService.ResolvedPrice resolved =
                    resolveCatalogPrice(plan, BillingCycle.fromString(cycle), displayCurrency, regionCode);
            String currency = resolved.checkoutCurrency();
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency.toLowerCase(Locale.ROOT))
                            // CONTRAT §2.1 : amountMinor est DÉJÀ en unités mineures.
                            // Aucun ×100 ici — c'était exactement ce qui débitait
                            // 0,29 € pour un plan affiché à 29 €.
                            .setUnitAmount(resolved.amountMinor())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(plan.getName())
                                    .setDescription(plan.getDescription())
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams.SubscriptionData.Builder subData = SessionCreateParams.SubscriptionData.builder()
                .putAllMetadata(metadata(tenantId, userId, plan.getKey(), cycle));
        if (plan.getTrialDays() != null && plan.getTrialDays() > 0) {
            subData.setTrialPeriodDays(plan.getTrialDays().longValue());
        }
        builder.setSubscriptionData(subData.build());

        try {
            Session session = Session.create(builder.build(), requestOptions());
            log.info("[Stripe] session checkout créée — tenant={} plan={} cycle={} session={}",
                    tenantId, plan.getKey(), cycle, session.getId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", session.getUrl());
            result.put("sessionId", session.getId());
            return result;
        } catch (StripeException e) {
            log.error("[Stripe] échec création session checkout — tenant={}", tenantId, e);
            throw new BusinessRuleException("Stripe indisponible: " + e.getMessage(), "STRIPE_ERROR");
        }
    }

    /**
     * Résout le prix unitaire applicable, en refusant explicitement toute
     * situation où Stripe ne sait pas chiffrer.
     *
     * <p>On ne retombe jamais sur {@code SaasPlan.priceMonthly} : ce sont des
     * montants majeurs sans contrat d'unité, et leur réintroduction ici
     * reproduirait la facturation 100 fois trop basse (faille F1).</p>
     */
    private PricingCatalogService.ResolvedPrice resolveCatalogPrice(SaasPlan plan, BillingCycle cycle,
                                                                    String displayCurrency, String regionCode) {
        if (pricingCatalogService == null) {
            throw new BusinessRuleException(
                    "Le catalogue de tarifs n'est pas disponible", "PRICING_CATALOG_UNAVAILABLE");
        }
        BillingCycle resolvedCycle = cycle;
        String resolvedCurrency = displayCurrency;
        PricingCatalogService.ResolvedPrice resolved = null;
        if (resolvedCurrency != null && !resolvedCurrency.isBlank()) {
            resolved = pricingCatalogService.resolvePrice(plan.getKey(), resolvedCurrency, resolvedCycle, regionCode)
                    .orElse(null);
        }
        if (resolved == null) {
            // Devise non précisée ou sans prix : on tente le plan dans la devise
            // du plan, puis dans la devise de base du catalogue.
            resolvedCurrency = plan.getCurrency();
            resolved = pricingCatalogService.resolvePrice(plan.getKey(), resolvedCurrency, resolvedCycle, regionCode)
                    .orElse(null);
        }
        if (resolved == null) {
            throw new BusinessRuleException(
                    "Aucun prix configuré pour le plan " + plan.getKey() + " (" + resolvedCycle.name().toLowerCase(Locale.ROOT) + ")",
                    "PLAN_PRICE_MISSING");
        }
        String checkoutCurrency = resolved.checkoutCurrency();
        if (checkoutCurrency == null || checkoutCurrency.isBlank()) {
            throw new BusinessRuleException(
                    "Devise de débit absente pour le plan " + plan.getKey(), "PLAN_PRICE_MISSING");
        }
        String lower = checkoutCurrency.toLowerCase(Locale.ROOT);
        if (!INLINE_PRICE_CURRENCIES.contains(lower)) {
            // Stripe refuse price_data inline pour les devises sans unité mineure.
            // Le Price doit exister : le super-admin le déclenche depuis
            // /platform/saas/plans, ou le batch le crée au prochain passage.
            throw new BusinessRuleException(
                    "Le plan " + plan.getKey() + " est tarifé en " + checkoutCurrency
                            + ", devise sans centimes : un Price Stripe doit être créé avant le checkout"
                            + " (synchronisation automatique ou saisie manuelle dans l'espace d'administration)",
                    "STRIPE_PRICE_ID_REQUIRED");
        }
        return resolved;
    }

    /**
     * Crée une session Portail client (gestion moyen de paiement / factures /
     * résiliation). Nécessite un client Stripe déjà associé à l'abonnement.
     */
    public Map<String, Object> createPortalSession(UUID tenantId) {
        requireEnabled();
        Optional<TenantSubscription> sub = subscriptionRepository.findCurrentByTenantId(tenantId);
        String customerId = sub.map(TenantSubscription::getStripeCustomerId).orElse(null);
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleException(
                    "Aucun compte client Stripe associé — souscrivez d'abord un plan payant",
                    "NO_STRIPE_CUSTOMER");
        }
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(customerId)
                        .setReturnUrl(props.getSuccessUrl())
                        .build();
        try {
            com.stripe.model.billingportal.Session portal =
                    com.stripe.model.billingportal.Session.create(params, requestOptions());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", portal.getUrl());
            return result;
        } catch (StripeException e) {
            log.error("[Stripe] échec création portail — tenant={}", tenantId, e);
            throw new BusinessRuleException("Stripe indisponible: " + e.getMessage(), "STRIPE_ERROR");
        }
    }

    RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(props.getSecretKey()).build();
    }

    private Map<String, String> metadata(UUID tenantId, UUID userId, String planKey, String cycle) {
        Map<String, String> md = new LinkedHashMap<>();
        md.put("tenant_id", tenantId.toString());
        if (userId != null) {
            md.put("user_id", userId.toString());
        }
        md.put("plan_key", planKey);
        md.put("billing_cycle", cycle);
        return md;
    }

    private void requireEnabled() {
        if (!props.isEnabled()) {
            throw new BusinessRuleException(
                    "La facturation Stripe n'est pas activée (STRIPE_SECRET_KEY absente)",
                    "STRIPE_DISABLED");
        }
    }
}

package com.discipolat.modules.payments.stripe;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * erreur métier explicite (jamais un appel réseau silencieux). Les prix sont
 * lus depuis {@code saas_plans} : price_id Stripe si configuré, sinon prix
 * en ligne pour les devises à 2 décimales uniquement (EUR/USD) — les devises
 * 0-décimales (XAF…) exigent un price_id créé dans le tableau de bord Stripe.</p>
 */
@Service
public class StripeBillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    /** Devises à 2 décimales autorisées pour les prix en ligne (unit_amount en centimes). */
    private static final Set<String> INLINE_PRICE_CURRENCIES = Set.of("eur", "usd");

    private final StripeBillingProperties props;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;

    public StripeBillingService(StripeBillingProperties props,
                                SaasPlanRepository planRepository,
                                TenantSubscriptionRepository subscriptionRepository) {
        this.props = props;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
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
                                                     String planKey, String billingCycle) {
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

        String priceId = "yearly".equals(cycle)
                ? plan.getStripePriceIdYearly() : plan.getStripePriceIdMonthly();
        if (priceId != null && !priceId.isBlank()) {
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build());
        } else {
            Long amount = "yearly".equals(cycle) ? plan.getPriceYearly() : plan.getPriceMonthly();
            String currency = plan.getCurrency() == null ? "" : plan.getCurrency().toLowerCase(Locale.ROOT);
            if (amount == null || amount <= 0) {
                throw new BusinessRuleException(
                        "Aucun prix configuré pour le plan " + plan.getKey() + " (" + cycle + ")",
                        "PLAN_PRICE_MISSING");
            }
            if (!INLINE_PRICE_CURRENCIES.contains(currency)) {
                throw new BusinessRuleException(
                        "Le plan " + plan.getKey() + " (devise " + currency.toUpperCase(Locale.ROOT)
                                + ") exige un stripe_price_id_" + cycle + " créé dans le tableau de bord Stripe",
                        "STRIPE_PRICE_ID_REQUIRED");
            }
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(amount)
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

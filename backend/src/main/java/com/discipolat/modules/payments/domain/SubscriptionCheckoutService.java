package com.discipolat.modules.payments.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.tenants.domain.PricingCatalogService;
import com.discipolat.modules.tenants.domain.PricingRegionResolver;
import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SubscriptionService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Passage en caisse unifié des ABONNEMENTS SaaS — OM / MoMo / Wave / Visa.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité T6 (raccordement CinetPay).
 *
 * <p><b>Problème résolu.</b> Jusqu'ici l'abonnement en ligne ne savait ouvrir
 * qu'une caisse Stripe ({@code StripeBillingController}). Un prospect africain
 * réglé par Orange Money, MTN MoMo ou Wave, ou par carte Visa hors Stripe,
 * restait sans issue. Or l'agrégateur CinetPay, fusionné dans develop, sait
 * encaisser ces quatre canaux derrière une seule redirection CPS. Ce service
 * est le <em>chemin</em> abonnement → caisse CinetPay → activation.</p>
 *
 * <p><b>Un seul chemin, deux phases.</b> {@link #checkout} résout le prix
 * régional exact, vérifie que le canal est réellement disponible (jamais un
 * bouton mort), pose une {@link PaymentIntent} payée via la passerelle et
 * enregistre une {@link SaasSubscriptionCheckout} de corrélation. La
 * confirmation n'arrive PAS ici : elle arrive au webhook, qui rappelle
 * {@link #confirmActivation} pour activer l'abonnement. Séparer l'ouverture de
 * la caisse de son règlement est ce qui rend le flux fiable — le payeur peut
 * fermer l'onglet, la vérité du « payé » vient de l'API prestataire.</p>
 *
 * <p><b>Fail-safe.</b> Si aucun prestataire capable d'encaisser le canal choisi
 * n'est branché (CinetPay coupé ET Stripe coupé pour la carte, aucun opérateur
 * mobile pour MoMo), on refuse avant d'ouvrir une caisse : mieux vaut un
 * message honnête qu'un paiement promis et impossible. Le tarif facturé est
 * celui résolu pour la région du tenant, figé sur la corrélation — une hausse
 * décidée pendant que le payeur est sur la page CPS ne change pas sa facture.</p>
 */
@Service
@Transactional
public class SubscriptionCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCheckoutService.class);

    private final PricingCatalogService catalogService;
    private final PricingSettingsService settingsService;
    private final PricingRegionResolver regionResolver;
    private final PaymentChannelResolver channelResolver;
    private final PaymentGatewayService gatewayService;
    private final MobileMoneyProviderRegistry providerRegistry;
    private final SaasSubscriptionCheckoutRepository checkoutRepository;
    private final SubscriptionService subscriptionService;
    private final TenantRepository tenantRepository;

    public SubscriptionCheckoutService(PricingCatalogService catalogService,
                                       PricingSettingsService settingsService,
                                       PricingRegionResolver regionResolver,
                                       PaymentChannelResolver channelResolver,
                                       PaymentGatewayService gatewayService,
                                       MobileMoneyProviderRegistry providerRegistry,
                                       SaasSubscriptionCheckoutRepository checkoutRepository,
                                       SubscriptionService subscriptionService,
                                       TenantRepository tenantRepository) {
        this.catalogService = catalogService;
        this.settingsService = settingsService;
        this.regionResolver = regionResolver;
        this.channelResolver = channelResolver;
        this.gatewayService = gatewayService;
        this.providerRegistry = providerRegistry;
        this.checkoutRepository = checkoutRepository;
        this.subscriptionService = subscriptionService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Ouvre une caisse d'abonnement et retourne la redirection prestataire.
     *
     * @param tenantId  tenant qui souscrit (contexte authentifié)
     * @param userId    auteur de la souscription
     * @param planKey   plan demandé (STARTUP / GROWTH / NETWORK)
     * @param billingCycle mensuel ou annuel (chaîne libre, normalisée)
     * @param currency  devise souhaitée, ou {@code null} pour la devise par défaut de la région
     * @param regionHint région forcée (affichage only), ou {@code null} pour la région du tenant
     * @param channel   canal demandé : {@code CARD} (Visa/MC) ou {@code MOMO} (OM/MoMo/Wave)
     * @param operator  opérateur mobile précis ({@code ORANGE_MONEY}/{@code MTN_MOMO}/{@code WAVE}),
     *                  ou {@code null} pour laisser choisir le premier disponible
     * @param phone     MSISDN du payeur (utile pour MoMo, optionnel pour la carte)
     */
    public Map<String, Object> checkout(UUID tenantId, UUID userId, String planKey, String billingCycle,
                                        String currency, String regionHint, String channel,
                                        String operator, String phone) {
        if (planKey == null || planKey.isBlank()) {
            throw new BusinessRuleException("Plan manquant pour l'abonnement", "SUBSCRIPTION_PLAN_MISSING");
        }
        PaymentChannel requested = parseChannel(channel);

        // 1) Région : celle du tenant (pays) sauf forçage d'affichage autorisé.
        String tenantCountry = tenantRepository.findById(tenantId)
                .map(Tenant::getCountry).orElse(null);
        String region = regionResolver.resolve(null, regionHint, tenantCountry)
                .orElseGet(() -> settingsService.current().getDefaultRegion());

        // 2) Devise : celle demandée si permise, sinon la devise de la région/plateforme.
        BillingCycle cycle = BillingCycle.fromString(billingCycle);
        String effectiveCurrency = catalogService.resolveDisplayCurrency(currency);

        // 3) Prix régional exact (repli GLOBAL inclus dans le résolveur).
        Optional<PricingCatalogService.ResolvedPrice> resolved =
                catalogService.resolvePriceRespectingPolicy(planKey, effectiveCurrency, cycle, region);
        if (resolved.isEmpty()) {
            throw new BusinessRuleException(
                    "Aucun tarif disponible pour ce plan (" + planKey + ", " + effectiveCurrency + ", " + region + ")",
                    "SUBSCRIPTION_PRICE_MISSING");
        }
        PricingCatalogService.ResolvedPrice price = resolved.get();

        // 4) Le canal demandé est-il RÉELLEMENT encaissable ? (jamais de CTA mort)
        List<PaymentChannel> available = channelResolver.available(
                price.currency(), price.checkoutCurrency(), price.stripePriceId(),
                price.stripePriceStatus(), price.channels());
        if (!available.contains(requested)) {
            throw new BusinessRuleException(
                    "Le canal " + requested.name() + " n'est pas disponible pour ce tarif. Canaux ouverts : "
                            + (available.isEmpty() ? "aucun" : available.stream().map(Enum::name).toList()),
                    "SUBSCRIPTION_CHANNEL_UNAVAILABLE");
        }

        // 5) Routage canal → opérateur prestataire.
        PaymentIntent.Operator intentOperator = resolveOperator(requested, operator);

        // 6) Intention de paiement + appel prestataire (CinetPay renvoie l'URL CPS).
        PaymentIntent intent = PaymentIntent.builder()
                .userId(userId)
                .operator(intentOperator)
                .phoneNumber(phone)
                .amount(new BigDecimal(price.amount()))
                .currency(price.currency())
                .purpose(PaymentIntent.Purpose.SAAS_SUBSCRIPTION)
                .status(PaymentIntent.Status.PENDING)
                .build();
        PaymentIntent initiated = gatewayService.initiate(intent);

        // 7) Corrélation : qui paie quoi, pour activer à la confirmation.
        SaasSubscriptionCheckout checkout = SaasSubscriptionCheckout.builder()
                .tenantId(initiated.getTenantId() != null ? initiated.getTenantId() : tenantId)
                .userId(userId)
                .planKey(price.planKey())
                .billingCycle(price.cycle().name())
                .regionCode(price.regionCode())
                .currency(price.currency())
                .amountMinor(price.amountMinor())
                .paymentChannel(requested.name())
                .operator(intentOperator.name())
                .paymentIntentId(initiated.getId())
                .providerReference(initiated.getProviderReference())
                .checkoutUrl(initiated.getCheckoutUrl())
                .status(SubscriptionCheckoutStatus.PENDING)
                .build();
        SaasSubscriptionCheckout saved = checkoutRepository.save(checkout);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkoutId", saved.getId().toString());
        result.put("intentId", initiated.getId() == null ? null : initiated.getId().toString());
        result.put("providerReference", initiated.getProviderReference());
        result.put("checkoutUrl", initiated.getCheckoutUrl());
        result.put("providerName", initiated.getProviderName());
        result.put("instructions", initiated.getInstructions());
        result.put("planKey", price.planKey());
        result.put("billingCycle", price.cycle().name());
        result.put("currency", price.currency());
        result.put("amountMinor", price.amountMinor());
        result.put("amount", price.amount());
        result.put("region", price.regionCode());
        result.put("channel", requested.name());
        result.put("operator", intentOperator.name());
        result.put("status", saved.getStatus().name());
        log.info("[SubCheckout] caisse ouverte tenant={} plan={} canal={} ref={} url={}",
                tenantId, price.planKey(), requested.name(), initiated.getProviderReference(),
                initiated.getCheckoutUrl() != null);
        return result;
    }

    /**
     * Confirme une caisse au webhook : payé → abonnement activé, idempotent.
     *
     * <p>Appelé quand la passerelle a marqué l'intention CONFIRMED après
     * re-validation auprès du prestataire. Une caisse déjà ACTIVE n'est pas
     * réactivée (les re-jeux de notify sont sans effet de bord).</p>
     */
    @Transactional
    public boolean confirmActivation(String providerReference) {
        Optional<SaasSubscriptionCheckout> found = checkoutRepository.findByProviderReference(providerReference);
        if (found.isEmpty()) {
            // Intention qui n'est pas un abonnement (dîme/offrande) : rien à activer.
            return false;
        }
        SaasSubscriptionCheckout checkout = found.get();
        if (checkout.getStatus() == SubscriptionCheckoutStatus.ACTIVE) {
            return true; // idempotent
        }
        checkout.setStatus(SubscriptionCheckoutStatus.ACTIVE);
        checkoutRepository.save(checkout);

        // Figeage respecté : on active au plan/cycle souscrits, pas au catalogue du jour.
        String priceSource = "MOMO".equals(checkout.getPaymentChannel()) ? "MOMO" : checkout.getPaymentChannel();
        subscriptionService.subscribePaid(checkout.getTenantId(), checkout.getPlanKey(),
                checkout.getBillingCycle(), checkout.getUserId(), priceSource);
        log.info("[SubCheckout] abonnement ACTIVÉ tenant={} plan={} cycle={} ref={}",
                checkout.getTenantId(), checkout.getPlanKey(), checkout.getBillingCycle(), providerReference);
        return true;
    }

    /** Marque une caisse échouée (refus prestataire) — exploitée par un future reconcile. */
    @Transactional
    public void markFailed(String providerReference, String reason) {
        checkoutRepository.findByProviderReference(providerReference).ifPresent(checkout -> {
            if (checkout.getStatus() == SubscriptionCheckoutStatus.PENDING) {
                checkout.setStatus(SubscriptionCheckoutStatus.FAILED);
                checkoutRepository.save(checkout);
                log.warn("[SubCheckout] caisse échouée ref={} : {}", providerReference, reason);
            }
        });
    }

    /** État d'une caisse, pour le polling de la face avant/mobile après la redirection. */
    @Transactional(readOnly = true)
    public Map<String, Object> statusByReference(String providerReference, UUID callerTenantId) {
        SaasSubscriptionCheckout checkout = checkoutRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new com.discipolat.common.domain.EntityNotFoundException(
                        "SubscriptionCheckout", "providerReference", providerReference));
        // Anti-IDOR : on ne divulgue une caisse qu'au tenant qui l'a ouverte.
        if (callerTenantId != null && !callerTenantId.equals(checkout.getTenantId())) {
            throw new com.discipolat.common.domain.EntityNotFoundException(
                    "SubscriptionCheckout", "providerReference", providerReference);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("checkoutId", checkout.getId().toString());
        body.put("planKey", checkout.getPlanKey());
        body.put("billingCycle", checkout.getBillingCycle());
        body.put("currency", checkout.getCurrency());
        body.put("amountMinor", checkout.getAmountMinor());
        body.put("channel", checkout.getPaymentChannel());
        body.put("operator", checkout.getOperator());
        body.put("status", checkout.getStatus().name());
        body.put("activated", checkout.getStatus() == SubscriptionCheckoutStatus.ACTIVE);
        return body;
    }

    /* ------------------------------ Internes ------------------------------ */

    private PaymentChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new BusinessRuleException("Canal de paiement manquant (CARD ou MOMO)",
                    "SUBSCRIPTION_CHANNEL_MISSING");
        }
        PaymentChannel parsed;
        try {
            parsed = PaymentChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new BusinessRuleException("Canal inconnu : " + channel, "SUBSCRIPTION_CHANNEL_UNKNOWN");
        }
        if (parsed != PaymentChannel.CARD && parsed != PaymentChannel.MOMO) {
            throw new BusinessRuleException(
                    "Seuls la carte (Visa/Mastercard) et le Mobile Money sont encaissables en ligne ici",
                    "SUBSCRIPTION_CHANNEL_UNSUPPORTED");
        }
        return parsed;
    }

    /**
     * Opérateur prestataire pour le canal.
     *
     * <p>Carte → {@code CARD}. Mobile Money → l'opérateur demandé s'il est servi
     * par un prestataire branché, sinon le premier d'une liste de préférence
     * (Orange Money, MTN MoMo, Wave) réellement disponible. On ne devine jamais
     * un opérateur inexistant : mieux vaut refuser que promettre un 503.</p>
     */
    private PaymentIntent.Operator resolveOperator(PaymentChannel channel, String requestedOperator) {
        if (channel == PaymentChannel.CARD) {
            return PaymentIntent.Operator.CARD;
        }
        if (requestedOperator != null && !requestedOperator.isBlank()) {
            PaymentIntent.Operator op = parseOperator(requestedOperator);
            requireServable(op);
            return op;
        }
        // Préférence OM → MoMo → Wave ; le registre sait qui est réellement branché.
        for (PaymentIntent.Operator candidate : List.of(
                PaymentIntent.Operator.ORANGE_MONEY, PaymentIntent.Operator.MTN_MOMO, PaymentIntent.Operator.WAVE)) {
            if (isServable(candidate)) {
                return candidate;
            }
        }
        throw new BusinessRuleException(
                "Aucun opérateur Mobile Money n'est branché pour encaisser cet abonnement",
                "SUBSCRIPTION_MOMO_OPERATOR_UNAVAILABLE");
    }

    private PaymentIntent.Operator parseOperator(String raw) {
        try {
            return PaymentIntent.Operator.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new BusinessRuleException("Opérateur inconnu : " + raw, "SUBSCRIPTION_OPERATOR_UNKNOWN");
        }
    }

    private void requireServable(PaymentIntent.Operator operator) {
        if (!isServable(operator)) {
            throw new BusinessRuleException(
                    "L'opérateur " + operator.getLabel() + " n'est pas branché auprès d'un prestataire",
                    "SUBSCRIPTION_OPERATOR_NOT_CONFIGURED");
        }
    }

    private boolean isServable(PaymentIntent.Operator operator) {
        // La disponibilité technique est déjà tranchée en amont par PaymentChannelResolver ;
        // ici on vérifie qu'un provider résoluble existe, sinon la passerelle retomberait
        // sur une référence locale sans URL de caisse (abonnement jamais activé).
        MobileMoneyProvider provider = providerRegistry.find(operator);
        return provider != null && provider.isEnabled();
    }
}

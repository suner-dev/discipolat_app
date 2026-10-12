package com.discipolat.modules.payments.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.tenants.domain.PricingCatalogService;
import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanPrice;
import com.discipolat.modules.tenants.domain.SaasPlanPriceRepository;
import com.discipolat.modules.tenants.domain.SaasPlanPricingSettings;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service de gestion des abonnements SaaS via Mobile Money (MTN MoMo).
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3.
 *
 * <p>Implémente la tokenization MTN MoMo pour les prélèvements récurrents :
 * <ol>
 *   <li>Première souscription : crée un API User MTN, obtient la clé API, stocke le token.</li>
 *   <li>Renouvellements : utilise l'API User ID pour déclencher le prélèvement sans interaction utilisateur.</li>
 *   <li>Échecs : retentatives via scheduler, passage en FAILED après max retries.</li>
 * </ol>
 *
 * <p><b>Seul MTN MoMo est supporté</b> pour le récurrent : M-Pesa (STK Push) et
 * Orange Money ne supportent pas la tokenization replayable.</p>
 */
@Service
@Transactional
public class MomoSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(MomoSubscriptionService.class);

    /** Nombre maximum de tentatives de prélèvement avant de marquer FAILED. */
    private static final int MAX_RETRY_COUNT = 3;

    /** Intervalle entre deux tentatives de prélèvement échoué (24h). */
    private static final Duration RETRY_INTERVAL = Duration.ofHours(24);

    private final SaasMomoSubscriptionRepository subscriptionRepository;
    private final SaasPlanPriceRepository priceRepository;
    private final SaasPlanRepository planRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantPlanPolicy planPolicy;
    private final MobileMoneyProviderRegistry momoRegistry;
    private final PaymentGatewayService paymentGatewayService;
    private final PricingCatalogService pricingCatalogService;
    private final PricingSettingsService pricingSettingsService;
    private final SaasPlanRepository saasPlanRepository;

    public MomoSubscriptionService(SaasMomoSubscriptionRepository subscriptionRepository,
                                   SaasPlanPriceRepository priceRepository,
                                   SaasPlanRepository planRepository,
                                   TenantSubscriptionRepository tenantSubscriptionRepository,
                                   TenantPlanPolicy planPolicy,
                                   MobileMoneyProviderRegistry momoRegistry,
                                   PaymentGatewayService paymentGatewayService,
                                   PricingCatalogService pricingCatalogService,
                                   PricingSettingsService pricingSettingsService,
                                   SaasPlanRepository saasPlanRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.priceRepository = priceRepository;
        this.planRepository = planRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.planPolicy = planPolicy;
        this.momoRegistry = momoRegistry;
        this.paymentGatewayService = paymentGatewayService;
        this.pricingCatalogService = pricingCatalogService;
        this.pricingSettingsService = pricingSettingsService;
        this.saasPlanRepository = saasPlanRepository;
    }

    // ── Souscription initiale ────────────────────────────────────────────────

    /**
     * Initie un abonnement SaaS via MTN MoMo (première paiement + tokenization).
     *
     * @param tenantId    tenant qui souscrit
     * @param planKey     plan demandé (ex: STARTUP)
     * @param billingCycle cycle de facturation
     * @param phone       numéro Mobile Money (format MSISDN, ex: 2376xxxxxxxx)
     * @return l'abonnement créé (statut PENDING, provider_reference = API user token)
     */
    @Transactional
    public SaasMomoSubscription subscribe(UUID tenantId, String planKey, String billingCycle, String phone) {
        // 1. Résoudre le plan et son prix pour la devise du tenant
        SaasPlanPricingSettings settings = pricingSettingsService.current();
        String displayCurrency = pricingCatalogService.resolveDisplayCurrency(null);
        BillingCycle cycle = BillingCycle.fromString(billingCycle);

        Optional<PricingCatalogService.ResolvedPrice> resolved = pricingCatalogService.resolvePrice(
                planKey, displayCurrency, cycle, null);

        if (resolved.isEmpty()) {
            throw new BusinessRuleException("Aucun prix pour le plan " + planKey, "PLAN_PRICE_MISSING");
        }

        PricingCatalogService.ResolvedPrice price = resolved.get();
        SaasPlanPrice priceEntity = priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
                planKey, price.currency(), BillingCycle.fromString(billingCycle), "GLOBAL")
                .orElseThrow(() -> new BusinessRuleException("Prix introuvable pour le plan " + planKey, "PRICE_NOT_FOUND"));

        // 2. Vérifier que le canal MOMO est disponible pour cette devise
        if (!price.channels().contains(PaymentChannel.MOMO)) {
            throw new BusinessRuleException(
                    "Le plan " + planKey + " en " + price.currency() + " n'accepte pas le Mobile Money",
                    "PLAN_NO_MOMO_CHANNEL");
        }

        // 3. Vérifier que l'opérateur MTN MoMo est activé
        MobileMoneyProvider momoProvider = momoRegistry.find(PaymentIntent.Operator.MTN_MOMO);
        if (momoProvider == null || !momoProvider.isEnabled()) {
            throw new BusinessRuleException(
                    "MTN MoMo non configuré ou désactivé",
                    "MOMO_NOT_CONFIGURED");
        }

        // 5. Créer l'abonnement en base (statut PENDING)
        SaasMomoSubscription subscription = SaasMomoSubscription.builder()
                .tenantId(tenantId)
                .planKey(planKey)
                .priceId(priceEntity.getId())
                .planPriceMinor(price.amountMinor())
                .currency(price.currency())
                .operator("MTN_MOMO")
                .phone(phone)
                .status(MomoSubscriptionStatus.PENDING)
                .nextChargeAt(Instant.now())
                .build();

        subscription = subscriptionRepository.save(subscription);

        // 6. Initier le premier paiement (création API User MTN + premier paiement)
        initiateFirstPayment(subscription);

        return subscription;
    }

    /**
     * Initie le premier paiement qui crée l'API User MTN MoMo et obtient le token.
     * Le token est stocké dans provider_reference pour les renouvellements.
     */
    private void initiateFirstPayment(SaasMomoSubscription subscription) {
        try {
            // 1. Créer un API User MTN MoMo
            String apiUserId = createMtnApiUser(subscription.getTenantId());
            if (apiUserId == null) {
                markFailed(subscription, "Impossible de créer l'API User MTN MoMo");
                return;
            }

            // 2. Obtenir la clé API pour cet API User
            String apiKey = getMtnApiKey(apiUserId);
            if (apiKey == null) {
                markFailed(subscription, "Impossible d'obtenir la clé API MTN MoMo");
                return;
            }

            // 3. Stocker l'API User ID comme provider_reference (utilisé comme partyId pour les renouvellements)
            subscription.setProviderReference(apiUserId);
            subscription.setMetadata(subscription.getMetadata() + String.format(", \"api_user_id\": \"%s\", \"api_key\": \"%s\"", apiUserId, apiKey));
            subscriptionRepository.save(subscription);

            // 4. Lancer le premier Request to Pay avec l'API User ID comme partyId
            initiateRequestToPayWithApiUser(subscription, apiUserId);

        } catch (Exception e) {
            log.error("[MomoSub] Erreur lors de l'initiation du premier paiement pour tenant={}", subscription.getTenantId(), e);
            markFailed(subscription, "Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Crée un API User MTN MoMo pour la tokenization.
     * POST /v1_0/apiuser
     */
    private String createMtnApiUser(UUID tenantId) {
        try {
            MtnMomoProvider provider = (MtnMomoProvider) momoRegistry.find(PaymentIntent.Operator.MTN_MOMO);
            if (provider == null || !provider.isEnabled()) {
                log.warn("[MomoSub] MTN MoMo non configuré");
                return null;
            }

            // Accéder aux propriétés MTN via le provider
            // On utilise le client HTTP directement pour l'appel de création d'API User
            PaymentProviderProperties props = getMtnProperties();
            if (props == null) return null;

            String url = props.getMtnBaseUrl() + "/v1_0/apiuser";
            String apiUserId = UUID.randomUUID().toString();

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("providerCallbackHost", props.getMtnCallbackHost());

            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(bodyMap);

            String credentials = props.getMtnClientId() + ":" + props.getMtnClientSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("X-Reference-Id", apiUserId)
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[MTN MoMo] createApiUser HTTP {} — apiUserId={}", response.statusCode(), apiUserId);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return apiUserId;
            } else {
                log.error("[MTN MoMo] createApiUser failed {} — {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("[MTN MoMo] createApiUser error", e);
            return null;
        }
    }

    /**
     * Obtient la clé API pour un API User MTN MoMo.
     * POST /v1_0/apiuser/{apiUserId}/apikey
     */
    private String getMtnApiKey(String apiUserId) {
        try {
            PaymentProviderProperties props = getMtnProperties();
            if (props == null) return null;

            String url = props.getMtnBaseUrl() + "/v1_0/apiuser/" + apiUserId + "/apikey";

            String credentials = props.getMtnClientId() + ":" + props.getMtnClientSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[MTN MoMo] getApiKey HTTP {} — apiUserId={}", response.statusCode(), apiUserId);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response.body());
                return json.get("apiKey").asText();
            } else {
                log.error("[MTN MoMo] getApiKey failed {} — {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("[MTN MoMo] getApiKey error", e);
            return null;
        }
    }

    /**
     * Lance un Request to Pay en utilisant l'API User ID comme partyId (mode récurrent).
     */
    private void initiateRequestToPayWithApiUser(SaasMomoSubscription subscription, String apiUserId) {
        try {
            PaymentProviderProperties props = getMtnProperties();
            if (props == null) {
                markFailed(subscription, "Configuration MTN MoMo manquante");
                return;
            }

            String url = props.getMtnBaseUrl() + "/collection/v1_0/requesttopay";
            String referenceId = UUID.randomUUID().toString();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("amount", String.valueOf(subscription.getPlanPriceMinor()));
            bodyMap.put("currency", subscription.getCurrency());
            bodyMap.put("externalId", subscription.getProviderReference());

            Map<String, String> payer = new LinkedHashMap<>();
            payer.put("partyIdType", "API_USER");
            payer.put("partyId", apiUserId); // API User ID pour le mode récurrent
            bodyMap.put("payer", payer);

            Map<String, String> payerNote = new LinkedHashMap<>();
            payerNote.put("category", "Subscription");
            payerNote.put("description", "Abonnement SaaS " + subscription.getPlanKey());
            payerNote.put("subCategory", "SAAS_SUBSCRIPTION");
            bodyMap.put("payerNote", payerNote);

            String body = mapper.writeValueAsString(bodyMap);

            // Récupérer le token d'accès
            String accessToken = getMtnAccessToken(props);
            if (accessToken == null) {
                markFailed(subscription, "Impossible d'obtenir le token d'accès MTN");
                return;
            }

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Reference-Id", referenceId)
                    .header("X-Target-Environment", "sandbox")
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[MTN MoMo] requestToPay (API User) HTTP {} — ref={}", response.statusCode(), referenceId);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                subscription.setStatus(MomoSubscriptionStatus.PENDING);
                subscription.setMetadata(subscription.getMetadata() + ", \"last_request_reference\": \"" + referenceId + "\"");
                subscriptionRepository.save(subscription);
            } else {
                log.error("[MTN MoMo] requestToPay failed {} — {}", response.statusCode(), response.body());
                markFailed(subscription, "Erreur Request to Pay: " + extractError(response.body()));
            }
        } catch (Exception e) {
            log.error("[MTN MoMo] requestToPay error", e);
            markFailed(subscription, "Erreur Request to Pay: " + e.getMessage());
        }
    }

    private String getMtnAccessToken(PaymentProviderProperties props) {
        try {
            String credentials = props.getMtnClientId() + ":" + props.getMtnClientSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String url = props.getMtnBaseUrl() + "/collection/token/";

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response.body());
                return json.get("access_token").asText();
            }
            return null;
        } catch (Exception e) {
            log.error("[MTN MoMo] getAccessToken error", e);
            return null;
        }
    }

    private PaymentProviderProperties getMtnProperties() {
        // Récupérer les propriétés via le provider registry
        MobileMoneyProvider provider = momoRegistry.find(PaymentIntent.Operator.MTN_MOMO);
        if (provider instanceof MtnMomoProvider mtnProvider) {
            try {
                java.lang.reflect.Field field = MtnMomoProvider.class.getDeclaredField("props");
                field.setAccessible(true);
                return (PaymentProviderProperties) field.get(mtnProvider);
            } catch (Exception e) {
                log.error("[MTN MoMo] Impossible d'accéder aux propriétés", e);
            }
        }
        return null;
    }

    private static String extractError(String body) {
        if (body == null || body.isBlank()) return "unknown";
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(body);
            if (json.has("message")) return json.get("message").asText();
            if (json.has("error")) return json.get("error").asText();
            if (json.has("reason")) return json.get("reason").asText();
        } catch (Exception ignored) { }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    // ── Renouvellement (scheduler) ─────────────────────────────────────────

    /**
     * Traite les abonnements dont l'échéance est due.
     * Exécuté toutes les heures par le scheduler.
     */
    @Scheduled(fixedDelayString = "${app.momo.subscription.retry-interval-ms:3600000}")
    @Transactional
    public void processDueSubscriptions() {
        Instant now = Instant.now();
        List<SaasMomoSubscription> due = subscriptionRepository.findByStatusAndNextChargeAtBefore(
                MomoSubscriptionStatus.ACTIVE, Instant.now());

        for (SaasMomoSubscription sub : due) {
            processRenewal(sub);
        }

        // Retry des échecs
        List<SaasMomoSubscription> failed = subscriptionRepository.findByStatus(MomoSubscriptionStatus.FAILED);
        for (SaasMomoSubscription sub : failed) {
            if (shouldRetry(sub)) {
                retryFailedPayment(sub);
            }
        }
    }

    /**
     * Tente de prélever le montant pour un renouvellement.
     * Utilise l'API User token stocké dans provider_reference.
     */
    @Transactional
    public void processRenewal(SaasMomoSubscription subscription) {
        if (subscription.getProviderReference() == null || subscription.getProviderReference().isBlank()) {
            log.warn("[MomoSub] Pas de provider_reference pour tenant={} — impossible de renouveler",
                    subscription.getTenantId());
            markFailed(subscription, "Token MTN MoMo manquant");
            return;
        }

        // Construire l'intent de renouvellement avec l'API User ID comme partyId
        PaymentIntent intent = PaymentIntent.builder()
                .tenantId(subscription.getTenantId())
                .operator(PaymentIntent.Operator.MTN_MOMO)
                .phoneNumber(subscription.getProviderReference()) // API User ID comme partyId
                .amount(java.math.BigDecimal.valueOf(subscription.getPlanPriceMinor(), 0))
                .currency(subscription.getCurrency())
                .purpose(PaymentIntent.Purpose.SAAS_SUBSCRIPTION)
                .build();

        // L'initiateur Mobile Money doit utiliser l'API User ID
        // Le provider MTN MoMo doit supporter l'envoi sans interaction utilisateur
        // quand le partyId est un API User ID valide

        MobileMoneyProvider provider = momoRegistry.find(PaymentIntent.Operator.MTN_MOMO);
        if (provider == null) {
            markFailed(subscription, "MTN MoMo non configuré");
            return;
        }

        MobileMoneyProvider.Result result = provider.initiate(intent);
        if (result.externalRedirect()) {
            // Paiement initié — attendre webhook pour confirmer
            subscription.setStatus(MomoSubscriptionStatus.ACTIVE);
            // nextChargeAt sera mis à jour par le webhook quand paiement confirmé
        } else {
            markFailed(subscription, result.instructions());
        }
    }

    /** Détermine si un abonnement en échec doit être retenté. */
    private boolean shouldRetry(SaasMomoSubscription sub) {
        // Max 3 retries, espacés de 24h minimum
        if (sub.getRetryCount() >= MAX_RETRY_COUNT) {
            return false;
        }
        if (sub.getLastRetryAt() != null) {
            Instant nextRetry = sub.getLastRetryAt().plus(RETRY_INTERVAL);
            if (Instant.now().isBefore(nextRetry)) {
                return false;
            }
        }
        return true;
    }

    /** Réessaie un paiement échoué. */
    private void retryFailedPayment(SaasMomoSubscription sub) {
        sub.setStatus(MomoSubscriptionStatus.PENDING);
        sub.setNextChargeAt(Instant.now());
        sub.setRetryCount(sub.getRetryCount() + 1);
        sub.setLastRetryAt(Instant.now());
        subscriptionRepository.save(sub);
        processRenewal(sub);
    }

    /** Marque un abonnement comme échoué définitivement. */
    private void markFailed(SaasMomoSubscription sub, String reason) {
        sub.setStatus(MomoSubscriptionStatus.FAILED);
        sub.setMetadata(sub.getMetadata() + ", \"failure_reason\": \"" + reason + "\"");
        sub.setNextChargeAt(Instant.now().plus(RETRY_INTERVAL));
        sub.setRetryCount(sub.getRetryCount() + 1);
        sub.setLastRetryAt(Instant.now());
        subscriptionRepository.save(sub);
        log.warn("[MomoSub] Échec tenant={} — {}", sub.getTenantId(), reason);
    }

    // ── Gestion webhook (appelé par PaymentGatewayService) ─────────────────

    /**
     * Traite la notification de paiement MTN MoMo pour un abonnement.
     * Appelé par PaymentGatewayService.handleWebhook().
     */
    @Transactional
    public void handleWebhook(String providerReference, String status, String reason) {
        Optional<SaasMomoSubscription> opt = subscriptionRepository.findByProviderReference(providerReference);
        if (opt.isEmpty()) {
            log.warn("[MomoSub] Webhook pour référence inconnue: {}", providerReference);
            return;
        }

        SaasMomoSubscription sub = opt.get();

        switch (status.toUpperCase()) {
            case "SUCCESSFUL":
                // Paiement réussi — activer l'abonnement
                sub.setStatus(MomoSubscriptionStatus.ACTIVE);
                sub.setNextChargeAt(nextChargeDate(sub));
                // Réinitialiser le compteur de retries
                sub.setRetryCount(0);
                sub.setLastRetryAt(null);
                subscriptionRepository.save(sub);
                // Activer l'abonnement SaaS côté tenant
                activateTenantSubscription(sub.getTenantId(), sub.getPlanKey());
                log.info("[MomoSub] Renouvellement réussi tenant={}", sub.getTenantId());
                break;

            case "FAILED":
            case "PENDING":
                // Laisser en PENDING, le scheduler réessaiera
                break;

            default:
                markFailed(opt.get(), "Statut MTN inattendu: " + status);
        }
    }

    /** Calcule la prochaine date d'échéance selon le cycle. */
    private Instant nextChargeDate(SaasMomoSubscription sub) {
        // Pour simplifier : même jour le mois prochain (ou l'année)
        // En production, utiliser la date exacte du cycle
        return Instant.now().plus(Duration.ofDays(30));
    }

    /** Active l'abonnement SaaS côté tenant après paiement réussi. */
    private void activateTenantSubscription(UUID tenantId, String planKey) {
        // Réutilise la logique SaaS existante
        // TODO: Appeler SaasPlanService.subscribe() ou équivalent
    }
}
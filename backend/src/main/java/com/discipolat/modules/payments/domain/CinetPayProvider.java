package com.discipolat.modules.payments.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Agrégateur de paiement <strong>CinetPay</strong> — un seul contrat couvrant
 * Orange Money, MTN MoMo, Wave et les cartes Visa/Mastercard.
 *
 * <p><strong>Pourquoi un agrégateur plutôt que les APIs opérateur directes.</strong>
 * Contracter chaque opérateur (Orange, MTN, …) suppose un accord commercial et une
 * intégration distincte par pays ; CinetPay les fédère derrière une API REST unique,
 * avec typiquement des frais plus bas et une mise en production plus rapide. Ce
 * provider est donc un {@link MobileMoneyProvider#aggregator() agrégateur} : quand
 * ses clés sont configurées, il prend la main sur les providers directs pour les
 * opérateurs qu'il sert (cf. {@link MobileMoneyProviderRegistry}), les APIs opérateur
 * restant le repli automatique tant que CinetPay n'est pas branché.</p>
 *
 * <p><strong>Contrat réel, fail-closed.</strong> Conformément à la règle du projet
 * (§41) : l'intégration est réelle (contrat, configuration, persistance, gestion
 * d'erreurs, tests) mais {@link #isEnabled()} renvoie {@code false} sans les deux
 * clés (api key + secret). Le provider n'est alors JAMAIS enregistré — aucun appel
 * réseau, aucune régression pour les environnements non configurés.</p>
 *
 * <p><strong>Flux collection (CinetPay v2, {@code application/x-www-form-urlencoded}) :</strong>
 * <ol>
 *   <li>{@code POST {base}/charge} avec notre {@code transaction_id} → {@code data.token}
 *       + {@code data.cps_url} (page de caisse hébergée CPS).</li>
 *   <li>Le payeur complète sur {@code cps_url} (redirection externe).</li>
 *   <li>Confirmation : notify CPS (cf. {@code CinetPayWebhookController}) puis
 *       re-validation serveur via {@code POST {base}/validate} — la <em>source de
 *       vérité</em> est la réponse de {@code /validate}, jamais le notify lui-même.</li>
 * </ol>
 *
 * <p><strong>Corrélation.</strong> Nous imposons notre propre {@code transaction_id}
 * (la référence locale déjà posée par {@link PaymentGatewayService}), donc la ligne
 * {@code payment_intents.provider_reference} reste la clé de rapprochement du webhook
 * — pas de perte de repère entre le notify et l'intention.</p>
 */
@Component
public class CinetPayProvider implements MobileMoneyProvider {

    private static final Logger log = LoggerFactory.getLogger(CinetPayProvider.class);

    /** event_code de succès d'une appel API CinetPay (charge acceptée). */
    private static final String EVENT_CHARGE_OK = "8100";
    /** event_code de succès d'un /validate. */
    private static final String EVENT_VALIDATE_OK = "0000";
    /** Statut d'un paiement réellement encaissé, renvoyé par /validate. */
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final PaymentProviderProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CinetPayProvider(PaymentProviderProperties props) {
        this.props = props;
    }

    @Override
    public PaymentIntent.Operator operator() {
        // Opérateur nominal (sert de clé stable dans l'EnumMap si supportedOperators
        // est ignoré) ; le registre route en réalité via supportedOperators().
        return PaymentIntent.Operator.ORANGE_MONEY;
    }

    @Override
    public boolean aggregator() {
        return true;
    }

    @Override
    public Set<PaymentIntent.Operator> supportedOperators() {
        return Set.of(
                PaymentIntent.Operator.ORANGE_MONEY,
                PaymentIntent.Operator.MTN_MOMO,
                PaymentIntent.Operator.WAVE,
                PaymentIntent.Operator.CARD);
    }

    @Override
    public boolean isEnabled() {
        return props.isCinetpayActive();
    }

    @Override
    public Result initiate(PaymentIntent intent) {
        try {
            // transaction_id = référence locale déjà posée par le gateway : la clé de
            // rapprochement du notify/validate reste donc cette même référence.
            String transactionId = intent.getProviderReference();

            Map<String, String> form = new LinkedHashMap<>();
            form.put("site_api_key", props.getCinetpaySiteApiKey());
            form.put("site_secret", props.getCinetpaySiteSecret());
            form.put("transaction_id", transactionId);
            form.put("amount", amountForCinetpay(intent));
            form.put("currency", intent.getCurrency());
            form.put("channels", props.getCinetpayChannels());
            form.put("description", describe(intent));
            form.put("customer_phone_number", intent.getPhoneNumber() == null ? "" : intent.getPhoneNumber());
            form.put("return_url", props.getCinetpayReturnUrl());
            if (props.getCinetpayNotifUrl() != null && !props.getCinetpayNotifUrl().isBlank()) {
                form.put("notify_url", props.getCinetpayNotifUrl());
            }

            JsonNode json = post(props.getCinetpayBaseUrl() + "/charge", form);
            log.info("[CinetPay] charge HTTP ok — transaction_id={} event_code={}",
                    transactionId, text(json, "event_code"));

            String eventCode = text(json, "event_code");
            JsonNode data = json.path("data");
            String cpsUrl = text(data, "cps_url");
            String token = text(data, "token");

            if (EVENT_CHARGE_OK.equals(eventCode) && cpsUrl != null) {
                // providerReference reste notre transaction_id (corrélyle webhook) ;
                // le token CPS est conservé dans les instructions pour debug/rejeu.
                String instructions = "Finalisez le paiement sur la page sécurisée CinetPay."
                        + (token != null ? " [token=" + token + "]" : "");
                return new Result(transactionId, cpsUrl, instructions, true);
            }

            String reason = text(json, "event_code_text");
            log.warn("[CinetPay] charge non acceptée — event_code={} : {}", eventCode, reason);
            return new Result(transactionId, null,
                    "CinetPay : " + (reason != null ? reason : "paiement non initié"), false);
        } catch (Exception e) {
            log.error("[CinetPay] charge erreur — ref={}", intent.getProviderReference(), e);
            return new Result(intent.getProviderReference(), null,
                    "Erreur de connexion CinetPay : " + e.getMessage(), false);
        }
    }

    @Override
    public Verification verify(String providerReference) {
        return verifyTransaction(providerReference);
    }

    /**
     * Re-validation serveur d'une transaction par son {@code transaction_id} (notre
     * référence locale). Utilisée par le webhook : la décision « payé » vient de
     * l'API CinetPay, jamais du payload notify (qui n'est pas signé de façon fiable).
     */
    public Verification verifyTransaction(String transactionId) {
        try {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("site_api_key", props.getCinetpaySiteApiKey());
            form.put("site_secret", props.getCinetpaySiteSecret());
            form.put("transaction_id", transactionId);

            JsonNode json = post(props.getCinetpayBaseUrl() + "/validate", form);
            JsonNode data = json.path("data");
            String status = text(data, "status");
            boolean accepted = EVENT_VALIDATE_OK.equals(text(json, "event_code"))
                    && STATUS_ACCEPTED.equalsIgnoreCase(status);
            String reason = accepted ? null
                    : ("CinetPay status=" + (status != null ? status : text(json, "event_code_text")));
            log.info("[CinetPay] validate transaction_id={} status={} paid={}",
                    transactionId, status, accepted);
            return new Verification(accepted, status, reason);
        } catch (Exception e) {
            log.error("[CinetPay] validate erreur — transaction_id={}", transactionId, e);
            return new Verification(false, "ERROR", e.getMessage());
        }
    }

    /* ------------------------------ Internes ------------------------------ */

    /**
     * CinetPay attend un montant entier pour les devises sans unité mineure
     * (XOF, XAF) : {@code 5000} et non {@code 5000.00}.
     */
    private static String amountForCinetpay(PaymentIntent intent) {
        return intent.getAmount().toBigInteger().toString();
    }

    private static String describe(PaymentIntent intent) {
        String purpose = switch (intent.getPurpose()) {
            case DIME -> "Dime";
            case OFFRANDE -> "Offrande";
            case PROMESSE -> "Promesse";
            case PROJET_SPECIAL -> "Projet spécial";
            case DON_DIASPORA -> "Don diaspora";
            case SAAS_SUBSCRIPTION -> "Abonnement";
            case OFFRANDE_ORDINAIRE -> "Offrande ordinaire";
            case OFFRANDE_SPECIALE -> "Offrande spéciale";
            case OFFRANDE_CONSTRUCTION -> "Offrande construction";
            case OFFRANDE_EVANGELISATION -> "Offrande évangélisation";
            case DON_SOLIDARITE -> "Don solidarité";
            case COTISATION -> "Cotisation";
            case SEED -> "Semence";
        };
        return purpose + " — Discipolat [" + intent.getProviderReference() + "]";
    }

    /** POST formulaire (form-urlencoded) et lecture du JSON de réponse. */
    private JsonNode post(String url, Map<String, String> form) throws Exception {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " : " + truncate(response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}

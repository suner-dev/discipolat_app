package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * A3 (M8) — Décaissement via PayPal Payouts (API v1 /v1/payments/payouts).
 *
 * <p>Le destinataire est un email PayPal ou un identifiant {@code payer_id} ;
 * la devise est ISO-4217. Authentification : client credentials (Basic
 * base64(client-id:secret)), jeton obtenu à chaque appel (durée de vie 900 s,
 * usage unitaire par opération). Aucun appel sans credentials : 503
 * {@code PAYOUT_PAYPAL_NOT_CONFIGURED}.</p>
 *
 * <p>Webhooks : PayPal publie une vérification par certificat
 * (rest-api-sdk) non implémentable sans dépendance nouvelle ; la vérification
 * offerte ici est le HMAC-SHA256 du corps avec {@code webhook-secret}
 * (en-tête {@code X-Webhook-Signature}), fail-closed si absent. Cette limite
 * est documentée honnêtement dans {@link #disabledReason()} et la doc
 * d'exploitation plutôt que masquée par un {@code return true}.</p>
 */
public class PayPalPayoutProvider implements PayoutProvider {

    static final String KEY = "paypal";
    private static final String DEFAULT_BASE_URL = "https://api-m.paypal.com";

    private final PayoutProvidersProperties.Provider config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PayPalPayoutProvider(PayoutProvidersProperties.Provider config,
                                HttpClient httpClient,
                                ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled() && StripePayoutProvider.hasText(config.getApiKey())
                && StripePayoutProvider.hasText(config.getApiSecret());
    }

    @Override
    public String disabledReason() {
        if (!config.isEnabled()) {
            return "drapeau app.payments.providers.paypal.enabled=false";
        }
        if (!isEnabled()) {
            return "client-id et/ou client-secret PayPal absents de la configuration";
        }
        return null;
    }

    @Override
    public PayoutResult initiate(PayoutRequest request) {
        if (!isEnabled()) {
            throw new DomainException("Décaissement PayPal non configuré",
                    HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_PAYPAL_NOT_CONFIGURED",
                    Map.of("reason", String.valueOf(disabledReason())));
        }
        try {
            String token = accessToken();
            JsonNode payload = objectMapper.createObjectNode()
                    .put("sender_batch_id", request.reference())
                    .set("items", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("recipient_id", request.recipient())
                                    .set("amount", objectMapper.createObjectNode()
                                            .put("currency_code", request.currency())
                                            // L'unité mineure est convertie pour le contrat PayPal
                                            .put("value", majorAmount(request)))));
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/payments/payouts"))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2) {
                return PayoutResult.failed(body.path("details").path(0).path("issue").asText(null),
                        "PayPal a refusé le payout : " + body.path("message").asText("erreur inconnue"));
            }
            return PayoutResult.pending(body.path("batch_header").path("payout_batch_id").asText(),
                    "lot de payouts PayPal créé");
        } catch (IOException e) {
            throw gatewayFailure("PayPal injoignable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw gatewayFailure("Appel PayPal interrompu", e);
        }
    }

    @Override
    public PayoutStatus queryStatus(String providerReference) {
        if (!isEnabled() || providerReference == null || providerReference.isBlank()) {
            return PayoutStatus.UNKNOWN;
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/payments/payouts/"
                                    + URLEncoder.encode(providerReference, StandardCharsets.UTF_8)))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + accessToken())
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return PayoutStatus.UNKNOWN;
            }
            return switch (objectMapper.readTree(response.body()).path("batch_status").asText()) {
                case "SUCCESS" -> PayoutStatus.SETTLED;
                case "FAILED", "BLOCKED", "DENIED" -> PayoutStatus.FAILED;
                case "PENDING" -> PayoutStatus.PENDING;
                default -> PayoutStatus.UNKNOWN;
            };
        } catch (IOException e) {
            return PayoutStatus.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PayoutStatus.UNKNOWN;
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> headers, String rawBody) {
        if (!StripePayoutProvider.hasText(config.getWebhookSecret()) || rawBody == null) {
            return false;   // fail-closed
        }
        String signature = StripePayoutProvider.getIgnoreCase(headers, "X-Webhook-Signature");
        if (signature == null) {
            return false;
        }
        String expected = PayoutCrypto.hmacSha256Hex(config.getWebhookSecret(), rawBody);
        return PayoutCrypto.constantTimeEquals(expected, signature);
    }

    /** Monnaie majeure formatée : PayPal exige « 10.50 », jamais des centimes bruts. */
    private String majorAmount(PayoutRequest request) {
        int decimals = java.util.Currency.getInstance(request.currency()).getDefaultFractionDigits();
        if (decimals <= 0) {
            return String.valueOf(request.amountMinor());
        }
        return java.math.BigDecimal.valueOf(request.amountMinor(), decimals)
                .toPlainString();
    }

    private String accessToken() throws IOException, InterruptedException {
        String basic = Base64.getEncoder().encodeToString(
                (config.getApiKey() + ":" + config.getApiSecret()).getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/oauth2/token"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Basic " + basic)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials", StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("PayPal a refusé l'authentification (HTTP " + response.statusCode() + ")");
        }
        return objectMapper.readTree(response.body()).path("access_token").asText();
    }

    private String baseUrl() {
        return StripePayoutProvider.hasText(config.getBaseUrl()) ? config.getBaseUrl() : DEFAULT_BASE_URL;
    }

    private static DomainException gatewayFailure(String reason, Exception cause) {
        return new DomainException(reason + " — décaissement non confirmé",
                HttpStatus.BAD_GATEWAY, "PAYOUT_GATEWAY_UNAVAILABLE", Map.of("detail", cause.toString()));
    }
}

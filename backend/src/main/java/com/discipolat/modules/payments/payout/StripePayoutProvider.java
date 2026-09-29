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
import java.util.Map;

/**
 * A3 (M8) — Décaissement via Stripe (Transfers, API v1, contrat public).
 *
 * <p>Le destinataire ({@code PayoutRequest.recipient}) est un identifiant de
 * compte Connect Stripe ({@code acct_…} / {@code tr_…} selon le flux) ; la
 * devise est un code ISO-4217 géré nativement par Stripe. Aucun appel réseau
 * n'est tenté sans credentials : 503 {@code PAYOUT_STRIPE_NOT_CONFIGURED},
 * conformément à la règle « aucun mock en production, 503 honnête ».</p>
 *
 * <p>Webhooks : vérification conforme au schéma officiel
 * {@code Stripe-Signature: t=<timestamp>,v1=<signature>} — HMAC-SHA256 de
 * {@code "<t>.<body>"} avec le secret du endpoint, comparaison en temps
 * constant et tolérance d'horloge de 5 minutes (anti-replay).</p>
 */
public class StripePayoutProvider implements PayoutProvider {

    static final String KEY = "stripe";
    private static final String DEFAULT_BASE_URL = "https://api.stripe.com";
    private static final long REPLAY_TOLERANCE_SECONDS = 300;

    private final PayoutProvidersProperties.Provider config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StripePayoutProvider(PayoutProvidersProperties.Provider config,
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
        return config.isEnabled() && hasText(config.getApiKey());
    }

    @Override
    public String disabledReason() {
        if (!config.isEnabled()) {
            return "drapeau app.payments.providers.stripe.enabled=false";
        }
        if (!hasText(config.getApiKey())) {
            return "clé d'API Stripe absente de la configuration";
        }
        return null;
    }

    @Override
    public PayoutResult initiate(PayoutRequest request) {
        if (!isEnabled()) {
            throw new DomainException("Décaissement Stripe non configuré",
                    HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_STRIPE_NOT_CONFIGURED",
                    Map.of("reason", String.valueOf(disabledReason())));
        }
        String form = "amount=" + request.amountMinor()
                + "&currency=" + request.currency().toLowerCase()
                + "&destination=" + enc(request.recipient())
                + "&transfer_group=" + enc(request.reference());
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/transfers"))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + config.getApiKey())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2) {
                String message = body.path("error").path("message").asText("erreur Stripe");
                return PayoutResult.failed(body.path("id").asText(null),
                        "Stripe a refusé le transfert : " + message);
            }
            return PayoutResult.pending(body.path("id").asText(), "transfert Stripe créé");
        } catch (IOException e) {
            throw gatewayFailure("Stripe injoignable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw gatewayFailure("Appel Stripe interrompu", e);
        }
    }

    @Override
    public PayoutStatus queryStatus(String providerReference) {
        if (!isEnabled() || providerReference == null || providerReference.isBlank()) {
            return PayoutStatus.UNKNOWN;
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl() + "/v1/transfers/" + enc(providerReference)))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + config.getApiKey())
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return PayoutStatus.UNKNOWN;
            }
            // Statut officiel d'un transfert : "paid" une fois reversé.
            return "paid".equals(objectMapper.readTree(response.body()).path("status").asText())
                    ? PayoutStatus.SETTLED : PayoutStatus.PENDING;
        } catch (IOException e) {
            return PayoutStatus.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PayoutStatus.UNKNOWN;
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> headers, String rawBody) {
        if (!hasText(config.getWebhookSecret()) || rawBody == null) {
            return false;   // fail-closed : sans secret, rien n'est jamais accepté
        }
        String header = getIgnoreCase(headers, "Stripe-Signature");
        if (header == null) {
            return false;
        }
        String timestamp = null;
        String signature = null;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "t".equals(kv[0])) {
                timestamp = kv[1];
            } else if (kv.length == 2 && "v1".equals(kv[0])) {
                signature = kv[1];
            }
        }
        if (timestamp == null || signature == null) {
            return false;
        }
        try {
            long age = Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(timestamp));
            if (age > REPLAY_TOLERANCE_SECONDS) {
                return false;   // anti-replay : une signature âgée n'est pas une signature valide
            }
        } catch (NumberFormatException malformed) {
            return false;
        }
        String expected = PayoutCrypto.hmacSha256Hex(config.getWebhookSecret(), timestamp + "." + rawBody);
        return PayoutCrypto.constantTimeEquals(expected, signature);
    }

    private String baseUrl() {
        return hasText(config.getBaseUrl()) ? config.getBaseUrl() : DEFAULT_BASE_URL;
    }

    private static DomainException gatewayFailure(String reason, Exception cause) {
        return new DomainException(reason + " — décaissement non confirmé",
                HttpStatus.BAD_GATEWAY, "PAYOUT_GATEWAY_UNAVAILABLE", Map.of("detail", cause.toString()));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String getIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String direct = headers.get(name);
        if (direct != null) {
            return direct;
        }
        return headers.entrySet().stream()
                .filter(e -> name.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}

package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * A3 (M8) — Prélèvement / virement SEPA via le connecteur bancaire du tenant.
 *
 * <p>Choix assumé : Discipolat n'est pas une banque et ne parle pas directement
 * aux infrastructures TARGET2/STP. Le provider parle à un <b>connecteur
 * bancaire</b> (PSD2 AIS/PIS chez un prestataire type GoCardless/ALFIGA/Togocash)
 * configuré par le {@code base-url}. Sans connecteur, 503
 * {@code PAYOUT_SEPA_NOT_CONFIGURED} — jamais de « succès » local.</p>
 *
 * <p>La validation IBAN est réelle (mod-97, ISO 13616) et s'effectue
 * localement avant tout appel réseau : c'est la partie de la norme qui
 * appartient au produit, le reste est délégué au connecteur.</p>
 */
public class SepaDirectDebitProvider implements PayoutProvider {

    static final String KEY = "sepa";
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    private final PayoutProvidersProperties.Provider config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SepaDirectDebitProvider(PayoutProvidersProperties.Provider config,
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
        return config.isEnabled() && StripePayoutProvider.hasText(config.getBaseUrl());
    }

    @Override
    public String disabledReason() {
        if (!config.isEnabled()) {
            return "drapeau app.payments.providers.sepa.enabled=false";
        }
        if (!StripePayoutProvider.hasText(config.getBaseUrl())) {
            return "aucun connecteur bancaire configuré (app.payments.providers.sepa.base-url)";
        }
        return null;
    }

    @Override
    public PayoutResult initiate(PayoutRequest request) {
        if (!isEnabled()) {
            throw new DomainException("Connecteur SEPA non configuré",
                    HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_SEPA_NOT_CONFIGURED",
                    Map.of("reason", String.valueOf(disabledReason())));
        }
        // Le destinataire d'un flux SEPA est un IBAN : validé localement (ISO 13616).
        if (!isValidIban(request.recipient())) {
            return PayoutResult.failed(null, "IBAN invalide (contrôle mod-97 ISO 13616) : "
                    + maskIban(request.recipient()));
        }
        try {
            JsonNode payload = objectMapper.createObjectNode()
                    .put("endToEndId", request.reference())
                    .put("iban", request.recipient().replace(" ", "").toUpperCase())
                    .put("amountMinor", request.amountMinor())
                    .put("currency", request.currency());
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(config.getBaseUrl() + "/sepa/credit-transfers"))
                            .timeout(Duration.ofSeconds(15))
                            .header("Content-Type", "application/json")
                            .header("Authorization", authHeader())
                            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2) {
                return PayoutResult.failed(body.path("id").asText(null),
                        "Le connecteur bancaire a refusé le virement : " + body.path("message").asText("raison absente"));
            }
            return PayoutResult.pending(body.path("id").asText(), "ordre SEPA transmis au connecteur");
        } catch (IOException e) {
            throw new DomainException("Connecteur SEPA injoignable — décaissement non confirmé",
                    HttpStatus.BAD_GATEWAY, "PAYOUT_GATEWAY_UNAVAILABLE", Map.of("detail", e.toString()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Appel SEPA interrompu", HttpStatus.BAD_GATEWAY, "PAYOUT_GATEWAY_UNAVAILABLE");
        }
    }

    @Override
    public PayoutStatus queryStatus(String providerReference) {
        if (!isEnabled() || providerReference == null || providerReference.isBlank()) {
            return PayoutStatus.UNKNOWN;
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(config.getBaseUrl() + "/sepa/credit-transfers/"
                                    + URLEncoder.encode(providerReference, StandardCharsets.UTF_8)))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", authHeader())
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return PayoutStatus.UNKNOWN;
            }
            return switch (objectMapper.readTree(response.body()).path("status").asText()) {
                case "ACSC", "SETTLED", "PDNG_OK" -> PayoutStatus.SETTLED;
                case "ACCP", "PDNG" -> PayoutStatus.PENDING;
                case "RJCT", "FAIL" -> PayoutStatus.FAILED;
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
        String signature = StripePayoutProvider.getIgnoreCase(headers, "X-Connector-Signature");
        if (signature == null) {
            return false;
        }
        String expected = PayoutCrypto.hmacSha256Hex(config.getWebhookSecret(), rawBody);
        return PayoutCrypto.constantTimeEquals(expected, signature);
    }

    /**
     * Contrôle ISO 13616 : longueur 15-34, pays en 2 lettres, puis reorganisation
     * (quartets déplacés en fin) et remainder mod-97 == 1.
     */
    static boolean isValidIban(String iban) {
        if (iban == null) {
            return false;
        }
        String normalized = iban.replace(" ", "").toUpperCase();
        if (normalized.length() < 15 || normalized.length() > 34
                || !normalized.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$")) {
            return false;
        }
        String rotated = normalized.substring(4) + normalized.substring(0, 4);
        StringBuilder digits = new StringBuilder();
        for (char c : rotated.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                digits.append(c - 'A' + 10);
            } else {
                return false;
            }
        }
        return new BigInteger(digits.toString()).mod(NINETY_SEVEN).equals(new BigInteger("1"));
    }

    /** Masque un IBAN pour les journaux/messages : ne conserver que pays + derniers caractères. */
    static String maskIban(String iban) {
        if (iban == null || iban.length() < 6) {
            return "***";
        }
        String normalized = iban.replace(" ", "").toUpperCase();
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 4);
    }

    private String authHeader() {
        return StripePayoutProvider.hasText(config.getApiKey())
                ? "Bearer " + config.getApiKey()
                : "None";
    }
}

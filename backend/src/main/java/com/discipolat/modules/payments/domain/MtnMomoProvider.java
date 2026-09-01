package com.discipolat.modules.payments.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider MTN Mobile Money (MoMo) — API Collection v1.0.
 *
 * <p>Flux collection :
 * <ol>
 *   <li>POST /collection/token/ → access_token (Basic auth : client_id:client_secret)</li>
 *   <li>POST /collection/v1_0/requesttopay → reference opérateur + instructions</li>
 *   <li>GET /collection/v1_0/requesttopay/{referenceId} → statut</li>
 * </ol>
 *
 * <p>Sandbox : {@code https://sandbox.momodeveloper.mtn.com} (clés d'abonnement séparées).</p>
 */
@Component
class MtnMomoProvider implements MobileMoneyProvider {

    private static final Logger log = LoggerFactory.getLogger(MtnMomoProvider.class);

    private final PaymentProviderProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpiresAt;

    MtnMomoProvider(PaymentProviderProperties props) {
        this.props = props;
    }

    @Override
    public PaymentIntent.Operator operator() {
        return PaymentIntent.Operator.MTN_MOMO;
    }

    @Override
    public boolean isEnabled() {
        return props.isMtnEnabled();
    }

    @Override
    public Result initiate(PaymentIntent intent) {
        try {
            String token = getAccessToken();
            String referenceId = UUID.randomUUID().toString();

            String url = props.getMtnBaseUrl() + "/collection/v1_0/requesttopay";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("amount", intent.getAmount().toPlainString());
            bodyMap.put("currency", intent.getCurrency() != null ? intent.getCurrency() : "XOF");
            bodyMap.put("externalId", intent.getProviderReference());

            Map<String, String> payer = new LinkedHashMap<>();
            payer.put("partyIdType", "MSISDN");
            payer.put("partyId", intent.getPhoneNumber());
            bodyMap.put("payer", payer);

            Map<String, String> payerNote = new LinkedHashMap<>();
            payerNote.put("category", "Bill Payment");
            payerNote.put("description", "Paiement " + intent.getOperator().getLabel());
            payerNote.put("subCategory", intent.getPurpose() != null ? intent.getPurpose().name() : "General");
            bodyMap.put("payerNote", payerNote);

            Map<String, String> payeeNote = new LinkedHashMap<>();
            payeeNote.put("category", "Bill Payment");
            payeeNote.put("description", "Discipolat — " + intent.getOperator().getLabel());
            bodyMap.put("payeeNote", payeeNote);

            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("X-Reference-Id", referenceId)
                    .header("X-Target-Environment", "sandbox")
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[MTN MoMo] initiate HTTP {} — ref={}", response.statusCode(), intent.getProviderReference());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new Result(
                        referenceId,
                        null,
                        "Validez la demande sur votre téléphone MTN Mobile Money.",
                        false
                );
            } else {
                log.error("[MTN MoMo] initiate failed {} — {}", response.statusCode(), response.body());
                String reason = extractError(response.body());
                return new Result(
                        intent.getProviderReference(),
                        null,
                        "Erreur MTN MoMo: " + reason,
                        false
                );
            }
        } catch (Exception e) {
            log.error("[MTN MoMo] initiate error — ref={}", intent.getProviderReference(), e);
            return new Result(
                    intent.getProviderReference(),
                    null,
                    "Erreur de connexion MTN MoMo: " + e.getMessage(),
                    false
            );
        }
    }

    @Override
    public Verification verify(String providerReference) {
        try {
            String token = getAccessToken();
            String url = props.getMtnBaseUrl()
                    + "/collection/v1_0/requesttopay/" + providerReference;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("X-Target-Environment", "sandbox")
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[MTN MoMo] verify HTTP {} — ref={}", response.statusCode(), providerReference);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String status = json.has("status") ? json.get("status").asText() : "UNKNOWN";
                boolean paid = "SUCCESSFUL".equalsIgnoreCase(status);
                String reason = null;
                if (!paid && json.has("reason") && !json.get("reason").isNull()) {
                    reason = json.get("reason").asText();
                }
                return new Verification(paid, status, reason);
            } else {
                log.warn("[MTN MoMo] verify failed {} — ref={}", response.statusCode(), providerReference);
                return new Verification(false, "ERROR", "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("[MTN MoMo] verify error — ref={}", providerReference, e);
            return new Verification(false, "ERROR", e.getMessage());
        }
    }

    // ---- OAuth2 ----

    private synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        try {
            String credentials = props.getMtnClientId() + ":" + props.getMtnClientSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String url = props.getMtnBaseUrl() + "/collection/token/";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Ocp-Apim-Subscription-Key", props.getMtnSubscriptionKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3500;
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);

            log.info("[MTN MoMo] OAuth2 token obtained, expires in {}s", expiresIn);
            return cachedToken;
        } catch (Exception e) {
            log.error("[MTN MoMo] OAuth2 token error", e);
            throw new RuntimeException("Impossible d'obtenir le token MTN MoMo", e);
        }
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
}

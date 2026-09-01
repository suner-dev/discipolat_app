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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provider Orange Money (Orange Money Web Payment API).
 *
 * <p>Flux collection :
 * <ol>
 *   <li>POST /oauth/v3/token → access_token (Basic auth : client_id:client_secret)</li>
 *   <li>POST /orange-money-webpay/dev/v1/webpayment → payment_url + pay_token</li>
 *   <li>GET /orange-money-webpay/dev/v1/webpayment/{pay_token}/status → statut</li>
 * </ol>
 *
 * <p>Sandbox : {@code https://api.orange.com} (identifiants sandbox).</p>
 */
@Component
class OrangeMoneyProvider implements MobileMoneyProvider {

    private static final Logger log = LoggerFactory.getLogger(OrangeMoneyProvider.class);

    private final PaymentProviderProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpiresAt;

    OrangeMoneyProvider(PaymentProviderProperties props) {
        this.props = props;
    }

    @Override
    public PaymentIntent.Operator operator() {
        return PaymentIntent.Operator.ORANGE_MONEY;
    }

    @Override
    public boolean isEnabled() {
        return props.isOrangeEnabled();
    }

    @Override
    public Result initiate(PaymentIntent intent) {
        try {
            String token = getAccessToken();
            String reference = UUID.randomUUID().toString();

            String url = props.getOrangeBaseUrl() + "/orange-money-webpay/dev/v1/webpayment";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("merchant_key", props.getOrangeMerchantKey());
            bodyMap.put("currency", intent.getCurrency());
            bodyMap.put("order_id", intent.getProviderReference());
            bodyMap.put("amount", intent.getAmount().toPlainString());
            bodyMap.put("return_url", props.getOrangeReturnUrl());
            bodyMap.put("cancel_url", props.getOrangeReturnUrl());
            bodyMap.put("notif_url", props.getOrangeNotifUrl() != null
                    ? props.getOrangeNotifUrl()
                    : props.getOrangeReturnUrl() + "/webhook");
            bodyMap.put("lang", "fr");
            bodyMap.put("reference", reference);

            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Orange Money] initiate HTTP {} — ref={}", response.statusCode(), intent.getProviderReference());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String paymentUrl = json.has("payment_url") ? json.get("payment_url").asText() : null;
                String payToken = json.has("pay_token") ? json.get("pay_token").asText() : null;

                return new Result(
                        payToken != null ? payToken : reference,
                        paymentUrl,
                        paymentUrl != null
                                ? "Complétez le paiement sur la page Orange Money."
                                : "Validez la demande sur votre téléphone Orange Money.",
                        paymentUrl != null
                );
            } else {
                log.error("[Orange Money] initiate failed {} — {}", response.statusCode(), response.body());
                String reason = extractError(response.body());
                return new Result(
                        intent.getProviderReference(),
                        null,
                        "Erreur Orange Money: " + reason,
                        false
                );
            }
        } catch (Exception e) {
            log.error("[Orange Money] initiate error — ref={}", intent.getProviderReference(), e);
            return new Result(
                    intent.getProviderReference(),
                    null,
                    "Erreur de connexion Orange Money: " + e.getMessage(),
                    false
            );
        }
    }

    @Override
    public Verification verify(String providerReference) {
        try {
            String token = getAccessToken();
            String url = props.getOrangeBaseUrl()
                    + "/orange-money-webpay/dev/v1/webpayment/" + providerReference + "/status";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Orange Money] verify HTTP {} — payToken={}", response.statusCode(), providerReference);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String status = json.has("status") ? json.get("status").asText() : "UNKNOWN";
                boolean paid = "SUCCESS".equalsIgnoreCase(status);
                String reason = null;
                if (!paid && json.has("message") && !json.get("message").isNull()) {
                    reason = json.get("message").asText();
                }
                return new Verification(paid, status, reason);
            } else {
                log.warn("[Orange Money] verify failed {} — payToken={}", response.statusCode(), providerReference);
                return new Verification(false, "ERROR", "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("[Orange Money] verify error — payToken={}", providerReference, e);
            return new Verification(false, "ERROR", e.getMessage());
        }
    }

    // ---- OAuth2 ----

    private synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        try {
            String url = props.getOrangeBaseUrl() + "/oauth/v3/token";

            String formBody = "grant_type=client_credentials";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(
                            (props.getOrangeApiKey() + ":" + props.getOrangeMerchantKey()).getBytes()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3500;
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);

            log.info("[Orange Money] OAuth2 token obtained, expires in {}s", expiresIn);
            return cachedToken;
        } catch (Exception e) {
            log.error("[Orange Money] OAuth2 token error", e);
            throw new RuntimeException("Impossible d'obtenir le token Orange Money", e);
        }
    }

    private static String extractError(String body) {
        if (body == null || body.isBlank()) return "unknown";
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(body);
            if (json.has("message")) return json.get("message").asText();
            if (json.has("error")) return json.get("error").asText();
        } catch (Exception ignored) { }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }
}

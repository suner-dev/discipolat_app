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
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider M-Pesa (Safaricom Daraja API) — Lipa Na M-Pesa Online (STK Push).
 *
 * <p>Flux collection :
 * <ol>
 *   <li>POST /oauth/v1/generate → access_token (Basic auth : consumer_key:consumer_secret)</li>
 *   <li>POST /mpesa/stkpush/v1/processrequest → CheckoutRequestID + MerchantRequestID</li>
 *   <li>POST /mpesa/stkpushquery/v1/query → statut du STK push</li>
 * </ol>
 *
 * <p>Sandbox : {@code https://sandbox.safaricom.co.ke} (identifiants sandbox).</p>
 */
@Component
class MpesaProvider implements MobileMoneyProvider {

    private static final Logger log = LoggerFactory.getLogger(MpesaProvider.class);

    private final PaymentProviderProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String cachedToken;
    private volatile LocalDateTime tokenExpiresAt;

    MpesaProvider(PaymentProviderProperties props) {
        this.props = props;
    }

    @Override
    public PaymentIntent.Operator operator() {
        return PaymentIntent.Operator.M_PESA;
    }

    @Override
    public boolean isEnabled() {
        return props.isMpesaEnabled();
    }

    @Override
    public Result initiate(PaymentIntent intent) {
        try {
            String token = getAccessToken();
            String timestamp = LocalDateTime.now(ZoneId.of("Africa/Nairobi"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (props.getMpesaShortCode() + props.getMpesaPasskey() + timestamp)
                            .getBytes(StandardCharsets.UTF_8));

            String url = props.getMpesaBaseUrl() + "/mpesa/stkpush/v1/processrequest";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("BusinessShortCode", props.getMpesaShortCode());
            bodyMap.put("Password", password);
            bodyMap.put("Timestamp", timestamp);
            bodyMap.put("TransactionType", "CustomerPayBillOnline");
            bodyMap.put("Amount", intent.getAmount().longValue());
            bodyMap.put("PartyA", intent.getPhoneNumber());
            bodyMap.put("PartyB", props.getMpesaShortCode());
            bodyMap.put("PhoneNumber", intent.getPhoneNumber());
            String callbackUrl = props.getMpesaNotifUrl() != null && !props.getMpesaNotifUrl().isBlank()
                    ? props.getMpesaNotifUrl()
                    : (props.getOrangeNotifUrl() != null ? props.getOrangeNotifUrl() : "https://example.com/callback");
            bodyMap.put("CallBackURL", callbackUrl);
            bodyMap.put("AccountReference", intent.getProviderReference());
            bodyMap.put("TransactionDesc", "Paiement " + intent.getOperator().getLabel());

            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[M-Pesa] initiate HTTP {} — ref={}", response.statusCode(), intent.getProviderReference());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String checkoutRequestId = json.has("CheckoutRequestID")
                        ? json.get("CheckoutRequestID").asText() : null;
                String merchantRequestId = json.has("MerchantRequestID")
                        ? json.get("MerchantRequestID").asText() : null;
                String responseCode = json.has("ResponseCode")
                        ? json.get("ResponseCode").asText() : null;

                boolean success = "0".equals(responseCode);

                return new Result(
                        checkoutRequestId != null ? checkoutRequestId : merchantRequestId,
                        null,
                        success
                                ? "Validez la demande STK Push sur votre téléphone M-Pesa."
                                : "Erreur M-Pesa: " + (json.has("ResponseDescription")
                                ? json.get("ResponseDescription").asText() : responseCode),
                        false
                );
            } else {
                log.error("[M-Pesa] initiate failed {} — {}", response.statusCode(), response.body());
                return new Result(
                        intent.getProviderReference(),
                        null,
                        "Erreur M-Pesa: HTTP " + response.statusCode(),
                        false
                );
            }
        } catch (Exception e) {
            log.error("[M-Pesa] initiate error — ref={}", intent.getProviderReference(), e);
            return new Result(
                    intent.getProviderReference(),
                    null,
                    "Erreur de connexion M-Pesa: " + e.getMessage(),
                    false
            );
        }
    }

    @Override
    public Verification verify(String providerReference) {
        try {
            String token = getAccessToken();
            String timestamp = LocalDateTime.now(ZoneId.of("Africa/Nairobi"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (props.getMpesaShortCode() + props.getMpesaPasskey() + timestamp)
                            .getBytes(StandardCharsets.UTF_8));

            String url = props.getMpesaBaseUrl() + "/mpesa/stkpushquery/v1/query";

            String body = objectMapper.writeValueAsString(new LinkedHashMap<>() {{
                put("BusinessShortCode", props.getMpesaShortCode());
                put("Password", password);
                put("Timestamp", timestamp);
                put("CheckoutRequestID", providerReference);
            }});

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[M-Pesa] verify HTTP {} — ref={}", response.statusCode(), providerReference);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String responseCode = json.has("ResponseCode") ? json.get("ResponseCode").asText() : null;
                String resultCode = json.has("ResultCode") ? json.get("ResultCode").asText() : null;
                String resultDesc = json.has("ResultDescription") ? json.get("ResultDescription").asText() : null;

                // ResultCode "0" = succès
                boolean paid = "0".equals(resultCode) || "0".equals(responseCode);
                String status = paid ? "SUCCESS" : (" resultCode=" + resultCode);

                return new Verification(paid, status, paid ? null : resultDesc);
            } else {
                log.warn("[M-Pesa] verify failed {} — ref={}", response.statusCode(), providerReference);
                return new Verification(false, "ERROR", "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("[M-Pesa] verify error — ref={}", providerReference, e);
            return new Verification(false, "ERROR", e.getMessage());
        }
    }

    // ---- OAuth2 ----

    private synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiresAt != null && LocalDateTime.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        try {
            String credentials = props.getMpesaConsumerKey() + ":" + props.getMpesaConsumerSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getMpesaBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials"))
                    .header("Authorization", "Basic " + basicAuth)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? Integer.parseInt(json.get("expires_in").asText()) : 3500;
            tokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn - 60);

            log.info("[M-Pesa] OAuth2 token obtained, expires in {}s", expiresIn);
            return cachedToken;
        } catch (Exception e) {
            log.error("[M-Pesa] OAuth2 token error", e);
            throw new RuntimeException("Impossible d'obtenir le token M-Pesa", e);
        }
    }
}

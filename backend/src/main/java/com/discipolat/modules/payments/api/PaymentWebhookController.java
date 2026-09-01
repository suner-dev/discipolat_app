package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Webhooks opérateurs Mobile Money — réceptionne les callbacks de confirmation.
 *
 * <p>Chaque callback est logué dans {@code webhook_logs} pour audit trail.</p>
 *
 * <p>Sécurité : chaque webhook vérifie la signature/origine via
 * {@link WebhookSignatureVerifier}. Sans secret configuré, la vérification
 * est désactivée (mode sandbox).</p>
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGatewayService gatewayService;
    private final PaymentProviderProperties props;
    private final MobileMoneyProviderRegistry providerRegistry;
    private final WebhookSignatureVerifier verifier;
    private final WebhookLogService webhookLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentGatewayService gatewayService,
                                     PaymentProviderProperties props,
                                     MobileMoneyProviderRegistry providerRegistry,
                                     WebhookSignatureVerifier verifier,
                                     WebhookLogService webhookLogService) {
        this.gatewayService = gatewayService;
        this.props = props;
        this.providerRegistry = providerRegistry;
        this.verifier = verifier;
        this.webhookLogService = webhookLogService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-Pesa (Safaricom Daraja) — STK Push callback
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/mpesa")
    public ResponseEntity<?> mpesaCallback(@RequestBody String rawBody,
                                            @RequestHeader Map<String, String> headers,
                                            HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String sourceIp = getClientIp(request);
        WebhookLog logEntry = webhookLogService.logReceived(
                "M_PESA", "/webhooks/mpesa", sourceIp, rawBody, extractHeaders(headers));

        // Vérification sécurité
        if (!verifier.verifyMpesa(rawBody, sourceIp, props.getMpesaWebhookSecret())) {
            webhookLogService.markVerified(logEntry, false, elapsed(start));
            log.warn("[Webhook:M-Pesa] Vérification échouée — refusé depuis {}", sourceIp);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid M-Pesa callback source"));
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            if (stkCallback.isMissingNode()) {
                webhookLogService.markError(logEntry, "Body.stkCallback manquant", 400, elapsed(start));
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid M-Pesa callback format"));
            }

            String checkoutRequestId = stkCallback.has("CheckoutRequestID")
                    ? stkCallback.get("CheckoutRequestID").asText() : null;
            String merchantRequestId = stkCallback.has("MerchantRequestID")
                    ? stkCallback.get("MerchantRequestID").asText() : null;
            int resultCode = stkCallback.has("ResultCode")
                    ? stkCallback.get("ResultCode").asInt(-1) : -1;
            String resultDesc = stkCallback.has("ResultDesc")
                    ? stkCallback.get("ResultDesc").asText() : null;

            String reference = checkoutRequestId != null ? checkoutRequestId : merchantRequestId;
            if (reference == null) {
                webhookLogService.markError(logEntry, "No reference in callback", 400, elapsed(start));
                return ResponseEntity.badRequest().body(Map.of("error", "No reference in callback"));
            }

            boolean success = resultCode == 0;
            String reason = success ? null : (resultDesc != null ? resultDesc : "M-Pesa ResultCode=" + resultCode);

            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);
            webhookLogService.markProcessed(logEntry, reference, result.getId(), 200,
                    "{\"status\":\"" + result.getStatus().name() + "\"}", elapsed(start));

            log.info("[Webhook:M-Pesa] ✅ ref={} — code={} — {}ms", reference, resultCode, elapsed(start));
            return ResponseEntity.ok(Map.of("status", result.getStatus().name(), "reference", reference));
        } catch (Exception e) {
            webhookLogService.markError(logEntry, e.getMessage(), 500, elapsed(start));
            log.error("[Webhook:M-Pesa] Erreur traitement callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Orange Money — Web Payment notification + HMAC-SHA256
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/orange")
    public ResponseEntity<?> orangeCallback(@RequestBody String rawBody,
                                             @RequestHeader(value = "X-Orange-Signature", required = false) String signature,
                                             @RequestHeader Map<String, String> headers,
                                             HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String sourceIp = getClientIp(request);
        WebhookLog logEntry = webhookLogService.logReceived(
                "ORANGE_MONEY", "/webhooks/orange", sourceIp, rawBody, extractHeaders(headers));

        // Vérification HMAC-SHA256
        if (!verifier.verifyOrange(rawBody, signature, props.getOrangeMerchantKey())) {
            webhookLogService.markVerified(logEntry, false, elapsed(start));
            log.warn("[Webhook:Orange] Signature HMAC invalide — refusé");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid Orange Money signature"));
        }

        try {
            JsonNode json = objectMapper.readTree(rawBody);
            String payToken = json.has("pay_token") ? json.get("pay_token").asText() : null;
            String orderId = json.has("order_id") ? json.get("order_id").asText() : null;
            String status = json.has("status") ? json.get("status").asText() : "UNKNOWN";

            String reference = payToken != null ? payToken : orderId;
            if (reference == null) {
                webhookLogService.markError(logEntry, "No reference in notification", 400, elapsed(start));
                return ResponseEntity.badRequest().body(Map.of("error", "No reference in notification"));
            }

            boolean success = "SUCCESS".equalsIgnoreCase(status);
            String reason = success ? null : "Orange status: " + status;

            PaymentIntent result = null;
            try {
                result = gatewayService.handleWebhook(reference, success, reason);
            } catch (Exception e) {
                if (orderId != null && !orderId.equals(reference)) {
                    result = gatewayService.handleWebhook(orderId, success, reason);
                    reference = orderId;
                } else {
                    throw e;
                }
            }

            webhookLogService.markProcessed(logEntry, reference, result.getId(), 200,
                    "{\"status\":\"" + result.getStatus().name() + "\"}", elapsed(start));

            log.info("[Webhook:Orange] ✅ ref={} — status={} — {}ms", reference, status, elapsed(start));
            return ResponseEntity.ok(Map.of("status", result.getStatus().name(), "reference", reference));
        } catch (Exception e) {
            webhookLogService.markError(logEntry, e.getMessage(), 500, elapsed(start));
            log.error("[Webhook:Orange] Erreur traitement notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MTN MoMo — Vérification manuelle signée
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/mtn/verify")
    public ResponseEntity<?> mtnManualVerify(@RequestBody Map<String, Object> body,
                                              @RequestHeader(value = "X-MTN-Signature", required = false) String signature,
                                              @RequestHeader Map<String, String> headers,
                                              HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String sourceIp = getClientIp(request);
        String bodyStr = body.toString();
        WebhookLog logEntry = webhookLogService.logReceived(
                "MTN_MOMO", "/webhooks/mtn/verify", sourceIp, bodyStr, extractHeaders(headers));

        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) {
            webhookLogService.markError(logEntry, "Missing 'reference' field", 400, elapsed(start));
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
        }

        // Vérification HMAC-SHA256
        if (!verifier.verifyMtn(reference, signature, props.getMtnSubscriptionKey())) {
            webhookLogService.markVerified(logEntry, false, elapsed(start));
            log.warn("[Webhook:MTN] Signature invalide pour ref={} — refusé", reference);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid MTN signature"));
        }

        try {
            MobileMoneyProvider mtnProvider = providerRegistry.find(PaymentIntent.Operator.MTN_MOMO);
            if (mtnProvider != null) {
                MobileMoneyProvider.Verification verification = mtnProvider.verify(reference);
                PaymentIntent result = gatewayService.handleWebhook(
                        reference, verification.paid(),
                        verification.paid() ? null : verification.failureReason());

                webhookLogService.markProcessed(logEntry, reference, result.getId(), 200,
                        "{\"status\":\"" + result.getStatus().name() + "\",\"operatorStatus\":\""
                                + verification.operatorStatus() + "\"}", elapsed(start));

                log.info("[Webhook:MTN] ✅ ref={} — paid={} — {}ms", reference, verification.paid(), elapsed(start));
                return ResponseEntity.ok(Map.of(
                        "status", result.getStatus().name(),
                        "reference", reference,
                        "operatorStatus", verification.operatorStatus()));
            }

            boolean success = Boolean.TRUE.equals(body.get("success"));
            String reason = body.get("reason") != null ? body.get("reason").toString() : null;
            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);

            webhookLogService.markProcessed(logEntry, reference, result.getId(), 200,
                    "{\"status\":\"" + result.getStatus().name() + "\"}", elapsed(start));
            return ResponseEntity.ok(Map.of("status", result.getStatus().name(), "reference", reference));
        } catch (Exception e) {
            webhookLogService.markError(logEntry, e.getMessage(), 500, elapsed(start));
            log.error("[Webhook:MTN] Erreur vérification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Webhook générique — HMAC-SHA256 du body
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/generic")
    public ResponseEntity<?> genericCallback(@RequestBody String rawBody,
                                              @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                              @RequestHeader Map<String, String> headers,
                                              HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String sourceIp = getClientIp(request);
        WebhookLog logEntry = webhookLogService.logReceived(
                "GENERIC", "/webhooks/generic", sourceIp, rawBody, extractHeaders(headers));

        if (!verifier.verifyGeneric(rawBody, signature, props.getOrangeWebhookSecret())) {
            webhookLogService.markVerified(logEntry, false, elapsed(start));
            log.warn("[Webhook:Generic] Signature invalide — refusé");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        try {
            JsonNode json = objectMapper.readTree(rawBody);
            String reference = json.has("reference") ? json.get("reference").asText() : null;
            if (reference == null) {
                webhookLogService.markError(logEntry, "Missing 'reference' field", 400, elapsed(start));
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
            }
            boolean success = json.has("success") && json.get("success").asBoolean();
            String reason = json.has("reason") && !json.get("reason").isNull()
                    ? json.get("reason").asText() : null;

            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);
            webhookLogService.markProcessed(logEntry, reference, result.getId(), 200,
                    "{\"status\":\"" + result.getStatus().name() + "\"}", elapsed(start));

            log.info("[Webhook:Generic] ✅ ref={} — success={} — {}ms", reference, success, elapsed(start));
            return ResponseEntity.ok(Map.of("status", result.getStatus().name(), "reference", reference));
        } catch (Exception e) {
            webhookLogService.markError(logEntry, e.getMessage(), 500, elapsed(start));
            log.error("[Webhook:Generic] Erreur traitement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API lecture des logs (admin only)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Liste paginée des logs webhook avec filtres optionnels. */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<?> listLogs(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WebhookLog> logs = webhookLogService.search(provider, status, page, size);
        return ResponseEntity.ok(Map.of(
                "content", logs.getContent(),
                "totalElements", logs.getTotalElements(),
                "totalPages", logs.getTotalPages(),
                "currentPage", logs.getNumber()));
    }

    /** Stats globales des webhooks (admin only). */
    @GetMapping("/logs/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> logStats() {
        return ResponseEntity.ok(webhookLogService.stats());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilitaires
    // ═══════════════════════════════════════════════════════════════════════════

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private int elapsed(long start) {
        return (int) (System.currentTimeMillis() - start);
    }

    /** Extrait les headers pertinents pour le log (sanitisés). */
    private Map<String, String> extractHeaders(Map<String, String> headers) {
        if (headers == null) return Map.of();
        Set<String> interesting = Set.of(
                "content-type", "x-forwarded-for", "x-real-ip",
                "x-orange-signature", "x-mtn-signature", "x-webhook-signature",
                "user-agent", "host");
        return headers.entrySet().stream()
                .filter(e -> interesting.contains(e.getKey().toLowerCase()))
                .collect(Collectors.toMap(Map.Entry::getKey, e ->
                        e.getValue() != null && e.getValue().length() > 200
                                ? e.getValue().substring(0, 200) + "…" : e.getValue()));
    }
}

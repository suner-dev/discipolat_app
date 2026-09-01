package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.*;
import com.discipolat.modules.payments.domain.PaymentProviderProperties;
import com.discipolat.modules.payments.domain.WebhookSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhooks opérateurs Mobile Money — réceptionne les callbacks de confirmation.
 *
 * <p>Chaque opérateur a son propre format de callback et son mécanisme de
 * vérification :</p>
 * <ul>
 *   <li><strong>M-Pesa (Safaricom)</strong> : POST JSON stkCallback + IP whitelisting</li>
 *   <li><strong>Orange Money</strong> : POST JSON + HMAC-SHA256 dans X-Orange-Signature</li>
 *   <li><strong>MTN MoMo</strong> : pas de webhook natif — HMAC sur l'endpoint manuel</li>
 *   <li><strong>Générique</strong> : HMAC-SHA256 dans X-Webhook-Signature</li>
 * </ul>
 *
 * <p>Sécurité : chaque webhook vérifie la signature/origine. Sans secret configuré,
 * la vérification est désactivée (mode sandbox). En production, un secret est
 * OBLIGATOIRE. Un webhook reçu deux fois est idempotent.</p>
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGatewayService gatewayService;
    private final PaymentProviderProperties props;
    private final MobileMoneyProviderRegistry providerRegistry;
    private final WebhookSignatureVerifier verifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentGatewayService gatewayService,
                                     PaymentProviderProperties props,
                                     MobileMoneyProviderRegistry providerRegistry,
                                     WebhookSignatureVerifier verifier) {
        this.gatewayService = gatewayService;
        this.props = props;
        this.providerRegistry = providerRegistry;
        this.verifier = verifier;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-Pesa (Safaricom Daraja) — STK Push callback
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Callback M-Pesa reçu par Safaricom après validation STK Push.
     *
     * <p>Sécurité : vérification IP source + lookup référence en base.</p>
     *
     * <p>Format Safaricom :
     * <pre>{
     *   "Body": {
     *     "stkCallback": {
     *       "MerchantRequestID": "...",
     *       "CheckoutRequestID": "wsCO_{id}",
     *       "ResultCode": 0,
     *       "ResultDesc": "The service request is processed successfully."
     *     }
     *   }
     * }</pre></p>
     */
    @PostMapping("/mpesa")
    public ResponseEntity<?> mpesaCallback(@RequestBody String rawBody,
                                            HttpServletRequest request) {
        String sourceIp = getClientIp(request);
        log.info("[Webhook:M-Pesa] Callback reçu depuis {}", sourceIp);

        // Vérification sécurité M-Pesa (IP +TLS)
        if (!verifier.verifyMpesa(rawBody, sourceIp, props.getMpesaWebhookSecret())) {
            log.warn("[Webhook:M-Pesa] Vérification échouée — IP={}, refusé", sourceIp);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid M-Pesa callback source"));
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            if (stkCallback.isMissingNode()) {
                log.warn("[Webhook:M-Pesa] Format inattendu — Body.stkCallback manquant");
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
                log.warn("[Webhook:M-Pesa] Pas de référence dans le callback");
                return ResponseEntity.badRequest().body(Map.of("error", "No reference in callback"));
            }

            boolean success = resultCode == 0;
            String reason = success ? null : (resultDesc != null ? resultDesc : "M-Pesa ResultCode=" + resultCode);

            log.info("[Webhook:M-Pesa] ref={} — code={} — success={} — ip={}", reference, resultCode, success, sourceIp);
            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);

            return ResponseEntity.ok(Map.of(
                    "status", result.getStatus().name(),
                    "reference", reference));
        } catch (Exception e) {
            log.error("[Webhook:M-Pesa] Erreur traitement callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Orange Money — Web Payment notification + HMAC-SHA256
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Notification Orange Money après paiement web.
     *
     * <p>Sécurité : HMAC-SHA256 du body dans le header X-Orange-Signature,
     * signé avec le merchant key.</p>
     *
     * <p>Format Orange :
     * <pre>{
     *   "status": "SUCCESS",
     *   "pay_token": "...",
     *   "notif_token": "...",
     *   "order_id": "OM-240822-XXXX",
     *   "amount": "5000",
     *   "currency": "XOF"
     * }</pre></p>
     */
    @PostMapping("/orange")
    public ResponseEntity<?> orangeCallback(@RequestBody String rawBody,
                                             @RequestHeader(value = "X-Orange-Signature", required = false) String signature,
                                             HttpServletRequest request) {
        log.info("[Webhook:Orange] Notification reçue depuis {}", getClientIp(request));

        // Vérification HMAC-SHA256 avec le merchant key
        if (!verifier.verifyOrange(rawBody, signature, props.getOrangeMerchantKey())) {
            log.warn("[Webhook:Orange] Signature HMAC invalide — refusé");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid Orange Money signature"));
        }

        try {
            JsonNode json = objectMapper.readTree(rawBody);

            String payToken = json.has("pay_token") ? json.get("pay_token").asText() : null;
            String orderId = json.has("order_id") ? json.get("order_id").asText() : null;
            String status = json.has("status") ? json.get("status").asText() : "UNKNOWN";

            // La référence peut être pay_token ou order_id (notre référence locale)
            String reference = payToken != null ? payToken : orderId;
            if (reference == null) {
                log.warn("[Webhook:Orange] Pas de référence dans la notification");
                return ResponseEntity.badRequest().body(Map.of("error", "No reference in notification"));
            }

            boolean success = "SUCCESS".equalsIgnoreCase(status);
            String reason = success ? null : "Orange status: " + status;

            log.info("[Webhook:Orange] ref={} — status={} — success={}", reference, status, success);

            // Orange envoie le pay_token comme référence, mais notre intent peut avoir
            // soit le pay_token soit la référence locale comme providerReference.
            PaymentIntent result = null;
            try {
                result = gatewayService.handleWebhook(reference, success, reason);
            } catch (Exception e) {
                if (orderId != null && !orderId.equals(reference)) {
                    log.info("[Webhook:Orange] Tentative avec order_id={}", orderId);
                    result = gatewayService.handleWebhook(orderId, success, reason);
                } else {
                    throw e;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", result.getStatus().name(),
                    "reference", reference));
        } catch (Exception e) {
            log.error("[Webhook:Orange] Erreur traitement notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MTN MoMo — Vérification manuelle signée
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Endpoint de vérification manuelle pour MTN MoMo, protégé par HMAC.
     *
     * <p>MTN ne fournit pas de callback push. Ce endpoint permet au scheduler
     * ou à un tiers de vérifier/pousser la confirmation. La requête doit
     * être signée avec HMAC-SHA256 dans le header {@code X-MTN-Signature}.</p>
     *
     * <p>Signature = HMAC-SHA256(reference, subscription_key)</p>
     */
    @PostMapping("/mtn/verify")
    public ResponseEntity<?> mtnManualVerify(@RequestBody Map<String, Object> body,
                                              @RequestHeader(value = "X-MTN-Signature", required = false) String signature) {
        log.info("[Webhook:MTN] Vérification manuelle reçue");

        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
        }

        // Vérification HMAC-SHA256 de la référence
        if (!verifier.verifyMtn(reference, signature, props.getMtnSubscriptionKey())) {
            log.warn("[Webhook:MTN] Signature invalide pour ref={} — refusé", reference);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid MTN signature"));
        }

        try {
            // Vérification via le provider MTN réel si configuré
            MobileMoneyProvider mtnProvider = providerRegistry.find(PaymentIntent.Operator.MTN_MOMO);
            if (mtnProvider != null) {
                MobileMoneyProvider.Verification verification = mtnProvider.verify(reference);
                log.info("[Webhook:MTN] Vérification API — paid={} status={}", verification.paid(), verification.operatorStatus());

                PaymentIntent result = gatewayService.handleWebhook(
                        reference,
                        verification.paid(),
                        verification.paid() ? null : verification.failureReason());

                return ResponseEntity.ok(Map.of(
                        "status", result.getStatus().name(),
                        "reference", reference,
                        "operatorStatus", verification.operatorStatus()));
            }

            // Fallback : traitement direct sans appel API
            boolean success = Boolean.TRUE.equals(body.get("success"));
            String reason = body.get("reason") != null ? body.get("reason").toString() : null;
            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);

            return ResponseEntity.ok(Map.of(
                    "status", result.getStatus().name(),
                    "reference", reference));
        } catch (Exception e) {
            log.error("[Webhook:MTN] Erreur vérification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Webhook générique — HMAC-SHA256 du body
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Webhook générique — format unifié pour tous les opérateurs.
     *
     * <p>Sécurité : HMAC-SHA256 du body dans le header {@code X-Webhook-Signature},
     * signé avec le secret partagé {@code app.payments.webhook-secret}.</p>
     *
     * <p>Body : {@code { "reference": "...", "success": true/false, "reason": "..." }}</p>
     */
    @PostMapping("/generic")
    public ResponseEntity<?> genericCallback(@RequestBody String rawBody,
                                              @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        // Vérification HMAC-SHA256
        if (!verifier.verifyGeneric(rawBody, signature, props.getOrangeWebhookSecret())) {
            log.warn("[Webhook:Generic] Signature invalide — refusé");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        try {
            JsonNode json = objectMapper.readTree(rawBody);
            String reference = json.has("reference") ? json.get("reference").asText() : null;
            if (reference == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
            }
            boolean success = json.has("success") && json.get("success").asBoolean();
            String reason = json.has("reason") && !json.get("reason").isNull()
                    ? json.get("reason").asText() : null;

            log.info("[Webhook:Generic] ref={} — success={}", reference, success);
            PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);
            return ResponseEntity.ok(Map.of(
                    "status", result.getStatus().name(),
                    "reference", reference));
        } catch (Exception e) {
            log.error("[Webhook:Generic] Erreur traitement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing error"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilitaires
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Extrait l'IP réelle du client (en tenant compte des proxies/load balancers).
     */
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
}

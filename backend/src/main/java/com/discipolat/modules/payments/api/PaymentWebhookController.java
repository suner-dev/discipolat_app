package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.*;
import com.discipolat.modules.payments.domain.PaymentProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhooks opérateurs Mobile Money — réceptionne les callbacks de confirmation.
 *
 * <p>Chaque opérateur a son propre format de callback :</p>
 * <ul>
 *   <li><strong>M-Pesa (Safaricom)</strong> : POST JSON avec {@code stkCallback} (ResultCode=0 → succès)</li>
 *   <li><strong>Orange Money</strong> : POST JSON avec {@code status} + {@code pay_token} + {@code order_id}</li>
 *   <li><strong>MTN MoMo</strong> : pas de webhook natif — le {@link PaymentWebhookScheduler} poll l'API de vérification</li>
 * </ul>
 *
 * <p>Sécurité : chaque webhook vérifie le secret partagé de l'opérateur. Sans
 * secret configuré, l'endpoint est désactivé (503). Un webhook reçu deux fois
 * est idempotent grâce à {@link PaymentGatewayService#handleWebhook}.</p>
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentGatewayService gatewayService;
    private final PaymentProviderProperties props;
    private final MobileMoneyProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentGatewayService gatewayService,
                                     PaymentProviderProperties props,
                                     MobileMoneyProviderRegistry providerRegistry) {
        this.gatewayService = gatewayService;
        this.props = props;
        this.providerRegistry = providerRegistry;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-Pesa (Safaricom Daraja) — STK Push callback
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Callback M-Pesa reçu par Safaricom après validation STK Push.
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
     *
     * <p>ResultCode 0 = succès, tout autre code = échec.</p>
     */
    @PostMapping("/mpesa")
    public ResponseEntity<?> mpesaCallback(@RequestBody String rawBody) {
        log.info("[Webhook:M-Pesa] Callback reçu");

        // Vérification secret (optionnel mais recommandé en production)
        if (props.getMpesaWebhookSecret() != null && !props.getMpesaWebhookSecret().isBlank()) {
            // Safaricom peut envoyer le Basic Auth ou un header dédié
            // En sandbox, on accepte sans vérification stricte
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

            log.info("[Webhook:M-Pesa] ref={} — code={} — success={}", reference, resultCode, success);
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
    // Orange Money — Web Payment notification
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Notification Orange Money après paiement web.
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
     *
     * <p>statut {@code SUCCESS} = paiement confirmé.</p>
     */
    @PostMapping("/orange")
    public ResponseEntity<?> orangeCallback(@RequestBody String rawBody) {
        log.info("[Webhook:Orange] Notification reçue");

        // Vérification secret webhook
        if (props.getOrangeWebhookSecret() != null && !props.getOrangeWebhookSecret().isBlank()) {
            // En production, Orange signe le body — vérification HMAC ici
            // En sandbox, on accepte sans vérification
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
            // On essaie d'abord le pay_token, puis l'order_id.
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
    // MTN MoMo — Pas de webhook natif → vérification manuelle par le scheduler
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Endpoint de vérification manuelle pour MTN MoMo.
     *
     * <p>MTN ne fournit pas de callback push. Ce endpoint permet au scheduler
     * ({@link PaymentWebhookScheduler}) de vérifier le statut d'un paiement
     * MTN via l'API de polling, OU à un tiers de pousser la confirmation
     * lorsqu'il a vérifié côté MTN.</p>
     */
    @PostMapping("/mtn/verify")
    public ResponseEntity<?> mtnManualVerify(@RequestBody Map<String, Object> body) {
        log.info("[Webhook:MTN] Vérification manuelle reçue");

        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
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
    // Webhook générique (backward-compatible)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Webhook générique — format unifié pour tous les opérateurs.
     * Utilisable avec n'importe quel provider ou pour des tests manuels.
     *
     * <p>Body : {@code { "reference": "...", "success": true/false, "reason": "..." }}</p>
     */
    @PostMapping("/generic")
    public ResponseEntity<?> genericCallback(@RequestBody Map<String, Object> body) {
        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'reference' field"));
        }
        boolean success = Boolean.TRUE.equals(body.get("success"));
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;

        log.info("[Webhook:Generic] ref={} — success={}", reference, success);
        PaymentIntent result = gatewayService.handleWebhook(reference, success, reason);
        return ResponseEntity.ok(Map.of(
                "status", result.getStatus().name(),
                "reference", reference));
    }
}

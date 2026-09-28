package com.discipolat.modules.payments.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint webhook Stripe — reçoit les événements de facturation SaaS.
 *
 * <p>Sécurité : la signature {@code Stripe-Signature} est vérifiée avec le
 * secret du tableau de bord ({@code STRIPE_WEBHOOK_SECRET}) sur le corps brut
 * (fail-closed : 503 si le secret n'est pas configuré, jamais de traitement
 * non signé). L'endpoint est public dans SecurityConfig mais inexploitable
 * sans signature valide.</p>
 *
 * <p>Idempotence : un événement déjà traité répond 200 sans re-jouer le
 * traitement (Stripe retente jusqu'à 24 h en cas de réponse non-2xx).</p>
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeBillingProperties props;
    private final StripeBillingWebhookService webhookService;

    public StripeWebhookController(StripeBillingProperties props,
                                   StripeBillingWebhookService webhookService) {
        this.props = props;
        this.webhookService = webhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, Object>> stripeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            HttpServletRequest request) {

        if (!props.isWebhookEnabled()) {
            log.warn("[Stripe:Webhook] STRIPE_WEBHOOK_SECRET non configuré — endpoint désactivé");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Stripe webhook not configured"));
        }
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing Stripe-Signature header"));
        }

        Event event;
        try {
            event = Webhook.constructEvent(rawBody, signature, props.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("[Stripe:Webhook] signature invalide — refusé depuis {}", clientIp(request));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid signature"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid payload"));
        }

        try {
            boolean processed = webhookService.handle(event, clientIp(request), rawBody);
            if (!processed) {
                return ResponseEntity.ok(Map.of("received", true, "duplicate", true));
            }
            return ResponseEntity.ok(Map.of("received", true));
        } catch (RuntimeException e) {
            // 500 : Stripe retentera la livraison (le statut ERROR est tracé).
            log.error("[Stripe:Webhook] échec traitement événement {}", event.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Webhook processing failed"));
        }
    }

    private String clientIp(HttpServletRequest request) {
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

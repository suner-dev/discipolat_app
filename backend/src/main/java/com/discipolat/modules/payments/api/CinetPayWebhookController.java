package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.CinetPayProvider;
import com.discipolat.modules.payments.domain.MobileMoneyProvider;
import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.PaymentProviderProperties;
import com.discipolat.modules.payments.domain.SubscriptionCheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook de notification CinetPay (notify_url) — Tithe &amp; Offering 2.0.
 *
 * <p><strong>Sécurité : la source de vérité est l'API, pas le notify.</strong>
 * Le notify CPS n'est pas signé de façon fiable. Le endpoint ne se fie donc
 * JAMAIS au champ {@code status_code} reçu : il re-valide la transaction auprès
 * de {@code POST /validate} (cf. {@link CinetPayProvider#verifyTransaction}) et
 * ne confirme l'intention que si CinetPay répond {@code ACCEPTED}. Un notify
 * falsifié ne peut donc pas marquer un paiement comme payé.</p>
 *
 * <p><strong>Fail-closed.</strong> Tant que CinetPay n'est pas configuré
 * (clés absentes), l'endpoint répond 503 et ne touche aucune donnée — comme le
 * webhook générique qui refuse sans secret.</p>
 *
 * <p><strong>Idempotence.</strong> {@link PaymentGatewayService#handleWebhook}
 * est idempotent (une intention déjà CONFIRMED n'est pas recalculée), donc les
 * re-jeux du notify sont sans effet de bord.</p>
 */
@RestController
@RequestMapping("/api/v1/payments/cinetpay")
public class CinetPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(CinetPayWebhookController.class);

    private final PaymentGatewayService gatewayService;
    private final CinetPayProvider cinetPayProvider;
    private final PaymentProviderProperties props;
    private final SubscriptionCheckoutService subscriptionCheckoutService;

    public CinetPayWebhookController(PaymentGatewayService gatewayService,
                                     CinetPayProvider cinetPayProvider,
                                     PaymentProviderProperties props,
                                     SubscriptionCheckoutService subscriptionCheckoutService) {
        this.gatewayService = gatewayService;
        this.cinetPayProvider = cinetPayProvider;
        this.props = props;
        this.subscriptionCheckoutService = subscriptionCheckoutService;
    }

    /**
     * Réception du notify CPS. Le corps est du {@code x-www-form-urlencoded}
     * (format historique CinetPay) ; une variante JSON est tolérée. La décision
     * de confirmation vient exclusivement du re-{@code /validate} serveur.
     */
    @PostMapping("/notify")
    public ResponseEntity<?> notify(@RequestParam(required = false) Map<String, String> formParams,
                                     @RequestBody(required = false) Map<String, Object> jsonBody) {
        if (!props.isCinetpayActive()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "CinetPay non configuré — app.mobilemoney.cinetpay.*"));
        }

        String transactionId = extractTransactionId(formParams, jsonBody);
        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "transaction_id absent"));
        }

        // Re-validation auprès de l'API CinetPay : seule autorité du « payé ».
        MobileMoneyProvider.Verification verification =
                cinetPayProvider.verifyTransaction(transactionId);

        try {
            PaymentIntent intent = gatewayService.handleWebhook(
                    transactionId, verification.paid(), verification.failureReason());
            log.info("[CinetPay] notify transaction_id={} -> {}", transactionId, intent.getStatus());

            // Un paiement d'abonnement confirmé ACTIVE l'abonnement correspondant.
            // La corrélation provider_reference -> caisse est idempotente : un re-jeu
            // de notify ne double-activera pas. En cas d'échec, on marque la caisse.
            if (intent.getPurpose() == PaymentIntent.Purpose.SAAS_SUBSCRIPTION) {
                if (intent.getStatus() == PaymentIntent.Status.CONFIRMED) {
                    subscriptionCheckoutService.confirmActivation(transactionId);
                } else if (intent.getStatus() == PaymentIntent.Status.FAILED) {
                    subscriptionCheckoutService.markFailed(transactionId, verification.failureReason());
                }
            }

            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("status", intent.getStatus().name());
            ack.put("transaction_id", transactionId);
            return ResponseEntity.ok(ack);
        } catch (RuntimeException e) {
            // Intention inconnue / déjà réglée : on acquitte sans 500 pour ne pas
            // déclencher une boucle de retries CPS, mais on journalise.
            log.warn("[CinetPay] notify non traité transaction_id={} : {}", transactionId, e.getMessage());
            return ResponseEntity.ok(Map.of("status", "IGNORED", "transaction_id", transactionId));
        }
    }

    private static String extractTransactionId(Map<String, String> form, Map<String, Object> json) {
        if (form != null) {
            String tid = firstNonBlank(form.get("transaction_id"), form.get("reference"));
            if (tid != null) return tid;
        }
        if (json != null) {
            Object tid = json.get("transaction_id") != null ? json.get("transaction_id") : json.get("reference");
            return tid == null ? null : tid.toString();
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}

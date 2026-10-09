package com.discipolat.modules.platform.api;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.payments.payout.PayoutRequest;
import com.discipolat.modules.payments.payout.PayoutResult;
import com.discipolat.modules.payments.payout.PayoutService;
import com.discipolat.modules.payments.payout.PayoutStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A3 (M8) — Point d'entrée HTTP du décaissement multi-canaux.
 *
 * <p>Ce contrôleur est ce qui manquait : {@code PayoutProvider} et son registre
 * étaient écrits, testés et documentés, mais <b>aucun endpoint ne les
 * atteignait</b>. Les cinq canaux (Stripe, PayPal, SEPA, virement, Mobile Money)
 * étaient donc inatteignables depuis l'application — y compris par un
 * administrateur qui aurait payé la configuration.</p>
 *
 * <p>Surface exposée, volontairement étroite :</p>
 * <ul>
 *   <li>{@code GET  /status} — diagnostic des canaux (aucun secret) ;</li>
 *   <li>{@code POST /{providerKey}} — décaissement unitaire ;</li>
 *   <li>{@code GET  /{providerKey}/{reference}/status} — réconciliation ;</li>
 *   <li>{@code POST /{providerKey}/webhook} — réception d'un retour opérateur,
 *       avec vérification de signature fail-closed.</li>
 * </ul>
 *
 * <p>Garde : {@code @authz.isTenantAdmin()} — la même vérification par table
 * ({@code tenant_memberships}) que les autres écrans d'administration, et non un
 * {@code hasAnyRole} qui ne lirait que le claim du JWT. Un décaissement depuis
 * l'église A avec un jeton valide sur B doit échouer, sinon c'est un transfert
 * d'argent inter-tenant.</p>
 */
@RestController
@RequestMapping("/api/v1/payments/payouts")
@PreAuthorize("@authz.isTenantAdmin()")
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    /** Diagnostic des canaux de décaissement disponibles. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(payoutService.channelStatus());
    }

    /**
     * Déclenche un décaissement.
     *
     * <p>Le montant est fourni en <b>unité mineure</b> ({@code amountMinor}) :
     * c'est la représentation auditable du SPI. Un montant en devise décimale
     * passerait par un {@code double}, jamais exact, et rendrait la
     * réconciliation multi-devises impossible.</p>
     */
    @PostMapping("/{providerKey}")
    public ResponseEntity<PayoutResult> initiate(@PathVariable String providerKey,
                                                  @RequestBody Map<String, Object> request) {
        String recipient = asString(request.get("recipient"));
        String currency = asString(request.get("currency"));
        String reference = asString(request.get("reference"));
        long amountMinor = asLong(request.get("amountMinor"));
        String idempotencyKey = asString(request.get("idempotencyKey"));
        Map<String, String> metadata = asMetadata(request.get("metadata"));

        PayoutRequest payoutRequest = new PayoutRequest(
                idempotencyKey != null ? idempotencyKey : reference,
                recipient,
                amountMinor,
                currency == null ? null : currency.toUpperCase(),
                reference,
                metadata);

        return ResponseEntity.ok(payoutService.initiate(providerKey, payoutRequest));
    }

    /** Réconciliation : état d'une référence côté fournisseur. */
    @GetMapping("/{providerKey}/{reference}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String providerKey,
                                                       @PathVariable String reference) {
        PayoutStatus status = payoutService.queryStatus(providerKey, reference);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", providerKey);
        body.put("reference", reference);
        // `UNKNOWN` est une réponse LÉGITIME : l'opérateur est momentanément
        // injoignable. La sérialiser en erreur HTTP ferait boucler le job de
        // réconciliation sur une panne temporaire.
        body.put("status", status == null ? PayoutStatus.UNKNOWN.name() : status.name());
        return ResponseEntity.ok(body);
    }

    /**
     * Réception d'un retour opérateur.
     *
     * <p>Répond 202 quoi qu'il soit : le traitement est asynchrone par nature.
     * Mais la signature est vérifiée AVANT toute confiance — un corps non
     * signé ne change aucun état.</p>
     */
    @PostMapping("/{providerKey}/webhook")
    public ResponseEntity<Map<String, Object>> webhook(@PathVariable String providerKey,
                                                        @RequestHeader Map<String, String> headers,
                                                        @RequestBody(required = false) String rawBody) {
        boolean signatureValid = payoutService.verifyWebhookSignature(providerKey, headers, rawBody);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("provider", providerKey);
        body.put("signatureValid", signatureValid);
        body.put("accepted", signatureValid);

        if (!signatureValid) {
            // 400 explicite : le corps est rejeté, l'appelant (l'opérateur) doit
            // le savoir pour ne pas rejouer indéfiniment un événement forgé.
            body.put("reason", "signature absente ou invalide");
            return ResponseEntity.badRequest().body(body);
        }
        return ResponseEntity.accepted().body(body);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            throw new DomainException("amountMinor est requis (unité mineure de la devise)",
                    HttpStatus.BAD_REQUEST, "PAYOUT_AMOUNT_REQUIRED");
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException invalid) {
            throw new DomainException("amountMinor doit être un entier : " + value,
                    HttpStatus.BAD_REQUEST, "PAYOUT_AMOUNT_INVALID");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> asMetadata(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        map.forEach((key, item) -> {
            if (key != null) {
                metadata.put(String.valueOf(key), item == null ? "" : String.valueOf(item));
            }
        });
        return metadata;
    }
}
package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.TaxReceiptService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentGatewayService service;
    private final TaxReceiptService taxReceiptService;
    private final com.discipolat.modules.payments.domain.RecurringDonationService recurringDonationService;

    /** Secret partagé du webhook opérateur (vide en dev → endpoint ouvert). */
    @Value("${app.payments.webhook-secret:}")
    private String webhookSecret;

    public PaymentController(PaymentGatewayService service, TaxReceiptService taxReceiptService,
                             com.discipolat.modules.payments.domain.RecurringDonationService recurringDonationService) {
        this.service = service;
        this.taxReceiptService = taxReceiptService;
        this.recurringDonationService = recurringDonationService;
    }

    /** P12 — Reçu fiscal PDF pour un paiement confirmé. */
    @GetMapping("/{id}/tax-receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> taxReceipt(@PathVariable UUID id) {
        PaymentIntent payment = service.findById(id);
        if (payment.getStatus() != PaymentIntent.Status.CONFIRMED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        byte[] pdf = taxReceiptService.generatePdf(payment);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=recu-fiscal-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /** Démarre un paiement (dîme, offrande, don diaspora…). */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntent> initiate(@RequestBody PaymentIntent intent) {
        return ResponseEntity.ok(service.initiate(intent));
    }

    /**
     * Webhook opérateur (M-Pesa / MTN / Orange…) : confirmation de paiement.
     * Sécurité : si `app.payments.webhook-secret` est configuré, l'appel doit
     * présenter le header `X-Webhook-Secret` correspondant (HMAC partagé).
     * Sans secret configuré (dev/sandbox local), l'endpoint reste ouvert.
     */
    @PostMapping("/webhook")
    public ResponseEntity<PaymentIntent> webhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String providedSecret,
            @RequestBody Map<String, Object> body) {
        if (!webhookSecret.isBlank() && !webhookSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String reference = body.get("reference").toString();
        boolean success = Boolean.TRUE.equals(body.get("success"));
        String reason = body.containsKey("reason") ? body.get("reason").toString() : null;
        return ResponseEntity.ok(service.handleWebhook(reference, success, reason));
    }

    /** Simulation de confirmation (sandbox/dev uniquement). */
    @PostMapping("/{id}/simulate-confirmation")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<PaymentIntent> simulate(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "true") boolean success) {
        PaymentIntent intent = service.findById(id);
        if (intent.getStatus() != PaymentIntent.Status.CONFIRMED) {
            service.handleWebhook(intent.getProviderReference(), success, success ? null : "Simulated failure");
        }
        return ResponseEntity.ok(service.findById(id));
    }

    /** Statut d'un paiement — polling mobile pendant le parcours (auteur ou super-utilisateur). */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntent> status(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findByIdForCurrentUser(id));
    }

    /** Mes paiements — vue « Mes dons » accessible à tous les rôles. */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentIntent>> mine() {
        return ResponseEntity.ok(service.mine());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntent> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<PaymentIntent>> recent() {
        return ResponseEntity.ok(service.recent());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    // === P12 — Dons récurrents ===

    /** Crée un don récurrent (dîme mensuelle, offrande hebdomadaire…). */
    @PostMapping("/recurring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.discipolat.modules.payments.domain.RecurringDonation> createRecurring(
            @RequestBody com.discipolat.modules.payments.domain.RecurringDonation donation) {
        return ResponseEntity.ok(recurringDonationService.create(donation));
    }

    /** Annule un don récurrent. */
    @PostMapping("/recurring/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.discipolat.modules.payments.domain.RecurringDonation> cancelRecurring(
            @PathVariable UUID id) {
        return ResponseEntity.ok(recurringDonationService.cancel(id));
    }

    /** Mes dons récurrents. */
    @GetMapping("/recurring/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> myRecurring() {
        return ResponseEntity.ok(recurringDonationService.mine());
    }

    /** Stats dons récurrents (admin). */
    @GetMapping("/recurring/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> recurringStats() {
        return ResponseEntity.ok(recurringDonationService.stats());
    }
}

package com.discipolat.modules.payments.api;

import com.discipolat.modules.payments.domain.PaymentGatewayService;
import com.discipolat.modules.payments.domain.PaymentIntent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentGatewayService service;

    public PaymentController(PaymentGatewayService service) {
        this.service = service;
    }

    /** Démarre un paiement (dîme, offrande, don diaspora…). */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntent> initiate(@RequestBody PaymentIntent intent) {
        return ResponseEntity.ok(service.initiate(intent));
    }

    /**
     * Webhook opérateur (M-Pesa / MTN / Orange…) : confirmation de paiement.
     * En production : sécurisé par signature HMAC opérateur + IP allowlist.
     */
    @PostMapping("/webhook")
    public ResponseEntity<PaymentIntent> webhook(@RequestBody Map<String, Object> body) {
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

    /** Statut d'un paiement — polling mobile pendant le parcours. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntent> status(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
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
}

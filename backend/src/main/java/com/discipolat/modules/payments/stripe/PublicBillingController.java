package com.discipolat.modules.payments.stripe;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Le frontend ne connaît pas les clés secrètes : cette vue publique indique
 * simplement si la caisse en ligne Stripe est disponible (pour afficher ou
 * masquer les CTA « Souscrire »).
 */
@RestController
@RequestMapping("/api/v1/public/billing")
public class PublicBillingController {

    private final StripeBillingProperties props;

    public PublicBillingController(StripeBillingProperties props) {
        this.props = props;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("stripeEnabled", props.isEnabled()));
    }
}

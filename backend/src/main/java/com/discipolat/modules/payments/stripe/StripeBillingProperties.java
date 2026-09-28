package com.discipolat.modules.payments.stripe;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration de la facturation SaaS Stripe (abonnements carte bancaire).
 *
 * <p>Convention du projet : injection par {@code @Value} et désactivation par
 * défaut — ni clé ni webhook ne sont actifs tant que {@code STRIPE_SECRET_KEY}
 * / {@code STRIPE_WEBHOOK_SECRET} ne sont pas fournis. Le webhook est
 * fail-closed : sans secret, aucun événement n'est accepté (503).</p>
 */
@Component
@Getter
public class StripeBillingProperties {

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.stripe.success-url:http://localhost:5173/billing?checkout=success}")
    private String successUrl;

    @Value("${app.stripe.cancel-url:http://localhost:5173/pricing?checkout=cancelled}")
    private String cancelUrl;

    /** La facturation Stripe est active dès qu'une clé secrète est configurée. */
    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    /** Le webhook n'accepte du trafic que si le secret de signature est connu. */
    public boolean isWebhookEnabled() {
        return isEnabled() && webhookSecret != null && !webhookSecret.isBlank();
    }
}

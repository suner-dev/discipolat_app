package com.discipolat.modules.payments.payout;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A3 (M8) — Configuration declarative des fournisseurs de décaissement.
 *
 * <p>Chaque fournisseur est sous un flag dédié, désactivé par défaut :</p>
 * <pre>
 * app.payments.providers.stripe.enabled=false
 * app.payments.providers.paypal.enabled=false
 * app.payments.providers.sepa.enabled=false
 * app.payments.providers.bank.enabled=false
 * app.payments.providers.mtn.enabled=false   # décaissement Mobile Money
 * app.payments.providers.orange.enabled=false
 * app.payments.providers.mpesa.enabled=false
 * </pre>
 *
 * <p>Aucun secret n'a de valeur par défaut : les clés/API secrets viennent
 * exclusivement de variables d'environnement. Un fournisseur {@code enabled}
 * sans credentials reste inactif ({@link PayoutProvider#isEnabled()} ==
 * false) — c'est la définition d'un fail-closed utile plutôt que d'un
 * démarrage qui casse.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.payments.providers")
public class PayoutProvidersProperties {

    private final Provider stripe = new Provider();
    private final Provider paypal = new Provider();
    private final Provider sepa = new Provider();
    private final Provider bank = new Provider();
    private final Provider mtn = new Provider();
    private final Provider orange = new Provider();
    private final Provider mpesa = new Provider();

    /**
     * Bloc générique : les fournisseurs n'ont pas tous le même vocabulaire de
     * credentials, mais tous ont (au moins) une clé d'identification, un
     * secret et une URL de base.
     */
    @Getter
    @Setter
    public static class Provider {
        /** Flag d'activation — défaut false (fail-closed). */
        private boolean enabled = false;
        /** identifiant d'application (Stripe api key, PayPal client-id, …). */
        private String apiKey = "";
        /** secret partagé (Stripe webhook signing secret, PayPal client-secret, …). */
        private String apiSecret = "";
        /** URL de base de l'API fournisseur ; vide = URL officielle par défaut selon le provider. */
        private String baseUrl = "";
        /** secret de vérification des webhooks, distinct du secret d'API si le fournisseur les sépare. */
        private String webhookSecret = "";
    }
}

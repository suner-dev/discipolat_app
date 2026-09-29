package com.discipolat.modules.payments.payout;

import java.util.Map;

/**
 * A3 (M8) — SPI « décaissement universel ».
 *
 * <p>Le produit ne doit plus être structuré autour de l'Afrique francophone
 * seule : tout fournisseur de sortie d'argent (Mobile Money, Stripe, PayPal,
 * prélèvement SEPA, virement bancaire) implémente cette interface et est
 * activable par configuration, désactivé par défaut (règle fail-closed §3.5
 * d'AGENT_ORCHESTRATION).</p>
 *
 * <p>Règles communes à toutes les implémentations :</p>
 * <ul>
 *   <li>{@link #isEnabled()} est la vérité configuration : un provider
 *       {@code enabled} sans credentials n'est PAS actif — le registre ne le
 *       route pas et le statut affiché est {@code configured=false}.</li>
 *   <li>Jamais de succès simulé : sans configuration réelle,
 *       {@link #initiate} lève une {@code DomainException} 503 avec un code
 *       explicite (précédent : {@code STT_NOT_CONFIGURED}).</li>
 *   <li>{@link #verifyWebhookSignature} est fail-closed : sans secret
 *       configuré ou sans signature valide, {@code false}.</li>
 * </ul>
 */
public interface PayoutProvider {

    /** Clé canonique : {@code stripe}, {@code paypal}, {@code sepa}, {@code bank}, {@code mtn}, {@code orange}, {@code mpesa}. */
    String key();

    /** Actif = flag de configuration ET credentials réellement exploitables. */
    boolean isEnabled();

    /**
     * Déclenche un décaissement réel auprès du fournisseur.
     *
     * @throws com.discipolat.common.exception.DomainException 503 si le
     *         fournisseur n'est pas configuré pour le décaissement, 502 en cas
     *         d'échec de la passerelle fournisseur.
     */
    PayoutResult initiate(PayoutRequest request);

    /** Interroge le statut d'une référence fournisseur. Ne lève pas : {@link PayoutStatus#UNKNOWN} si inaccessible. */
    PayoutStatus queryStatus(String providerReference);

    /** Vérification de signature de webhook, fail-closed (voir règles ci-dessus). */
    boolean verifyWebhookSignature(Map<String, String> headers, String rawBody);

    /** Raison honnête quand {@code isEnabled()} est faux ; {@code null} sinon. Expose au diagnostic, jamais un secret. */
    default String disabledReason() {
        return null;
    }
}

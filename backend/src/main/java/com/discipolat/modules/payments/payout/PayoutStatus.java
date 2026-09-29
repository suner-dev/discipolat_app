package com.discipolat.modules.payments.payout;

/**
 * A3 (M8) — Statut canonique d'un décaissement, indépendamment du vocabulaire
 * propre à chaque fournisseur (Stripe « paid », PayPal « SUCCESS », opérateur
 * mobile « Confirmed »…). La mapping depuis le statut brut du fournisseur
 * appartient à chaque implémentation de {@link PayoutProvider}.
 *
 * <p>{@link #UNKNOWN} n'est pas un échec : c'est l'état honnête quand le
 * fournisseur ne peut pas être interrogé (référence inconnue, service
 * injoignable). Un payout n'est JAMAIS marqué SETTLED par défaut.</p>
 */
public enum PayoutStatus {
    /** Accepté par le fournisseur, pas encore réglé. */
    PENDING,
    /** Réglé — l'argent a quitté (ou atteint) le compte, selon le canal. */
    SETTLED,
    /** Refusé ou définitivement échoué. */
    FAILED,
    /** État non connu du fournisseur — interrogation à retenter. */
    UNKNOWN
}

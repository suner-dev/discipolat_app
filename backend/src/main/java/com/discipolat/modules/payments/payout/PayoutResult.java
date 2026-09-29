package com.discipolat.modules.payments.payout;

/**
 * A3 (M8) — Résultat d'une initiation de décaissement.
 *
 * @param providerReference référence côté fournisseur (idempotente côté
 *                          métier via {@code PayoutRequest.reference})
 * @param status            statut canonique au moment de la réponse
 * @param detail            message lisible — en cas d'échec, la raison
 *                          renvoyée par le fournisseur, jamais un texte
 *                          inventé
 */
public record PayoutResult(String providerReference, PayoutStatus status, String detail) {

    public static PayoutResult pending(String providerReference, String detail) {
        return new PayoutResult(providerReference, PayoutStatus.PENDING, detail);
    }

    public static PayoutResult failed(String providerReference, String detail) {
        return new PayoutResult(providerReference, PayoutStatus.FAILED, detail);
    }
}

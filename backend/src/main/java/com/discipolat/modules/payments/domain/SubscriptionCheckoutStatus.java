package com.discipolat.modules.payments.domain;

/**
 * Statut d'un passage en caisse d'abonnement SaaS payé par redirection externe
 * (agrégateur CinetPay : Orange Money, MTN MoMo, Wave, Visa/Mastercard).
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité T6.
 *
 * <p>Distinct de {@link MomoSubscriptionStatus} (cycle de vie de la tokenization
 * récurrente MTN) : ici on suit une <em>caisse</em> ouverte puis réglée, pas un
 * jeton rejouable.</p>
 */
public enum SubscriptionCheckoutStatus {
    /** Caisse ouverte, paiement en attente sur la page du prestataire. */
    PENDING,
    /** Payé et confirmé au webhook : abonnement du tenant activé. */
    ACTIVE,
    /** Paiement refusé/expiré. */
    FAILED,
    /** Abandon (annulé côté client ou admin). */
    CANCELLED
}

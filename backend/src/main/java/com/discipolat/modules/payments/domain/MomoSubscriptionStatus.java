package com.discipolat.modules.payments.domain;

/**
 * Statut d'un abonnement SaaS via Mobile Money.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.3.
 */
public enum MomoSubscriptionStatus {
    PENDING,      // En attente de premier paiement / tokenization
    ACTIVE,       // Abonnement actif, prélèvements programmés
    FAILED,       // Échec de paiement (réessai possible via scheduler)
    CANCELLED,    // Annulé par l'utilisateur ou admin
    EXPIRED       // Expiré (ex: token invalide, max retries atteint)
}
package com.discipolat.modules.tenants.enums;

/**
 * État de la synchronisation d'un prix de catalogue vers un Price Stripe.
 *
 * <p>Couvre le cas des devises 0-décimales (XAF, XOF, JPY, KRW) que Stripe
 * refuse en {@code price_data} inline : un {@code Price} doit être créé au
 * préalable. L'alternative reste possible — le super-admin peut saisir lui-même
 * l'identifiant et le marquer {@link #MANUAL}.</p>
 */
public enum StripePriceStatus {

    /** Le canal ne passe pas par Stripe, ou la devise est à 2 décimales (prix inline possible). */
    NOT_REQUIRED,
    /** Un Price doit être créé / est en attente de création. */
    PENDING,
    /** Price créé côté Stripe, {@code stripe_price_id} renseigné. */
    SYNCED,
    /** Échec de création : {@code stripe_sync_error} contient la cause. */
    FAILED,
    /** Identifiant saisi manuellement par le super-admin. */
    MANUAL
}
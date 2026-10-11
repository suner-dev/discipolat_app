package com.discipolat.modules.tenants.enums;

/**
 * Source de résolution de la région tarifaire (contrat §2.2).
 *
 * <p>Aucune source n'est active par défaut hors {@link #TENANT} : la géolocalisation
 * par IP est volontairement absente de la v1 (dépendance externe + charge RGPD),
 * et l'en-tête HTTP n'est accepté que si le super-admin active explicitement
 * {@code trust_region_header}.</p>
 */
public enum PricingRegionSource {

    /** Région déclarée par le tenant. */
    TENANT,
    /** En-tête HTTP (edge/reverse-proxy), si {@code trust_region_header} est actif. */
    HEADER,
    /** Paramètre {@code ?region=XX}, si autorisé. */
    QUERY,
    /** Réservé — non implémenté en v1 (RGPD). */
    IP
}
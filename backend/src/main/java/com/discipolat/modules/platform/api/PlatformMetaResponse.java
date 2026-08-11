package com.discipolat.modules.platform.api;

/**
 * Méta-données publiques de la plateforme (sans authentification).
 * Permet au frontend d'adapter son affichage : badge BÊTA, visibilité des
 * comptes de démonstration, version affichée. Aucune donnée sensible.
 */
public record PlatformMetaResponse(
        String appName,
        String version,
        String environment,
        boolean betaMode,
        boolean demoAccountsEnabled
) {}

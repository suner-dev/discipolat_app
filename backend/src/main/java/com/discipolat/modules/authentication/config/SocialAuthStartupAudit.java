package com.discipolat.modules.authentication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Audit de démarrage de l'authentification par identité externe.
 *
 * <p><b>Ce qu'il refuse de faire :</b> lever une exception. Une configuration
 * sociale incomplète n'est pas une faille de la plateforme, c'est une
 * <b>fonctionnalité éteinte</b> : les endpoints répondent 503 et le frontend
 * masque le bouton. Faire tomber l'API entière pour ça rendrait indisponible
 * la connexion par mot de passe, qui reste le chemin principal. Le
 * comportement est donc <b>fail-closed et bruyant</b>, jamais silencieux.
 *
 * <p>Le journal indique sans ambiguïté : fournisseur actif, fournisseur demandé
 * mais inutilisable, et avertissement de sécurité quand Microsoft est ouvert à
 * tous les tenants.
 */
@Component
public class SocialAuthStartupAudit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthStartupAudit.class);

    private final SocialAuthProperties properties;
    private final String environment;

    public SocialAuthStartupAudit(SocialAuthProperties properties,
                                  @Value("${app.environment:dev}") String environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean strict = "prod".equalsIgnoreCase(environment) || "beta".equalsIgnoreCase(environment);
        SocialAuthProperties.Google google = properties.getGoogle();
        SocialAuthProperties.Microsoft microsoft = properties.getMicrosoft();

        if (!google.isEnabled() && !microsoft.isEnabled()) {
            log.info("[SocialAuth] Aucune identité externe activée (Google et Microsoft désactivés) — "
                    + "la connexion par mot de passe reste le seul moyen d'accès.");
            return;
        }

        if (google.isMisconfigured()) {
            log.error(strict
                    ? "[SocialAuth] 🔴 Google activé mais aucun client-id fourni : les endpoints Google "
                    + "répondront 503 et aucun bouton ne doit être affiché."
                    : "[SocialAuth] ⚠️ Google activé mais sans client-id : fournisseur inutilisable.");
        }
        if (microsoft.isMisconfigured()) {
            log.error(strict
                    ? "[SocialAuth] 🔴 Microsoft activé mais client-id vide : les endpoints Microsoft "
                    + "répondront 503 et aucun bouton ne doit être affiché."
                    : "[SocialAuth] ⚠️ Microsoft activé mais sans client-id : fournisseur inutilisable.");
        }

        if (google.isConfigured()) {
            log.info("[SocialAuth] ✅ Google actif (audiences acceptées : {})", google.acceptedAudiences());
        }
        if (microsoft.isConfigured()) {
            String tenant = microsoft.getTenantId();
            log.info("[SocialAuth] ✅ Microsoft actif (tenant : {}, tenants autorisés : {})",
                    tenant,
                    microsoft.getAllowedTenantIds() == null || microsoft.getAllowedTenantIds().isEmpty()
                            ? "aucune restriction"
                            : microsoft.getAllowedTenantIds());
            if ("common".equalsIgnoreCase(tenant)) {
                log.warn("[SocialAuth] 🟠 Microsoft accepte TOUS les tenants (tenantId=common) : "
                        + "une organisation tierce peut connecter ses utilisateurs. Renseignez "
                        + "APP_AUTH_SOCIAL_MICROSOFT_TENANT_ID pour restreindre.");
            }
            if (!strict) {
                log.info("[SocialAuth] ℹ️ environments beta/prod : les identités externes rattachées "
                        + "sont conservées telles quelles ; aucun secret client n'est stocké.");
            }
        }

        if (!properties.isAllowAccountLinking()) {
            log.info("[SocialAuth] ℹ️ Rattachement d'identité désactivé : les comptes existants "
                    + "ne pourront pas utiliser « Se connecter avec Google ».");
        }
    }
}

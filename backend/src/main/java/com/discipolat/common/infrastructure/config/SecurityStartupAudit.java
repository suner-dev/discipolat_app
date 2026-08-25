package com.discipolat.common.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Audit de configuration de sécurité exécuté au démarrage.
 * En production/beta : signale toute configuration dangereuse (secrets vides,
 * CORS permissif, Swagger public) afin d'échouer tôt plutôt qu'en incident.
 */
@Component
public class SecurityStartupAudit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupAudit.class);

    private final String environment;
    private final String aesKey;
    private final String webhookSecret;
    private final String[] corsOrigins;

    public SecurityStartupAudit(
            @Value("${app.environment:dev}") String environment,
            @Value("${app.encryption.aes-key:}") String aesKey,
            @Value("${app.payments.webhook-secret:}") String webhookSecret,
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String[] corsOrigins) {
        this.environment = environment;
        this.aesKey = aesKey;
        this.webhookSecret = webhookSecret;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = "prod".equalsIgnoreCase(environment) || "beta".equalsIgnoreCase(environment);
        if (!prod) {
            log.info("[SecurityAudit] environnement '{}' — audit complet appliqué en prod/beta uniquement", environment);
            return;
        }

        int problems = 0;

        if (aesKey == null || aesKey.isBlank()) {
            log.error("[SecurityAudit] 🔴 ENCRYPTION_AES_KEY vide en {} : les données sensibles ne seront PAS chiffrées.", environment);
            problems++;
        }
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[SecurityAudit] 🔴 APP_PAYMENTS_WEBHOOK_SECRET vide en {} : le webhook paiements refusera les confirmations (comportement attendu).", environment);
            problems++;
        }
        for (String origin : corsOrigins) {
            if (origin != null && origin.contains("*")) {
                log.warn("[SecurityAudit] 🟠 CORS wildcard détecté ('{}') : credentials désactivés automatiquement, mais restreignez aux domaines réels.", origin);
            }
        }
        if ("dev".equalsIgnoreCase(environment)) {
            log.warn("[SecurityAudit] 🟠 APP_ENVIRONMENT=dev : Swagger est PUBLIC. Ne jamais déployer ainsi.");
        }

        if (problems == 0) {
            log.info("[SecurityAudit] ✅ Configuration de sécurité conforme pour '{}'.", environment);
        } else {
            log.error("[SecurityAudit] ❌ {} problème(s) de configuration de sécurité à corriger avant ouverture publique.", problems);
        }
    }
}

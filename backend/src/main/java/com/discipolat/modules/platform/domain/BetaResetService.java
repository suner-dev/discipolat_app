package com.discipolat.modules.platform.domain;

import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.config.DataInitializer;
import com.discipolat.modules.audit.domain.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Réinitialisation de l'environnement de BÊTA-TEST :
 * - suppression de toutes les données créées par les testeurs ;
 * - restauration du jeu de données de démonstration (migration V2) ;
 * - recréation des comptes de test (DataInitializer).
 *
 * STRICTEMENT RÉSERVÉ À L'ENVIRONNEMENT BÊTA : le service refuse de
 * s'exécuter tant que `app.beta-testing.reset-enabled` n'est pas vrai
 * (activé uniquement par le profil `beta`, jamais par défaut ni en prod).
 */
@Service
@Transactional
public class BetaResetService {

    private static final Logger log = LoggerFactory.getLogger(BetaResetService.class);

    /**
     * Tables système/configuration — JAMAIS tronquées par un reset.
     * NB : `audit_logs` possède une FK vers `users` : il est donc emporté par
     * le TRUNCATE ... CASCADE de `users` malgré cette liste. C'est accepté :
     * les actions des testeurs font partie des données de test réinitialisées.
     * `feedbacks` n'a PAS de FK vers `users` → conservé entre deux resets.
     */
    private static final Set<String> KEEP_TABLES = Set.of(
            "flyway_schema_history",
            "church_settings",
            "platform_modules",
            "menu_entries",
            "custom_field_definitions",
            "audit_logs",
            "feedbacks"
    );

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final DataInitializer dataInitializer;
    private final AuditService auditService;

    @Value("${app.beta-testing.reset-enabled:false}")
    private boolean resetEnabled;

    @Value("${app.environment:dev}")
    private String environment;

    public BetaResetService(JdbcTemplate jdbcTemplate, DataSource dataSource,
                            DataInitializer dataInitializer, AuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.dataInitializer = dataInitializer;
        this.auditService = auditService;
    }

    /**
     * Réinitialise les données de l'environnement bêta.
     *
     * NOT_SUPPORTED : le TRUNCATE ne doit PAS rester dans une transaction
     * ouverte (il verrouillerait toutes les tables tronquées et bloquerait
     * la restauration du seed qui tourne sur une autre connexion).
     * Hors transaction, chaque instruction est en auto-commit → les verrous
     * sont libérés immédiatement.
     *
     * @return résumé de l'opération (tables tronquées, statut).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> reset() {
        // Double garde : le flag de réinitialisation ET l'environnement.
        // Même si BETA_RESET_ENABLED était forcé en prod, on refuse ici.
        if ("prod".equalsIgnoreCase(environment)) {
            throw new BadRequestException(
                    "La réinitialisation bêta est strictement interdite sur l'environnement de production.");
        }
        if (!resetEnabled) {
            throw new BadRequestException(
                    "La réinitialisation bêta est désactivée sur cet environnement (" + environment + ").");
        }

        // 1. Tronquer toutes les tables métier (données testeurs), conserver la configuration.
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        List<String> toTruncate = tables.stream()
                .map(String::toLowerCase)
                .filter(t -> !KEEP_TABLES.contains(t))
                .toList();

        if (!toTruncate.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", toTruncate) + " RESTART IDENTITY CASCADE");
        }

        // 2. Restaurer le jeu de données de démonstration.
        // NB : on n'exécute PAS la migration V2 (schéma obsolète depuis V27) —
        // on restaure db/demo/seed-demo.sql, aligné sur le schéma actuel.
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/demo/seed-demo.sql"));
        } catch (Exception e) {
            throw new IllegalStateException("Échec de la restauration des données de démonstration", e);
        }

        // 3. Recréer les comptes de test + espace membre (idempotent).
        dataInitializer.run();

        // Journalisation non bloquante : l'utilisateur déclencheur vient d'être
        // tronqué et recréé (nouvel UUID) → la FK audit_logs.utilisateur_id
        // n'existe plus. Un échec de journalisation ne doit pas annuler le reset.
        try {
            auditService.logSimple("BETA_RESET", "PLATFORM", null);
        } catch (Exception e) {
            log.warn("BETA_RESET non journalisé en base (utilisateur déclencheur tronqué) : {}", e.getMessage());
        }

        log.warn("Environnement bêta réinitialisé : {} tables tronquées, données de démonstration restaurées.",
                toTruncate.size());

        return Map.of(
                "status", "OK",
                "environment", environment,
                "truncatedTables", toTruncate.size(),
                "resetAt", LocalDateTime.now().toString()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status() {
        return Map.of(
                "environment", environment,
                "resetEnabled", resetEnabled
        );
    }
}

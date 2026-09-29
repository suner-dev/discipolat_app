package com.discipolat.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate de non-régression de la chaîne Flyway sur PostgreSQL réel (racine de la
 * famille de bugs H1-H2, NEED-HELP-02 option c, passe-plat Agent B HANDOVER P3).
 *
 * <p>Contexte : le profil de test applicatif tourne sur H2 avec
 * {@code spring.flyway.enabled:false} et {@code ddl-auto:create-drop} — les
 * migrations ne sont donc <b>jamais</b> exécutées par la suite unitaire, et un
 * script valide seulement sur H2, ou jamais testé, peut casser le déploiement
 * réel. Ce test ferme ce trou : il applique V1..V192 sur un PostgreSQL 16 neuf
 * via Testcontainers, puis exige (1) la chaîne complète sans erreur et son
 * idempotence au second passage, (2) la version cible atteinte, (3) la
 * neutralisation effective des comptes de démonstration en bout de chaîne
 * (V192), (4) un {@code validate()} propre — aucune dérive entre le schéma et
 * les métadonnées d'ordre/appliqués.
 *
 * <p>Honnêteté d'exécution : {@code disabledWithoutDocker=true} — sans daemon
 * Docker (CI sans runner containerisé), le test est <b>skip comptabilisé</b>,
 * jamais un faux PASS. C'est le même contrat que les skips Redis-gated de la
 * suite. La preuve d'exécution locale est archivée dans
 * {@code reports/plan-2agents/agentA.md}.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationChainPostgreSqlTest {

    /** Version minimale attendue en bout de chaîne (incrémenter à chaque vague). */
    private static final int EXPECTED_MIN_VERSION = 192;

    // Note d'environnement : Docker Engine 29 refuse les clients d'API < 1.40 et
    // docker-java (shadé par Testcontainers 1.21.0) retombe sur 1.32 sans
    // négociation explicite — le démon est alors déclaré « absent » et cette
    // classe part en SKIP tort (3/3 SKIP mesuré), au lieu des 3/3 PASS verts.
    // La négociation `api.version=1.44` est câblée dans maven-surefire-plugin
    // (backend/pom.xml) car elle doit être posée AVANT l'extension JUnit.

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("discipolat_chain_gate");

    /** Chaîne appliquée une seule fois pour la classe ; les tests partagent cet état. */
    private static MigrateResult firstPass;

    @BeforeAll
    static void applyFullChainOnce() {
        firstPass = newFlyway().migrate();
    }

    @Test
    @DisplayName("La chaîne Flyway V1→V192 s'applique intégralement sur PostgreSQL 16 neuf")
    void fullMigrationChainAppliesOnRealPostgres() {
        assertThat(firstPass.success)
                .as("la chaîne complète V1..V%d doit s'appliquer sans erreur", EXPECTED_MIN_VERSION)
                .isTrue();
        assertThat(firstPass.migrationsExecuted).isGreaterThan(150);
        assertThat(Integer.parseInt(firstPass.targetSchemaVersion))
                .as("version cible ≥ V%d", EXPECTED_MIN_VERSION)
                .isGreaterThanOrEqualTo(EXPECTED_MIN_VERSION);

        // Idempotence : rejouer migrate sur une base à jour ne doit plus rien exécuter
        // et ne jamais lever d'erreur (protection des redéploiements).
        MigrateResult secondPass = newFlyway().migrate();
        assertThat(secondPass.success).isTrue();
        assertThat(secondPass.migrationsExecuted).isZero();
    }

    @Test
    @DisplayName("En bout de chaîne, aucun compte @discipolat.com ne reste loginable (V192)")
    void demoAccountsAreNeutralizedAtEndOfChain() throws Exception {
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT count(*) FROM users "
                            + "WHERE email LIKE '%@discipolat.com' "
                            + "AND statut = 'ACTIVE' AND deleted = false")) {
                rs.next();
                assertThat(rs.getInt(1))
                        .as("fail-closed V192 : zéro compte démo actif en bout de chaîne")
                        .isZero();
            }
        }
    }

    @Test
    @DisplayName("validate() : aucune dérive entre le schéma appliqué et les métadonnées Flyway")
    void schemaIsValidatesAgainstMigrationMetadata() {
        assertThat(newFlyway().validateWithResult().validationSuccessful)
                .as("validate après migrate : checksums et ordre cohérents")
                .isTrue();
    }

    private static Flyway newFlyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
    }
}

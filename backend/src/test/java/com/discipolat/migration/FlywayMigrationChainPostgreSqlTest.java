package com.discipolat.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.discipolat.modules.mentoring.domain.MentorSuggestion;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate de non-régression de la chaîne Flyway sur PostgreSQL réel (racine de la
 * famille de bugs H1-H2, NEED-HELP-02 option c, passe-plat Agent B HANDOVER P3).
 *
 * <p>Contexte : le profil de test applicatif tourne sur H2 avec
 * {@code spring.flyway.enabled:false} et {@code ddl-auto:create-drop} — les
 * migrations ne sont donc <b>jamais</b> exécutées par la suite unitaire, et un
 * script valide seulement sur H2, ou jamais testé, peut casser le déploiement
 * réel. Ce test ferme ce trou : il applique V1..V205 sur un PostgreSQL 16 neuf
 * via Testcontainers, puis exige (1) la chaîne complète sans erreur et son
 * idempotence au second passage, (2) la version cible atteinte, (3) la
 * neutralisation effective des comptes de démonstration en bout de chaîne
 * (V192), (4) l'unicité des dictionnaires réellement portée par tenant
 * (V193 — le 500 de création d'un second tenant, attrapé par le replay de
 * recette §5.5), (5) un {@code validate()} propre — aucune dérive entre le
 * schéma et les métadonnées d'ordre/appliqués, (6) AUCUNE dérive
 * entité→schéma : chaque table {@code @Table} du code doit exister dans le
 * PostgreSQL réellement migré — garde systématique de la famille H (le
 * renommage events→legacy_events de V158, invisible sous H2, est exactement
 * le défaut que cette passe attrape ; V194 le résout), (7) l'arbitrage D1 côté
 * schéma : la table vivante {@code event} accepte le vocabulaire français du
 * contrat §3 et porte les colonnes d'isolation (V203), (8) l'unicité des
 * familles réellement portée par tenant (V204, arbitrage D4 — le doublon de nom
 * entre deux églises, prouvé sur PG réel), (9) les coordonnées géographiques et
 * les preuves de pointage réellement en {@code double precision} — le type que
 * mappe l'entité (V205, dérive NUMERIC→float8 rattrapée par le gate de contrat
 * EventTableContractTest, invisible sous H2 et bloquante en {@code validate}).
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
    private static final int EXPECTED_MIN_VERSION = 205;

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
    @DisplayName("La chaîne Flyway V1→V205 s'applique intégralement sur PostgreSQL 16 neuf")
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
    @DisplayName("V193 : l'unicité des dictionnaires est par tenant, plus mondiale (500 du second tenant)")
    void dictionaryUniquenessIsTenantScoped() throws Exception {
        // Discriminant : avec l'ancien UNIQUE (dict_key, code) de V42, la seconde
        // insertion (autre tenant, mêmes dict_key/code) leverait uq_dict_code —
        // c'est exactement le 500 observé le 2026-09-29 sur le replay PG réel.
        // tenant_id porte une FK vers tenants : on crée deux tenants réels
        // (seules colonnes sans défaut : name, slug), qu'on purge ensuite.
        String tenantA = "00000000-0000-0000-0000-0000000000a1";
        String tenantB = "00000000-0000-0000-0000-0000000000a2";
        String insertDict = "INSERT INTO dictionary_entries (id, tenant_id, dict_key, code, label, ordre, actif, is_default) "
                + "VALUES (uuid_generate_v4(), '%s', 'EVENT_TYPE', 'SORTIE', 'Sortie', 1, TRUE, TRUE)";
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tenants (id, name, slug) VALUES "
                    + "('" + tenantA + "', 'Gate Dict A', 'gate-dict-a'), "
                    + "('" + tenantB + "', 'Gate Dict B', 'gate-dict-b')");
            try {
                // Deux tenants distincts, mêmes (dict_key, code) : doit passer.
                statement.executeUpdate(String.format(insertDict, tenantA));
                statement.executeUpdate(String.format(insertDict, tenantB));
                // Même tenant, même (dict_key, code) : doit être refusé.
                org.junit.jupiter.api.Assertions.assertThrows(
                        java.sql.SQLException.class,
                        () -> statement.executeUpdate(String.format(insertDict, tenantA)),
                        "la collision doit rester bloquante À L'INTÉRIEUR d'un tenant");
            } finally {
                statement.executeUpdate("DELETE FROM dictionary_entries WHERE tenant_id IN ('"
                        + tenantA + "','" + tenantB + "')");
                statement.executeUpdate("DELETE FROM tenants WHERE id IN ('" + tenantA + "','" + tenantB + "')");
            }
        }
    }

    @Test
    @DisplayName("V203 : la table vivante event accepte le vocabulaire FR du contrat et porte l'isolation métier")
    void eventVivanteCarriesFrenchContractAndScoping() throws Exception {
        // Discriminants D1 (schéma) : avant V203, un INSERT avec status='PLANIFIE'
        // ou type='REUNION' était REFUSÉ par les CHECK de V158 (anglais only), et
        // les colonnes famille_id/department_id/resource_scope n'existaient pas —
        // sans elles le port de l'entité Event serait un relâchement d'accès.
        // On vérifie aussi que le verrouillage reste réel : un vocabulaire
        // inventé de part ni d'autre doit être refusé.
        String tenantGate = "00000000-0000-0000-0000-0000000000b1";
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tenants (id, name, slug) VALUES "
                    + "('" + tenantGate + "', 'Gate Event FR', 'gate-event-fr')");
            try {
                String organizer = scalar(statement, "SELECT id FROM users LIMIT 1");
                // Vocabulaire FR du contrat §3 + colonne d'isolation : accepté.
                statement.executeUpdate("INSERT INTO event (id, tenant_id, title, type, status,"
                        + " start_at, organizer_id, famille_id, department_id, visibility) VALUES ("
                        + "uuid_generate_v4(), '" + tenantGate + "', 'Événement gate', 'REUNION',"
                        + " 'PLANIFIE', NOW(), '" + organizer + "', NULL, NULL, 'CHURCH')");
                try (ResultSet rs = statement.executeQuery(
                        "SELECT status, type, resource_scope FROM event WHERE tenant_id = '"
                                + tenantGate + "'")) {
                    rs.next();
                    assertThat(rs.getString(1)).isEqualTo("PLANIFIE");
                    assertThat(rs.getString(2)).isEqualTo("REUNION");
                    assertThat(rs.getString(3))
                            .as("resource_scope défaut posé par V203 (isolation plateforme)")
                            .isEqualTo("TENANT_GLOBAL");
                }
                // Vocabulaire anglais (Church OS) : toujours accepté — union, pas remplacement.
                statement.executeUpdate("INSERT INTO event (id, tenant_id, title, type, status,"
                        + " start_at, organizer_id, visibility) VALUES ("
                        + "uuid_generate_v4(), '" + tenantGate + "', 'EN gate', 'MEETING',"
                        + " 'DRAFT', NOW(), '" + organizer + "', 'CHURCH')");
                // Vocabulaire inventé : refusé (les CHECK protègent encore).
                org.junit.jupiter.api.Assertions.assertThrows(
                        java.sql.SQLException.class,
                        () -> statement.executeUpdate("INSERT INTO event (id, tenant_id, title,"
                                + " type, status, start_at, organizer_id, visibility) VALUES ("
                                + "uuid_generate_v4(), '" + tenantGate + "', ' hors contrat',"
                                + " 'KARAOKE', 'PLANIFIE', NOW(), '" + organizer + "', 'CHURCH')"),
                        "un type hors des deux vocabulaires doit rester bloquant");
            } finally {
                statement.executeUpdate("DELETE FROM event WHERE tenant_id = '" + tenantGate + "'");
                statement.executeUpdate("DELETE FROM tenants WHERE id = '" + tenantGate + "'");
            }
        }
    }

    @Test
    @DisplayName("V205 : lat/long et preuves de pointage reellement en double precision")
    void geolocationColumnsAreDoublePrecision() throws Exception {
        // Discriminant : V201/V202 deposaient ces colonnes en NUMERIC alors que
        // Event/EventRegistration les mappent Double — `ddl-auto: validate` refuse
        // de demarrer sur PG reel, invisible sous H2 (create-drop). V205 convertit ;
        // ce gate le verrouille pour que la derive ne revienne pas.
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT table_name, column_name, data_type FROM information_schema.columns "
                            + "WHERE table_schema='public' AND ("
                            + "(table_name='event' AND column_name IN ('latitude','longitude')) OR "
                            + "(table_name='event_registrations' AND column_name IN "
                            + "('checkin_latitude','checkin_longitude','checkin_accuracy_m','checkin_distance_m')))")) {
                int checked = 0;
                while (rs.next()) {
                    checked++;
                    assertThat(rs.getString(3))
                            .as("%s.%s doit etre double precision (type mappe par l'entite)",
                                    rs.getString(1), rs.getString(2))
                            .isEqualTo("double precision");
                }
                assertThat(checked)
                        .as("les 6 colonnes geolocalisees doivent exister")
                        .isEqualTo(6);
            }
        }
    }

    @Test
    @DisplayName("V204 : l'unicité des familles est par tenant, plus mondiale (arbitrage D4)")
    void familyUniquenessIsTenantScoped() throws Exception {
        // Discriminant : avec l'UNIQUE (nom) mondial de V6, le second INSERT
        // (autre tenant, même nom) levait uk_families_nom — comportement prouvé
        // en erreur sur PG réel le 2026-09-29 (NEED-HELP D4). Mêmes fixtures
        // réelles que le test V193 : un users seedé par la chaîne (NOT NULL + FK
        // chef_famille_id de families ; departement_id a été retiré par V27).
        String tenantA = "00000000-0000-0000-0000-0000000000c1";
        String tenantB = "00000000-0000-0000-0000-0000000000c2";
        String insertFamily = "INSERT INTO families (id, tenant_id, nom,"
                + " chef_famille_id) VALUES (uuid_generate_v4(), '" + "%s" + "', "
                + "'DupGate Famille', '%s')";
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tenants (id, name, slug) VALUES "
                    + "('" + tenantA + "', 'Gate Fam A', 'gate-fam-a'), "
                    + "('" + tenantB + "', 'Gate Fam B', 'gate-fam-b')");
            String chef = scalar(statement, "SELECT id FROM users LIMIT 1");
            String sqlA = String.format(insertFamily, tenantA, chef);
            String sqlB = String.format(insertFamily, tenantB, chef);
            try {
                // Deux tenants distincts, même nom : doit passer (défaut D4 corrigé).
                statement.executeUpdate(sqlA);
                statement.executeUpdate(sqlB);
                // Même tenant, même nom : doit rester refusé.
                org.junit.jupiter.api.Assertions.assertThrows(
                        java.sql.SQLException.class,
                        () -> statement.executeUpdate(sqlA),
                        "la collision doit rester bloquante À L'INTÉRIEUR d'un tenant");
            } finally {
                statement.executeUpdate("DELETE FROM families WHERE tenant_id IN ('"
                        + tenantA + "','" + tenantB + "')");
                statement.executeUpdate("DELETE FROM tenants WHERE id IN ('"
                        + tenantA + "','" + tenantB + "')");
            }
        }
    }

    private static String scalar(Statement statement, String query) throws java.sql.SQLException {
        try (ResultSet rs = statement.executeQuery(query)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    @DisplayName("validate() : aucune dérive entre le schéma appliqué et les métadonnées Flyway")
    void schemaIsValidatesAgainstMigrationMetadata() {
        assertThat(newFlyway().validateWithResult().validationSuccessful)
                .as("validate après migrate : checksums et ordre cohérents")
                .isTrue();
    }

    @Test
    @DisplayName("Aucune dérive entité→schéma : chaque table @Table existe dans le PostgreSQL réellement migré")
    void everyEntityTableExistsInMigratedSchema() throws Exception {
        // Garde systématique de la famille H. Le profil H2 des tests unitaires
        // (ddl-auto create-drop, Flyway désactivé) régénère les tables depuis
        // les entités : une migration qui renomme/supprime une table réelle
        // sans publier le mappage (V158 : events→legacy_events) y est
        // INVISIBLE et explose seulement en production. Ici, la chaîne complète
        // est appliquée sur PG 16, puis chaque @Table du code doit exister.
        Set<String> schemaTables = new HashSet<>();
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
            while (rs.next()) {
                schemaTables.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
        List<String> missing = new ArrayList<>();
        int scanned = 0;
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.discipolat")) {
            Class<?> entityClass = Class.forName(candidate.getBeanClassName());
            Table table = entityClass.getAnnotation(Table.class);
            assertThat(table)
                    .as("chaque entité doit déclarer @Table explicite (contrat du scan : %s)",
                            candidate.getBeanClassName())
                    .isNotNull();
            scanned++;
            if (!schemaTables.contains(table.name().toLowerCase(Locale.ROOT))) {
                missing.add(entityClass.getSimpleName() + " → " + table.name());
            }
        }
        assertThat(scanned)
                .as("le scan doit trouver la totalité des entités JPA (garde-fou anti-régression du scan lui-même)")
                .isGreaterThan(250);
        assertThat(missing)
                .as("tables d'entités absentes du schéma réellement migré (dérive de famille H)")
                .isEmpty();
    }

    @Test
    @DisplayName("Mot réserve PG « analyse » : mapping cité et verrouillé (mentoring, mine de prod Render)")
    void analyseReservedWordIsQuotedInMappingAndSchema() throws Exception {
        // Racine PRODUCTION réelle : ANALYSE est un mot RÉSERVÉ PostgreSQL. Sans
        // citation dans le mapping, Hibernate émet `select … analyse …` et tout
        // `GET /mentoring` / `POST /mentoring/generate` tombe en « syntax error at
        // or near "analyse" » sur la base réelle. H2 (mode PostgreSQL, profil de
        // test create-drop) MASQUE ce défaut — d'où l'obligation de le vérifier
        // sur le conteneur PG de ce gate, pas sous H2.

        // (1) Garde anti-dérive du mapping : la colonne doit rester explicitement
        // citée. Le retirer en silence réintroduirait le bug de production.
        Column colonne = MentorSuggestion.class
                .getDeclaredField("analyse")
                .getAnnotation(Column.class);
        assertThat(colonne)
                .as("le champ analyse doit porter un @Column explicite")
                .isNotNull();
        assertThat(colonne.name())
                .as("le mapping doit citer la colonne -> \"analyse\"")
                .isEqualTo("\"analyse\"");

        // (2) Preuve sur PostgreSQL réel, schéma migré : le mot nu est refusé par
        // le parseur (réservé), la forme citée passe — exactement l'écart que le
        // mapping corrige.
        try (Connection connection = java.sql.DriverManager.getConnection(
                     POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    java.sql.SQLException.class,
                    () -> statement.executeQuery("SELECT analyse FROM mentor_suggestions").close(),
                    "« analyse » nu doit rester refusé par PostgreSQL — sinon la citation du mapping n'a plus de raison d'être");
            try (ResultSet rs = statement.executeQuery("SELECT \"analyse\" FROM mentor_suggestions")) {
                rs.next(); // la forme citée doit s'exécuter sans erreur de syntaxe
            }
        }
    }

    private static Flyway newFlyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
    }
}

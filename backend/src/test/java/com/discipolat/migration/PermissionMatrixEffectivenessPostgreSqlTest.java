package com.discipolat.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PARAMÉTRISATION PAR ÉGLISE — le garde fail-closed du serveur, vérifié sur la
 * <b>vérité terrain</b>.
 * <p>
 * {@link com.discipolat.common.test.PermissionKeyCatalogGuardTest} vérifie en
 * statique que la clé d'un garde existe dans le catalogue. Ce test va plus loin
 * et pose LA question qui décide si un endpoint est utilisable : <b>après avoir
 * appliqué la chaîne Flyway complète, cette clé est-elle détenue par au moins un
 * rôle global ?</b>
 * <p>
 * Réponse non ⇒ {@code @authz.can} ne se résout pour personne : l'endpoint
 * renvoie 403 à tous, y compris l'administrateur de l'église. C'est un mode de
 * panne <b>invisible</b> : aucun écran ne le consomme directement, les tests
 * H2 ne le voient pas (le seed peut contenir la permission sans la paire), et la
 * seule trace est un 403 en production.
 * <p>
 * Pourquoi une base réelle plutôt qu'un parseur de SQL ? Les grants « blanket »
 * de V135/V136 sont exprimés en {@code JOIN permissions p ON p.scope = 'TENANT'}
 * et {@code p.key NOT IN (…)}. Évaluer ça par regex serait fragile : au premier
 * grant de forme nouvelle, un parseur trop optimiste déclare des clés
 * « couvertes » qui ne le sont pas — exactement le défaut inverse de celui que
 * ce garde combat. La base répond, elle, sans interprétation.
 * <p>
 * {@code @perm.has('DOMAIN','ACTION')} est hors périmètre : le bean est
 * permissif en l'absence de ligne (voir {@code PermissionGuard}).
 * <p>
 * Nécessite Docker : sans lui, la classe est <b>SKIP</b> (jamais un faux PASS),
 * comme {@link FlywayMigrationChainPostgreSqlTest}.
 */
@Testcontainers
class PermissionMatrixEffectivenessPostgreSqlTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("discipolat_matrix_gate");

    @BeforeAll
    static void migrateChain() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static List<String> guardKeys() throws IOException {
        Pattern guard = Pattern.compile("@authz\\.can\\(\\s*'([A-Z][A-Z0-9_]*)'");
        List<String> keys = new ArrayList<>();
        Path root = Path.of("src", "main", "java");
        assertThat(Files.isDirectory(root))
                .as("cwd inattendu : le test suppose d'être exécuté depuis `backend/`")
                .isTrue();
        try (Stream<Path> files = Files.walk(root.toAbsolutePath())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = guard.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    keys.add(m.group(1));
                }
            }
        }
        return keys;
    }

    private static Set<String> query(String sql) throws SQLException {
        Set<String> out = new LinkedHashSet<>();
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    @Test
    @DisplayName("chaque clé @authz.can existe dans la table permissions après migration")
    void everyGuardKeyIsSeeded() throws Exception {
        Set<String> guards = new LinkedHashSet<>(guardKeys());
        assertThat(guards).as("scan des gardes vide").isNotEmpty();

        Set<String> present = query("SELECT key FROM permissions WHERE key = ANY (ARRAY["
                + guards.stream().map(k -> "'" + k + "'").reduce((a, b) -> a + "," + b).orElse("")
                + "]::text[])");

        Set<String> missing = new LinkedHashSet<>(guards);
        missing.removeAll(present);
        assertThat(missing)
                .as("clés de garde absentes de la table `permissions` après migration : "
                        + "l'endpoint est inaccessible à tous les rôles (403 silencieux)")
                .isEmpty();
    }

    @Test
    @DisplayName("chaque clé @authz.can est détenue par au moins un rôle global (endpoint utilisable)")
    void everyGuardKeyIsHeldByAtLeastOneGlobalRole() throws Exception {
        Set<String> guards = new LinkedHashSet<>(guardKeys());
        Set<String> orphan = query("SELECT DISTINCT p.key FROM permissions p "
                + "WHERE p.key = ANY (ARRAY["
                + guards.stream().map(k -> "'" + k + "'").reduce((a, b) -> a + "," + b).orElse("")
                + "]::text[]) "
                + "AND NOT EXISTS (SELECT 1 FROM role_permissions rp "
                + "JOIN roles r ON r.id = rp.role_id "
                + "WHERE rp.permission_id = p.id AND r.tenant_id IS NULL)");

        assertThat(orphan)
                .as("clés de garde qu'aucun rôle global ne détient : le pont d'équivalence ne "
                        + "peut rien résoudre, l'endpoint renvoie 403 à TOUT le monde — y "
                        + "compris le pasteur. Ajouter la paire (rôle, clé) en migration additive, "
                        + "ou utiliser une clé déjà couverte.")
                .isEmpty();
    }
}
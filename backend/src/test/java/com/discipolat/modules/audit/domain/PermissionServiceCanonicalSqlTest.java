package com.discipolat.modules.audit.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PREUVE D'EXÉCUTION de la surface héritée remise en marche (Phase 9).
 *
 * <p><strong>Pourquoi ce fichier, alors que {@code PermissionServiceTest} existe
 * déjà.</strong> Ce dernier joue le SQL contre un mock de {@code JdbcTemplate} : il
 * verrouille la <em>forme</em> des requêtes, pas leur exécution. Or la surface est
 * morte précisément parce qu'un mock acceptait des colonnes inventées —
 * {@code role}, {@code enabled}, {@code can_read} sur {@code role_permissions} —
 * là où la migration {@code V135} avait recréé cette table en
 * {@code (role_id, permission_id)}. Le serveur répondait SQLSTATE 42703 depuis des
 * mois, et la suite unitaire restait verte. Ici, les requêtes sont réellement
 * envoyées à un moteur SQL.
 *
 * <p><strong>Le schéma de cette base est le schéma canonique, et rien d'autre.</strong>
 * Seules {@code roles}, {@code permissions}, {@code role_permissions},
 * {@code tenant_memberships}, {@code member_role_assignments} et {@code role_assignment}
 * sont créées, avec les colonnes que les migrations {@code V135}/{@code V136}/{@code V154}/
 * {@code V226} posent réellement. {@code platform_roles} et {@code permission_catalog}
 * sont <strong>volontairement absentes</strong> : c'est le CONTRÔLE NÉGATIF — si une
 * requête revenait y chercher des droits, la base le refuserait (« table not found »)
 * au lieu de laisser passer une réponse qui ne correspond à aucun garde appliqué.
 * Le premier test le constate explicitement.
 *
 * <p><strong>Le multi-tenant est testé, pas seulement écrit.</strong> Une bonne partie
 * des cas ci-dessous vérifie que la ligne qui décide est bien <em>la surcouche de
 * l'église courante quand elle existe</em> — en lecture comme en écriture. Un
 * {@code WHERE key = ?} sans cette précaution écrirait sur le modèle partagé par
 * toutes les églises depuis la session d'une seule.
 */
@DisplayName("Surface héritée /api/v1/permissions — exécution réelle sur le schéma canonique")
class PermissionServiceCanonicalSqlTest {

    private static final String URL = "jdbc:h2:mem:perm_canonical_sql;DB_CLOSE_DELAY=-1"
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
            + ";NON_KEYWORDS=key,value,year,month,day";

    private static JdbcTemplate jdbc;
    private PermissionService service;

    // Deux églises, pour que l'isolation ne soit pas déclarative mais observée.
    private static final UUID EGLISE_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID EGLISE_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private UUID diacreGlobal;
    private UUID diacreB;
    private UUID membreGlobal;
    private UUID lire;
    private UUID modifier;
    private UUID supprimer;
    private UUID hardDelete;

    /* ==================== montage du décor ==================== */

    @BeforeAll
    static void creerLeSchemaCanonique() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(URL);
        jdbc = new JdbcTemplate(dataSource);

        // Colonnes et contraintes reproduites depuis V135 (roles, permissions,
        // role_permissions), V136 (tenant_memberships.role_id), V154 (role_assignment)
        // et V226 (member_role_assignments). Rien de plus : c'est ce qui rend la preuve
        // utile.
        jdbc.execute("CREATE TABLE roles ("
                + " id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,"
                + " tenant_id UUID NULL,"
                + " key VARCHAR(50) NOT NULL,"
                + " label VARCHAR(100) NOT NULL,"
                + " description VARCHAR(2000),"
                + " system BOOLEAN NOT NULL DEFAULT FALSE,"
                + " priority INTEGER NOT NULL DEFAULT 0,"
                + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " CONSTRAINT uk_role_tenant_key UNIQUE (tenant_id, key))");

        jdbc.execute("CREATE TABLE permissions ("
                + " id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,"
                + " tenant_id UUID NULL,"
                + " key VARCHAR(100) NOT NULL UNIQUE,"
                + " label VARCHAR(150) NOT NULL,"
                + " description VARCHAR(2000),"
                + " scope VARCHAR(20) NOT NULL CHECK (scope IN"
                + "   ('GLOBAL','TENANT','CHURCH','SUB_CHURCH','DEPARTMENT','FAMILY','OWN')),"
                + " category VARCHAR(50),"
                + " system BOOLEAN NOT NULL DEFAULT FALSE,"
                + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + " updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        // La forme POST-V135 : deux colonnes, pas de bit enabled, pas de R/W/D, pas de
        // scope. Toute requête qui croyait encore à l'ancienne forme échoue ici.
        jdbc.execute("CREATE TABLE role_permissions ("
                + " role_id UUID NOT NULL,"
                + " permission_id UUID NOT NULL,"
                + " PRIMARY KEY (role_id, permission_id))");

        jdbc.execute("CREATE TABLE tenant_memberships ("
                + " id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,"
                + " tenant_id UUID NOT NULL,"
                + " user_id UUID NOT NULL,"
                + " role_id UUID NOT NULL)");
        jdbc.execute("CREATE TABLE member_role_assignments ("
                + " id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,"
                + " tenant_id UUID NOT NULL,"
                + " user_id UUID NOT NULL,"
                + " role_id UUID NOT NULL)");
        jdbc.execute("CREATE TABLE role_assignment ("
                + " id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,"
                + " tenant_id UUID NOT NULL,"
                + " person_id UUID NOT NULL,"
                + " role_id UUID NOT NULL)");
    }

    @BeforeEach
    void viderEtPlanterLesGraines() {
        jdbc.update("DELETE FROM role_permissions");
        jdbc.update("DELETE FROM tenant_memberships");
        jdbc.update("DELETE FROM member_role_assignments");
        jdbc.update("DELETE FROM role_assignment");
        jdbc.update("DELETE FROM roles");
        jdbc.update("DELETE FROM permissions");
        TenantContext.clear();

        lire = insertPermission("FAMILY_READ", "FAMILY", "FAMILLES");
        modifier = insertPermission("FAMILY_UPDATE", "FAMILY", "FAMILLES");
        supprimer = insertPermission("FAMILY_DELETE", "FAMILY", "FAMILLES");
        hardDelete = insertPermission("USERS_EXECUTE_HARDDELETE", "TENANT", "USERS");

        membreGlobal = insertRole("MEMBRE", null, true, "Membre");
        diacreGlobal = insertRole("DIACRE", null, false, "Diacre");
        diacreB = insertRole("DIACRE", EGLISE_B, false, "Diacre (église B)");

        // Le modèle global de DIACRE : lecture + écriture, jamais suppression.
        grant(diacreGlobal, lire);
        grant(diacreGlobal, modifier);
        grant(membreGlobal, lire);

        service = new PermissionService(jdbc);
    }

    @AfterEach
    void rendLeContexteLibre() {
        TenantContext.clear();
    }

    private UUID insertRole(String key, UUID tenantId, boolean system, String label) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO roles (id, tenant_id, key, label, description, system)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                id, tenantId, key, label, "seed", system);
        return id;
    }

    private UUID insertPermission(String key, String scope, String category) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system)"
                        + " VALUES (?, NULL, ?, ?, ?, ?, ?, TRUE)",
                id, key, "Libellé " + key, "Description " + key, scope, category);
        return id;
    }

    private void grant(UUID roleId, UUID permissionId) {
        jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                roleId, permissionId);
    }

    private int droitsDe(UUID roleId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", Integer.class, roleId);
        return n == null ? 0 : n;
    }

    private Map<String, Object> ligneDe(UUID roleId) {
        return jdbc.queryForMap("SELECT tenant_id, label FROM roles WHERE id = ?", roleId);
    }

    private boolean tableExiste(String table) {
        try {
            jdbc.queryForList("SELECT 1 FROM " + table + " LIMIT 1");
            return true;
        } catch (RuntimeException echec) {
            return false;
        }
    }

    private static Map<String, Object> trouve(List<Map<String, Object>> lignes, String permission) {
        return lignes.stream()
                .filter(l -> permission.equals(l.get("permission")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ligne absente de la réponse : " + permission
                        + " — reçu " + lignes));
    }

    /* ==================== 0. la preuve porte sur la vraie forme ==================== */

    @Test
    @DisplayName("Le schéma de preuve ignore platform_roles et permission_catalog (contrôle négatif)")
    void lesTablesMortesNExistentPasDansCettePreuve() {
        assertThat(tableExiste("role_permissions")).isTrue();
        // CONTRÔLE NÉGATIF : la surface héritée lisait ces deux tables pour quatre de
        // ses six URL. Si le SQL y retournait, ce n'est pas une assertion qui le dirait,
        // c'est la base : « table not found ».
        assertThat(tableExiste("platform_roles"))
                .as("aucun droit n'est lu ici : le serveur n'y applique rien")
                .isFalse();
        assertThat(tableExiste("permission_catalog")).isFalse();
        // Et la forme pré-V135 n'existe pas davantage : pas de colonne « enabled ».
        assertThatThrownBy(() -> jdbc.queryForList(
                "SELECT role, permission, enabled, can_read, can_write, can_delete, scope FROM role_permissions"))
                .as("c'est exactement la requête qui produisait le SQLSTATE 42703 en production")
                .isInstanceOf(RuntimeException.class);
    }

    /* ==================== 1. lecture de la matrice ==================== */

    @Test
    @DisplayName("GET /permissions répond, et la ligne porte le nom de la clé, pas un identifiant")
    void laMatriceSeLitReelement() {
        List<Map<String, Object>> lignes = service.getAllPermissions();

        assertThat(lignes).extracting(l -> (String) l.get("role"))
                .containsExactlyInAnyOrder("DIACRE", "DIACRE", "MEMBRE");
        assertThat(trouve(lignes, "FAMILY_READ").get("enabled")).isEqualTo(true);
        assertThat(trouve(lignes, "FAMILY_READ").get("scope"))
                .as("la portée vient de permissions.scope, la seule que les gardes appliquent")
                .isEqualTo("FAMILY");
    }

    @Test
    @DisplayName("Le triplet R/W/D est projeté du nom de la clé et rendu aussi en camelCase")
    void leTripletRWDEstProjete() {
        List<Map<String, Object>> lignes = service.getPermissionsByRole("DIACRE");

        Map<String, Object> lecture = trouve(lignes, "FAMILY_READ");
        assertThat(lecture.get("can_read")).isEqualTo(true);
        assertThat(lecture.get("can_write")).isEqualTo(false);
        assertThat(lecture.get("can_delete")).isEqualTo(false);
        // Le défaut d'affichage : PermissionsPage lisait canRead/canWrite/canDelete,
        // l'ancien SQL ne renvoyait que des colonnes snake_case — la grille granulaire
        // n'a donc jamais rien montré, même quand le serveur répondait.
        assertThat(lecture.get("canRead")).isEqualTo(true);
        assertThat(lecture.get("canWrite")).isEqualTo(false);

        Map<String, Object> ecriture = trouve(lignes, "FAMILY_UPDATE");
        assertThat(ecriture.get("canWrite")).isEqualTo(true);
        assertThat(ecriture.get("canRead")).isEqualTo(false);

        assertThat(service.getPermissionsByRole("DIACRE"))
                .as("FAMILY_DELETE n'est pas accordé au modèle global : il n'apparaît pas")
                .noneMatch(l -> "FAMILY_DELETE".equals(l.get("permission")));
    }

    @Test
    @DisplayName("Les niveaux READ/WRITE/DELETE/MANAGE et la portée se lisent sur les clés réelles")
    void lesNiveauxEtLaPortee() {
        assertThat(service.hasPermissionLevel("DIACRE", "FAMILY_READ", "READ")).isTrue();
        assertThat(service.hasPermissionLevel("DIACRE", "FAMILY_READ", "WRITE")).isFalse();
        assertThat(service.hasPermissionLevel("DIACRE", "FAMILY_UPDATE", "WRITE")).isTrue();
        assertThat(service.hasPermissionLevel("DIACRE", "FAMILY_UPDATE", "MANAGE")).isTrue();
        assertThat(service.hasPermissionLevel("DIACRE", "FAMILY_DELETE", "DELETE"))
                .as("clé non accordée : restrictif, comme avant")
                .isFalse();

        assertThat(service.getPermissionScope("DIACRE", "FAMILY_READ")).isEqualTo("FAMILY");
        assertThat(service.getPermissionScope("DIACRE", "FAMILY_DELETE")).isEqualTo("NONE");

        // Sur-utilisateurs : la matrice ne les restreint pas (inchangé).
        assertThat(service.hasPermission("ADMIN", "FAMILY_DELETE")).isTrue();
        assertThat(service.userHasPermission(List.of("QUELQUUN"), "FAMILY_DELETE")).isFalse();
    }

    /* ==================== 2. écriture de la matrice ==================== */

    @Test
    @DisplayName("Accorder puis révoquer écrit réellement une ligne, et la retire")
    void ecrireAccordeEtRevoque() {
        TenantContext.setTenantId(EGLISE_A);

        Map<String, Object> rendu = service.updatePermission("DIACRE", "FAMILY_DELETE", true);
        assertThat(rendu.get("enabled")).isEqualTo(true);
        // La ligne est en base : ce n'est plus un UPDATE sans effet qui renvoie un 200.
        assertThat(droitsDe(diacreGlobal)).isEqualTo(3);
        assertThat(service.hasPermission("DIACRE", "FAMILY_DELETE")).isTrue();

        service.updatePermission("DIACRE", "FAMILY_DELETE", false);
        assertThat(droitsDe(diacreGlobal)).isEqualTo(2);
        assertThat(service.hasPermission("DIACRE", "FAMILY_DELETE")).isFalse();
    }

    @Test
    @DisplayName("Écrire est idempotent : recocher la même case ne duplique rien")
    void accorderDeuxFoisNeChangeRien() {
        service.updatePermission("DIACRE", "FAMILY_DELETE", true);
        service.updatePermission("DIACRE", "FAMILY_DELETE", true);

        assertThat(droitsDe(diacreGlobal)).isEqualTo(3);
    }

    @Test
    @DisplayName("Clé de permission inconnue = refus, et rien n'est écrit (défaut D7)")
    void cleInconnueEstRefusee() {
        // CONTRÔLE NÉGATIF : l'ancien code faisait un UPDATE sur une ligne inexistante,
        // ne touchait à rien et répondait 200. L'opérateur croyait sa case cochée.
        int avant = droitsDe(diacreGlobal);

        assertThatThrownBy(() -> service.updatePermission("DIACRE", "FAMILY_EXPLODE", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAMILY_EXPLODE");
        assertThatThrownBy(() -> service.updatePermission("ROLE_SAN_PAPIER", "FAMILY_READ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROLE_SAN_PAPIER");

        assertThat(droitsDe(diacreGlobal)).isEqualTo(avant);
    }

    @Test
    @DisplayName("Le bit qui gouverne une écriture granulaire est celui que la clé représente")
    void leBitQuiGouverneEstCielDeLaCle() {
        // décocher « Suppression » sur une clé de lecture ne doit pas révoquer la lecture
        service.updatePermissionRWD("DIACRE", "FAMILY_READ", false, true, false, "TENANT");
        assertThat(service.hasPermission("DIACRE", "FAMILY_READ"))
                .as("la case lecture est décochée → la clé se retire, et elle seule")
                .isFalse();

        service.updatePermissionRWD("DIACRE", "FAMILY_UPDATE", true, false, false, "TENANT");
        assertThat(service.hasPermission("DIACRE", "FAMILY_UPDATE"))
                .as("la case écriture est décochée → la clé se retire")
                .isFalse();

        Map<String, Object> rendu =
                service.updatePermissionRWD("DIACRE", "FAMILY_DELETE", true, true, true, "TENANT");
        assertThat(service.hasPermission("DIACRE", "FAMILY_DELETE")).isTrue();
        assertThat(rendu.get("scope"))
                .as("la portée demandée (« TENANT ») n'est pas appliquée : celle rendue est la "
                        + "valeur réelle du serveur, pour que l'appelant ne croie pas avoir obtenu "
                        + "autre chose")
                .isEqualTo("FAMILY");
    }

    /* ==================== 3. portée par église ==================== */

    @Test
    @DisplayName("La surcouche de l'église fait écran au modèle global, en lecture")
    void laSurcoucheFaitEcran() {
        TenantContext.setTenantId(EGLISE_B);

        assertThat(service.getPermissionsByRole("DIACRE"))
                .as("l'église B a son propre DIACRE, sans droits : le DIACRE global ne resurgit pas")
                .isEmpty();
        assertThat(service.hasPermission("DIACRE", "FAMILY_READ")).isFalse();

        TenantContext.clear();
        assertThat(service.getPermissionsByRole("DIACRE"))
                .as("hors contexte, c'est le modèle global qui décide")
                .hasSize(2);
    }

    @Test
    @DisplayName("Écrire depuis une église n'écrit pas sur le modèle partagé")
    void ecrireNeTouchePasLeModeleGlobal() {
        TenantContext.setTenantId(EGLISE_B);

        service.updatePermission("DIACRE", "FAMILY_DELETE", true);

        assertThat(droitsDe(diacreB)).as("la surcouche porte la révocation").isEqualTo(1);
        assertThat(droitsDe(diacreGlobal))
                .as("toutes les autres églises garderont-elles la suppression des familles ? "
                        + "Ce n'était pas la demande")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("La liste des rôles ne donne qu'une ligne par clé et jamais un rôle d'une autre église")
    void laListeDesRolesResteLocale() {
        TenantContext.setTenantId(EGLISE_B);
        List<String> cles = service.listRoles().stream()
                .map(r -> (String) r.get("key"))
                .toList();

        assertThat(cles.stream().filter("DIACRE"::equals).count()).isEqualTo(1);
        assertThat(cles).contains("MEMBRE");
        assertThat(service.listRoles().stream()
                .filter(r -> "DIACRE".equals(r.get("key")))
                .findFirst().orElseThrow().get("tenantId"))
                .as("c'est la ligne de l'église courante qui est présentée")
                .isEqualTo(EGLISE_B);

        // Un rôle privé d'une autre église reste invisible.
        TenantContext.setTenantId(EGLISE_A);
        jdbc.update("INSERT INTO roles (id, tenant_id, key, label, description, system)"
                        + " VALUES (?, ?, 'CAISSE', 'Caissier', ?, FALSE)",
                UUID.randomUUID(), EGLISE_A, "seed");
        assertThat(service.listRoles()).extracting(r -> (String) r.get("key")).contains("CAISSE");

        TenantContext.setTenantId(EGLISE_B);
        assertThat(service.listRoles()).extracting(r -> (String) r.get("key"))
                .as("le rôle « CAISSE » de l'église A ne doit pas apparaître dans la liste de l'église B")
                .doesNotContain("CAISSE");
    }

    @Test
    @DisplayName("updateRole écrit sur la ligne qui décide, pas sur toutes celles du même nom")
    void modifierUnRoleNecritQuuneLigne() {
        TenantContext.setTenantId(EGLISE_B);

        service.updateRole("DIACRE", "Diacre de B", "Portée : famille uniquement");

        assertThat(ligneDe(diacreB).get("label")).isEqualTo("Diacre de B");
        assertThat(ligneDe(diacreGlobal).get("label"))
                .as("le modèle global garde son libellé")
                .isEqualTo("Diacre");
    }

    @Test
    @DisplayName("getRole sur une clé hors de portée = introuvable, sans fuite inter-église")
    void unRoleDuneAutreEgliseEstIntrouvable() {
        jdbc.update("INSERT INTO roles (id, tenant_id, key, label, description, system)"
                        + " VALUES (?, ?, 'CAISSE', 'Caissier', ?, FALSE)",
                UUID.randomUUID(), EGLISE_A, "seed");

        TenantContext.setTenantId(EGLISE_B);
        assertThatThrownBy(() -> service.getRole("CAISSE")).isInstanceOf(NoSuchElementException.class);
    }

    /* ==================== 4. cycle de vie des rôles ==================== */

    @Test
    @DisplayName("createRole + duplicateRole : la copie est la surcouche locale de l'église")
    void dupliquerDepuisUneEgliseCreeUneSurcouche() {
        TenantContext.setTenantId(EGLISE_A);

        service.createRole("VISITEUR", "Visiteur de passage", "Accueil");
        assertThat(jdbc.queryForList("SELECT id FROM roles WHERE key = 'VISITEUR' AND tenant_id = ?",
                EGLISE_A)).hasSize(1);

        service.duplicateRole("MEMBRE", "COPIE_MEMBRE", "Copie du modèle membre");
        UUID copie = jdbc.queryForObject(
                "SELECT id FROM roles WHERE key = 'COPIE_MEMBRE'", UUID.class);

        assertThat(ligneDe(copie).get("tenant_id"))
                .as("dupliquer le modèle global depuis une église ne doit pas créer un second "
                        + "modèle global, sinon la paramétrisation par église est impossible")
                .isEqualTo(EGLISE_A);
        assertThat(droitsDe(copie)).isEqualTo(droitsDe(membreGlobal));
        assertThat(ligneDe(copie).get("label")).isEqualTo("Copie du modèle membre");

        // Un libellé vide retombe sur la clé : roles.label est NOT NULL.
        service.duplicateRole("MEMBRE", "SANS_LIBELLE", null);
        UUID sansLibelle = jdbc.queryForObject("SELECT id FROM roles WHERE key = 'SANS_LIBELLE'", UUID.class);
        assertThat(ligneDe(sansLibelle).get("label")).isEqualTo("SANS_LIBELLE");
    }

    @Test
    @DisplayName("Créer ou dupliquer un nom déjà pris est refusé, sans rien écrire")
    void lesNomsEnDoubleSontRefuses() {
        assertThatThrownBy(() -> service.createRole("DIACRE", "Diacre bis", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existe déjà");
        assertThatThrownBy(() -> service.duplicateRole("DIACRE", "MEMBRE", "x"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.duplicateRole("ROLE_SAN_PAPIER", "NOUVEAU", "x"))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(jdbc.queryForList("SELECT id FROM roles WHERE key = 'NOUVEAU'")).isEmpty();
    }

    @Test
    @DisplayName("deleteRole refuse un rôle système et un rôle encore porté, puis réussit")
    void supprimerUnRoleNeCascadeAucuneAffectation() {
        UUID membre = UUID.fromString("cccccccc-0000-0000-0000-000000000009");
        jdbc.update("INSERT INTO tenant_memberships (id, tenant_id, user_id, role_id)"
                + " VALUES (?, ?, ?, ?)", UUID.randomUUID(), EGLISE_B, membre, diacreB);

        TenantContext.setTenantId(EGLISE_B);

        assertThatThrownBy(() -> service.deleteRole("MEMBRE"))
                .as("un rôle système ne se supprime pas")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rôle système");

        assertThatThrownBy(() -> service.deleteRole("DIACRE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("affectation");
        assertThat(jdbc.queryForList("SELECT id FROM roles WHERE id = ?", diacreB)).hasSize(1);

        jdbc.update("DELETE FROM tenant_memberships WHERE user_id = ?", membre);
        service.deleteRole("DIACRE");

        assertThat(jdbc.queryForList("SELECT id FROM roles WHERE id = ?", diacreB)).isEmpty();
        assertThat(jdbc.queryForList("SELECT id FROM roles WHERE id = ?", diacreGlobal))
                .as("la suppression d'une surcouche laisse le modèle global intact")
                .hasSize(1);
        assertThat(droitsDe(diacreGlobal)).isEqualTo(2);
    }

    /* ==================== 5. catalogue ==================== */

    @Test
    @DisplayName("Le catalogue sert les clés que le serveur applique réellement")
    void leCatalogueVientDeLaTableDesDroits() {
        List<Map<String, Object>> catalogue = service.listPermissionCatalog();

        assertThat(catalogue).hasSize(4);
        Map<String, Object> entree = catalogue.stream()
                .filter(c -> "FAMILY_READ".equals(c.get("key")))
                .findFirst().orElseThrow();
        assertThat(entree.get("module")).isEqualTo("FAMILLES");
        assertThat(entree.get("defaultScope")).isEqualTo("FAMILY");
        assertThat((String) entree.get("scopeDescription")).contains("famille");
        assertThat(entree.get("ordre")).isNotNull();
    }
}

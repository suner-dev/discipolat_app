package com.discipolat.modules.audit.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Contrats de la surface héritée, réécrite sur le modèle canonique.
 *
 * <p>Ce que cette classe vérifie : la <em>forme</em> des requêtes (jointure
 * {@code role_permissions → roles → permissions}, jamais {@code platform_roles} ni
 * {@code permission_catalog}), le <em>nom</em> des lignes rendues à l'appelant et la
 * sémantique des refus. Ce qu'elle ne peut pas vérifier : que le SQL est
 * <em>exéculable</em> — un mock accepte une colonne inventée, et c'est exactement
 * ainsi que la surface a vécu pendant des mois en SQLSTATE 42703. La preuve
 * d'exécution est rendue par {@code PermissionServiceCanonicalSqlTest}, qui joue ces
 * requêtes contre un vrai H2 portant le schéma canonique.
 *
 * <p>Choix d'écriture des bouchons : le SQL est <em>routé par fragment</em>
 * ({@link #sonde}) et les appels sont <em>relevés</em> ({@link #derniereLecture()},
 * {@link #derniereEcriture()}) plutôt que captés par {@code ArgumentCaptor} sur des
 * varargs. Deux raisons : la signature {@code queryForList(String, Object…)} rend la
 * capture de varargs ambiguë, et un test qui dépend de l'<em>ordre</em> des sondes
 * casse à chaque réécriture interne sans que le contrat visible change. Ce qui est
 * verrouillé ici est ce que l'appelant voit, pas ce que la classe se raconte.
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PermissionService permissionService;

    /** Jointure canonique attendue dans toute requête de la matrice. */
    private static final String JOINTURE =
            "FROM role_permissions rp JOIN roles r ON r.id = rp.role_id JOIN permissions p ON p.id = rp.permission_id";

    private static final UUID ROLE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID PERMISSION_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final UUID EGLISE = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    /** Réponses par fragment de SQL ; défaut : vide, donc « non accordé ». */
    private final Map<String, List<Map<String, Object>>> reponses = new LinkedHashMap<>();
    private final List<Object[]> lectures = new ArrayList<>();
    private final List<Object[]> ecritures = new ArrayList<>();
    private final List<Object[]> comptages = new ArrayList<>();
    private int porteursACompter = 0;

    @BeforeEach
    void brancheLeSondage() {
        lenient().when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    lectures.add(invocation.getArguments());
                    for (Map.Entry<String, List<Map<String, Object>>> entree : reponses.entrySet()) {
                        if (sql.contains(entree.getKey())) {
                            return entree.getValue();
                        }
                    }
                    return List.of();
                });
        lenient().when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    ecritures.add(invocation.getArguments());
                    return 1;
                });
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    comptages.add(invocation.getArguments());
                    return porteursACompter;
                });
        permissionService = new PermissionService(jdbcTemplate);
    }

    @AfterEach
    void rendLeContexteLibre() {
        TenantContext.clear();
    }

    /** Le résultat d'une sonde, pour un fragment de SQL donné. */
    private void sonde(String fragment, List<Map<String, Object>> lignes) {
        reponses.put(fragment, lignes);
    }

    private static List<Map<String, Object>> une(Map<String, Object> ligne) {
        return List.of(ligne);
    }

    /** {@code Map.of} refuse les valeurs nulles ; les lignes SQL en portent. */
    private static Map<String, Object> ligne(Object... cleValeur) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < cleValeur.length; i += 2) {
            map.put(String.valueOf(cleValeur[i]), cleValeur[i + 1]);
        }
        return map;
    }

    private static String sql(Object[] appel) {
        return (String) appel[0];
    }

    /** Les valeurs liées aux {@code ?}, hors premier argument (le SQL). */
    private static List<Object> valeurs(Object[] appel) {
        return List.of(appel).subList(1, appel.length);
    }

    private Object[] derniereLecture() {
        return lectures.get(lectures.size() - 1);
    }

    private Object[] derniereEcriture() {
        return ecritures.get(ecritures.size() - 1);
    }

    /** Une ligne de rôle, telle que la listent listRoles / getRole. */
    private static Map<String, Object> ligneRole(String cle, boolean system, UUID tenantId) {
        return ligne("role_key", cle, "role_label", "Libellé " + cle, "role_description", null,
                "role_system", system, "role_tenant_id", tenantId, "nb_permissions", 12L);
    }

    /* ==================== lecture de la matrice ==================== */

    @Test
    @DisplayName("GET matrice : jointure canonique, projection R/W/D rendue en snake ET camelCase")
    void getAllPermissions_ShouldReturnList() {
        sonde("SELECT r.key AS role_key, p.key AS permission_key", List.of(
                ligne("role_key", "ADMIN", "permission_key", "USER_CREATE", "permission_scope", "TENANT"),
                ligne("role_key", "PASTEUR", "permission_key", "FAMILY_CREATE", "permission_scope", "TENANT")
        ));

        List<Map<String, Object>> result = permissionService.getAllPermissions();

        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).get("role"));
        assertEquals("USER_CREATE", result.get(0).get("permission"));
        // La présence de la ligne est le seul bit du modèle canonique : enabled fait foi.
        assertEquals(true, result.get(0).get("enabled"));
        // USER_CREATE est une clé d'écriture : lecture fausse, écriture vraie.
        assertEquals(false, result.get(0).get("can_read"));
        assertEquals(true, result.get(0).get("can_write"));
        // L'éditeur hérité lit camelCase : sans ces clés, ses colonnes R/W/D restaient vides.
        assertEquals(true, result.get(0).get("canWrite"));
        assertEquals("TENANT", result.get(0).get("scope"));

        String sql = sql(derniereLecture());
        assertTrue(sql.contains(JOINTURE), sql);
        assertTrue(sql.contains("ORDER BY r.key, p.key"), sql);
        assertFalse(sql.contains("platform_roles"), sql);
        // La ligne de rôle qui décide, pas toutes celles portant le même nom.
        assertTrue(sql.contains("d.tenant_id"), sql);
    }

    @Test
    void getAllPermissions_WhenEmpty_ShouldReturnEmptyList() {
        List<Map<String, Object>> result = permissionService.getAllPermissions();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GET par rôle : la clé est normalisée et passée en paramètre, pas concaténée")
    void getPermissionsByRole_ShouldReturnFilteredResults() {
        sonde("SELECT r.key AS role_key, p.key AS permission_key",
                une(ligne("role_key", "FAISEUR", "permission_key", "SOUL_CREATE", "permission_scope", "TENANT")));

        List<Map<String, Object>> result = permissionService.getPermissionsByRole("faiseur");

        assertEquals(1, result.size());
        assertEquals("FAISEUR", result.get(0).get("role"));
        String sql = sql(derniereLecture());
        assertTrue(sql.contains("AND r.key = ?"), sql);
        assertFalse(sql.contains("FAISEUR"), "la clé ne doit pas être concaténée dans le SQL");
        assertTrue(valeurs(derniereLecture()).contains("FAISEUR"), valeurs(derniereLecture()).toString());
    }

    @Test
    void getPermissionsByRole_WithUnknownRole_ShouldReturnEmpty() {
        List<Map<String, Object>> result = permissionService.getPermissionsByRole("UNKNOWN_ROLE");

        assertTrue(result.isEmpty());
    }

    /* ==================== écriture de la matrice ==================== */

    @Test
    @DisplayName("PUT enabled=true : accord par INSERT sur les identifiants canoniques")
    void updatePermission_EnablePermission_ShouldUpdateAndReturnResult() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));
        sonde("SELECT p.id AS permission_id", une(ligne("permission_id", PERMISSION_ID)));
        sonde("SELECT p.scope AS perm_scope", une(ligne("perm_scope", "TENANT")));

        Map<String, Object> result = permissionService.updatePermission("faiseur", "soul_create", true);

        assertEquals("FAISEUR", result.get("role"));
        assertEquals("SOUL_CREATE", result.get("permission"));
        assertEquals(true, result.get("enabled"));
        String sql = sql(derniereEcriture());
        assertEquals("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", sql);
        assertEquals(List.of(ROLE_ID, PERMISSION_ID), valeurs(derniereEcriture()));
    }

    @Test
    @DisplayName("PUT enabled=false : révocation ciblant la ligne de rôle qui décide")
    void updatePermission_DisablePermission_ShouldUpdateAndReturnDisabled() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));
        sonde("SELECT p.id AS permission_id", une(ligne("permission_id", PERMISSION_ID)));
        sonde("SELECT p.scope AS perm_scope", une(ligne("perm_scope", "TENANT")));

        Map<String, Object> result = permissionService.updatePermission("pasteur", "family_delete", false);

        assertEquals("PASTEUR", result.get("role"));
        assertEquals("FAMILY_DELETE", result.get("permission"));
        assertEquals(false, result.get("enabled"));
        assertEquals(false, result.get("canWrite"));
        String sql = sql(derniereEcriture());
        assertEquals("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?", sql);
        assertEquals(List.of(ROLE_ID, PERMISSION_ID), valeurs(derniereEcriture()));
    }

    @Test
    @DisplayName("PUT sur une clé inconnue : refus, plus de 200 silencieux (défaut D7)")
    void updatePermission_WithUnknownRoleOrPermission_ShouldReject() {
        // Sonde rôle non renseignée : la réponse par défaut est vide = rôle inconnu.
        IllegalArgumentException leve = assertThrows(IllegalArgumentException.class,
                () -> permissionService.updatePermission("unknown", "unknown_perm", true));

        assertTrue(leve.getMessage().contains("UNKNOWN"), leve.getMessage());
        // Contrôle négatif : rien n'est écrit. L'ancien code faisait un UPDATE à 0 ligne
        // d'impact et renvoyait un 200 — l'opérateur croyait sa case cochée.
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("PUT sur une permission inconnue : refus, et le rôle n'est pas écrit non plus")
    void updatePermission_WithUnknownPermissionKey_ShouldReject() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));

        IllegalArgumentException leve = assertThrows(IllegalArgumentException.class,
                () -> permissionService.updatePermission("FAISEUR", "FAMILY_EXPLODE", true));

        assertTrue(leve.getMessage().contains("FAMILY_EXPLODE"), leve.getMessage());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("RWD : la clé de suppression se révoque quand la suppression est décochée")
    void updatePermissionRWD_DeleteKeyUnchecked_ShouldRevoke() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));
        sonde("SELECT p.id AS permission_id", une(ligne("permission_id", PERMISSION_ID)));

        Map<String, Object> result = permissionService.updatePermissionRWD(
                "FAISEUR", "FAMILY_DELETE", true, true, false, "GLOBAL");

        assertEquals(false, result.get("enabled"));
        assertEquals("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?",
                sql(derniereEcriture()));
    }

    @Test
    @DisplayName("RWD : le scope rendu est celui de la permission, pas celui qu'on a demandé")
    void updatePermissionRWD_ReturnsServerScopeNotRequestedScope() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));
        sonde("SELECT p.id AS permission_id", une(ligne("permission_id", PERMISSION_ID)));
        sonde("SELECT p.scope AS perm_scope", une(ligne("perm_scope", "TENANT")));

        Map<String, Object> result = permissionService.updatePermissionRWD(
                "FAISEUR", "FAMILY_CREATE", true, true, false, "OWN");

        // Le modèle canonique ne porte pas de scope par ligne (rôle, permission) :
        // prétendre avoir appliqué « OWN » serait un mensonge d'affichage.
        assertEquals("TENANT", result.get("scope"));
    }

    /* ==================== accord / niveaux ==================== */

    @Test
    void hasPermission_WithGrantedKey_ShouldReturnTrue() {
        sonde("SELECT 1 " + JOINTURE, une(ligne("un", 1)));

        boolean result = permissionService.hasPermission("faiseur", "soul_create");

        assertTrue(result);
        String sql = sql(derniereLecture());
        assertTrue(sql.startsWith("SELECT 1 " + JOINTURE), sql);
        // Le rôle et la permission sont normalisés en majuscules et liés en paramètres.
        assertTrue(valeurs(derniereLecture()).containsAll(List.of("FAISEUR", "SOUL_CREATE")),
                valeurs(derniereLecture()).toString());
    }

    @Test
    void hasPermission_WithRevokedKey_ShouldReturnFalse() {
        boolean result = permissionService.hasPermission("FAISEUR", "FAMILY_DELETE");

        assertFalse(result);
    }

    @Test
    void hasPermission_WithUnknownRole_ShouldReturnFalseRestrictif() {
        // Sémantique sécurisée : pas de ligne dans la matrice = refusé (restrictif par défaut).
        boolean result = permissionService.hasPermission("UNKNOWN", "ANY_PERMISSION");

        assertFalse(result);
    }

    @Test
    void hasPermission_WithAdminRole_ShouldAlwaysReturnTrue() {
        boolean result = permissionService.hasPermission("ADMIN", "ANY_PERMISSION");

        assertTrue(result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void hasPermission_WithPasteurRole_ShouldAlwaysReturnTrue() {
        boolean result = permissionService.hasPermission("PASTEUR", "ANY_PERMISSION");

        assertTrue(result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("Niveaux : READ sur une clé d'écriture = refusé ; MANAGE = écriture ou suppression")
    void hasPermissionLevel_SuitLaProjectionDeCle() {
        sonde("SELECT 1 " + JOINTURE, une(ligne("un", 1)));

        assertTrue(permissionService.hasPermissionLevel("FAISEUR", "FAMILY_READ", "READ"));
        assertFalse(permissionService.hasPermissionLevel("FAISEUR", "FAMILY_READ", "WRITE"));
        assertTrue(permissionService.hasPermissionLevel("FAISEUR", "FAMILY_DELETE", "DELETE"));
        assertTrue(permissionService.hasPermissionLevel("FAISEUR", "FAMILY_DELETE", "MANAGE"));
        assertFalse(permissionService.hasPermissionLevel("FAISEUR", "FAMILY_CREATE", "DELETE"));
    }

    @Test
    @DisplayName("Scope : lu sur permissions.scope (la ligne rôle×permission n'en porte plus)")
    void getPermissionScope_LitLeCatalogueCanonique() {
        sonde("SELECT 1 " + JOINTURE, une(ligne("un", 1)));
        sonde("SELECT p.scope AS perm_scope", une(ligne("perm_scope", "DEPARTMENT")));

        assertEquals("DEPARTMENT", permissionService.getPermissionScope("FAISEUR", "FAMILY_READ"));
    }

    @Test
    void getPermissionScope_SansAccord_ShouldReturnNone() {
        assertEquals("NONE", permissionService.getPermissionScope("FAISEUR", "FAMILY_READ"));
    }

    /* ==================== catalogue et rôles ==================== */

    @Test
    @DisplayName("Catalogue servi : table permissions (503 clés), pas permission_catalog (56)")
    void listPermissionCatalog_VientDuCatalogueCanonique() {
        sonde("SELECT p.key AS perm_key", une(ligne("perm_key", "ANNOUNCEMENTS_CREATE",
                "perm_label", "Créer une annonce", "perm_category", "ANNOUNCEMENTS",
                "perm_description", null, "perm_scope", "TENANT")));

        List<Map<String, Object>> catalog = permissionService.listPermissionCatalog();

        assertEquals(1, catalog.size());
        assertEquals("ANNOUNCEMENTS", catalog.get(0).get("module"));
        assertEquals("TENANT", catalog.get(0).get("defaultScope"));
        assertEquals(10, catalog.get(0).get("ordre"));
        String sql = sql(derniereLecture());
        assertTrue(sql.contains("FROM permissions p"), sql);
        assertFalse(sql.contains("permission_catalog"), sql);
    }

    @Test
    @DisplayName("Rôles : table roles avec comptage par role_id, plus platform_roles")
    void listRoles_VientDeLaTableCanonique() {
        sonde("SELECT r.key AS role_key, r.label", une(ligneRole("ADMIN", true, null)));

        List<Map<String, Object>> roles = permissionService.listRoles();

        assertEquals("ADMIN", roles.get(0).get("key"));
        assertEquals(true, roles.get(0).get("system"));
        assertEquals(true, roles.get(0).get("global"));
        assertEquals(12L, roles.get(0).get("nb_permissions"));
        String sql = sql(derniereLecture());
        assertTrue(sql.contains("FROM roles r"), sql);
        assertTrue(sql.contains("rp.role_id = r.id"), sql);
        assertFalse(sql.contains("platform_roles"), sql);
    }

    @Test
    @DisplayName("Rôles : une clé présente chez deux églises n'est jamais mélangée")
    void listRoles_SonneLaLigneQuiDecide() {
        TenantContext.setTenantId(EGLISE);
        sonde("SELECT r.key AS role_key, r.label", une(ligneRole("DIACRE", false, EGLISE)));

        permissionService.listRoles();

        String sql = sql(derniereLecture());
        assertTrue(sql.contains("(r.tenant_id IS NULL OR r.tenant_id = ?)"), sql);
        assertTrue(sql.contains("ORDER BY d.tenant_id NULLS LAST"), sql);
        assertTrue(valeurs(derniereLecture()).contains(EGLISE), valeurs(derniereLecture()).toString());
    }

    @Test
    void createRole_ExistingRole_ShouldRefuse() {
        sonde("SELECT 1 FROM roles r", une(ligne("un", 1)));

        assertThrows(IllegalStateException.class, () -> permissionService.createRole("ADMIN", "Ad", ""));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("createRole : la ligne porte le locataire du contexte, pas NULL")
    void createRole_PortLeLocataireCourant() {
        TenantContext.setTenantId(EGLISE);

        permissionService.createRole("DIACRE", "Diacre", "Accueil");

        // Le rôle créé est toujours non système : « system » est un littéral du SQL,
        // pas un paramètre — d'où quatre valeurs, pas cinq.
        assertTrue(sql(derniereEcriture()).contains("VALUES (?, ?, ?, FALSE, ?)"), sql(derniereEcriture()));
        assertEquals(List.of("DIACRE", "Diacre", "Accueil", EGLISE), valeurs(derniereEcriture()));
    }

    @Test
    @DisplayName("updateRole : écrit par identifiant, une colonne à la fois")
    void updateRole_EcritParIdentifiant() {
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));

        permissionService.updateRole("DIACRE", "Nouveau libellé", "Nouvelle description");

        assertEquals(2, ecritures.size());
        assertTrue(sql(ecritures.get(0)).startsWith("UPDATE roles SET label = ?, updated_at = ? WHERE id = ?"),
                sql(ecritures.get(0)));
        assertTrue(sql(ecritures.get(1)).startsWith("UPDATE roles SET description = ?, updated_at = ? WHERE id = ?"),
                sql(ecritures.get(1)));
        assertEquals(List.of(ROLE_ID), valeurs(ecritures.get(1)).subList(2, 3));
    }

    @Test
    @DisplayName("updateRole sur un rôle hors de portée : introuvable, rien n'est écrit")
    void updateRole_RoleIntrouvable_ShouldFail() {
        assertThrows(NoSuchElementException.class,
                () -> permissionService.updateRole("DIACRE", "X", null));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("duplicateRole : la copie est la surcouche de l'église courante, droits copiés")
    void duplicateRole_CreeUneSurcoucheLocale() {
        TenantContext.setTenantId(EGLISE);
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));

        permissionService.duplicateRole("MEMBRE", "COPIE", "Copie du modèle");

        String insertionRole = sql(ecritures.get(0));
        assertTrue(insertionRole.startsWith("INSERT INTO roles (id, key, label, description, system, tenant_id)"),
                insertionRole);
        assertEquals(EGLISE, valeurs(ecritures.get(0)).get(4));
        assertTrue(sql(ecritures.get(1)).contains("INSERT INTO role_permissions (role_id, permission_id)"),
                sql(ecritures.get(1)));
        assertTrue(sql(ecritures.get(1)).contains("NOT EXISTS"), sql(ecritures.get(1)));
    }

    @Test
    @DisplayName("Suppression de rôle : refusée tant que des membres le portent (jamais de cascade silencieuse)")
    void deleteRole_WithHolders_ShouldRefuse() {
        sonde("SELECT r.key AS role_key, r.label", une(ligneRole("DIACRE", false, EGLISE)));
        sonde("SELECT r.id AS role_id", une(ligne("role_id", ROLE_ID)));
        porteursACompter = 1;

        IllegalStateException leve = assertThrows(IllegalStateException.class,
                () -> permissionService.deleteRole("DIACRE"));
        assertTrue(leve.getMessage().contains("DIACRE"), leve.getMessage());
        // Une par table d'affectation : le compte rendu à l'opérateur est la somme,
        // pas le résultat d'une seule requête.
        assertTrue(leve.getMessage().contains("3 affectation"), leve.getMessage());
        // Trois tables d'affectation sont interrogées : aucune ne doit être oubliée,
        // sinon le rôle partirait en CASCADE en emportant des membres.
        assertEquals(3, comptages.size());
        assertTrue(comptages.stream().map(PermissionServiceTest::sql)
                .allMatch(s -> s.contains("WHERE 1=1 AND m.role_id = ?")), String.join("\n",
                comptages.stream().map(PermissionServiceTest::sql).toList()));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void deleteRole_SystemRole_ShouldRefuse() {
        sonde("SELECT r.key AS role_key, r.label", une(ligneRole("ADMIN", true, null)));

        assertThrows(IllegalStateException.class, () -> permissionService.deleteRole("ADMIN"));
        assertTrue(comptages.isEmpty(), "un rôle système est refusé avant même de compter");
    }

    @Test
    @DisplayName("userHasPermission : ADMIN court-circuite, sinon la clé doit être accordée")
    void userHasPermission_AccordOuRefus() {
        assertTrue(permissionService.userHasPermission(List.of("ADMIN"), "FAMILY_DELETE"));
        assertFalse(permissionService.userHasPermission(List.of("FAISEUR"), "FAMILY_DELETE"));
        assertFalse(permissionService.userHasPermission(List.of(), "FAMILY_DELETE"));
    }
}

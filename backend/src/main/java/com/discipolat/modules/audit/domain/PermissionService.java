package com.discipolat.modules.audit.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Matrice des permissions (rôle → permission), surface héritée
 * {@code /api/v1/permissions}.
 *
 * <p><strong>Pourquoi ce fichier a été réécrit, et non supprimé.</strong> Son SQL
 * visait la table {@code role_permissions} dans sa forme <em>antérieure</em> à la
 * migration {@code V135__create_multi_tenant_core_tables.sql}, qui a supprimé et
 * recréé cette table en jointure {@code (role_id, permission_id)} :
 *
 * <pre>
 *   \d role_permissions   --&gt;  role_id uuid, permission_id uuid   (deux colonnes)
 *   SELECT role, permission, enabled, can_read, can_write, can_delete, scope
 *     FROM role_permissions  --&gt;  ERROR: column "role" does not exist  (SQLSTATE 42703)
 * </pre>
 *
 * <p>Six URL sur dix répondaient donc mécaniquement par une exception SQL, et les
 * quatre autres lisaient {@code platform_roles} / {@code permission_catalog}, deux
 * tables que <strong>aucun garde d'autorisation ne consulte</strong>. Le choix
 * retenu est la remise en marche : chaque requête ci-dessous est écrite sur le
 * modèle canonique {@code roles} / {@code permissions} /
 * {@code role_permissions(role_id, permission_id)}, c'est-à-dire <em>les mêmes
 * lignes que lit {@code AuthorizationService}</em>. La surface héritée et la
 * surface {@code /api/v1/admin/roles} ne peuvent donc plus se contredire.
 *
 * <p><strong>Ce que le modèle canonique n'exprime pas, et comment c'est traité.</strong>
 * La jointure ne porte ni {@code enabled}, ni {@code can_read}/{@code can_write}/
 * {@code can_delete}, ni {@code scope} par couple (rôle, permission) :
 * <ul>
 *   <li>{@code enabled} = <em>présence de la ligne</em>. Une ligne accordée est
 *       vraie ; une absence est fausse — déjà le comportement restrictif d'avant.</li>
 *   <li>Le triplet R/W/D est une <strong>projection</strong> calculée sur le nom de
 *       la clé ({@code FAMILY_READ} → lecture, {@code FAMILY_UPDATE} → écriture,
 *       {@code FAMILY_DELETE} → suppression). Le bit qui fait foi reste
 *       {@code enabled} ; la règle exacte est dans {@link #projection}.</li>
 *   <li>{@code scope} est une propriété de la <em>permission</em>
 *       ({@code permissions.scope}) et de l'<em>appartenance</em>
 *       ({@code tenant_memberships.scope}, ajouté en V136) — plus de la ligne
 *       rôle×permission. Une écriture de scope n'est donc pas répercutée, et la
 *       valeur renvoyée est la valeur réelle du serveur : l'appelant ne croit pas
 *       avoir obtenu ce qu'il n'a pas obtenu.</li>
 * </ul>
 *
 * <p><strong>Écrire une clé inconnue est refusé.</strong> L'ancien code exécutait un
 * {@code UPDATE} sur une ligne inexistante : aucun impact, un 200, et l'opérateur
 * croyait sa case cochée — c'est le défaut D7. Les écritures passent par
 * {@link #requireRole} et {@link #requirePermission}, qui lèvent
 * {@link IllegalArgumentException} (traduit en 400), dans la continuité directe de
 * ce que fait déjà {@code /api/v1/admin/roles} depuis la Phase 9.
 *
 * <p>Le multi-tenant est traité explicitement, comme avant : ce service fait du SQL
 * brut, qui échappe au filtre Hibernate. La convention suit
 * {@code PermissionResolver} — le rôle propre à l'église prime sur le rôle global,
 * et un rôle ou une permission {@code tenant_id IS NULL} est partagé.
 */
@Service("auditPermissionService")
@Transactional
public class PermissionService {

    private final JdbcTemplate jdbcTemplate;

    public PermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Constructeur de requête. Il existe pour une raison précise : le SQL et ses
     * paramètres sont posés <em>ensemble</em>. La version d'avant composait des
     * fragments à gauche et comptait les {@code ?} de portée à droite ; dès qu'une
     * requête ajoutait une clause, l'ordre des paramètres se décalait et le filtre
     * tenant s'appliquait à la mauvaise colonne — une portée élargie sans message
     * d'erreur. Un appel {@code eq} / {@code porteeRole} ne peut plus produire ce
     * décalage.
     */
    private final class Requete {

        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        Requete sql(String fragment) {
            sql.append(fragment);
            return this;
        }

        /** Égale une colonne (avec le {@code ?} correspondant posé dans la foulée). */
        Requete eq(String colonne, Object valeur) {
            sql.append(" AND ").append(colonne).append(" = ?");
            params.add(valeur);
            return this;
        }

        /**
         * Ajoute un prédicat <em>complet</em> et pose la valeur de son {@code ?}.
         *
         * <p>Il existe parce que {@link #eq} ne sait que comparer une colonne à une
         * valeur : la portée tenant est une disjonction, {@code (x.tenant_id IS NULL
         * OR x.tenant_id = ?)}. En passant ce prédicat par {@code eq}, le constructeur
         * y ajoutait son propre {@code = ?} — {@code (…) = ?} — et le moteur refusait
         * la requête (« Parameter #n is not set ») dès qu'un contexte tenant était
         * actif. Un mock ne lit pas le SQL : c'est le second bug attrapé par la preuve
         * d'exécution {@code PermissionServiceCanonicalSqlTest}, et le motif pour
         * lequel elle existe.
         */
        Requete clause(String predicat, Object valeur) {
            sql.append(" AND ").append(predicat);
            params.add(valeur);
            return this;
        }

        /**
         * Pose une valeur pour un {@code ?} déjà écrit dans le fragment. Réservé aux
         * clauses {@code SET}, où la colonne vient du code et non d'une entrée
         * utilisateur ; partout ailleurs, préférer {@link #eq}, qui lie le texte et
         * la valeur dans le même geste.
         */
        Requete val(Object valeur) {
            params.add(valeur);
            return this;
        }

        /** Portée canonique d'un rôle : global partagé ou instance de l'église courante. */
        Requete porteeRole(String alias) {
            UUID tenantId = tenantId();
            if (tenantId != null) {
                clause("(" + alias + ".tenant_id IS NULL OR " + alias + ".tenant_id = ?)", tenantId);
            }
            return this;
        }

        /** Portée canonique d'une permission. */
        Requete porteePermission(String alias) {
            UUID tenantId = tenantId();
            if (tenantId != null) {
                clause("(" + alias + ".tenant_id IS NULL OR " + alias + ".tenant_id = ?)", tenantId);
            }
            return this;
        }

        /**
         * Ne laisse passer que la ligne de rôle qui <strong>décide</strong> pour le
         * contexte courant&nbsp;: la surcouche de l'église si elle existe, sinon le
         * modèle global.
         *
         * <p>Cette clause existe parce que {@code uk_role_tenant_key} autorise la
         * même clé sur deux lignes. Sans cette clause, une église qui a personnalisé
         * «&nbsp;DIACRE&nbsp;» voyait aussi les droits du «&nbsp;DIACRE&nbsp;» global&nbsp;:
         * deux sources de vérité dans la même liste, et un droit révoqué localement qui
         * ressurgissait par le modèle partagé — pire, une écriture envoyée sur
         * <em>les deux</em> lignes, c'est-à-dire sur tous les locataires. C'est la règle
         * déjà appliquée par {@code PermissionResolver} et par
         * {@code RoleManagementService.findByKey} (la ligne tenant prime, le global est
         * le repli).
         */
        Requete roleQuiDecide(String alias) {
            UUID tenantId = tenantId();
            sql(" AND ").sql(alias).sql(".id = (SELECT d.id FROM roles d WHERE d.key = ")
                    .sql(alias).sql(".key");
            if (tenantId != null) {
                clause("(d.tenant_id IS NULL OR d.tenant_id = ?)", tenantId);
            }
            // Une surcouche porte un tenant_id non nul : elle passe avant le modèle.
            // Hors contexte tenant (plateforme), c'est le modèle global qui décide.
            sql(" ORDER BY d.tenant_id ").sql(tenantId != null ? "NULLS LAST" : "NULLS FIRST")
                    .sql(" LIMIT 1)");
            return this;
        }

        List<Map<String, Object>> rows() {
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
        }

        boolean present() {
            return !rows().isEmpty();
        }

        int update() {
            return jdbcTemplate.update(sql.toString(), params.toArray());
        }

        int compter() {
            Integer n = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
            return n == null ? 0 : n;
        }
    }

    /* ======================== Locataire ======================== */

    private UUID tenantId() {
        return TenantContext.getTenantId();
    }

    /**
     * La jointure canonique. Les alias y sont fixés ({@code r}, {@code p},
     * {@code rp}) parce que les clauses de portée les mentionnent en dur.
     */
    private static final String JOINTURE =
            "FROM role_permissions rp JOIN roles r ON r.id = rp.role_id JOIN permissions p ON p.id = rp.permission_id";

    /** Le couple (rôle, permission) est-il accordé ? C'est le seul bit du modèle canonique. */
    private boolean accorde(String role, String permission) {
        return new Requete()
                .sql("SELECT 1 " + JOINTURE + " WHERE 1=1")
                .eq("r.key", role.toUpperCase(Locale.ROOT))
                .eq("p.key", permission.toUpperCase(Locale.ROOT))
                .roleQuiDecide("r")
                .porteePermission("p")
                .present();
    }

    /* ======================== Matrice des permissions ======================== */

    public List<Map<String, Object>> getAllPermissions() {
        return projeter(new Requete()
                .sql("SELECT r.key AS role_key, p.key AS permission_key, p.scope AS permission_scope "
                        + JOINTURE + " WHERE 1=1")
                .porteeRole("r")
                .roleQuiDecide("r")
                .porteePermission("p")
                .sql(" ORDER BY r.key, p.key")
                .rows());
    }

    public List<Map<String, Object>> getPermissionsByRole(String role) {
        return projeter(new Requete()
                .sql("SELECT r.key AS role_key, p.key AS permission_key, p.scope AS permission_scope "
                        + JOINTURE + " WHERE 1=1")
                .eq("r.key", role.toUpperCase(Locale.ROOT))
                .porteeRole("r")
                .roleQuiDecide("r")
                .porteePermission("p")
                .sql(" ORDER BY p.key")
                .rows());
    }

    public Map<String, Object> updatePermission(String role, String permission, boolean enabled) {
        String r = role.toUpperCase(Locale.ROOT);
        String p = permission.toUpperCase(Locale.ROOT);
        requireRole(r);
        requirePermission(p);
        ecrire(r, p, enabled);
        return projection(r, p, scopeCourant(p), enabled);
    }

    /**
     * Met à jour les permissions granulaires (lecture/écriture/suppression) + scope.
     *
     * <p>Le modèle canonique n'accorde qu'un seul bit par (rôle, permission) : la
     * présence de la clé. Le bit qui gouverne l'écriture est donc celui que la clé
     * représente réellement — {@code FAMILY_DELETE} se retire si la suppression est
     * décochée, {@code FAMILY_UPDATE} si l'écriture est décochée — et les deux autres
     * dimensions sont informelles. Sans cette règle, décocher « Suppression » sur une
     * clé de lecture aurait prétendu révoquer un droit que cette clé ne porte pas.
     */
    public Map<String, Object> updatePermissionRWD(String role, String permission,
                                                     boolean canRead, boolean canWrite, boolean canDelete,
                                                     String scope) {
        String r = role.toUpperCase(Locale.ROOT);
        String p = permission.toUpperCase(Locale.ROOT);
        requireRole(r);
        requirePermission(p);
        boolean grantie = estSuppression(p) ? canDelete : (estLecture(p) ? canRead : canWrite);
        ecrire(r, p, grantie);
        return projection(r, p, scopeCourant(p), grantie);
    }

    /**
     * Vérifie si un rôle donné a une permission activée explicitement.
     * RESTRICTIF par défaut : si aucune ligne n'existe, retourne false (sécurité).
     * Les rôles ADMIN et PASTEUR conservent un accès complet via {@link #userHasPermission}.
     */
    public boolean hasPermission(String role, String permission) {
        String r = role.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(r) || "PASTEUR".equals(r)) return true;
        return accorde(r, permission.toUpperCase(Locale.ROOT));
    }

    /**
     * Vérifie si un rôle a un niveau de permission (read/write/delete) donné.
     * Niveau "manage" = write OR delete.
     *
     * <p>Le niveau est évalué sur la projection de la clé : le modèle canonique ne
     * stocke pas de niveaux séparés par ligne, il stocke des clés.
     */
    public boolean hasPermissionLevel(String role, String permission, String level) {
        String r = role.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(r) || "PASTEUR".equals(r)) return true;
        String p = permission.toUpperCase(Locale.ROOT);
        if (!accorde(r, p)) return false;
        return switch (level == null ? "" : level.toUpperCase(Locale.ROOT)) {
            case "READ" -> estLecture(p);
            case "WRITE" -> !estLecture(p);
            case "DELETE" -> estSuppression(p);
            case "MANAGE" -> !estLecture(p) || estSuppression(p);
            default -> true;   // niveau non nommé : la présence de la clé suffit, comme avant
        };
    }

    /**
     * Retourne le scope configuré pour un rôle + permission. Depuis V135, la portée
     * n'est plus portée par la ligne (rôle, permission) : cette méthode lit
     * {@code permissions.scope}, qui est la valeur réellement appliquée par les gardes.
     */
    public String getPermissionScope(String role, String permission) {
        String p = permission.toUpperCase(Locale.ROOT);
        if (!accorde(role.toUpperCase(Locale.ROOT), p)) {
            return "NONE";
        }
        return scopeCourant(p);
    }

    /* ======================== Catalogue des permissions ======================== */

    /**
     * Le catalogue servi. Il est lu dans {@code permissions} — la table que les
     * gardes consultent — et non plus dans {@code permission_catalog}, qui n'a jamais
     * contenu que 56 clés là où le serveur en applique 503. C'est la correction du
     * défaut D7 sur la surface héritée : les cases montrées ici sont les droits réels.
     */
    public List<Map<String, Object>> listPermissionCatalog() {
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT p.key AS perm_key, p.label AS perm_label, p.category AS perm_category, "
                        + "p.description AS perm_description, p.scope AS perm_scope "
                        + "FROM permissions p WHERE 1=1")
                .porteePermission("p")
                .sql(" ORDER BY p.category, p.key")
                .rows();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        int ordre = 0;
        for (Map<String, Object> row : rows) {
            ordre++;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", row.get("perm_key"));
            entry.put("label", row.get("perm_label"));
            Object category = row.get("perm_category");
            entry.put("module", category == null ? "General" : category.toString());
            entry.put("description", row.get("perm_description"));
            entry.put("ordre", ordre * 10);
            String scope = row.get("perm_scope") == null ? "GLOBAL" : row.get("perm_scope").toString();
            entry.put("defaultScope", scope);
            entry.put("scopeDescription", descriptionDePortee(scope));
            out.add(entry);
        }
        return out;
    }

    /* ======================== Gestion des rôles ======================== */

    /**
     * Rôles visibles : les rôles partagés ({@code tenant_id IS NULL}) et, dans un
     * contexte tenant, les surcouches locales de l'église. {@code platform_roles}
     * n'est plus consultée — le serveur n'y lit aucun droit.
     */
    public List<Map<String, Object>> listRoles() {
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT r.key AS role_key, r.label AS role_label, r.description AS role_description, "
                        + "r.system AS role_system, r.tenant_id AS role_tenant_id, "
                        + "(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = r.id) AS nb_permissions "
                        + "FROM roles r WHERE 1=1")
                .porteeRole("r")
                .roleQuiDecide("r")
                .sql(" ORDER BY r.system DESC, r.key")
                .rows();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(role(row));
        }
        return out;
    }

    public Map<String, Object> getRole(String key) {
        String k = key.toUpperCase(Locale.ROOT);
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT r.key AS role_key, r.label AS role_label, r.description AS role_description, "
                        + "r.system AS role_system, r.tenant_id AS role_tenant_id, "
                        + "(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = r.id) AS nb_permissions "
                        + "FROM roles r WHERE 1=1")
                .eq("r.key", k)
                .porteeRole("r")
                .roleQuiDecide("r")
                .rows();
        if (rows.isEmpty()) throw new NoSuchElementException("Rôle introuvable : " + k);
        return role(rows.getFirst());
    }

    public void createRole(String key, String label, String description) {
        String k = key.toUpperCase(Locale.ROOT);
        if (existe(k)) {
            throw new IllegalStateException("Ce rôle existe déjà : " + k);
        }
        // {@code roles.label} est NOT NULL : un libellé absent retombe sur la clé plutôt
        // que de faire échouer l'insertion d'un message illisible.
        jdbcTemplate.update(
                "INSERT INTO roles (key, label, description, system, tenant_id) VALUES (?, ?, ?, FALSE, ?)",
                k, libelle(label, k), description, tenantId());
    }

    public void updateRole(String key, String label, String description) {
        String k = key.toUpperCase(Locale.ROOT);
        UUID roleId = roleQuiDecideId(k);
        if (roleId == null) {
            throw new NoSuchElementException("Rôle introuvable : " + k);
        }
        if (label != null && !label.isBlank()) {
            poser(roleId, "label", label);
        }
        if (description != null) {
            poser(roleId, "description", description);
        }
    }

    /**
     * Une seule colonne à la fois, paramètres posés dans l'ordre exact du SQL, et une
     * cible désignée par son {@code id} — plus par sa clé. La version d'avant écrivait
     * deux {@code UPDATE} dont le premier laissait ses deux premiers {@code ?} sans
     * valeur (exception immédiate) et le second comptait à la main les paramètres de
     * portée : dès qu'un contexte tenant était actif, il en manquait un. Viser l'{@code id}
     * unique de la ligne qui décide rend ce décalage impossible et empêche une église de
     * modifier le modèle global par un effet de bord.
     *
     * <p>{@code updated_at} est réécrit ici parce que l'H2 des tests n'a pas le
     * déclencheur PostgreSQL {@code update_roles_updated_at}.
     *
     * @param colonne nom fixe choisi par le code, jamais une entree utilisateur
     */
    private void poser(UUID roleId, String colonne, String valeur) {
        new Requete()
                .sql("UPDATE roles SET " + colonne + " = ?, updated_at = ? WHERE id = ?")
                .val(valeur)
                .val(LocalDateTime.now())
                .val(roleId)
                .update();
    }

    /**
     * Duplique un rôle (source) : copie ses permissions accordées vers un nouveau
     * rôle. Le nouveau rôle est non système et porte le locataire du <strong>contexte
     * courant</strong> — dupliquer le modèle global depuis une église crée donc une
     * surcouche locale de cette seule église, et non un second modèle partagé.
     */
    public void duplicateRole(String sourceKey, String newKey, String label) {
        String src = sourceKey.toUpperCase(Locale.ROOT);
        String dst = newKey.toUpperCase(Locale.ROOT);
        UUID sourceId = roleQuiDecideId(src);
        if (sourceId == null) throw new NoSuchElementException("Rôle source introuvable : " + src);
        if (existe(dst)) throw new IllegalStateException("Ce rôle existe déjà : " + dst);

        UUID copie = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO roles (id, key, label, description, system, tenant_id) VALUES (?, ?, ?, ?, FALSE, ?)",
                copie, dst, libelle(label, dst), "Dupliqué de " + src, tenantId());
        // La copie des droits reste en base : aucun UUID ne voyage en Java, et
        // NOT EXISTS rend l'opération idempotente si elle est rejouée.
        jdbcTemplate.update(
                "INSERT INTO role_permissions (role_id, permission_id) "
                        + "SELECT ?, rp.permission_id FROM role_permissions rp WHERE rp.role_id = ? "
                        + "AND NOT EXISTS (SELECT 1 FROM role_permissions x "
                        + "WHERE x.role_id = ? AND x.permission_id = rp.permission_id)",
                copie, sourceId, copie);
    }

    public void deleteRole(String key) {
        String k = key.toUpperCase(Locale.ROOT);
        Map<String, Object> role = getRole(k);
        if (Boolean.TRUE.equals(role.get("system"))) {
            throw new IllegalStateException("Impossible de supprimer un rôle système : " + k);
        }
        UUID roleId = roleQuiDecideId(k);
        // Aucune suppression « par cascade » : member_role_assignments est en RESTRICT
        // (erreur brute), tenant_memberships et role_assignment en SET NULL / CASCADE
        // (détachement silencieux). L'un comme l'autre ferait disparaître une affectation
        // que l'opérateur n'a pas demandée : on refuse donc explicitement, avec le compte.
        int porteurs = compterAffectations("tenant_memberships", roleId)
                + compterAffectations("member_role_assignments", roleId)
                + compterAffectations("role_assignment", roleId);
        if (porteurs > 0) {
            throw new IllegalStateException("Impossible de supprimer le rôle " + k
                    + " : il est encore porté par " + porteurs + " affectation(s)."
                    + " Retirez d'abord ces affectations.");
        }
        new Requete().sql("DELETE FROM role_permissions WHERE role_id = ?").val(roleId).update();
        new Requete().sql("DELETE FROM roles WHERE id = ?").val(roleId).update();
    }

    private int compterAffectations(String table, UUID roleId) {
        return new Requete()
                .sql("SELECT COUNT(*) FROM " + table + " m WHERE 1=1")
                .eq("m.role_id", roleId)
                .compter();
    }

    /**
     * Vérifie si un utilisateur (avec ses rôles) possède une permission.
     * Super-utilisateurs (ADMIN, PASTEUR) : toujours vrai.
     * Permission explicitement désactivée → faux.
     * Absence de ligne → faux (restrictif, SÉCURITÉ).
     */
    public boolean userHasPermission(Collection<String> userRoles, String permission) {
        if (userRoles == null || userRoles.isEmpty()) return false;
        String p = permission.toUpperCase(Locale.ROOT);
        for (String role : userRoles) {
            String r = role.toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(r) || "PASTEUR".equals(r)) return true;
            if (accorde(r, p)) return true;
        }
        return false;
    }

    /* ==================== Passage au modèle canonique ==================== */

    private boolean existe(String roleKey) {
        return new Requete().sql("SELECT 1 FROM roles r WHERE 1=1").eq("r.key", roleKey).porteeRole("r").present();
    }

    /**
     * La ligne de rôle qui décide pour le contexte courant, désignée par son identifiant
     * — toute écriture passe par là, afin qu'une église n'écrive jamais sur le modèle
     * partagé par un effet de bord de {@code WHERE key = ?}.
     */
    private UUID roleQuiDecideId(String roleKey) {
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT r.id AS role_id FROM roles r WHERE 1=1")
                .eq("r.key", roleKey)
                .roleQuiDecide("r")
                .rows();
        return rows.isEmpty() ? null : (UUID) rows.getFirst().get("role_id");
    }

    private void requireRole(String roleKey) {
        if (roleQuiDecideId(roleKey) == null) {
            throw new IllegalArgumentException("Rôle inconnu du serveur : " + roleKey);
        }
    }

    private void requirePermission(String permissionKey) {
        permissionId(permissionKey);
    }

    /**
     * Identifiant d'une clé de permission. La clé est UNIQUE dans {@code permissions}
     * (V135)&nbsp;: il n'existe jamais deux lignes concurrentes, donc pas de question de
     * surcouche à trancher ici — la portée ne sert qu'à ne pas servir la permission
     * privée d'une autre église.
     */
    private UUID permissionId(String permissionKey) {
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT p.id AS permission_id FROM permissions p WHERE 1=1")
                .eq("p.key", permissionKey)
                .porteePermission("p")
                .rows();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Clé de permission inconnue du serveur : " + permissionKey);
        }
        return (UUID) rows.getFirst().get("permission_id");
    }

    /** Accord ou révocation du couple (rôle, permission) — le seul bit du modèle canonique. */
    private void ecrire(String roleKey, String permissionKey, boolean accorder) {
        UUID roleId = roleQuiDecideId(roleKey);
        if (roleId == null) {
            throw new IllegalArgumentException("Rôle inconnu du serveur : " + roleKey);
        }
        UUID permissionId = permissionId(permissionKey);
        if (accorder) {
            if (accorde(roleKey, permissionKey)) {
                return;
            }
            new Requete()
                    .sql("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)")
                    .val(roleId)
                    .val(permissionId)
                    .update();
        } else {
            new Requete()
                    .sql("DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?")
                    .val(roleId)
                    .val(permissionId)
                    .update();
        }
    }

    private String scopeCourant(String permissionKey) {
        List<Map<String, Object>> rows = new Requete()
                .sql("SELECT p.scope AS perm_scope FROM permissions p WHERE 1=1")
                .eq("p.key", permissionKey)
                .porteePermission("p")
                .rows();
        if (rows.isEmpty() || rows.getFirst().get("perm_scope") == null) return "GLOBAL";
        return rows.getFirst().get("perm_scope").toString();
    }

    private List<Map<String, Object>> projeter(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(projection(text(row.get("role_key")), text(row.get("permission_key")),
                    text(row.get("permission_scope")), true));
        }
        return out;
    }

    /**
     * La ligne (rôle, permission) et son triplet R/W/D projeté.
     *
     * <p>Règle, dans cet ordre : une clé dont un segment nomme la suppression
     * ({@code DELETE}, {@code HARDDELETE}, {@code PURGE}) porte la suppression ; une
     * clé dont un segment nomme la lecture ({@code READ}, {@code VIEW}, {@code LIST},
     * {@code GET}, {@code FIND}, {@code SEARCH}, {@code COUNT}, {@code STATS},
     * {@code PRINT}, {@code EXPORT}, {@code SUGGEST}) ne porte que la lecture ; toute
     * autre clé est une écriture. {@code enabled} fait foi : c'est la présence de la
     * ligne, et la seule chose que le serveur applique.
     *
     * <p>Les valeurs sont rendues <em>aussi</em> en camelCase : l'éditeur hérité
     * ({@code PermissionsPage}) lit {@code canRead}/{@code canWrite}/{@code canDelete}
     * alors que l'ancien SQL ne renvoyait que des colonnes snake_case — l'affichage
     * granulaire ne fonctionnait donc jamais, même quand le serveur répondait.
     */
    private Map<String, Object> projection(String role, String permission, String scope, boolean grantie) {
        boolean lecture = estLecture(permission);
        boolean suppression = estSuppression(permission);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", role);
        entry.put("permission", permission);
        entry.put("enabled", grantie);
        entry.put("can_read", grantie && lecture);
        entry.put("can_write", grantie && !lecture);
        entry.put("can_delete", grantie && suppression);
        entry.put("scope", scope);
        entry.put("canRead", grantie && lecture);
        entry.put("canWrite", grantie && !lecture);
        entry.put("canDelete", grantie && suppression);
        return entry;
    }

    private Map<String, Object> role(Map<String, Object> row) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", row.get("role_key"));
        entry.put("label", row.get("role_label"));
        entry.put("description", row.get("role_description"));
        entry.put("system", Boolean.TRUE.equals(row.get("role_system")));
        entry.put("tenantId", row.get("role_tenant_id"));
        entry.put("global", row.get("role_tenant_id") == null);
        entry.put("nb_permissions", row.get("nb_permissions"));
        return entry;
    }

    private static final Set<String> SEGMENTS_SUPPRESSION = Set.of("DELETE", "HARDDELETE", "PURGE");
    private static final Set<String> SEGMENTS_LECTURE = Set.of("READ", "VIEW", "LIST", "GET", "FIND",
            "SEARCH", "COUNT", "STATS", "PRINT", "EXPORT", "SUGGEST");
    private static final Pattern SEGMENTS = Pattern.compile("[^_]+");

    private static boolean estSuppression(String cle) {
        return segments(cle).stream().anyMatch(SEGMENTS_SUPPRESSION::contains);
    }

    private static boolean estLecture(String cle) {
        if (estSuppression(cle)) {
            return false;
        }
        return segments(cle).stream().anyMatch(SEGMENTS_LECTURE::contains);
    }

    private static Set<String> segments(String cle) {
        Set<String> out = new LinkedHashSet<>();
        var m = SEGMENTS.matcher(cle == null ? "" : cle);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    private static String descriptionDePortee(String scope) {
        return switch (scope) {
            case "GLOBAL" -> "Accès à toutes les données de la plateforme.";
            case "TENANT" -> "Accès limité à l'église courante.";
            case "CHURCH" -> "Accès limité à l'Église.";
            case "SUB_CHURCH" -> "Accès limité à la sous-Église.";
            case "DEPARTMENT" -> "Accès limité aux départements assignés.";
            case "FAMILY" -> "Accès limité à la famille assignée.";
            case "OWN" -> "Accès limité aux propres données de l'utilisateur.";
            default -> null;
        };
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    /** {@code roles.label} est NOT NULL&nbsp;: un libellé vide retombe sur la clé. */
    private static String libelle(String label, String cle) {
        return label == null || label.isBlank() ? cle : label;
    }
}

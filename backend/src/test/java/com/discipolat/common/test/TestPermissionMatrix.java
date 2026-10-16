package com.discipolat.common.test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Double de test du pont d'équivalence pour les tranches {@code @WebMvcTest}.
 *
 * <p>Sous slice, Flyway est désactivé : les tables {@code roles}/{@code
 * permissions}/{@code role_permissions} sont vides et le bean {@code authz} est
 * un mock. Sans source d'autorité, tout garde {@code @authz.can('CLÉ', …)}
 * refuserait y compris les cas positifs légitimes (403 au lieu de 200).
 *
 * <p>Ce fixture rejoue la <b>matrice par défaut V258/V259</b> telle qu'elle est
 * écrite dans les migrations Flyway (ressources du classpath de test) : une
 * permission est accordée si l'une des autorités actives du jeton porte un rôle
 * que la matrice attache à cette clé. Sémantique identique au pont de
 * {@code AuthorizationService} : portée TENANT et scopeId {@code null}
 * uniquement (fail-closed ailleurs), rôles globaux seulement.
 *
 * <p>Une clé absente du catalogue refusé (false) : le fixture n'élargit jamais
 * arbitrairement — même invariant qu'en production (additif seulement).
 */
public final class TestPermissionMatrix {

    /** Migrations qui définissent la matrice rôle→permission par défaut. */
    private static final String[] MATRIX_MIGRATIONS = {
            "db/migration/V258__parametrisation_permissions_par_eglise.sql",
            "db/migration/V259__parametrisation_permissions_suite_phase3.sql",
            "db/migration/V260__parametrisation_admin_events_read.sql",
    };

    /**
     * Backfill d'héritage global → surcouche locale homonyme, miroir exact de
     * V259 §5 / V260 §2 mais en dialecte H2 (pas d'{@code ON CONFLICT}). À
     * rejouer par les tests d'intégration qui créent eux-mêmes un rôle LOCAL
     * (les fixtures People) : sous profil test, Flyway est coupé, donc
     * l'héritage de production ne s'applique pas et le pont d'équivalence
     * résoudrait un rôle local vide → 403 sur les cas positifs légitimes.
     * Idempotent (NOT EXISTS) ; ne touche que les rôles locaux déjà présents.
     */
    public static final String INHERIT_GLOBAL_GRANTS_H2 =
            "INSERT INTO role_permissions (role_id, permission_id) "
            + "SELECT local.id, rp.permission_id "
            + "FROM roles g "
            + "JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL "
            + "JOIN role_permissions rp ON rp.role_id = g.id "
            + "WHERE g.tenant_id IS NULL "
            + "AND NOT EXISTS (SELECT 1 FROM role_permissions x "
            + "WHERE x.role_id = local.id AND x.permission_id = rp.permission_id)";

    /** Paires {@code ('RÔLE', 'PERMISSION')} des blocs {@code INSERT INTO role_permissions}. */
    private static final Pattern ROLE_PERM_PAIR = Pattern.compile(
            "\\(\\s*'([A-Z][A-Z0-9_]*)'\\s*,\\s*'([A-Z][A-Z0-9_]*)'\\s*\\)");

    private static volatile Map<String, Set<String>> keyToRoles;

    private TestPermissionMatrix() {
    }

    /** Clé permission → rôles globaux qui la détiennent (matrice V258+V259). */
    public static Map<String, Set<String>> keyToRoles() {
        Map<String, Set<String>> local = keyToRoles;
        if (local == null) {
            synchronized (TestPermissionMatrix.class) {
                local = keyToRoles;
                if (local == null) {
                    local = loadMatrix();
                    keyToRoles = local;
                }
            }
        }
        return local;
    }

    /**
     * Équivalent du pont {@code AuthorizationService#can} pour les slices :
     * {@code scopeType == "TENANT"} et {@code scopeId == null} exigés, puis
     * intersection autorités actives ∩ rôles de la matrice pour cette clé.
     */
    public static boolean grantsViaActiveAuthorities(String permissionKey, String scopeType, Object scopeId) {
        if (scopeId != null || !"TENANT".equalsIgnoreCase(scopeType)) {
            return false; // fail-closed : isolation par nœud portée par appartenances (invariant 4)
        }
        Set<String> roles = keyToRoles().get(permissionKey);
        if (roles == null || roles.isEmpty()) {
            return false; // clé inconnue de la matrice = refus (le fixture n'élargit jamais)
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            String role = name.startsWith("ROLE_") ? name.substring("ROLE_".length()) : name;
            if (roles.contains(role.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Set<String>> loadMatrix() {
        Map<String, Set<String>> matrix = new HashMap<>();
        for (String resource : MATRIX_MIGRATIONS) {
            String sql = readClasspath(resource);
            int cursor = 0;
            while ((cursor = sql.indexOf("INSERT INTO role_permissions", cursor)) >= 0) {
                int end = sql.indexOf(';', cursor);
                String block = sql.substring(cursor, end < 0 ? sql.length() : end);
                Matcher matcher = ROLE_PERM_PAIR.matcher(block);
                while (matcher.find()) {
                    matrix.computeIfAbsent(matcher.group(2), key -> new HashSet<>()).add(matcher.group(1));
                }
                cursor = end < 0 ? sql.length() : end;
            }
        }
        if (matrix.isEmpty()) {
            throw new IllegalStateException(
                    "matrice V258/V259 vide — migrations introuvables ou format inattendu");
        }
        return Collections.unmodifiableMap(matrix);
    }

    private static String readClasspath(String resource) {
        try (InputStream in = TestPermissionMatrix.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("migration absente du classpath de test : " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("lecture impossible : " + resource, e);
        }
    }
}

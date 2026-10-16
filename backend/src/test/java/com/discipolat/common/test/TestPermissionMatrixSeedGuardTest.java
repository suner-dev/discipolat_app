package com.discipolat.common.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verrou anti-dérive du seed H2 — PARAMÉTRISATION PAR ÉGLISE (Phase 3).
 *
 * <p>Le profil test (H2, Flyway coupé) rejoue la matrice rôle→permission de
 * V258/V259 via {@code import.sql} (généré par .e2e-tmp/param/p4/gen_h2_seed.py).
 * Si une migration modifie la matrice sans régénérer le seed — ou inversement si
 * le seed est édité à la main — les tests d'intégration mentiraient sur les
 * gardes réelles. Ce test compare les paires {@code ('RÔLE','CLÉ')} effectives :
 * matrice des migrations ≡ paires du seed (rôles globaux uniquement, comme en
 * production où le JOIN {@code tenant_id IS NULL} filtre les rôles absents).
 */
class TestPermissionMatrixSeedGuardTest {

    private static final Pattern PAIR = Pattern.compile(
            "\\(\\s*'([A-Z][A-Z0-9_]*)'\\s*,\\s*'([A-Z][A-Z0-9_]*)'\\s*\\)");

    @Test
    @DisplayName("import.sql (seed H2) porte exactement les paires effectives de V258+V259")
    void seedMatrixEquivalatesMigrations() throws Exception {
        Set<String> migrationPairs = migrationEffectivePairs();
        String seed = read("import.sql");
        // paires du bloc role_permissions uniquement
        Set<String> seedPairs = new HashSet<>();
        int idx = seed.indexOf("INSERT INTO role_permissions");
        String block = seed.substring(idx, seed.indexOf(';', idx));
        Matcher bm = PAIR.matcher(block);
        while (bm.find()) {
            seedPairs.add(bm.group(1) + "\u0000" + bm.group(2));
        }
        assertThat(seedPairs)
                .as("le seed H2 doit être strictement équivalent à la matrice V258/V259 "
                        + "— régénérer via gen_h2_seed.py")
                .isEqualTo(migrationPairs);
    }

    /** Paires V258+V259 dont le rôle est un rôle GLOBAL créé par ces migrations ou la baseline V135/V154. */
    private Set<String> migrationEffectivePairs() throws IOException {
        Set<String> createdRoles = new HashSet<>();
        // rôles globaux existant en production avant V258 (V135 §12, V154 §6)
        createdRoles.addAll(Set.of("PLATFORM_SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN",
                "CHURCH_ADMIN", "CHURCH_LEADER", "DEPARTMENT_ADMIN", "DEPARTMENT_LEADER",
                "FAMILY_LEADER", "DISCIPLE_MAKER", "MEMBER", "GUEST",
                "HEALTH_STAFF", "HEALTH_LEAD"));
        Set<String> perms = new HashSet<>();
        Map<String, Set<String>> pairsByRole = new HashMap<>();
        for (String res : new String[]{
                "db/migration/V258__parametrisation_permissions_par_eglise.sql",
                "db/migration/V259__parametrisation_permissions_suite_phase3.sql",
                "db/migration/V260__parametrisation_admin_events_read.sql"}) {
            String sql = read(res);
            // 1) rôles créés par ces migrations : tuples ('KEY','Label',N) des blocs INSERT INTO roles
            Matcher rm = Pattern.compile("\\(\\s*'([A-Z][A-Z0-9_]*)'\\s*,\\s*'[^']*'\\s*,\\s*\\d+\\s*\\)")
                    .matcher(section(sql, "INSERT INTO roles"));
            while (rm.find()) {
                createdRoles.add(rm.group(1));
            }
            // 2) permissions créées : tuples ('KEY','Label','CAT') des blocs INSERT INTO permissions
            Matcher pm = Pattern.compile("\\(\\s*'([A-Z][A-Z0-9_]*)'\\s*,\\s*'[^']*'\\s*,\\s*'([A-Z0-9_]+)'\\s*\\)")
                    .matcher(section(sql, "INSERT INTO permissions"));
            while (pm.find()) {
                perms.add(pm.group(1));
            }
            // 3) paires de la matrice
            Matcher bm = PAIR.matcher(section(sql, "INSERT INTO role_permissions"));
            while (bm.find()) {
                pairsByRole.computeIfAbsent(bm.group(1), k -> new HashSet<>()).add(bm.group(2));
            }
        }
        Set<String> effective = new HashSet<>();
        pairsByRole.forEach((role, keys) -> {
            if (createdRoles.contains(role)) {
                keys.forEach(k -> effective.add(role + "\u0000" + k));
            }
        });
        // toutes les permissions citées doivent exister dans le catalogue des deux migrations
        for (String pair : effective) {
            assertThat(perms).contains(pair.split("\u0000", 2)[1]);
        }
        assertThat(effective).isNotEmpty();
        return effective;
    }

    private static String section(String sql, String marker) {
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while ((idx = sql.indexOf(marker, idx)) >= 0) {
            int end = sql.indexOf(';', idx);
            out.append(sql, idx, end < 0 ? sql.length() : end).append('\n');
            idx = end < 0 ? sql.length() : end;
        }
        return out.toString();
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = TestPermissionMatrixSeedGuardTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(in).as("ressource de test absente : %s", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

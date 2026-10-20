package com.discipolat.common.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RATCHET PARAMÉTRISATION PAR ÉGLISE — le même trou que côté client, côté
 * SERVEUR.
 * <p>
 * Le gel {@code R7} (règle d'architecture) interdit le retour du littéral de
 * rôle, et {@link TestPermissionMatrixSeedGuardTest} vérifie seed H2 ≡
 * migrations. Mais rien ne vérifiait la question qui compte pour un garde :
 * <b>la clé citée par {@code @authz.can('…')} existe-t-elle dans le catalogue
 * servi ?</b>
 * <p>
 * Le pont d'équivalence est <b>fail-closed</b> : une clé inconnue ne se résout
 * pas, donc l'endpoint renvoie 403 — <b>pour tout le monde, silencieusement</b>.
 * C'est l'exact pendant serveur du défaut {@code DRESS_CODE_CREATE} du web, et
 * c'est <b>plus grave</b> : une route morte se voit, un endpoint mort sur
 * {@code /api/…} ne se voit pas (les écrans qui l'appellent ont déjà leur
 * propre garde de page).
 * <p>
 * Le test lit le catalogue <b>dans les migrations</b> plutôt qu'un fixture : un
 * JSON recopié se désynchronise au premier {@code INSERT} oublié, ce qui est
 * précisément la panne que ce garde doit empêcher.
 * <p>
 * Portée volontairement limitée à la garde canonique {@code @authz.can}. Le
 * bean « perm » ("@perm.has('DOMAIN','ACTION')") n'est plus concerné : il a été
 * supprimé en Phase 9 — il lisait {@code role_permissions} dans sa forme
 * pré-V135, d'où une erreur SQL systématique pour les rôles non ADMIN/PASTEUR.
 * Il ne reste qu'un seul verrou à garder.
 */
class PermissionKeyCatalogGuardTest {

    /** Clés de permission servies, lues dans les migrations qui portent le catalogue. */
    private static final List<String> CATALOG_MIGRATIONS = List.of(
            "V135__create_multi_tenant_core_tables.sql",
            "V136__add_membership_scope_and_role_fk.sql",
            "V258__parametrisation_permissions_par_eglise.sql",
            "V259__parametrisation_permissions_suite_phase3.sql",
            "V260__parametrisation_admin_events_read.sql",
            "V261__parametrisation_cles_ui_routes.sql",
            "V262__parametrisation_cles_ui_intra_pages.sql",
            "V270__parametrisation_cles_ui_residuel_dashboard.sql",
            "V271__visibilite_archives_et_assistants.sql");

    private static final Pattern KEY_TOKEN = Pattern.compile("^([A-Z][A-Z0-9_]*)$");

    private static Path moduleRoot() {
        Path p = Path.of("src", "main", "java");
        if (!Files.isDirectory(p)) {
            throw new IllegalStateException(
                    "racine du module backend introuvable (cwd=" + Path.of("").toAbsolutePath()
                            + ") — le test suppose d'être exécuté depuis `backend/`");
        }
        return Path.of("").toAbsolutePath();
    }

    /**
     * Clés de permission servies, lues depuis la SOURCE UNIQUE des migrations
     * (Phase 9 — p9g). Avant cette bascule, cette classe maintenait une liste
     * de 9 migrations et un analyseur échappement-sensible en parallèle de
     * {@link PermissionCatalogSource}, du {@code TestPermissionMatrixSeedGuardTest}
     * et du {@code TestPermissionMatrix} — déjà dérivés (9 vs 7 vs 4 fichiers).
     * Les helpers privés {@link #firstColumnOfTuples(String)},
     * {@link #firstStringLiteral(String)} et {@link #statementsOf(String, String)}
     * sont conservés sans être supprimés, pour la référence historique de
     * l'algorithmique et pour les messages d'erreur qu'ils portaient.
     */
    private static Set<String> catalogKeys() throws IOException {
        return PermissionCatalogSource.catalogKeys();
    }


    /**
     * Première colonne de chaque tuple d'un {@code VALUES}.
     * <p>
     * Parenthèses équilibrées, PAS de regex « {@code ('CLÉ',} » : V135 écrit
     * {@code (uuid_generate_v4(), NULL, 'MEMBER_READ', …)} — la clé est la
     * <b>troisième</b> colonne, et une regex collée à la parenthèse la rate.
     * C'est l'erreur qui fit annoncer 13 fausses clés manquantes au premier
     * passage de ce test (et qui fit disparaître `UI_MEMBER_REQUESTS_GLOBAL_SCOPE_ACCESS`
     * du seed H2, cf. §4.1 du handover). Les apostrophes SQL échappées ({@code ''})
     * sont traitées, un libellé peut donc contenir n'importe quoi.
     */
    private static Set<String> firstColumnOfTuples(String statement) {
        Set<String> out = new LinkedHashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        boolean inString = false;
        for (int i = 0; i < statement.length(); i++) {
            char c = statement.charAt(i);
            if (inString) {
                if (c == '\'') {
                    if (i + 1 < statement.length() && statement.charAt(i + 1) == '\'') {
                        i++;
                        continue;
                    }
                    inString = false;
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
            } else if (c == '(') {
                stack.push(i);
            } else if (c == ')' && !stack.isEmpty()) {
                int open = stack.pop();
                String tuple = statement.substring(open + 1, i);
                String first = firstStringLiteral(tuple);
                if (first != null && KEY_TOKEN.matcher(first).matches()) {
                    out.add(first);
                }
            }
        }
        return out;
    }

    /** Premier littéral SQL d'un fragment (gère {@code ''}). */
    private static String firstStringLiteral(String chunk) {
        int i = 0;
        while (i < chunk.length()) {
            if (chunk.charAt(i) == '\'') {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (j < chunk.length()) {
                    char c = chunk.charAt(j);
                    if (c == '\'') {
                        if (j + 1 < chunk.length() && chunk.charAt(j + 1) == '\'') {
                            sb.append('\'');
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    sb.append(c);
                    j++;
                }
                return sb.toString().trim();
            }
            i++;
        }
        return null;
    }

    /** Blocs `INSERT INTO <table> … ;`. */
    private static List<String> statementsOf(String sql, String table) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("INSERT\\s+INTO\\s+" + table + "\\b[\\s\\S]*?;",
                Pattern.CASE_INSENSITIVE).matcher(sql);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    /** Clés citées par les gardes `@authz.can('CLÉ', …)` de la production. */
    private static List<String> guardKeys() throws IOException {
        Pattern guard = Pattern.compile("@authz\\.can\\(\\s*'([A-Z][A-Z0-9_]*)'");
        List<String> keys = new ArrayList<>();
        Path root = moduleRoot().resolve("src/main/java");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = guard.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    keys.add(m.group(1));
                }
            }
        } catch (UncheckedIOException e) {
            throw new IOException(e);
        }
        return keys;
    }

    @Test
    @DisplayName("toute clé @authz.can existe dans le catalogue servi (endpoint qui ne serait mort pour personne)")
    void everyGuardKeyExistsInServedCatalog() throws IOException {
        Set<String> catalog = catalogKeys();
        List<String> guards = guardKeys();

        assertThat(catalog)
                .as("catalogue lu vide — le parseur de migrations a probablement décroché")
                .hasSizeGreaterThan(400);
        assertThat(guards).as("aucun garde @authz.can trouvé : le scan de src/main/java a décroché").isNotEmpty();

        Set<String> missing = new LinkedHashSet<>(guards);
        missing.removeAll(catalog);
        assertThat(missing)
                .as("clés de garde ABSENTES du catalogue servi : le pont est fail-closed, "
                        + "donc l'endpoint renvoie 403 pour tous les rôles, sans aucun signal. "
                        + "Créer la clé (migration additive) ou corriger la faute de frappe.")
                .isEmpty();
    }

    @Test
    @DisplayName("aucune clé de garde ne porte le préfixe d'un module inexistant (typo grossière)")
    void guardKeysAreNotTyposOfExistingKeys() throws IOException {
        // Détection de proximité : une clé absente du catalogue mais très proche
        // d'une clé existante est très probablement une faute de frappe
        // (`EVENETS_READ` au lieu de `EVENTS_READ`). Le message designe le voisin.
        Set<String> catalog = catalogKeys();
        List<String> missing = guardKeys().stream().filter(k -> !catalog.contains(k)).distinct().toList();
        for (String key : missing) {
            List<String> near = catalog.stream()
                    .filter(k -> levenshtein(k, key) <= 3)
                    .limit(3)
                    .toList();
            assertThat(near)
                    .as("clé de garde absente du catalogue : %s%s", key,
                            near.isEmpty() ? "" : " — clé existante(s) proche(s) : " + near)
                    .isEmpty();
        }
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = cur;
            cur = swap;
        }
        return prev[b.length()];
    }
}
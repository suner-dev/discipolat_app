package com.discipolat.common.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GARDINE DE LA SOURCE UNIQUE — Phase 9 (p9g).
 *
 * <p>Le manifeste {@code parametrisation/catalog-migrations.txt} et l'analyseur
 * {@link PermissionCatalogSource} ne valent que si le manifeste reste complet
 * et exact. Cette classe tient deux promesses symétriques :
 * <ol>
 *   <li>Toute migration {@code V≥135} qui insère des clés dans
 *       {@code permissions} est déclarée {@code catalog} ; et toute migration
 *       qui insère une paire littérale {@code ('RÔLE','CLÉ')} <em>dans une
 *       clause {@code VALUES}</em> de {@code role_permissions} est déclarée
 *       {@code matrix}.</li>
 *   <li>Réciproquement, aucune ligne du manifeste ne désigne un fichier absent
 *       du dépôt — sinon une migration a été renommée/supprimée sans mise à
 *       jour du contrat.</li>
 * </ol>
 *
 * <p><strong>Pourquoi « clause VALUES » et pas un simple {@code IN (…)}</strong>.
 * V135 et V136 écrivent {@code INSERT INTO role_permissions … SELECT r.id, p.id
 * FROM roles r JOIN permissions p ON p.key IN ('MEMBER_READ','REPORT_CREATE')} :
 * deux clés côte à côte dans un {@code IN (…)}, ce qui ressemble à une paire
 * sans en être une. Les compter comme matrice fabriquerait des droits inventés
 * (exactement ce que documente le manifeste en excluant V135/V136 du tag
 * {@code matrix}). Le test reproduit cette distinction : un tuple n'est
 * reconnu comme paire QUE s'il suit le mot-clé {@code VALUES} hors littéral
 * et commentaire.
 *
 * <p>La borne {@code V≥135} vient du fait qu'avant V135, {@code role_permissions}
 * avait le schéma {@code (role, permission, enabled, …)} — l'analyseur
 * canonique n'a pas à connaître ce modèle disparu, et une migration ancienne
 * écrite dans ce dialecte n'a aucun droit d'être tagguée.
 *
 * <p>Le test lit le système de fichiers, pas le classpath : il doit voir les
 * migrations <em>avant</em> que Maven les copie dans {@code target/classes},
 * pour qu'un {@code git pull} d'une migration non déclarée soit rouge dès la
 * première exécution.
 */
class PermissionCatalogSourceTest {

    private static final Pattern VERSION = Pattern.compile("^V(\\d{3,})__.+\\.sql$");

    private static Path migrationsDir() {
        Path p = Path.of("src/main/resources/db/migration");
        if (!Files.isDirectory(p)) {
            throw new IllegalStateException(
                    "répertoire des migrations introuvable (cwd=" + Path.of("").toAbsolutePath()
                            + ") — le test suppose d'être exécuté depuis `backend/`");
        }
        return p;
    }

    /** Nom de fichier → tags déclarés au manifeste. */
    private static Map<String, Set<String>> manifestTags() {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (PermissionCatalogSource.Migration m : PermissionCatalogSource.manifest()) {
            map.put(m.file(), m.tags());
        }
        return map;
    }

    // ==================== sémantique « mérite le tag » ====================

    /** Une migration porte {@code catalog} si un INSERT INTO permissions expose au moins une clé littérale. */
    private static boolean meriteCatalog(String sql) {
        for (String stmt : PermissionCatalogSource.insertStatements(sql, "permissions")) {
            if (!PermissionCatalogSource.firstKeyOfEachTuple(stmt).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Une migration porte {@code matrix} si un INSERT INTO role_permissions
     * contient, <em>dans une clause VALUES</em>, au moins un tuple de exactement
     * deux littéraux qui sont tous deux des clés. La restriction à VALUES est
     * la raison pour laquelle V135/V136 — dont les SELECT…IN(…) pourraient
     * passer pour des paires — restent hors du tag.
     */
    private static boolean meriteMatrix(String sql) {
        for (String stmt : PermissionCatalogSource.insertStatements(sql, "role_permissions")) {
            int apres = findValuesMotCle(stmt);
            if (apres < 0) {
                continue;
            }
            String apresValues = stmt.substring(apres);
            for (String tuple : PermissionCatalogSource.tupleContents(apresValues)) {
                List<String> lits = PermissionCatalogSource.stringTokens(tuple);
                if (lits.size() == 2
                        && PermissionCatalogSource.isKey(lits.get(0))
                        && PermissionCatalogSource.isKey(lits.get(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Position <em>après</em> le mot-clé {@code VALUES} hors littéral et hors
     * commentaire, ou -1 si absent. La recherche caractère par caractère est
     * ce qui distingue {@code 'VALUES sont…'} (libellé) de {@code VALUES (…)}
     * (mot-clé SQL).
     */
    private static int findValuesMotCle(String stmt) {
        int i = 0;
        while (i < stmt.length()) {
            char c = stmt.charAt(i);
            if (c == '\'') {
                int j = i + 1;
                while (j < stmt.length()) {
                    if (stmt.charAt(j) == '\'') {
                        if (j + 1 < stmt.length() && stmt.charAt(j + 1) == '\'') {
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                i = j + 1;
                continue;
            }
            if (c == '-' && i + 1 < stmt.length() && stmt.charAt(i + 1) == '-') {
                int nl = stmt.indexOf('\n', i);
                i = nl < 0 ? stmt.length() : nl + 1;
                continue;
            }
            if (c == '/' && i + 1 < stmt.length() && stmt.charAt(i + 1) == '*') {
                int close = stmt.indexOf("*/", i + 2);
                i = close < 0 ? stmt.length() : close + 2;
                continue;
            }
            if ((c == 'V' || c == 'v')
                    && i + 6 <= stmt.length()
                    && stmt.regionMatches(true, i, "VALUES", 0, 6)
                    && estBordeMot(stmt, i, i + 6)) {
                return i + 6;
            }
            i++;
        }
        return -1;
    }

    /** Bornes de mot SQL : ni lettre, ni chiffre, ni soulignement de part et d'autre. */
    private static boolean estBordeMot(String s, int avant, int apres) {
        boolean gaucheOK = avant == 0
                || !(Character.isLetterOrDigit(s.charAt(avant - 1)) || s.charAt(avant - 1) == '_');
        boolean droiteOK = apres >= s.length()
                || !(Character.isLetterOrDigit(s.charAt(apres)) || s.charAt(apres) == '_');
        return gaucheOK && droiteOK;
    }

    // ==================== tests ====================

    @Test
    @DisplayName("toute migration V≥135 avec INSERT INTO permissions…VALUES est tagguée `catalog`")
    void catalogueEstComplet() throws IOException {
        Set<String> declares = new LinkedHashSet<>();
        for (var e : manifestTags().entrySet()) {
            if (e.getValue().contains(PermissionCatalogSource.TAG_CATALOG)) declares.add(e.getKey());
        }
        Set<String> oublis = nouveauxFichiersNonDeclares(meriteCatalogFn(), declares);
        assertThat(oublis)
                .as("migrations qui insèrent des clés `permissions` sans être déclarées `catalog` "
                        + "dans le manifeste — le contrat servi serait incomplet et les gardes "
                        + "@authz.can correspondantes mourraient en 403 silencieux (fail-closed). "
                        + "Ajouter la ligne dans `backend/src/test/resources/parametrisation/catalog-migrations.txt`.")
                .isEmpty();
    }

    @Test
    @DisplayName("toute migration V≥135 avec paire ('RÔLE','CLÉ') en VALUES est tagguée `matrix`")
    void matriceEstComplete() throws IOException {
        Set<String> declares = new LinkedHashSet<>();
        for (var e : manifestTags().entrySet()) {
            if (e.getValue().contains(PermissionCatalogSource.TAG_MATRIX)) declares.add(e.getKey());
        }
        Set<String> oublis = nouveauxFichiersNonDeclares(meriteMatrixFn(), declares);
        assertThat(oublis)
                .as("migrations dont le `role_permissions` en clause VALUES contient des paires "
                        + "littérales non déclarées `matrix` — le seed H2 (import.sql) serait en "
                        + "retard de ces accords ; un test @WebMvcTest passerait au vert sur une "
                        + "clé que le serveur refuse, ou l'inverse.")
                .isEmpty();
    }

    /** Itère src/main/resources/db/migration en V≥135, retourne les fichiers où predicate==true et non déclarés. */
    private static Set<String> nouveauxFichiersNonDeclares(java.util.function.Predicate<String> predicate,
                                                             Set<String> declares) throws IOException {
        Set<String> oublis = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(migrationsDir())) {
            List<Path> tries = files.filter(p -> p.getFileName().toString().endsWith(".sql")).toList();
            for (Path f : tries) {
                String name = f.getFileName().toString();
                Matcher m = VERSION.matcher(name);
                if (!m.matches()) continue;
                if (Integer.parseInt(m.group(1)) < 135) continue;
                String sql = Files.readString(f, StandardCharsets.UTF_8);
                if (predicate.test(sql) && !declares.contains(name)) {
                    oublis.add(name);
                }
            }
        }
        return oublis;
    }

    private static java.util.function.Predicate<String> meriteCatalogFn() {
        return PermissionCatalogSourceTest::meriteCatalog;
    }

    private static java.util.function.Predicate<String> meriteMatrixFn() {
        return PermissionCatalogSourceTest::meriteMatrix;
    }

    @Test
    @DisplayName("aucune ligne du manifeste ne désigne un fichier absent du dépôt")
    void manifesteNeMentPas() throws IOException {
        Set<String> presents = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(migrationsDir())) {
            files.forEach(p -> presents.add(p.getFileName().toString()));
        }
        Set<String> absents = new LinkedHashSet<>();
        for (String name : manifestTags().keySet()) {
            if (!presents.contains(name)) absents.add(name);
        }
        assertThat(absents)
                .as("fichier déclaré au manifeste mais absent de src/main/resources/db/migration/ "
                        + "— soit une suppression non documentée, soit une faute de frappe ; "
                        + "la doctrine « ne rien supprimer » impose de rétablir le fichier ou "
                        + "de retirer explicitement la ligne du manifeste avec un message de commit.")
                .isEmpty();
    }

    @Test
    @DisplayName("V135/V136 restent hors `matrix` : leurs SELECT…IN(...) ne sont pas des paires")
    void v135EtV136NeSontPasMatrix() throws IOException {
        // Contrôle négatif du tri `VALUES` vs `IN (…)` : la clause IN(…) de V135
        // (« p.key IN ('MEMBER_READ','REPORT_CREATE') », deux adjacentes) pourrait
        // passer pour une paire sous une regex naïve. Ce test fige la décision du
        // manifeste et prouve que la distinction est porteuse.
        for (String nom : List.of("V135__create_multi_tenant_core_tables.sql",
                "V136__add_membership_scope_and_role_fk.sql")) {
            String sql = Files.readString(migrationsDir().resolve(nom), StandardCharsets.UTF_8);
            // Sanity-check : le texte SQL contient bien des tuples de 2 clés…
            long deuxClesIn = PermissionCatalogSource.insertStatements(sql, "role_permissions").stream()
                    .flatMap(stmt -> PermissionCatalogSource.tupleContents(stmt).stream())
                    .map(PermissionCatalogSource::stringTokens)
                    .filter(l -> l.size() == 2
                            && PermissionCatalogSource.isKey(l.get(0))
                            && PermissionCatalogSource.isKey(l.get(1)))
                    .count();
            // …et pourtant le filtre VALUES les exclut toutes.
            assertThat(meriteMatrix(sql))
                    .as("%s contient %d tuples à 2 littéraux-clés (dans des IN), "
                            + "mais aucun dans une clause VALUES — donc pas de tag matrix",
                            nom, deuxClesIn)
                    .isFalse();
            assertThat(manifestTags().getOrDefault(nom, Set.of()))
                    .as("V135/V136 portent le tag catalog (ils insèrent bien des permissions)")
                    .contains(PermissionCatalogSource.TAG_CATALOG);
        }
    }

    @Test
    @DisplayName("l'analyseur lit un libellé à apostrophe échappée ('' ) sans perdre la clé")
    void analyseurEstEscapeAware() {
        // Contrôle négatif central : un libellé « L''église » cassait les
        // regex naïves `'([^']+)'` des anciens parseurs — la clé disparaissait
        // silencieusement du seed H2, donc des tests au vert sur un contrat
        // inexistant (le défaut consigné dans V270, §4.1 du handover).
        String sql = "INSERT INTO permissions (key, label, category) VALUES "
                + "('UI_L_EGLISE_ACCESS', 'Accès à L''église', 'UI'), "
                + "('UI_AUTRE', 'Ordinaire', 'UI');";
        Set<String> cles = new LinkedHashSet<>();
        for (String stmt : PermissionCatalogSource.insertStatements(sql, "permissions")) {
            cles.addAll(PermissionCatalogSource.firstKeyOfEachTuple(stmt));
        }
        assertThat(cles).containsExactlyInAnyOrder("UI_L_EGLISE_ACCESS", "UI_AUTRE");
    }

    @Test
    @DisplayName("les commentaires -- et /* */ sont ignorés dans la découpe des instructions")
    void analyseurIgnoreLesCommentaires() {
        // Un « INSERT INTO permissions (fantôme) … ; » en commentaire ne doit
        // PAS être reconnu comme instruction — sinon la moindre ligne
        // d'exemple dans une note de version fausse le catalogue.
        String sql = "-- INSERT INTO permissions (fantome) VALUES ('A','B');\n"
                + "INSERT INTO permissions (key, label) VALUES ('UI_X', 'Label');\n"
                + "/* INSERT INTO permissions (fantome2) VALUES ('C','D'); */\n";
        List<String> stmts = PermissionCatalogSource.insertStatements(sql, "permissions");
        Set<String> cles = new LinkedHashSet<>();
        for (String s : stmts) cles.addAll(PermissionCatalogSource.firstKeyOfEachTuple(s));
        assertThat(cles).as("une seule instruction réelle, les deux en commentaires sont ignorées")
                .containsExactly("UI_X");
    }

    @Test
    @DisplayName("findValuesMotCle distingue le mot-clé SQL d'un libellé contenant « VALUES »")
    void valuesNeSeConfondPasAvecUnLibelle() {
        // Un libellé « Les VALUES sont… » ou un nom de colonne « VALUES_OK »
        // ne déclenche PAS le détecteur. Sans ce contrôle, un tuple de
        // 2 clés apparaissant dans un commentaire de libellé serait compté
        // comme une paire.
        assertThat(findValuesMotCle("INSERT INTO t (a) VALUES (1,2)"))
                .as("mot-clé SQL suivi d'espace puis parenthèse")
                .isPositive();
        assertThat(findValuesMotCle("INSERT INTO t (a) -- note : VALUES historiques\n (1,2)"))
                .as("« VALUES » en commentaire → non détecté")
                .isEqualTo(-1);
        assertThat(findValuesMotCle("INSERT INTO t (a) VALUES_OK (1,2)"))
                .as("« VALUES_OK » comme identifiant → non détecté")
                .isEqualTo(-1);
        assertThat(findValuesMotCle("INSERT INTO t (a) 'un libellé VALUES';"))
                .as("« VALUES » dans un littéral → non détecté")
                .isEqualTo(-1);
    }

    @Test
    @DisplayName("matrixPairs couvre les 7 migrations tagguées, pas seulement les 4 historiques")
    void matriceCouvreToutLeManifeste() {
        // Le fixture `TestPermissionMatrix` lisait 4 fichiers (V258..V261) quand
        // le seed H2 en joue 7 (V258..V271) — exactement la dérive que le
        // manifeste unique doit faire disparaître. Ce test verrouille
        // l'ordre de grandeur observé : 445 clés de permission servies au total
        // (toutes les 7 migrations), contre environ 380 sous l'ancien
        // périmêtre 4 fichiers. Le seuil de 400 est donc volontairement au-dessus
        // de l’ancien plateau et largement sous le plateau actuel — il
        // rougirait si des fichiers taggués matrix étaient silencieusement retirés.
        Map<String, Set<String>> m = PermissionCatalogSource.matrix();
        assertThat(m).as("catalogue de la matrice (clés ayant ≥1 rôle global)").isNotEmpty();
        assertThat(m.size())
                .as("matrice servie sur les 7 migrations V258..V271 (~450 clés), "
                        + "pas les 4 anciennes (~380 clés — dérive historique)")
                .isGreaterThan(400);
    }
}

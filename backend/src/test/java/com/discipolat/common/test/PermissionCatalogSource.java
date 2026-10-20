package com.discipolat.common.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SOURCE UNIQUE de lecture du contrat serveur (catalogue de permissions + matrice
 * rôle→permission), lu dans les migrations Flyway elles-mêmes.
 *
 * <p><strong>Pourquoi cette classe existe.</strong> Cinq listes de migrations et
 * trois analyseurs différents coexistaient ({@code PermissionKeyCatalogGuardTest},
 * {@code TestPermissionMatrixSeedGuardTest}, {@code TestPermissionMatrix},
 * {@code tools/parametrisation/gen_h2_seed.py},
 * {@code frontend/src/__tests__/helpers/servedCatalog.ts}) — déjà désynchronisés :
 * 9 fichiers, 7, 4 selon l'endroit. Et trois d'entre eux extrayaient les tuples par
 * regex naïve : {@code '([^']+)}' pour un libellé, {@code sql.indexOf(';')} pour une
 * fin d'instruction. Un libellé contenant une apostrophe SQL échappée
 * ({@code 'L''église'}) y cassait la reconnaissance, et la clé disparaissait
 * <em>silencieusement</em> du seed H2 — donc des tests d'intégration au vert sur des
 * gardes {@code @authz.can} qui n'existaient plus. La contrainte avait fini par
 * s'écrire dans la migration elle-même (V270 : « les libellés de ce bloc évitent
 * l'apostrophe ») : une règle de <em>contenu</em> dictée par une limite
 * d'<em>outillage</em>, exactement ce qu'il ne fallait pas laisser s'installer.
 *
 * <p><strong>Ce que fait cet analyseur.</strong> Il marche au caractère, en
 * tenant compte des littéraux {@code ''}, des commentaires de ligne et de bloc,
 * et des parenthèses imbriquées — {@code V135} écrit
 * {@code (uuid_generate_v4(), NULL, 'MEMBER_READ', …)}, où la clé est la troisième
 * colonne : une regex collée à la parenthèse la manque. Il ne gère <em>pas</em> le
 * quoting dollar ni les escapes backslash : aucun des fichiers du manifeste n'en
 * contient, et {@code PermissionCatalogSourceTest} rougit si un tel marquage
 * apparaît dans une instruction inspectée, plutôt que de lire faux en silence.
 *
 * <p>Le manifeste {@code parametrisation/catalog-migrations.txt} (ressource de test)
 * est la seule liste faisant autorité ; les taggueurs {@code catalog} et {@code matrix}
 * y sont explicites parce que {@code V135}/{@code V136} contiennent des
 * {@code p.key IN ('A','B')} qui ressemblent à des paires sans en être : les compter
 * comme matrice inventerait des droits.
 */
public final class PermissionCatalogSource {

    /** Tag « ce fichier insère des lignes dans `permissions`/`roles` ». */
    public static final String TAG_CATALOG = "catalog";
    /** Tag « ce fichier insère des paires littérales ('RÔLE','CLÉ') dans `role_permissions` ». */
    public static final String TAG_MATRIX = "matrix";

    private static final String MANIFEST_RESOURCE = "parametrisation/catalog-migrations.txt";
    private static final String MIGRATION_PREFIX = "db/migration/";
    private static final Pattern KEY_TOKEN = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern VERSION_PREFIX = Pattern.compile("^V(\\d+)__.+\\.sql$");

    public record Migration(String file, Set<String> tags) {
        public boolean tagged(String tag) {
            return tags.contains(tag);
        }

        /** Chemin de ressource classpath complet. */
        public String resource() {
            return MIGRATION_PREFIX + file;
        }
    }

    private static volatile List<Migration> manifest;

    private PermissionCatalogSource() {
    }

    // ==================== manifeste ====================

    public static List<Migration> manifest() {
        List<Migration> local = manifest;
        if (local == null) {
            local = readManifest();
            manifest = local;
        }
        return local;
    }

    private static List<Migration> readManifest() {
        List<Migration> parsed = new ArrayList<>();
        for (String line : read(MANIFEST_RESOURCE).split("\n")) {
            String entry = line.trim();
            if (entry.isEmpty() || entry.startsWith("#")) {
                continue;
            }
            List<String> tokens = Arrays.asList(entry.split("\\s+"));
            String file = tokens.get(0);
            Set<String> tags = new LinkedHashSet<>(tokens.subList(1, tokens.size()));
            if (tags.isEmpty()) {
                throw new IllegalStateException("migration sans tag dans " + MANIFEST_RESOURCE + " : " + file);
            }
            if (!VERSION_PREFIX.matcher(file).matches()) {
                throw new IllegalStateException("nom de migration invalide dans le manifeste : " + file);
            }
            parsed.add(new Migration(file, Collections.unmodifiableSet(tags)));
        }
        if (parsed.isEmpty()) {
            throw new IllegalStateException("manifeste vide : " + MANIFEST_RESOURCE);
        }
        return Collections.unmodifiableList(parsed);
    }

    /** Fichiers déclarés pour un tag donné. */
    public static List<Migration> migrations(String tag) {
        return manifest().stream().filter(m -> m.tagged(tag)).toList();
    }

    /** Numéro de version extrait du nom de fichier ({@code V258__…} → 258). */
    public static int version(String file) {
        var m = VERSION_PREFIX.matcher(file);
        if (!m.matches()) {
            throw new IllegalArgumentException("pas une migration versionnée : " + file);
        }
        return Integer.parseInt(m.group(1));
    }

    // ==================== contenu des migrations ====================

    /** Texte complet d'une migration, lu depuis le classpath de test. */
    public static String sql(Migration migration) {
        return read(migration.resource());
    }

    /** Clés du catalogue {@code permissions}, sur tous les fichiers taggués {@code catalog}. */
    public static Set<String> catalogKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Migration migration : migrations(TAG_CATALOG)) {
            String sql = sql(migration);
            for (String statement : insertStatements(sql, "permissions")) {
                keys.addAll(firstKeyOfEachTuple(statement));
            }
        }
        return keys;
    }

    /**
     * Matrice par défaut : {@code clé permission → rôles globaux qui la détiennent},
     * sur les fichiers taggués {@code matrix}. Seuls les tuples composés
     * <em>exactement</em> de deux littéraux sont retenus — {@code ('MEMBER_READ',
     * 'FAMILY_READ')} issu d'un {@code IN (…)} n'est pas une paire.
     */
    public static Map<String, Set<String>> matrix() {
        Map<String, Set<String>> matrix = new LinkedHashMap<>();
        for (String[] pair : matrixPairs()) {
            matrix.computeIfAbsent(pair[1], k -> new LinkedHashSet<>()).add(pair[0]);
        }
        return matrix;
    }

    /** Paires {@code [rôle, clé]} littérales des fichiers taggués {@code matrix}. */
    public static List<String[]> matrixPairs() {
        List<String[]> pairs = new ArrayList<>();
        for (Migration migration : migrations(TAG_MATRIX)) {
            for (String statement : insertStatements(sql(migration), "role_permissions")) {
                for (String tuple : tupleContents(statement)) {
                    List<String> literals = stringTokens(tuple);
                    if (literals.size() == 2
                            && isKey(literals.get(0)) && isKey(literals.get(1))) {
                        pairs.add(new String[]{literals.get(0), literals.get(1)});
                    }
                }
            }
        }
        return pairs;
    }

    /**
     * Clés des rôles créés par les migrations du manifeste ({@code INSERT INTO roles}),
     * telle que la comprend {@code TestPermissionMatrixSeedGuardTest} : tuple de
     * démarration {@code ('CLÉ', 'Libellé', priorité)} — premier littéral, deuxième
     * littéral ignoré (il peut contenir apostrophes et point-virgules), troisième
     * champ numérique.
     */
    public static Set<String> createdRoleKeys() {
        Set<String> roles = new LinkedHashSet<>();
        for (Migration migration : manifest()) {
            for (String statement : insertStatements(sql(migration), "roles")) {
                for (String tuple : tupleContents(statement)) {
                    List<String> literals = stringTokens(tuple);
                    if (literals.size() >= 2 && isKey(literals.get(0))) {
                        roles.add(literals.get(0));
                    }
                }
            }
        }
        return roles;
    }

    // ==================== primitives d'analyse ====================

    /**
     * Instructions {@code INSERT INTO <table> … ;} d'un script, coupées aux
     * point-virgules <em>hors</em> littéral et hors commentaire.
     *
     * <p>La comparaison se fait sur une copie normalisée (sauts de ligne en espaces,
     * minuscules) : {@code INSERT INTO\n    role_permissions} est une écriture
     * fréquente des migrations et litterait une recherche mot-à-mot.
     */
    public static List<String> insertStatements(String sql, String table) {
        String marker = "insert into " + table.toLowerCase(java.util.Locale.ROOT) + " ";
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            i = skipIgnored(sql, i);
            if (i >= sql.length()) {
                break;
            }
            int end = nextStatementEnd(sql, i);
            String statement = sql.substring(i, end);
            String normalise = statement.replace('\n', ' ').replace('\r', ' ')
                    .toLowerCase(java.util.Locale.ROOT);
            int at = normalise.indexOf(marker);
            if (at >= 0 && estDeboutTable(normalise, at + marker.length(), table)) {
                out.add(statement);
            }
            i = end + 1;
        }
        return out;
    }

    /** Le mot qui suit {@code insert into <table>} est bien une borne de mot. */
    private static boolean estDeboutTable(String normalise, int apresTable, String table) {
        if (apresTable >= normalise.length()) {
            return true;
        }
        char suivant = normalise.charAt(apresTable);
        return !(Character.isLetterOrDigit(suivant) || suivant == '_');
    }

    /** Position du point-virgule fermant l'instruction commencée à {@code from} (fin de script si absent). */
    private static int nextStatementEnd(String sql, int from) {
        int i = from;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = endOfString(sql, i);
                continue;
            }
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                i = endOfLine(sql, i);
                continue;
            }
            if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                i = endOfBlockComment(sql, i);
                continue;
            }
            if (c == ';') {
                return i;
            }
            i++;
        }
        return sql.length();
    }

    /** Avance {@code from} au-delà des blancs, commentaires et instructions vides. */
    private static int skipIgnored(String sql, int from) {
        int i = from;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                i = endOfLine(sql, i);
            } else if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                i = endOfBlockComment(sql, i);
            } else {
                return i;
            }
        }
        return i;
    }

    private static int endOfLine(String sql, int from) {
        int nl = sql.indexOf('\n', from);
        return nl < 0 ? sql.length() : nl + 1;
    }

    private static int endOfBlockComment(String sql, int from) {
        int close = sql.indexOf("*/", from + 2);
        return close < 0 ? sql.length() : close + 2;
    }

    /** Fin d'un littéral {@code '…'}, apostrophes doublées ({@code ''}) comprises. */
    private static int endOfString(String sql, int openQuote) {
        int i = openQuote + 1;
        while (i < sql.length()) {
            if (sql.charAt(i) == '\'') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return sql.length(); // littéral non fermé : laisse le garde le constater
    }

    /** Contenus des parenthèses d'un fragment, de la plus externe à la plus interne. */
    public static List<String> tupleContents(String fragment) {
        List<String> out = new ArrayList<>();
        List<Integer> stack = new ArrayList<>();
        int i = 0;
        while (i < fragment.length()) {
            char c = fragment.charAt(i);
            if (c == '\'') {
                i = endOfString(fragment, i);
                continue;
            }
            if (c == '-' && i + 1 < fragment.length() && fragment.charAt(i + 1) == '-') {
                i = endOfLine(fragment, i);
                continue;
            }
            if (c == '(') {
                stack.add(i);
            } else if (c == ')' && !stack.isEmpty()) {
                int open = stack.remove(stack.size() - 1);
                out.add(fragment.substring(open + 1, i));
            }
            i++;
        }
        return out;
    }

    /** Littéraux d'un tuple, décodés ({@code ''} → {@code '}), blancs de bordure retirés. */
    public static List<String> stringTokens(String tuple) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < tuple.length()) {
            if (tuple.charAt(i) == '"') {
                int close = tuple.indexOf('"', i + 1);
                i = close < 0 ? tuple.length() : close + 1;
                continue;
            }
            if (tuple.charAt(i) == '\'') {
                StringBuilder value = new StringBuilder();
                int j = i + 1;
                while (j < tuple.length()) {
                    char c = tuple.charAt(j);
                    if (c == '\'') {
                        if (j + 1 < tuple.length() && tuple.charAt(j + 1) == '\'') {
                            value.append('\'');
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    value.append(c);
                    j++;
                }
                out.add(value.toString().trim());
                i = j + 1;
                continue;
            }
            i++;
        }
        return out;
    }

    /**
     * Première clé de chaque tuple d'une instruction : le <b>premier</b> littéral du
     * tuple, retenu seulement s'il a la forme d'une clé. Couvre
     * {@code ('UI_X_ACCESS', 'Libellé', 'UI')} comme
     * {@code (uuid_generate_v4(), NULL, 'MEMBER_READ', …)} — où le premier littéral
     * <em>est</em> la clé, les deux premiers champs n'étant pas des littéraux.
     *
     * <p>Volontairement strict : ne pas chercher plus loin dans le tuple évite
     * qu'un {@code IN ('A','B')} croisé dans une sous-requête gonfle le catalogue
     * et masque la vraie clé manquante.
     */
    public static Set<String> firstKeyOfEachTuple(String statement) {
        Set<String> out = new LinkedHashSet<>();
        for (String tuple : tupleContents(statement)) {
            List<String> literals = stringTokens(tuple);
            if (!literals.isEmpty() && isKey(literals.get(0))) {
                out.add(literals.get(0));
            }
        }
        return out;
    }

    public static boolean isKey(String candidate) {
        return candidate != null && KEY_TOKEN.matcher(candidate).matches();
    }

    private static String read(String resource) {
        try (InputStream in = PermissionCatalogSource.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("ressource de test absente : " + resource
                        + " — le manifeste des migrations doit vivre sous src/test/resources");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

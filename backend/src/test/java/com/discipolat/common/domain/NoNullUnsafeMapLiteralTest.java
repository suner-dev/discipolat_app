package com.discipolat.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou systémique contre la réintroduction du défaut {@code Map.of}.
 *
 * <h2>Le défaut</h2>
 *
 * <p>{@code Map.of(...)} lève une {@link NullPointerException} si une seule
 * valeur est nulle. Dans ce code base, les charges utiles d'audit et d'évènement
 * transportent régulièrement des valeurs legitimately absentes. Résultat mesuré :
 * des HTTP 500, et — la plupart de ces appels étant dans un
 * {@code @Transactional} — des <b>rollbacks</b>, donc des écritures perdues.
 * Trois occurrences ont été trouvées et corrigées de cette façon ; un audit
 * complet en a révélé une trentaine de la même famille.
 *
 * <h2>Pourquoi un test plutôt que trente correctifs</h2>
 *
 * <p>Corriger les sites un par un ne suffit pas : le motif est idiomatic dans ce
 * code base, il reviendra. Ce test le rend <b>impossible à réintroduire</b>
 * silencieusement, et il fait office de critère d'acceptation du chantier : tant
 * qu'il échoue, la famille de défauts n'est pas fermée.
 *
 * <h2>Les deux règles</h2>
 *
 * <ol>
 *   <li><b>Règle 1, déterministe.</b> Un ternaire produisant {@code null} dans un
 *       {@code Map.of(...)} est une NPE <em>garantie</em> : la branche nulle est
 *       triviellement atteignable. Détection lexicale, sans faux positif possible.</li>
 *   <li><b>Règle 2, par position.</b> Un {@code Map.of(...)} sur une ligne
 *       construisant une charge utile d'audit ou d'évènement doit passer par
 *       {@link Payloads}. C'est la position qui rend le payload fragile.</li>
 * </ol>
 */
class NoNullUnsafeMapLiteralTest {

    private static final Path SOURCE_ROOT = Paths.get("src/main/java");

    /**
     * Crémaillère de la règle 2 : nombre maximal de sites encore en position
     * d'audit, mesuré le 2026-09-28 après la première vague de correction.
     *
     * <p>La règle 2 est <b>heuristique</b> (elle repose sur la position de
     * l'appel, pas sur une analyse de nullabilité), et le relevé mesuré par ce test s'élève à
     * 120 sites répartis sur une soixanteaine de fichiers. Il serait malhonnête de la
     * transformer en porte bloquante : le test échouerait alors pour un
     * reliquat que personne n'a encore traité, et l'équipe s'habituerait à
     * l'ignorer — c'est-à-dire à perdre tout son pouvoir de signal.
     *
     * <p>Ce nombre est donc un <b>plafond</b> : il ne peut que décroître. Chaque vague de correction le fait baisser, et le test
     * affiche en cas d'échec la liste complète des sites restants, donc le
     * reliquat est toujours visible et toujours actionnable.
     *
     * <p>La règle 1, elle, reste une porte <b>stricte</b> : elle ne détecte que
     * des NPE garanties, sans aucun faux positif possible.
     */
    private static final int AUDIT_POSITION_BACKLOG_CEILING = 122;

    /** Appels qui construisent une charge utile transmise à un service d'audit ou d'évènement. */
    private static final List<String> AUDIT_POSITIONS = List.of(
            "auditService.log(",
            "auditService.logSimple(",
            "publishCreated(",
            "publishReassigned(",
            "publishUpdated(",
            "publishDeleted(");

    @Test
    @DisplayName("Règle 1 : aucun Map.of ne contient un ternaire produisant null")
    void noMapOfWithNullTernary() throws IOException {
        List<String> offenders = new ArrayList<>();
        forEachJavaSource((path, relative, content) -> {
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (isComment(line) || !line.contains("Map.of(")) {
                    continue;
                }
                if (line.contains(": null") || line.contains(":null")) {
                    offenders.add(relative + ":" + (i + 1) + "  " + line.strip());
                }
            }
        });

        assertThat(offenders)
                .as("""
                        Map.of() avec un ternaire produisant null : NPE garantie, donc 500 \
                        et souvent rollback. A remplacer par Payloads.of(...).""")
                .isEmpty();
    }

    @Test
    @DisplayName("Règle 2 : les charges utiles d'audit utilisent Payloads, pas Map.of")
    void auditPayloadsUsePayloads() throws IOException {
        List<String> offenders = new ArrayList<>();
        forEachJavaSource((path, relative, content) -> {
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (isComment(lines[i])) {
                    continue;
                }
                if (!AUDIT_POSITIONS.stream().anyMatch(lines[i]::contains)) {
                    continue;
                }
                // La charge utile peut être écrite sur les lignes suivantes de
                // l'appel : on balaie l'instruction entière, sinon les appels
                // multi-lignes échappent invisibles au garde-fou.
                int end = Math.min(lines.length, i + STATEMENT_WINDOW_LINES);
                for (int j = i; j < end; j++) {
                    if (isComment(lines[j])) {
                        continue;
                    }
                    if (lines[j].contains("Map.of(")) {
                        offenders.add(relative + ":" + (j + 1) + "  " + lines[j].strip());
                        break;
                    }
                }
            }
        });

        assertThat(offenders.size())
                .as("""
                        Crémaillère : %d sites en position d'audit utilisent encore Map.of \
                        (plafond : %d). Le plafond ne peut que baisser. Sites restants :
                        %s""",
                        offenders.size(), AUDIT_POSITION_BACKLOG_CEILING, offenders)
                .isLessThanOrEqualTo(AUDIT_POSITION_BACKLOG_CEILING);
    }

    @Test
    @DisplayName("Payloads refuse un nombre impair d'arguments plutôt que de tronquer")
    void payloadsRejectsDanglingKey() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Payloads.of("cle", "valeur", "cleSansValeur")))
                .hasMessageContaining("paires clé/valeur");
    }

    @Test
    @DisplayName("Payloads conserve l'ordre des clés et tolère le null")
    void payloadsKeepsOrderAndAcceptsNull() {
        var payload = Payloads.of("b", null, "a", 1);
        assertThat(payload.keySet()).containsExactly("b", "a");
        assertThat(payload).containsEntry("b", null).containsEntry("a", 1);
    }

    /** Une charge utile s'écrit rarement sur une seule ligne : on regarde l'instruction entière. */
    private static final int STATEMENT_WINDOW_LINES = 10;

    private static boolean isComment(String line) {
        String stripped = line.strip();
        return stripped.startsWith("//") || stripped.startsWith("*") || stripped.startsWith("/*");
    }

    private void forEachJavaSource(SourceVisitor visitor) throws IOException {
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return; // exécuté hors du module backend : rien à vérifier
        }
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                // Ce fichier contient lui-même des exemples `Map.of` dans sa javadoc.
                if (path.getFileName().toString().equals("NoNullUnsafeMapLiteralTest.java")
                        || path.getFileName().toString().equals("Payloads.java")) {
                    continue;
                }
                visitor.visit(path, SOURCE_ROOT.relativize(path).toString(),
                        Files.readString(path, StandardCharsets.UTF_8));
            }
        }
    }

    @FunctionalInterface
    private interface SourceVisitor {
        void visit(Path path, String relativePath, String content);
    }
}

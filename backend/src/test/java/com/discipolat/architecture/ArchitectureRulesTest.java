package com.discipolat.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * V0.1 (ADR-002, {@code docs/architecture/backend-target-architecture.md} §4) — les règles
 * R1..R6 de la clean architecture, <strong>exécutées par la machine</strong> à chaque
 * {@code mvn verify}.
 *
 * <p><strong>Pourquoi ce test existe.</strong> Une règle non exécutée n'est pas une règle : un
 * document d'architecture que rien ne vérifie se délite en quelques trimestres. Un test qui
 * rougit sur la déviation la documente au contraire pour l'équipe suivante — et pour un auditeur
 * en due-diligence, qui peut lire ce fichier et savoir exactement ce qui est permis.
 *
 * <p><strong>Mode avertissement (règle A7 « dark launch »).</strong> Le backend d'aujourd'hui
 * n'est pas en clean architecture, et ce n'est pas l'objet de V0 : l'état constaté est donc
 * <em>gelé</em> dans {@code architecture-freeze.txt}. Ce fichier est un <strong>plafond</strong>,
 * pas un blanc-seing. Concrètement on peut ajouter du code conforme, on ne peut pas en ajouter
 * d'impur, et chaque PR de la migration V1 fait baisser le compteur.
 *
 * <p><strong>Deux granularités, parce qu'un ratchet de 11 875 lignes est inutilisable.</strong>
 * Mesure faite sur ce dépôt : 2 253 classes importées, R1 = 8 017 violations, R4 = 2 948 — les
 * règles de <em>pureté</em> du domaine gèlent presque tout, et les énumérer ne dit rien de plus
 * que « le domaine n'est pas pur », déjà su. Elles sont donc gelées <strong>par compte</strong>
 * ({@code plafond|R1|8017}) : la dette ne monte pas, et le détail complet est publié dans le
 * rapport CI. Les règles de <em>frontière</em> (R3 entre-contextes, R5 transport→persistance,
 * R6 cycles), elles, sont gelées <strong>par objet</strong> : une arête nouvelle entre deux
 * contexts, une fuite nouvelle d'un contrôleur vers un repository, ou un cycle nouveau font
 * rougir — alors que 354 arêtes, 15 fuites et 100 cycles tiennent en revue de code. La liste
 * d'arêtes R3 est d'ailleurs la carte des blocages d'extraction : {@code platform -> tenants}
 * compte à lui seul 65 accès aux internes, ce qui explique pourquoi aucun contexte ne sort
 * aujourd'hui sans casse.
 *
 * <p><strong>Regénérer le gel</strong> (constat initial, ou après un nettoyage volontaire) :
 * <pre>
 *   mvn -o test -Dtest=ArchitectureRulesTest -Darchitecture.freeze.update=true
 * </pre>
 *
 * <p>Volontairement <strong>aucun</strong> {@code @SpringBootTest} : ArchUnit travaille sur les
 * octets de {@code target/classes}, le contexte Spring n'est pas démarré. Le test coûte dix
 * secondes et ne peut pas rougir à cause de l'état d'une base.
 */
@DisplayName("Règles d'architecture R1..R6 (ArchUnit, plafond gelé)")
class ArchitectureRulesTest {

    /** Gel de la dette historique : plafond par compte, liste par objet. */
    private static final Path FICHIER_GEL = Paths.get(System.getProperty(
            "architecture.freeze.file", "src/test/resources/architecture/architecture-freeze.txt"));

    /** Rapport machine complet, publié en artifact CI — détaillé mais jamais bloquant. */
    private static final Path RAPPORT = Paths.get("target/architecture-report.txt");

    /**
     * Les descriptions ArchUnit portent la localisation du code fautive
     * ({@code in (DepartmentService.java:87)}). On la retire : une dette ne change pas de nature
     * parce qu'une ligne voisine a été ajoutée, et le diff du gel doit rester lisible.
     */
    private static final Pattern LOCALISATION = Pattern.compile(" ?\\((?:[A-Za-z0-9_]+\\.java(?::\\d+)?)(?:, \\d+)?\\)");

    /** Les blocs de cycle d'ArchUnit sont multi-lignes : tout est aplati sur une ligne. */
    private static final Pattern BLANCS = Pattern.compile("\\s+");

    /**
     * Clé R6 : la paire de contexts en couplage réciproque, telle qu'émise dans le message
     * (toujours triée par {@code compareTo}, donc indépendante de l'ordre d'itération du graphe).
     */
    private static final Pattern PAIRE_R6 = Pattern.compile("Couplage reciproque entre contexte '(\\w+)' et '(\\w+)'");

    private static final boolean METTRE_A_JOUR_LE_GEL = Boolean.getBoolean("architecture.freeze.update");

    /** Racine des contexts délimités : {@code com.discipolat.modules.<context>.<couche>}. */
    private static final String RACINE_CONTEXTS = "com.discipolat.modules.";

    /**
     * Les <em>internes</em> d'un contexte : entités et repositories. La couche {@code api} est
     * réputée interface publiée du contexte — c'est par elle (ou par un événement) que passe le
     * couplage licite, conformément à l'ADR-002.
     */
    private static final Set<String> COUCHES_INTERNES = Set.of("domain", "repository");

    private static final JavaClasses CLASSES_PRODUCTION = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.discipolat");

    /** Plafond numérique (dette massive déjà constatée) ou liste d'objets (frontière fine). */
    private enum Granularite { PLAFOND, OBJETS }

    /**
     * Une règle, sa granularité de gel, et — pour les règles à objets — la fonction qui dérive
     * une clé compacte et stable de la description ArchUnit. Une clé nulle jette un erreur :
     * cela garantit qu'une évolution du format de message d'ArchUnit ne passe pas inaperçue.
     */
    private record Regle(String id, String nom, Granularite granularite,
                         Function<String, String> cleObjet, ArchRule rule) {

        static Regle plafond(String id, String nom, ArchRule rule) {
            return new Regle(id, nom, Granularite.PLAFOND, null, rule);
        }

        static Regle objets(String id, String nom, Function<String, String> cleObjet, ArchRule rule) {
            return new Regle(id, nom, Granularite.OBJETS, cleObjet, rule);
        }
    }

    // ------------------------------------------------------------------ R1 · le domaine ignore l'infrastructure
    // Le cœur métier doit être testable sans contexte Spring, réutilisable, migrable.
    private static ArchRule r1DomaineSansInfrastructure() {
        return noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "com.fasterxml.jackson..",
                        "io.swagger..",
                        "org.springframework.data..",
                        "org.springframework.web..",
                        "org.springframework.boot..",
                        "org.springframework.context..",
                        "org.springframework.security..")
                .allowEmptyShould(true)
                .because("R1 — le domaine est une bibliothèque pure ; persistence, sérialisation "
                        + "et transport appartiennent aux adaptateurs");
    }

    // ------------------------------------------------------------------ R2 · application ne connaît pas les adaptateurs
    // Règle prospective : aucune couche ..application.. n'existe encore (elle arrive en V0.6,
    // pilote governance/departments). allowEmptyShould(true) la laisse verte aujourd'hui.
    private static ArchRule r2ApplicationIgnoreLesAdaptateurs() {
        return noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapters.out..",
                        "..adapters.in..",
                        "jakarta.persistence..",
                        "org.springframework.web..")
                .allowEmptyShould(true)
                .because("R2 — le use-case orchestre le domaine via des ports ; il ne connaît ni "
                        + "JPA, ni HTTP, ni un broker");
    }

    // ------------------------------------------------------------------ R3 · frontières entre contexts
    // Condition explicite plutôt que slices().notDependOnEachOther() : cette dernière interdit
    // tout couplage entre contexts et gèlerait des dizaines de milliers d'arêtes, noyant le
    // signal. On n'interdit que ce que l'ADR-002 interdit vraiment — pénétrer les INTERNES
    // (domaine, repositories) d'un autre contexte. La clé de gel est l'ARÊTE contexte→contexte.
    private static ArchRule r3FrontieresEntreContextes() {
        return classes().should(new ArchCondition<JavaClass>(
                "ne pas accéder aux internes (domain, repository) d'un autre contexte") {
            @Override
            public void check(JavaClass classe, ConditionEvents evenements) {
                String monContexte = contexteDe(classe.getName());
                if (monContexte == null) {
                    return; // hors périmètre : common, security, câblage applicatif…
                }
                for (Dependency dependance : classe.getDirectDependenciesFromSelf()) {
                    String cible = dependance.getTargetClass().getName();
                    String contexteCible = contexteDe(cible);
                    if (contexteCible == null || contexteCible.equals(monContexte)
                            || !estInterne(contexteCible, cible)) {
                        continue; // l'api publiée du contexte voisin est une frontière licite
                    }
                    evenements.add(new SimpleConditionEvent(classe, false,
                            "Contexte '" + monContexte + "' accede aux internes de '" + contexteCible
                                    + "' via " + cible));
                }
            }
        }).allowEmptyShould(true)
                .because("R3 — un contexte ne pénètre pas les structures de données d'un autre : "
                        + "le couplage passe par un port publié ou par un événement");
    }

    // ------------------------------------------------------------------ R4 · pas d'annotations de conteneur dans le domaine
    private static ArchRule r4DomaineSansAnnotationsDeConteneur() {
        return noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.stereotype..",
                        "org.springframework.transaction..")
                .allowEmptyShould(true)
                .because("R4 — @Service/@Component/@Repository/@Transactional sont des décisions "
                        + "de câblage, pas du métier ; elles vivent dans les adaptateurs");
    }

    // ------------------------------------------------------------------ R5 · le transport ne touche pas la persistance
    private static ArchRule r5ControleurNeTouchePasLaPersistence() {
        return noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..repository..",
                        "jakarta.persistence..",
                        "org.springframework.jdbc..")
                .allowEmptyShould(true)
                .because("R5 — un contrôleur traduit un protocole ; s'il pilote une transaction ou "
                        + "un dépôt, la logique métier devient inexportable");
    }

    // ------------------------------------------------------------------ R6 · aucun couplage réciproque entre contexts
    // C'est la règle la plus coûteuse à réparer tardivement, donc la plus rentable à surveiller
    // tôt : deux contexts qui se connaissent mutuellement ne peuvent plus être séparés.
    //
    // Choix assumé APRÈS mesure : on n'utilise PAS slices().beFreeOfCycles(). invocée à blanc sur
    // ce dépôt, elle énumère plus de 100 cycles de 19 tranches chacun, dans un ordre qui dépend
    // du parcours du graphe — le même code, deux exécutions, 93 « nouveaux » cycles sur 96 : un
    // ratchet non reproductible est pire que pas de ratchet du tout (il apprend aux équipes à
    // laisser un test rouge sans le lire). On gèle donc la PAIRE en couplage réciproque, qui est
    // une propriété mathématique du graphe, donc stable. Les cycles longs ne sont pas oubliés :
    // ils sont condensés dans le rapport par les composants fortement connexes, qui sont, eux
    // aussi, uniques — c'est la mesure qui dit ce que V0-B peut réellement extraire.
    private static ArchRule r6AucunCouplageReciproque() {
        return classes().should(new ArchCondition<JavaClass>(
                "ne pas dépendre réciproquement d'un autre contexte") {
                    @Override
                    public void check(JavaClass classe, ConditionEvents evenements) {
                        // propriété agrégée : tout est émis dans finish(), voir le commentaire de la règle
                    }

                    @Override
                    public void finish(ConditionEvents evenements) {
                        Map<String, Set<String>> arcs = aretesParContexte();
                        arcs.forEach((source, cibles) -> cibles.forEach(cible -> {
                            if (source.compareTo(cible) < 0 && arcs.getOrDefault(cible, Set.of()).contains(source)) {
                                evenements.add(new SimpleConditionEvent(source + " <-> " + cible, false,
                                        "Couplage reciproque entre contexte '" + source + "' et '" + cible + "'"));
                            }
                        }));
                    }
                })
                .allowEmptyShould(true)
                .because("R6 — un couplage réciproque entre deux contexts rend leur extraction "
                        + "future matériellement impossible : il faut le voir le jour où il naît");
    }

    /**
     * Graphe orienté {@code contexte -> contexts qu'il utilise}, calculé une seule fois : c'est la
     * source unique de R6 et du rapport, ce qui garantit que les deux racontent la même histoire.
     */
    private static Map<String, Set<String>> aretesParContexte() {
        if (GRAPHE_CONTEXTES == null) {
            Map<String, Set<String>> arcs = new TreeMap<>();
            for (JavaClass classe : CLASSES_PRODUCTION) {
                String source = contexteDe(classe.getName());
                if (source == null) {
                    continue;
                }
                for (Dependency dependance : classe.getDirectDependenciesFromSelf()) {
                    String cible = contexteDe(dependance.getTargetClass().getName());
                    if (cible != null && !cible.equals(source)) {
                        arcs.computeIfAbsent(source, k -> new TreeSet<>()).add(cible);
                    }
                }
            }
            GRAPHE_CONTEXTES = arcs;
        }
        return GRAPHE_CONTEXTES;
    }

    private static Map<String, Set<String>> GRAPHE_CONTEXTES;

    /**
     * Composants fortement connexes du graphe de contexts (Algorithme de Tarjan, itératif).
     * Un composant de taille &gt; 1 est un blocage d'extraction : aucun de ses membres ne peut
     * devenir un module Maven autonome sans casser les autres. Unicité mathématique : le résultat
     * ne dépend pas de l'ordre de parcours, contrairement à une énumération de cycles.
     */
    private static List<Set<String>> composantsFortementConnexes() {
        Map<String, Set<String>> arcs = aretesParContexte();
        Map<String, Integer> indice = new LinkedHashMap<>();
        Map<String, Integer> plusBas = new LinkedHashMap<>();
        Map<String, Boolean> dansLaPile = new LinkedHashMap<>();
        List<String> pile = new ArrayList<>();
        List<Set<String>> compositeurs = new ArrayList<>();
        int[] compteur = {0};
        // Tous les sommets : les contexts qui sortent ET ceux qui ne reçoivent que (les feuilles
        // entrantes n'ont pas d'arc sortant et n'apparaissent donc pas dans les clés du graphe).
        Set<String> sommets = new TreeSet<>(arcs.keySet());
        arcs.values().forEach(sommets::addAll);
        sommets.forEach(contexte -> {
            indice.put(contexte, -1);
            plusBas.put(contexte, -1);
            dansLaPile.put(contexte, false);
        });

        for (String racine : sommets) {
            if (indice.get(racine) != -1) {
                continue;
            }
            // Tarjan non récursif : la pile d'appels est explicite pour ne dépendre de la taille du graphe
            Deque<String[]> parcours = new ArrayDeque<>();
            parcours.push(new String[]{racine, null});
            while (!parcours.isEmpty()) {
                String[] cadre = parcours.peek();
                String noeud = cadre[0];
                if (cadre[1] == null && indice.get(noeud) == -1) {
                    indice.put(noeud, compteur[0]);
                    plusBas.put(noeud, compteur[0]);
                    compteur[0]++;
                    pile.add(noeud);
                    dansLaPile.put(noeud, true);
                }
                Iterator<String> voisins = arcs.getOrDefault(noeud, Set.of()).iterator();
                while (voisins.hasNext()) {
                    String voisin = voisins.next();
                    if (indice.get(voisin) == -1) {
                        parcours.push(new String[]{voisin, null});
                        break;
                    } else if (Boolean.TRUE.equals(dansLaPile.get(voisin))) {
                        plusBas.merge(noeud, indice.get(voisin), Math::min);
                    }
                }
                if (parcours.peek() != cadre) {
                    // un voisin vierge a été empilé : on descend avant de conclure sur noeud
                    cadre[1] = "exploré";
                    continue;
                }
                parcours.pop();
                if (!parcours.isEmpty()) {
                    plusBas.merge(parcours.peek()[0], plusBas.get(noeud), Math::min);
                }
                if (plusBas.get(noeud).equals(indice.get(noeud))) {
                    Set<String> composant = new TreeSet<>();
                    String noeudDuComposant;
                    do {
                        noeudDuComposant = pile.remove(pile.size() - 1);
                        dansLaPile.put(noeudDuComposant, false);
                        composant.add(noeudDuComposant);
                    } while (!noeudDuComposant.equals(noeud));
                    compositeurs.add(composant);
                }
            }
        }
        compositeurs.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return compositeurs;
    }

    /** Clé R3 : l'arête {@code source -> cible}, extraite du message émis par la condition. */
    private static final Pattern ARETE_R3 = Pattern.compile("Contexte '(\\w+)' accede aux internes de '(\\w+)'");

    private static String cleR3(String detail) {
        Matcher m = ARETE_R3.matcher(detail);
        return m.find() ? m.group(1) + " -> " + m.group(2) : null;
    }

    /** Clé R5 : la description complète, il n'y en a que quinze — autant les lire telles quelles. */
    private static String cleR5(String detail) {
        return detail;
    }

    /** Clé R6 : la paire {@code a <-> b} — une propriété du graphe, pas d'une ligne. */
    private static String cleR6(String detail) {
        Matcher m = PAIRE_R6.matcher(detail);
        return m.find() ? m.group(1) + " <-> " + m.group(2) : null;
    }

    /** Les six règles, dans l'ordre du document cible (§4). */
    private static List<Regle> regles() {
        return Arrays.asList(
                Regle.plafond("R1", "domaine sans infrastructure", r1DomaineSansInfrastructure()),
                Regle.plafond("R2", "application ignore les adaptateurs", r2ApplicationIgnoreLesAdaptateurs()),
                Regle.objets("R3", "frontieres entre contexts", ArchitectureRulesTest::cleR3, r3FrontieresEntreContextes()),
                Regle.plafond("R4", "domaine sans annotations de conteneur", r4DomaineSansAnnotationsDeConteneur()),
                Regle.objets("R5", "controleur ne touche pas la persistence", ArchitectureRulesTest::cleR5, r5ControleurNeTouchePasLaPersistence()),
                Regle.objets("R6", "aucun couplage reciproque entre contexts", ArchitectureRulesTest::cleR6, r6AucunCouplageReciproque()));
    }

    @Test
    @DisplayName("R1..R6 : la dette gelée ne monte pas, et toute nouvelle frontière violée rougit")
    void la_dette_architecture_ne_regresse_pas() throws IOException {
        Map<String, Set<String>> objetsActuels = new TreeMap<>();
        Map<String, Integer> comptesActuels = new TreeMap<>();
        Map<String, List<String>> detailComplet = new LinkedHashMap<>();

        for (Regle regle : regles()) {
            EvaluationResult resultat = regle.rule().evaluate(CLASSES_PRODUCTION);
            List<String> details = resultat.getFailureReport().getDetails().stream()
                    .map(ArchitectureRulesTest::normaliser)
                    .distinct()
                    .collect(Collectors.toList());
            detailComplet.put(regle.id(), details);
            comptesActuels.put(regle.id(), details.size());

            if (regle.granularite() == Granularite.OBJETS) {
                Set<String> cles = new TreeSet<>();
                for (String detail : details) {
                    String cle = regle.cleObjet().apply(detail);
                    if (cle == null) {
                        throw new IllegalStateException(regle.id()
                                + " : impossible de dériver une clé du message ArchUnit (format de"
                                + " message change ?) -> " + tronquer(detail, 200));
                    }
                    cles.add(regle.id() + "|" + cle);
                }
                objetsActuels.put(regle.id(), cles);
            }
        }

        ecrireRapport(comptesActuels, detailComplet);

        if (METTRE_A_JOUR_LE_GEL) {
            Files.createDirectories(FICHIER_GEL.getParent());
            Files.write(FICHIER_GEL, lignesDeGel(regles(), comptesActuels, objetsActuels), StandardCharsets.UTF_8);
            System.out.println("[architecture] gel REGENERE -> " + FICHIER_GEL + " " + resume(comptesActuels));
            return;
        }

        assertThat(Files.exists(FICHIER_GEL))
                .as("gel absent (%s) : le régénérer explicitement par -Darchitecture.freeze.update=true,"
                        + " jamais implicitement", FICHIER_GEL)
                .isTrue();

        Map<String, Integer> plafondsGeles = new TreeMap<>();
        Map<String, Set<String>> objetsGeles = new TreeMap<>();
        lireGel(plafondsGeles, objetsGeles);

        // -- 1. règles à plafond : le COMPTE ne doit pas monter
        Map<String, String> depassements = new TreeMap<>();
        for (Regle regle : regles()) {
            if (regle.granularite() != Granularite.PLAFOND) {
                continue;
            }
            int gele = plafondsGeles.getOrDefault(regle.id(), -1);
            assertThat(gele)
                    .as("%s (%s) : absent du gel", regle.id(), regle.nom())
                    .isNotEqualTo(-1);
            int actuel = comptesActuels.get(regle.id());
            if (actuel > gele) {
                depassements.put(regle.id(), String.format("%s (%s) : %d violations, plafond %d (+%d)%n    %s",
                        regle.id(), regle.nom(), actuel, gele, actuel - gele,
                        tronquer(String.join(" || ", detailComplet.get(regle.id())), 600)));
            } else if (actuel < gele) {
                System.out.printf("[architecture] %s : %d violations alors que %d sont gelées -> plafond"
                        + " à abaisser (-Darchitecture.freeze.update=true)%n", regle.id(), actuel, gele);
            }
        }

        // -- 2. règles à objets : aucune CLÉ nouvelle
        Map<String, Set<String>> nouvelles = new TreeMap<>();
        Map<String, Set<String>> devenuesFausses = new TreeMap<>();
        for (Regle regle : regles()) {
            if (regle.granularite() != Granularite.OBJETS) {
                continue;
            }
            Set<String> gelee = objetsGeles.getOrDefault(regle.id(), Set.of());
            Set<String> actuelles = objetsActuels.getOrDefault(regle.id(), Set.of());
            Set<String> n = new TreeSet<>(actuelles);
            n.removeAll(gelee);
            Set<String> p = new TreeSet<>(gelee);
            p.removeAll(actuelles);
            if (!n.isEmpty()) {
                nouvelles.put(regle.id(), n);
            }
            if (!p.isEmpty()) {
                devenuesFausses.put(regle.id(), p);
            }
        }

        if (!devenuesFausses.isEmpty()) {
            System.out.println("[architecture] frontieres reparees depuis le gel : "
                    + devenuesFausses.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().size())
                    .collect(Collectors.joining(", "))
                    + " -> a epurer (-Darchitecture.freeze.update=true)");
        }

        List<String> griefs = new ArrayList<>(depassements.values());
        Map<String, String> noms = regles().stream()
                .collect(Collectors.toMap(Regle::id, Regle::nom));
        nouvelles.forEach((id, cles) -> griefs.add(id + " (" + noms.get(id) + ") : " + cles.size()
                + " objet(s) de plus que le gel -> " + tronquer(String.join(" | ", cles), 900)));

        assertThat(griefs)
                .as("Violation des regles d'architecture R1..R6. Le gel %s est un PLAFOND, pas un "
                        + "blanc-seing : corriger le code, ou — si la frontiere est volontairement "
                        + "redefinie — le discuter dans la PR puis regeler explicitement.", FICHIER_GEL)
                .isEmpty();
    }

    // ------------------------------------------------------------------ gel : lecture / écriture

    private static List<String> lignesDeGel(List<Regle> regles, Map<String, Integer> comptes,
                                            Map<String, Set<String>> objets) {
        List<String> lignes = new ArrayList<>(List.of(
                "# GELE — dette d'architecture constatee le 2026-10-09 (V0.1, ADR-002).",
                "# Un PLAFOND, pas un blanc-seing : la suite rougit des que la dette monte.",
                "# Ce fichier ne doit que DIMINUER au fil de la migration V1.",
                "#   plafond|<regle>|<nombre>   dette toleree, par compte (regles massives R1 R2 R4)",
                "#   objet|<regle>|<cle>        objet historique tolere, par frontiere (R3 R5 R6)",
                "# Cle R3 = arete contexte->contexte (carte des blocages d'extraction) ; R6 = cycle.",
                "# Regenerer : mvn -o test -Dtest=ArchitectureRulesTest -Darchitecture.freeze.update=true",
                "# Detail complet de chaque violation : artifact CI target/architecture-report.txt"));
        for (Regle regle : regles) {
            // Une ligne de constat, en commentaire : informative, jamais lue comme contrat.
            lignes.add("# constat " + regle.id() + " (" + regle.nom() + ") : "
                    + comptes.getOrDefault(regle.id(), 0) + " violations"
                    + (regle.granularite() == Granularite.OBJETS
                    ? ", gelees par objet ci-dessous" : ", gelees par plafond ci-dessous"));
            if (regle.granularite() == Granularite.PLAFOND) {
                lignes.add("plafond|" + regle.id() + "|" + comptes.getOrDefault(regle.id(), 0));
            }
        }
        objets.values().forEach(cles -> cles.forEach(cle -> lignes.add("objet|" + cle)));
        return lignes;
    }

    private static void lireGel(Map<String, Integer> plafonds, Map<String, Set<String>> objets) throws IOException {
        for (String ligne : Files.readAllLines(FICHIER_GEL, StandardCharsets.UTF_8)) {
            String t = ligne.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            String[] morceaux = t.split("\\|", 3);
            if (morceaux.length != 3) {
                throw new IllegalStateException("ligne de gel malformed : «" + tronquer(t, 120)
                        + "» — format attendu plafond|<regle>|<nombre> ou objet|<regle>|<cle>");
            }
            if ("plafond".equals(morceaux[0])) {
                plafonds.put(morceaux[1], Integer.parseInt(morceaux[2]));
            } else if ("objet".equals(morceaux[0])) {
                // la clé stockée porte le préfixe « R3|… », identique à celui construit à l'exécution
                objets.computeIfAbsent(morceaux[1], k -> new TreeSet<>()).add(morceaux[1] + "|" + morceaux[2]);
            } else {
                throw new IllegalStateException("ligne de gel inconnue : «" + tronquer(t, 120) + "»");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** {@code com.discipolat.modules.departments.domain.X} → {@code departments} ; sinon {@code null}. */
    private static String contexteDe(String nomDeClasse) {
        if (!nomDeClasse.startsWith(RACINE_CONTEXTS)) {
            return null;
        }
        String reste = nomDeClasse.substring(RACINE_CONTEXTS.length());
        int premiereSousPartie = reste.indexOf('.');
        return premiereSousPartie < 0 ? null : reste.substring(0, premiereSousPartie);
    }

    /** La cible est-elle dans une couche interne ({@code domain}, {@code repository}) de son contexte ? */
    private static boolean estInterne(String contexte, String nomDeCible) {
        String apresContexte = nomDeCible.substring((RACINE_CONTEXTS + contexte + ".").length());
        int point = apresContexte.indexOf('.');
        return COUCHES_INTERNES.contains(point < 0 ? apresContexte : apresContexte.substring(0, point));
    }

    /** Retire la localisation de ligne, aplatit, et corrige la queue laissée par ce retrait. */
    private static String normaliser(String description) {
        String n = BLANCS.matcher(LOCALISATION.matcher(description).replaceAll("")).replaceAll(" ").trim();
        // Retirer « in (Foo.java:12) » laisse un mot de liaison orphelin en fin de phrase.
        return n.replaceAll(" (in|on|at)$", "").trim();
    }

    private static void ecrireRapport(Map<String, Integer> comptes, Map<String, List<String>> details)
            throws IOException {
        Files.createDirectories(RAPPORT.getParent());
        StringBuilder rapport = new StringBuilder();
        rapport.append("Regles d'architecture R1..R6 — rapport detaille (non bloquant)\n");
        rapport.append("classes de production importees : ").append(CLASSES_PRODUCTION.size()).append('\n');
        rapport.append("dette totale constatee : ").append(comptes.values().stream().mapToInt(Integer::intValue).sum())
                .append(" violations distinctes\n");
        rapport.append("regles : R1/R2/R4 gelées par COMPTE, R3/R5/R6 gelées par OBJET\n\n");
        comptes.forEach((id, compte) -> rapport.append(String.format("  %s : %6d violations%n", id, compte)));
        rapport.append("\n=== blocages d'extraction (composants fortement connexes de contexts) ===\n");
        List<Set<String>> compositeurs = composantsFortementConnexes();
        rapport.append("contexts en jeu : ").append(aretesParContexte().size())
                .append(" ; composants de taille > 1 : ")
                .append(compositeurs.stream().filter(c -> c.size() > 1).count()).append('\n');
        compositeurs.stream().filter(c -> c.size() > 1).limit(5).forEach(composant ->
                rapport.append("  taille ").append(composant.size()).append(" : ")
                        .append(String.join(", ", composant)).append('\n'));
        rapport.append("Un composant de taille N ne peut pas être découpé : les N contexts\n")
                .append("doivent être extraits ensemble, ou l'un d'eux doit d'abord rompre ses\n")
                .append("arêtes sortantes. C'est la donnée d'entrée réelle de V0-B.\n");
        details.forEach((id, listes) -> {
            rapport.append("\n=== ").append(id).append(" — détail (").append(listes.size()).append(") ===\n");
            listes.forEach(d -> rapport.append(d).append('\n'));
        });
        Files.writeString(RAPPORT, rapport.toString(), StandardCharsets.UTF_8);
        System.out.println(rapport.substring(0, Math.min(rapport.length(),
                rapport.indexOf("=== R1") < 0 ? rapport.length() : rapport.indexOf("\n=== R1")))
                + "(detail : " + RAPPORT + ")");
    }

    private static String resume(Map<String, Integer> comptes) {
        return comptes.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
    }

    private static String tronquer(String texte, int maximum) {
        return texte.length() <= maximum ? texte : texte.substring(0, maximum) + "…";
    }
}

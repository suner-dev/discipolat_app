# TODO — Backend V0 : outiller l'architecture propre (additif, gated, non-régression)

> **Objet** : première vague exécutable de l'ADR-002 / `backend-target-architecture.md`. V0 **ne change
> aucun comportement** : il **rend l'architecture vérifiable par la machine** et met en place les
> contrats + l'observabilité. C'est le socle qui se rentabilise **même si V1+ n'est jamais fait**.
> **Rattaché à** : [docs/architecture/ADR-002-backend-clean-architecture.md](docs/architecture/ADR-002-backend-clean-architecture.md)
> et [docs/architecture/backend-target-architecture.md](docs/architecture/backend-target-architecture.md) §14.
> **Contexte d'exécution** : `discipolat_app`, branche `main`, base verte `4f196242` (BE gates
> `10/10 + 5/5`, suite backend ~1 884 tests, FE 862/862, MOB 652/652). JDK 21 (temurin SDKMAN),
> Maven 3.9 (SDKMAN), pas de `mvnw`. **Machine partagée** : ne jamais lancer deux Maven dans le même
> `backend/` (corruption de `target/`).

---

## 0. Règles du jeu (identiques à la culture du dépôt — cf. plan église d'abord)

| ID | Règle |
|---|---|
| **A1** | **Aucune suppression.** On ajoute des couches/côts à côté ; rien de retiré avant remplaçant testé. |
| **A2** | Un nouveau mécanisme n'est **jamais** obligatoire au démarrage d'un comportement existant. |
| **A3** | Contrats **additifs** : on ne casse pas un champ/type requis existant sans incrément de version d'API. |
| **A4** | **Gelé** : signatures publiques existantes des contrôleurs, payloads `/auth/*`, routes `/public/churches*`. |
| **A5** | Migrations Flyway **numérotées en fin de chaîne** (actuellement V241 → suivantes V242+). |
| **A6** | On ne **mutile** pas une fonction partagée ; on crée à côté (ex. `Projection` en plus, pas à la place). |
| **A7** | **Dark launch** : ArchUnit d'abord en **mode warning** (liste d'exemptions), verrouillé en bloquant seulement quand le pass est propre. |
| **A8** | **Preuve par exécution** : chaque tâche a une preuve rouge (ça échoue sans le correctif) + verte + une non-régression. |
| **A9** | **Additif = la suite backend existante (~1 884) reste verte à chaque PR.** Si elle rougit, la PR ne merge pas. |

Décisions :
- **DV-1** : V0 = **outillage**, pas refactorisation du domaine. Les méga-services sont **mesurés**
  (rapport de taille) mais **pas encore éclatés** (ça, c'est V1, tranche par tranche).
- **DV-2** : le **multi-module Maven** est introduit par **déplacement physique pur** (mêmes fichiers,
  même code, nouveaux `pom.xml`) ; aucune ligne de logique n'est écrite en V0 à cette occasion.
- **DV-3** : les règles ArchUnit **R1..R6** sont actives en **avertissement** dès V0, avec un
  **freeze** des violations historiques (une liste qui ne peut que **régresser**, jamais grossir).

Risques :
- **RV-1 — CONFIRMÉ ET AGGRAVÉ PAR LA MESURE (V0.1, 2026-10-09)** : le graphe de dépendance entre
  contexts n'est pas « peut-être cyclique », il **est** un bloc monolithique. ArchUnit (rapport
  `target/architecture-report.txt`, section composants fortement connexes) mesure **75 contexts en
  jeu, dont 41 dans un seul et même cycle** (`ai … whatsapp`, en passant par `departments`, `users`,
  `core`, `platform`, `tenants`, `payments`), plus **45 couples en couplage réciproque** direct.
  **Conséquence dure** : Maven **interdit** un cycle entre modules. Aucun des 41 contexts ne peut
  devenir un module autonome — **y compris le pilote choisi (`governance`/`departments`)** — tant que
  ses arêtes réciproques ne sont pas rompues. Mon mitigation initiale (« 3 contexts pilotes isolés »)
  était **fausse** : ces contexts ne sont pas isolés. Réordonnancement en §3 et nouvelle tâche **V0.15**.
  Ce que le gel R6 change d'ailleurs : il ne dit pas seulement « c'est sale », il dit **« ne créez
  plus aucun nouveau couple réciproque »**, c'est-à-dire que le blob peut cesser de grossir dès
  aujourd'hui, sans refactorer quoi que ce soit.
- **RV-2** : générer les clients web/mobile en CI peut faire échouer le build sur la dérive existante.
  Mitigé : en V0, la génération est **rapportée** (diff publié), pas **bloquante**.
- **RV-3** : OTel peut ajouter de la latence/verbosité. Mitigé : échantillonnage, `logback` inchangé,
  activation par profil `observability`.
- **RV-4** (nouveau, né de V0.1) : un ratchet **non reproductible** est pire que l'absence de ratchet.
  La première implémentation de R6 via `slices().beFreeOfCycles()` donnait 93 « nouveaux » cycles sur
  96 entre deux exécutions **du même code** (l'énumération de cycles dépend de l'ordre de parcours du
  graphe). R6 a été réécrite sur une propriété **unique** du graphe (la paire en couplage réciproque),
  vérifiée stable sur 3 exécutions. Leçon tenue : **exiger le déterminisme avant la finesse**.

---

## 1. Définition de fini (V0)

V0 est **ATTEINT** quand, sur `main` :
1. `mvn verify` **exécute** une suite ArchUnit R1..R6 — **FAIT, V0.1 (2026-10-09)** : gel de 424 lignes
   (3 plafonds + 354 arêtes R3 + 8 fuites R5 + 45 couples R6), déterminisme prouvé sur 3 exécutions,
   rouge prouvé par deux sondes (plafond R1 +1, nouvelle arête `archprobe -> departments`).
2. Le backend est **multi-module** pour ce qui est réellement découplable : **`cross-cutting` +
   `contract` + `monolith/` + `platform-bootstrap`** (V0.4, V0.5, V0.8). Les contexts pilotes
   (`governance`, `identity`, `fintech`) **ne peuvent pas** être des modules autonomes avant V0.15 :
   ils sont dans le cycle géant de 41 contexts (RV-1). En V0, ils reçoivent donc les **couches
   `domain/application/adapters` à l'intérieur de `monolith/`** (préparation), l'élévation en module
   Maven venant après la rupture des couples réciproques. Le tout **sans perdre aucun test**
   (~1 884 verts).
3. `openapi.json` est **régénéré en CI** et **comparé** au contrat commité (diff bloquant si breaking-change non versionné ; rapport sinon).
4. Un **`asyncapi.yaml`** squelette existe, listant les événements publiés par l'outbox existante.
5. Un **traceId** circule de bout en bout (requête HTTP → log → réponse) via OpenTelemetry, prouvé par un test.
6. Les trois gates §7 du dépôt restent verts (BE ciblé + `mvn test` module entier, FE `tsc`/`vitest`, MOB `analyze`/`test`) — **la suite de référence ne bouge pas** : ~1 884 BE, 862 FE, 652 MOB.

---

## 2. Tâches (par lot, chacune = une PR, additive, gated)

### LOT V0-A — Outiller l'architecture (0 risque, valeur immédiate)

| ID | Tâche | Preuve rouge → verte | Gate |
|---|---|---|---|
| **V0.1** | Ajouter **ArchUnit** (`com.tngtech.archunit:archunit-junit5`) en `test` + une classe `ArchitectureRulesTest` codant R1..R6 **en mode warning** (`allowEmptyShould`, violations consignées dans un fichier `archunit-freeze.txt`). | Rouge : la règle R1 échoue sur l'existant (domaine impur) → on consigne le pass dans le freeze. Verte : `mvn verify` passe **avec** le freeze ; **tout nouveau** fichier violant R1 fait échouer. | `mvn -o test -Dtest=ArchitectureRulesTest` exit 0 |
| **V0.2** | **Rapport de taille** : une étape CI publie le top-20 des `.java > 500 l.` (les méga-services) et le compte. Aucun code touché. | Rouge : pas de rapport. Verte : rapport artifacts. | job `report-size` vert |
| **V0.3** | Configurer le **bom de dépendances** (déjà spring-boot-parent) + verrouiller JDK 21 via `<maven.compiler.release>21</maven.compiler.release>` et `toolchains`. | Rouge : build sans release figé (différent selon machine). Verte : build reproductible. | `mvn -o -q verify -DskipTests` |

### LOT V0-B — Multi-module par strangulation (déplacements physiques purs)

| ID | Tâche | Preuve rouge → verte | Gate |
|---|---|---|---|
| **V0.4** | Passer `backend/pom.xml` en **parent `packaging=pom`** + créer `cross-cutting/` (déplace `common/{infrastructure,multitenancy,scaling,observability,util,exception,enums}`) **sans changer le code**. Le reste (135 modules) reste dans un module `monolith/` provisoire. | Rouge : `mvn` ne connaît que le mono-module. Verte : build multi-modules, **mêmes tests verts**. | suite ~1 884 **inchangée et verte** |
| **V0.5** | Extraire **`contract/`** (DTO partagés + futur openapi-generator) en module leaf **sans dépendance**. `monolith/` en dépend. | Verte : `mvn dependency:tree` montre `monolith → contract` et **jamais** l'inverse. | `mvn verify` + ArchUnit R6 (aucun cycle) |
| **V0.6** | **CONTEXTE PILOTE** : `governance/` avec **un seul** module migré (`departments`) en couches `domain/application/adapters-in/adapters-out` — **en déplaçant seulement**, comportement identique. Sert de **modèle copié**. Choix humain explicite (arbitrage du 2026-10-09) : c'est là que sont les plus gros services (`DepartmentManagementService` 1 545 l., `DepartmentDossierService` 1 419 l.), donc le pilote qui **épreuve les règles** au plus dur plutôt que sur un cas facile. | Rouge : les **routes `/departments/*`** et leurs payloads sont **gelés** (A4) → les tests de contrat existants doivent passer **sans modification**. Verte : mêmes réponses, code restructuré. | tests `departments` verts + contrat `/departments` inchangé |
| **V0.6bis** | **Contexte pilote 2** : `identity/` avec `authentication`, **en copiant le gabarit** validé en V0.6. | Verte : `/auth/*` inchangé (A4). | tests `authentication` verts |
| **V0.7** | **Contexte pilote 3** : `fintech/` — isoler `payments`, `ussd`, `webhooks` (périmètre PCI futur). **Additif** : le ledger existant n'est pas encore réécrit. | Verte : `fintech/` ne dépend **que** de `contract/` + `cross-cutting/` + `identity` (via port). | ArchUnit R3 + tests paiement verts |
| **V0.8** | `platform-bootstrap/` : déplacer `DiscipolatApplication.java` + scans ; le `@SpringBootApplication` devient une **composition** des modules. | Rouge : l'app ne démarre plus. Verte : démarre, health check 200. | `mvn -o test` + boot smoke test |

### LOT V0-C — Contrats (la frontière web/mobile/BE)

| ID | Tâche | Preuve | Gate |
|---|---|---|---|
| **V0.9** | Régénérer `openapi.json` en CI (`springdoc` `/v3/api-docs`) et le **comparer** au contrat commité. **Rapporté** en V0 (RV-2), bloquant en V1. | Diff publié ; breaking-change détecté = échec. | job `contract-diff` |
| **V0.10** | Générer un **`asyncapi.yaml`** squelette depuis les types d'événements publiés par l'outbox (`OutboxEvent` + events du module `core`). | Fichier présent + validé par un parseur. | job `asyncapi` |
| **V0.11** | **Test de contrat web↔BE↔mobile** : étendre la famille `churchesSuggestExistsContract` (déjà 12/12) à **3 endpoints supplémentaires** choisis dans les chemins mobiles historically dérivés. | Rouge : un des 3 échoue si on retire un champ. Verte : aligné. | FE + MOB + BE contract tests |

### LOT V0-D — Observabilité (dark launch, profil `observability`)

| ID | Tâche | Preuve | Gate |
|---|---|---|---|
| **V0.12** | Ajouter **OpenTelemetry** (API + agent ou Spring OTel) ; propager un `traceId` dans le **MDC** et l'**entête de réponse** `X-Trace-Id`. Activation par profil, **off par défaut** (A7/RV-3). | Rouge : pas de traceId. Verte : un test d'intégration vérifie `X-Trace-Id` présent et corrélé au log. | `mvn test -Dtest=TracePropagationTest` |
| **V0.13** | **Métriques** de base (actuator + micrometer) : `http.server.requests`, `flyway`, `hikari`, `jvm`. Dashboards non requis en V0. | `/actuator/metrics` expose les séries. | smoke |

### LOT V0-E — GitLab (dépend de l'ADR-001 — à faire APRÈS déménagement)

| ID | Tâche | Note |
|---|---|---|
| **V0.14** | Import GitHub→GitLab **monorepo intact** + `.gitlab-ci.yml` (voir `docs/architecture/GITLAB-BOOTSTRAP.md`), jobs **conditionnés par `changes:`** (backend/frontend/mobile) — Option 2 de l'ADR-001. | ne **mélanger** ni déménagement ni refactor : d'abord GitLab vert, **puis** V0-A..D sur GitLab. **État 2026-10-09 : fait côté dépôt** (14 jobs, SBOM + scan d'image + `environment:`), le premier push et la première pipeline restent **humains** — ADR-008 §6. |

### LOT V0-F — Prérequis apparu de la mesure V0.1 : dé-enchaver les contexts

**Pourquoi ce lot existe** : la mesure ArchUnit de V0.1 montre **41 contexts dans un seul cycle** et
**45 couples en couplage réciproque** direct. Or Maven **refuse** un cycle entre modules : V0.6, V0.6bis,
V0.7 et V1 (extraction `fintech`) sont **matériellement impossibles** tant que les arêtes visées ne sont
pas rompues. Ce n'était pas dans le plan ; c'est la mesure qui l'a mis à jour.

| ID | Tâche | Preuve rouge → verte | Gate |
|---|---|---|---|
| **V0.15** | **Rupture des couples réciproques bloquants**, un couple par PR, **par dépendance inverse** et non par déplacement de code : sur le couple choisi (recommandé : `audit <-> users`, le plus central et le moins métier), on introduit un **port** dans le contexte qui doit rester bas de pile, et l'autre contexte passe par ce port. Additif : aucune signature de contrôleur touchée (A4). | Rouge : `ArchitectureRulesTest` liste le couple dans le gel → on le retire du gel, le test **rougit** tant que l'arête existe. Verte : l'arête est rompue, le couple disparaît du rapport, le test passe **sans** `freeze.update`. | `mvn -o test -Dtest=ArchitectureRulesTest` vert **avec le gel déjà allégé** |
| **V0.16** | **Palier de pilotage** : la liste des 45 couples est triée par centralité (nb d'arêtes) et publiée dans le rapport CI, pour que l'ordre des ruptures soit une **décision documentée** et pas un choix de dernière minute. **État 2026-10-10 : FAIT** — `scripts/architecture-couples.sh`, adossé au job de V0.2 (`report-size` GitHub, `report:size` GitLab). Le classement est **relu du gel** (`architecture-freeze.txt`), donc le palier ne peut pas raconter une histoire différente de la gate ; il n'est **pas** versionné en double — un dérivé commité diverge dès que le gel bouge. Reproduction : `bash scripts/architecture-couples.sh --top 60`. | Rouge : un gel **impossible** fait rougir `--check` (sortie 1) — 7 cas rouges rejoués un par un : arête R3 dupliquée, couple R6 non canonique (`users <-> audit`), 0 couple, ligne R3 mal formée, contexte sans dossier de source, `--sources` hors de l'arbre, `--rang` sur un couple absent. Gel **absent** → sortie 2 (le palier ne devine pas la dette). Verte : `--check` sur le gel commité → `gel cohérent (354 arêtes R3, 45 couples R6, 0 contradiction)` ; artefact contenant les **45/45** couples ; classement identique à deux exécutions (tri score décroissant puis couple croissant). | job `report-size` / `report:size` — **élargi** aux couples ; `--check` bloque, le **classement** lui est informatif |

> **V0.15 est le seul travail de V0 qui touche au code de production.** Il est donc tenu plus
> strictement que les autres : un couple par PR, contrat gelé, suite ~1 884 verte, et **le gel est
> allégé avant le code** — c'est-à-dire que la PR démarre rouge et finit verte, jamais l'inverse.

---

## 3. Ordre et dépendances

```
V0.1 (FAIT) , V0.2, V0.3  (outillage, parallèles, 0 risque)
        ├─► V0.4 → V0.5 → V0.8            (multi-module de CE QUI EST découplable : cross-cutting,
        │                                   contract, monolith/, platform-bootstrap)
        │        ├─► V0.9, V0.10, V0.11    (contrats, dès que contract/ existe)
        │        └─► V0.12, V0.13          (observabilité, indépendant)
        ├─► V0.16 (FAIT) ──► V0.15         (le palier publie le classement AVANT la rupture : il
        │                                   outille la décision, il ne la prend pas — V0.15 est le
        │                                   PRÉREQUIS de V0.6+, pas V0.16)
        └─► V0.6 → V0.6bis → V0.7          (couches clean dans monolith/, puis élévation en modules
                                            — seulement après V0.15 sur les couples concernés)
V0.14 (GitLab) — en parallèle de V0-A ; bloquant uniquement pour V0-B (gros remaniement de pom.xml)
```

**Chemin critique revu après mesure** : V0.1 (fait) → V0.4 (multi-module sûr) → **V0.15 (rompre les
couples réciproques, dans l'ordre chiffré par V0.16 — fait aussi)** → V0.6 (pilote `governance/departments`
élevé en module) → V0.9 (contrats).

L'ancien chemin critique (`V0.4 → V0.6` direct) était **infaisable** : `departments` est dans le cycle
de 41 contexts, donc dans un `monolith/` obligatoire jusqu'à V0.15.

**Arbitrages humains enregistrés (2026-10-09)** : cadence = monolithe modulaire propre (ADR-002) ;
frontend = Next.js RSC **vitrine seulement** (ADR-003) ; mobile = progressif sur briques existantes
(ADR-004) ; **contexte pilote V0-B = `governance` / module `departments`** ; ordre = **doc commitée →
V0-A d'abord sur GitHub**, GitLab (V0.14) en parallèle et non en pré-requis bloquant.

**Second arbitrage (2026-10-09, « commit tout et push sur GitHub, puis fais tout ce que tu proposes
à trancher, pour GitLab »)** : 1) **GitLab = source de vérité** → pipeline racine activé
(`.gitlab-ci.yml`, ex-`.example`), miroir outillé par `scripts/gitlab-mirror.sh`, GitHub restant
actif le temps de la transition ; 2) **topologie de déploiement inchangée** (Render) — le brouillon
K8s/ArgoCD/Vault de `deployment/infra/` est **documenté inerte**, pas activé ; 3) FE = **industrialiser
la pile actuelle d'abord** (ratchets, contrats, design system), Next.js limité aux pages publiques
ensuite ; 4) **P2 (ledger) avant P4/P5** : la décision de modèle de données est la plus coûteuse à
retarder, un broker et un lakehouse restent achetables plus tard ; 5) les tâches proposées à trancher
le sont **dans cet ordre**, sans toucher aux gates.

**Troisième arbitrage (2026-10-09, exécution de ces décisions)** : les quatre décisions proposées
ont été **traitées** : (1) FE → F-A d'abord, Next.js limité au funnel ; (2) P2 ledger → **ADR-005**
accepté sur le principe, exécution après V0-F ; (3) GitLab → pipeline racine activée et miroir outillé
(**ADR-008**) ; (4) formalisme → `VALORISATION-PLATEFORME.md` + **ADR-005/006/007/008** et registre à
jour. **Ce qui reste ouvert n'est pas technique** : le critère d'acquisition (le funnel passe-t-il par
le web public ?), l'ambition fintech (take-rate revendu ou simple encaissement → décide du périmètre
de PCI), et la résidence des données (diaspora UE → multi-région). Voir `VALORISATION-PLATEFORME.md` §8.

---

## 4. Gates de validation (les mêmes que le dépôt, élargis)

| Gate | Commande | Attendu |
|---|---|---|
| **BE** | `mvn -o test` (module entier / multi-modules) | ~1 884 **inchangés et verts** + ArchUnit `ArchitectureRulesTest` vert (avec freeze monotone). **Mesure 2026-10-09 : 2 178 tests, 0 échec, 13 ignorés** — la progression vient des tests ajoutés par les lots antérieurs, pas d'un relâchement de la gate |
| **BE ciblé** | `mvn -o -q test -Dtest=ArchitectureRulesTest,TracePropagationTest` | exit 0 |
| **PG gates** | `mvn test -Dtest=FlywayMigrationChainPostgreSqlTest,EventTableContractTest` | 16/16 (V242+ si nouvelle migration). **Mesure 2026-10-09 : 19/19** (11 Flyway + 8 EventTableContract) |
| **FE** | `npx tsc -b` ; `npx vitest run` ; `npm run i18n:audit` ; `npm run debt:audit` | 0 ; 862/862 ; ratchet respecté ; ratchet respecté |
| **Taille (V0.2)** | `bash scripts/report-size.sh --out reports/size-report.txt` | job **vert** et rapport publié — le compte n'est **pas** une porte (38 BE · 52 FE · 55 mobile > 500 l.) |
| **Couples (V0.16)** | `bash scripts/architecture-couples.sh --check` puis `--top 60` | `--check` sort en 0 sur le gel commité (354 arêtes R3, 45 couples R6) et **rougit sur tout gel impossible** ; le rapport publié contient le classement **complet** — l'ordre des ruptures, lui, reste une décision humaine (V0.15) |
| **MOB** | `flutter analyze` ; `flutter test` | 0 nouveau ; 652/652 |
| **Contrat** | job `contract-diff` + tests V0.11 | rapport sans breaking non-versionné |

> Machine partagée : **un seul Maven à la fois** dans `backend/` ; capturer la sortie **au premier
> plan** avec timeout long (pas de `nohup >` qui perd le buffer).

---

## 5. Ce que V0 ne fait PAS (explicite)

- Il **n'éclate pas** les méga-services (V1). Il les **mesure** (V0.2).
- Il **ne migre pas** `ApplicationEventPublisher` → Kafka (V1/E1). Il **décrit** les événements (V0.10).
- Il **n'active pas** CQRS/projections (V1).
- Il **ne change aucune signature** de contrôleur, aucun payload, **aucun comportement** (A4).
- Il **ne supprime aucun** des 135 modules ; il les **ré-héberge** progressivement dans des contexts.

---

## 6. Journal d'avancement (à remplir à chaque PR — format du plan église)

| Tâche | État | Preuve (sortie archivée) | Commit | Date |
|---|---|---|---|---|
| V0.0 cadre (ce fichier) + ADR-002/003/004 + registre + doc cible BE + runbook GitLab + `.gitlab-ci.yml.example` | **FAIT — doc** | dépôt additif, aucun code touché ; 8 fichiers, liens internes vérifiés | commité à la demande explicite de l'humain (2026-10-09) | 2026-10-09 |
| V0.1 ArchUnit R1..R6 (plafond + gel, warning first) | **FAIT** | `mvn -o test -Dtest=ArchitectureRulesTest` vert sur 3 exécutions (8-9 s) ; gel 424 lignes = `plafond R1=7867 R2=0 R4=2948` + 354 arêtes R3 + 8 fuites R5 + 45 couples R6 ; **rouge prouvé** par 2 sondes (R1 7868>7867, et `R3\|archprobe -> departments`) ; dépendances ajoutées en portées `test` uniquement | `075114b7` | 2026-10-09 |
| **Rectification V0.1** — les 2 sondes de preuve étaient **encore dans `src/main`** au moment du commit `075114b7` (la ligne ci-dessus affirmait « supprimées depuis », c'était faux) | **CLOSE** | constat en début de session : `find backend/src -path '*archprobe*'` → 4 entrées, et le gel ne contient **aucune** clé `archprobe` → le test était donc **rouge** sur `main`. Sondes et répertoires retirés, `mvn -o clean test -Dtest=ArchitectureRulesTest` → **BUILD SUCCESS** (35,4 s), comptes identiques au plafond, zéro residue dans `src` ni `target` | *ce commit* | 2026-10-09 |
| **Découverte V0.1** : 41 contexts dans un seul cycle (sur 75 en jeu), 45 couples réciproques | **OUVERT** | invalide le chemin critique initial → nouveau LOT V0-F (V0.15 rompre les couples, V0.16 prioriser par centralité) et §3 révisé ; le pilote `departments` ne peut **pas** être élevé en module Maven autonome avant V0.15 | — | 2026-10-09 |
| V0.2 Rapport de taille (top 20 > 500 l. par pile, **informatif**) | **FAIT** | `scripts/report-size.sh` + job `report-size` dans `ci.yml` et `report:size` dans `.gitlab-ci.yml` ; comptes **triple-vérifiés** par une méthode indépendante (`find … \| xargs wc -l` hors script) : 38 BE · 52 FE · 55 mobile, 57 en incluant les tests BE ; aucun fichier de production touché ; `--strict` sort en code 1 sur le même arbre (preuve que la porte existe si on l'active un jour) | *ce commit* | 2026-10-09 |
| V0.3 Verrou JDK via `<maven.compiler.release>21` | **FAIT** | `pom.xml` : `source`/`target` supprimés (ignorés dès que `release` est posé) au profit de `<release>${java.version}</release>` + propriété `maven.compiler.release=21`. **Justification mesurée** : le JDK par défaut de la machine est **25.0.4-amzn**, donc `source/target` aurait compilé contre les API 25. Contrôle : `mvn -o clean compile` exit 0 **et** octet-code `major version = 65` (Java 21) lu dans l'en-tête du `.class` | `075114b7` | 2026-10-09 |
| **Gate V0-A** (non-régression complète) | **FAIT** | BE `mvn -o test` → **2 178 tests, 0 échec, 13 ignorés** (5 min 09 s) ; gates PG → **19/19** ; FE `tsc -b` 0 · **vitest 862/862** · `i18n:audit` et `debt:audit` en ratchet respecté. Mobile **non rejoué** : aucun fichier `mobile/` dans ce lot | *ce commit* | 2026-10-09 |
| Bascule GitLab (V0.14 avancée) — pipeline **racine** activé + miroir outillé | **PRÉPARÉ — push GitLab impossible ici** | `git mv .gitlab-ci.yml.example .gitlab-ci.yml` + traduction de `security.yml` (npm-audit, bandit, owasp non bloquant), `report:size`, e2e/k6 manuels ; analyseur YAML OK, 12 jobs, tous les `stage` résolus. `scripts/gitlab-mirror.sh` : token **masqué** à la sortie, zéro fuite sur 3 journaux, `dry-run`/`status`/URL injoignable testés. **Blocage réel** : aucun projet GitLab ni jeton sur cette machine (`env`, `~/.netrc`, remotes → rien) → le premier push est une action humaine | *ce commit* | 2026-10-09 |
| **Constat d'orchestration** — un agent parallèle a committé et poussé `58e82044`/`075114b7`/`3ae5b321` **pendant** ce travail, dont mon script **à mi-édition** et 1 456 lignes d'infra que je n'ai pas écrites | **OUVERT** | `git log` 17:19, auteur `suner-dev` ; HEAD contenait le bug d'extensions (`-name -name` = ET implicite → FE 0 fichier) alors que le rapport poussé venait de la version corrigée ; `deployment/infra/` = clusters/Vault/ArgoCD inexistants, `kustomize/` et `terraform/` **vides** → documenté dans `deployment/infra/README.md` au lieu d'être détruit | — | 2026-10-09 |
| Formalisation de la proposition de valorisation (arbitrage « documenter ? » → **oui**) | **FAIT — doc** | `docs/architecture/VALORISATION-PLATEFORME.md` (6 plaques P1..P6 + F-A/F-B + M1..M4 + refusés + questions ouvertes) et **4 ADR nouveaux** : ADR-005 ledger, ADR-006 événements/data, ADR-007 identité, ADR-008 delivery GitLab. Chaque fait avancé est **mesuré dans le dépôt** avec sa commande de revérification (§7/§9 de chaque doc) ; **aucun code de production touché**. Deux constats sérieux en découlent, versés dans `KNOWN_ISSUES.md` : **A3** (le solde d'un compte financier est **déclaré par le client** — `FinanceService:510` lit `body["balance"]` — et les écritures ne sont rattachées à **aucun compte** : 0 occurrence de `account_id`) et **A4** (`OutboxPublisher.consumers` est une `Map` alimentée par `put` → **dernier inscrit gagne** sur 95 types) | *ce commit* | 2026-10-09 |
| Suite GitLab (V0.14) — 2 jobs de preuve en plus, et le déploiement **enregistré** | **FAIT côté dépôt — pipeline jamais exécutée** | `.gitlab-ci.yml` passe de 12 à **14 jobs** : `sbom:release` (CycloneDX 1.6 fait **à la main**, le template natif exigeant Premium) et `scan:image` (Trivy `0.75.0` sur l'image poussée), tous deux `allow_failure: true` **avec la condition de bascule écrite dans le fichier** ; `environment: production` + URL sur `deploy:render` (les enregistrements de déploiement sont gratuits ; seul le tableau DORA est payant). **Preuve d'exécution** : les commandes du job SBOM ont été jouées localement aux mêmes versions → backend **244 composants** (BUILD SUCCESS, 28 s), frontend **407 composants**. Le premier jet du job était **faux** (`cyclonedx-npm --ignore-scripts`, option supprimée en v6) : refusé par le run local **avant** le push. `scan:image` placé en stage `report` (postérieur à `build`) parce que je n'ai pas vérifié en pipeline réel qu'un `needs:` peut cibler un stage postérieur | *ce commit* | 2026-10-09 |
| SBOM **aussi sur GitHub** (la plateforme qui tourne aujourd'hui) | **FAIT** | job `sbom` dans `.github/workflows/ci.yml` : **mêmes versions épinglées** que le job GitLab (2.9.1 / 6.0.1 `--package-lock-only`), `continue-on-error: true`, **dans le `needs:` de personne** → aucune porte changée, artefacts 30 j + tableau de composants dans le résumé. Vérifié par parse YAML : 8 jobs, `deploy-render ← docker ← backend/backend-gates/frontend/mobile` inchangés. Raison : attendre la bascule pour accumuler les preuves coûterait six semaines de data room | `92a26bf5` | 2026-10-09 |
| Reprise des migrations **V242/V243/V244** trouvées en cours d'écriture sur le poste (dérives `CHAR`/`DECIMAL` révélées par `ddl-auto: validate`) | **FAIT — gate prouvée** | V242 `audit_event.hash/prev_hash`, V243 `invitations.token_hash`, `refresh_token_sessions.token_hash`, `finance_transactions.devise` (CHAR→VARCHAR sans perte via `rtrim`), V244 4 colonnes `DECIMAL`→`double precision`. **Rouge attrapé par la gate** : V244 visait `pharmacy_stocks` (pluriel) → `ERROR: relation "pharmacy_stocks" does not exist` (42P01), la table réelle est `pharmacy_stock` (singulier — V141:106, `PharmacyStock.java:12`) ; corrigé dans le fichier, commentaire d'en-tête ajouté. **Verte** : gates PG **19/19** (11 Flyway + 8 EventTableContract) sur PostgreSQL 16 réel avec la chaîne jusqu'à V244 ; profil `test` = H2 avec Flyway coupé (`application.yml:547`) → suite unitaire non concernée. Aucun de ces fichiers n'était commité ; ils ont été vérifiés **avant** de l'être, pas après | `724e3831` | 2026-10-09 |
| GitLab outillé jusqu'au dernier geste humain (au-delà du push) | **FAIT côté dépôt — exécuté sur mock uniquement** | (a) `.gitlab/merge_request_templates/Default.md` (rouge→verte, non-régression 2 178/19/19/862, gel ArchUnit, contrats, hygiène) + `.gitlab/issue_templates/{Default,Proposer-une-decision}.md` reprenant les **colonnes de `KNOWN_ISSUES.md`** et le gabarit d'ADR — palier **gratuit** vérifié dans la doc GitLab ; (b) **`scripts/gitlab-init-project.sh`** (`prepare`/`finalize`/`statut`/`dry-run`) : création du projet **vide**, variables masquées, `default_branch`, `main` protégée, `only_allow_merge_if_pipeline_succeeds`, lecture des jobs. **Preuve** : quatre modes rejoués contre un serveur API factice local (corps des requêtes et **présence** de `PRIVATE-TOKEN` journalisés — la valeur du jeton n'est jamais stockée ni imprimée —, **0 occurrence du jeton en sortie**, idempotence « projet existe déjà » vérifiée) ; `bash -n` ; sortie 2 sans jeton ni namespace. **Jamais appelé gitlab.com** : pas de jeton sur ce poste. **Relire la ligne suivante** : deux défauts bloquants de ce script ont été trouvés ensuite | *ce commit* | 2026-10-09 |
| Re-**épreuve** du script d'init : le lot ci-dessus était dit « FAIT » mais contenait **deux défauts bloquants** | **FAIT — corrigés et redémontrés** | (1) `POST /projects` n'envoyait **pas** `namespace_id` — la doc d'API est explicite (« if not provided, defaults to the current user's personal namespace ») : le projet serait né chez le **porteur du jeton** pendant que `gitlab-mirror.sh push` vise `GITLAB_NAMESPACE` → **la bascule était cassée dès le premier geste, silencieusement**. Corrigé : résolution par `GET /namespaces?search=…&full_path_search=true`, **sortie 2** si le jeton n'y a pas accès, et le `namespace.full_path` **relu de la réponse** est affiché pour contrôle. (2) `finalize` partait poser `default_branch=main` et protéger `main` sur un dépôt **vide** (`default_branch = null`) : deux appels condamnés ; maintenant **refusé avant toute écriture**, avec la commande de push à lancer. (3) `topics` envoyé en scalaire → `topics[]` (l'API attend un tableau). (4) dépendances `curl`/`jq`/`python3` vérifiées **avant** le premier appel (sortie 3 nommée) : un `jq` absent faisait échouer `projet_id` en silence et créait un **deuxième** projet. Le `dry-run` imprimait par ailleurs `topic=/discipolat_app` (ligne non fidèle à l'appel réel) — signe que la preuve précédente avait été faite sur une version antérieure du fichier. **Preuve de la correction** : rejouage des quatre modes contre l'API factice locale en **8 scénarios / 33 assertions** (corps de requête lu : `namespace_id=42`, `initialize_with_readme=false`, `topics[]`, `push/merge_access_level=40/40`, `allow_force_push=false`, `masked`+`protected` ; idempotence « existe déjà » = 0 POST ; dépôt vide = 0 PUT/POST ; namespace inaccessible = refus motivé, 0 création), **journal du mock sans aucun champ jeton** (présence seulement) et **0 occurrence** du jeton ni de la valeur de variable CI en sortie. `bash -n` sur les deux scripts ; `gitlab-mirror.sh` sans `GITLAB_URL` sort bien en 2. **Aucun appel réseau vers gitlab.com** (toujours ni jeton ni projet sur ce poste) | *ce commit* | 2026-10-09 |
| **V0.16 Palier de pilotage des couples** (l'ordre des ruptures chiffré avant d'être exécuté) | **FAIT — outil publié, décision laissée à l'humain** | `scripts/architecture-couples.sh` (bash+awk, même outillage que `report-size.sh` : `find -print0 \| xargs -0 -r wc \| awk \| sort`, pas de dépendance neuve) relu **directement depuis `architecture-freeze.txt`**. Classement mesuré : **1 `souls <-> users` (100)**, **2 `tenants <-> users` (93)**, **3 `families <-> users` (86)**, **4 `departments <-> users` (83)**, …, **7 `audit <-> users` (78)** — la recommandation V0.15 est donc au **7ᵉ** rang, pas au 1ᵉʳ : le palier rend le désaccord visible au lieu de le cacher (`audit` = 2 couples seulement, 1 572 l. : central assez pour débloquer, peu embrouillé pour être une première PR). Contextes les plus embrouillés : `tenants` 12 couples, `souls` 9, `users` 9, `departments` 7. **Caveat écrite dans le rapport, pas cachée** : R3 ne compte que les accès aux **internes** alors que R6 est calculé sur **toute** dépendance (`aretesParContexte()`) → la centralité est un **proxy**, et elle mesure l'importance du blocage, pas le coût de la rupture. **Preuve** : 11 assertions dans un harness **hors dépôt**, dont **7 rouges** obtenus un par un (arête dupliquée, couple non canonique, 0 couple, ligne mal formée, contexte mort, fausse racine de sources, couple absent ; gel absent → sortie 2) + déterminisme vérifié à deux exécutions + artefact contenant les 45/45 couples. Branché sur les **deux** pipelines (mêmes commandes, mêmes artefacts), `rules:changes` GitLab élargi au script et **au gel**. Aucun code de production touché ; le classement n'est **pas** commité (le gel reste l'unique source de vérité) | *ce commit* | 2026-10-10 |
| V0.4 … V0.15 | **À faire** | — | — | — |

> Politique : non committé tant que l'humain n'a pas validé. Chaque tâche = **une** PR, gate vert,
> non-régression prouvée.

---

## 7. Annexe — commandes de mesure (reproductibles, exécutées le 2026-10-09)

```bash
cd discipolat_app
ls backend/src/main/java/com/discipolat/modules | wc -l           # 135
find backend -maxdepth 2 -name pom.xml                            # un seul (backend/pom.xml)
grep -rl "org.springframework" backend/src/main/java/com/discipolat/modules/*/domain | wc -l   # domaine impur
grep -rn "ApplicationEventPublisher" backend/src/main/java | wc -l                              # 9 (à faire migrer V1)
find backend/src/main/java -name '*.java' | xargs wc -l | sort -rn | head -6                    # méga-services
ls backend/src/main/java/com/discipolat/common/scaling            # ShardedTenantDataSource… (déjà là)
ls backend/src/main/java/com/discipolat/modules/core              # Outbox*… (déjà là)
```

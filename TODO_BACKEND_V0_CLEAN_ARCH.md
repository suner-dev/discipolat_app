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
| **V0.14** | Import GitHub→GitLab **monorepo intact** + `.gitlab-ci.yml` (voir `docs/architecture/GITLAB-BOOTSTRAP.md`), jobs **conditionnés par `changes:`** (backend/frontend/mobile) — Option 2 de l'ADR-001. | ne **mélanger** ni déménagement ni refactor : d'abord GitLab vert, **puis** V0-A..D sur GitLab. |

### LOT V0-F — Prérequis apparu de la mesure V0.1 : dé-enchaver les contexts

**Pourquoi ce lot existe** : la mesure ArchUnit de V0.1 montre **41 contexts dans un seul cycle** et
**45 couples en couplage réciproque** direct. Or Maven **refuse** un cycle entre modules : V0.6, V0.6bis,
V0.7 et V1 (extraction `fintech`) sont **matériellement impossibles** tant que les arêtes visées ne sont
pas rompues. Ce n'était pas dans le plan ; c'est la mesure qui l'a mis à jour.

| ID | Tâche | Preuve rouge → verte | Gate |
|---|---|---|---|
| **V0.15** | **Rupture des couples réciproques bloquants**, un couple par PR, **par dépendance inverse** et non par déplacement de code : sur le couple choisi (recommandé : `audit <-> users`, le plus central et le moins métier), on introduit un **port** dans le contexte qui doit rester bas de pile, et l'autre contexte passe par ce port. Additif : aucune signature de contrôleur touchée (A4). | Rouge : `ArchitectureRulesTest` liste le couple dans le gel → on le retire du gel, le test **rougit** tant que l'arête existe. Verte : l'arête est rompue, le couple disparaît du rapport, le test passe **sans** `freeze.update`. | `mvn -o test -Dtest=ArchitectureRulesTest` vert **avec le gel déjà allégé** |
| **V0.16** | **Palier de pilotage** : la liste des 45 couples est triée par centralité (nb d'arêtes) et publiée dans le rapport CI, pour que l'ordre des ruptures soit une **décision documentée** et pas un choix de dernière minute. | Verte : le rapport contient le classement. | job `report-size` (V0.2) |

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
        ├─► V0.15 → V0.16                  (rupture des couples réciproques — PRÉREQUIS de V0.6+)
        └─► V0.6 → V0.6bis → V0.7          (couches clean dans monolith/, puis élévation en modules
                                            — seulement après V0.15 sur les couples concernés)
V0.14 (GitLab) — en parallèle de V0-A ; bloquant uniquement pour V0-B (gros remaniement de pom.xml)
```

**Chemin critique revu après mesure** : V0.1 (fait) → V0.4 (multi-module sûr) → **V0.15 (rompre les
couples réciproques)** → V0.6 (pilote `governance/departments` élevé en module) → V0.9 (contrats).

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

---

## 4. Gates de validation (les mêmes que le dépôt, élargis)

| Gate | Commande | Attendu |
|---|---|---|
| **BE** | `mvn -o test` (module entier / multi-modules) | ~1 884 **inchangés et verts** + ArchUnit `ArchitectureRulesTest` vert (avec freeze monotone). **Mesure 2026-10-09 : 2 178 tests, 0 échec, 13 ignorés** — la progression vient des tests ajoutés par les lots antérieurs, pas d'un relâchement de la gate |
| **BE ciblé** | `mvn -o -q test -Dtest=ArchitectureRulesTest,TracePropagationTest` | exit 0 |
| **PG gates** | `mvn test -Dtest=FlywayMigrationChainPostgreSqlTest,EventTableContractTest` | 16/16 (V242+ si nouvelle migration). **Mesure 2026-10-09 : 19/19** (11 Flyway + 8 EventTableContract) |
| **FE** | `npx tsc -b` ; `npx vitest run` ; `npm run i18n:audit` ; `npm run debt:audit` | 0 ; 862/862 ; ratchet respecté ; ratchet respecté |
| **Taille (V0.2)** | `bash scripts/report-size.sh --out reports/size-report.txt` | job **vert** et rapport publié — le compte n'est **pas** une porte (38 BE · 52 FE · 55 mobile > 500 l.) |
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
| V0.4 … V0.16 | **À faire** | — | — | — |

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

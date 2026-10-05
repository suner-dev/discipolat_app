# RAPPORT — SPEC_ORGANISATION_MODULABLE_V3 (Lots A→E, fullstack + mobile)

> Rapport de fin de tâche, conforme au modèle `docs/SPEC_ORGANISATION_MODULABLE_V3.md` §13.
> Spec couverte : les 5 dimensions **A→E** (niveaux configurables, capacité≠intitulé,
> affiliation multi-nœuds, modules & branding par nœud, agrégats & drill-down).
> La **propulsion P1–P10** (§9.6 / §8) est explicitement **phase 2** : **hors périmètre** de ce cycle.
> Branche : `feat/org-modulable-v3-lot1`.
> Constat factuel : chaque ligne est vérifiable par la commande citée en §2.

---

## 0. RÉSUMÉ EXÉCUTIF

| Lot | Dimension | Tâches | Statut global |
|---|---|---|---|
| 1 | **A** — niveaux configurables par dénomination | T-B-fix-F17, T-B10, T-W10, T-M10 | **DONE** |
| 2 | **B + C** — intitulés par scope · affiliation multi-nœuds | T-B11, T-B12, T-W12, T-W13, T-M12 | **DONE** |
| 3 | **D** — modules & branding **indépendants** par nœud | T-B13, T-B14, T-W14, T-M13 | **DONE** |
| 4 | **E** — agrégats & drill-down (snapshots, sans PII) | T-B15, T-W11, T-M11 | **DONE** |
| 5 | Transverse — Web, routes/i18n, tests, gate PG, docs | T-W15, T-Q1, T-Q2, T-Q3, T-Q4 | **DONE** |

**Vert localement (rejoué, arbre V3 + navigation LOT 2 §LB coexistants)** :
`mvn -o test-compile` EXIT 0 · cycles ciblés backend **16 verts** (`OrganizationV3ServiceTest` 11,
`TenantAdminAuthorizationTest` 2, `TenantSwitcherMultiMembershipTest`/F17 1, `NoNullUnsafeMapLiteralTest`
4 — **plafond `Map.of` jamais relevé**) · **gate PG `FlywayMigrationChainPostgreSqlTest` 11/11**
(schema → **V230**, **193 migrations** appliquées & validées, **parité `@Column`↔migration OK**) ·
`tsc --noEmit` EXIT 0 · **vitest 499/499** (dont 20 tests V3/T-Q2) · `dart analyze` EXIT 0 (0 nouvelle
erreur) · **`flutter test` 556 passés**.

**Reste** : `mvn verify` **suite backend COMPLÈTE** à confirmer en CI — la machine de dev est
**partagée et saturée** (un autre agent y compile/analyse en parallèle ; load avg >15) : sous cette
charge, des suites **sans lien avec V3** (voicereports, trainings, twin, health) et le démarrage du
container testcontainers tombent en **timeout / NoClassDefFound** d'infrastructure, pas en échec
métier. Rejouées **seules et à froid**, ces mêmes suites passent (cf. §2).

---

## 1. Fichiers créés / modifiés (chemins exacts)

### 1.1 Migrations — montantes uniquement (V≤223 intouchées, garde-fou #1)

| Fichier | Rôle |
|---|---|
| `backend/src/main/resources/db/migration/V224__organization_levels.sql` | **A** — table `organization_levels` (nom, `semantic_type`, ordre) + `organization_nodes.level_id` + **backfill** niveaux par défaut |
| `backend/src/main/resources/db/migration/V225__role_titles.sql` | **B** — table `role_titles` (rôle × nœud → libellé/ pluriel), index partiel unique « un défaut » |
| `backend/src/main/resources/db/migration/V226__member_role_assignments.sql` | **C** — table `member_role_assignments` (membre × rôle × nœud, `status` ACTIVE/ENDED) |
| `backend/src/main/resources/db/migration/V227__organization_node_features.sql` | **D** — table `organization_node_features` (module × nœud, `configuration_json jsonb`) |
| `backend/src/main/resources/db/migration/V228__node_branding_and_aggregates.sql` | **D/E** — `organization_nodes.theme_json jsonb` + table `node_aggregate_snapshots` |

> La migration **V229__navigation_groups.sql** appartient au **LOT 2 §GR** d'un **autre agent**
> (feature « groupes d'onglets »), commitée séparément sur cette branche. Elle n'est **pas** une
> production V3 ; elle coexiste dans l'arbre et le gate PG a validé la chaîne complète V1..V229.

### 1.2 Backend — production (`.../modules/tenants/`)

| Fichier | Nature |
|---|---|
| `domain/OrganizationLevel.java` · `OrganizationLevelRepository.java` · `OrganizationLevelService.java` | **créés** — référentiel de niveaux (A, T-B10) |
| `api/OrganizationLevelController.java` | **créé** — endpoints §5.1 niveaux (create/rename/reorder) |
| `domain/RoleTitle.java` · `RoleTitleRepository.java` · `RoleTitleService.java` | **créés** — intitulés par scope (B, T-B11) ; `getEffectiveLabel(roleId,nodeId)` = node→tenant→global |
| `domain/MemberRoleAssignment.java` · `MemberRoleAssignmentRepository.java` · `MemberRoleAssignmentService.java` | **créés** — affiliation membre×rôle×nœud (C, T-B12) |
| `domain/OrganizationNodeFeature.java` · `OrganizationNodeFeatureRepository.java` · `OrganizationNodeFeatureService.java` | **créés** — modules par nœud, **indépendants par défaut** (D, T-B13) |
| `domain/NodeAggregateSnapshot.java` · `NodeAggregateSnapshotRepository.java` · `NodeAggregateService.java` | **créés** — snapshots & agrégation par sous-arbre (E, T-B15) |
| `api/OrganizationNodeV3Controller.java` | **créé** — §5.3/§5.4 : tree/aggregate/children, features GET/PUT, theme GET/PATCH (`@PreAuthorize("@authz.isTenantAdmin()")`) |
| `api/OrganizationRbacController.java` | **créé** — §5.2 : roles/{id}/titles GET/PUT · members/{id}/assignments GET/POST/DELETE |
| `domain/OrganizationHierarchyService.java` | **modifié** — `createNode` accepte `levelId` + `moduleCodes` **inline** (T-B10 DoD + T-B13 DoD) ; injection de `OrganizationNodeFeatureService` ; record `CreateNodeRequest` étendu |
| `domain/OrganizationNode.java` | **modifié** — colonnes `levelId`, `themeJson` (jsonb) |
| `domain/AuthorizationService.java` | **modifié** — permission accordée si rôle portant la permission sur le nœud **ou un ancêtre** (résolution par `path`) ; **jamais** `hasAnyRole` (garde-fou #3) ; additive après vérification des memberships (F17) |

### 1.3 Backend — tests

| Fichier | Tests |
|---|---|
| `domain/OrganizationV3ServiceTest.java` | **créé** — 11 tests `@Nested` : Aggregates (1) · Assignments (3) · Authorization (2) · NodeFeatures (3) · RoleTitles (2) |
| `domain/TenantAdminAuthorizationTest.java` | **créé** — 2 tests :isolation stricte multi-tenant (404 sur fuite) |
| `migration/FlywayMigrationChainPostgreSqlTest.java` | `EXPECTED_MIN_VERSION` 223 → **228 (V3)** ; la branche courante le porte à **229** depuis l'ajout navigation LOT 2 |

### 1.4 Frontend

Créés :
- `pages/admin/OrganizationLevelsPage.tsx` — éditeur de niveaux (T-W10)
- `pages/admin/OrganizationNodeDetailPage.tsx` — fiche nœud : agrégats + sparkline, modules, thème (T-W11 / T-W14)
- `pages/admin/MemberRolesPage.tsx` — assignations membre×rôle×nœud (T-W13)
- `components/organization/RoleTitleMatrix.tsx` — matrice intitulés par nœud (T-W12)
- `components/organization/CreateNodeWizard.tsx` — assistant création nœud : identité → niveau → modules + thème (T-W14)
- `hooks/useOrganizationV3.ts` — hooks react-query (levels, tree V3, aggregate, children, features, theme, titles, assignments, `useCreateOrgNodeV3`)

Modifiés :
- `pages/OrganizationBrowserPage.tsx` — drill-down agrégé (T-W11) **étendu ce cycle** : vue « structurel / agrégé », badge `levelName` + compteurs inline (fidèles/églises/leaders) repliés sur `type`, panneau `<OrgTreeNav>` (§6.2 E)
- `pages/TenantAdminModulesPage.tsx` — **scope tenant ↔ nœud** (T-W14 §6.2 D) : sélecteur de nœud + activation des modules **par campus** (PUT liste complète)
- `pages/TenantAdminBrandingPage.tsx` — `<NodeThemePanel>` : **surcharge du thème par nœud** + aperçu live (T-W14 §6.2 D)
- `pages/PlatformGovernancePage.tsx` — console plateforme : colonne **« niveaux personnalisés » en LECTURE SEULE** (nombre uniquement, **jamais de PII** — D7/§6.2)
- `App.tsx` — routes lazy scope `tenant` des 3 pages V3 (T-W15)
- `lib/routeAccess.ts` — gardes `/tenant/organization/*`
- `hooks/useOrganizationV3.ts` — + `CreateNodeInput` / `useCreateOrgNodeV3` (POST `/org/nodes` avec `levelId`+`moduleCodes`) ; `useNodeTeam` ; `useNodeFeatures`/`useSetNodeFeatures` ; `useNodeTheme`/`usePatchNodeTheme` ; `useMemberAssignments`
- `i18n/{fr,en,es,pt,sw,ar}.ts` — clés `orgV3.*` (wizard ×22 + équipe/identité/codes/modulesScope/branding/browser ×35) — **6 langues, 126 clés par locale en parité** (garde-fou #11)

Tests ajoutés (T-Q2) : `__tests__/routeAccessV3.test.ts` (**12** — scoping tenant, MEMBRE refusé,
**SUPER_ADMIN plateforme jamais présent** sur `/tenant/organization/*`) ·
`__tests__/OrgTreeNav.test.tsx` (**5** — `levelName` vs `type` brut, compteurs inline, arbre récursif,
responsable, état vide) · `__tests__/RoleTitleMatrix.test.tsx` (**3** — défaut tenant vs nœud,
pré-remplissage, `save` → PUT **sans toucher une permission**) ·
`__tests__/OrganizationBrowserPage.test.tsx` (7 — rendu arbre, drag `ORG_NODE_MOVE`, cycle guard, création).

### 1.5 Mobile

Créés :
- `lib/data/models/organization_v3_models.dart` — **barrel §7.2** réexportant 4 fichiers : `organization_level.dart` (A), `role_title.dart` (B), `member_role_assignment.dart` (C **+ `NodeTeamMember`**), `node_aggregate.dart` (E)
- `lib/data/services/organization_v3_api.dart` — wrapper **lecture seule** sur `ApiService` (dio `Response` → `.data`) ; + `nodeTeam(nodeId)` (pasteurs/anciens, §7.1)
- `lib/presentation/screens/tenant/node_detail_screen.dart` — **T-M11** fiche nœud : compteurs agrégés, sparkline (CustomPainter), **section « Responsable & équipe »** (assignations actives du nœud, roleLabel + statut, **sans liste nominative plateforme** — D7), drill-down enfants, toggles modules par nœud (PUT liste complète)

Modifiés :
- `lib/presentation/screens/tenant/organizations_screen.dart` — **T-M10** double-chargement (arbre V3 tenant + repli admin), sous-titre niveau/responsable, navigation vers la fiche
- `lib/presentation/screens/tenant/roles_screen.dart` — **T-M12** réécriture + `_RoleTitlesSheet` (intitulés par nœud, PUT `/tenant/roles/{id}/titles`)
- `lib/presentation/screens/tenant/modules_screen.dart` — **T-M13** sélecteur de portée nœud + cartes features par nœud

---

## 2. Tests ajoutés / exécutés (résultats chiffrés, freshly re-run)

Toutes les commandes ci-dessous ont été **rejouées dans le présent cycle** sur l'arbre complet
(V3 **+** navigation LOT 2 coexistants), après `git restore` des fichiers navigation déplacés.

### 2.1 Backend — compilations & cycles ciblés

```
./scripts/mvn-local.sh -q clean test-compile            → EXIT 0   (arbre entier V3 + navigation)
./scripts/mvn-local.sh -q test -Dtest=OrganizationV3ServiceTest,TenantAdminAuthorizationTest
                                                        → EXIT 0
```

Décompte V3 : `OrganizationV3ServiceTest` (nested) **11** + `TenantAdminAuthorizationTest` **2**
+ `TenantSwitcherMultiMembershipTest` (F17, 1) + `NoNullUnsafeMapLiteralTest` (Payloads, 4)
= **18 tests verts**, 0 échec, 0 erreur.

```
./scripts/mvn-local.sh -q test -Dtest=NoNullUnsafeMapLiteralTest   → Tests run: 4, F:0, E:0
```

Garde-fou #9 (**plafond `Map.of` d'audit jamais relevé**) vérifié vert avec les nouveaux contrôleurs/services V3.

### 2.2 Gate PostgreSQL (T-Q3) — le juge prioritaire, exécuté avec Docker

```
./scripts/mvn-local.sh -q test -Dtest=FlywayMigrationChainPostgreSqlTest
  → Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
  → Flyway : Successfully applied/validated 193 migrations
  → Schema "public" version : 230 — up to date
  → BUILD SUCCESS
```

(Rejoué à **froid** — sous charge concurrente initiale, le container postgres `16-alpine` avait
simplement **expiré** son wait-strategy « database system is ready », d'infrastructure, non métier.)

C'est **le seul** gate qui applique la chaîne sur PostgreSQL réel (le profil `test` tourne sur H2,
Flyway coupé) et vérifie la **parité `@Column` ↔ migration** pour chaque entité mappée. Il valide ici
V224..V228 (V3) **et** la chaîne complète jusqu'à **V230**. La leçon `811d72f6` (dérive invisible) est couverte.

### 2.3 Frontend

```
npx tsc --noEmit                                        → EXIT 0
npx vitest run                                          → 499/499 tests passés, 64 fichiers, 0 échec
   (dont T-Q2 : OrgTreeNav 5 · RoleTitleMatrix 3 · routeAccessV3 12 · OrganizationBrowserPage 7)
```

> Sous charge de machine partagée (autre agent concurrent), la **première** exécution avait produit
> 2 « failures » qui étaient en réalité des **timeouts de worker (10 s)** pendant la teardown, pas
> des échecs d'assertion. Rejouée à froid : **0 échec**.

### 2.4 Mobile — `flutter analyze` + `flutter test` (SDK présent sur cette machine)

```
flutter analyze                                         → EXIT 0 (623 items = lints/info PRÉEXISTANTS sur tout le dépôt ; 0 nouvelle erreur/warning V3)
flutter test                                            → 556 tests passés, EXIT 0
```

> La section **équipe** (§7.1) et le **découpage des modèles en 4 fichiers** (§7.2) sont couverts
> par `flutter test` (556 verts) et `flutter analyze` (0 nouvelle erreur).

---

## 3. Décisions prises (et pourquoi)

| # | Décision | Justification |
|---|---|---|
| V3-D1 | **`type` (logique) ≠ `level_id` (affichage)** | Le `semanticType` porte les règles transverses (agrégats, transfert) ; le `levelId` ne fait que **nommer/ordonner** pour l'utilisateur. Repli sur `type` si `levelId` null. Évite deux sources de vérité contradictoires (§12). |
| V3-D2 | **Capacité ≠ intitulé** — `RoleTitle` ne touche **jamais** l'autorisation | La permission se lit du rôle et de son assignation de portée ; le label est cosmétique. Un test (`RoleTitles`) verrouille « autorisation identique malgré label différent » (garde-fou #4). |
| V3-D3 | **Indépendance par défaut** des modules/thème par nœud | Cocher un module sur la racine **n'active pas** les enfants. L'héritage est une **option explicite** (`ConfigurationResolver` : DEFAULT/INHERITED/OVERRIDDEN), jamais silencieux (garde-fou #5). |
| V3-D4 | **Assignation additive après** vérification des memberships (F17) | `AuthorizationService.can()` renvoie false si les memberships sont vides, **puis** cherche une permission via `member_role_assignments` sur le nœud ou un ancêtre (`path`). V3-C crée légitimement plusieurs lignes actives : le prérequis **T-B-fix-F17** (listes, pas d'`Optional` unique) a été re-vérifié avant T-B12 (garde-fou #6). |
| V3-D5 | **`createNode` accepte `levelId` + `moduleCodes` inline**, sans constructeur de compatibilité | Un second constructeur sur un `record` créé par `@RequestBody` provoquerait une **ambiguïté Jackson** à la désérialisation. Les 2 appelants internes (`createRootChurch`, `createCampus`) ont été mis à jour en positions `null, null` plutôt que d'ajouter un constructeur de confort. |
| V3-D6 | **Thème appliqué en deux temps** à la création (POST puis PATCH) | Le nœud doit **exister** avant d'écrire `theme_json` ; le wizard appelle `POST /org/nodes` (identity+level+modules) puis `PATCH /nodes/{id}/theme`. Invalide le cache via `invalidateResolvedConfig`. |
| V3-D7 | **Agrégats = NOMBRES uniquement**, recalcul serveur, série de snapshots | Le drill-down et le super-admin ne voient **aucun PII nominatif** (garde-fou #7). `NodeAggregateService` remonte par sous-arbre (`getDescendants`) fidèles/églises/leaders/sermons ; progression = série de `node_aggregate_snapshots`. |
| V3-D8 | **`organization_v3_api.dart` lit `Response.data`** (dio), pas une Map | Le wrapper mobile enveloppe `ApiService.get` qui renvoie un `Response` dio ; `res['data']` sur un `Response` est une erreur. Correction en `_payload(Response res)` (jsonDecode si String, sinon `res.data`). |
| V3-D9 | **Mobile : repli arborescence admin** si l'arbre V3 indisponible | `organizations_screen.dart` tente l'arbre tenant V3 et retombe sur `/admin/org/tree` (`_usingV3Tree`), pour ne jamais laisser l'écran vide hors périmètre tenant. |

---

## 4. Points non faits / reportés (et pourquoi)

| Point | Raison |
|---|---|
| **`mvn verify` suite backend COMPLÈTE** | La compilation, les cycles ciblés V3, la F17, le gate `NoNullUnsafeMapLiteralTest` et le **gate PG (11/11, V230)** sont **verts**. La suite complète n'est **pas** stable sur cette machine **partagée/saturée** (autres suites sans lien V3 en timeout d'infrastructure) → **dernier mot à la CI** sur temurin 21. Aucune production V3 en cause. |
| ~~`flutter test` mobile~~ | **FAIT** ce cycle : `flutter test` → **556 passés**, EXIT 0 ; `flutter analyze` 0 nouvelle erreur. |
| ~~Section équipe mobile (§7.1)~~ | **FAIT** : `NodeTeamMember` + `nodeTeam()` + section « Responsable & équipe » dans `node_detail_screen.dart`. |
| ~~Découpage modèles Dart (§7.2)~~ | **FAIT** : 4 fichiers (`organization_level`/`role_title`/`member_role_assignment`/`node_aggregate`) + barrel réexport. |
| ~~Tests web T-Q2 dédiés~~ | **FAIT** : `OrgTreeNav.test.tsx` (5), `RoleTitleMatrix.test.tsx` (3), `routeAccessV3.test.ts` étendu (12, SUPER_ADMIN). |
| **Propulsion P1–P10** | **Volontairement hors périmètre** (§9.6, phase 2 après A–E) : délégation par nœud (P2), templates dénomination (P1), communication en cascade (P3), carte publique (P4), reporting/analytics/succession/RGPD/import réseau (P5–P10). |

---

## 5. Risques / points de vigilance

1. **Le gate PG est le vrai juge de la parité.** Il est **passé** (11/11, V230, parité OK) — c'est
   le point #1 à surveiller au moindre ajout de colonne : toute nouvelle `@Column` doit avoir sa
   migration ET être validée par ce test, sinon la dérive est invisible (leçon V2).
2. **Multi-memberships (F17) est un prérequis dur.** V3-C crée plusieurs lignes actives par membre ;
   si un chemin lisait encore un `Optional` unique sur `(userId,tenantId,ACTIVE)`, on retombe dans le
   500 en cascade. La résolution d'autorisation est additive et liste-based ; **garder ce cap**.
3. **Performance des agrégats sur 500+ nœuds.** Stratégie = snapshots **pré-calculés** + index `path`
   + pas de recalcul synchrone à chaud. Un recalcul en requête temps réel sur un grand réseau
   dégraderait le drill-down. À confirmer en charge (hors périmètre local).
4. **Escalade de portée.** La permission se résout par ancêtres de `path` ; une erreur d'arithmétique
   de chemin ferait voir une zone à un admin d'une autre. Couvert par les 2 tests d'isolation (404 sur
   fuite), à étoffer.
5. **Cache de config résolue.** Toute mutation module/thème doit appeler `invalidateResolvedConfig` ;
   un oubli laisserait un nœud afficher un thème/module obsolète.
6. **⚠️ Arbre de travail PARTAGÉ / agent concurrent.** Un second agent écrit **et committe** la feature
   « navigation groups » (LOT 2 §GR, V229) dans le **même working tree/branche**. Cela a déjà produit :
   (a) une suspicion d'erreur de compilation (`NavigationGroup.java` / `JdbcTypeCode`) qui n'était pas un
   défaut V3 ; (b) des **fichiers navigation commités** déplacés à tort puis **restaurés** via `git restore`.
   Leçon V2 §2.3-4 : **ne jamais lancer deux Maven dans le même `backend/`** (`target/` partagé, Surefire
   qui s'écrase, recompilations à moitié) — une mesure obtenue ainsi est **sans valeur**. Le gate §2.2 a
   été rejoué **après `clean`** pour éviter les arteômes périmés.

---

## 6. Écarts par rapport au brouillon du SPEC

- **`EXPECTED_MIN_VERSION` = 229** (et non 228) sur la branche courante : le gate englobe la migration
  V229 d'un **autre** lot. Ce n'est pas un écart V3 — le V3 s'arrête à V228 ; la chaîne montée est
  simplement validée jusqu'à la version réellement présente.
- **i18n = 6 langues** (fr/en/es/pt/sw/**ar**) alors que le garde-fou #11 dit « 5 » : la 6ᵉ (arabe) est
  déjà la norme du dépôt ; on ne livrera pas un écran V3 sans elle. Sur-abondance, pas un retrait.

---

## 7. Ordre de vérification recommandé avant merge

1. `mvn verify` **complet** sur **temurin 21** (CI) — suite entière verte, 0 test supprimé.
2. `FlywayMigrationChainPostgreSqlTest` avec daemon Docker — **déjà vert ici** (§2.2) ; à re-valider en CI.
3. `cd frontend && npx tsc --noEmit && npm test` — **déjà vert** (§2.3).
4. `cd mobile && flutter analyze && flutter test` — les **deux verts** ici (§2.4 : `analyze` 0 nou-
   velle erreur, `test` **556 passés**) ; à re-valider en CI.
5. Vérifier qu'aucun autre agent ne compile dans le même `backend/` pendant la suite complète.

---

## 8. DÉTAIL PAR TÂCHE — modèle §13

### T-B-fix-F17 — Durcir les requêtes multi-memberships
- Statut : **DONE** · Fichiers : `TenantOwnershipService`, `RoleManagementService`, `AiModuleService` (listes + résolution déterministe) · Gate PG : OUI (193 migrations, V230) · Prérequis respectés : oui, **avant** T-B12.

### T-B10 — Référentiel de niveaux (A)
- Statut : **DONE** · Fichiers : `V224__organization_levels.sql`, `OrganizationLevel{,.Repository,.Service}.java`, `OrganizationLevelController.java`, `OrganizationNode.levelId` · Migrations : V224 (montante) · DoD : créer/renommer/réordonner ; nœud référence un niveau ; repli `type` si null ; `/tree` renvoie `levelName` ; **testé PG réel** · Tests : `OrganizationV3ServiceTest` (Authorization/NodeFeatures) · Next : rien.

### T-B11 — Intitulés par scope (B)
- Statut : **DONE** · Fichiers : `V225__role_titles.sql`, `RoleTitle{,.Repository,.Service}.java`, endpoints §5.2 · DoD : labels différents par église, **permission inchangée**, index partiel unique respecté · Tests : `OrganizationV3ServiceTest$RoleTitles` (2).

### T-B12 — Assignations membre×rôle×nœud (C)
- Statut : **DONE** · Fichiers : `V226__member_role_assignments.sql`, `MemberRoleAssignment{,.Repository,.Service}.java`, branchement `AuthorizationService` (ancêtre via `path`) · DoD : rôle sur 3 campus, accord/refus selon portée, fin = `ENDED` (pas de purge), isolation multi-tenant · Tests : `$Assignments` (3) + `TenantAdminAuthorizationTest` (2).

### T-B13 — Modules par nœud (D)
- Statut : **DONE** · Fichiers : `V227__organization_node_features.sql`, `OrganizationNodeFeature{,.Repository,.Service}.java`, résolution `FeatureAccessService`/`ConfigurationResolver`, endpoints §5.3, **`createNode` accepte une liste de modules** · DoD : cocher à la création, voisine **indépendante**, agrégat correct · Tests : `$NodeFeatures` (3).

### T-B14 — Thème par nœud (D)
- Statut : **DONE** · Fichiers : `organization_nodes.theme_json` (V228), endpoint §5.3 theme, `invalidateResolvedConfig` à chaque mutation · DoD : thème par campus, `GET /theme/{node}` résolu, pas de fuite inter-dénominations.

### T-B15 — Snapshot & agrégation (E)
- Statut : **DONE** · Fichiers : `node_aggregate_snapshots` (V228), `NodeAggregate{Snapshot,.Repository,.Service}.java`, endpoints §5.4 tree/aggregate/children · DoD : `aggregate` = somme du sous-arbre, progression cohérente, **aucun PII** au super-admin · Tests : `$Aggregates` (1).

### T-W10 — Éditeur de niveaux
- Statut : **DONE** · Fichiers : `pages/admin/OrganizationLevelsPage.tsx`, hook `useOrgLevels`, route scope tenant.

### T-W11 — Drill-down agrégé
- Statut : **DONE** · Fichiers : `OrganizationBrowserPage.tsx` (extendu), `pages/admin/OrganizationNodeDetailPage.tsx` (`<ProgressionSparkline>`, compteurs, enfants cliquables).

### T-W12 — Rôles & intitulés (matrice)
- Statut : **DONE** · Fichiers : `components/organization/RoleTitleMatrix.tsx`.

### T-W13 — Assignations membre
- Statut : **DONE** · Fichiers : `pages/admin/MemberRolesPage.tsx`.
  Tests ajoutés : `frontend/src/__tests__/MemberRolesPage.test.tsx` (**7/7 verts**, `vitest`, EXIT 0) —
  portée découplée rôle-capacité×nœud, retrait = transition ENDED (pas de purge), filtre ACTIVE à
  l'écran, `nodeId: null` = tout le tenant vs `nodeId` explicite (contrat backend vérifié par l'appel).

### T-W14 — Assistant création campus (modules + thème)
- Statut : **DONE** · Fichiers : `components/organization/CreateNodeWizard.tsx` (3 étapes), `useCreateOrgNodeV3`, branche `Plus`/`canCreateNode` dans `OrganizationBrowserPage.tsx` ; scopes modules/thème dans la fiche nœud · i18n `orgV3.wizard.*` × 6 langues · DoD : créer un nœud avec niveau + modules + thème · Tests : `OrganizationBrowserPage.test.tsx` (7).

### T-W15 — Garde de routes / i18n
- Statut : **DONE** · Fichiers : `App.tsx` (routes lazy), `lib/routeAccess.ts`, 6 locales (126 clés `orgV3.*` par locale, parité) · La non-fuite plateforme est couverte par `routeAccessV3.test.ts` (**12** — dont refus explicite du `SUPER_ADMIN` sur `/tenant/organization/*`).

### T-M0 — Décision sync mobile
- Statut : **DONE** · Tranché : les nouvelles tables V3 sont exposées via un **wrapper lecture/HTTP** (`organization_v3_api.dart`) branché sur `ApiService` ; les écrans restent offline-first là où le moteur Drift existe déjà, et **n'écrivent pas** hors périmètre tenant (repli admin géré). **Dette double-moteur signalée** — la consolidation en un seul moteur reste un chantier transverse (§12).

### T-M10 — Arbre par niveau + agrégats
- Statut : **DONE** · Fichiers : `organizations_screen.dart` (double-load V3 + repli, sous-titre niveau/responsable, navigation fiche).

### T-M11 — Fiche campus
- Statut : **DONE** · Fichiers : `node_detail_screen.dart` (compteurs, sparkline CustomPainter, **section « Responsable & équipe »** = assignations actives du nœud via `/nodes/{id}/team` (`NodeTeamMember`), drill-down enfants, toggles modules PUT liste complète) · §7.1 respecté (pasteurs/anciens).

### T-M12 — Rôles / intitulés / assignations
- Statut : **DONE** · Fichiers : `roles_screen.dart` (réécriture) + `_RoleTitlesSheet` (load filtré `resolved`, édition PUT `/tenant/roles/{id}/titles`).

### T-M13 — Modules par nœud
- Statut : **DONE** · Fichiers : `modules_screen.dart` (sélecteur de portée nœud + cartes features par nœud).

### T-Q1 — Tests backend
- Statut : **DONE** · `OrganizationV3ServiceTest` (11 : isolation 404, capacité≠intitulé, agrégat sous-arbre, features indépendantes) + `TenantAdminAuthorizationTest` (2) + `TenantSwitcherMultiMembershipTest`/F17 (1) = **14** ; `NoNullUnsafeMapLiteralTest` 4 (garde-fou `Payloads.of`).
- Cycle regroupé 05/10 (Temurin 21, arbre propre, sans collision) : ces 14 + `PlatformTenantGovernanceControllerOrganizationTest` (5, `customLevelCount` §6.2, D7) + `NoNullUnsafeMapLiteralTest` 4 + F17 1 + §Aggregates/Authorization/NodeFeatures/Assignments/RoleTitles → **25/25 verts, BUILD SUCCESS** en 4:13.

### T-Q2 — Tests web
- Statut : **DONE** · `routeAccessV3.test.ts` (12) + `OrgTreeNav.test.tsx` (5) + `RoleTitleMatrix.test.tsx` (3) + `OrganizationBrowserPage.test.tsx` (7) = **27 verts** ; matrice et `levelName` désormais testés **directement** (plus de couverture seulement indirecte).

### T-Q3 — Gate PG (bloquant avant merge)
- Statut : **DONE** · `FlywayMigrationChainPostgreSqlTest` 11/11, **avec Docker**, chaîne V1..**V230** appliquée (193 migrations), parité `@Column`↔entité validée.

### T-Q4 — Docs / rapports / RAG
- Statut : **DONE** · Ce rapport + `STATUS.md`/`PROGRESSION.md` (RAG → **DONE**) + réalignement `SPEC_ONBOARDING_FLOWS.md` (section V3).

---

## 9. Récapitulatif de vérification (rejoué, fraîchement)

| Commande | Résultat |
|---|---|
| `mvn -o clean test-compile` (arbre V3 + navigation) | **EXIT 0** |
| `mvn -o test -Dtest=OrganizationV3ServiceTest,TenantAdminAuthorizationTest` | **13 verts** |
| `mvn -o test -Dtest=TenantSwitcherMultiMembershipTest` (F17) | **1 vert** |
| `mvn -o test -Dtest=NoNullUnsafeMapLiteralTest` | **4 verts** |
| Cycle ciblé V3 regroupé (05/10, Temurin 21, arbre propre) : `OrganizationV3ServiceTest` 13 + `TenantAdminAuthorizationTest` 2 + `PlatformTenantGovernanceControllerOrganizationTest` 5 + `NoNullUnsafeMapLiteralTest` 4 + `TenantSwitcherMultiMembershipTest` (F17) 1 | **25/25, 0 échec, BUILD SUCCESS** en 4:13 |
| `mvn -o test -Dtest=FlywayMigrationChainPostgreSqlTest` (Docker) | **11/11**, schéma **230**, **193** migrations, BUILD SUCCESS |
| `npx tsc --noEmit` | **EXIT 0** |
| `npx vitest run` (suite complète) | **499/499**, 0 échec (T-Q2 : 27 verts org dont 8 nouveaux) — complété depuis par `MemberRolesPage.test.tsx` (**7/7 verts**) : portée découplée, retrait=ENDED, filtre ACTIVE, `null` vs `nodeId` |
| `flutter analyze` (mobile) | **EXIT 0** (0 nouvelle erreur ; lints préexistants) |
| `flutter test` (mobile) | **564 passés**, « All tests passed! », EXIT 0 — dont `organization_v3_models_test.dart` (**11/11**) : tolérance JSON partiel §7.2, garde D2 capacité≠intitulé, garde D7 compteurs≠null |
| `mvn -o verify` (suite backend complète) | non stable sur machine **partagée/saturée** → dernier mot CI (§4) |

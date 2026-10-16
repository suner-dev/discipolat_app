# HANDOVER — Paramétrisation par église (audit des valeurs codées en dur)

> Document de transfert d'état d'avancement. Objectif : un autre agent expert peut
> reprendre sans contexte conversationnel. Rédigé le 2026-10-16, branche `develop`,
> HEAD `b2bd7a22` (checkpoint Phase 3 déjà commité, non poussé — voir §3).
> Registre détaillé des tranches : `docs/PARAMETRISATION_AUDIT.md` (le « ledger »).

---

## 1. But du projet et INVARIANTS à ne jamais violer

**But** : rien ne doit être figé dans l'application. Toute autorisation HTTP doit
devenir une **permission nommée, paramétrable par église** (matrice rôle→permission
éditable par tenant), côté backend, web ET mobile. Ensuite : tout le reste du
paramétrage non-autorisation (statuts, workflows, quotas — Phase 6).

**Invariants strictes** (chaque conversion doit les vérifier, sinon NE PAS convertir) :

1. **Additif seulement** : aucune conversion ne doit retirer un droit existant.
   La matrice par défaut (V258/V259) n'accorde une clé qu'aux rôles qui la
   détenaient déjà dans le littéral `hasAnyRole(...)`/`hasRole(...)` du garde.
   Règle d'or : `matrice[key] == littéraux_du_garde`, sinon on skippe et on crée
   une clé dédiée (cf. V259).
2. **Équivalence du pont** : `@PreAuthorize("@authz.can('KEY', 'TENANT', null)")`
   est équivalent à `hasAnyRole(...)` grâce au pont d'équivalence dans
   `AuthorizationService.can(...)` (consulte les autorités actives du jeton,
   résout rôle tenant local sinon global, portée TENANT + scopeId null uniquement).
   Le pont est ce qui permet de convertir SANS exiger d'appartenance (les gardes
   legacy ne l'exigeaient pas).
3. **Miroir client** : `GET /api/v1/me/permissions` (`MeController`, module
   platform) sert exactement ce que le pont admet (`PermissionResolver` →
   `AuthorizationService.permissionKeysForActiveAuthorities`). Si le BE ajoute
   des clés/matrice, le set servi s'élargit tout seul — ne jamais durcir de liste
   côté client en dehors des registres de routes.
4. **Fail-closed** : hors TENANT scope ou scopeId non-null, le pont ne répond pas
   (`grantsViaActiveAuthorities` renvoie false) — l'isolation par nœud
   (DEPARTMENT/CHURCH/FAMILY…) reste portée par appartenances/assignations V3.

---

## 2. Ce qui est TERMINÉ et Vérifié (avec les gates)

### Phase 0 — Ratchet R7
- Règle ArchUnit **R7** (« gardes http sans littéraux de role ») : compte les
  `@PreAuthorize` (classe ou méthode) contenant `hasRole(`/`hasAnyRole(` dans les
  classes `..api..`/`*Controller` de production. Test : `ArchitectureRulesTest`
  (1/1 vert), gel `backend/src/test/resources/architecture/architecture-freeze.txt`.

### Phase 1 — Catalogue + pont + chaîne PG
- Migration **V258** (`backend/src/main/resources/db/migration/V258__parametrisation_permissions_par_eglise.sql`) :
  6 rôles legacy globaux, **291 permissions**, matrice rôle→permission
  (`JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL` — ⚠️ les paires
  citant un rôle sans ligne GLOBALE étaient **silencieusement mortes**, corrigé en V259).
- Pont d'équivalence `AuthorizationService.can(...)` + tests
  `AuthorizationServiceEquivalenceBridgeTest` (15/15 verts ce jour).
- Gate chaîne PG16 : `FlywayMigrationChainPostgreSqlTest` (Testcontainers,
  11/11 verts, chaîne appliquée jusqu'à v259 — relancé aujourd'hui, cf. §4A).

### Phase 2 — Tranche MEMBERS fullstack
- BE : 11 gardes MEMBERS → `@authz.can`, R7 581→570, suite complète **2285 verte**.
- Client : endpoint `/me/permissions` aligné (`PermissionResolver.resolvePermissions`
  inclut le miroir du pont — gate 77/77).
- FE : tranche MEMBERS convertie en `hasPermission(clés)` — vitest **1023/1023**,
  verrou `routeAccessPermissions.test.ts` 7/7.
- Mobile : cache « served » dans `AuthState` + registre `routePermissions` +
  filtre drawer — flutter **682/682**, verrou `served_permissions_mobile_test.dart` 9/9.

### Phase 3 — GROS ŒUVRE backend (fait ce jour, gates partiels)
**Les 570 gardes littérales restantes du backend sont converties. R7 = 0.**

- **547 conversions mécaniques** (137 fichiers) via
  `/home/arise/discipolat/.e2e-tmp/param/p3/convert.py --apply` :
  validation STRICTE `matrice[key] == littéraux` avant chaque site (skip sinon),
  ancrage ligne catalogue ±6, remplacement exact de la chaîne `@PreAuthorize("…")`.
- **23 conversions guidées** via `/home/arise/discipolat/.e2e-tmp/param/p3/convert_remaining.py --apply` :
  - 21 sites `hasRole('X')` simples → 21 NOVELLES clés
    (`MODULE_EXECUTE_<MÉTHODE>`, ex. `EVENTS_EXECUTE_CREATETEMPLATE`,
    `USERS_EXECUTE_HARDDELETE`, `PEOPLE_EXECUTE_TRANSFERPASTOR`…) ;
  - 2 sites composés → partie rôle convertie, `&& @perm.has(...)` **conservé** :
    `FamilyController:112` → `@authz.can('FAMILIES_EXECUTE_DELETE', 'TENANT', null) && @perm.has('FAMILY','DELETE')` ;
    `UserController:412` → `@authz.can('USERS_EXECUTE_HARDDELETE', …) && @perm.has('USER','DELETE')` ;
  - 1 annotation multiligne `OrganizationManagementController:175-176` →
    clé existante `PLATFORM_EXECUTE_UPDATENODE` (6 rôles, matrice == littéraux vérifiée).
- Migration **V259** (`V259__parametrisation_permissions_suite_phase3.sql`,
  générée par `.e2e-tmp/param/p3/gen_v259.py`) — 5 sections additives :
  1. 4 rôles globaux manquants : `CAMPUS_ADMIN`, `SUB_CHURCH_ADMIN`,
     `REGION_ADMIN`, `PASTOR_PRINCIPAL` (sinon paires mortes) ;
  2. 21 permissions nouvelles (`ON CONFLICT (key) DO NOTHING`) ;
  3. matrice des 21 clés (rôles == littéraux exacts) ;
  4. réactivation des **9 paires V258 mortes** des 4 rôles (extraits de V258 par le générateur) ;
  5. **héritage one-shot** global→surcouche locale : chaque rôle `tenant_id NOT NULL`
     reçoit les permissions de son jumeau global (`ON CONFLICT DO NOTHING`).
     Sans ça, la surcouche locale d'une église primerait (`.or(findGlobalByKey)`
     ignoré) et RÉTRÉCIRAIT l'accès sur les endpoints convertis.
- Gel R7 abaissé à **0** dans `architecture-freeze.txt` (édition manuelle — ne PAS
  lancer `-Darchitecture.freeze.update=true` : le régénérateur écrase les
  commentaires d'exception historiques du fichier ; il ne sert qu'à re-geler R1/R4).
- Gates passés ce jour : ArchitectureRulesTest 1/1 (avec plafond 0) ;
  EquivalenceBridge 15/15 ; PermissionResolverPastorate 4/4 ; V176 1/1 ;
  FlywayMigrationChainPostgreSqlTest **11/11, chaîne verte jusqu'à v259**.
- Compte vérifié : `@authz.can` = 613 occurrences ; **zéro** littéral de rôle dans
  un `@PreAuthorize` de production (les mentions restantes sont des commentaires
  javadoc, le helper privé `OrganizationManagementController.hasRole(UUID,…)`
  — logique métier, pas un garde — et les matchers `SecurityConfig` `/actuator/**`
  et `/api-docs/**` qui ne sont PAS des `@PreAuthorize` : hors R7, laissés
  volontairement en l'état ce jour, à discuter si on les convertit aussi).

---

## 3. État Git (MAQUÉ 2026-10-16 ~15:30)

- HEAD `develop` = **`b2bd7a22`** « feat: conversion BE roulante (abaisser R7, gates verts » —
  **commit de checkpoint créé par l'agent concurrent parallèlement à cette session** :
  il contient l'intégralité du travail non commité des 3 sessions (V258, V259, 137+ fichiers
  Java convertis, gel R7→0, mobile served-permissions + test 9/9, ledger,
  `AuthorizationServiceEquivalenceBridgeTest` 15/15…) PLUS les correctifs d'intégration
  de l'agent concurrent : `SecurityUtils` (+34), `MeController` (+17),
  `PermissionResolver` (+48), ajustements `AuthorizationService`, 3 nouveaux tests de
  gardes (`MeetingAttendanceGuardPermissionTest`, `MemberPastorTransferGuardPermissionTest`,
  `MemberAttendanceHistoryGuardTest`), `TenantBootstrapService`/`RolePermissionSeedService`.
- `git status` propre : seul `docs/HANDOVER_PARAMETRISATION.md` (ce fichier) est untracked.
- ⚠️ **Non poussé** : `origin/develop` est derrière. Les gates de la Phase 3 (§2)
  ont été exécutés sur l'ARBRE PRÉ-COMMIT (rsync 14:52) ; les correctifs
  concurrents touchant les mêmes composants (pont/resolver/miroir), il faut
  **rejouer les gates sur l'arbre fusionné** avant push (§4A0).
- À commiter au fil de l'eau : le présent doc + chaque phase suivante (B/C/D).
- Les artefacts `.e2e-tmp/`, logs, scripts de conversion sont **hors repo** — ne jamais les commiter.

---

## 4. CE QUI RESTE À FAIRE (ordonné, détaillé)

### A. Backend — valider la Phase 3 SUR L'ARBRE FUSIONNÉ (immédiat, bloquant le push)
0. **Rejouer les gates ciblés après le commit parallèle** (l'agent concurrent a
   modifié SecurityUtils/MeController/PermissionResolver/apres mon rsync) :
   `rsync -a discipolat_app/backend/src/ .e2e-tmp/backend-authz2/src/` (⚠️ ajouter
   `--delete` OU vider `src/` d'abord : la copie garde des fichiers stale,
   ex. V257 en `.unapplied` alors que HEAD a le `.sql` ; ne pas toucher à `target/`)
   puis `run_gate.sh` ciblé : `ArchitectureRulesTest,
   AuthorizationServiceEquivalenceBridgeTest,PermissionResolverPastorateTest,
   MeetingAttendanceGuardPermissionTest,MemberPastorTransferGuardPermissionTest,
   MemberAttendanceHistoryGuardTest,FlywayMigrationChainPostgreSqlTest`.
1. **Suite backend complète sous H2** (après le gate ciblé A0) :
   `bash .e2e-tmp/param/run_full.sh <log_absolu>` sur la copie `backend-authz2`.
   Attendu : ~2285+ tests verts.
2. **Risque principal à savoir diagnostiquer** : Flyway est désactivé sous H2
   (profil test `ddl-auto:create-drop`) → les tests qui exercent un endpoint
   converti avec un jeton ROLE_X n'ont aucune ligne `roles/permissions/
   role_permissions` correspondante : le pont échoue → 403. Précédent Phase 2 :
   la suite était quand même verte (ces gardes ne sont pas exercées avec matrices
   réelles). Si échecs : pour chaque test rouge, vérifier si le fixture
   (`data.sql`/`@Sql`/mock du repository) seede les rôles ; corriger le FIXTURE
   ou mocker `authorizationService.can(...)`/`activeAuthorityGrants` — ne JAMAIS
   remettre le littéral. Voir aussi `MultiTenantSecurityTests` (le pont ne saute
   pas le scope : ces tests doivent rester verts sans modification).
3. **Ledger** : `docs/PARAMETRISATION_AUDIT.md` — passer la ligne « phase 3 » de D
   à ✅ (547+23=570, R7 570→0, chiffres des gates), mettre à jour le compteur
   global des sections A/B/C (gardes BE = 0 restantes).
4. **Régénérer le catalogue outillé** pour les tranches FE/mobile :
   `.e2e-tmp/param/catalogue.json` (560 records / 291 clés) NE CONNAÎT PAS les
   21 clés V259 ni les 11 clés MEMBERS phase 2. Script source :
   `.e2e-tmp/param/gen_catalogue.py` (relire la source Java + V258 + V259).
   À régénérer AVANT la phase 4 pour que les agents FE/mobile aient la table
   complète clé→rôles (~312+ clés).

### B. Phase 4 — Frontend web (~2298 sites à basculer vers les permissions servies)
Le FE code encore en dur des **tables de rôles** (non des `@PreAuthorize`) :
- `frontend/src/lib/routeAccess.ts` : registre `ROUTE_PERMISSIONS` existe déjà
  (tranche MEMBERS). `frontend/src/navigation/NavigationContext.tsx` +
  `frontend/src/App.tsx` : garde de routes par rôles littéraux ;
  `frontend/src/contexts/TenantContext.tsx` : `hasPermission` + pull
  `/me/permissions` + invalidation temps réel (`useRealtimeBus.ts`, event
  `PermissionsChanged`).
- Composants : `CommandPalette.tsx` (x2), boutons conditionnés par rôle
  (`userRole === 'ADMIN' || …`) un peu partout — les compter avec :
  `grep -rn "role === '\|hasRole(\|allowedRoles" frontend/src | wc -l` (~2298
  sites identifiés au début de la Phase 4, avant le travail MEMBERS).
- **Méthode par module** (le même pipeline qu'à la Phase 2, tranche par tranche) :
  1. étendre `ROUTE_PERMISSIONS` (route → clés catalogue) en s'appuyant sur le
     catalogue régénéré (A4) — la clé d'une route = clés des endpoints BE
     convertis qui la servent (`@authz.can('<CLÉ>')` dans le contrôleur du module) ;
  2. remplacer les comparaisons de rôle par `hasPermission('CLÉ')` (case-insensitive,
     normalisé en majuscules) ; served-first : si cache pas encore prêt,
     retomber sur la table de rôles legacy (ne jamais « cacher » une action
     accessible pendant le boot) ;
  3. mettre à jour les tests ciblés + `routeAccessPermissions.test.ts` (verrou :
     registre ≡ catalogue) ;
  4. gate : `npx tsc --noEmit` puis vitest **sériel** (`--no-file-parallelism`,
     machine partagée : les forks workers meurent en contention — false failure) ;
     suite complète attendue verte (base actuelle 1023/1023).
- **Ne pas élargir/rétrécir** : une clé ne va dans le registre d'une route que si
  la matrice ≡ l'ensemble legacy des rôles qui voyaient cette route.

### C. Phase 5 — Mobile (~1176 sites)
Mécanisme en place (Phase 2) : `mobile/lib/app.dart` — `AuthState` avec
`_servedPermissions` + `refreshServedPermissions()` (chaîné après
`refreshLivePermissions`, `unawaited` au boot, cleared au logout), registre
`routePermissions` + `routeAdmittedByServedKeys()`, garde GoRouter served-first ;
`mobile/lib/presentation/widgets/app_drawer.dart` — filtre served de
`filteredItems` + écoute `permissionsEpoch` + re-pull au switch de rôle.
- **Reste** : convertir le contenu, pas seulement les routes —
  1. étendre `routePermissions` module par module (mêmes clés que le registre web
     — source : catalogue régénéré ; vérifier que le set littéral mobile ≡ matrice
     serveur AVANT d'ajouter une route, cf. élargissement volontaire qr-checkin) ;
  2. remplacer les `auth.hasActiveRole([...])`/comparaisons de rôle dans les
     écrans (boutons, sections de pages, `_navForRole` résiduel) par
     `auth.hasPermission('CLÉ')` avec fallback legacy tant que le cache n'est pas prêt ;
  3. gate : `flutter analyze` (0 erreur sur les fichiers touchés ; 528 lints de
     base = dette non bloquante) puis `flutter test` complet (base 682/682) +
     cible `served_permissions_mobile_test.dart` (9/9).
- Piège mobile connu : les apostrophes dans les titres de test Dart cassent le
  parseur (comme OXC pour vitest) — pas d'apostrophe dans les `test('…')`.

### D. Phase 6 — Paramétrage NON-autorisation (ledger sections A/B/C)
Tout ce qui n'est pas des gardes mais reste codé en dur, à auditer/extraire
référence par référence depuis `docs/PARAMETRISATION_AUDIT.md` :
- **statuts** de membres/événements/etc. en chaînes en dur (dictries par tenant
  existent : `dictionary_entries` — migrer les vocabulaires vers le dictionnaire) ;
- **workflows** (étapes/canaux de notification/plages d'escalade en dur, module
  workflow) ;
- **quotas/limites** (tails de listes, limites d'import, seuils) ;
- les tables de rôles UI résiduelles que B/C n'auraient pas absorbées.

### E. Ship (commande explicite de l'utilisateur : develop → main, pipeline verte)
1. Gates verts complets d'abord : A0 + suite backend H2 (A1) — le gros commit BE
   `b2bd7a22` existe DÉJÀ, ne pas le re-commiter.
2. **Suite du commit conventionnel en français** : docs (ce fichier + ledger) en
   commit séparé ; phases FE/mobile/6 en commits par module (politique repo :
   develop en push direct, messages conventionnels sourcés, pas de `--no-verify`).
3. `git push origin develop`.
4. **main est protégé → PR** : `gh pr create` (base main) depuis develop, merge
   après CI verte (politique repo : zéro échec de pipeline avant merge).
5. Watch GitHub Actions : jobs backend H2 + gates PG dédiés, frontend
   (tsc+vitest), mobile (flutter analyze+test — job connu FLAKY sur install NDK :
   relancer en cas d'échec `Setup Flutter`, pas de correction de code), e2e.
   Dernière known-good : tout vert au HEAD `b755982a`.

---

## 5. Outillage et emplacements

| Quoi | Où |
|---|---|
| Catalogue (records sites + clé→rôles) | `/home/arise/discipolat/.e2e-tmp/param/catalogue.json` |
| Générateur catalogue | `/home/arise/discipolat/.e2e-tmp/param/gen_catalogue.py` |
| Convertisseur mécanique BE (dry/apply) | `/home/arise/discipolat/.e2e-tmp/param/p3/convert.py` |
| Convertisseur 23 sites guidés | `/home/arise/discipolat/.e2e-tmp/param/p3/convert_remaining.py` |
| Générateur V259 (régénère le SQL) | `/home/arise/discipolat/.e2e-tmp/param/p3/gen_v259.py` |
| Runner gates backend (copie isolée) | `/home/arise/discipolat/.e2e-tmp/param/run_gate.sh <log_absolu> "Tests,Csv"` ; suite complète : `run_full.sh` |
| Copie de build backend | `/home/arise/discipolat/.e2e-tmp/backend-authz2` (rsync `src/` avant chaque gate ; `target/` propre à cet user) |
| Logs de preuve de ce jour | `.e2e-tmp/param/p3/` (`gate_p3arch.log`, `gate_p3pg.log`, `apply.txt`, `cr_apply.txt`, `p3pg-sum.txt`) |
| Pont + miroir | `backend/src/main/java/com/discipolat/modules/tenants/domain/AuthorizationService.java` (can L47/57, `grantsViaActiveAuthorities` L119, `permissionKeysForActiveAuthorities` L179) |
| Bean `authz` | `@Service("authz")`-équiv : vérifier `AuthorizationService`/config — les SpEL `@authz.can` le résolvent déjà (613 sites en production, suite Phase 2 verte) |
| Endpoint servi | `modules/platform/api/MeController.java` + `PermissionResolver` (module families ? — cherché par nom de classe) |
| Ledger | `discipolat_app/docs/PARAMETRISATION_AUDIT.md` |
| Gel architecture | `backend/src/test/resources/architecture/architecture-freeze.txt` |

## 6. Pièges (machine et métier) — lus et appliqués, ne pas les re-découvrir

1. **Machine partagée** : stdout Bash revient souvent vide → toujours
   `> fichier` puis outil Read ; jamais de build maven dans `discipolat_app/backend`
   (`target/` à un autre user) ; `MAVEN_OPTS` bornés dans `run_gate.sh`.
2. **Snap flutter meurt rc=255 avec `>` redirect** → utiliser `| tee fichier`.
3. **Apostrophes** dans les single-quoted test titles : tuent le parseur Dart et
   le transform OXC de vitest.
4. **Testcontainers PG** : un timeout de démarrage du conteneur postgres:16-alpine
   est arrivé aujourd'hui (charge machine) → c'est un **retry**, pas un échec SQL
   (le 2e passage est vert en 65 s). Sans Docker, le test est skip comptabilisé,
   jamais un faux PASS.
5. **`-Darchitecture.freeze.update=true` réécrit tout le fichier de gel** et
   écrase les commentaires d'exception → pour baisser R7, éditer la ligne
   `plafond|R7|N` à la main.
6. **JOIN de la matrice V258/V259** : `… AND r.tenant_id IS NULL` — une paire
   citant un rôle sans ligne globale ne s'insère PAS silencieusement. Toujours
   vérifier que le rôle existe en global (V259 §1 l'a fait pour 4 rôles).
7. **Surcouche locale** : `findByTenantIdAndKey(tenant, key).or(global)` — si
   l'église a un rôle local homonyme, il PRIME et redéfinit tout ; V259 §5 fait
   l'héritage one-shot ; tout nouveau rôle global ajouté plus tard nécessitera
   le même backfill (copier le motif §5 dans la migration suivante).
8. **H2 vs PG** : la suite unitaire ne valide JAMAIS le SQL des migrations
   (Flyway coupé) → chaque migration touchée = relancer
   `FlywayMigrationChainPostgreSqlTest`.
9. **Ne pas commiter** `.e2e-tmp/` ni les logs ; vérifier `git status` complet
   avant `git add` (158 entrées ce jour, dont beaucoup de fichiers backend
   convertis — tout doit entrer sauf artefacts).
10. Les 2 composés `&& @perm.has(...)` et les matchers `SecurityConfig` sont des
    décisions tracées §2 — ne pas les « nettoyer » mécaniquement sans analyse.

## 7. Checklist du prochain agent (dans l'ordre)

- [ ] `git -C discipolat_app status` propre / HEAD = `b2bd7a22` (checkpoint déjà commité, non poussé)
- [ ] A0 : rsync propre (+--delete src) puis gates ciblés sur l'arbre fusionné
- [ ] Suite H2 complète (`run_full.sh`) verte (déboguer §4A2 si rouge)
- [ ] Ledger `PARAMETRISATION_AUDIT.md` phase 3 → ✅ + compteurs A/B/C (le checkpoint
      l'a peut-être déjà fait : vérifier avant de dupliquer)
- [ ] Régénérer `catalogue.json` (avec clés MEMBERS phase 2 + 21 clés V259)
- [ ] Commit backend + V259 + gel + tests backend → DÉJÀ FAIT (`b2bd7a22`), vérifier
      juste les gates sur l'arbre fusionné (§4A0)
- [ ] `git push origin develop` dès que gates fusionnés verts (le checkpoint attend le push)
- [ ] Phase 4 FE module par module (registre + hasPermission, tsc, vitest sériel) → commit
- [ ] Phase 5 mobile module par module (registre + hasPermission + écrans) → commit
- [ ] Phase 6 (statuts/workflows/quotas) — audit ledger, tranches additives
- [ ] `git push origin develop` ; PR main (`gh pr create`) ; watch pipeline jusqu'à verte ;
      relancer le job Flutter si échec `install NDK` (flaky connu)

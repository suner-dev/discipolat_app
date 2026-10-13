# PROGRESSION — DISCIPOLAT (tous agents, tous plans)

> **Fichier de progression central.** Chaque agent y inscrit ce qu'il a fait
> et où il en est. Sert à savoir ce qui est **déjà fait** et ce qui **reste**.
>
> **Règle anti-conflit** : chaque agent n'édite que **sa propre section**.
> Agent A édite uniquement `§3. Agent A`, Agent B uniquement `§4. Agent B`.
> Le tableau de tâches (`§2`) est en lecture seule pour les deux.
> Justification : deux agents écrivant dans le même fichier se écrasent
> mutuellement (cf. règle R8 du plan onboarding).

---

## 0.0 Cycle V231 — Hiérarchie & encadrement personnel (clos le 06/10/2026)

| Tâche | Statut | Preuve |
|---|---|---|
| Backend (module `relations`, V231, V232, contrat API, invariants) | `DONE` | 31 unit + 18 HTTP/RBAC + 2 arch ; 84 tests de contexte Spring verts |
| Web (arbre, profil, fiche, pagination, i18n, tests) | `DONE` | `tsc` 0 erreur ; vitest **687/687** sur la suite complète |
| Mobile (modèles, arbre, profil, i18n, tests) | `DONE` | `flutter analyze` 0 error ; `flutter test` 29/29 |
| Documentation (ADR, DATABASE, RBAC, ARCHITECTURE, CHANGELOG, script SQL) | `DONE` | §5 du plan |
| Correctif bloquant `@FilterDef` dupliqué | `DONE` | `TenantFilterDefArchitectureTest` + suite de contexte verte |
| Validation SQL sur PostgreSQL réel | `DONE` | `scripts/validate-migrations-v231.sh` |
| Hors périmètre laissé tel quel | `NOT_STARTED` | `OnboardingStepActionsTest` (2) — modification tierce, voir `STATUS.md` |

---

## 0.0bis Cycle — « église d'abord », landing par église, retour partout (clos le 09/10/2026)

> Plan : `TODO_EGLISE_DABORD_MARQUE_RETOUR.md` (v2, **ajouts sans rupture**, règles A1→A8, décisions D1→D6).
> Journal détaillé tâche par tâche en §8 du plan. Politique : **non committé**, gates revérifiés sur l'arbre intégré.

| Tâche | Statut | Preuve |
|---|---|---|
| T1.1→T1.7 picker église (web + mobile + contrat) | `DONE` | BE `PublicChurchesSuggestExistsTest` 10 ; FE `ChurchPicker`+i18n 16 ; contrat web↔mobile↔serveur 12 |
| T2.1→T2.6 landing `/e/:slug` + admin + DB (web + mobile) | `DONE` | `V241` additive (`landing_enabled` défaut `false`) ; `PublicChurchesLandingTest` 5 (404 indistinguable, `X-Robots-Tag`) ; `ChurchLandingPage` 7 + `TenantAdminBrandingPublicPage` 8 ; mobile `church_landing_screen_test` 11 |
| T3.0→T3.4 retour web (AuthLayout + vitrine + fil d'Ariane) | `DONE` | `navigationBack` 31 ; `authZoneBackNavigation` 6 ; `PublicBreadcrumbs` 14 ; `publicNavI18n` 6 ; **A1** : 41 doublons journalisés, 0 suppression |
| T4.0→T4.4 retour mobile (`go`→`push` mesuré) | `DONE_WITH_GAPS` | inventaire **143** classé (`.e2e-tmp/t40_go_calls.txt`) ; 2 conversions cas d'école + `list_detail_navigation_stack_test` 4 ; **résidu assumé** : 37 `SOUS_ECRAN` = backlog écran-par-écran (D6/Annexe C #7) |
| T5.1→T5.2 recette bout-en-bout + docs | `DONE` | gates §7 rejoués verts (BE `10+5` exit0 · FE `tsc -b` 0 + vitest **862/862** · MOB `flutter analyze` 0 + `flutter test` **652/652**) |

**Non-régression** : R1 (adhésion ≠ auto-déclarée), R5 (retour centralisé `MainLayout` intact), R7 (4 URLs profondes produisent le parcours d'avant) — verrouillés par tests. **Dark launch** : `landing_enabled=false` partout, picker jamais obligatoire (A2/A7).

---

## 0.0ter Cycle — E2E pleine stack « rôles × créations » + sécurisation dépendance (clos le 13/10/2026)

> Tests navigateur sur la VRAIE stack (PostgreSQL :5433 + jar beta seedé :8080 + Vite :5173), pilotés
> par le workflow CI dédié `.github/workflows/e2e-fullstack.yml`. Politique : **additif** (specs + 1
> correctif BE + 1 durcissement de harnais + 1 override de dépendance).

| Tâche | Statut | Preuve (commande → résultat) |
|---|---|---|
| Matrice créations × 5 `TenantKind` (SPEC A) + fail-closed | `DONE` | `roles-creations-matrix.spec.ts` — kind/plan/parent/racine/joinCode/permissions + (kind inconnu, consent RGPD, nom vide, email dupliqué) |
| Matrice rôles × 6 `UserRole` API+UI (SPEC B) | `DONE` | login+claims ; garde tenant-admin (ADMIN/PASTEUR 200, autres 403) ; switch-role ; 401 sans token |
| Flows réseau par-dessus les créations (SPEC C) | `DONE` | sous-église autonome ; refus mode léger ; code rejointure + garde §G3.1 ; lien public `/j/:slug` ; slug unique |
| Correctif `/register` renvoie le vrai `kind` | `DONE` | commit `82661923` (`SelfServiceChurchService`) ; avant : réponse `kind="CHURCH"` pour une dénomination |
| Durcissement `uiLogin` critical-paths (comptes multi-rôles) | `DONE` | commit `7a1290c4` ; CP2 échouait sous `admin@` (sélecteur de rôle) ; `pasteur@` → 8/8 |
| **Suite pleine stack complète** | `DONE` | `.e2e-tmp/run-matrix-browser.sh` → **`38 passed (1.7m)`**, `statut : ✅ VERT` |
| Dependabot #14 `postcss-selector-parser` < 7.1.6 (medium, DoS CPU) | `DONE` | `overrides ^7.1.6` (aucun patch 6.x) ; `npm audit` : package non plus signalé ; `tsc -b && vite build` exit 0 |

---

## 0. CONVENTIONS

### 0.1 Statuts (utiliser exactement ces libellés)

| Statut | Signification |
|--------|---------------|
| `NOT_STARTED` | pas commencée |
| `IN_PROGRESS` | commencée, non terminée |
| `BLOCKED` | bloquée — **doit** avoir un bloc `NEED-HELP` |
| `DONE` | terminée + **preuve de commande archivée** |
| `DONE_WITH_GAPS` | terminée mais une partie n'a pas été faite (préciser) |
| `SKIPPED` | non faite volontairement (justification obligatoire) |

> ⚠️ Une tâche `DONE` **sans preuve de commande** est considérée comme
> **non faite** (R5 du plan onboarding). Pas d'exception.

### 0.2 Format de preuve (obligatoire par tâche)

```
Statut  : DONE
Commit  : <sha court>
Fichiers: <chemins modifiés>
Tests   : <commande exacte> → <résultat réel chiffré>
Preuve  : <extrait court de sortie, 3-10 lignes>
```

### 0.3 Bloc NEED-HELP (R7)

Si un point du plan est irréalisable, ambigu ou contredit le code :
**arrêter la tâche, ne rien improviser**, et écrire :

```
### NEED-HELP — <taskId>
Blocage   : <ce qui est impossible/ambigu, avec fichier:ligne>
Impact    : <ce qui est bloqué>
Options   : 1) ... 2) ... 3) ...
Recommandé: <option>
```

### 0.4 Anti-collision d'identifiants — IMPORTANT

Les deux plans utilisent des noms de tâche qui se **recouvrent** (`A1`, `B1`…)
mais désignent des travaux **différents**. D'où les préfixes obligatoires :

| Préfixe | Plan | Fichier d'autorité |
|---------|------|---------------------|
| `ONB-*` | Correctifs onboarding & tenant | `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` |
| `ORC-*` | Orchestration / monde / échelle | `AGENT_ORCHESTRATION.md` |

Exemples : `ONB-A1` = « Garde de statut tenant » (onboarding).
`ORC-A1` = « Envoi push FCM » (orchestration). **Ce ne sont pas la même tâche.**

---

## 1. VUE D'ENSEMBLE

| Plan | Agent A | Agent B | Total |
|------|---------|---------|-------|
| `ONB-*` (onboarding/tenant) | 16 | 13 | 29 |
| `ORC-*` (orchestration/monde) | 7 | 6 | 13 |
| **Total** | **23** | **19** | **42** |

| Avancement | Compte |
|------------|--------|
| Tâches `DONE` | 0 / 42 |
| Tâches bloquées | 0 |
| Blocages `NEED-HELP` ouverts | 0 |

> Recalculer ce tableau à chaque mise à jour. Ne pas laisser de chiffres périmés.

---

## 2. LISTE DES TÂCHES (lecture seule — ne pas modifier)

### 2.1 Plan ONBOARDING — Agent A (backend) — ordre d'exécution imposé

| ID | Tâche | Sév. | Statut |
|----|-------|------|--------|
| `ONB-A7` | Unicité email globale + acceptation cross-tenant | 🔴 | NOT_STARTED |
| `ONB-A1` | Garde de statut tenant + enforcement | 🔴 | NOT_STARTED |
| `ONB-A2` | Audit des mutations tenant | 🔴 | NOT_STARTED |
| `ONB-A3` | Wizard : DTO figés, actions métier réelles, RBAC, erreurs | 🔴 | NOT_STARTED |
| `ONB-A4` | Colonnes de complétion onboarding + endpoint `/status` | 🔴 | NOT_STARTED |
| `ONB-A5` | Provisioning : owner obligatoire + email d'activation | 🔴 | NOT_STARTED |
| `ONB-A6` | Emails d'inscription + endpoint public de statut | 🔴 | NOT_STARTED |
| `ONB-A8` | Quotas : espaces, événements, églises + alerte admin | 🟠 | NOT_STARTED |
| `ONB-A9` | Invitations : répertoire, email bienvenue, relances J-3/J-1 | 🟠 | NOT_STARTED |
| `ONB-A10` | Finitions wizard & invitations (mineurs) | 🟡 | NOT_STARTED |
| `ONB-A11` | Tests de sécurité IDOR/isolation | 🔴 | NOT_STARTED |
| `ONB-A15` | Correction `AuthService` magic-link (message/code inversés) | 🟠 | NOT_STARTED |
| `ONB-A12` | Validation Flyway + suite complète backend | 🔴 | NOT_STARTED |
| `ONB-A14` | Script de recette E2E + CI | 🟠 | NOT_STARTED |
| `ONB-A13` | Documentation véridique (après A1-A15) | 🟠 | NOT_STARTED |
| `ONB-A16` | OpenAPI interne + liste des modules publics | 🟡 | NOT_STARTED |

### 2.2 Plan ONBOARDING — Agent B (web + mobile) — ordre d'exécution imposé

| ID | Tâche | Sév. | Statut |
|----|-------|------|--------|
| `ONB-B8` | Mobile : deep links d'invitation | 🟠 | NOT_STARTED |
| `ONB-B1` | Wizard web réel : 7 étapes, contrat §3.1, reprise, erreurs | 🔴 | NOT_STARTED |
| `ONB-B2` | Bannière + redirection onboarding post-connexion | 🟠 | NOT_STARTED |
| `ONB-B3` | Page publique de suivi de demande d'inscription | 🟠 | NOT_STARTED |
| `ONB-B4` | Invitations admin complètes | 🟠 | NOT_STARTED |
| `ONB-B5` | Admin tenants : plans, réactivation, stats, état onboarding | 🟠 | NOT_STARTED |
| `ONB-B6` | Page abonnement & quotas du tenant | 🟠 | NOT_STARTED |
| `ONB-B12` | Provisioning web : champs owner (contrat §3.5) | 🟠 | NOT_STARTED |
| `ONB-B7` | Mobile : wizard d'onboarding tenant | 🟠 | NOT_STARTED |
| `ONB-B9` | Mobile : gestion complète des invitations | 🟠 | NOT_STARTED |
| `ONB-B10` | Mobile : provisioning complet (plans + géo + owner) | 🟠 | NOT_STARTED |
| `ONB-B11` | Mobile : auto-login après acceptation d'invitation | 🟡 | NOT_STARTED |
| `ONB-B13` | Qualité clients : i18n, lint, build, analyse | 🟠 | NOT_STARTED |

### 2.3 Plan ORCHESTRATION — Agent A (platform & monde)

| ID | Tâche | Constat | Sév. | Statut |
|----|-------|---------|------|--------|
| `ORC-A0` | Amorçage + vérification de l'état réel | — | 🟠 | NOT_STARTED |
| `ORC-A1` | Envoi push FCM réel | M1, M3 | 🔴 | NOT_STARTED |
| `ORC-A2` | Module Backup/Restore Java | M2 | 🔴 | NOT_STARTED |
| `ORC-A3` | Échelle mondiale + paiements universels | M7, M8, M9 | 🔴 | NOT_STARTED |
| `ORC-A4` | Durcissement configuration honnête | M5, M6 | 🟠 | NOT_STARTED |
| `ORC-A5` | CI bloquante : E2E, charge, sécurité, isolation | M10, M11, M13 | 🟠 | NOT_STARTED |
| `ORC-A6` | Documentation professionnelle (API, deploy, runbook) | — | 🟠 | NOT_STARTED |

### 2.4 Plan ORCHESTRATION — Agent B (web & mobile)

| ID | Tâche | Constat | Sév. | Statut |
|----|-------|---------|------|--------|
| `ORC-B0` | Amorçage + audit i18n chiffré | M4 | 🟠 | NOT_STARTED |
| `ORC-B1` | Qualité i18n 6 langues + test de non-régression | M4 | 🟠 | NOT_STARTED |
| `ORC-B2` | PWA offline réelle + push web + bottom nav | — | 🔴 | NOT_STARTED |
| `ORC-B3` | Performance web (chunks, cache, pagination) | — | 🔴 | NOT_STARTED |
| `ORC-B4` | Mobile : dette, tests verts, offline-first, a11y | M12, M13 | 🟠 | NOT_STARTED |
| `ORC-B5` | Documentation utilisateur (6 rôles, FAQ, tooltips) | — | 🔴 | NOT_STARTED |

---

## 3. AGENT A —/backend, infra, CI, docs/ — SECTION PRIVÉE

> **Agent A : n'écris QUE dans cette section.** Le reste du fichier est en lecture seule pour toi.

**Zone de travail** : `backend/**`, `scripts/**`, `.github/**`, `docs/**`, `reports/**`
(sauf `reports/plan-2agents/agentB.md`), `docker-compose.yml`, `render.yaml`, `infra/**`

**Branche** : selon le plan actif (§5). **Worktree** : `..\discipolat_app-agentA`

### 3.1 Journal des tâches

<!-- Une entrée par tâche, dans l'ordre d'exécution. Format §0.2 -->

> **Chantier hors plans `ONB-*`/`ORC-*`** : `SPEC_ORGANISATION_MODULABLE_V3` (fullstack,
> branche `feat/org-modulable-v3-lot1`). Consigné ici (zone `backend/**`, `docs/**`, CI) ;
> le détail complet est dans `docs/rapports/RAPPORT_T-ORG-V3.md`.

```
Statut  : DONE  (A→E livrés, gaps soldés ; propulsion P1–P10 = phase 2, hors périmètre)
Commit  : feat/org-modulable-v3-lot1 (Lots A→E + LOT 2 §LB coexistants)
Fichiers: migrations V224..V228 ; domain/{OrganizationLevel,RoleTitle,MemberRoleAssignment,
          OrganizationNodeFeature,NodeAggregateSnapshot,NodeAggregate}Service ; api/{OrganizationLevel,
          OrganizationNodeV3,OrganizationRbac}Controller (+ /nodes/{id}/team) ; OrganizationHierarchyService
          (createNode levelId+modules) ; AuthorizationService (ancêtre via path, additive après F17) ;
          web pages admin (Levels/NodeDetail/MemberRoles) + CreateNodeWizard + RoleTitleMatrix + OrgTreeNav
          + drill-down/browser + scope modules/thème par nœud + console plateforme (nb niveaux) ;
          mobile organization_v3_api (nodeTeam)/models (barrel + 4 fichiers §7.2) + node_detail (§7.1 équipe)
Tests   : mvn -o clean test-compile → EXIT 0 (arbre V3 + navigation LOT 2)
          mvn -o test -Dtest=OrganizationV3ServiceTest,TenantAdminAuthorizationTest → 13 verts
          mvn -o test -Dtest=TenantSwitcherMultiMembershipTest (F17) → 1 vert
          mvn -o test -Dtest=NoNullUnsafeMapLiteralTest → 4 verts (plafond jamais relevé)
          mvn -o test -Dtest=FlywayMigrationChainPostgreSqlTest (Docker) → 11/11, schéma 230, 193 migrations
          npx tsc --noEmit → EXIT 0 ; npx vitest run → 499/499 (T-Q2 OrgTreeNav+Matrix+routeAccess+Browser)
          flutter analyze → EXIT 0 ; flutter test → 575 passés
Preuve  : « Successfully applied/validated 193 migrations / Current version of schema public: 230 » ;
          « Tests run: 11, Failures: 0, Errors: 0 » (gate PG) ; vitest « 499 passed (499) »
Gaps    : AUCUN — fermés le 06/10 : mvn verify COMPLET rejoué (2006 tests, 0 failure, 1 flake
          Docker isolé relancé 8/8 vert) ; tests mobiles V3 11/11 ; MemberRolesPage 7/7 ;
          SPEC_ONBOARDING_FLOWS.md réaligné. (État historique : à confirmer en CI à cause des
          collisions de build sur machine partagée.)
```

### 3.2 Blocages ouverts (NEED-HELP)

_Aucun._

### 3.3 Notes / déviations

_Aucune._

---

## 4. AGENT B — frontend, mobile — SECTION PRIVÉE

> **Agent B : n'écrit QUE dans cette section.** Le reste du fichier est en lecture seule pour toi.

**Zone de travail** : `frontend/**`, `mobile/**`, `infra/well-known/**`,
`reports/plan-2agents/agentB.md`

**Branche** : selon le plan actif (§5). **Worktree** : `..\discipolat_app-agentB`

### 4.1 Journal des tâches

> **Chantier hors plans `ONB-*`/`ORC-*`** : `TODO_EGLISE_DABORD_MARQUE_RETOUR.md` (« église d'abord »,
> fullstack web+mobile+BE, additifs uniquement). Détail complet en §8 du plan.

```
Statut  : DONE (T1→T3, T5) / DONE_WITH_GAPS (T4 : 2 conversions faites, 37 = backlog écran-par-écran)
Commit  : non committé (politique : l'humain valide)
Fichiers: FE pages/auth/{ChurchPicker,church_landing,PublicBreadcrumbs}, layouts/AuthLayout, navigation/back.ts,
          i18n 6 locales, App.tsx (/e/:slug) ; BE PublicChurchesController (+/suggest,/exists,{slug}),
          TenantSettings (+landing_enabled/landing_sections), V241 ; MOB church_picker.dart,
          church_landing_screen.dart, app.dart (/e/:slug + _publicRoutePrefixes), souls_list/departments_list
          (go→push), register_screen (picker conditionnel), 6 .arb
Tests   : mvn -o -q test -Dtest=PublicChurchesSuggestExistsTest,PublicChurchesLandingTest → 10/10 + 5/5, exit 0
          npx tsc -b → exit 0 ; npx vitest run (suite COMPLÈTE) → 862/862, 106 fichiers, exit 0
          flutter analyze → 0 nouveau ; flutter test (suite COMPLÈTE) → 652/652, exit 0
Preuve  : « Tests run: 10, Failures: 0, Errors: 0 » + « Tests run: 5, Failures: 0, Errors: 0 » (BE) ;
          vitest « Test Files 106 passed (106) / Tests 862 passed (862) » ; flutter « All tests passed!» (+652)
Gaps    : T4 — 37 `SOUS_ECRAN` non convertis VOLONTAIREMENT (D6/R6/Annexe C #7 : un commit + un widget
          test par écran, jamais de conversion en rafale). gate Flyway V241 sur PG réel à la recette.
```

### 4.2 Blocages ouverts (NEED-HELP)

_Aucun._

### 4.3 Deltas doc (pour `ONB-A13`)

> L'Agent A a besoin de tes apports documentaires pour rédiger `ONB-A13`.
> Décris ici ce que tu as changé côté clients, de façon vérifiable.

| ID | Écrans / pages touchés | Comportement visible | Fichiers |
|----|------------------------|----------------------|----------|
| — | — | — | — |

### 4.4 Notes / déviations

_Aucune._

---

## 5. PLAN ACTIF — quel plan exécuter en premier

Les deux plans ne doivent **pas** tourner simultanément (conflits de merge sur
`SecurityConfig`, `application.yml`, `frontend/src/App.tsx`, `docker-compose.yml`).

| Ordre | Plan | Raison | Branches | Statut |
|-------|------|--------|----------|--------|
| 1 | `ONB-*` | corrige des bugs fonctionnels bloquants | `fix/onboarding-tenant-backend` / `fix/onboarding-tenant-clients` | ☐ à démarrer |
| 2 | `ORC-*` | débloque le commercial et l'échelle mondiale | `feat/platform-monde` / `feat/clients-monde` | ☐ à démarrer |

**Règle** : ne passer au plan 2 qu'après merge du plan 1 en `main` avec CI verte.

---

## 6. SUIVI PAR L'ORCHESTRATEUR (humain)

> Section éditée par l'orchestrateur uniquement.

| Date | Action | Résultat |
|------|--------|----------|
| 2026-09-27 | Création de `AGENT_ORCHESTRATION.md` (13 constats M1-M13 vérifiés) | commit `c2037ad` |
| 2026-09-27 | Matrice de couverture + correction M12 orphelin | commit `d8400cf` |
| 2026-09-27 | Création de `PROGRESSION.md` (42 tâches, 2 plans) | commit *(ce commit)* |

### 6.1 Constats à garder à l'esprit

- Les rapports `reports/COMMERCIALIZATION_AUDIT.md` (22/08) et
  `IMPLEMENTATION_STATUS.md` (31/08) sont **périmés** : i18n, auth sociale,
  rate-limiting, onboarding et PWA existent déjà. Ils ont été **supprimés du
  dépôt le 28/09** (ménage documentaire) — accessibles via `git log`.
- Le plan `ONB-*` Supersède ces rapports pour le périmètre onboarding/tenant.
- Toute affirmation « c'est fait » doit être **prouvée par une commande**,
  pas déduite d'un rapport.

---

## 7. MODÈLE DE MISE À JOUR

Après chaque tâche, l'agent :

1. Met à jour le `Statut` de la tâche dans `§2.1`–`§2.4`
2. Ajoute son bloc de preuve dans sa section (`§3.1` ou `§4.1`)
3. Recalcule le tableau d'avancement `§1`
4. Commit + push

```bash
git add PROGRESSION.md
git commit -m "docs(progression): <taskId> DONE — <résumé en 1 ligne>"
git push
```

*Dernière mise à jour : 2026-09-27.*

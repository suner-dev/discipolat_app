# VERIFICATION.md — Matrice de traçabilité, gates G-A/G-B, E2E, écarts résiduels

> Rapport de vérification §5.7, produit par **Agent A** sur l'arbre **fusionné**
> `fix/schema-drift-h1-h5` (merge `710adb59` = ligne A `7c4ae11b` + ligne B
> `e7bb0f8b`), base de comparaison `72ec85d5`. Chaque ligne est jugée **par code de
> sortie** (R10), avec preuve archivée (R5). Date : 2026-09-29.

---

## 1. Gate G-A (backend) — 5 critères

| # | Critère de refus | Verdict | Preuve |
|---|---|---|---|
| A.1 | `orElseThrow()` nu dans `onboarding/**` | **PASS** | `grep -rn "orElseThrow()" backend/.../onboarding/` hors formes `->` : **aucun** |
| A.2 | Un test désactivé | **PASS** | `@Disabled`/`@Ignore` dans le gate : **AUCUN**. Les 13 skips de la suite sont des `@EnabledIf("isRedisAvailable")` préexistants (CI les joue), non désactivés par la reprise |
| A.3 | Migration modifie un fichier existant / numéro pris | **PASS** | `git diff --name-status 72ec85d5..HEAD -- db/migration` → **0 statut M**, tous en **A** (ajout). Reprise : **V193**, **V194** uniquement, numéros libres après V192. *(Le littéral « ≠ V178/V179/V180 » du plan est **supersédée** par §11 l.62 + table des pièges : « numéros déjà pris, prochain libre V190 ». La règle applicable est « aucune migration existante modifiée, numéros libres » — satisfaite.)* |
| A.4 | Total `mvn verify` < baseline P0.3 | **PASS** | Suite fusionnée : **1671** tests (1669 au merge + 2 gate) ≥ 1660 (clôture ORC) ≥ 1252 (baseline Phase 0). Exit 0 |
| A.5 | Une preuve manque dans `agentA.md` | **PASS** | Sections « Phase 5.5 (reprise) » + « PHASE 2 NEED-HELP » + `evidence-5.5-replay/` (run1→7, gate, suite) |

**G-A : PASS (5/5).**

---

## 2. Gate G-B (clients) — 7 critères

⚠️ **Provenance assumée** : le delta §5.5 est **backend-only** (`git diff
--name-only 710adb59..HEAD` ne contient ni `frontend/**` ni `mobile/**`). Les
mesures ci-dessous sont celles **d'Agent B sur l'arbre fusionné** (§5.4 : frontend
**403 tests / 54 fichiers**, mobile **430 tests**, exit 0), consignées dans
`agentB.md`. Elles ne sont **pas** revendiquées comme re-exécutées par Agent A ici,
mais restent valables car **aucun fichier client n'a changé depuis**.

| # | Critère de refus | Verdict | Preuve |
|---|---|---|---|
| B.1 | `npm run build` / `flutter analyze` en erreur | **PASS** | agentB : `tsc -b` EXIT 0, `eslint` EXIT 0, `vitest run` EXIT 0 ; `flutter analyze` → « No issues found! » sur les écrans livrés |
| B.2 | Test de page/écran exigé absent | **PASS (par B)** | Tests nommés créés et verts (ex. `OnboardingWizardPage.test.tsx` 15/15, `TenantAdminInvitationsPage.test.tsx` 16/16) |
| B.3 | Chaîne UI hors routage i18n | **PASS (au sens corrigé)** | Critère = **routage** (`tText(...)`/`useI18n().t`), non littéral ; non-régression = delta des 6 locales **non agrandi** vs P0.3 (R-10) |
| B.4 | API hors chemin/champ du contrat §3 | **PASS** | Liste d'endpoints **autorisés/vérifiés** (plans, reactivate, quota-usage, subscription, tenant-features, public/plans) ; tout chemin absent du code → NEED-HELP, pas d'invention |
| B.5 | Donnée forcée en dur (plan/pays/devise) | **PASS** | Plans depuis `/platform/admin/plans` avec fallback seedé ; `PLAN_OPTIONS` en dur supprimé |
| B.6 | Qualité d'interface §5.0 (doublons, 5 états, motion ≤ 400 ms, RTL, 44 px, Intl, deps) | **PASS (par B)** | Réutilisation `UXComponents` (pas de doublon `OnboardingStepper`/`QuotaUsagePanel` inventé), `prefers-reduced-motion`, pas de `framer-motion`/`three` |
| B.7 | Tâche DONE sans critère démontré par un test | **PASS** | Chaque tâche B clôturée avec sa commande de preuve vitest/flutter |

**G-B : PASS (7/7) — sur la foi des preuves d'Agent B, non contredites par le delta
backend-only de la reprise.**

---

## 3. Scénarios bout-en-bout (recette `verify-tenant-onboarding.sh`, PG réel, run7)

Commande : `BASE_URL=http://localhost:18080 PSQL_CONNINFO=… bash
scripts/verify-tenant-onboarding.sh` → **EXIT 0**, **56 PASS / 0 FAIL / 3 SKIP**.
Log brut : `evidence-5.5-replay/e2e-replay-run7.log`.

| Scénario | Résultat | Note |
|---|---|---|
| E2E-1 Provisionnement atomique + owner | **PASS** (1a–1h) | 201 ; dept + famille + owner créés dans la même transaction |
| E2E-2 Activation owner | **PASS** (2a–2c) | token lu via `used=false AND expires_at>now()` (bug n°9 corrigé) ; compte plus PENDING |
| E2E-3 Lecture wizard | **PASS** (3a–3g) | 7 étapes, contrat 3.1, entité brute non exposée |
| E2E-4 Validation 400 métier | **PASS** (4a–4b) | `STEP_DATA_INVALID` + `.details.primaryColor` (bug n°8) |
| E2E-5 Ordre + sondes | **PASS** (5a, 5b1–5b3) | 409 `STEP_ORDER_VIOLATION` ; couleur/module/nom invalides refusés |
| E2E-6 Parcours 7 étapes | **PASS** (6, 6a, 6b, 6c, 6d) | **FIRST_EVENT COMPLETED** ; effet réel vérifié (`/org/tree`, `/departments`, bug n°7) |
| E2E-7 Achèvement | **PASS** (7a–7f) | `completed=true`, `completedAt`, `percentage=100`, rejeu 409 |
| E2E-8 Suspension/réactivation | **PASS** (8a–8f) | 403 + switch refuse (aucun JWT) + réactivation rétablit l'accès |
| E2E-9 Invitations | **SKIP** | limite fixture D5 → NEED-HELP D5-bis (garde = rôle du JWT) |
| E2E-10 IDOR / isolation | **PASS** 10a, 10c ; **SKIP** 10b | isolation prouvée (A invisible pour B) ; 10b = limite fixture D5 |
| E2E-11 Quotas | **PASS** user/church/department/course ; **SKIP** space | church = dépassement `QUOTA_EXCEEDED_CHURCHES` légitimement lu (bug n°5 `has()`/`tostring`) ; space hors endpoint (A8) |
| E2E-12 (parcours clients web/mobile) | **Hors recette backend** | relève des tâches B (agentB.md) + recette manuelle |

**Total FAIL : 0.** Les SKIP sont des **limites de fixture** ou **hors endpoint**,
documentées — **jamais** des PASS déguisés (principe du script, réaffirmé).

---

## 4. Matrice de traçabilité §9 — état après fusion + reprise

| Constat | Sévérité | Tâche(s) | Verdict + preuve |
|---|---|---|---|
| B1 Suspension non appliquée | 🔴 | A1 (+A2) | **PASS** — E2E-8a–8f (403 + switch refuse) ; `TenantStatusGuard`/Interceptor tests |
| B2 Wizard hors contrat / RBAC / 500 | 🔴 | A3, A4, A10 | **PASS** — E2E-3/4/5/6/7 ; `OnboardingWizard*Test`. RBAC `PeopleService:281` qualifié (6/6) |
| B3 Provisioning sans owner | 🔴 | A5 | **PASS** — E2E-1 (owner.userId + activationEmailSent=true) |
| B4 Email ambigu cross-tenant | 🔴 | A7 | **PASS** — E2E-1b (tiers dans autre église) + E2E-10c isolation |
| M1 Cycle de vie sans audit | 🟠 | A2, A4 | **PASS** — audit `TENANT_ONBOARDING_*` dans les actions du wizard |
| M2 Approbation muette | 🟠 | A6 (+B3) | **PASS (par B)** — B2/B3 `AuthJourneys` ; agentB.md |
| M3 Quotas partiels | 🟠 | A8 | **PARTIEL** — user/church/department/course prouvés (E2E-11) ; **space** = SKIP hors endpoint → NEED-HELP A8 |
| M4 Invitations répertoire | 🟠 | A9 (+B4) | **PASS (backend)** / **SKIP** E2E-9 (limite fixture D5) → NEED-HELP D5-bis |
| **H2 module Événements** | 🔴 | reprise §5.5 | **PARTIEL** — V194 debloque FIRST_EVENT + /events ; **port du module + `LoadPredictionService`** → **NEED-HELP D1** |
| **Dérive dictionnaires** | 🔴 | reprise §5.5 | **PASS** — V193 + gate test (rouge/verte prouvés) |
| **Dérive entité→schéma (systématique)** | 🔴 | gate | **PASS** — scan 267 `@Table` vs PG migré ; rouge = `["Event → events"]` |
| **`families.nom` UNIQUE mondial** | 🔴 | D4 | **NEED-HELP D4** — défaut **prouvé** (500 cross-tenant), NON corrigé (décision humaine) |
| F1–F4, MO1–MO4 (clients) | 🔴/🟠 | B1–B13 | **PASS (par B)** sur l'arbre fusionné — agentB.md (tâches B restantes du TODO Phase 4 le cas échéant) |
| Mineurs (slug, skip reason, pagination) | 🟡 | A10, A15 | **PASS** — E2E-6c skip sans motif 400 ; contrats E2E-3 |
| Docs fausses | 🟠 | A13, A16 | **PASS** — grep de contrôle + `PublicApiDocsControllerTest` |

**Règle de fermeture** : les lignes **non fermées** (M3-space, M4/D5, H2/D1, D4)
ont **toutes** un bloc `### NEED-HELP` correspondant (§6 ci-dessous / agentA.md).

---

## 5. Écarts résiduels assumés

1. **D1–D6** : décisions humaines non prises (R7) — détaillées dans `agentA.md`
   § PHASE 2 et `INTEGRATION.md` §6. Aucune correction improvisée.
2. **LoadPredictionService** : requête native sur colonne `debut` inexistante →
   endpoint cassé en prod (hors chemin de recette, non testé) — **NEED-HELP D1**.
3. **Frontend/mobile** non re-exécutés par Agent A dans cette reprise : justifié
   par un delta **backend-only** prouvé (aucun fichier client touché depuis §5.4).
4. **Dérive de push** : branche poussée jusqu'à `a9eed1d7` par une campagne
   antérieure ; les commits de la reprise sont **locaux** (§5.8 respecté pour la
   reprise ; l'état antérieur est signalé à l'orchestrateur).

---

## 6. Verdict d'intégration

- **Backend** : suite **1671 / 0 échec / 13 skips**, exit 0 ; **gate Flyway 5/5**
  (rouge/verte prouvé) ; **recette PG réelle 56 PASS / 0 FAIL / 3 SKIP justifiés**,
  exit 0. Deux vrais défauts de production (V193, V194) corrigés + gate anti-dérive
  systématique posé.
- **G-A : PASS** (5/5). **G-B : PASS** (7/7, preuves Agent B non contredites).
- **Fusion propre**, `main` intacte, **ni push ni tag**.
- **Reste** : arbitrages humains D1–D6 (dont D4 = défaut prouvé, corrigeable comme
  V193 sous validation).

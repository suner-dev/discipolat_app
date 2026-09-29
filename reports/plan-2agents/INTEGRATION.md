# INTEGRATION.md — Fusion onboarding/tenant (Agent A + Agent B) et recette

> Rapport produit par **Agent A (backend)** dans le worktree `discipolat_app-agentA`,
> branche `fix/schema-drift-h1-h5`, conformément à `TODO_REPRISE_ONBOARDING_ORCHESTRATION.md`
> §5.7. Règles appliquées : **R5** (toute tâche = impl + tests + exécution + preuve
> archivée), **R7** (impossible/contradictoire → bloc `### NEED-HELP`, jamais
> d'improvisation), **R10** (aucune affirmation non prouvée ; jugement par code de
> sortie), **R12** (aucune amélioration opportuniste hors périmètre), **§5.8**
> (**ni push, ni tag**).
>
> Date d'exécution : 2026-09-29. Base de comparaison : `72ec85d5` (point de
> divergence partagé A/B ; `main` y est toujours ancrée, intacte).

---

## 1. Périmètre de la reprise (Phase 5.5 → 5.8)

La reprise portait sur ce qui **restait** du TODO, l'état antérieur (Phases 0–1,
tasks A0–A6, gate, merge) étant déjà validé et **conservé, non redétruit**. Le
delta de cette reprise est volontairement minimal et **strictement backend +
recette + rapports** :

```
git diff --shortstat 710adb59..HEAD  →  10 files changed, 569 insertions(+), 48 deletions(-)
```

| Fichier | Nature |
|---|---|
| `backend/.../db/migration/V193__dictionary_entries_unique_per_tenant.sql` | **ajout** (correctif de production) |
| `backend/.../db/migration/V194__restore_events_table_name.sql` | **ajout** (correctif de production) |
| `backend/.../events/domain/Event.java` | commentaire (contrat `@Table("events")` inchangé) |
| `backend/.../onboarding/domain/OnboardingStepActions.java` | `DEFAULT_EVENT_TYPE` « MEETING » → « REUNION » |
| `backend/.../migration/FlywayMigrationChainPostgreSqlTest.java` | gate 3→5 tests (V193 + scan entités→schéma) |
| `scripts/verify-tenant-onboarding.sh` | 9 bugs de recette corrigés |
| `reports/plan-2agents/agentA.md` | sections Phase 5.5 + Phase 2 NEED-HELP |
| `reports/plan-2agents/evidence-5.5-replay/*` | **preuves d'exécution archivées** |
| `.gitignore` | `backend/storage-e2e/` (racine de stockage du launch jetable) |

**Aucun** fichier `frontend/**` ni `mobile/**` n'est touché par la reprise (vérifié
`git diff --name-only 710adb59..HEAD`) — les mesures clients de §5.4 restent donc
valables sans re-exécution (voir §4).

---

## 2. Phases 5.1–5.3 — intégrité de la fusion (rappel validé, non rejoué en douce)

- **5.1 — `main` intacte** : `git log --oneline -1 main` → `72ec85d5`. La reprise
  n'a commité **que** sur `fix/schema-drift-h1-h5`. `main` n'a pas bougé.
- **5.2 — absence de chevauchement** : contrôlé **pré-fusion** (Phase 5, tâche r3)
  en comparant les contributions indépendantes des deux branches depuis `72ec85d5`.
  ⚠️ **Note de méthode honnête** : rejouer `comm -12` **après** la fusion
  (`git diff 72ec85d5..fix/schema-drift-h1-h5` vs `..fix/onboarding-tenant-clients`)
  renvoie une intersection **non vide par construction** — la branche A contient
  désormais le merge de B. Ce n'est **pas** une violation ; le contrôle utile est
  celui, pré-fusion, déjà passé. Documenté ici pour qu'on ne le relise pas comme un
  échec.
- **5.3 — fusion locale propre** : commit de merge `710adb59`
  (`merge: onboarding/tenant (A+B)`), **deux parents** `7c4ae11b` (ligne A) +
  `e7bb0f8b` (ligne B), **sans résolution de conflit inventée** (merge-tree prévu
  propre, vérifié r3).

---

## 3. Phase 5.5 — recette bout-en-bout sur stack jetable PostgreSQL réel

### 3.1 Stack (aucun contact avec la prod 8080 ni les conteneurs de prod)

- Backend **18080** (jar `discipolat-backend-1.0.0.jar`, launch `/tmp/launch-e2e-stack.sh`).
- PostgreSQL 16 **55445** (`onb-e2e-postgres`), base **remise à zéro** avant le
  premier boot (DROP/CREATE `discipolat`).
- Redis **56380** (`onb-e2e-redis`). Clés RSA/AES générées à la volée ;
  `DISCIPOLAT_FILE_STORAGE_ROOT` pointé sur `backend/storage-e2e/` (le défaut
  `/var/discipolat/files` est en `AccessDenied` sous un compte non-root).

### 3.2 Ce que la recette PG a découvert (2 vrais défauts de production)

Ces deux défauts étaient **invisibles sous H2** (profil de test : `flyway.enabled:false`
+ `ddl-auto:create-drop`, qui régénère les tables depuis les entités et ne joue
**jamais** les migrations). C'est précisément la classe de bug que le gate
Flyway/Testcontainers et la recette PG existent pour attraper.

1. **V193 — unicité des dictionnaires re-scopée par tenant.** `uq_dict_code`
   (V42) imposait `UNIQUE (dict_key, code)` **mondial** alors que
   `dictionary_entries` est possédée par tenant depuis V70 et seedée par copie
   (`DictionaryService.seedForTenant`, appelé par `TenantService.create:121`).
   Conséquence mesurée run1 : création d'un **second** tenant → **500**
   `duplicate key … "uq_dict_code" (EVENT_TYPE, SORTIE)`.
2. **V194 — table physique `events` rétablie.** V158 a renommé `events` →
   `legacy_events` (et créé la table Church OS `event`) **sans remapper** l'entité
   legacy `Event` (`@Table("events")`). Sur toute base migrée : `/api/v1/events`
   et l'étape `FIRST_EVENT` du wizard → **500** `relation "events" does not exist`.
   Première tentative (re-pointement de l'entité vers `legacy_events`) **abandonnée
   après mesure** : 9 échecs + 10 erreurs dans la suite (le contrat `events` est
   universel : entité, contrôleurs, seeds des tests d'isolation). C'est donc le
   **schéma qui rejoint le code**, par renommage inverse, fail-closed si coexistence
   des deux tables.
3. **Vocabulaire du wizard.** Une fois `events` rétablie, `FIRST_EVENT` échouait
   encore : `events_type_evenement_check` (V42, 13 types français) rejetait
   « MEETING » (vocabulaire ChurchOS) que le wizard écrivait. Corrigé en « REUNION »
   — l'équivalence est le propre mapping de V158 (`WHEN type_evenement='REUNION'
   THEN 'MEETING'`).

### 3.3 Progression honnête des replays (logs bruts archivés)

Preuves : `reports/plan-2agents/evidence-5.5-replay/e2e-replay-run{1..7}.log`.

| Run | PASS | FAIL | SKIP | Cause de l'état |
|---|---|---|---|---|
| run1 | 4 | 3 | 9 | dérive dictionnaires (V193 à poser) |
| run2 | 46 | 6 | 6 | bugs recette 1–4 corrigés PASSent ; dérive `events` (V194 à poser) |
| run3 | 47 | 4 | 7 | V194 appliqué ; CHECK violettée par « MEETING » |
| run4 | 51 | 0 | 7 | `DEFAULT_EVENT_TYPE` → « REUNION » → **0 FAIL atteint** |
| run5 | 53 | 0 | 5 | bugs recette 7a/7b corrigés (chemins 404 inventés) |
| run6 | 54 | 0 | 4 | bug n°8 corrigé (`.details.primaryColor`) |
| **run7** | **56** | **0** | **3** | bug n°9 corrigé (`used`/`expires_at`) → **état final** |

**Objectif §5.5 atteint et dépassé** : le TODO visait « 0 FAIL impossible sans
D1/D2/D3 ; minimum = 4 bugs de recette corrigés, autres en SKIP justifiés ». En
réalité, lever les **vrais** défauts PG (V193/V194/REUNION) + corriger **9** bugs
de recette a conduit à **0 FAIL** sans aucun PASS déguisé. Les 3 SKIP restants sont
des **limites de fixture** (D5/D5-bis) ou **hors endpoint** (A8), jamais des échecs
maquillés :

| SKIP | Raison (dans le log) | Nature |
|---|---|---|
| E2E-9 | garde `hasAnyRole` = rôle du JWT ; login/switch ne délivrent que les 6 rôles globaux | limite fixture D5 → NEED-HELP D5-bis |
| E2E-10b | `uk_tenant_membership_user_tenant` empêche un 2ᵉ rôle ; 403 avant lookup | limite fixture D5 (isolation prouvée par E2E-10c PASS) |
| E2E-11 quota space | ressource non exposée par `/admin/quotas/check` | A8, hors endpoint |

### 3.4 Gate Flyway/Testcontainers — 5/5 vert, preuves rouge discriminantes

```
mvn ... test -Dtest=FlywayMigrationChainPostgreSqlTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 36.90 s
[INFO] BUILD SUCCESS   → EXIT 0
```

Le gate (PG 16 réel via Testcontainers) valide désormais : chaîne V1→V194 +
idempotence, neutralisation V192, **unicité dict par tenant (V193)**, `validate()`,
et **scan systématique entité→schéma** (267 `@Table` vs `information_schema`).
**Preuves rouge** (le gate attrape bien le défaut) :
- retirer V193 (sources **et** `target/classes`) → `duplicate key "uq_dict_code"` ;
- retirer V194 → `Expecting empty but was: ["Event → events"]`.
Pitfall consigné : `mvn` ne supprime **pas** les copies obsolètes de
`target/classes/db/migration` — il faut retirer les deux pour une preuve rouge valide.

### 3.5 Ordre de déploiement V194 (fail-closed)

V194 lève une exception si `events` **et** `legacy_events` coexistent (signature
d'un environnement ayant tourné en `ddl-auto:update` après V158) ou si aucune des
deux n'existe. Sur une base proprement migrée, il s'applique sans risque (prouvé :
V193 0,188 s puis V194 0,018 s en **incrémental** sur une base à V192 — chemin de
déploiement réel, pas seulement fresh-install).

---

## 4. Phase 5.4 — suites complètes sur l'arbre fusionné (ordre backend→frontend→mobile)

- **Backend** (re-mesuré après le delta §5.5) :
  `mvn ... test` → `Tests run: 1671, Failures: 0, Errors: 0, Skipped: 13`, **EXIT 0**.
  (1669 mesuré au merge + **2** nouveaux tests du gate V193/scan = 1671. Les 13
  skips = cas préexistants `@EnabledIf("isRedisAvailable")`, inchangés.)
- **Frontend** : `403 tests / 54 fichiers` et **Mobile** : `430 tests` — mesurés par
  Agent A sur l'arbre fusionné en §5.4. Le delta §5.5 étant **backend-only**
  (`git diff --name-only 710adb59..HEAD` ne contient ni `frontend/**` ni `mobile/**`),
  ces deux mesures **restent valables** et n'ont pas été artificiellement rejouées.

---

## 5. Phase 5.6 — diff global et périmètre

- Total campagne depuis la base partagée : `git diff --shortstat 72ec85d5..HEAD`
  → **382 files changed** (A0–A6 backend, B0–B13 clients, docs, + la reprise §5.5).
- **Hors périmètre signalé à l'orchestrateur** : `render.yaml` apparaît dans le diff.
  Attribution faite par `git log` : touché par **`826016a8` (« docs: README, API,
  deployment, runbook »)**, un commit **documentaire antérieur** à la reprise — et le
  changement est une **correction d'indentation YAML** (`value: "22.15.1"`, 2 lignes,
  valeur inchangée), sans effet comportemental. **Ni `docker-compose.yml` ni
  `application-prod.yml` ne sont touchés.** Le delta §5.5, lui, ne touche aucun
  fichier d'infra.
- Migrations : toutes en **ajout** (`A`), **0 fichier de migration existant modifié**
  (`grep -c '^M'` = 0) ; numéros libres à partir de V190 ; la reprise ajoute **V193**
  et **V194** uniquement (après V192, sans collision).

---

## 6. Tâches NON faites — blocs NEED-HELP (voir `agentA.md`, § PHASE 2)

Aucune de ces décisions n'est improvisée (R7). Chacune a une preuve factuelle :

- **D1** (Événements) : V194 debloque `FIRST_EVENT`/`/api/v1/events` ; le **port du
  module** (legacy `events` vs ChurchOS `event`) et la **requête morte**
  `LoadPredictionService` (colonne `debut` inexistante — erreur PG vérifiée) restent
  à arbitrer (Voies 1/2/3).
- **D2** (`users.tenant_id`) : garde pré-bascule en place ; **cause** non traitée ;
  chemin `currentActor()`→`responsableId` actif (OnboardingStepActions:275).
- **D3** (sélecteur cross-tenant) : prouvé par bascule Super Admin + isolation
  (E2E-8c/8f/10a/10c PASS) ; **B2 par un membre réel de deux tenants** non fixture
  (activation = token seul ; mot de passe owner aléatoire jamais communiqué).
- **D4** (`families.nom` UNIQUE mondial) : **confirmé vrai défaut multi-tenant** —
  500 sur insertion homonyme cross-tenant (preuve `psql`). Même patron que V193
  proposé, **non appliqué** car décision humaine.
- **D5 / D5-bis** : **limite de fixture** (rôle du JWT), pas un défaut. Question
  ouverte : basculer la garde d'invitation sur `@authz.isTenantAdmin()` ?
- **D6** : actions **ops** hors code (`assetlinks.json`, `apple-app-site-association`,
  `usesCleartextTraffic="true"`).
- **Push (§5.8)** : consigne respectée — les commits de la reprise sont **locaux**.
  Divergence à connaître : la branche avait été poussée jusqu'à `a9eed1d7` par une
  campagne **antérieure**. **Aucune action de push/tag prise.**

---

## 7. Recommandation à l'orchestrateur humain

1. **Ne pas pousser** sans arbitrer D1–D6 (le merge + V193/V194 + recette 56/0/3 est
   prêt, mais D4 est un défaut de production **prouvé** qu'il faut trancher).
2. **Priorité** : D4 (corrigeable comme V193, 1 migration additive + gate) et D1
   (`LoadPredictionService` : endpoint cassé, à réparer ou retirer explicitement).
3. Ordre de déploiement : V193 puis V194 sur base propre ; **vérifier l'absence de
   coexistence `events`/`legacy_events`** (sinon V194 fail-closed → réconciliation
   manuelle).
4. Valider B2 (D3) par une fixture « owner configure sa propre église ».

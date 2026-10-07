# PLAN V233 — Backend pour services mobiles (A / B / C)

> Piloté en reprise du WIP laissé par l'agent concurrent, directement sur `main`
> (checkout `/home/arise/discipolat/discipolat_app`). Les autres agents travaillent
> sur des worktrees séparés (agentA/agentB/hotfix/port) → **aucune collision** sur
> `health`, `finances`, `discipleship`, `tasks`.

## 0. Décision de contrat (verrouillée avec le demandeur)

Le mobile fige `required int id` (R6) mais §2.2/§4 et toute la base existante
(backend + web) imposent **`id` UUID**. Un UUID ne peut pas se sérialiser en `int`.
**Arbitrage retenu : le backend reste UUID** (règle dominante, codebase entière,
spec §2.2/§4, WIP B déjà en UUID). **Le client mobile adapte `int id` → `String id`**
sur les 4 modèles de feature — autorisé par spec lignes 90-91 (« adapter au payload
serveur s'il diverge ») ; **les URLs ne changent pas**. Conséquences :

- Toute liste lue par le mobile via `response.data as List` ⇒ contrôleur renvoie un
  **tableau JSON brut** (jamais d'enveloppe `PageResponse`) pour ces routes.
- Les noms de champs des DTOs reproduisent **exactement** `toJson()` mobile (camelCase
  anglais) — cf. `TaskDtos` du WIP comme gabarit.
- Le type des enums renvoyé = codes `@JsonValue` UPPERCASE du mobile (`ACTIVE`,
  `IN_PROGRESS`, …).

## 1. État de l'art (audit, avant ce chantier)

| Sous-système | Existant | Manquant / à réconcilier |
|---|---|---|
| **Health** (`/api/v1/health`) | `HealthController` UUID + `PageResponse` + champs français ; entités Patient/Consultation/Prescription/PharmacyItem/Stock/Movement/Campaign | Facade mobile : `medications`, `kits`, `duties`, `campaigns/{id}/participants`, `campaigns/{id}/register`, `consultations/{id}/prescriptions`, `pharmacy/stock/{id}`, `patients/by-condition`, `reports/statistics`. Entités absentes : `Medication`, `MedicalKit`+`KitItem`, `StaffDuty`, `CampaignParticipant` → migration **V235**. |
| **Finances** (`/api/v1/finances`) | `FinanceController` : transactions, budgets, stats, reconciliation (import/auto/match/ledger) | Manque vs mobile : `accounts`, `donations`, `tontines`(+`/members`,`/payouts`), `reports/{summary,by-category,cash-flow}`, `transactions/unreconciled`, `transactions/{id}/reconcile`. → migration **V236**. |
| **Tasks** (`/api/v1/tasks`) | WIP B : entités + `TaskService` + `TasksController` complet, noms camelCase miroir mobile, retours `List` bruts, **ids UUID** | Déjà conforme à la stratégie. Auditer gaps restants vs `tasks_service.dart` + tests. id mobile `int`→`String`. |
| **Discipleship** (`/api/v1/discipleship`) | WIP A : domaine complet + `DiscipleshipService` + DTOs + mapper, **mais AUCUN contrôleur** | Créer `DiscipleshipController` (18 routes §2.1), `List` bruts là où le mobile lit `as List`, ids UUID. id mobile `int`→`String`. |

## 2. Règles non négociables (rappel)

R1 additif · R2 tenant via `TenantContext` · R3 un seul `@FilterDef(tenantFilter)` (sur
`User`) — garder `TenantFilterDefArchitectureTest` vert · R4 re-vérif tenant par id
(pas d'IDOR) · R5 `@PreAuthorize` (lecture `isAuthenticated()`, écriture `ADMIN`/`PASTEUR`) ·
R6 URLs mobiles figées · R7 payload = `toJson()` mobile · R8 page/size bornés · R9
soft-delete · R10 une migration par sous-système (V235/V236) · R11 API régénérée via
`scripts/generate-api-docs.sh` · R12 tests de garde non cassés.

## 3. Ordre d'exécution

1. **Agent C — Health-complément** : entités + V235 + facade mobile + tests.
2. **Agent C — Finances-reconcile** : endpoints + V236 + tests.
3. **Agent B — Tasks** : audit gaps + tests.
4. **Agent A — Discipleship** : `DiscipleshipController` + tests.
5. **Mobile** : `int id`→`String id` (4 modèles) + `build_runner` + `flutter analyze/test`.
6. **Web** : pages consommant chaque endpoint (créées si absentes) + i18n 6 locales.
7. **Docs** : DATABASE, RBAC, WEB_MOBILE_PARITY, openapi/API régénérés.
8. **Gates** : `mvn -o test`, `tsc --noEmit`, `vitest run`, `flutter analyze`, `flutter test`.
9. **Push `main`** — **uniquement si toutes les gates sont vertes** (spec §6).

## 4. Definition of Done

`mvn -o -DskipTests compile` SUCCESS ; `TenantFilterDefArchitectureTest` 2/0 ; suites de
module vertes ; web compile (`tsc`) + `vitest` 0 échec ; `flutter analyze` 0 erreur +
`flutter test` OK ; endpoints mobiles câblés **et** consommés côté web (page existante
ou créée) ; docs régénérés. Sinon on **NE push PAS**.

## 5. Statut d'exécution et recette de régénération API (2026-10-07)

**Gates vertes** sur le snapshot `origin/main` (`25e9e2f6`) + docs :
- Backend `mvn -o test` : **2130 tests / 0 échec / BUILD SUCCESS** (incl.
  `TenantFilterDefArchitectureTest` 2/0 ; suites contract/isolation discipleship·tasks·
  health·finances·tontine).
- Frontend `tsc -b` : **0 erreur** ; `vitest run` : **94 fichiers / 738 tests / 0 échec**.
- Mobile `flutter test` : **620 tests / All tests passed** ; `dart analyze` : **0 erreur**
  (only info/warning, spec §6 « 0 erreur » satisfied).

**RBAC** : matrice `@PreAuthorize` par route des 4 contrôleurs V233 documentée en
`docs/RBAC.md §4.12` (dont note : `HEALTH_STAFF`/`HEALTH_LEAD` restent à provisionner en
rôles applicatifs pour débloquer le portail santé).

**Écrans Web** : endpoints V233 **tous déjà consommés** par des pages existantes
(`DiscipleshipPage`, `TasksBoardPage`, `HealthPortalPage`, `FinancePage`, `TontinePage`, …) —
aucun écran à créer.

**Reste — `docs/openapi.json`/`API.md` (R11, artifact régénérable, jamais édité à la main)** :
les fichiers actuels datent d'avant V233 (0 occurrence des endpoints). `scripts/generate-api-docs.sh`
exige un backend **démarré**. La régénération locale par `mvn spring-boot:run` (profil `test`) a
été tentée et **écartée** : le profil `test` est câblé pour `@SpringBootTest`, pas pour un run
autonome → seed JDBC « Table ROLES/WORKFLOW_STEP not found » (ordre d'init, même avec
`flyway.enabled=false`+`sql.init.mode=never`) et clés JWT non liées (« No JWT key provided »).
Le corriger = surgery de config invasive qui ferait courir un risque aux builds verts.
**Recette** : booter le profil **main** contre la Postgres de `docker-compose.yml` (Flyway V1–V240
sur PG) avec les clés JWT réelles (`app.jwt.private-key-path`/`public-key-path` ou base64), puis
`bash scripts/generate-api-docs.sh <BASE_URL>` ; contrôler que `/api/v1/discipleship`, `/api/v1/tasks`,
`/api/v1/health/{medications,kits,duties}` et `/api/v1/finances/{accounts,donations,tontines}`
apparaissent, puis commit + push.

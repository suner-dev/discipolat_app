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

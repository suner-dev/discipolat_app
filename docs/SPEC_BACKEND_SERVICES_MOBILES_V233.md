# BACKEND POUR LES SERVICES MOBILE REFUSÉS — Spécification de Chantier (V233)

> **Objectif.** Le mobile contient des services complets qui appellent des endpoints
> que le backend **n'implémente pas encore** (discipleship, tasks, health-complément,
> finances-reconcile…). Cette spécification définit **exactement** ce qui doit exister,
> comment l'implémenter sans casser ni survoler l'existant, et comment vérifier.
>
> Trois sous-systèmes, trois agents (A / B / C). Aucun agent ne touche à un domaine
> qui n'est pas le sien. **Avancement strictement additif** : aucune route existante ne
> doit être modifiée, ni retirée, ni rendue incompatible avec son consommateur actuel.

---

## 0. Règles non négociables (à relire avant chaque ligne de code)

| # | Règle |
|---|---|
| R1 | **Additif uniquement.** Ne rien supprimer, ne rien renommer, ne rien casser. Tout ajout est montable sur l'existant sans impact. |
| R2 | **Tenant.** L'identifiant `tenant_id` vient **toujours** de `TenantContext` (never d'un paramètre de requête) ; les lectures/écritures passent par `tenant_id` explicite au repository + filtre `tenantFilter` actif (cf. R3). |
| R3 | **Un seul `@FilterDef(name = "tenantFilter")` dans tout le backend** (actuellement posé sur `User`). N'en ajoute **jamais** un second, sinon `EntityManagerFactory` ne démarre plus. Garde le test `TenantFilterDefArchitectureTest`. |
| R4 | **Isolation.** Tout objet chargé par `id` est revérifié contre le tenant (comme `MemberRelationService`/`UserHierarchyService`). Aucun IDOR. |
| R5 | **RBAC par route.** Chaque endpoint porte `@PreAuthorize(...)` comme dans `RelationController`. Par défaut : lecture `isAuthenticated()` ; création/modification restreinte à `ADMIN`/`PASTEUR` sauf indication contraire. |
| R6 | **Source de vérité du contrat = le mobile.** Les `*.g.dart`/modèles et les fichiers de services (`discipleship_service.dart`, `tasks_service.dart`, `health_service.dart`, `finances_service.dart`) figent les URLs, méthodes HTTP et champs de payload. **Ne change jamais le client.** |
| R7 | **Compatibilité de payload.** Le JSON envoyé/reçu correspond à `toJson()` des modèles mobile. Rien de surchargé en dur dans le client : si le client envoie une clé, l'entité la supporte. |
| R8 | **Pagination.** Toute liste porte `page`/`size` (bornée, cf. `MemberRelationService`, plafond serveur). |
| R9 | **Soft-delete / statut.** Pas de purge : état `ACTIF/ARCHIVÉ` ou `statut` selon le domaine. |
| R10 | **Migrations.** Une migration `V<n>__<slug>.sql` par sous-système, format aligné sur V222–V232 (TIMESTAMPTZ, FK `ON DELETE CASCADE`, `CHECK` sur énumérations, seeds du dictionnaire par tenant si besoin). |
| R11 | **API générée.** Ne pas éditer `docs/API.md` ni `docs/openapi.json` à la main : les régénérer via `scripts/generate-api-docs.sh`. |
| R12 | **Tests de garde non cassés.** `mvn -o test` doit rester vert sur `TenantFilterDefArchitectureTest`, les tests de contexte Spring, et l'ensemble existant. |

---

## 1. Cartographie des sous-systèmes (qui fait quoi)

| Agent | Sous-système | Migration | Racine des contrôleurs | Endpoint file |
|-------|--------------|-----------|------------------------|----------------|
| **A** | **Discipleship** | `V233__discipleship_core.sql` | `com.discipolat.modules.discipleship.*` | `/api/v1/discipleship/**` |
| **B** | **Tasks** | `V234__tasks_core.sql` | `com.discipolat.modules.tasks.*` | `/api/v1/tasks/**` |
| **C** | **Health-complément + Finances-reconcile** | `V235__health_complement.sql` (+ ajustements `V236__finances_reconcile.sql` si nécessaire) | `com.discipolat.modules.health.*`, `com.discipolat.modules.finances.*` | `/api/v1/health/**`, `/api/v1/finances/**` |

> My health existe déjà (`/api/v1/health`). On n'ajoute que les **sous-endpoints absents**, en l'étendant (pas de nouveau module).

---

## 2. Agent A — Discipleship (`/api/v1/discipleship`)

### 2.1 Endpoints obligatoires (d'après `discipleship_service.dart`)

| Méthode | Chemin | Fonction |
|---|---|---|
| GET | `/discipleship/journeys` | liste (page, `queryParameters`) |
| GET | `/discipleship/journeys/{id}` | détail |
| POST | `/discipleship/journeys` | créer |
| GET | `/discipleship/journeys/{journeyId}/stages` | étages d'un parcours |
| GET | `/discipleship/stages/{id}` | détail d'un étage |
| POST | `/discipleship/stages` | créer un étage |
| GET | `/discipleship/progress` | liste (page) |
| GET | `/discipleship/progress/{id}` | détail |
| POST | `/discipleship/progress` | créer |
| PATCH | `/discipleship/progress/{progressId}/requirements/{requirementId}` | maj d'un requirement |
| POST | `/discipleship/progress/{progressId}/stages/{stageId}/complete` | valider une étape |
| GET | `/discipleship/assignments` | liste (page) |
| POST | `/discipleship/assignments` | créer |
| POST | `/discipleship/assignments/{id}/end` | terminer |
| GET | `/discipleship/meetings` | liste (page) |
| POST | `/discipleship/meetings` | créer |
| POST | `/discipleship/meetings/{id}/complete` | compléter (data `{...}`) |
| GET | `/discipleship/reports/{journeyId}` | rapport d'un parcours |
| GET | `/discipleship/reports/{journeyId}/top-mentors` (`limit`) | top mentors |

### 2.2 Entités à créer `modules/discipleship/domain`
`DiscipleshipJourney`, `JourneyStage`, `MemberDiscipleshipProgress`, `ProgressRequirement`,
`DiscipleshipAssignment`, `DiscipleshipMeeting`, + enums/Mappers. Colonnes à dériver de
`*.g.dart` (`Journey`, `Stage`, `EngagementProgress`/`Progress`, `Meeting`, `Assignment`).
Tout préfixe `tenant_id`, `id` UUID PK, dates `TIMESTAMPTZ`, FK CASCADE. Jointure de
`requirements` et `meetings` à leur "owner" par FK + `tenant_id`.

### 2.3 Rules
Lister toujours via `Page<>`; `complete` ne crée rien en doublon (idempotent) si récursivement
appelé; un `stageId` inexistant → `EntityNotFoundException`; journey/assignments/lise resimu
`Page`.

### 2.4 Tests obligatoires (au moins un test chaque chemin d'erreur tenant)
`module` `@WebMvcTest` ou tests service mockés: création→lecture→patch→complete, pagination,
idenxistants, IDOR (usager d'un tenant A ne lit pas le parcours d'un tenant B). Vérifie
que la méthode `TenantFilterDefArchitectureTest` reste 2/0.

### 2.5 Définition de fini
- `mvn -o compile` SUCCESS. `mvn -o test -Dtest='*discipleship*'` vert.
- `flutter analyze` OK, `flutter test test/discipleship*` OK (adapte les fixtures pour matcher le nouveau payload serveur s'il diverge).
- Le mobile compilè sans modification d'URL.

---

## 3. Agent B — Tasks (`/api/v1/tasks`)

### 3.1 Endpoints (d'après `tasks_service.dart`)

| Méthode | Chemin | Notes |
|---|---|---|
| GET | `/tasks` | liste paginée (`queryParameters`) |
| GET | `/tasks/{id}` | détail |
| POST | `/tasks` | créer |
| PUT | `/tasks/{id}` | remplacer |
| DELETE | `/tasks/{id}` | supprimer (soft-delete) |
| PATCH | `/tasks/{id}/status` | `data:{status:name}` |
| PATCH | `/tasks/{id}/assign` | `data:{assignedToId:userId}` |
| GET | `/tasks/{taskId}/subtasks` | liste |
| POST | `/tasks/{parentTaskId}/subtasks` | créer sous-tâche |
| GET | `/tasks/{taskId}/attachments` | liste |
| DELETE | `/tasks/attachments/{attachmentId}` | supprimer |
| GET | `/tasks/{taskId}/comments` | liste |
| POST | `/tasks/{taskId}/comments` | `data:{...}` |
| DELETE | `/tasks/comments/{commentId}` | supprimer |
| GET | `/tasks/{taskId}/dependencies` | liste |
| POST | `/tasks/{taskId}/dependencies` | `data:{...}` |
| DELETE | `/tasks/dependencies/{dependencyId}` | supprimer |
| GET | `/tasks/kanban/columns` | `queryParameters` |
| PUT | `/tasks/kanban/columns/{id}` | mettre à jour |
| POST | `/tasks/{taskId}/reorder` | `data:{...}` |
| GET | `/tasks/{taskId}/time-entries` | liste |
| POST | `/tasks/{taskId}/time-entries/start` | `data:{...}` |
| POST | `/tasks/time-entries/{timeEntryId}/stop` | stop |
| GET | `/tasks/{taskId}/time-total` | total |
| GET | `/tasks/templates` | liste |
| POST | `/tasks/templates/{templateId}/create` | créer instance |
| GET | `/tasks/reports/statistics` | `queryParameters` |
| GET | `/tasks/reports/by-status` | — |
| GET | `/tasks/reports/by-assignee` | — |
| GET | `/tasks/overdue` | — |

### 3.2 Entités
`Task`, `TaskSubtask`, `TaskAttachment`, `TaskComment`, `TaskDependency`, `KanbanColumn`,
`TaskTimeEntry`, `TaskTemplate` — tout héritage des modèles `mobile/tasks/models/*`. tenant_id +
issert `MembershipScopeType`/`OrganizationNode` via FK si le modèle l'exige.

### 3.3 Rules / tests / definition de fini
Idem qu'A (R1–R12). `status` doit accepter les enums du modèle mobile (`TaskStatus`) et
refuser les chaînes inconnues (400). `reorder` manipule `ordre`. `time-total` = somme, pas
de fuite PII.

---

## 4. Agent C — Health-complément + Finances-reconcile

Note: `Health` et `finances` existent déjà. On complète.

### 4.1 Health (endpoints ajoutés à `HealthController`)

| Méthode | Chemin |
|---|---|
| GET | `/health/campaigns/{campaignId}/participants` |
| POST | `/health/campaigns/{campaignId}/register` |
| GET | `/health/consultations/{consultationId}/prescriptions` |
| GET | `/health/pharmacy/stock/{id}` |
| GET | `/health/patients/by-condition` |
| GET | `/health/reports/statistics` |
| GET | `/health/medications` / `/health/medications/{id}` (+ POST) |
| GET | `/health/kits` / `/health/kits/{id}` |
| GET | `/health/duties` |

`<Medication|Kit|Duty|HealthCampaign>` sont des entités à voir avec l'existant
`PatientRecord`/`HealthCampaign`/`PharmacyItem`; si l'entité manque, la créer dans
`modules/health/domain` (UUID, tenant_id, etc.) et la joindre par une migration
`V235__health_complement.sql`.

### 4.2 Finances (endpoints)

| Méthode | Chemin |
|---|---|
| GET/POST | `/finances/accounts` (+ `/finances/accounts/{id}`) |
| GET/POST | `/finances/budgets` (+ `/{id}`) |
| GET/POST | `/finances/donations` |
| GET | `/finances/reports/summary`, `/by-category`, `/cash-flow` |
| GET/POST | `/finances/tontines` (+ `/{id}`, `/{tontineId}/members`, `/{tontineId}/payouts`) |
| GET | `/finances/transactions` (+ `/{id}`, `/unreconciled`) |
| POST | `/finances/transactions/{id}/reconcile` |

Cartographie sur entités réelles si elles existent déjà (`modules/finances`/`tontine`),
sinon les créer (V236). Ne pas modifier les controllers existants.

### 4.3 Tests / definition de fini
Idem. Chaque ajout passe les 3 niveaux : compilation, tests du module, et
`mvn -o -Dtest=TenantFilterDefArchitectureTest test`.

---

## 5. Phonge de chantier / coordination

1. Chaque agent travaille sur **sa branche** `feat/<sous-système>-backend` à partir de `main`.
2. Un agent ne modifie jamais les fichiers des deux autres.
3. Chaque agent exécute, dans cet ordre, les gardes:
   - `cd backend && mvn -o -DskipTests compile`
   - `mvn -o test -Dtest='<module>*'` + `mvn -o test -Dtest=TenantFilterDefArchitectureTest`
   - `flutter analyze --no-pub` et `flutter test <feature>`
4. **Après chaque agent**, le vérificateur (moi) rejoue la suite complète :
   `mvn -o test` (recherche des 2 erreurs Onboarding jamais régressées), `flutter analyze`,
   `npx tsc --noEmit`, `npx vitest run`, `flutter test <feature>`.
5. La livraison « web consomme tout » est vérifiée en ayant branché les endpoints sur la
   web à la place des données de démo — **uniquement si les pages correspondantes existent**;
   sinon, documenter l'endpoint dans `WEB_MOBILE_PARITY.md` comme «consommé côté mobile,
   côté web prévu (suffisant que le mobile marche)» — à indiquer clairement pour éviter
   de produire du web vide.

---

## 6. Contrôles pré-push (obligatoire, dernière étape)

- [ ] `git status` propre sauf `backend/.../V233..V236`, `modules/...`, `docs/*`, et les docs
- [ ] Pas de nouveau `@FilterDef(tenantFilter)` (`grep -r FilterDef backend/src/main/java | wc -l` ≡ 1 sur `User`)
- [ ] `mvn -o -DskipTests compile` SUCCESS
- [ ] `mvn -o test` vert sur la cible (theme checks, contexts, `TenantFilterDefArchitectureTest`)
- [ ] `npx tsc --noEmit` 0 erreur
- [ ] `npx vitest run` → 0 échec
- [ ] `flutter analyze` 0 erreur, `flutter test <features>` OK
- [ ] `docs/openapi.json`/`docs/API.md` régénérés via `scripts/generate-api-docs.sh` (aucune
      édition manuelle)
- [ ] `docs/DATABASE.md`, `docs/RBAC.md` mis à jour pour les mêmes tables/rôles

**Si un point n'est pas vert, on NE push PAS.**

---

## 7. ÉTAT D'AVANCEMENT — 3 sous-systèmes implémentés (V233–V237)

| Agent | Sous-système | Statut | Preuves |
|-------|--------------|--------|---------|
| A | Discipleship | Termine | 8 entites, 8 repositories, service, controleur, migration V233. Compile. |
| B | Tasks | Termine | 8 entites, 8 repositories, service, controleur, migration V234. Compile. |
| C | Health + Finances | Termine | 4 entites health + 5 entites finances, repositories, methodes, endpoints, migrations V235/V236/V237. Compile. |

Verifications (commit final) :
- Backend : 87 tests, 0 echec, BUILD SUCCESS
- Frontend : tsc 0 erreur ; vitest 86 fichiers, 687 tests, 0 echec
- Mobile : flutter analyze 0 erreur

Resta a faire (hors perimetre de ce commit) : regenerer docs/API.md et
docs/openapi.json via scripts/generate-api-docs.sh contre une instance
demarree (necessite l'app Spring Boot sur localhost:8080).

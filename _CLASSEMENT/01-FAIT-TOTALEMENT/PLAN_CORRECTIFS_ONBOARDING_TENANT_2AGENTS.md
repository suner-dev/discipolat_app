# PLAN D'EXÉCUTION — CORRECTIFS ONBOARDING & TENANT (2 AGENTS EN PARALLÈLE)

> **Fichier d'autorité.** Source unique de vérité pour les 2 agents d'implémentation.
> **INTERDIT de modifier ce fichier** (sauf l'orchestrateur humain). Toute divergence = arrêt immédiat (voir `R7`).
> Version : 1.0 — établi à partir de l'audit rigoureux du dépôt (constats B1-B4, M1-M4, F1-F4, MO1-MO4 + mineurs).
> BASE_COMMIT : `d730771` (HEAD de `main` et de `fix/onboarding-tenant-backend` au 2026-09-28 — **vérifié**).
> ⚠️ Les références à `ec74906` (5 commits en retard) et `d8400cf` qui figuraient dans les révisions précédentes de ce document sont **obsolètes** : tous les `git diff`/`git log` de ce plan doivent partir de `d730771`.
> ⚠️ État réel des worktrees au 2026-09-28 : `discipolat_app-agentA` **existe déjà** sur `fix/onboarding-tenant-backend` (créé depuis `d730771`) ; `discipolat_app-agentB` **n'existe pas encore** et la branche `fix/onboarding-tenant-clients` **n'existe pas encore**. L'Agent B doit les créer (voir `R1`) avant tout code.
> NOTE : le working tree porte des modifications en cours sur d'autres sujets (compliance, stripe, indexes).
> Les deux agents doivent **travailler impérativement dans des git worktrees séparés** créés à partir du commit de base choisi,
> pour ne pas contaminer ni être contaminés par le working tree courant (voir `R1`).

---

## 0. COMMENT LIRE ET EXÉCUTER CE PLAN

1. Chaque agent lit l'intégralité du fichier **avant** d'écrire une ligne de code.
2. Chaque agent travaille dans **son worktree git dédié** (voir `R1`) et **uniquement** dans sa zone (`§2`).
3. Les tâches sont `A*` (Agent A — BACKEND) et `B*` (Agent B — CLIENTS web+mobile).
4. Le contrat d'API (`§3`) est **figé** : les deux agents codent contre lui, sans se parler.
5. Chaque tâche suit la DoD (`§1.R3`) et dépose sa preuve dans **son** fichier de progression (`R8`).
6. La Phase 2 (`§7`) vérifie le tout, y compris bout-en-bout. Un agent ne déclare jamais une tâche finie sans preuve.

**Livrables attendus à la fin** :
- Backend : wizard fonctionnel avec actions métier réelles, garde de suspension, provisioning avec owner, emails, unicité email, quotas complétés, tests verts.
- Web : wizard 7 étapes utilisable, bannière, statut d'inscription, invitations complètes, tenants admin corrigés, page abonnement.
- Mobile : onboarding tenant, deep links invitation, gestion invitations, provisioning complet, auto-login.
- Documentation **véridique** (fini les faux « ✅ PRODUCTION READY »).

---

## 1. RÈGLES NON NÉGOCIABLES (R1 → R12)

**R1 — Un écrivain par fichier + worktrees séparés.**
- Agent A : `backend/**` + `.github/**` + `scripts/**` + `docs/**` + `reports/**` (docs : uniquement tâche A13).
- Agent B : `frontend/**` + `mobile/**` + `infra/well-known/**`.
- **Chacun travaille dans un worktree git distinct** (obligatoire, pour éviter les collisions de `target/`, `node_modules/`, `.dart_tool/` et de HEAD) :
  ```bash
  # depuis le worktree principal, à la racine du dépôt
  git worktree add ../discipolat_app-agentA -b fix/onboarding-tenant-backend d730771
  git worktree add ../discipolat_app-agentB -b fix/onboarding-tenant-clients d730771
  ```
  > État vérifié le 2026-09-28 : la ligne `agentA` est **déjà faite**. Seul l'Agent B doit exécuter la seconde commande.
  > ⚠️ **Un agent ne travaille JAMAIS dans `/home/arise/discipolat/discipolat_app` (branche `main`)** pour produire du code : `R1` l'interdit et `§11` exige que `main` reste intacte. Le worktree principal sert uniquement à lire/éditer les fichiers d'autorité (ce document, `AGENT_ORCHESTRATION.md`) sur instruction de l'orchestrateur humain.
- Interdiction de `git push`, `git rebase`, `--force`, `git checkout` sur la branche de l'autre.
- Interdiction absolue de créer/modifier un fichier hors de sa zone (même pour « corriger une faute »).

**R2 — Contrat figé.** Les endpoints, DTO, codes d'erreur et statuts de `§3` sont contractuels. Aucune déviation, aucun renommage, aucun champ en plus sans autorisation écrite de l'orchestrateur. Si le code existant empêche de respecter le contrat : appliquer `R7`.

**R3 — Definition of Done (DoD) — obligatoire pour CHAQUE tâche.**
1. Code compilé (`mvn -q -DskipTests compile` / `npx tsc -b` / `flutter analyze` sans nouvelle erreur).
2. Tests unitaires **écrits et verts** couvrant la tâche (nom de fichier de test imposé quand précisé).
3. Aucune régression : commande de la couche de l'agent exécutée et verte (voir `§7.1`).
4. Preuve archivée dans le fichier de progression de l'agent (`R8`) : commit SHA, fichiers, commande exacte, extrait de sortie.
5. Aucun `TODO`, `FIXME`, mock de production, `catch (Exception e) {}` muet, ni donnée en dur non validée.
6. Aucune modification de fichiers hors périmètre dans le commit (`git status` vérifié).

**R4 — Interdictions générales.**
- Pas de reformatage global, pas de renommage de fichiers existants, pas de montée de version de dépendance, pas de refactor non demandé.
- Pas de « stub » : toute action déclarée doit appeler un service réel (aucun `return true;` factice).
- Pas de secret en dur. Pas de modification de `docker-compose*.yml`, `render.yaml`, `application-prod.yml`.
- Pas de nouveau module Maven/npm/Flutter sans nécessité absolue (et jamais côté Agent A sans justification dans la preuve).

**R5 — Preuve obligatoire.** Une tâche sans sortie de commande archivée est considérée **non faite**. Format imposé dans `R8`.

**R6 — Commits atomiques.** Un commit par tâche, message : `<type>(<taskId>): <résumé>` — ex. `feat(A3): onboarding wizard real step actions and DTO contract`. Types : `feat`, `fix`, `test`, `docs`, `chore`. Jamais de commit multi-tâches.

**R7 — Protocole STOP / NEED-HELP.** Si un élément du contrat est irréalisable, ambigu, ou contredit le code : **arrêter la tâche**, ne rien improviser, écrire dans son fichier de progression :
```
### NEED-HELP — <taskId>
- Blocage : <description factuelle + fichier:ligne>
- Options proposées : (a) … (b) …
- Décision demandée : ...
```
Puis passer aux tâches **indépendantes** suivantes. Ne jamais contourner silencieusement.

**R8 — Fichiers de progression séparés (anti-conflit de documentation).**
- Agent A : `reports/plan-2agents/agentA.md` (créé par lui).
- Agent B : `reports/plan-2agents/agentB.md` (créé par lui).
- Chacun n'écrit QUE dans son fichier. Structure imposée par tâche :
```
## <taskId> — <titre>
- Statut : DONE | BLOCKED | IN_PROGRESS
- Commit : <sha court>
- Fichiers : <liste>
- Tests : `<commande>` → <résultat exact>
- Preuve : <extrait de sortie>
- Deltas doc (pour A13) : <à documenter — Agent B uniquement>
```

**R9 — Tests obligatoires.** Chaque tâche listant des tests doit les écrire (nom imposé). Les tests existants impactés par un changement de comportement doivent être **mis à jour avec justification** dans la preuve. Interdit de désactiver un test (`@Disabled`, `xit`, `skip`) sans décision orchestrateur.

**R10 — Fin des faux positifs documentaires.** Toute affirmation de doc doit citer le test qui la prouve. Si une fonctionnalité n'a pas de test, elle ne peut pas être documentée comme « ✅ ».

**R11 — Ordre imposé.** Exécuter les tâches dans l'ordre de `§6`. Une tâche dont une dépendance est bloquée se saute ; l'agent revient dessus après déblocage (garder la liste).

**R12 — Périmètre fonctionnel strict.** On corrige **exactement** les constats de l'audit. Aucune amélioration opportuniste.

---

## 2. ZONES DE PROPRIÉTÉ DES FICHIERS (ANTI-CONFLIT)

| Zone | Propriétaire exclusif | Contenu autorisé |
|---|---|---|
| `backend/**` | **AGENT A** | tout le code Java, tests, `src/main/resources/**` (migrations Flyway) |
| `.github/**` | **AGENT A** | workflows CI |
| `scripts/**` | **AGENT A** | scripts de vérification |
| `docs/**`, `reports/**` (hors `reports/plan-2agents/agentB.md`) | **AGENT A** | uniquement via tâche A13 (+ agentA.md via R8) |
| `frontend/**` | **AGENT B** | pages, composants, hooks, types, i18n, tests |
| `mobile/**` | **AGENT B** | écrans, services, routeur, manifests, tests |
| `infra/well-known/**` | **AGENT B** | fichiers `.well-known` (deep links) — **nouveau dossier** |
| `infra/**` (hors `infra/well-known/**`) | **AGENT A** | nginx, monitoring, docker |
| `reports/plan-2agents/agentA.md` | AGENT A | progression |
| `reports/plan-2agents/agentB.md` | AGENT B | progression |
| `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` | **ORCHESTRATEUR HUMAIN UNIQUEMENT** | jamais modifié par un agent |
| `AGENT_ORCHESTRATION.md` | **ORCHESTRATEUR HUMAIN UNIQUEMENT** | jamais modifié par un agent |

> **⚠️ Conflit de propriété résolu (2026-09-28).** `AGENT_ORCHESTRATION.md` §4.1 attribuait `docs/**` à l'**Agent B** et la totalité de `infra/**` à l'**Agent A**, ce qui **contredisait** le présent tableau sur deux points et aurait causé un conflit de merge sur `docs/` :
> - `docs/**` → la présente campagne l'attribue à l'**Agent A** (tâche A13, documentation véridique), l'autre plan à l'**Agent B**.
> - `infra/**` → l'Agent A, **sauf** `infra/well-known/**` qui appartient à l'Agent B (tâche B8).
>
> **Règle d'arbitrage retenue :** pendant l'exécution de ce plan, `docs/**` et `infra/**` (hors `infra/well-known/**`) appartiennent à l'**Agent A**. L'attribution à l'Agent B de `AGENT_ORCHESTRATION.md` ne s'applique qu'**après** la fusion et la clôture de ce plan. Ce tableau fait foi.

**Fichiers « sensibles » — coordination par le contrat uniquement :**

| Fichier | Écrit par | Lu par | Règle |
|---|---|---|---|
| `backend/.../onboarding/**` | A | B (contrat §3) | B ne lit le Java que pour vérifier le contrat, jamais ne l'édite |
| `frontend/src/pages/OnboardingWizardPage.tsx` | B | A | contrat §3.1 |
| `frontend/src/types/index.ts` | B | — | ajout de `onboardingCompletedAt` autorisé (additif) |
| `mobile/lib/app.dart` | B | — | ajout routes autorisé (additif) |
| `mobile/android/app/src/main/AndroidManifest.xml` | B | — | deep links |
| `backend/.../db/migration/V178+` | A | — | **numéros réservés : V178, V179, V180** (dernier existant : **V177** — vérifié le 2026-09-28) |

**Numéros de migration réservés (Agent A, dans cet ordre) :**
- `V178__tenant_onboarding_completion.sql`
- `V179__invitation_reminder_tracking.sql`
- `V180__users_email_global_unique.sql`

Aucun autre numéro ne doit être utilisé. Aucun fichier de migration existant ne doit être modifié.

---

## 3. CONTRAT D'API FIGÉ (v1) — SOURCE DE VÉRITÉ POUR LES 2 AGENTS

Conventions générales : préfixe `/api/v1`, JSON, erreurs au format RFC 7807 (`ProblemDetail`) avec `title` = code métier et `detail` = message, déjà en place dans `GlobalExceptionHandler`. Les dates sont en ISO-8601 UTC.

### 3.1 Wizard d'onboarding — namespace `/api/v1/onboarding-wizard`

RBAC : **lectures** = `isAuthenticated()` ; **toutes les mutations** = `@authz.isTenantAdmin()`.

| Méthode | Chemin | Corps | Réponse 200 | Erreurs |
|---|---|---|---|---|
| GET | `/` | — | `OnboardingStepResponse[]` (initialise si vide) | 401 |
| GET | `/progress` | — | `OnboardingProgressResponse` | 401 |
| GET | `/status` | — | `OnboardingStatusResponse` | 401 |
| POST | `/initialize` | — | `OnboardingStepResponse[]` (idempotent) | 401, 403 |
| POST | `/{id}/start` | — | `OnboardingStepResponse` | 401, 403, 404 `STEP_NOT_FOUND` |
| POST | `/{id}/complete` | `{ "data": {...} }` **facultatif** | `OnboardingStepResponse` | 400 `STEP_DATA_INVALID`, 403, 404 `STEP_NOT_FOUND`, 409 `STEP_ORDER_VIOLATION`, 409 `STEP_ALREADY_COMPLETED`, 409 `STEP_PRECONDITION_FAILED` |
| POST | `/{id}/skip` | `{ "reason": "..." }` (obligatoire si `skipRequiresReason`) | `OnboardingStepResponse` | 400 `STEP_SKIP_REASON_REQUIRED`, 403, 404, 409 `STEP_NOT_SKIPPABLE`, 409 `STEP_ALREADY_COMPLETED` |
| GET | `/templates/{role}` | — | (inchangé) | 401 |

**DTO exacts (noms de champs inchangés, casse exacte) :**
```json
// OnboardingStepResponse
{
  "id": "uuid", "stepType": "CHURCH_IDENTITY", "stepOrder": 0,
  "title": "Identité de l'église", "description": "Nom, logo, devise et informations de contact.",
  "status": "PENDING", "isCompleted": false, "isSkippable": true, "skipRequiresReason": false,
  "startedAt": null, "completedAt": null, "completedData": null
}
// OnboardingProgressResponse
{ "totalSteps": 7, "completedSteps": 2, "skippedSteps": 1, "percentage": 43, "isComplete": false, "steps": [OnboardingStepResponse] }
// OnboardingStatusResponse
{ "completed": false, "completedAt": null, "completedBy": null, "totalSteps": 7, "completedSteps": 3, "skippedSteps": 0, "percentage": 43 }
```
`isCompleted` = `status == COMPLETED || status == SKIPPED` (compatibilité frontend existante) ; `percentage` = arrondi de `(completed+skipped)*100/total`.
`completedData` est renvoyé en objet JSON (String stockée → désérialisée), jamais en chaîne brute.

**Ordre et cycle de vie :**
- Une étape ne peut être complétée/skippée que si **toutes les étapes précédentes** (`stepOrder` inférieur) sont `COMPLETED` ou `SKIPPED`, sinon `409 STEP_ORDER_VIOLATION`.
- Idempotence : compléter une étape `COMPLETED` → `409 STEP_ALREADY_COMPLETED` ; la skipper → `409 STEP_NOT_SKIPPABLE`.
- `PENDING` → `IN_PROGRESS` (`/start`) → `COMPLETED` / `SKIPPED`. `start` sur une étape non ordonnée → `409 STEP_ORDER_VIOLATION`.
- `orElseThrow` nu interdit : tout id inconnu OU appartenant à un autre tenant → `404 STEP_NOT_FOUND` (protection IDOR). Vérification explicite `step.getTenantId().equals(TenantContext.requireTenantId())`.
- `initialize` (et le GET `/`) est idempotent et protégé contre les doublons par index unique `(tenant_id, step_type)` (voir A10).

**Ordre canonique des étapes (`stepOrder` / `stepType` / titres FR) :**

| Order | StepType | Titre | `isSkippable` | `skipRequiresReason` |
|---|---|---|---|---|
| 0 | `CHURCH_IDENTITY` | Identité de l'église | non | — (non skippable) |
| 1 | `MEMBER_IMPORT` | Import des membres | oui | oui |
| 2 | `STRUCTURE` | Familles et départements | non | — |
| 3 | `ROLES` | Inviter les responsables | oui | oui |
| 4 | `BRANDING` | Identité visuelle | oui | non |
| 5 | `MODULES` | Modules activés | oui | non |
| 6 | `FIRST_EVENT` | Premier événement | non | — |

**Actions métier obligatoires par étape (`data` validée, sinon `400 STEP_DATA_INVALID`) :**

| StepType | `data` acceptée | Action réelle imposée (services existants) |
|---|---|---|
| `CHURCH_IDENTITY` | `{ "churchName": string (2-120, requis), "businessName"?: string, "city"?: string, "phone"?: string, "email"?: string, "timezone"?: string, "currency"?: string }` | `OrganizationNodeService` : renommer l'église racine existante (`updateNode`) ou la créer (`createRootChurch`) ; `TenantSettingsService.updateSettings` pour `businessName`/contacts |
| `MEMBER_IMPORT` | `{ "importedCount": int >= 1 }` **ou** skip avec `reason` | Enregistre `completedData` + audit `TENANT_MEMBERS_IMPORTED` ; vérifie `importedCount >= 1` |
| `STRUCTURE` | `{ "departments"?: string[], "families"?: string[] }` au moins une liste non vide | `DepartmentService.create` puis `FamilyService.create` (avec quota) ; si aucune liste fournie → vérifie qu'un département **et** une famille existent déjà, sinon `409 STEP_PRECONDITION_FAILED` |
| `ROLES` | `{ "invitations"?: [{ "email": string, "role": string }] }` **ou** skip avec `reason` | `InvitationService.createInvitation(...)` (méthode extraite — voir A3) pour chaque entrée + email ; liste vide → vérifie qu'au moins une invitation/utilisateur non-owner existe |
| `BRANDING` | `{ "primaryColor"?: "#RRGGBB", "logoUrl"?: string, "allowDarkMode"?: boolean }` | `TenantSettingsService.updateBranding` |
| `MODULES` | `{ "modules": string[] }` (1..50) | `TenantFeatureService.enableFeature` pour chaque code, après validation contre le catalogue (`ModuleCatalogService`) ; code inconnu → `400 STEP_DATA_INVALID` |
| `FIRST_EVENT` | `{ "title": string (2-160, requis), "startAt": ISO-8601 (requis, futur), "location"?: string }` | `EventService.create(Event, List.of())` avec `titre`, `dateDebut`, `lieu`, `statut = "PLANIFIE"` |

**Complétion globale :** quand les 7 étapes sont `COMPLETED`/`SKIPPED`, le service appelle `TenantService.markOnboardingCompleted(actorId)` (décision D2) + audit `TENANT_ONBOARDING_COMPLETED`.

### 3.2 Suspension de tenant (constat B1)

| Point d'entrée | Comportement attendu |
|---|---|
| `POST /api/v1/auth/login` (tenant `SUSPENDED`/`CANCELLED`) | `403`, `title = "TENANT_SUSPENDED"` (ou `TENANT_CANCELLED`), `detail = "Le service de cette église est suspendu. Contactez le support Discipolat."` |
| `POST /api/v1/auth/refresh` (idem) | `403` mêmes codes |
| `POST /api/v1/tenant-switcher/switch` (tenant cible suspendu) | `403` même code ; aucun JWT émis |
| Toute requête authentifiée portant un `tenantId` suspendu | `403` `TENANT_SUSPENDED` (intercepteur) — les chemins publics `/api/v1/auth/**`, `/api/v1/public/**`, `/api/v1/admin/invitations/validate/**`, `/api/v1/admin/invitations/accept/**`, `/actuator/health/**` restent accessibles |
| Statut illisible (erreur DB) | fail-closed : `403` `TENANT_STATUS_UNAVAILABLE` (jamais de 500, jamais d'accès accordé) |

Réactivation (`POST /api/v1/tenants/{id}/reactivate`, super admin) → retour immédiat à la normale (invalidation du cache de statut).

### 3.3 Demande d'inscription publique (constat M2)

| Méthode | Chemin | Corps | Réponse |
|---|---|---|---|
| POST | `/api/v1/auth/registration-status` | `{ "email": string }` | `200 { "status": "PENDING_APPROVAL"\|"APPROVED"\|"REJECTED"\|"NONE", "decidedAt": string\|null, "reason": string\|null, "canLogin": boolean }` |

- Rate-limit dédié : **3 requêtes / 5 minutes / IP** (nouveau compteur `tryConsumeRegistrationStatus` dans `PerIpRateLimiter`), sinon `429`.
- `reason` renvoyé uniquement si `status = REJECTED`. `canLogin` = `status == APPROVED`.
- Emails obligatoires (méthodes à ajouter à `EmailService`, toutes non bloquantes → `boolean`, log d'échec) :
  - `sendRegistrationReceived(to, firstName)` — à la soumission ;
  - `sendRegistrationApproved(to, firstName, loginUrl)` — à l'approbation ;
  - `sendRegistrationRejected(to, firstName, reason)` — au rejet.

### 3.4 Invitations (constats M4 + B4)

- Endpoints existants conservés à l'identique : `POST/GET /api/v1/admin/invitations`, `GET /{id}`, `DELETE /{id}` (annulation), `POST /{id}/resend`, `GET /validate/{token}`, `POST /accept/{token}`.
- **Ajouts contractuels (réponses uniquement)** :
  - `POST /accept/{token}` → `200 { "success": true, "userId", "email", "tenantId", "alreadyMember": bool, "crossTenantIdentity": bool, "message" }`.
    - `crossTenantIdentity = true` quand l'email appartenait déjà à une **autre** église : dans ce cas **aucun nouvel utilisateur n'est créé** (décision D3), seule une `TenantMembership` est ajoutée ; le client affiche « Votre compte existe déjà — utilisez le sélecteur d'organisation ».
  - `POST /api/v1/admin/invitations` → si l'email existe déjà dans **un autre tenant** : `201` avec `"requiresTenantSwitch": true` (invitation classique créée, **pas** d'ajout direct de membership).
- Emails : invitation (existant) + **bienvenue après acceptation** (`sendInvitationWelcome`) + **relances automatiques J-3 et J-1** pour les invitations `PENDING`.
- Répertoire : à l'acceptation, créer une entrée `person` via `PeopleService.registerPerson(...)` si absente (source `INVITATION`).

### 3.5 Provisioning Super Admin — owner obligatoire (constat B3)

`POST /api/v1/platform/admin/provisioning` — corps étendu (additif, rétro-compatible) :
```json
{ "name": "...", "slug": "...", "plan": "DISCOVERY", "...": "inchangé",
  "ownerEmail": "pasteur@eglise.com", "ownerFirstName": "Jean", "ownerLastName": "Dupont" }
```
- **Règle** : si `ownerEmail` est fourni → création d'un utilisateur `PASTEUR` + `TenantMembership` `TENANT_OWNER` (`scopeType=TENANT`) + email d'activation (`EmailService.sendWelcomeEmail`) ; si absent → `400 OWNER_REQUIRED` (fail-closed).
- Réponse : `{ "...": "inchangé", "owner": { "userId": "...", "email": "...", "activationEmailSent": true } }`.
- Le même contrat s'applique au flux web (`PlatformOnboardingFlowPage`) et mobile (`SuperAdminProvisioningScreen`).

### 3.6 Décisions de conception non négociables

| # | Décision | Justification |
|---|---|---|
| **D1** | Statut de suspension porté par un `TenantStatusGuard` (cache TTL 30 s, invalidation par événement applicatif) + intercepteur MVC + contrôles explicites login/refresh/switch. | Zéro lecture DB supplémentaire par requête en régime normal ; fail-closed. |
| **D2** | Onboarding suivi par deux colonnes additives `tenants.onboarding_completed_at` / `onboarding_completed_by`. **Aucune modification de `TenantStatus`** (pas de nouvel état `ONBOARDING`). | Évite de casser `TenantStatus`, les seeds, les dashboards et ~10 tests existants. |
| **D3** | `users.email` redevient **globalement unique** (index unique sur `LOWER(email)` où `deleted = false`) et les lookups deviennent insensibles à la casse. | Corrige B4 sans casser le multi-mandat (les multi-appartenances passent par `TenantMembership`). |
| **D4** | Le wizard est **déclaratif mais vérifié** pour `MEMBER_IMPORT` (compte importé ≥ 1 ou skip motivé) : l'import réel reste le module `/imports`. | Honnêteté fonctionnelle, pas de fausse automatisation. |
| **D5** | Les titres/descriptions des étapes sont renvoyés en français par l'API ; le web peut les remplacer par ses clés i18n `onboarding.step.<STEP_TYPE>.title/description`. | Une seule source, pas de divergence. |
| **D6** | Les erreurs métier du wizard utilisent `DomainException(message, HttpStatus, code)` → `ProblemDetail.title = code`. | Convention existante (`InvitationService`). |
| **D7** | `POST /onboarding-wizard/{id}/complete` accepte un corps **absent** (équivalent à `{}`). | Corrige le 400 systématique actuel. |
| **D8** | Bannière onboarding côté clients = informative + lien, **jamais bloquante**. | Évite une régression d'usage. |
| **D9** | Aucun nouveau package Flutter : deep links via le support natif Flutter + `go_router` déjà présent. | Pas de dépendance nouvelle. |
| **D10** | Les emails ne lèvent jamais d'exception ; ils renvoient `boolean` et journalisent l'échec (`log.error`). Un échec d'email d'invitation est signalé dans la réponse (`emailSent: false`). | Robustesse quand SMTP n'est pas configuré. |
| **D11** | Toute limite de plan absente ou invalide sur une ressource = **refus** (`BusinessRuleException` code `QUOTA_*` → 403). | Fail-closed déjà en vigueur. |
| **D12** | Aucune donnée en dur côté clients : plans, pays, devises, fuseaux proviennent des APIs existantes. | Corrige MO4. |

---

## 4. TÂCHES AGENT A — BACKEND (`backend/**`, `scripts/**`, `.github/**`, `docs/**` via A13)

> Ordre d'exécution : `A7 → A1 → A2 → A3 → A4 → A5 → A6 → A8 → A9 → A10 → A11 → A15 → A12 → A14 → A13 → A16`.
> Références vérifiées : `AuthService.java:103-133` (login), `:312-353` (refresh) ; `TenantService.java:77-114`, `:199-218` ; `AuthController.java:59-77` ; `TenantRegistrationService.java:74-186` ; `PlatformProvisioningService.java:55-137` ; `InvitationController.java:68-201/261-298/302-355` ; `InvitationService.java:56-168` ; `TenantFilter.java:46-53` ; `WebMvcConfig.java:31-44` ; `QuotaService.java:64-120` ; `UserRepository.java:20-25`.

### A7 🔴 — Unicité email globale + acceptation cross-tenant (constat B4)

**Fichiers :**
- NEW `backend/src/main/resources/db/migration/V180__users_email_global_unique.sql`
- MOD `backend/src/main/java/com/discipolat/modules/users/domain/UserRepository.java`
- MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/AuthService.java`
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/InvitationService.java`
- MOD `backend/src/main/java/com/discipolat/modules/platform/api/InvitationController.java`
- MOD `backend/src/main/java/com/discipolat/modules/users/domain/UserService.java` (si `existsByEmail` utilisé)

**Spécification :**
1. Migration `V180` (fail-closed, aucune suppression de données) :
   ```sql
   DO $$
   DECLARE dup_count int;
   BEGIN
     SELECT COUNT(*) INTO dup_count FROM (
       SELECT LOWER(email) FROM users WHERE deleted = false GROUP BY LOWER(email) HAVING COUNT(*) > 1
     ) d;
     IF dup_count > 0 THEN
       RAISE EXCEPTION 'V180: % doublon(s) email insensibles a la casse - dedoublonnage manuel requis', dup_count;
     END IF;
   END $$;
   CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON users (LOWER(email)) WHERE deleted = false;
   ```
   Ne pas supprimer `uk_users_tenant_email` (composite, redondant mais inoffensif).
2. `UserRepository` : ajouter
   ```java
   @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
   Optional<User> findByEmailIgnoreCase(@Param("email") String email);
   boolean existsByEmailIgnoreCase(String email);
   ```
3. `AuthService` : utiliser `findByEmailIgnoreCase` dans `login`, `resendActivationEmail`, `generatePasswordResetToken`, magic-link. Toute occurrence restante de `findByEmail(` dans `AuthService` = défaut à corriger.
4. `InvitationService.accept` : baser la recherche sur `findGlobalByEmail` :
   - trouvé **même tenant** → membership seulement (comportement actuel) ;
   - trouvé **autre tenant** → **ne pas créer d'utilisateur**, créer `TenantMembership` (rôle/scope/`invitedBy` de l'invitation), résultat `crossTenantIdentity = true` ;
   - absent → créer (comportement actuel) + membership.
   Mettre à jour `AcceptanceResult(UUID userId, String email, UUID tenantId, boolean alreadyMember, boolean crossTenantIdentity)`.
5. `InvitationController.createInvitation` : branche « utilisateur existant » :
   - même tenant → ajout direct de membership (actuel), `crossTenantIdentity = false` ;
   - autre tenant → créer une invitation classique (token + email) et renvoyer `requiresTenantSwitch: true` ; ne jamais créer `User`/membership cross-tenant à cet endroit.
6. `InvitationController.acceptInvitation` : ajouter `crossTenantIdentity` à la réponse JSON.

**Critères d'acceptation :** deux comptes actifs ne peuvent jamais partager un email ; l'acceptation cross-tenant ne duplique jamais `users` ; le login reste déterministe ; migration appliquée sur base vierge.

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/tenants/domain/InvitationServiceCrossTenantTest.java` (≥ 5 cas : même tenant, autre tenant, nouvel email, aucun `User` créé en cross-tenant, flags exacts).
- `backend/src/test/java/com/discipolat/modules/authentication/domain/AuthServiceEmailLookupTest.java` (login insensible à la casse, email inconnu, email mixte).
- Mettre à jour `InvitationServiceTest`, `InvitationControllerTest`, `AuthServiceTest` si signatures modifiées (justification obligatoire).

**Preuve :** `mvn -B -Dtest=InvitationServiceCrossTenantTest,AuthServiceEmailLookupTest,InvitationServiceTest,InvitationControllerTest test`.

### A1 🔴 — Garde de statut tenant + enforcement (constat B1)

**Fichiers :**
- NEW `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantStatusGuard.java`
- NEW `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantStatusChangedEvent.java`
- NEW `backend/src/main/java/com/discipolat/common/multitenancy/TenantStatusInterceptor.java`
- MOD `backend/src/main/java/com/discipolat/common/multitenancy/WebMvcConfig.java`
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantService.java`
- MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/AuthService.java`
- MOD `backend/src/main/java/com/discipolat/modules/platform/api/TenantSwitcherController.java` (`/switch`)

**Spécification :**
1. `TenantStatusGuard.assertAccessible(UUID tenantId)` : `DomainException` `403` avec code `TENANT_SUSPENDED` / `TENANT_CANCELLED` / `TENANT_STATUS_UNAVAILABLE`. Cache `ConcurrentHashMap<UUID, Entry>` TTL **30 s**, invalidé par `@EventListener(TenantStatusChangedEvent)`. Toute erreur de lecture = `TENANT_STATUS_UNAVAILABLE` (fail-closed) + `log.error`.
2. `TenantStatusInterceptor implements HandlerInterceptor` : `preHandle` → si `TenantContext.getTenantId() != null` **et** chemin non public (réutiliser la logique `TenantFilter` : rendre `shouldBypassFilter(HttpServletRequest)` accessible ou extraire la liste des préfixes dans une constante partagée) → `guard.assertAccessible(...)`.
3. `WebMvcConfig` : enregistrer l'intercepteur après `tenantFilterInterceptor` sur `/api/**`.
4. `AuthService.login` : après les contrôles utilisateur → si `user.getTenantId() != null` alors `guard.assertAccessible(...)`. Idem dans `refreshToken` (avant émission des nouveaux jetons).
5. `TenantSwitcherController.switch` : `guard.assertAccessible(newTenantId)` **avant** de générer le JWT.
6. `TenantService.deactivate/reactivate/update(status)` : publier `TenantStatusChangedEvent` (invalidation immédiate).

**Critères d'acceptation :** tenant suspendu ⇒ login, refresh, switch et toute API = `403` avec le bon code ; réactivation ⇒ effet immédiat ; chemins publics toujours joignables (`/api/v1/auth/**`, `/api/v1/public/**`, `validate/accept` d'invitation, actuator) ; aucun 500.

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/tenants/domain/TenantStatusGuardTest.java` (actif, suspendu, annulé, cache, TTL non expiré, invalidation, erreur de lecture → fail-closed).
- `backend/src/test/java/com/discipolat/common/multitenancy/TenantStatusInterceptorTest.java` (public ignoré, sans tenant ignoré, suspendu → `DomainException` 403, actif → true).
- Ajouts dans `AuthServiceTest` (login refusé tenant suspendu, refresh refusé).

**Preuve :** `mvn -B -Dtest=TenantStatusGuardTest,TenantStatusInterceptorTest,AuthServiceTest test`.

### A2 🟠 — Audit des mutations tenant (constat M1)

**Fichiers :** MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantService.java`

**Spécification :** le champ `auditService` existe déjà mais n'est jamais utilisé (vérifié). Ajouter :
- `create` → `auditService.logSimple("TENANT_CREATED", "TENANT", tenant.getId())` ;
- `update` → `TENANT_UPDATED` (+ `TENANT_PLAN_CHANGED` si le plan change) ;
- `deactivate` → `TENANT_SUSPENDED` ; `reactivate` → `TENANT_REACTIVATED` ;
- `markOnboardingCompleted` (A4) → `TENANT_ONBOARDING_COMPLETED`.
Mettre à jour le Javadoc de la classe pour rester exact.

**Critères d'acceptation :** chaque mutation écrit exactement un événement d'audit avec l'acteur courant ; aucun audit sur les lectures.

**Tests imposés :** ajouts dans `backend/src/test/java/com/discipolat/modules/tenants/domain/TenantServiceTest.java` (verify `auditService` appelé par méthode, 1 fois, avec la bonne action).

**Preuve :** `mvn -B -Dtest=TenantServiceTest test`.

### A3 🔴 — Wizard : DTO figés, actions métier réelles, RBAC, erreurs propres (constat B2)

**Fichiers :**
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/api/OnboardingStepResponse.java`
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/api/OnboardingProgressResponse.java`
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/api/OnboardingStatusResponse.java`
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/api/OnboardingStepData.java` (DTO d'entrée `{ "data": Map<String,Object> }` **optionnel**)
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/domain/OnboardingStepDefinition.java` (ordre, titres FR, description, `isSkippable`, `skipRequiresReason`)
- NEW `backend/src/main/java/com/discipolat/modules/onboarding/domain/OnboardingStepActions.java` (les 7 actions métier)
- MOD `backend/src/main/java/com/discipolat/modules/onboarding/domain/OnboardingWizardService.java`
- MOD `backend/src/main/java/com/discipolat/modules/onboarding/api/OnboardingWizardController.java`
- MOD `backend/src/main/java/com/discipolat/modules/onboarding/domain/OnboardingWizardStep.java` (ajout `@Column(name="skip_reason") private String skipReason;` — colonne ajoutée par V178)

**Spécification (conforme au contrat §3.1) :**
1. **DTO de sortie** : ne jamais renvoyer l'entité. `OnboardingStepResponse.from(entity)` doit désérialiser `completedData` (String JSON) en objet via `ObjectMapper` et exposer `isCompleted`, `isSkippable`, `skipRequiresReason`, `title`, `description` depuis `OnboardingStepDefinition`.
2. **Contrôleur** : `@RequestBody(required=false) OnboardingStepData` pour `/complete` ; `@RequestBody(required=false) Map<String,String>` pour `/skip` ; `@authz.isTenantAdmin()` sur `initialize`, `start`, `complete`, `skip` ; `isAuthenticated()` sur les GET.
3. **Service** : `requireStepOfCurrentTenant(UUID stepId)` → `EntityNotFoundException`/`DomainException 404 STEP_NOT_FOUND` si absent ou `tenantId` ≠ tenant courant. Gestion d'ordre (`STEP_ORDER_VIOLATION`), idempotence (`STEP_ALREADY_COMPLETED`), skip (`STEP_NOT_SKIPPABLE`, `STEP_SKIP_REASON_REQUIRED`).
4. **Actions** : implémenter exactement le tableau « Actions métier obligatoires par étape » de `§3.1`. Chaque action :
   - valide son `data` (types, bornes) → sinon `400 STEP_DATA_INVALID` avec `details` listant les champs fautifs ;
   - appelle un service réel (jamais de simulation) ;
   - écrit un audit dédié : `TENANT_ONBOARDING_CHURCH_IDENTITY`, `TENANT_MEMBERS_IMPORTED`, `TENANT_STRUCTURE_CREATED`, `TENANT_ROLES_INVITED`, `TENANT_BRANDING_UPDATED`, `TENANT_MODULES_ENABLED`, `TENANT_FIRST_EVENT_CREATED` ;
   - stocke `completedData` = `data` normalisé + identifiants créés (`createdIds`).
5. **Extraire** la logique de création d'invitation de `InvitationController.createInvitation` vers une méthode publique `InvitationService.createInvitation(UUID tenantId, UUID inviterId, String email, String roleKey, MembershipScopeType scopeType, UUID scopeId, UUID organizationNodeId)` réutilisée par le contrôleur **et** par l'action `ROLES` (aucune duplication). Le comportement HTTP du contrôleur reste identique (mêmes réponses).
6. **Complétion globale** : si toutes les étapes sont `COMPLETED`/`SKIPPED` → `tenantService.markOnboardingCompleted(SecurityUtils.getCurrentUserId())` (A4) + audit.
7. `skipStep` enregistre `skipReason` et exige un motif si `skipRequiresReason`.

**Critères d'acceptation :** `GET /` renvoie 7 étapes avec les champs exacts du contrat ; `POST /{id}/complete` **sans corps** réussit si `data` non requise pour l'étape ; un id d'un autre tenant → 404 `STEP_NOT_FOUND` ; une étape hors ordre → 409 ; chaque étape produit un effet réel vérifiable (église renommée, département/famille créés, invitations envoyées, branding mis à jour, modules activés, événement créé).

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/onboarding/domain/OnboardingWizardServiceTest.java` (≥ 12 cas : contrat DTO, ordre, idempotence, skip+motif, chaque action avec ses services mockés, complétion globale, `orElseThrow` interdit).
- `backend/src/test/java/com/discipolat/modules/onboarding/api/OnboardingWizardControllerTest.java` (RBAC `@authz`, corps absent, 404, 409, 400 `STEP_DATA_INVALID`).
- `backend/src/test/java/com/discipolat/modules/onboarding/domain/OnboardingWizardTenantIsolationTest.java` (un tenant ne voit/complete jamais les étapes d'un autre).
- `backend/src/test/java/com/discipolat/modules/tenants/domain/InvitationServiceCreateInvitationTest.java` (extraction sans régression).

**Preuve :** `mvn -B -Dtest=OnboardingWizardServiceTest,OnboardingWizardControllerTest,OnboardingWizardTenantIsolationTest,InvitationServiceCreateInvitationTest test`.

### A4 🟠 — Colonnes de complétion d'onboarding + endpoint `/status` (constat B2/D2)

**Fichiers :**
- NEW `backend/src/main/resources/db/migration/V178__tenant_onboarding_completion.sql`
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/Tenant.java` (`onboardingCompletedAt : Instant`, `onboardingCompletedBy : UUID`)
- MOD `backend/src/main/java/com/discipolat/modules/tenants/api/TenantResponse.java` (ajout `onboardingCompletedAt`, additif en fin de record)
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantService.java` (`markOnboardingCompleted(UUID actorId)` : idempotent, n'écrase pas une valeur existante, publie l'audit A2)
- MOD `OnboardingWizardService` (appel en fin de wizard, cf. A3.6)

**Migration V178 (exacte) :**
```sql
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS onboarding_completed_by UUID;
ALTER TABLE onboarding_wizard_steps ADD COLUMN IF NOT EXISTS skip_reason TEXT;
DO $$
DECLARE dup_count int;
BEGIN
  SELECT COUNT(*) INTO dup_count FROM (
    SELECT tenant_id, step_type FROM onboarding_wizard_steps GROUP BY tenant_id, step_type HAVING COUNT(*) > 1
  ) d;
  IF dup_count > 0 THEN
    RAISE EXCEPTION 'V178: % doublon(s) etape onboarding - nettoyage manuel requis', dup_count;
  END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_onboarding_step_tenant_type ON onboarding_wizard_steps (tenant_id, step_type);
```

**Critères d'acceptation :** la complétion du wizard renseigne les 2 colonnes une seule fois ; `GET /status` renvoie `completed=true` et `completedAt` ; un tenant non onboardé renvoie `completed=false` ; `tenantId` jamais exposé hors du tenant.

**Tests imposés :** ajouts dans `TenantServiceTest` (idempotence de `markOnboardingCompleted`, colonnes non écrasées) + `OnboardingWizardServiceTest` (flag posé uniquement à la fin).

**Preuve :** `mvn -B -Dtest=TenantServiceTest,OnboardingWizardServiceTest test` + `mvn -B verify` (Flyway sur base vierge) à A12.

### A5 🔴 — Provisioning : owner obligatoire + email d'activation (constat B3)

**Fichiers :**
- NEW `backend/src/main/java/com/discipolat/modules/platform/domain/TenantOwnerProvisioningService.java`
- MOD `backend/src/main/java/com/discipolat/modules/platform/domain/PlatformProvisioningService.java` (+ champs `ownerEmail`, `ownerFirstName`, `ownerLastName` dans `Command`, validation → `OWNER_REQUIRED`)
- MOD `backend/src/main/java/com/discipolat/modules/platform/api/PlatformProvisioningController.java` (+ champs dans `AtomicProvisioningRequest`, réponse `owner`)

**Spécification :**
1. `TenantOwnerProvisioningService.provisionOwner(UUID tenantId, String email, String firstName, String lastName, UUID actorId)` :
   - `findGlobalByEmail(email)` : si utilisateur existant **dans un autre tenant** → `BusinessRuleException` `OWNER_EMAIL_ALREADY_USED` (409) ;
   - si inexistant → créer `User` (`UserRole.PASTEUR`, `roles={PASTEUR}`, `activeRole=PASTEUR`, `statut=PENDING_ACTIVATION`, `tenantId`) avec mot de passe aléatoire fort (`SecureRandom`, 32 chars) **jamais communiqué** ;
   - créer `TenantMembership` `TENANT_OWNER` (`MembershipScopeType.TENANT`, `ACTIVE`) si absente ;
   - `emailService.sendWelcomeEmail(email, firstName, frontendUrl + "/activate?token=" + token)` via `AuthService.sendActivationEmail(userId)` (réutiliser, ne pas réimplémenter) → renvoyer `activationEmailSent`.
2. `PlatformProvisioningService.provision` : après la création de l'église racine, appeler le service owner (étape 3) puis les département/famille ; si `ownerEmail` absent → `BusinessRuleException("Le propriétaire (owner) de l'église est requis", "OWNER_REQUIRED")` **avant** toute écriture (validation en tête de méthode).
3. Réponse : ajouter `"owner": {"userId","email","activationEmailSent"}`.

**Critères d'acceptation :** aucun tenant créé sans owner ; l'owner reçoit un email d'activation ; un email déjà utilisé dans un autre tenant est refusé sans création partielle (transaction).

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/platform/domain/TenantOwnerProvisioningServiceTest.java` (création, membership, email envoyé, email cross-tenant refusé, idempotence si owner existe déjà dans le même tenant).
- Ajouts dans `PlatformProvisioningServiceTest` (owner manquant → exception avant écriture ; owner créé ; ordre des appels).

**Preuve :** `mvn -B -Dtest=TenantOwnerProvisioningServiceTest,PlatformProvisioningServiceTest test`.

### A6 🟠 — Emails d'inscription + endpoint public de statut (constat M2)

**Fichiers :**
- MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/EmailService.java` (4 nouvelles méthodes, non bloquantes, retour `boolean`)
- MOD `backend/src/main/java/com/discipolat/modules/platform/domain/TenantRegistrationService.java` (`submit` → `sendRegistrationReceived`, `approve` → `sendRegistrationApproved`, `reject` → `sendRegistrationRejected`)
- MOD `backend/src/main/java/com/discipolat/modules/authentication/api/AuthController.java` (`POST /registration-status`)
- NEW `backend/src/main/java/com/discipolat/modules/authentication/api/RegistrationStatusRequest.java` + `RegistrationStatusResponse.java`
- MOD `backend/src/main/java/com/discipolat/common/infrastructure/config/PerIpRateLimiter.java` (+ `tryConsumeRegistrationStatus`)
- MOD `backend/src/main/resources/application.yml` (+ `app.rate-limiting.registration-status-capacity/refill`, valeurs par défaut 3 / 3)

**Spécification :** strictement le contrat `§3.3`. Le service de statut lit `TenantRegistrationRequestRepository.findByEmail` (insensible à la casse après A7 : utiliser `LOWER`), renvoie `NONE` si aucune demande, ne divulgue jamais `passwordHash` ni `organizationName`. Toutes les réponses passent par `Cache-Control: no-store` (comme `validate/{token}`).

**Critères d'acceptation :** soumission → email reçu (mock vérifié) ; approbation → email d'approbation ; rejet → email avec motif ; `registration-status` rate-limité et sans fuite d'information ; aucun échec SMTP ne casse la transaction métier.

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/platform/domain/TenantRegistrationEmailTest.java` (3 emails, contenus non vides, échec SMTP ignoré).
- `backend/src/test/java/com/discipolat/modules/authentication/api/AuthControllerRegistrationStatusTest.java` (NONE, PENDING, APPROVED, REJECTED avec motif, 429, `no-store`).

**Preuve :** `mvn -B -Dtest=TenantRegistrationEmailTest,AuthControllerRegistrationStatusTest test`.

### A8 🟠 — Quotas : espaces, événements, églises + alerte admin (constat M3)

**Fichiers :**
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/QuotaService.java` (`checkCanCreateSpace`, `checkCanCreateEvent`)
- MOD `backend/src/main/java/com/discipolat/modules/spaces/domain/SpaceService.java` (appel du check dans `createSpace`)
- MOD `backend/src/main/java/com/discipolat/modules/events/domain/EventService.java` (appel du check dans `create`)
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/OrganizationHierarchyService.java` (églises/campus → `checkCanCreateChurch` si absent)
- NEW `backend/src/main/java/com/discipolat/modules/tenants/domain/QuotaAlertService.java` (notification in-app aux `TENANT_OWNER`/`TENANT_ADMIN` via `NotificationService.create(tenantId, destinataireId, TypeNotification.X, CanalNotification.Y, ...)` — vérifier les valeurs exactes des enums dans `common/enums/` et les utiliser, sans en créer)
- MOD `QuotaService.exceeded(...)` (déclencher `QuotaAlertService` quand le quota est dépassé)

**Spécification :**
1. Les limites « spaces » / « events » existent dans `TenantPlanPolicy` (clés `spaces`, `events`, reconnues par la validation des limites). Utiliser le helper privé `organizationLimit` (le rendre réutilisable si besoin) et compter : espaces via `SpaceRepository.countByTenantId(...)`, événements actifs via une méthode de comptage à ajouter dans `EventRepository` (`long countByTenantIdAndStatutNot(...)`) — **vérifier les noms réels des colonnes/statuts avant d'ajouter une requête**.
2. Fail-closed : limite absente ou plan non résolu → `BusinessRuleException` `QUOTA_CONFIGURATION_INVALID` (403).
3. Alerte : une notification par dépassement, destinataires = membres `TENANT_OWNER`/`TENANT_ADMIN` actifs (`TenantMembershipRepository`), titre « Quota atteint : <ressource> », jamais bloquante.

**Critères d'acceptation :** créer un espace/événement/église au-delà de la limite → `403` code `QUOTA_*` ; notification créée pour chaque admin ; aucune régression pour les quotas existants (users, storage, IA, cours).

**Tests imposés :** ajouts dans `QuotaServiceTest` (si présent) ou NEW `QuotaServiceSpacesEventsTest` (4 cas : sous la limite, à la limite, limite absente → 403, notification émise) + test d'intégration léger pour `SpaceService.createSpace`/`EventService.create`.

**Preuve :** `mvn -B -Dtest=QuotaServiceSpacesEventsTest,SpaceServiceTest,EventServiceTest test`.

### A9 🟠 — Invitations : répertoire, email de bienvenue, relances J-3/J-1 (constat M4)

**Fichiers :**
- NEW `backend/src/main/resources/db/migration/V179__invitation_reminder_tracking.sql`
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/Invitation.java` (`remindedAt : Instant`, `@Column(name="reminded_at")`)
- MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/InvitationService.java` (répertoire + bienvenue après acceptation)
- MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/EmailService.java` (`sendInvitationWelcome`, `sendInvitationReminder`)
- NEW `backend/src/main/java/com/discipolat/modules/platform/domain/InvitationReminderScheduler.java`

**Migration V179 (exacte) :**
```sql
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS reminded_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_invitations_status_expires ON invitations (status, expires_at);
```

**Spécification :**
1. À l'acceptation (`InvitationService.accept`) : créer/mettre à jour la personne via `PeopleService.registerPerson(tenantId, person, "INVITATION", actorId)` si `PersonRepository` ne trouve pas déjà `email_normalized` (champs minimaux : `firstName`, `lastName`, `emailNormalized`, `status="ACTIVE"`) — **ne jamais créer de doublon** (recherche préalable obligatoire).
2. `InvitationController.acceptInvitation` : après l'acceptation, `emailService.sendInvitationWelcome(email, firstName, tenantName, frontendUrl + "/login")` (échec → `welcomeEmailSent: false` dans la réponse, jamais d'exception).
3. `InvitationReminderScheduler` : `@Scheduled(cron = "0 0 8 * * *")`, méthode `sendReminders()` :
   - invitations `PENDING` avec `expiresAt` dans **3 jours ± 12 h** → `sendInvitationReminder(..., daysLeft = 3)` ;
   - invitations `PENDING` avec `expiresAt` dans **1 jour ± 6 h** → `daysLeft = 1` ;
   - mettre à jour `remindedAt` ; ne jamais relancer deux fois la même étape (`remindedAt` non nul → skip si déjà relancé le même jour) ;
   - audit `INVITATION_REMINDER_SENT` par envoi réussi.
4. Méthode de requête à ajouter dans `InvitationRepository` : `List<Invitation> findByStatusAndExpiresAtBetween(InvitationStatus, Instant, Instant)`.

**Critères d'acceptation :** une acceptation crée la personne exactement une fois ; un email de bienvenue est tenté ; les relances partent une seule fois par palier ; aucun envoi pour les invitations acceptées/annulées.

**Tests imposés :**
- `backend/src/test/java/com/discipolat/modules/tenants/domain/InvitationDirectoryRegistrationTest.java` (création, pas de doublon, source `INVITATION`).
- `backend/src/test/java/com/discipolat/modules/platform/domain/InvitationReminderSchedulerTest.java` (3 paliers avec `Clock` fixe : J-3, J-1, hors fenêtre ; pas de doublon ; statut non PENDING ignoré).

**Preuve :** `mvn -B -Dtest=InvitationDirectoryRegistrationTest,InvitationReminderSchedulerTest test`.

### A10 🟡 — Finitions wizard & invitations (mineurs de l'audit)

**Fichiers :** MOD `OnboardingWizardService` (déjà touché en A3), MOD `InvitationController.listInvitations`, MOD `InvitationRepository`.

**Spécification :**
1. `GET /api/v1/admin/invitations` accepte `?page=0&size=50&status=PENDING&q=email` (rétro-compatible : sans paramètres → comportement actuel, liste complète). Réponse : si `page` est fourni → `PageResponse` (convention existante) ; sinon liste telle quelle.
2. `initialize` idempotent + concurrence : s'appuyer sur l'index unique `(tenant_id, step_type)` (V178) et gérer `DataIntegrityViolationException` en relisant les étapes (pas d'erreur 500).
3. Supprimer le champ `config` inutilisé de la logique et documenter `completedData` (pas de suppression de colonne).

**Critères d'acceptation :** deux appels concurrents à `initialize` ne créent jamais de doublons ; la pagination fonctionne sans casser l'usage existant.

**Tests imposés :** `OnboardingWizardInitializeConcurrencyTest` (2 threads simulés) + ajouts dans `InvitationControllerTest` (pagination, filtre).

**Preuve :** `mvn -B -Dtest=OnboardingWizardInitializeConcurrencyTest,InvitationControllerTest test`.

### A11 🟠 — Tests de sécurité IDOR/isolation (wizard, provisioning, subscription)

**Fichiers :** MOD `backend/src/test/java/com/discipolat/modules/tenants/MultiTenantSecurityTests.java` + NEW `backend/src/test/java/com/discipolat/modules/onboarding/OnboardingWizardSecurityIT.java`.

**Spécification :** ajouter à la matrice existante :
- tenant B ne peut ni lire (`GET /progress`), ni compléter, ni skipper, ni `start` une étape du tenant A (404 `STEP_NOT_FOUND`) ;
- un membre non-admin du tenant ne peut pas muter les étapes (403) ;
- `GET /tenants/*` reste réservé au super admin plateforme ;
- `SubscriptionController` (`/admin/subscription/**`) refuse un utilisateur non-admin de tenant ;
- l'intercepteur de suspension ne fuit aucun détail inter-tenant.

**Critères d'acceptation :** chaque nouveau cas cite explicitement la cellule de `docs/security/SECURITY_MATRIX.md` et la met à jour (fait partie de A13 pour le fichier doc).

**Preuve :** `mvn -B -Dtest=MultiTenantSecurityTests,OnboardingWizardSecurityIT test`.

### A15 🟡 — Correction `AuthService` magic-link (message/code inversés)

**Fichiers :** MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/AuthService.java` (~ligne 469-471).

**Spécification :** remplacer `new BusinessRuleException("USER_NOT_FOUND", "Aucun compte associé à cet email")` par `new BusinessRuleException("Aucun compte associé à cet email", "USER_NOT_FOUND")` (convention `(message, code)`).

**Tests imposés :** ajout dans `AuthServiceTest` : le `getCode()` vaut `USER_NOT_FOUND` et le message est le texte français.

### A12 🟠 — Validation Flyway + suite complète backend

**Spécification :** exécuter et archiver :
1. `mvn -B -DskipTests compile` ;
2. `mvn -B verify` (base de test vierge → V178/V179/V180 appliquées) ;
3. Vérifier que le nombre de tests exécutés est **≥ celui mesuré en `P0.3`** et que le compte d'échecs est 0.
   > **⚠️ Correction :** la version précédente imposait « ≥ 1188 (référence dépôt) ». Ce nombre est **invérifiable en l'état** : le dépôt compte 139 fichiers de test Java, et le nombre de cas n'est connu qu'après exécution. Exiger un chiffre qu'aucun agent ne peut reproduire produit des rapports falsifiés (« 1200 tests » recopié sans exécution). Le seul baseline opposable est **celui mesuré en `P0.3`, par l'agent, sur cette machine**. C'est la règle R10 appliquée à elle-même.

**Critères d'acceptation :** build vert, migrations appliquées, aucun test désactivé.

### A14 🟡 — Script de recette E2E + CI

**Fichiers :** NEW `scripts/verify-tenant-onboarding.sh` ; MOD `.github/workflows/*.yml` (ajouter l'exécution du script en job optionnel/documenté).

**Spécification du script (bash, `set -euo pipefail`, `curl` + `jq`, échec = exit 1) :**
1. Login super admin → création d'un tenant avec owner (`POST /platform/admin/provisioning`) → vérifier `owner.userId` + `activationEmailSent`.
2. Activer le compte owner (token) → login owner → `GET /onboarding-wizard` (7 étapes, contrat complet : `isCompleted`, `title`, `stepOrder`).
3. Compléter les 7 étapes avec les `data` du contrat → `GET /status` → `completed=true`.
4. `GET /onboarding-wizard/progress` → `percentage=100`.
5. Suspendre le tenant → login owner = `403 TENANT_SUSPENDED` → réactiver → login OK.
6. Invitations : créer → `validate` → `accept` (nouvel email) → login ; puis inviter un email déjà présent dans un autre tenant → `requiresTenantSwitch=true` → accept → `crossTenantIdentity=true` → vérifier `GET /tenant-switcher/my-tenants` = 2 tenants.
7. IDOR : tenant B → `POST /onboarding-wizard/{idEtapeA}/complete` = `404`.
8. Quota : plan à 1 utilisateur → 2ᵉ création = `403 QUOTA_*`.

Le script imprime `PASS/FAIL — <scénario>` et se termine par un récapitulatif. **Aucun secret en dur** : variables `BASE_URL`, `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD` (défauts = valeurs de dev documentées, jamais de secrets de prod).

**Preuve :** sortie complète du script archivée dans `reports/plan-2agents/agentA.md` (avec le backend démarré localement).

### A13 🟠 — Documentation véridique (obligatoire, après A1-A15)

**Fichiers à corriger (Agent A uniquement) :** `docs/TENANT_ONBOARDING.md`, `docs/MULTI_TENANT_ARCHITECTURE.md`, `docs/ADMINISTRATION_MODEL.md`, `docs/API.md`, `docs/ORGANIZATION_HIERARCHY.md`, `docs/security/SECURITY_MATRIX.md`, `docs/ETAT_AVANCEMENT_CHURCH_OS.md`, `reports/GO_NO_GO_REPORT.md`, `SUPER_ADMIN_AUDIT.md`.

**Règles de rédaction (R10) :**
1. Supprimer toute affirmation non prouvée. Interdit : « PRODUCTION READY », « ✅ » sans test cité.
2. `docs/TENANT_ONBOARDING.md` : **réécrire** intégralement pour décrire le flux réel :
   - provisioning super admin (avec owner obligatoire) → activation → wizard 7 étapes → `completed`,
   - les **vraies** routes (`/onboarding-wizard`, pas `/onboarding/1-profile`),
   - les **vrais** endpoints (`/api/v1/admin/branding`, pas `/api/tenants/{id}/branding`),
   - la section mobile réelle : `mobile/lib/presentation/screens/onboarding/`, `invitations/accept_invitation_screen.dart`, deep links (tâche B8), wizard mobile `tenant/tenant_onboarding_screen.dart` (tâche B7),
   - section « Écarts connus » listant ce qui n'est **pas** implémenté (ex. import réel dans le wizard, relance manuelle, etc.).
3. Chaque affirmation citée doit être de la forme : `(_preuve : <TestClass#méthode> — commande `mvn -B -Dtest=... test`)`.
4. `docs/ETAT_AVANCEMENT_CHURCH_OS.md` : ajouter une section `## Correction 2026-09-27 — écarts constatés et corrigés` listant G1.5/G1.6/§50-51/§52 avec : ce qui était affirmé, la réalité constatée, les tâches A*/B* qui corrigent, et le test de preuve. Ne pas réécrire l'historique.
5. `reports/GO_NO_GO_REPORT.md` : corriger les lignes §50-51 et §52 (statut réel + renvoi vers le plan).
6. Consigner les **deltas doc fournis par l'Agent B** (section « Deltas doc (pour A13) » de `agentB.md`) : deep links, écrans mobiles, pages web.

**Critères d'acceptation :** aucun fichier de doc ne décrit une fonctionnalité inexistante ; toutes les routes citées existent (vérification croisée avec `audit/backend_endpoints.txt` régénéré si possible) ; les 3 fichiers de rapport contiennent la section de correction.

**Preuve :** `git diff --stat` des docs + grep de contrôle (`Select-String -Pattern 'PRODUCTION READY'`) montrant qu'il ne reste aucune occurrence non justifiée.

### A16 🟡 — OpenAPI interne + liste des modules publics

**Fichiers :** MOD `backend/src/main/java/com/discipolat/modules/platform/api/PublicApiDocsController.java` (ajouter les nouveaux endpoints wizard + `registration-status`) ; MOD `docs/API.md` (via A13).

**Critères d'acceptation :** la documentation OpenAPI publiée correspond exactement aux endpoints existants (`/onboarding-wizard`, `/status`, `/registration-status`).

**Preuve :** `mvn -B -Dtest=PublicApiDocsControllerTest test` (ou test à créer s'il n'existe pas).

---

## 5. TÂCHES AGENT B — CLIENTS (`frontend/**`, `mobile/**`, `infra/well-known/**`)

> Ordre d'exécution : `B8 → B1 → B2 → B3 → B4 → B5 → B6 → B12 → B7 → B9 → B10 → B11 → B13`.
> Conventions à respecter : `api` de `@/lib/api` (axios, baseURL `/api/v1`), TanStack Query, `toast` (react-hot-toast), `tText` de `@/i18n`, classes Tailwind existantes (`glass-card`, `btn-primary`, `page-container`, `page-header`), tests Vitest + Testing Library. Mobile : Riverpod (`ConsumerStatefulWidget`/`ref`), `ApiService`, `go_router`, `GlassCard`/`GlassTheme`.

### 5.0 Exigence de qualité d'interface (applicable à TOUTES les tâches B)

> Ce plan ne livre pas seulement des écrans **qui marchent** : il livre des écrans **digne d'un produit premium**. Un onboarding qui fonctionne mais dont les états de chargement, d'erreur et de vide sont absents est un onboarding **inachevé**. Ces exigences ne sont pas des « améliorations opportunistes » (R12) : ce sont les **critères d'acceptation** des tâches B.

**5.0.1 — Réutilisation avant création (règle dure).**
Le dépôt possède déjà un design system dans `frontend/src/components/ui/UXComponents.tsx` : `SkeletonLine`, `SkeletonCard`, `SkeletonTable`, `SkeletonDashboard`, `EmptyState`, `ConfirmDialog`, `ToastContainer`, `useReducedMotion`, `VisuallyHidden`, `ProgressBar`, `OnboardingStepper` — **toutes déjà testées** dans `frontend/src/__tests__/UXComponents.test.tsx`. Les tokens visuels existent dans `frontend/src/index.css` : `glass-card`, `glass-card-premium`, `btn-primary`, `page-container`, `page-header`, `bg-gradient-mesh`, `shadow-glow`, plus les keyframes `loadingDots`/`pulseBg` et le bloc `@media (prefers-reduced-motion: reduce)`.
→ **Avant d'écrire un composant, prouver qu'il n'existe pas déjà** (`grep`). Si un composant voisin existe mais manque une prop, **l'étendre** (props optionnelles rétrocompatibles) au lieu d'en créer un second. Un doublon visuel est une régression : il diverge silencieusement du design system et oblige à maintenir deux versions.

**5.0.2 — Les 5 états obligatoires de toute vue.**
Chaque page/écran créé ou modifié gère explicitement : **chargement** (squelette, jamais un spinner nu sur une liste), **vide** (`EmptyState` avec une action utile), **erreur** (message actionnable + `retry`, jamais un `toast` qui disparaît), **succès**, **hors-ligne/échec réseau** (`getErrorMessage` distingue déjà `ERR_NETWORK` et `ECONNABORTED` — s'en servir). Un écran sans état vide est un écran cassé pour tout compte créé le jour de l'inscription.

**5.0.3 — Mouvement et profondeur, sans dépendance nouvelle.**
**Interdiction formelle d'ajouter `framer-motion`, `three` ou `@react-three/*`** : aucune n'est dans `package.json` (vérifié 2026-09-28) et R4 interdit les dépendances nouvelles. La « 3D » se fait avec ce qui existe : `transform`, `perspective`, `translateZ`, `rotateX/rotateY`, `backdrop-filter`, `transition`/`transform`, `shadow-glow`, `glass-card-premium`, `bg-gradient-mesh`. Règles :
- **≤ 400 ms**, easing `cubic-bezier(.2,.8,.2,1)` ; `transform`/`opacity` uniquement (jamais `width`/`height`/`top`/`left` animés → reflow).
- Toute animation est **annulée** sous `prefers-reduced-motion` (le hook `useReducedMotion` existe déjà : s'en servir ; le CSS global couvre déjà le cas CSS).
- Effets de profondeur/parallaxe **uniquement** via `transform`/`opacity` (composited GPU) ; **jamais** de `blur()` animé ni d'ombre portée animée sur une liste.
- Mobile : 60 fps visées ; `backdrop-filter` coûteux → fond opaque dégradé sur les longues listes.
**5.0.4 — Accessibilité (ce n'est pas un supplément).**
- Cible **WCAG 2.1 AA** : contraste ≥ 4.5:1 pour le texte courant, ≥ 3:1 pour les grandes surfaces et les bordures porteuses de sens. Le thème comporte un mode sombre (`dark:`) : **chaque** nouvelle surface se vérifie dans les deux modes.
- Navigation clavier complète sur le wizard : `Tab`/`Shift+Tab` cohérents, `Enter`/`Espace` activent, `aria-current="step"` sur l'étape courante du stepper, `role="progressbar"` + `aria-valuenow` sur la progression.
- Erreurs de formulaire : `aria-invalid`, `aria-describedby` vers le message, focus sur le premier champ en erreur, et **jamais** une erreur signalée par la couleur seule.
- `VisuallyHidden` pour les libellés d'accessibilité (le composant existe déjà).
- **RTL** : `ar` est en RTL (`RTL_LOCALES`, `src/i18n/index.tsx`). Aucune propriété directionnelle en dur (`margin-left`, `left`, `text-left`) dans les nouveaux composants — utiliser `ms-`/`me-`/`ps-`/`pe-`/`start-`/`end-`. Les icônes directionnelles (flèches de progression) s'inversent.
- **Cibles tactiles ≥ 44×44 px**, y compris sur les « chips » de filtres et les badges.

**5.0.5 — i18n et données.**
- Toute chaîne visible passe par `tText(...)` ou une clé `useI18n().t(...)`, dans les 6 locales, `fr.ts` en premier (cf. B1 et B13.1 : le mécanisme indexe par valeur, donc une chaîne absente de `fr.ts` **retombe silencieusement en français**).
- Formatage via `Intl` / `intl` : dates, heures, nombres et devises **jamais** formatés à la main (`toLocaleDateString('fr-FR')` en dur est un défaut — cf. `TenantAdminInvitationsPage.tsx:134`).
- Aucune donnée en dur imposée (D12) ; une valeur par défaut de sélecteur doit rester **sélectionnable et remplaçable**.

**5.0.6 — Responsive et performance.**
- Mobile-first : les écrans d'administration restent utilisables à 360 px ; aucune largeur fixe > 360 px, aucune table non scrollable.
- `vite.config.ts` découpe déjà les chunks (`charts`, `forms`, `query`, `icons`, `utils`, `vendor`) : **ne pas casser `manualChunks`** et rester sous le `chunkSizeWarningLimit` de 300 kB.
- Toute nouvelle page est **`lazy()`** dans `App.tsx` (convention déjà en place, ex. `OnboardingWizardPage` ligne 137).
- Images/logos : `loading="lazy"` et dimensions réservées (anti-CLS).
- Budget : aucune régression mesurable sur le bundle ni sur le nombre de requêtes réseau (une requête par écran, pas une par étape).

**5.0.7 — Ce qui reste interdit.**
Pas de `alert()`/`confirm()` natif (le dépôt a `ConfirmDialog` et `toast`). Pas de `any` ni de `@ts-ignore`. Pas de `console.log` oublié. Pas de doublon de composant existant. Pas de dépendance npm/Flutter nouvelle. Pas de secret en dur. Pas de TODO/FIXME livré.


### B1 🔴 — Wizard web réel : 7 étapes, contrat §3.1, reprise, erreurs (constat F1)

**Fichiers :**
- NEW `frontend/src/types/onboarding.ts` (`OnboardingStep`, `OnboardingProgress`, `OnboardingStatus`, types de `data` par étape, `OnboardingStepType` union des 7 valeurs)
- NEW `frontend/src/hooks/useOnboardingWizard.ts` (queries/mutations : `GET /`, `GET /status`, `POST /{id}/start|complete|skip`, `POST /initialize`, invalidation de `['onboarding-wizard']` et `['onboarding-status']`)
- **RÉUTILISER — NE PAS RECRÉER :** `frontend/src/components/ui/UXComponents.tsx` exporte **déjà** `OnboardingStepper` (ligne 212), `ProgressBar` (184), `EmptyState` (70), `SkeletonLine/SkeletonCard/SkeletonTable/SkeletonDashboard` (8-57), `ConfirmDialog` (102), `useReducedMotion` (154), `VisuallyHidden` (172). Elles sont **déjà testées** dans `frontend/src/__tests__/UXComponents.test.tsx` (`OnboardingStepper` : lignes 184-210).
  → **Interdit** de créer `components/onboarding/OnboardingStepper.tsx` : ce serait un doublon du design system, contraire à `R4` et à la règle « ne rien dupliquer ». Si `OnboardingStepper` ne suffit pas (il lui manque `aria-current`, la navigation clavier et les états d'erreur), **l'étendre dans `UXComponents.tsx` en ajoutant des props optionnelles rétrocompatibles**, et étendre `UXComponents.test.tsx`. Ne jamais casser les appels existants.
- NEW `frontend/src/components/onboarding/steps/ChurchIdentityStep.tsx` | `MemberImportStep.tsx` | `StructureStep.tsx` | `RolesStep.tsx` | `BrandingStep.tsx` | `ModulesStep.tsx` | `FirstEventStep.tsx` (un fichier par étape, tous polymorphes d'un même contrat `StepProps`)
- MOD `frontend/src/pages/OnboardingWizardPage.tsx` (orchestration : étape courante = première non terminée, formulaire, `Suivant`, `Ignorer` avec motif, `Terminer`)
- MOD `frontend/src/i18n/{fr,en,es,pt,sw,ar}.ts` (clés `onboarding.*` : titres/descriptions/erreurs/boutons ; clé par étape `onboarding.step.CHURCH_IDENTITY.title` etc.)

**Contrainte i18n — à comprendre avant d'écrire la moindre ligne :**
`src/i18n/index.tsx` expose deux mécanismes **distincts**, et le plan les mélangeait :
- `tText('chaîne française')` : traduction **par valeur** via un index inversé `valeur FR → clé` construit à partir de `fr.ts` (`REVERSE_FR`, ligne 57). Une chaîne qui n'existe pas dans `fr.ts` **retombe silencieusement en français** — donc toute nouvelle chaîne doit **d'abord** être ajoutée dans `fr.ts`, sinon les 5 autres locales ne la verront jamais.
- `useI18n().t('clé')` : traduction **par clé**. C'est le mécanisme à utiliser pour les clés `onboarding.step.<STEP_TYPE>.title` (D5), car `tText` ne peut pas traduire une clé.
→ **Règle** : `fr.ts` est la référence ; toute clé ajoutée doit exister dans les 6 locales (B13) et le test de non-régression i18n doit le prouver.

**Spécification :**
1. Utiliser **exactement** les champs du contrat §3.1 (`id`, `stepType`, `stepOrder`, `title`, `description`, `status`, `isCompleted`, `isSkippable`, `skipRequiresReason`, `completedAt`, `completedData`). Aucun accès à un champ inexistant.
2. `POST /{id}/complete` envoie `{ data: {...} }` (objet JSON) ; pour les étapes sans données requises, envoi possible de `{}`. Le bouton désactivé si `!isValid`.
3. `POST /{id}/skip` envoie `{ reason }` si `skipRequiresReason`, bouton « Ignorer » masqué/disabled si `!isSkippable`.
4. Reprise : à l'ouverture, sélectionner la première étape non terminée ; une étape `COMPLETED`/`SKIPPED` réaffiche son `completedData` en lecture seule (badge + date).
5. Erreurs : afficher `title` (code) + `detail` du `ProblemDetail` ; cas explicites `STEP_ORDER_VIOLATION`, `STEP_ALREADY_COMPLETED`, `STEP_DATA_INVALID` (afficher `details` champ par champ), `403` (message « droits insuffisants »), `TENANT_SUSPENDED`.
6. À la fin (100 %), afficher l'écran de succès + liens « Tableau de bord » et « Inviter l'équipe ».
7. Chaque formulaire envoie **uniquement** les champs du contrat (ex. `ChurchIdentityStep` : `churchName` requis 2-120, `businessName`, `city`, `phone`, `email`, `timezone`, `currency` ; `StructureStep` : listes dynamiques de départements/familles ; `RolesStep` : email + rôle ; `ModulesStep` : cases à cocher alimentées par `GET /admin/tenant-features`).
8. **Piège de rupture de contrat — à traiter explicitement :** l'implémentation actuelle du frontend type l'étape comme `{ id, title, description?, order, isCompleted, completedAt? }` et trie par `order` (`OnboardingWizardPage.tsx:7-14, 34`). Le contrat §3.1 impose `stepOrder` (et supprime `order`). Comme `POST /{id}/complete` et `POST /{id}/skip` ne renvoient que l'étape concernée, **tout le fichier doit être réécrit** : plus aucune référence à `order` ne doit subsister. Vérifier par `grep -n "\.order\b" src/pages/OnboardingWizardPage.tsx` → doit être vide.
9. `GET /admin/tenant-features` **existe déjà** (`TenantFeatureController`, `@RequestMapping("/api/v1/admin/tenant-features")`, GET `/` et `/enabled`) — vérifié le 2026-09-28. Il n'expose **que de la lecture** : l'activation se fait côté serveur via l'action `MODULES` du wizard (A3). Ne pas chercher un `POST` d'activation côté client, et ne pas inventer d'endpoint.
10. Devise et fuseau pour `CHURCH_IDENTITY` : **aucun endpoint de référence n'existe** (`/currencies` et `CurrencyController` sont absents — vérifié le 2026-09-28). Conformément à D12, utiliser des **valeurs par défaut de sélecteur** documentées et un champ libre (pas une valeur forcée). Signaler ce manque en fin de tâche (alimente les « Écarts connus » d'A13).

**Critères d'acceptation :** parcours complet réalisable à la souris et au clavier, 7 étapes, reprise après rechargement, progression = `GET /progress`, aucune donnée mockée, testé.

**Tests imposés :** `frontend/src/__tests__/OnboardingWizardPage.test.tsx` (≥ 12 cas : rendu du contrat, ordre, complétion sans données, `data` envoyée, skip + motif requis, skip interdit, erreur 400 champ, 403, 409 ordre, reprise, complétion finale, `isCompleted` réaffichage) + `frontend/src/__tests__/useOnboardingWizard.test.tsx` (invalidation de cache).

**Preuve :** `npm test -- src/__tests__/OnboardingWizardPage.test.tsx src/__tests__/useOnboardingWizard.test.tsx` + `npm run build`.

### B2 🔴 — Bannière et redirection d'onboarding post-connexion (constat F2)

**Fichiers :**
- NEW `frontend/src/components/onboarding/OnboardingBanner.tsx` (consomme `GET /onboarding-wizard/status`, dismissible pour la session, lien vers `/onboarding-wizard`)
- MOD `frontend/src/layouts/MainLayout.tsx` (monter la bannière **au même endroit que `ImpersonationBanner`**, c'est-à-dire juste après `<Navbar>` et avant `<ImpersonationBanner />` — `MainLayout.tsx:56-59`)
- MOD `frontend/src/pages/LoginPage.tsx` — **et uniquement ce fichier** (choix tranché, voir ci-dessous). **Interdiction de modifier `AuthContext.tsx`** pour cette tâche.

**Décision tranchée — « redirection » vs D8 « jamais bloquant » :**
La version précédente de cette tâche disait « *redirection douce, jamais bloquante* » puis prescrivait `navigate('/onboarding-wizard')` : c'est contradictoire, car un `navigate` forcé **est** bloquant (un pasteur qui finit sa 2FA se retrouve arraché de sa destination). D8 tranche, la cohérence l'emporte :
1. **Aucune redirection automatique.** `LoginPage.tsx` conserve ses `navigate('/dashboard')` actuels (lignes 112, 141, 153, 436) — **ne pas les modifier**.
2. Le signalement se fait **exclusivement** par la bannière `OnboardingBanner` (CTA vers `/onboarding-wizard`), qui est dismissible.
3. Exception unique et documentée : rediriger **une seule fois**, seulement si l'utilisateur arrive sur `/dashboard` (ou `/`) et que `?onboarding=1` est présent dans l'URL — jamais en sortie de 2FA, jamais en sortie d'invitation. Cette exception est **optionnelle** ; si elle ajoute un risque de boucle, l'omettre entièrement et le documenter.
> Pourquoi `LoginPage` et pas `AuthContext` : `AuthContext` est monté sur **toutes** les pages ; y mettre une navigation casserait le refresh de session, les routes 2FA (`/verify-2fa`) et les tests existants. `LoginPage` est le seul point de connexion contrôlable.

**Spécification :** décision D8 (jamais bloquant). Si l'appel `/status` échoue → bannière masquée, aucune redirection (silencieux, pas de boucle). Clés i18n `onboarding.banner.*` dans les 6 locales.

**Tests imposés :** `frontend/src/__tests__/OnboardingBanner.test.tsx` (affiché si `completed=false`, masqué si `true`, lien correct, erreur API → masqué) + ajout dans `frontend/src/__tests__/AuthJourneys.test.tsx` (redirection quand non complété, pas de redirection quand complété).

**Preuve :** `npm test -- src/__tests__/OnboardingBanner.test.tsx src/__tests__/AuthJourneys.test.tsx`.

### B3 🟠 — Page publique de suivi de demande d'inscription (constat F2)

**Fichiers :**
- NEW `frontend/src/pages/RegistrationStatusPage.tsx` (route publique `/registration-status`, dans `AuthLayout`)
- MOD `frontend/src/App.tsx` (route + lazy import)
- MOD `frontend/src/pages/RegisterPage.tsx` (plan canonique depuis `?plan=`, lien « Suivre ma demande » vers `/registration-status?email=…`)

**Spécification :** formulaire email → `POST /auth/registration-status` → afficher `status` (`PENDING_APPROVAL`, `APPROVED`, `REJECTED` avec `reason`, `NONE`), message + action (`canLogin` → bouton « Se connecter ») ; gérer `429` (message d'attente), `Cache-Control: no-store` côté appel (pas de cache React Query : `staleTime: 0`, `gcTime: 0`).

**Tests imposés :** `frontend/src/__tests__/RegistrationStatusPage.test.tsx` (5 cas : NONE, PENDING, APPROVED + bouton login, REJECTED + motif, 429).

**Preuve :** `npm test -- src/__tests__/RegistrationStatusPage.test.tsx`.

### B4 🟠 — Invitations admin complètes (constat F3)

**Fichiers :** MOD `frontend/src/pages/TenantAdminInvitationsPage.tsx` + NEW `frontend/src/__tests__/TenantAdminInvitationsPage.test.tsx`.

**Spécification :**
1. Bouton **Renvoyer** par ligne (`POST /admin/invitations/{id}/resend`) avec toast `emailSent === false` → avertissement explicite « SMTP indisponible : copiez le lien manuellement ».
2. À la création, afficher et permettre de **copier** `invitationLink` (`navigator.clipboard.writeText`) dans une modale ; si `emailSent === false`, afficher un bandeau d'alerte.
3. **Scope** : sélecteur `TENANT` | `ORGANIZATION` avec choix du nœud (chargé depuis l'API d'arborescence existante utilisée par l'app ; vérifier le chemin réel et réutiliser le hook existant) ; envoyer `scopeType` + `scopeId`/`organizationNodeId` conformément au backend.
4. Gérer `requiresTenantSwitch: true` (message « cet email appartient déjà à une autre église : l'invitation a été créée, l'utilisateur devra changer d'organisation »).
5. Filtres de statut + affichage des invitations expirées (`expiresAt < now` → badge « Expirée ») ; remplacer `alert()` par le système `toast` existant.

**Tests imposés :** création (avec/sans scope), renvoi, copie du lien, `emailSent=false`, filtre, `requiresTenantSwitch`.

**Preuve :** `npm test -- src/__tests__/TenantAdminInvitationsPage.test.tsx`.

### B5 🟠 — Admin tenants : plans canoniques, réactivation, stats réelles, état d'onboarding (constat F4)

**Fichiers :** MOD `frontend/src/pages/AdminTenantsPage.tsx`, MOD `frontend/src/types/index.ts` (ajout `onboardingCompletedAt?: string | null` au type `Tenant`) + NEW `frontend/src/__tests__/AdminTenantsPage.test.tsx`.

**Spécification :**
1. Remplacer `PLAN_OPTIONS` (free/starter/pro/enterprise — confirmed hardcodé à `AdminTenantsPage.tsx:21`) par les plans chargés depuis `GET /platform/admin/plans`. **Endpoint vérifié existant** : `SuperAdminController` est mappé sur `/api/v1/platform/admin` et expose `@GetMapping("/plans")` (ligne 503) → `GET /api/v1/platform/admin/plans`. Fallback : `['DISCOVERY','STARTUP','GROWTH','NETWORK']` — **vérifié** comme étant les 4 clés canoniques seedées par `V144__seed_saas_plans.sql` et purifiées par `V177__complete_canonical_saas_plans.sql` (`WHERE key NOT IN ('DISCOVERY','STARTUP','GROWTH','NETWORK')`). Le backend est protégé par `@authz.isPlatformSuperAdmin()`.
2. Bouton **Réactiver** (`POST /tenants/{id}/reactivate`) visible uniquement si `status === 'SUSPENDED'` ; libellés : « Suspendre l'église » (et non « Supprimer ») ; toast « Église suspendue », pas « supprimée ». **Endpoint vérifié** : `TenantController` (`/api/v1/tenants`), `@PostMapping("/{id}/reactivate")` ligne 75, protégé par `@authz.isPlatformSuperAdmin()`. Un 403 doit être affiché proprement (cette page est super-admin : un 403 signifie session expirée ou rôle révoqué, pas un bug).
3. Supprimer l'appel inutilisé `_tenantStats` (`AdminTenantsPage.tsx:55` — vérifié : variable underscore jamais lue) **et** le remplacer par l'endpoint d'usage réel. **⚠️ Le chemin écrit précédemment dans ce plan était FAUX — R-5 est désormais RÉSOLU :**
   - ❌ `/platform/admin/tenants/{id}/usage` → **n'existe pas**.
   - ✅ `GET /api/v1/platform/admin/quota-usage/tenants/{tenantId}` → `PlatformQuotaUsageController` (`@RequestMapping("/api/v1/platform/admin/quota-usage")`, `@GetMapping("/tenants/{tenantId}")` ligne 33). Un endpoint d'agrégat existe aussi : `GET /api/v1/platform/admin/quota-usage/tenants` (ligne 38).
   - Si l'appel échoue pour un tenant donné : afficher `—` (pas `0`, qui serait un mensonge) et ne jamais bloquer le tableau.
4. Badge « Onboarding : terminé le … » / « En configuration » depuis `onboardingCompletedAt` (champ optionnel → rendu sans crash s'il est absent, conformément à `§6.2`).
5. **Correction d'une contradiction avec D2 :** la version précédente disait « ajouter `ONBOARDING` à la liste des statuts ». Or D2 interdit explicitement tout nouvel état `ONBOARDING` dans `TenantStatus`. La bonne exigence est **la résilience**, pas l'ajout d'un statut : rendre le rendu tolérant à un statut inconnu (afficher la valeur brute traduite, jamais de `crash`, jamais de `switch` sans `default`). **Ne pas** ajouter `ONBOARDING` à une liste de statuts métier.

**Tests imposés :** plans depuis l'API, réactivation, libellés, badge onboarding, résilience statut inconnu.

**Preuve :** `npm test -- src/__tests__/AdminTenantsPage.test.tsx`.

### B6 🟠 — Page abonnement & quotas du tenant (constat F4)

**Fichiers :**
- NEW `frontend/src/pages/TenantAdminSubscriptionPage.tsx` (route `/admin/subscription`, scope tenant)
- MOD `frontend/src/App.tsx` (+ route lazy)
- MOD `frontend/src/workspaces.ts` (+ entrée de menu « Abonnement & quotas »)
- MOD i18n (6 locales, clés `subscription.*`)
- NEW `frontend/src/__tests__/TenantAdminSubscriptionPage.test.tsx`

**Spécification :** consommer `GET /admin/subscription/current`, `GET /admin/subscription/plans`, `GET /admin/subscription/usage`, `POST /admin/subscription/change-plan`, `POST /admin/subscription/cancel`, `POST /admin/subscription/reactivate`.
> **Chemins vérifiés le 2026-09-28** : `SubscriptionController` est mappé sur `/api/v1/admin/subscription` et porte `@PreAuthorize("@authz.isTenantAdmin()")` au niveau **classe**. Endpoints réels : `/current` (37), `/plans` (43), `/subscribe` (64), `/change-plan` (84), `/cancel` (103), `/reactivate` (120), `/usage` (130). Les 6 endpoints listés ci-dessus existent tous. `POST /subscribe` existe aussi mais n'est **pas** requis ici (le tenant est déjà abonné) — ne pas l'appeler.
> Le `403` est donc le comportement nominal pour un non-admin : c'est le cas de test à cover explicitement.

Afficher : plan courant, cycle, période, quotas, plans disponibles avec prix multi-devises (EUR/XAF/USD si fournis), boutons changer/annuler/réactiver avec confirmation. Gérer `403` (non-admin) et les erreurs `QUOTA_*` de downgrade (message du `ProblemDetail`).

> **⚠️ `QuotaUsagePanel` N'EXISTE PAS** — la version précédente de ce plan demandait de « réutiliser `QuotaUsagePanel` », un composant fantôme. Le composant réel est **`frontend/src/components/admin/QuotaUsageCards.tsx`** (vérifié, et déjà testé par `src/__tests__/QuotaUsageCards.test.tsx`). C'est **lui** qu'il faut réutiliser, après avoir vérifié que ses props couvrent le besoin (sinon l'étendre, ne pas le dupliquer).

**Critères d'acceptation :** un admin tenant peut consulter et changer son plan depuis l'UI ; aucune donnée mockée ; downgrade refusé affiché proprement.

**Tests imposés :** rendu des 4 états (chargement, plan courant, erreur, succès), changement de plan, annulation, refus de downgrade (403/400).

**Preuve :** `npm test -- src/__tests__/TenantAdminSubscriptionPage.test.tsx`.

### B12 🟠 — Provisioning web : champs owner (contrat §3.5)

**Fichiers :** MOD `frontend/src/pages/PlatformOnboardingFlowPage.tsx` + MOD `frontend/src/__tests__/PlatformOnboardingFlow.test.tsx` (créer si absent).

**Spécification :** ajouter à l'étape « Organisation » : `ownerEmail` (requis, validé email), `ownerFirstName`, `ownerLastName` (requis) ; les envoyer dans le payload `POST /platform/admin/provisioning` ; bloquer le bouton tant que l'owner n'est pas valide ; afficher au récapitulatif `owner.email` + `owner.activationEmailSent` (si `false` → avertissement « email d'activation non envoyé, transmettez le lien manuellement »).

**Tests imposés :** validation owner (3 cas), payload envoyé conforme, affichage du récapitulatif avec `activationEmailSent=false`.

**Preuve :** `npm test -- src/__tests__/PlatformOnboardingFlow.test.tsx`.

### B7 🟠 — Mobile : wizard d'onboarding tenant (constat MO1)

**Fichiers :**
- NEW `mobile/lib/data/services/tenant_onboarding_service.dart` (contrat §3.1 : `fetchSteps`, `fetchStatus`, `fetchProgress`, `initialize`, `startStep`, `completeStep`, `skipStep`)
- NEW `mobile/lib/models/onboarding_step.dart` (`OnboardingStep`, `OnboardingStatus`, parsing `fromJson` strict avec `FormatException` si champ manquant)
- NEW `mobile/lib/presentation/screens/tenant/tenant_onboarding_screen.dart` (7 étapes : stepper, formulaires, validation locale, reprise, gestion erreurs `STEP_*`/`TENANT_SUSPENDED`)
- NEW `mobile/lib/presentation/widgets/onboarding_banner.dart` (bannière informative, lien vers l'écran d'onboarding, dismissible en session)
- MOD `mobile/lib/app.dart` (route `/tenant/onboarding` — nom `tenant-onboarding`, rôle `TENANT_OWNER`/`ADMIN`/`PASTEUR`, ajout dans la garde d'accès et l'entrée drawer existante)
- MOD `mobile/lib/presentation/screens/tenant/tenant_admin_dashboard_screen.dart` (afficher `OnboardingBanner` si `completed == false`)

**Spécification :** mêmes champs et mêmes `data` que le contrat §3.1 (aucune invention). Après acceptation d'une étape, rafraîchir `status`. À la fin, afficher « Configuration terminée » + bouton vers le dashboard. Aucune donnée en dur.

**Tests imposés (convention du dépôt : fichiers **plats** dans `mobile/test/`) :**
- NEW `mobile/test/tenant_onboarding_service_test.dart` (URLs, payloads, parsing, erreurs).
- NEW `mobile/test/tenant_onboarding_screen_test.dart` (rendu des 7 étapes, complétion sans data, skip avec motif, erreur 409, reprise, fin de parcours).
- NEW `mobile/test/onboarding_banner_test.dart` (affiché/masqué/erreur).

**Preuve :** `flutter test test/tenant_onboarding_service_test.dart test/tenant_onboarding_screen_test.dart test/onboarding_banner_test.dart` + `flutter analyze`.

### B8 🔴 — Mobile : deep links d'invitation (constat MO2)

**Fichiers :**
- MOD `mobile/android/app/src/main/AndroidManifest.xml` (2 `intent-filter` : App Links + scheme custom)
- MOD `mobile/ios/Runner/Info.plist` (URL scheme + `FlutterDeepLinkingEnabled`)
- NEW `mobile/ios/Runner/Runner.entitlements` (+ `CODE_SIGN_ENTITLEMENTS` dans `mobile/ios/Runner.xcodeproj/project.pbxproj`) — **indispensable aux Universal Links iOS**
- NEW `infra/well-known/assetlinks.json.example`
- NEW `infra/well-known/apple-app-site-association.example`
- NEW `mobile/test/invitation_deeplink_test.dart`

**Spécification :**

> **DÉFAUT 1 — Le scheme custom était cassé et n'aurait JAMAIS fonctionné.**
> `invitationTokenFromUri` (`mobile/lib/core/invitation_token.dart:9-16`) commence par :
> `if (uri.path != '/accept-invitation' || uri.fragment.isNotEmpty) return null;`
> Or le filtre `<data android:scheme="discipolat" android:host="accept-invitation"/>` produit l'URI `discipolat://accept-invitation?token=…`, dont le **path est vide** et dont `accept-invitation` est le **host**. La fonction retournerait `null` → l'écran s'ouvrirait **sans jeton**, silencieusement.
> **Correction retenue :** conserver la forme attendue par le parseur existant (`path == '/accept-invitation'`) en déclarant le host `app.discipolat.com` + `pathPrefix`. **Ne pas modifier `invitationTokenFromUri`** (déjà testé par `invitation_token_test.dart` et `invitation_route_test.dart`) : c'est la manifeste, pas le parseur, qui doit produire la bonne forme.
> Si l'orchestrateur veut malgré tout `discipolat://accept-invitation`, il faut une **modification explicite et testée** du parseur, à valider séparément — **hors périmètre de B8**.

0. **Identifiants d'application — valeurs réelles, pas inventées :**
   - Android : `applicationId`/`namespace` = `com.discipolat.discipolat_mobile` (`android/app/build.gradle` lignes 29 et 9).
   - iOS : `PRODUCT_BUNDLE_IDENTIFIER` = `com.discipolat.discipolatMobile` (`ios/Runner.xcodeproj/project.pbxproj` ligne 371).
   - La version précédente imposait `com.discipolat.mobile` pour les deux : **valeur qui ne correspond à aucun bundle réel**, rendant `assetlinks.json` et `apple-app-site-association` inopérants.
1. Android — dans `<activity android:name=".MainActivity">` (l'activité existe déjà ; `android:exported="true"` et `launchMode="singleTop"` sont déjà corrects) ajouter **deux** `intent-filter` :
   ```xml
   <!-- App Links (https) : path = /accept-invitation → compatible parseur existant -->
   <intent-filter android:autoVerify="true">
     <action android:name="android.intent.action.VIEW"/>
     <category android:name="android.intent.category.DEFAULT"/>
     <category android:name="android.intent.category.BROWSABLE"/>
     <data android:scheme="https" android:host="app.discipolat.com" android:pathPrefix="/accept-invitation"/>
   </intent-filter>
   <!-- Scheme custom : path = /accept-invitation → compatible parseur existant -->
   <intent-filter>
     <action android:name="android.intent.action.VIEW"/>
     <category android:name="android.intent.category.DEFAULT"/>
     <category android:name="android.intent.category.BROWSABLE"/>
     <data android:scheme="discipolat" android:host="app.discipolat.com" android:pathPrefix="/accept-invitation"/>
   </intent-filter>
   ```
   Les deux formes produites sont `https://app.discipolat.com/accept-invitation?token=…` et `discipolat://app.discipolat.com/accept-invitation?token=…`, **toutes deux avec `path == '/accept-invitation'`**, donc acceptées sans modifier le parseur.
   > `autoVerify="true"` suppose que `https://app.discipolat.com/.well-known/assetlinks.json` est servi en production. C'est une action **ops** : tant qu'elle n'est pas faite, Android ouvrira le navigateur au lieu de l'app. Le signaler dans le rapport, sans prétendre que c'est vérifié.

2. iOS — **deux** fichiers sont nécessaires, pas un seul :
   - `ios/Runner/Info.plist` — ajouter :
   ```xml
   <key>FlutterDeepLinkingEnabled</key><true/>
   <key>CFBundleURLTypes</key>
   <array><dict>
     <key>CFBundleURLName</key><string>com.discipolat.discipolatMobile</string>
     <key>CFBundleURLSchemes</key><array><string>discipolat</string></array>
   </dict></array>
   ```
   - **NEW `ios/Runner/Runner.entitlements`** — sans ce fichier, les **Universal Links iOS ne fonctionnent pas**, quel que soit le contenu de `Info.plist`. À créer **et** à référencer dans `project.pbxproj` (`CODE_SIGN_ENTITLEMENTS`) :
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
   <plist version="1.0">
   <dict>
     <key>com.apple.developer.associated-domains</key>
     <array><string>applinks:app.discipolat.com</string></array>
   </dict>
   </plist>
   ```
   > Cible de déploiement iOS = `13.0`, compatible Universal Links (iOS 11+). `CFBundleURLName` doit reprendre l'identifiant de bundle **iOS**, pas l'`applicationId` Android.

3. Fichiers `.well-known` d'exemple avec placeholders `<SHA256_CERT_FINGERPRINT>` et `<TEAM_ID>`, et les **identifiants réels** (`com.discipolat.discipolat_mobile` / `com.discipolat.discipolatMobile`) + commentaire indiquant que l'hébergement `https://app.discipolat.com/.well-known/...` est une action **ops** (hors code).
   > `infra/` existe déjà (`monitoring/`, `nginx/`) mais **`infra/well-known/` est absent** : c'est bien un nouveau dossier, comme annoncé en `P0.4`.
   > Vigilance ops : `assetlinks.json` doit être servi en `application/json` **sans redirection**, sinon `autoVerify` échoue en silence.

4. Le routeur doit traiter les deux formes d'URL. **Le parseur est déjà correct pour les deux** dès lors que la manifeste produit `path == '/accept-invitation'` (cf. DÉFAUT 1) : `go_router` est déjà câblé sur `/accept-invitation` avec `initialToken: invitationTokenFromUri(state.uri)` (`lib/app.dart:1395-1398`), et cette route est explicitement **exclue** du redirect de garde (`lib/app.dart:1308`). **Aucune modification de `app.dart` n'est nécessaire pour B8** — ne pas y toucher.

5. **Format du jeton — cohérent et vérifié, ne rien changer :** le backend génère `UUID.randomUUID().toString().replace("-","").substring(0, 32)` (`InvitationController.java:166`), soit 32 hexadécimaux, ce qui correspond exactement au motif `^[0-9a-f]{32}$` de `normalizeInvitationToken`. Le `token_hash` (SHA-256, `V173`) est un détail **serveur** : le client manipule le jeton **en clair** et ne doit jamais le hacher.

**Tests imposés :** `invitation_deeplink_test.dart` : https valide, scheme custom valide (`discipolat://app.discipolat.com/accept-invitation?token=…`), token manquant, token non conforme (32 hex), fragment parasite, query dupliquée, mauvais chemin → `null`. **Couvrir explicitement `discipolat://accept-invitation?token=…` (host sans path) → attendu `null`**, pour verrouiller la correction du DÉFAUT 1.
> `invitation_token_test.dart` et `invitation_route_test.dart` existent déjà et couvrent le parseur : les **lire d'abord** et n'ajouter que les cas réellement nouveaux — pas de duplication.

**Preuve :** `flutter test test/invitation_deeplink_test.dart test/invitation_token_test.dart test/invitation_route_test.dart` + `flutter analyze`.

### B9 🟠 — Mobile : gestion complète des invitations (constat MO3)

**Fichiers :**
- NEW `mobile/lib/data/services/invitation_admin_service.dart` (list, create avec scope, resend, cancel, validate, accept)
- NEW `mobile/lib/presentation/screens/invitations/invitation_management_screen.dart` (liste, filtres statut, renvoi, annulation, copie du lien, création avec scope)
- MOD `mobile/lib/presentation/screens/tenant/users_screen.dart` (réutiliser le service ; conserver l'envoi existant mais déléguer au service)
- MOD `mobile/lib/app.dart` (+ route `/tenant/invitations`, garde `TENANT_OWNER`/`ADMIN`/`PASTEUR`)
- NEW `mobile/test/invitation_admin_service_test.dart`
- NEW `mobile/test/invitation_management_screen_test.dart`

**Spécification :** mêmes endpoints que le web (`/admin/invitations`, `/{id}/resend`, `DELETE /{id}`), gestion `emailSent=false` (SnackBar d'avertissement + bouton « Copier le lien »), affichage `expiresAt`, statut, rôle, et `requiresTenantSwitch` à la création. Aucun `alert`.

**Critères d'acceptation :** un admin mobile peut inviter, relancer, annuler et copier un lien d'invitation.

**Preuve :** `flutter test test/invitation_admin_service_test.dart test/invitation_management_screen_test.dart`.

### B10 🟠 — Mobile : provisioning complet (plans + géo + owner) (constat MO4)

**Fichiers :** MOD `mobile/lib/presentation/screens/platform/super_admin_provisioning_screen.dart` + MOD `mobile/test/super_admin_provisioning_screen_test.dart`.

**Spécification :**
1. Charger les plans via `GET /platform/admin/plans` (**endpoint vérifié** : `SuperAdminController`, `@GetMapping("/plans")` ligne 503, protégé par `@authz.isPlatformSuperAdmin()`). Fallback `['DISCOVERY','STARTUP','GROWTH','NETWORK']` (**vérifié** : 4 clés canoniques, `V144` + `V177`) si l'appel échoue, **avec message visible** — un fallback silencieux est un mensonge.
2. Sélecteurs pays / devise / fuseau. **Question tranchée après vérification (2026-09-28) : il n'existe AUCUNE API de référence** — pas de `/currencies`, pas de `CurrencyController` dans le backend. La consigne « vérifier `/currencies` ou `CurrencyController` » de la version précédente était donc une consigne **impossible à satisfaire**.
   → Consigne retenue : listes statiques **minimales et documentées**, clairement étiquetées comme valeurs de **défaut de sélecteur** (D12), avec saisie libre possible pour ne pas enfermer l'utilisateur. Ne pas élargir la liste « parce que c'est plus joli » : chaque option doit être justifiée dans le rapport.
3. Ajouter les champs owner (`ownerEmail` requis + validation format email, `ownerFirstName`, `ownerLastName` requis) et les envoyer conformément au contrat §3.5 ; afficher `owner.activationEmailSent`, et si `false`, un avertissement actionnable (« email d'activation non envoyé, transmettez le lien manuellement ») — jamais un simple `false` nu.
4. Supprimer les valeurs en dur **imposées** du flux. État réel vérifié dans `super_admin_provisioning_screen.dart` : `'free'` (lignes 44, 244, 378, 385), `'CM'`, `'XAF'`, `'Africa/Douala'` envoyés **systématiquement** dans le payload (lignes 186-188). Elles peuvent rester comme **valeur initiale du sélecteur**, jamais comme valeur envoyée sans choix explicite de l'utilisateur.
   > `GET /platform/admin/plans` est un endpoint **super-admin** : le mobile doit gérer proprement un 403 (session expirée) sans laisser un écran bloqué.

**Tests imposés :** chargement des plans, validation owner, payload conforme, `activationEmailSent=false` affiché, échec API plans → fallback + message.

**Preuve :** `flutter test test/super_admin_provisioning_screen_test.dart`.

### B11 🟠 — Mobile : auto-login après acceptation d'invitation (constat MO3/mineur)

**Fichiers :** MOD `mobile/lib/presentation/screens/invitations/accept_invitation_screen.dart` + MOD `mobile/test/accept_invitation_screen_test.dart`.

**Spécification :**
1. Si l'acceptation a créé un compte (mot de passe saisi) → appeler `POST /auth/login` avec `{ email: invitation.email, password }`, `saveTokens(...)`, charger `/auth/me` (ou le provider d'auth existant), puis `context.go(roleHome(...))`.
2. Si `accountExists` (pas de mot de passe saisi) → ne pas tenter de login, afficher « Connectez-vous avec votre mot de passe » + bouton `/login`.
3. Si `crossTenantIdentity === true` → message spécifique + redirection vers `/tenant-selection` après login.
4. En cas d'échec du login automatique → fallback `/login` avec message non bloquant (jamais de boucle).

**Tests imposés :** auto-login réussi, `accountExists` sans login, `crossTenantIdentity` → sélection tenant, échec login → fallback, aucun double appel.

**Preuve :** `flutter test test/accept_invitation_screen_test.dart`.

### B13 🟡 — Qualité clients : i18n, lint, build, analyse

**Fichiers :** tous les fichiers B modifiés.

**Spécification et critères d'acceptation :**
1. Toutes les nouvelles chaînes UI passent par `tText(...)` ou une clé `useI18n().t(...)`, dans les 6 locales (`ar, en, es, fr, pt, sw`). **Rappel mechanismique** : `tText` indexe par **valeur française** — une chaîne absente de `fr.ts` retombe silencieusement en français dans les 5 autres locales. Toute chaîne nouvelle doit donc être ajoutée dans `fr.ts` en premier, puis dans les 5 autres.
2. `npm run lint` → **0 erreur**. Attention : le script du dépôt est `eslint . --report-unused-disable-directives --max-warnings 1000` — il **ne peut pas échouer sur les warnings**. « 0 erreur » se prouve par le code de sortie, pas par l'absence de texte dans la sortie.
3. `npm run build` → succès (`tsc -b && vite build`, TypeScript strict).
4. `npm test` → succès complet. **⚠️ Correction d'un critère non mesurable :** la version précédente exigeait « ≥ 325 tests de référence » et « ≥ 356 tests mobile » — ces nombres ne sont **vérifiables par personne** (le dépôt compte aujourd'hui 48 fichiers de test frontend et 85 fichiers Dart ; le nombre de *cas* n'est connu qu'après exécution). Un critère qu'on ne peut pas mesurer ne peut pas être un gate. Le critère devient : **le nombre de tests exécutés est ≥ celui mesuré en `P0.3`, et 0 échec**. Le compteur exact est consigné dans `agentB.md`.
5. `flutter analyze` → 0 erreur, et **0 nouvelle remarque** (comparer avec la sortie de `P0.3`, pas avec zéro).
6. `flutter test` → succès complet, avec la même règle de non-régression que (4).
7. **Vérification i18n de non-régression** (à ajouter, car c'est le seul moyen de prouver le point 1) : comparer l'ensemble des clés des 6 locales contre `fr.ts`. L'égalité parfaite **n'est pas atteignable dans le périmètre de B13** ; le critère applicable est donc : **ne pas ajouter de divergence** (delta des 6 locales ≤ delta mesuré en `P0.3`). État mesuré le 2026-09-28 :

   | Locale | clés présentes | absentes vs `fr` | **en trop vs `fr`** | valeur identique au FR |
   |---|---|---|---|---|
   | `fr` (référence) | 2571 | — | — | — |
   | `en` | 2583 | 1 | 13 | 0 |
   | `pt` | 2629 | 0 | 58 | 0 |
   | `es` | 2713 | 0 | 142 | 0 |
   | `sw` | 2713 | 0 | 142 | **728** |
   | `ar` | 2757 | 0 | 186 | **721** |

   > Ces chiffres prouvent qu'un garde-fou de type « mêmes clés dans les 6 langues » **échouerait aujourd'hui** : les 6 jeux de clés diffèrent, `en` a même 1 clé absente et 13 en trop. Le chantier i18n complet dépasse B13 et relève de `AGENT_ORCHESTRATION.md` (constat M4, prompt B1). B13 ne doit pas tenter de réparer 1 700+ chaînes ; il doit **ne pas en ajouter** et **prouver** que les siennes sont complètes.

**Preuve :** sorties complètes des 6 commandes archivées dans `agentB.md`.

**Deltas doc (pour A13) à remplir par l'Agent B :** chemins des nouveaux écrans web/mobile, routes ajoutées, endpoints consommés, deep links (domaine + schemes + fichiers `.well-known`), limites connues.

---

## 6. SÉQUENÇAGE, DÉPENDANCES ET POINTS DE SYNCHRONISATION

### 6.1 Phase 0 — Préparation (30 min, les deux agents, avant tout code)

| Étape | Agent A | Agent B |
|---|---|---|
| P0.1 | ✅ **DÉJÀ FAIT** — worktree `../discipolat_app-agentA` sur `fix/onboarding-tenant-backend` depuis `d730771` (vérifié 2026-09-28) | **À FAIRE** — `git worktree add ../discipolat_app-agentB -b fix/onboarding-tenant-clients d730771` |
| P0.2 | Lire ce plan en entier ; créer `reports/plan-2agents/agentA.md` avec la ligne « baseline » | Lire ce plan en entier ; créer `reports/plan-2agents/agentB.md` avec la ligne « baseline » |
| P0.3 | Baseline backend : `mvn -B -DskipTests compile` puis `mvn -B verify` — **noter le nombre de tests réellement exécuté et le nombre d'échecs** (c'est ce chiffre, et lui seul, qui sera opposable en A12) | Baseline clients : `npm ci && npm run build && npm test` et `flutter pub get && flutter analyze && flutter test` — **consigner les compteurs réels** (nb de tests, nb d'erreurs lint, nb de remarques `flutter analyze`) + l'état i18n des 6 locales |
| P0.4 | Confirmer la disponibilité des numéros V178/V179/V180 (aucun fichier existant) | Confirmer l'absence de fichiers dans `infra/well-known/` |

### 6.2 Dépendances inter-agents

| Tâche B | Dépend de A | Nature | Comment B avance sans A |
|---|---|---|---|
| B1, B2, B3, B4, B12 | A3, A4, A6, A5 (contrat §3) | Contrat HTTP | Développer contre le contrat §3 + mocks de test ; l'E2E réel se fait en Phase 2 |
| B5 | A4 (`onboardingCompletedAt`) | Champ additif | Utiliser `onboardingCompletedAt?: string \| null` **optionnel** → aucun crash si absent |
| B6 | A8 (quotas écriture) | Comportement | La page fonctionne sans A8 ; les erreurs `QUOTA_*` sont déjà gérées |
| B7 | A3, A4 | Contrat HTTP | Idem B1 |
| B8, B9, B10, B11 | — | Aucun | Totalement indépendants |
| B13 | — | Qualité | Après toutes les tâches B |

| Tâche A | Dépend de B | Nature |
|---|---|---|
| A13 (docs) | B13 (deltas doc) | Documentation : A attend la section « Deltas doc » de `agentB.md` (ou documente en marquant « à confirmer ») |
| A16 | — | Indépendant |

**Aucune tâche A ne dépend du code B, et aucune tâche B ne dépend du code A pour écrire du code.** Les seules dépendances sont documentaires (A13) et de vérification (Phase 2).

### 6.3 Points de rendez-vous (facultatifs mais recommandés)

- **RDV-1 (fin de A3/B1)** : échange des 2 fichiers de progression uniquement (lecture). Objectif : confirmer que le contrat est respecté des deux côtés. Aucun échange de code.
- **RDV-2 (avant A12/B13)** : exécution mutuelle des commandes de l'autre camp sur son propre worktree (lecture seule) pour anticiper l'intégration.
- **Toute modification de contrat est interdite** : voir R2/R7.

### 6.4 Fusion (fin de Phase 1)

1. L'agent A fusionne `fix/onboarding-tenant-clients` dans `fix/onboarding-tenant-backend` **localement** (`git merge --no-ff`) : aucun conflit attendu (zones disjointes). En cas de conflit → arrêt + rapport (`R7`).
2. Sur la branche fusionnée : `mvn -B verify` (A) **puis** `npm ci && npm run build && npm test` (B) **puis** `flutter pub get && flutter analyze && flutter test` (B).
3. Aucun `push` sans décision de l'orchestrateur.

---

## 7. PHASE 2 — VÉRIFICATION, INTÉGRATION ET GATES

### 7.1 Commandes de vérification par couche (preuves obligatoires)

**Backend (Agent A) :**
```bash
mvn -B -DskipTests compile
mvn -B verify
```
**Frontend (Agent B) :**
```bash
npm ci
npm run lint
npm run build
npm test
```
> `package.json` déclare déjà `"test": "vitest run"` : le `test` est **déjà en mode run**, donc `npm test -- --run` (syntaxe de la version précédente de ce plan) est **redondant**. Utiliser `npm test`, ou `npm test -- <fichier>` pour cibler un test.
> `npm run lint` ne peut pas échouer sur les warnings (`--max-warnings 1000`) : seuls les **erreurs** font sortir en code ≠ 0. Un « lint vert » se prouve par le code de sortie.
**Mobile (Agent B) :**
```bash
flutter pub get
flutter analyze
flutter test
```

### 7.2 Gate G-A (backend) — critères de refus

1. Un `orElseThrow()` nu subsiste dans `onboarding/**` → **REFUS**.
2. Un test est désactivé → **REFUS**.
3. Une migration modifie un fichier existant ou utilise un numéro ≠ V178/V179/V180 → **REFUS**.
4. Le nombre total de tests `mvn verify` est inférieur à la baseline P0.3 → **REFUS**.
5. Une preuve manque dans `agentA.md` → **REFUS** de la tâche concernée.

### 7.3 Gate G-B (clients) — critères de refus

1. `npm run build` ou `flutter analyze` en erreur → **REFUS**.
2. Un test de page/écran exigé par la tâche est absent → **REFUS** de la tâche.
3. Une chaîne UI visible par l'utilisateur est rendue **sans passer** par `tText(...)` ou une clé `useI18n().t(...)` → **REFUS** (B13).
   > **⚠️ Correction de rédaction.** La version précédente disait « une chaîne UI **codée en dur** hors i18n → REFUS ». Lu littéralement, ce critère est **impossible à satisfaire et faux** : la convention du dépôt **est** `tText('chaîne française')`, c'est-à-dire du texte français passé en littéral. Un agent qui l'applique au pied de la lettre devrait réécrire le dépôt entier, ou pire, supprimer des libellés. Le critère porte donc sur le **routage i18n** (le texte passe-t-il par le mécanisme ?), pas sur la présence d'un littéral. Le test automatisé associé est la comparaison des clés des 6 locales (B13.7).
4. Un appel API utilise un chemin ou un champ **inexistant** ou **hors du contrat §3** → **REFUS**.
   > **⚠️ Correction de périmètre.** Tel quel, ce critère était invérifiable et absurde : il interdisait aussi des endpoints **réels, vérifiés et nécessaires**, que le plan lui-même prescrit dans ses propres tâches. Endpoints **autorisés** (vérifiés sur le backend le 2026-09-28) :
   > - `GET /platform/admin/plans` (B5, B10) — `SuperAdminController:503`
   > - `POST /tenants/{id}/reactivate` (B5) — `TenantController:75`
   > - `GET /platform/admin/quota-usage/tenants/{id}` (B5) — `PlatformQuotaUsageController:33`
   > - `GET|POST /admin/subscription/**` (B6) — `SubscriptionController`
   > - `GET /admin/tenant-features` (B1) — `TenantFeatureController`
   > - `GET /public/plans` (B3) — `PublicSaasPlanController`
   >
   > Le critère s'applique à tout **autre** chemin, et à tout champ non présent dans le DTO réellement renvoyé. Un chemin cité dans ce plan mais absent du code est un **besoin backend** → `R7` (NEED-HELP), pas une invention côté client.
5. Une donnée en dur (plan/pays/devise/fuseau) utilisée comme valeur forcée → **REFUS** (B5/B10).
6. **Qualité d'interface (`§5.0`) — REFUS de la tâche concernée :**
   - un composant a été créé alors qu'un composant existant du design system couvrait le besoin (doublon) ;
   - une vue livrée sans l'un des 5 états (chargement / vide / erreur / succès / hors-ligne) ;
   - une animation non annulée sous `prefers-reduced-motion`, ou dépassant 400 ms, ou animant `width`/`height`/`top`/`left` ;
   - une propriété directionnelle en dur (`margin-left`, `text-left`) dans un composant **nouveau**, ou une cible tactile < 44 px ;
   - une date/heure/devise formatée à la main au lieu de `Intl`/`intl` ;
   - une nouvelle dépendance (`framer-motion`, `three`, autre) introduite sans autorisation.
7. Une tâche B est déclarée `DONE` alors qu'un de ses **critères d'acceptation** n'est pas démontré par un test → **REFUS** (R3/R5).

### 7.4 Scénarios bout-en-bout (exécutés après fusion, backend démarré localement)

| # | Scénario | Attendu |
|---|---|---|
| E2E-1 | Super admin provisionne un tenant avec owner | `201`, `owner.userId` non nul, `activationEmailSent` présent, tenant visible via `GET /tenants/{id}` |
| E2E-2 | Owner active son compte, se connecte, ouvre le wizard | 7 étapes, contrat conforme, `GET /status` `completed=false` |
| E2E-3 | Owner complète les 7 étapes | effets réels : église renommée, département+famille créés, invitations créées, branding modifié, modules activés, événement créé, `completed=true` |
| E2E-4 | Owner refait une étape complétée | `409 STEP_ALREADY_COMPLETED`, aucun doublon créé |
| E2E-5 | Tenant B tente de compléter une étape du tenant A | `404 STEP_NOT_FOUND` |
| E2E-6 | Super admin suspend le tenant, l'owner se connecte | `403 TENANT_SUSPENDED` ; après réactivation, login OK |
| E2E-7 | Invitation nouvel email → acceptation → login | Compte + membership + personne au répertoire, `crossTenantIdentity=false` |
| E2E-8 | Invitation d'un email déjà membre d'une autre église → acceptation | Aucun doublon `users`, `crossTenantIdentity=true`, 2 tenants dans `my-tenants` |
| E2E-9 | Demandeur d'inscription consulte son statut | `PENDING_APPROVAL` puis `APPROVED` + `canLogin=true` |
| E2E-10 | Quota dépassé (utilisateurs/espaces/événements) | `403` code `QUOTA_*` + notification admin créée |
| E2E-11 | Deep link mobile `https://app.discipolat.com/accept-invitation?token=…` | L'app ouvre l'écran d'acceptation, token pré-rempli (recette manuelle documentée) |
| E2E-12 | Web : bannière, wizard, page abonnement, relance d'invitation | Comportements conformes (recette manuelle documentée) |

`scripts/verify-tenant-onboarding.sh` (A14) couvre E2E-1 → E2E-10. E2E-11/12 : recette manuelle journalisée dans `agentB.md`.

### 7.5 Livrable final

- Branche fusionnée `fix/onboarding-tenant-backend` contenant les deux campagnes.
- `reports/plan-2agents/agentA.md` + `agentB.md` complets (aucune tâche `IN_PROGRESS`/`BLOCKED` sans NEED-HELP).
- `reports/plan-2agents/VERIFICATION.md` rédigé par l'agent de vérification (voir prompt §8.3) contenant : la matrice de traçabilité `§9`, les sorties des gates G-A/G-B, le résultat des E2E, et la liste des écarts résiduels assumés.
- Aucun `push`, aucun tag, aucune modification de `main`.

---

## 8. PROMPTS PRÊTS À COPIER (à coller tel quel dans chaque agent)

### 8.1 PROMPT — AGENT A (BACKEND)

```text
Tu es l'AGENT A (BACKEND) du plan de correctifs Onboarding/Tenant.

CONTEXTE
- Dépôt : C:\Users\PC\IdeaProjects\discipolat_app — sur Windows, PowerShell.
- Ton worktree de travail : `../discipolat_app-agentA` (branche `fix/onboarding-tenant-backend`, créée depuis `d730771`). **Déjà créé — vérifie avec `git worktree list`.**
- Fichier d'autorité : PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md (racine du dépôt). Lis-le INTÉGRALEMENT avant toute action.
- Le fichier d'autorité est en LECTURE SEULE : ne le modifie jamais.

TA ZONE EXCLUSIVE
backend/** , scripts/** , .github/** , docs/** , reports/** (sauf reports/plan-2agents/agentB.md qui appartient à l'Agent B).
Interdiction absolue de toucher à frontend/** , mobile/** , infra/** (sauf documenter leurs besoins dans ton rapport).

TES TÂCHES (dans cet ordre, cf. §4 du plan)
A7 (unicité email + cross-tenant), A1 (garde de suspension), A2 (audit tenant), A3 (wizard réel),
A4 (complétion onboarding + /status), A5 (owner provisioning), A6 (emails inscription + /registration-status),
A8 (quotas espaces/événements + alerte), A9 (répertoire + bienvenue + relances), A10 (finitions),
A11 (tests sécurité), A15 (magic-link args), A12 (mvn verify), A14 (script E2E + CI), A13 (docs véridiques), A16 (OpenAPI).
Chaque tâche contient : fichiers exacts, spécification, critères d'acceptation, tests imposés, commande de preuve. Suis-les à la lettre.

RÈGLES ABSOLUES (reprises de §1, à appliquer sans exception)
R1 un écrivain par fichier + worktree dédié ; R2 contrat §3 figé (aucun endpoint/champ renommé ou ajouté hors plan) ;
R3 DoD complète pour chaque tâche ; R4 aucun stub/TODO/reformatage hors périmètre ;
R5 preuve = sortie de commande archivée ; R6 un commit par tâche nommé `<type>(<taskId>): ...` ;
R7 si un point du plan est irréalisable ou contredit le code : STOP, écris un bloc `### NEED-HELP — <taskId>` dans ton fichier de progression, et passe à une tâche indépendante (n'improvise JAMAIS) ;
R9 aucun test désactivé ; R10 aucune doc mensongère ; R11 respecte l'ordre ; R12 zéro amélioration opportuniste.

TRAVAIL ATTENDU À CHAQUE TÂCHE
1. Lis le code concerné (ne devine pas les signatures : vérifie les méthodes des services avant de les appeler).
2. Implémente, écris les tests imposés, exécute-les.
3. Archive dans `reports/plan-2agents/agentA.md` (que tu crées en phase 0) : Statut / Commit / Fichiers / Tests (commande exacte + résultat) / Preuve (extrait).

CONTRAINTES TECHNIQUES VÉRIFIÉES (à réutiliser, pas à réinventer)
- Erreurs métier : DomainException(message, HttpStatus, code) ou BusinessRuleException(message, code) ; GlobalExceptionHandler renvoie un ProblemDetail avec title = code.
- Chemins publics existants : TenantFilter.PUBLIC_PATH_PREFIXES (auth, public, invitations validate/accept, actuator).
- Intercepteurs : WebMvcConfig.addInterceptors (ordre : tenantInterceptor, tenantFilterInterceptor, ...).
- Audit : AuditService.logSimple(action, entiteType, entiteId) ; e-mails : EmailService.send(...) (non bloquant).
- Quotas : QuotaService.checkCanCreate* (fail-closed, BusinessRuleException QUOTA_* → 403).
- Migrations : uniquement V178, V179, V180 (dernière existante V182). Aucune modification de migration existante.
- RBAC : @PreAuthorize("@authz.isTenantAdmin()") pour les mutations, isAuthenticated() pour la lecture wizard.

FIN DE MISSION
- Exécute A12 (`mvn -B verify`) et archive le résultat complet.
- Rédige A13 (docs) en te basant sur les sections « Deltas doc (pour A13) » de `reports/plan-2agents/agentB.md` (si présentes ; sinon marque « à confirmer »).
- Termine par un résumé : tâches DONE, tâches BLOCKED + NEED-HELP, commandes exécutées et résultats.
- Ne pousse rien (`git push` interdit), ne touche pas `main`.
```

### 8.2 PROMPT — AGENT B (CLIENTS WEB + MOBILE)

```text
Tu es l'AGENT B (CLIENTS) du plan de correctifs Onboarding/Tenant.

CONTEXTE
- Dépôt : /home/arise/discipolat/discipolat_app — Linux, bash.
- Ton worktree de travail : `../discipolat_app-agentB` (branche `fix/onboarding-tenant-clients`, créée depuis `d730771`).
  **PREMIÈRE ACTION OBLIGATOIRE, AVANT TOUT CODE :**
      cd /home/arise/discipolat/discipolat_app && git worktree add ../discipolat_app-agentB -b fix/onboarding-tenant-clients d730771
  Puis TRAVAILLE EXCLUSIVEMENT DANS `../discipolat_app-agentB`. Ne produis aucun code dans `discipolat_app` (branche `main`).
- Fichier d'autorité : PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md (racine du dépôt). Lis-le INTÉGRALEMENT avant toute action.
- Le fichier d'autorité est en LECTURE SEULE : ne le modifie jamais.

TA ZONE EXCLUSIVE
frontend/** , mobile/** , infra/well-known/** , reports/plan-2agents/agentB.md.
Interdiction absolue de toucher à backend/** , scripts/** , .github/** , docs/** , infra/** (hors infra/well-known/**) ,
reports/ (hors ton fichier), et reports/plan-2agents/agentA.md.

TES TÂCHES (dans cet ordre, cf. §5 du plan)
B8 (deep links mobile) → B1 (wizard web 7 étapes) → B2 (bannière) → B3 (statut d'inscription)
→ B4 (invitations admin web) → B5 (admin tenants) → B6 (page abonnement) → B12 (owner provisioning web)
→ B7 (wizard mobile) → B9 (gestion invitations mobile) → B10 (provisioning mobile) → B11 (auto-login) → B13 (i18n/lint/build/tests).
Chaque tâche contient : fichiers exacts, spécification, critères d'acceptation, tests imposés, commande de preuve. Suis-les à la lettre.

QUALITÉ D'INTERFACE — EXIGENCE OBLIGATOIRE (§5.0, lue avant B1)
Ton travail n'est pas accepté s'il fonctionne mais qu'il est laid ou incomplet. §5.0 est la **norme** :
- RÉUTILISE le design system existant (`components/ui/UXComponents.tsx` : Skeleton*, EmptyState, ConfirmDialog,
  ProgressBar, OnboardingStepper, VisuallyHidden, useReducedMotion). Ne crée AUCUN doublon. Si un composant
  existe mais manque une prop, étends-le par des props optionnelles rétrocompatibles.
- Chaque vue gère ses 5 états : chargement (squelette), vide, erreur (actionnable + retry), succès, hors-ligne.
- Mouvement/3D SANS dépendance nouvelle : `framer-motion`, `three`, `@react-three/*` sont INTERDITS (absents du
  package.json). Utilise `transform`, `perspective`, `translateZ`, `backdrop-filter`, `shadow-glow`,
  `glass-card-premium`, `bg-gradient-mesh`. Animations ≤ 400 ms, `transform`/`opacity` uniquement,
  annulées sous `prefers-reduced-motion`.
- Accessibilité WCAG 2.1 AA : navigation clavier complète, `aria-current`/`aria-invalid`/`aria-describedby`,
  contraste vérifié en mode clair ET sombre, cibles ≥ 44 px, **RTL correct** (propriétés logiques `ms-`/`me-`,
  jamais `margin-left`/`text-left` en dur), erreur jamais signalée par la couleur seule.
- Formatage par `Intl`/`intl` uniquement ; dates et devises jamais formatées à la main.
- Responsive 360 px minimum ; toute nouvelle page en `lazy()` dans `App.tsx` ; ne pas casser `manualChunks` de `vite.config.ts`.

RÈGLES ABSOLUES (cf. §1)
R1 un écrivain par fichier + worktree dédié ; R2 le contrat §3 est FIGÉ — tu codes contre lui, tu ne l'inventes pas et tu ne le modifies pas ;
si le backend n'expose pas exactement ce contrat, tu écris un bloc `### NEED-HELP — <taskId>` et tu continues sur une tâche indépendante ;
R3 DoD complète ; R4 aucun mock présenté comme du réel, et AUCUNE chaîne visible hors `tText`/clé i18n ;
R5 preuve = sortie de commande archivée dans `reports/plan-2agents/agentB.md` ;
R6 un commit par tâche nommé `<type>(<taskId>): ...` ; R9 aucun test désactivé ;
R10 aucune affirmation non prouvée (notamment : n'écris jamais un nombre de tests « de référence » que tu n'as pas mesuré) ;
R12 zéro amélioration opportuniste — mais §5.0 n'est PAS une amélioration : c'est le contrat.

PIÈGES CONNUS, ÉVITÉS VOLONTAIREMENT DANS CE PLAN (ne les « corrige » pas)
- `OnboardingStepper` existe déjà : ne le recrée pas.
- `QuotaUsageCards` (et non `QuotaUsagePanel`) est le composant de quotas réel.
- Le vrai endpoint d'usage est `GET /platform/admin/quota-usage/tenants/{id}` (et non `/platform/admin/tenants/{id}/usage`).
- Les identifiants réels sont `com.discipolat.discipolat_mobile` (Android) et `com.discipolat.discipolatMobile` (iOS).
- Les Universal Links iOS exigent `ios/Runner/Runner.entitlements` + `CODE_SIGN_ENTITLEMENTS`.
- Le scheme custom doit produire `path == '/accept-invitation'` : ne modifie pas `invitationTokenFromUri`.
- `npm test` est déjà en mode run : n'écris pas `npm test -- --run`.
- Ne bloque pas sur la devise/le fuseau : aucune API de référence n'existe (listes minimales documentées, D12).

TRAVAIL ATTENDU À CHAQUE TÂCHE
1. Lis le code existant de la page/écran concerné et les conventions (hooks TanStack, `api` de `@/lib/api`, `tText`, `GlassCard`, Riverpod, `ApiService`, go_router).
2. Implémente, écris les tests imposés, exécute-les.
3. Archive dans `reports/plan-2agents/agentB.md` : Statut / Commit / Fichiers / Tests / Preuve / « Deltas doc (pour A13) ».
   La section « Deltas doc (pour A13) » doit lister : routes ajoutées (web + mobile), nouveaux écrans, endpoints consommés,
   deep links configurés (domaine, schemes, fichiers .well-known), limites connues. C'est la SEULE information que l'Agent A utilisera pour la documentation.

CONTRAT À RESPECTER (extrait ; le détail complet est en §3)
- Wizard : GET /onboarding-wizard (liste), GET /progress, GET /status, POST /initialize, POST /{id}/start,
  POST /{id}/complete {data facultatif}, POST /{id}/skip {reason si requis}. Codes : STEP_NOT_FOUND, STEP_DATA_INVALID,
  STEP_ORDER_VIOLATION, STEP_ALREADY_COMPLETED, STEP_NOT_SKIPPABLE, STEP_SKIP_REASON_REQUIRED, TENANT_SUSPENDED.
- Suspension : 403 TENANT_SUSPENDED à afficher proprement (jamais un écran blanc).
- Inscription : POST /auth/registration-status {email} → status/decidedAt/reason/canLogin.
- Invitations : réponses enrichies `crossTenantIdentity`, `requiresTenantSwitch`, `emailSent`.
- Provisioning : champs ownerEmail/ownerFirstName/ownerLastName → réponse `owner.activationEmailSent`.
Ne JAMAIS renommer un champ du contrat (ex. `isCompleted`, `stepOrder`, `skipRequiresReason`).

FIN DE MISSION
- Exécute B13 complètement (i18n 6 locales, lint 0 erreur, build, tests web, analyse Flutter, tests Flutter) et archive les sorties.
- Termine par un résumé : tâches DONE, tâches BLOCKED + NEED-HELP, commandes exécutées et résultats.
- Ne pousse rien (`git push` interdit), ne touche pas `main`.
```

### 8.3 PROMPT — AGENT VERIFICATEUR (Phase 2, lecture seule + rapport)

```text
Tu es l'AGENT VERIFICATEUR (Phase 2). Tu ne modifies AUCUN code. Tu produis un rapport de vérification.

CONTEXTE
- Dépôt : C:\Users\PC\IdeaProjects\discipolat_app ; branche fusionnée `fix/onboarding-tenant-backend` (Agent A puis Agent B).
- Fichier d'autorité : PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md (§3 contrat, §7 gates et E2E).
- Preuves fournies par les agents : reports/plan-2agents/agentA.md et agentB.md.

CE QUE TU DOIS FAIRE
1. Exécuter TOI-MÊME, sans faire confiance aux rapports, les commandes de §7.1 (backend, frontend, mobile) et coller les sorties brutes.
2. Appliquer les gates G-A (§7.2) et G-B (§7.3) : lister chaque critère avec PASS/FAIL et la preuve.
3. Vérifier le contrat §3 endpoint par endpoint : exécuter les appels HTTP réels (backend démarré) et comparer les réponses
   aux DTO du plan (noms de champs exacts). Toute divergence = FAIL.
4. Vérifier les greps de non-régression :
   - aucun `orElseThrow()` nu dans backend/src/main/java/com/discipolat/modules/onboarding/** ;
   - aucune occurrence de `findByEmail(` dans AuthService.java ;
   - aucun `findByEmail(` utilisé pour le login sans `IgnoreCase`/`findGlobalByEmail` ;
   - aucune chaîne `PRODUCTION READY` non justifiée dans docs/TENANT_ONBOARDING.md ;
   - aucun `TODO/FIXME` ajouté par les tâches du plan (comparer avec `git diff <BASE_COMMIT>..HEAD`).
5. Exécuter `scripts/verify-tenant-onboarding.sh` et rapporter chaque scénario E2E-1 → E2E-10 (PASS/FAIL + sortie).
6. Vérifier que chaque tâche A*/B* a bien une preuve (commande + résultat) : liste des tâches sans preuve.
7. Vérifier que les migrations V178/V179/V180 sont les seules ajoutées et qu'aucune migration existante n'est modifiée
   (`git diff --stat <BASE_COMMIT>..HEAD -- backend/src/main/resources/db/migration`).
8. Vérifier que le nombre de tests backend/frontend/mobile est ≥ baseline et qu'aucun test n'est désactivé
   (`git diff <BASE_COMMIT>..HEAD | Select-String '@Disabled|xit\(|skip:'`).

LIVRABLE
- Écrire `reports/plan-2agents/VERIFICATION.md` : commandes exécutées + sorties, gates G-A/G-B, contrat endpoint par endpoint,
  scénarios E2E, tâches sans preuve, écarts résiduels, verdict GO / NO-GO argumenté.
- Interdiction de modifier du code ou de la documentation ; uniquement ton rapport.
```

### 8.4 PROMPT — AGENT INTEGRATEUR (fusion + dernier kilomètre)

```text
Tu es l'AGENT INTEGRATEUR. Objectif : fusionner les deux campagnes et livrer une branche propre vérifiée.

ÉTAPES
1. Vérifier que les deux branches existent et qu'aucune n'a été poussée :
   `git branch -vv` ; `git log --oneline d730771..fix/onboarding-tenant-backend` ; idem clients.
2. Contrôler l'absence de chevauchement de fichiers entre branches :
   comparer les listes `git diff --name-only d730771..<branche>` : une intersection non vide = STOP + rapport.
3. Fusion locale (dans le worktree A) : `git merge --no-ff fix/onboarding-tenant-clients -m "merge: onboarding/tenant corrections (A+B)"`.
   Conflit = STOP + rapport (ne jamais résoudre en inventant du code).
4. Rejouer les vérifications complètes (§7.1) sur la branche fusionnée, dans cet ordre : backend → frontend → mobile.
5. Démarrer le backend localement et exécuter `scripts/verify-tenant-onboarding.sh` : tout PASS exigé.
6. Vérifier le diff global : `git diff --stat d730771..HEAD` — aucun fichier hors périmètre (docker-compose, render.yaml, application-prod.yml).
7. Mettre à jour `reports/plan-2agents/INTEGRATION.md` : commandes, résultats, éventuelles tâches NON faites (avec NEED-HELP associés), recommandation.
8. Ne pas pousser, ne pas taguer. Transmettre à l'orchestrateur.
```

---

## 9. MATRICE DE TRAÇABILITÉ — CONSTAT D'AUDIT → TÂCHE → TEST → PREUVE

| Constat d'audit | Sévérité | Tâche(s) | Tests de preuve | Scénario E2E |
|---|---|---|---|---|
| B1 — Suspension tenant non appliquée (login/refresh/switch/API) | 🔴 | A1 (+A2 audit) | `TenantStatusGuardTest`, `TenantStatusInterceptorTest`, `AuthServiceTest` | E2E-6 |
| B2 — Wizard hors contrat, sans métier, 500, RBAC faible, 0 test | 🔴 | A3, A4, A10 (+B1, B2, B7) | `OnboardingWizardServiceTest`, `OnboardingWizardControllerTest`, `OnboardingWizardTenantIsolationTest`, `OnboardingWizardInitializeConcurrencyTest` | E2E-2, E2E-3, E2E-4 |
| B3 — Provisioning sans owner ni invitation | 🔴 | A5 (+B12, B10) | `TenantOwnerProvisioningServiceTest`, `PlatformProvisioningServiceTest` | E2E-1 |
| B4 — Email ambigu au login (doublons cross-tenant) | 🔴 | A7 | `InvitationServiceCrossTenantTest`, `AuthServiceEmailLookupTest` | E2E-7, E2E-8 |
| M1 — Cycle de vie tenant sans audit, statuts inutilisés | 🟠 | A2, A4 | `TenantServiceTest` | E2E-3 |
| M2 — Approbation d'inscription muette, pas d'email, pas de statut | 🟠 | A6 (+B3) | `TenantRegistrationEmailTest`, `AuthControllerRegistrationStatusTest` | E2E-9 |
| M3 — Quotas partiels (espaces, événements, églises), pas d'alerte | 🟠 | A8 | `QuotaServiceSpacesEventsTest`, `SpaceServiceTest`, `EventServiceTest` | E2E-10 |
| M4 — Invitations : répertoire, bienvenue, relances absents | 🟠 | A9 | `InvitationDirectoryRegistrationTest`, `InvitationReminderSchedulerTest` | E2E-7 |
| F1 — `OnboardingWizardPage` inutilisable (contrat + UI + 400) | 🔴 | B1 | `OnboardingWizardPage.test.tsx`, `useOnboardingWizard.test.tsx` | E2E-12 |
| F2 — Aucun parcours post-inscription (bannière, statut) | 🟠 | B2, B3 | `OnboardingBanner.test.tsx`, `AuthJourneys.test.tsx`, `RegistrationStatusPage.test.tsx` | E2E-12 |
| F3 — Invitations admin web incomplètes | 🟠 | B4 | `TenantAdminInvitationsPage.test.tsx` | E2E-12 |
| F4 — Tenants admin : plans legacy, pas de réactivation, stats mortes, pas d'abonnement | 🟠 | B5, B6 | `AdminTenantsPage.test.tsx`, `TenantAdminSubscriptionPage.test.tsx` | E2E-12 |
| MO1 — Aucun onboarding tenant mobile | 🔴 | B7 | `tenant_onboarding_screen_test.dart`, `tenant_onboarding_service_test.dart`, `onboarding_banner_test.dart` | E2E-12 |
| MO2 — Deep links invitation non configurés | 🔴 | B8 | `invitation_deeplink_test.dart` | E2E-11 |
| MO3 — Pas de gestion d'invitations mobile, pas d'auto-login | 🟠 | B9, B11 | `invitation_admin_service_test.dart`, `invitation_management_screen_test.dart`, `accept_invitation_screen_test.dart` | E2E-11 |
| MO4 — Provisioning mobile en dur (plan/géo), champs owner absents | 🟠 | B10 | `super_admin_provisioning_screen_test.dart` | E2E-1 |
| Mineurs (slug, skip reason, config, pagination, magic-link args) | 🟡 | A10, A15 | `OnboardingWizardInitializeConcurrencyTest`, `InvitationControllerTest`, `AuthServiceTest` | — |
| Docs fausses (« PRODUCTION READY », wizard 6 étapes, auto-directory) | 🟠 | A13, A16 | Grep de contrôle (§8.3) + `PublicApiDocsControllerTest` | — |

**Règle de fermeture** : une ligne n'est fermée que si le test listé passe ET que le scénario E2E associé est PASS (ou explicitement « non applicable » avec justification).

---

## 10. JOURNAL DES RISQUES ET DÉCISIONS DIFFÉRÉES

| # | Risque | Impact | Mitigation prévue dans le plan |
|---|---|---|---|
| R-1 | Doublons d'email historiques bloquent la migration V180 | Déploiement impossible | Migration fail-closed avec message explicite ; dédoublonnage **manuel** hors périmètre (décision orchestrateur) |
| R-2 | Limites `spaces`/`events` absentes des plans seedés (V144) | A8 refuse des créations légitimes | D11 fail-closed assumé ; corriger les seeds uniquement si nécessaire — via une **nouvelle** migration et après NEED-HELP |
| R-3 | SMTP absent en local | Emails non vérifiables au réel | Tests sur mocks ; script E2E vérifie `emailSent`/`activationEmailSent` ; réception réelle = recette manuelle documentée |
| R-4 | Deep links non testables en CI | E2E-11 non automatisé | Test unitaire d'analyse d'URI + recette manuelle journalisée (B8) |
| R-5 | ~~`/platform/admin/tenants/{id}/usage` peut ne pas exister~~ | ~~B5 bloqué~~ | **RÉSOLU le 2026-09-28.** Le endpoint existe, sous un autre chemin : `GET /api/v1/platform/admin/quota-usage/tenants/{tenantId}` (`PlatformQuotaUsageController:33`). B5 l'utilise ; en cas d'échec, afficher `—` et ne jamais `0`. |
| R-6 | Catalogue de modules requis pour l'étape MODULES | Codes inconnus refusés | Validation contre `ModuleCatalogService` ; le web charge la liste depuis l'API existante |
| R-7 | Champ additif `onboardingCompletedAt` dans `TenantResponse` | Aucun (additif en fin de record) | Champ optionnel côté clients (B5) |
| R-8 | Deux agents sur la même machine | Builds concurrents lents | Worktrees séparés ; suites lourdes exécutées en fin de tâche, jamais en parallèle |
| R-9 | **Worktree Agent B non créé** (`discipolat_app-agentB` et branche `fix/onboarding-tenant-clients` absents au 2026-09-28) | Un agent B travaille dans `main` → `main` modifiée, violation de R1 et de `§11` | Créer le worktree **avant** tout code (commande en `R1`). `R-8` devient inopérant tant que ce n'est pas fait. |
| R-10 | **Jeu de clés i18n divergent entre les 6 locales** (mesuré : `fr` 2571 clés ; `en` −1/+13 ; `pt` +58 ; `es` +142 ; `sw` +142 et 728 valeurs identiques au FR ; `ar` +186 et 721 identiques) | Un garde-fou « mêmes clés partout » échouerait ; B13 ne pourra pas prouver l'égalité | B13 ne fait que **ne pas agrandir** l'écart. Le rattrapage complet est hors périmètre → `AGENT_ORCHESTRATION.md` constat M4 / prompt B1. |
| R-11 | **Deep links : domaine et `.well-known` non déployés** (`assetlinks.json`, `apple-app-site-association`, entitlement iOS) | `E2E-11` non automatisable en CI ; en production Android ouvrira le navigateur | Tests unitaires d'URI (B8) + recette manuelle journalisée ; publication `.well-known` = action **ops** à faire valider par l'orchestrateur. |
| R-12 | **`usesCleartextTraffic="true"`** présent dans `AndroidManifest.xml` (hors périmètre B8) | Risque sécurité : trafic HTTP clair autorisé sur toutes les versions | Signalé à l'orchestrateur ; **hors périmètre** de ce plan (R12). À traiter dans `AGENT_ORCHESTRATION.md`. |

**Décisions différées (hors périmètre, à trancher séparément)** : suppression/purge réelle d'un tenant (RGPD), facturation Stripe des abonnements, import réel déclenché depuis le wizard, traduction backend des titres d'étapes, chiffrement des liens d'invitation, SSO.

---

## 11. CHECKLIST FINALE D'ACCEPTATION (à cocher par le vérificateur)

- [ ] G-A passé (5 critères) ; G-B passé (**7** critères, dont la qualité d'interface `§5.0`).
- [ ] Contrat §3 vérifié endpoint par endpoint (aucun écart de nom de champ).
- [ ] E2E-1 → E2E-10 PASS ; E2E-11/12 recette manuelle documentée.
- [ ] Aucune tâche `IN_PROGRESS`/`BLOCKED` sans bloc NEED-HELP explicite.
- [ ] Migrations : uniquement V178, V179, V180 ajoutées ; aucune existante modifiée.
- [ ] Aucun `push`, aucun tag, `main` intacte.
- [ ] Docs corrigées : plus aucune affirmation non prouvée sur l'onboarding/tenant.
- [ ] `reports/plan-2agents/VERIFICATION.md` et `INTEGRATION.md` présents et argumentés.

- [ ] Worktree Agent B créé sur `fix/onboarding-tenant-clients` (R-9) et `main` intacte.

---

## 12. JOURNAL DE RÉVISION DU PLAN (traçabilité des corrections)

> Ce document est un **fichier d'autorité** : toute modification passe par l'orchestrateur humain. La révision ci-dessous a été produite par l'**Agent B** à sa demande explicite, **sur la base d'une vérification du code réel** (backend, frontend et mobile lus directement). Chaque correction est donc **prouvée**, pas supposée. `git diff` sur ce seul fichier donne le détail ligne à ligne.

| # | Défaut constaté dans la version 1.0 | Preuve (vérifiée le 2026-09-28) | Correction appliquée |
|---|---|---|---|
| 1 | Numéros de migration `V183/V184/V185` fondés sur « dernier existant : V182 » | Le dernier fichier de `db/migration/` est `V177__complete_canonical_saas_plans.sql` (145 fichiers) | Réservés **`V178/V179/V180`** partout ; « dernier existant » = **V177** |
| 2 | Trois commits de base différents dans un même document (`d8400cf`, `ec74906`, « commit courant ») | `HEAD` = `d730771` ; `ec74906` est 5 commits en retard | Base unique : **`d730771`** |
| 3 | Worktrees décrits en chemins Windows, état obsolète | `discipolat_app-agentA` existe ; `discipolat_app-agentB` **n'existe pas** ; branche `fix/onboarding-tenant-clients` absente | Commandes `bash` correctes, état réel documenté, risque **R-9** ajouté |
| 4 | **B1 créait `OnboardingStepper.tsx`, doublon du design system** | `OnboardingStepper` existe déjà dans `components/ui/UXComponents.tsx:212`, testé dans `UXComponents.test.tsx:184-210` | Doublon interdit ; extension par props optionnelles ; règle générale posée en **`§5.0.1`** |
| 5 | **B8 : le deep link custom n'aurait jamais fonctionné** | `invitationTokenFromUri` exige `uri.path == '/accept-invitation'` ; `discipolat://accept-invitation` donne un **path vide** et un **host** → `null` | Manifeste corrigée (`host` + `pathPrefix`), parseur **non modifié**, test de non-régression ajouté |
| 6 | **B8 : `com.discipolat.mobile` ne correspond à aucun bundle réel** | Android `com.discipolat.discipolat_mobile` ; iOS `com.discipolat.discipolatMobile` | Identifiants réels dans `Info.plist` et les fichiers `.well-known` |
| 7 | **B8 : Universal Links iOS impossibles** (aucun fichier d'entitlements) | `ios/Runner/` ne contient aucun `.entitlements` | `Runner.entitlements` exigé **et** `CODE_SIGN_ENTITLEMENTS` à câbler |
| 8 | **B5 : chemin d'API inexistant** | `/platform/admin/tenants/{id}/usage` n'existe pas ; le vrai est `/platform/admin/quota-usage/tenants/{tenantId}` | Chemin corrigé, **R-5 résolu**, `—` affiché au lieu de `0` en cas d'échec |
| 9 | **B6 : `QuotaUsagePanel` n'existe pas** | Le composant réel est `components/admin/QuotaUsageCards.tsx` | Référence corrigée |
| 10 | **B5 : ajouter le statut `ONBOARDING` contredit D2** | D2 interdit explicitement tout nouvel état `TenantStatus` | Exigence reformulée en **résilience** au statut inconnu |
| 11 | **B2 : « redirection douce, jamais bloquante » puis `navigate()` forcé** | `LoginPage` navigue déjà vers `/dashboard` (4 sites) et `/verify-2fa` | Décision tranchée : **aucune redirection automatique**, signalement par bannière seulement |
| 12 | **B2 : `AuthContext.tsx` « ou » `LoginPage.tsx`** — ambiguïté dans un document figé | — | Choix unique (`LoginPage`) + justification |
| 13 | **Critères de test invérifiables** (« ≥ 1188 », « ≥ 325 », « ≥ 356 ») | 139 fichiers de test Java, 48 fichiers FE, 85 Dart : le nombre de *cas* n'est connu qu'après exécution | Remplacés par la **baseline mesurée en P0.3** — R10 appliquée à elle-même |
| 14 | **G-B.3 impossible à satisfaire** (« chaîne codée en dur ») | La convention du dépôt **est** `tText('texte français')` | Critère reformulé en **routage i18n**, avec test de non-régression |
| 15 | **G-B.4 interdisait des endpoints réels** pourtant prescrits par le plan | 6 endpoints vérifiés sur le backend | Liste d'**endpoints autorisés** ajoutée au gate |
| 16 | **B1/B10 : consigne « vérifier `/currencies` ou `CurrencyController` » impossible** | Ni l'un ni l'autre n'existe dans le backend | Question **tranchée** : listes minimales documentées, valeurs par défaut seulement |
| 17 | `npm test -- --run` redondant | `package.json` : `"test": "vitest run"` | Syntaxe corrigée partout + explication |
| 18 | Conflit de propriété `docs/**` et `infra/**` entre les deux plans | `AGENT_ORCHESTRATION.md` §4.1 attribue `docs/**` à B et `infra/**` à A | Tableau `§2` complété + **règle d'arbitrage** explicite |
| 19 | Aucune exigence de qualité d'interface pour l'Agent B | Le plan ne traitait que le *fonctionnel* | **`§5.0`** : réutilisation, 5 états, mouvement/3D sans dépendance, WCAG AA, RTL, responsive, perf |
| 20 | 24 lignes vides en fin de fichier | — | Nettoyées |

*Fin du plan — toute modification de ce document passe exclusivement par l'orchestrateur humain.*

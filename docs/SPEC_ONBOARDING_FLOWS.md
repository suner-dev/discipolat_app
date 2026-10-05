# SPEC_ONBOARDING_FLOWS — Refonte des flows d'entrée (landing, église, codes de rejointure, super admin, annonces)

> Document de référence pour reprise par tout agent. État d'avancement : **spéc validée, implémentation en cours**.
> Périmètre : **Backend (Java/Spring) + Frontend Web (React) + Mobile (Flutter)**.

---

## 0. Contexte et problèmes constatés

1. **Landing page** : pas de CTA d'entrée claire. Les deux gestes fondamentaux (« Créer une église », « Rejoindre une église avec un code ») n'existent pas.
2. **Super Admin plateforme** : côté web, `navForRole()` (frontend/src/workspaces.ts) retombe sur `FULL_NAV` pour tout rôle inconnu → le super admin voit les menus d'église (dashboard membre/chef de famille). Le backend est déjà étanche (`@authz.isPlatformSuperAdmin()`), mais le front ne l'isole pas.
3. **Rejointure** : seul le lien par `tenantSlug` existe (`/register?tenant=<slug>` → `AuthService.registerInChurch`). Aucun code court persistant par église ni par sous-église, pas de self-service « créer mon église » immédiat (le flux actuel est une *demande* approuvée par le Super Admin via `TenantRegistrationService.submit`).
4. **Sous-communautés** : le modèle `OrganizationNode` (materialized path) existe mais rien n'expose de code/lien par sous-église, ni de flow « l'admin parent = roi, délègue » façon WhatsApp Communities.
5. **Annonces publiques** : aucun espace où une église peut publier une annonce (événement) visible sur le landing page.

## 1. Décisions actées (autonome, ne pas reposer la question)

| # | Décision |
|---|----------|
| D1 | **Self-service ON** : « Créer une église » crée immédiatement le tenant + le fondateur comme `TENANT_OWNER` (plus d'attente d'approbation). Le flux existant « demande de création » (registration-requests) reste disponible pour l'approbation manuelle Super Admin. |
| D2 | **Format des codes** : `BETHEL-7K2X` = préfixe slug du tenant (8 car. max, sans caractères spéciaux) + `-(suffixe de 4 caractères)` dans un alphabet **non ambigu** `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (pas de I, L, O, U, 0, 1). Stocké normalisé en MAJUSCULES. Codes régénérables, un code actif principal par église / sous-église. |
| D3 | **Sous-églises = nœuds du même tenant** (`OrganizationNode` de type `CHURCH`/`CAMPUS`/`SUB_CHURCH`), pas des tenants separates. Chaque nœud peut porter son propre code de rejointure (`tenant_join_codes.org_node_id`). |
| D4 | **Annonces publiques : modération obligatoire par défaut** (workflow `DRAFT → PENDING_MODERATION → PUBLISHED/REJECTED`, `EXPIRED` automatique). Un Super Admin peut publier directement (auto-approuvé). |
| D5 | **Statuts tenant ban/blocked** : on réutilise `TenantStatus` existant (`SUSPENDED` = blocé, `CANCELLED` = supprimé/ban) + colonnes d'audit `suspendedReason`/`suspendedAt` portées par les tables nouvelles plutôt que modifier l'enum en base (risque check-constraint V135). Avertissements et litiges = tables dédiées. |
| D6 | Transfert de propriété « façon WhatsApp » : endpoint `POST /api/v1/tenant/ownership/transfer {toUserId}` réservé `TENANT_OWNER`, + `POST /api/v1/tenant/ownership/claim-replacement` (procédure assistée si le-owner disparaît : le 2e `TENANT_ADMIN` le plus ancien peut revendiquer, avec audit + email à tous les owners). |
| D7 | Le membre qui rejoint via code/lien n'a **plus jamais** à ressaisir le code : son compte est rattaché au tenant (`TenantMembership`), la connexion suivante résout le tenant automatiquement (mécanisme existant `ActiveTenantService.resolveTokenTenantId`). |
| D8 | Invitation email automatique après rejointure (confirmation + rappel du code used) et invitation email proactive « invite une personne » par code (réutilise `InvitationService` existant, emails non bloquants). |
| D9 | Livrables code : migrations Flyway **V219, V220, V221** (jamais modifier les existantes). |
| D10 | Nouveaux endpoints publics sous `/api/v1/public/**` : `GET` déjà `permitAll` ; il faut ajouter dans `SecurityConfig` `POST /api/v1/public/join/lookup` et `POST /api/v1/public/announcements`NON — seul `lookup` est un POST public (rate-limit sévère anti-énumération). |

## 2. Spécification fonctionnelle

### A. Landing page — 2 CTA + annonces

- **Hero** : deux boutons primaires :
  - `Créer mon église` → `/register?mode=church` (**CONTRAT FIGÉ — le paramètre
    d'URL est `mode=church`, pas `intent=…`** : c'est ce que lit
    `RegisterPage.tsx`. Toute divergence casserait le CTA. F4 de l'audit
    du 04/10/2026.) Le formulaire porte aussi le **type d'organisation**
    (`kind` : `CHURCH` par défaut, `DENOMINATION`, `ASSOCIATION`,
    `ORGANIZATION`, `MEGA_ASSOCIATION` — SPEC_ORGANISATION_DENOMINATION_V2 D2),
    puis POST `/auth/register` avec `createChurch=true, churchName=…, kind=…`
    → compte actif immédiatement, `TENANT_OWNER`, session réémise.
  - `Rejoindre une église` → `/join` (saisie code ou slug → lookup public → si trouvé : proposition « créer mon compte membre » (`/register?joinCode=…`) ou « me connecter » (`/login?joinCode=…`) → après connexion, rattachement automatique membership). Si l'utilisateur est **connecté** et que le code appartient à sa **même dénomination**, l'IHM annonce un **TRANSFERT** (pas une nouvelle adhésion).
- Section **annonces publiques** (carrousel) : `GET /api/v1/public/announcements` (PUBLISHED, non expirées, tri dateEvenement) — carte : titre, église, ville, date, visuel, lien/code d'accès. CTA « J'y suis invité → /join ».

### B. Console Super Admin isolée

- `workspaces.ts` : créer `PLATFORM_NAV` (Dashboard, Tenants, Demandes d'inscription, Litiges, Avertissements, Annonces publics (modération), Plans SaaS / Branding, Impersonation, Audit, Réglages système, Docs API). `navForRole`/`Sidebar` : si `user.platformSuperAdmin === true` → `PLATFORM_NAV` uniquement, **jamais** `FULL_NAV`.
- `App.tsx` : les routes tenant (`/dashboard`, `/people`, …) sous scope tenant ; si platformSuperAdmin sans membership tenant actif → redirection `/platform/dashboard` (HomeGate existant, à compléter).
- Backend nouvelles surfaces console :
  - `GET/POST /api/v1/platform/tenants/{id}/suspend|resume|ban|unban` (raison obligatoire, audit, email au owner).
  - `POST /api/v1/platform/tenants/{id}/warnings` + `GET` (historique avertissements).
  - `tenant_disputes` : `POST/GET /api/v1/platform/tenants/{id}/disputes`, clôture avec résolution.
- Mobile : l'app n'expose pas de console super admin (hors périmètre), mais le flag `platformSuperAdmin` du login doit empêcher l'affichage du menu église (garde simple + écran « console sur le web »).

### C. Codes de rejointure (église + sous-églises)

- Table `tenant_join_codes` : id, tenant_id, org_node_id (nullable), code UNIQUE, label (nom affiché : « Église principale », « Campus Nord »), join_mode `OPEN|APPROVAL`, is_active, created_by, created_at, rotated_at.
- Génération auto à la création du tenant (self-service ET provisioning plateforme) : code = `slug(8)–XXXX`.
- Admin tenant (`TENANT_OWNER`/`TENANT_ADMIN` via `@authz.isTenantAdmin()`) :
  - `GET /api/v1/tenant/join-codes` (liste, codes actifs + historiques)
  - `POST /api/v1/tenant/join-codes {orgNodeId?, label, joinMode}` (nouveau code, ex. par sous-église)
  - `POST /api/v1/tenant/join-codes/{id}/rotate` (nouvelle valeur, ancien code meurt)
  - `PATCH /api/v1/tenant/join-codes/{id} {label?, joinMode?, isActive?}`
  - `DELETE /api/v1/tenant/join-codes/{id}` (soft : isActive=false)
- Lien vanity : `/j/<slug>` (tenant) et `/j/<slug>/<code>` (sous-église) → même page `/join` préremplie.
- Lookup public : `POST /api/v1/public/join/lookup {code | slug}` → `{found, churchName, orgNodeName?, joinMode, slug, requiresApproval}` — **sans jamais révéler d'email/PII ni le tenant_id**. Rate-limit `tryConsumeJoinLookup` (ex. 10/min/IP).
- Rejointure authentifiée : `POST /api/v1/tenant/join {code}` (compte déjà connecté qui rejoint une autre église → crée `TenantMembership` MEMBRE, `OPEN` direct, `APPROVAL` → insert `tenant_join_requests` PENDING + email admin). Après login, `ActiveTenantService` a déjà le membership → plus aucune saisie.
- Inscription par code : `RegisterRequest` + `joinCode` → branche `registerInChurch`-similaire avec résolution code→tenant (+orgNode scope `CHURCH`/`CAMPUS` si code de sous-église : membership avec `scopeType` du nœud et `scopeId=orgNodeId`), email d'activation, puis `peopleService.registerPerson(..., "SELF_SIGNUP", …)` à l'activation (pattern existant).

### D. Propriété transférable (admin « roi »)

- À la création self-service : fondateur = `TENANT_OWNER` (roleLegacy ADMIN) — via `TenantOwnerProvisioningService.ensureOwnerMembership`.
- `GET /api/v1/tenant/ownership` (owner actuel + admins listés pour désignation d'un remplaçant).
- `POST /api/v1/tenant/ownership/transfer {toUserId}` — `TENANT_OWNER` seulement ; le nouveau owner est obligatoirement un membre `TENANT_ADMIN`/`MEMBRE` existant du tenant ; audit + emails aux deux.
- `POST /api/v1/tenant/ownership/request-replacement {reason}` — par un `TENANT_ADMIN` quand l'owner est absent/inactif ; crée une demande traitée par la plateforme (Super Admin) ; l'admin « le plus ancien » est suggéré dans la réponse.
- Délégation façon WhatsApp : `POST /api/v1/tenant/members/{userId}/promote-admin` / `demote-admin` (TENANT_OWNER uniquement) — réutilise les rôles `Role` globaux/tenant existants.

### E. Annonces publiques + modération

- Table `public_announcements` : id, tenant_id, org_node_id?, titre, description, visuel_url?, ville?, pays?, date_evenement?, lien_url?, join_code_ref (string, code ou slug affiché), statut `DRAFT|PENDING_MODERATION|PUBLISHED|REJECTED|EXPIRED`, moderation_note, moderated_by/at, published_at, expires_at, created_by/at.
- Admin église (`isTenantAdmin`) : CRUD `/api/v1/tenant/announcements` ; `SUBMIT` → PENDING_MODERATION.
- Super Admin : `GET /api/v1/platform/announcements?status=`, `POST /{id}/approve|reject {note}` ; publication directe possible (`publish-direct`).
- Public : `GET /api/v1/public/announcements` (PUBLISHED && (expires_at null || > now), max 20, tri dateEvenement desc), pas de PII.
- Job de purge : `@Scheduled` quotidien → PUBLISHED expirées → EXPIRED.

## 3. Modèle de données (Flyway)

- **V219__tenant_join_codes.sql** : `tenant_join_codes`, `tenant_join_requests` (id, tenant_id, org_node_id?, user_id?, email, code, status PENDING/APPROVED/REJECTED, created_at, handled_at/handled_by), index UNIQUE sur `code` (actif seulement — contrainte partielle `WHERE is_active`), indexes tenant/org_node. Check constraints sur enums.
- **V220__public_announcements.sql** : `public_announcements` + indexes statut/date, check statut.
- **V221__tenant_governance.sql** : `tenant_warnings` (tenant_id, message, created_by, created_at, ack_at), `tenant_disputes` (tenant_id, sujet, description, statut OPEN/IN_REVIEW/CLOSED, resolution, ouvert/clos par, timestamps).
- Nothing to change on `tenants` (statuts réutilisés) ; `users` inchangé.

## 4. API — récapitulatif

Public (`/api/v1/public/**`) :
- `GET  /join/resolve?code=` et `POST /join/lookup {code|slug}` (lookup = POST ajouté en permitAll, rate-limit dur)
- `GET  /announcements`
- `GET  /churches/{slug}` (existant enrichi : renvoie le code actif + slug si publié)

Authentifié tenant (`@authz.isTenantAdmin()` sauf mention) :
- `GET/POST/PATCH/DELETE /api/v1/tenant/join-codes…`
- `POST /api/v1/tenant/join {code}` (tout membre connecté)
- `GET/POST /api/v1/tenant/ownership…` (transfer : TENANT_OWNER)
- `GET/POST /api/v1/tenant/announcements…` + `POST /{id}/submit`

Plateforme (`@authz.isPlatformSuperAdmin()`) :
- `POST /api/v1/platform/tenants/{id}/suspend|resume|ban|unban`
- `GET/POST /api/v1/platform/tenants/{id}/warnings`
- `GET/POST /api/v1/platform/tenants/{id}/disputes…`
- `GET /api/v1/platform/announcements…` + `approve|reject|publish-direct`

Auth (public) :
- `POST /auth/register` enrichi : `joinCode`, `createChurch`, `churchName` (validations croisées : createChurch ⊄ avec joinCode/tenantSlug ; slug/code invalides → 400 `JOIN_CODE_NOT_FOUND`).

## 5. Tâches détaillées (pour reprise agent)

### Backend
1. **BE-1 V219 + domain** : migration ; entités `TenantJoinCode`/`TenantJoinRequest` + repos ; `JoinCodeService` (generate unique w/ retry, resolve, rotate, CRUD, lookup DTO sans PII). Fichiers : `backend/src/main/java/com/discipolat/modules/tenants/domain/…`, `resources/db/migration/V219__tenant_join_codes.sql`.
2. **BE-2 câblage provisioning** : `TenantOwnerProvisioningService` / `SelfServiceChurchService` (nouveau) : reproduit `PlatformProvisioningService.provision` (crossTenant.callForTenantSwitch + `TenantContext.setTenantId` + restore finally) → `tenantService.create` + `organizationNodeService.createRootChurch` + `ensureOwnerMembership` + `JoinCodeService.generateForTenant` + email bienvenue. `RegisterRequest` + `AuthController.register` + `AuthService.resolveSignupTenant` : branches `joinCode` et `createChurch`.
3. **BE-3 API join codes + join + ownership** : `TenantJoinCodeController`, `PublicJoinController` (`/api/v1/public/join/lookup` POST permitAll), `TenantJoinService.join(code)` (membership + scope nœud + APPROVAL→request), `TenantOwnershipController` (transfer/claim/promote/demote), rate-limit `PerIpRateLimiter.tryConsumeJoinLookup`.
4. **BE-4 V220 annonces** : `PublicAnnouncement` entity/repo/`AnnouncementService` (workflow + job expiration), `TenantAnnouncementController` (isTenantAdmin), `PlatformAnnouncementModerationController` (isPlatformSuperAdmin), `PublicAnnouncementsController` (GET).
5. **BE-5 V221 gouvernance** : `TenantWarning`/`TenantDispute` + `PlatformTenantGovernanceController` (suspend/resume/ban/unban + warnings + disputes) — suspension via `TenantService`/repo, `TenantStatusGuard` bloque déjà les tenants non ACTIVE ; audit via `AuditService` existant.
6. **BE-6 SecurityConfig** : ajouter `POST /api/v1/public/join/lookup` en permitAll ; vérifier `TenantFilter` ignore `/api/v1/public` (déjà le cas).

### Frontend
1. **FE-1 isolement super admin** : `workspaces.ts` → `PLATFORM_NAV`, `isPlatformUser(user)` ; `Sidebar.tsx` : si platformSuperAdmin → PLATFORM_NAV ; `RouteGuards.tsx`/`HomeGate` : garde cohérente (le flag `platformSuperAdmin` vient de `AuthResponseFactory` via `AuthContext`).
2. **FE-2 landing** : `Hero.tsx` 2 CTA ; `LandingPage.tsx` + `PublicAnnouncementsSection` (carrousel fetch `GET /public/announcements`) ; route `/join` (+ `/join?code=`, `/j/:slug`, `/j/:slug/:code` dans `App.tsx` lazy) → `JoinChurchPage.tsx` (lookup → CTA register?joinCode / login?joinCode) ; `RegisterPage.tsx` : mode créer église (`intent=create-church` → champ nom d'église, `createChurch:true`) et mode joinCode ; `LoginPage` : accepter `?joinCode=` post-login → `POST /tenant/join`.
3. **FE-3 admin join-codes** : page « Codes d'accès » (liste/créer/rotation/lien à partager, y compris par sous-église via org node picker existant) + section « Propriété » (transfert, admins).
4. **FE-4 annonces** : page admin annonces (CRUD + statut) + page plateforme modération (file PENDING, approve/reject).
5. **FE-5 i18n** : clés FR/EN/ES/SW/AR via `tText()` fallback ; pas de nouvelles dépendances.

### Mobile (Flutter)
1. **MO-1** `api_service.dart` : helpers `publicLookup(code)`, `joinByCode(code)`, `createChurchRegister(…)` (extends payload register).
2. **MO-2** `register_screen.dart` : segmented « Créer mon église » / « Rejoindre une église » — champs churchName / joinCode ; après création → message owner + nav dashboard ; code non trouvé → snackbar.
3. **MO-3** `login_screen.dart` : champ « Code (optionnel) » → après login `POST /tenant/join` ; route `/join` (deep link `discipolat://join/<code>` via go_router, lien `https://…/j/…` — pattern `assetlinks.json` existant).
4. **MO-4** landing mobile : écrans d'accueil pré-auth (le welcome existant) — remplacer le CTA « Demander la création d'église » par le nouveau register screen.
5. **MO-5** tests : `widget_test` sur le mode rejoindre + payload register (mock Dio).

### Tests
- Backend : `JoinCodeServiceTest` (génération unique, rotation, lookup), `TenantJoinServiceTest` (OPEN vs APPROVAL, scope nœud), `SelfServiceChurchServiceTest` (owner membership + code auto + tenant actif), `AuthControllerRegisterJoinCodeTest` (validation croisée), `AnnouncementWorkflowTest` (DRAFT→PUBLISHED→EXPIRED, modération).
- Frontend : `__tests__` — navForRole platform (pas de FULL_NAV pour super admin), JoinChurchPage lookup flow, annonces section.
- `mvn -q compile` + tests ciblés, `npx tsc --noEmit`, `flutter analyze` (si SDK dispo).

## 6. Garde-fous et non-régression

- Ne JAMAIS exposer `tenant_id` / emails dans les réponses publiques.
- Toute bascule de tenant en requête passe par `CrossTenantScopeAccess.callForTenantSwitch` (H4) avec restauration `TenantContext` en `finally`.
- Emails d'échec = non bloquants (try/catch log), pattern `InvitationService`.
- `TenantStatusGuard`/`TenantStatus` inchangés ; ban = `CANCELLED`, blocage = `SUSPENDED` + raison en table de gouvernance.
- Migrations montantes uniquement (V219+), pas d'édition des V≤218.
- Le flux legacy « demande de création approuvée par Super Admin » reste intact.

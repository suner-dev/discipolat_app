# Audit strict & Super Admin — Discipolat (23 septembre 2026)

> Périmètre : mobile (Flutter), front-end (React/Vite), backend (Spring Boot),
> landing page, et construction du système **Super Admin** avec le flux guidé
> **Tenant → Église → Département → Famille**. Rien n'a été supprimé.

## 1. État initial (avant travaux)

| Domaine | Constat | Gravité |
|---------|---------|---------|
| Mobile | `flutter analyze` : **87 erreurs** (modèles freezed obsolètes, overrides de `ApiService` non conformes, `isLive` hors scope, appel de provider family sans record) | Bloquant |
| Mobile | `app.dart` : routes GoRouter **`/events` et `/finances` déclarées 2 fois** → le router plante au démarrage (`duplication fullpaths`), 6 tests en échec | Bloquant |
| Mobile | Build runner : 736 fichiers générés absents/périmés | Bloquant |
| Front-end | — (tsc 0 erreur, 321 tests verts) | OK |
| Backend | — (compilait) | OK |
| Landing | Menu mobile : chevron « accordéon » cliquable **sans sous-menu** (affordance morte) | Mineur |

## 2. Corrections mobile (`fbfb628d`)

1. **Régénération complète** via `dart run build_runner build --delete-conflicting-outputs`
   (736 outputs) : les modèles `freezed`/`json_serializable` (tasks, events,
   discipleship, health, finances…) sont alignés sur leurs sources.
2. **67 fakes de tests** alignés sur la signature réelle
   `ApiService.get/delete(..., {params, queryParameters})`.
3. **6 erreurs résiduelles** corrigées à la source :
   - `tasks_screen.dart` : `_tasksProvider` est une `FutureProvider.family` à
     argument record → appel corrigé en `_tasksProvider((status, priority, type))`
     (2 occurrences).
   - `event_detail_screen.dart` : `isLive` calculé dans `_buildPlaceholder`
     (il était local à `build`) et `const Text(condition ? … : …)` invalide
     rendu dynamique.
4. **Routes GoRouter** : suppression des **déclarations dupliquées** (pas des
   fonctionnalités) au profit des écrans *feature-complete*
   (`EventsScreen`, `FinancesScreen`), cohérents avec les routes filles
   `/events/:id`, `/events/create`. Le chemin `/events` reste défini une fois.

**Résultat : `flutter analyze` = 0 erreur** (87 → 0), les 6 tests qui échouaient
passent.

## 3. Super Admin — système de provisionnement guidé

### 3.1 Backend (`aa293545`)

Nouveau `PlatformProvisioningController`
(`/api/v1/platform/admin/provisioning`, `@PreAuthorize("@authz.isPlatformSuperAdmin()")`) :

| Étape | Endpoint | Implémentation |
|-------|----------|----------------|
| 1. Tenant | `POST /platform/admin/tenants` | `SuperAdminController` existant (réutilisé, non dupliqué) |
| 2. Église | `POST …/provisioning/church` | `OrganizationNodeService.createRootChurch` (ROOT_CHURCH) |
| 3. Département | `POST …/provisioning/department` | `DepartmentService.create(CreateDepartmentRequest)` |
| 4. Famille | `POST …/provisioning/family` | `FamilyService.create(CreateFamilyRequest)` |

Principes :
- **Zéro duplication de logique métier** : les services existants (validation
  du responsable/chef de famille, unicité du nom de famille, code de nœud,
  propagation, audit) sont réutilisés tels quels.
- **Ciblage tenant explicite** : `TenantContext.setTenantId(tenantId)` pendant
  l'opération (restauration du contexte précédent en `finally`) → l'auto-listener
  Hibernate rattache `tenant_id` et les lectures passent par
  `TenantAwareSimpleJpaRepository` dans le bon périmètre.
- **Traçabilité** : chaque étape journalise une action d'audit
  (`PROVISIONING_*_CREATED`) avec l'identité **réelle** du super admin.
- **Contrat d'erreur** explicite (`400` + champ fautif) quand tenantId/nom
  manque ; les règles métier (`BusinessRuleException`) remontent via le
  `GlobalExceptionHandler` existant.

### 3.2 Front-end (`59f949d3`)

`PlatformOnboardingFlowPage` (`/platform/onboarding`, `scope="platform"`) :

- Stepper **cliquable** : on ne peut entrer dans une étape que si la précédente
  est créée ; retour possible sur les étapes complétées.
- Formulaires par étape (tenant : nom/slug auto/plan/pays/devise/fuseau/langue ;
  église ; département : responsable nouveau **ou** existant ; famille : chef
  nouveau **ou** existant) avec **validation avant appel** et `toast` d'erreur.
- Récapitulatif : IDs/noms/chemins créés + accès direct aux tenants et au
  dashboard plateforme.
- Entrée visible : bouton « Provisionner une organisation (guidé) » dans le
  dashboard plateforme.
- Responsive : `sm:` sur les grilles de formulaire, boutons flex-wrap, cartes
  de récap `grid-cols-1 sm:grid-cols-2`.

### 3.3 Mobile (`ccc8b0b4`)

`SuperAdminProvisioningScreen` (`/platform/onboarding`, rôles ADMIN/PASTEUR/
PLATFORM_*), même flux 4 étapes :

- Stepper horizontal scrollable, `ChoiceChip` pour les modes
  « nouveau/existant », formulaires `TextField`/`DropdownButtonFormField`,
  validation locale, `SnackBar` succès/erreur, récap + reset + retour dashboard.
- `ApiService` injectable (même pattern que les écrans platform existants) →
  2 tests widget verts (`test/super_admin_provisioning_screen_test.dart`) :
  flux complet (4 POST dans l'ordre, `tenantId` propagé) et blocage de l'étape 1
  sans nom.
- Entrée dans le drawer admin (libellé + route).

## 4. Landing page — audit responsive/cliquable/configurable (`1813b722`)

- **Contrôlé au navigateur réel** : `scripts/e2e-landing-responsive.js`
  (puppeteer-core + Chrome système) sur 4 viewports.
- **Configurable** : identité publique (nom, logo, email, téléphone, site)
  depuis `SettingsContext` dans navbar, footer et modale démo ; couleurs via les
  variables de marque ; ancres/CTA alimentées par l'i18n (6 langues).
- **Cliquable & fonctionnel** : CTA Connexion/Commencer → `/login`, modale démo
  (ouverture + validation + envoi), ancre Tarifs qui positionne la section à
  **64 px** du haut (= offset exact du header), fermeture automatique du menu,
  footer (ancres + `mailto:`).
- **Responsive** : **aucun débordement horizontal** à 375/390/768/1440 px ;
  menu hamburger (< 1024 px) et nav desktop ; mockups décoratifs du Hero
  masqués sur mobile (`hidden lg:block`).
- Correction : chevron « accordéon » mort du menu mobile remplacé par une flèche
  décorative (le contrôle suggérait un sous-menu inexistant).

**Résultat E2E : 34 étapes — TOUT PASSE.**

## 5. Recette globale

| Contrôle | Résultat |
|----------|----------|
| `flutter analyze` | **0 erreur** (87 → 0) ; 529 remarques info/warning non bloquantes |
| `flutter test` (complet) | **336 tests verts** (328 déjà OK + 6 récupérés par la correction des routes + 2 tests Super Admin) |
| `npx tsc -b` | **0 erreur** |
| `npx vitest run` | **321 tests / 42 fichiers** verts |
| `eslint` | **0 erreur** (339 warnings `any`/hooks préexistants) |
| `npm run build` (prod) | **BUILD_EXIT=0** (2769 modules) |
| `mvn compile` | **OK** |
| `mvn test` | **1188 tests, 0 failure, 0 error, 0 skipped** (129 classes) |
| E2E landing responsive | **34 étapes vertes** (375/390/768/1440 px) |

## 6. Commits livrés (poussés sur GitHub `origin/main`)

1. `972af31f` — état complet avant audit (tout le non commité).
2. `fbfb628d` — mobile : 0 erreur analyze + routes doublons corrigées.
3. `aa293545` — backend : contrôleur de provisionnement super admin.
4. `59f949d3` — front : wizard 4 étapes + route + CTA dashboard.
5. `ccc8b0b4` — mobile : écran super admin + route + drawer + tests.
6. `1813b722` — landing : menu mobile corrigé + script E2E responsive.

## 7. Audit de sécurité — failles trouvées et corrigées (2026-09-24)

Audit ciblé « isolation inter-églises » demandé par le client. **Deux failles
réelles ont été trouvées (préexistantes) et corrigées** :

| Faille | Impact | Correction |
|--------|--------|------------|
| `TenantController` (`/api/v1/tenants`) protégé par `hasAnyRole('ADMIN','PASTEUR')` | **Tout admin ou pasteur d'église pouvait lister, lire, modifier, supprimer et réactiver TOUS les tenants** (et l'inscription auto-provisionnée crée un compte `ADMIN` — `AuthService:543` : la faille était exploitable par tout nouvel inscrit) | Les 7 endpoints sont passés à `@PreAuthorize("@authz.isPlatformSuperAdmin()")` |
| `SuperAdminController.archiveTenant` **sans aucun `@PreAuthorize`** | **Tout utilisateur authentifié pouvait archiver n'importe quelle église** (statut → CANCELLED) | Ajout de `@PreAuthorize("@authz.isPlatformSuperAdmin()")` |

Conséquences traitées en cascade :
- Web : la route `/admin/tenants` est passée de `roles={['ADMIN','PASTEUR']}` à
  `scope="platform"` (le tenant admin ne peut plus ouvrir l'onglet « Églises »).
- Mobile : `/admin/tenants` passe à `['PLATFORM_SUPER_ADMIN','PLATFORM_BILLING_ADMIN']`,
  et le drawer expose une navigation **plateforme** séparée
  (`_platformNav` : provisionnement + tenants) visible **uniquement** pour les
  rôles `PLATFORM_*` ; l'admin d'église n'y a plus accès.
- **Aucun compte `PLATFORM_SUPER_ADMIN` n'existait** (le rôle est créé par le
  seed, mais pas le user) : le flow super admin était donc inutilisable en l'état.
  `DataInitializer.seedPlatformSuperAdmin()` crée désormais
  `superadmin@discipolat.com` (mot de passe de dev `password123`, à changer en
  prod) avec une membership du rôle global `PLATFORM_SUPER_ADMIN`, de façon
  idempotente.

Boutons morts de l'interface super admin (superdashboard) : « + Nouveau Tenant »,
« Éditer » (tenant), « + Nouveau Plan », « Éditer »/« Désactiver » (plan) étaient
des boutons **sans handler**. Tous sont câblés :
- « + Nouveau Tenant » → wizard de provisionnement ;
- « Éditer » (tenant) → modale nom/plan/langue/fuseau → `PUT /platform/admin/tenants/{id}` (endpoint ajouté) ;
- « Suspendre / Réactiver » → endpoints existants ;
- Plans → modale (clé, nom, description, prix mensuel/annuel) → `POST /platform/admin/plans`
  (l'upsert gère désormais `isActive`, ajoutée) ; « Désactiver » protégé pour
  ne pas pouvoir désactiver le plan `free`.

**Recette après correctifs** : `mvn test` = 1188 tests verts (EXIT=0),
`tsc` = 0, `vitest` = 321/321, `flutter analyze` = 0 erreur, tests mobile verts.

## 8. État réel de l'IA / chat / vocal / paiements (sans promesse « parfait »)

| Domaine | Présence vérifiée dans le code | Ce qui n'est PAS vérifié |
|---------|--------------------------------|--------------------------|
| IA | Backend `modules/ai` (AiAssistantController/Module/Credits + AiAssistantService, aiPredictions, aiVisitNotes), web `AiAssistantPage`/`AiPredictionsPage`/`AiVisitNotesPage`, mobile `ai_assistant`/`ai_predictions`/`ai_visit_notes` | Le service IA pointe sur **Ollama local** (`app.ai.ollama-url:http://localhost:11434`, modèle `llama3`) et le STT/TTS sur l'API OpenAI (`APP_SPEECH_API_URL`, `APP_TTS_API_URL`) : **sans serveur Ollama et sans clés API en prod, l'IA ne répond pas**. Aucune exécution réelle n'a été faite. |
| Chat / streaming | Backend `modules/messages` + `modules/streaming` (`LiveStreamController /api/v1/streams`, `StreamChatController /api/stream-chat`), web `MessagesPage`/`ConversationsPage`/`StreamingChat`, mobile `messages`/`group_messages`/`streaming` | Aucun test de bout en bout (WebSocket, temps réel) n'a été exécuté. |
| Assistant vocal | Backend `modules/prophetic/api/VoiceAssistantController` → `/api/v1/voice/{process,commands,tts,tts-status}` + providers `WhisperSpeechToTextProvider`, `OpenAiTextToSpeechProvider` ; **le mobile et le web appellent bien ces endpoints** | Dépend des clés STT/TTS ; jamais exécuté avec une vraie clé. |
| Paiements | Backend `modules/payments` : providers **MTN MoMo, Orange Money, M-Pesa**, `PaymentGatewayService`, webhooks + scheduler + signature, dons récurrents, reçus fiscaux, `PaymentController` + `PaymentWebhookController` ; web `AdminPaymentDashboardPage`/`PricingPage`/`GivingPage` ; mobile `giving` | Les clés (`MTN_CLIENT_ID/SECRET`, `ORANGE_API_KEY`…) sont **vides** dans `.env.example` et `MTN_BASE_URL` pointe par défaut sur le **sandbox MTN** : le « paiement en prod » exige le remplissage des secrets et le basculement hors sandbox. **Non vérifié en prod.** |

## 9. Points restant à traiter (documentés, non corrigés volontairement)

- Les 339 warnings ESLint (`any`, hooks) sont préexistants et sans impact
  fonctionnel ; les traiter relève d'un chantier de refactor dédié.
- Les 529 remarques `flutter analyze` sont des `info` (API dépréciées Flutter :
  `withOpacity`, `value` de `DropdownButton`) — non bloquantes.
- La tarification de la landing reste en dur (« Sur devis ») : elle est statique
  côté marketing ; la brancher sur les plans SaaS demande un endpoint **public**
  de lecture des plans (l'existant est réservé au super admin).


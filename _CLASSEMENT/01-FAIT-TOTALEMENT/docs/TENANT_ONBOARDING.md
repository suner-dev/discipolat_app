# Onboarding d'un tenant

> **Statut : vérifié sur le code au 2026-09-28.** Chaque affirmation de ce document
> porte sa preuve. Les routes inexistantes qui figuraient dans la version
> précédente ont été supprimées (voir « Écarts corrigés » en fin de document).

---

## 1. Le flux réel, de bout en bout

Il n'existe **pas** de pages `onboarding/1-profile`, `onboarding/2-identity`, etc.
Le parcours est un **wizard de 7 étapes** exposé par un contrôleur unique, et le
provisionnement d'un tenant est une **opération atomique** du Super Admin.

```
Super Admin                    Plateforme                    Église
     │                             │                            │
     │ POST /platform/admin/       │                            │
     │      provisioning           │                            │
     ├────────────────────────────►│ crée tenant + église racine │
     │                             │ + département + famille    │
     │                             │ + compte OWNER + token     │
     │◄────────────────────────────┤ d'activation               │
     │                             │                            │
     │                    email d'activation ──────────────────►│ l'owner active son compte
     │                                                        │ POST /api/v1/auth/activate
     │                                                        │
     │                             │        GET  /onboarding-wizard      │
     │                                                        ├──────► 7 étapes
     │                             │        POST /{id}/start              │
     │                             │        POST /{id}/complete          │
     │                             │        POST /{id}/skip              │
     │                             │        GET  /onboarding-wizard/status
     │                             │        GET  /onboarding-wizard/progress
     │                             │◄────────────────────────── completed=true
```

### 1.1 Provisionnement atomique (Super Admin)

| Élément | Valeur réelle | Preuve |
|---|---|---|
| Endpoint | `POST /api/v1/platform/admin/provisioning` | `PlatformProvisioningController:67` |
| Autorisation | `@PreAuthorize("@authz.isPlatformSuperAdmin()")` | idem |
| Réponse | `201` avec `tenant`, `church`, `department`, `family`, `owner` | `PlatformProvisioningController:100-119` |
| Owner | **obligatoire** — refus `OWNER_REQUIRED` si absent | `PlatformProvisioningServiceTest#refusesToProvisionATenantWithoutOwnerAndWritesNothing` |
| Base | SQLAlchemy, pas de construction de tenant en cascade | `PlatformProvisioningServiceTest#provisionsTheWholeOrganizationInsideOneServiceCall` |

> ⚠️ **Ce endpoint est actuellement indisponible sur une base migrée.** Le
> provisioning échoue en HTTP 500 à cause du constat H8 : la colonne
> `organization_nodes.path` est de type `ltree` dans les migrations alors que
> l'entité la mappe en `String`. Voir `reports/plan-2agents/agentA.md` § H2–H8.
> Ne pas considérer ce parcours comme opérationnel tant que H8 n'est pas traité.

### 1.2 Activation du compte owner

| Élément | Valeur réelle | Preuve |
|---|---|---|
| Activation | `POST /api/v1/auth/activate` | `AuthController:172` |
| Suivi public | `POST /api/v1/auth/registration-status` | `AuthController:58` |
| Contrat de suivi | ne divulgue ni mot de passe, ni nom d'organisation ; `NONE` pour une adresse inconnue | `TenantRegistrationServiceTest#publicSubmissionDoesNotCreateTenantOrUser` |
| Cache | `no-store` sur le suivi de demande | `TenantRegistrationEmailTest` |

> L'email d'activation n'est envoyé que si un SMTP est configuré ; sinon le
> contrat expose `activationEmailSent: false` et l'activation reste possible via
> le token. Ce n'est **pas** un blocage (décision D10 du plan).

### 1.3 Le wizard : 7 étapes, une seule route

Base : `/api/v1/onboarding-wizard` (`OnboardingWizardController:28`).

| Méthode | Route | Effet | Preuve |
|---|---|---|---|
| `GET` | `/onboarding-wizard` | les 7 étapes du tenant | `OnboardingWizardServiceTest#getSteps_returnsTheSevenCanonicalStepsWithExactContractFields` |
| `GET` | `/onboarding-wizard/progress` | avancement (`percentage`) | `OnboardingWizardServiceTest#getProgress_percentageIsRoundedAndCountsSkippedSteps` |
| `GET` | `/onboarding-wizard/status` | `completed`, `completedAt` | `OnboardingWizardServiceTest#getStatus_reportsCompletionWithActorWhenColumnsAreSet` |
| `POST` | `/onboarding-wizard/initialize` | initialise les 7 étapes, une seule fois | `OnboardingWizardInitializeConcurrencyTest#concurrentInitializationNeverDuplicatesSteps` |
| `POST` | `/onboarding-wizard/{id}/start` | passe l'étape à `IN_PROGRESS` | `OnboardingWizardServiceTest#startStep_movesPendingToInProgress` |
| `POST` | `/onboarding-wizard/{id}/complete` | exécute l'action métier de l'étape | `OnboardingWizardServiceTest#completeStep_delegatesToTheRealBusinessActionAndStoresItsOutcome` |
| `POST` | `/onboarding-wizard/{id}/skip` | saut, motif obligatoire selon l'étape | `OnboardingWizardServiceTest#skipStep_requiresAReasonWhenTheStepDemandsOne` |
| `GET` | `/onboarding-wizard/templates/{role}` | modèles de contenu par rôle | `OnboardingWizardServiceTest#roleTemplate_stillReturnsTheChecklistForEachRole` |

**Les 7 étapes et leur ordre** (`OnboardingStepDefinition:32-58`) :

| # | `stepType` | Skippable | Motif requis | Action métier réelle |
|---|---|---|---|---|
| 0 | `CHURCH_IDENTITY` | non | — | renomme/crée l'église racine + réglages tenant |
| 1 | `MEMBER_IMPORT` | oui | **oui** | déclaratif vérifié (`importedCount >= 1`) |
| 2 | `STRUCTURE` | non | — | crée départements et familles |
| 3 | `ROLES` | oui | **oui** | crée de vraies invitations |
| 4 | `BRANDING` | oui | non | couleur, logo, thème sombre |
| 5 | `MODULES` | oui | non | active des modules du catalogue (code inconnu refusé) |
| 6 | `FIRST_EVENT` | non | — | crée un événement réel |

### 1.4 Fin du parcours

`GET /onboarding-wizard/status` renvoie `completed: true` et `completedAt` dès que
les 7 étapes sont `COMPLETED` **ou** `SKIPPED`. Colonnes : `onboarding_completed_at`,
`onboarding_completed_by` (migration `V183`).

---

## 2. Contrat d'une étape (§3.1)

```jsonc
{
  "id": "uuid",
  "stepType": "CHURCH_IDENTITY",   // 7 valeurs possibles
  "stepOrder": 0,                  // 0..6
  "title": "Identité de l'église",
  "description": "Nom, logo, devise et informations de contact.",
  "status": "PENDING",             // PENDING | IN_PROGRESS | COMPLETED | SKIPPED
  "isCompleted": false,            // booléen, toujours présent
  "isSkippable": false,
  "skipRequiresReason": false,
  "startedAt": null,
  "completedAt": null
}
```

Points qui ont coûté cher et sont donc verrouillés par test :

- `isCompleted` est **toujours** un booléen (`OnboardingWizardServiceTest#getSteps_returnsTheSevenCanonicalStepsWithExactContractFields`).
- Le corps de `POST /complete` est **facultatif** (décision D7) : l'absence de
  corps donne une erreur **métier** nommée, pas un 400 technique de Jackson
  (`OnboardingWizardServiceTest#completeStep_withNoBodyAtAllSucceedsForAStepThatNeedsNoData`).
- Un `data` invalide donne `400 STEP_DATA_INVALID` **avec le champ fautif**
  (`OnboardingWizardControllerTest#invalidData_gives400WithFieldDetail`).

### 2.1 Erreurs nommées à respecter côté client

| `title` | HTTP | Déclencheur |
|---|---|---|
| `STEP_ORDER_VIOLATION` | 409 | étape complétée hors ordre |
| `STEP_ALREADY_COMPLETED` | 409 | rejeu d'une étape terminée |
| `STEP_DATA_INVALID` | 400 | `data` invalide, champ fautif dans `properties` |
| `STEP_NOT_FOUND` | 404 | étape d'un autre tenant (ne rien révéler) |
| `TENANT_SUSPENDED` | 403 | tenant suspendu (B1) |

---

## 3. Les vraies API à utiliser (et non celles qui étaient documentées)

| Besoin | Endpoint réel | Preuve |
|---|---|---|
| Modifier un tenant | `PUT /api/v1/tenants/{id}` | `TenantController:61` |
| Branding | `GET`/`PUT /api/v1/admin/branding` | `BrandingController:35,43` |
| Hiérarchie (lecture) | `GET /api/v1/org/tree`, `/api/v1/org/tree/flat` | `OrganizationHierarchyController:40,47` |
| Hiérarchie (écriture) | `POST/PUT /api/v1/admin/org/nodes` | `OrganizationManagementController:105,174` |
| Départements | `/api/v1/departments` | `DepartmentController` |
| Familles | `/api/v1/families` | `FamilyController` |
| Événements | `/api/v1/events` | `EventController` |
| Utilisateurs | `/api/v1/users` | `UserController` |
| Invitations | `/api/v1/admin/invitations` | `InvitationController:30` |
| Sélecteur d'église | `GET /api/v1/tenant-switcher/my-tenants`, `POST /api/v1/tenant-switcher/switch` | `TenantSwitcherController:159,187` |

> Les routes `/api/tenants/{id}` et `/api/tenants/{id}/branding` **sans le
> préfixe `/v1`**, ainsi que `/api/org/campus` et `/api/org/units`, n'existent
> pas. Elles ont été retirées de cette page.

---

## 4. Section mobile

| Élément | Chemin réel |
|---|---|
| Écrans d'onboarding | `mobile/lib/presentation/screens/onboarding/` |
| Acceptation d'invitation | `mobile/lib/presentation/screens/invitations/accept_invitation_screen.dart` |
| Wizard mobile | `mobile/lib/presentation/screens/tenant/tenant_onboarding_screen.dart` |
| Deep links d'invitation | `https://app.discipolat.com/accept-invitation?token=<32hex>` et `discipolat://app.discipolat.com/accept-invitation?token=<32hex>` |
| Fichiers d'association | `.well-known/assetlinks.json`, `.well-known/apple-app-site-association` |

> Deep links et écrans mobiles : fournis par l'Agent B
> (`reports/plan-2agents/agentB.md`, section « Deltas doc »).

**Limite connue, importante** : la publication des fichiers `.well-known` en
production et l'Associated Domains côté compte Apple Developer **ne relèvent pas
du code**. Sans cela, `autoVerify` échoue silencieusement et Android ouvre le
navigateur. La recette E2E-11 reste donc **manuelle**.

---

## 5. Écarts connus — ce qui n'est PAS implémenté

Cette section est aussi importante que la précédente : elle empêche de croire
à des fonctionnalités qui n'existent pas.

| Écart | Conséquence | Preuve du constat |
|---|---|---|
| **L'import de membres du wizard est déclaratif** | `MEMBER_IMPORT` déclare un nombre, il n'importe rien. « 0 membre importé » est refusé | `OnboardingStepActionsTest#memberImportRejectsZero` |
| **Relance manuelle d'invitation** | pas d'action manuelle ; il existe un `POST /{id}/resend` côté API mais aucun parcours web/mobile | `InvitationController:278` |
| **Provisioning atomique cassé en base migrée** | HTTP 500 (constat H8, `path` en `ltree`) | recette E2E, `a14-e2e-run.log` |
| **Module Événements non aligné sur le schéma** | l'entité `Event` pointe une table `events` qui n'a jamais été créée par les migrations (constat H2) | recette E2E : `relation "events" does not exist` |
| **Le sélecteur d'église était inopérant** | corrigé dans `fix/schema-drift-h1-h5`, non encore mergé dans `main` | `CrossTenantReadScopeTest` |
| **Aucun test d'intégration sur PostgreSQL en CI** | la CI utilise H2 avec `ddl-auto: create-drop` : une dérive migration/entité est **mathématiquement invisible** en CI | recette E2E, constats H1–H8 |

---

## 6. Écarts corrigés dans cette version

| Ce qui était écrit | Réalité | Correction |
|---|---|---|
| « PRODUCTION READY » (mention historique) | non prouvé, et contredit par 5 défauts bloquants (H1–H8) | statut vérifié, défauts énumérés |
| Routes `/onboarding/1-profile` … `/onboarding/6-*` | inexistantes | routes réelles du wizard §1.3 |
| `PUT /api/tenants/{id}` | il manque `/v1` : c'est `PUT /api/v1/tenants/{id}` | corrigé §3 |
| `PUT /api/tenants/{id}/branding` | inexistant | `PUT /api/v1/admin/branding` |
| `/api/org/campus`, `/api/org/units` | inexistants | `/api/v1/org/tree`, `/api/v1/admin/org/nodes` |
| 6 écrans d'onboarding | 7 étapes, un seul contrôleur | §1.3 |
| Provisionnement décrit comme une suite d'écrans | une opération atomique | §1.1 |

---

## 7. Vérifier soi-même

```bash
# le wizard expose bien 7 étapes et les vraies routes
curl -s localhost:8080/api/v1/public/docs/openapi.yaml | grep onboarding-wizard

# routes d'organisation réellement exposées
grep -rn '@RequestMapping("/api/v1' backend/src/main/java --include=*Controller.java

# suite backend
cd backend && mvn -B -Dtest='Onboarding*' test
```

# Audit de conformité contrat — Agent B ↔ Agent A (2026-09-28)

Vérification **exécutée**, pas déclarative. Source de vérité = le code backend réel
(commit de l'Agent A fusionné dans `a42d654`).

## 1. Routes : 7/7 résolvent des deux côtés

| Opération | Backend réel | Frontend | Mobile |
|---|---|---|---|
| Liste   | `GET /api/v1/onboarding-wizard` | ✅ | ✅ |
| Progression | `GET /api/v1/onboarding-wizard/progress` | ✅ | ✅ |
| Statut  | `GET /api/v1/onboarding-wizard/status` | ✅ | ✅ |
| Init    | `POST /api/v1/onboarding-wizard/initialize` | ✅ | ✅ |
| Start   | `POST /api/v1/onboarding-wizard/{id}/start` | ✅ | ✅ |
| Complete| `POST /api/v1/onboarding-wizard/{id}/complete` | ✅ | ✅ |
| Skip    | `POST /api/v1/onboarding-wizard/{id}/skip` | ✅ | ✅ |

Préfixe vérifié : axios `baseURL = /api/v1` (web) et `ApiConfig.baseUrl = .../api/v1`
(mobile, `productionUrl = https://discipolat-api.onrender.com/api/v1`).

## 2. DTO de sortie : égalité stricte des 3 faces

`OnboardingStepResponse` — **12 champs**, identiques côté backend, web et mobile :
`id, stepType, stepOrder, title, description, status, isCompleted, isSkippable,
skipRequiresReason, startedAt, completedAt, completedData`

`OnboardingStatusResponse` — **7 champs**, identiques côté backend, web et mobile :
`completed, completedAt, completedBy, totalSteps, completedSteps, skippedSteps, percentage`

## 3. Formes de requête (là où un décalage donne un 400 silencieux)

| Endpoint | Backend attend | Le client envoie | Verdict |
|---|---|---|---|
| `POST /{id}/complete` | `OnboardingStepData(Map<String,Object> data)`, corps **facultatif** | `{ "data": {…} }` ou `{}` | ✅ objet, jamais une String |
| `POST /{id}/skip` | `Map<String,String>`, champ `reason`, corps facultatif | `{ "reason": "…" }` (omitempty) | ✅ |
| `POST /auth/registration-status` | `RegistrationStatusRequest` | `{ email }` | ✅ |
| `GET /admin/tenant-features` | lecture seule | GET | ✅ |

## 4. Autres endpoints consommés par mes écrans

| Endpoint | Présent backend | Consommé par |
|---|---|---|
| `POST /api/v1/auth/registration-status` | ✅ `AuthController:58` | `RegistrationStatusPage` |
| `GET /api/v1/admin/tenant-features` | ✅ `TenantFeatureController` | `ModulesStep` (web + mobile) |

## 5. Points de vigilance assumés

- **RBAC** : le backend exige `@authz.isTenantAdmin()` sur les mutations. Un membre non-admin
  recevra `403` — les écrans traitent ce cas par un message dédié, pas un écran blanc.
- **Mobile** : le wizard n'est **pas encore routé** dans `app.dart` : le service, le modèle,
  l'écran et les 7 formulaires sont livrés et analysés, mais l'écran n'est pas encore
  atteignable depuis la navigation. Reste à faire avant de considérer B7 terminé.

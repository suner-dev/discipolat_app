# Audit Super Admin — Discipolat (2025-09-24)

## Résumé exécutif

| Couche | État | Score | Critique |
|--------|------|-------|----------|
| **Backend** | Quasi complet | 85/100 | Doublons contrôleurs, pas de feature flags persistés |
| **Frontend Web** | Squelettique | 45/100 | Dashboard stubs, aucun CRUD tenant/plans/impersonation UI |
| **Mobile** | Partiel (wizard only) | 35/100 | Pas de dashboard plateforme, pas liste tenants actionnable, pas entrée navigation |

---

## 1. Backend — Existant

### Endpoints `/api/v1/platform/admin` (SuperAdminController)

| Méthode | Path | Fonction | Status |
|---------|------|----------|--------|
| GET | `/dashboard` | Métriques globales + plans + subscriptions | ✅ |
| GET | `/tenants` | Liste paginée + filtres status/plan | ✅ |
| POST | `/tenants` | Création tenant + abonnement si payant | ✅ |
| POST | `/tenants/{id}/suspend` | Suspendre tenant | ✅ |
| POST | `/tenants/{id}/reactivate` | Réactiver tenant | ✅ |
| POST | `/tenants/{id}/archive` | Archiver (CANCELLED) | ✅ |
| GET | `/tenants/{id}` | Détail tenant + subscription + membres | ✅ |
| GET | `/plans` | Liste plans SaaS | ✅ |
| POST | `/plans` | Create/Update plan | ✅ |
| GET | `/feature-flags` | Flags en dur (pas DB) | ⚠️ stub |
| PUT | `/feature-flags/{key}` | Toggle flag (pas DB) | ⚠️ stub |
| POST | `/impersonate` | Démarrer impersonation (court TTL) | ✅ |
| POST | `/impersonate/stop` | Arrêter impersonation | ✅ |

### Endpoints `/api/v1/platform/admin/provisioning` (PlatformProvisioningController)

| Méthode | Path | Fonction |
|---------|------|----------|
| POST | `/church` | Église racine (OrganizationNode ROOT_CHURCH) |
| POST | `/department` | Département + responsable (nouveau/existant) |
| POST | `/family` | Famille + chef (nouveau/existant) |

### Problèmes backend

1. **Doublon** : `SuperAdminDashboardController` (`/platform/admin/dashboard/*`) recouvre `SuperAdminController` (`/platform/admin/dashboard`, `/tenants`, `/plans`).
2. **Feature flags** : En dur dans le contrôleur, pas persistés en DB.
3. **Pas d'audit logs query** : Pas d'endpoint pour lire les logs d'audit plateforme.
4. **Pas de quota/usage par tenant** : Endpoint manquant pour superviser consommation.

---

## 2. Frontend Web — Existant

### Pages

| Route | Page | État |
|-------|------|------|
| `/platform/dashboard` | `PlatformAdminDashboard` | ⚠️ Metrics OK, **boutons sans handlers** (Nouveau Tenant, Editer, Nouveau Plan, Editer/Désactiver plan) |
| `/platform/onboarding` | `PlatformOnboardingFlowPage` | ✅ Wizard 4 étapes complet, fonctionnel |
| `/platform/tenants` | `AdminTenantsPage` | ✅ CRUD complet (créer/éditer/supprimer/détail) mais **scope ADMIN** pas PLATFORM |
| `/platform/saas/plans` | `AdminTenantsPage` (réutilisée) | ⚠️ Même page, pas dédiée plans |
| `/platform/branding` | `TenantAdminBrandingPage` | Tenant-level |

### Gaps critiques frontend

- **Aucun modal création tenant** depuis le dashboard plateforme (bouton `+ Nouveau Tenant` ligne 145-147 sans `onClick`)
- **Aucune action** suspend/reactivate/archive sur lignes tenants (bouton "Editer" ligne 187 sans handler)
- **Aucune UI** impersonation, feature flags, audit logs
- **Plans** : boutons "Editer"/"Désactiver" sans handlers (lignes 237-242)
- **Route `/platform/tenants`** utilise `roles={['ADMIN']}` pas `scope="platform"` → accessible aux admins d'église (fuite de privilège)

---

## 3. Mobile — Existant

### Écrans platform (`presentation/screens/platform/`)

| Fichier | Route | État |
|---------|-------|------|
| `super_admin_provisioning_screen.dart` | `/platform/onboarding` | ✅ Wizard 4 étapes complet |
| `admin_tenants_screen.dart` | `/admin/tenants` | ⚠️ **Read-only** liste, pas d'actions, appelle `/tenants` (pas `/platform/admin/tenants`) |
| `admin_settings_screen.dart` | `/admin/settings` | Tenant-level |
| `platform_modules/menus/pages_screen.dart` | `/admin/modules|menus|pages` | Tenant-level |

### Gaps critiques mobile

1. **Aucun dashboard plateforme** — pas de métriques globales, pas d'entrée dans le drawer
2. **`AdminTenantsScreen`** : read-only, mauvais endpoint (`/tenants` vs `/platform/admin/tenants`), pas de suspend/reactivate/archive, pas de création
3. **Pas d'écrans** : Plans, Feature Flags, Impersonation, Audit Logs
4. **Navigation** : Drawer (`main_scaffold.dart`) n'a **aucune entrée Super Admin** — seulement "Demandes admin" → `/admin-requests`

---

## 4. Parité Web ↔ Mobile — Matrice

| Fonction | Backend | Web | Mobile |
|----------|---------|-----|--------|
| Dashboard métriques | ✅ | ✅ (lecture) | ❌ |
| Liste tenants + filtres | ✅ | ✅ (AdminTenantsPage) | ⚠️ (read-only, wrong endpoint) |
| Créer tenant | ✅ | ✅ (wizard) | ✅ (wizard) |
| Suspend/Reactivate/Archive tenant | ✅ | ❌ (UI manquante) | ❌ |
| Détail tenant + subscription | ✅ | ✅ (modal detail) | ❌ |
| Plans CRUD | ✅ | ❌ (UI stubs) | ❌ |
| Feature flags | ⚠️ (stub) | ❌ | ❌ |
| Impersonation | ✅ | ❌ | ❌ |
| Audit logs query | ❌ | ❌ | ❌ |
| Provisionnement guidé (4 étapes) | ✅ | ✅ | ✅ |

---

## 5. Plan d'action priorisé

### P0 — Bloquants (cette semaine)

1. **Backend** : Fusionner `SuperAdminDashboardController` → `SuperAdminController`, supprimer le doublon
2. **Backend** : Persister feature flags en DB (table `platform_feature_flags`)
3. **Frontend** : Brancher tous les boutons stubs du dashboard (modals créer/éditer tenant, actions suspend/reactivate/archive, plans CRUD)
4. **Frontend** : Corriger route `/platform/tenants` → `scope="platform"`
5. **Mobile** : Créer `PlatformAdminDashboardScreen` (métriques + actions rapides)
6. **Mobile** : Refaire `AdminTenantsScreen` → endpoint `/platform/admin/tenants` + actions + création
7. **Mobile** : Ajouter entrée "Super Admin" dans le drawer → `/platform/dashboard`

### P1 — Important (semaine suivante)

8. **Frontend + Mobile** : Écran Impersonation (liste sessions actives, démarrer/arrêter)
9. **Frontend + Mobile** : Écran Feature Flags (toggle persisté)
10. **Frontend + Mobile** : Écran Audit Logs (query paginée, filtres)
11. **Mobile** : `AdminPlansScreen`, `AdminFeatureFlagsScreen`

### P2 — Nice to have

12. **Backend** : Endpoint quota/usage par tenant
13. **Frontend + Mobile** : Supervision quotas, health checks plateforme

---

## 6. Fichiers à modifier (référence rapide)

### Backend
- `backend/src/main/java/com/discipolat/modules/platform/api/SuperAdminController.java` — consolidé
- `backend/src/main/java/com/discipolat/modules/platform/api/SuperAdminDashboardController.java` — **SUPPRIMER**
- `backend/src/main/java/com/discipolat/modules/platform/domain/PlatformFeatureFlag.java` — nouvelle entité
- `backend/src/main/java/com/discipolat/modules/platform/api/FeatureFlagController.java` — nouveau
- Migration Flyway pour `platform_feature_flags`

### Frontend
- `frontend/src/pages/PlatformAdminDashboard.tsx` — brancher tous les handlers
- `frontend/src/pages/AdminTenantsPage.tsx` — déjà OK, vérifier route
- `frontend/src/App.tsx` — ligne 1093-1094 corriger `scope="platform"`
- Nouveaux : `PlatformImpersonationPage.tsx`, `PlatformFeatureFlagsPage.tsx`, `PlatformAuditLogsPage.tsx`, `AdminPlansPage.tsx`

### Mobile
- `mobile/lib/presentation/screens/platform/platform_admin_dashboard_screen.dart` — **NOUVEAU**
- `mobile/lib/presentation/screens/platform/admin_tenants_screen.dart` — **REFACTOR complet**
- `mobile/lib/presentation/screens/platform/admin_plans_screen.dart` — **NOUVEAU**
- `mobile/lib/presentation/screens/platform/admin_feature_flags_screen.dart` — **NOUVEAU**
- `mobile/lib/presentation/screens/platform/admin_impersonation_screen.dart` — **NOUVEAU**
- `mobile/lib/app.dart` — routes + drawer entry
- `mobile/lib/widgets/app_drawer.dart` — entrée Super Admin

---

## 7. Commandes de validation

```bash
# Backend
cd backend && ./mvnw compile test

# Frontend
cd frontend && npm run build && npm run lint

# Mobile
cd mobile && flutter analyze && flutter test
```
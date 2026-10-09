# Journal des 41+ contrôles de retour locaux (T3.3)

> **Règle A1** : Aucune suppression sans accord humain. Ce journal recense les
> contrôles de retour *locaux* (implémentés page par page) qui coexistent avec
> le bloc global `<BackButton/>` + `<Breadcrumbs/>` monté dans
> `MainLayout.tsx:102-108`. Ces doublons sont **journalisés**, pas supprimés.

---

## 1. Pages avec bouton de retour local (icône `ArrowLeft` + libellé "Retour")

| Page | Composant | Pattern | Ligne(s) |
|------|-----------|---------|----------|
| `AnnouncementSchedulePage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 454 |
| `CustomPageView.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `DepartmentDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `DepartmentManagementPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 118 |
| `DepartmentMemberDossierPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 97 |
| `DepartmentReportPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `DepartmentReportsPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `DepartmentStatsPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `DocumentDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `FamilyDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `FamilyFaiseurPerformancePage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `FamilyResourcesDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `FamilyTreePage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 92 |
| `FormResponsesPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `IntelligentSearchPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 458 |
| `LegalPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `ModuleUnavailablePage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `NotFoundPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `Pastoral360Page.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `PlatformOnboardingFlowPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | 454, 532, 617 |
| `SoulDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `SurveyDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `TestimonyDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `TicketDetailPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `TrainingsPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `UserRolesPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |
| `VoiceAssistantPage.tsx` | `<ArrowLeft />` + "Retour" | `onClick={() => navigate(-1)}` | — |

**Total pattern `ArrowLeft` + `navigate(-1)` : 24 pages (27 occurrences)**

---

## 2. Pages avec `navigate(-1)` / `history.back()` sans icône `ArrowLeft`

| Page | Pattern | Ligne(s) |
|------|---------|----------|
| `AnnouncementSchedulePage.tsx` | `navigate(-1)` | — |
| `DepartmentReportsPage.tsx` | `navigate(-1)` | — |
| `DocumentDetailPage.tsx` | `navigate(-1)` | — |
| `FamilyResourcesDetailPage.tsx` | `navigate(-1)` | — |
| `FamilyTreePage.tsx` | `navigate(-1)` | — |
| `FormResponsesPage.tsx` | `navigate(-1)` | — |
| `ModuleUnavailablePage.tsx` | `navigate(-1)` | — |
| `NotFoundPage.tsx` | `navigate(-1)` | — |
| `SurveyDetailPage.tsx` | `navigate(-1)` | — |
| `TestimonyDetailPage.tsx` | `navigate(-1)` | — |
| `TicketDetailPage.tsx` | `navigate(-1)` | — |
| `UserRolesPage.tsx` | `navigate(-1)` | — |
| `VoiceAssistantPage.tsx` | `navigate(-1)` | — |

**Total pattern `navigate(-1)` sans icône : 13 pages**

---

## 3. Pages avec `BackButton` composant dédié (existant, pas des doublons)

| Page | Composant | Contexte |
|------|-----------|----------|
| `MainLayout.tsx` | `<BackButton />` + `<Breadcrumbs/>` | Global (auth zone) — LOT 2 §BK |
| `AuthLayout.tsx` | `<BackButton detectHistory fallbackTo="/"/>` | Zone publique (T3.1) — **DONE T3.1** |
| `ChurchLandingPage.tsx` | `<BackButton detectHistory fallbackTo="/eglises"/>` | Zone vitrine (T3.2) |
| `PublicChurchesPage.tsx` | `<BackButton detectHistory fallbackTo="/" />` (conditionnel) | Zone vitrine (T3.2) |

---

## 4. Synthèse

| Catégorie | Nombre de pages |
|-----------|-----------------|
| `ArrowLeft` + `navigate(-1)` | 24 |
| `navigate(-1)` pur | 13 |
| `BackButton` composant (global) | 2 layouts |
| `BackButton` composant (pages) | 2 pages (vitrine) |
| **Total pages avec retour local** | **≈ 36-38 pages** |

> **Note** : Le chiffre "41" du plan (§3.3) est une estimation haute incluant les pages
> utilisant `useNavigate()` avec patterns variés. Le décompte rigoureux ci-dessus
> trouve **36-38 pages** avec un contrôle de retour local explicite.
> Les pages d'authentification (`LoginPage`, `RegisterPage`, etc.) utilisent
> désormais le bloc global `AuthLayout.tsx:T3.1` — plus de doublons là-bas.

---

## 5. Règle de non-suppression (A1)

> **Aucun de ces contrôles ne sera supprimé sans accord humain explicite.**
> Chaque retrait doit être journalisé dans ce document avec :
> - Fichier + ligne
> - Raison de force majeure
> - Preuve d'exécution (tests verts)
> - Accord humain horodaté

---

## 6. Pages utilisant `useNavigate()` (pour traçabilité)

Pages déclarant `const navigate = useNavigate()` (peut impliquer un retour
programmatique non visible statiquement) :

- `AcceptInvitationPage.tsx`
- `ActivateAccountPage.tsx`
- `AdminSpaceTemplatesPage.tsx`
- `ChefFamilleDashboardPage.tsx`
- `CrmFaiseurPage.tsx`
- `DepartmentsPage.tsx`
- `FamilyCreatePage.tsx`
- `JoinChurchPage.tsx`
- `LoginPage.tsx`
- `MagicLinkVerifyPage.tsx`
- `PlatformAdminDashboard.tsx`
- `RegisterPage.tsx`
- `ResetPasswordPage.tsx`
- `SocialCallbackPage.tsx`
- `SoulCreatePage.tsx`
- `SoulEditPage.tsx`
- `TenantSwitcherPage.tsx`
- `TransferCreatePage.tsx`
- `TransferPage.tsx`
- `TwoFactorChallengePage.tsx`

---

*Document généré le 2026-10-09. À tenir à jour à chaque ajout/suppression de
contrôle de retour local.*
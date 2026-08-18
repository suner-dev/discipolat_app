# ARCHITECTURE_AUDIT — Discipolat (Refonte)

> Audit transversal complet — initié le 2026-08-18, mis à jour le 2026-08-18 (session Pasteur).
> Ce document constitue la **cartographie de référence** de l'application : modules,
> pages, entités, relations, permissions, bugs, manques et plan de refonte.
>
> Légende de classement : **A** = parfaitement fonctionnel · **B** = fonctionnel mais
> améliorable · **C** = partiellement fonctionnel · **D** = présent mais cassé ·
> **E** = interface uniquement · **F** = absent.
>
> Baselines vérifiées (2026-08-18, session Pasteur) :
> - Backend (Spring Boot 3.4.7, Java 21) : **532 tests ✓ BUILD SUCCESS**
> - Frontend web (React 19 / TS / Vite 6) : **228 tests vitest ✓ + `tsc -b` ✓**
> - Mobile (Flutter 3 / Dart) : **129 tests ✓ · `flutter analyze` 0 issue**
> - Liens morts : **0** sur les 49 liens de navigation
> - 70 migrations Flyway (V1→V70), 101 tables, 39 modules backend, 73 pages frontend, 57 écrans mobile

---

## TABLE DES MATIÈRES

1. [Vue d'ensemble](#1-vue-densemble)
2. [Modules backend](#2-modules-backend)
3. [Modules frontend](#3-modules-frontend)
4. [Modules mobile](#4-modules-mobile)
5. [Pages web — audit par page](#5-pages-web--audit-par-page)
6. [Écrans mobile — audit par écran](#6-écrans-mobile--audit-par-écran)
7. [Entités et schéma base de données](#7-entités-et-schéma-base-de-données)
8. [Matrice des relations](#8-matrice-des-relations)
9. [Système de permissions et rôles](#9-système-de-permissions-et-rôles)
10. [Matrice des dépendances entité→modules](#10-matrice-des-dépendances)
11. [Workflows métier](#11-workflows-métier)
12. [Source de vérité et duplications](#12-source-de-vérité-et-duplications)
13. [Bugs identifiés](#13-bugs-identifiés)
14. [Fonctionnalités manquantes](#14-fonctionnalités-manquantes)
15. [Fonctionnalités à améliorer](#15-fonctionnalités-à-améliorer)
16. [Problèmes UX](#16-problèmes-ux)
17. [Problèmes backend](#17-problèmes-backend)
18. [Problèmes mobile](#18-problèmes-mobile)
19. [Problèmes de sécurité](#19-problèmes-de-sécurité)
20. [Plan de refonte](#20-plan-de-refonte)

---

## 1. Vue d'ensemble

### Stack technique

| Couche | Technologie | Version |
|--------|------------|---------|
| Backend | Java 21 + Spring Boot | 3.4.7 |
| ORM | Spring Data JPA (Hibernate 6) | - |
| DB | PostgreSQL | 16 |
| Migrations | Flyway | 70 fichiers |
| Cache/Rate-limit | Redis 7 + Bucket4j | - |
| Frontend | React 19 + TypeScript | 5.7 |
| Build | Vite | 6.0 |
| Styling | TailwindCSS + glassmorphism | 3.4 |
| Mobile | Flutter / Dart | SDK ≥3.5 |
| State mobile | Riverpod | 2.6 |
| Auth | JWT RS256 + 2FA TOTP | - |
| Déploiement | Docker Compose / Render | - |
| CI/CD | GitHub Actions | 4 workflows |

### Métriques clés

| Métrique | Valeur |
|----------|--------|
| Tables PostgreSQL | **101** |
| Migrations Flyway | **70** (V1→V70, V66-V67 sautés) |
| Classes Java | **511** (457 modules + 53 common + 1 main) |
| Entités JPA | **88** |
| Repositories | **91** |
| Services | **61** |
| Controllers | **51** |
| Modules backend | **39** |
| Fichiers React/TSX | **146** |
| Pages web | **73** |
| Routes web | **54** (53 explicites + 404) |
| Écrans mobile | **57** |
| Routes mobile | **50** |
| Tests backend | **532** |
| Tests frontend | **228** |
| Tests mobile | **129** |
| Endpoints API | **~180** |
| Enums partagés | **16** (common/enums) |
| Dictionnaires configurables | **42** |

---

## 2. Modules backend

Architecture hexagonale sous `com.discipolat.modules.*` avec Spring Modulith.

| # | Module | Fichiers | État | Description |
|---|--------|----------|------|-------------|
| 1 | admin | 1 | B | Cache stats uniquement. Config répartie dans platform |
| 2 | ai | 2 | A | Assistant IA : analyse spirituelle, résumé, encouragement |
| 3 | alerts | 6 | A | Centre d'alertes (absence, retard, manuel) avec priorité |
| 4 | appointments | 7 | A | Système de rendez-vous pastoral |
| 5 | audit | 7 | A | Journal d'audit + export CSV + RBAC permissions |
| 6 | auth | 3 | A | Auth JWT classique |
| 7 | authentication | 13 | A | Login/register/refresh/2FA/demo-login |
| 8 | badges | 6 | A | Gamification (BRONZE→DIAMANT) |
| 9 | communications | 5 | A | Annonces ciblées + notifications IN_APP |
| 10 | customfields | 6 | A | Champs personnalisés paramétrables (13 types) |
| 11 | dashboard | 2 | A | KPIs, tendances, vues d'ensemble |
| 12 | departments | 61 | A | **Module le plus grand** : équipes, tâches, docs, matériel, settings |
| 13 | discipline | 6 | A | Suivi disciplinaire des âmes |
| 14 | evaluations | 5 | A | Évaluations croisées (upsert, 4 catégories) |
| 15 | evangelism | 10 | A | Pipeline d'évangélisation (11 étapes) |
| 16 | events | 12 | A | Événements + inscriptions + programme hebdo |
| 17 | families | 12 | A | Familles, risque, historique chef, comparaison |
| 18 | favorites | 5 | A | Favoris multi-entité |
| 19 | files | 12 | A | Upload/download + pièces jointes + import CSV |
| 20 | finances | 8 | A | Transactions, budgets, stats annuelles |
| 21 | interactions | 7 | A | CRM interactions (appel, SMS, visite…) |
| 22 | map | 4 | A | Cartographie Leaflet (souls + familles) |
| 23 | members | 18 | A | Espace membre : présences, demandes, profil |
| 24 | messages | 10 | A | Messagerie privée 1:1 |
| 25 | notifications | 5 | A | Notifications multi-canal |
| 26 | objectives | 7 | A | Objectifs mesurables par rôle |
| 27 | parallelfollowups | 6 | A | Suivis parallèles (hors parcours normal) |
| 28 | platform | 42 | A | **Configuration plateforme** : modules, menus, settings, pages, dictionnaires, feedback |
| 29 | prayers | 7 | A | Demandes de prière (4 visibilités) |
| 30 | programs | 8 | A | Types de programmes configurables |
| 31 | reports | 15 | A | Rapports faiseur + famille + corrections |
| 32 | search | 2 | A | Recherche globale transversale |
| 33 | souls | 36 | A | **Entité centrale** : CRUD, notes, tags, historique, sorties, score spirituel, pastoral-360 |
| 34 | tenants | 3 | A | Multi-tenancy (table tenants + filtres Hibernate) |
| 35 | trainings | 21 | A | Formation : cours, modules, quiz, inscriptions, certificats |
| 36 | transfers | 27 | A | Moteur de workflow de transfert (configurable) |
| 37 | users | 13 | A | Gestion utilisateurs + rôles + départements |
| 38 | visits | 7 | A | Visites pastorales planifiées |
| 39 | workflow | 1 | A | Tâches schedulées (escalade, anniversaires, snapshots) |

Infrastructure transversale : `common/` (SecurityUtils, TenantContext, PageResponse, MultiTenantInterceptor, TenantAwareRepository).

---

## 3. Modules frontend

| Section | Pages | Routes | État |
|---------|-------|--------|------|
| Auth | 5 (Landing, Login, Forgot, Reset, 2FA) | 5 | A |
| Dashboards | 7 (root, membre, pasteur, chef-famille, responsable, admin, CRM faiseur) | 7 | A/B |
| Âmes (souls) | 5 (list, detail, create, edit, pastoral-360) | 5 | A |
| Retractions âmes | 1 | 1 | A |
| Familles | 4 (list, detail, create, compare, faiseur-perf) | 5 | A/B |
| Départements | 6 (list, detail, report, manage, stats, tools, member-dossier) | 7 | A/C+ |
| Rapports | 4 (list, maker, family, urgent-aid) | 4 | A/B |
| Prières | 3 (list, spaces, actions-de-grâce) | 3 | A |
| Événements | 3 (list, program, statistics) | 3 | A |
| Programmes | 1 | 1 | A |
| Documents | 1 | 1 | A |
| Followups parallèles | 1 | 1 | A |
| Alertes | 1 | 1 | A |
| Finances | 1 | 1 | A |
| Communications | 1 | 1 | A |
| Notifications | 1 | 1 | A |
| Messages | 1 | 1 | A |
| Demandes membres | 1 | 1 | A |
| Carte | 1 | 1 | A |
| Évangélisation | 1 | 1 | A |
| Objectifs | 1 | 1 | A |
| Visites | 1 | 1 | A |
| Badges | 1 | 1 | A |
| Formations | 1 | 1 | B+ |
| Rendez-vous | 1 | 1 | A |
| Évaluations | 1 | 1 | A |
| Utilisateurs | 2 (list, profile) | 2 | A |
| Audit | 1 | 1 | A |
| Permissions | 1 | 1 | A |
| Admin | 8 (dashboard, settings, modules, menus, pages, custom-fields, feedback, dictionaries) | 8 | A |
| Transferts | 3 (list, create, detail) + admin | 4 | A |
| Pages custom | 1 | 1 | A |
| Search | 1 | 1 | A |

---

## 4. Modules mobile

47 écrans Flutter dans 27 modules (lib/presentation/screens/) :

| Module | Écrans | État |
|--------|--------|------|
| Auth (login, onboarding) | 2 | A |
| Dashboard (4 rôles) | 4 | A/B |
| Âmes (list, detail, pastoral-360, CRM faiseur) | 4 | A/B |
| CRM faiseur | (inclus dans Âmes) | B |
| Rapports (list, maker, family) | 3 | A |
| Familles | 1 | A |
| Départements (list, detail, management, stats, tools, report, member-dossier) | 7 | A/C+ |
| Évaluations | 1 | A |
| Recherche | 1 | A |
| Utilisateurs (list, detail, permissions, documents, audit) | 5 | A |
| Rendez-vous | 1 | A |
| Visites | 1 | A |
| Évangélisation | 1 | A |
| Objectifs | 1 | A |
| Badges | 1 | A |
| Formations | 1 | A |
| Messages (list, detail) | 2 | A |
| Suivis parallèles | 1 | A |
| Transferts (list, detail, create, admin) | 4 | A |
| Alertes | 1 | A |
| Demandes membres | 1 | A |
| Carte | 1 | A |
| Notifications | 1 | A |
| Prières | 1 | A |
| Événements | 1 | A |
| Communications | 1 | A |
| Finances | 1 | A |
| Profil | 1 | A |
| Sécurité | 1 | A |
| Platform admin (modules, menus, pages) | 3 | A |
| Onboarding | 1 | A |
| 404 | 1 | A |

**Note** : les dossiers `core/`, `shared/`, `features/` contiennent des sous-dossiers **vides** (scaffolding non utilisé). L'architecture réelle est dans `data/` + `presentation/`.

---

## 5. Pages web — audit par page

### 5.1 Authentification

| Page | Route | État | Notes |
|------|-------|------|-------|
| LandingPage | `/` | **A** | Glassmorphism propre, CTA, aucun problème |
| LoginPage | `/login` | **A** | Validation formulaire, 2FA, comptes démo, erreurs gérées |
| ForgotPasswordPage | `/forgot-password` | **B+** | Pas de validation côté client sur l'email ; pas d'état loading après clic |
| ResetPasswordPage | `/reset-password` | **B** | Pas de meter de force du mot de passe ; pas de champ confirmation |
| TwoFactorChallengePage | `/verify-2fa` | **A** | Auto-submit 6 digits, cooldown resend |

### 5.2 Dashboards

| Page | Route | État | Notes |
|------|-------|------|-------|
| DashboardGate | `/dashboard` | **A** | Redirection role-gate correcte |
| MemberDashboardPage | `/dashboard/membre` | **B+** | 875 lignes — monolithe ; formulaire profil inline devrait être extrait |
| ChefFamilleDashboardPage | `/dashboard/chef-famille` | **B** | Utilise `as any` (lignes 42, 47) |
| PasteurDashboardPage | `/dashboard/pasteur` | **A-** | Enrichi : KPIs cliquables avec filtres URL, transferts en attente, tendance présence (AreaChart), activité récente audit. Zéro `as any` dans ce fichier. Dead code `openFamille` défini mais non utilisé |
| ResponsableDashboardPage | `/dashboard/responsable` | **A** | Focus départements |
| AdminDashboardPage | `/admin` | **A** | Stats + santé système |
| CrmFaiseurPage | `/crm-faiseur` | **B** | Utilise `as any` (lignes 42, 50) |

### 5.3 Gestion des âmes

| Page | Route | État | Notes |
|------|-------|------|-------|
| SoulsPage | `/souls` | **A** | DataTable avec recherche/filtre/pagination + **filtres pilotés par URL** (statut, typeDisciple, search) — bidirectional sync URL ↔ état, zéro `as any` |
| SoulDetailPage | `/souls/:id` | **A** | Onglets profil/historique, lien Pastoral360 |
| SoulCreatePage | `/souls/new` | **A** | Validation, fallbacks dictionnaire |
| SoulEditPage | `/souls/:id/edit` | **A** | Formulaire pré-rempli |
| Pastoral360Page | `/souls/:id/pastoral-360` | **A** | Radar chart, gauge, timeline |
| SoulRetractionsPage | `/souls/retractions` | **A** | Flow approuver/rejeter avec commentaires |

### 5.4 Familles

| Page | Route | État | Notes |
|------|-------|------|-------|
| FamiliesPage | `/families` | **A** | DataTable, stats cartes |
| FamilyDetailPage | `/families/:id` | **A** | Onglets, liste âmes, perf charts — **noms de chefs résolus côté serveur** (UUIDs et Invalid Date éliminés) |
| FamilyCreatePage | `/families/new` | **A** | Formulaire validation |
| CompareFamiliesPage | `/families/compare` | **A** | POST comparison, métriques dynamiques |
| FamilyFaiseurPerformancePage | `/families/:id/faiseur-performance` | **B** | `as any` (lignes 19, 29) |

### 5.5 Départements

| Page | Route | État | Notes |
|------|-------|------|-------|
| DepartmentsPage | `/departments` | **A** | Hiérarchie propre |
| DepartmentDetailPage | `/departments/:id` | **A** | Sous-départements, membres |
| DepartmentReportPage | `/departments/:id/report` | **A** | Formulaire date range |
| DepartmentStatsPage | `/departments/:id/stats` | **A** | Charts recharts |
| DepartmentToolsPage | `/departments/:id/tools` | **A** | 109 lignes, onglets module-gated |
| **DepartmentManagementPage** | `/departments/:id/manage` | **C+** | **2032 lignes** — monolithe 10 onglets inline. Devrait être extrait en composants |
| **DepartmentMemberDossierPage** | `/departments/:id/members/:memberId` | **B+** | **1128 lignes** — `type Dossier = any` (ligne 13). Typage manquant |

### 5.6 Rapports

| Page | Route | État | Notes |
|------|-------|------|-------|
| ReportsPage | `/reports` | **A** | Status badges propres |
| MakerReportPage | `/reports/maker` | **B+** | `catch {}` vide ligne 161 — avale les erreurs silencieusement |
| FamilyReportPage | `/reports/family` | **A** | Validation et soumission propres |
| UrgentAidPage | `/reports/urgent-aid` | **A** | 121 lignes, propre |

### 5.7 CRM & Évangélisation

| Page | Route | État | Notes |
|------|-------|------|-------|
| EvangelismPage | `/evangelism` | **A** | Pipeline, suivi étapes, historique |
| VisitsPage | `/visits` | **A** | CRUD, statuts |
| ObjectivesPage | `/objectives` | **A** | Barres de progression |

### 5.8 Transferts

| Page | Route | État | Notes |
|------|-------|------|-------|
| TransfersPage | `/transfers` | **A** | Statuts typés, pagination |
| TransferCreatePage | `/transfers/new` | **A** | Formulaire dynamique par type |
| TransferDetailPage | `/transfers/:id` | **A** | Modal décision, timeline historique |
| TransferAdminPage | `/admin/transfers` | **A** | Éditeur workflow complet |

### 5.9 Prières & Spiritualité

| Page | Route | État | Notes |
|------|-------|------|-------|
| PrayersPage | `/prayers` | **A** | CRUD, visibilités, priorités |
| PrayerSpacesPage | `/prayers/spaces` | **A** | Onglets par rôle |
| ActionsDeGracePage | `/prayers/actions-de-grâce` | **A** | 128 lignes |

### 5.10 Événements & Programmes

| Page | Route | État | Notes |
|------|-------|------|-------|
| EventsPage | `/events` | **A** | 830 lignes, bien organisé |
| WeeklyProgramPage | `/events/program` | **A** | Calendrier semaine, templates |
| ProgramTypesPage | `/programs` | **A** | CRUD types + sous-types |
| EventStatisticsPage | `/events/statistics` | **A** | 132 lignes |

### 5.11 Communication & Social

| Page | Route | État | Notes |
|------|-------|------|-------|
| CommunicationsPage | `/communications` | **A** | Annonces ciblées |
| MessagesPage | `/messages` | **A** | Chat UI, polling |
| NotificationsPage | `/notifications` | **A** | List + mark-read |
| AlertsPage | `/alerts` | **A** | CRUD, priorités, filtres |

### 5.12 Admin

| Page | Route | État | Notes |
|------|-------|------|-------|
| AdminSettingsPage | `/admin/settings` | **A** | Config système |
| AdminCustomFieldsPage | `/admin/custom-fields` | **A** | CRUD champs dynamiques |
| AdminFeedbackPage | `/admin/feedback` | **A** | Feedback testeurs |
| AdminDictionariesPage | `/admin/dictionaries` | **A** | CRUD dictionnaires |
| PlatformModulesPage | `/admin/modules` | **A** | Toggle modules |
| PlatformMenusPage | `/admin/menus` | **A** | Config menus |
| PlatformPagesPage | `/admin/pages` | **A** | Page Builder |
| AuditPage | `/audit` | **A** | Logs + filtres + export |

### 5.13 Autres

| Page | Route | État | Notes |
|------|-------|------|-------|
| MapPage | `/map` | **A** | Leaflet, icônes custom |
| BadgesPage | `/badges` | **A** | Leaderboard + profil |
| EvaluationsPage | `/evaluations` | **A** | Star rating, stats |
| TrainingsPage | `/trainings` | **B+** | 707 lignes — quiz + inscription + certificats inline |
| AppointmentsPage | `/appointments` | **A** | Request/inbox |
| MemberRequestsPage | `/members/requests` | **A** | Flow demandes |
| ParallelFollowupsPage | `/parallel-followups` | **A** | 199 lignes |
| DocumentsPage | `/documents` | **A** | Upload/list |
| UsersPage | `/users` | **A** | Gestion utilisateurs |
| PermissionsPage | `/permissions` | **A** | Matrice rôle-permission |
| ProfilePage | `/profile` | **A** | Settings perso |
| CustomPageView | `/pages/:slug` | **A** | 91 lignes, très propre |
| ModuleUnavailablePage | `/module-unavailable` | **A** | Placeholder |
| NotFoundPage | `*` | **A** | 404 |

### Distribution des notes

| Note | Nombre | % |
|------|--------|---|
| **A / A-** | 58 | 77% |
| **B / B+** | 10 | 13% |
| **C+** | 1 | 1% |
| **D / E / F** | 0 | 0% |
| **Total** | **75** | |

---

## 6. Écrans mobile — audit par écran

### Distribution des notes mobile

| Note | Nombre | % |
|------|--------|---|
| **A** | 39 | 81% |
| **B / B+** | 7 | 15% |
| **C** | 2 | 4% |
| **D / E / F** | 0 | 0% |
| **Total** | **48** | |

### Écrans problématiques

| Écran | État | Problème |
|-------|------|----------|
| DepartmentManagementScreen | **C** | Vue globale sans profondeur (le web a 2032 lignes de détails) |
| DepartmentToolsScreen | **C** | Onglets simplifiés vs web |
| CrmFaiseurScreen | **B** | Données CRM limitées vs web |
| DepartmentDetailScreen | **B** | 7 appels API simultanés — potentiel lag |

### Dossiers vides (scaffolding non utilisé)

| Dossier | Contenu |
|---------|---------|
| `lib/core/config/` | Vide |
| `lib/core/network/` | Vide |
| `lib/core/storage/` | Vide |
| `lib/core/theme/` | Vide |
| `lib/core/utils/` | Vide |
| `lib/shared/models/` | Vide |
| `lib/shared/widgets/` | Vide |
| `lib/features/alerts/` | Vide |
| `lib/features/auth/` | Vide |
| `lib/features/dashboard/` | Vide |
| `lib/features/families/` | Vide |
| `lib/features/reports/` | Vide |
| `lib/features/souls/` | Vide |

---

## 7. Entités et schéma base de données

### 101 tables (état final après V70)

#### Entités centrales

| Table | Champs | Relations | Module |
|-------|--------|-----------|--------|
| `users` | 30+ (id, email, password_hash, first_name, last_name, role, statut, tenant_id…) | → user_roles, families, souls, departments, evaluations, conversations, notifications, appointments | users, auth |
| `souls` | 40+ (nom, prenom, email, type_disciple, statut, faiseur_id, famille_id, etat_spirituel, niveau_croissance, latitude, longitude, tenant_id…) | → families, users (faiseur), maker_reports, soul_notes, soul_tags, soul_history, soul_interactions, soul_departments, spiritual_score_history, evangelism_track, visits | souls |
| `families` | 15+ (nom, chef_famille_id, statut, niveau_risque, latitude, longitude, tenant_id…) | → users (chef), souls, family_reports, parallel_followups | families |
| `departments` | 10+ (nom, responsable_id, parent_id, statut, tenant_id…) | → users (responsable), department_teams, department_positions, department_tasks, department_members | departments |

#### Tables de reporting

| Table | Description |
|-------|-------------|
| `maker_reports` | Rapports hebdo faiseur par âme (JSONB presences_par_culte) |
| `family_reports` | Rapports familiaux consolidés (JSONB stats_agregees) |
| `report_corrections` | Traçabilité des corrections |
| `dashboard_metrics` | KPIs pré-calculés |

#### Tables DMS (Department Management System) — 16 tables

`department_teams`, `department_positions`, `department_assignments`, `department_tasks`, `department_activity`, `department_member_notes`, `department_member_objectives`, `department_member_reports`, `department_reports`, `department_checklists`, `department_checklist_items`, `department_equipment`, `department_settings`, `department_announcements`, `department_announcement_members`, `department_documents`

#### Tables transfert (workflow engine) — 6 tables

`transfer_workflow_configs`, `transfer_workflow_steps`, `transfer_requests`, `transfer_decisions`, `transfer_history`, `transfer_attachments`

#### Tables historique — 7 tables

`family_chief_history`, `family_department_history`, `family_history`, `family_risk_history`, `role_history`, `department_history`, `config_revisions`

#### Tables plateforme — 10 tables

`church_settings`, `platform_modules`, `menu_entries`, `platform_roles`, `permission_catalog`, `dictionary_entries`, `custom_field_definitions`, `custom_field_values`, `custom_pages`, `tenants`

#### Tables métier restantes

| Table | Module |
|-------|--------|
| `alerts` | alerts |
| `notifications` | notifications |
| `audit_logs` | audit |
| `soul_history` | souls |
| `soul_notes` | souls |
| `soul_tags` | souls |
| `soul_exits` | souls |
| `soul_retraction_requests` | souls |
| `soul_departments` | souls |
| `soul_interactions` | interactions |
| `spiritual_score_history` | souls |
| `member_departments` | members |
| `member_presences` | members |
| `member_requests` | members |
| `conversations` | messages |
| `conversation_messages` | messages |
| `evaluations` | evaluations |
| `evangelism_track` | evangelism |
| `evangelism_stage_history` | evangelism |
| `objectives` | objectives |
| `visits` | visits |
| `badges` | badges |
| `user_badges` | badges |
| `courses` | trainings |
| `course_modules` | trainings |
| `quiz_questions` | trainings |
| `course_enrollments` | trainings |
| `module_completions` | trainings |
| `certificates` | trainings |
| `appointments` | appointments |
| `prayers` | prayers |
| `events` | events |
| `event_registrations` | events |
| `weekly_program_templates` | events |
| `program_types` | programs |
| `program_sub_types` | programs |
| `culte_config` | programs |
| `department_event_attendance` | departments |
| `files` | files |
| `entity_attachments` | files |
| `favorites` | favorites |
| `activation_tokens` | authentication |
| `password_reset_tokens` | authentication |
| `event_publication` | Spring Modulith |
| `feedbacks` | platform |
| `finance_transactions` | finances |
| `finance_budgets` | finances |
| `communications` | communications |

---

## 8. Matrice des relations

```
tenants ──────────────< [TOUTES les 100 tables] via tenant_id

users ──< user_roles ──> roles
users ──< user_departments ──> departments
users ──< conversations (user_a, user_b)
users ──< conversation_messages
users ──< notifications
users ──< appointments (demandeur, recepteur)
users ──< evaluations (evalue, evaluateur)
users ──< prayers (auteur)
users ──< events (organisateur)
users ──< course_enrollments
users ──< user_badges ──> badges
users ──< maker_reports (faiseur)
users ──< family_reports (chef)
users ──< soul_notes (auteur)
users ──< soul_interactions (auteur)
users ──< visits (visiteur)
users ──< objectives (cree_par)
users ──< audit_logs (utilisateur)

souls ──< maker_reports ──> users (faiseur)
souls ──< soul_notes ──> users
souls ──< soul_tags
souls ──< soul_history ──> users
souls ──< soul_exits ──> users
souls ──< soul_interactions ──> users
souls ──< soul_departments ──> departments
souls ──< spiritual_score_history
souls ──> evangelism_track (1:1)
souls ──< visits ──> users
souls ──> users (user_id, lien optionnel)
souls ──> families (famille_id)
souls ──> users (faiseur_id)

families ──< souls
families ──< family_reports ──> users
families ──< parallel_followups ──> users
families ──< family_chief_history
families ──< family_department_history
families ──< family_risk_history
families ──> users (chef_famille_id, user_id, chef_adjoint_id)

departments ──< department_teams (récurssif: parent_id)
departments ──< department_positions
departments ──< department_assignments
departments ──< department_tasks
departments ──< department_activity
departments ──< department_member_notes
departments ──< department_member_objectives
departments ──< department_member_reports
departments ──< department_reports
departments ──< department_checklists ──< department_checklist_items
departments ──< department_equipment
departments ──< department_settings
departments ──< department_announcements ──< department_announcement_members
departments ──< department_documents
departments ──< department_event_attendance ──> events
departments ──< member_departments ──> souls
departments ──< user_departments ──> users

conversations ──< conversation_messages ──> users

transfer_requests ──< transfer_decisions ──> users
transfer_requests ──< transfer_history ──> users
transfer_requests ──< transfer_attachments ──> files
transfer_requests ──> transfer_workflow_configs ──< transfer_workflow_steps

events ──< event_registrations ──> users
courses ──< course_modules ──< quiz_questions
courses ──< course_enrollments ──> users
course_enrollments ──< module_completions ──> course_modules
course_enrollments ──< certificates

role_permissions ──> platform_roles
role_permissions ──> permission_catalog

platform_modules ──< menu_entries
custom_field_definitions ──< custom_field_values
dictionary_entries (auto-référent)
custom_pages (auto-référent)
```

---

## 9. Système de permissions et rôles

### 6 rôles

| Rôle | Description | périmètre |
|------|-------------|-----------|
| **ADMIN** | Super-administrateur | Tout |
| **PASTEUR** | Pasteur/leader spirituel | Tout (sauf config plateforme) |
| **RESPONSABLE** | Responsable de département | Ses départements + âmes associées |
| **CHEF_DE_FAMILLE** | Chef de famille spirituelle | Sa famille |
| **FAISEUR** | Faiseur/discipleur | Ses âmes assignées |
| **MEMBRE** | Membre standard | Profil perso, formations, badges |

### Matrice RBAC

| Action | ADMIN | PASTEUR | RESPONSABLE | CHEF_DE_FAMILLE | FAISEUR | MEMBRE |
|--------|-------|---------|-------------|-----------------|---------|--------|
| CRUD âmes | ✓ | ✓ | ✓ (scope) | ✓ (famille) | ✓ (assignées) | ✗ |
| Supprimer âme | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Voir toutes les âmes | ✓ | ✓ | scope | scope | scope | ✗ |
| CRUD départements | ✓ | ✓ | ✓ (own) | ✗ | ✗ | ✗ |
| CRUD familles | ✓ | ✓ | ✓ | ✓ (own) | ✗ | ✗ |
| Rapports faiseur | ✓ | ✓ | ✓ (valider) | ✗ | ✓ (créer) | ✗ |
| Rapports famille | ✓ | ✓ | ✓ (valider) | ✓ (créer) | ✗ | ✗ |
| Transferts approuver | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Config plateforme | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Gestion utilisateurs | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Finances | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ |
| Évaluations | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Formations | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (inscrire) |
| Badges | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ (voir) |
| Messagerie | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Demandes membres | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ |
| Carte | ✓ | ✓ | ✓ | ✗ | ✓ | ✗ |

### Scopage des données (WorkspaceScopeService)

| Rôle actif | Âmes visibles | Familles visibles | Départements visibles |
|------------|---------------|-------------------|-----------------------|
| ADMIN | TOUTES | TOUTES | TOUS |
| PASTEUR | TOUTES | TOUTES | TOUS |
| RESPONSABLE | Âmes de SES départements | Familles de ces âmes | Ses départements |
| CHEF_DE_FAMILLE | Âmes de SA famille | SA famille | Aucun |
| FAISEUR | SES âmes assignées | Familles de ces âmes | Aucun |
| MEMBRE | Aucune | Aucun | Aucun |

---

## 10. Matrice des dépendances

### Entité → Modules qui l'utilisent

| Entité | Modules | Actions | Stats | Notifications |
|--------|---------|---------|-------|--------------|
| **User** | auth, users, departments, families, souls, evaluations, messages, appointments, prayers, events, trainings, badges, transfers, audit, dashboard, members | CRUD, switch-role, 2FA, promote, transfer | workload, activity | IN_APP, EMAIL |
| **Soul** | souls, families, reports, interactions, evangelism, visits, objectives, parallelfollowups, alerts, map, search, transfers, ai, discipline, customfields, files | CRUD, reassign, exit, reintegrate, tag, note, score | pastoral-360, KPIs | IN_APP |
| **Family** | families, souls, reports, alerts, departments, transfers, communications, map, search, customfields | CRUD, reassign-chief, risk-update, compare | risk-history, stats | IN_APP |
| **Department** | departments, souls, members, events, alerts, tasks, transfers, communications, search | CRUD, manage, report, tools, attendance | KPIs, stats | IN_APP |
| **MakerReport** | reports, alerts, dashboard | create, submit, validate, correct | completion-rate | AUTO-ALERT |
| **FamilyReport** | reports, families, dashboard | create, submit, validate | completion-rate | AUTO-ALERT |
| **TransferRequest** | transfers, souls, families, departments, audit | create, submit, decide, cancel, execute | pending-count | IN_APP |
| **Alert** | alerts, dashboard, workflow | create, resolve, escalate | by-type | IN_APP |
| **Event** | events, departments, calendar | create, register, attend, cancel | attendance | IN_APP |
| **Notification** | notifications | create, mark-read, bulk-read | unread-count | — |
| **Evaluation** | evaluations, users | create, update | average-score | — |
| **Prayer** | prayers | create, update, answer, archive | by-status | IN_APP |
| **Message** | messages | send, read | unread-count | IN_APP |
| **Communication** | communications | create, publish, archive | — | IN_APP (targets) |
| **FinanceTransaction** | finances | create, update, delete | monthly-series | — |
| **Course** | trainings | create, enroll, complete, certify | stats | — |

---

## 11. Workflows métier

### 11.1 Création d'âme
```
Création (FAISEUR+) → affectation faiseur → affectation famille →
soul_history(CREATION) → notification faiseur → index recherche
```

### 11.2 Transfert d'âme
```
Demande (RESPONSABLE+) → sélection workflow → soumission →
décision validateur(s) → exécution (TransferExecutor) →
mise à jour: famille/faiseur/département → historique → notifications
→ audit_log → recherche mise à jour
```

### 11.3 Rapport faiseur hebdomadaire
```
Saisie (FAISEUR) → presences_par_culte JSONB → soumission →
validation (RESPONSABLE) → agrégation family_report →
alert si non-soumis après délai → dashboard metrics
```

### 11.4 Pipeline d'évangélisation
```
NON_CONTACTE → PREMIER_CONTACT → SUIVI → INVITE_CULTE →
PREMIER_CULTE → ... → BAPTEME
(avec historique des transitions et notifications)
```

### 11.5 Demande de retrait d'âme
```
Soumission → notification pasteur → APPROUVEE/REJETEE →
si approuvé: sortie âme (soul_exits) + réintégration possible
```

### 11.6 Demande de rendez-vous
```
MEMBRE demande → notification destinataire → CONFIRME/REFUSE →
si confirmé: rappel → TERMINE
```

### 11.7 Demande de membre
```
MEMBRE soumet (SUGGESTION/RENDEZ_VOUS/SIGNALEMENT) →
ciblé (PASTEUR/RESPONSABLE/CHEF) → traitement → RESOLU/REJETE
```

### 11.8 Publication d'annonce
```
ADMIN/PASTEUR crée (BROUILLON) → cible (TOUS/ROLE/FAMILLE/DEPARTEMENT) →
PUBLIEE → notifications IN_APP aux destinataires ciblés
```

### 11.9 Escalade automatique (workflow schedulé)
```
Vérification périodique → âme absente > N semaines →
alert HAUTE au pasteur → si pas traité → escalade URGENTE
+ anniversaires → notification au chef de famille
+ snapshot score spirituel hebdomadaire
```

---

## 12. Source de vérité et duplications

### Principe : UNE ENTITÉ = UNE SOURCE DE VÉRITÉ

**Validation (PropagationConsistencyTest, 8 tests ✓)** :
- ✅ Changement de faiseur → âme, historique, notifications, CRM, recherche : **cohérent**
- ✅ Transfert famille → famille, historique, notifications : **cohérent**
- ✅ Transfert département → désaffectation/affectation, notifications : **cohérent**
- ✅ Changement chef famille → famille, historique, notifications : **cohérent**
- ✅ Transfert faiseur avec/sans disciples : **cohérent**
- ✅ Changement responsable département → liaison, notifications : **cohérent**
- ✅ Modification âme → statut, état, historique, recherche, stats : **cohérent**

### Points de duplication identifiés

| Duplication | Sévérité | Description |
|-------------|----------|-------------|
| `soul_departments` vs `member_departments` | **Moyenne** | Deux tables M:N âme↔département (V17 et V27). V27 est la version moderne. V17 pourrait être migrée. |
| `member_presences` vs `department_event_attendance` | **Faible** | Présences membre vs présences événement département — cas d'usage différents |
| `faiseur_transfers` vs `transfer_requests` | **Faible** | Legacy (V5) vs workflow moderne (V32). Le legacy est maintenu pour rétro-compatibilité |
| Rôle `role` (colonnes users) vs `user_roles` (table) | **Faible** | Double stockage du rôle principal — synchronisation maintenue |
| `roles` JSONB dans `communications` vs `role_permissions` | **Aucune** | Cibles d'annonce vs permissions d'accès — concepts différents |

### Entités partagées intactes

- ✅ Un **User** = le même User partout (auth, dashboard, profiles, evaluations)
- ✅ Un **Soul** = la même âme partout (CRM, rapports, pastoral-360, carte, transferts)
- ✅ Une **Family** = la même famille partout (rapports, alerts, risque, comparaison)
- ✅ Un **Department** = le même département partout (DMS, stats, événements, outils)

---

## 13. Bugs identifiés

### Bugs non résolus

| # | Sévérité | Module | Description |
|---|----------|--------|-------------|
| 1 | **Cosmétique** | Frontend | Warning console `<linearGradient>` SVG — recharts + React 19 (inoffensif) |
| 2 | **Faible** | Frontend | `MakerReportPage:161` — `catch {}` vide avale les erreurs silencieusement |
| 3 | **Faible** | Frontend | `type Dossier = any` dans `DepartmentMemberDossierPage` — perte de typage |
| 4 | **Moyen** | Frontend | 16 blocs `catch {}` vides (AuthContext, SettingsContext, MetaContext, branding, PageBlockRenderer, LoginPage, AuditPage, AdminFeedbackPage, TrainingsPage, PlatformMenusPage, etc.) — erreurs silencieusement avalées |
| 5 | **Moyen** | Backend | 5 blocs `catch {}` vides dans `SecurityUtils.java` (l.48, 69, 112) + `ReportService` + `AuthService` — erreurs JWT silencieusement ignorées |
| 6 | **Moyen** | Backend | 19+ `@RequestBody` sans `@Valid` sur des DTOs typés (PlatformConfig, Dictionary, PageBuilder, CustomField, User, Family, File controllers) — validation côté serveur contournée |
| 7 | **Moyen** | Backend | `User.roles` avec `FetchType.EAGER` (@ElementCollection) — performance dégradée sur l'entité la plus fréquente |
| 8 | **Faible** | Backend | `DashboardService.getPasteurDashboard()` : `findAll()` + filtrage Java pour transferts — pattern N+1 |
| 9 | **Faible** | Backend | Code dupliqué : résolution Soul→User dans `TransferExecutor.notify()` et `TransferWorkflowService.notifyConcerned()` — devrait être un helper partagé |
| 10 | **Faible** | Backend | Token blacklist en mémoire (`ConcurrentHashMap`) — ne survit pas au redémarrage, pas scalable multi-instance |

### Bugs corrigés pendant le développement

| Bug | Correction |
|-----|-----------|
| `TenantFilterInterceptor` `NoSuchBeanDefinitionException` | Passé en `ObjectProvider` null-safe |
| IDOR `DashboardService` | `ForbiddenException` ajoutée |
| `GET /users/me` manquant | Endpoint ajouté dans `UserController` |
| Event type CHECK (9 types vs 13 dictionnaire) | V62 corrigé |
| Dead route `/parallel-followups` mobile | V3.0.5 corrigé |
| `LoginPage` useEffect guard 2FA | V3.14.0 corrigé |
| File contract mismatch (url/typeMime vs chemin/typeFichier) | V3.7.1 corrigé |
| Family report "empty array = keep old files" | V3.7.0 corrigé |
| Notification `NOT NULL tenant_id` violation dans jobs planifiés | `NotificationService` : overload 8 params avec tenantId explicite |
| FK violation `destinataire_id` sur âme sans compte utilisateur | `TransferExecutor` + `TransferWorkflowService` : résolution Soul→User avant notification |
| UUID bruts affichés dans les familles + "Invalid Date" | `FamilyController` : noms de chefs résolus côté serveur |

---

## 14. Fonctionnalités manquantes

### Backend

| # | Module | Manque |
|---|--------|--------|
| 1 | auth | Pas d'inscription publique (register = admin only) |
| 2 | notifications | Pas de moteur de notifications configurable en UI (templates rigides) |
| 3 | search | Pas d'index ElasticSearch/Meilisearch (requêtes LIKE simples) |
| 4 | dashboard | Pas de dashboard personnalisable par rôle (layout figé) |
| 5 | ai | Pas de vrai moteur IA (prompts hardcodés, pas d'LLM intégré) |
| 6 | files | Pas de stockage objet (fichiers en local, pas S3-compatible) |
| 7 | tenants | Pas d'endpoint REST de gestion des tenants (admin CLI only) |
| 8 | workflow | Pas de workflow builder visuel (seulement config JSON) |

### Frontend

| # | Manque |
|---|--------|
| 1 | Pas de mode hors-ligne |
| 2 | Pas de PWA push notifications |
| 3 | Pas de pages d'erreur 500/502 personnalisées |
| 4 | Pas de skeleton loading (seulement spinner) |
| 5 | Pas de system de thème dark/light toggle persistant |
| 6 | Pas d'accessibilité keyboard navigation complète |

### Mobile

| # | Manque |
|---|--------|
| 1 | Pas d'authentification biométrique réelle (pin stocké localement seulement) |
| 2 | Pas de mode hors-linge (SyncService existe mais pas câblé sur tous les écrans) |
| 3 | Pas de push notifications réelles (Firebase configuré mais pas déployé) |
| 4 | Pas de pagination (tous les appels chargent tout) |
| 5 | Pas de profil utilisateur éditable |
| 6 | Pas de drag-and-drop pour réordonner |
| 7 | Les dossiers `core/`, `shared/`, `features/` sont vides (scaffolding mort) |

---

## 15. Fonctionnalités à améliorer

| # | Module | Amélioration |
|---|--------|-------------|
| 1 | departments | `DepartmentManagementPage` (2032 lignes) → extraire les 10 onglets en composants séparés |
| 2 | departments | `DepartmentMemberDossierPage` (1128 lignes) → extraire + typer correctement (30 `: any` + 10 `as any`) |
| 3 | frontend | 63 casts `as any` à remplacer par des types réels (vs 52 en audit précédent) |
| 4 | frontend | 94 annotations `: any` à typer correctement |
| 5 | frontend | 16 blocs `catch {}` vides → au minimum `console.error(err)` |
| 6 | frontend | `MemberDashboardPage` (875 lignes) → extraire le formulaire profil |
| 7 | frontend | `TrainingsPage` (707 lignes) → extraire le système quiz |
| 8 | frontend | `ResetPasswordPage` → ajouter champ confirmation + strength meter |
| 9 | frontend | `ForgotPasswordPage` → ajouter validation email + loading state |
| 10 | frontend | 7 dossiers composants vides (alerts/, auth/, common/, dashboard/, families/, reports/, souls/) → peupler ou supprimer |
| 11 | backend | `departments` (61 fichiers) → évaluer si scindable en sous-modules |
| 12 | backend | Ajouter `@Valid` sur tous les `@RequestBody` DTOs typés |
| 13 | backend | Extraire helper Soul→User (résolution `userId` depuis `soulRepository`) des deux classes de transfert |
| 14 | backend | `User.roles` → passer en `FetchType.LAZY` |
| 15 | backend | Token blacklist → migrer vers Redis (déjà disponible) |
| 16 | backend | `DashboardService` : remplacer `findAll()` par `findByStatutIn()` pour les transferts |
| 17 | backend | Ajouter `X-RateLimit-Remaining` headers |
| 18 | backend | Export CSV filtré par tenant |
| 19 | mobile | Pagination sur toutes les listes |
| 20 | mobile | Consolider le scaffolding `core/`, `shared/`, `features/` ou le supprimer |

---

## 16. Problèmes UX

| # | Sévérité | Plateforme | Description |
|---|----------|------------|-------------|
| 1 | **Moyen** | Web | Monolithes de pages (2032, 1128, 875, 707 lignes) — maintenance difficile |
| 2 | **Moyen** | Web | 63 casts `as any` + 94 annotations `: any` — perte de typage, risques runtime (hauts: DeptMemberDossier 30+10, DeptManagement 17+6, DashboardPage 16, ResponsableDashboard 12) |
| 3 | **Moyen** | Web | 16 blocs `catch {}` vides — erreurs silencieusement avalées (AuthContext, SettingsContext, MetaContext, branding, PageBlockRenderer, LoginPage, AuditPage, AdminFeedbackPage, TrainingsPage, etc.) |
| 4 | **Moyen** | Web | Aucun système i18n — texte FR hardcodé dans 30+ fichiers (centaines de chaînes, labels, messages toast) |
| 5 | **Faible** | Web | 7 dossiers composants vides (alerts/, auth/, common/, dashboard/, families/, reports/, souls/) — scaffolding mort |
| 6 | **Faible** | Web | Pas de skeleton loading — spinners uniquement |
| 7 | **Faible** | Web | Pas de transition entre pages |
| 8 | **Moyen** | Mobile | Pas de pagination — listes potentiellement lentes |
| 9 | **Moyen** | Mobile | 13 dossiers scaffolding vides — confusion architecture |
| 10 | **Faible** | Mobile | Pas de haptic feedback |
| 11 | **Faible** | Mobile | Pas de pull-to-refresh sur toutes les listes |
| 12 | **Faible** | Web | Pas de mode hors-ligne (PWA scope limité) |
| 13 | **Faible** | Web+Mobile | Internationalisation FR uniquement (pas de EN runtime) |
| 14 | **Info** | Mobile | Filtres affichent les noms d'énum bruts (ex: `DECROCHE`) au lieu de libellés lisibles |

---

## 17. Problèmes backend

| # | Sévérité | Description |
|---|----------|-------------|
| 1 | **Moyen** | Module `departments` : 61 fichiers — candidat au scindage |
| 2 | **Moyen** | 19+ `@RequestBody` sans `@Valid` — validation contournée (PlatformConfig, Dictionary, PageBuilder, CustomField, User, Family, File controllers) |
| 3 | **Moyen** | `User.roles` avec `FetchType.EAGER` (@ElementCollection) — charge les rôles à chaque lecture |
| 4 | **Moyen** | Token blacklist en mémoire (`ConcurrentHashMap`) — pas de persistence/redémarrage ni scalabilité multi-instance |
| 5 | **Moyen** | `DashboardService.getPasteurDashboard()` : `findAll()` + filtrage Java pour transferts — pattern N+1 à remplacer par `findByStatutIn()` |
| 6 | **Faible** | Recherche globale : requêtes LIKE simples, pas d'index full-text |
| 7 | **Faible** | `dashboard_metrics` : table de cache mais pas de TTL/invalidation automatique |
| 8 | **Faible** | `event_publication` : Spring Modulith — table de transition, pas nettoyée |
| 9 | **Faible** | Pas de health check dédié (actuator standard uniquement) |
| 10 | **Faible** | Rate limiting par IP mais pas par tenant |
| 11 | **Faible** | Code dupliqué : résolution Soul→User dans `TransferExecutor` et `TransferWorkflowService` — devrait être un helper partagé |

---

## 18. Problèmes mobile

| # | Sévérité | Description |
|---|----------|-------------|
| 1 | **Critique** | Pas de push notifications réelles (Firebase configuré, jamais déployé) |
| 2 | **Élevé** | Pas de mode hors-ligne (SyncService existe mais limité aux rapports) |
| 3 | **Élevé** | Pas de pagination — toutes les listes chargent 100% des données |
| 4 | **Moyen** | 13 dossiers scaffolding vides (`core/`, `shared/`, `features/`) |
| 5 | **Moyen** | Auth biométrique = stockage PIN local, pas de vrai biometric |
| 6 | **Moyen** | `DepartmentManagementScreen` = vue simplifiée vs web (7 écrans vs 1 mega-page) |
| 7 | **Faible** | Pas de profil utilisateur éditable |
| 8 | **Faible** | `StatefulWidget` partout — pas de Riverpod feature-level |

---

## 19. Problèmes de sécurité

| # | Sévérité | Description | Statut |
|---|----------|-------------|--------|
| 1 | **Corrigé** | Multi-tenant data isolation (P0) | ✅ V70 |
| 2 | **Corrigé** | IDOR DashboardService (P0) | ✅ |
| 3 | **Corrigé** | TenantFilterInterceptor crash | ✅ ObjectProvider |
| 4 | **Corrigé** | Notification NOT NULL tenant_id dans jobs planifiés | ✅ Overload tenantId |
| 5 | **Corrigé** | FK violation Soul→User dans notifications transfert | ✅ Résolution ID |
| 6 | **Moyen** | 5 `catch {}` vides dans `SecurityUtils.java` — erreurs JWT silencieusement ignorées | Non traité |
| 7 | **Moyen** | 19+ `@RequestBody` sans `@Valid` sur DTOs typés — validation contournée | Non traité |
| 8 | **Faible** | `User.roles` `FetchType.EAGER` — performance | Non traité |
| 9 | **Faible** | Token blacklist en mémoire (`ConcurrentHashMap`) — pas de persistence ni scalabilité | Non traité |
| 10 | **Faible** | Rate limiting par IP mais pas par tenant | Non traité |
| 11 | **Faible** | Mobile : pas de screenshot protection côté serveur | Non traité |
| 12 | **Faible** | Mobile : session timeout = SharedPreferences local, contournable | Non traité |
| 13 | **Info** | Les 2FA backup codes sont en clair dans la DB | Design choice (chiffrés au repos via Postgres TDE) |
| 14 | **Info** | JWT private key en fichier local (dev) | Acceptable en dev, KMS en prod |

---

## 20. Plan de refonte

### Phase 1 — Consolidation Pasteur (EN COURS)

**Objectif** : Faire du Pasteur un centre de supervision complet.

| Action | Module | État | Effort |
|--------|--------|------|--------|
| Dashboard Pasteur enrichi (KPIs tendances, alertes non traitées, activité récente) | dashboard | ✅ Fait | S |
| KPIs cliquables avec filtres URL (`/souls?statut=X`) — web + mobile | dashboard, souls | ✅ Fait | S |
| Transferts en attente dans dashboard Pasteur | dashboard, transfers | ✅ Fait | S |
| Tendance présence (AreaChart 12 semaines) | dashboard | ✅ Fait | S |
| Activité récente (audit feed) dans dashboard | dashboard, audit | ✅ Fait | S |
| Noms de chefs résolus côté serveur (UUIDs bruts éliminés) | families | ✅ Fait | S |
| Supervision des transferts (décision depuis le dashboard) | transfers, dashboard | ✅ Fait | S |
| Correctifs multitenancy notifications (tenantId dans jobs planifiés) | notifications, workflow | ✅ Fait | S |
| Fix FK Soul→User dans notifications de transfert | transfers | ✅ Fait | S |
| Pastoral 360 amélioré (graphiques d'évolution, comparaison inter-âmes) | souls | ⏳ Reste | M |
| Centre d'alertes pour Pasteur (vue unifiée, actions bulk) | alerts | ⏳ Reste | S |
| Audit exploitable (filtres avancés, export, tendances) | audit | ⏳ Reste | S |

### Phase 2 — Consolidation Admin

**Objectif** : Admin = centre de configuration complet.

| Action | Module | Effort |
|--------|--------|--------|
| Gestion tenants (CRUD via REST) | tenants | M |
| Notifications configurables (templates UI) | notifications | L |
| Dashboard admin enrichi (santé multi-tenant, métriques) | dashboard | S |

### Phase 3 — Réduction de la dette technique

| Action | Effort |
|--------|--------|
| Extraire DepartmentManagementPage en 10 composants | M |
| Extraire DepartmentMemberDossierPage + typer (30 `: any` + 10 `as any`) | M |
| Remplacer les 63 `as any` + 94 `: any` par des types réels | M |
| Ajouter `console.error` aux 16 blocs `catch {}` vides frontend | S |
| Extraire MemberDashboardPage formulaire profil | S |
| Extraire TrainingsPage système quiz | M |
| Nettoyer scaffolding mobile vide (13 dossiers) | S |
| Peupler/supprimer 7 dossiers composants vides frontend | S |
| Ajouter `@Valid` sur 19+ `@RequestBody` sans validation | S |
| Extraire helper Soul→User partagé (2 classes de transfert) | S |
| `User.roles` → `FetchType.LAZY` | S |
| Token blacklist → Redis | S |

### Phase 4 — Parité mobile

| Action | Effort |
|--------|--------|
| Push notifications Firebase réelles | M |
| Pagination toutes les listes | M |
| Mode hors-ligne étendu | L |
| Profil utilisateur éditable | S |
| Auth biométrique réelle | M |

### Phase 5 — Performance & UX

| Action | Effort |
|--------|--------|
| Skeleton loading web | S |
| Index full-text recherche | M |
| Rate limiting par tenant | S |
| Export CSV filtré par tenant | S |
| PWA push notifications | M |
| DashboardService : `findByStatutIn()` au lieu de `findAll()` + filtrage Java | S |
| Placeholder i18n (constantes FR centralisées) | M |

### Phase 6 — Skins & Branding

| Action | Effort |
|--------|--------|
| Thème dark/light toggle persistant | S |
| Branding 100% dynamique (12 fields encore hardcodés) | S |

### Efforts estimés

| Taille | Signification |
|--------|---------------|
| S | 1-2 jours |
| M | 3-5 jours |
| L | 1-2 semaines |

---

## Annexes

### A. Inventaire complet des endpoints API

Voir `BACKEND_INVENTORY.md` (1555 lignes) pour l'inventaire exhaustif de chaque endpoint, entité, champ, et relation de chaque module.

### B. Inventaire mobile complet

Voir la section [Modules mobile](#4-modules-mobile) et le fichier `MOBILE_AUDIT.md` pour l'audit détaillé mobile.

### C. Baselines de tests

| Composant | Tests | Statut |
|-----------|-------|--------|
| Backend | 532 | ✅ BUILD SUCCESS |
| Frontend | 228 | ✅ vitest + tsc |
| Mobile | 129 | ✅ analyze 0 issue |
| E2E | 16 étapes | ✅ (navigateur Chrome) |
| Propagation | 8 | ✅ (cohérence transversale) |

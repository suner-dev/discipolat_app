# Audit Complet & 20 Fonctionnalités Incontournables — Discipolat

> **Date** : 20 août 2026  
> **Objet** : Revue minutieuse de l’application fullstack/mobile (Spring Boot + React + Flutter), diagnostic des forces/faiblesses, et 20 propositions de fonctionnalités puissantes, modernes et futuristes — validées pour le **marché local africain** ET l’**échelle mondiale**.  
> **Auteur** : Analyse synthétique — Cline

---

## 1. Identité de l’application

**Discipolat** est une **plateforme SaaS de gestion et de suivi du discipolat ecclésiastique**. Née en contexte francophone, elle est aujourd’hui en pleine expansion internationale. Elle couvre le *fullstack mobile* avec trois couches parfaitement alignées :

| Couche | Techno | Points forts identifiés |
|---|---|---|
| **Backend** | Java 21 / Spring Boot 3 / Spring Modulith / PostgreSQL 16 / Redis 7 / JWT RS256 / Flyway (63 migrations) / Bucket4j | 38 modules métier, ~454 tests, architecture hexagonale + Modulith, workflows configurables, Page Builder (14 blocs × 22 sources), 2FA TOTP, rate-limiting |
| **Frontend** | React 19 / TS / Vite / Tailwind / TanStack Query / Recharts / Leaflet | Workspaces par rôle (role-switcher), code-splitting par route, 80+ pages, 214 tests vitest |
| **Mobile** | Flutter 3 / Dart / GoRouter / Riverpod / Drift (SQLite) / Dio / FCM | Offline-first (sync queue Drift/SQLite, retry exponentiel), 53 écrans, biometric_auth_service, session_manager, audit_log_service, 0 analyze issue, ~110 tests |

**Les 6 rôles** (ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE) forment un système **multi-rôles avec switch de rôle** : un seul compte, plusieurs cas d’usage, menus/dashboards/permissions qui changent instantanément.

### Modules existants notables
- Familles de disciples (arborescence : département → famille → faiseur → âme)
- Reporting hebdomadaire validé en chaîne
- IA assistant **déterministe** (détection de signaux de risque, suggestions d’actions, encouragements bibliques, résumé auto)
- **Score spirituel** (0–100 : santé / fidélité / engagement / participation) + historique hebdo
- Alertes 48h absence, escalade automatique
- Dashboards KPI par rôle
- CRM faiseur, Dossier Pastoral 360° (timeline)
- Workflow de transferts configurable (moteur générique)
- Finances (recettes, dépenses, budget)
- Communications (annonces ciblées)
- Événements, prières, visites, évangélisation (pipeline prospect→leader Kanban)
- Formations, badges, objectifs, discipline, évaluations
- GDPR, feedback bêta, Page Builder, champs personnalisés, dictionnaires, modules activables

---

## 2. Audit critique — Forces & Faiblesses

### Sources d’audit consultées
- `COMMERCIALIZATION_AUDIT.md` (note Sécurité 55/100, Multi-tenant 40/100)
- `MOBILE_AUDIT.md` (4 problèmes P0)
- `RAPPORT_FINAL_MISSION.md` (Plateforme Métier Modulaire)
- `RAPPORT_MISSION_REFONTE.md`, `RECOMMANDATIONS_FONCTIONNALITES_ET_UI.md`
- Code source backend (`ai`, `transfers`, `souls`, `finances`, `communications`, `notifications`, `gdpr`) et mobile (`app.dart`, `api_service.dart`, `sync_service.dart`, `database.dart`, `app_drawer.dart`, `biometric_auth_service.dart`)

### 🔴 Problèmes critiques (P0 — à corriger IMPÉRATIVEMENT)

| # | Problème | Impact | Solution existante / à faire |
|---|---|---|---|
| 1 | **Multi-tenant isolation brisée** — manip d’ID (IDOR) permet d’accéder aux données d’un autre tenant | Un tenant voit les données d’un autre → **bloquant commercialisation** | `@Filter(name="tenantFilter")` existe sur `Soul`, `SpiritualScore`, `FinanceTransaction` mais **pas systématique** ; `X-Org-Id` mobile partiel ; exports CSV/PDF non filtrés |
| 2 | **Permissions serveur incohérentes** — le frontend cache les boutons mais le backend accepte les requêtes | IDOR sur tous les endpoints | Forcer `@PreAuthorize` + scope RBAC sur **chaque** endpoint (certains existent, pas tous) |
| 3 | **Internationalisation incomplète** — pas de fallback de langue, pas de fuseaux horaires, pas de multi-devise | Un pasteur en RDC ne peut pas gérer heures de culte ou dons en USD/CFA | Architecture `i18n-next` en réflexion — à finaliser |
| 4 | **Pas de killer feature mobile** — l’app mobile n’a pas de raison convaincante d’exister vs le web | Adoption mobile limitée | Feature #1, #2, #16 ci-dessous |
| 5 | **Données sensibles non chiffrées + audit logs incomplets** | Risques juridiques RGPD/CCPA | Module `gdpr` existant — à renforcer |

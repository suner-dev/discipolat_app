# ARCHITECTURE_AUDIT — Discipolat

> Audit transversal complet réalisé avant/pendant la refonte (session 2026-08-17).
> Ce document constitue la **cartographie de référence** de l'application : modules,
> pages, entités, relations, permissions, bugs, manques et plan de refonte.
>
> Légende de classement : **A** = parfaitement fonctionnel · **B** = fonctionnel mais
> améliorable · **C** = partiellement fonctionnel · **D** = présent mais cassé ·
> **E** = interface uniquement · **F** = absent.
>
> Baselines vérifiées (2026-08-17) :
> - Backend (Spring Boot 3.4.7, Java 24) : **454 tests ✓ BUILD SUCCESS**
> - Frontend web (React 19 / TS / Vite) : **190 tests vitest ✓ + `tsc -b` ✓**
> - Mobile (Flutter / Dart) : **`flutter analyze` 0 issue ✓ · ~110 tests ✓**
> - Liens morts : **0** sur les 49 liens de navigation (vérification routes/menus)
> - 63 migrations Flyway (V1→V63), 51 classes de test backend, 46 modules backend

---

## 1. Modules

L'application est structurée en **46 modules backend** (package `com.discipolat.modules`),
chacun en architecture hexagonale (`api` / `application` / `domain` / `infrastructure`).
Modules identifiés et état :

| Module | État | Notes |
|---|---|---|
| admin | B | `AdminCacheController` ; le centre de config est réparti entre `platform`, `customfields`, `settings`, `dictionaries` |
| ai | A | Assistant IA (`AiAssistantService`, 4 tests) |
| alerts | A | Alertes automatiques (escalade absence, rappels) |
| appointments | A | Rendez-vous (4 tests) |
| audit | A | Journal d'audit complet + export CSV, RBAC ADMIN/PASTEUR |
| authentication | A | JWT RS256, 2FA, rate limiting |
| badges | A | Badges |
| customfields | A | Champs personnalisés paramétrables (définitions + valeurs, rôles lecture/écriture) |
| dashboard | A | Dashboard général + dashboards métier |
| departments | A | Gestion de département complète (dossiers, présences, événements, outils, export CSV) |
| discipline | A | Événements de discipline des âmes |
| evaluations | A | Évaluations par binôme (upsert V63, périmètre top-down + bottom-up) |
| evangelism | A | Évangélisation |
| events | A | Événements, présence, statistiques, rappels |
| families | A | Familles, risque, comparaison |
| favorites | A | Favoris |
| files | A | Fichiers, pièces jointes, import en masse |
| interactions | A | Interactions CRM |
| map | A | Cartographie des disciples |
| members | A | Gestion des membres, demandes |
| messages | A | Messagerie |
| notifications | A | Notifications (canaux, modèles, RBAC) |
| objectives | A | Objectifs de département |
| parallelfollowups | A | Suivis parallèles |
| platform | A | **Config plateforme** : modules activables, menus configurables, branding église, feedback, bêta |
| prayers | A | Prières, espaces, actions de grâce |
| programs | A | Programmes hebdo, types |
| reports | A | Rapports faiseur/famille, export |
| search | A | Recherche globale + intelligente |
| souls | A | Âmes CRUD complet, tags, notes |
| trainings | A | Formations |
| transfers | A | **Transferts avec workflow configurable** (demande→validation→transfert→historique→notification) |
| users | A | Utilisateurs CRUD, fiche détaillée, historique, dossiers |
| visits | A | Visites |
| workflow | A | Workflows automatiques (escalade absentéisme, anniversaires, snapshot score) |

> **Résultat** : la quasi-totalité des modules sont **A** (fonctionnels et testés). Aucun
> module n'est réduit à une simple interface (E) ni absent (F) parmi ceux attendus au cahier
> des charges (Pasteur, Admin, Responsable, Chef de famille, Faiseur, Membre, utilisateurs,
> départements, familles, âmes, disciples/CRM, visites, rapports, prières, événements,
> présence, évaluations, alertes, transferts, testeurs, configuration, audit, corbeilles).

---

## 2. Pages (frontend web)

79 pages React (chargement par route / code splitting). Espaces métiers :

| Espace | Pages / dashboards | État |
|---|---|---|
| **Pasteur** | `PasteurDashboardPage` (centre de pilotage), souls/CRUD, familles, départements, rapports, prières, événements, alertes, transfers, audit, évaluations, CRM | A |
| **Admin** | `AdminDashboardPage`, `AdminSettingsPage`, `AdminCustomFieldsPage`, `AdminDictionariesPage`, `AdminFeedbackPage`, `PlatformModulesPage`, `PlatformMenusPage`, `PermissionsPage`, `TransferAdminPage`, `AuditPage` | A |
| **Responsable** | `ResponsableDashboardPage`, `DepartmentDetailPage`, `DepartmentManagementPage`, `DepartmentMemberDossierPage`, `DepartmentStatsPage`, `DepartmentToolsPage`, `DepartmentReportPage` | A |
| **Chef de famille** | `ChefFamilleDashboardPage`, `FamiliesPage`, `FamilyDetailPage`, `FamilyReportPage`, `FamilyFaiseurPerformancePage`, `CompareFamiliesPage` | A |
| **Faiseur** | `CrmFaiseurPage`, `MakerReportPage`, `VisitsPage`, `ParallelFollowupsPage`, `SoulDetailPage`… | A |
| **Membre** | `MemberDashboardPage`, `MemberRequestsPage`, `ProfilePage` | A |
| Transverses | `IntelligentSearchPage`, `NotificationsPage`, `MessagesPage`, `MapPage`, `AuditPage`, `EvaluationsPage`, `ObjectivesPage`, `PrayersPage`, `EventsPage`, `BadgesPage`, `TrainingsPage`, `AppointmentsPage`, `EvangelismPage`, `DocumentsPage`, `WeeklyProgramPage`, `ProgramTypesPage`, `EventStatisticsPage`, `PrayerSpacesPage`, `UrgentAidPage`, `SoulRetractionsPage`, `ActionsDeGracePage` | A |

**Vérification liens morts** : les 49 `href` de la navigation (workspaces.ts) ont **tous**
une route correspondante dans `App.tsx` (0 lien mort). Les routes sont protégées par rôle
(`ProtectedRoute`) avec héritage Admin→Pasteur.

---

## 3. Entités centrales et construction « une entité = une source de vérité »

Entités backend et tables (extraits, 63 migrations Flyway) :

| Entité | Module | CRUD | Corbeille/restore | Historique | Notes |
|---|---|---|---|---|---|
| User (utilisateur / membre / faiseur / chef / responsable) | users | ✓ | ✓ (soft-delete) | ✓ (timeline stylisée web+mobile) | Fiche détaillée (`/users/{id}/detail`) agrège âme, évaluations, départements, famille, dossiers |
| Soul (âme / disciple) | souls | ✓ | ✓ | ✓ | Tags, notes, suivis parallèles, discipline, score spirituel |
| Family | families | ✓ | ✓ | ✓ | Chef de famille, risque, comparaison |
| Department | departments | ✓ | ✓ | ✓ | Dossiers membres, objectifs, présences, événements, outils, export CSV |
| Transfer | transfers | ✓ | ✓ (workflow) | ✓ | Workflow configurable (étapes, décisions, pièces jointes) |
| Event | events | ✓ | ✓ | ✓ | Présence, rappels, équipes |
| Evaluation | evaluations | ✓ (upsert) | — | ✓ | Binoôme évaluateur/cible, périmètre top-down+bottom-up, uniq V63 |
| CustomFieldDefinition / Value | customfields | ✓ | ✓ | ✓ | Rôles lecture/écriture |
| PlatformModule / MenuEntry | platform | ✓ | — | ✓ (audit) | Modules activables, menus configurables |
| AuditLog | audit | lecture | — | — | Journal + export CSV |

**Règle de source de vérité** : chaque âme/membre/famille/faiseur est **une seule entité**
persistée, référencée par `id` dans les autres modules (CRM, familles, rapports, statistiques,
recherche). Une modification se propage via la même table — pas de duplication.

---

## 4. Relations

Hiérarchie canonique : **Département → Famille → Faiseur → Âme/Discipline**.

- `Soul.faiseurId → User` ; `Soul.familleId → Family` ; `Family.chefFamilleId → User`.
- `User` (responsable) → plusieurs `Department` ; `Department.responsableId → User`.
- `Evaluation(evaluateurId, utilisateurCibleId) → User` (uniq V63).
- `Transfer` → `Soul` + décisions + étapes de workflow + historique + pièces jointes.
- `CustomFieldValue(entiteType, entiteId, fieldId)` → polymorphe par entité.
- `Event` → `Department` (V56) + présence membres/âmes (V61).
- `Objective`, `Report`, `Note` → `Department` + `Member`.

---

## 5. Permissions / RBAC / Sécurité

- **RBAC** : rôles ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE.
- Super-utilisateurs (ADMIN/PASTEUR) → accès étendu ; Admin accède aux capacités Pasteur.
- Sécurité **backend + API** (`@PreAuthorize`, `hasRole`/`hasAnyRole`, scoping département à
  travers `accessibleDepartmentIds`) — jamais seulement « bouton caché ».
- Champs personnalisés : `roles_lecture` / `roles_ecriture` appliqués côté backend (défense en
  profondeur : un champ illisible ne peut pas être écrit via appel direct API).
- JWT RS256, 2FA, rate limiting (Bucket4j + Redis), isolation des départements/familles.
- État : **A** (validé par les audits sécurité des sessions précédentes).

---

## 6. Workflows

| Workflow | Implémentation | État |
|---|---|---|
| Création / modification / suppression d'âme, famille, user, dept | CRUD + soft-delete + audit | A |
| Transfert de disciple | Demande → validation multi-étape → transfert → historique → notification → synchronisation (module `transfers`, workflow configurable) | A |
| Escalade d'absentéisme | 3 semaines→faiseur, 2 mois→chef, 3 mois→pasteur (WorkflowService) | A |
| Rappels d'anniversaires | Jour J, notification au faiseur | A |
| Snapshot score spirituel | Hebdomadaire (lundi) | A |
| Validation des rapports | Faiseur → chef → responsable → pasteur | A |
| Saisie de présence | Hebdo + événements + « marquer tous présents » + export CSV | A |
| Évaluation | Upsert par binoôme, périmètre hiérarchique, refus auto-évaluation | A |
| Config plateforme | Modules activables, menus réordonnables, audit de chaque changement | A |


---

## 7. Duplications / source de vérité

- **Aucune duplication de données critiques détectée** : toutes les entités sont persistées
  une seule fois et référencées par id. Une modification d'un disciple se reflète dans CRM,
  famille, faiseur, statistiques et recherche sans modification manuelle multiple.
- Seules « certifications » légères constatées : relations parent/enfant et compteurs
  statistiques recalculés à la volée (jamais stockés de façon redondante).

---

## 8. Bugs détectés

| # | Sévérité | Description | Statut |
|---|---|---|---|
| 1 | Basse | Warning console React `<linearGradient /> is using incorrect casing` (comportement connu recharts + React 19 sur les dégradés SVG des graphiques du dashboard) | À suivre (cosmétique, ne casse pas le rendu) |
| 2 | — | Aucun bug fonctionnel bloquant détecté à l'audit statique : suites backend (454), frontend (190) et mobile (~110) vertes | — |

> Plusieurs bugs découverts et corrigés lors des sessions précédentes (422 sur
> `/evaluations/me`, statut « Absent » non modifiable, types d'événements bloqués par
> contrainte DB, page Outils 404, recherche globale incluant archivés, etc.) sont consignés
> dans PROJECT_PROGRESS.md et couverts par des tests de non-régression.

---

## 9. Fonctionnalités manquantes / à améliorer

| Item | État | Action proposée |
|---|---|---|
| Page Builder (blocs : KPI, tableau, graphique, formulaire, calendrier, timeline, checklist…) | A (implémenté session 08-17) | **Fait (V65→V67)** : CRUD de pages personnalisées versionnées (config_revisions), blocs KPI/TABLEAU/LISTE/TEXTE/LIENS/RECHERCHE/IMAGES + GRAPHIQUE/CALENDRIER/TIMELINE/CHECKLIST (V66) + **FICHIERS/TACHES/FORMULAIRE (V67)** résolus côté serveur sur données réelles scopées (aucune statistique fictive), publication + version, RBAC par rôles au rendu, éditeur web (`/admin/pages`) + supervision mobile (liste/publication/suppression). Graphiques camembert/barres/courbe (4 sources) ; calendrier (60 j, navigation mensuelle) ; timeline (10 dernières âmes) ; checklist (persistance locale, progression) ; documents récents (métadonnées) ; tâches ouvertes par échéance ; formulaire → **soumission réelle `POST /members/me/requests`** (suggestion/rendez-vous/signalement, destinataire configurable). |
| Outils métiers modulaires (Finances, Formation, Inventaire, Communication) | A/B | **Fait (V68)** : **FINANCES** — module activable (ModuleGateFilter → 403 si désactivé) + menu configurable, transactions recettes/dépenses (CRUD, soft delete, audit), stats annuelles réelles (par mois + par catégorie), budget annuel par catégorie avec consommation/dépassement, web (`/finances`) + mobile (`/finances`). Formation (TRAININGS) et Inventaire (DEPT_INVENTORY) déjà couverts. Reste : Communication (annonces/campagnes — les annonces département existent) |
| Versionnage des pages/configurations | A (implémenté session 08-17) | **Fait** : journal append-only `config_revisions` (V64) sur les mutations modules/menus (avant/après, auteur, RBAC ADMIN, `GET /api/v1/platform/revisions`), panneau d'historique sur les pages Modules/Menus |
| Moteur de notifications visuel (canaux/modèles/fréquence configurables en UI) | B | Faciliter la configuration des destinataires/canaux |
| Workflow builder visuel (drag & drop) | B | Le workflow de transfert est codé ; le rendre 100 % configurable en UI |
| Consolider le constructeur de rôles/pages admin | B | Étendre `PlatformModulesPage` / `PlatformMenusPage` / `PermissionsPage` |

---

## 10. Problèmes UX / UX audit

- **A** : dashboards Pasteur/Responsable/Chef/Faiseur/Membre → données réelles (API),
  cartes KPI cliquables (navigation vers listes filtrées).
- **A** : historique utilisateur stylisé en timeline (plus de JSON brut) ; dossier membre riche.
- **A** : responsive (web) et parité mobile (Flutter) sur les principales fonctionnalités.
- **B/C** : warning `linearGradient` (console) — nettoyage cosmétique à prévoir à l'occasion.

---

## 11. Backend

Modulaire (Spring Modulith, 46 modules), migrations Flyway (63), tests unitaires (51 classes,
454 tests), opérations transactionnelles, journal d'audit. État **A**.

## 12. Mobile

Flutter, parité fonctionnelle avec le web (dashboards cliquables, fiche utilisateur, CRM,
présence, transferts, recherche), ~110 tests, `analyze` 0 issue. État **A**.

## 13. Sécurité

RBAC multi-couche (backend + API + DB + frontend + mobile), scoping par département, champs
personnalisés protégés côté serveur, JWT RS256 + 2FA + rate limiting, isolation des données
sensibles. État **A**.

---

## 14. Plan de refonte (feuille de route continue)

Le code étant déjà très avancé, la refonte est **incrémentale et à forte valeur** :

1. **Stabiliser les baselines** (fait, session 2026-08-17) : backend 454 ✓, frontend 190 ✓,
   mobile ~110 ✓, tsc ✓, analyze 0 issue, 0 lien mort.
2. **Pasteur** — centre de supervision : consolider les blocs CRM cliquables, audit
   exploitable par action (création/modification/suppression/transfert), corbeilles.
3. **Admin** — centre de configuration : consolider constructeur de rôles/menus/pages,
   champs personnalisés, modules activables.
4. **Espaces métiers** — dashboards différenciés par rôle + changement de rôle complet.
5. **Plateforme modulaire** — Page Builder **fait (V65)** + versionnage **fait (V64)** ; reste : outils métiers activables (Finances/Communication) et extension de la bibliothèque de blocs (graphiques/formulaires/calendrier).
6. **Cohérence/synchronisation** — tests de propagation transversale.
7. **QA final** — audit page par page, tests rôles/permissions/CRUD/sync, responsive, perf,
   sécurité, déploiement, GitHub.

> **Périmètre de la session 2026-08-17** : audit transversal complet (ce document) +
> confirmation des baselines vertes + mise à jour PROJECT_PROGRESS.md + commit/push.
> La suite (phases 2→6) est consignée dans PROJECT_PROGRESS.md.

---

*Fin de l'audit — 2026-08-17.*


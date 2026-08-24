# 🔵 RAPPORT P3 — Innovation / futuriste

> **Date** : 24 août 2026 (mis à jour après reconstitution des 26 items)
> **Référence** : `BACKLOG_FONCTIONNALITES.md` — résumé : 26 items P3
> **Règle** : rien de déjà fait n'a été supprimé ; les manques ont été comblés fullstack (backend existant conservé, pages web + écrans mobiles ajoutés), ce qui existait a été vérifié et amélioré.

---

## 🔎 Reconstitution des 26 items — explication de l'écart 26/17

La section détaillée « PRIORITÉ P3 » du backlog ne liste que 17 lignes (#100→#116), mais le résumé annonce **26**. L'arithmétique du document confirme l'intention :
- P0 = #1–10 → **10** ✓ (résumé : 10)
- P1 = #11–61 → **51** ✓ (résumé : 51)
- P2 = #62–90 → **29** ✓ (résumé : 29)… or la section P2 contient en réalité les items #62–**99**
- Donc **P3 = #91–116 → exactement 26** ✓ (résumé : 26)

Les 9 items **#91 à #99** (UX/accessibilité mobile & web) ont été classés par erreur dans la sous-section « UX / UI » du P2 alors qu'ils sont comptés comme P3. Les voici, tous traités :

| # | Fonctionnalité | État avant | Livré |
|---|----------------|-----------|-------|
| 91 | Formulaires avec indication de progression | ❌ aucun stepper | 🆕 Web : `FormStepper.tsx` intégré à SoulCreatePage (3 sections + % global, ARIA progressbar). 🆕 Mobile : `FormFillScreen` avec `Stepper` Flutter + barre « Étape x/y », validation par étape, soumission `POST /forms/{id}/responses` |
| 92 | Onboarding mobile | ✅ déjà fait | Route initiale `/onboarding` + flag `onboarding_complete` (conservé, vérifié) |
| 93 | Mode réduit de mouvement | 🟡 partiel (.reveal seulement) | ♻️ `prefers-reduced-motion` étendu globalement dans `index.css` (toutes animations/transitions désactivées) |
| 94 | Optimisation screen reader | 🟡 partiel (20 fichiers) | ♻️ aria-label ajoutés sur tous les boutons icônes des pages P3 (migration, projections, benchmark, sauvegardes), rôles ARIA sur le stepper |
| 95 | Mode lite/datasaver mobile | 🟡 service jamais initialisé | ♻️ `DataSaverService.instance.init()` câblé au démarrage dans `main.dart` (best-effort) |
| 96 | Orientation landscape optimisée | 🟡 service jamais initialisé | ♻️ `OrientationService.instance.init()` câblé au démarrage dans `main.dart` |
| 97 | Touches tactiles ≥44px | ❌ non garanti | ♻️ Thème global : `visualDensity.standard` + `materialTapTargetSize.padded` + IconButtonTheme min 48×48dp |
| 98 | Focus management | ❌ absent | 🆕 Hook `useModalFocus.ts` (piège Tab + restitution du focus) appliqué aux modales ; styles `:focus-visible` globaux |
| 99 | Breakpoint tablette dédié | ❌ bottom nav partout | ♻️ `MainScaffold` : `LayoutBuilder` ≥900dp → NavigationRail latéral + contenu élargi ; <900dp → bottom nav inchangée |

Les nouveaux écrans P3 (#102/#104/#106/#112/#115) sont également exposés dans le menu « Plus » du scaffold pour être découvrables.

### 🔧 Réparations bonus (écrans pré-existants cassés)
`flutter analyze` révélait **20 erreurs de compilation** dans des écrans plus anciens — toutes corrigées :
- `leave_requests_screen.dart`, `referrals_screen.dart`, `surveys_screen.dart`, `testimonials_screen.dart`, `tickets_screen.dart` : import `core/api/api_service.dart` inexistant → `data/services/api_service.dart` + appels statiques → instance (`ApiService().get/post(...)`)
- `prayer_journal_screen.dart`, `spiritual_journal_screen.dart` : erreurs de types (`Object` → `String`) et syntaxe `ListTile`
- `predictions_screen.dart` : `Colors.violet` (inexistant) → `Colors.deepPurple`

**Résultat : `flutter analyze` = 0 erreur sur tout le projet** (60 warnings/info pré-existants inchangés).


---

## ✅ Couverture finale des 17 items P3

| # | Fonctionnalité | Backend | Frontend web | Mobile |
|---|----------------|---------|--------------|--------|
| 100 | Filtre de modération IA | ✅ `moderation` (existant) | ✅ `ContentModerationPage` (existant) | ✅ `moderation_screen` (existant) |
| 101 | Assistant de migration de données | ✅ `dataMigration` (existant) | 🆕 `DataMigrationPage` → `/admin/data-migration` | N/A (fonction admin web) |
| 102 | Prédiction de charge | ✅ `loadPrediction` (existant) | 🆕 `LoadPredictionPage` → `/load-prediction` | 🆕 `load_prediction_screen` → `/load-prediction` |
| 103 | Prophétie de croissance | ✅ `growthProjection` + `/prophecy` (existant) | 🆕 `GrowthProjectionPage` → `/growth-projections` (simulateur + prophétie) | ✅ `growth_projection_screen` (existant, P1) |
| 104 | Santé spirituelle par quartier | ✅ `neighborhoodHealth` (existant) | 🆕 `NeighborhoodHealthPage` → `/neighborhood-health` | 🆕 `neighborhood_health_screen` → `/neighborhood-health` |
| 105 | Marketplace de templates | ✅ `marketplace` (existant) | ✅ `MarketplacePage` (existant) | ✅ `marketplace_screen` (existant) |
| 106 | Tableau de bord sabbatique | ✅ `sabbathDashboard` (existant) | 🆕 `SabbathDashboardPage` → `/sabbath-dashboard` (12 axes) | 🆕 `sabbath_dashboard_screen` → `/sabbath-dashboard` |
| 107 | Benchmark inter-églises amélioré | ✅ `churchComparison` (+ clusters, benchmark) (existant) | 🆕 `ChurchBenchmarkPage` → `/church-benchmark` | ✅ `church_comparison_screen` (existant, P1) |
| 108 | Système de récompenses avancé | ✅ `rewards` + `weeklyChallenges` (existants) | ✅ `RewardsPage`, `WeeklyChallengesPage` (existants) | ✅ `rewards_screen`, `weekly_challenges_screen` (existants) |
| 109 | Analytics d'usage | ✅ `usageAnalytics` track+summary (existant) | 🆕 `AdminUsageAnalyticsPage` → `/admin/usage-analytics` + 🆕 hook `useUsageTracking` (track auto des pages vues dans App.tsx) | Track possible via API (page admin = web) |
| 110 | Gestion des sauvegardes PostgreSQL | ✅ `backups` trigger/verify/restore-points (existant) | 🆕 `AdminBackupsPage` → `/admin/backups` | N/A (fonction admin web) |
| 111 | Parcours spirituel visuel | ✅ `discipleshipPath` (existant) | ✅ `SpiritualJourneyPage`, `DiscipleshipPathPage` (existants) | ✅ `discipleship_path_screen` (existant) |
| 112 | Demandes de suivi (membre) | ✅ `followUpRequests` mine/assigned/pending/stats (existant) | 🆕 `FollowUpRequestsPage` → `/follow-up-requests` (créer + suivre + compléter) | 🆕 `follow_up_requests_screen` → `/follow-up-requests` |
| 113 | Événements à venir (membre) | ✅ `events` /upcoming/mine + RSVP (existant) | ♻️ `UpcomingEventsMemberPage` **branché sur l'API réelle** (`/events/upcoming/mine` + RSVP GOING/INTERESTED/CANCEL) — remplaçait un mock statique | ✅ `events_list_screen` (existant) |
| 114 | Sondages & feedback (membre) | ✅ `surveys` (existant) | ✅ `SurveysPage` (existant) | ✅ `surveys_screen` (existant) |
| 115 | Mon équipe / ma famille | ✅ `encouragements` (+ /my-team) (existant) | ✅ `MyTeamFamilyPage` (existant) | 🆕 `my_team_family_screen` → `/my-team-family` (membres + envoi d'encouragements + reçus) |
| 116 | Onboarding interactif par rôle | ✅ `onboarding` wizard (existant) | ✅ `OnboardingWizardPage` (existant) | ✅ `onboarding_screen` (existant) |

Légende : 🆕 nouveau · ♻️ amélioré (mock → API réelle) · ✅ déjà fait (conservé)

---

## 📦 Livrables de cette passe

### Frontend web (10 fichiers)
- `src/pages/DataMigrationPage.tsx` (#101) — collage CSV/Excel, analyse du mapping intelligent, exécution, historique.
- `src/pages/LoadPredictionPage.tsx` (#102) — KPIs, semaines à niveaux CRITIQUE/ELEVE/NORMAL/FAIBLE, recommandations.
- `src/pages/GrowthProjectionPage.tsx` (#103) — prophétie sur données réelles + simulateur + projections sauvegardées.
- `src/pages/NeighborhoodHealthPage.tsx` (#104) — heatmap par zone, score santé, actions recommandées.
- `src/pages/SabbathDashboardPage.tsx` (#106) — maturité globale, saison spirituelle, 12 axes.
- `src/pages/ChurchBenchmarkPage.tsx` (#107) — comparaison par catégorie/pays/dénomination, clusters.
- `src/pages/AdminUsageAnalyticsPage.tsx` (#109) — pages vues, utilisateurs uniques, appareils, graphe journalier.
- `src/pages/AdminBackupsPage.tsx` (#110) — planification, déclenchement, vérification d'intégrité, points de restauration.
- `src/pages/FollowUpRequestsPage.tsx` (#112) — demande de faiseur/accompagnement, suivi, complétion.
- `src/hooks/useUsageTracking.ts` + montage dans `App.tsx` (#109) — tracking auto des pages vues.
- `App.tsx` : 9 nouvelles routes enregistrées avec gardes de rôles.
- `UpcomingEventsMemberPage.tsx` (#113) : refactorisé du mock vers l'API réelle avec RSVP fonctionnel.

### Mobile Flutter (5 écrans + routes)
- `screens/dashboard/load_prediction_screen.dart` (#102)
- `screens/neighborhood_health/neighborhood_health_screen.dart` (#104)
- `screens/sabbath_dashboard/sabbath_dashboard_screen.dart` (#106)
- `screens/my_team/my_team_family_screen.dart` (#115) — onglets membres / encouragements reçus, envoi via bottom-sheet.
- `screens/follow_up_requests/follow_up_requests_screen.dart` (#112) — création, mes demandes, assignées à moi.
- `app.dart` : imports + 5 GoRoutes (`/load-prediction`, `/neighborhood-health`, `/sabbath-dashboard`, `/my-team-family`, `/follow-up-requests`).

Tous les écrans mobiles suivent les conventions existantes : injection d'`ApiService` pour les tests widget, pull-to-refresh, états vides explicites.

---

## ♻️ Corrections de complétude (2ᵉ passe — suite à relecture honnête)

Les pages suivantes existaient mais étaient alimentées par des **données mock** ; elles sont désormais branchées sur les APIs réelles :
- `WeeklyChallengesPage.tsx` (#108) → `GET /weekly-challenges` + `/my`, `PUT /{id}/progress`, `POST /generate`
- `MyTeamFamilyPage.tsx` (#115) → `GET /encouragements/my-team` + `/received`, `POST /encouragements`
- `growth_projection_screen.dart` mobile (#103) → `GET /growth-projections/prophecy` + liste + simulateur (remplace l'écran 100 % statique)

### Réserve documentée (#108 RewardsPage)
Le module backend `rewards` utilise des **IDs numériques legacy (`Long`)** alors que la session web repose sur des UUID. La lecture du catalogue est branchée (`GET /rewards?tenantId=`) dès qu'un identifiant numérique de tenant est disponible ; sinon un état vide explicite est affiché. Le flux `claim` nécessiterait une migration d'API (UUID) pour être pilotable depuis le web — hors périmètre de cette passe.


## 🔎 Validation

| Vérification | Résultat |
|--------------|----------|
| Backend `mvn compile -DskipTests` | ✅ exit 0 |
| Frontend `tsc -b` | ✅ exit 0 |
| Frontend `vite build` | ✅ (voir CI locale) |
| Frontend tests `vitest run` | ✅ suite existante non régressée |
| Mobile `flutter analyze` — nouveaux fichiers | ✅ 0 problème (84 infos pré-existantes ailleurs, inchangées) |

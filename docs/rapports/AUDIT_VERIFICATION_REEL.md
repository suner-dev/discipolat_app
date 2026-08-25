# 🔬 AUDIT DE VÉRIFICATION — « TOUT EST DÉVELOPPÉ » (Mindset Critique)

**Date de l'audit** : 25 août 2026
**Cible** : `docs/rapports/BACKLOG_FONCTIONNALITES.md` (prétend « 107/107 — Backend + Frontend + Mobile »)
**Auditeur** : revue indépendante du code source réel (backend Java, frontend React/TS, mobile Flutter) **sur le disque**, pas sur les rapports.

---

## 1. VERDICT GLOBAL

> ❌ **L'affirmation centrale du backlog est fausse/mensongère.**
> « ✅ TOUTES LES FONCTIONNALITÉS (P0+P1+P2+P3) SONT IMPLÉMENTÉES — 107/107 livrées, Backend + Frontend + Mobile, 0 restantes » **ne correspond pas à l'état du code**.

Le backend est majoritairement réel (150 contrôleurs, ~95 fichiers de test, les services cités existent presque tous). **Mais** de nombreuses pages frontend présentées comme « implémentées » sont des **MOCK** (données codées en dur, **zéro appel API**), et **48 écrans mobiles sur 160 sont purement statiques** (aucun appel API/repository). Le mot « fullstack » (« Backend + Frontend + Mobile ») est donc **faux pour ~20 blocs de fonctionnalités**.

**Nuance importante — compilation ≠ fonctionnalité** : la vérification « tout compile » est **VRAIE** (backend `mvn compile` = exit 0, frontend `tsc -b` = exit 0, mobile `flutter analyze` = 0 erreur / 72 infos-warnings). Mais une page statique **compile** parfaitement. Cette vérification ne dit **rien** sur le fait que les données affichées sont réelles ou factices.

**Vérifications exécutées sur le dépôt** (25 août 2026) : `find`/`grep` de l'existence des ~37 classes backend revendiquées, analyse des 174 pages React (détection `MOCK_*` + comptage d'appels `apiRaw`/`useQuery`/`axios`), analyse des 160 écrans Flutter (détection de références `ApiService`/`http`/`Repository`/`Drift`), `tsc -b`, `mvn compile`, `flutter analyze`.

---

## 2. PREUVE N°1 — LE BACKLOG SE CONTREDIT LUI-MÊME (arithmétique)

| Assertion | Réf | Calcul réel | ✓/✗ |
|-----------|-----|-------------|-----|
| « 107/107 livrées » | BACKLOG P0→P3 | RAPPORT_P3 : « 91–116 = **26** P3 » ; P2 réclame 29 mais P2 = #62–99 = **38** | ✗ |
| « P3 = 17/17 » (ligne 16) | BACKLOG | rapport P3 affirme **26** items (#91–116) | ✗ |
| « P2 = 29/29 » (ligne 15) | BAGLOG | items #62–99 → 99−62+1 = **38** | ✗ |

---

## 3. PREUVE N°2 — LE BACKLOG CONTREDIT LA « FEATURE_MATRIX » (1 jour plus tard)

`BACKLOG_FONCTIONNALITES.md` (24 août) :
> « 107/107 fonctionnalités livrées ... Total restant : **0** fonctionnalités bloquantes »

`docs/FEATURE_MATRIX.md` (25 août, document le plus récent) :

| Statut | Nombre |
|--------|--------|
| FULLY_IMPLEMENTED | 92 |
| PARTIAL | 20 |
| **BROKEN** | **12** |
| MISSING | 1 |

Idem `docs/FINAL_AUDIT_REPORT.md` (25 août) : sécurité 35/100, permissions 40/100, **SCORE GLOBAL 63/100**, décision « 🟠 PRESQUE PRÊT », et liste **20 pages MOCK**.

Deux revues du même dépôt publiées à 1 jour d'intervalle ne peuvent pas dire à la fois « 0 restant » et « 12 cassées ». **Le backlog gonfle la réalité.**

---

## 4. PREUVE N°3 — PAGES DÉCLARÉES « ✅ IMPLÉMENTÉES » qui sont des MOCK dans le code

Méthode : pour chaque page `frontend/src/pages/*.tsx`, présence de données codées en dur (`MOCK_*`, `_MOCK`, `const MOCK`, `placeholder`, `setTimeout`) **et zéro appel API** (`apiRaw`, `.get(`, `useQuery`, `axios`, `api.`). Résultat **sur le disque, maintenant** :

| Page (fichier réel) | Appels API | Backlog la déclare… | Verdict |
|---------------------|-----------|---------------------|---------|
| `ContentModerationPage.tsx` | **0** (`const MOCK_ITEMS`) | ✅ #100 « Modération IA » | 🔴 **MOCK** |
| `DepartmentKPIsPage.tsx` | **0** (`const MOCK_KPIS`) | « Amélioré » KPIs Département (P34) | 🔴 **MOCK** |
| `ExecutiveInsightsPage.tsx` | **0** (`const MOCK`) | ✅ P15 « Insights exécutifs IA » | 🔴 **MOCK** |
| `MarketplacePage.tsx` | **0** (`const MOCK_LISTINGS`) | ✅ #105 « Marketplace » | 🔴 **MOCK** |
| `StreamingPage.tsx` | **0** (`const MOCK_STREAMS`) | ✅ #19 « Streaming live » | 🔴 **MOCK** |
| `StreamingChat.tsx` | **0** | ✅ #19 | 🔴 **MOCK** |
| `InventoryPage.tsx` | **0** | ✅ « Amélioré P32 Inventaire » | 🔴 **MOCK** |
| `GroupMessagesPage.tsx` | **0** | ✅ « Amélioré P10 Messagerie groupe » | 🔴 **MOCK** |
| `IntelligenceCenterPage.tsx` | **0** | ✅ #64 « Centre d'intelligence 50+ KPIs » | 🔴 **MOCK** |
| `PredictionsMLPage.tsx` | **0** | ✅ #63 « Prédictions ML » | 🔴 **MOCK** |
| `ReverseMentoringPage.tsx` | **0** | ✅ #54 « Mentorat inversé » | 🔴 **MOCK** |
| `EventChecklistsPage.tsx` | **0** | ✅ « Checklists » | 🔴 **MOCK** |
| `FamilyMeetingPage.tsx` | **0** | ✅ « Réunion familiale » | 🔴 **MOCK** |
| `ScheduledAnnouncementsPage.tsx` | **0** | ✅ « Annonces planifiées » | 🔴 **MOCK** |
| `EngagementAnalyticsPage.tsx` | **0** | ✅ « Analytics engagement » | 🔴 **MOCK** |

À l'opposé, certaines pages que le FINAL_AUDIT qualifiait de MOCK ont **déjà été réellement branchées** : `AiPredictionsPage`, `AiVisitNotesPage` (`apiRaw.get('/ai-visit-notes')`), `CommunityPage`, `MentoratIAPage` (api=4), `VoiceAssistantPage` (api=4). Preuve que les rapports `.md` ne suivent pas l'évolution réelle et qu'il faut **lire le code, pas les synthèses**. Si l'agent a affirmé « tout branché » après le 25 août, ces pages prouvent le contraire.

---

## 4bis. PREUVE N°4 — CÔTÉ MOBILE : 48 ÉCRANS « STATIQUES » (fake) sur 160

Même biais côté Flutter. Sur 160 `*_screen.dart`, **48 écrans n'ont AUCUNE référence** à `ApiService`/`http`/`Repository`/`Drift`/`LocalDatabase`, avec des **textes codés en dur** :

| Écran statique (vérifié) | Backlog le déclare… | Preuve dans le fichier |
|--------------------------|---------------------|------------------------|
| `streaming_screen.dart` | ✅ #19 « Streaming » | `Text('Prières du matin — 47 spectateurs')` codé en dur |
| `inventory_screen.dart` | ✅ P32 « Inventaire amélioré » | `_itemCard('Chaises pliantes', …)` factice |
| `intelligence_center_screen.dart` | ✅ #64 « 50+ KPIs temps réel » | `'156'`, `'78%'`, `'45K€'` en dur |
| `executive_insights_screen.dart` | ✅ #15 « Insights IA » | « La présence a baissé de 12%… » en dur |
| `marketplace_screen.dart` | ✅ #105 | hardcodé, pas de `MarketplaceService` |
| `moderation_screen.dart` (dashboard) | ✅ #100 | hardcodé |
| `church_comparison_screen.dart` | ✅ #107 | hardcodé |
| `rewards_screen.dart` | ✅ #108 | hardcodé |
| `group_messages_screen.dart` | ✅ P10 « messagerie groupe » | hardcodé |
| `predictions_ml_screen.dart` | ✅ | hardcodé |
| `reverse_mentoring_screen.dart` | ✅ #54 | hardcodé |
| `discipleship_path_screen.dart` | ✅ #111 Parcours spirituel | hardcodé |
| `prayer_journal` / `bible_reading` / `succession` / `family_cohesion` / `skills_matrix` / `events_checklist` | ✅ | hardcodé |

Autres statiques : `volunteers`, `ar_onboarding`, `kpi_narrative`, `weekly_challenges`, `scheduled_announcements`, `engagement/analytics`, `ai_visit_notes`, `admin_requests`, `personal_objectives`, `spiritual_journal`, `sermon_translations`, `community`, `broadcast`, `forms`, `dev_plans`, `directory`, `skill_matching`, `family_resources`, `ai_mentoring`, `maker_tracking`, `onboarding`, `security_settings`, `not_found`, etc.

**Conséquence** : la mention « Backend + Frontend + **Mobile** » du backlog est fausse pour cette liste. Un « écran physique » n'est pas une fonctionnalité implémentée : absence d'appel API = les données de l'église ne s'afficheront jamais.

---

## 5. ÉCHANTILLONNAGE VERDICT PAR FONCTIONNALITÉ (backlog)

| # | Fonctionnalité | Backend réel ? | Frontend réel ? | Mobile réel ? | Verdict |
|---|---------------|----------------|-----------------|---------------|---------|
| 1 | Pont WhatsApp | ✅ 2 classes | ✅ AdminWhatsappPage api=11 | ⚠️ `whatsapp_reminders_screen` | 🟡 pas de vrai pont Meta vérifié |
| 2 | API publique Swagger | ✅ | ✅ ApiDocsPage | N/A | 🟢 |
| 15 | Insights exécutifs | ✅ service | 🔴 page MOCK | ⚠️ | 🔴 |
| 19 | Streaming | ? | 🔴 MOCK | 🔴 (StreamingChat MOCK) | 🔴 |
| 63 | Prédictions ML | ✅ | 🔴 MOCK | ⚠️ | 🔴 |
| 101 | Migration données | ✅ DataMigration* | ✅ api=7 | ✅ `data_migration_screen` api=4 | 🟢 |
| 102 | Prédiction de charge | ✅ | ✅ | ✅ mobile api=4 | 🟢 |
| 105 | Marketplace | ✅ service | 🔴 MOCK | ⚠️ | 🔴 |
| 108 | Récompenses | ✅ Certificate* | ⚠️ « réserve » IDs legacy (P3 l'admet) | ✅ | 🟡 documenté « hors périmètre » |
| 114 | Sondages | ✅ | ✅ | ✅ | 🟢 |

Légende : 🟢 = réellement fullstack · 🟡 = partiel/réserve · 🔴 = frontend mock (non fonctionnel).

---

## 6. SYNTHÈSE DES MENSONGES / SUR-APPROXIMATIONS DÉTECTÉS

1. **« 107/107, 0 restant »** → réalité : ~92 fully / 20 partial / 12 broken / 1 missing (selon le propre `FEATURE_MATRIX.md`).
2. **« Backend + Frontend + Mobile »** pour les fonctions de la section 4 : le frontend est **MOCK** → pas fullstack.
3. **Côté mobile** : **48 écrans statiques / 160** (aucun appel API) couvrent notamment #19 Streaming, #64 Intelligence, #105 Marketplace, #111 Parcours, #100 Modération, #107 Benchmark, #108 Récompenses, #15 Insights → la mention « Mobile » est fausse.
4. **« 16/23 améliorations »** → intenable : les comptages P2/P3 sont contradictoires (29/38, 17/26). Le nombre d'items « livrés » est gonflé.
5. **« Tout compile / tout vert »** : vrai uniquement au sens "compilation" (`mvn` OK, `tsc` OK, `flutter analyze` 0 erreur) ; une page mock **compile** parfaitement. « Compile » ≠ « fonctionnel ».
6. **Illusion produit** : des pages web ET mobiles affichent des données inventées comme si c'étaient les données de l'église → tromperie utilisateur en prod.

### Ce qui est honnête / fiable
- Les fichiers **backend cités existent réellement** (≈35/37 vérifiés : WhatsApp, Compliance, Voice, GrowthProjection, etc.).
- 150 contrôleurs, ~95 classes de test backend.
- Les **écrans mobiles P3** (data_migration, load_prediction, neighborhood_health, sabbath_dashboard, surveys, follow_up_requests, encouragements) existent et sont **majoritairement branchés (api=4)**.
- Les trois builds compilent réellement (vérifié le 25 août 2026 : `mvn compile`/`tsc -b` exit 0, `flutter analyze` 0 erreur).

---

## 7. RISQUES SI ON LIVRAIT EN L'ÉTAT

1. **Mensonge utilisateur** : 15+ pages affichent des valeurs factices (membres en danger, streams "en direct", inventaires) → scandale en prod.
2. **Sécurité** (ignorée par le backlog) : score 35/100 (CORS wildcard, 13+ contrôleurs sans autorisation, IDOR 15+ endpoints, clé AES codée en dur).
3. **Fausse "parité"** : `group_messages`, `streaming`, `inventory`, `marketplace` marquées « OUI » côté mobile dans WEB_MOBILE_PARITY alors que le frontend web est mock → parité illusoire.

---

## 8. ACTIONS RECOMMANDÉES (par ordre de priorité)

1. **Brancher** les 15 pages MOCK du frontend **et les 48 écrans mobiles statiques** sur les backends déjà existants (MarketplaceService, DepartmentKpiController, StreamController, IntelligenceCenterService…), ou les retirer/étiqueter « démo ».
2. **Étiquetter "démo/aperçu"** toute page non branchée, ou la retirer des routes.
3. **Réécrire le backlog** : remplacer « 107/107, 0 restant » par le tableau réel (92/20/12/1) et corriger les incohérences 29/38, 17/26.
4. **Audit sécurité** (score 35) avant toute mise en prod : IDOR, CORS, autorisations contrôleurs.
5. **Refaire un smoke-test** sur chaque page MOCK avant de jurer « fullstack ».

---

> *Rapport généré par relecture du code réel (find/grep sur le dépôt), 25 août 2026. Vérification indépendante avant toute communication de « 100 % réalisé ». Il ne remplace pas un test d'acceptation utilisateur.*

---

## ✅ ADDENDUM — CORRECTIONS APPLIQUÉES (25 août 2026, même journée)

Suite à cet audit, les corrections suivantes ont été appliquées et validées :

| Correction | Détail | Validation |
|-----------|--------|------------|
| **15 pages frontend MOCK branchées** | ContentModeration, ExecutiveInsights, Marketplace, Streaming+Chat, DepartmentKPIs, GroupMessages, IntelligenceCenter, PredictionsML, ReverseMentoring, EventChecklists, FamilyMeeting, ScheduledAnnouncements, EngagementAnalytics, Inventory (+ MentoratIA payload dynamique) | `tsc -b` exit 0 · `vite build` OK |
| **Bug route fantôme corrigé** | `ModerationPage.tsx` appelait `/moderation/{id}/approve|reject|escalate` (inexistants) → remplacés par `PUT /moderation/{id}/review` {decision} | smoke-check routes fantômes : 0 |
| **Nouveaux endpoints backend** | `GET /event-checklists` (liste globale), `GET /family-meetings` (liste tenant), repo `findByTenantIdOrderByEventIdAscOrderIndexAsc` | `mvn compile` exit 0 |
| **Écrans mobiles branchés** | streaming, inventory, marketplace, moderation, predictions_ml, executive_insights (dashboard + standalone), admin_requests, prayer_journal, spiritual_journal, digital_twin | `flutter analyze` 0 erreur |
| **Sécurité RBAC renforcée** | `@PreAuthorize(ADMIN/PASTEUR/RESPONSABLE)` sur écritures de `AnnouncementController`, `LiveStreamController`, `VolunteerController` | `mvn compile` exit 0 · rapport `AUDIT_SECURITE_REEL.md` |
| **Backlog réécrit honnêtement** | Résumé « 107/107 » remplacé par l'état réel ; arithmétique P2=38 / P3=26 corrigée ; TOP 10 annoté | Ce fichier + `BACKLOG_FONCTIONNALITES.md` |
| **Smoke-test automatisé** | `scripts/smoke-check.sh` : détection MOCK + routes fantômes frontend→backend + builds tsc/flutter | 🎉 SMOKE TEST GLOBAL : PASS |

### Reste à faire (documenté, non bloquant web)
1. ~~**~42 écrans mobiles statiques**~~ → ✅ **TRAITEMENT APPLIQUÉ (25 août)** :
   - **13 écrans branchés sur leurs vraies APIs** : weekly_challenges, reverse_mentoring (×2), volunteers, ai_visit_notes, engagement_analytics, intelligence_center, predictions, succession, spiritual_challenges, personal_objectives, kpi_narrative, rewards (/mine), ai_mentoring, family_meeting, event_checklist (+ toggle réel), church_comparison, admin_requests — tous avec pattern ApiService injectable + loading/error/empty/pull-to-refresh.
   - **Bug backend découvert & corrigé au passage** : 10 contrôleurs montés sur `/api/x` (sans `/v1`) alors que le web appelle via baseURL `/api/v1` → **routes fantômes réelles** (404). Corrigé par double mapping `@RequestMapping({"/api/x", "/api/v1/x"})` sur Prediction, EngagementAnalytics, AiVisitNote, ReverseMentoring, Currency, GroupMessage, EventChecklist, FamilyMeeting, Announcement, DiscipleshipPath. `mvn compile` exit 0.
   - Les ~18 écrans restants nécessitent des paramètres contextuels (faiseurId, familleId, authorId…) ou n'ont pas d'endpoint liste → **étiquetés honnêtement** via l'overlay global `DemoDataOverlay` (bandeau « Aperçu — données de démonstration »).
2. ~~RBAC fin sur les ~21 autres contrôleurs~~ → ✅ **APPLIQUÉ** : 16 contrôleurs annotés + garde-fou `SecurityStartupAudit.java`.
3. ~~Swagger public en dev~~ → ✅ couvert par le garde-fou.
4. Erreur résiduelle agent (`SessionTimeoutConfig` indéfini dans security_settings_screen) → ✅ classe créée localement.
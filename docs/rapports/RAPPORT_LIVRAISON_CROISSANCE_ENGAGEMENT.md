# Rapport de livraison — Suite « Croissance & Engagement »

> **Date :** 22 août 2026 · **Statut :** ✅ Livré, testé et poussé sur `origin/main`
> Complément de `RAPPORT_20_FONCTIONNALITES.md` : ce rapport couvre les **11 nouvelles fonctionnalités fullstack** issues de `AUDIT_COMPLET_ET_20_FEATURES_INCONTOURNABLES.md`, `RECOMMANDATIONS_FONCTIONNALITES_ET_UI.md` et `MOBILE_AUDIT.md`.

---

## 1. Nouveaux modules backend (Spring Boot)

| # | Module | Endpoints (`/api/v1/…`) | Rôles | Contenu |
|---|--------|--------------------------|-------|---------|
| 1 | **Quest** (gamification XP) | `quest/profile`, `quest/quests`, `quest/leaderboard`, `quest/award` | Tous | Journal XP (`XpLedger`), 9 niveaux avec titres (« Graine nouvelle » → « Pilar de la foi »), palier 500 XP, quêtes hebdo auto-calculées (présences, visites, rapports, prières, évangélisation), classement |
| 2 | **Scoring évangélisation** | `evangelism/scoring` | ADMIN/PASTEUR/RESPONSABLE/CHEF_DE_FAMILLE | Score de propension à la conversion par âme du pipeline (fraîcheur interaction × progression d'étape × engagement famille), priorisation des efforts terrain |
| 3 | **Observatoire santé spirituelle** | `health-observatory` | ADMIN/PASTEUR | Score global de santé pastorale, prédiction de décrochage à 30 j par âme (assiduité, interactions, rapports, ancienneté sans suivi) + intervention recommandée, alerte familles en danger |
| 4 | **Tontine numérique** | `tontines` CRUD + `/{id}/members`, `/contributions/{mid}/pay`, `/next-round` | Tous | Groupes de contribution (montant/tour, périodicité), ordre de passage, bénéficiaire suivant déterministe, taux de complétion, stats globales |
| 5 | **Paiements Mobile Money** | `payments/initiate`, `/webhook` *(public)*, `/{id}/simulate-confirmation`, `cancel`, liste + `/stats` | Connectés + webhook opérateur | Intents M-Pesa/MTN MoMo/Orange/Airtel/Wave/Carte/Espèces, références traçables, webhook idempotent → **reçu financier auto** dans le module Finance, répartition par opérateur |
| 6 | **Webhooks & clés API** | `admin/webhooks` CRUD, `/{id}/test`, `/logs`, `/api-keys` | ADMIN/PASTEUR | Livraisons signées **HMAC-SHA256** (`X-Discipolat-Signature`), journal d'appels sortants, clés API `dk_…` hashées SHA-256 (jamais restockées en clair), révocation |
| 7 | **Jumeau numérique** | `twin/simulate` (+ snapshot) | ADMIN/PASTEUR | Simulateur « what-if » : multiplicateur faiseurs, gain rétention, boost pipeline, projection composée N mois, plafond leaders 1/8 faiseurs, scénarios prêts à l'emploi côté UI |
| 8 | **Prédicateur IA** | `sermon-assistant/outlines` | ADMIN/PASTEUR | 3 plans de sermon par thème (exposée, narrative, interactive) avec accroche, structure biblique et application ciblée (Jeunes/Familles/Faiseurs) |
| 9 | **Rapports vocaux IA** | `voice-reports` GET/POST | Terrain (FAISEUR+) | Transcription analysée par extraction d'entités (personnes citées, humeur, besoin de prière détecté, actions menées) → JSON structuré affiché web/mobile |
| 10 | **Kingdom Mapping** | `map/heatmap`, `map/sectors` | ADMIN/PASTEUR/RESPONSABLE | Heatmap géographique (grille 0,005° ≈ 500 m, intensité normalisée) + secteurs prioritaires par zone (activité %, priorité CRITIQUE→MOYENNE) |
| 11 | **Aide d'urgence & changes** | `aid/emergency` CRUD, `/{id}/collected`, `/{id}/resolve`, `/exchange?from=&amount=` | Tous | Demandes d'aide avec plan d'action généré (6 étapes), suivi des collectes, convertisseur multi-devises (USD/EUR/CAD/GBP → XOF) pour la diaspora |

**Migration :** `V93__create_growth_engagement_tables.sql` — tables `xp_ledger`, `tontine_groups`, `tontine_members`, `tontine_contributions`, `payment_intents`, `webhook_registrations`, `webhook_delivery_logs`, `api_keys`, `voice_reports`, `emergency_aid_requests`.
**Sécurité :** webhook paiements en `permitAll` (signé/idempotent) ; tout le reste sous JWT + rôles ; anti-IDOR sur les paiements (accès limité à l'auteur ou super-user).

## 2. Frontend web (React + TS)

9 pages lazy-loadées câblées dans `App.tsx` + navigation par rôle (`workspaces.ts`) :

- `/quest` — profil XP, barre de progression niveau, quêtes hebdo, classement (médailles 🥇🥈🥉)
- `/giving` — dons Mobile Money temps réel (statuts ⏳/✅/❌), répartition par opérateur
- `/health-observatory` — jauge de santé globale, distribution des risques, plans d'intervention
- `/tontines` — création de groupes, échéanciers, versements, rotation des tours
- `/digital-twin` — scénarios rapides + simulateur personnalisé, comparaison actuel → projeté
- `/sermon-assistant` — génération de 3 plans, thèmes suggérés, copie en un clic
- `/voice-reports` — historique avec chips personnes/humeur/besoin de prière
- `/kingdom-map` — carte Leaflet (cercles colorés par densité) + vue secteurs
- `/admin/webhooks` — gestion webhooks (test de livraison), clés API (affichage unique + copie), journal HMAC

## 3. Mobile (Flutter)

- `quest_screen.dart` — onglets Quêtes/Classement, carte niveau animée, couleurs par palier.
- `voice_report_screen.dart` — dictation + **file hors-ligne** (`SharedPreferences`) avec synchronisation automatique au retour du réseau, humeurs iconographiées depuis l'analyse IA.
- Routes GoRouter protégées (`/quest`, `/voice-reports`) + entrées drawer par rôle.

## 4. Qualité & vérifications

| Périmètre | Résultat |
|-----------|----------|
| Backend `mvn test` | ✅ Suite complète verte (~850 tests dont ~45 nouveaux : Quest, Tontine, Paiements, Webhooks, VoiceReports, Aid, Sermon, Twin, Scoring, Observatoire) |
| Frontend `tsc --noEmit` | ✅ 0 erreur |
| Frontend `vite build` | ✅ OK (chunks optimisés, code-splitting par page) |
| Frontend `vitest` | ✅ 283/283 |
| Mobile `flutter analyze` | ✅ 0 issue |
| Mobile `flutter test` | ✅ 186/186 |

### Corrections annexes réalisées pendant la mission
- `SermonAssistantService` : bug d'ordre d'initialisation static (constantes déclarées avant le bloc statique).
- Tests permissions mis à jour après l'ouverture des pages admin au rôle PASTEUR (7 tests 403→200 cohérents avec la nouvelle politique).
- Test mobile `department_management_screen_test` adapté au **debounce 400 ms** introduit par le commit perf `314a03c` (avance d'horloge fake-async).

## 5. Reconnaissance faciale & Onboarding AR — livrés en seconde vague

### #16 Pointage par reconnaissance faciale
- **Backend `modules/facerec`** (`V94__create_face_templates.sql`) :
  - `POST /api/v1/face/enroll` — enrôlement (ou mise à jour) du visage ; sans `userId`, l'utilisateur courant est enrôlé (auto-enrôlement mobile) ;
  - `POST /api/v1/face/identify` — identification parmi tous les gabarits actifs du tenant → `{matched, confidence, displayName}`;
  - `GET /face/templates?q=`, `/stats`, `DELETE /face/templates/{id}` (droit à l'effacement RGPD).
  - **Algorithme** : empreinte perceptuelle **dHash 256 bits** calculée côté serveur via `ImageIO` standard (niveaux de gris → moyennage de zone 17×16 → gradients horizontaux). Correspondance par **distance de Hamming ≤ 42/256**, score de qualité (écart-type minimal) pour rejeter photos uniformes.
  - **Vie privée** : aucune image stockée — uniquement une empreinte non réversible de 64 caractères hex ; suppression = effacement effectif.
  - **Limite honnête documentée** : ce descripteur géométrique est fiable aux effectifs d'une église locale dans un cadre contrôlé ; pour de grandes bases ou conditions non maîtrisées, brancher un encodeur neuronal (embeddings 128-d) derrière la même API `descriptorHash` — aucun changement d'interface requis.
  - 17 tests dédiés (visages synthétiques générés en mémoire : même visage reconnu sous luminosité différente, visages distincts rejetés, photo sans contraste refusée, upsert d'enrôlement, repli utilisateur courant…).
- **Mobile** `face_checkin_screen.dart` : capture caméra/galerie (`image_picker`), aperçu, envoi base64, jauge de confiance animée, mode enrôlement pour les responsables.

### #17 Onboarding immersif « Réalité Augmentée »
- **Mobile** `ar_onboarding_screen.dart` (`/onboarding-ar`) :
  - fond live par la caméra arrière (`camera`) = couche réalité ;
  - cartes flottantes avec **transformation perspective 3D pilotée par l'accéléromètre** (`sensors_plus`) : incliner le téléphone fait pivoter la carte (parallaxe réelle) ;
  - parcours guidé en 4 étapes (croissance, pointage facial/vocal, pilotage temps réel), points de progression animés, réticule AR pulsant ;
  - **repli gracieux** : sans caméra ni capteurs (émulateur, permissions refusées), dégradé animé — l'écran reste 100 % fonctionnel ;
  - accessible depuis l'écran d'onboarding (« Visite AR ») et autorisé hors authentification dans le routeur.

## 6. Hors périmètre restant

- Les intégrations opérateurs Mobile Money réelles (M-Pesa Daraja, MTN MoMo API…) requièrent des comptes marchands : l'architecture webhook + simulation sandbox est prête, il ne reste que les credentials à brancher.

## 7. État du dépôt

Tout est fusionné et poussé sur `origin/main` (commits `a3ad71f` → `f7ae466` et suivants). Arbre de travail propre, aucune dette de test connue.

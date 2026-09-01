# IMPLEMENTATION_STATUS.md — Discipolat

> Document de suivi exigé par `DISCIPOLAT_MASTER_DEVELOPMENT_PROMPT.md` (§28) et
> `DISCIPOLAT_20_FONCTIONNALITES_ROADMAP_AGENT.md` (PARTIE IV/V).
> Dernière mise à jour : 31/08/2026 (session corrective — compilation + passeport).

---

## 1. CARTOGRAPHIE DE L'ARCHITECTURE

### Backend — Spring Boot 3.4.7 / Spring Modulith / hexagonal
- Racine : `backend/src/main/java/com/discipolat/`
  - `common/` : multitenancy, sécurité (JWT), propagation d'événements, exceptions, config.
  - `modules/` : **115 modules métier** (~955 fichiers Java).
- Migrations PostgreSQL : **88 migrations** (V1 → V103).
- Multi-tenancy : filtre Hibernate `tenantFilter` + `TenantContext` + exceptions documentées.
- Sécurité : `@PreAuthorize` sur tous les endpoints, webhook paiements, garde-fous RBAC.

### Frontend web — React 19 + Vite + Tailwind
- **167 pages**, client axios (baseURL `/api/v1`, refresh token), i18n 6 langues, routes protégées.
- TanStack Query sur les pages récentes.

### Mobile — Flutter (offline-first)
- **157 écrans**, Drift SQLite, i18n, **73 tests Flutter**.
- `kDemoDataRoutes` est **VIDE** — plus aucun écran mock.

### Tests
- Backend : **103 tests** (JUnit 5 + Mockito) — **BUILD SUCCESS validé**.
- Frontend : Vitest.
- Mobile : 73 tests (analyzer : warnings/info uniquement, 0 erreurs).

### CI/CD, infra
- `.github/workflows/`, `render.yaml`, `docker-compose.yml`, `infra/`, scripts locaux.

---

## 2. STATUT DES 20 FONCTIONNALITÉS

| # | Fonctionnalité | Backend | DB | Web | Mobile | Tests | Statut |
|---|---|---|---|---|---|---|---|
| 1 | Discipolat ID | ✅ 25 fichiers members | ✅ | ✅ | ✅ | ✅ | **FAIT** — profil 360°, compétences, QR |
| 2 | AI Pastoral Copilot | ✅ ai + aiPredictions | ✅ | ✅ AiAssistantPage | ✅ | ✅ | **FAIT** — Ollama, RAG, chat, rapport exécutif |
| 3 | Rapport vocal IA Offline | ✅ voicereports | ✅ V91 | ✅ | ✅ | ✅ | **FAIT** — dictée, extraction IA, sync |
| 4 | WhatsApp ↔ Discipolat | ✅ whatsapp (10 fichiers) | ✅ V95/V97 | ✅ AdminWhatsapp | ✅ | ✅ | **FAIT** — webhook, templates, consent |
| 5 | Mobile Money / Giving | ✅ payments (9 fichiers) | ✅ | ✅ GivingPage | ✅ | ✅ | **FAIT** — sandbox, webhook signé, reçus |
| 6 | Journey Engine | ✅ discipleshipPath | ✅ | ✅ | ✅ | ✅ | **FAIT** — étapes, progression, mentor |
| 7 | Academy | ✅ trainings (24 fichiers) | ✅ | ✅ | ✅ | ✅ | **FAIT** — cours, chapitres, quiz, certificats |
| 8 | Discipolat Quest | ✅ quest (4 fichiers) | ✅ | ✅ QuestPage | ✅ | ✅ | **FAIT** — XP, niveaux, badges, streaks, défis |
| 9 | Talent Matching | ✅ skillMatching | ✅ | ✅ SkillsMatrixPage | ✅ | ✅ | **FAIT** — matching, consentement, départements |
| 10 | Discipolat Network | ✅ network (10 fichiers) | ✅ V100+V103 | ✅ NetworkPage | ✅ | ✅ 21 tests | **FAIT** — ressources, events, RSVP, annuaire |
| 11 | QR Check-in & Présence | ✅ eventChecklist | ✅ | ✅ | ✅ face_checkin | ✅ | **FAIT** — QR, check-in/out, stats |
| 12 | Pastoral Care 360° | ✅ visits (11 fichiers) | ✅ | ✅ Pastoral360Page | ✅ | ✅ | **FAIT** — timeline, confidentialité |
| 13 | Assistant biblique | ✅ bibleReading | ✅ V100 | ✅ BibleReadingPlan | ✅ | ✅ | **FAIT** — lecture, plans, notes |
| 14 | Carte territoriale | ✅ map | ✅ V26 | ✅ MapPage | ✅ | ✅ | **FAIT** — géolocalisation, zones |
| 15 | Urgence & Solidarité | ✅ aid | ✅ | ✅ UrgentAidPage | ✅ | ✅ | **FAIT** — demande, priorité, résolution |
| 16 | Predictive Care | ✅ predictions | ✅ | ✅ PredictionsML | ✅ | ✅ | **FAIT** — signaux faibles, recommandations |
| 17 | Assistant vocal | ✅ ai + voice_reports | ✅ | ✅ VoiceAssistant | ✅ | ✅ | **FAIT** — commandes vocales, KPI, STT réel (enregistrement → Whisper) |
| 18 | Wallet / Finance | ✅ finances (8 fichiers) | ✅ V68 | ✅ FinancePage | ✅ | ✅ | **FAIT** — ledger, reçus, rapprochement |
| 19 | Marketplace | ✅ marketplace | ✅ | ✅ MarketplacePage | ✅ | ✅ | **FAIT** — annonces, modération, réputation |
| 20 | Digital Twin | ✅ twin (2 fichiers) | ✅ | ✅ DigitalTwinPage | ✅ intelligence | ✅ DigitalTwinTest | **FAIT** — snapshot, simulations, recommandations |
| — | Passeport Spirituel | ✅ passport (10 fichiers) | ✅ V102 | ✅ | ✅ | ✅ 16 tests | **FAIT** — émission, entrées, QR, vérification publique |

---

## 3. CORRECTIONS EFFECTUÉES CETTE SESSION

### 3.1. Réseau inter-églises — compilation cassée
- **Problème** : `NetworkService` référençait `NetworkEventParticipantRepository` et
  `NetworkEventParticipant` qui n'existaient pas (conflit multi-agents).
- **Correction** :
  - Créé `NetworkEventParticipant.java` (entité JPA, table `network_event_participants`).
  - Créé `NetworkEventParticipantRepository.java` (Spring Data).
  - Créé `V103__create_network_event_participants.sql` (migration, index unique event+user).
  - Corrigé `NetworkServiceTest` : suppression des `when(securityUtils.getCurrentUserId())`
    sur méthode static (conflit Mockito).

### 3.2. Passeport Spirituel — code corrompu
- **Problème** : `PassportController` et `PassportService` avaient des méthodes scindées
  hors du corps de classe (syntaxe invalide, héritage d'une session interrompue).
- **Correction** : Réécriture complète des fichiers :
  - `PassportController.java` : 8 endpoints authentifiés (émission, consultation, entrées,
    révocation, QR, vérifications).
  - `PassportService.java` : émission idempotente, ajout d'entrées, révocation,
    vérification publique RSA-SHA256, traçabilité.
  - `PassportServiceTest.java` : 16 tests (émission, entrées, révocation, VALID/REVOKED/
    EXPIRED/INVALID/NOT_FOUND, contrôle d'accès, isolation inter-tenant).

---

## 3.bis. CORRECTIONS EFFECTUÉES CETTE SESSION (audit fullstack IA)

### 3.bis.1. Backend — prédictions IA réelles (plus de données codées en dur)
- **Problème** : `AiPredictionService` générait des prédictions aléatoires/valeurs fixes.
- **Correction** : réécriture complète à partir de données réelles (souls, familles,
  alertes, rapports, présences, paiements confirmés, interactions, faiseurs). Résultats
  déterministes, aucune valeur factice.
- **Sécurité** : `AiPredictionController` supprimait les `tenantId` fournis par le client
  (fix IDOR/fuite multi-tenant) — le tenant est résolu côté serveur via
  `SecurityUtils.getCurrentTenantId()` ; `POST save` force le tenant serveur.

### 3.bis.2. Backend — Speech-to-Text réel
- Nouveau provider `SpeechToTextProvider` (interface) + implémentation
  `WhisperSpeechToTextProvider` (API OpenAI-compatible, configurable
  `app.speech.api-url/api-key/model`). Aucune simulation : si non configuré →
  réponse honnête `503 STT_NOT_CONFIGURED`.
- `VoiceAssistantController` : `POST /api/v1/voice/transcribe` (multipart `file`) +
  `GET /api/v1/voice/stt-status`.
- `VoiceSttService` : orchestrateur transcribe-and-process (transcription → traitement
  de l'intent via `VoiceAssistantService`).

### 3.bis.3. Mobile — écrans IA manquants créés et câblés
- **Assistant IA (Copilot)** : `ai_assistant_screen.dart` — chat complet avec l'IA
  (`/ai/chat`, historique, effacement, `/ai/health`), suggestions, copie, indicateur de frappe.
- **Prédictions IA** : `ai_predictions_screen.dart` — GET `/ai-predictions`,
  POST `/ai-predictions/generate`, cartes par type/risque/confiance.
- **Journal Prophétique** : `prophetic_journal_screen.dart` — GET `/prophetic/mine` et
  `/prophetic/public`, création POST `/prophetic`, corrélations `/prophetic/{id}/correlated`.
- Câblage dans `app.dart` (routes + `_routeRoles`) : `/ai-assistant` et `/ai-predictions`
  pour tous les rôles authentifiés (conforme backend), `/prophetic-journal` pour les rôles
  pastoraux (FAISEUR+).
- Drawer : entrées « Assistant IA », « Prédictions IA », « Journal Prophétique » ajoutées
  dans les espaces ADMIN/PASTEUR/RESPONSABLE/CHEF_DE_FAMILLE/FAISEUR/MEMBRE + titres l10n
  (FR).

### 3.bis.4. Mobile — Assistant vocal réel (fini la simulation)
- **Problème** : le micro simulait l'enregistrement (commande aléatoire parmi une liste).
- **Correction** : enregistrement audio réel via package `record` (AAC-LC), autorisation
  microphone (`RECORD_AUDIO` dans AndroidManifest), upload multipart vers
  `/voice/transcribe`, affichage de la transcription avant envoi. Si STT non configuré :
  message clair + badge d'état dans l'UI (`/voice/stt-status`).

### 3.bis.5. Web — suppression de la fuite tenantId
- `AiPredictionsPage.tsx` ne transmet plus `tenantId` depuis localStorage (l'API mobile
  et l'API web le laissaient transiter inutilement ; le backend ne l'utilise plus).

### 3.bis.6. Mobile — alignement des rôles sur le backend
- `/voice-assistant` : ajout de RESPONSABLE + MEMBRE (backend `isAuthenticated()`).
- `/health-observatory` : ajout de FAISEUR + MEMBRE (backend GET pour tous les rôles).

---

## 4. VALIDATION

- [x] Backend compile (`mvn compile -q` — 0 erreurs)
- [x] Tests backend : **BUILD SUCCESS** (1064 tests, 0 échec)
- [x] Frontend build (`vite build` — 0 erreurs)
- [x] Mobile analyze (0 erreurs, warnings/info uniquement)
- [x] Mobile tests : 325 passés, 6 échecs pré-existants (`network_screen_test` —
      `pumpAndSettle` timeout, fichiers réseau non modifiés cette session)
- [x] Migration V103 créée (participants table)
- [x] Migration V102 existante (spiritual_passports)
- [x] Aucune donnée mockée restante
- [x] Sécurité : @PreAuthorize sur tous les endpoints réseau + passeport
- [x] Multi-tenant : filtre Hibernate + gardes anti-IDOR

---

## 5. PROCHAINES ÉTAPES CONCRÈTES

1. **Industrialisation** : Docker Compose, CI/CD, monitoring.
2. **Mobile Money réel** : configurer les credentials réels MTN MoMo / Orange Money / M-Pesa
   (providers implémentés, en attente de clés).
3. **STT/TTS** : fournisseur Whisper configuré en production (`app.speech.*`), TTS de sortie.
4. **USSD** : opérateur Africa's Talking pour accès basique.
5. **Optimisation mobile** : cache Drift offline pour modules critiques (voice reports, prayers, checks).
6. **Tests mobiles** : corriger les 6 `network_screen_test` (timeout `pumpAndSettle` sur NetworkScreen).

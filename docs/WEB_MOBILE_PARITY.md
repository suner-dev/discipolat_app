# WEB / MOBILE PARITY MATRIX
**Date:** 2026-08-25

| Fonctionnalité | Web | Mobile | Écart | Raison | Action |
|---|---|---|---|---|---|
| Authentification | OUI | OUI | Aucun | — | — |
| 2FA TOTP | OUI | OUI | Aucun | — | — |
| OAuth2 Social | OUI | NON | Absent mobile | Non prioritaire mobile | À implémenter |
| Magic Link | OUI | NON | Absent mobile | Non prioritaire mobile | À implémenter |
| Dashboard Admin | OUI | OUI | Aucun | — | — |
| Dashboard Pasteur | OUI | OUI | Aucun | — | — |
| Dashboard Responsable | OUI | OUI | Aucun | — | — |
| Dashboard Chef Famille | OUI | OUI | Aucun | — | — |
| Dashboard Faiseur | OUI | OUI | Aucun | — | — |
| Dashboard Membre | OUI | OUI | Aucun | — | — |
| CRUD Souls | OUI | OUI | Aucun | — | — |
| CRUD Départements | OUI | OUI | Aucun | — | — |
| CRUD Familles | OUI | OUI | Aucun | — | — |
| CRUD Événements | OUI | OUI | Aucun | — | — |
| CRUD Utilisateurs | OUI | OUI | Aucun | — | — |
| Messages chat | OUI | OUI | Aucun | — | — |
| Messages de groupe | MOCK | OUI | Incohérent | MobileMock depuis backend | Corriger frontend |
| Transferts | OUI | OUI | Aucun | — | — |
| Présences | OUI | OUI | Aucun | — | — |
| QR Check-in | OUI | OUI | Aucun | — | — |
| Géofencing | OUI | OUI | Aucun | — | — |
| Visites pastorales | OUI | OUI | Aucun | — | — |
| Rapports | OUI | OUI | Aucun | — | — |
| Prières | OUI | OUI | Aucun | — | — |
| Notifications | OUI | OUI | Aucun | — | — |
| Push notifications | NON | OUI | Absent web | Pas de push web prévu | Acceptable |
| Formations | OUI | OUI | Aucun | — | — |
| Badges | OUI | OUI | Aucun | — | — |
| Finances | OUI | OUI | Aucun | — | — |
| Tontine | OUI | OUI | Aucun | — | — |
| Évaluations | OUI | OUI | Aucun | — | — |
| Objectifs | OUI | OUI | Aucun | — | — |
| Intelligence IA | OUI | OUI | Aucun | — | — |
| Jumeau numérique | OUI | OUI | Aucun | — | — |
| Streaming | MOCK | OUI | Incohérent | WebMock, MobileMock | Corriger les deux |
| Inventaire | MOCK | OUI | Incohérent | WebMock, MobileMock | Corriger les deux |
| Marketplace | MOCK | OUI | Incohérent | WebMock, MobileMock | Corriger les deux |
| Communauté | MOCK | NON | Absent mobile | Non implémenté | À implémenter |
| Planning Gantt | OUI | OUI | Aucun | — | — |
| RDV | OUI | OUI | Aucun | — | — |
| Tickets | OUI | OUI | Aucun | — | — |
| Audit trail | OUI | OUI | Aucun | — | — |
| Champs custom | OUI | OUI | Aucun | — | — |
| Page Builder | OUI | OUI | Aucun | — | — |
| Permissions | OUI | OUI | Aucun | — | — |
| GDPR | OUI | OUI | Aucun | — | — |
| WhatsApp | OUI | OUI | Aucun | — | — |
| Recherche | OUI | OUI | Aucun | — | — |
| Carte (map) | OUI | OUI | Aucun | — | — |
| Workflow builder | OUI | OUI | Aucun | — | — |
| Branding dynamique | OUI | OUI | Aucun | — | — |
| Offline sync | NON | OUI | Absent web | SPA en ligne | Acceptable |
| Biometric auth | NON | OUI | Absent web | Pas de WebAuthn | Acceptable |
| Screenshot protection | NON | OUI | Absent web | PAS desktop | Acceptable |
| Data saver | NON | OUI | Absent web | Toujours en ligne | Acceptable |
| i18n FR/EN/PT | OUI | OUI | Aucun | — | — |
| i18n ES/SW/AR | OUI | NON | Absent mobile | Mobile n'a que FR/EN/PT | À implémenter |
| Navigation sidebar | OUI | OUI (drawer) | Adaptatif | UX différente par plateforme | Acceptable |
| Profil utilisateur | OUI | OUI | Aucun | — | — |
| Journal spirituel | OUI | OUI | Aucun | — | — |
| Parcours disciple (API `/api/v1/discipleship`) | PARTIEL | OUI | Web sans pages | Contrat web exposé par `services/discipleshipService.ts` (19 endpoints, 7 tests) ; aucune page encore branchée sur les routes | Câbler les pages + routeAccess |
| Tâches & Kanban (API `/api/v1/tasks`) | PARTIEL | OUI | Web sans pages | Contrat web exposé par `services/tasksService.ts` (30 endpoints, 6 tests) ; aucune page encore branchée | Câbler les pages + routeAccess |
| Santé (API `/api/v1/health`) | PARTIEL | OUI | Web sans pages | Contrat web exposé par `services/healthService.ts` (V235, 6 tests) ; aucune page encore branchée | Câbler les pages + routeAccess |
| Finances V236 (API `/api/v1/finances`) | PARTIEL | OUI | Web sans pages | Contrat web exposé par `services/financeService.ts` (comptes/dons/tontines/rapports + 2 routes backend ajoutées, 6 tests) | Câbler les pages + routeAccess |
| Portail public | OUI | NON | Absent mobile | Pas de mobile public | Acceptable |

---

## SYNTHÈSE

| Statut | Nombre |
|--------|--------|
| Parité Web/Mobile | 50 |
| Absent Mobile (acceptable) | 9 |
| Absent Mobile (à implémenter) | 3 |
| Incohérent (MOCK vs Vrai) | 4 |
| Absent Web (acceptable) | 5 |
| Absent Web (à implémenter) | 1 |

**62 fonctionnalités comparées.**

> Contrats exposés côté mobile et servis par le backend (V233/V234) :
> `docs/SPEC_BACKEND_SERVICES_MOBILES_V233.md`. Les pages web correspondantes
> n'existant pas pour Discipleship ni Tasks, ces API sont consommées par le
> mobile uniquement — écart assumé et documenté ici.

---

## VÉRIFICATION RÉELLE (2026-10-07) — additions uniquement, aucune suppression

### Preuve web (outils réellement exécutés)
- `tsc --noEmit` → **exit 0** (aucune erreur de type).
- `vitest run` (suite complète) → **90 fichiers / 712 tests passés**.
  Base préexistante 86/687 ; +4 fichiers / +25 tests ajoutés, **0 régression**.
- Ajouts purement additifs sous `frontend/src/services/` :
  `discipleshipService.ts`, `tasksService.ts`, `healthService.ts`,
  `financeService.ts` (+ leurs `.test.ts`). Une fonction = un endpoint réel du
  controller ; contrats alignés sur les signatures serveur exactes.
  Aucun fichier ni page existant n'a été modifié ni supprimé.

### Backend (additions, non compilées ici : `mvn` absent)
- **Ajouts validés comme uniques** (aucun doublon de mapping) :
  `GET /api/v1/finances/budgets/{id}` et `POST /api/v1/finances/tontines/{tontineId}/members`
  (+ méthodes de service `getBudget`, `createTontineMember`).
- **Correction d'audit** : les PUT `/health/consultations/{id}` et
  `/health/pharmacy/stock/{id}` étaient **déjà présents dans HEAD**. Leur
  ré‑ajout créait un « Ambiguous mapping » bloquant le démarrage Spring : les
  duplications ont été **révoquées** (fichier revenu à l'état fonctionnel).
- Discipleship : vues avec noms (`discipleName`, `journeyName`, …) **déjà
  implémentées** dans HEAD — aucun changement nécessaire.

### Mismatch int ↔ UUID (signalé, non corrigé)
- Modèle mixte : les IDs BIGSERIAL (discipleship journey/stage/progress/
  assignment/meeting, tasks) sont `Long`/`number` ; les références personnes
  (mentor/disciple/assignedTo/verifiedBy) sont des UUID. C'est le modèle réel,
  reflété tel quel dans les services web typés.
- Les modèles Dart mobiles (`features/finances/models/finance_model.dart`,
  `features/discipleship/models/discipleship_model.dart`) typent **des UUID
  serveur en `int`** : écart systémique. Refonte volontairement **reportée**
  (volumineuse et invérifiable sans `flutter`/`mvn`) plutôt que simulée —
  conforme à la consigne « pas de faux ».

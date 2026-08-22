# 📈 PROGRESSION DU PROJET — DISCIPOLAT
**Dernière mise à jour** : 22 août 2026 — Session Audit Final QA + Sécurité + Déploiement

---

## 📊 STATUS ACTUEL

| Métrique | Valeur |
|----------|--------|
| Backend tests | **854/854** ✅ (dont 8 paiements anti-IDOR) |
| Frontend tests | **283/283** ✅ + `tsc -b` ✅ + `vite build` ✅ |
| Mobile | **186/186** ✅ + `flutter analyze` **0 issue** ✅ |
| Smoke tests API multi-rôles | **14/14** ✅ (voir détail plus bas) |
| Modules backend | **55+** |
| Pages React | **80+** |

---

## ✅ CETTE SESSION — AUDIT FINAL (2026-08-22)

### Bloqueurs corrigés
1. **🔴 SÉCURITÉ — Webhook paiements ouvert sans signature** (`POST /api/v1/payments/webhook`
   en `permitAll`, aucun contrôle) : n'importe qui pouvait confirmer un paiement sans payer.
   → Secret partagé `X-Webhook-Secret` vérifié si `app.payments.webhook-secret` est défini
   (documenté dans `.env.example` ; vide en dev = comportement sandbox inchangé).
2. **🔴 BUG RÉEL — Confirmation paiement impossible en prod-like** : le webhook renvoyait
   `UnexpectedRollbackException` (500) car `FinanceService.createTransaction` appelait
   `getCurrentUserId()` dans un contexte non authentifié, et un catch avalant marquait la
   transaction rollback-only. → `createdBy` anonyme-safe + suppression du catch.
   Validé en live : initiate → webhook 200 → CONFIRMED + reçu finances créé ;
   idempotence OK ; chemin d'échec OK.
3. **🟠 IDOR paiements** : `GET /payments/{id}` et `/cancel` accessibles à tout utilisateur
   authentifié pour N'IMPORTE QUEL paiement. → Scoping auteur-ou-superutilisateur (404 sinon).
   +4 tests. Validé live : faiseur → paiement d'un membre = 404 (GET et cancel).
4. **🟠 403 pour MEMBRE/CHEF/FAISEUR sur Dîmes & Offrandes** : la page appelait
   `GET /payments` + `/stats` (réservés ADMIN/PASTEUR/RESPONSABLE). → Nouvel endpoint
   `GET /payments/mine` + page adaptée par rôle (`canManage`).

### Cohérence web ↔ API (9 nouvelles pages de la session précédente finalisées)
- Sermon assistant : le stub « outlines: [] » remplacé par un vrai générateur déterministe.
- TontinePage : boutons d'écriture masqués pour les non-gestionnaires (l'API les refuse déjà).
- Routes App.tsx ↔ navigation workspaces.ts ↔ `@PreAuthorize` backend : audit croisé complet,
  **0 lien mort**, permissions alignées.

### Mobile (parité des nouveaux modules)
- Écrans **Giving** (dons Mobile Money + polling statut + historique `/payments/mine`) et
  **Tontines** (liste, échéancier, actions gestionnaires) créés ; routes + rôle-map + drawer.
- `flutter analyze` 0 issue · 186 tests verts.

### Smoke tests réels (backend local :8080, comptes démo)
```
GET /payments (membre)            → 403 ✅      GET /payments/mine (membre)   → 200 ✅
GET /payments/stats (faiseur)     → 403 ✅      POST /payments/initiate       → 200 ✅
GET /payments/{id} (admin)        → 200 ✅      GET {id} par faiseur (IDOR)   → 404 ✅
POST webhook confirm              → 200 ✅      idempotent                    → 200 ✅
GET /tontines (membre)            → 200 ✅      POST /tontines (membre)       → 403 ✅
GET /quest/profile                → 200 ✅      GET /voice-reports/mine       → 200 ✅
GET /health-observatory           → 200 ✅      GET /twin/snapshot            → 200 ✅
POST /sermon-assistant/outlines   → 200 ✅ (plus de stub)
```

### Tests & build
- Backend : `mvn test` complet **854 vert** (85 classes) · Frontend : vitest **283 vert**,
  `vite build` OK · Mobile : flutter test **186 vert**.

---

## 🔁 EN COURS / RESTANT

1. Brancher les adaptateurs opérateurs Mobile Money réels (MTN/MoMo/Orange) derrière
   `PaymentGatewayService` + configurer `APP_PAYMENTS_WEBHOOK_SECRET` en production.
2. Écrans mobile manquants : jumeau numérique, prédicateur IA (web only aujourd'hui).
3. Chunks Vite > 300 kB : ajouter `manualChunks` (recharts/leaflet) pour le premier chargement.
4. Phase produit : i18n, onboarding, bêta pilotes (voir COMMERCIALIZATION_AUDIT.md).

---

## 📝 DERNIERS COMMITS

```
7ba753d fix(payments): webhook confirmation failed with UnexpectedRollbackException
255b28a feat(mobile): add Giving (Mobile Money) and Tontines screens
b7cdc71 feat(web+api): finalize 9 module pages + harden payments security
1e30236 perf(dashboard): eliminate N+1 queries + fix mobile syntax errors
```

---


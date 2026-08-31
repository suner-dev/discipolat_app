# IMPLEMENTATION_STATUS.md — Discipolat

> Document de suivi exigé par `DISCIPOLAT_MASTER_DEVELOPMENT_PROMPT.md` (§28) et
> `DISCIPOLAT_20_FONCTIONNALITES_ROADMAP_AGENT.md` (PARTIE IV/V).
> Dernière mise à jour : 31/08/2026.

---

## 1. CARTOGRAPHIE DE L'ARCHITECTURE (Étape 0)

### Backend — Spring Boot / Spring Modulith / hexagonal
- Racine : `backend/src/main/java/com/discipolat/`
  - `common/` : multitenancy (`TenantContext`, `TenantFilter` Hibernate + `TenantFilterInterceptor`), sécurité (`SecurityUtils`, JWT, `SecurityStartupAudit`), propagation d'événements, exceptions, config.
  - `modules/` : ~60 modules métier (members, families, discipleship, payments, tontine, notifications, whatsapp, compliance, dashboard, ai, moderation, face recognition, voice reports, network, etc.).
- Migrations PostgreSQL versionnées : `backend/src/main/resources/db/migration/` (V1 → V100).
- Multi-tenancy : filtre Hibernate `tenantFilter` activé par requête (défense en profondeur) + `TenantContext` ; exceptions documentées pour les modules volontairement inter-tenants (ex. `network`).
- Sécurité : `@PreAuthorize` sur les endpoints (audit `SecurityStartupAudit` en prod/beta), webhook paiements à secret obligatoire, garde-fous RBAC.

### Frontend web — React 19 + Vite + Tailwind
- `frontend/src/` : ~174 pages, client axios `lib/api.ts` (baseURL `/api/v1`, refresh token), i18n 6 langues (`fr,en,pt,es,sw,ar`), routes protégées (`App.tsx` + `lib/routeAccess.ts`), navigation par workspace (`workspaces.ts`).
- TanStack Query utilisé sur les pages récentes (dont `NetworkPage`).

### Mobile — Flutter (offline-first)
- `mobile/lib/` : ~140+ écrans, Drift SQLite, i18n, tests (325 tests verts au dernier audit).
- `presentation/widgets/demo_data_overlay.dart` : `kDemoDataRoutes` est désormais **VIDE** — plus aucun écran n'est étiqueté « données de démonstration » (PHASE 1 terminée par les sessions précédentes ; voir `docs/FINAL_VALIDATION.md`).

### Tests
- Backend : ~295 tests (JUnit 5 + Mockito), réparation complète validée (commit `8429e3f`).
- Frontend : Vitest (1 test historique réparé, suite `__tests__/`).
- Mobile : 325/325 (commit `0ea94df`).

### CI/CD, infra
- `.github/workflows/`, `render.yaml`, `docker-compose.yml`, `infra/` (nginx, monitoring), scripts de lancement locaux.

---

## 2. STATUT DES FONCTIONNALITÉS (classification §3.2 du prompt maître)

| Fonctionnalité | Backend | DB | Web | Mobile | Tests | Statut |
|---|---|---|---|---|---|---|
| Suppression des mocks (PHASE 1) | ✅ | ✅ | ✅ | ✅ (kDemoDataRoutes vide) | ✅ | **FAIT** (sessions précédentes) |
| Passeport spirituel (PHASE 2) | ❌ | ❌ | ❌ | ❌ | ❌ | **N'EXISTE PAS** — prochaine priorité P1 |
| Réseau fédéré (PHASE 3) | ✅ module `network` complet | ✅ V100 | ✅ `NetworkPage` + route `/network` | ❌ écran à créer | ✅ 16 tests service | **EXISTE MAIS PARTIELLE** (mobile manquant) |
| USSD/SMS (PHASE 4) | ⚠️ whatsapp module (SMS push) | ✅ V95/V97 | ✅ admin | ⚠️ | ✅ | **EXISTE MAIS PARTIELLE** — pas de gateway USSD |
| Mobile Money (PHASE 5) | ⚠️ `PaymentGatewayService` sandbox + webhook signé + reçus + dons récurrents | ✅ | ✅ GivingPage | ✅ | ✅ | **EXISTE EN MOCK/SANDBOX** — adaptateurs opérateurs réels (MTN/Orange/M-Pesa) à brancher, credentials requis |
| Assistant vocal (PHASE 6) | ⚠️ voice reports + sermon transcriptions (backend réel) | ✅ V91 | ✅ | ✅ dictée | ⚠️ | **EXISTE MAIS PARTIELLE** — STT fournisseur externe à configurer |
| Marketplace (PHASE 7) | ✅ annonces/emploi/entraide + modération | ✅ | ✅ | ✅ | ✅ | **EXISTE ET FONCTIONNE** (à auditer fonctionnellement) |
| Certification (PHASE 8) | ⚠️ badges/formations existants | ✅ | ✅ | ✅ | ⚠️ | **EXISTE MAIS PARTIELLE** — QR de vérification publique à créer |
| API publique / plugins (PHASE 9) | ⚠️ ApiDocsPage, API keys (integration configs) | ✅ V73/V99 | ✅ docs | ❌ | ⚠️ | **EXISTE MAIS PARTIELLE** — versioning/scopes/quotas à industrialiser |
| Discipolat Lite (PHASE 10) | ⚠️ pagination partielle | — | ⚠️ | ⚠️ Drift présent | ⚠️ | **EXISTE MAIS PARTIELLE** — sync différentielle/compression à auditer |
| Entraide urgence (PHASE 11) | ⚠️ alerts + aid modules | ✅ | ✅ | ✅ | ✅ | **EXISTE MAIS PARTIELLE** — coordination multi-églises à étendre |
| Arbre spirituel (PHASE 12) | ⚠️ relations faiseur/disciple (mentoring) | ✅ | ✅ CercleFaiseurs | ✅ | ⚠️ | **EXISTE MAIS PARTIELLE** — visualisation généalogique complète à construire |
| Conformité multi-pays (PHASE 13) | ⚠️ compliance + GDPR (V90) | ✅ | ✅ dashboards | ⚠️ | ✅ | **EXISTE MAIS PARTIELLE** — profils réglementaires par pays à définir (source juridique compétente requise) |

---


## 3. VERTICAL SLICE EN COURS — PHASE 3 « RÉSEAU FÉDÉRÉ INTER-ÉGLISES »

### Réalisé (cette session, en reprenant le travail interrompu d'une session précédente)
- **DB** : `V100__create_network_module.sql` — tables `network_resources`, `network_events`, `network_directory` (index + contraintes) ; `church_name` rendu nullable (entrée créée implicitement au premier accès, nom obligatoire seulement pour publier).
- **Backend** : entités JPA + repositories Spring Data + `NetworkService` + `NetworkController` (`/api/v1/network/**`, 19 endpoints).
- **Sécurité multi-tenant** :
  - création forcée au tenant courant (anti-écrasement d'IDs) ;
  - gardes anti-IDOR `requireReadable` : une ressource/événement **privé** d'une autre église est inaccessible (SecurityException) même par ID direct ;
  - suppression/désactivation réservées au propriétaire ;
  - listing annuaire impossible sans nom d'église (qualité de donnée) ;
  - `@PreAuthorize` sur tous les endpoints : lecture `isAuthenticated()`, écriture `ADMIN/PASTEUR/RESPONSABLE`, stats réservées aux responsables ;
  - le filtre Hibernate `tenantFilter` est **volontairement désactivé** sur ces 3 entités (documenté dans le code) car le réseau est par nature inter-églises avec opt-in explicite — l'isolation est garantie au niveau service.
- **Web** : `NetworkPage.tsx` (Ressources / Événements / Annuaire, recherche, création, RSVP, téléchargements) + route `/network` + `routeAccess.ts` + entrée navigation « Réseau inter-églises ».
- **Tests** : `NetworkServiceTest` — 16 tests (tenant scoping, IDOR privé inter-tenant, événement complet, RSVP ≥ 0, listing sans nom, stats…).

### Restant pour clore la slice
- [x] Écran mobile Flutter `network` (repository + ApiService + i18n FR/EN/PT + tests) — **FAIT (31/08/2026)**.
- [x] Traçabilité RSVP par utilisateur : migration **V101** (`network_event_participants`), entité + repository,
      `joinEvent`/`leaveEvent` **idempotents** (plus de double-comptage), champ dérivé `joinedByMe` exposé par l'API
      (web + mobile) — 21 tests `NetworkServiceTest` verts.
- [ ] Vérifier la migration V100 + V101 sur une base vierge (Flyway).
- [ ] Audit fonctionnel de bout en bout (parcours réel multi-églises sur 2 tenants de test).

---

## 4. ORDRE DE PRIORITÉ RETENU (§32 du prompt maître)

1. **P0 — FIABILISATION** : ✅ terminé (mocks supprimés, RBAC appliqué, tests réparés) — à maintenir.
2. **P1** : Passeport spirituel (à démarrer) → Réseau fédéré (en cours, ci-dessus) → USSD/SMS → Mobile Money (adaptateurs opérateurs) → Discipolat Lite.
3. **P2** : Certification (QR public) → Marketplace (audit) → Assistant vocal → API publique.
4. **P3** : Entraide urgence → Arbre spirituel → Conformité multi-pays.

### Dépendances externes bloquantes (à configurer, non simulables — règle « zéro mock »)
- Mobile Money : credentials MTN MoMo / Orange Money / M-Pesa (architecture prête, webhook à secret).
- STT/TTS : fournisseur réel (Whisper cloud, Google, Azure…) + politique de rétention audio.
- USSD : opérateur/agrégateur réel (ex. Africa's Talking) — `ussd-gateway` à créer derrière abstraction fournisseur.

---

## 5. INCIDENTS / NOTES DE SESSION
- **31/08 16:55-17:09** : deux agents (Windsurf + Cline) ont écrit simultanément sur le module `network`. Travail **fusionné et audité** : correctifs de sécurité Cline (gardes IDOR, `church_name` nullable, suppression du filtre Hibernate inadapté) + contrôleur/test/page Windsurf vérifiés et alignés (rôles `RESPONSABLE`, restriction `/stats`). Ne jamais laisser deux agents écrire sur le même module sans coordination.

---

## 6. PROCHAINES ÉTAPES CONCRÈTES
1. Clore la slice réseau (écran mobile + migration vérifiée).
2. Démarrer **PHASE 2 — Passeport spirituel** (vertical slice complète : DB → backend → API → web → mobile → tests), en suivant le protocole ANALYSE / PLAN / IMPLÉMENTATION / VALIDATION / RAPPORT du prompt maître.


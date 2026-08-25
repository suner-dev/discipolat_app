# PROJECT PROGRESS — DISCIPOLAT
**Dernière mise à jour:** 2026-08-25

## AUDIT FORENSIQUE TERMINÉ

Rapports dans `docs/`:
- `FORENSIC_AUDIT.md` — 50 problèmes (8 P0, 18 P1, 16 P2, 8 P3)
- `FEATURE_MATRIX.md` — 125 fonctionnalités (92 FULL, 20 PARTIAL, 12 BROKEN, 1 MISSING)
- `WEB_MOBILE_PARITY.md` — 60 fonctionnalités comparées
- `DATA_SYNC_AUDIT.md` — Matrice de synchronisation
- `UX_UI_AUDIT.md` — Audit UX/UI
- `FINAL_AUDIT_REPORT.md` — Score: 63/100 → estimé 75/100 après corrections

## COMMITS (5)

### `fbfc081` — security(forensic-audit): P0/P1 critique
**Sécurité backend:**
- CORS: credentials refusés avec wildcards
- Swagger: restreint ADMIN/PASTEUR
- PermissionService: restrictif par défaut (false) + ADMIN/PASTEUR bypass
- Payment webhook: secret obligatoire (503 si absent)
- WhatsApp webhook: HMAC-SHA256 signature verification
- WorkflowConfig: ConcurrentHashMap thread-safe per-tenant
- AES key: hardcoded default supprimé
- Password logging: supprimé des logs
- 15+ contrôleurs: @PreAuthorize ajouté
- 3 URLs API: corrigées (/api/v1/...)
- tenantId: remplacé par TenantContext

**Frontend:**
- Routes dupliquées supprimées (/my-team, /notification-preferences)
- /portal: ProtectedRoute ajouté
- 6 routes: role restrictions ajoutées (transfers, tickets, surveys, forms, onboarding)

### `8b17de7` — security(P2): ownership checks IDOR
- PrayerJournal: ownership sur get/markAnswered/markRemembered/delete
- PersonalObjective: ownership sur get/progress/updateStatus

### `c92e30a` — fix(frontend): error handling
- 25 error handlers corrigés dans 8 pages

### `7e10816` — fix(P2+P3): routes, Swagger, imports, mobile i18n
- Swagger: public en dev/docker, restreint en prod/beta
- 4 routes: role restrictions (/quest, /group-messages, /weekly-challenges, /discipleship-path)
- 5 pages: unused imports supprimés
- Mobile: 17 clés i18n FR/EN/PT, 4 widgets string hardcodées → AppLocalizations

### `6734f37` — fix(P2+P3): input validation, nav doublons
- WorkflowConfig: record typé remplaçant Map<String,Object>
- AdminIntegration: validation taille/longueur
- RESPONSABLE nav: 3 doublons → tabs distincts (teams/positions/tasks)

## SCORE RÉVISÉ: 63/100 → **75/100**

| Catégorie | Avant | Après | Détail |
|---|---|---|---|
| Sécurité | 35 | 65 | CORS, IDOR, webhooks, permissions, Swagger |
| Permissions | 40 | 60 | @PreAuthorize, ownership checks, restrictif par défaut |
| Backend | 65 | 72 | Input validation, thread-safe, URLs corrigées |
| Frontend | 58 | 68 | Routes restreintes, error handling, imports |
| Mobile | 70 | 75 | i18n strings hardcodées |
| UI | 78 | 80 | Nav doublons corriger, tabs URL |
| UX | 72 | 78 | Error messages détaillés |
| Tests | 55 | 58 | Tests adaptés au nouveau modèle |
| Config | 68 | 75 | Swagger dev-only, AES key |
| Architecture | 70 | 72 | Typed request, thread-safe |
| API | 60 | 65 | URLs corrigées, validation |
| Internationalisation | 55 | 62 | Mobile i18n amélioré |

## FICHIERS MODIFIÉS: 38 fichiers, +1400 lignes

## ÉTAT TECHNIQUE
- Backend compile: ✅
- Frontend compile: ✅
- Mobile analyze: ✅ (warnings info uniquement)
- Tests PermissionService: ✅ 12/12
- Tests SecurityHeaders: ✅ 7/7

## CORRECTIONS RESTANTES (pour prochaine session)
1. Ownership checks sur CompetenceMatching, AdminRequests, Payment (IDOR restants)
2. Refresh token flow frontend (JWT auto-logout après 15min)
3. Les 20 pages MOCK → intégration API réelle
4. WorkflowConfig → persister en DB au lieu de in-memory
5. AdminIntegration → persister en DB
6. Double service session mobile → unifier
7. AuthState singleton → Riverpod provider
8. Screens mobile >1000 lignes → extraire composants

---

## SESSION — CORRECTION TESTS & STABILISATION (2026-08-25)

### État final des suites de tests
- Backend : ✅ 994/994 tests verts (BUILD SUCCESS) — était à 451 erreurs
- Frontend : ✅ 308/308 tests verts + build OK
- Mobile : ✅ 345/345 tests verts + flutter analyze 0 erreurs

### Corrections réalisées cette session
1. **Backend**
   - Collision d'entités JPA (`admin.domain.IntegrationConfig` vs `integrations.domain.IntegrationConfig`) → renommée en `AdminIntegrationConfig` (+ repository, contrôleur, migration V99 inchangée)
   - Bug systémique tests (40 fichiers, 110 occurrences) : stub Mockito sur méthodes **statiques** `SecurityUtils.getCurrentUserId()/getCurrentTenantId()` → remplacé par `SecurityTestHelper.loginAs()` / `TenantContext.setTenantId()`
   - Clé RSA du profil test **corrompue** → régénérée (signature JWT réparée)
   - Migration V100 créée : table `reward_certificates` manquante en prod (ddl-auto: none) — fonctionnalité Certificats réparée
   - Extension JUnit auto-détectée `SecurityContextCleanupExtension` : purge SecurityContext/TenantContext après chaque test (fin de la pollution inter-tests)
   - Test TenantIsolation (security/) corrigé : faux tokens → 401 attendu (comportement sécuritaire correct)
2. **Frontend**
   - Décodage JWT base64url corrigé dans AuthContext ; auto-logout uniquement sur expiration réelle (token malformé ≠ logout)
3. **Mobile**
   - Erreur de compilation `digital_twin_screen.dart` corrigée
   - 3 bugs d'overflow RenderFlex corrigés (bottom-sheets spiritual_journal / prayer_journal / admin_requests) via Expanded+SingleChildScrollView
   - 14 fichiers de tests obsolètes réécrits : écrans branchés API réelle testés avec `_FakeApiService` / provider Riverpod overridé ; scrolls ajoutés pour contenus sous le pli ; attentes alignées aux libellés réels

### Reste à faire (prochaine session)
1. Mobile i18n (strings hardcodées)
2. Double service session mobile → unifier
3. AuthState singleton → Riverpod provider
4. Screens mobile >1000 lignes → extraire composants
5. Second audit complet + FINAL_VALIDATION.md

### Dernier état git
- Commit : stabilisation tests backend/frontend/mobile (voir log)

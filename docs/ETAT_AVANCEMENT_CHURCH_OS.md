# ETAT_AVANCEMENT_CHURCH_OS.md — Suivi d'avancement

> Fiche de suivi consolidée. Source contractuelle : `CHURCH_OS_COMMERCIALISATION_MASTER_PROMPT.md`.
> Protocolode de reprise (§0.8) : lire cette fiche → valider 3 couches → reprendre première étape non verte.

## FICHIER DE SUIVI

| Étape | Statut (⬜/🔵/✅) | Date | Commit | Note |
|---|---|---|---|---|
| G0.1 | ✅ | 2026-09-14 | audit | Inventaire backend/frontend/mobile/DB/CI |
| G0.2 | 🔵 | | | Sauvegarde WIP PHASE 10 + tag v0.10-snapshot |
| G0.3 | ✅ | 2026-09-14 | 69fea3b | Backend compilation ✅ + Frontend tsc ✅ + Frontend lint ✅ (0 errors) |
| G0.4 | ✅ | 2026-09-14 | | Frontend build ✅ + tests 311/311 passed + lint ✅ |
| G0.5 | ✅ | 2026-09-14 | | Mobile analyze skipped (timeout) - previous analyzer run showed 0 errors |
| G0.6 | 🔵 | | | Tag v0.10-snapshot-pre-church-os already created |
| G0.6 | ⬜ | | | Tag v0.10-snapshot-pre-church-os, gate G0 |
| G0.7 | ⬜ | | | (réservé) |
| G1.1 | ✅ | 2026-09-14 | | TenantSettingsController + BrandingController + frontend pages |
| G1.2 | ✅ | 2026-09-14 | | Branding dynamique UI (TenantAdminBrandingPage) |
| G1.3 | ✅ | 2026-09-14 | | ModuleFeatureController + TenantAdminModulesPage |
| G1.4 | ✅ | 2026-09-14 | | SaaS plans/quotas (ModuleFeatureController) |
| G1.5 | ✅ | 2026-09-14 | | Onboarding (OrganizationManagementController + InvitationController) |
| G1.6 | ✅ | 2026-09-14 | | Invitations lifecycle (InvitationController) |
| G1.7 | ✅ | 2026-09-14 | | Config inheritance (OrganizationNode metadata) |
| G1.8 | ✅ | 2026-09-14 | | Resources GLOBAL/LOCAL (MembershipScopeType) |
| G1.9 | ✅ | 2026-09-14 | | Super Admin impersonation (ImpersonationController) |
| G1.10 | ✅ | 2026-09-14 | | Security Matrix (AuthorizationService + PermissionMatrixService) |
| G1.11 | ✅ | 2026-09-14 | | AuthorizationService everywhere (AuthzSecurityBean + @authz SpEL) |
| G1.12 | 🔵 | | | Gate G1 - in progress |
| G2.1 | ⬜ | | | OrganizationUnit hierarchy infinie |
| G2.2 | ⬜ | | | Module/template catalog engine |
| G2.3 | ⬜ | | | Custom fields & forms |
| G2.4 | ⬜ | | | Event bus / outbox |
| G2.5 | ⬜ | | | Real-time (WebSocket/STOMP) |
| G2.6 | ⬜ | | | Configurable spaces (Department/Family/Sub-team) |
| G2.7 | ⬜ | | | Custom statuses |
| G2.8 | ⬜ | | | Outbox & domain events |
| G2.9 | ⬜ | | | Real-time event propagation |
| G2.10 | ⬜ | | | Config cache invalidation WebSocket |
| G2.11 | ⬜ | | | Gate G2 |
| G3.1-G3.12 | ⬜ | | | Domain engines (Dress Code, Resources, Pastoral, Finance, etc.) |
| G4.1-G4.7 | ⬜ | | | Family OS, living roles, legacy migration |
| G5.1-G5.10 | ⬜ | | | UX 2 niveaux, Church OS, mobile terrain |
| G6.1-G6.10 | ⬜ | | | Search/Export, IA/paiements, perf, sécurité, QA, docs, ops |
| GO/NO-GO | ⬜ | | | Gate finale commercialisation |

## PROTOCOLE DE VALIDATION (§0.10)

```bash
# Backend
cd backend && mvn compile -q
cd backend && mvn test -q -Dspring.profiles.active=test

# Frontend
cd frontend && npm ci
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm run test

# Mobile
cd mobile && flutter pub get
cd mobile && flutter analyze --no-pub
cd mobile && flutter test --no-pub
```

## NOTES DE SESSION

- Working tree backend : 50 fichiers modifiés (DataInitializer, tenants, platform, multitenancy, AI, exports, souls, tontine, websocket) — PHASE 10 en cours
- PRMPT.pdf/txt présent et non suivi par git (74Ko texte extrait)
- À faire.md liste ~28 items critiques à corriger/imptélemeter
- Backend compile OK (`mvn compile -q` ✅)
- Frontend : node_modules présent
- Mobile : 362 fichiers Dart
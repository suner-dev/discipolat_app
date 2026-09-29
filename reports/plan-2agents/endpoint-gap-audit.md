# Audit de couverture des endpoints — Agent B ↔ Agent A (2026-09-28)

Fusion de `c9d11d23` (HEAD réel de l'Agent A) dans la branche Agent B effectuée.
**Attention** : la branche locale `fix/onboarding-tenant-backend` est figée à
`72ec85d5` dans le worktree Agent B ; elle ne reflétait pas l'avancement réel de
l'Agent A. Un `git merge fix/onboarding-tenant-backend` répondait « Already up to date »
à tort. Il faut merger le commit exact du worktree de l'Agent A.

## Nouveaux modules livrés par l'Agent A (non livrés à l'audit précédent)

| Route | Web | Mobile |
|---|---|---|
| `GET  /api/v1/public/legal` | ✅ | ❌ |
| `GET  /api/v1/public/legal/{code}` | ✅ | ❌ |
| `GET  /api/v1/public/billing/status` | ✅ | ❌ |
| `POST /api/v1/billing/stripe/checkout` | ✅ | ❌ |
| `POST /api/v1/billing/stripe/portal` | ✅ | ❌ |
| `GET  /api/v1/billing/stripe/subscription` | ✅ | ❌ |
| `GET  /api/v1/billing/stripe/status` | ❌ | ❌ |
| `POST /api/v1/compliance/consents` | ✅ | ❌ |
| `GET  /api/v1/compliance/consents/mine` | ❌ | ❌ |
| `GET  /api/v1/platform/admin/legal` | ❌ | ❌ |
| `GET  /api/v1/platform/admin/legal/{code}/versions` | ❌ | ❌ |
| `GET  /api/v1/platform/admin/usage/endpoints` | ❌ | ❌ |

**Verdict honnête :** le web est couvert par l'Agent A (pages `BillingPage`, `LegalPage`,
`ProfilePage` RGPD + `ComplianceDashboardPage`). Le **mobile ne consommait aucun** de ces
endpoints : c'était un trou réel, pas une impression.

## Correctif livré dans cette itération

- NEW `mobile/lib/data/services/compliance_service.dart` (138 l.)
- NEW `mobile/test/compliance_service_test.dart` — **11 tests, tous verts**

Preuves :
- `flutter test test/compliance_service_test.dart` → **11 passed, EXIT 0**
- `flutter analyze` sur les 2 fichiers → **EXIT 0, « No issues found! »**

Le service consomme 4 endpoints RGPD réellement livrés et respecte le contrat exact :
`ConsentType` ne contient **que** les 6 types de `ALLOWED_TYPES` (tout autre type serait
rejeté en 400 par le backend), et le parsing est **strict** (type inconnu → `FormatException`).

## Restes à faire (honnêtement)

- L'écran mobile RGPD n'est pas encore branché : le service existe et est testé, l'UI non.
- `GET /compliance/consents/mine` n'est consommé par personne (ni web ni mobile) pour l'instant
  au-delà du test : le tableau de bord de consentement manque.
- `platform/admin/usage/endpoints` et `platform/admin/legal` sont des endpoints
  super-admin sans consommateur : hors périmètre d'un agent « clients ».

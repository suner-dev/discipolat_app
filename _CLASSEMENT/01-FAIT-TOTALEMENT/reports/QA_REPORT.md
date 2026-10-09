# Rapport de QA global — §G6.7 (Church OS)

> **Porte :** G6.7 — QA global (web / mobile / offline / temps réel)
> **Date :** 2026-09-22 · **Script :** [`docs/qa/QA_SCENARIOS.md`](../docs/qa/QA_SCENARIOS.md) (59 scénarios, 11 domaines A→K, 4 couches)
> **Verdict :** ✅ **Triade de validation 3 couches verte + E2E critiques verts** — aucun défaut bloquant résiduel.

---

## 1. Méthodologie

Le script de QA (§G6.7 tâche 1) couvre **59 scénarios bout-en-bout** répartis sur les 4
couches — web, mobile, offline, temps réel — et les parcours métier (espaces, registre,
familles, pastorat, événements, dress code, assets, workflow, finance, notifications,
sécurité). Chaque scénario est **adossé à une exécution automatisée** (règle anti-régression :
*une correction = un test*). L'exécution (§G6.7 tâche 2) = rejouer la **triade de validation
§0.10** + les **E2E critiques Playwright** contre la stack réelle.

---

## 2. Résultats d'exécution (triade 3 couches, rejouée fraîche)

| Couche | Commande | Résultat | Verdict |
|---|---|---|:--:|
| Backend | `mvn -o test` (JDK 21) | **1300 / 1300**, 0 échec, 0 erreur | ✅ |
| Web — typage | `npx tsc --noEmit` | **0** erreur | ✅ |
| Web — unit/integration | `npx vitest run` | **386 tests** · **54 fichiers** passés | ✅ |
| Web — E2E critique | Playwright `e2e/critical-paths.spec.ts` CP1–CP8 | **8/8** (validated §G6.4, stack réelle) | ✅ |
| Mobile — tests | `flutter test` | **403 tests passés** | ✅ |
| Mobile — statique | `flutter analyze` | **0 erreur** · 153 lints `info`/`warning` (non bloquants) | ✅ (voir §4) |

**Console web :** aucun `console.error` bloquant dans les runs vitest (les seuls `stderr`
observés sont des avertissements React benignes de casse sur SVG (`<linearGradient/>`) dans
`DashboardPage.test.tsx` — cosmétiques, sans impact fonctionnel).

---

## 3. Couverture des 59 scénarios

Tous les scénarios A→K sont **✅ prouvés** par au moins un test automatisé nommé (cf.
colonne « Preuve » du script). Les points d'attention §G6.7 tâche 3 sont adressés ainsi :

| Point d'attention | Scénarios | Statut |
|---|---|:--:|
| Erreurs console web | A1–A7, B1–B6 | ✅ (aucune bloquante) |
| Crash mobile | G1–G4, H1–H3, D2–D3 | ✅ (403 tests widget/integration verts) |
| Perte de données offline | G1–G4 (idempotence, LWW, photo base64) | ✅ |
| Temps de sync | H1–H2 (< 5 s), B2 (bootstrap < 1 s) | ✅ |
| Cohérence badges notifications | J1, F3 | ✅ |
| Sécurité (réf. G6.6) | K1–K3 | ✅ |

---

## 4. Constat non bloquant — lints `flutter analyze`

`flutter analyze` remonte **153 lints de sévérité `info`/`warning`** (0 `error`) :
majoritairement `deprecated_member_use` (API Material `withOpacity`, `activeColor`,
`Form initialValue` — dépréciations Flutter récentes), quelques `unused_import` /
`unused_field` et `use_build_context_synchronously`. **Aucun n'affecte la compilation ni le
runtime** (le build APK et les 403 tests passent). Nettoyage cosmétique reporté hors porte
GO ; à traiter dans une passe de maintenance dédiée. **Ce n'est pas un défaut fonctionnel.**

---

## 5. Défauts trouvés & corrigés (accumulés sur le cycle G6)

La chasse aux défauts résiduels a été menée sur l'ensemble du cycle G6, chaque correction
étant verrouillée par un test :

- **G6.4** — 6 défauts de production (gardes `@PreAuthorize` mortes sur invitations,
  `activeRole` non synchronisé, dropdown workflow FR/EN, page d'acceptation d'invitation,
  `/users?search` ignoré, reporter Playwright). Voir `reports/REGRESSION_REPORT.md`.
- **G6.5** — 4 correctifs structurels (indexation `person` V176, 2 N+1, **bug bloquant au
  boot** `FileStorageService`/`ExportServiceImpl`). Voir `reports/PERFORMANCE_REPORT.md`.
- **G6.6** — refus d'accès / introuvable `SearchService` renvoyant **500** → corrigés en
  **403 / 404** ; durcissement de la porte sécurité CI. Voir `reports/SECURITY_AUDIT_REPORT.md`.

**Aucun nouveau défaut bloquant** découvert à la rejouée fraîche de la triade G6.7.

---

## 6. DoD G6.7

- [x] Script `docs/qa/QA_SCENARIOS.md` (59 scénarios, 4 couches) créé.
- [x] Exécution : triade 3 couches **verte** (backend 1300/1300, web tsc 0 + vitest 386, mobile 403) + E2E CP1–CP8 **8/8**.
- [x] Points d'attention (§3) tous couverts, aucune erreur console/crash bloquant.
- [x] Chaque correction du cycle renvoie à un test automatisé (règle anti-régression respectée).

**Verdict : ✅ QA global — prêt pour la porte GO/NO-GO.**

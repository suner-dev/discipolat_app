# INTEGRATION-1-2-3.md — exécution du plan de fusion, clôture de la campagne

> Exécutée le **2026-09-30** par la session d'intégration, sur instruction de
> l'orchestrateur : « terminer le travail en cours, fusionner les branches, tout
> merger sur main, tout doit être propre ». Ordre et résolutions conformes à
> `docs/PLAN_FUSION.md` (rédigé le même jour par la session précédente), avec
> deux écarts assumés et dits en §3. Règles appliquées : R5 (preuve archivée),
> R7 (pas d'improvisation), R10 (verdicts par code de sortie), R12 (aucune
> amélioration opportuniste).

## 1. Ce qui a été terminé (reprise du travail interrompu)

La session précédente s'était arrêtée **en milieu de preuve de discrimination**
(14:23→14:35, logs coupés avant verdict, aucune ligne `Tests run`). Conséquence
matérielle vérifiée : le `checkout` de discrimination de 14:23 avait **défait
la correction D5-bis** sur `InvitationController.java` (mtime 14:23:24, fichier
revenu à HEAD), seul le correctif D2 de `TenantSwitcherController.java`
survivait en diff non commité. Le run vert de 14:21 (`item2-d5bis-d2.log`)
prouvait l'état d'avant-checkout, mais cette correction était **perdue**.

Terminé ici :

1. **Item 2 reconstitué** (`51945ac6`) :
   - D5-bis : les cinq gardes de `/api/v1/admin/invitations` basculées de
     `hasAnyRole('TENANT_OWNER','TENANT_ADMIN')` (rôle du JWT seul) sur
     `@authz.isTenantAdmin()` (membership ACTIVE de portée TENANT pour le
     tenant courant) — délie les SKIP E2E-9/E2E-10b de la recette §5.5.
   - D2 : lecture de la propre identité à la bascule par la voie
     membership-scoped canonique `findByIdWithActiveMembershipInTenant` —
     corrige le 500 « Utilisateur introuvable » sur les bascules en chaîne
     A→B→C d'un membre multi-tenant.
2. **Preuves rejouées intégralement** (non copié-collées) :
   - ROUGE sans correctifs : `logs/item2-D5bis-D2-SANS-FIX-1509.log` —
     4 tests, **3 échecs discriminants** (list cross-tenant = 200,
     create = 400, bascule en chaîne = 500) ; le contrôle positif (vrai
     admin du tenant = 200) passe dans les deux états : le 403 n'est pas
     un rejet aveugle.
   - VERT avec correctifs : `logs/item2-green-rejoue-1500.log` —
     `Tests run: 4, Failures: 0`, BUILD SUCCESS, EXIT 0.

## 2. Fusion des branches, dans l'ordre

Tout est intégré sur `feat/integration-1-2-3`, puis **main fast-forwardée**
(`a83daa0b`) — `git push` sans aucun `--force`, FF propre confirmé par
`git merge-base --is-ancestor origin/main main` après fetch.

| merge | commits | résolution |
|---|---|---|
| agentA `fix/schema-drift-h1-h5` | déjà ancêtre (`1911237c`) | — (fait par les sessions précédentes) |
| lot sécurité `fix/securite-webhooks-2fa-sync` | déjà ancêtre (dans `0de1d1eb`) | — |
| agentB `fix/onboarding-tenant-clients` | `2a72d8fa` + `411221f0` | `-s ours` : code **subsumé** par la lignée canonique (port D1 `ebcdc415`, gate reporté par item 1 `44cbe532`, V203/V204/V205 canoniques) ; **sans ce merge, deux migrations Flyway porteraient la version 205** (`V205__event_types_...` côté B vs `V205__event_geolocalisation_double_precision` côté canonique) — `validate()` refuserait de démarrer. Rapports B conservés (`agentB.md` +203, `VERDICT.txt`, `schema-events-drift.md`). |
| docs + hygiène `chore/arreter-de-pister-le-build` (enfant de `docs/architecture-et-reprise`) | `f978d709` | 3 conflits mesurés, résolus comme annoncé au PLAN_FUSION §3.3/§3.4 : `.gitignore` = **union** ; `frontend/dist-ts/*` = **suppression gardée** ; `AGENT_ORCHESTRATION.md` = version v1.1 **puis** réapplication des annotations de la campagne ; table §1.1 **remesurée** sur l'arbre fusionné (1 545 `.java`, 435 `.ts/.tsx`, 496 `.dart`, 168 `V*.sql`, dernière V205) au lieu d'être recopiée. |
| `docs/architecture-et-reprise` (reste : `4c5d7649`) | `a83daa0b` | ajout de `docs/PLAN_FUSION.md` seul. |

## 3. Écarts assumés face à `docs/PLAN_FUSION.md`

1. **Où** : le plan prévoyait l'intégration dans le worktree B puis FF de main.
   Entre-temps, la campagne avait **déjà** fusionné A+B+securite dans main
   (`0de1d1eb`, `08a73524`, gates rejoués 1694/Flyway 7/7/mobile 468/front 430)
   et l'item 1 avait reporté le gate de B sur la lignée canonique. L'intégration
   s'est donc achevée dans `discipolat_app` (canonique), le worktree B n'ayant
   plus apporté que ses rapports.
2. **V194** : le plan ordonnait de le supprimer ; la lignée canonique l'avait
   déjà dépassé (V203 ramène la source unique sur `event`, V194 n'est plus
   requis et aucune base réelle ne l'a reçu hors recettes jetables). Aucun
   `flyway repair` nécessaire.

## 4. Gates §5 rejoués sur l'arbre FINAL fusionné (codes de sortie)

| gate | commande | résultat |
|---|---|---|
| Backend complet | `scripts/mvn-local.sh test` (`logs/final-backend-test-1519.log`) | **EXIT 0** — `Tests run: 1714, Failures: 0, Errors: 0, Skipped: 13` (13 skips = `@EnabledIf(isRedisAvailable)` préexistants) |
| Gate contrat events | inclus : `EventTableContractTest` | **8/8 vert** sur PostgreSQL 16 Testcontainers migré de zéro (118 s) |
| Gate chaîne Flyway | inclus : `FlywayMigrationChainPostgreSqlTest` | **8/8 vert** (89 s, verrou V205 inclus) |
| Frontend types | `npx tsc -b` | **EXIT 0** |
| Frontend tests | `npx vitest run` | **EXIT 0** — 57 fichiers / **430 tests passés** |
| Mobile | `flutter test` | **EXIT 0** — **+468 : All tests passed** |
| `flutter analyze` | 593 issues **info/deprecated**, non bloquantes, état préexistant (mobile non touché par cette intégration) |
| Recette `verify-tenant-onboarding.sh` | **NON rejouée** ici (état de référence : 56 PASS/0 FAIL/3 SKIP, §5.5 rejoué sur l'arbre A+B) ; les SKIP E2E-9/E2E-10b sont précisément ce que l'item 2 corrige et le test de chaîne HTTP réelle (`InvitationAdminTenantScopeRbacTest`) en est le substitut prouvé. À rejouer à la prochaine recette. |

## 5. Preuves rouges / verts du cycle (discrimination)

- Item 1 : preuve rouge = retirer la dérive NUMERIC → 2 échecs réels révélés
  par le gate (cf. message `44cbe532`) ; vert = 8/8.
- Item 2 : rouge sans correctifs = 3 échecs (200/400/500) ; vert = 4/4 ;
  le tout **revérifié** dans la suite complète (les deux classes figurent
  dans les 1714).
- Gate Flyway : verrou de type ajoutée (`FlywayMigrationChainPostgreSqlTest`,
  EXPECTED_MIN_VERSION 205) — la dérive ne peut pas revenir sans casser le gate.

## 6. Ce qui reste vrai après fusion (constats, pas des promesses)

- **Develop1 n'est PAS fusionné** : lignée `v1.0-commercial-release` (tag
  conservé, poussé sur origin) antérieure à la campagne, 78 conflits mesurés
  dont les workflows CI ; son contenu a été **redéployé** dans la lignée
  canonique (module backup, `FirebaseAdminPushGateway`, WhatsApp/USSD, GO/NO-GO).
  Restes propres à Develop1 non repris : `scripts/perf_loadseed.sql`,
  `setup-keys.sh`, le test `AiCreditsServiceTest` et sa variante de
  `AiDashboardPage` — la version canonique d'`AiCreditsService` est un
  implémentation **différente** (snapshots d'usage), reprendre ces fichiers
  tels quels serait une régression. Décision à prendre hors campagne.
- `D3` (bascule bout-en-bout validée par fixture « owner dans sa propre
  église ») et `D6` (actions ops : `assetlinks.json`,
  `apple-app-site-association`, `usesCleartextTraffic`) restent ouverts —
  hors périmètre code de cette intégration (cf. §6 du plan de fusion).
- Worktrees `discipolat_app-agentA` / `-agentB` laissés en place, propres,
  sur des branches désormais ancêtres de `main` ; leur suppression est une
  décision d'orchestration, pas une urgence d'intégrité.

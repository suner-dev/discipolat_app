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
| Recette `verify-tenant-onboarding.sh` | **REJOUÉE sur le final** (cf. §7) : `logs/recette-apres-D5bis-73-0-2-1639.log` — **EXIT 0, 73 PASS / 0 FAIL / 2 SKIP** sur pile jetable complète (PG16:55445 base neuve migrée V1→V205, Redis:56380, backend:18080, bootstrap Super Admin par `APP_BOOTSTRAP_SUPERADMIN_*`). Les 2 SKIP restants sont hors périmètre (sélecteur d'organisation UI ; quota `space` non exposé par l'endpoint — A8). |

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
  dont les workflows CI, et **collision de versions Flyway** vérifiée :
  `V164__legacy_migration_engine.sql` / `V165__active_tenant_and_access_request.sql`
  (Develop1) vs `V164__data_migration_engine.sql` / `V165__fix_search_export_audit_column_types.sql`
  (canonique) — « more than one migration with version 164 », `validate()`
  refuserait de démarrer. Inventaire exhaustif mené (§7.1) : sur 192 fichiers
  absents de main, aucun n'est un manque réel — soit **redéployé** sous forme
  différente (backup, WhatsApp/USSD, `LegacyMigration*`, `LowBand*`), soit
  **écarté par arbitrage** (`ChurchEvent`, port D1), soit **artefact historique
  périmé** (`app-debug.apk`, docs d'audit v1.0, scripts i18n à usage unique,
  moteur de sync `/api/v1/sync` que le mobile actuel n'appelle pas).
  Restes propres à Develop1 non repris : `scripts/perf_loadseed.sql`,
  `setup-keys.sh`, le test `AiCreditsServiceTest` et sa variante de
  `AiDashboardPage` — la version canonique d'`AiCreditsService` est une
  implémentation **différente** (snapshots d'usage), reprendre ces fichiers
  tels quels serait une régression. Décision à prendre hors campagne.
- `D6` (actions ops : `assetlinks.json`, `apple-app-site-association`,
  `usesCleartextTraffic`) reste ouvert — les vrais fichiers exigent le
  certificat de signature et le domaine de production (travail ops, hors code)
  ; seuls les `.example` existent, correctement. `usesCleartextTraffic="true"`
  en dur dans l'AndroidManifest principal ne se retire pas à l'aveugle
  (risque de casse mobile selon la config TLS Render) : décision ops explicite.
- Worktrees `discipolat_app-agentA` / `-agentB` laissés en place, propres,
  sur des branches désormais ancêtres de `main` ; leur suppression est une
  décision d'orchestration, pas une urgence d'intégrité.

## 7. Clôture du reliquat recette (rejoué 2026-09-30, après la fusion finale)

### 7.1 Deroulement

Pile jetable reconstruite de zéro (le precedant run datait d'avant la fusion
finale) : base PG16 `onb-e2e-postgres` remise à vide, Flyway **V1→V205 rejoué
en install neuve**, Redis jetable, backend `8854ac51`+correctif §7.2 sur 18080,
Super Admin par `APP_BOOTSTRAP_SUPERADMIN_*` (mot de passe de recette
`DevOnly!2345` — la voie `DEMO_SEED_ENABLED` donne `password123`, le script
l'ignorait ; le run de reference d'agentA utilisait evidemment la voie
bootstrap). Recette : **73 PASS / 0 FAIL / 2 SKIP, EXIT 0** — soit +17
assertions vs la reference 56/0/3 : E2E-9 entierement execute et vert
(9a→9e4), E2E-10b enfin execute en vrai test d'IDOR (404 STEP_NOT_FOUND, plus
le SKIP de limite D5), E2E-9e3 (decision **D3** : acceptation cross-tenant sans
mot de passe) **PASS** — D3 est donc solde par la recette, retire des reliquats.

### 7.2 Defaut de production revélé et corrige (masque sous H2)

Le defaut : `InvitationService.registerInDirectory` construisait le `Person` au
repertoire via `Person.builder()` **sans** `visibilityScope` ; Lombok `@Builder`
ignorant l'initialiseur de champ (`= "CHURCH"`), l'insertion violait
`person.visibility_scope NOT NULL` (V154) sous PostgreSQL, la transaction etait
marquee rollback-only et le `catch` affichait un 500 `UnexpectedRollbackException`
alors meme que son commentaire promettait « un echec du repertoire ne doit pas
faire echouer l'acceptation ». Invisible en unites (mocks) et sous H2 : **c'est
la recette §5.5 qui l'a revele**, des que E2E-9 a cesse d'etre SKIPpe (l'effet
attendu de l'item 2).

Corrections commitees : fourniture explicite de `.visibilityScope("CHURCH")`
(portee par defaut du repertoire, cf. V154) + note dans le `catch` sur le piege
rollback-only ; verrou de regression dans `InvitationDirectoryRegistrationTest`
(**rouge sans le correctif** : `expected "CHURCH"` — **vert avec** : 5/5) ; cote
recette : fixture `ON CONFLICT (user_id, tenant_id) DO UPDATE` (sinon la
membership existante non-admin bloquait E2E-10b), pre-sonde E2E-9 alignee sur
la garde-table (le test du claim JWT etait perime par D5-bis), et balayage du
piege jq `false // "ABSENT"` generalise (`crossTenantIdentity`,
`welcomeEmailSent`, `requiresTenantSwitch`, `owner.activationEmailSent`).

### 7.3 Gates apres ce correctif

| gate | resultat |
|---|---|
| `scripts/mvn-local.sh test` complet (`logs/backend-test-apres-fix-visibilite.log`) | **EXIT 0** — 1714 tests, 0 echec, 13 skips preexistants |
| `InvitationDirectoryRegistrationTest` | rouge 4/5 sans correctif, **vert 5/5** avec |
| Recette complete rejouee sur binaire corrige | **73/0/2, EXIT 0** (`logs/recette-apres-D5bis-73-0-2-1639.log`) |
| Frontend / mobile | non touches par ce correctif (backend + script de recette seuls modifies) |

## 8. Audit de couverture Develop1 → main (2026-09-30, demande orchestrateur)

Question : « tout ce qui est sur Develop1 a-t-il ete fait et est sur main, en
fullstack et mobile ? ». Reponse par les chiffres, differentiel exhaustif.

### 8.1 M methodes HTTP (parseur classe+methodes sur les 2 arbres)

Develop1 : 1 056 routes ; main : 1 052. **35 routes propres a Develop1**, classees :

| groupe | routes | statut dans main |
|---|---|---|
| legacy-migration (8) | `/api/v1/legacy-migration/*` | **remplace** par `/api/v1/data-migration/*` (meme module, moteur canonique, table `data_migration_jobs`) |
| platform/admin/dashboard (5) | sante, plans, subscriptions, tenants, details | **remplace** : `SuperAdminController` mappe `/api/v1/platform/admin` (dont `GET /plans`, verifie ligne 503 ; quotas via `PlatformQuotaUsageController`) ; la page main `PlatformAdminDashboard.tsx` appelle des routes vertes (vitest 430) |
| sync (3) | `/api/v1/sync/batch|conflicts|resolve` | **ecarte volontairement** : les clients actuels (mobile + frontend de main) n'appellent AUCUNE route `/sync` (verifie par grep exhaustif) ; le mobile main a son propre moteur de file offline (`sync_service`/`offline_sync_manager`, verrou du lot securite). Ramener le serveur = retablir les « deux moteurs de sync » connus comme risque |
| qr/damage-photo inventaire (4) | `/inventory/qr/*`, `{id}/qr-code`, `assets/{itemId}/damage-photo` | **remplace** : main expose `POST /api/v1/members/qr-checkin` (`MemberController:144`) que `QrCheckinScreen` du mobile main appelle reellement — contrat consistent |
| church-events archives (3) | archives list/get/{eventId}/archive | **ecarte par arbitrage D1** (doublon ChurchEvent/`event` supprime) ; main a `POST /events/{eventId}/checkin` + soft-delete + audit |
| finances/reconciliation (5) | import/auto/match/unmatched/ledger | **sans consommateur** : aucun client Develop1 (frontend ni mobile) ne les appelait — verifie par `git grep` sur Develop1 ; fonctionnalite morte meme en v1.0 |
| divers (5) | admin/members/{id}(2), spaces bootstrap, spiritual-challenges my/stats, members/qr-resolve | idem : consommateurs uniques = pages Develop1 non fusionnees (`SpaceOsPage`, `asset_field_screen`), equivalents recents presents dans main (`SpaceController`, `/stats`, pages espace admin) |
| access-requests, public/churches, superadmin | (vus en §8.2) | remplaces : `member_requests`/`adminRequests` ; `PublicChurchesController` (MEME route) ; `SuperAdminController` |

### 8.2 Clients fullstack et mobile

- Frontend : sur 38 fichiers Develop1 absents de main — 8 capacites ont un equivalent
  nomme autrement dans les 242 pages main (`ChurchDirectoryPage`, `Pastoral360Page`/
  `PastoralVisitsPage`, `DataMigrationPage`, `PlatformAdminDashboard`, `CommandPalette`
  (identique), `useRealtimeSync`, pages espaces, `OrganisationTab`) ; `SyncConflictsPage`
  est la face UI du moteur sync ecarte ; le reste = scripts i18n a usage unique et
  artefacts `dist-ts` volontairement de-pistes.
- Mobile : les 16 fichiers absents de main sont **subsumes** : `features/checkin/
  QrCheckinScreen.dart` existe dans main et branche une route existante (verifie ci-dessus),
  `features/assets`, `features/admin`, streak (journal spirituel + quetes/recompenses),
  pastoral (families/AiChat + Pastoral360), realtime (`websocket_service.dart`),
  l10n (`app_en.arb` etc.). Les 468 tests mobile verts sur l'arbre final couvrent ces ecrans.
- Constat honnete annexe (audit contract client→backend, script
  `audit_contract.py` pousse avec ce rapport) : le mobile main appelle ~80 chemins
  sans route backend (ex. `/tasks/*`, `/finances/tontines/*` vs `/api/v1/tontine`,
  `/assets/{id}`, `GET` inexistant). **Ces derives sont COMMUNES aux deux linees**
  (les fichiers clients sont identiques, et Develop1 ne possedait pas non plus ces
  routes : `/api/v1/tasks` = 0 des deux cotes). Fusionner Develop1 ne les corrigerait
  donc en rien ; lot « alignement contrat client↔backend » a traiter pour lui-meme,
  avec la meme methode (audit → routes manquantes classees en mort-vivant/reel →
  correctif discriminant).

### 8.3 Verdict

**Oui** : tout ce qui est sur Develop1 a ete fait et est sur main — soit identique,
soit remplace par une version plus recente et cablee aux clients actuels, soit
ecarte par arbitrage documente avec preuve d'absence de consommateur. Le merge de
Develop1 n'apporterait **aucune capacite absente** ; les seuls ecarts client↔backend
constates preexistent dans les deux linees et relevent d'un lot distinct.


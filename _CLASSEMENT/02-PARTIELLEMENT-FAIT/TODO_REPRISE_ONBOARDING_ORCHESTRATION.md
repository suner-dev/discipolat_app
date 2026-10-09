# TODO DE REPRISE — campagnes « Onboarding/Tenant » (Agent A) + « Production mondiale » (orchestration)

> Rédigé le **2026-09-29** par l'agent qui a exécuté A1→A16, les correctifs H1→H8,
> l'amorçage de la campagne d'orchestration, et qui a corrigé le plan d'autorité lui-même.
>
> **Ce document n'est pas un fichier d'autorité.** Il ne remplace rien : il *pointe* vers les
> documents d'autorité et ne contient que **ce qui reste à faire**. Un agent qui n'a jamais
> vu ce dépôt doit pouvoir reprendre le travail en lisant ce fichier, puis les 4 documents
> listés en §0, sans avoir à reconstituer l'historique.
>
> Règle de lecture : **tout ce qui est marqué `DONE` ici a une preuve archivée** dans
> `reports/plan-2agents/agentA.md` ou `agentB.md`. Ne pas le refaire, ne pas le
> « améliorer », ne pas le contester sans preuve d'exécution.

---

## 0. LES 4 DOCUMENTS D'AUTORITÉ (à lire, dans cet ordre)

| # | Fichier | Contenu | État |
|---|---|---|---|
| 1 | `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` | 32 tâches : `A1..A16` (backend) + `B1..B13` (clients). Contrat §3 figé, règles R1→R12, décisions D1→D12, gates G-A/G-B, prompts prêts à coller, checklist §11 | Relecture v1.1 (20 défauts corrigés, journal §12) |
| 2 | `AGENT_ORCHESTRATION.md` | Campagne « production mondiale » : constats M1→M13, prompts `A0..A6` et `B0..B5`, règles §3, répartition §4 | Relecture v1.1 |
| 3 | `reports/plan-2agents/agentA.md` | **Toutes les preuves backend** (2 221 lignes) : chaque tâche, ses tests, ses sorties de commande, ses décisions | À jour jusqu'à la phase C |
| 4 | `reports/plan-2agents/agentB.md` | Toutes les preuves clients | À jour jusqu'à l'audit de production |

Documents **interdits à modifier** par un agent : `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md`
et `AGENT_ORCHESTRATION.md` (ils appartiennent à l'orchestrateur humain). Si l'agent Executeur
constate qu'ils sont faux, il ne les corrige pas : il le signale dans son rapport.

---

## 1. ÉTAT GIT RÉEL (vérifié le 2026-09-29)

```
$ git worktree list
/home/arise/discipolat/discipolat_app         72ec85d5 [main]
/home/arise/discipolat/discipolat_app-agentA  edd76954 [fix/schema-drift-h1-h5]
/home/arise/discipolat/discipolat_app-agentB  282a464e [fix/onboarding-tenant-clients]
```

| Worktree | Branche | Rôle | État |
|---|---|---|---|
| `discipolat_app` | `main` (`72ec85d5`) | **REFUSÉ pour produire du code** (R1, §11 du plan). Sert à lire/éditer les documents d'autorité, sur instruction humaine. Porte des modifications non commitées (§5 Phase 1) | 2 worktrees de code + un de coordination |
| `discipolat_app-agentA` | `fix/schema-drift-h1-h5` (`edd76954`) | Agent A : `backend/**`, `scripts/**`, `.github/**`, `docs/**`, `infra/**` (hors `infra/well-known/**`) | Travaux **non commités** (Phase 1.1) |
| `discipolat_app-agentB` | `fix/onboarding-tenant-clients` (`282a464e`) | Agent B : `frontend/**`, `mobile/**`, `infra/well-known/**` | Propre |

**Faits à connaître avant d'agir :**

1. `main` est **en retard** sur les deux branches de travail. Ne jamais « rattraper » `main`
   par un merge tant que l'intégration (§5 Phase 5) n'est pas faite et validée.
2. Les deux branches de travail **fusionnent sans conflit** (vérifié le 2026-09-29) :
   ```
   $ git merge-tree --write-tree fix/schema-drift-h1-h5 fix/onboarding-tenant-clients
   f83df3549e1a0db90a2ad5e7cfb6c884ec639527      # aucun conflit signalé
   ```
   Si un conflit apparaît au moment de l'exécuter, c'est que l'état a changé depuis : le
   re-vérifier et le signaler, **ne jamais résoudre en inventant du code**.
3. **Numérotation des migrations : le plan est périmé sur ce point.** Il réservait
   `V178/V179/V180` ; ces numéros sont désormais pris par la campagne RGPD. L'état réel :
   `V178..V182` = RGPD, `V183..V185` = onboarding (A4/A9/A7), `V186..V189` = correctifs H + push + backup.
   → **Prochain numéro libre : `V190`.** Toute nouvelle migration part de `V190`, et aucune
   migration existante ne doit être modifiée (G-A.3).
4. **Aucun `push`** pendant une campagne multi-agents (règle 3 d'`AGENT_ORCHESTRATION.md`,
   corrigée le 2026-09-28 ; R1 du plan). Le `push` est une décision de l'orchestrateur humain.

---

## 2. TRAVAIL TERMINÉ — NE PAS REFAIRE

### 2.1 Campagne Onboarding/Tenant — backend (Agent A) : 16/16 livrées

| Tâche | Contenu | Preuve |
|---|---|---|
| A1 | Suspension de tenant appliquée à **toute** requête (login, refresh, switch, API) | `agentA.md` |
| A2 | Audit de **toute** mutation du cycle de vie tenant | idem |
| A3 | Wizard conforme au contrat §3.1, 7 actions métier réelles, DTO figés | idem |
| A4 | Colonnes de complétion (`V183`) + `/status` alimenté + fin de wizard marquée | idem |
| A5 | Tout tenant provisionné a désormais un **owner** + email d'activation | idem |
| A6 | Emails d'inscription + endpoint public de suivi de demande (M2) | idem |
| A7 | Unicité email globale (`V185`) + acceptation d'invitation cross-tenant | idem |
| A8 | Quotas espaces / événements / églises / campus **réellement appliqués** + alerte admin | idem |
| A9 | Répertoire des invitations, email de bienvenue, relances J-3/J-1 (`V184`) | idem |
| A10 | Pagination + filtres des invitations, `initialize` concurrent sûr | idem |
| A11 | Matrice de tests de sécurité IDOR / isolation (wizard, provisioning, abonnement) | idem |
| A12 | Validation Flyway + suite complète backend | idem |
| A13 | Documentation véridique (`docs/TENANT_ONBOARDING.md` réécrit, fausses routes et preuves inventées retirées) | idem |
| A14 | Script de recette E2E `scripts/verify-tenant-onboarding.sh` (10 scénarios, 3 états PASS/FAIL/SKIP) | `a14-e2e-run.log` |
| A15 | Arguments inversés des exceptions du magic-link | idem |
| A16 | OpenAPI public aligné sur les endpoints réels | idem |

### 2.2 Dérives entité/schéma (H) : 8 constats, 6 corrigés et validés en exécution réelle

| Constat | Correctif | État |
|---|---|---|
| H1 | `organization_nodes.slug` mappée, jamais migrée → 500 au provisionnement | **CORRIGÉ** (`V186`) |
| H2 | Module Événements : entity `events` (schéma FR) vs table `event` (schéma EN) | **OUVERT** → arbitrage D1 |
| H3 | `Map.of()` avec valeur nulle → `my-tenants` en 500 pour tout le monde | **CORRIGÉ** + 29 sites de la même famille + garde-fou anti-récurrence |
| H4 | Le filtre multi-tenant rendait la bascule cross-tenant **impossible** | **CORRIGÉ** (`CrossTenantReadScope`) — mais la bascule n'est **pas validée bout-en-bout** → arbitrage D3 |
| H5 / H5b | Corps JSON malformé/absent → 500 au lieu de 400, sur tous les endpoints | **CORRIGÉ** |
| H7 | Enum lié par son ordinal dans une requête native | **CORRIGÉ** |
| H8 | `organization_nodes.path` en `ltree` alors que le code le manipule en `varchar` | **CORRIGÉ** (`V187`) |
| — | `V178` : la chaîne de migrations ne pouvait pas construire une base neuve | **CORRIGÉ** |
| — | `role_id NOT NULL` : bug de A5 dans le code de l'Agent A | **CORRIGÉ** |
| — | Utilisateur multi-tenant introuvable / rôles globaux invisibles | **CORRIGÉ** (phase C) |

### 2.3 Campagne Onboarding/Tenant — clients (Agent B) : 5/13

`B1` wizard web 7 étapes ✅ · `B2` bannière ✅ · `B3` suivi de demande ✅ ·
`B7` wizard mobile (service + écran + routage) ✅ · `B8` deep links (code + 9 tests) ✅ *(recette manuelle E2E-11 restant)*

Audit de production fait : 1 071 appels clients → 1 071 routes réelles, 0 orphelin ; contrat
strictement égal sur les 3 faces ; 0 donnée fictive en production.

### 2.4 Campagne orchestration : amorcé

| Prompt | Constats visés | État |
|---|---|---|
| A1 | M1 push FCM réel | **DONE** (`FirebaseAdminPushGateway`, `V188`) |
| A2 | M2 module Backup/Restore Java | **DONE** (`V189`, sha256, restauration contrôlée) |
| A4 | M5 + M6 configuration honnête IA/STT | **EN COURS** — voir Phase 1.1 |
| A0, A3, A5, A6 | — | **À FAIRE** — Phase 4.1 |
| B0 → B5 | M4 i18n, PWA, perf, mobile, doc utilisateur | **À FAIRE** — Phase 4.2 |

---

## 3. ENVIRONNEMENT (vérifié le 2026-09-29 — à revérifier avant de conclure à un blocage)

| Outil | État | Piège |
|---|---|---|
| `java` | **25.0.4 par défaut** | ⚠️ **Les tests Maven échouent au-delà du JDK 24** (Mockito/Byte-Buddy). Utiliser le **JDK 21** : `export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.12+1.1-tem` |
| `mvn` | 3.9.16 | — |
| `node` / `npm` | 22.23.2 | `npm test` est **déjà** en mode run (`vitest run`) : ne pas écrire `npm test -- --run` |
| `flutter` | **3.38.7 — `flutter pub get` MARCHE maintenant** (`Got dependencies!`) et `flutter test` passe | Le blocage mobile `NEED-HELP — B8` (du 28/09) est **levé** : ne pas le re-déclarer. Vérifié : `mobile/test/invitation_deeplink_test.dart` → 9/9 verts |
| `vitest` | Fonctionnel fichier par fichier | La suite **globale** en parallèle (20 workers) peut expirer sur cette machine (RAM) : valider par exécution **isolée** ou `--maxWorkers=2` si un échec est un timeout, ce n'est pas un défaut de code |
| RAM / cœurs | 20 cœurs, 15 Go (≈6 Go libres) | Ne pas lancer backend + frontend + mobile en parallèle (R-8) |
| Réseau | `pub.dev` OK | — |

---

## 4. RÈGLES ABSOLUES (valables pour tout ce qui suit)

1. **R1** — un écrivain par fichier, un worktree par agent, aucun code produit dans `main`.
2. **R2** — le contrat §3 du plan est **figé** : tu codes contre lui, tu ne l'inventes pas, tu ne l'amendes pas.
3. **R3** — DoD complète par tâche : implémentation **+ tests imposés + exécution + preuve archivée**.
4. **R4** — aucun mock présenté comme du réel, aucune chaîne visible hors `tText` / clé i18n, aucune dépendance nouvelle.
5. **R5** — une preuve = la **sortie de commande**, archivée dans ton fichier de progression.
6. **R6** — un commit par tâche, message conventional commits en français : `<type>(<taskId>): <quoi> et <pourquoi>`.
7. **R7** — si un point est irréalisable ou contredit le code : **STOP**, écris un bloc `### NEED-HELP — <taskId>`, passe à une tâche indépendante, n'improvise jamais.
8. **R9** — aucun test désactivé (`@Disabled`, `xit(`, `skip:`) pour obtenir un vert.
9. **R10** — **aucune affirmation non prouvée** : n'écris jamais un nombre de tests « de référence » que tu n'as pas mesuré ; un « build vert » se prouve par le code de sortie, pas par l'absence de texte rouge (`npm run lint` est configuré `--max-warnings 1000`, il ne peut pas échouer sur des warnings).
10. **R11** — respecte l'ordre des tâches.
11. **R12** — zéro amélioration opportuniste. *(§5.0 du plan n'est PAS une amélioration : c'est le contrat de qualité d'interface.)*
12. **Arbitrage de propriété** : pendant cette campagne, `docs/**` et `infra/**` (hors `infra/well-known/**`) appartiennent à **l'Agent A** (`docs` via A13). L'attribution à l'Agent B contenue dans `AGENT_ORCHESTRATION.md` §4.1 ne s'applique qu'**après** clôture de cette campagne.
13. **Migration suivante = `V190`** (voir §1.3), jamais `V178..V189`, jamais de modification d'une migration appliquée.

---

# 5. LE TODO — À FAIRE

## PHASE 0 — Reprise sans perte (30 min, bloquante pour tout le reste)

- [ ] **0.1** Dans `discipolat_app-agentA` (branche `fix/schema-drift-h1-h5`), **11 fichiers modifiés + 3 nouveaux ne sont pas commités** : `OllamaProperties`, `SpeechToTextProperties`, `AiAssistantService`, `LlmProviderService`, `AiVisitNoteService`, `VoiceAssistantController`, `application.yml`, 4 tests, + `SystemConfigSummaryController`, `AiFallbackTest`, `ConfigSummaryTest`.
      → Les **valider puis commiter** (Phase 1.1) ou `git stash`er. **Ne jamais laisser ce travail en l'air** : un `git checkout` le détruirait.
- [ ] **0.2** Dans `discipolat_app` (`main`), modifications non commitées d'une **autre** campagne :
      `User.java` (secrets 2FA en `WRITE_ONLY` — fuite JSON), `PaymentController` + `UssdController` (webhooks **fail-closed** : 503 sans secret), `mobile/lib/data/local/sync_service.dart` + `offline_sync_manager.dart` + **nouveau** `sync_lock.dart`, `render.yaml`, `.github/workflows/backup-postgres.yml`, **nouveau** `scripts/bootstrap_prod.sql`.
      → Elles sont **bonnes et non fusionnées** : il faut soit les commiter sur une branche dédiée, soit les.savefig. **Décision orchestrateur** — ne pas les mélanger dans une branche de campagne.
- [ ] **0.3** Toujours dans `main` : les **révisions des deux documents d'autorité** (plan v1.1, journal §12 des 20 défauts corrigés) sont non commitées. Elles n'ont de valeur que si elles sont commitées : c'est une décision d'orchestrateur humain (les documents lui appartiennent).
- [ ] **0.4** Vérifier que `main` n'a pas été modifiée par inadvertance : `git diff d730771..main --name-only` ne doit montrer que ce qui est listé ci-dessus.

## PHASE 1 — Solder les travaux en cours

- [ ] **1.1 — Orchestration A4 (M5 + M6) : configuration honnête IA/STT** (Agent A, worktree A)
      - [ ] Vérifier que chaque service AI/STT a un comportement **fail-closed** documenté quand l'API n'est pas configurée (pas d'appel fantôme, pas de `localhost` codé en dur : `OLLAMA_URL` par défaut, fail-closed par défaut).
      - [ ] `SystemConfigSummaryController` : **ne doit exposer aucun secret** — vérifier champ par champ ce que l'endpoint `/config-summary` rend (clés d'API, URLs signées, tokens FCM = interdits).
      - [ ] Tests : `AiFallbackTest`, `ConfigSummaryTest`, `OllamaHealthTest`, `OllamaPropertiesTest`, `AiConfigurationPropertiesBindingTest`, `AiAssistantServiceTest`.
      - [ ] Preuve : `JAVA_HOME=<jdk21> mvn -B verify` (sortie archivée) + compte exact de tests **mesuré**.
      - [ ] Commit : `config(ai): configuration IA/STT honnete, fallback fail-closed + endpoint de synthèse sans secret`
- [ ] **1.2 — Lot sécurité non commité dans `main`** (voir 0.2) : à fusionner **après** validation, jamais en douce. Le point à vérifier en priorité : `@JsonProperty(access = WRITE_ONLY)` sur `twoFactorSecret`/`twoFactorBackupCodes` protège aussi les sérialisations **imbriquées** (entité `User` incluse dans des réponses brutes de santé/transfers) — donc ajouter un test de non-régression qui sérialise un `User` et assert l'absence de `two_factor_secret`.

## PHASE 2 — Arbitrages bloquants (décision HUMAINE requise — ne pas improviser)

Chacun engage l'architecture ou le schéma de production. Les analyses sont dans
`reports/plan-2agents/agentA.md` (sections « Constats H2 et H8 », « Constat architectural
majeur », « Ce qui reste bloqué »). **Rédiger un bloc `### NEED-HELP` et continuer le reste
si la décision n'est pas prise.**

- [ ] **D1 — H2, module Événements** : l'entité pointe `events` au schéma français (`titre`,
      `date_debut`, `lieu`, `statut`…), les migrations ne créent que `event` au schéma anglais
      (`title`, `start_at`…). Ce n'est pas un renommage, c'est un **port de module**. Bloque
      l'étape `FIRST_EVENT` du wizard, 4 scénarios E2E et le module Événements entier en production migrée.
      - Voie 1 : aligner la base (migration `V190` : table `events` conforme) → **crée deux sources de vérité** sur les événements, à refuser par défaut.
      - Voie 2 : aligner le code sur la base (porter `Event` et `LoadPredictionService` vers `event`) → **le travail correct**, hors périmètre initial.
      - Voie 3 : traiter H2 comme chantier séparé, documenté.
- [ ] **D2 — `users.tenant_id` (tenant d'origine) vs tenant d'action** : le contrôle d'accès est
      correct, mais la lecture de l'utilisateur qui suit s'exécute **après** le changement de
      `TenantContext`, donc encore sous le filtre du tenant précédent.
      - Voie 1 : `findById` sur `users` devient membership-scoped (`EXISTS (SELECT 1 FROM tenant_memberships …)`) → corrige la **cause**, mais modifie le prédicat de **toutes** les lectures d'utilisateurs : exige une revue d'isolation complète.
      - Voie 2 : les étapes du wizard n'utilisent plus `currentActor()` comme `responsableId` → cible le symptôme, laisse le défaut.
      - Voie 3 : fixture de recette plus représentative (c'est l'**owner** qui configure son église) → ne change pas le code de production.
- [ ] **D3 — H4, validation bout-en-bout du sélecteur cross-tenant** : le correctif est écrit et
      5 tests verrouillent le contrat (dont un qui échoue si le filtre n'est pas rétabli), mais
      `POST /api/v1/tenant-switcher/switch` répond encore `500 IllegalStateException: Utilisateur
      introuvable`. Tant que ce point n'est pas vert, le scénario **B2 du plan** (« un
      utilisateur inscrit dans deux églises choisit son organisation ») n'est **pas** delivered.
- [ ] **D4 — `families.nom` porte une contrainte UNIQUE globale** : deux églises ne peuvent pas
      avoir une famille du même nom. L'unicité devrait être `(tenant_id, nom)`. Non corrigé
      (défaut de modèle). Migration `V190` + backfill si validé.
- [ ] **D5 — `E2E-9a` et les sondes IDOR/quota (10b, 11)** : la fixture accorde au Super Admin une
      membership `TENANT_ADMIN`, mais `hasAnyRole` teste le rôle du **JWT** — comportement RBAC
      attendu, **limite de la fixture**, pas un défaut. Corriger la fixture, pas le RBAC.
- [ ] **D6 — Actions ops hors périmètre code** : publication de `https://app.discipolat.com/.well-known/assetlinks.json`
      et `apple-app-site-association` (sans quoi Android ouvre le navigateur) ; et
      `usesCleartextTraffic="true"` présent dans `AndroidManifest.xml` (risque sécurité, hors
      périmètre B8). À traiter dans une campagne dédiée, pas en douce.

## PHASE 3 — Tâches clients restantes du plan Onboarding (worktree B)

Ordre imposé par le plan §5 : **B4 → B5 → B6 → B12 → B9 → B10 → B11 → B13**, puis clôture de B8.

Web d'abord (ne dépend pas de Flutter), mobile ensuite.

- [ ] **B4 — Invitations admin complètes** (constat F3) — plan §5, ligne 758
      Fichiers : `frontend/src/pages/TenantAdminInvitationsPage.tsx` (existe, à compléter),
      `frontend/src/components/tenant/…` au besoin, i18n 6 locales.
      Tests imposés : création (avec/sans scope), renvoi, copie du lien, `emailSent=false`, filtre, `requiresTenantSwitch`.
      Preuve : `cd frontend && npx vitest run src/__tests__/TenantAdminInvitationsPage.test.tsx` (**le fichier de test n'existe pas encore**).
- [ ] **B5 — Admin tenants : plans canoniques, réactivation, stats réelles, état d'onboarding** (F4) — plan ligne 773
      - Plans depuis `GET /api/v1/platform/admin/plans` (**endpoint vérifié**, `SuperAdminController:503`), fallback `['DISCOVERY','STARTUP','GROWTH','NETWORK']` (clés seedées par `V144`/`V177`). Supprimer `PLAN_OPTIONS` en dur (`AdminTenantsPage.tsx:21`).
      - Bouton **Réactiver** visible si `status === 'SUSPENDED'` ; libellés « Suspendre l'église » ; `POST /api/v1/tenants/{id}/reactivate` (ligne 75, super-admin). Un 403 = session expirée, à afficher proprement.
      - Stats : `GET /api/v1/platform/admin/quota-usage/tenants/{tenantId}` (`PlatformQuotaUsageController:33`). En cas d'échec → afficher `—`, **jamais `0`**.
      - Badge onboarding depuis `onboardingCompletedAt` (champ optionnel).
      - Résilience : statut inconnu rendu tolérant. **Ne pas** ajouter de statut `ONBOARDING` (interdit par D2).
      Preuve : `npx vitest run src/__tests__/AdminTenantsPage.test.tsx` (**à créer**).
- [ ] **B6 — Page abonnement & quotas du tenant** (F4) — plan ligne 791
      Endpoints (tous vérifiés, classe entière en `@PreAuthorize("@authz.isTenantAdmin()")`) :
      `/current`, `/plans`, `/change-plan`, `/cancel`, `/reactivate`, `/usage`. Ne pas appeler `/subscribe`.
      Réutiliser **`QuotaUsageCards`** (`frontend/src/components/admin/QuotaUsageCards.tsx`, déjà testé) — **`QuotaUsagePanel` n'existe pas**.
      Le `403` est le cas nominal d'un non-admin : cas de test obligatoire.
      Preuve : `npx vitest run src/__tests__/TenantAdminSubscriptionPage.test.tsx` (**à créer**).
- [ ] **B12 — Provisioning web : champs owner** (contrat §3.5) — plan ligne 814
      Fichier : `frontend/src/pages/PlatformOnboardingFlowPage.tsx`.
      Tests : validation owner (3 cas), payload conforme, récapitulatif avec `activationEmailSent=false`.
      Preuve : `npx vitest run src/__tests__/PlatformOnboardingFlow.test.tsx` (**à créer**).
- [ ] **B9 — Mobile : gestion complète des invitations** (MO3) — plan ligne 922
      Il existe `mobile/lib/presentation/screens/invitations/accept_invitation_screen.dart` (acceptation) ; **pas d'écran de gestion** (liste, création, renvoi) ni de service mobile correspondant.
- [ ] **B10 — Mobile : provisioning complet (plans + géo + owner)** (MO4) — plan ligne 938
- [ ] **B11 — Mobile : auto-login après acceptation d'invitation** (MO3/mineur) — plan ligne 954
      Vérifié : `accept_invitation_screen.dart` ne fait **aucun** échange de jeton → l'auto-connexion n'existe pas.
- [ ] **B13 — Qualité clients : i18n, lint, build, analyse** — plan ligne 968
      `flutter analyze` 0 erreur / 0 nouvelle remarque, `npx tsc -b`, `npx vitest run`, `npx vite build`, clés i18n dans les **6 locales**.
- [ ] **Clôture de B8** : le code et les 9 tests d'URI sont verts. Ce qui reste = la **recette manuelle
      E2E-11** (ouvrir un vrai lien d'invitation sur un appareil), à journaliser, plus la décision ops D6.
- [ ] **Qualité d'interface — obligatoire pour TOUTES ces tâches (§5.0 du plan)** : réutiliser le
      design system (`components/ui/UXComponents.tsx` : `Skeleton*`, `EmptyState`, `ConfirmDialog`,
      `ProgressBar`, `OnboardingStepper`, `VisuallyHidden`, `useReducedMotion`) — **aucun doublon** ;
      5 états par vue (chargement / vide / erreur+retry / succès / hors-ligne) ; mouvement ≤ 400 ms
      en `transform`/`opacity` uniquement, annulé sous `prefers-reduced-motion` ;
      **interdiction formelle** de `framer-motion`, `three`, `@react-three/*` (absents de `package.json`) ;
      WCAG 2.1 AA (contraste clair **et** sombre, `aria-current`/`aria-invalid`/`aria-describedby`,
      cibles ≥ 44 px, **RTL** avec propriétés logiques `ms-`/`me-`, jamais `margin-left` en dur) ;
      `Intl`/`intl` pour dates et devises ; responsive 360 px ; toute nouvelle page en `lazy()` dans `App.tsx` ;
      ne pas casser `manualChunks` de `vite.config.ts`.

## PHASE 4 — Campagne « production mondiale » (`AGENT_ORCHESTRATION.md`)

À n'engager **qu'après** clôture et merge de la campagne Onboarding (règle de coexistence en tête
du fichier : sinon conflits sur `SecurityConfig`, `application.yml`, `App.tsx`, `docker-compose.yml`).

### 4.1 — Agent A (worktree A, `backend/**` + `scripts/**` + `.github/**` + `docs/**`)

- [ ] **A0** Amorçage : baseline chiffrée et réelle (LOC, nombre de fichiers de test, **nombre de cas mesuré**), matrice de couverture §1.4 vérifiée avant de déclarer quoi que ce soit « terminé ».
- [ ] **A3 — M7 + M8 + M9 : échelle mondiale et paiements universels** (P0, le plus lourd)
      - M7 : sharding / partitionnement — l'objectif 10⁶–10⁸ tenants n'est pas atteignable en mono-PostgreSQL. Fondations + abstraction, **sans** migration risquée dans la foulée (une conversion partitionnée se prépare dans un script documenté, pas dans le même commit).
      - M8 : fournisseurs hors Afrique. État réel : seuls `MtnMomoProvider`, `OrangeMoneyProvider`, `MpesaProvider` existent. `SaasPlan`/`TenantSubscription` ont des colonnes `stripe_price_id_*` (**stockage d'identifiants seulement** — l'intégration Stripe est ce qui manque, pas le mot). Ajouter `StripePayoutProvider`, `PayPalPayoutProvider`, SEPA/virement, ISO-4217.
      - M9 : l'abstraction existe déjà (`MobileMoneyProvider` + `MobileMoneyProviderRegistry`) mais ne couvre que le Mobile Money africain ; la logique de don/transaction reste liée à l'XOF.
- [ ] **A5 — CI/CD : tests E2E, charge, sécurité, isolation multi-tenant bloquants** (M10, M11, M13)
      Constat vérifié : `.github/workflows/` **ne contient aucun workflow k6 ni E2E navigateur** ; `puppeteer-core` est une devDependency utilisée par un seul script. À faire : suite E2E navigateur bloquante, `performance-tests/k6-load-test.js` branché, tests d'isolation inter-tenant sur le flux HTTP complet, étape de backup-restore testée.
- [ ] **A6 — Documentation professionnelle** : README, API, deployment, runbook **véridiques** (une doc fausse est pire que pas de doc), index navigable dans `docs/`.

### 4.2 — Agent B (worktree B, `frontend/**` + `mobile/**`)

- [ ] **B0 — Amorçage + audit i18n** : script dans **`frontend/scripts/i18n-audit.mjs`** (sa zone), **jamais** dans `scripts/` racine. Mesure de référence à relever : `fr` = 2 571 clés ; `en` −1/+13 ; `pt` +58 ; `es` +142 ; `sw` +142 dont **728 valeurs identiques au français** ; `ar` +186 dont **721 identiques**.
- [ ] **B1 — M4 : qualité i18n** : compléter les 6 locales, corriger les valeurs non traduites, `flutter gen-l10n` sur les `.arb`.
      ⚠️ **Ne jamais éditer `app_localizations.dart` à la main** (9 846 lignes générées) : toute édition est écrasée au prochain `gen-l10n`. Si l'outil est indisponible, corriger les `.arb` et le signaler.
      ⚠️ Le test « les 6 langues ont le même ensemble de clés » **échouerait dès le premier run** (elles diffèrent déjà) et serait désactivé. Critère retenu : le test échoue si le **delta** **augmente** par rapport à la référence versionnée en fixture.
- [ ] **B2 — PWA offline réelle + notifications** : service worker de production, page offline, stratégie de mise à jour, push web, navigation mobile.
- [ ] **B3 — Performance web** : chunks (allonger Recharts, query keys tenant-aware, pagination serveur, CLS, skeletons) — gain avant/après **mesuré** et reporté.
- [ ] **B4 — Mobile : dette et robustesse** : écrans > 1 000 lignes (mesuré : 5 — `department_management_screen.dart` 1 441, `app_drawer.dart` 1 377, `department_member_dossier_screen.dart` 1 360, `department_tools_screen.dart` 1 336, `department_detail_screen.dart` 1 326 ; **exclure** les fichiers générés `*.g.dart`, `*.freezed.dart`, `app_localizations.dart`).
- [ ] **B5 — Documentation utilisateur** : guides par rôle bilingues, démarrage 5 min, FAQ 20 questions, tooltips sur les 15 actions les moins intuitives, `docs/UTILISATEUR/README.md` (**le dossier n'existe pas encore**), lien « Aide » dans l'application.

## PHASE 5 — Intégration, fusion, recette

- [ ] **5.1** Vérifier qu'aucune branche n'a été poussée et que `main` est intacte : `git branch -vv`.
- [ ] **5.2** Contrôler l'absence de chevauchement de fichiers entre les deux branches :
      `comm -12 <(git diff --name-only 72ec85d5..fix/schema-drift-h1-h5) <(git diff --name-only 72ec85d5..fix/onboarding-tenant-clients)`
      → intersection non vide = STOP + rapport.
- [ ] **5.3** Fusion locale (dans le worktree A) :
      `git merge --no-ff fix/onboarding-tenant-clients -m "merge: onboarding/tenant (A+B)"`
      Conflit = STOP + rapport (**ne jamais résoudre en inventant du code**). La fusion est **prévue propre** (vérifié le 2026-09-29) ; si elle ne l'est plus, l'état a changé depuis.
- [ ] **5.4** Rejouer la vérification complète sur la branche fusionnée, dans cet ordre : **backend → frontend → mobile**. Suites lourdes **en fin de tâche, jamais en parallèle**.
- [ ] **5.5** Rejouer la recette : `scripts/verify-tenant-onboarding.sh` sur un backend jetable (port 1 8080), PostgreSQL et Redis jetables, clés RSA/AES générées à la volée. **Ne jamais toucher au 8 080 de production ni aux conteneurs de production.**
      Dernier état connu : **42 PASS / 10 FAIL / 7 SKIP**. Les 10 échecs sont identifiés (4 = bugs de la recette à corriger, 4 = H2, 1 = limite de fixture D5, 2 = conséquences de cette fixture). Objectif de cette phase : **42 PASS → 0 FAIL impossible sans D1/D2/D3** ; le minimum attendu est que **les 4 bugs de recette soient corrigés** et que les autres soient `SKIP` justifiés, jamais `PASS` déguisés.
- [ ] **5.6** Diff global : `git diff --stat 72ec85d5..HEAD` — aucun fichier hors périmètre (`docker-compose.yml`, `render.yaml`, `application-prod.yml` sont hors périmètre de cette campagne).
- [ ] **5.7** Produire les **deux rapports manquants** (ils n'existent pas encore) :
      - `reports/plan-2agents/INTEGRATION.md` — commandes, résultats, tâches **non faites** avec leurs NEED-HELP, recommandation.
      - `reports/plan-2agents/VERIFICATION.md` — matrice de traçabilité §9, sorties des gates G-A/G-B, résultat des E2E, écarts résiduels assumés.
- [ ] **5.8** Ne pas pousser, ne pas taguer. Transmettre à l'orchestrateur humain.

## PHASE 6 — Checklist finale (plan §11)

- [ ] Gate G-A passé (5 critères) et gate G-B passé (7 critères, dont la qualité d'interface §5.0).
- [ ] Contrat §3 vérifié endpoint par endpoint, aucun écart de nom de champ (3 faces : backend = web = mobile).
- [ ] E2E-1 → E2E-10 PASS ; E2E-11/12 : recette manuelle documentée.
- [ ] Aucune tâche `IN_PROGRESS`/`BLOCKED` sans bloc NEED-HELP explicite.
- [ ] Migrations : uniquement celles ajoutées par la campagne, à partir de `V190` ; **aucune migration existante modifiée**.
- [ ] Aucun `push`, aucun tag, `main` intacte.
- [ ] Plus aucune affirmation non prouvée dans la documentation sur l'onboarding/tenant.
- [ ] `VERIFICATION.md` et `INTEGRATION.md` présents et argumentés.
- [ ] Aucune tâche livrée sans preuve de test exécuté (R3 + R5).

---

## 6. PIÈGES CONNUS — NE LES « CORRIGE » PAS

Ces points ont déjà été vérifiés dans le code réel. Les « corriger » Break quelque chose ou
introduisent une duplicata.

| Piège | Vérité vérifiée |
|---|---|
| Créer `components/onboarding/OnboardingStepper.tsx` | `OnboardingStepper` **existe déjà** dans `components/ui/UXComponents.tsx:212`, testé (`UXComponents.test.tsx:184-210`). Étendre par props optionnelles, ne jamais dupliquer |
| Réutiliser `QuotaUsagePanel` | Ce composant **n'existe pas**. Le vrai est `components/admin/QuotaUsageCards.tsx` |
| `GET /platform/admin/tenants/{id}/usage` | **N'existe pas.** Le vrai : `GET /api/v1/platform/admin/quota-usage/tenants/{tenantId}` |
| Bundle id `com.discipolat.mobile` | **N'existe nulle part.** Android = `com.discipolat.discipolat_mobile`, iOS = `com.discipolat.discipolatMobile` |
| Universal Links iOS | Exigent `ios/Runner/Runner.entitlements` **et** `CODE_SIGN_ENTITLEMENTS` dans le `project.pbxproj` |
| `discipolat://accept-invitation` | `invitationTokenFromUri` exige `uri.path == '/accept-invitation'` ; cette forme donne un **path vide** → `null` → l'app s'ouvre sans jeton. C'est la **manifeste** qu'il faut corriger (host + `pathPrefix`), **pas** le parseur |
| Ajouter le statut `ONBOARDING` | Interdit par D2. L'exigence est la **résilience** à un statut inconnu |
| `npm test -- --run` | `package.json` contient déjà `"test": "vitest run"`. Redondant |
| Migrer en `V178/V179/V180` | Numéros **déjà pris** (RGPD). Prochain libre : **`V190`** |
| « Nombre de tests de référence ≥ 1188 / ≥ 325 / ≥ 356 » | Chiffres **invérifiables** (le nombre de cas n'est connu qu'après exécution). Seul baseline opposable : celui **mesuré par l'agent sur sa machine** |
| Chercher un endpoint devises/fuseaux (`/currencies`) | **N'existe pas** (`CurrencyController` absent). Listes minimales documentées + valeurs par défaut sélectionnables (D12) |
| `flutter pub get` cassé | **Résolu le 2026-09-29** : `Got dependencies!`, `flutter test` passe. Le NEED-HELP du 28/09 est périmé |
| Un build « vert » parce qu'aucune erreur n'est affichée | `npm run lint` est en `--max-warnings 1000` : il ne peut pas échouer sur des warnings. Juger sur le **code de sortie** |
| Modifier `AGENT_ORCHESTRATION.md` / le plan | Interdits (fichiers d'autorité). Signaler, ne pas corriger |

---

## 7. PROBLÈMES D'INFRASTRUCTURE À NE PAS CONFONDRE AVEC DES DÉFAUTS DE CODE

| Symptôme | Diagnostic | Ce qu'il ne faut **pas** faire |
|---|---|---|
| `mvn test` échoue sur des mocks/ByteBuddy | JDK 25 par défaut ; le support s'arrête à 24 | Reprendre `JAVA_HOME` sur le **JDK 21** (`~/.sdkman/candidates/java/21.0.12+1.1-tem`) |
| Vitest : « Timeout waiting for worker to respond », des dizaines de fichiers « en échec » | 20 workers sur une machine à ~6 Go libres | **Ne pas** modifier un test. Rejouer isolé ou `--maxWorkers=2` ; un échec par timeout est un problème de machine |
| Un rapport annonce un nombre de tests « en échec » obtenu par un script de lots | Artefact du script (option de pool invalide) | Vérifier par exécution directe avant de conclure (cf. le cas livedéjà dans `agentB.md`) |
| La recette E2E renvoie `SKIP` sur 7 scénarios | Les scénarios non exécutables sont `SKIP`, **jamais** `PASS` | Ne pas « arranger » le compteur. Un `SKIP` est une information, pas un échec à masquer |

---

## 8. MODÈLE DE RAPPORT (à remplir en fin de chaque tâche)

```text
## <taskId> — <titre>
- Statut : DONE | PARTIEL | BLOCKED (+ bloc ### NEED-HELP)
- Commit : <sha> — <message>
- Fichiers : <liste exacte>
- Tests : <commande exacte exécutée> → <résultat exact, nombre de cas mesuré>
- Preuve : <extrait de sortie, pas un résumé>
- Contraintes de migration : <numéros utilisés>
- Limites connues : <ce qui n'a pas été fait et pourquoi>
```

---

## 9. EN UNE LIGNE

Le **backend** de la campagne Onboarding est terminé et validé en exécution réelle ; ce qui
manque, ce sont **la décision humaine sur H2 et l'architecture multi-tenant (D1/D2/D3)**, la
**moitié des tâches clients (B4→B13)**, la **campagne orchestration (A3/A5/A6 + B0→B5)**, la
**fusion des deux branches** et les **deux rapports de clôture**. Ne refaites rien de ce qui est
en §2 : il est prouvé.

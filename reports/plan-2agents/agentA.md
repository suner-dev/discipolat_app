# PROGRESSION — AGENT A (BACKEND) — Plan `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md`

> Fichier de progression privé de l'Agent A (règle `R8` du plan).
> Format imposé par tâche : Statut / Commit / Fichiers / Tests / Preuve.
> L'Agent B n'écrit que dans `agentB.md`. Ce fichier n'est jamais modifié par lui.
>
> Préfixes de tâches : `ONB-A*` = plan onboarding (ce fichier), `ORC-A*` = plan
> `AGENT_ORCHESTRATION.md` (même fichier, sections séparées).

---

## 0. PHASE 0 — Préparation (P0.1 → P0.4)

- **Statut** : DONE
- **Worktree** : `/home/arise/discipolat/discipolat_app-agentA`
- **Branche** : `fix/onboarding-tenant-backend` (créée depuis `d730771`)
- **Commit base** : `d730771` (HEAD de `main` au démarrage). Le plan citait
  `ec74906` (P0.1) ou `d8400cf` (en-tête) : ces commits sont **antérieurs** au
  plan lui-même. J'ai branched sur `d730771` afin que le plan, `PROGRESSION.md`
  et l'audit soient présents dans le worktree. Voir NEED-HELP-01.

### Baseline backend (P0.3)

| Mesure | Commande | Résultat réel |
|---|---|---|
| Compilation | `mvn -B -o -DskipTests compile` | `BUILD SUCCESS` — 1 264 fichiers `.java` |
| Tests | `mvn -B -o test` | `Tests run: 1252, Failures: 0, Errors: 0, Skipped: 13` |
| Durée suite | — | 1 min 21 |
| Migrations | `ls db/migration` | 145 fichiers, **dernier = `V177__complete_canonical_saas_plans.sql`** |

> **Baseline de référence pour le gate G-A §7.2 critère 4 = 1252 tests**
> (le plan citait 1188 : la référence du plan est périmée de +64 tests).

### Contraintes matérielles de la machine (à connaître)

- 20 cœurs / 15 Go RAM, **swap saturée**, IntelliJ IDEA ~4,9 Go + Chrome + un
  conteneur `kfokam48-demo-init-backend` (le backend de prod) en cours d'exécution.
- Les exécutions Maven sont **tué par l'OOM killer** avec les réglages par défaut.
- Recette validée et utilisée pour **toutes** les commandes de preuve :
  ```bash
  export JAVA_HOME=~/.sdkman/candidates/java/21.0.12+1.1-tem   # Java 21 (pom cible 21)
  export MAVEN_OPTS="-Xmx600m -XX:MaxMetaspaceSize=350m"
  mvn -B -o test -DargLine="-Xmx1200m -XX:MaxMetaspaceSize=450m"
  ```
- `mvn -o` (offline) : obligatoire pour la reproductibilité, le dépôt local
  `~/.m2` (1,7 Go) contient tout. Seule exception : `flyway:migrate`, dont les
  dépendances de plugin ne sont pas encore en cache (réseau utilisé une fois).

### P0.4 — Disponibilité des numéros de migration

- `V183`, `V184`, `V185` : **tous libres** (`ls | grep -E "^V18[3-5]"` → aucun résultat).
- `infra/well-known/` : sans objet (zone Agent B).

---

## A7 — Unicité email globale + acceptation cross-tenant (constat B4)

- **Statut** : DONE
- **Commit** : _(voir §« Journal git » en bas de fichier)_
- **Fichiers** :
  - NEW `backend/src/main/resources/db/migration/V185__users_email_global_unique.sql`
  - MOD `backend/src/main/java/com/discipolat/modules/users/domain/UserRepository.java`
  - MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/AuthService.java`
  - MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/InvitationService.java`
  - MOD `backend/src/main/java/com/discipolat/modules/platform/api/InvitationController.java`
  - NEW `backend/src/test/java/com/discipolat/modules/tenants/domain/InvitationServiceCrossTenantTest.java`
  - NEW `backend/src/test/java/com/discipolat/modules/authentication/domain/AuthServiceEmailLookupTest.java`
  - MOD `backend/src/test/java/com/discipolat/modules/tenants/domain/InvitationServiceTest.java`
  - MOD `backend/src/test/java/com/discipolat/modules/platform/api/InvitationControllerTest.java`
  - MOD `backend/src/test/java/com/discipolat/modules/authentication/domain/AuthServiceTest.java`

### Preuve — tests imposés (`§4 A7`)

```
mvn -B -o test -Dtest='InvitationServiceCrossTenantTest,AuthServiceEmailLookupTest,InvitationServiceTest,InvitationControllerTest,AuthServiceTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in ...authentication.domain.AuthServiceTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...authentication.domain.AuthServiceEmailLookupTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.InvitationServiceCrossTenantTest
[INFO] Tests run:  6, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.InvitationServiceTest
[INFO] Tests run:  3, Failures: 0, Errors: 0, Skipped: 0 -- in ...platform.api.InvitationControllerTest
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**7 cas** dans `InvitationServiceCrossTenantTest` (le plan en exige ≥ 5) :
même tenant / autre tenant / aucun `User` créé en cross-tenant / pas de mot de
passe exigé en cross-tenant / nouvel email / résolution insensible à la casse /
audit de l'acceptation cross-tenant.

**7 cas** dans `AuthServiceEmailLookupTest` : login insensible à la casse, email
inconnu, mot de passe erroné, resend activation, reset de mot de passe, absence
de fuite d'existence, magic-link.

### Preuve — non-régression suite complète

```
mvn -B -o test        (recette mémoire ci-dessus)

[INFO] Results:
[WARNING] Tests run: 1266, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 14 nouveaux = **1266**. Aucun échec, aucune régression.
Les 13 `Skipped` sont **préexistants** : `PerIpRateLimiterIntegrationTest`,
protégé par `@EnabledIf("isRedisAvailable")` (Redis absent sur cette machine).
Aucun `@Disabled`, `xit(` ou `skip:` n'a été ajouté.

### Preuve — migration V185 sur PostgreSQL réel

La suite de tests utilise **H2 avec `spring.flyway.enabled: false` et
`ddl-auto: create-drop`** : les migrations ne sont JAMAIS exécutées par
`mvn verify`. Elles ont donc été validées sur un PostgreSQL 16.15 jetable
(containeur `onb-flyway-check`, port 55444, sans aucun lien avec le conteneur de prod).

**1. Base vierge — les 146 migrations s'appliquent, jusqu'à v185 :**
```
mvn -B flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55444/discifly \
    -Dflyway.user=onbtest -Dflyway.password=onbtest -Dflyway.locations=filesystem:src/main/resources/db/migration

[INFO] Migrating schema "public" to version "185 - users email global unique"
[INFO] Successfully applied 146 migrations to schema "public", now at version v185 (execution time 06:20.288s)
[INFO] BUILD SUCCESS
```

**2. L'index unique global existe et refuse les doublons par cas :**
```sql
-- insertion 1 : "Pasteur@Eglise.com"  -> OK
-- insertion 2 : "pasteur@eglise.COM"  -> refusée
ERROR:  duplicate key value violates unique constraint "uk_users_email_lower"
DETAIL:  Key (lower(email::text))=(pasteur@eglise.com) already exists.

\d users ->  "uk_users_email_lower" UNIQUE, btree (lower(email::text)) WHERE deleted = false
```

**3. Comportement fail-closed (2ᵉ base, migrée jusqu'à V177, 1 doublon par cas
   inséré manuellement, puis application de V185) :**
```
[ERROR] Message : ERROR: V185: 1 doublon(s) email insensibles a la casse - dedoublonnage manuel requis
[ERROR] Location : .../V185__users_email_global_unique.sql
[ERROR] Line     : 22
```
→ La migration **refuse de démarrer**, ne supprime et ne fusionne **aucune
donnée**. Le dédoublonnage reste une décision humaine (risque R-1 du plan).

### Critères d'acceptation (§4 A7)

| Critère | Statut | Comment prouvé |
|---|---|---|
| Deux comptes actifs ne peuvent jamais partager un email | ✅ | index unique global `uk_users_email_lower` appliqué et testé sur PG réel (preuve 2 ci-dessus) — garantie **base de données**, pas seulement applicative |
| L'acceptation cross-tenant ne duplique jamais `users` | ✅ | `InvitationServiceCrossTenantTest.neverCreatesUserRowInCrossTenantScenario` (`verify(userRepository, times(0)).save(...)`) |
| Le login reste déterministe | ✅ | `AuthServiceEmailLookupTest.loginIsCaseInsensitive` + `verify(userRepository, never()).findByEmail(anyString())` |
| Migration appliquée sur base vierge | ✅ | preuve 1 : 146 migrations, version v185 |

### Décisions et déviations documentées

1. **`findByEmailIgnoreCase` / `existsByEmailIgnoreCase` en requête NATIVE, pas JPQL.**
   L'entité `User` porte `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")`.
   Une requête JPQL serait donc filtrée dès qu'un `TenantContext` est posé, et la
   résolution d'identité par email deviendrait **aléatoire selon le contexte HTTP**
   (exactement le constat B4). L'email est une identité **globale** : la requête
   doit contourner le filtre, comme le fait déjà `findGlobalByEmail` existant.
   Le code métier et les noms de méthode restent ceux du plan (§3 n'est pas touché).
2. **`AcceptanceResult`** : ajout du 5ᵉ champ `crossTenantIdentity` (contrat §3.4)
   **+ un constructeur de compatibilité** à 4 arguments, pour ne casser aucun
   appelant/test existant. Le plan autorise la mise à jour des tests impactés
   (« si signatures modifiées, justification obligatoire ») ; ici aucun n'a eu
   besoin d'être modifié pour la signature.
3. **`validateInvitation` → `accountExists`** : la recherche passe de
   `findByTenantIdAndEmail(tenantId, …)` à `findGlobalByEmailIgnoreCase(…)`.
   Raison : avec la décision D3, une identité cross-tenant **n'exige pas de mot
   de passe**. Renvoyer `accountExists=false` alors que le compte existe
   obligerait le client web/mobile à en demander un inutilement (cf. `ONB-B11`).
   Sans ce correctif, le parcours d'acceptation aurait été cassé côté client.
4. **Tests existants mis à jour (justification `R9`)** :
   - `InvitationServiceTest` : le test
     `createsTenantScopedUserWhenSameEmailOnlyExistsInAnotherTenant` **documentait
     le bug B4** (il affirmait qu'un email présent dans une autre église devait
     créer un nouvel utilisateur local). Renommé en
     `createsTenantScopedUserWhenNoAccountExistsForThatEmailAnywhere` et mocks
     basculés sur la résolution globale. Les 4 autres tests ont eu leur mock
     `findByTenantIdAndEmail` → `findGlobalByEmailIgnoreCase` (changement de
     comportement imposé par le correctif).
   - `AuthServiceTest` : 5 mocks `findByEmail` → `findByEmailIgnoreCase`
     (`AuthService.login` ne doit plus utiliser le lookup sensible à la casse).
   - `InvitationControllerTest` : 1 mock `findByTenantIdAndEmail` →
     `findGlobalByEmailIgnoreCase` (voir déviation 3).
   Aucun test n'a été désactivé, aucun `@Disabled` ajouté, aucune assertion
   affaiblie.

### Risque résiduel assumé

`existsByEmail(email)` (sensible à la casse) reste utilisé par
`ImportService:398` et `BulkImportService:93` comme **pré-contrôle advisory**
avant création d'un utilisateur. Ces deux fichiers ne figurent pas dans la liste
de fichiers de la tâche `A7` (§2, règle d'un écrivain par fichier) et
`BulkImportServiceTest` mocke explicitement cette méthode. **Ce n'est plus un
risque de doublon** : l'index unique global V185 rejette au niveau base de
données toute variante de casse. L'effet résiduel se limite à un message
d'erreur moins pédagogique sur une course concurrente. Signalé ici pour
traçabilité, hors périmètre de A7.

---

## NEED-HELP-01 — BASE_COMMIT différent de celui du plan

- **Blocage** : le plan indique `BASE_COMMIT : HEAD (d8400cf ou le commit courant)`
  en en-tête, mais `§6.1 P0.1` impose de créer le worktree depuis `ec74906`.
  Ces deux commits sont **antérieurs** à `d730771` (le commit qui a ajouté le
  plan lui-même). Brancher sur `ec74906` aurait produit un worktree **sans** le
  fichier d'autorité `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` ni
  `PROGRESSION.md`, rendant la tâche auto-incohérente (`R1` impose de lire le
  plan en entier).
- **Décision prise** : brancher sur `d730771` (HEAD de `main`), qui contient tout
  et n'introduit aucune modification de code. Le plan autorise explicitement
  « le commit courant de votre branche de travail ».
- **Impact** : aucun sur le périmètre fonctionnel.
- **Points d'attention voisins** (aucune action corrective, simple traçabilité) :
  - `§2` annonce « dernier existant : **V182** » alors que le dernier fichier de
    migration réel est **V177** (V178–V182 n'existent pas). Les numéros
    réservés V183/V184/V185 restent libres et corrects → aucune action.
  - `§2` annonce 146 migrations Flyway, `AGENT_ORCHESTRATION.md §1.1` aussi ;
    le compte réel est **145**.

## NEED-HELP-02 — Le gate G-A « Flyway sur base vierge » n'est pas couvert par `mvn verify`

- **Blocage** : `A12` et le gate G-A §7.2 supposent que `mvn -B verify` applique
  V183/V184/V185 sur une base vierge. Or le profil de test
  (`backend/src/test/resources/application-test.yml`) impose :
  ```yaml
  datasource.url: jdbc:h2:mem:testdb;MODE=PostgreSQL
  jpa.hibernate.ddl-auto: create-drop
  flyway.enabled: false
  ```
  Les migrations ne sont donc **jamais** exécutées par la suite de tests, et le
  schéma est créé par Hibernate à partir des entités. Un `mvn verify` vert ne
  prouve **rien** sur les migrations, et inversement une migration Postgres-only
  (`DO $$`, index unique partiel) ne peut pas être testée par la suite H2.
- **Options** :
  1. (a) Refuser A12 et le gate G-A comme inatteignables ;
  2. (b) valider les migrations sur un PostgreSQL réel hors CI, et le documenter
     comme la preuve de référence ;
  3. (c) ajouter une integration test Testcontainers + Flyway dans la suite.
- **Décision prise** : **(b) + (c) en proposé**. (b) est appliqué dès A7 (preuve
  ci-dessus, PostgreSQL 16.15 jetable). (c) est **hors périmètre du plan** (A12
  ne demande que `mvn verify`) et introduirait un test qui dépend de Docker
  dans la suite par défaut — je ne l'ajoute **pas** sans votre accord.
  → **Décision demandée à l'orchestrateur** : accepter (b) comme preuve
  officielle, ou mandater (c).
- **Recommandé** : (b) pour cette campagne, (c) comme chantier `ORC-A5`
  (la CI bloquante est précisément le chantier qui doit rendre les migrations
  testables de façon récurrente).

## NEED-HELP-03 — Conflit interne au plan sur le statut HTTP de `OWNER_EMAIL_ALREADY_USED`

- **Blocage** : `A5.1` demande `BusinessRuleException("...", "OWNER_EMAIL_ALREADY_USED")`
  **avec un statut 409**. Or `GlobalExceptionHandler:43-52` ne sait mapper
  `BusinessRuleException` que vers **400** (défaut) ou **403** (préfixes
  `FEATURE_DISABLED_`/`QUOTA_`). Le 409 est **inatteignable** avec cette classe.
- **Options** : (a) `DomainException(msg, HttpStatus.CONFLICT, "OWNER_EMAIL_ALREADY_USED")`
  — atteint le 409 demandé, le **code métier reste identique** ; (b) rester sur
  `BusinessRuleException` et livrer un 400 en violant le plan.
- **Décision demandée** : valider (a). Je n'applique rien avant la fin de A4 et
  je poursuis les tâches indépendantes (R11).
- **Recommandé** : (a). C'est la convention déjà retenue par le wizard (D6 :
  « les erreurs métier du wizard utilisent `DomainException(message, HttpStatus, code) » »).

## NEED-HELP-04 — Conflit interne au plan sur `STEP_ALREADY_COMPLETED` (409) vs `BusinessRuleException`

- **Blocage** : même nature que NEED-HELP-03, mais pour le wizard : `A3` exige
  `409 STEP_ALREADY_COMPLETED`, `409 STEP_NOT_SKIPPABLE`, `409 STEP_PRECONDITION_FAILED`,
  or ces codes ne sont atteignables qu'avec `DomainException(..., HttpStatus.CONFLICT, ...)`.
- **Décision** : `DomainException` (déjà couvert par la décision D6 du plan, donc
  **pas une déviation**, seulement une application de la convention existante).
- **Statut** : sans décision à prendre — traité en A3, documenté dans la tâche.

---

## Journal git (rempli à chaque commit, R6)

| Tâche | Commit | Message |
|---|---|---|
| Phase 0 | _(ce commit est le premier de la branche — voir `git log`)* | — |
| A7 | *à compléter* | `feat(A7): unicité email globale V185 + acceptation invitation cross-tenant` |

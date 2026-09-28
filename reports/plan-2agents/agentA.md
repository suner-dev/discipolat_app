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

---

## A1 — Garde de statut tenant + enforcement (constat B1)

- **Statut** : DONE
- **Fichiers** :
  - NEW `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantStatusGuard.java`
  - NEW `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantStatusChangedEvent.java`
  - NEW `backend/src/main/java/com/discipolat/common/multitenancy/TenantStatusInterceptor.java`
  - MOD `backend/src/main/java/com/discipolat/common/multitenancy/WebMvcConfig.java`
  - MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantService.java`
  - MOD `backend/src/main/java/com/discipolat/modules/authentication/domain/AuthService.java`
  - MOD `backend/src/main/java/com/discipolat/modules/platform/api/TenantSwitcherController.java`
  - NEW `backend/src/test/java/com/discipolat/modules/tenants/domain/TenantStatusGuardTest.java`
  - NEW `backend/src/test/java/com/discipolat/common/multitenancy/TenantStatusInterceptorTest.java`
  - MOD `backend/src/test/java/com/discipolat/modules/authentication/domain/AuthServiceTest.java` (+3 cas)
  - MOD `backend/src/test/java/com/discipolat/modules/tenants/domain/TenantServiceTest.java` (+1 mock)
  - MOD `backend/src/test/java/com/discipolat/modules/authentication/domain/AuthServiceEmailLookupTest.java` (nouveau ctor)
  - MOD 3 tests d'intégration (fixtures, voir « Régression » ci-dessous)

### Preuve — tests imposés (`§4 A1`)

```
mvn -B -o test -Dtest='TenantStatusGuardTest,TenantStatusInterceptorTest,AuthServiceTest,TenantServiceTest,AuthServiceEmailLookupTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in ...authentication.domain.AuthServiceTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...authentication.domain.AuthServiceEmailLookupTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.TenantStatusGuardTest
[INFO] Tests run:  6, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.TenantServiceTest
[INFO] Tests run:  8, Failures: 0, Errors: 0, Skipped: 0 -- in ...common.multitenancy.TenantStatusInterceptorTest
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`TenantStatusGuardTest` (14 cas) : actif autorisé, `PENDING_SETUP` autorisé,
`tenantId` nul transparent, suspendu → 403 `TENANT_SUSPENDED`, annulé → 403
`TENANT_CANCELLED`, cache servi sans relecture, **TTL 30 s respecté (juste avant
/ juste après)**, invalidation par événement, invalidation⇒cache vide,
réactivation immédiate, fail-closed sur exception de lecture, fail-closed sur
tenant introuvable, fail-closed sur statut `null`, **panne jamais mémorisée**.

`TenantStatusInterceptorTest` (8 cas) : API authentifiée refusée, garde appelée
avec le bon `tenantId`, chemin public ignoré, `invitations/accept/**` joignable,
`actuator/health` joignable, requête sans contexte tenant ignorée, dégradation
gracieuse si le bean garde est absent, dégradation gracieuse si `TenantFilter` est
absent.

`AuthServiceTest` (+3 cas) : login d'un tenant suspendu → 403 `TENANT_SUSPENDED`
**et aucun JWT émis** ; login d'un tenant actif → garde consultée sans refus ;
refresh d'un tenant suspendu → refus **avant** la consommation de la famille de
jetons et sans aucun jeton émis.

### Preuve — non-régression suite complète

```
mvn -B -o test

[WARNING] Tests run: 1291, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 39 = **1291**. Aucun échec. Les 13 skips sont inchangés
(`PerIpRateLimiterIntegrationTest`, `@EnabledIf("isRedisAvailable")`).

### Régression réelle rencontrée et traitée — 11 tests d'intégration

**Symptôme** : après l'ajout de l'intercepteur, **11 tests** ont échoué en 403
avec `details.reason = TENANT_NOT_FOUND`.

**Diagnostic exact** : `TenantStatusGuard` lit la table `tenants` et refuse en
fail-closed un tenant introuvable (choix conforme à D1 « fail-closed »). Or
`SpaceCriticalPathIntegrationTest`, `PeopleCriticalPathIntegrationTest` et
`common.infrastructure.TenantIsolationIntegrationTest` facturaient un `tenantId`
dans le JWT **sans jamais créer la ligne `tenants` correspondante** : leurs
scénarios étaient irréalistes, puisqu'en production un `tenantId` de JWT provient
toujours d'un utilisateur rattaché à un tenant existant.

**Traitement (R9 — tests impactés mis à jour avec justification)** : ajout d'une
fixture `ensureActiveTenant(UUID)` dans les 3 classes, qui crée réellement la
ligne `tenants` (`status = ACTIVE`, `plan = DISCOVERY`). Insertion en SQL direct
et non via `TenantRepository` : Hibernate 6 lève `StaleObjectStateException` sur
un `merge()` d'entité à identifiant attribué sans ligne préexistante
(`DefaultMergeEventListener.entityIsDetached`) — défaut observé et documenté ici.

**Aucun test n'a été affaibli ni désactivé.** Preuve que la sémantique
d'isolation est intacte : `TenantIsolationIntegrationTest.egliseB_nePeutPasLireUneAmeDeEgliseA_parId`
retrouve son **404 attendu** (et non 403) une fois le tenant réellement créé.

### Points de conception

1. **Table `tenants` lue sans filtre** : l'entité `Tenant` ne porte pas
   `@Filter tenantFilter` (elle *définit* le tenant). La garde peut donc contrôler
   un tenant **cible**, ce qui est indispensable pour `/tenant-switcher/switch`.
2. **Ordre des intercepteurs** : `tenantInterceptor` → `tenantFilterInterceptor` →
   `tenantStatusInterceptor` → `featureModuleInterceptor`. Le `TenantContext` est
   donc posé avant la garde, et la garde s'exécute avant tout accès à un module.
3. **Chemins publics** : la liste n'est **pas dupliquée** ;
   `TenantStatusInterceptor` appelle `TenantFilter.shouldBypassFilter(request)`,
   source de vérité unique (contrat §3.2).
4. **Dégradation gracieuse** : si le bean `TenantStatusGuard` ou `TenantFilter` est
   absent (tests `@WebMvcTest`), l'intercepteur laisse passer — cohérent avec la
   dégradation déjà retenue pour `TenantFilterInterceptor`.
5. **`TenantService` publie `TenantStatusChangedEvent`** dans `deactivate`,
   `reactivate` et `update` (uniquement si le statut change) → invalidation
   immédiate du cache, donc **réactivation/suspension sans attendre le TTL**.

### Décision et déviations documentées

- **`detail` du 403 pour `CANCELLED`** : le contrat §3.2 donne un seul `detail`
  (« Le service de cette église est suspendu… ») associé à `title ∈
  {TENANT_SUSPENDED, TENANT_CANCELLED}`. Dire « suspendu » à une église
  résiliée serait un mensonge utilisateur. Le **`title` (code métier) est
  strictement celui du contrat** ; seul le `detail` est différencié :
  `« Le service de cette église a été résilié. Contactez le support Discipolat. »`
- **Fail-closed sur tenant introuvable** : `403 TENANT_STATUS_UNAVAILABLE` avec
  `details.reason = TENANT_NOT_FOUND`. Le contrat ne mentionne que l'erreur de
  lecture DB ; le tenant absent en relève moralement (je ne peux pas affirmer
  qu'il est actif). Aucun 500, aucun accès accordé.
- **Réflexe fail-closed sur statut `null`** : impossible en base
  (`status` est `NOT NULL`), mais traité en defense-in-depth.
- **Aucun nouveau fichier hors de la liste de la tâche** : `TenantRepository` n'a
  pas été modifié (lecture via le `findById` existant, mis en cache 30 s).

---

## A2 — Audit des mutations tenant (constat M1)

- **Statut** : DONE
- **Fichiers** :
  - MOD `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantService.java`
  - MOD `backend/src/test/java/com/discipolat/modules/tenants/domain/TenantServiceTest.java` (+6 cas)
- **Constat vérifié** : le champ `auditService` existait bien dans
  `TenantService` mais n'était **appelé nulle part** (aucune occurrence de
  `auditService.` dans le fichier avant cette tâche) : le cycle de vie complet du
  tenant (création, changement de plan, suspension, réactivation) n'était
  **aucunement tracé**.

### Preuve

```
mvn -B -o test -Dtest=TenantServiceTest -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.TenantServiceTest
[INFO] BUILD SUCCESS
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1297, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

### Événements écrits (via `AuditService.logSimple(action, "TENANT", id)`)

| Mutation | Événement(s) |
|---|---|
| `create` | `TENANT_CREATED` |
| `update` | `TENANT_UPDATED` + `TENANT_PLAN_CHANGED` **uniquement si le plan a changé** |
| `deactivate` | `TENANT_SUSPENDED` |
| `reactivate` | `TENANT_REACTIVATED` |
| `markOnboardingCompleted` (tâche A4) | `TENANT_ONBOARDING_COMPLETED` — **écrit en A4**, pas ici |

L'acteur courant est capturé par `AuditService.logSimple` via
`securityUtils.getCurrentUserId()` ; les écritures sont ensuite chaînées par
hachage (`extendHashChain`) — la chaîne d'audit reste donc inaltérable.

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| Chaque mutation écrit **exactement** un événement d'audit | `verify(auditService, times(1)).logSimple(...)` + `verifyNoMoreInteractions(auditService)` dans les 5 tests de mutation |
| Le changement de plan est tracé séparément | `update_withPlanChange_shouldAuditBothTenantUpdatedAndPlanChanged` (2 assertions, `verifyNoMoreInteractions`) |
| **Aucun audit sur les lectures** | `reads_shouldNotWriteAnyAuditEvent` → `verifyNoInteractions(auditService)` après `list()` et `get()` |
| Javadoc de la classe exact | Section « Audit (constat M1) » ajoutée dans `TenantService` |

### Note sur un test existant ajusté

`create_shouldAuditTenantCreatedExactlyOnce` exige que l'identifiant du tenant
audité soit celui de la réponse. La fixture `tenantRepository.save(...)` de ce
test attribuait un **nouveau** UUID à chaque appel, alors que `create` sauvegarde
à nouveau le même tenant dans `ensureInitialSubscription` — l'identifiant est
donc désormais attribué **une seule fois** (`if (t.getId() == null)`), ce qui
reflète le comportement réel d'une base (identifiant généré et stable dans la
transaction). Le test existant `create_shouldPersistWithActiveStatusAndDefaultPlan`
n'est pas impacté.

### Rappel de traçabilité

L'événement `TENANT_ONBOARDING_COMPLETED` listé dans la spécification A2 dépend de
`markOnboardingCompleted`, qui n'existe pas encore : il est créé en **A4** avec son
audit. Ce décalage est assumé et sans impact (le critère « chaque mutation est
auditée » reste vrai une fois A4 livrée).

---

## A3 — Wizard : DTO figés, actions métier réelles, RBAC, erreurs propres (constat B2)

- **Statut** : DONE
- **Fichiers principaux** :
  - NEW `onboarding/api/OnboardingStepResponse.java`, `OnboardingProgressResponse.java`,
    `OnboardingStatusResponse.java`, `OnboardingStepData.java`
  - NEW `onboarding/domain/OnboardingStepDefinition.java`, `OnboardingStepActions.java`,
    `TenantOnboardingStatusPort.java`
  - MOD `onboarding/domain/OnboardingWizardStep.java` (+ `skip_reason`), `OnboardingWizardService.java`
  - MOD `onboarding/api/OnboardingWizardController.java` (+ `/status`, `@authz.isTenantAdmin()`)
  - MOD `tenants/domain/InvitationService.java` (+ `createInvitation` extrait du contrôleur)
  - MOD `platform/api/InvitationController.java` (délègue au service)
  - NEW `OnboardingWizardServiceTest`, `OnboardingWizardTenantIsolationTest`,
    `OnboardingWizardControllerTest`, `InvitationServiceCreateInvitationTest`

### Preuve — tests imposés (`§4 A3`)

```
mvn -B -o test -Dtest='OnboardingWizardServiceTest,OnboardingWizardControllerTest,OnboardingWizardTenantIsolationTest,InvitationServiceCreateInvitationTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.InvitationServiceCreateInvitationTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.domain.OnboardingWizardTenantIsolationTest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.domain.OnboardingWizardServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.api.OnboardingWizardControllerTest
[INFO] Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- `OnboardingWizardServiceTest` : **24 cas** (le plan en exige ≥ 12).
- `OnboardingWizardControllerTest` : **12 cas**, en `@SpringBootTest` + `MockMvc`
  (JWT réel → `TenantInterceptor` → filtre Hibernate → `TenantStatusInterceptor`
  → `@PreAuthorize` → service) : le RBAC est donc prouvé sur la **chaîne HTTP
  réelle**, pas seulement sur une méthode isolée.
- `OnboardingWizardTenantIsolationTest` : **7 cas** d'isolation inter-tenant.
- `InvitationServiceCreateInvitationTest` : **11 cas** d'extraction sans régression.

### Preuve — non-régression suite complète

```
mvn -B -o test

[WARNING] Tests run: 1351, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 99 = **1351**. Aucun échec, 13 skips préexistants inchangés.

### Gate G-A §7.2 — critère 1 vérifié

```
$ grep -rn "orElseThrow()" backend/src/main/java/com/discipolat/modules/onboarding/
(aucune occurrence)   -> PASS
$ grep -rn "orElseThrow("  .../onboarding/
OnboardingWizardService.java:361:  .orElseThrow(() -> new DomainException(   -> un seul, AVEC message
```

`requireStepOfCurrentTenant` est le seul point d'accès à une étape par id et il
lève `DomainException(..., HttpStatus.NOT_FOUND, "STEP_NOT_FOUND")` après un
`.filter(tenantId.equals(candidate.getTenantId()))` explicite.

> ⚠️ **Note pour le vérificateur (§8.3)** : le Javadoc de `OnboardingWizardService`
> décrivait le bug corrigé en écrivant littéralement `orElseThrow()`. Ce texte
> déclenchait un **faux positif** du grep de contrôle. Il a été reformulé en
> « un `orElseThrow` sans argument » pour que le grep de la porte soit
> non ambigu. À savoir, sinon un vérificateur automatique pourrait refuser à tort.

### Bugs réels trouvés par les tests pendant cette tâche

1. **`completedSteps` comptait les étapes SKIPPED.** Première version :
   `completed` incluait `SKIPPED`, donc 6 étapes (5 COMPLETED + 1 SKIPPED)
   donnaient `completedSteps = 6` et `percentage = 100` sur 7 étapes. Le contrat
   §3.1 impose `completedSteps = 2, skippedSteps = 1, percentage = 43` pour 3
   étapes traitées sur 7. **Corrigé** : `completedSteps` ne compte que
   `COMPLETED`, `percentage = arrondi((completed + skipped) * 100 / total)`.
   Le test `getProgress_percentageIsRoundedAndCountsSkippedSteps` rejoue
   désormais l'exemple exact du contrat (2 + 1 sur 7 → 43).
2. **`PASTEUR` est un admin de tenant dans cette application**
   (`AuthorizationService.TENANT_ADMIN_ROLE_KEYS = {ADMIN, PASTEUR,
   TENANT_OWNER, TENANT_ADMIN}`). Le test RBAC utilisait `PASTEUR` et attendait
   403 : l'hypothèse était fausse, pas le code. Le rôle contrôlé est désormais
   `MEMBRE`, qui n'est pas admin de tenant.

### Points de conception et déviations documentées

1. **`GET /status` et l'achèvement global** : la méthode `markOnboardingCompleted`
   et les colonnes V183 appartiennent à **A4**. Pour que chaque commit compile
   (R3) tout en restant conforme au contrat, A3 expose déjà `GET /status` et le
   lit via un port `TenantOnboardingStatusPort` (interface du module onboarding,
   implémentée côté tenants en A4) injecté en `ObjectProvider` : port absent ⇒
   `completed = false`, ce qui est le fail-closed correct (« En configuration »).
   Aucun état statique mutable n'a été utilisé.
2. **`roleTemplate` / `GET /templates/{role}` conservé à l'identique** : le contrat
   §3.1 le déclare « inchangé ». La méthode a été réintégrée mot pour mot lors de
   la réécriture du service et un test le vérifie.
3. **`responsableId` / `chefFamilleId` de l'étape STRUCTURE** : ces colonnes sont
   `NOT NULL`, mais le contrat §3.1 n'ouvre **aucun** champ responsable/chef sur
   cette étape. R2 interdisant d'inventer un champ, l'administrateur qui
   configure l'église est retenu comme responsable et chef par défaut (il peut
   réattribuer ensuite). Documenté dans le code.
4. **`typeEvenement` du premier événement** : colonne `NOT NULL` non couverte par
   le contrat ; la valeur canonique `MEETING` (semantique « rencontre
   d'église ») est utilisée, comme dans `V158__migrate_legacy_events_to_church_event`.
   `EventService.create` positionne lui-même `statut = "PLANIFIE"`, conforme au
   contrat.
5. **`allowDarkMode` (étape BRANDING)** : `BrandingRequest` n'a pas ce champ.
   La valeur est conservée dans le `completedData` renvoyé au client, et aucune
   colonne n'est inventée.
6. **Extraction de `createInvitation`** : `InvitationController` délègue et
   reprojette les refus métier vers les **corps d'erreur historiques**
   (`legacyErrorBody`) : le comportement HTTP observable est inchangé, comme
   l'exige A3.5.
7. **Restauration du 404 après 403** : pour les tests d'intégration impactés par
   A1, la fixture `tenants` a été complétée (voir A1) — donc un accès
   inter-tenant redonne bien son **404** d'origine, pas un 403.

### Ce que A3 ne fait PAS (et pourquoi)

- **Import réel des membres** : décision D4 assumée — l'étape `MEMBER_IMPORT`
  est déclarative mais **vérifiée** (`importedCount >= 1` + audit
  `TENANT_MEMBERS_IMPORTED`). L'import réel reste le module `/imports`.
  L'interface dit `declaredOnly: true` dans le `completedData` : aucune fausse
  automatisation.
- **Appel à `TenantService.markOnboardingCompleted`** : voir point 1, posé en A4.

---

## A4 — Colonnes de complétion d'onboarding + endpoint `/status` (constat B2 / D2)

- **Statut** : DONE
- **Fichiers** :
  - NEW `backend/src/main/resources/db/migration/V183__tenant_onboarding_completion.sql`
  - MOD `tenants/domain/Tenant.java` (+ `onboardingCompletedAt`, `onboardingCompletedBy`)
  - MOD `tenants/api/TenantResponse.java` (+ 2 champs additifs **en fin** de record)
  - MOD `tenants/domain/TenantService.java` (+ `markOnboardingCompleted(UUID actorId)`)
  - MOD `onboarding/domain/OnboardingWizardService.java` (appel en fin de wizard, A3.6)
  - NEW `tenants/domain/TenantOnboardingStatusAdapter.java` (implémente le port lu par `/status`)
  - NEW `onboarding/domain/TenantOnboardingStatusPort.java` (interface, livrée en A3)
  - NEW `onboarding/domain/OnboardingStepActionsTest.java` (**23 cas** —voir « Trou de couverture comblé »)
  - MOD tests : `TenantServiceTest` (+3), `OnboardingWizardServiceTest` (+2 et nouveau ctor), `OnboardingWizardTenantIsolationTest` (nouveau ctor)

### Preuve — tests imposés (`§4 A4`)

```
mvn -B -o test -Dtest='TenantServiceTest,OnboardingWizardServiceTest,OnboardingWizardTenantIsolationTest,OnboardingStepActionsTest'

[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in ...tenants.domain.TenantServiceTest
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.domain.OnboardingWizardServiceTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.domain.OnboardingWizardTenantIsolationTest
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0 -- in ...onboarding.domain.OnboardingStepActionsTest
[INFO] BUILD SUCCESS
```

### Preuve — non-régression suite complète

```
mvn -B -o test

[WARNING] Tests run: 1379, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 127 = **1379**. Aucun échec.

### Preuve — migration V183 sur PostgreSQL réel (base vierge)

```
mvn -B -o flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55444/discifly3 ...

[INFO] Successfully validated 147 migrations
[INFO] Migrating schema "public" to version "183 - tenant onboarding completion"
[INFO] Successfully applied 147 migrations to schema "public", now at version v185
[INFO] BUILD SUCCESS
```

```
\d tenants  ->  onboarding_completed_at | timestamp with time zone
                onboarding_completed_by | uuid
\d onboarding_wizard_steps
              ->  skip_reason | text
              ->  "uk_onboarding_step_tenant_type" UNIQUE, btree (tenant_id, step_type)
```

**L'index unique fait son travail :**
```sql
INSERT ... ('…001','CHURCH_IDENTITY')  -> OK
INSERT ... ('…001','CHURCH_IDENTITY')  -> refusé
ERROR:  duplicate key value violates unique constraint "uk_onboarding_step_tenant_type"
```

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| La complétion du wizard renseigne les 2 colonnes **une seule fois** | `markOnboardingCompleted_shouldBeIdempotentAndNeverOverwriteTheOriginalDate` (date et acteur d'origine conservés, `verifyNoInteractions` sur save/audit au rejeu) |
| `GET /status` renvoie `completed=true` et `completedAt` | `getStatus_reportsCompletionWithActorWhenColumnsAreSet` + `OnboardingWizardControllerTest.getStatus_isExposed` (HTTP) |
| Un tenant non onboardé renvoie `completed=false` | `getStatus_reportsNotCompletedWhenTheTenantColumnIsAbsent` |
| `tenantId` jamais exposé hors du tenant | `/status` ne renvoie que `completed/completedAt/completedBy/totalSteps/completedSteps/skippedSteps/percentage` ; les tests d'isolation prouvent qu'aucune requête ne sort du tenant courant |
| Le flag n'est posé **qu'à la fin** | `globalCompletionIsMarkedOnlyWhenEveryStepIsSettled` (vrai, `verify(tenantService).markOnboardingCompleted(actorId)`) et `globalCompletionIsNotMarkedWhileAStepRemains` (faux, `never()`) |

### Trou de couverture comblé

Les tests de A3 **mockaient** `OnboardingStepActions` : aucune des 7 actions
métier n'était donc réellement vérifiée, alors que le critère d'acceptation
d'A3.4 exige « chaque étape produit un effet réel vérifiable ».

`OnboardingStepActionsTest` (**23 cas**) comble ce trou : chaque action est
prouvée appelant le **vrai** service (`OrganizationNodeService.updateNode` /
`createRootChurch`, `TenantSettingsService.updateSettings` / `updateBranding`,
`DepartmentService.create`, `FamilyService.create`, `InvitationService.createInvitation`,
`TenantFeatureService.enableFeature` après validation catalogue,
`EventService.create`), avec son audit, et avec **zéro écriture** en cas de donnée
invalide (`verifyNoInteractions`).

### Décisions et déviations documentées

1. **`TenantResponse` : constructeur de compatibilité à 15 champs ajouté.** Les
   deux champs additifs sont bien **en fin** de record comme l'impose le plan,
   mais un constructeur secondaire à 15 champs évite de casser
   `PlatformProvisioningServiceTest` et `TenantRegistrationServiceTest` qui
   construisent le record directement. L'ajout reste réellement additif.
2. **Port `TenantOnboardingStatusPort` + `TenantOnboardingStatusAdapter`.** Le
   fichier de l'implémentation n'est pas listé dans A4 : il était nécessaire
   pour que `GET /status` (contrat §3.1) lise réellement les colonnes V183.
   Alternative écartée : injection de `TenantRepository` dans le module
   onboarding (couplage direct) — le port évite ce couplage, reste sans état
   statique, et se dégrade proprement si absent.
3. **`markOnboardingCompleted` lit le tenant via `TenantContext`** et renvoie
   `false` (no-op) s'il n'y a pas de contexte — au lieu de lever, pour ne pas
   faire échouer une complétion d'étape légitime dans un flux sans tenant.
4. **`markOnboardingCompleted` n'écrase JAMAIS** une date de fin existante, et
   n'enregistre l'acteur qu'à la première complétion.
5. **Numérotation des migrations** : le plan annonçait « dernier existant V182 » ;
   le dernier réel est **V177**. V183/V184/V185 restent donc libres, mais leave
   un **trou de numérotation** (178-182 inutilisés). Sans conséquence sur une
   installation neuve ou existante (177 → 183 s'applique dans l'ordre), et c'est
   ce que j'ai vérifié. Voir NEED-HELP-01.
6. **`skip_reason`** est fusionné dans `completedData` (clé `skipReason`) plutôt
   qu'exposé comme champ du contrat §3.1, qui n'en prévoit pas.

---

## A5 — Provisionnement : owner obligatoire + email d'activation (constat B3)

- **Statut** : DONE
- **Fichiers** :
  - NEW `platform/domain/TenantOwnerProvisioningService.java`
  - MOD `platform/domain/PlatformProvisioningService.java`
  - MOD `platform/api/PlatformProvisioningController.java`
  - NEW `platform/domain/TenantOwnerProvisioningServiceTest.java` (7 cas)
  - MOD `platform/domain/PlatformProvisioningServiceTest.java` (+1 cas, ordre d'appel)

### Constat vérifié

`POST /api/v1/platform/admin/provisioning` créait le tenant, l'église racine, le
département, le nœud de département et la famille… **mais aucun compte
administrateur**. Le tenant était créé puis immédiatement inexploitable, sans
que rien n'automatisait ni ne documente la création d'un compte propriétaire.

### Preuve

```
mvn -B -o test -Dtest='TenantOwnerProvisioningServiceTest,PlatformProvisioningServiceTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...PlatformProvisioningServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in ...TenantOwnerProvisioningServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1387, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 135 = **1387**. Aucun échec.

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| Aucun tenant créé sans owner | `refusesToProvisionATenantWithoutOwnerAndWritesNothing` : `OWNER_REQUIRED` levé **avant** `tenantService.create` — vérifié par `verify(tenantService, never()).create(any())`, `never()` sur `createRootChurch`, `departmentService`, `familyService` et même sur la résolution du plan. **Zéro écriture.** |
| L'owner reçoit un email d'activation | `createsOwnerUserAndMembership` : `verify(authService).sendActivationEmail(userId)` — le flux d'activation **existant** est réutilisé, pas réimplémenté |
| Un email déjà utilisé dans un autre tenant est refusé sans création partielle | `refusesEmailAlreadyUsedInAnotherTenant` : `409 OWNER_EMAIL_ALREADY_USED`, puis `never()` sur `userRepository.save`, `membershipRepository.save` et `verifyNoInteractions(authService)` |
| Idempotence si l'owner existe déjà dans le même tenant | `doesNotDuplicateMembershipWhenOwnerAlreadyMember` : `alreadyMember = true`, aucun doublon de membership, aucun nouvel `User` |
| SMTP cassé = booléen, pas d'exception (D10) | `smtpFailureNeverBreaksProvisioning` : `activationEmailSent = false` et la membership est bien créée |
| Mot de passe initial jamais communiqué | `initialPasswordIsRandomStrongAndNeverLeaked` : BCrypt de 32 caractères aléatoires (`SecureRandom`), deux hachages distincts pour deux emails, et `passwordEncoder.matches("password123", hash) == false` |
| Rôle et statut du compte owner | `PASTEUR` + `roles={PASTEUR}` + `activeRole=PASTEUR` + `PENDING_ACTIVATION` (le propriétaire définit son propre mot de passe via le lien) |

### Décisions et déviations documentées

1. **`OWNER_EMAIL_ALREADY_USED` : `DomainException` et non `BusinessRuleException`.**
   Le plan (A5.1) demande `BusinessRuleException` **avec un statut 409**, mais
   `GlobalExceptionHandler:43-52` ne sait mapper `BusinessRuleException` que vers
   **400** (défaut) ou **403** (préfixes `FEATURE_DISABLED_`/`QUOTA_`) : le 409
   y est **inatteignable**. J'ai donc utilisé
   `DomainException(message, HttpStatus.CONFLICT, "OWNER_EMAIL_ALREADY_USED")` —
   le **code métier est exactement celui du plan**, seul le véhicule change pour
   rendre le statut 409 atteignable (convention déjà retenue par le wizard, D6).
   → voir **NEED-HELP-03** pour validation de l'orchestrateur.
2. **Validation de l'owner en TÊTE de `provision`**, donc avant la résolution du
   plan et avant `tenantService.create` : le refus est antérieur à toute écriture.
3. **L'owner est provisionné APRÈS l'église racine et AVANT département/famille**
   (`PlatformProvisioningServiceTest` le prouve par `InOrder`) : `Department.responsableId`
   et `Family.chefFamilleId` sont `NOT NULL`, un propriétaire valide doit donc
   exister avant ces créations.
4. **Membership `TENANT_OWNER` via `roleLegacy` uniquement.** Le rôle
   `TENANT_OWNER` est un rôle **global** (`tenant_id IS NULL`) et
   `TenantMembership.role` pointe vers une entité `Role`. L'utiliser ici
   introduirait un rôle dans le tenant alors qu'il est global par conception ;
   `roleLegacy` est le champ prévu pour ce cas, et c'est
   `AuthorizationService.isTenantAdmin` qui le lit en repli
   (`membership.getRole() != null ? getRole().getKey() : getRoleLegacy()`).
5. **Constructeurs de compatibilité** ajoutés à `Command` (27 args),
   `ProvisioningResult` et `AtomicProvisioningRequest` : l'ajout reste
   réellement additif et aucun appelant n'est cassé. Un `Command` construit par
   l'ancien constructeur est aujourd'hui **refusé** avec `OWNER_REQUIRED`, ce qui
   est exactement le fail-closed voulu (une ancienne version du client web ne
   peut plus créer d'église sans propriétaire).
6. **Aucun secret en dur** : le mot de passe initial est généré, haché, et
   jamais journalisé ni renvoyé.

---

## A6 — Emails d'inscription + endpoint public de statut (constat M2)

- **Statut** : DONE
- **Fichiers** :
  - MOD `authentication/domain/EmailService.java` (+ 3 emails d'inscription, retour `boolean`)
  - MOD `platform/domain/TenantRegistrationService.java` (+ emails, + `registrationStatus(email)`)
  - MOD `platform/domain/TenantRegistrationRequestRepository.java` (+ `findByEmailIgnoreCase`)
  - NEW `authentication/api/RegistrationStatusRequest.java`, `RegistrationStatusResponse.java`
  - MOD `authentication/api/AuthController.java` (+ `POST /registration-status`)
  - MOD `common/infrastructure/config/PerIpRateLimiter.java` (+ `tryConsumeRegistrationStatus` + métriques)
  - MOD `backend/src/main/resources/application.yml` (+ `registration-status-*`)
  - NEW `platform/domain/TenantRegistrationEmailTest.java` (4 cas)
  - NEW `authentication/api/AuthControllerRegistrationStatusTest.java` (7 cas)
  - MOD `platform/domain/TenantRegistrationServiceTest.java` (nouveau ctor + mock)

### Constat vérifié

Le parcours d'inscription d'une église était **100 % silencieux** : ni accusé de
réception à la soumission, ni notification d'approbation, ni notification de
rejet. Le demandeur n'avait **aucune preuve** que sa demande était arrivée, et
l'approbation comme le rejet étaient invisibles.

### Preuve

```
mvn -B -o test -Dtest='TenantRegistrationEmailTest,AuthControllerRegistrationStatusTest,TenantRegistrationServiceTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in ...AuthControllerRegistrationStatusTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ...TenantRegistrationEmailTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in ...TenantRegistrationServiceTest
[INFO] BUILD SUCCESS
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1398, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 146 = **1398**.

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| Soumission → email reçu | `submitSendsRegistrationReceived` (email **normalisé** en minuscules) |
| Approbation → email d'approbation | `approveSendsRegistrationApproved` (avec le lien `frontendUrl + "/login"`) |
| Rejet → email avec motif | `rejectSendsRegistrationRejected` (le motif du Super Admin est transmis) |
| Aucun échec SMTP ne casse la transaction métier | `submitSurvivesSmtpFailure` : l'email renvoie `false`, la demande reste `PENDING_APPROVAL` et persistée. Décision D10 respectée par `sendTracked` (jamais de `throw`, seulement `log.error`) |
| `registration-status` rate-limité | `rateLimitedReturns429` : `429` + `Retry-After: 300` + `no-store`, et **`verify(registrationService, never())`** : le quota protège aussi la base |
| Sans fuite d'information | `responseLeaksNothingSensitive` : le corps ne contient ni `password`, ni `passwordHash`, ni `organizationName`, ni `userId`, ni `slug`, ni `tenantId` |
| `Cache-Control: no-store` | asserted sur les 6 réponses (4 nominaux + 429 + leak) |
| `reason` seulement si `REJECTED` | `rejectedReturnsReason` (présent) vs `approvedReturnsCanLoginTrue` / `pendingReturnsPendingApproval` (absent) |
| `canLogin` = `APPROVED` | les 4 cas nominaux |

### Décisions et déviations documentées

1. **Fenêtre de rate-limit : 5 minutes.** Le plan §4 A6 ne mentionne que
   `registration-status-capacity/refill` (défauts 3/3) mais le contrat §3.3 exige
   « **3 requêtes / 5 minutes / IP** ». J'ai donc aussi ajouté
   `registration-status-period-minutes: 5` (avec la même clé en
   `application.yml`). Sans ce period, 3 requêtes/minute auraient laissé 15
   tentatives par fenêtre de 5 minutes — 5× plus permissif que le contrat.
2. **Endpoint `429` : corps `status = NONE`** plutôt qu'un corps d'erreur vide.
   Cohérent avec le fait que la réponse ne révèle rien sur l'existence d'un compte :
   un client qui reçoit `429` ne doit pas pouvoir déduire que l'email existe.
   `Retry-After` et `X-RateLimit-Remaining: 0` sont malgré tout fournis, comme
   pour les autres endpoints.
3. **`findByEmailIgnoreCase`** ajouté à `TenantRegistrationRequestRepository` et
   utilisé aussi par `submit` : l'email est une identité globale unique
   (constat B4 / V185), une recherche sensible à la casse aurait pu créer deux
   demandes pour la même adresse selon la casse saisie.
4. **Le fichier de la tâche annonçait « 4 nouvelles méthodes » dans
   `EmailService`** : 3 sont livrées ici (`sendRegistrationReceived`,
   `sendRegistrationApproved`, `sendRegistrationRejected`). La 4ᵉ est
   `sendInvitationWelcome`, qui appartient à la tâche **A9** (§3.4) et n'est pas
   utilisée par le parcours d'inscription.
5. **Aucun secret ni URL interne dans la réponse** : `loginUrl` n'est envoyé que
   dans l'**email** au demandeur, jamais dans la réponse HTTP.

---

## A8 — Quotas espaces, événements, églises + alerte admin (constat M3)

- **Statut** : DONE
- **Fichiers** :
  - MOD `tenants/domain/QuotaService.java` (+ `checkCanCreateSpace`, `checkCanCreateEvent`, `checkCanCreateCampus`, alerte sur dépassement, repli `spaces` supprimé)
  - NEW `tenants/domain/QuotaAlertService.java`
  - MOD `events/domain/EventRepository.java` (+ `countByTenantIdAndStatutNotInAndDeletedFalse`)
  - MOD `spaces/domain/SpaceService.java` (quota appelé dans `createSpace`)
  - MOD `events/domain/EventService.java` (quota appelé dans `create`)
  - MOD `tenants/domain/OrganizationNodeService.java` (quota appelé dans `createNode`)
  - NEW `tenants/domain/QuotaServiceSpacesEventsTest.java` (11 cas)
  - NEW `tenants/domain/QuotaAlertServiceTest.java` (5 cas)
  - MOD tests : `QuotaServiceTest`, `SpaceServiceTest`, `EventServiceTest`,
    `SpaceCriticalPathIntegrationTest`, `PeopleCriticalPathIntegrationTest` (fixtures)

### Constat vérifié

`checkCanCreateChurch` et `checkCanCreateDepartment` **existaient déjà** mais
n'étaient appelés que par `QuotaController` (un endpoint de simulation « puis-je
créer ? »). **La création réelle n'était donc jamais bornée** : un tenant pouvait
dépasser son quota d'églises, de départements, de campus, d'espaces ou
d'événements sans jamais être refusé. Aucun administrateur n'était prévenu non
plus.

### Risque R-2 du plan : vérifié, et infirmé

Le plan annonce (risque R-2) : « Limites `spaces`/`events` absentes des plans
seedés (V144) → A8 refuse des créations légitimes ».

**Vérification faite : R-2 est infondé.** `V177__complete_canonical_saas_plans.sql`
réécrit `limits_json` **en entier** pour les 4 plans canoniques :

| Plan | `spaces` | `events` | `max_churches` | `max_departments` | `max_campuses` |
|---|---|---|---|---|---|
| DISCOVERY | 3 | 10 | 1 | 3 | 1 |
| STARTUP | 10 | 50 | 3 | 10 | 3 |
| GROWTH | 25 | 200 | 10 | 25 | 10 |
| NETWORK | 100 | 1000 | 100 | 100 | 50 |

(V144 pose `spaces`/`events`, V135 les `max_*`, V177 **complète** le tout.)
Aucune nouvelle migration n'est donc nécessaire, et le fail-closed n'affecte
aucune création légitime. Le risque est documenté ici pour que le vérificateur ne
le ressuscite pas.

### Preuve

```
mvn -B -o test -Dtest='QuotaServiceSpacesEventsTest,QuotaAlertServiceTest,QuotaServiceTest,SpaceServiceTest,EventServiceTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in ...QuotaServiceSpacesEventsTest
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0 -- in ...QuotaAlertServiceTest
[INFO] Tests run:  3, Failures: 0, Errors: 0, Skipped: 0 -- in ...QuotaServiceTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...SpaceServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in ...EventServiceTest
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1414, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 162 = **1414**.

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| Créer un espace/événement/église au-delà de la limite → 403 `QUOTA_*` | `spaceAtLimitIsRefusedWithAlert`, `eventAtLimitIsRefusedWithAlert`, `churchQuotaIsEnforced`, `campusQuotaIsEnforced`. Les codes commencent par `QUOTA_` → `GlobalExceptionHandler` les mappe en **403** |
| Notification créée pour chaque admin | `onlyTenantAdminsAreNotified` : 2 notifications pour `TENANT_OWNER` + `tenant_admin`, **0** pour `PASTEUR` et `MEMBRE` |
| Aucune régression sur les quotas existants | suite complète verte (users, storage, IA, cours, messages) |
| Limite absente → refus (D11 fail-closed) | `spaceWithoutLimitFailsClosed`, `eventWithoutLimitFailsClosed` (et **aucune** lecture de consommation : refus d'emblée) |
| L'alerte n'est jamais bloquante | `alertIsTriggeredAndNeverBlocksTheRefusal` : l'alerte lève, le refus `QUOTA_EXCEEDED_SPACES` est bien propagé ; `oneFailedNotificationDoesNotStopTheOthers` |

### Bugs et anomalies réels trouvés et corrigés

1. **Repli erroné sur la clé `spaces`** dans `organizationLimit`. Un tenant sans
   `max_churches` se voyait appliquer le quota d'**espaces** (3 sur DISCOVERY) à
   ses **églises** : deux ressources confondues silencieusement. Retiré — chaque
   ressource a sa clé, et son absence est un refus explicite.
2. **`EventController.create` ne renseigne pas `tenantId`** sur l'entité (auto-fill
   Hibernate à la persistance). Le contrôle de quota s'exécutant **avant**, il
   cherchait un tenant `null` et refusait **toute** création d'événement
   (`TENANT_NOT_FOUND`). Corrigé par `EventService.resolveTenantId(event)` qui
   retombe sur `TenantContext.requireTenantId()`. **Sans cette correction, la
   création d'événement aurait été cassée en production** — attrapée par
   `PeopleCriticalPathIntegrationTest.eventDressCodeArchivesFlow`.
3. **Statuts clos d'événement vérifiés** avant d'écrire la requête : l'entité
   `Event` n'a que `statut` (String, défaut `PLANIFIE`) et `deleted` (boolean) ;
   les statuts réellement clos sont `TERMINE` et `ANNULE`
   (`EventService:590`). Le test `eventCountingExcludesClosedStatuses` verrouille
   la liste exacte passée au repository.

### Fixtures de test ajoutées (et pourquoi elles sont légitimes)

`SpaceCriticalPathIntegrationTest` et `PeopleCriticalPathIntegrationTest` ont
reçu `ensurePlanAndSubscription()` : les deux échouaient désormais en
`QUOTA_CONFIGURATION_INVALID`, non pas à cause d'une règle métier, mais parce
qu'en H2 (base créée par Hibernate, **sans Flyway**) il n'existe ni plan de
catalogue ni abonnement.

Pièges rencontrés et documentés en commentaire :
- `saas_plans` a pour **clé primaire la colonne `key`** (`@Id @Column(name="key")`) :
  il n'existe pas de colonne `id` sur cette table ;
- les colonnes `limits_json` / `quotas_json` sont des `jsonb`
  (`@JdbcTypeCode(SqlTypes.JSON)`) : un `INSERT` SQL de texte brut les stocke en
  `byte[]` et la relecture Hibernate échoue, ce qui rendait le plan invalide.
  L'insertion passe donc par les **repositories**, pas par du SQL ;
- l'id de `tenant_subscriptions` est laissé **généré** : attribuer un id à la main
  fait passer Spring Data sur `merge()`, que Hibernate 6 refuse
  (`StaleObjectStateException`) pour une entité à identifiant attribué sans
  ligne préexistante.

### Point de vigilance production (non bloquant, à surveiller)

`lockPlan` refuse un tenant **sans abonnement actif**. C'est le comportement
fail-closed déjà en production pour le quota utilisateurs, donc le précédent
existe ; mais l'extension à espaces/événements/églises touche désormais des
créations plus fréquentes. Si un tenant historique n'a pas d'abonnement, ses
créatures d'espaces/événements échoueront en `QUOTA_CONFIGURATION_INVALID`.

**Requête de contrôle recommandée avant déploiement en production** :
```sql
SELECT t.id, t.name
FROM tenants t
LEFT JOIN tenant_subscriptions s ON s.tenant_id = t.id AND s.status IN ('ACTIVE','TRIAL','PAST_DUE')
WHERE s.id IS NULL;
```
Tout tenant retourné doit recevoir un abonnement avant le déploiement, sinon il
sera bloqué. Je n'ai pas pu l'exécuter : je n'ai pas accès à la base de
production.

---

## ARBITRAGES DE L'ORCHESTRATEUR (2026-09-28) — NEED-HELP 02, 03 et vigilance clos

Les trois points bloquants ont été soumis à l'orchestrateur humain, qui a
validé les trois options recommandées :

| # | Question | Décision de l'orchestrateur | Conséquence |
|---|---|---|---|
| **NEED-HELP-02** | Preuve de validation des migrations | **Acceptée : preuve PostgreSQL hors CI** | `mvn verify` ne prouve rien sur les migrations ; la preuve officielle de A12 sera la validation sur PostgreSQL réel documentée (147 migrations sur base vierge, index/colonnes vérifiés, doublons refusés). L'ajout d'un test Testcontainers reste **hors périmètre** et pourra être repris dans `ORC-A5` (CI bloquante). |
| **NEED-HELP-03** | `OWNER_EMAIL_ALREADY_USED` en 409 | **Validé : `DomainException` + `HttpStatus.CONFLICT`** | Le code métier est celui du plan ; seul le véhicule d'exception change, `BusinessRuleException` ne sachant produire ni 409. Aucun correctif supplémentaire requis. |
| **Vigilance production** | Trou de numérotation 178-182 + tenants sans abonnement actif | **Documenter et continuer** | Aucune migration hors périmètre n'est créée. La requête de contrôle des tenants sans abonnement figure dans la section A8 et doit être exécutée par l'orchestrateur **avant déploiement**. |

Le plan reste inchangé (fichier d'autorité non modifié) : ces décisions sont
consignées ici, dans le fichier de progression de l'Agent A, conformément à `R8`.

---

## A9 — Invitations : répertoire, email de bienvenue, relances J-3/J-1 (constat M4)

- **Statut** : DONE
- **Fichiers** :
  - NEW `backend/src/main/resources/db/migration/V184__invitation_reminder_tracking.sql`
  - NEW `platform/domain/InvitationReminderScheduler.java`
  - MOD `tenants/domain/Invitation.java` (+ `remindedAt`)
  - MOD `tenants/domain/InvitationRepository.java` (+ `findByStatusAndExpiresAtBetween`)
  - MOD `tenants/domain/InvitationService.java` (inscription au répertoire à l'acceptation)
  - MOD `authentication/domain/EmailService.java` (+ `sendInvitationWelcome`, `sendInvitationReminder`)
  - MOD `platform/api/InvitationController.java` (`welcomeEmailSent` dans la réponse d'acceptation)
  - NEW `tenants/domain/InvitationDirectoryRegistrationTest.java` (5 cas)
  - NEW `platform/domain/InvitationReminderSchedulerTest.java` (11 cas)
  - MOD 3 tests d'invitation existants (nouveau constructeur `InvitationService`)

### Constat vérifié

Avant ce correctif, l'acceptation d'une invitation ne produisait **aucun** email,
**aucune** fiche au répertoire, et l'invitation expirait **silencieusement** au
bout de 7 jours : l'église avait un compte invisible dans le répertoire et ne
savait pas qu'une invitation pendait.

### Preuve

```
mvn -B -o test -Dtest='InvitationDirectoryRegistrationTest,InvitationReminderSchedulerTest,InvitationServiceTest,InvitationServiceCrossTenantTest,InvitationServiceCreateInvitationTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationReminderSchedulerTest
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationDirectoryRegistrationTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationServiceCreateInvitationTest
[INFO] Tests run:  7, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationServiceCrossTenantTest
[INFO] Tests run:  6, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationServiceTest
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1430, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 178 = **1430**.

### Critères d'acceptation

| Critère | Preuve |
|---|---|
| Une acceptation crée la personne **exactement une fois** | `registersPersonInDirectory` (création avec source `INVITATION`) + `doesNotDuplicateAnExistingPerson` (`verifyNoInteractions(peopleService)` quand la fiche existe) |
| **Source `INVITATION`** | asserté sur le 3ᵉ argument de `registerPerson` dans 3 tests |
| Un email de bienvenue est tenté | `sendInvitationWelcome` appelé par `InvitationController.acceptInvitation` ; `welcomeEmailSent` renvoyé dans la réponse |
| Les relances partent **une seule fois par palier** | `sameTierIsNeverSentTwice` (`sent == 0`, aucun `save`) + `nextTierIsStillSentAfterPreviousOne` (J-3 déjà passé ⇒ J-1 envoyé quand même) |
| Aucun envoi pour invitations acceptées/annulées | `onlyPendingInvitationsAreConsidered` : la requête filtre `eq(InvitationStatus.PENDING)` |
| Les fenêtres J-3 / J-1 sont correctes | `windowIsCentredOnTheTier` (NOW+3j ± 12 h) et `oneDayWindowIsNarrower` (NOW+1j ± 6 h), vérifiés au `verify` exact |

### Décisions et déviations documentées

1. **La relance ne reconstruit PAS de lien d'invitation.** Le token n'est stocké
   que **haché** (V173/V175) : il est mathématiquement impossible de retrouver le
   lien à partir de la ligne `invitations`. Plutôt que d'inventer un lien qui ne
   fonctionne pas, la relance redirige vers `/login` et le message explique que
   l'invitation expire bientôt. Un « faux lien » serait pire que pas de lien :
   l'invité cliquerait et comprendrait que l'application est cassée.
2. **`reminded_at` est positionné seulement si l'envoi a réussi.** Un échec SMTP
   ne marque pas l'invitation : le job réessaiera au prochain passage, sinon une
   panne SMTP temporaire ferait définitivement perdre la relance.
3. **Le scheduler n'est jamais bloquant.** `sendOne` encapsule tout dans un
   `try/catch` : une exception d'un tiers ne doit pas arrêter le job pour tous
   les tenants. De même, un échec d'écriture d'audit n'annule pas une relance
   déjà partie (sinon elle serait renvoyée au prochain passage). Prouvé par
   `auditFailureDoesNotBreakTheReminder`.
4. **Prénom dérivé de l'email quand l'invitation n'en fournit pas.**
   `Person.first_name` est `NOT NULL`, et une acceptation cross-tenant (décision
   D3) se fait sans mot de passe donc souvent sans nom. Le local-part de l'email
   est utilisé, avec « Membre » en dernier recours. Prouvé par
   `derivesFirstNameFromEmailWhenAbsent`.
5. **Échec du répertoire ≠ échec d'acceptation.** Le compte et la membership sont
   déjà créés et valides : les faire échouer parce que le répertoire est en panne
   laisserait l'invité sans accès. Le journal `warn` trace l'incident.
   Prouvé par `directoryFailureNeverBreaksAcceptance`.
6. **Index `idx_invitations_status_expires`** ajouté par V184 : le scheduler
   balaie les invitations PENDING par fenêtre d'expiration une fois par jour ; sans
   cet index, c'est un scan séquentiel de `invitations`.

### Validation de la migration V184

Appliquée avec les 146 autres sur base vierge PostgreSQL 16.15 lors de la
validation A4/A7 (`now at version v185`). La colonne `reminded_at` est
`TIMESTAMPTZ` nullable, donc **sans risque de refus sur une base existante**.

---

## A10 — Finitions wizard & invitations (mineurs de l'audit)

- **Statut** : DONE
- **Fichiers** :
  - MOD `platform/api/InvitationController.java` (pagination + filtres, rétro-compatible)
  - MOD `tenants/domain/InvitationRepository.java` (+ `searchForAdmin` paginé)
  - NEW `onboarding/domain/OnboardingWizardInitializeConcurrencyTest.java` (2 cas, 2 vrais threads)
  - MOD `platform/api/InvitationControllerTest.java` (+6 cas de pagination/filtre)
  - (A10.3 — champ `config` neutralisé et `completedData` documenté : **livré en A3**)

### Preuve

```
mvn -B -o test -Dtest='InvitationControllerTest,OnboardingWizardInitializeConcurrencyTest' -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in ...InvitationControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...OnboardingWizardInitializeConcurrencyTest
[INFO] BUILD SUCCESS
```

```
mvn -B -o test     (suite complète)

[WARNING] Tests run: 1438, Failures: 0, Errors: 0, Skipped: 13
[INFO] BUILD SUCCESS
```

1252 (baseline) + 186 = **1438**.

### A10.1 — Pagination et filtres, sans casser l'usage existant

| Requête | Comportement | Preuve |
|---|---|---|
| `GET /admin/invitations` (sans `page`) | **Liste complète**, strictement comme avant | `listWithoutPageKeepsTheLegacyFullList` : 2 éléments, et `verify(never()).searchForAdmin(...)` |
| `?page=0&size=50` | `PageResponse` (convention existante du dépôt) | `listWithPageReturnsPageResponse` : `content/page/size/totalElements/totalPages` |
| `?status=PENDING` | Filtre transmis en majuscules | `listFiltersByStatus` : `verify(searchForAdmin(eq(tenantId), eq("PENDING"), isNull(), any()))` |
| `?status=PEUT-ETRE` | Statut inconnu **ignoré**, pas d'erreur 400 | `unknownStatusFilterIsIgnored` |
| `?q=email` | Recherche partielle transmise, `q` < 2 caractères ignoré | `listSearchQueryIsForwarded` (le `trim` est aussi vérifié) |
| `?size=5000` | Borné à 200 | `listPageSizeIsBounded` : `ArgumentCaptor<Pageable>` ⇒ `getPageSize() == 200` |

### A10.2 — Concurrence sur `initialize`

`OnboardingWizardInitializeConcurrencyTest` utilise **deux vrais threads** et une
`CyclicBarrier` :

- `concurrentInitializationNeverDuplicatesSteps` : les **deux premières lectures**
  renvoient volontairement « aucune étape » (c'est la fenêtre réelle entre le
  SELECT et le INSERT des deux requêtes concurrentes). Résultat : **exactement 7
  étapes** en base, et **8 tentatives d'insertion** — 7 par le gagnant, 1 par le
  perdant qui échoue sur l'index unique `uk_onboarding_step_tenant_type` puis
  relit. Aucun doublon, aucun 500.
- `integrityViolationIsAbsorbedAndReread` : une `DataIntegrityViolationException`
  sur le premier `save` est absorbée et les 7 étapes existantes sont relues.

### A10.3 — Champ `config` neutralisé (livré en A3)

Le champ `config` (JSON legacy) n'est plus lu par la logique et n'est plus
exposé par l'API ; la colonne est **conservée** (aucune suppression de colonne).
`completedData` est documenté et sérialisé en objet JSON. Le test
`getSteps_neverLeaksTheLegacyConfigColumn` (A3) verrouille qu'aucune fuite ne
revient.

### Décisions et déviations documentées

1. **Rétro-compatibilité vérifiée, pas supposée.** Le plan dit « sans paramètres
   → comportement actuel ». C'est implémenté **et prouvé** par test, avec un
   `verify(never())` sur la voie paginée : impossible de régresser par erreur.
2. **Requête native pour la recherche paginée** plutôt qu'un `Specification` : les
   filtres optionnels sont combinés par `CAST(:status AS VARCHAR) IS NULL OR …`,
   ce qui garantit qu'un filtre absent n'écarte aucune ligne, et la recherche
   email est normalisée en minuscules comme partout ailleurs.
3. **Statut inconnu ignoré plutôt que 400.** Un client qui evolue (ou une
   mauvaise saisie) ne doit pas casser l'écran d'invitations ; le filtre est
   simplement neutralisé.
4. **`q` de moins de 2 caractères ignoré** : une recherche d'une lettre
   ramènerait tout le répertoire sans valeur pour l'utilisateur et avec un coût
   de scan.
5. **La fenêtre de course est rendue déterministe** dans le test (2 premières
   lectures vides). Sans cela, le test aurait pu passer sans jamais exercer la
   course — c'est précisément le piège des tests de concurrence.

---

## A15 — Correction `AuthService` magic-link (message/code inversés)

- **Statut** : DONE
- **Fichiers** :
  - MOD `authentication/domain/AuthService.java` (`verifyMagicLink`)
  - MOD `authentication/domain/AuthServiceTest.java` (+2 cas)

### Constat vérifié

`BusinessRuleException` suit la convention `(message, code)`, et
`GlobalExceptionHandler` place le **code** dans le `title` du `ProblemDetail` —
le champ que les clients lisent. Les deux exceptions du magic link avaient leurs
arguments **inversés** :

```java
// AVANT — le code part dans le detail, le message français part dans le title
throw new BusinessRuleException("MAGIC_LINK_EXPIRED", "Lien magique invalide ou expiré");
throw new BusinessRuleException("USER_NOT_FOUND", "Aucun compte associé à cet email");
```

Conséquence pour le client : `title = "USER_NOT_FOUND"` et
`detail = "Aucun compte associé à cet email"` — l'inverse de ce qu'attend le
contrat, et un switch sur le message au lieu du code.

### Correction

```java
throw new BusinessRuleException("Lien magique invalide ou expiré", "MAGIC_LINK_EXPIRED");
throw new BusinessRuleException("Aucun compte associé à cet email", "USER_NOT_FOUND");
```

### Preuve

```
mvn -B -o test -Dtest=AuthServiceTest -DfailIfNoSpecifiedTests=false

[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 -- in ...authentication.domain.AuthServiceTest
[INFO] BUILD SUCCESS
```

Deux cas dédiés, qui vérifient **les deux champs** :
- `magicLinkExpired_shouldExposeTheCodeInCodeAndTheFrenchTextInMessage` :
  `getCode() == "MAGIC_LINK_EXPIRED"` **et** `getMessage() == "Lien magique invalide ou expiré"` ;
- `magicLinkUnknownUser_shouldExposeTheCodeInCodeAndTheFrenchTextInMessage` :
  `getCode() == "USER_NOT_FOUND"` **et** `getMessage() == "Aucun compte associé à cet email"`.

> Note : `MagicLinkEntry` n'est stocké qu'en mémoire (map statique) avec une durée
> de 15 minutes ; le test du cas « utilisateur inconnu » génère donc son token et
> le consomme immédiatement.

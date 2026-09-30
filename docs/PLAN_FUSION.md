# Plan de fusion — prêt à l'exécution, en attente du signal de l'orchestrateur

> Rédigé le **2026-09-30**. Aucune fusion n'a été faite. Aucune branche n'a été
> poussée. Ce document décrit l'ordre exact, les conflits **déjà mesurés** et les
> arbitrages qui restent à rendre.
>
> Vérifications de ce document : toutes en lecture seule (`git merge-tree
> --write-tree`, `git merge-base --is-ancestor`, requêtes SQL sur conteneurs
> jetables). Aucune n'a modifie un worktree.

---

## 0. État des six branches

| branche | base | tip | commits non poussés |.worktree propre |
|---|---|---|---|---|
| `main` | — | `72ec85d5` | 0 | oui |
| `fix/schema-drift-h1-h5` (agent A) | main | `dbcb8563` | 20 | **NON — 6 fichiers en cours** |
| `fix/onboarding-tenant-clients` (agent B) | main | `15f7dfc7` | 17 | oui |
| `fix/securite-webhooks-2fa-sync` | main | `55ca8af9` | 1 | — |
| `docs/architecture-et-reprise` | main | `e733dce0` | 1 | — |
| `chore/arreter-de-pister-le-build` | ↑ docs | `55117684` | 2 | — |

Les trois dernières sont nées de `main` pour committer le travail qui y était
en suspens (§0.2 et §0.3 du TODO de reprise), conformément à R1 : `main` ne
produit pas de code.

`chore/arreter-de-pister-le-build` est **enfant de** `docs/architecture-et-reprise`
(les deux commits d'hygiène sont unscoped sur la révision v1.1 des documents
d'autorité). Les deux se fusionnent donc d'un bloc.

---

## 1. Ce qui est déjà vérifié sur le push

`origin/main` (`21e74abe`) est **ancêtre** des deux branches de campagne :

```
$ git merge-base --is-ancestor origin/main fix/schema-drift-h1-h5    -> vrai
$ git merge-base --is-ancestor origin/main fix/onboarding-tenant-clients -> vrai
```

Les 5 commits amont absents de `main` local (`3ea0c7ad` RGPD … `21e74abe`) sont
**déjà** dans A et dans B. Conséquence : une fois `main` fast-forwarré sur
l'intégration, `git push origin main` est un **fast-forward propre**. Aucun
`--force` n'est nécessaire, sur aucune branche. Le risque pour le distant est nul.

---

## 2. Ordre de fusion

L'intégration se fait **dans le worktree B**, qui est propre et inactif. Le
worktree A n'est **pas** touché (voir §4).

```bash
cd /home/arise/discipolat/discipolat_app-agentB
git checkout -b integration/d1-onboarding-tenant          # de B (15f7dfc7)
git merge --no-ff fix/schema-drift-h1-h5 -m "merge: A+B, arbitrage D1 (table vivante event)"
#   -> 1 conflit : Event.java  (§3.1)
# -> supprimer V194                                         (§3.2)
# -> rejouer le gate Flyway + la suite backend
git merge --no-ff chore/arreter-de-pister-le-build -m "merge: documents d'autorite v1.1 + hygiene git"
#   -> 4 conflits                                            (§3.3)
git merge --no-ff fix/securite-webhooks-2fa-sync -m "merge: lot securite (2FA, webhooks, sync mobile)"
#   -> 0 conflit, mesure
```

Puis, seulement après validation :

```bash
cd /home/arise/discipolat/discipolat_app
git merge --ff-only integration/d1-onboarding-tenant
git push origin main fix/schema-drift-h1-h5 fix/onboarding-tenant-clients \
           fix/securite-webhooks-2fa-sync docs/architecture-et-reprise \
           chore/arreter-de-pister-le-build integration/d1-onboarding-tenant
```

---

## 3. Conflits — tous déjà comptés, avec leur résolution

### 3.1 `Event.java` — 1 conflit (A × B)

`git merge-tree --write-tree fix/schema-drift-h1-h5 fix/onboarding-tenant-clients`
ne signale **que** ce fichier, malgré 306 fichiers touchés par les deux branches
(ils partagent la base `2d64e1c3` et se complètent).

**Résolution : prendre la version de B.** Motif : à cet instant, A n'a
**committé** que des migrations (V203, V204) et son port Java est encore
*non committé* (§4). La version committée de `Event.java` côté A est donc
l'**avant-port** (`@Table("events")`), et la version B est le port
**complet et prouvé** : gate de 8 tests sur PostgreSQL 16 migré de zéro, preuve
rouge incluse, suite backend à 1506 tests verts.

### 3.2 `V194__restore_events_table_name.sql` — à SUPPRIMER, décision requise

`V194` (branche A) renomme `legacy_events` → `events`. C'est la **résolution
opposée** de l'arbitrage D1, et elle est incomplète : après ce renommage, la
table `events` **n'a ni `latitude`, ni `longitude`, ni `geofence_radius_m`**, que
`Event.java` mappe. Mesuré sur PostgreSQL 16 réel. `/api/v1/events` échouerait
quand même.

Les deux migrations **ne peuvent pas coexister**. `V194` doit donc partir.

⚠ **Conséquence à connaître avant de le faire** : `V194` est appliqué dans
`onb-e2e-postgres` (base jetable de la recette, up depuis 43 h). Le supprimer de
la chaîne y provocera un *« detected applied migration not resolved locally »*.
Il faut donc, dans cet ordre : recréer la base jetable **ou** passer
`flyway repair` dessus. La production (`kfokam48`) n'a que 4 migrations
appliquées et **aucun V194** : elle n'est pas concernée.

### 3.3 `.gitignore` + 2 artefacts — 3 conflits (chore × A/B)

- `.gitignore` : A et B ont ajouté des lignes depuis `main`. Résolution : **garder
  l'union** des trois versions.
- `frontend/dist-ts/tsconfig.node.tsbuildinfo` et `frontend/dist-ts/vite.config.d.ts.map` :
  `modify/delete` — supprimés par la branche chore, modifiés par A et B (c'est du
  cache de compilation qui change à chaque build). Résolution : **garder la
  suppression**. C'est l'objet même du commit d'hygiène.

### 3.4 `AGENT_ORCHESTRATION.md` — 1 conflit (docs × A/B)

`main` carries la révision v1.1 du document ; A et B ont leur propre évolution de
ce fichier (A y a consigné les NEED-HELP du §5.5). Résolution : **prendre la
version la plus récente des deux branches de campagne**, puis réappliquer les
delta de la v1.1 de `main` par-dessus. C'est le seul conflit qui demande un
arbitrage éditorial, pas une décision de schema.

---

## 4. Le point bloquant : le travail en cours de l'agent A

Au moment de la rédaction, le worktree A contenait **6 fichiers modifiés non
committés**, portant sur exactement les fichiers que B avait déjà portés :
`Event.java`, `EventService.java`, `EventRepository.java`,
`EventRegistrationRepository.java`, `EventController.java`,
`ChurchEventRepository.java`.

Deux faits mesurés :

1. **Le worktree A ne compile pas.** `ChurchEventRepository` pointe déjà sur
   `Event`, mais `ChurchEvent.java` existe toujours et les 9 consommateurs n'ont
   pas été repris. `mvn -o compile` → `cannot find symbol` sur
   `findByDateDebutBetweenAndDeletedFalse` et 5 autres.
2. **L'agent A écrivait encore** : les 6 fichiers avaient été modifiés 15 à
   75 secondes avant la mesure.

**Ce travail n'a donc pas été committé**, pour trois raisons : il ne compile
pas ; il fait doublon avec le port de B, complet et prouvé ; et le worktree
était en cours d'écriture, y toucher l'aurait corrompu.

**Conséquence sur le plan** : si l'agent A commite son port avant le signal, le
conflit `Event.java` passera de 1 à **6 fichiers**. Il faudra alors choisir
explicitement la version de B sur les six, et écarter celle de A.

**Recommandation** : demander à l'agent A d'abandonner son port au profit de
celui de B (`git checkout -- .` dans son worktree), puisque B est complet,
prouvé et compatible avec l'arbitrage retenu. La décision lui revient, pas à moi.

---

## 5. Vérifications à rejouer après fusion, dans cet ordre

1. `EventTableContractTest` (Testcontainers, PostgreSQL 16 **migré de zéro**,
   `ddl-auto: none`) — 8 tests. C'est le seul qui voie une dérive de migration.
2. Suite backend complète : référence actuelle **1506 tests, 0 échec, 0 erreur,
   13 skips**. Ne jamais verdir un test désactivé pour obtenir ce chiffre.
3. `FlywayMigrationChainPostgreSqlTest` (gate de l'agent A) — version cible à
   mettre à jour après le retrait de V194.
4. `scripts/verify-tenant-onboarding.sh` sur PostgreSQL et Redis **jetables**,
   jamais le 8080 de production. Référence avant travaux : 51 PASS / 0 FAIL /
   7 SKIP.
5. `npx tsc -b`, `npx vitest run` (430 tests), `flutter analyze`, `flutter test`
   (468 tests) — le mobile n'a pas été touché par l'intégration, mais le merge
   réécrit des fichiers partagés.

---

## 6. Ce qui reste en suspens, et qui n'est pas du ressort de ce plan

- **`audit_event.hash`** : `V135` crée la colonne en `CHAR(64)`, `AuditEvent.java:65`
  la déclare `varchar(64)`. Le profil `dev` (`ddl-auto: validate`) **ne démarre
  donc sur aucune base migrée**. Module audit, hors périmètre events.
- **`D2`** (`users.tenant_id` vs tenant d'action) et **`D3`** (bascule
  cross-tenant non validée bout-en-bout) : arbitrage non rendu, `EventService`
  n'est pas concerné mais l'étape `FIRST_EVENT` du wizard en dépend.
- **`D6`** : publication de `assetlinks.json` / `apple-app-site-association`, et
  `usesCleartextTraffic="true"` dans `AndroidManifest.xml`. Actions ops.

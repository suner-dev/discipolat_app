# PASSE-PLAT À L'AGENT A — travaux backend non faits

> Rédigé le 2026-09-29 par l'Agent B (frontend/mobile), qui **ne touche pas** `backend/**` (R1).
> Chaque ligne est vérifiée par lecture du code réel dans le worktree de l'Agent A.
> Ordre proposé : **P0 (sécurité) → P1 (contrat) → P2 (fraîcheur)**.

---

## P0 — Sécurité : 9 comptes de démonstration dans le schéma de production

**Constat, en 3 étapes vérifiables :**

1. `V2__seed_data.sql:9-19` insère **9 utilisateurs `ACTIVE`** :
   `pasteur@`, `responsable1@`, `responsable2@`, `chef1@`, `chef2@`, `faiseur1..4@discipolat.com`,
   avec `password_hash = 'PLACEHOLDER'`.
2. `V8__fix_password_hashes.sql:12-16` remplace `PLACEHOLDER` par le BCrypt de **`password123`**
   — le mot de passe est écrit en clair dans l'en-tête du fichier.
3. `V70__add_multitenancy.sql:179` fait `UPDATE users SET tenant_id = '00000000-…-0001' WHERE
   tenant_id IS NULL` → ces comptes sont rattachés au **tenant par défaut**.

Aucune migration ne les supprime, et **aucune ne crée de `tenant_memberships`** pour eux.
Or `AuthService:147` ne contrôle que `tenantStatusGuard.assertAccessible(user.getTenantId())` :
le tenant par défaut étant ACTIVE, **le login aboutit et délivre un JWT** avec le rôle
`PASTEUR` / `RESPONSABLE` / `FAISEUR`.

**Le runtime de démo, lui, est correctement protégé** — à ne pas casser :
`DataInitializer.run()` → `boolean demoOrBeta = !isProductionEnvironment() && (seedDemoAccounts ||
"beta".equalsIgnoreCase(environment))`, défaut `DEMO_SEED_ENABLED:false`.
Le problème n'est **pas** le code : c'est que les migrations, elles, créent ces comptes.

**Correctif proposé — une seule migration, fail-closed, sans perte de donnée :**

```sql
-- V192__neutralize_demo_accounts.sql
-- Les 9 comptes créés par V2 (mot de passe « password123 », documenté dans le
-- dépôt) sont neutralisés : ils ne peuvent plus s'authentifier, mais aucune
-- donnée métier n'est supprimée.

UPDATE users
   SET statut = 'INACTIVE',
       deleted = true,
       updated_at = CURRENT_TIMESTAMP
 WHERE email IN ('pasteur@discipolat.com', 'responsable1@discipolat.com',
                 'responsable2@discipolat.com', 'chef1@discipolat.com',
                 'chef2@discipolat.com', 'faiseur1@discipolat.com',
                 'faiseur2@discipolat.com', 'faiseur3@discipolat.com',
                 'faiseur4@discipolat.com')
   AND password_hash = '$2a$10$xf6qwOh4g8AidlGwgyD8S.Vbl7FVv3dNkO5GI7.iE/dgrveA5/j..';

-- Filet de sécurité : si le hash évolue, on rattrape par le suffixe d'email.
DO $$
DECLARE remaining int;
BEGIN
    SELECT count(*) INTO remaining FROM users
     WHERE email LIKE '%@discipolat.com' AND statut = 'ACTIVE' AND deleted = false;
    IF remaining > 0 THEN
        RAISE EXCEPTION 'V192: % compte(s) de démonstration encore actif(s) — décision requise', remaining;
    END IF;
END $$;
```

⚠️ **Deux réserves à trancher par l'orchestrateur avant d'appliquer :**
- **Numéro de migration** : l'Agent A travaille en ce moment sur `V190`/`V191`. Vérifier le dernier
  numéro committed avant de choisir.
- **Le `RAISE EXCEPTION` est délibérément fail-closed** : si quelqu'un a créé un vrai compte en
  `@discipolat.com` (démo ≠ donnée réelle), la migration échoue au lieu de le désactiver en silence.
  C'est le bon défaut, mais il faut le savoir.

**Vérification à exécuter après** (2 minutes, à faire en même temps que la recette A14) :
```
docker compose -f <isolated> up -d db     # base jetable
mvn -B flyway:migrate
curl -s -XPOST .../api/v1/auth/login -d '{"email":"pasteur@discipolat.com","password":"password123"}'
# attendu : 401/403 — PAS un jeton
```

---

## P1 — Contrat : le module `tasks` du mobile n'a jamais eu de backend

`features/tasks/services/tasks_service.dart` (mobile) appelle **11 endpoints qui n'existent pas** :
`/tasks`, `/tasks/{id}`, `/tasks/{id}/assign`, `/tasks/{id}/status`, `/tasks/kanban/columns`,
`/tasks/kanban/columns/{id}`, `/tasks/overdue`, `/tasks/reports/by-assignee`,
`/tasks/reports/by-status`, `/tasks/reports/statistics`, `/tasks/templates`.

Le seul contrôleur existant est `gantt/api/TeamTaskController.java` → `@RequestMapping("/api/v1/team-tasks")`,
avec `UUID` en `@PathVariable` et `PATCH /{id}/status`, `PATCH /{id}/progression`. Il n'expose ni
kanban, ni overdue, ni reports par assignee/statistiques, ni templates.

Le web n'a **que** `TeamTasksPage.tsx` sur `/team-tasks` : le web est aligné, le mobile non.

**Décision demandée** (pas de code de ma part) :
- **(a)** Porter les filtres du mobile sur `/api/v1/team-tasks` — quelques heures, côté Agent B.
  **C'est l'option que je recommande** : elle supprime le doublon au lieu de le créer.
- **(b)** Construire `/api/v1/tasks` en plus — costly, et il faudrait justifier pourquoi deux APIs
  de tâches coexistent.
- **(c)** Supprimer le module orphelin côté mobile.

Le module fantôme n'est plus routable (route `/tasks` retirée le 2026-09-29), donc **ce n'est plus
un défaut visible par l'utilisateur** : c'est de la dette à arbitrer.

---

## P2 — Fraîcheur : les 13 TODO backend, triés

| Fichier | TODO | Nature |
|---|---|---|
| `core/service/OutboxConsumers.java:285` | déléguer à `FinanceService` (dépenses TCO automatiques) | Manque la délégation |
| `core/service/OutboxConsumers.java:290` | déléguer à `AnalyticsService` (compteurs) | Manque la délégation |
| `tenants/domain/ConfigurationResolver.java:223` | injecter `SimpMessagingTemplate` / `ApplicationEventPublisher` | Configuration non câblée |
| `people/service/PeopleService.java:182` | filtres avancés (campus, sans espace, sans famille) | **Fonctionnalité annoncée et non faite** (jointures) |
| `people/service/PeopleService.java:281` | contrôle `authorizationService` | **Trou de sécurité potentiel** à qualifier |
| `families/service/FamilyOSService.java:269` | filtre par campus | Fonctionnalité |
| `families/service/PermissionResolver.java:71` | appendices de pastorat | Fonctionnalité |
| `backup/infrastructure/BackupDescriptorRepository.java:20` | reprendre un `CREATE TABLE IF NOT EXISTS` en migration | Migration manquante (module backup créé par l'Agent A) |
| `events/domain/EventTask.java:46` | `status = "TODO"` | **valeur métier**, pas un marqueur — fausse positive |

**Priorité P2 : `PeopleService:281`** (autorisation) avant le reste. Le reste est de la fonctionnalité,
pas de la correction.

---

## P3 — Ce que l'Agent B ne touchera pas, et qui reste ouvert chez l'Agent A

- **La suite backend ne tourne pas sur PostgreSQL** (`application-test.yml` : `flyway.enabled: false`,
  `ddl-auto: create-drop`, `h2-init.sql` de 7 lignes). C'est la cause racine de H1/H2 : des 500 en
  production avec une suite verte. Testcontainers est **déclaré dans `pom.xml` mais utilisé par 0
  test** (`grep -rl @Testcontainers backend/src/test` → 0). C'est le chantier le plus rentable de
  toute la liste, et il est déjà à moitié installé.
- **`ddl-auto` selon le profil** : `none` (prod, `application.yml:21`), `update` (profil `docker`,
  **:253**), `validate` (**:279**), `create-drop` (tests). Un profil où Hibernate écrit le schéma est
  un défaut de production.
- **JaCoCo absent** du `pom.xml`, donc le « 1 469 tests verts » n'est accompanied d'aucune mesure.
- **Arbitrages D1 / D2 / D3** du plan (H2 `events`, `users.tenant_id` vs tenant d'action, validation
  bout-en-bout du switch cross-tenant) : toujours ouverts, et ils bloquent 4 scénarios E2E côté client.

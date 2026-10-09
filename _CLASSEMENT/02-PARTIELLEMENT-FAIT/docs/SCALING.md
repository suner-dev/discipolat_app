# SCALING — Trajectoire d'échelle de Discipolat (A3 / M7)

> Document d'architecture vivante, produit du chantier ORC-A3. État de vérité :
> **Niveau 1 exploité, fondations Niveau 2-3 posées dans le code**. Rien de ce
> qui est décrit plus bas n'est « déjà en place » ; chaque niveau dit
> explicitement ce qui reste à faire.

## Cible produit

Plusieurs **millions de tenants**, des **centaines de millions d'églises** à
terme, sur **toute la planète**. Le dimensionnement n'est pas une option :
c'est la contrainte qui a dicté les choix figés ci-dessous.

## Ce qui est PROUVÉ dans le code aujourd'hui (Niveau 1)

| Mécanisme | Preuve |
|---|---|
| Isolation applicative par `tenant_id` | `@Filter(tenantFilter)` sur les entités + `TenantContext` + `TenantIsolationIntegrationTest` |
| Schéma partagé, une base PostgreSQL | `docker-compose.yml`, Flyway V1→V191 |
| Index composés `(tenant_id, …)` sur les tables chaudes | migration `V191__index_composes_tables_chaudes.sql` (vérifiés contre `pg_indexes`, que les manquants ajoutés) |
| Frontière d'abstraction DataSource/tenant | `common/scaling/TenantDataSource` + `SingleDatabaseTenantDataSource` (délégation stricte, zéro régression) |
| Fonction de routage figée `hash(tenantId) % N` | `common/scaling/ShardRouting` (splitmix64 sur les 16 octets de l'UUID, stable inter-JVM, testée) |
| Devises ISO-4217 exactes par transaction | `V190`, `Iso4217CurrencyValidator`, `GET /api/v1/platform/currencies` |

## Trajectoire — Niveau 1 → Niveau 5

### Niveau 1 — Monolithe durci (ÉTAT ACTUEL, ~0 → 20 000 tenants)
Une base, une app, plusieurs instances stateless derrière un load balancer.
Coût : 1 gros nœud PostgreSQL (ex. 16 vCPU / 64 Go / NVMe) ≈ 200-600 €/mois.
**Point de bascule** : verrouillages/maintenance d'une table chaude > minutes, ou connections saturées malgré PgBouncer.

### Niveau 2 — Partitionnement local (~20 000 → 200 000 tenants)
Convertir les tables les plus volumineuses (mesure `pg_class.reltuples`, jamais
d'intuition) en tables partitionnées RANGE sur `tenant_id`.
**Ne reste pas à l'état de migration Flyway** : le chemin d'exécution sûr est
documenté pas à pas dans [`scripts/partition-hot-tables.sql`](../scripts/partition-hot-tables.sql)
(table parallèle + contrôle d'exhaustivité + bascule par RENAME, table
d'origine conservée). Coût : le même nœud, +place disque 2× pendant la bascule.
**Point de bascule** : croissance de la table > verrouillage récurrent en VACUUM/REINDEX.

### Niveau 3 — Horizontal partitioning / sharding de clusters (~200 000 → 2 M tenants)
Le code est **déjà prêt** : `TenantDataSource.dataSourceFor(tenantId)` est le
seuil à franchir. Travaux restants :
1. Implémenter un `ShardedTenantDataSource` (map shard→DataSource, pool par shard) ;
2. Placer le DataSource dans `AbstractRoutingDataSource` au niveau Hibernate ;
3. Migrer un tenant d'un shard à l'autre à chaud (gel écritures + copie + bascule) ;
4. Régler `app.scaling.shard-count` (N figé pour la vie de l'anneau : changer N
   = re-sharding planifié, jamais un re-hash sauvage).
Coût : ×N clusters PostgreSQL (≈ 300-900 €/mois par shard de 200 k tenants).

### Niveau 4 — Cellules complètes par région (~2 M → 10 M tenants)
Réplique applicative + DB + Redis + stockage de fichiers par cellule
(ex. UE / US / APAC), routage par localisation du tenant (RGEO ou enregistrement
à l'onboarding). Bénéfice collatéral : conformité locale des données (RGPD,
LGPD, NDPR) — la localisation devient une propriété du tenant, pas une exception.
Coût : ~3 cellules minimum ≈ 5-15 k€/mois infra, hors bande passante sortante.

### Niveau 5 — Séparation des charges + sharding fonctionnel (> 10 M tenants)
- tables « flux » (notifications, events, audit) sur un chemin d'écriture
  dédié (Kafka/queue → ingestion batch) ;
- gros tenants « whale » dans leur propre cluster (tiering commercial Premium) ;
- lecture analytique hors du chemin transactionnel (réplica ou entrepôt).

## Ce qui est HONNÊTEMENT resté à faire (ne pas le masquer)

- Aucun `ShardedTenantDataSource` multi-cluster : seul le seam existe, assumé.
- Aucune table convertie en partitionnée : la base de référence n'a aucune
  table à l'échelle qui le justifie ; le script prêt-à-exécuter est livré.
- Le cache applicatif (Redis) et les files d'attente restent mono-instance.
- La réplication inter-régions (Niveau 4) n'est pas amorcée.

## Comment mesurer (à faire avant chaque décision, jamais à l'aveugle)

```sql
-- Volume réel par table (estimation du planificateur) :
SELECT relname, reltuples::bigint AS est_rows, pg_size_pretty(pg_total_relation_size(oid))
  FROM pg_class WHERE relkind='r' ORDER BY reltuples DESC LIMIT 15;
-- Attentes de locks / connections (signal Niveau 1→2) :
SELECT count(*) FROM pg_stat_activity;
SELECT max(n_wait) FROM (SELECT count(*) n_wait FROM pg_locks GROUP BY pid) s;
```

Métriques applicatives à surveiller : P95 par endpoint, connexions pool,
taille `notifications`, `pg_stat_statements` top-10 temps total.

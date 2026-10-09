# ADR-006 — Événements comme produit (outbox → broker + schémas) puis data/IA en actif

> **Statut** : **Accepté sur le principe** (humain 2026-10-09) — **exécution non démarrée**, et
> **volontairement placée après ADR-005** (ledger) : la décision de modèle de données d'abord,
> l'infrastructure de flux ensuite. Aucun code touché par ce document.
> **Date** : 2026-10-09. **Mesures** : exécutées sur `main` @ `9148caa1`.
> **Couvre** les plaques **P4** (événements) et **P5** (data & IA) de
> [VALORISATION-PLATEFORME.md](VALORISATION-PLATEFORME.md) §2.

---

## 1. Ce qui existe déjà (c'est la bonne nouvelle, et elle est chiffrée)

| Brique | État mesuré | Preuve |
|---|---|---|
| **Transactional outbox** | **réelle** : table `outbox_event` avec `tenant_id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload_json jsonb`, `status`, `attempts`, `available_at`, `created_at`, `published_at` ; statuts `PENDING / PUBLISHED / FAILED / DISCARDED` | `modules/core/domain/OutboxEvent.java`, migrations |
| **Distribution** | un `OutboxPublisher` + `OutboxDispatcher` (@Scheduled `fixedDelay = 5000`, plus un passage de nuit `cron = "0 0 3 * * *"`) + `OutboxConsumers` qui enregistre **95 consommateurs** | `modules/core/service/` |
| **Déduplication par consommateur** | **table `processed_event`** (`consumer`, `event_id`, `processed_at`) → l'idempotence de consommation est déjà un concept du système | `modules/core/domain/ProcessedEvent.java` |
| **Latence temps réel** | relais « stream » invoqué **après commit** de la transaction métier (jamais d'événement fantôme sur rollback), publié sur `/topic/tenant:{id}/events` ; les clients dédupliquent par `eventId` | `OutboxPublisher` §G5.8, `OutboxConsumers:149` |
| **Webhooks sortants** | **signés HMAC-SHA256** (`X-Discipolat-Signature`) + `WebhookDeliveryLog` (donc rejouables/auditable), `ApiKey` pour l'authentification tierce | `modules/webhooks/domain/WebhookService.java:121-199` |
| **Seam LLM** | un seul point de sortie : `LlmProviderService` (13 fichiers / 1 927 l. dans `ai`), avec **metering déjà né** (`AiUsage`, `AiCreditsService`) | `modules/ai/domain/` |
| **Sharding / caches** | `ShardedTenantDataSource`, `ShardRouting`, `TenantShardExecutor`, Redis tenant-aware | `common/scaling`, `common/caching` |

Un acquéreur qui demande « avez-vous du découplage événementiel ? » peut donc être répondu **oui,
avec preuve**, et pas « oui, sur PowerPoint ». C'est exactement ce que la due-diligence achète.

## 2. Les quatre limites mesurées (ce qui empêche d'appeler ça une plateforme d'événements)

| # | Limite | Conséquence business |
|---|---|---|
| 1 | **Aucun broker** : `grep -rli "kafka\|amqp\|debezium" backend/src/main/java` → **0**, et aucune dépendence dans `backend/pom.xml`. Tout est **in-process** dans le même JVM | un consommateur lent ou qui lève bloque la distribution des autres ; impossible de faire tourner un consommateur sur une autre machine (GPU, batch) |
| 2 | **Un seul consommateur par type d'événement** : `Map<String, Consumer<OutboxEvent>> consumers` avec `consumers.put(eventType, …)` → **dernier inscrit gagne** | brancher un 2ᵉ usage (analytics, data lake, partenaire) sur un type existant **remplace silencieusement** le premier → une classe entière de pannes muettes |
| 3 | **Pas de schéma de contrat** : `event_type` est une `String(100)`, `payload_json` un `jsonb` libre → ni version, ni type imposé, ni compatibilité vérifiable | un consommateur tiers se casse au premier renommage de champ, et on ne peut pas le savoir **avant** de déployer |
| 4 | **Aucun actif data** : `dashboard` (1 589 l.) et `search` (1 070 l.) agrègent à la volée côté applicatif ; `gdpr` (7 fichiers) contient **0 occurrence de « consent »** | pas de rétention mesurable par cohortes, pas de valeur de données accumulée, et une réponse GDPR incomplète sur la lignée du consentement |

## 3. Décision

### 3.1 P4 — événements (dans cet ordre, chaque étape autonome et monnayable)

1. **Règle d'abord, infra ensuite** : transformer `consumers` en **liste par type** (un événement,
   N consommateurs indépendants, chacun avec son curseur et sa `processed_event`). Aucun broker
   requis, ~une journée, et ça supprime la limite §2.2 **immédiatement**.
2. **Registry de schémas + AsyncAPI** : chaque `event_type` obtient un schéma **versionné**
   (`semver`, compatibilité *backward* imposée en CI), généré et vérifié comme `openapi.json`
   l'est déjà pour le REST. Un changement cassant = une nouvelle version majeure + une période de
   double émission. **Le contrat est un actif** : il rend les intégrations tierces vendables.
3. **Relais outbox → broker managé** (Redpanda ou Kafka managé) **en ajout, pas en remplacement** :
   l'outbox reste la source de vérité locale ; un *tap* publie les mêmes événements sur un topic.
   Bénéfices : consommateurs hors JVM, rejouabilité par offset, et **le lac se remplit tout seul**.
   Trigger d'adoption (§2.1 franchi) : un 3ᵉ consommateur réel d'un même flux, ou un consommateur
   qui ne peut pas tourner dans le JVM (batch GPU).
4. **CQRS sur les trois lectures chères** (`dashboard`, `search`, `analytics`) : projections
   matérialisées alimentées par les événements, Redis pour la fraîcheur, SQL applicatif en repli.
   Objectif chiffré : p95 < 300 ms sur le dashboard principal, **mesuré**, pas espéré.

### 3.2 P5 — data & IA (après P4, jamais avant)

5. **CDC (Debezium) → lakehouse** : Iceberg sur S3 + dbt + un entrepôt columnaire (ClickHouse ou
   Snowflake). La table `outbox_event` devient un **abonnement**, pas une refonte : rien à changer
   dans le domaine.
6. **Couche sémantique** : un dictionnaire versionné (`membre`, `fidèle`, `présence`, `don`,
   `department`) dans dbt. C'est **ça** le fossé, pas les modèles : sans sémantique unique, chaque
   tableau de bord réinvente la définition et la donnée ne capitalise pas.
7. **L'IA reste un adaptateur** : la règle est déjà vraie dans le code (`LlmProviderService` est le
   seul point de sortie) — elle est **écrite en règle d'architecture** et vérifiée par ArchUnit :
   `domain/` ne dépend de rien qui parle à un fournisseur de LLM. Les modèles consomment des
   **features** calculées dans le lac, pas des appels en direct dans une requête utilisateur.
8. **Consentement et lignée** : journal d'événements de consentement (qui, quand, pour quel
   traitement, retiré comment) **avant** toute exploitation analytique des données personnelles —
   c'est une condition d'achat enterprise et une obligation GDPR, et aujourd'hui **0 ligne** ne le porte.
9. **Metering** : `AiUsage`/`AiCreditsService` deviennent le socle d'un comptage **par tenant**
   (tokens, appels, stockage, requêtes) → facturation à l'usage et profilage de coût de service.

## 4. Ce que cet ADR ne fait PAS

- Il **ne migre pas** `ApplicationEventPublisher` → Kafka « pour faire moderne ». Le relais est un
  *tap* sur une outbox **déjà existante**, et l'outbox n'est pas retirée.
- Il **n'extrait aucun service** : la contrainte du SCC de 41 contexts s'applique intégralement
  (VALORISATION §2). Brancher un broker ne rend pas les contexts indépendants.
- Il **ne promet pas d'IA** : sans P5 (§3.2.6), une brique « agents IA » est un coût récurrent sans
  actif. Aucune ligne d'IA n'est commandée ici.
- Il **ne touche pas** aux contrats HTTP en vigueur, ni au gel ArchUnit (qui ne doit pas monter).

## 5. Options rejetées (et pourquoi)

| Option | Rejetée parce que |
|---|---|
| Publier un sujet MQTT / un bus dès maintenant, avant la liste par type | le défaut §2.2 (dernier inscrit gagne) se reproduirait à l'identique, mais avec un réseau et des offsets à gérer |
| Kafka auto-hébergé | 3+ brokers, Zookeeper/KRaft, rebalances : l'exploitation n'est pas encore payée par le revenu ; un managé ou Redpanda suffit |
| Refondre les événements en Axon / event sourcing intégral | l'event sourcing *remplace* la base de records ; ici on a 150 904 lignes et un `deleted` logique partout : ça ne se reprend pas d'un coup |
| Data lake avant ledger (ADR-005) | un lac dont la source monétaire n'est pas prouvable empoisonne chaque indicateur en aval, et c'est **invisible** jusqu'à l'audit |
| Un « feature store » acheté maintenant | rien à servir : les features pertinentes (rétention, attrition) supposent le lac et la sémantique du §3.2 |

## 6. Lot exécutable (après ADR-005 §6) — rouge puis verte

| Tâche | Contenu | Rouge | Verte |
|---|---|---|---|
| **E1** | `consumers` → liste par type + curseur par consommateur | un test enregistre **deux** consommateurs sur un même type : aujourd'hui le premier ne reçoit rien (rouge) | les deux reçoivent, la dédup `processed_event` reste correcte par consommateur |
| **E2** | Schéma + registry (`event_type` versionné, vérifié en CI) | renommer un champ d'un payload **passe** aujourd'hui sans rien casser côté CI → test de contrat rouge | un changement cassant sans bump de version **rougit** le pipeline |
| **E3** | Tap outbox → topic (managé, `allow_failure` d'abord) | — | un événement métier écrit dans l'outbox apparaît sur le topic, et le relire ne double aucun effet |
| **E4** | Projections CQRS `dashboard` / `search` | mesure p95 réelle avant/après (le rouge est le **chiffre**, pas un test) | p95 < 300 ms sur le parcours mesuré, et la vue SQL de repli reste verte |
| **E5** | Journal de consentement + sémantique dbt | absence totale (§2.4) prouvée par grep | le consentement d'un membre est traçable (qui/quand/pourquoi/retrait) |

**Gates** : `mvn -o test` inchangé et vert ; gates PostgreSQL 19/19 ; gel ArchUnit non augmenté ;
les 95 types consommés existants gardent leur comportement (test de non-régression par type).

## 7. Comment revérifier les faits

```bash
grep -rli "kafka\|amqp\|debezium" backend/src/main/java | wc -l                       # 0
grep -n "Map<String, Consumer<OutboxEvent>> consumers" backend/src/main/java/com/discipolat/modules/core/service/OutboxPublisher.java
grep -n "consumers.put" backend/src/main/java/com/discipolat/modules/core/service/OutboxPublisher.java   # dernier inscrit gagne
grep -c "registerConsumer(" backend/src/main/java/com/discipolat/modules/core/service/OutboxConsumers.java # 95
grep -rn "fixedDelay" backend/src/main/java/com/discipolat/modules/core/service/OutboxDispatcher.java     # 5000
grep -rni "consent" backend/src/main/java/com/discipolat/modules/gdpr | wc -l          # 0
grep -n "hmacSha256\|X-Discipolat-Signature" backend/src/main/java/com/discipolat/modules/webhooks/domain/WebhookService.java
```

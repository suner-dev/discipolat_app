# ADR-002 — Backend : monolithe modulaire propre (hexagonale + DDD), par strangulation

> **Statut** : proposé. **Date** : 2026-10-09. **Contexte mesuré** : `main` @ `4f196242`,
> 135 modules `com.discipolat.modules/<x>/{api,domain,service,repository,config}`, ~1 540 fichiers
> Java, ~150 kLOC, **un seul `pom.xml`** (pas de multi-module). Déjà présents et sains :
> multi-tenant Hibernate (`TenantFilterIntegrator`), **sharding** (`ShardedTenantDataSource`),
> **transactional outbox** (`OutboxEvent`/`OutboxPublisher`/`OutboxConsumers`), Redis tenant-aware,
> WebSocket temps réel, webhooks fail-closed, paiement USSD, mode basse-bande (`LowBand`).
> **Doc approfondie** : [backend-target-architecture.md](backend-target-architecture.md).
> **Exécution immédiate** : [`/TODO_BACKEND_V0_CLEAN_ARCH.md`](../../TODO_BACKEND_V0_CLEAN_ARCH.md).

---

## Décision

Le backend adopte une **architecture propre hexagonale (ports & adaptateurs) + conception par
domaines (DDD)**, appliquée à un **monolithe modulaire** dont les frontières de modules sont
**rendues mécaniquement étanches** (ArchUnit + multi-module Maven), puis **stranglé** bounded
context par bounded context. On **n'extrait en services autonomes que sur critère explicite**
(scale, résidence, équipe), jamais d'emblée.

## Les 6 engagements non négociables

1. **Domaine pur** : `domain/` n'importe **ni Spring, ni JPA, ni HTTP**. La politique métier est
   testable sans contexte Spring.
2. **Cas d'usage = une classe = une transaction.** Le `*Service`God-object (p. ex.
   `DepartmentManagementService` 1545 l., `DashboardService` 1480 l.) éclate en use cases.
3. **Frontières étanches par la machine** : un module ne touche un autre module **que** par un port
   partagé ou un **événement** — jamais par un import interne. Vérifié par ArchUnit **et** par la
   compilation multi-module.
4. **Contrats publiés** : OpenAPI (REST) + **AsyncAPI** (événements), régénérés en CI, avec tests de
   contrat web↔mobile↔serveur (on étend `churchesSuggestExistsContract`, déjà vert).
5. **Outbox → broker** : l'outbox existante devient le relais vers un bus (Kafka/Redpanda). Le
   domaine ignore le transport ; seul un adaptateur outgoing connaît le broker.
6. **Lecture et écriture séparées quand ça coince** : les *reads* lourds passent en **projections
   CQRS** (vues matérialisées / cache) plutôt qu'en agrégation à la volée.

## Options considérées et rejetées (avec raison)

| Option | Rejetée parce que |
|---|---|
| **Microservices d'emblée** sur 135 modules | Aucune équipe pour les porter ; latence réseau + distribution des transactions + enfers d'ops ; on casserait le meilleur atout de détection des dérives de contrat (le diff transversal, cf. ADR-001). |
| **Nettoyage sans outil** (« on fera attention ») | Une règle non exécutée par une machine n'est pas une règle : la dérive de couplage revient en semaines. |
| **Réécriture from scratch** | 150 kLOC à re-certifier, 0 valeur fonctionnelle pendant des mois, régressions introuvables. Le choix le plus cher et le moins défendable. |
| **CQRS/Event Sourcing partout** | Ceremony injustifiée sur l'immense majorité des CRUD. On le réserve aux vrais goulots (dashboard, recherche, ledger). |

## Conséquences

- **Positives** : due-diligence « architecture prouvée » ; extraction de services qui devient un
  simple déplacement de frontière ; observabilité et contrats vérifiables ; le multi-tenant existant
  se prolonge naturellement vers 3 régimes d'isolation.
- **Coûts / risques** : le multi-module Maven sur 135 modules est bruyant la 1ʳᵉ semaine (mitigé :
  on commence par **ArchUnit seul**, qui ne casse RIEN, puis on découpe par vagues) ; le broker
  ajoute une dépendance d'infra (mitigé : relais outbox déjà découplé du domaine).
- **Règle de conduite** : chaque PR = **une** frontière ou **un** use case, tests au vert, zéro
  comportement modifié. Une PR d'architecture n'apporte pas de fonctionnalité ; les deux flux
  cohabitent.

## Comment cette décision se vérifie (critères de fini)

- `mvn verify` échoue si un `domain/` importe `org.springframework..` ou `jakarta.persistence..`.
- `mvn verify` échoue si `modules/A` accède aux internes de `modules/B` (hors port/événement).
- Le rapport de complexité cyclomatique par méthode est publié et borne les méga-services.
- `openapi.json` et `asyncapi.yaml` sont générés en CI et testés contre les clients web + mobile.

*Reproductible : `grep -rl "org.springframework" backend/src/main/java/com/discipolat/modules/*/domain | wc -l`
(état actuel du domaine non pur) ; `find backend -maxdepth 2 -name pom.xml` (un seul module) ;
`grep -rn "ApplicationEventPublisher" backend/src/main/java | wc -l` (9 usages in-process à faire migrer vers l'outbox/broker).*

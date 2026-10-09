# ARCHITECTURE BACKEND CIBLE — proposition

> **Statut** : proposition, à valider avant implémentation. Rédigé le 2026-10-09 contre l'état réel
> du dépôt `main` @ `4f196242` (mesures reproductibles en pied). **Il accompagne**
> [ADR-002](ADR-002-backend-clean-architecture.md) ; ici on décrit la **cible**, pas la décision.
> **Contexte mesuré** : 135 modules, 1 seul `pom.xml`, ~150 kLOC, Java 21 + Spring Boot 3.
> Déjà présents : Hibernate multi-tenant + sharding, outbox, Redis, WebSocket, webhooks, paiement
> USSD, IA, exports, sync. Le **capital** est là, la **structure** reste à rendre rigide.

---

## 0. LA PROPOSITION EN UNE PHRASE

> **Quatre couches par module (`domain` · `application` · `adapters.in` · `adapters.out`) dont les
> dépendances sont **imposables par la machine** (ArchUnit + multi-module Maven), des **contrats
> publiés** (OpenAPI + AsyncAPI) comme seule interface entre le backend et les clients web/mobile,
> un **bus d'événements** qui branché sur l'outbox **déjà existant** sans toucher au domaine, et des
> **tranches verticales (bounded contexts)** migrées **une par une** sans jamais arrêter la
> production de fonctionnalités.**

---

## 1. LES 7 DÉCISIONS STRUCTURANTES (et les causes mesurées qu'elles traitent)

| # | Décision | Cause racine qu'elle traite | Si on ne la prend pas |
|---|---|---|---|
| **D1** | **Tranches verticales par bounded context** (domaine possédant ses use-cases + ses ports) | 135 modules en pratique sans frontière stricte, accouplement silencieuse (ex. `DashboardService` agrège des entités de dizaines de modules) | Toute extraction de service = chirurgie à cœur ouvert ; une PR « propre » devient un commit de 30 fichiers |
| **D2** | **Frontières rendues illisibles par le linter** (ArchUnit + Maven `<modules>`) | 0 règle d'import, `domain` importe Spring/JPA partout, `modules/A` touche internes `modules/B` | La dérive revient en semaines ; le « on fera attention » ne marche pas (cf. `frontend-target-architecture.md` §3) |
| **D3** | **Cas d'usage = une classe = une transaction** (application layer explicite) | méga-services (1 545 l., 1 480 l., 1 419 l., 934 l., 860 l., …) | On ne sait plus où est la politique métier, ni qui ouvre la transaction, ni comment la tester isolément |
| **D4** | **Contrats publiés** : `openapi.json` (REST) + **AsyncAPI** (événements) **générés** en CI, clients web/mobile **générés depuis** ces contrats | ~80 chemins mobiles sans backend (dérive de contrat identifiée lors de l'audit d'intégration) | La parité mobile/web/BE est une chasse au trésor à chaque campagne |
| **D5** | **Outbox → broker**, relais par la file existante (`OutboxPublisher`) | 9 usages `ApplicationEventPublisher` in-process (non résilients, non observables) | Impossible de brancher entrepôt IA / webhooks sortants / analytics temps réel proprement |
| **D6** | **CQRS ciblé** sur les *reads* lourds (dashboard, recherche, analytics) | `DashboardService` 1 480 l. agrège à la volée, `SearchService` 934 l. balaye tout | Latence qui croît avec la data ; l'app ne scale pas au-delà de quelques dizaines de tenants |
| **D7** | **IAM policy-driven** (RBAC → ABAC via OPA/Cerbos), audit trail, séparation authentification vs autorisation | rôles codés en dur dans les contrôleurs, `CrossTenantScopeAccess` artisanal | Vendre à l'enterprise = refuser la due-diligence de sécurité |

---

## 2. CARTOGRAPHIE DES BOUNDED CONTEXTS (des 135 modules → 11 contextes)

> Ce n'est pas une réécriture : c'est le **regroupement de façade** des 135 modules en contextes qui
> ont un **langage ubique propre** et une **frontière transactionnelle** claire. Chaque module
> existant **entre dans un** contexte. Les noms actuels restent valides.

| Contexte (dossier Maven) | Modules existants qui y entrent | Rôle dans la valorisation |
|---|---|---|
| `identity` | authentication, users, tenants, onboarding, security, gdpr, compliance, scoping, audit | **prérequis** à tout le reste — SSO/RBAC/ABAC |
| `people` | members, souls, families, relationships, directory, volunteers, attendance | le **système de record** du fidèle |
| `discipleship` | discipleship, smallgroups, mentoring, missions, spiritualGifts, groups, cohorts, courses, curriculum, prays, promises, bible, books, videos, resources, sermons, teachings, confessions, churchMembership, beliefs, sacraments, churchYear, confession | la **mission produit** — le « pourquoi » du client |
| `governance` | governance, leadership, sessions, decisions, motions, votes, committees, appointments, policies, documents, successionPlan | vend l'**enterprise / dénomination** (multi-Églises) |
| `events` | events, services, liturgy, sermons, appointments, media, rooms, facilities, logistics, equipment | la **vie de l'Église locale** |
| **`fintech`** ⭐ | contributions, tithes, offerings, ledger, finances, payments, ussd, wallet, refunds, expenses, budgets, invoices, pledges, campaigns | **×10 du TAM** — là est vraiment le milliard (cf. §12) |
| `communication` | messages, whatsapp, voicemail, announcements, notifications, email, sms, push, media | le **lien fidèle↔pasteur** quotidien |
| `platform` | publicApi, integrations, webhooks, sync, data-migration, exports, imports, admin, backups | **l'écosystème** (effet de réseau, ouvreurs tiers) |
| `intelligence` ⭐ | analytics, reports, ai, insights, kpi, growth, prediction, recommendations, engagement, dashboards, reports, notifications, tasks, … | **le fossé data/IA** (rétention, churn, priorisation) |
| `ui-services` | navbar, search, favorites, recents, help, translations, theme, featureflags | sert web + mobile via contrats générés |
| `cross-cutting` | common/{multitenancy, scaling, infrastructure, observability, security, util} | ne dépend **de rien** ; tout le reste dépend de lui |

⭐ = **les deux contextes à haute valeur** pour la valorisation : **fintech** (produit à forte marge,
PCI-DSS requis, déjà entamé avec USSD/webhooks fail-closed) et **intelligence** (données + modèles
IA = moat réel, cf. §11).

---

## 3. L'ARBRE CIBLE d'un module — ce que deviennent les actuels

```
backend/
├── pom.xml                                       ← parent, packaging=pom, <modules> par contexte
├── <context>/                                    (identity, people, discipleship, fintech, …)
│   ├── pom.xml                                 ← dépend UNIQUEMENT de cross-cutting + contrats partagés
│   ├── domain/                                  ← entités + value objects + policy pure (0 Spring, 0 JPA)
│   │   └── src/main/java/com/discipolat/<ctx>/<module>/domain/
│   ├── application/                             ← UseCases (commande), orchestration, transactions
│   │   └── .../<module>/application/
│   ├── adapters-in/                             ← @RestController, @KafkaListener, @Scheduled, @TransactionalEventListener
│   └── adapters-out/                            ← RepositoryImpl (JPA), PaymentGateway (USSD), SmsSender, LlmAdapter
├── cross-cutting/                                ← common/ actuel, reconditionné
│   ├── observability/                            ← OpenTelemetry, MDC, traceId propagation
│   ├── multitenancy/                             ← déjà là (Hibernate + Redis + sharding)
│   └── ...
├── platform-bootstrap/                           ← DiscipolatApplication.java, @SpringBootApplication, scan des contextes
└── contract/                                     ← DTO partagés générés (openapi-generator / asyncapi-generator)
```

Le **domaine actuel** (`modules/<x>/domain/*Service.java` qui mélange politique métier, JPA, Spring
Transactions) se **scinde** en trois :
- **`domain/`** ← les règles métier pures, sans annotation Spring/JPA.
- **`application/`** ← les use cases qui **orchestrent** et portent `@Transactional`.
- **`adapters-out/`** ← les `*Repository` JPA (Spring Data) qui étaient dans `domain/`.

Le `modules/<x>/api/` actuel (`@RestController`) → **`adapters-in/web/`**. Les `modules/<x>/config/` →
déplacés dans `platform-bootstrap` ou dans `adapters-out/config` selon la portée.

**Principe de migration (règle d'or)** : *aucun* de ces déplacements n'est obligatoire pour livrer
une fonctionnalité. On crée la nouvelle structure **parallèlement** et on migre un module à la fois.
Rien n'est supprimé avant d'avoir son remplaçant testé. (C'est exactement la discipline de
`frontend-target-architecture.md` §2 — appliquée au BE.)

---

## 4. LE CŒUR : LES RÈGLES DE DÉPENDANCE (c'est ici que se gagne l'archi)

Six règles, **exécutées par ArchUnit à chaque `mvn verify`**. Une règle non exécutée n'est pas une
règle (cf. frontend doc §3).

```
   adapters-in ──► application ──► domain ◄── adapters-out
                        ▲                          ▲
                        │                          │
                   cross-cutting ◄─────────────────┘
        (contract = seul point d'échange entre contexts)
```

| # | Règle ArchUnit | Interdit | Pourquoi |
|---|---|---|---|
| R1 | `..domain..` n'accède à **aucun** paquet tiers hors JDK + `jakarta.validation` | `import org.springframework.*` ; `import jakarta.persistence.*` | Le cœur métier est testable sans contexte Spring, réutilisable, migrable |
| R2 | `..application..` peut importer `domain/` et des **ports**, jamais des adaptateurs concrets | `..adapters.out..` dans `application` | La couche use-case orchestre, elle ne connaît ni JPA ni Kafka |
| R3 | Un module d'un contexte **n'importe pas** les internes d'un autre contexte | `import com.discipolat.fintech.domain.*` depuis `people` | Le couplage passe par un **port publié** ou un **événement** (contract) |
| R4 | Les **annotations** Spring (`@Service`, `@Component`, `@Autowired`, `@Transactional`) n'apparaissent **que** dans `adapters-*` et `application` | dans `domain` | Le domaine reste une bibliothèque pure |
| R5 | Les `@RestController` n'appellent **que** les use-cases `application` | un contrôleur qui ouvre une transaction ou parle à un repository | On sépare transport HTTP et logique métier (la 1ʳᵉ est fine, la 2ᵈ est le produit) |
| R6 | Aucun **cycle** de paquet entre contexts | `A ⇄ B` | Un cycle rend l'extraction future **impossible** |

Exemple de config ArchUnit à poser (à copier telle quelle dans V0) :

```java
@ArchTest
static final ArchRule domaine_pur =
    noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..",
          "io.swagger..", "com.discipolat..adapters..");

@ArchTest
static final ArchRule frontieres_entre_contexts =
    slices().matching("com.discipolat.(*)..").should().notDependOnEachOther()
      .ignoreDependency(
          desPackageCommençantPar("com.discipolat.contract."),  // le contrat partagé est autorisé
          desPackageCommençantPar("com.discipolat."));
```

**Effet secondaire immédiat** : `mvn verify` qui échoue sur une violation **documente** l'architecture.
Une équipe qui arrive peut lire `ArchRulesTest.java` et savoir **exactement** ce qui est permis. C'est
ce qui se vend en due-diligence.

---

## 5. ANATOMIE D'UN BOUNDED CONTEXT (l'exemple réel : `departments`, aujourd'hui 3 services de 1 545+1 419+684 = **3 648 lignes**)

```java
// ─── domain/Department.java ─────────────────────────────────────────────────
// Entité pure, pas d'annotation JPA, politique métier dedans.
public record DepartmentId(String value) { ... }

public class Department {
  private final DepartmentId id, TenantId tenantId, String nom, ResponsableId responsableId;
  // invariant métier : « un département a au moins un responsable »
  public void attacherResponsable(MembreId m) { ... }
  // pas de setter, pas d'@Entity, pas de Spring
}

// ─── application/CreateDepartmentUseCase.java ───────────────────────────────
@ApplicationService   // annotation custom, ou @Service dans adapters-out
@Transactional
class CreateDepartmentUseCase {
  private final DepartmentRepository repo;             // port (interface domain/)
  private final TenantGuard tenantGuard;               // port (identity/)
  private final OutboxPort outbox;                     // port (événement, cf. §7)

  DepartmentId execute(CreateDepartment cmd) {
    tenantGuard.assertInScope(cmd.tenantId());
    var d = Department.creeDepuis(cmd);
    repo.save(d);
    outbox.publish(new DepartmentCreated(d.id(), d.tenantId()));
    return d.id();
  }
}

// ─── adapters-out/persistence/DepartmentRepositoryImpl.java ────────────────
// Le JPA vit ICI. Il implémente le port domain.
@RepositoryFor(Department.class)
class DepartmentRepositoryImpl implements DepartmentRepository { /* JPA + mapper */ }

// ─── adapters-in/web/DepartmentController.java ──────────────────────────────
@RestController
class DepartmentController {
  private final CreateDepartmentUseCase useCase;
  @PostMapping("/departments") public DepartmentDto create(@RequestBody CreateDepartment.Request r) {
    return DepartmentDto.from(useCase.execute(r.toCommand()));
  }
}
```

**Effet mesurable** : `DepartmentManagementService` (1 545 l.) **disparaît en tant que classe**. Le
même code se retrouve dans : un modèle de domaine (200 l., testable sans Spring), une dizaine de
use-cases (~40 l. chacun, transaction claire), un adaptateur JPA, un contrôleur. La revue de PR
redemande ne prend plus 10 minutes par fichier.

---

## 6. D4 — CONTRATS : l'OpenAPI n'est pas une doc, c'est la **frontière**

- `openapi.json` : **existant** (`docs/openapi.json`, cf. commit `c97ee545`). Le régénérer à la CI est
  obligatoire. Il devient l'**unique source de vérité** pour les clients web (`orval`) et mobile
  (`openapi-generator --language dart`).
- `asyncapi.yaml` : généré depuis les **events** publiés par l'outbox (cf. §7). Aujourd'hui il n'y en
  a pas. V0 doit l'introduire.
- **Contrôles CI** :
  1. Le backend **ne peut pas merger** un breaking change sans version d'API incrémentée ;
  2. Les **clients régénérés** web + mobile doivent **builder** dans la PR backend ;
  3. Un **test de contrat** (déjà fait pour `churchesSuggestExistsContract`, 12/12) est exigé pour
     chaque endpoint critique.
- Effet direct : la dérive **~80 chemins mobiles sans backend** (audit d'intégration mémoire) devient
  **impossible à introduire** sans que la CI le signale.

---

## 7. D5 — ÉVÉNEMENTS : outbox existante → broker, en trois temps

L'**outbox** est déjà là (`OutboxEvent` + `OutboxPublisher` + `OutboxConsumers`, dans `modules/core`).
C'est la **brique la plus difficile** et elle marche. Le chemin vers un vrai bus (Kafka/Redpanda) ne
touche **pas le domaine** :

| Étape | Contenu | Ce qui change |
|---|---|---|
| **E1** | Publier dans l'outbox **à la place** des `ApplicationEventPublisher` (9 usages) | plus rien d'in-process ; tout passe par la table |
| **E2** | Un **relay** `OutboxPublisher → Kafka topic` (déjà un composant de polling, à brancher sur un producteur Spring Kafka) | les consommateurs externes (analytics, webhooks sortants, data lake) arrivent **sans toucher au code métier** |
| **E3** | **AsyncAPI** générée depuis la liste des événements + test de contrat web↔BE↔mobile | on peut faire évoluer un consommateur sans casser les autres |

**Cas d'usage business concrets débloqués** :
- analytics temps réel → `intelligence` (§2, le moat) ;
- webhooks sortants pour intégrations tierces (Slack, WhatsApp Business, mobile money) → écosystème ;
- déclenchement asynchrone de jobs lourds (emails, IA, exports) ;
- extraction de `fintech` ou `notification` en service autonome **quand** le besoin apparaît, sans
  réécriture.

---

## 8. D6 — CQRS ciblé (là où ça paie, pas partout)

Le **vrai** problème n'est pas « tout en CQRS » — c'est 3 ou 4 **reads** agrégatifs lourds :
`DashboardService` (1 480 l.), `SearchService` (934 l.), `ExecutiveInsightsService`, `reports`.

Cible :
- **write** = use-cases (transactionnels, §5) → publient des **événements** dans l'outbox (§7) ;
- **read** = **projections** alimentées par ces événements → tables de matière / vues SQL dédiées /
  cache Redis. Les écrans *dashboards* ne lisent plus les tables métier agrégées à la volée, ils
  lisent la **projection**.
- Coût d'entrée maîtrisé : on introduit une abstraction `Projection<Key, View>` **sans** broker au
  début (juste un `@Scheduled` + `OutboxEvent` déjà écrit), puis on branche Kafka en E2/E3.

Pour le reste du CRUD, **rien ne change** : on garde le pattern repo + service simple, en couches.

---

## 9. Multi-tenancy & isolation — les 3 régimes (prolongement de l'existant)

| Régime | Qui | Comment | Où on en est aujourd'hui |
|---|---|---|---|
| **A. Pool partagé** (discriminateur `tenant_id`) | SMB, 95 % des églises | `TenantFilterIntegrator` Hibernate (déjà en place) + préfix Redis (`TenantAwareRedisTemplate`) | ✅ opérationnel |
| **B. Schéma par tenant** | mid, exigence d'isolation | migrer Flyway par schema + datasource routing | 🔜 une option à brancher sur `TenantDataSource` |
| **C. Base/region dédiée** | enterprise, diaspora UE, résidence RGPD | `ShardedTenantDataSource` (déjà écrit) + réplication | ✅ infra présente, manque le **pilotage** |

→ **Le sharding existe** (`common/scaling/ShardedTenantDataSource`, `TenantShardExecutor`). Il faut
seulement le rendre *pilotable par tenant* (une table `tenant_storage_placement`) et exposer un
**runbook** d'entrée/sortie. C'est ce qui fait basculer une vente enterprise (€ millions/ARR).

---

## 10. EXTRACTION en services autonomes — sur critère, pas au feeling

Un module **devient** service déployable séparément **quand** au moins **deux** critères sont vrais :
(a) **scale de trafic** propre (ex. notifications SMS/push en rafale) ;
(b) **résidence des données** distincte (RGPD, paiement) ;
(c) **cycle d'équipe** divergent ;
(d) **couplage de déploiement** intenable.

Candidats naturels dans **cet ordre** (valeur / coût) :

1. **`fintech`** ⭐ : PCI-DSS impose un périmètre d'audit réduit → **séparer le ledger** est une
   **exigence** réglementaire, pas une préférence.
2. **`communication/lowband`** : SMS/USSD/push en rafale, trafic très variable → autoscale propre.
3. **`intelligence`** batch/IA (GPU, coûts de tokens) : besoin d'isoler le budget.
4. **`platform/webhooks`** : les webhooks sortants ne doivent **jamais** bloquer la transaction
   principale.

Chaque extraction se fait **sans toucher au domaine** : l'outbox (§7) devient le contrat binaire, et
le module extrait consomme/publie sur les topics AsyncAPI.

---

## 11. Data & IA (le moat réel)

- Chaque **événement** (§7) est dupliqué vers un **data lake** (S3/GCS) en format ouvert (Parquet/Iceberg).
- Un **entrepôt** (DuckDB → ClickHouse → Snowflake/BigQuery, par paliers) expose des modèles
  dimensionnels par contexte (`people`, `discipleship`, `fintech`, `events`).
- **dbt** pour les transformations (versionnées, testées, prouvées en due-diligence).
- **Feature store** à termes pour les modèles : prédiction d'attrition fidèle, score d'engagement,
  recommandations pastoriales. Aujourd'hui il y a `ai`, `predictions`, `intelligence` **en code** mais
  pas **de data platform** en amont → le passage à l'échelle de ces modules l'imposera.

**Réalité business** : c'est cette section qui **vaut** la valorisation, pas la Clean Architecture
elle-même. Mais sans ADR-002, les points §7 et §2 ne sont pas possibles sans risque.

---

## 12. Fintech : le ×10 du TAM (et le vrai sujet des milliards)

Un logiciel de gestion d'église plafonne. Un **rail de paiement** pour l'écosystème ecclésial
francophone (dîmes, offrandes, campagnes, missions, bourses, tontines d'église) ne plafonne pas :
le volume annuel circule dans les **centaines de milliards USD** (Afrique + diaspora). Les briques
existent (paiements, USSD, webhooks fail-closed) ; il manque :

- un **ledger append-only** avec double-entrée, idempotence (clé idempotence serveur déjà présente),
  réconciliation, audit trail (module `audit` déjà là) ;
- **PCI-DSS** (scope réduit par isolation fintech, cf. §10) ;
- conformité **réglementation des micro-paiements** par pays (CEMA Afrique de l'Ouest, etc.) ;
- **KYC/AML** minimal côté tenant (rôle de `identity` §2).

**C'est ici, et uniquement ici, qu'un multiple de ×10 devient réaliste.** L'architecture propre est la
**condition nécessaire** (frontières §2, événements §7, extraction §10) mais pas suffisante : il faut
un **partenariat PSP** (Mobile Money, Wave, Orange Money) et les licences locales. À planifier avec
la direction juridique/business, pas en code.

---

## 13. Sécurité & conformité (ce qu'on vend en due-diligence)

| Domaine | Cible | Outil |
|---|---|---|
| Authentification | OIDC, MFA, sessions, refresh (existant, durcir) | Spring Security + Keycloak/Authorization Server |
| Autorisation | **RBAC → ABAC via OPA/Cerbos** (politique en code versionné, pas en `if` de contrôleur) | D7 |
| Multi-tenant | isolation testée par régime (§9) + `CrossTenantScopeAccess` **explicite, audité** | tests dédiés |
| RGPD | droit à l'export / à l'oubli, résidence UE, **registre des traitements** | module `gdpr` existant à durcir |
| SOC 2 / ISO 27001 | journal d'audit immuable (déjà : `audit`, `outbox`), revue de code obligatoire, gestion des incidents | à planifier |
| PCI-DSS | isolation `fintech` (§10), segmentation réseau, vaulting tokens | à planifier |

**Vente enterprise = case-coche de cette table.**

---

## 14. PLAN DE MIGRATION — 4 phases, 1 slice par PR, jamais d'arrêt de production

**Règle de conduite, non négociable** : chaque PR = **une** tranche migrée, tests au vert, **zéro
comportement modifié**. Une PR d'architecture **n'apporte pas** de fonctionnalité. Les deux flux
cohabitent. C'est la même discipline que `frontend-target-architecture.md` §13.

| Phase | Durée | Contenu | Critère de sortie mesurable |
|---|---|---|---|
| **V0 — Outiller** | 1-2 sem. | ArchUnit + R1..R6 en *mode avertissement* ; **multi-module Maven** par bounded context (déplacements purs, sans code touché) ; OpenAPI régénéré en CI ; AsyncAPI squelette ; **OpenTelemetry** + traceId dans MDC. **Voir `/TODO_BACKEND_V0_CLEAN_ARCH.md`.** | `mvn verify` échoue si un nouveau fichier viole R1-R6 ; les anciens sont *freeze* (liste d'exemptions qui **régrescit**) |
| **V1 — Socle** | 4-8 sem. | Scission `domain`/`application`/`adapters-*` module par module (par domaine §2) ; méga-services éclatés en use-cases ; relais outbox → Kafka ; CQRS sur 3 projections ; OPA/Cerbos | compteur `*Service.java > 500 l.` → 0 ; 100 % des endpoints ont un test de contrat web/mobile/BE |
| **V2 — Plateforme** | 2-3 mois | API publique versionnée + quotas + OAuth tiers ; extraction `fintech` ; data lake + dbt | la plateforme est **appelable par un tiers** ; ledger fintech isolé |
| **V3 — Fossé IA + international** | continu | feature store, modèles de rétention ; résidences UE + Afrique ; multi-devises réel | la rétention s'améliore **mesurablement** par les modèles ; on ouvre une région sans réécrire |

**Ordre des contextes V0** : **cross-cutting** (ne dépend de rien, 0 risque) → `identity` (le plus
traversant) → `fintech` (le plus valorisable) → le reste par ROI.

**Piège nommé, même que le frontend doc §13** : la migration échoue si on la fait « quand on aura le
temps ». Elle ne marche que si **chaque PR est petite** (≤ 500 lignes) et **immédiatement mergée**.

---

## 15. CE QUE CETTE ARCHITECTURE RÉPARE CONCRÈTEMENT

| Constat mesuré | Réparé par | Vérifiable par |
|---|---|---|
| 135 modules sans frontière | V0 | ArchUnit refuse un import interdit |
| Méga-services 1 545 l., 1 480 l., 1 419 l., 934 l. | V1 | `find … -name '*.java' -exec wc -l` max < 200 (hors tests) |
| `domain/` impur (Spring, JPA partout) | V0 + V1 | R1 ArchUnit ; `grep org.springframework **/domain/**` → 0 |
| Dérive de contrat ~80 chemins mobiles sans backend | V0 | les clients web/mobile sont **générés** en CI |
| `ApplicationEventPublisher` in-process | V1 | relais outbox→Kafka (E2) |
| `DashboardService` agrège à la volée | V1 | projections CQRS |
| Multi-tenant non *pilotable* entre régimes | V1 | table `tenant_storage_placement` + tests §9 |
| 0 observabilité distribuée | V0 | un traceId de bout en bout web → BE → mobile, visible dans Grafana/Jaeger |
| Sécurité « en dur dans les contrôleurs » | V1 | politique OPA versionnée en Git |

---

## 16. CE QUE JE REFUSE — ET POURQUOI (contre-expertise)

| Refusé | Raison |
|---|---|
| **Microservices d'emblée** sur 135 modules | aucune équipe pour les porter, latence, enfers d'ops, distribution des transactions ; le monolithe modulaire bien découpé **contient déjà** la capacité d'extraire — cf. ADR-002 |
| **Event sourcing partout** | ceremony + coût cognitif sur 130 modules qui n'en ont pas besoin. On le réserve aux reads lourds (§8) et au ledger fintech (§12) |
| **CQRS généralisé** | mêmes raisons |
| **Réécriture from scratch** | cf. frontend doc §15 — même verdict |
| **Nettoyer sans ArchUnit** | une règle non exécutée par la machine n'est pas une règle |
| **Passer à Kotlin/Quarkus/Golang** « parce que c'est mieux » | Java 21 + Spring Boot est un choix industriel parfaitement correct ici ; changer de langue en pleine migration = deux projets au lieu d'un |
| **Kubernetes obligatoire** | l'infra actuelle (Render + Docker Compose) est saine pour la taille ; K8s se justifie **quand** la table §10 a déclenché ≥ 3 extractions, pas avant |

---

## 17. LE COÛT, HONNÊTEMENT

| Phase | Charge | Ce qu'il faut savoir |
|---|---|---|
| **V0** | 1-2 sem. | Rentabilité **immédiate**, risque **nul** (ArchUnit en mode warning + déplacements purs). À faire même si la suite n'est jamais faite. |
| **V1** | 4-8 sem. | C'est le vrai chantier. Il paie en due-diligence, en vitesse de migration multi-tenant, en observabilité. |
| **V2** | 2-3 mois | Dépend d'une **décision business** (ouvrir l'API = risque de sécurité, + contrat). |
| **V3** | continu | Où est le **vrai** fossé de valorisation. |

**Le risque principal** n'est pas technique, c'est **l'arrêt en cours de route**. La parade : V0
livre de la valeur **immédiate** (contrats générés, observabilité, `mvn verify` qui protège
l'architecture) donc aucune suspicion ne s'installe dans l'équipe.

---

## 18. DÉCISIONS À PRENDRE MAINTENANT

1. **Activer V0** (ArchUnit warning + multi-module Maven + OpenAPI CI + OTel) — 1-2 sem., 0 risque, cf.
   `/TODO_BACKEND_V0_CLEAN_ARCH.md`.
2. **Choisir le 1ᵉʳ contexte pilote de strangulation** : **TRANCHÉ le 2026-10-09 → `governance`,
   module `departments`** (arbitrage humain). Raison assumée : c'est là que sont les plus gros
   services mesurés (`DepartmentManagementService` 1 545 l., `DepartmentDossierService` 1 419 l.), donc
   le pilote qui **épreuve les règles R1-R6 sur le cas le plus dur** plutôt que sur un cas facile —
   un gabarit validé ici se copie ailleurs, l'inverse ne garantit rien. Mon texte recommandait
   `identity` (meilleur effet de démonstration) ; **`identity` devient V0.6bis** et `fintech` V0.7.
   Note : le module s'appelle `departments` et son plus gros service est dans `departments/domain/`
   — donc R1 (domaine pur) y **échouera** immédiatement, ce qui est exactement la preuve rouge
   recherchée en V0.1.
3. **Décider la plateforme d'événements** : Kafka (le standard), Redpanda (plus simple à opérer), ou
   AWS MSK / Confluent Cloud (si vous êtes déjà sur un cloud — checkRender actuel).
4. **Trancher : GitLab d'abord** (ADR-001, 1 jour) **puis** V0 backend. **Précision après arbitrage
   humain** : GitLab est **lancé en parallèle** de V0-A (outillage) et non comme pré-requis bloquant —
   V0-A ne touche que `pom.xml` et des tests, il n'a donc aucun intérêt à attendre le déménagement.
   En revanche V0-B (multi-module, gros remaniement de `pom.xml`) **doit** être exécuté sur le
   pipeline GitLab vert pour ne pas déménager un chantier ouvert.

---

*Toutes les mesures de ce document sont reproductibles sur `main` @ `4f196242` (2026-10-09) :
`ls backend/src/main/java/com/discipolat/modules | wc -l` (135) ; `find backend -name pom.xml` (un seul) ;
`wc -l backend/src/main/java/com/discipolat/modules/departments/domain/DepartmentManagementService.java`
(1 545) ; `ls backend/src/main/java/com/discipolat/common/multitenancy backend/src/main/java/com/discipolat/common/scaling`
(multi-tenant + sharding déjà présents) ; `grep -rln OutboxPublisher backend/src/main/java` (outbox déjà là).*

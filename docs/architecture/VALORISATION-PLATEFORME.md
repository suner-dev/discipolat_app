# Trajectoire de valorisation — cartes des plaques BE / FE / Mobile / Plateforme

> **Objet** : relier chaque décision d'architecture à un **levier de valorisation vérifiable**, et
> dire pour chaque levier **où on en est réellement** (mesure, pas intention). Ce document est la
> version écrite de la proposition d'architecture demandée le 2026-10-09 et arbitrée le même jour ;
> il **n'exécute rien** : aucune ligne de code de production n'a été touchée par ce document.
> **À lire avec** : [ADR-REGISTRE.md](ADR-REGISTRE.md), [ADR-002](ADR-002-backend-clean-architecture.md),
> [ADR-005](ADR-005-ledger-immutabilite.md) (ledger), [ADR-006](ADR-006-data-evenements-ia.md) (data/IA),
> [ADR-007](ADR-007-iam-policy-driven.md) (identité), [ADR-008](ADR-008-delivery-gitlab-preuves.md) (delivery).

---

## 0. Ce que ce document ne promet pas

**Aucune architecture ne valorise un projet à un milliard.** La valorisation vient de la demande,
de la rétention et de la marge. L'architecture est un **multiplicateur de durabilité** : elle décide
si ce qui tient à 100 clients tient à 100 000, et si un acquéreur peut **vérifier** ce qu'on lui
raconte. Toutes les cases « où on en est » ci-dessous sont donc **mesurées dans le dépôt**, avec la
commande en annexe (§9).

Deux conséquences assumées :

1. les plaques sont ordonnées par **coût de report** (ce qui devient plus cher à décider plus tard),
   pas par brillance ;
2. une capacité **déjà écrite** vaut plus qu'une capacité **proposée** : le capital de ce projet n'est
   pas la clean architecture, c'est la surface fintech + partenaire déjà codée (Stripe, MTN MoMo,
   Orange Money, M-Pesa, PayPal payout, USSD, tontine, transferts, marketplace, API publique,
   webhooks). Le travail est de la rendre **provable**, pas de la réécrire.

---

## 1. Mécanisme : ce qu'un acquéreur paie, et la propriété technique correspondante

| Driver de valorisation | Ce qu'il exige techniquement | Où on en est (mesuré 2026-10-09) |
|---|---|---|
| **NRR** (ventes additionnelles au même client) | multi-tenant profond, isolation par régime, white-label, feature flags, intégrations tiers | 3 régimes d'isolation **documentés** (`backend-target-architecture.md` §9) ; briques réelles : `common/multitenancy` (`TenantFilterIntegrator`, `CurrentTenantResolver`), `common/scaling` (`ShardedTenantDataSource`, `ShardRouting`, `TenantShardExecutor`). **Pas encore imposé** par un test de frontière (R3 gelé : 354 arêtes contexte→contexte) |
| **Take-rate** (revenu sur flux, pas sur siège) | ledger exact, idempotence, réconciliation, multi-rails | `finances` 21 fichiers / 2 009 l. ; rails **déjà écrites** dans `payments` (42 fichiers / **5 303 l.** : MTN MoMo, Orange Money, M-Pesa, Stripe, PayPal payout, `MobileMoneyProviderRegistry`, `WebhookLog` + vérificateur de signature). **Aucune écriture en partie double** : `finance_transactions.type IN ('RECETTE','DEPENSE')`, et **zéro occurrence de `account_id`** dans tout le module `finances` → voir ADR-005. C'est **le trou n°1** |
| **Moat data** (coût de changement du client) | lac d'événements, CDC, sémantique, modèles, lignée de consentement | Outbox **réelle** (18 fichiers ; `outbox_event` avec `aggregate_type/id`, `event_type`, `payload_json jsonb`, `status`, `attempts`, `available_at`, `published_at`). IA applicative : `ai` 13 fichiers / 1 927 l. dont `LlmProviderService` (seam unique) et `AiCreditsService`/`AiUsage` (**metering déjà né**). Aucun broker, aucun lakehouse, **0 occurrence de consentement** dans `gdpr` → ADR-006 |
| **Marge / coût de service** | profilage par tenant, mesure d'usage, caches, projections lues, autoscaling dimensionné | `dashboard` 2 fichiers / **1 589 l.** agrège à la volée ; `search` 1 070 l. ; Redis tenant-aware présent ; **pas de CQRS** |
| **Décote risque réduite** | SOC 2 / GDPR / PCI-DSS, DR chiffré (RTO/RPO), audit trail, secrets, code auditable | **actif réel** : ArchUnit R1..R6 avec gel monotone (424 lignes : R1 7 867 · R2 0 · R3 952 · R4 2 948 · R5 8 · R6 45), **2 178 tests BE verts**, gates PostgreSQL **19/19**, `ddl-auto: validate`, module `audit` présent, `gdpr` 7 fichiers |
| **Optionnalité** (produits nouveaux sans réécrire) | contrats publiés, API partenaire, webhooks, marketplace | `publicapi`, `webhooks`, `integrations`, `marketplace` (4 fichiers) **existent en code** ; contrats ouvertes : l'API mobile n'est pas générée (~80 chemins sans backend, cf. ADR-004) |

---

## 2. Backend — la plateforme en six plaques

Grammaire déjà acceptée (ADR-002) : quatre couches `domain · application · adapters.in · adapters.out`,
frontières imposées par ArchUnit + multi-module Maven, migration **une tranche verticale à la fois**.
Ce qui suit **ajoute les plaques monnayables** sans contredire l'ADR.

| # | Plaque | Choix professionnel | Trigger d'adoption (chiffré) | Driver payé | ADR |
|---|---|---|---|---|---|
| **P1** | **Identité & accès** | un seul contexte `identity` : OIDC (IdP externe **ou** Keycloak auto-hébergé), SCIM pour le provisioning enterprise, **autorisation policy-driven** (OPA/Cerbos) au lieu de 1 001 `@PreAuthorize` répartis dans 231 fichiers | premier client > 5 000 membres, ou premier contrat exigeant SSO/provisioning | NRR enterprise, décote sécurité | [ADR-007](ADR-007-iam-policy-driven.md) |
| **P2** ⭐ | **Ledger** | journal en **partie double append-only**, clé d'idempotence sur toute écriture d'argent, solde **dérivé** et non déclaré, réconciliation continue vs extraits providers, devise en unités mineures | **maintenant**, avant d'ouvrir un franc de take-rate | take-rate, licence bancaire | [ADR-005](ADR-005-ledger-immutabilite.md) |
| **P3** | **Orchestration de paiement** | façade unique multi-rails (carte, mobile money, USSD, virement) ; **aucune logique de rail dans `domain`** ; PCI réduit par tokenisation + champs hébergés | premier volume > 10 000 transactions/mois | marge, conformité | ADR-005 §3 (P3 y est traité avec P2, même modèle de données) |
| **P4** | **Événements comme produit** | Outbox (**existant**) → broker managé (Redpanda/Kafka) + **registry de schémas** + AsyncAPI publié ; webhooks sortants signés et rejouables ; CQRS sur les lectures lourdes (`dashboard`, `search`, `analytics`) | 3 consommateurs d'un même flux, ou lecture > 300 ms p95 | moat, intégrations | [ADR-006](ADR-006-data-evenements-ia.md) |
| **P5** | **Data & IA** | CDC (Debezium) → lakehouse (Iceberg sur S3 + dbt + ClickHouse/Snowflake), couche sémantique, feature store ; l'IA reste un **adaptateur** qui consomme des features — jamais le domaine qui appelle un LLM en direct (le seam `LlmProviderService` est déjà là) | ≥ 3 questions d'analytique impossibles en SQL applicatif | moat, rétention | [ADR-006](ADR-006-data-evenements-ia.md) |
| **P6** | **Plateforme d'exécution** | conteneurs + orchestration **au déclencheur**, IaC, secrets managés, **SLO chiffrées** avec budget d'erreur, OpenTelemetry corrélé, DR **prouvé par exercice**, metering d'usage par tenant (prolonge `AiUsage`) | multi-région ou > 100 tenants actifs | marge, crédibilité due-diligence | [ADR-008](ADR-008-delivery-gitlab-preuves.md) |

**Contrainte dure issue de la mesure, à ne pas contourner.** 41 des 75 contexts forment **un seul
composant fortement connexe**, et 45 couples réciproques subsistent (R6, gelé objet par objet). Donc
**P2..P6 ne peuvent pas être livrés en « microservices »** tant que les couples ne sont pas rompus
(LOT V0-F, tâche `V0.15` de `TODO_BACKEND_V0_CLEAN_ARCH.md`). La séquence crédible est
*modulariser → rompre les couples → extraire*. Extraire avant = promettre une architecture et livrer
un **distributed monolith** : c'est le premier défaut qu'un acquéreur technique trouve, et le plus
coûteux à cacher.

---

## 3. Frontend — deux cibles, un coût honnête

État mesuré : Vite 8 + React 19 + `react-router` 7.18 + TanStack Query 5 + `@azure/msal-browser` ;
550 fichiers dans `frontend/src` ; ratchets i18n et dette **déjà câblés en CI**.

| Option | Ce qu'elle achète | Coût / risque |
|---|---|---|
| **F-A — SPA industrialisée** *(retenue d'abord)* | vélocité immédiate, aucune réécriture, due-diligence déjà forte sur les ratchets. Contenu : monorepo `pnpm`+`turbo`, **design system publié** en paquet versionné (le blanc-label et l'AA en dépendent), clients API **générés depuis `openapi.json`**, taxonomie d'état (server / client / URL), i18n ICU lazy, budgets de perf chiffrés + RUM, Playwright sur les 10 parcours qui rapportent | faible ; ne paie pas le SEO public |
| **F-B — Next.js RSC + edge** *(limité au funnel)* | SEO + TTFB sur les pages d'**acquisition** (là où le funnel devient monnayable), streaming | réel : MSAL et `react-router` à ré-implanter, double pile pendant la transition, `use client` comme nouvelle source de dette (ADR-003) |

Le critère qui tranche n'est **pas** technique : *est-ce que l'acquisition passe par le web public ?*
Si oui (dons en ligne, découverte d'église via `/e/:slug`), F-B se paie **sur les pages d'entrée
seulement**, pas sur le cockpit. Si l'acquisition est relationnelle (réseaux d'églises, bouche-à-oreille,
pasteurs), F-A suffit et F-B est une taxe. **Question ouverte, see §8.**

Transverse FE qui vaut de l'argent dans **les deux** options : multi-tenant visuel (thème + domaine +
marque par tenant résolus au runtime, pas recompilés — `applyScopedBranding` existe), accessibilité
**AA** (condition d'achat enterprise et public), contract-first, télémétrie avec SLO exposées dans un
tableau de bord client.

---

## 4. Mobile — le différenciateur du marché réel

État mesuré : Flutter 3.35.6, Riverpod + `go_router`, **`drift` déjà au contrat**, module `lowband`
déjà là (côté BE : `LowBandNotifyService` branché sur l'outbox), Firebase messaging, 116 fichiers de
test, **couche `domain/` absente** de `mobile/lib` (il y a `core data features models presentation`),
et `ApiService()` instancié en dur **260 fois**.

Pour un produit dont les rails sont USSD et mobile money, l'architecture qui crée la valorisation
n'est pas « clean Flutter » (c'est de l'hygiène) : c'est **la promesse que l'argent et les données du
client ne se perdent jamais**. Quatre briques, dans l'ordre de rentabilité :

| Brique | Choix pro | Pourquoi ça vaut |
|---|---|---|
| **M1 — Journal d'opérations offline** | file d'écriture locale **idempotente** (la clé d'idempotence serveur existe déjà dans `payments`), relecture au retour réseau, **politique de conflit par type de donnée** : LWW pour le non-financier, **refus/saga** pour le financier — jamais de fusion silencieuse d'un montant | le don en zone blanche ne se perd pas → rétention mesurable ; c'est la brique qui rend P2 crédible côté client |
| **M2 — Contract-first** | DTO **générés** depuis `openapi.json` / `asyncapi.yaml` en CI, avec contrôle de dérive bloquant | supprime la classe entière de bugs « ~80 chemins sans backend » ; c'est ce qu'un acquéreur teste en premier |
| **M3 — Intégrité & secrets** | Keystore / Secure Enclave, **Play Integrity / App Attest** sur les appels sensibles (paiement, tirage de dons), certificat épinglé sur les endpoints d'argent, aucune donnée financière en clair dans `drift` | sans ça, aucun processeur de paiement ne signe un contrat |
| **M4 — Basses ressources** | budgets chiffrés : payload p95, appels réseau par écran, charge initiale sur device d'entrée de gamme, mode économie de données, télémétrie offline-first | le TAM est exactement là ; un concurrent riche en bande passante y échoue |

Règle interne (ADR-004, inchangée) : `features/<x>/{domain,data,presentation}`, DI réelle (les 260
`ApiService()`), `domain/` sans framework. Le mobile devient **miroir du backend** : mêmes noms de
contexts, mêmes ports → une équipe peut servir les deux, et un acquéreur peut compter les équipes.

---

## 5. Le socle de livraison : GitLab comme machine à preuves

Choix arbitré le 2026-10-09 (ADR-001, sens du miroir précisé depuis). Ce n'est pas un choix
« GitLab est mieux » : c'est le choix **d'un seul endroit où la preuve se fabrique**. Voir
[ADR-008](ADR-008-delivery-gitlab-preuves.md) pour l'état réel du pipeline et ce qui reste humain.

- **source de vérité unique** (code + MR + ADR + pipeline), GitHub répliqué pendant la transition ;
- **pipeline en une définition** : `contract → build → test (BE 2 178 / FE 862 / mobile) → gel
  d'architecture → scans (secrets, dépendances, SAST) → SBOM → image → scan d'image → Flyway sur PG
  réel → déploiement` ;
- **environnements nommés** : c'est ce qui rend une SLO tenable et un rollback racontable ;
- **revue d'artefacts** : chaque release porte SBOM + rapports de scan + preuve du gel → l'export
  « data room » devient **automatique**, au lieu d'une semaine de chasse pré-levée.

Franchissement à dire : la migration GitLab **ne bloque pas** V0 et **n'a aucun effet sur la
valorisation en elle-même** ; elle accélère sa *démonstration*.

---

## 6. Ce qu'on refuse, et pourquoi (contre-expertise)

| Refusé | Raison |
|---|---|
| Microservices maintenant | 41 contexts dans un seul SCC : on extrairait un distributed monolith, facturé en incidents |
| Kubernetes le 1<sup>er</sup> jour | la complexité d'exploitation n'est pas encore payée par la volumétrie. Le brouillon `deployment/infra/` (ArgoCD/Helm/Vault/K8s, 420 l. de pipeline + charts) est **documenté inerte**, pas activé — cf. son README |
| Réécriture du backend | 150 904 lignes dans `src/main`, 1 536 fichiers, 2 178 tests : la réécriture détruit précisément la valeur (la connaissance métier encodée) |
| Blockchain / ledger tokenisé / « Web3 église » | aucune demande mesurée, un risque de conformité, et un signal **négatif** en due-diligence technique |
| « Agents IA » comme architecture | l'IA est un adaptateur au service d'une question chiffrée ; sans P5 (data), c'est un coût récurrent sans actif |
| Remplacer l'auth multitenant d'un coup | l'IAM est le point de rupture le plus cher ; on la rend policy-driven et on migrera l'IdP par **double émission**, pas par big-bang (ADR-007) |

---

## 7. Ordre retenu (arbitrage humain du 2026-10-09) et liaison avec le plan en cours

```
V0-A (fait : ArchUnit + gel, JDK 21 verrouillé, rapport de taille, gates 2 178 / 19 / 862)
  └─ V0-F : rompre les couples réciproques (45 ; un couple par PR, dépendance inversée)
       └─ pilote governance/departments élevé en module      ← arbitrage humain (cas le plus dur)
            └─ P2 LEDGER + idempotence                        ← la couche qui vaut le plus tôt
                 └─ P4 événements (outbox→broker, schémas)  +  M1/M2 mobile contract-first
                      └─ P5 data/IA  →  P1 IAM policy-driven  →  P6 SLO/DR/multi-région
```

Le seul **réordonnancement** ajouté au plan accepté : **P2 (ledger) avant P4/P5**. Un ledger
append-only avec idempotence est une décision de **modèle de données** — donc de plus en plus chère à
mesure que le volume s'écrit dessus. Un broker et un lakehouse sont des décisions
**d'infrastructure**, réversibles et achetables plus tard. C'est aussi le prérequis dur de tout
revenu en take-rate.

---

## 8. Ce qui reste à décider (non technique)

1. **Acquisition** : le funnel passe-t-il par le web public ? Si oui → F-B sur les pages d'entrée.
   Si non → F-A seulement, et ADR-003 est à **restreindre**, pas à exécuter.
2. **Ambition fintech** : take-rate revendu (→ P2/P3 + PCI, et donc ADR-005 en lot dédié) ou
   simple encaissement de dons (→ P2 en version minimale : append-only + idempotence, sans PCI).
   Ce choix change le multiple, pas le code des autres modules.
3. **Marché cible enterprise** : si un contrat > 5 000 membres est signé avant 2027, P1 (SSO/SCIM)
   passe **avant** P5.
4. **Résidence des données** : diaspora UE ⇒ GDPR strict ⇒ P6 avec multi-région, sinon P6 reste
   « conteneurs managés + Render ».

---

## 9. Annexes — reproduction des mesures

```bash
# Périmètre backend
find backend/src/main/java -name '*.java' | wc -l                    # 1 536 fichiers
find backend/src/main/java -name '*.java' -exec cat {} + | wc -l     # 150 904 lignes
ls -d backend/src/main/java/com/discipolat/modules/*/ | wc -l         # 135 modules

# Gel d'architecture (V0.1) et ses comptes
grep '^plafond|' backend/src/test/resources/architecture/architecture-freeze.txt
#   plafond|R1|7867   plafond|R2|0   plafond|R4|2948
awk -F'|' '$1=="objet"{c[$2]++} END{for(k in c) print k, c[k]}' \
  backend/src/test/resources/architecture/architecture-freeze.txt      # R3 354 · R5 8 · R6 45

# Absence de partie double et de rattachement compte<->transaction
grep -rn "account_id\|accountId" backend/src/main/java/com/discipolat/modules/finances   # 0
grep -n "CHECK (type IN" backend/src/main/resources/db/migration/V68__finances_module.sql

# Autorisation non policy-driven
grep -r "@PreAuthorize" backend/src/main/java --include=*.java | wc -l   # 1 001
grep -rl "@PreAuthorize" backend/src/main/java --include=*.java | wc -l   # 231 fichiers
grep -rli "scim\|openpolicyagent\|cerbos" backend/src/main/java | wc -l    # 0

# Outbox réelle, aucun broker
ls backend/src/main/java/com/discipolat/modules/core/domain/OutboxEvent.java
grep -rli "kafka\|amqp\|debezium" backend/src/main/java | wc -l             # 0

# Metering IA déjà né, consentement absent
ls backend/src/main/java/com/discipolat/modules/ai/domain/AiCreditsService.java
grep -rni "consent" backend/src/main/java/com/discipolat/modules/gdpr | wc -l # 0

# Mobile : pas de domain/, DI absente
ls mobile/lib                                                # core data features models presentation
grep -rn "ApiService()" mobile/lib --include=*.dart | wc -l   # 260
```

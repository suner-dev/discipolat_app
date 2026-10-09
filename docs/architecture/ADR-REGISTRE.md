# Registre des décisions d'architecture (ADR)

> **Objet** : instance unique qui ordonne les décisions structurantes BE / FE / Mobile / Plateforme,
> et les relie à la trajectoire de valorisation. Ce registre **n'invente aucune mesure** : chaque
> fait cités est vérifiable dans le dépôt (commandes en pied de chaque ADR).
> **Règle de conduite** : une décision = un ADR court, horodaté, avec options rejetées + raison.
> On ne remplace jamais un ADR, on le **supersède** (statut `Superseded by ADR-00N`).

---

## 0. Le but — et comment l'architecture y contribue (honnêteté)

Une valorisation à plusieurs **milliards** n'est pas portée par des frameworks mais par :
1. un **marché** massif et adressable (ici : l'écosystème des églises francophones d'Afrique + diaspora,
   et par extension la **fintech d'assistance/dons** qui multiplie le TAM par ~10) ;
2. un **fossé** (moat) défendable : **données + IA** de rétention des fidèles, **effet de réseau**
   (place de marché / API tierces), **coût de changement** (le Church-OS devient le système de record) ;
3. l'**absence de risque** perçu en due-diligence : sécurité, conformité (RGPD / SOC 2 / ISO 27001 /
   **PCI-DSS** pour le paiement), scalabilité prouvée, observabilité.

**L'architecture est le multiplicateur des points 2 et 3.** Elle ne crée pas la demande, mais elle est
la condition pour encaisser des tarifs enterprise, ouvrir une plateforme à des tiers, et ne pas casser
sous la charge. Ce registre priorise donc les capacités par **ROI de valorisation**, pas par élégance.

---

## 1. Registre

| ADR | Décision | Statut | Date | Impact valorisation |
|---|---|---|---|---|
| [ADR-001](ADR-001-separer-les-repos-et-gitlab.md) | Migrer sur GitLab **monorepo intact** d'abord (Option 1, puis Option 2 `changes:`) ; split de dépôts ensuite, seulement si besoin réel | `Accepté` (humain 2026-10-09) | 2026-09-29 | Débloque DevSecOps, résidences UE, scans par écosystème |
| [ADR-002](ADR-002-backend-clean-architecture.md) | Backend = **monolithe modulaire propre** (hexagonale + DDD), strangulation, PAS microservices d'emblée | `Accepté` (humain 2026-10-09) | 2026-10-09 | Due-diligence ; extraction de services à la demande ; CQRS/événementiel |
| [ADR-003](ADR-003-frontend-nextjs-rsc.md) | Frontend = **Next.js App Router / RSC** pour la vitrine et le SEO des landing `/e/:slug` ; le SPA authentifié évolue vers les tranches verticales de `frontend-target-architecture.md` | `Accepté` (humain 2026-10-09) | 2026-10-09 | Acquisition organique (funnel) ; design system multi-marque |
| [ADR-004](ADR-004-mobile-clean-progressive.md) | Mobile = Flutter **feature-first, clean architecture miroir du BE**, offline-first **progressif** capitalisant sur sync-lock + LowBand existants | `Accepté` (humain 2026-10-09) | 2026-10-09 | Rétention marché connexions faibles ; SDK réutilisable |

Docs cibles approfondies (les ADR pointent vers elles) :
- Backend : [backend-target-architecture.md](backend-target-architecture.md)
- Frontend : [frontend-target-architecture.md](frontend-target-architecture.md) (existe déjà, 480 l.)
- Plan d'exécution backend immédiat : [`/TODO_BACKEND_V0_CLEAN_ARCH.md`](../../TODO_BACKEND_V0_CLEAN_ARCH.md)
- Bootstrap GitLab + pipeline : [GITLAB-BOOTSTRAP.md](GITLAB-BOOTSTRAP.md)

---

## 2. Carte « capacité architecturale → levier de valorisation »

| Levier (pourquoi le multiple monte) | Capacité technique requise | ADR / doc |
|---|---|---|
| Tarifs **enterprise / multi-région** | multi-tenant à 3 régimes (pool → schema → DB dédiée), résidence données, SSO OIDC, audit trail | ADR-002 §multi-tenant |
| **Fintech** (dîmes, offrandes, mobile-money/USSD) → ×10 TAM | ledger immuable, idempotence, webhooks fail-closed, **PCI-DSS**, conciliation | ADR-002 §extract |
| **Place de marché / écosystème** (effet de réseau) | API publique versionnée, plans/quotas, OAuth tiers, webhooks sortants | ADR-002 §contrats |
| **Data + IA** (fossé) | événements→entrepôt, feature store, modèles de rétention/attrition | ADR-002 §événementiel |
| **Rétention** marché à faible bande passante | offline-first, file d'écriture, sync idempotente | ADR-004 |
| **Acquisition** organique (CAC bas) | SSR/RSC, SEO landing `/e/:slug`, perf Core Web Vitals | ADR-003 |
| **Due-diligence sans friction** | ArchUnit + contrats + observabilité + SOC2/ISO + CI reproductible | tous |

---

## 3. Ordre d'attaque retenu (validé par l'humain le 2026-10-09 — cohérent avec la culture « additif, ne jamais rien casser »)

0. **Contexte pilote V0-B = `governance` / module `departments`** (arbitrage humain, pas `identity`
   comme recommandé d'abord) : c'est le cas le plus dur (1 545 + 1 419 lignes dans `domain/`), donc le
   seul gabarit dont la validation se copie ailleurs avec garantie. `identity` → V0.6bis, `fintech` → V0.7.
1. **GitLab** (ADR-001, Option 1 — 1 jour, monorepo intact), **lancé en parallèle** de V0-A et non en
   pré-requis bloquant ; V0-B en revanche attend un pipeline GitLab vert.
2. **Backend V0** (`TODO_BACKEND_V0_CLEAN_ARCH.md`) → outillage qui **prouve** l'archi (ArchUnit,
   multi-module Maven, contrats, OTel) sans changer un seul comportement. Rentable même si la suite
   n'est jamais faite.
3. **Frontend P0** (règles de frontière ESLint, `max-warnings 0`) + **vitrine Next.js/RSC** pour le SEO.
4. **Mobile V0** (feature-first + injection) → sans réécrire.

> Chaque étape livre de la valeur **immédiate** et mesurable ; aucune n'exige de « tout arrêter ».

---

*Toutes les mesures sont reproductibles : `find backend/src/main/java/com/discipolat/modules -maxdepth 1 -type d | wc -l` (135 modules) ;
`grep -rn "ShardedTenantDataSource\|OutboxPublisher" backend/src/main/java | wc -l` (sharding + outbox déjà présents) ;
`wc -l backend/src/main/java/com/discipolat/modules/departments/domain/DepartmentManagementService.java` (1545).*

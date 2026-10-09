# KNOWN_ISSUES — Discipolat (registre des points ouverts)

> **Objet** : liste **honnête, datée et vérifiable** de tout ce qui est connu et
> non résolu. Chaque ligne porte : **constat · preuve (commande reproductible) ·
> impact · propriétaire · statut · date**.
>
> **Règle d'intégrité** : aucun défaut connu n'est déplacé, reformulé pour avoir
> l'air plus léger, ni omis. On range, on ne cache pas. Ce document est la source
> de vérité sur l'état résiduel ; il prime sur toute ancienne affirmation de
> rapport ou de cahier des charges.

**Dernière mise à jour** : 2026-10-09 · **Référence de mesure** : branche `main`.

---

## Légende des statuts

| Statut | Signification |
|---|---|
| `OUVERT` | Non traité, identifié. |
| `ATTÉNUÉ` | Réduit par un garde-fou, non supprimé. |
| `PLANIFIÉ` | Tâche définie, non exécutée. |
| `SURVEILLÉ` | Sans impact tant qu'une condition reste vraie. |

---

## A. Juridique / gouvernance

| # | Constat | Preuve | Impact | Propriétaire | Statut | Date |
|---|---|---|---|---|---|---|
| J1 | **Entité juridique et mentions légales non finalisées** — textes CGU/confidentialité sont des **gabarits**, à faire valider par un juriste selon la juridiction (RGPD/UE, Afrique, US). | `grep -rl "gabarit\|template" docs/legal/ backend/src/main/resources/db/migration/V179*` | Bloquant commercial (conformité) | Business/Legal | `OUVERT` | 2026-10-09 |
| J2 | **Titulaire de la propriété intellectuelle à formaliser** — `LICENSE` propriétaire et `NOTICE` désormais présents ; reste à faire pointer `LICENSE` vers l'entité juridique réelle et à faire valider par un conseil. | `head -3 LICENSE` | Levée de doute en due diligence | Business/Legal | `RÉSOLU 2026-10-09` — `LICENSE` + `NOTICE` ajoutés ; validation juridique finale reste ouverte (J1) | 2026-10-09 |
| J3 | **Stripe / paiements jamais testés en live** (ni avec clés de test). | `STATUS.md` § « Limites connues » | Bloquant monétisation | Produit | `OUVERT` | 2026-10-09 |

## B. Traction / mise en marché

| # | Constat | Preuve | Impact | Propriétaire | Statut | Date |
|---|---|---|---|---|---|---|
| T1 | **Aucun client payant / aucune église pilote active documentée** — pas d'ARR ni de métriques d'usage réelles. | Absence de `[BUSINESS_INPUT]` renseigné dans `PRODUCTION_READINESS.md` ; mesure d'usage démarrant à zéro | Plafond de valorisation | Business | `OUVERT` | 2026-10-09 |
| T2 | **Environnements staging/beta/prod non provisionnés** côté cloud (opération d'exécution hors code). | `render.yaml` présent, instance Render en veille (plan gratuit) | Risque de « time-to-first-revenue » | Ops | `OUVERT` | 2026-10-09 |

## C. Qualité technique — dette assumée

| # | Constat | Preuve | Impact | Propriétaire | Statut | Date |
|---|---|---|---|---|---|---|
| Q1 | **Suite unitaire principale exécutée sur H2** (`create-drop`, `flyway:false`) ; seule la chaîne Flyway est validée sur PostgreSQL réel (Testcontainers). | `cat backend/src/test/resources/application-test.yml` | Les tests métier ne prouvent pas le schéma de prod | Backend | `PLANIFIÉ` | 2026-10-09 |
| Q2 | **Profil `docker` en `ddl-auto: update`** (Hibernate écrit le schéma) — désaligné de `none` (prod) et `validate` (dev). | `grep -n ddl-auto backend/src/main/resources/application.yml` | Dérive schéma possible hors migrations | Backend | `RÉSOLU 2026-10-09` — aligné sur `validate` (schéma piloté par Flyway uniquement) | 2026-10-09 |
| Q3 | **Dette frontend** : ~270 occurrences `any`, pages > 1000 lignes, `queryKey` non systématiquement scopées tenant. | `cd frontend && npm run debt:audit` | Maintenabilité, risque de fuite cache cross-tenant | Frontend | `ATTÉNUÉ 2026-10-09` — ratchet de non-régression en CI (`npm run debt:audit` + `npm run i18n:audit`) ; la dette ne peut plus grossir. `npm run lint` désormais bloquant en CI (0 erreur) | 2026-10-09 |
| Q4 | **Méga-services backend** (jusqu'à ~1545 lignes/fichier). | `find backend/src/main/java -name "*.java" -exec wc -l {} + \| sort -rn \| head` | Maintenabilité | Backend | `SURVEILLÉ` | 2026-10-09 |
| Q5 | **~14 `TODO` résiduels** dans le code produit (dont 2 délégations outbox → Finance/Analytics, non bloquantes). | `grep -rn "TODO" backend/src/main/java \| grep -v TaskStatus` | Dette mineure | Backend | `OUVERT` | 2026-10-09 |
| Q6 | **Couverture de code non mesurée** (JaCoCo actif mais aucun seuil défini). | `grep -n jacoco backend/pom.xml` | Qualité affichée, non garantie | Backend | `PLANIFIÉ` | 2026-10-09 |
| Q7 | **SCA Java** — le scan OWASP `dependency-check` est assuré par le job CI dédié « owasp-backend » (`security.yml`, rapport en artifact, non bloquant ; porte bloquante Java = dependency-review). Documenté dans `pom.xml` et `SECURITY.md`. | `grep -rn dependency-check .github/workflows/security.yml backend/pom.xml` | Sécurité dépendances | Backend/Sécu | `RÉSOLU 2026-10-09` (documenté + traçable) | 2026-10-09 |

## D. Architecture / échelle

| # | Constat | Preuve | Impact | Propriétaire | Statut | Date |
|---|---|---|---|---|---|---|
| A1 | **Sharding/partitionnement : fondations posées** (`ShardedTenantDataSource`, `TenantShardExecutor`) mais **mono-instance** par défaut (non activé en production). | `ls backend/src/main/java/com/discipolat/common/scaling/` | Plafond d'échelle (10⁶+ tenants) | Backend | `ATTÉNUÉ` | 2026-10-09 |
| A2 | **Refonte frontend (tranches verticales)** cible non implémentée — pages encore « à plat », seul `features/auth` existe. | `ls frontend/src/features/` | Maintenabilité front | Frontend | `PLANIFIÉ` | 2026-10-09 |

---

## Comment ce document est tenu à jour

- Chaque correctif **ferme** une ligne (statut → `RÉSOLU <date>`) ou la fait
  régresser, jamais grossir en silence.
- Toute nouvelle découverte est ajoutée **ici avant** tout correctif.
- Un acquéreur sérieux lit ce fichier **avant le code** : c'est le document qui
  crée le plus de confiance, et le seul dont l'inexactitude ait des conséquences
  juridiques.

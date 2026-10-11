# KNOWN_ISSUES — Discipolat (registre des points ouverts)

> **Objet** : liste **honnête, datée et vérifiable** de tout ce qui est connu et
> non résolu. Chaque ligne porte : **constat · preuve (commande reproductible) ·
> impact · propriétaire · statut · date**.
>
> **Règle d'intégrité** : aucun défaut connu n'est déplacé, reformulé pour avoir
> l'air plus léger, ni omis. On range, on ne cache pas. Ce document est la source
> de vérité sur l'état résiduel ; il prime sur toute ancienne affirmation de
> rapport ou de cahier des charges.

**Dernière mise à jour** : 2026-10-11 · **Référence de mesure** : branche `main`.

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
| J3 | **Stripe / paiements jamais testés en live** (ni avec clés de test). Les deux defects qui bloquaient la vente sont corriges : la facturation etait **100x trop basse** (montant majeur envoye dans `unit_amount`, qui attend des centimes — CROISSANCE debite 0,29 EUR au lieu de 29 EUR) et le plan STARTUP en XAF etait **non souscriptable** (`STRIPE_PRICE_ID_REQUIRED`). Voir `docs/TODO_TARIFS_PAIEMENTS_INTERNATIONAL.md`. Reste a executer : un checkout reel en mode test (EUR + XAF, puis changement de montant). | `docs/API_TARIFS.md` §5 ; `StripeBillingService` ; `StripePriceSyncService` | Bloquant monetisation (partiellement leve) | Produit | `PARTIELLEMENT RESOLU 2026-10-11` — F1 et F2 corriges et testes (2200 tests backend) ; cloture sur un test Stripe en mode test | 2026-10-11 |

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
| A3 | **Le module `finances` n'est pas un ledger** : écriture simple (`type IN ('RECETTE','DEPENSE')`), **aucune colonne `account_id`** (les comptes et les écritures ne sont pas rattachés), et le **solde est déclaré par le client** (`createAccount` lit `body["balance"]`) sans aucun recalcul depuis les écritures (`setBalance` : 0 occurrence). Écritures mutables (`updated_at`, `deleted`, `reconciled`). | `grep -rn "account_id\|accountId\|setBalance" backend/src/main/java/com/discipolat/modules/finances` (0) · `sed -n '504,516p' backend/src/main/java/com/discipolat/modules/finances/domain/FinanceService.java` · `grep -n "CHECK (type IN" backend/src/main/resources/db/migration/V68__finances_module.sql` | **Exactité monétaire invérifiable** : bloque tout revenu en take-rate, toute licence bancaire, et la due-diligence fintech | Backend/Fintech | `OUVERT` — **décision prise** : `docs/architecture/ADR-005-ledger-immutabilite.md` (journal append-only en partie double, solde dérivé, idempotence). Exécution planifiée **après V0-F**, lots L1..L4 | 2026-10-09 |
| A4 | **Outbox : un seul consommateur par type d'événement.** `OutboxPublisher.consumers` est une `Map<String, Consumer>` alimentée par `consumers.put(eventType, …)` → **le dernier inscrit écrase le précédent**. 95 types enregistrés, tous dans le même JVM, aucun broker. | `grep -n "Map<String, Consumer<OutboxEvent>> consumers\|consumers.put" backend/src/main/java/com/discipolat/modules/core/service/OutboxPublisher.java` · `grep -c registerConsumer( backend/src/main/java/com/discipolat/modules/core/service/OutboxConsumers.java` (95) | Brancher un 2ᵉ usage (analytics, lac, partenaire) sur un type existant **désactive silencieusement** le premier → panne muette | Backend/Événements | `OUVERT` — correction **E1** de `docs/architecture/ADR-006-data-evenements-ia.md` (liste par type + curseur ; ~une journée, aucun broker requis) | 2026-10-09 |

---

## E. Exploitation / CI / exposition

| # | Constat | Preuve | Impact | Propriétaire | Statut | Date |
|---|---|---|---|---|---|---|
| E1 | **Job CI/CD « Mobile - Flutter » rouge en permanence sur `main`** (19 échecs / 0 succès sur les 100 dernières runs), alors que la suite complète passe en local. Comme `docker` a `needs:[backend, backend-gates, frontend, mobile]` et `deploy-render` a `needs:[docker]`, **ce seul job gelait toute la chaîne de déploiement**. Cause mesurée : l'assertion `find.widgetWithText(OutlinedButton, 'Réessayer')` dépend du `runtimeType` interne renvoyé par la fabrique `OutlinedButton.icon`, qui **change selon la version du SDK** (0 candidat en 3.35.6, 1 en 3.38.7) ; le bouton s'affiche et se tape dans les deux versions — **ce n'était pas un défaut produit**. | Diagnostic croisé sur le même runner et le même commit, deux SDK (runs `38139939446` puis `38140441205`, branche éphémère `diag/flutter-3356-widgettree`) : 3.38.7 → « 11 tests passed », 3.35.6 → « 9 tests passed, 2 failed » ; compteurs mesurés en CI : `find.text('Réessayer')=1` mais `find.byType(OutlinedButton)=0` / `ButtonStyleButton=0` sous 3.35.6. Échecs : `mobile/test/church_landing_screen_test.dart:182` et `:198`. | Déploiement production impossible ; CI de `main` illisible en continu (signal perdu) | Mobile/CI | `RÉSOLU 2026-10-11` — bouton ciblé par `Semantics(identifier:'churchLanding.retry')`, même convention que `churchLanding.website` / `churchPicker.cta.*` ; preuve : run `38141372840` **652/652, 0 échec sous 3.35.6**. Sur `main` (run `38144498336`) : `Mobile - Flutter` **success**, et `Docker Build & Push` success — l'image déployable `ghcr.io/suner-dev/discipolat_app/backend:18e7ffd8…` est poussée. La CI/CD de `main` ne reste rouge **que** sur `Deploy to Render` (voir E4). | 2026-10-11 |
| E2 | **Dérive de version SDK** : `ci.yml` punit `flutter-version: 3.35.6` (= borne basse de `mobile/pubspec.yaml`, `flutter: '>=3.35.6'`) alors que les développements locaux tournent sur **3.38.7**. La CI valide donc une version que personne n'utilise au poste de travail : les cassures de test apparaissent en CI, jamais en local. | `grep -n "flutter-version" .github/workflows/ci.yml` (3.35.6) · `grep -n "flutter:" mobile/pubspec.yaml` ( borne >=3.35.6) · `flutter --version` local (3.38.7) | Faux positifs / faux négatifs de non-régression mobile ; coût de diagnostic récurrent (cf. E1) | Mobile/CI | `OUVERT` — trancher : soit aligner la CI sur la version de développement, soit élever et **tenir** la borne basse documentée ; dans les deux cas tester les deux via matrix | 2026-10-11 |
| E3 | **Une instance de démonstration a été exposée publiquement avec un accès ADMIN**, via un tunnel Cloudflare éphémère (`cloudflared tunnel --url http://nginx:80`) montant la stack `docker-compose` locale, dont `command: ["--seed-demo-accounts=true"]` (l.50) et `APP_BETA_TESTING_SEED_DEMO_ACCOUNTS: "true"` (l.65) activent le seed de comptes démo. **Constat prouvé, pas supposé** : `POST /api/v1/auth/login` avec `admin@discipolat.com / password123` (identifiants **publiés dans le README**) a renvoyé **HTTP 200 avec un JWT valide portant `roles:["PASTEUR","ADMIN"]`** depuis Internet. | Réplication : `docker compose up` puis `curl -X POST https://<tunnel>/api/v1/auth/login -d '{"email":"admin@discipolat.com","password":"password123"}'` ; mot de passe en dur `DataInitializer.java:38` (`DEFAULT_PASSWORD = "password123"`) ; garde-fou existant **insuffisant** : il ne refuse ce secret que hors mode démo (`DataInitializer.java:481`) | **Brèche de sécurité réelle** (take-over admin multi‑tenant depuis Internet) ; en due diligence, un tel antécédent est éliminatoire si non déclaré et non corrigé | Sécu/Ops | `ATTÉNUÉ 2026-10-11` — tunnel **coupé** (`docker stop discipolat-cloudflared`, URL vérifiée morte : HTTP 530, login impossible) ; **la condition de ré‑exposition reste entière** : un `docker compose up` suivant rouvre la même porte. Ouvert : rendre le mot de passe démo surchargeable par variable d'environnement et refuser tout tunnel public avec seed actif | 2026-10-11 |
| E4 | **Aucun canal public ne dessert le produit** : `discipolat-api.onrender.com` ne répond pas (le keep‑alive curl sort en `28` après 95 s — l'instance ne sert pas, ce n'est pas un cold start), le statique bêta renvoie `404`, et `discipolat.com` **ne résout pas** (domaine non enregistré). Les secrets `RENDER_API_KEY` / `RENDER_API_SERVICE_ID` sont **absents** de la CI (`actions/secrets` renvoie 0 entrée), donc `deploy-render` ne pouvait pas déployer même avec une CI verte. | `curl -sS -o /dev/null -w '%{http_code}' --max-time 60 https://discipolat-api.onrender.com/actuator/health` → `000` · `gh api repos/:owner/:repo/actions/secrets --jq '.secrets[].name'` → vide · `getent hosts discipolat.com` → aucune réponse | **Valeur perçue nulle** pour un acheteur : sans démo en ligne, aucune due diligence n'aboutit ; verrou d'exploitation avant tout argument commercial | Ops/Business | `OUVERT` — E1 levé, la porte de déploiement est rouverte ; reste : provisionner Render (plan + secrets) et enregistrer le domaine. **État mesuré après merge** : `CI/CD` de `main` est passée de « rouge depuis 19 runs » à **rouge uniquement sur `Deploy to Render`** — l'étape garde-foue (`ci.yml:335`, test `-z` sur un secret injecté vide) sort en `::error::Secrets RENDER_API_KEY et RENDER_API_SERVICE_ID manquants`. Par contraste, `Deploy Bêta to Render` est **vert en trompe-l'œil** (`deploy-beta.yml:38-40`) : il **ignore** silencieusement le déploiement quand le secret manque — d'où le `404` de la bêta. Les deux hostnames `discipolat-api` et `discipolat` **existent encore** côté Render (ils ne répondent rien pendant 25 s au lieu de renvoyer un `404` immédiat comme les noms inconnus), mais ne servent pas. | 2026-10-11 |
| E5 | **Deux portes du workflow `security.yml` échouent pour raison de configuration GitHub**, pas pour un défaut de code : `dependency-review` échoue parce que la **Dependency graph n'est pas activée** sur le dépôt, et le job **Bandit** sort en `1` alors qu'il rapporte **0 problème en sévérité HIGH** (65 LOW / 4 MEDIUM) — la porte est donc mal calibrée. | `gh pr checks` run `38141372847` : « ##[error]Dependency review is not supported on this repository. Please ensure that Dependency graph is enabled » ; log Bandit : « High: 0 » puis « Process completed with exit code 1 » | CI sécurité bruitée : une porte qui échoue toujours pour la mauvaise raison finit par être ignorée | Sécu/CI | `OUVERT` — activer Dependency graph (réglage dépôt, hors code) et corriger le filtre de sévérité Bandit | 2026-10-11 |

---

## Comment ce document est tenu à jour

- Chaque correctif **ferme** une ligne (statut → `RÉSOLU <date>`) ou la fait
  régresser, jamais grossir en silence.
- Toute nouvelle découverte est ajoutée **ici avant** tout correctif.
- Un acquéreur sérieux lit ce fichier **avant le code** : c'est le document qui
  crée le plus de confiance, et le seul dont l'inexactitude ait des conséquences
  juridiques.

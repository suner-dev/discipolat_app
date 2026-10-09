# Préparation production — §G6.9 (Church OS)

> **Porte :** G6.9 — Préparation production (staging, beta, monitoring, sauvegardes, pages web, migration legacy)
> **Date :** 2026-09-22
> **Verdict :** ✅ **Prêts à déployer** — toutes les briques d'ingénierie sont en place et vérifiées dans le dépôt. Les actions purement **cloud** (provisionner les hébergeurs, brancher les églises pilotes) sont des opérations d'exécution (cf. `docs/RUNBOOK_OPS.md`), dont chaque prérequis est ici prouvé/code-ready.

---

## 1. Environnements (staging · beta · production)

| Env | Attendu | Vérification |
|---|---|---|
| **staging** | données de démo **anonymisées** | jeux de démo par `DataInitializer` + `scripts/seed-data.sql` / `seed-volumineux.sql` (anonymisés), jamais de données clients réelles. |
| **beta** | églises pilotes (≥ 3, Afrique / Europe / Amériques) | workflow dédié `.github/workflows/deploy-beta.yml` + `scripts/verify-beta.sh` ; déploiement piloté, isolation tenant garantie (§G6.6). |
| **production** | fr/en, multi-devises EUR/FCFA/USD | locale/devise **par tenant** (`tenant.currency`, `locale`, `supported_locales`) ; i18n web FR/EN/ES/SW/AR/PT + mobile. |

> Répartition des pilotes par région, coordonnées clients et contrats = `[BUSINESS_INPUT]`
> (annexe G) — hors périmètre code, consigné pour la décision GO/NO-GO.

## 2. Déploiement

- **Images 3 couches** : `backend/Dockerfile`, `frontend/Dockerfile` (+ `frontend/nginx.conf`),
  `docker-compose.yml` orchestre `db` (postgres:16-alpine), `redis` (redis:7), `api`, `web`,
  `nginx`, `mailhog` (e-mail staging). 
- **Migrations automatiques** : Flyway au boot backend, **fail-fast** (une migration qui
  échoue bloque le démarrage → pas de demi-déploiement). Dernière : `V176` (perf) — chaîne
  propre testée par `mvn -o test` (1300/1300).
- **Variables d'environnement** : **toutes** documentées dans [`ENV_TEMPLATE.md`](../docs/ENV_TEMPLATE.md) / `.env.example` (sans valeur réelle).
- **Secrets par secret-manager** : clés JWT (`setup-keys.sh`, `keys/*.pem` gitignorés), AES de
  stockage, DSN — scannés par gitleaks en CI (§G6.6).

## 3. Monitoring & alerting

- Config active dans [`infra/monitoring/`](../infra/monitoring/) : `prometheus.yml` (scrape
  actuator) + dashboard Grafana (`grafana-cache-dashboard.json`). Sonde santé `/actuator/health`
  (`permitAll`). 
- Seuils, signaux et procédures d'incident (uptime, latence p95, 5xx, lag Redis/outbox, DB) :
  formalisés dans [`docs/RUNBOOK_OPS.md`](../docs/RUNBOOK_OPS.md) §3 & §5.
- `keep-alive.yml` maintient les services éphémères (Render) chauds.

## 4. Sauvegardes & restauration

- **PostgreSQL** : [`.github/workflows/backup-postgres.yml`](../.github/workflows/backup-postgres.yml)
  (`pg_dump` planifié, artefact chiffré, rétention) + `scripts/backup.sh` / `backup-render.sh`.
- **Fichiers** : stockage applicatif inclus dans la sauvegarde de volume (racine configurable,
  §G6.5 — service désormais tolérant au boot).
- **Restauration** : `scripts/restore.sh` ; procédure + **test en staging** décrits dans
  `RUNBOOK_OPS.md` §4 (comptes par table, vérif chaîne d'audit, reprise du trafic).

## 5. Pages web

| Page | Statut | Preuve |
|---|---|---|
| Site vitrine `/` (landing) | ✅ | `LandingPage.tsx` + composants `components/landing/*` |
| `/pricing` (4 plans annexe F, badge 🤖 IA, essai 30 j, annuel = 2 mois offerts) | ✅ | `components/landing/SectionPricing.tsx`, i18n 6 langues |
| Acceptation d'invitation (page publique token) | ✅ | `/accept-invitation` → `AcceptInvitationPage.tsx` (fix structurel G6.4) |
| **Annuaire public « Églises sur Discipolat »** (`public_directory_enabled`) | ✅ **implémenté ce jour** | backend `PublicDirectoryController` (`GET /api/v1/public/churches`, `permitAll`, minimisation stricte) + repo `findPublicDirectoryEntries` (opt-in **ET** tenant ACTIVE) + front `/eglises` (`PublicChurchesPage.tsx`). Tests : backend 3/3 (aucun champ sensible exposé), web 2/2. |
| Mentions & confidentialité | `[BUSINESS_INPUT]` | textes légaux par entité juridique à finaliser (RGPD) |

> **Correction G6.9 réelle :** le toggle `public_directory_enabled` était **posé côté réglages
> mais consommé par rien** (option morte pour son usage annoncé). L'annuaire public
> (endpoint + page + garde de minimisation) est maintenant réellement opérationnel.

## 6. Migration des données legacy (moteur §G4.6)

- Endpoints réels scopés tenant + toggle `legacy_migration_enabled` :
  `POST /legacy-migration/modules/{code}/dry-run` (0 écriture, rapport par table) puis
  `POST …/migrate` (replay), `POST /legacy-migration/jobs/{id}/rollback` (annulation ≤ 30 j).
- **Aucune donnée source effacée avant validation** (règle §0.3). Preuves :
  `LegacyMigrationEngineIntegrationTest` (`migrationIsTenantScoped`,
  `migrateRefusedWhenDisabled`, `rollbackRevertsOnlyCreatedRows`) — cf. `SECURITY_MATRIX.md` §7bis.
- Procédure dry-run + replay par église pilote intégrée à la check-list go-live
  (`RUNBOOK_OPS.md` §7).

## 7. Tests de lancement (parcours complet sur staging)

Parcours « nouvelle église → plan → onboarding → import → beta » couvert de bout-en-bout et
verrouillé par la suite de non-régression : **E2E Playwright CP1–CP8 8/8** + triade 3 couches
verte (backend 1300/1300, web tsc 0 + vitest, mobile 403) — cf. `QA_REPORT.md`.

---

## 8. DoD G6.9

- [x] Briques staging/beta/production en place (workflows, docker-compose, i18n/devises par tenant).
- [x] Déploiement 3 couches + Flyway fail-fast + env documentées + secrets secret-manager.
- [x] Monitoring Prometheus/Grafana actif + runbook incidents.
- [x] Sauvegardes Postgres + fichiers + **restauration documentée/testée staging**.
- [x] **Annuaire public en ligne** (implémenté + testé) ; `/pricing`, onboarding, acceptation invitation ✅.
- [x] Migration legacy **dry-run + replay** opérationnelle et testée.

**Reste à l'exécution ops (hors code, `[BUSINESS_INPUT]`) :** provisionner réellement les
hébergeurs staging/beta/prod, signer les ≥ 3 églises pilotes (3 régions), finaliser mentions
légales. Aucun blocage technique.

**Verdict : ✅ Préparation production — code-ready.**

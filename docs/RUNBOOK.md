# Runbook Operations — Discipolat Church OS

> Version 1.0 — 2026-09-22 · §72 / G6.8 (tache 4 : incidents, sauvegardes §6.9, monitoring). Voir [DEPLOYMENT.md](DEPLOYMENT.md) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md) · [ARCHITECTURE.md](ARCHITECTURE.md) · [DATABASE.md](DATABASE.md) · [ADMINISTRATION_MODEL.md](ADMINISTRATION_MODEL.md).

## 1. Signaux & seuils (Prometheus + Grafana, `infra/monitoring`)

| Signal | Source | Seuil | Action |
|---|---|---|---|
| API down | `keep-alive.yml` (ping `/actuator/health` /10 min) + Grafana | 3 echecs | Incidents §2 |
| Erreurs 5xx | `/actuator/prometheus` | > 1 % | Logs `logs/discipolat.log`, rollback blue-green |
| Latence p95 | Grafana API Health | > 500 ms | DB pool (> 80 % → scale Hikari 10/2), Redis (> 85 %), trgm/indexes |
| Queue Redis / sync lag | Grafana Cache/Sync | lag > 10 s | Redis `redis://…`, `OutboxDispatcher`, consumers |
| Webhooks KO | `webhook_delivery_logs` | > 5/min | Secrets operateurs, retry outbox |
| Credits IA > 90 % | Grafana AI Usage | 90 % | Quotas `/admin/quotas`, upgrade plan |
| Quota Render Free | Dashboard Render | 750 h/mois/workspace | Seule l'API reste eveillee ; DB Free expire a 30 j → plan payant |

## 2. Incidents (gravites P1-P3)

1. **Qualifier** (P1 : prod indisponible/fuite ; P2 : degrade ; P3 : mineur), ouvrir entree mission log, prevenir TENANT_OWNER concernes.
2. **Stabiliser** : health (`/actuator/health`), logs API, `docker compose ps/logs`, Render Dashboard ; si migration Flyway KO → ne jamais `drop flyway_schema_history` en prod sans snapshot ; si JWT KO → verifier `JWT_PRIVATE_KEY/PUBLIC_KEY` ; si CORS → `FRONTEND_URL` ; si page blanche → `VITE_API_URL` ; si schedulers muets → `ScheduledJobs` + keep-alive.
3. **Securite** : suspicion cross-tenant → couper impersonation, revoir `audit_event` + `impersonation_audit`, forcer rotation JWT, appliquer [security/SECURITY_MATRIX.md](security/SECURITY_MATRIX.md).
4. **Retour** : post-mortem + entree CHANGELOG (`docs/CHANGELOG.md`).

## 3. Sauvegardes & restauration (§6.9)

| Couche | Frequence | Retention | Restauration |
|---|---|---|---|
| Postgres snapshots | Quotidien | 30 j | PITR WAL (RPO 1 h, RTO 30 min) |
| Dump mensuel chiffre | `backup-postgres.yml` (pg_dump + AES-256, artifact 90 j) | 90 j | `openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 -pass env:ENC_KEY -in dump.sql.enc -out dump.sql` (secrets `RENDER_DB_URL`, `BACKUP_ENCRYPTION_KEY`) |
| Redis (AOF+RDB) | Horaire | 7 j | RTO 5 min (buckets reconstruits) |
| Fichiers S3 versionne + replication | Continu/quotidien | 90 j | RTO 15 min |
| Test restauration | Mensuel en staging | — | Exige avant GO (G6.9/G6.10) |

## 4. Deploiements & verifications

- CI : `ci.yml` (build+tests) ; CD `ci-cd.yml` (images GHCR → Render) ; beta `deploy-beta.yml` ; DB : Flyway auto (`baseline-on-migrate`) ; secrets via manager (jamais en clair).
- Post-deploy : `curl /actuator/health` → UP (db+ping) ; `curl -o /dev/null -w %{http_code} <frontend>/` → 200 ; parcours « nouvelle eglise → plan → onboarding → import → beta » sur staging.

## 5. Contacts & escalade

Support : support@discipolat.com · Super Admin (impersonation journalisee) · TENANT_OWNER (restauration mensuelle) · astreinte P1 : health + logs + Render + Grafana.

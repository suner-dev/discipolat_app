# Runbook Operations — Discipolat Church OS

> Version 1.1 — 2026-09-29 (§72 / G6.8 ; tableau d'incidents A6 #4 ajouté).
> Voir [DEPLOYMENT.md](DEPLOYMENT.md) ([rotation JWT §13](DEPLOYMENT.md#13-rotation-des-clés-jwt-a6), [rollback §14](DEPLOYMENT.md#14-plan-de-rollback-retour-arrière--a6)) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md) · [ARCHITECTURE.md](ARCHITECTURE.md) · [DATABASE.md](DATABASE.md) · [ADMINISTRATION_MODEL.md](ADMINISTRATION_MODEL.md).

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

## 2. Incidents courants — symptôme / diagnostic / résolution (A6 #4)

| Symptôme | Diagnostic (commandes réelles) | Résolution |
|---|---|---|
| **API 5xx en rafale** | `curl -s https://discipolat-api.onrender.com/actuator/health` ; logs Render (Dashboard → Logs ou API `/logs`) : chercher l'exception par requête ; `docker compose logs api` en local | Si OOM/threads → §3 fuite mémoire ; si dependance KO (DB/Redis/SMTP) → ligne dédiée ci-dessous ; si code cassé après deploy → rollback [DEPLOYMENT §14](DEPLOYMENT.md#14-plan-de-rollback-retour-arrière--a6) |
| **Pool PostgreSQL saturé** (`Connection is not available`, timeBudget Hikari) | `/actuator/metrics/hikaricp.connections.active` et `...pending` ; en SQL : `SELECT count(*) FROM pg_stat_activity WHERE datname='discipolat';` | Modifier `HIKARI_MAX_POOL_SIZE` / `HIKARI_MIN_IDLE` (défauts `application.yml` : 50 / 10 ; connection-timeout 20 s, leak-detection 60 s) selon le plan puis redeploy ; si connexions `idle in transaction` → tuer (`pg_terminate_backend`) et chercher l'endpoint fautif dans les logs (transaction longue = bug métier) |
| **Redis indisponible** | Logs : `Rate limiting error for rl:...` (`PerIpRateLimiter`). **Comportement vérifié dans le code : fail-OPEN** — la requête est AUTORISÉE (`RateLimitResult.allowed(999)`) + warn : l'API reste debout, le quota anti-brute-force est temporairement perdu. | Ne PAS redémarrer l'API (elle n'en a pas besoin) : rétablir Redis (`docker compose up -d redis` / Dashboard Render → Redis). Risque résiduel : pendant la coupure, login/refresh ne sont plus throttés → surveiller `core.LoginAttempt`/`audit_event` ; si une tentative de force brute est détectée, bannir l'IP au niveau nginx/Cloudflare. |
| **Migrations Flyway échouées** (API ne démarre pas) | Log de boot : `FlywayMigrate ... V###__...: ERROR` ; `SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;` | [DEPLOYMENT §12](DEPLOYMENT.md#12-flyway-en-production--règles-dairain-a6) : corriger par une NOUVELLE migration additive, jamais éditer une migration appliquée ; `repair` uniquement après snapshot + correction manuelle ; rollback code si nécessaire (§14). Ne JAMAIS dropper `flyway_schema_history` en prod sans snapshot. |
| **Fuite mémoire JVM** (latence croissante → OOM, restart Render) | `/actuator/metrics/jvm.memory.used` (trend), `jcmd <pid> GC.heap_info`, heap dump si possible (`-XX:+HeapDumpOnOutOfMemoryError`) ; comparer les pics avec le traffic scheduler (`ScheduledJobs` — samedi 18h rappels, 6h absences) | Redeploy = soulagement immédiat, pas la cause : chercher les `findAll()` non paginés ou exports (`backend/exports`) dans le code des endpoints vus dans les logs ; limiter le tas via `JAVA_OPTS` cohérent avec le plan Render ; créer une issue avec le fragment de heap suspect. |
| **Certificat / HTTPS expiré** | `curl -svI https://discipolat.onrender.com 2>&1` puis chercher la ligne d'expiration du certificat ; les domains Render sont gérés par Render (LETSENCRYPT automatique). | Sur l'infra Render : relancer un deploy (renouvellement) ou ouvrir un ticket Render. Cas local via cloudflared trycloudflare : l'URL change à chaque redémarrage → mettre à jour `FRONTEND_URL` CORS. Un certificat CUSTOM relève du dashboard Cloudflare/Render, pas du code. |
| **401 massifs après rotation de clés** | attendu : les jetons signés avec l'ancienne clé ne vérifient plus — voir [DEPLOYMENT §13.4](DEPLOYMENT.md#13-rotation-des-clés-jwt-a6) | Communication de re-login ; la prochaine rotation se planifie (fenêtre creuse) ; vérifier que privée/publique ont été remplacées EN PAIRE (mix ancien privé + nouveau public = tous les jetons invalides). |
| **Webhooks opérateurs ignorés** (paiements Pending) | table `webhook_delivery_logs` ; logs : signature refusée = `PAYMENTS_WEBHOOK_SECRET` absent/différent de l'opérateur | Resynchroniser le secret côté Render + opérateur ; rejeu via l'endpoint webhook (idempotent par référence) — les confirmations sont REFUSÉES par conception sans secret valide (fail-closed). |

## 3. Procédure d'escalade (gravites P1-P3)

1. **Qualifier** (P1 : prod indisponible/fuite ; P2 : degrade ; P3 : mineur), ouvrir entree mission log, prevenir TENANT_OWNER concernes.
2. **Stabiliser** : health (`/actuator/health`), logs API, `docker compose ps/logs`, Render Dashboard ; si migration Flyway KO → ne jamais `drop flyway_schema_history` en prod sans snapshot ; si JWT KO → verifier `JWT_PRIVATE_KEY/PUBLIC_KEY` (rotation ? paire complete ?) ; si CORS → `FRONTEND_URL` ; si page blanche → `VITE_API_URL` ; si schedulers muets → `ScheduledJobs` + keep-alive.
3. **Securite** : suspicion cross-tenant → couper impersonation, revoir `audit_event` + `impersonation_audit`, forcer rotation JWT ([DEPLOYMENT §13](DEPLOYMENT.md#13-rotation-des-clés-jwt-a6)), appliquer [security/SECURITY_MATRIX.md](security/SECURITY_MATRIX.md). La barriere d'isolation est couverte par le test bloquant `TenantModuleIsolationEndToEndHttpTest` (le rejouer en local : `mvn test -Dtest=TenantModuleIsolationEndToEndHttpTest`).
4. **Retour** : post-mortem + entree CHANGELOG (`docs/CHANGELOG.md`).

## 4. Sauvegardes & restauration (§6.9)

| Couche | Frequence | Retention | Restauration |
|---|---|---|---|
| Postgres snapshots | Quotidien | 30 j | PITR WAL (RPO 1 h, RTO 30 min) |
| Dump mensuel chiffre | `backup-postgres.yml` (pg_dump + AES-256, artifact 90 j) | 90 j | `openssl enc -d -aes-256-cbc -pbkdf2 -iter 100000 -pass env:ENC_KEY -in dump.sql.enc -out dump.sql` (secrets `RENDER_DB_URL`, `BACKUP_ENCRYPTION_KEY`) |
| Redis (AOF+RDB) | Horaire | 7 j | RTO 5 min (buckets reconstruits) |
| Fichiers S3 versionne + replication | Continu/quotidien | 90 j | RTO 15 min |
| Test restauration | Mensuel en staging | — | Exige avant GO (G6.9/G6.10) |

## 5. Deploiements & verifications

- CI bloquante : `ci.yml` (mvn verify backend, tsc+vitest frontend, flutter mobile — artifacts de rapports a chaque job) ; securite : `security.yml` (gitleaks historique complet, dependency-review PR, npm audit high, bandit high) ; e2e : `e2e.yml` (activer a la premiere spec `e2/specs/**.spec.ts`) ; charge : `perf.yml` (k6, porte bloquante **P95 > 2000 ms → echec**) ; beta `deploy-beta.yml` ; DB : Flyway auto (`baseline-on-migrate`, additif uniquement — [DEPLOYMENT §12](DEPLOYMENT.md#12-flyway-en-production--règles-dairain-a6)) ; secrets via manager (jamais en clair).
- Post-deploy : `curl /actuator/health` → UP (db+ping) ; `curl -o /dev/null -w %{http_code} <frontend>/` → 200 ; parcours « nouvelle eglise → plan → onboarding → import → beta » sur staging.

## 6. Contacts & escalade

Support : support@discipolat.com · Super Admin (impersonation journalisee) · TENANT_OWNER (restauration mensuelle) · astreinte P1 : health + logs + Render + Grafana.

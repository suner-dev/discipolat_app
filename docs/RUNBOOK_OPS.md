# Runbook opérations — §G6.8 (Church OS)

> Document d'exploitation pour l'équipe ops : déploiement, monitoring, sauvegardes,
> restauration et procédures d'incident. S'appuie sur les briques réellement en place :
> workflows GitHub (`.github/workflows/`), `docker-compose.yml`, `infra/monitoring`,
> `infra/nginx`, scripts `scripts/backup*.sh` / `scripts/restore.sh`.

---

## 1. Environnements

| Env | But | Accès | Deploy |
|---|---|---|---|
| **staging** | validation pré-prod, données de démo **anonymisées** | équipe uniquement | workflow `ci-cd.yml` (branche de staging) |
| **beta** | églises pilotes (Afrique / Europe / Amériques) | pilotes + équipe | `.github/workflows/deploy-beta.yml` |
| **production** | clients payants (fr/en, multi-devises EUR/FCFA/USD) | public | release promotion (cf. `DEPLOYMENT.md`) |

Variables d'environnement : voir [`ENV_TEMPLATE.md`](./ENV_TEMPLATE.md) (exhaustif, sans
valeurs réelles). Secrets (clés JWT `keys/*.pem`, `AES` stockage, DSN Postgres, Redis)
fournis par **secret-manager** / `setup-keys.sh`, **jamais** commités.

---

## 2. Déploiement

- **Images** : 3 couches — backend (`backend/Dockerfile`), frontend (`frontend/Dockerfile`
  + `frontend/nginx.conf`), base de données / cache via `docker-compose.yml`.
- **Migrations** : **Flyway** automatiques au boot backend (`baseline-on-migrate`,
  ordering V###). Une migration échouée **bloque** le démarrage (fail-fast) → rollback
  de la release, pas de demi-déploiement.
- **Promotion** : merge → CI verte (build + tests 3 couches + sécurité) → image taggée →
  staging → smoke check (`scripts/smoke-check.sh`, `scripts/verify-beta.sh`) → production.

Référence complète : [`DEPLOYMENT.md`](./DEPLOYMENT.md).

---

## 3. Monitoring & alerting

Pile : **Prometheus + Grafana** (config dans [`infra/monitoring/`](../infra/monitoring/)).

| Signal | Métrique / sonde | Seuil d'alerte | Action |
|---|---|---|---|
| Disponibilité | uptime HTTP `/actuator/health` | 3 checks échoués | page ops ; vérifier le pod/service |
| Latence | p95 par endpoint (cf. budgets §71) | p95 > 500 ms soutenu | examiner plans DB (`pg_stat_statements`), N+1 |
| Erreurs | taux 5xx | > 1 % / 5 min | lire logs structurés, identifie traceId |
| Base | connexions Postgres, réplication, verrouillages | pool > 80 % | augmenter pool / tuer requêtes longues |
| Cache / file | Redis mémoire, lag outbox (`outbox_event` PENDING) | backlog croissant | vérifier `OutboxPublisher`, relance consommateurs |
| Disco | espace `person`/`event_attendance`/exports | > 80 % volume | archivage / extension |

**Logs** : `discipolat.log` (rotation gz). Corrélation par `traceId` ; les stack traces
d'exceptions non gérées sont journalisées par `GlobalExceptionHandler` (cf. sécurité §G6.6 —
un 403/404 applicatif ne doit plus polluer en 500).

---

## 4. Sauvegardes & restauration

- **PostgreSQL** : workflow [`.github/workflows/backup-postgres.yml`](../.github/workflows/backup-postgres.yml)
  (`pg_dump` planifié, artefact chiffré, rétention hors-site) + `scripts/backup.sh` /
  `scripts/backup-render.sh`.
- **Fichiers** : stockage applicatif (upload/exports, racine `discipolat.file.storage.root`,
  §39) inclus dans la sauvegarde de volume.
- **Restauration** : `scripts/restore.sh` — **testée en staging** avant toute promo
  (critère DoD §G6.9). Procédure :
  1. Geler les écritures (maintenance) ; 2. restaurer dump dans une instance de contrôle ;
  3. vérifier intégrité (comptes par table, chaîne d'audit `audit_event.hash`) ;
  4. restaurer les fichiers ; 5. reprise du trafic.
- **Fréquence cible** : quotidienne + pré-déploiement ; **RPO ≤ 24 h**, **RTO ≤ 2 h**
  (à affiner contractuellement par plan).

---

## 5. Procédures d'incident

### 5.1 Détection
Alerte Grafana/Prometheus ou rapport utilisateur. Severities : **P1** (indispo / fuite
données), **P2** (dégradation fonction majeure), **P3** (mineur).

### 5.2 Tri & atténuation
1. Confiner le périmètre (tenant concerné ? feature flag / module désactivable ?).
2. P1 sécurité (cross-tenant, fuite) : couper le vecteur (rotation secret, révocation JWT,
   module `low_band_enabled`/`public_directory_enabled` en cause) → investigation.
3. Sauvegarder l'état (logs, dump) **avant** toute intervention destructrice.

### 5.3 Résolution & post-mortem
1. Correctif + **test de non-régression** (règle G6.7 : une correction = un test).
2. Redéploiement contrôlé + smoke check.
3. Post-mortem dans `reports/` (timeline, cause racine, action). Aucun blame.

### 5.4 Contacts & escalade
Owner produit · DevOps (infra/CI) · Backend (API/DB) · Sécurité (P1). Matrice des gardes et
Endpoints sensibles : [`security/SECURITY_MATRIX.md`](./security/SECURITY_MATRIX.md).

---

## 6. Vérification d'intégrité périodique

- **Chaîne d'audit** : `GET /audit/events/verify-chain` (ADMIN/PASTEUR) — détecte toute
  altération de `audit_event` (hash chaîné `prev_hash`). Bannière `{valid, checked}` sur `/audit`.
- **Multi-tenant** : la suite `MultiTenantSecurityTests` + `TenantIsolationIntegrationTest`
  tourne en CI ; toute régression d'isolation = pipeline rouge (bloquant au déploiement).

---

## 7. Check-list de lancement (go-live d'une église pilote)

- [ ] Tenant créé (plan, devise, timezone, langue) + branding ; onboarding wizard validé.
- [ ] Import legacy éventuel **dry-run puis replay** (toggle `legacy_migration_enabled`, §G4.6), rapport par table sans perte source.
- [ ] Toggles contractuels posés (`offline_mode`, `low_band_enabled`, `public_directory_enabled`).
- [ ] Premier backup post-import vérifié ; monitoring actif sur le tenant.
- [ ] Smoke check (`scripts/verify-beta.sh`) vert.

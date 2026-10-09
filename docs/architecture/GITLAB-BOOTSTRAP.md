# Bootstrap GitLab — runbook de migration GitHub → GitLab (monorepo intact)

> **Rattaché à** [ADR-001](ADR-001-separer-les-repos-et-gitlab.md). **Stratégie retenue** :
> **Option 1 puis 2** — on déménage le **monorepo tel quel**, on active le pipeline **conditionné par
> `changes:`**, et on ne **splitte pas** les dépôts (l'ADR-001 le déconseine tant qu'aucune équipe
> distincte n'exige cette séparation).
> **Règle cardinale** : **ne jamais mélanger** le déménagement et une refonte. D'abord GitLab vert,
> **ensuite** `TODO_BACKEND_V0_CLEAN_ARCH.md`.

---

## 0. Pré-vol (sur GitHub, avant de toucher à quoi que ce soit)

```bash
cd discipolat_app
git tag backup-before-gitlab main && git push origin backup-before-gitlab   # filet de retour
git log --oneline -5                                                        # HEAD = 4f196242, gates verts
```

**Inventaire des secrets GitHub à recréer dans GitLab (Settings → CI/CD → Variables)** :

| Variable | Où aujourd'hui | Sensible ? | Note |
|---|---|---|---|
| `RENDER_API_KEY` | GitHub secret | 🔐 masked | clef API Render |
| `RENDER_API_SERVICE_ID` | GitHub secret | masqué | id service `discipolat-api` |
| `GITHUB_TOKEN` (registry) | implicite | — | **remplacé** par `CI_REGISTRY_*` GitLab (auto) |
| clés JWT de prod (`JWT_PRIVATE_KEY_PATH`) | hors-repo (paths) | 🔐 | **ne pas** mettre en variable CI : chemin + vault |

> **Ne jamais** commité de clé privée. Le pipeline **génère** des clés RSA **de test** à chaque job
> (comme `ci.yml`), ce n'est pas un secret.

---

## 1. Créer le projet GitLab et pousser le monorepo avec l'historique

Deux méthodes — choisir **Import** (le plus simple, conserve MR/issues si besoin) :

```bash
# Méthode A — push mirroir depuis GitHub (conserve TOUT l'historique + branches)
cd discipolat_app
git remote add gitlab git@gitlab.com:<groupe>/discipolat_app.git
git push gitlab main                       # ou: git push gitlab --mirror   pour toutes refs
# NB: le push d'un pack de ~59 Mo peut nécessiter --no-thin si erreur réseau (leçon GitHub déjà vue)
```

```text
# Méthode B — GitLab → New project → Import project → GitHub → sélectionner le repo (import complet)
```

**Après import** : vérifier que `backup-before-gitlab` et les tags sont présents côté GitLab
(`git ls-remote gitlab`). Ne **supprimer GitHub** qu'après le §4 vert (voir §6 pour la bascule).

---

## 2. Runners & variables

- **Runners** : les GitLab-hosted runners suffisent pour backend/frontend. Pour `mobile` et
  `docker:pg-gates` (Docker-in-Docker), activer un runner avec **dind** ou les **privileged jobs** ;
  sinon un runner self-hosted Docker.
- **Variables CI** : recréer la table §0 dans GitLab. `CI_REGISTRY_USER/PASSWORD/IMAGE` sont fournis
  automatiquement par le **Container Registry** GitLab (→ GHCR devient `registry.gitlab.com/...`).

---

## 3. Activer le pipeline

```bash
cd discipolat_app
git mv .gitlab-ci.yml.example .gitlab-ci.yml
git commit -m "ci: activer le pipeline GitLab (traduction fidèle de ci.yml, jobs filtrés par changes:)"
git push gitlab main
```

Le pipeline [`.gitlab-ci.yml.example`](../../.gitlab-ci.yml.example) reproduit **exactement** les jobs
`ci.yml` : `backend:h2` (mvn verify, service redis), `backend:pg-gates` (Testcontainers PG),
`frontend` (tsc/vitest/build), `mobile` (analyze/test/apk), `docker:backend` (build+push registry),
`deploy:render`. Les `rules:changes:` = **Option 2** de l'ADR-001 (indépendance de CI sans split).

**Vérification job par job** (attendus = planche verte actuelle) :
| Job | Attendu |
|---|---|
| `backend:h2` | suite ~1 884 verte |
| `backend:pg-gates` | 16/16 PG 16 |
| `frontend` | tsc 0 · vitest 862/862 · build ok |
| `mobile` | analyze 0 nouveau · test 652/652 · APK debug |
| `docker:backend` | image poussée dans le registry GitLab |
| `deploy:render` | HTTP 201/202 sur `main` |

---

## 4. Chemin de fer de bascule (progressif, sans trou dans la raquette)

1. §1-§3 ci-dessus → pipeline GitLab **vert** en parallèle de GitHub (les deux coexistent un temps).
2. Protéger `main` sur GitLab (Settings → Repository → Protected branches) + exiger **un MR approuvé**
   + pipeline vert pour merger (remplace la « porte commune » `needs:[backend,frontend,mobile]`).
3. Traduire les **autres workflows** GitHub au rythme de leur utilité (voir §5).
4. Une fois **2 semaines** sans incident GitLab : désactiver les workflows GitHub (`.github/workflows`
   en lecture seule), rendre le dépôt GitHub **archivé** ou **miroir push-only** vers GitLab.

---

## 5. Autres workflows GitHub → plan de traduction (à faire après le §3 vert)

| Fichier GitHub | Rôle | Équivalent GitLab | Priorité |
|---|---|---|---|
| `ci-cd.yml` | orchestration | fusionné dans `.gitlab-ci.yml` (stages) | haute |
| `security.yml` | SAST/DAST/SCA | job `security` : `dependency-check`/Trivy + `gitlab-sast` templates | haute (due-diligence) |
| `e2e.yml` | Playwright | job `e2e` (image `mcr.microsoft.com/playwright`) | moyenne |
| `perf.yml` | k6/perf-tests | job `perf` (image `grafana/k6`) | moyenne |
| `deploy-beta.yml` | déploiement bêta | job `deploy:beta` (rule branch) | moyenne |
| `backup-postgres.yml`, `backup-restore-test.yml` | dumps/restaure PG | `schedule` cron GitLab | à planifier |
| `keep-alive.yml` | ping backend | **obsolète si Render always-on** ; sinon cron | basse |

> On ne duplique pas au hasard : chaque job doit **prouver** sa valeur (leçon ADR-001 — le split/la
> CI multiples n'apportent rien sans besoin réel).

---

## 6. Rollback

Tant que GitHub n'est **pas archivé**, un retour est trivial :
```bash
git push origin main --force-with-lease     # sur GitHub (le tag backup existe)
```
Le tag `backup-before-gitlab` est le point d'ancrage. **Ne jamais** `--force` sans `--force-with-lease`.

---

## 7. Enchaînement avec l'architecture

Une fois le §3 vert : enchaîner sur [`/TODO_BACKEND_V0_CLEAN_ARCH.md`](../../TODO_BACKEND_V0_CLEAN_ARCH.md)
(V0-A outillage → V0-B multi-module → V0-C contrats → V0-D observabilité), **exécuté sur GitLab**.
Les jobs `contract-diff`/`asyncapi`/`report-size` de V0 s'ajoutent alors comme **nouveaux stages** du
même pipeline.

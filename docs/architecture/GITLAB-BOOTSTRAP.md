# Bootstrap GitLab — runbook de migration GitHub → GitLab (monorepo intact)

> **Rattaché à** [ADR-001](ADR-001-separer-les-repos-et-gitlab.md). **Stratégie retenue** :
> **Option 1 puis 2** — on déménage le **monorepo tel quel**, on active le pipeline **conditionné par
> `changes:`**, et on ne **splitte pas** les dépôts (l'ADR-001 le déconseille tant qu'aucune équipe
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

## 3. Activer le pipeline — **FAIT le 2026-10-09 (côté dépôt)**

Le `git mv` est **fait et commité** : le pipeline vit désormais à la racine,
[`.gitlab-ci.yml`](../../.gitlab-ci.yml). GitLab ne lit **que** ce fichier-là — le brouillon
`deployment/infra/.gitlab-ci.yml` (K8s/ArgoCD/Vault) reste **inerte** tant que ses prérequis
n'existent pas, voir [deployment/infra/README.md](../../deployment/infra/README.md).

```bash
# Ce qui reste à faire (exige un projet GitLab + un jeton : non exécutable ici)
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh dry-run     # ce qui sera poussé, sans écrire
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh push        # push --mirror + contrôle que les SHA correspondent
```

Le pipeline reproduit **exactement** les jobs de `ci.yml` **et** de `security.yml` :
`backend:h2` (mvn verify, service redis), `backend:pg-gates` (Testcontainers PG via dind),
`frontend` (tsc · lint · i18n:audit · debt:audit · vitest · build), `mobile` (analyze/test/apk),
`report:size` (V0.2), `security:npm-audit`, `security:bandit`, `security:owasp-backend`
(non bloquant, comme côté GitHub), `e2e:playwright` et `performance:k6` (manuel),
`docker:backend` (build+push registry GitLab), `deploy:render`.
Les `rules:changes:` = **Option 2** de l'ADR-001 (indépendance de CI sans split).

**Ce qui n'est PAS traduit volontairement** : `keep-alive`, `backup-postgres` (déprécié),
`backup-restore-test`, `deploy-beta`, `ci-cd` — tous déclenchés par `schedule:` ou doublons.
Les dupliquer ferait partir ces jobs **deux fois** pendant la période de miroir.

**Vérification job par job** (attendus = mesures du 2026-10-09 sur cette machine) :
| Job | Attendu | Vérifié localement ? |
|---|---|---|
| `backend:h2` | suite **2 178** tests verte (dont `ArchitectureRulesTest`) | **oui** — `mvn -o test` → BUILD SUCCESS, 13 ignorés |
| `backend:pg-gates` | **19/19** (11 Flyway + 8 EventTableContract) | **oui** — `mvn -o test -Dtest=…` → BUILD SUCCESS |
| `frontend` | tsc 0 · vitest **862/862** · i18n et dette en ratchet · build ok | **oui** |
| `mobile` | analyze 0 nouveau · test 652/652 · APK debug | **non** — aucun fichier `mobile/` touché par ce lot |
| `report:size` | rapport publié (38 BE · 52 FE · 55 mobile > 500 l.) | **oui** — `scripts/report-size.sh` |
| `docker:backend` | image poussée dans le registry GitLab | **non** — impossible sans projet GitLab |
| `deploy:render` | HTTP 201/202 sur `main` | **non** — idem, + variables à recréer |

> **Honnête limite** : la grammaire GitLab CI de `.gitlab-ci.yml` est validée par analyseur YAML
> et relue job par job, mais **aucun pipeline n'a encore tourné** — la première exécution réelle
> peut révéler des défauts d'images ou de `rules:`. C'est le §4 (chemin de fer) qui les corrigera,
> pas un « ça devrait marcher ».

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

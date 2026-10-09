# ADR-008 — Delivery GitLab : la pipeline comme machine à preuves

> **Statut** : **Accepté** (humain 2026-10-09 : « GitLab = source de vérité, fais tout ce que tu
> proposes à trancher »). **Une partie est exécutée** (pipeline racine activée, miroir outillé),
> **une partie reste humaine** (créer le projet et le jeton : rien de tout cela n'existe sur ce
> poste). Ce document dit lequel des deux, ligne par ligne — c'est tout son intérêt.
> **Date** : 2026-10-09. **État vérifié sur** `main` @ `9148caa1`.
> **À lire avec** [ADR-001](ADR-001-separer-les-repos-et-gitlab.md) (la décision de bascule) et
> [GITLAB-BOOTSTRAP.md](GITLAB-BOOTSTRAP.md) (le runbook d'exécution).

---

## 1. La thèse (pourquoi GitLab, et pas « parce que c'est GitLab »)

La migration GitLab **n'a aucun effet sur la valorisation en elle-même**. Elle accélère sa
**démonstration** : un acquéreur, un auditeur SOC 2 ou un DSI ne croient pas une architecture, ils
**relisent une preuve datée**. Si chaque merge produit les artefacts (tests, gel d'architecture,
SBOM, scans, rapport de taille), l'export « data room » devient **automatique** au lieu d'être une
semaine de chasse avant chaque levée. GitLab est retenu parce qu'il met ce cycle dans **un seul**
objet versionné (`.gitlab-ci.yml`) avec environnements, MR obligatoires et registry d'images — pas
parce qu'il est « mieux que GitHub ».

## 2. État réel, mesuré (et distinction exécuté / non exécuté)

| Élément | État | Vérification |
|---|---|---|
| `.gitlab-ci.yml` à la **racine** | ✅ **activé** (`git mv` depuis `.gitlab-ci.yml.example`), **14 jobs**, 6 stages | parse YAML local : 14 jobs, toutes les `stages:` résolues |
| `backend:h2`, `backend:pg-gates` (dind + Testcontainers), `frontend`, `mobile` | ✅ traduits de `ci.yml` (parité job-par-job), avec les ratchets `i18n:audit` / `debt:audit` et `tsc --noEmit` | commandes réellement exécutées en local le 2026-10-09 : BE **2 178** verts, gates PG **19/19**, FE **862/862** |
| `report:size` (V0.2) | ✅ branché sur `scripts/report-size.sh`, artefact 30 j | script exécuté localement : 38 BE · 52 FE · 55 mobile > 500 l. |
| `security:npm-audit`, `security:bandit`, `security:owasp-backend` | ✅ traduits de `security.yml` ; owasp en **`allow_failure: true`** (le flux NVD rend ce scan aléatoire, le rendre bloquant apprendrait à l'équipe à ignorer un rouge) | mêmes commandes locales que le workflow GitHub |
| Secret-Detection (template Core) | ✅ inclus | disponible sur tous les paliers |
| SAST / Dependency-Scanning / SBOM **par template GitLab** | ⚠️ **commentés** : palier **Premium+**. Le SBOM est produit **à la main** (voir §4) pour ne pas dépendre du palier | en-tête du fichier |
| `e2e:playwright`, `performance:k6` | ✅ présents, **`when: manual`** ; Playwright **s'auto-saute** tant qu'aucune spec n'existe (un job qui rougit sans cause = un rouge qu'on apprend à ignorer) | lecture du fichier |
| **Le pipeline n'a JAMAIS tourné sur GitLab** | ❌ | aucun projet, aucun runner, aucun jeton sur ce poste (`env`, `~/.netrc`, `git remote -v` → uniquement l'origin GitHub) |
| Le premier push GitLab | ❌ **action humaine** | `scripts/gitlab-mirror.sh` refuse de tourner sans `GITLAB_URL` et **ne simule pas** le succès |
| `deployment/infra/` (K8s/ArgoCD/Vault/Helm, ~420 l. de pipeline + charts) | 🚫 **brouillon inerte, documenté comme tel** | `deployment/infra/README.md` : clusters, serveur ArgoCD, `vault.discipolat.internal`, `kustomize/`, `terraform/` — mesurés inexistants/vide |

## 3. Décision — la pipeline comme **unique définition** de la preuve

1. **Un seul pipeline, à la racine.** GitLab ne lit que le fichier racine ; tout autre fichier
   (dont `deployment/infra/.gitlab-ci.yml`) est **inerte** tant qu'il n'est pas explicitement
   désigné ou inclus. On ne multiplie pas les définitions de porte.
2. **Les portes existantes sont traduites, pas réinventées.** Le gel ArchUnit, les gates PostgreSQL,
   les ratchets i18n/dette, le rapport de taille et les scans déjà verts sur GitHub passent tels
   quels. **Une migration qui change une porte pendant le transfert est une migration non vérifiable.**
3. **Sens du miroir pendant la transition** (précision arbitrée le 2026-10-09) : on pousse toujours
   sur **GitHub** (lui seul déclenche Render aujourd'hui) et GitHub **réplique** vers GitLab.
   L'inversion n'intervient qu'après **2 semaines sans incident** de pipeline GitLab
   (`GITLAB-BOOTSTRAP.md` §4).
4. **MR obligatoires + pipeline verte** sur `main` protégé côté GitLab, pour remplacer la « porte
   commune » implicite de la CI GitHub.
5. **Environnements nommés** : le job de déploiement déclare un `environment` (avec son URL) pour
   que chaque déploiement laisse un **enregistrement** (qui, quand, quelle SHA, réussi/échoué).
   Sans cela, aucune SLO, aucun rollback racontable, et pas de métriques DORA plus tard.
6. **Artefacts = data room** : SBOM (CycloneDX), rapports de scan, `size-report.txt`, rapports de
   tests JUnit, gel d'architecture → **expire_in 30 jours minimum**, et une release tagguée conserve
   le lot. Une release sans SBOM n'est pas vendable en due-diligence.
7. **Déploiement : Render inchangé.** La topologie n'est **pas** modifiée par cet ADR (arbitrage
   humain). Le brouillon K8s reste une **cible** documentée, à activer au déclencheur (> 100 tenants
   actifs ou multi-région), jamais « parce que les manifests sont écrits ».

## 4. Ajouts exécutés le 2026-10-09 (et seulement eux)

| Job | Rôle | Palier | Risque assumé |
|---|---|---|---|
| `sbom:release` | CycloneDX 1.6 JSON : backend via `cyclonedx-maven-plugin:2.9.1` (**244 composants** mesurés), frontend via `@cyclonedx/cyclonedx-npm@6.0.1 --package-lock-only` (**407 composants** mesurés), artefacts 30 j | Core (hors template, qui exige Premium) | `allow_failure: true` au départ : la génération dépend du réseau de résolution. **À rendre bloquant après le premier vert observé en pipeline** — changement d'une ligne, et c'est écrit dans le fichier |
| `scan:image` | scan **Trivy** (`aquasec/trivy:0.75.0`) de l'image poussée `$CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHA`, sévérités CRITICAL/HIGH, stage `report` car il consomme la sortie de `docker:backend` | Core | `allow_failure: true` au départ, même raison : une liste de CVE sans ligne de base ne doit pas imposer son verdict |
| `environment:` sur `deploy:render` | enregistre le déploiement Render comme **déploiement GitLab** (nom `production` + URL) | Core (les enregistrements de déploiement sont **gratuits** ; seul le tableau de bord DORA est payant) | aucun : le job fait la même chose, GitLab le **sait** maintenant |

**Preuve d'exécution, pas d'intention** : les commandes de `sbom:release` ont été jouées localement le
2026-10-09 avec les **mêmes versions épinglées** que le fichier (Maven : BUILD SUCCESS en 28 s, 244
composants ; frontend : 407 composants). Au passage, la **première version du job était fausse** : elle
appelait `cyclonedx-npm --ignore-scripts`, option **supprimée en v6** — le run local l'a refusée
(`error: unknown option '--ignore-scripts'`) avant le push, ce qu'aucune relecture attentive n'aurait
attrapé. Le stage de `scan:image` est `report` et non `security` pour la même raison de véracité :
je n'ai pas vérifié en pipeline réel qu'un `needs:` peut cibler un stage postérieur.

Les templates **Premium** (SAST, Dependency-Scanning, SBOM géré, DORA) restent commentés **avec le
palier requis écrit en face** : si un palier payant est souscrit, l'activation est un décommentage,
pas une réécriture.

## 5. Ce qui est volontairement **non** traduit

| Workflow GitHub | Pourquoi |
|---|---|
| les jobs en `schedule:` (ex. scans nocturnes) | deux sources de vérité = **deux départs** pour le même scan ; la planification se règle côté GitLab (`CI/CD → Schedules`) après la bascule |
| ce qui dépend d'un secret GitHub spécifique (GHCR, `GITHUB_TOKEN`) | remplacé par `CI_REGISTRY_*` natifs GitLab |
| les workflows d'expérimentation jetables | à retraduire **à l'usage**, pas par réflexe de parité : la parité n'est pas un objectif |

La liste exacte figure dans `GITLAB-BOOTSTRAP.md` §3, avec la colonne « vérifié localement ? ».

## 6. Ce qui reste à faire, côté humain (impossible sans l'opérateur)

```bash
# 1. Créer le projet GitLab, VIDE, visibilité privée (interface ou API)
# 2. Pousser l'historique complet et vérifier l'égalité des SHA :
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh dry-run          # rien n'écrit
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh push             # puis assert local == remote
# 3. Recréer les variables CI : RENDER_API_KEY, RENDER_API_SERVICE_ID, PERF_JWT_TOKEN
#    (masquées) — voir GITLAB-BOOTSTRAP.md §0 ; le script les rappelle lui-même.
# 4. Settings → Repository → Protected branches = main ; Merge requests → « Pipeline must succeed ».
# 5. Lire la PREMIÈRE pipeline : c'est elle qui prouve, pas ce document.
```

`scripts/gitlab-mirror.sh` ne **simule jamais** un succès : sans `GITLAB_URL` il imprime la
procédure et sort en 2 ; après push il compare `main` local et `main` distant et **échoue** si ils
diffèrent ; le jeton est masqué à chaque ligne de sortie (prouvé : 0 occurrence du jeton de test dans
les journaux).

## 7. Options rejetées (et pourquoi)

| Option | Rejetée parce que |
|---|---|
| Bascule « big-bang » (GitHub coupé le jour 1) | Render est déclenché par GitHub aujourd'hui ; couper avant la première pipeline GitLab verte = **plus aucun déploiement possible** pendant la durée de l'incident |
| Activer `deployment/infra/.gitlab-ci.yml` (ArgoCD/Vault/K8s) tout de suite | ses prérequis sont mesurés inexistants ; le premier rouge viendrait de l'infra et non du code — le meilleur moyen d'apprendre à ignorer un rouge |
| Réécrire la pipeline en YAML ancré sur les templates GitLab (Auto DevOps) | Auto DevOps impose ses portes ; on veut **les portes existantes et déjà vertes**, traduites à l'identique |
| Rendre le SBOM et le scan d'image bloquant immédiatement | ces deux jobs dépendent du réseau de résolution : ils rougiraient pour une raison non liée au code. `allow_failure` **avec une date et une condition de bascule** est la position honnête |
| Attendre la fin de V0 pour parler delivery | la pipeline est ce qui **prouve** V0 ; l'activer en parallèle ne touche ni au code ni aux portes |

## 8. Comment revérifier

```bash
python3 - <<'PY'          # structure du pipeline (14 jobs, stages résolues)
import yaml
d = yaml.safe_load(open('.gitlab-ci.yml'))
st = d['stages']; jobs = {k: v for k, v in d.items() if isinstance(v, dict) and 'script' in v}
print(len(jobs), 'jobs :', sorted(jobs))
print('stages inconnues :', [k for k, v in jobs.items() if v.get('stage') not in st])
print('tolerants       :', sorted(k for k, v in jobs.items() if v.get('allow_failure')))
print('environnements  :', {k: v['environment'] for k, v in jobs.items() if v.get('environment')})
PY
grep -n "allow_failure" .gitlab-ci.yml          # quels jobs sont tolérants aujourd'hui, et pourquoi
sed -n '/^## 3/,/^## 4/p' docs/architecture/GITLAB-BOOTSTRAP.md   # tableau job par job, colonne « verifie localement ? »
GITLAB_URL= scripts/gitlab-mirror.sh dry-run 2>&1 | head -3        # sort en 2, ne simule rien
```

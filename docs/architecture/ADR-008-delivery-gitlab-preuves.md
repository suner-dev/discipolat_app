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
| `report:size` (V0.2 + V0.16) | ✅ branché sur `scripts/report-size.sh` **et** `scripts/architecture-couples.sh`, deux artefacts 30 j | les deux scripts exécutés localement : taille 38 BE · 52 FE · 55 mobile > 500 l. ; **classement des 45 couples R6** (1 `souls <-> users` 100 … 7 `audit <-> users` 78), `--check` vert et **rouge sur les 7 corruptions** d'un gel impossible |
| `security:npm-audit`, `security:bandit`, `security:owasp-backend` | ✅ traduits de `security.yml` ; owasp en **`allow_failure: true`** (le flux NVD rend ce scan aléatoire, le rendre bloquant apprendrait à l'équipe à ignorer un rouge) | mêmes commandes locales que le workflow GitHub |
| Secret-Detection (template Core) | ✅ inclus | disponible sur tous les paliers |
| SAST / Dependency-Scanning / SBOM **par template GitLab** | ⚠️ **commentés** : palier **Premium+**. Le SBOM est produit **à la main** (voir §4) pour ne pas dépendre du palier | en-tête du fichier |
| `e2e:playwright`, `performance:k6` | ✅ présents, **`when: manual`** ; Playwright **s'auto-saute** tant qu'aucune spec n'existe (un job qui rougit sans cause = un rouge qu'on apprend à ignorer) | lecture du fichier |
| **Le pipeline n'a JAMAIS tourné sur GitLab** | ❌ | aucun projet, aucun runner, aucun jeton sur ce poste (`env`, `~/.netrc`, `git remote -v` → uniquement l'origin GitHub) |
| Le premier push GitLab | ❌ **action humaine** | `scripts/gitlab-mirror.sh` refuse de tourner sans `GITLAB_URL` et **ne simule pas** le succès |
| Création du projet + portes par API | 🛠️ **écrit, prouvé sur mock** | `scripts/gitlab-init-project.sh` : `bash -n`, `dry-run` sans jeton, et `prepare`/`finalize`/`statut` rejoués contre un serveur API factice local — **33 assertions** sur ses journaux, 8 scénarios ; **0 appel réseau** depuis ce poste (aucun jeton) |
| Deux défauts du script, **trouvés par le rejouage et corrigés** | ✅ **corrigés + redémontrés** | (1) `POST /projects` sans `namespace_id` : la documentation d'API dit « defaults to the current user's personal namespace » → le projet aurait été créé chez le **porteur du jeton**, alors que `gitlab-mirror.sh push` vise `GITLAB_NAMESPACE` : le push tombait sur un chemin inexistant. (2) `finalize` sur projet **vide** (`default_branch = null`) : les deux appels suivants étaient condamnés et le script les envoyait quand même. Le mock **rejette** une création dont `namespace_id` n'est pas le numéro attendu (la régression serait donc visible), et le script **refuse** le `finalize` sur dépôt vide sans rien écrire |
| Modèles de MR/issue (`.gitlab/`) | ✅ **versionnés dans le dépôt** | `.gitlab/merge_request_templates/Default.md`, `.gitlab/issue_templates/{Default,Proposer-une-decision}.md` ; palier **gratuit** vérifié dans la documentation GitLab (« Tier: Free ») |
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
6. **Artefacts = data room** : SBOM (CycloneDX), rapports de scan, `size-report.txt`,
   `couples-report.txt` (l'ordre chiffré des ruptures d'architecture, V0.16), rapports de tests
   JUnit, gel d'architecture → **expire_in 30 jours minimum**, et une release tagguée conserve
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

**Le même SBOM tourne dès aujourd'hui sur GitHub** (job `sbom` de `.github/workflows/ci.yml`, mêmes
versions épinglées, `continue-on-error: true`, artefacts 30 j). Raison : GitHub est la plateforme qui
**déclenche Render en ce moment** — attendre la bascule pour commencer à accumuler les preuves
**coûterait** six semaines de data room. Rien d'existant n'est modifié : le job n'est dans le `needs:` de
personne, il ne peut donc ni bloquer un déploiement ni changer une porte.

## 5. Ce qui est volontairement **non** traduit

| Workflow GitHub | Pourquoi |
|---|---|
| les jobs en `schedule:` (ex. scans nocturnes) | deux sources de vérité = **deux départs** pour le même scan ; la planification se règle côté GitLab (`CI/CD → Schedules`) après la bascule |
| ce qui dépend d'un secret GitHub spécifique (GHCR, `GITHUB_TOKEN`) | remplacé par `CI_REGISTRY_*` natifs GitLab |
| les workflows d'expérimentation jetables | à retraduire **à l'usage**, pas par réflexe de parité : la parité n'est pas un objectif |

La liste exacte figure dans `GITLAB-BOOTSTRAP.md` §3, avec la colonne « vérifié localement ? ».

## 6. Ce qui reste à faire, côté humain (impossible sans l'opérateur)

```bash
# 1. Créer le projet GitLab, VIDE, visibilité privée — par script plutôt qu'en cliquant :
export GITLAB_TOKEN=<PAT scope « api »> GITLAB_NAMESPACE=<groupe>
scripts/gitlab-init-project.sh dry-run      # 0 appel réseau : les appels qui seraient faits
scripts/gitlab-init-project.sh prepare      # POST /projects · variables CI masquées
# 2. Pousser l'historique complet et vérifier l'égalité des SHA :
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh dry-run          # rien n'écrit
GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
  scripts/gitlab-mirror.sh push             # puis assert local == remote
# 3. Poser les portes (branche par défaut, main protégée, pipeline obligatoire) :
scripts/gitlab-init-project.sh finalize
#    Les variables CI sont créées par « prepare » si RENDER_API_KEY, RENDER_API_SERVICE_ID et
#    PERF_JWT_TOKEN sont dans l'environnement ; sinon le script le DIT et ne les invente pas.
# 4. Règles de MR : les modèles sont versionnés (.gitlab/merge_request_templates/Default.md,
#    .gitlab/issue_templates/) — palier gratuit. Les approbations par utilisateur et CODEOWNERS
#    obligatoires sont Premium : non comptés comme porte.
# 5. LIRE la PREMIÈRE pipeline : c'est elle qui prouve, pas ce document.
scripts/gitlab-init-project.sh statut
```

`scripts/gitlab-init-project.sh` ne **simule aucun appel** : sans jeton il sort en 2, ses quatre modes
(`prepare`, `finalize`, `statut`, `dry-run`) ont été rejoués contre un **serveur API factice local**
— **33 assertions**, 8 scénarios (projet à créer · projet existant · dépôt vide · namespace
inaccessible · sorties sans jeton). Le journal du mock ne stocke que la **présence** du jeton
(colonne `token_presente`, schéma vérifié : aucun champ jeton), et **0 occurrence** du jeton ni de la
valeur de variable CI n'apparaît dans la sortie du script (le jeton ne transite pas par les arguments,
donc pas visible dans `ps`).

**Ce que le rejouage a changé au script** (un test qui ne trouve aucun défaut n'a rien testé) :
`namespace_id` est désormais **résolu par `GET /namespaces?search=…&full_path_search=true`** et le
script **sort en 2** si ce namespace n'est pas accessible au jeton, au lieu de créer le projet dans le
mauvais namespace ; `finalize` **refuse un dépôt vide** (`default_branch = null`) sans aucune écriture,
alors qu'il partait poser deux appels condamnés ; `topics[]` est envoyé en **tableau** (l'API attend
un tableau) ; les dépendances `curl`/`jq`/`python3` sont vérifiées **avant** le premier appel (sortie 3).

**Ce qui reste prouvé par le mock et non par gitlab.com** : les noms d'attributs sont relus dans la
documentation d'API, mais la sémantique réelle (par ex. le refus de protéger une branche inexistante,
le code HTTP renvoyé par un `POST /projects` dont le groupe est en visibilité restreinte) ne peut être
observée qu'au premier essai humain.

> **Le harnais factice n'est PAS versionné** (convention du dépôt : aucun artefact de test jetable dans
> Git). Il est rejouable en décrivant ses deux pièces : un serveur HTTP local qui journalise
> `{meth, path, query, token_presente, corps}` et refuse un `POST /projects` sans le `namespace_id`
> attendu, et une batterie de 33 assertions sur ce journal + sur la sortie des quatre modes. Les
> commandes de §8, elles, ne demandent aucun mock et revérifient l'essentiel (syntaxe, `dry-run`,
> sorties 2/3, `namespace_id` présent dans le script).

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
ls .gitlab/merge_request_templates .gitlab/issue_templates   # les modèles versionnés (palier gratuit)
bash -n scripts/gitlab-init-project.sh                       # syntaxe
scripts/gitlab-init-project.sh dry-run | head -4             # les appels, sans réseau
grep -n "namespace_id" scripts/gitlab-init-project.sh        # l'attribut qui manque = projet au mauvais endroit
mkdir -p /tmp/sans-jq && for b in bash curl python3 sed head tr mktemp rm sort; do ln -sf "$(command -v $b)" /tmp/sans-jq/$b; done
PATH=/tmp/sans-jq bash scripts/gitlab-init-project.sh dry-run   # sort en 3 : « dépendance absente : jq »
GITLAB_NAMESPACE=groupe scripts/gitlab-init-project.sh prepare   # sort en 2 : pas de jeton, pas de succès simulé
sed -n '/^## 3/,/^## 4/p' docs/architecture/GITLAB-BOOTSTRAP.md   # tableau job par job, colonne « verifie localement ? »
GITLAB_URL= scripts/gitlab-mirror.sh dry-run 2>&1 | head -3        # sort en 2, ne simule rien
# Ajouté par V0.16 (aucun réseau, aucune dépendance neuve — le même outillage que report-size.sh) :
bash -n scripts/architecture-couples.sh                          # syntaxe
bash scripts/architecture-couples.sh --check                     # « gel cohérent (354 arêtes R3, 45 couples R6) »
bash scripts/architecture-couples.sh --rang "audit <-> users"    # le rang publié du couple recommandé V0.15
sed -n '/^report:size:/,/^security/p' .gitlab-ci.yml             # les deux rapports, le même job
```

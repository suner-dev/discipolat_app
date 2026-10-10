# ADR-008 — Delivery GitLab : la pipeline comme machine à preuves

> **Statut** : **Accepté** (humain 2026-10-09 : « GitLab = source de vérité, fais tout ce que tu
> proposes à trancher »). **Une partie est exécutée** (pipeline racine activée, miroir outillé,
> **pipeline auto-validée par son propre stage `validate` depuis le 2026-10-10**), **une partie reste
> humaine** (créer le projet et le jeton : rien de tout cela n'existe sur ce poste). Ce document dit
> lequel des deux, ligne par ligne — c'est tout son intérêt.
> **Date** : 2026-10-09, relu et **mesuré le 2026-10-10**. **État vérifié sur** `main` @ `42b3e306`.
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
| `.gitlab-ci.yml` à la **racine** | ✅ **activé** (`git mv` depuis `.gitlab-ci.yml.example`), **16 jobs**, 6 stages — 14 le 2026-10-09, +`ci:self-check` et la scission `sbom:release` → `sbom:backend`/`sbom:frontend` le 2026-10-10 | `python3 scripts/gitlab-ci-selfcheck.py --jobs` : 16 jobs, toutes les `stages:` résolues, aucun stage sans emploi |
| **Auto-validation de la structure du pipeline** | ✅ **FAIT et branché** — `scripts/gitlab-ci-selfcheck.py`, job `ci:self-check` (stage `validate`), **même commande** dans le job `report-size` de GitHub | 17 scénarios rejoués hors dépôt : 1 témoin vert + **11 rouges obtenus un par un** (stage inconnue, need vers un job inexistant, need en stage postérieur, `optional:` retiré, `allow_failure:` retiré d'un manuel, image `:latest`, npm appelé dans une image Maven, chemin de script faux, **et 2 couplages de version image ≠ épingle du dépôt**) + 1 « couplage non contrôlable » signalé au lieu d'être déclaré conforme + 3 refus (YAML illisible → 2, fichier absent → 2, construction non représentée → 2) + dépendance PyYAML absente → 3 + déterminisme sur deux exécutions |
| **Trois défauts bloquants trouvés par cette auto-validation, et corrigés** | ✅ **corrigés + redémontrés le 2026-10-10** | (1) `scan:image` `needs: [docker:backend]` **avec une règle sur `$CI_COMMIT_TAG`** : sur une pipeline de tag, `docker:backend` n'est pas créé → la référence YAML est formelle (« the pipeline fails to create »), **aucune release n'aurait pu lancer une pipeline**. Corrigé par `needs: [{job: docker:backend, optional: true}]`. (2) `performance:k6` : `- if: main` + `when: manual` **sans** `allow_failure: true` — or avec `rules:when:manual`, `allow_failure` vaut **false par défaut** (contrairement à `when: manual` hors rules) : job manuel **bloquant** dans le stage `test`, donc pipeline `blocked`, `security`/`build`/`deploy` jamais lancés, et toute merge impossible tant que `finalize` pose `only_allow_merge_if_pipeline_succeeds`. Corrigé (parité avec `e2e:playwright`, qui avait déjà la ligne). (3) `deploy:render` en `curlimages/curl:**latest**`, contrairement à la règle que le fichier s'impose ailleurs ; épinglé à `8.13.0` (tag vérifié au registre). Ces trois défauts avaient survécu à **plusieurs relectures** du 2026-10-09 : relire un YAML ne suffit pas, il faut lui appliquer la sémantique |
| **Outillage mesuré DANS l'image de chaque job** (et pas sur le poste) | ✅ **FAIT le 2026-10-10**, table `OUTILS_PAR_IMAGE` du script de self-check | `docker run --rm --entrypoint sh <IMAGE> -c 'for t in mvn node npm npx python3 git curl openssl find xargs awk apt-get apk docker; do command -v $t …'; done'` : l'image Maven **n'a ni node, npm, npx ni python3** ; `docker:27` (Alpine) amène `mvn` sur **JDK 21.0.10** via `apk add maven openjdk21` (donc `docker:backend` jouable) ; `ubuntu:24.04` n'a **ni git, ni python3, ni curl** avant installation ; `python:3.12-slim` a pip 25.0.1 et accepte `pyyaml==6.0.1` ; `aquasec/trivy:0.75.0` répond `Version: 0.75.0` |
| **Existence des 11 références d'images** | ✅ **prouvée au registre** (jeton anonyme, aucun dépôt GitLab nécessaire) | `python3 scripts/gitlab-ci-selfcheck.py --registry` → Docker Hub / GHCR / MCR : 11/11 `present` avec digest (dont `ghcr.io/cirruslabs/flutter:3.35.6`, `mcr.microsoft.com/playwright:v1.63.0-jammy` — tag **re-vérifié le 2026-10-10** après correction, `aquasec/trivy:0.75.0`) |
| **Deux défauts latents du job `e2e:playwright`, trouvés en jouant SA commande DANS SON image** | ✅ **corrigés + rouge reproduit avant/après, puis gate ajoutée (S9)** | La commande du job a été rejouée en conteneur (`npm ci` + `npx playwright test`) avec **une spec témoin**, dans l'image que le job déclarait (`v1.49.0-jammy`) : (1) `e2/playwright.config.ts` portait le glob `` `specs/**/*.spec.ts` `` **dans son commentaire bloc** — la suite barre-étoile de `**/` **ferme le commentaire**, le reste de la ligne devient du code → `SyntaxError: /w/playwright.config.ts: Unexpected token (11:31)` ; (2) l'image embarquait les navigateurs de la révision **1148** alors que `e2/package-lock.json` épingle `@playwright/test` **1.63.0**, qui cherche **1243** → `browserType.launch: Executable doesn't exist at /ms-playwright/chromium_headless_shell-1243/…` suivi de « *Looks like Playwright was just updated to 1.63.0. Please update docker image as well.* » Corrigés : commentaire réécrit (le glob n'a plus rien à y faire), image épinglée à `v1.63.0-jammy` (= le lock), et le couplage devient la **famille S9** du self-check : le tag d'une image qui embarque son outillage est confronté à la version épinglée par le dépôt (lock npm pour Playwright, borne basse du `pubspec.yaml` **et** pin GitHub pour Flutter). **Pourquoi aucun check existant ne les voyait** : le YAML était structurellement parfait, et le job se taisait (`exit 0` quand 0 spec) — le premier dépôt de spec aurait donc rougi **sans signe avant-coureur**, avec un rouge dont la cause (la CI) ne ressemble pas à la symptomatologie (le test) |
| **Le rouge de `--registry` est OBSERVÉ, pas supposé** | ✅ **mesuré le 2026-10-10** (relance le jour même) | la même commande ressort en **sortie 1** : `ghcr.io` refuse le jeton anonyme (`AUTH ANONYME REFUSE`, puis `dial tcp 140.82.121.34:443: i/o timeout` sur le `docker pull`) → 10/11 présentes, `flutter` en rouge, bilan « 1 image(s) absente(s) du registre ». Deux conséquences : (a) le drapeau n'est pas décoratif, il sait rougir sur un fait réel ; (b) **c'est la raison pour laquelle cette preuve ne peut pas être une porte de pipeline** — un registre tiers en panne ferait rougir une pipeline sans faute de code, d'où `--registry` hors pipeline et `allow_failure: true` sur les jobs réseau |
| `backend:h2`, `backend:pg-gates` (dind + Testcontainers), `frontend`, `mobile` | ✅ traduits de `ci.yml` (parité job-par-job), avec les ratchets `i18n:audit` / `debt:audit` et `tsc --noEmit` | commandes réellement exécutées en local le 2026-10-09 : BE **2 178** verts, gates PG **19/19**, FE **862/862** |
| `report:size` (V0.2 + V0.16) | ✅ branché sur `scripts/report-size.sh` **et** `scripts/architecture-couples.sh`, deux artefacts 30 j | **rejoué dans l'image du job** (`ubuntu:24.04`) le 2026-10-10 : les quatre lignes de `script:` sortent en 0 ; taille **38 BE · 52 FE · 55 mobile** > 500 l. ; **353 arêtes R3, 44 couples R6** (mesure du 2026-10-10, après la rupture de `audit <-> users` par `42b3e306` — les 354/45 du 2026-10-09 étaient la mesure d'avant) |
| `security:npm-audit`, `security:bandit`, `security:owasp-backend` | ✅ traduits de `security.yml` ; owasp en **`allow_failure: true`** (le flux NVD rend ce scan aléatoire, le rendre bloquant apprendrait à l'équipe à ignorer un rouge) | mêmes commandes locales que le workflow GitHub |
| Secret-Detection (template Core) | ✅ inclus | disponible sur tous les paliers |
| SAST / Dependency-Scanning / SBOM **par template GitLab** | ⚠️ **commentés** : palier **Premium+**. Le SBOM est produit **à la main** (voir §4) pour ne pas dépendre du palier | en-tête du fichier |
| `e2e:playwright`, `performance:k6` | ✅ présents, **`when: manual` + `allow_failure: true`** ; Playwright **s'auto-saute** tant qu'aucune spec n'existe (un job qui rougit sans cause = un rouge qu'on apprend à ignorer) | `performance:k6` : procédure d'installation **jouée dans `ubuntu:24.04`** le 2026-10-10 → `k6 v2.3.0` installé et `K6_OK` (le run lui-même reste manuel, il frappe un environnement bêta partagé). **Attention sur `e2e:playwright`** : son `exit 0` en l'absence de spec masquait **deux défauts bloquants pour la première spec**, mesurés en conteneur le 2026-10-10 et corrigés (voir la ligne « Deux défauts latents » ci-dessus) |
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

## 4. Ajouts exécutés (2026-10-09, puis 2026-10-10)

| Job | Rôle | Palier | Risque assumé |
|---|---|---|---|
| `sbom:backend` *(2026-10-09 comme `sbom:release`, scindé le 2026-10-10)* | CycloneDX 1.6 JSON du backend via `cyclonedx-maven-plugin:2.9.1` (**244 composants** mesurés), image `maven:3.9-eclipse-temurin-21` (mvn 3.9.16 / JDK 21.0.12 **vérifiés dans l'image**) | Core (hors template, qui exige Premium) | `allow_failure: true` au départ : la génération dépend du réseau de résolution. **À rendre bloquant après le premier vert observé en pipeline** — changement d'une ligne, et c'est écrit dans le fichier |
| `sbom:frontend` *(né le 2026-10-10)* | même CycloneDX pour le frontend via `@cyclonedx/cyclonedx-npm@6.0.1 --package-lock-only` (**407 composants** mesurés), image `node:23` | Core | idem. **Cette scission est une correction, pas un raffinement** : l'unique job `sbom:release` appelait `npm`/`npx` **dans l'image Maven**, qui n'en contient pas (mesuré en conteneur le 2026-10-10 : ni `node`, ni `npm`, ni `npx`, ni `python3`). Le job aurait rougi à son premier run |
| `scan:image` | scan **Trivy** (`aquasec/trivy:0.75.0`) de l'image poussée `$CI_REGISTRY_IMAGE/backend:$CI_COMMIT_SHA`, sévérités CRITICAL/HIGH, stage `report` car il consomme la sortie de `docker:backend` | Core | `allow_failure: true` au départ, même raison : une liste de CVE sans ligne de base ne doit pas imposer son verdict. `needs: docker:backend` **`optional: true`** depuis le 2026-10-10 (sinon la pipeline de tag n'est pas créée, voir §2) |
| `ci:self-check` *(né le 2026-10-10, premier job du stage `validate` jusqu'ici vacant)* | `python3 scripts/gitlab-ci-selfcheck.py` : la pipeline **valide sa propre structure** avant de pouvoir rougir en production (stages, `needs`, manuels bloquants, images épinglées, binaires réellement présents dans l'image, chemins cités, **couplages de version image ↔ épingle du dépôt**) | Core | `needs: []`, hors du chemin de déploiement : il ne peut donc **ni** bloquer `deploy:render` **ni** masquer une vraie porte. Sa commande est rejouée à l'identique côté GitHub (§2, job `report-size`) |
| `environment:` sur `deploy:render` | enregistre le déploiement Render comme **déploiement GitLab** (nom `production` + URL) | Core (les enregistrements de déploiement sont **gratuits** ; seul le tableau de bord DORA est payant) | aucun : le job fait la même chose, GitLab le **sait** maintenant |

**Preuve d'exécution, pas d'intention** : les commandes des deux jobs SBOM ont été jouées avec les
**mêmes versions épinglées** que le fichier (Maven : BUILD SUCCESS en 28 s, 244 composants ; frontend :
407 composants). **Ce que cette phrase avait de faux** : la preuve du 2026-10-09 a été faite **sur le
poste**, qui a `node` — pas **dans l'image du job**, qui ne l'a pas. La mesure en conteneur du
2026-10-10 l'a révélé, d'où la scission ci-dessus. Leçon écrite noir sur blanc : *un « joué
localement » ne vaut preuve que s'il a été joué dans l'image d'exécution.*
Au passage, la **première version du job était fausse** : elle appelait `cyclonedx-npm --ignore-scripts`,
option **supprimée en v6** — le run local l'a refusée (`error: unknown option '--ignore-scripts'`) avant
le push, ce qu'aucune relecture attentive n'aurait attrapé. Le stage de `scan:image` est `report` et non
`security` pour la même raison de véracité : je n'ai pas vérifié en pipeline réel qu'un `needs:` peut
cibler un stage postérieur.

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
# La structure du pipeline, par le script que la pipeline elle-même exécute (remplace depuis le
# 2026-10-10 le snippet YAML ad-hoc qui ne faisait que recompter les jobs) :
python3 scripts/gitlab-ci-selfcheck.py                       # 16 jobs, 6 stages → « 0 rouge » (sortie 0)
python3 scripts/gitlab-ci-selfcheck.py --jobs                # inventaire jobs / stages / tolérants / manuels / environnements
#   sortie mesurée le 2026-10-10 : 16 jobs, 6 stages, **6 tolérants** (e2e:playwright, performance:k6,
#   sbom:backend, sbom:frontend, scan:image, security:owasp-backend), **2 manuels** (les deux tolérants —
#   un manuel sans `allow_failure` serait BLOQUANT), **1 environnement** (`deploy:render -> production`).
#   Ces cinq lignes remplacent le recomptage à l'œil qui avait laissé passer le manuel bloquant.
python3 scripts/gitlab-ci-selfcheck.py --registry            # + existence des 11 images au registre (jeton anonyme)
#   PREUVE DATÉE, pas acquise : sortie **1 observée le 2026-10-10** sur `ghcr.io` (jeton anonyme refusé)
#   après un 11/11 le même jour — un rouge de registre n'est pas un rouge de pipeline, ne pas « corriger » le YAML pour ça.
python3 -c "import ast; ast.parse(open('scripts/gitlab-ci-selfcheck.py').read())"   # syntaxe, sans écrire d'artefact
#   (et PAS `bash -n` sur ce fichier : c'est du Python, `bash -n` y sort en 2 — mesuré le 2026-10-10)
grep -n "allow_failure" .gitlab-ci.yml          # quels jobs sont tolérants aujourd'hui, et pourquoi
ls .gitlab/merge_request_templates .gitlab/issue_templates   # les modèles versionnés (palier gratuit)
bash -n scripts/gitlab-init-project.sh                       # syntaxe
scripts/gitlab-init-project.sh dry-run                       # les 12 lignes d'appels qui seraient faits, sans réseau
#   (pas de `| head -4` : la troncature tue le producteur par SIGPIPE — 141 sous `set -o pipefail`, donc
#   une recette qui rougit selon le shell de celui qui la tape. Mesuré le 2026-10-10.)
grep -n "namespace_id" scripts/gitlab-init-project.sh        # l'attribut qui manque = projet au mauvais endroit
mkdir -p /tmp/sans-jq && for b in bash curl python3 sed head tr mktemp rm sort; do ln -sf "$(command -v $b)" /tmp/sans-jq/$b; done
PATH=/tmp/sans-jq bash scripts/gitlab-init-project.sh dry-run   # sort en 3 : « dépendance absente : jq »
GITLAB_NAMESPACE=groupe scripts/gitlab-init-project.sh prepare   # sort en 2 : pas de jeton, pas de succès simulé
sed -n '/^## 3/,/^## 4/p' docs/architecture/GITLAB-BOOTSTRAP.md   # tableau job par job, colonne « Vérifié localement ? »
GITLAB_URL= scripts/gitlab-mirror.sh dry-run 2>&1                 # sort en 2, ne simule rien (10 lignes de procédure)
# Le rouge de la porte de self-check s'obtient sans mock, en corrompant UNE ligne (harnais hors dépôt,
# 17 scénarios ; le plus direct) : sed 's/^      optional: true$//' .gitlab-ci.yml > /tmp/c.yml
#   python3 scripts/gitlab-ci-selfcheck.py --file /tmp/c.yml       # sortie 1, « S4-need-absent (scan:image) »
# Rouge S9, le plus vicieux (le YAML est parfait, seul le dépôt contradit l'image) : on remet en arrière
sed 's|image: mcr.microsoft.com/playwright:v1.63.0-jammy|image: mcr.microsoft.com/playwright:v1.49.0-jammy|' .gitlab-ci.yml > /tmp/c9.yml
python3 scripts/gitlab-ci-selfcheck.py --file /tmp/c9.yml          # sortie 1, « S9-couplage-version (e2e:playwright) »
# Et le hameçon de non-régression du glob fautif doit MORDRE, sinon « 0 ligne trouvée » ne prouve rien :
git show 42b3e306:e2/playwright.config.ts | grep -cE '^\s*\*.*\*\*/.+'   # sortie 0 : la version d'avant correction est bien dénoncée
# Ajouté par V0.16 (aucun réseau, aucune dépendance neuve — le même outillage que report-size.sh) :
bash -n scripts/architecture-couples.sh                          # syntaxe
bash scripts/architecture-couples.sh --check                     # « gel cohérent (353 arêtes R3, 44 couples R6, 0 contradiction) » — mesure du 2026-10-10
bash scripts/architecture-couples.sh --rang "souls <-> users"    # le couple n°1 de centralité ; `audit <-> users`, recommandé par V0.15, a été **rompu** par `42b3e306` : `--rang` répond désormais « couple inconnu », et c'est le signe que la rupture est faite
sed -n '/^report:size:/,/^security/p' .gitlab-ci.yml             # les deux rapports, le même job
```

> **Ce bloc est exécuté, pas publié.** Le 2026-10-10, ses **23 commandes** ont été tapées telles quelles
> par un harnais hors dépôt qui contrôle la sortie **et** le code de sortie annoncé (« RECETTE VERTE :
> 23 commandes rejouées »). Le rejouage a trouvé **deux fautes dans la recette elle-même** :
> (a) `bash -n` appliqué à un fichier **Python** sort en 2 — une porte qui rougit alors que la pipeline
> est saine (remplacé par `ast.parse`) ; (b) `… | head -4` et `… | head -3` : la troncature tue le
> producteur par SIGPIPE, donc la même ligne rend 0 ou **141** selon que celui qui la tape a `set -o
> pipefail` — les deux pipes de troncature sont supprimées (sortie 0 et 2 obtenues sans ambiguïté).
> **Depuis la même date, le contrôle est anti-dérive** : un second harnais **extrait les lignes de ce
> bloc markdown** et les exécute, en lisant l'attente dans la ligne du doc elle-même (`sort en N`,
> `sortie N`). Résultat mesuré : **21 lignes de commande dans le bloc, 20 exécutées et vertes, 1 sautée**
> (`--registry`, preuve réseau). **Quatre rouges obtenus pour prouver que le garde-fou mord** : annonce
> fausse (`sort en 9` sur une commande qui sort 0), annonce **illisible** (`sort en deux` — refusé au lieu
> d'être silencieusement ramené à 0), **commande ajoutée au doc sans être jouée** (une `false` insérée dans
> le bloc rougit immédiatement), et **hameçon vide** : la ligne qui vérifie que `e2/playwright.config.ts`
> ne referme plus un commentaire bloc par un glob réussissait… parce que le fichier est corrigé. Elle est
> donc **dédoublée** dans le bloc ci-dessus par sa contre-preuve, qui exige que la version **d'avant
> correction** soit dénoncée. Référence prise sur `42b3e306` (`git show 42b3e306:e2/playwright.config.ts`)
> et non sur `HEAD` : dès le commit de correction, `HEAD` désigne la version saine et la preuve deviendrait
> silencieusement muette — c'est exactement l'erreur qui a été commise puis corrigée dans cette session.
> Une recette qu'on ne retape pas de bout en bout à chaque lot n'est pas une recette : c'est une liste de
> souhaits.

> Les nombres de la ligne `--check` **bougent à chaque rupture** : ils sont la mesure du gel commité,
> pas une constante à reporter dans les documents. Les publications du 2026-10-09/10-10 disant
> « 354 arêtes / 45 couples » (dont `docs/architecture/backend-target-architecture.md`,
> `VALORISATION-PLATEFORME.md`, `ADR-005`) sont donc **en retard d'une rupture** — corrigées ici pour
> ce qui concerne la delivery, à reprendre ailleurs quand le gel bougera de nouveau.

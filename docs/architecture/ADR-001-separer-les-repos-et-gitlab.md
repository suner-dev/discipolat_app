# ADR-001 — Séparer les dépôts et migrer vers GitLab

> **Statut** : **Accepté (humain 2026-10-09)** — monorepo **intact** (pas de split), GitLab retenu
> comme **source de vérité**, GitHub en miroir pendant la transition. Le pipeline racine
> `.gitlab-ci.yml` est activé (ex-`.gitlab-ci.yml.example`) ; le **premier push GitLab reste à faire**
> faute de projet et de jeton (voir GITLAB-BOOTSTRAP.md §3).
> **Date** : 2026-09-29. **Mesures** : exécutées sur `main` @ `72ec85d5`, 2 730 fichiers versionnés,
> 469 commits depuis le 2026-07-29, pack git 59 Mo.
> **Question** : « un dépôt backend, un dépôt frontend, un dépôt mobile, et tout déplacer sur GitLab ».

---

## 1. VERDICT EN DEUX MOTS

**Le découpage est techniquement propre et sans risque. La migration GitLab est banale. Mais les
deux décisions sont indépendantes, et le découpage n'apporte que ce que vous cherchez si ce que vous
cherchez est bien « autonomie de déploiement » — auquel cas le monorepo le fait déjà.**

---

## 2. CE QUE LA MESURE DIT (et qui contredit l'intuition)

### 2.1 Le couplage de code est ** nul** — le split est facile

| Vérification | Résultat |
|---|---|
| Le frontend référence-t-il un fichier du backend ? (`../backend`, `backend/src`, `openapi`) | **0 occurrence** |
| Le mobile référence-t-il un fichier du backend ? | **0 occurrence** |
| Contextes de build Docker | **déjà indépendants** : `./backend`, `./frontend` (`docker-compose.yml:47,103`) |
| `backend/Dockerfile` | ne copie **que** le JAR — jamais le frontend |
| Comment se font-ils connaître ? | par **URL** seulement : `VITE_API_URL` / base URL mobile |

→ Aucun import, aucun fichier partagé, aucun build.context commun. **`git filter-repo --path
backend --path-rename backend/:`** fonctionne et préserve les 469 commits. Ce n'est pas un coup de
force.

### 2.2 Le couplage **opérationnel** est en revanche total

| Élément | État mesuré | Conséquence d'un split |
|---|---|---|
| Déploiement Render | **un seul** `render.yaml` qui déclare 3 services depuis **un** dépôt, via `rootDir: backend` et `rootDir: frontend` (`render.yaml:110,123-127,229,239-243`) | Il faut 3 blueprints ou 3 fichiers de config Render, et renommer l'image `suner-dev/discipolat_app/backend` |
| CI GitHub | `ci.yml` et `ci-cd.yml` ont des jobs `backend`/`frontend`/`mobile` et **une porte de sortie commune** `needs: [backend, frontend, mobile]` (`ci.yml:116`) | 3 pipelines au lieu d'une porte unique. La garantie « les 3 sont verts ensemble » disparaît |
| Migration GitLab | 2 workflows (~200 lignes chacun) à réécrire en syntaxe GitLab CI, **+3 caches, +3 jeux de secrets, +3 runners** | ~2-3 jours de travail, sans benefit architectural |
| Scripts | 10 scripts shell/JS traversent les 3 couches ; 19 docs décrivent frontend **et** backend | Il faut décider où ils vont (4ᵉ dépôt, ou duplicer) |
| `mobile` en CI | on ne construit qu'un **APK de debug** (`ci.yml:111`) — aucune chaîne de release | L'« indépendance » du mobile est aujourd'hui théorique |

### 2.3 Le vrai coût du split n'est pas technique, il est **organisationnel**

Le dépôt actuel a produit 16 tâches backend dont **la moitié n'existaient que parce que les trois
couches étaient dans le même commit**. Les tasks B de l'agent B étaient `BLOCKED` en attendant des
tâches A, et les corrections de contrat (champ `stepOrder` au lieu de `order`, endpoint
`quota-usage/tenants/{id}`, composant `QuotaUsageCards`) ont été trouvées **en comparant les trois
couches dans le même diff**. Avec 3 dépôts, chacune de ces corrections devient : 2 PR, une fenêtre
de déploiement, et un état intermédiaire **cassé** (front déployé sur un backend qui n'a pas encore
le champ).

**Autrement dit : le monorepo est aujourd'hui votre meilleur outil de *détection* des dérives de
contrat — parce qu'un diff montre les trois couches.** Le split le supprime, à moins de le
remplacer par un mécanisme (voir §4).

---

## 3. CE QUE CHAQUE OPTION APPORTE RÉELLEMENT

| Bénéfice invoqué | Vrai avec un monorepo ? | Vrai avec 3 dépôts ? |
|---|---|---|
| Déployer le front sans toucher le back | ✅ **déjà** : Render a 3 services depuis 1 dépôt, avec `rootDir` | ✅ |
| CI plus rapide / caches séparés | ✅ avec des workflows filtrés par chemin (`paths:`) | ✅ |
| Rollback indépendant | ✅ déjà | ✅ |
| Accès séparés (contractor sur le front) | ❌ | ✅ **vrai bénéfice** |
| Clone plus léger / onboarding ciblé | ❌ (2 730 fichiers, 59 Mo) | ✅ |
| Scan de sécurité par écosystème (Maven ≠ npm ≠ pub) | ❌ (une CVE Maven bloque tout le repo) | ✅ **vrai bénéfice** |
| Équipes totalement séparées, cycles de release divergents | ❌ | ✅ **seul cas qui le justifie vraiment** |
| Corriger backend + front dans **un** commit atomique | ✅ **avantage du monorepo** | ❌ **perte sèche** |
| « Ça fait plus sérieux / plus propre » | — | ⚠️ **ça ne corrige aucun des 12 constats de l'audit frontend** |

---

## 4. LA SEULE MANIÈRE CORRECTE DE FAIRE LE SPLIT : CONTRAT PUBLIÉ ET ÉPINGLÉ

Si le split est décidé, il **doit** s'accompagner de ceci, sinon la dérive de contrat est une
question de semaines (et vous la découvrez en production).

1. **Le backend est la source de vérité.** Il publie un artefact **versionné** :
   `openapi.json` taggé, OU mieux un paquet publié dans le **GitLab Package Registry** :
   `@discipolat/api-types` (npm) et `discipolat_api` (pub.dev) généré par `orval` / `openapi-generator`.
2. **Front et mobile épinglent une version**, pas une branche :
   `"@discipolat/api-types": "0.4.2"` · `discipolat_api: 0.4.2` dans `pubspec.yaml`.
3. **Un contrôle de contrat en CI, dans chaque dépôt** : au build, aller chercher l'`openapi.json`
   **de la branche par défaut du dépôt backend** et comparer avec la version épinglée.
   → Échec **bloquant** si un champ a disparu ou changé de type (breaking change).
4. **Une porte de déploiement** : le front ne se déploie pas si le contrat épinglé ≠ contrat
   déployé. C'est le remplacement de `needs: [backend, frontend, mobile]`.
5. **Un 4ᵉ dépôt `platform`** (ou `discipolat-infra`) pour ce qui est aujourd'hui transverse :
   `docker-compose.yml`, `render.yaml`, `infra/`, `scripts/`, `docs/`, `runbooks`. Sans lui, ces
   fichiers n'ont pas de propriétaire.

---

## 5. LES TROIS OPTIONS

| | **Option 1 — Monorepo sur GitLab** (recommandée *si* la motivation est l'autonomie de déploiement) | **Option 2 — Monorepo sur GitLab + CI filtrée par chemin** | **Option 3 — 3 dépôts** |
|---|---|---|---|
| Contenu | 1 dépôt, 1 `.gitlab-ci.yml` | 1 dépôt, 1 pipeline, 3 jobs conditionnés par `changes:` | 3 dépôts + 1 dépôt platform |
| Autonomie de déploiement | déjà là |idem + caches séparés |idem |
| Risque de dérive de contrat | nul | nul | **élevé sans §4** |
| Refactorisation du frontend | 1 PR | 1 PR | 1 PR par dépôt |
| Accès séparés / scans sécurité par éco | ❌ | ❌ | ✅ |
| Coût | ~1 jour | ~2-3 jours | ~4-6 jours (+ contrat publié ~1 sem.) |
| Recommandé si | vous voulez juste GitLab | vous voulez de la vitesse CI et des pipelines séparés | équipes distinctes, contracting, ou exigence client |

**Ma recommandation, dans l'ordre :**

1. **Migrer sur GitLab d'abord, monorepo intact** (1 jour, aucune perte, vous vérifiez que la CI et
   Render fonctionnent). Ne pas mélanger les deux opérations.
2. **Puis** appliquer l'**Option 2** (2-3 jours) : 3 jobs conditionnés par `changes:` → vous obtenez
   l'indépendance de CI et de déploiement, le risque de dérive reste nul, et le monorepo garde son
   avantage de diff transversal.
3. **Décider le split (Option 3) plus tard**, et seulement si un vrai besoin apparaît (équipes
   séparées, accès distincts, ou exigence imposée). Si c'est un « ça serait plus pro », la réponse
   honnête est que **le problème que vous voulez résoudre est l'architecture du frontend**, et
   ça se règle avec `docs/architecture/frontend-target-architecture.md`, pas en découpant le dépôt.

---

## 6. SI LE SPLIT EST DÉCIDÉ : la méthode, dans l'ordre (2 h, pas plus)

```bash
# 0. sauvegarde
git tag backup-before-split main && git push origin backup-before-split

# 1. depuis le dépôt d'origine, créer les 3 dépôts vides (GitHub ou GitLab) SANS README

# 2. extraire l'historique en conservant les commits (filter-repo, PAS de copier-coller)
pip install git-filter-repo
git filter-repo --path backend/ --path-rename backend/:        --force  # →/backend
git remote add backend git@gitlab.com:.../discipolat-backend.git && git push backend main
git checkout main
git filter-repo --path frontend/ --path-rename frontend/:      --force  # → /frontend
git remote add frontend git@gitlab.com:.../discipolat-frontend.git && git push frontend main
git checkout main
git filter-repo --path mobile/ --path-rename mobile/:           --force  # → /mobile
git remote add mobile git@gitlab.com:.../discipolat-mobile.git && git push mobile main
```

**Règles non négociables de cette opération :**

- **Jamais** de `cp -r` : on perd l'historique, le blame, et la seule chose qui a permis de détecter
  les dérives de contrat (le diff transversal).
- Chaque dépôt reçoit **son** `.gitignore` (attention : les exclusions actuelles sont à la racine et
  mentionnent `target/`, `node_modules/`, `.dart_tool/` — vérifier qu'elles sont reportées).
- Le dépôt `platform` reçoit `infra/ scripts/ docs/ docker-compose.yml render.yaml` — le premier
  commit est un « nouveau dépôt » (pas d'historique), c'est assumé.
- Après le split, chaque dépôt a **son** pipeline : le backend garde Flyway + Maven, le front garde
  `tsc/vitest/build`, le mobile garde `flutter analyze/test`. Les 2 workflows actuels sont **coupés
  en 3**, pas recopiés.
- Vérifier `render.yaml` : `repo:` pointe vers l'ancien dépôt GitHub et `rootDir:` disparaît.

---

## 7. CE QU'IL FAUT TRANCHER — **tranché par l'humain le 2026-10-09**

1. **Pourquoi GitLab ?** Réponse retenue : GitLab devient la **source de vérité** de la livraison
   (pipeline unique, environnements, traçabilité MR → suite → artefacts de preuve). Ce n'est **pas**
   un prérequis de l'architecture : la bascule se fait **en parallèle** de V0. La raison commerciale
   précise (hébergement UE, exigence client, coût des minutes CI) reste à expliciter si un acquéreur
   la demande — l'ADR ne porte que la décision technique.
2. **Y a-t-il des équipes ou des clients distincts ?** **Non** à ce jour → le split des dépôts est
   **formellement écarté** (Option 2 : monorepo + `rules:changes:`).
3. **Y a-t-il une release dans les 6-8 semaines ?** **Oui** (Render, en continu) → donc migration en
   **miroir** d'abord, et **aucun** changement de topologie de déploiement dans le même mouvement.
   Pour cette raison précise, le `.gitlab-ci.yml` de `deployment/infra/` (K8s/ArgoCD/Vault) reste
   **inerte** : voir [deployment/infra/README.md](../../deployment/infra/README.md).

---

*Toutes les mesures de ce document sont reproductibles : `git ls-files | wc -l`, `git count-objects -vH`,
`grep -rn "backend/src\|\.\./backend" frontend/src mobile/lib`, `grep -n "rootDir\|repo:" render.yaml`,
`grep -n "needs:" .github/workflows/ci.yml`.*

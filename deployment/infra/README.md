# `deployment/infra/` — **cible**, pas l'état actuel

> **Statut : brouillon non actif.** Aucun de ces fichiers n'est appliqué, et aucun
> environnement décrit ici n'existe à ce jour. Le déploiement **réel** du projet est
> **Render** (API + static site), piloté par `.github/workflows/ci.yml` (job
> `deploy-render`) et par le job `deploy:render` du pipeline racine `.gitlab-ci.yml`.
> Ajouté le 2026-10-09 par l'agent de l'orchestration parallèle ; **documenté tel quel**
> par l'agent V0, qui n'a pas écrit ces manifests et ne peut donc pas en attester.

## Pourquoi ce fichier existe quand même

Une plateforme Kubernetes + ArgoCD + Vault est une **cible crédible** au-delà de ~100 tenants
actifs (cf. `docs/architecture/backend-target-architecture.md` §10, plaque P6). La décrire
tôt coûte peu. Ce qui coûte cher — et ce que ce README empêche — c'est de **laisser croire
à un lecteur (équipe suivante, acquéreur, auditeur) que cette infrastructure est en place**.

## Ce qui bloque l'activation, mesuré le 2026-10-09

| Prérequis exigé par ces manifests | État réel |
|---|---|
| Clusters nommés `discipolat-staging` / `discipolat-production` (contextes kube) | **inexistants** — aucun kubeconfig dans le dépôt, aucun cluster joignable |
| Serveur ArgoCD (12 références `apiVersion: argoproj.io/v1alpha1`) | **inexistant** — aucune URL d'instance, aucun sync wave vérifiable |
| Vault (`vault.discipolat.internal`, 6 références `vault.*` dans le chart Helm) | **inexistant** — les secrets vivent aujourd'hui en variables Render/GitHub et en fichiers de clés (`keys/`) |
| `kustomize/overlays/*/` (linté par le pipeline de ce dossier) | **répertoire vide** — le lint ne trouverait rien à valider |
| Terraform (`TERRAFORM_VERSION: 1.9.0` déclaré) | **répertoire `terraform/` vide** — aucun `.tf`, donc aucun état |
| Namespaces `discipolat-backend` / `-frontend` / `-mobile` / `-platform` / `-observability` | **non créés** (ils dépendent des clusters ci-dessus) |

**Défauts propres au `.gitlab-ci.yml` de ce dossier** (à corriger avant toute activation) :

1. GitLab ne lit **que le fichier racine** : ce fichier est **inerte** tel que placé. Pour
   l'activer il faut `Settings → CI/CD → General pipelines → Configuration file` =
   `deployment/infra/.gitlab-ci.yml` (ou un `include:` depuis la racine). **Ne pas** le faire
   avant que les prérequis ci-dessus existent : la baseline active est `.gitlab-ci.yml` à la
   racine, qui traduit fidèlement `ci.yml` et `security.yml`.
2. Les boucles de lint supposent le répertoire courant = **ce dossier**
   (`for chart in helm/*/Chart.yaml`) alors que le job s'exécute depuis la racine du dépôt.
   Chemins réels à écrire : `deployment/infra/helm/*/Chart.yaml`.
3. `VAULT_ADDR` / `KUBE_CONTEXT_*` sont des constantes en dur pointant des systèmes
   inexistants : un premier push ferait échouer ces jobs pour une raison qui n'est **pas**
   un défaut du code, ce qui est le meilleur moyen d'apprendre à l'équipe à ignorer un rouge.

## Règle de progression

Ne rien déployer depuis ce dossier tant que **chaque** prérequis du tableau ci-dessus n'est
pas (a) créé, (b) joignable depuis le runner, (c) prouvé par un run vert **et** par un
exercice de restauration. L'ordre proposé dans le plan de migration reste : modulariser
(`TODO_BACKEND_V0_CLEAN_ARCH.md`) → rompre les couples réciproques (V0-F) → extraire un
contexte → alors seulement parler orchestration.

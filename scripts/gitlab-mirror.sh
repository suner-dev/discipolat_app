#!/usr/bin/env bash
# Bascule GitHub -> GitLab (ADR-001, arbitrage humain du 2026-10-09 : GitLab = source de vérité,
# GitHub = miroir pendant la transition).
#
# Pousse l'HISTORIQUE COMPLET (miroir) vers un projet GitLab, puis configure le miroir
# GitHub -> GitLab répliqué par GitHub lui-même (Settings → Repository → Mirrored repositories),
# ce qui garde les deux dépôt alignés sans cron ni script tournant sur un poste.
#
# Prérequis (non négociables, et le script les vérifie plutôt que d'échouer à moitié) :
#   1. le projet GitLab existe et est VIDE (sinon le push --mirror écrase ou est refusé) ;
#   2. GITLAB_URL=https://oauth2:<TOKEN>@gitlab.com/<groupe>/<projet>.git
#      (token = scope write_repository, ou project token GitLab → Settings → Access Tokens) ;
#   3. aucune secret dans l'historique : lancer d'abord `gitleaks detect` (voir security.yml).
#
# Usage :
#   GITLAB_URL=... scripts/gitlab-mirror.sh dry-run    # ce qui sera poussé, sans écrire
#   GITLAB_URL=... scripts/gitlab-mirror.sh push       # push --mirror + vérification
#   GITLAB_URL=... scripts/gitlab-mirror.sh status     # compare les refs des deux côtés
#
# Le script n'imprime JAMAIS le token.

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RACINE"

ACTION="${1:-status}"

masquer() { sed -E 's#(https://)[^@/]+@#\1***@#g'; }

if [ -z "${GITLAB_URL:-}" ]; then
  cat >&2 <<'MSG'
GITLAB_URL est absent — rien n'est poussé, et rien ne sera simulé.

  export GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git"

Pour créer le projet au préalable (CLI glcr/gitlab ou interface) :
  - visibilité : privée (le code n'a aucune raison d'être public) ;
  - CI/CD : laisser le fichier racine .gitlab-ci.yml tel quel ;
  - paramètres à vérifier après le premier push : Protected branches = main,
    Merge requests → « Pipeline must succeed », et recréer dans Settings → CI/CD → Variables
    les variables RENDER_API_KEY, RENDER_API_SERVICE_ID, PERF_JWT_TOKEN (masquées).
MSG
  exit 2
fi

echo "== dépôt d'origine (GitHub) : $(git remote get-url origin | masquer)"
echo "== dépôt cible   (GitLab)   : $(echo "$GITLAB_URL" | masquer)"

# Le remote « gitlab » est (re)créféncé sans coller le token dans .git/config : on l'utilise
# en ligne de commande, jeton en mémoire le temps de la commande uniquement.
if git remote get-url gitlab >/dev/null 2>&1; then
  echo "(un remote 'gitlab' existe déjà : $(git remote get-url gitlab | masquer) — il sert de nom, l'URL/token vient de GITLAB_URL)"
fi

case "$ACTION" in
  dry-run)
    echo
    echo "== refs qui seraient poussées (--mirror = toutes les branches + tags + leur SHA)"
    git for-each-ref --format='  %(refname:short) %(objectname:short) -> %(upstream:short)%(upstream:track)' refs/heads refs/tags
    echo
    echo "== HEAD local vs origin/main"
    if [ "$(git rev-parse main)" = "$(git rev-parse origin/main)" ]; then
      etat="oui"
    else
      etat="NON - pousser d'abord sur GitHub"
    fi
    echo "  local  main : $(git rev-parse --short main)"
    echo "  origin      : $(git rev-parse --short origin/main)"
    echo "  a jour      : $etat"
    echo
    echo "(dry-run : aucune écriture effectuée)"
    ;;
  push)
    echo "== push --mirror (historique complet, branches et tags)"
    # --force n'est PAS utilisé : un miroir initial doit être un fast-forward intégral. Si GitLab
    # refuse, c'est que le projet n'est pas vide — et il vaut mieux s'arrêter là qu'écraser.
    git push --mirror "$GITLAB_URL" 2>&1 | masquer
    echo
    echo "== vérification : les SHA GitLab doivent correspondre aux SHA locaux"
    git ls-remote "$GITLAB_URL" 2>/dev/null | masquer | awk '{print "  " $2 " " substr($1,1,8)}' \
      | grep -E "refs/(heads/main|tags/)" | head -20
    local_main="$(git rev-parse refs/heads/main)"
    remote_main="$(git ls-remote "$GITLAB_URL" refs/heads/main | awk '{print $1}')"
    if [ -z "$remote_main" ]; then
      echo "ÉCHEC : refs/heads/main introuvable sur GitLab après le push." >&2
      exit 1
    fi
    if [ "$local_main" = "$remote_main" ]; then
      echo "OK : main identique des deux côtés ($(git rev-parse --short main))."
    else
      echo "ÉCHEC : divergence main local=$local_main gitlab=$remote_main" >&2
      exit 1
    fi
    echo
    echo "(le miroir CONTINU se règle côté GitHub : Settings → Import and export → Mirroring repositories,"
    echo " destination = l'URL GitLab avec token, « Trigger build » non coché, direction GitHub -> GitLab.)"
    ;;
  status)
    echo "== refs locales"
    git for-each-ref --format='  %(refname:short) %(objectname:short)' refs/heads refs/tags | head -20
    if [ -n "${GITLAB_URL:-}" ]; then
      echo "== refs GitLab"
      git ls-remote "$GITLAB_URL" 2>/dev/null | masquer \
        | awk '$2 ~ /refs\/(heads|tags)/ {print "  " $2 " " substr($1,1,8)}' | head -20 \
        || echo "  (GitLab injoignable avec cette URL — verifier le token et l'hote)"
    fi
    ;;
  *)
    echo "action inconnue : $ACTION (attendu dry-run | push | status)" >&2
    exit 2
    ;;
esac

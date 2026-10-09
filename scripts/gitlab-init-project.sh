#!/usr/bin/env bash
# Création et réglage du projet GitLab cible (ADR-008 §6, suite de ADR-001).
#
# POURQUOI CE SCRIPT : `gitlab-mirror.sh` pousse l'historique mais suppose le projet GitLab
# DÉJÀ créé et réglé. Or ce réglage (protéger main, exiger la pipeline, recréer les variables
# masquées, définir la branche par défaut) est exactement ce qui rend la bascule vérifiable — et
# il se rate facilement en cliquant dans l'interface. Le script ne fait que les appels d'API
# documentés, imprime la réponse de chacun, et ÉCHOUE bruyamment plutôt que de simuler.
#
# Usage (dans cet ordre) :
#   GITLAB_TOKEN=<PAT scope api> scripts/gitlab-init-project.sh prepare     # projet + variables
#   GITLAB_URL="https://oauth2:<TOKEN>@gitlab.com/<groupe>/discipolat_app.git" \
#     scripts/gitlab-mirror.sh push                                         # historique (autre script)
#   GITLAB_TOKEN=<PAT scope api> scripts/gitlab-init-project.sh finalize    # protection + portes
#   GITLAB_TOKEN=<PAT scope api> scripts/gitlab-init-project.sh statut      # première pipeline
#
# Modes :
#   prepare  = créer le projet s'il n'existe pas (idempotent), pousser les variables CI presentes
#   finalize = branche principale par défaut, main protégée, « pipelines must succeed »
#   statut   = état de la dernière pipeline et de ses jobs (ce que GitLab a réellement exécuté)
#   dry-run  = imprime ce qui serait appelé, AUCUN appel réseau, aucune écriture
#
# Sécurité : le jeton n'est jamais passé en argument (il transite par `--config` via /dev/fd,
# donc jamais visible dans `ps`), et toute sortie est masquée. Sans GITLAB_TOKEN, dry-run
# fonctionne et les modes d'écriture sortent en 2 au lieu d'inventer un succès.
#
# Namespace : l'API `POST /projects` n'accepte PAS un chemin, elle exige un `namespace_id`
# NUMÉRIQUE ; sans lui GitLab crée le projet dans l'espace personnel du JETON, ce qui ne
# correspondrait plus à l'URL de `gitlab-mirror.sh push`. Le script résout donc
# GITLAB_NAMESPACE via `GET /namespaces?search=…&full_path_search=true` et ÉCHOUE si le
# namespace n'est pas accessible au jeton — il ne déplace jamais le projet en secret.
#
# Paliers GitLab : tous les appels utilisés sont disponibles sur le palier gratuit. Les
# protections fines par utilisateur (approbations, CODEOWNERS) sont Premium : le script les
# signale, il ne les appelle pas.

set -euo pipefail

urlencode() { python3 -c 'import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1],safe=""))' "$1"; }
masquer() { sed -E "s#${GITLAB_TOKEN:-__jeton_absent__}#***#g"; }

# Dépendances vérifiées AVANT le premier appel ET avant la première utilisation de `python3`
# par `urlencode` : un `jq` absent ferait échouer `projet_id` en silence, et le script créerait
# un DEUXIÈME projet. On nomme ce qui manque.
for binaire in curl jq python3; do
  command -v "$binaire" >/dev/null 2>&1 || { echo "dépendance absente : $binaire — rien n'est appelé." >&2; exit 3; }
done

ACTION="${1:-}"
GITLAB_HOST="${GITLAB_HOST:-https://gitlab.com}"
API_BASE="${GITLAB_API:-$GITLAB_HOST/api/v4}"
NAMESPACE="${GITLAB_NAMESPACE:-}"
PROJECT="${GITLAB_PROJECT:-discipolat_app}"
VISIBLE="${GITLAB_VISIBILITY:-private}"
HOTE_SANS_SCHEME="${GITLAB_HOST#*://}"
TIMEOUT="${GITLAB_TIMEOUT:-25}"
CHEMIN="${NAMESPACE}/${PROJECT}"
CHEMIN_ENC="$(urlencode "$CHEMIN")"

CORPS="$(mktemp)"
trap 'rm -f "$CORPS"' EXIT

if [ -z "$ACTION" ]; then
  sed -n '2,36p' "$0" | masquer >&2
  exit 2
fi

# requette METHODE CHEMIN [donnee=urlencode ...] -> code HTTP, corps dans $CORPS
requette() {
  local meth="$1" chemin="$2"; shift 2
  local -a donnees=()
  local d
  for d in "$@"; do donnees+=(--data-urlencode "$d"); done
  curl --silent --show-error --max-time "$TIMEOUT" \
    --request "$meth" "${donnees[@]}" \
    --config <(printf 'header = "PRIVATE-TOKEN: %s"\n' "${GITLAB_TOKEN:-}") \
    --output "$CORPS" --write-out '%{http_code}' \
    "$API_BASE$chemin"
}

deja_une_ligne() { head -c 220 "$CORPS" | tr -d '\n' | masquer; }

cas_dry_run() {
  # Le dry-run doit rester LISIBLE sans namespace : on affiche un marqueur plutôt qu'un
  # « search= » vide qui ferait croire à un chemin correct.
  local ns_affiche
  if [ -n "$NAMESPACE" ]; then ns_affiche="$(urlencode "$NAMESPACE")"; else ns_affiche="<GITLAB_NAMESPACE>"; fi
  echo "== dry-run : appels qui seraient faits (aucun appel réseau, aucune écriture)"
  echo "  GET  $API_BASE/namespaces?search=$ns_affiche&full_path_search=true   (→ namespace_id ; sans lui le projet tombe dans l'espace personnel du jeton)"
  echo "  POST $API_BASE/projects            name=$PROJECT path=$PROJECT namespace_id=<résolu ci-dessus> visibility=$VISIBLE initialize_with_readme=false topics[]=$PROJECT"
  echo "  GET  $API_BASE/projects/${ns_affiche}%2F$PROJECT                              (réutilisation si le projet existe déjà)"
  local k
  for k in RENDER_API_KEY RENDER_API_SERVICE_ID PERF_JWT_TOKEN; do
    if [ -n "${!k:-}" ]; then
      echo "  POST $API_BASE/projects/:id/variables  key=$k masked=true protected=true  (valeur présente dans l'environnement, jamais imprimée)"
    else
      echo "  (variable $k absente de l'environnement : rien n'est créé pour elle — à faire après coup dans Settings → CI/CD → Variables)"
    fi
  done
  echo "  PUT  $API_BASE/projects/:id          default_branch=main only_allow_merge_if_pipeline_succeeds=true"
  echo "  POST $API_BASE/projects/:id/protected_branches  name=main push_access_level=40 merge_access_level=40 unprotect_access_level=40 allow_force_push=false"
  echo "  GET  $API_BASE/projects/:id/pipelines?per_page=1 puis /pipelines/:id/jobs  (mode statut)"
  echo
  echo "(les variables de porte par utilisateur — approbations, CODEOWNERS — sont Premium : non appelées ici)"
  exit 0
}

# résout GITLAB_NAMESPACE en identifiant numérique ; ne renvoie RIEN et sort en 2 si le
# namespace est inaccessible au jeton (plutôt que de créer le projet au mauvais endroit).
resoudre_namespace() {
  local code
  code="$(requette GET "/namespaces?search=$(urlencode "$NAMESPACE")&full_path_search=true&per_page=100")"
  [ "$code" = "200" ] || { echo "ÉCHEC lecture des namespaces (HTTP $code) : $(deja_une_ligne)" >&2; exit 1; }
  NS_ID="$(jq -r --arg p "$NAMESPACE" 'map(select(.full_path == $p)) | .[0].id // empty' "$CORPS")"
  if [ -z "$NS_ID" ]; then
    cat >&2 <<MSG
GITLAB_NAMESPACE="$NAMESPACE" introuvable parmi les namespaces accessibles à ce jeton.

Le script NE crée pas le projet : sans namespace_id, l'API le poserait dans l'espace
personnel du porteur du jeton — et le push d'historique viserait un chemin différent.
Vérifier : (a) l'orthographe exacte (chemin complet pour un sous-groupe, ex. « groupe/projet »),
(b) que ce jeton est membre de ce groupe, (c) la liste réelle :
  curl --header "PRIVATE-TOKEN: \$GITLAB_TOKEN" "$API_BASE/namespaces?per_page=100" | jq -r '.[].full_path'
MSG
    exit 2
  fi
  echo "$NS_ID"
}

exiger_jeton() {
  if [ -z "${GITLAB_TOKEN:-}" ]; then
    cat >&2 <<'MSG'
GITLAB_TOKEN absent — aucun appel réseau, et AUCUN succès simulé.

  export GITLAB_TOKEN=<personal access token, scope « api », date d'expiration courte>
  export GITLAB_NAMESPACE=<groupe ou compte>        # ex. discipolat
  # facultatif : GITLAB_HOST (défaut https://gitlab.com), GITLAB_PROJECT (défaut discipolat_app)

Le jeton n'est jamais imprimé ni stocké par ce script ; il ne transite pas en argument
(invisibles dans `ps`). Vérifier d'abord la structure sans réseau :
  scripts/gitlab-init-project.sh dry-run
MSG
    exit 2
  fi
  if [ -z "$NAMESPACE" ]; then
    echo "GITLAB_NAMESPACE absent (ex. GITLAB_NAMESPACE=discipolat) — le script ne devine pas où créer le projet." >&2
    exit 2
  fi
}

projet_id() {
  local code
  code="$(requette GET "/projects/$CHEMIN_ENC")"
  if [ "$code" = "200" ]; then
    jq -r '.id' "$CORPS"
  else
    return 1
  fi
}

case "$ACTION" in
  dry-run)
    cas_dry_run
    ;;

  prepare)
    exiger_jeton
    echo "== 1. projet $CHEMIN (API : $API_BASE)"
    ns_id="$(resoudre_namespace)"
    echo "   namespace $NAMESPACE → id=$ns_id (résolu par l'API, pas deviné)"
    if id="$(projet_id)"; then
      echo "   existe déjà (id=$id) : aucune création, aucun écrasement."
    else
      code="$(requette POST /projects \
        "name=$PROJECT" "path=$PROJECT" "namespace_id=$ns_id" "visibility=$VISIBLE" \
        "initialize_with_readme=false" "topics[]=$PROJECT" \
        "description=Miroir de référence : pipeline = machine à preuves (ADR-008).")"
      if [ "$code" != "201" ]; then
        echo "ÉCHEC création (HTTP $code) : $(deja_une_ligne)" >&2
        exit 1
      fi
      id="$(jq -r '.id' "$CORPS")"
      echo "   créé (id=$id, visibility=$VISIBLE, vide : initialize_with_readme=false)."
    fi
    echo "   id=$id  url=$(jq -r '.web_url' "$CORPS" | masquer)"
    echo "   namespace réel du projet : $(jq -r '.namespace.full_path // "?"' "$CORPS") (doit être « $NAMESPACE », sinon le push cible un autre chemin)"
    echo "   ATTENTION : un projet créé avec « initialize_with_readme=true » n'est PLUS vide →"
    echo "   le push --mirror est refusé ou force. C'est le piège n°1 de la bascule."

    echo "== 2. variables CI (masquées + protégées), seulement si présentes dans l'environnement"
    for k in RENDER_API_KEY RENDER_API_SERVICE_ID PERF_JWT_TOKEN; do
      if [ -n "${!k:-}" ]; then
        code="$(requette POST "/projects/$id/variables" \
          "key=$k" "value=${!k}" "masked=true" "protected=true")"
        if [ "$code" = "201" ]; then
          echo "   $k : créée (valeur jamais imprimée)."
        elif [ "$code" = "400" ]; then
          echo "   $k : déjà présente ou refusée (HTTP 400) — vérifier dans Settings → CI/CD → Variables."
        else
          echo "   $k : ÉCHEC HTTP $code : $(deja_une_ligne)" >&2
        fi
      else
        echo "   $k : absente de l'environnement → rien n'est créé. À saisir dans l'interface"
        echo "        (Settings → CI/CD → Variables, masquée + protégée), sinon deploy:render rougit."
      fi
    done

    echo "== 3. modèle de description de MR/issue : versionné dans le dépôt (.gitlab/), rien à faire ici"
    echo
    echo "Prochaine étape (historique) :"
    echo "  GITLAB_URL=\"https://oauth2:<TOKEN>@$HOTE_SANS_SCHEME/$CHEMIN.git\" scripts/gitlab-mirror.sh push"
    echo "Puis : scripts/gitlab-init-project.sh finalize"
    ;;

  finalize)
    exiger_jeton
    id="$(projet_id)" || { echo "projet $CHEMIN introuvable — lancer « prepare » puis le push d'abord." >&2; exit 1; }
    # `default_branch` est null tant que le dépôt n'a AUCUN commit : dans cet état, ni la
    # définition de la branche par défaut ni la protection de main ne peuvent aboutir.
    # On le DIT plutôt que de laisser partir deux appels condamnés.
    if [ "$(jq -r '.default_branch // "null"' "$CORPS")" = "null" ]; then
      echo "projet $CHEMIN existe mais il est VIDE (default_branch = null) : pousser l'historique d'abord —"
      echo "  GITLAB_URL=\"https://oauth2:<TOKEN>@$HOTE_SANS_SCHEME/$CHEMIN.git\" scripts/gitlab-mirror.sh push"
      echo "puis relancer « finalize ». Rien n'a été modifié."
      exit 2
    fi
    echo "== branche par défaut + portes de merge"
    code="$(requette PUT "/projects/$id" \
      "default_branch=main" "only_allow_merge_if_pipeline_succeeds=true" \
      "only_allow_merge_if_all_discussions_are_resolved=true" "issues_enabled=true" \
      "merge_requests_enabled=true" "wiki_enabled=false" "snippets_enabled=false")"
    [ "$code" = "200" ] || { echo "ÉCHEC réglages (HTTP $code) : $(deja_une_ligne)" >&2; exit 1; }
    echo "   HTTP $code : default_branch=$(jq -r .default_branch "$CORPS")"
    echo "                      « pipelines must succeed »=$(jq -r .only_allow_merge_if_pipeline_succeeds "$CORPS")"
    echo "                      « discussions résolues »=$(jq -r .only_allow_merge_if_all_discussions_are_resolved "$CORPS")"

    echo "== main protégée (pousser/fusionner = Maintainer, pas de force-push)"
    code="$(requette POST "/projects/$id/protected_branches" \
      "name=main" "push_access_level=40" "merge_access_level=40" \
      "unprotect_access_level=40" "allow_force_push=false")"
    case "$code" in
      201) echo "   protégée." ;;
      400) echo "   déjà protégée (HTTP 400) — relire dans Settings → Repository → Protected branches." ;;
      *)   echo "   ÉCHEC (HTTP $code) : $(deja_une_ligne)" >&2 ;;
    esac
    echo
    echo "Ce que le script NE fait PAS (Premium ou geste humain) :"
    echo "  - approbations par utilisateur / CODEOWNERS obligatoires (Premium) ;"
    echo "  - miroir GitHub → GitLab (Settings → Repository → Mirrored repositories, côté GitHub) ;"
    echo "  - la lecture de la première pipeline : pour ça, « statut »."
    ;;

  statut)
    exiger_jeton
    id="$(projet_id)" || { echo "projet $CHEMIN introuvable." >&2; exit 1; }
    code="$(requette GET "/projects/$id/pipelines?per_page=5")"
    [ "$code" = "200" ] || { echo "ÉCHEC lecture des pipelines (HTTP $code) : $(deja_une_ligne)" >&2; exit 1; }
    echo "== 5 dernières pipelines (ce que GitLab a réellement exécuté)"
    jq -r '.[] | "   #\(.id)  \(.sha[0:8])  \(.ref)  →  \(.status)  (source : \(.source))"' "$CORPS" | masquer
    pid="$(jq -r '.[0].id // empty' "$CORPS")"
    if [ -z "$pid" ]; then
      echo "   AUCUNE pipeline : le push n'a pas encore eu lieu, ou .gitlab-ci.yml n'est pas à la racine."
      exit 1
    fi
    code="$(requette GET "/projects/$id/pipelines/$pid/jobs?per_page=50")"
    [ "$code" = "200" ] || { echo "ÉCHEC lecture des jobs (HTTP $code)" >&2; exit 1; }
    echo "== jobs de la pipeline #$pid"
    jq -r '.[] | "   \(.status | ascii_upcase)\t\(.stage)\t\(.name)"' "$CORPS" | sort -k2 | masquer
    echo
    echo "À rendre bloquant après le premier vert observé : sbom:release et scan:image sont en"
    echo "allow_failure: true (dépendance réseau) — la condition est écrite dans .gitlab-ci.yml."
    ;;

  *)
    echo "action inconnue : $ACTION (attendu prepare | finalize | statut | dry-run)" >&2
    exit 2
    ;;
esac

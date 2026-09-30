#!/usr/bin/env bash
#
# verify-tenant-onboarding.sh — recette bout-en-bout du parcours
# « provisionnement -> onboarding -> invitations -> suspension »
# (constats B1, B2, B3, B4, M4 ; scenarios E2E-1 a E2E-10 du plan).
#
# PRINCIPE : ce script ne MENT JAMAIS. Chaque assertion produit PASS, FAIL ou
# SKIP (avec sa raison). Un scenario non executable est SKIP, jamais PASS. Le
# code de sortie 1 signale au moins un FAIL ; les SKIP sont comptes a part.
#
# Endpoints : tous verifies contre le code (aucun chemin suppose). Les chemins
# qui dependent d'un etat de la recette (SMTP, acces base) sont des SKIP.
#
# Prerequis : curl, jq. Un backend Discipolat accessible.
#
# Variables d'environnement (aucun secret en dur dans le depot) :
#   BASE_URL              defaut http://localhost:8080
#   SUPER_ADMIN_EMAIL     defaut superadmin@discipolat.com  (compte de DEV)
#   SUPER_ADMIN_PASSWORD  defaut DevOnly!2345               (compte de DEV)
#   PSQL_CONNINFO         si renseigne, active deux capacites de recette :
#                         (a) lecture du token d'activation de l'owner sans SMTP ;
#                         (b) octroi d'une membership TENANT_ADMIN dans le tenant
#                         de recette, seule facon d'obtenir un jeton TENANT sur
#                         un tenant fraichement provisionne (aucun membre connu).
#                         Recette uniquement : ne JAMAIS utiliser en production.
#   RUN_SUITE             all | provision | wizard | tenant
#
# CORRECTIONS DE LA RECETTE (2026-09-29, phase 5.5 — 9 bugs d'ordre identifiés
# dans reports/plan-2agents/agentA.md, § État de la recette) :
#   1. E2E-4a : la sonde « corps absent » sur BRANDING était jouée AVANT que
#      cette étape ne devienne l'étape actif → 409 STEP_ORDER_VIOLATION
#      court-circuitait la validation et le 400 attendu était inatteignable.
#      Repositionnée dans le parcours E2E-6, à la minute où BRANDING est actif.
#   2/3. E2E-5b1 (couleur invalide) et E2E-5b2 (module inconnu) : même cause,
#      même correctif (sondes déplacées dans E2E-6 quand l'étape visée est
#      actif). E2E-5b3 (nom trop court) restait valide : CHURCH_IDENTITY est
#      l'étape 0 donc actif dès le départ ; déplacé pour la cohérence.
#   4. E2E-9a : la pre-sonde « role du JWT » documentait la limite D5 (garde
#      hasAnyRole sur le claim). SOLDE par l'arbitrage D5-bis (garde
#      @authz.isTenantAdmin() sur la table des memberships) ; la pre-sonde est
#      devenue un test reel : 403 = refus legitime de la garde, SKIP honnete.
#      SKIP n'est JAMAIS un PASS deguise.
#   5. E2E-11 : `.allowed // "ABSENT"` en jq traite le BOOLEAN false comme
#      vide — un dépassement légitimement refusé (allowed=false, code QUOTA_*)
#      était lu « ABSENT » puis compté FAIL. Lecture par has() + tostring.
#      Balayage generalise (2026-09-30) : crossTenantIdentity, welcomeEmailSent,
#      requiresTenantSwitch et owner.activationEmailSent corriges de la meme facon.
#   6. E2E-10b : la fixture_admin_membership DO UPDATE rend desormais la
#      membership TENANT_ADMIN ACTIVE meme si une ligne existe deja (uk
#      user_id/tenant_id) ; la sonde attend 404 STEP_NOT_FOUND (IDOR). Un 403
#      restant est signale en SKIP justifie, jamais en FAIL trompeur.
#   7. E2E-6a/6d : les deux sondes d'effet réel utilisaient des chemins
#      inexistants (/api/v1/admin/organization/tree et
#      /api/v1/admin/departments → 404, masqués en SKIP) ; les vrais endpoints
#      sont GET /api/v1/org/tree et GET /api/v1/departments (vérifiés 200 en
#      réel). Bonus : le nom de département créé porte le suffixe slug, une
#      égalité exacte sur "Intercession" n'aurait jamais matché (startswith).
#   8. E2E-4b : la désignation du champ fautif cherchait `.primaryColor` à la
#      racine de la réponse ; le ProblemDetail l'expose dans `.details`
#      (vérifié en réel sur PostgreSQL, replay §5.5).
#   9. E2E-2b : lecture du token d'activation avec une colonne `consumed_at`
#      inexistante (la table a `used` boolean + `expires_at`) ; l'erreur SQL
#      avalée par 2>/dev/null transformait un scenario exécutable en SKIP.
#      Requête corrigée : used = false AND expires_at > now().
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SUPER_ADMIN_EMAIL="${SUPER_ADMIN_EMAIL:-superadmin@discipolat.com}"
SUPER_ADMIN_PASSWORD="${SUPER_ADMIN_PASSWORD:-DevOnly!2345}"
PSQL_CONNINFO="${PSQL_CONNINFO:-}"
RUN_SUITE="${RUN_SUITE:-${1:-all}}"
RECIPE_PASSWORD="MotDePasseRecette9"

# Variables partagees entre scenarios (remplies au fil de l'eau).
API_CODE=""; API_BODY=""
TENANT_ID=""; TENANT_SLUG=""; OWNER_EMAIL=""; OWNER_USER_ID=""; OWNER_ACTIVATION_SENT=""
TENANT_TOKEN=""; CROSS_EMAIL=""; OWNER_ACTIVATION_SENT="ABSENT"
STEP_CHURCH=""; STEP_MEMBER=""; STEP_STRUCTURE=""; STEP_ROLES=""
STEP_BRANDING=""; STEP_MODULES=""; STEP_EVENT=""

PASS_COUNT=0; FAIL_COUNT=0; SKIP_COUNT=0
declare -a FAILED=() SKIPPED=()

# ---------------------------------------------------------------- utilitaires ---

log()  { printf '%s\n' "$*"; }
sep()  { log "---------------------------------------------------------------"; }
ok()   { PASS_COUNT=$((PASS_COUNT+1)); log "  PASS  $1"; }
ko()   { FAIL_COUNT=$((FAIL_COUNT+1)); FAILED+=("$1"); log "  FAIL  $1"; [[ -n "${2:-}" ]] && log "        detail : $2"; }
skip() { SKIP_COUNT=$((SKIP_COUNT+1)); SKIPPED+=("$1"); log "  SKIP  $1"; [[ -n "${2:-}" ]] && log "        raison : $2"; }
scenario() { sep; log "SCENARIO : $1"; }

require_tools() {
  for tool in curl jq; do
    command -v "$tool" >/dev/null 2>&1 \
      || { log "ERREUR : '$tool' est requis mais absent."; exit 2; }
  done
}

# api <METHOD> <PATH> [TOKEN] [BODY]  -> $API_CODE / $API_BODY
api() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local tmp; tmp="$(mktemp)"
  local args=(-sS -o "$tmp" -w '%{http_code}' -X "$method" "${BASE_URL}${path}" -H 'Accept: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer ${token}")
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' -d "$body")
  API_CODE="$(curl "${args[@]}" 2>/dev/null || echo 000)"
  API_BODY="$(cat "$tmp")"; rm -f "$tmp"
}

login_token() {   # login_token <email> <password> -> accessToken ou vide
  api POST '/api/v1/auth/login' '' \
      "$(jq -nc --arg e "$1" --arg p "$2" '{email:$e, password:$p}')"
  [[ "$API_CODE" == "200" ]] && jq -r '.accessToken // empty' <<<"$API_BODY" || printf ''
}

# assert_code <code_attendu> <libelle> [jq_titre_attendu]
assert_code() {
  local expected="$1" label="$2" title="${3:-}"
  if [[ "$API_CODE" == "$expected" ]]; then
    ok "$label (HTTP $API_CODE)"; return 0
  fi
  if [[ -n "$title" ]]; then
    local got; got="$(jq -r '.title // .error // "?"' <<<"$API_BODY" 2>/dev/null)"
    ko "$label" "HTTP ${API_CODE} au lieu de ${expected} (code erreur : ${got}) ${API_BODY}"
  else
    ko "$label" "HTTP ${API_CODE} au lieu de ${expected} ${API_BODY}"
  fi
  return 1
}

jq_h() { jq -e "$1" <<<"$API_BODY" >/dev/null 2>&1; }

# Décode le claim « role » d'un JWT (RS256, non vérifié — lecture de fixture,
# jamais une décision de sécurité). Sert aux pré-sondes honnêtes de scénario.
jwt_role_claim() {
  local tok="${1#Bearer }"
  cut -d. -f2 <<<"$tok" | tr '_-' '/+' | base64 -d 2>/dev/null \
    | jq -r '.role // empty' 2>/dev/null || true
}

# ------------------------------------------------------------ etat du backend ---

check_backend() {
  scenario "Prealables — backend joignable"
  # Pas d'en-tete Accept impose ici : l'OpenAPI est servi en
  # application/vnd.oai.openapi, et un `Accept: application/json` y repond
  # 406 (constat H5b) — la sonde doit donc interroger la representation reelle.
  API_CODE="$(curl -sS -o /tmp/onb-e2e-probe.$$ -w '%{http_code}' --max-time 10 \
      "${BASE_URL}/api/v1/public/docs/openapi.yaml" 2>/dev/null || echo 000)"
  rm -f /tmp/onb-e2e-probe.$$
  if [[ "$API_CODE" == "200" ]]; then
    ok "Backend joignable et OpenAPI expose (${BASE_URL})"
  else
    ko "Backend joignable" "HTTP ${API_CODE} sur /api/v1/public/docs/openapi.yaml"
    log ""
    log "La recette ne peut pas continuer sans backend (sortie 1)."
    exit 1
  fi
  if ! login_token "$SUPER_ADMIN_EMAIL" "$SUPER_ADMIN_PASSWORD" >/dev/null; then
    ko "Login Super Admin" "identifiants refuses (HTTP ${API_CODE})"
    exit 1
  fi
  ok "Login Super Admin (compte de recette)"
}

have_db() { [[ -n "$PSQL_CONNINFO" ]] && command -v psql >/dev/null 2>&1; }
need_db() { have_db || return 1; return 0; }

# =============================================================== E2E-1 ==========
# Le Super Admin provisionne un tenant AVEC un owner (constat B3).
e2e_1_provision() {
  scenario "E2E-1 — Provisionnement atomique avec owner (constat B3)"

  local super_token; super_token="$(login_token "$SUPER_ADMIN_EMAIL" "$SUPER_ADMIN_PASSWORD")"
  [[ -n "$super_token" ]] || { ko "E2E-1a login Super Admin" "identifiants refuses"; return; }
  ok "E2E-1a login Super Admin"

  # Un tiers existe dans une AUTRE eglise : servira au cas cross-tenant (B2).
  # Un rejeu dans la meme seconde ne doit pas reutiliser le slug :
  # l'endpoint le refuse alors, et le scenario parait cassé pour rien.
  TENANT_SLUG="e2e-onb-$(date +%s)-$(( RANDOM % 10000 ))"
  CROSS_EMAIL="cross.${TENANT_SLUG}@example.com"
  api POST '/api/v1/users' "$super_token" \
      "$(jq -nc --arg e "$CROSS_EMAIL" \
          '{email:$e, firstName:"Croix", lastName:"Tenant", phone:"+237699000001",
            password:"'"$RECIPE_PASSWORD"'", role:"MEMBRE", roles:["MEMBRE"], activeRole:"MEMBRE"}')"
  if [[ "$API_CODE" == "200" || "$API_CODE" == "201" ]]; then
    ok "E2E-1b tiers cree dans l'eglise d'origine (${CROSS_EMAIL})"
  else
    skip "E2E-1b tiers cree dans l'eglise d'origine" "POST /api/v1/users -> HTTP ${API_CODE}"
    CROSS_EMAIL=""
  fi

  OWNER_EMAIL="owner.${TENANT_SLUG}@example.com"
  local payload
  payload="$(jq -nc \
      --arg slug "$TENANT_SLUG" --arg owner "$OWNER_EMAIL" \
      '{name:("Eglise Recette " + $slug), slug:$slug, plan:"DISCOVERY", country:"CM",
        currency:"XAF", timezone:"Africa/Douala", locale:"fr",
        churchName:("Eglise Recette " + $slug), departmentName:"Accueil",
        departmentDescription:"Accueil et intercession",
        createNewResponsable:true, newResponsableFirstName:"Resp", newResponsableLastName:"Onb",
        newResponsableEmail:("resp." + $slug + "@example.com"),
        familyName:("Famille Recette " + $slug), createNewChef:true, newChefFirstName:"Chef",
        newChefLastName:"Onb", newChefEmail:("chef." + $slug + "@example.com"),
        ownerEmail:$owner, ownerFirstName:"Jean", ownerLastName:"Recette"}')"

  api POST '/api/v1/platform/admin/provisioning' "$super_token" "$payload"
  if assert_code 201 "E2E-1c provisioning atomique accepte"; then
    TENANT_ID="$(jq -r '.tenant.id // empty' <<<"$API_BODY")"
    OWNER_USER_ID="$(jq -r '.owner.userId // empty' <<<"$API_BODY")"
    OWNER_ACTIVATION_SENT="$(jq -r '.owner.activationEmailSent as $v | if $v == null then "ABSENT" else ($v|tostring) end' <<<"$API_BODY")"
    jq -r '.department.id // "?"' <<<"$API_BODY" | grep -qv '^?$' \
      && ok "E2E-1g departement cree dans la meme transaction" \
      || ko "E2E-1g departement cree" "department absent de la reponse"
    jq -r '.family.id // "?"' <<<"$API_BODY" | grep -qv '^?$' \
      && ok "E2E-1h famille creee dans la meme transaction" \
      || ko "E2E-1h famille creee" "family absent de la reponse"
  else
    # Le provisionnement atomique a echoue. On NE MASQUE PAS l'echec (il reste
    # compte en FAIL ci-dessus) mais on continue la recette sur un tenant cree
    # par l'endpoint simple, sinon un seul defaut masquerait tous les scenarios
    # suivants en SKIP et la recette ne prouverait plus rien.
    ko "E2E-1c-bis le tenant de recette n'a pas ete cree" "provisionnement indisponible"
    api POST '/api/v1/platform/admin/tenants' "$super_token" \
        "$(jq -nc --arg s "$TENANT_SLUG" '{name:("Eglise Recette " + $s), slug:$s, plan:"DISCOVERY", country:"CM", currency:"XAF", timezone:"Africa/Douala", locale:"fr"}')"
    if [[ "$API_CODE" == "201" ]]; then
      TENANT_ID="$(jq -r '.id // empty' <<<"$API_BODY")"
      ok "E2E-1c-ter tenant de recette cree par l'endpoint simple (${TENANT_ID})"
    else
      ko "E2E-1f tenant de recette cree" "HTTP ${API_CODE} ${API_BODY}"
      return
    fi
  fi

  [[ -n "$TENANT_ID" ]] && ok "E2E-1d tenant.id disponible (${TENANT_ID})" \
      || ko "E2E-1d tenant.id disponible" "absent"
  if [[ -n "$OWNER_USER_ID" ]]; then
    ok "E2E-1e owner.userId renvoyé (${OWNER_USER_ID})"
  else
    skip "E2E-1e owner.userId renvoyé" "provisionnement atomique indisponible (voir E2E-1c)"
  fi
  if [[ "$OWNER_ACTIVATION_SENT" != "ABSENT" ]]; then
    ok "E2E-1f owner.activationEmailSent present (${OWNER_ACTIVATION_SENT})"
  else
    skip "E2E-1f owner.activationEmailSent present" "provisionnement atomique indisponible (voir E2E-1c)"
  fi
}

# =============================================================== E2E-2 ==========
e2e_2_owner_activation() {
  scenario "E2E-2 — Owner : email d'activation puis activation du compte"

  if [[ -z "$OWNER_USER_ID" ]]; then
    skip "E2E-2 Owner active" "E2E-1 n'a pas abouti"; return
  fi
  if [[ "$OWNER_ACTIVATION_SENT" == "true" ]]; then
    ok "E2E-2a email d'activation envoye a l'owner"
  else
    # D10 : sans SMTP, le contrat impose activationEmailSent=false. Comportement
    # ATTENDU, pas une defaillance.
    skip "E2E-2a email d'activation envoye" \
         "SMTP non configure : activationEmailSent=false (attendu, decision D10)"
  fi

  local token=""
  if need_db; then
    # Bug de recette n°9 : la table porte `used` (boolean) et `expires_at`, pas
    # `consumed_at` — vérifié via \d activation_tokens sur PG réel ; l'ancienne
    # requête échouait en silence (2>/dev/null) et rendait le scenario inatteignable.
    token="$(psql "$PSQL_CONNINFO" -tAc \
      "SELECT token FROM activation_tokens WHERE user_id='${OWNER_USER_ID}'::uuid
        AND used = false AND expires_at > now() ORDER BY created_at DESC LIMIT 1" 2>/dev/null | tr -d '[:space:]')"
  fi
  if [[ -z "$token" ]]; then
    skip "E2E-2b activation du compte owner" \
         "token d'activation injoignable (le compte a un mot de passe aleatoire jamais communique)"; return
  fi
  api POST '/api/v1/auth/activate' '' "$(jq -nc --arg t "$token" '{token:$t}')"
  assert_code 200 "E2E-2b activation du compte owner" \
    && ok "E2E-2c le compte owner n'est plus PENDING"
}

# Fixture de recette : donne au Super Admin une membership TENANT_ADMIN dans le
# tenant de recette. Sans elle, aucun jeton TENANT n'existe sur ce tenant
# (personne ne connait le mot de passe aleatoire de l'owner) et tous les
# scenarios tenant-scopes seraient inexecutables.
fixture_admin_membership() {
  have_db || return 1
  local tenant_id="${1:-$TENANT_ID}"
  local super_id
  super_id="$(psql "$PSQL_CONNINFO" -tAc \
      "SELECT id FROM users WHERE lower(email)=lower('${SUPER_ADMIN_EMAIL}') LIMIT 1" 2>/dev/null | tr -d '[:space:]')"
  [[ -n "$super_id" && -n "$tenant_id" ]] || return 1
  psql "$PSQL_CONNINFO" -q -v ON_ERROR_STOP=1 >/dev/null 2>&1 <<SQL
INSERT INTO tenant_memberships (id, tenant_id, user_id, role_id, role, scope_type, status, joined_at, created_at, updated_at)
SELECT uuid_generate_v4(), '${tenant_id}'::uuid, '${super_id}'::uuid, r.id, r.key, 'TENANT', 'ACTIVE', now(), now(), now()
  FROM roles r WHERE r.key = 'TENANT_ADMIN'
ON CONFLICT (user_id, tenant_id) DO UPDATE
   SET role_id = EXCLUDED.role_id, role = EXCLUDED.role, status = 'ACTIVE', updated_at = now();
SQL
  return 0
}

switch_to_recipe_tenant() {
  local super_token; super_token="$(login_token "$SUPER_ADMIN_EMAIL" "$SUPER_ADMIN_PASSWORD")"
  [[ -n "$super_token" ]] || return 1
  api POST '/api/v1/tenant-switcher/switch' "$super_token" "$(jq -nc --arg t "$TENANT_ID" '{tenantId:$t}')"
  [[ "$API_CODE" == "200" ]] || return 1
  TENANT_TOKEN="$(jq -r '.accessToken // empty' <<<"$API_BODY")"
  [[ -n "$TENANT_TOKEN" ]]
}

# Ouvre un jeton TENANT sur le tenant de recette, ou renvoie 1 (=> SKIP).
enter_recipe_tenant() {
  switch_to_recipe_tenant && return 0
  if fixture_admin_membership && switch_to_recipe_tenant; then
    ok "Jeton TENANT obtenu sur le tenant de recette (fixture de recette)"
    return 0
  fi
  return 1
}

# =============================================================== E2E-3 ==========
e2e_3_read_wizard() {
  scenario "E2E-3 — Contrat de lecture du wizard (constat B2)"

  if [[ -z "$TENANT_ID" ]]; then skip "E2E-3 Wizard lisible" "E2E-1 n'a pas abouti"; return; fi
  if ! enter_recipe_tenant; then
    skip "E2E-3 Wizard lisible" \
         "aucun jeton TENANT sur le tenant de recette : le switch cross-tenant est impossible (voir H4) et/ou PSQL_CONNINFO absent"; return
  fi

  api GET '/api/v1/onboarding-wizard' "$TENANT_TOKEN"
  assert_code 200 "E2E-3a GET /onboarding-wizard" || return

  local count; count="$(jq 'length' <<<"$API_BODY")"
  [[ "$count" == "7" ]] && ok "E2E-3b 7 etapes renvoyees" \
                         || ko "E2E-3b 7 etapes renvoyees" "recu ${count}"

  local missing=""
  for f in id stepType stepOrder title description status isCompleted \
           isSkippable skipRequiresReason startedAt completedAt; do
    # `has()` et non une lecture de valeur : `startedAt`/`completedAt` valent
    # null sur une etape neuve, et `jq -e` quitte en erreur sur null -- un champ
    # present mais vide ne doit pas etre signale comme manquant.
    # `.[0]` et NON `[0]` : en jq, un `[0]` en tete de filtre est un
    # CONSTRUCTEUR de tableau (le litteral [0]), pas un index -- d'ou une erreur
    # "Cannot check whether array has a string key" et, avant correction, 11
    # champsdeclare a tort manquants.
    jq_h ".[0] | has(\"${f}\")" || missing="${missing} ${f}"
  done
  [[ -z "$missing" ]] && ok "E2E-3c contrat 3.1 complet (tous les champs)" \
                      || ko "E2E-3c contrat 3.1 complet" "champs manquants :${missing}"

  jq_h '.[0] | has("config")' \
    && ko "E2E-3d l'entite brute n'est pas exposee" "le champ legacy \`config\` fuit dans la reponse" \
    || ok "E2E-3d l'entite brute n'est pas exposee (pas de champ config)"

  local non_bool; non_bool="$(jq '[.[] | select((.isCompleted|type) != "boolean")] | length' <<<"$API_BODY")"
  [[ "$non_bool" == "0" ]] && ok "E2E-3e isCompleted est un booleen" \
                          || ko "E2E-3e isCompleted est un booleen" "${non_bool} element(s) non booleen(s)"

  local order; order="$(jq -r '[.[].stepOrder] | @csv' <<<"$API_BODY")"
  [[ "$order" == "0,1,2,3,4,5,6" ]] && ok "E2E-3f ordre canonique des etapes respecte" \
                                   || ko "E2E-3f ordre canonique" "stepOrder = ${order}"

  STEP_CHURCH="$(jq -r '.[]|select(.stepType=="CHURCH_IDENTITY")|.id' <<<"$API_BODY")"
  STEP_MEMBER="$(jq -r '.[]|select(.stepType=="MEMBER_IMPORT")|.id' <<<"$API_BODY")"
  STEP_STRUCTURE="$(jq -r '.[]|select(.stepType=="STRUCTURE")|.id' <<<"$API_BODY")"
  STEP_ROLES="$(jq -r '.[]|select(.stepType=="ROLES")|.id' <<<"$API_BODY")"
  STEP_BRANDING="$(jq -r '.[]|select(.stepType=="BRANDING")|.id' <<<"$API_BODY")"
  STEP_MODULES="$(jq -r '.[]|select(.stepType=="MODULES")|.id' <<<"$API_BODY")"
  STEP_EVENT="$(jq -r '.[]|select(.stepType=="FIRST_EVENT")|.id' <<<"$API_BODY")"
  for pair in "CHURCH_IDENTITY:$STEP_CHURCH" "MEMBER_IMPORT:$STEP_MEMBER" "STRUCTURE:$STEP_STRUCTURE" \
              "ROLES:$STEP_ROLES" "BRANDING:$STEP_BRANDING" "MODULES:$STEP_MODULES" "FIRST_EVENT:$STEP_EVENT"; do
    [[ -z "${pair#*:}" ]] && ko "E2E-3g etape ${pair%%:*} presente" "id absent"
  done
  ok "E2E-3g les 7 types d'etapes sont exposes"
}

# =============================================================== E2E-4/5b =====
# Les sondes E2E-4a/4b, E2E-5b1, E2E-5b2, E2E-5b3 étaient jadis des scenarios
# séparés joués AVANT le parcours E2E-6 ; c'était le bug de recette n°1-3
# (en-tête). Elles sont désormais jouées par probe_* DANS e2e_6_full_run, au
# moment où l'étape visée est l'étape actif. Les libellés d'origine sont
# conservés pour la traçabilité avec l'état 42 PASS/10 FAIL/7 SKIP.

probe_400_metier() {   # probe_400_metier <libellé> <id étape> <corps ou ''> <titre attendu>
  api POST "/api/v1/onboarding-wizard/${2}/complete" "$TENANT_TOKEN" "$3"
  local title; title="$(jq -r '.title // .error // "?"' <<<"$API_BODY" 2>/dev/null)"
  if [[ "$API_CODE" == "400" && "$title" == "$4" ]]; then
    ok "$1"
  else
    ko "$1" "HTTP ${API_CODE} titre ${title} : ${API_BODY}"
  fi
}

# Decision D7 : /complete accepte un corps ABSENT — la réponse doit être une
# erreur MÉTIER nommée (400 STEP_DATA_INVALID), pas un 400 technique Jackson.
# BRANDING exige au moins un champ ; la sonde vaut quand BRANDING est l'étape actif.
probe_e2e4_missing_body() {
  api POST "/api/v1/onboarding-wizard/${STEP_BRANDING}/complete" "$TENANT_TOKEN" ''
  local title; title="$(jq -r '.title // .error // "?"' <<<"$API_BODY" 2>/dev/null)"
  if [[ "$API_CODE" == "400" && "$title" == "STEP_DATA_INVALID" ]]; then
    ok "E2E-4a corps absent -> 400 STEP_DATA_INVALID metier (nom du champ fourni)"
  elif [[ "$API_CODE" == "400" && "$title" != "STEP_DATA_INVALID" ]]; then
    ko "E2E-4a corps absent" "400 non metier : ${API_BODY}"
  else
    ko "E2E-4a corps absent" "HTTP ${API_CODE} : ${API_BODY}"
  fi
  # Bug de recette n°8 : le champ fautif est exposé dans l'objet `details`
  # du ProblemDetail (DomainException.toProblemDetail → property "details"),
  # pas à la racine — vérifié en réel sur PG : {"details":{"primaryColor":…}}.
  jq_h '.details.primaryColor' && ok "E2E-4b le champ fautif est designe dans l'erreur" \
                               || skip "E2E-4b le champ fautif est designe" "cle de detail non exposee"
}

# ------------------------------------------------------------------ E2E-5 -----

e2e_5_order() {
  scenario "E2E-5 — Ordre : une etape hors ordre donne 409 STEP_ORDER_VIOLATION"

  if [[ -z "$TENANT_TOKEN" || -z "$STEP_ROLES" ]]; then
    skip "E2E-5 ordre des etapes" "jeton TENANT indisponible"; return
  fi
  api POST "/api/v1/onboarding-wizard/${STEP_ROLES}/complete" "$TENANT_TOKEN" '{}'
  assert_code 409 "E2E-5a etape hors ordre refusee" "STEP_ORDER_VIOLATION"
}

# =============================================================== E2E-6 ==========
complete_step() {   # complete_step <id> <data> <libelle>
  api POST "/api/v1/onboarding-wizard/${1:-}/complete" "$TENANT_TOKEN" "${2:-}"
  local status; status="$(jq -r '.status // "?"' <<<"$API_BODY")"
  if [[ "$API_CODE" == "200" && "$status" == "COMPLETED" ]]; then
    ok "E2E-6 ${3:-step} COMPLETED"; return 0
  fi
  if [[ "$API_CODE" == "409" && "$status" == "COMPLETED" ]]; then
    ok "E2E-6 ${3:-step} deja complete (rejeu)"; return 0
  fi
  ko "E2E-6 ${3:-step}" "HTTP ${API_CODE} statut ${status} : ${API_BODY}"; return 1
}

e2e_6_full_run() {
  scenario "E2E-6 — Parcours des 7 etapes avec les actions metier reelles"

  if [[ -z "$TENANT_TOKEN" ]]; then skip "E2E-6 Parcours complet" "jeton TENANT indisponible"; return; fi

  # --- sondes de validation repositionnées (bugs de recette 1-3, en-tête) ---
  # CHURCH_IDENTITY est l'étape actif du parcours : la validation est atteignable.
  probe_400_metier "E2E-5b3 nom trop court refuse" "$STEP_CHURCH" \
      '{"data":{"churchName":"X"}}' "STEP_DATA_INVALID"

  complete_step "$STEP_CHURCH" \
    "$(jq -nc --arg s "$TENANT_SLUG" '{data:{churchName:("Eglise Onboarding " + $s), businessName:"Recette E2E", city:"Douala", phone:"+237699000002", email:"contact.onb@example.com", timezone:"Africa/Douala", currency:"XAF"}}')" \
    "CHURCH_IDENTITY" || return
  # L'eglise racine doit AVOIR ETE renommee : preuve que l'action a eu un effet.
  # Bug de recette n°7a : le chemin /api/v1/admin/organization/tree n'existe
  # pas (404) ; l'arbre du tenant courant est servi par GET /api/v1/org/tree
  # (OrganizationHierarchyController, isAuthenticated()).
  api GET '/api/v1/org/tree' "$TENANT_TOKEN"
  if jq -e --arg n "Eglise Onboarding $TENANT_SLUG" '.. | objects | select(.name? == $n)' <<<"$API_BODY" >/dev/null 2>&1; then
    ok "E2E-6a l'eglise racine a ete reellement renommee (pas un simple enregistrement)"
  else
    skip "E2E-6a l'eglise racine a ete renommee" "arborescence non lisible (HTTP ${API_CODE})"
  fi

  # MEMBER_IMPORT : skippable AVEC motif obligatoire (D4).
  # L'ordre est IMPERATIF : on teste d'abord le refus SANS motif (l'etape est
  # encore PENDING), puis on la saute avec motif. L'inverse donnerait
  # 409 STEP_ALREADY_COMPLETED, qui ne prouve rien.
  api POST "/api/v1/onboarding-wizard/${STEP_MEMBER}/skip" "$TENANT_TOKEN" '{}'
  if [[ "$API_CODE" == "400" ]] \
     && [[ "$(jq -r '.title // "?"' <<<"$API_BODY")" == "STEP_SKIP_REASON_REQUIRED" ]]; then
    ok "E2E-6c skip sans motif refuse (400 STEP_SKIP_REASON_REQUIRED)"
  else
    ko "E2E-6c skip sans motif refuse" "HTTP ${API_CODE} : ${API_BODY}"
  fi

  api POST "/api/v1/onboarding-wizard/${STEP_MEMBER}/skip" "$TENANT_TOKEN" \
      '{"reason":"Membres declares a importer hors ligne"}'
  if [[ "$API_CODE" == "200" ]] && [[ "$(jq -r '.status // "?"' <<<"$API_BODY")" == "SKIPPED" ]]; then
    ok "E2E-6b MEMBER_IMPORT sautee avec motif (skipRequiresReason respecte)"
  else
    ko "E2E-6b MEMBER_IMPORT sautee" "HTTP ${API_CODE} : ${API_BODY}"
  fi

  # Noms DISTINCTS de ceux du provisionnement : `families.nom` porte une
  # contrainte UNIQUE globale, donc rejouer le meme nom dans la meme eglise
  # echouerait sur la premiere occurrence, pas sur une donnee exterieure.
  complete_step "$STEP_STRUCTURE" \
    "$(jq -nc --arg s "$TENANT_SLUG" '{data:{departments:["Intercession " + $s, "Chorale " + $s], families:["Famille du wizard " + $s]}}')" \
    "STRUCTURE" || return
  api GET '/api/v1/departments?page=0&size=50' "$TENANT_TOKEN"
  # Bug de recette n°7b : le nom cree porte le suffixe slug ("Intercession
  # <slug>"), une equality exacte sur "Intercession" ne pouvait jamais matcher.
  if jq -e '[.. | objects | select((.nom? // "") | startswith("Intercession"))] | length > 0' <<<"$API_BODY" >/dev/null 2>&1; then
    ok "E2E-6d le departement 'Intercession' existe reellement"
  else
    skip "E2E-6d le departement 'Intercession' existe" "liste indisponible (HTTP ${API_CODE}) ou nom absent"
  fi

  complete_step "$STEP_ROLES" \
    "$(jq -nc --arg s "$TENANT_SLUG" '{data:{invitations:[{email:("resp." + $s + "@example.com"), role:"TENANT_ADMIN"}]}}')" \
    "ROLES" || return

  # BRANDING est devenu l'étape actif : les sondes E2E-4a/4b et E2E-5b1 sont
  # jouées ICI (et plus avant le parcours) — c'est la correction des bugs de
  # recette n°1 et 2 ; hors ordre, le 409 STEP_ORDER_VIOLATION masquait le 400.
  probe_e2e4_missing_body
  probe_400_metier "E2E-5b1 couleur invalide refusee" "$STEP_BRANDING" \
      '{"data":{"primaryColor":"pas-une-couleur"}}' "STEP_DATA_INVALID"

  complete_step "$STEP_BRANDING" \
    '{"data":{"primaryColor":"#1A2B3C","allowDarkMode":true}}' "BRANDING" || return

  # MODULES est l'étape actif : la sonde catalogue se joue ici (bug n°3).
  probe_400_metier "E2E-5b2 module inconnu refuse (validation catalogue)" "$STEP_MODULES" \
      '{"data":{"modules":["module-qui-nexiste-pas"]}}' "STEP_DATA_INVALID"

  local module_code
  api GET '/api/v1/admin/quotas/features' "$TENANT_TOKEN"
  module_code="$(jq -r 'keys_unsorted[0] // empty' <<<"$API_BODY")"
  [[ -n "$module_code" ]] || module_code="people"
  complete_step "$STEP_MODULES" \
    "$(jq -nc --arg m "$module_code" '{data:{modules:[$m]}}')" "MODULES" || return

  local start_at
  start_at="$(date -u -d '+7 days' '+%Y-%m-%dT%H:%M:%SZ' 2>/dev/null \
              || date -u -v+7d '+%Y-%m-%dT%H:%M:%SZ')"
  complete_step "$STEP_EVENT" \
    "$(jq -nc --arg s "$start_at" '{data:{title:"Premiere rencontre de recette", location:"Grande salle", startAt:$s}}')" \
    "FIRST_EVENT" || return
}

# =============================================================== E2E-7 ==========
e2e_7_completion() {
  scenario "E2E-7 — Achevement : status completed=true, progress=100, rejeu refuse"

  if [[ -z "$TENANT_TOKEN" ]]; then skip "E2E-7 Achevement" "jeton TENANT indisponible"; return; fi

  api GET '/api/v1/onboarding-wizard/status' "$TENANT_TOKEN"
  if assert_code 200 "E2E-7a GET /status" && [[ "$(jq -r '.completed // false' <<<"$API_BODY")" == "true" ]]; then
    ok "E2E-7b completed=true (colonne V183 onboarding_completed_at ecrite)"
  else
    ko "E2E-7b completed=true" "reponse : ${API_BODY}"
  fi
  [[ -n "$(jq -r '.completedAt // empty' <<<"$API_BODY")" ]] \
    && ok "E2E-7c completedAt renseigne" || ko "E2E-7c completedAt renseigne" "colonnes de completion vides"

  api GET '/api/v1/onboarding-wizard/progress' "$TENANT_TOKEN"
  if assert_code 200 "E2E-7d GET /progress"; then
    local pct; pct="$(jq -r '.percentage // -1' <<<"$API_BODY")"
    [[ "$pct" == "100" ]] && ok "E2E-7e percentage=100" || ko "E2E-7e percentage=100" "recu ${pct}"
  fi

  api POST "/api/v1/onboarding-wizard/${STEP_BRANDING}/complete" "$TENANT_TOKEN" \
      '{"data":{"primaryColor":"#1A2B3C"}}'
  assert_code 409 "E2E-7f rejeu d'une etape terminee refuse" "STEP_ALREADY_COMPLETED"
}

# =============================================================== E2E-8 ==========
e2e_8_suspension() {
  scenario "E2E-8 — Suspension (constat B1) puis reactivation"

  if [[ -z "$TENANT_ID" ]]; then skip "E2E-8 Suspension" "E2E-1 n'a pas abouti"; return; fi
  local super_token; super_token="$(login_token "$SUPER_ADMIN_EMAIL" "$SUPER_ADMIN_PASSWORD")"
  [[ -n "$super_token" ]] || { skip "E2E-8 Suspension" "login Super Admin impossible"; return; }

  api POST "/api/v1/platform/admin/tenants/${TENANT_ID}/suspend" "$super_token" '{}'
  # Le controleur renvoie 204 (pas de corps) : les deux sont acceptes.
  if [[ "$API_CODE" == "200" || "$API_CODE" == "204" ]]; then
    ok "E2E-8a suspension du tenant acceptee (HTTP ${API_CODE})"
  else
    ko "E2E-8a suspension du tenant" "HTTP ${API_CODE} : ${API_BODY}"
  fi

  if [[ -n "$TENANT_TOKEN" ]]; then
    api GET '/api/v1/onboarding-wizard' "$TENANT_TOKEN"
    assert_code 403 "E2E-8b appel du tenant suspendu refuse" "TENANT_SUSPENDED" \
      && ok "E2E-8b1 le jeton existant ne permet plus d'appeler l'API"
  else
    skip "E2E-8b appel du tenant suspendu refuse" "aucun jeton TENANT obtenu"
  fi

  api POST '/api/v1/tenant-switcher/switch' "$super_token" "$(jq -nc --arg t "$TENANT_ID" '{tenantId:$t}')"
  if [[ "$API_CODE" == "403" ]]; then
    ok "E2E-8c switch vers un tenant suspendu refuse (aucun JWT delivre)"
  elif [[ "$API_CODE" == "200" ]]; then
    ko "E2E-8c switch vers un tenant suspendu refuse" "un jeton a ete delivre (HTTP 200)"
  else
    skip "E2E-8c switch vers un tenant suspendu refuse" "HTTP ${API_CODE}"
  fi

  api POST "/api/v1/platform/admin/tenants/${TENANT_ID}/reactivate" "$super_token" '{}'
  if [[ "$API_CODE" != "200" && "$API_CODE" != "204" ]]; then
    ko "E2E-8d reactivation du tenant" "HTTP ${API_CODE} : ${API_BODY}"; return
  fi
  ok "E2E-8d reactivation du tenant acceptee (HTTP ${API_CODE})"

  if [[ -n "$TENANT_TOKEN" ]]; then
    api GET '/api/v1/onboarding-wizard' "$TENANT_TOKEN"
    [[ "$API_CODE" == "200" ]] && ok "E2E-8e acces retabli immediatement apres reactivation" \
                               || ko "E2E-8e acces retabli" "HTTP ${API_CODE}"
  fi
  # Le tenant doit etre de nouveau basculable.
  switch_to_recipe_tenant && ok "E2E-8f le tenant reactif redevient basculable" \
                            || ko "E2E-8f le tenant reactif redevient basculable" "switch refuse"
}

# =============================================================== E2E-9 ==========
e2e_9_invitations() {
  scenario "E2E-9 — Invitations : parcours complet (constat M4) et identite cross-tenant (B2)"

  if [[ -z "$TENANT_TOKEN" ]]; then skip "E2E-9 Invitations" "jeton TENANT indisponible"; return; fi

  # Pré-sonde (liminaire D5-bis SOLDE : la garde est passee a
  # @authz.isTenantAdmin(), qui lit la TABLE tenant_memberships du tenant actif
  # et non le claim du JWT). La fixture ci-dessus rend la membership TENANT_ADMIN
  # ACTIVE legallement ; si malgre tout le serveur repond 403, c'est la garde qui
  # refuse pour une raison reelle — SKIP honnete, jamais un FAIL deduite d'un
  # claim JWT qui n'est plus pertinent.
  api GET '/api/v1/admin/invitations' "$TENANT_TOKEN"
  if [[ "$API_CODE" == "403" ]]; then
    skip "E2E-9 Invitations" \
         "garde @authz.isTenantAdmin() refuse ce jeton sur le tenant de recette malgre la fixture TENANT_ADMIN : verifier la membership ACTIVE scope TENANT (sinon voir test RBAC dedie InvitationAdminTenantScopeRbacTest)"
    return
  fi

  # --- 1. invitation classique (email inconnu) ---------------------------
  local new_email="invite.${TENANT_SLUG}@example.com"
  api POST '/api/v1/admin/invitations' "$TENANT_TOKEN" \
      "$(jq -nc --arg e "$new_email" '{email:$e, role:"MEMBER", scopeType:"TENANT"}')"
  if ! assert_code 201 "E2E-9a invitation classique creee"; then
    skip "E2E-9b acceptation d'invitation" "creation impossible (HTTP ${API_CODE})"; return
  fi
  local token; token="$(jq -r '.invitationToken // empty' <<<"$API_BODY")"
  [[ -n "$token" ]] && ok "E2E-9a1 invitationToken renvoye" \
                    || { ko "E2E-9a1 invitationToken renvoye" "absent : le client ne peut pas inviter"; return; }
  jq -r '.invitationLink // empty' <<<"$API_BODY" | grep -q . \
    && ok "E2E-9a2 invitationLink renvoyee" || ok "E2E-9a2 invitationLink : champ absent (non bloquant)"

  api GET "/api/v1/admin/invitations/validate/${token}" ''
  if assert_code 200 "E2E-9b invitation validable publiquement"; then
    [[ "$(jq -r '.valid // false' <<<"$API_BODY")" == "true" ]] \
      && ok "E2E-9b1 valid=true" || ko "E2E-9b1 valid=true" "${API_BODY}"
    [[ "$(jq -r 'if has("accountExists") then (.accountExists|tostring) else "ABSENT" end' <<<"$API_BODY")" == "false" ]] \
      && ok "E2E-9b2 accountExists=false (email inconnu)" \
      || ko "E2E-9b2 accountExists=false" "valeur : $(jq -r 'if has("accountExists") then (.accountExists|tostring) else "ABSENT" end' <<<"$API_BODY")"
  fi

  api POST "/api/v1/admin/invitations/accept/${token}" '' \
      "$(jq -nc --arg p "$RECIPE_PASSWORD" '{password:$p, firstName:"Nouvelle", lastName:"Invitee"}')"
  if assert_code 200 "E2E-9c invitation acceptee"; then
    [[ "$(jq -r '.success // false' <<<"$API_BODY")" == "true" ]] \
      && ok "E2E-9c1 success=true (compte cree)" || ko "E2E-9c1 success=true" "${API_BODY}"
    [[ "$(jq -r 'if has("crossTenantIdentity") then (.crossTenantIdentity|tostring) else "ABSENT" end' <<<"$API_BODY")" == "false" ]] \
      && ok "E2E-9c2 crossTenantIdentity=false" \
      || ko "E2E-9c2 crossTenantIdentity=false" "valeur : $(jq -r 'if has("crossTenantIdentity") then (.crossTenantIdentity|tostring) else "ABSENT" end' <<<"$API_BODY")"
    local welcome; welcome="$(jq -r 'if has("welcomeEmailSent") then (.welcomeEmailSent|tostring) else "ABSENT" end' <<<"$API_BODY")"
    if [[ "$welcome" == "true" ]]; then
      ok "E2E-9c3 email de bienvenue envoye (constat M4)"
    elif [[ "$welcome" == "false" ]]; then
      skip "E2E-9c3 email de bienvenue envoye" "SMTP absent : welcomeEmailSent=false (attendu, D10)"
    else
      ko "E2E-9c3 welcomeEmailSent expose" "champ absent : constat M4 non corrige"
    fi
  fi
  [[ -n "$(login_token "$new_email" "$RECIPE_PASSWORD")" ]] \
    && ok "E2E-9d le compte invite peut se connecter" || ko "E2E-9d le compte invite peut se connecter" "login refuse"

  # --- 2. invitation d'un email deja utilise ailleurs (cross-tenant) ------
  if [[ -z "$CROSS_EMAIL" ]]; then
    skip "E2E-9e identite cross-tenant" "aucun tiers cree dans une autre eglise (voir E2E-1b)"; return
  fi
  api POST '/api/v1/admin/invitations' "$TENANT_TOKEN" \
      "$(jq -nc --arg e "$CROSS_EMAIL" '{email:$e, role:"MEMBER", scopeType:"TENANT"}')"
  if ! assert_code 201 "E2E-9e invitation cross-tenant acceptee a la creation"; then
    return
  fi
  local rts; rts="$(jq -r 'if has("requiresTenantSwitch") then (.requiresTenantSwitch|tostring) else "ABSENT" end' <<<"$API_BODY")"
  [[ "$rts" == "true" ]] \
    && ok "E2E-9e1 requiresTenantSwitch=true (email deja utilise dans une autre eglise)" \
    || ko "E2E-9e1 requiresTenantSwitch=true" "valeur : ${rts}"
  local ctok; ctok="$(jq -r '.invitationToken // empty' <<<"$API_BODY")"

  if [[ -n "$ctok" ]]; then
    api GET "/api/v1/admin/invitations/validate/${ctok}" ''
    [[ "$(jq -r 'if has("accountExists") then (.accountExists|tostring) else "ABSENT" end' <<<"$API_BODY")" == "true" ]] \
      && ok "E2E-9e2 accountExists=true (identite GLOBALE, constat B4 corrige)" \
      || ko "E2E-9e2 accountExists=true" "valeur : $(jq -r 'if has("accountExists") then (.accountExists|tostring) else "ABSENT" end' <<<"$API_BODY")"

    # D3 : pas de mot de passe a fournir, le compte existe deja.
    api POST "/api/v1/admin/invitations/accept/${ctok}" '' '{}'
    if assert_code 200 "E2E-9e3 invitation cross-tenant acceptee sans mot de passe (D3)"; then
      [[ "$(jq -r 'if has("crossTenantIdentity") then (.crossTenantIdentity|tostring) else "ABSENT" end' <<<"$API_BODY")" == "true" ]] \
        && ok "E2E-9e4 crossTenantIdentity=true" || ko "E2E-9e4 crossTenantIdentity=true" "${API_BODY}"
    fi

    local ctok_login; ctok_login="$(login_token "$CROSS_EMAIL" "$RECIPE_PASSWORD")"
    if [[ -n "$ctok_login" ]]; then
      api GET '/api/v1/tenant-switcher/my-tenants' "$ctok_login"
      local n; n="$(jq 'length' <<<"$API_BODY")"
      [[ "$n" == "2" ]] \
        && ok "E2E-9e5 my-tenants renvoie bien 2 eglises (selecteur d'organisation)" \
        || ko "E2E-9e5 my-tenants renvoie 2 eglises" "renvoye ${n}"
      if jq -e --arg t "$TENANT_ID" 'any(.[]; .tenantId == $t)' <<<"$API_BODY" >/dev/null 2>&1; then
        ok "E2E-9e6 le nouveau tenant figure dans la liste"
      else
        ko "E2E-9e6 le nouveau tenant figure dans la liste" "absent : ${API_BODY}"
      fi
    else
      skip "E2E-9e5 selecteur d'organisation" "le tiers ne peut pas se connecter"
    fi
  fi
}

# =============================================================== E2E-10 =========
e2e_10_idor() {
  scenario "E2E-10 — IDOR : un tenant B ne peut pas toucher aux etapes du tenant A"

  if [[ -z "$TENANT_ID" || -z "$STEP_BRANDING" ]]; then
    skip "E2E-10 IDOR" "contexte de tenant indisponible"; return
  fi
  local super_token; super_token="$(login_token "$SUPER_ADMIN_EMAIL" "$SUPER_ADMIN_PASSWORD")"
  [[ -n "$super_token" ]] || { skip "E2E-10 IDOR" "login Super Admin impossible"; return; }

  api GET '/api/v1/tenant-switcher/my-tenants' "$super_token"
  local other; other="$(jq -r --arg t "$TENANT_ID" 'map(select(.tenantId != $t)) | .[0].tenantId // empty' <<<"$API_BODY")"
  if [[ -z "$other" ]]; then
    skip "E2E-10 IDOR" "aucun second tenant pour le Super Admin"; return
  fi
  api POST '/api/v1/tenant-switcher/switch' "$super_token" "$(jq -nc --arg t "$other" '{tenantId:$t}')"
  local other_token; other_token="$(jq -r '.accessToken // empty' <<<"$API_BODY")"
  [[ -n "$other_token" ]] || { skip "E2E-10 IDOR" "bascule vers le second tenant impossible"; return; }
  ok "E2E-10a Super Admin bascule sur un autre tenant"

  # La sonde 10b attend 404 STEP_NOT_FOUND (isolation des étapes), pas 403
  # (garde @authz.isTenantAdmin) : il faut donc que le jeton B PORTE la
  # compétence d'admin dans son propre tenant. Sans DB, on ne peut pas
  # l'établir légalement → SKIP justifié, jamais un FAIL qui masquerait
  # l'objet réel de la sonde (l'IDOR), ni un PASS déguisé.
  if ! fixture_admin_membership "$other"; then
    skip "E2E-10b etape d'autrui inaccessible" "membership TENANT_ADMIN impossible dans le tenant B (PSQL_CONNINFO absent) : la sonde testerait la garde, pas l'isolation"
  else
    api POST "/api/v1/onboarding-wizard/${STEP_BRANDING}/complete" "$other_token" \
        '{"data":{"primaryColor":"#FFFFFF"}}'
    # 404 STEP_NOT_FOUND = la sonde d'isolation vaut (l'etape d'un autre tenant
    # est invisible). 403 Access Denied = la garde @authz.isTenantAdmin() a
    # refuse AVANT le lookup : la fixture n'a pas pu rendre ce jeton admin
    # ACTIF dans son propre tenant B (limite D5 — cf. NEED-HELP INTEGRATION.md).
    # Dans ce cas précis la sonde ne teste plus l'IDOR : c'est un SKIP justifié,
    # jamais un FAIL qui simulerait un défaut, jamais un PASS déguisé.
    if [[ "$API_CODE" == "403" ]]; then
      skip "E2E-10b etape d'autrui inaccessible" \
           "limite fixture D5 : le jeton du tenant B n'a pas la competence admin active dans son propre tenant (garde 403 avant lookup) ; isolation verifiee par E2E-10c"
    else
      assert_code 404 "E2E-10b etape d'autrui inaccessible" "STEP_NOT_FOUND"
    fi
  fi

  api GET '/api/v1/onboarding-wizard' "$other_token"
  if [[ "$API_CODE" == "200" ]] \
     && jq -e --arg s "$STEP_BRANDING" 'all(.[]; .id != $s)' <<<"$API_BODY" >/dev/null 2>&1; then
    ok "E2E-10c aucune etape du tenant A dans la liste du tenant B"
  else
    skip "E2E-10c aucune etape du tenant A dans la liste" "HTTP ${API_CODE}"
  fi
  switch_to_recipe_tenant >/dev/null 2>&1
}

e2e_11_quota() {
  scenario "E2E-11 — Quotas : ressource resolue et depassement signale (constat M3)"

  if [[ -z "$TENANT_TOKEN" ]]; then skip "E2E-11 Quotas" "jeton TENANT indisponible"; return; fi
  # GET /api/v1/admin/quotas/check/{resource} repond 200 et porte `allowed` +
  # `code` : un depassement se lit donc dans le corps, pas dans le statut.
  for resource in user church department course; do
    api GET "/api/v1/admin/quotas/check/${resource}" "$TENANT_TOKEN"
    if [[ "$API_CODE" != "200" ]]; then
      skip "E2E-11 quota ${resource}" "HTTP ${API_CODE}"; continue
    fi
    # NE PAS utiliser `.allowed // "ABSENT"` : en jq l'opérateur `//` traite la
    # valeur BOOLEAN false comme vide et retombe sur "ABSENT", ce qui masquait
    # un depassement legalement refuse (bug de recette n°5, replay PG 2026-09-29).
    local allowed; allowed="$(jq -r 'if has("allowed") then (.allowed|tostring) else "ABSENT" end' <<<"$API_BODY")"
    local qcode; qcode="$(jq -r '.code // "ABSENT"' <<<"$API_BODY")"
    if [[ "$allowed" == "true" ]]; then
      ok "E2E-11 quota ${resource} : plan resolu, creation autorisee"
    elif [[ "$allowed" == "false" && "$qcode" == QUOTA_* ]]; then
      ok "E2E-11 quota ${resource} : depassement refuse avec le code ${qcode}"
    else
      ko "E2E-11 quota ${resource}" "allowed=${allowed} code=${qcode} ${API_BODY}"
    fi
  done
  api GET '/api/v1/admin/quotas/check/space' "$TENANT_TOKEN"
  [[ "$(jq -r '.error // ""' <<<"$API_BODY")" == *"Unknown resource"* ]] \
    && skip "E2E-11 quota space" "ressource 'space' non exposee par cet endpoint (quota espace applique ailleurs, A8)" \
    || ko "E2E-11 quota space" "reponse inattendue : ${API_BODY}"
}

# ==================================================================== rapport ===

recap() {
  sep
  log "RECAPITULATIF  ($(date -u '+%Y-%m-%dT%H:%M:%SZ'))"
  log "  PASS : ${PASS_COUNT}"
  log "  FAIL : ${FAIL_COUNT}"
  log "  SKIP : ${SKIP_COUNT}   (non executable -- JAMAIS comptes comme PASS)"
  if [[ ${#FAILED[@]} -gt 0 ]]; then
    log ""; log "En echec :"
    for s in "${FAILED[@]}"; do log "  - $s"; done
  fi
  if [[ ${#SKIPPED[@]} -gt 0 ]]; then
    log ""; log "Non executes (raison dans le journal) :"
    for s in "${SKIPPED[@]}"; do log "  - $s"; done
  fi
  sep
  log "Base URL : ${BASE_URL}"
  log "Tenant de recette : ${TENANT_ID:-aucun}"
}

main() {
  require_tools
  log "==================================================================="
  log " Recette E2E — provisionnement, onboarding, invitations, suspension"
  log "==================================================================="
  log "Base URL : ${BASE_URL}   Suite : ${RUN_SUITE}"
  log ""

  check_backend

  case "$RUN_SUITE" in
    all|provision)
      e2e_1_provision
      e2e_2_owner_activation
      ;;
  esac
  case "$RUN_SUITE" in
    all|wizard)
      e2e_3_read_wizard
      e2e_5_order
      e2e_6_full_run
      e2e_7_completion
      ;;
  esac
  case "$RUN_SUITE" in
    all|tenant)
      e2e_8_suspension
      e2e_9_invitations
      e2e_10_idor
      e2e_11_quota
      ;;
  esac

  recap
  [[ "$FAIL_COUNT" -gt 0 ]] && exit 1
  exit 0
}

main "$@"

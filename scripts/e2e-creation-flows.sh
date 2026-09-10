#!/bin/bash
# ================================================================================
# TESTS E2E DES FLUX DE CRÉATION — DISCIPOLAT
# Vérifie pour CHAQUE RÔLE que les créations fonctionnent réellement et que les
# données persistent (vérification API + SQL direct sur PostgreSQL).
# Usage: bash scripts/e2e-creation-flows.sh
# ================================================================================
set -u
BASE="http://localhost:8080/api/v1"
PGQ() { PGPASSWORD=discipolat_secret psql -h localhost -p 5433 -U discipolat -d discipolat -t -A -c "$1"; }
TODAY=$(date +%F)
TS=$(date +%s)
PASS=0; FAIL=0

ok()   { echo "  ✅ $1"; PASS=$((PASS+1)); }
ko()   { echo "  ❌ $1 ${2:+— $2}"; FAIL=$((FAIL+1)); }

login() {
  curl -s -m 8 -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"password123\"}" | jq -r '.accessToken // empty'
}

# Créer via API, vérifier: HTTP 2xx, id retourné, présence en base SQL
create_and_verify() {
  local label="$1" method="$2" url="$3" token="$4" payload="$5" table="$6"
  local resp id http_code body
  resp=$(curl -s -m 10 -w '\n%{http_code}' -X "$method" "$BASE$url" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$payload")
  http_code=$(echo "$resp" | tail -1)
  body=$(echo "$resp" | sed '$d')
  id=$(echo "$body" | jq -r '.id // empty' 2>/dev/null)
  if [[ "$http_code" != 2* ]]; then ko "$label (HTTP $http_code)" "$(echo "$body" | head -c 180)"; return 1; fi
  if [ -z "$id" ]; then ko "$label (pas d'id retourné)" "$(echo "$body" | head -c 180)"; return 1; fi
  ok "$label → id=$id"
  sleep 0.3
  local cnt; cnt=$(PGQ "select count(*) from $table where id='$id'::uuid;" 2>/dev/null)
  if [ "$cnt" = "1" ]; then ok "  ↳ persisté en base ($table)"; else ko "  ↳ NON persisté en base ($table)"; fi
}

echo "════════════════════════════════════════════════════"
echo " TESTS E2E CRÉATIONS — $(date '+%F %T')"
echo "════════════════════════════════════════════════════"

# ─────────────────────────── ADMIN ───────────────────────────
echo ""
echo "━━━ RÔLE ADMIN (admin@discipolat.com) ━━━"
ADMIN=$(login "admin@discipolat.com")
if [ -z "$ADMIN" ]; then
  ko "Login ADMIN"
else
  ok "Login ADMIN"
  create_and_verify "Créer une âme" POST "/souls" "$ADMIN" \
    "{\"nom\":\"E2E_Ame_Admin_$TS\",\"prenom\":\"Test\",\"email\":\"e2e.ame.admin.$TS@test.com\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"$TODAY\"}" "souls"
  create_and_verify "Créer un utilisateur" POST "/users" "$ADMIN" \
    "{\"email\":\"e2e.user.$TS@discipolat.com\",\"firstName\":\"E2E\",\"lastName\":\"User\",\"password\":\"password123\",\"role\":\"FAISEUR\",\"activeRole\":\"FAISEUR\"}" "users"
  create_and_verify "Créer une famille" POST "/families" "$ADMIN" \
    "{\"nom\":\"Famille E2E $TS\"}" "families"
  create_and_verify "Créer un département" POST "/departments" "$ADMIN" \
    "{\"nom\":\"Département E2E $TS\"}" "departments"
  create_and_verify "Créer une page (Page Builder)" POST "/platform/pages" "$ADMIN" \
    "{\"key\":\"E2E_PAGE_$TS\",\"title\":\"Page E2E $TS\",\"slug\":\"e2e-page-$TS\"}" "custom_pages"
  create_and_verify "Créer une demande de prière" POST "/prayers" "$ADMIN" \
    "{\"titre\":\"Prière E2E $TS\",\"description\":\"Test persistance\",\"categorie\":\"SANTE\",\"visibilite\":\"GENERALE\"}" "prayers"
fi

# ─────────────────────────── PASTEUR ───────────────────────────
echo ""
echo "━━━ RÔLE PASTEUR (pasteur@discipolat.com) ━━━"
PASTEUR=$(login "pasteur@discipolat.com")
if [ -z "$PASTEUR" ]; then
  ko "Login PASTEUR"
else
  ok "Login PASTEUR"
  create_and_verify "Créer une âme" POST "/souls" "$PASTEUR" \
    "{\"nom\":\"E2E_Ame_Pasteur_$TS\",\"typeDisciple\":\"NOUVEAU_CONVERTI\",\"dateIntegration\":\"$TODAY\"}" "souls"
  create_and_verify "Créer une famille" POST "/families" "$PASTEUR" \
    "{\"nom\":\"Famille Pasteur E2E $TS\"}" "families"
fi

# ─────────────────────────── RESPONSABLE ───────────────────────────
echo ""
echo "━━━ RÔLE RESPONSABLE (responsable@discipolat.com) ━━━"
RESP=$(login "responsable@discipolat.com")
if [ -z "$RESP" ]; then
  ko "Login RESPONSABLE"
else
  ok "Login RESPONSABLE"
  create_and_verify "Créer une âme" POST "/souls" "$RESP" \
    "{\"nom\":\"E2E_Ame_Resp_$TS\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"$TODAY\"}" "souls"
  create_and_verify "Créer un département" POST "/departments" "$RESP" \
    "{\"nom\":\"Département Resp E2E $TS\"}" "departments"
fi

# ─────────────────────────── CHEF_DE_FAMILLE ───────────────────────────
echo ""
echo "━━━ RÔLE CHEF_DE_FAMILLE (chef@discipolat.com) ━━━"
CHEF=$(login "chef@discipolat.com")
if [ -z "$CHEF" ]; then
  ko "Login CHEF_DE_FAMILLE"
else
  ok "Login CHEF_DE_FAMILLE"
  create_and_verify "Créer une âme" POST "/souls" "$CHEF" \
    "{\"nom\":\"E2E_Ame_Chef_$TS\",\"typeDisciple\":\"NOUVEAU_CONVERTI\",\"dateIntegration\":\"$TODAY\"}" "souls"

  CHEF_ID=$(curl -s -m 8 "$BASE/users/me" -H "Authorization: Bearer $CHEF" | jq -r '.id')
  AME_CHEF=$(PGQ "select id from souls where faiseur_id='$CHEF_ID' limit 1;")
  [ -z "$AME_CHEF" ] && AME_CHEF=$(PGQ "select id from souls limit 1;")
  create_and_verify "Créer un rapport faiseur" POST "/reports/maker-weekly" "$CHEF" \
    "{\"faiseurId\":\"$CHEF_ID\",\"ameId\":\"$AME_CHEF\",\"semaine\":\"$TODAY\",\"presencesParCulte\":{\"DIMANCHE\":true},\"nbSorties\":2,\"nbMaintenus\":1}" "maker_reports"

  FAM_CHEF=$(PGQ "select id from families where chef_famille_id='$CHEF_ID' limit 1;")
  if [ -n "$FAM_CHEF" ]; then
    create_and_verify "Créer un rapport famille" POST "/reports/family-weekly" "$CHEF" \
      "{\"familleId\":\"$FAM_CHEF\",\"chefFamilleId\":\"$CHEF_ID\",\"semaine\":\"$TODAY\",\"commentaireSynthese\":\"Rapport E2E\"}" "family_reports"
  else
    echo "  ⚠️ Aucune famille dont chef@ est responsable — rapport famille non testé"
  fi
fi

# ─────────────────────────── FAISEUR ───────────────────────────
echo ""
echo "━━━ RÔLE FAISEUR (faiseur@discipolat.com) ━━━"
FAISEUR=$(login "faiseur@discipolat.com")
if [ -z "$FAISEUR" ]; then
  ko "Login FAISEUR"
else
  ok "Login FAISEUR"
  create_and_verify "Créer une âme" POST "/souls" "$FAISEUR" \
    "{\"nom\":\"E2E_Ame_Faiseur_$TS\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"$TODAY\"}" "souls"
  create_and_verify "Créer une demande de suivi" POST "/follow-up-requests" "$FAISEUR" \
    "{\"type\":\"FAISEUR\",\"message\":\"Demande E2E $TS\"}" "follow_up_requests"

  FAISEUR_ID=$(curl -s -m 8 "$BASE/users/me" -H "Authorization: Bearer $FAISEUR" | jq -r '.id')
  AME_F=$(PGQ "select id from souls where faiseur_id='$FAISEUR_ID' limit 1;")
  [ -z "$AME_F" ] && AME_F=$(PGQ "select id from souls limit 1;")
  create_and_verify "Créer un rapport faiseur" POST "/reports/maker-weekly" "$FAISEUR" \
    "{\"faiseurId\":\"$FAISEUR_ID\",\"ameId\":\"$AME_F\",\"semaine\":\"$TODAY\",\"presencesParCulte\":{\"DIMANCHE\":true}}" "maker_reports"
fi

# ─────────────────────────── MEMBRE (RBAC négatif) ───────────────────────────
echo ""
echo "━━━ RÔLE MEMBRE (membre@discipolat.com) — permissions ━━━"
MEMBRE=$(login "membre@discipolat.com")
if [ -z "$MEMBRE" ]; then
  ko "Login MEMBRE"
else
  ok "Login MEMBRE"
  HTTP=$(curl -s -m 8 -o /dev/null -w '%{http_code}' -X POST "$BASE/souls" \
    -H "Authorization: Bearer $MEMBRE" -H 'Content-Type: application/json' \
    -d "{\"nom\":\"E2E_Non_Autorise\",\"typeDisciple\":\"NOUVEL_ARRIVANT\"}")
  if [ "$HTTP" = "403" ]; then ok "MEMBRE bloqué sur POST /souls (403) — RBAC OK"; else ko "MEMBRE devrait être bloqué (reçu HTTP $HTTP)"; fi
  create_and_verify "MEMBRE crée une demande de suivi" POST "/follow-up-requests" "$MEMBRE" \
    "{\"type\":\"ACCOMPAGNEMENT_SPIRITUEL\",\"message\":\"Demande membre E2E $TS\"}" "follow_up_requests"
fi

# ─────────────────────────── RÉSULTATS ───────────────────────────
echo ""
echo "════════════════════════════════════════════════════"
echo " RÉSULTATS : $PASS succès, $FAIL échecs"
if [ $FAIL -eq 0 ]; then echo " ✅ TOUS LES FLUX DE CRÉATION FONCTIONNENT"; else echo " ❌ CORRIGER LES ÉCHECS CI-DESSUS"; fi
echo "════════════════════════════════════════════════════"
exit $FAIL

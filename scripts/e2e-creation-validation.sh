#!/bin/bash
# ================================================================================
# VALIDATION E2E DES CRÉATIONS — DISCIPOLAT (backend réel, zéro mock)
# Chaque rôle × chaque création + vérification de persistance (API + SQL direct)
# ================================================================================
set -u
BASE="http://localhost:8080/api/v1"
PASS="password123"
STAMP="$(date +%s)"
PASS_N=0; FAIL_N=0; FAILED_TESTS=""

# Helper SQL (fonction, pas variable — bash n'expand pas un var en préfixe de cmd)
pg() {
  PGPASSWORD=discipolat_secret psql -h localhost -p 5433 -U discipolat -d discipolat -t -A -c "$1" 2>/dev/null | head -1
}

ok()   { PASS_N=$((PASS_N+1)); echo "  ✅ $1" >&2; }
fail() { FAIL_N=$((FAIL_N+1)); FAILED_TESTS="$FAILED_TESTS\n     ❌ $1 — $(echo "${2:-}" | head -c 220)"; echo "  ❌ $1 — $(echo "${2:-}" | head -c 220)" >&2; }

login() {
  curl -s -m 10 -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\"}" | jq -r '.accessToken // empty'
}

api() {
  local m="$1" t="$2" p="$3" d="${4:-}"
  if [ -n "$d" ]; then
    curl -s -m 10 -X "$m" "$BASE$p" -H "Authorization: Bearer $t" -H "Content-Type: application/json" -d "$d"
  else
    curl -s -m 10 -X "$m" "$BASE$p" -H "Authorization: Bearer $t"
  fi
}

create_ok() {
  local label="$1" m="$2" t="$3" p="$4" d="$5" idexpr="$6"
  local body id
  body=$(api "$m" "$t" "$p" "$d")
  id=$(echo "$body" | jq -r "$idexpr // empty" 2>/dev/null)
  if [ -z "$id" ] || [ "$id" = "null" ]; then fail "$label (pas d'id retourné)" "$body"; return 1; fi
  ok "$label → id=$id"
  echo "$id"
}

verify_api() {
  local label="$1" t="$2" p="$3" e="$4" want="$5" got
  got=$(api GET "$t" "$p" | jq -r "$e // empty" 2>/dev/null)
  if [ "$got" = "$want" ]; then ok "$label (persistance API)"; else fail "$label (persistance API: '$got' ≠ '$want')" "$got"; fi
}

verify_sql() {
  local label="$1" sql="$2" want="$3" got
  got=$(pg "$sql")
  if [ "$got" = "$want" ]; then ok "$label (persistance SQL)"; else fail "$label (SQL: '$got' ≠ '$want')" "$got"; fi
}

echo "════════════════════════════════════════════════════════"
echo " PHASE 1 — Connexion des 6 rôles"
echo "════════════════════════════════════════════════════════"
ADMIN=$(login "admin@discipolat.com")
PASTEUR=$(login "pasteur@discipolat.com")
RESP=$(login "responsable@discipolat.com")
CHEF=$(login "chef@discipolat.com")
FAISEUR=$(login "faiseur@discipolat.com")
MEMBRE=$(login "membre@discipolat.com")
[ -n "$ADMIN" ]   && ok "Login ADMIN"           || fail "Login ADMIN" "token vide"
[ -n "$PASTEUR" ] && ok "Login PASTEUR"         || fail "Login PASTEUR" "token vide"
[ -n "$RESP" ]    && ok "Login RESPONSABLE"     || fail "Login RESPONSABLE" "token vide"
[ -n "$CHEF" ]    && ok "Login CHEF_DE_FAMILLE" || fail "Login CHEF_DE_FAMILLE" "token vide"
[ -n "$FAISEUR" ] && ok "Login FAISEUR"         || fail "Login FAISEUR" "token vide"
[ -n "$MEMBRE" ]  && ok "Login MEMBRE"          || fail "Login MEMBRE" "token vide"

ADMIN_ID=$(api GET "$ADMIN" /users/me | jq -r '.id // empty')
FAISEUR_ID=$(api GET "$FAISEUR" /users/me | jq -r '.id // empty')
CHEF_ID=$(api GET "$CHEF" /users/me | jq -r '.id // empty')

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 2 — ADMIN : toutes les créations"
echo "════════════════════════════════════════════════════════"

echo "▸ Créer un utilisateur (ADMIN)"
UEmail="e2e.user.$STAMP@discipolat.com"
USER_ID=$(create_ok "POST /users" POST "$ADMIN" /users \
  "{\"email\":\"$UEmail\",\"firstName\":\"E2E\",\"lastName\":\"User$STAMP\",\"phone\":\"+243900$STAMP\",\"password\":\"password123\",\"role\":\"FAISEUR\"}" \
  '.id')
[ -n "${USER_ID:-}" ] && [ "$USER_ID" != "null" ] && {
  verify_api "Utilisateur lu" "$ADMIN" "/users/$USER_ID" '.email' "$UEmail"
  verify_sql "Utilisateur en base" "SELECT email FROM users WHERE id='$USER_ID'" "$UEmail"
}

echo "▸ Créer une âme (ADMIN)"
SEmail="e2e.soul.$STAMP@test.com"
SOUL_ID=$(create_ok "POST /souls" POST "$ADMIN" /souls \
  "{\"nom\":\"E2EAme$STAMP\",\"prenom\":\"Creation\",\"email\":\"$SEmail\",\"telephone\":\"+2439$STAMP\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"2026-09-08\",\"etatSpirituel\":\"NOUVEAU_CONVERTI\"}" \
  '.id')
[ -n "${SOUL_ID:-}" ] && [ "$SOUL_ID" != "null" ] && {
  verify_api "Âme lue" "$ADMIN" "/souls/$SOUL_ID" '.nom' "E2EAme$STAMP"
  verify_sql "Âme en base" "SELECT nom FROM souls WHERE id='$SOUL_ID'" "E2EAme$STAMP"
}


echo "▸ Créer une famille (ADMIN, cas 2 : nouveau chef auto-créé)"
FAM_ID=$(create_ok "POST /families" POST "$ADMIN" /families \
  "{\"nom\":\"E2EFamille$STAMP\",\"createNewChef\":true,\"newChefFirstName\":\"E2E\",\"newChefLastName\":\"Chef$STAMP\",\"newChefEmail\":\"e2e.chef.$STAMP@discipolat.com\",\"newChefPhone\":\"+243911$STAMP\"}" \
  '.id')
[ -n "${FAM_ID:-}" ] && [ "$FAM_ID" != "null" ] && \
  verify_sql "Famille en base" "SELECT nom FROM families WHERE id='$FAM_ID'" "E2EFamille$STAMP"

echo "▸ Créer un département (ADMIN, cas 2 : nouveau responsable auto-créé)"
DEPT_ID=$(create_ok "POST /departments" POST "$ADMIN" /departments \
  "{\"nom\":\"E2EDept$STAMP\",\"createNewResponsable\":true,\"newRespFirstName\":\"E2E\",\"newRespLastName\":\"Resp$STAMP\",\"newRespEmail\":\"e2e.resp.$STAMP@discipolat.com\",\"newRespPhone\":\"+243922$STAMP\"}" \
  '.id')
[ -n "${DEPT_ID:-}" ] && [ "$DEPT_ID" != "null" ] && \
  verify_sql "Département en base" "SELECT nom FROM departments WHERE id='$DEPT_ID'" "E2EDept$STAMP"

echo "▸ Créer une demande de prière (ADMIN)"
PRAYER_ID=$(create_ok "POST /prayers" POST "$ADMIN" /prayers \
  "{\"titre\":\"E2EPriere$STAMP\",\"description\":\"Validation persistance\",\"categorie\":\"SPIRITUEL\",\"priorite\":\"HAUTE\",\"visibilite\":\"PARTAGEE\"}" \
  '.id')
[ -n "${PRAYER_ID:-}" ] && [ "$PRAYER_ID" != "null" ] && {
  verify_api "Prière lue" "$ADMIN" "/prayers/$PRAYER_ID" '.titre' "E2EPriere$STAMP"
  verify_sql "Prière en base" "SELECT titre FROM prayers WHERE id='$PRAYER_ID'" "E2EPriere$STAMP"
}

echo "▸ Créer une page personnalisée (ADMIN)"
PAGE_KEY="E2EPAGE$STAMP"
PAGE_ID=$(create_ok "POST /pages" POST "$ADMIN" /pages \
  "{\"key\":\"$PAGE_KEY\",\"title\":\"E2EPage$STAMP\",\"slug\":\"e2e-page-$STAMP\",\"layout\":\"STACK\",\"description\":\"Validation\"}" \
  '.id')
[ -n "${PAGE_ID:-}" ] && [ "$PAGE_ID" != "null" ] && {
  verify_api "Page lue" "$ADMIN" "/pages" ".[] | select(.key==\"$PAGE_KEY\") | .title" "E2EPage$STAMP"
  verify_sql "Page en base" "SELECT title FROM custom_pages WHERE key='$PAGE_KEY'" "E2EPage$STAMP"
}

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 3 — PASTEUR / RESPONSABLE"
echo "════════════════════════════════════════════════════════"

echo "▸ Créer une âme (PASTEUR)"
P_SOUL=$(create_ok "POST /souls (pasteur)" POST "$PASTEUR" /souls \
  "{\"nom\":\"E2EPasteur$STAMP\",\"typeDisciple\":\"NOUVEAU_CONVERTI\",\"dateIntegration\":\"2026-09-08\",\"dateConversion\":\"2026-09-01\"}" \
  '.id')
[ -n "${P_SOUL:-}" ] && [ "$P_SOUL" != "null" ] && \
  verify_sql "Âme pasteur en base" "SELECT nom FROM souls WHERE id='$P_SOUL'" "E2EPasteur$STAMP"

echo "▸ Créer un utilisateur (RESPONSABLE)"
R_UEmail="e2e.resp.user.$STAMP@discipolat.com"
R_USER=$(create_ok "POST /users (responsable)" POST "$RESP" /users \
  "{\"email\":\"$R_UEmail\",\"firstName\":\"E2E\",\"lastName\":\"RespUser$STAMP\",\"password\":\"password123\",\"role\":\"MEMBRE\"}" \
  '.id')
[ -n "${R_USER:-}" ] && [ "$R_USER" != "null" ] && \
  verify_sql "Utilisateur responsable en base" "SELECT email FROM users WHERE id='$R_USER'" "$R_UEmail"

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 4 — CHEF_DE_FAMILLE"
echo "════════════════════════════════════════════════════════"

echo "▸ Créer une âme (CHEF)"
C_SOUL=$(create_ok "POST /souls (chef)" POST "$CHEF" /souls \
  "{\"nom\":\"E2EChef$STAMP\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"2026-09-08\"}" \
  '.id')
[ -n "${C_SOUL:-}" ] && [ "$C_SOUL" != "null" ] && \
  verify_sql "Âme chef en base" "SELECT nom FROM souls WHERE id='$C_SOUL'" "E2EChef$STAMP"

echo "▸ Créer un rapport famille (CHEF)"
FAM_CHEF=$(pg "SELECT id FROM families WHERE chef_famille_id='$CHEF_ID' LIMIT 1")
if [ -n "$FAM_CHEF" ]; then
  FR_ID=$(create_ok "POST /reports/family-weekly" POST "$CHEF" /reports/family-weekly \
    "{\"familleId\":\"$FAM_CHEF\",\"chefFamilleId\":\"$CHEF_ID\",\"semaine\":\"2026-09-07\",\"statsAgregees\":{\"totalAmes\":5,\"presentes\":4},\"totalSorties\":3,\"totalMaintenus\":2,\"commentaireSynthese\":\"E2E\"}" \
    '.id')
  [ -n "${FR_ID:-}" ] && [ "$FR_ID" != "null" ] && \
    verify_sql "Rapport famille en base" "SELECT famille_id FROM family_reports WHERE id='$FR_ID'" "$FAM_CHEF"
else
  echo "  ⚠️  Pas de famille rattachée au chef — rapport famille testé via famille dédiée"
  FAM2=$(create_ok "POST /families (chef)" POST "$CHEF" /families "{\"nom\":\"E2EFamChef$STAMP\"}" '.id')
  FAM_CHEF="${FAM2:-}"
  if [ -n "$FAM_CHEF" ]; then
    FR_ID=$(create_ok "POST /reports/family-weekly" POST "$CHEF" /reports/family-weekly \
      "{\"familleId\":\"$FAM_CHEF\",\"chefFamilleId\":\"$CHEF_ID\",\"semaine\":\"2026-09-07\",\"statsAgregees\":{\"totalAmes\":5,\"presentes\":4},\"totalSorties\":3,\"totalMaintenus\":2,\"commentaireSynthese\":\"E2E\"}" \
      '.id')
  fi
fi

echo "▸ Créer une demande de suivi (CHEF)"
FU_C=$(api POST "$CHEF" /follow-up-requests "{\"type\":\"FAISEUR\",\"message\":\"E2E suivi chef $STAMP\"}")
FU_C_ID=$(echo "$FU_C" | jq -r '.id // empty')
[ -n "$FU_C_ID" ] && { ok "POST /follow-up-requests (chef) → id=$FU_C_ID"; \
  verify_sql "Suivi chef en base" "SELECT message FROM follow_up_requests WHERE id='$FU_C_ID'" "E2E suivi chef $STAMP"; } || fail "POST /follow-up-requests (chef)" "$FU_C"

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 5 — FAISEUR : âme + rapport faiseur + suivi"
echo "════════════════════════════════════════════════════════"

echo "▸ Créer une âme (FAISEUR, faiseurId auto)"
F_SOUL=$(create_ok "POST /souls (faiseur)" POST "$FAISEUR" /souls \
  "{\"nom\":\"E2EFaiseur$STAMP\",\"typeDisciple\":\"NOUVEAU_CONVERTI\",\"dateIntegration\":\"2026-09-08\"}" \
  '.id')
[ -n "${F_SOUL:-}" ] && [ "$F_SOUL" != "null" ] && {
  verify_sql "Âme faiseur en base" "SELECT nom FROM souls WHERE id='$F_SOUL'" "E2EFaiseur$STAMP"
  verify_sql "Âme rattachée au faiseur" "SELECT faiseur_id FROM souls WHERE id='$F_SOUL'" "$FAISEUR_ID"

  echo "▸ Créer un rapport faiseur (FAISEUR) — endpoint corrigé côté mobile"
  MR=$(api POST "$FAISEUR" /reports/maker-weekly "{\"faiseurId\":\"$FAISEUR_ID\",\"ameId\":\"$F_SOUL\",\"semaine\":\"2026-09-07\",\"presencesParCulte\":{\"Dimanche Matin\":true,\"Mercredi Soir\":false},\"nbSorties\":2,\"nbMaintenus\":1,\"notesComplementaires\":\"E2E rapport faiseur\"}")
  MR_ID=$(echo "$MR" | jq -r '.id // empty')
  [ -n "$MR_ID" ] && { ok "POST /reports/maker-weekly → id=$MR_ID"; \
    verify_api "Rapport faiseur lu" "$FAISEUR" "/reports/maker-weekly/$MR_ID" '.ameId' "$F_SOUL"; \
    verify_sql "Rapport faiseur en base" "SELECT ame_id FROM maker_reports WHERE id='$MR_ID'" "$F_SOUL"; } || fail "POST /reports/maker-weekly" "$MR"
}

echo "▸ Créer une demande de suivi (FAISEUR)"
FU_F=$(api POST "$FAISEUR" /follow-up-requests "{\"type\":\"ACCOMPAGNEMENT_SPIRITUEL\",\"message\":\"E2E suivi faiseur $STAMP\"}")
FU_F_ID=$(echo "$FU_F" | jq -r '.id // empty')
[ -n "$FU_F_ID" ] && { ok "POST /follow-up-requests (faiseur) → id=$FU_F_ID"; \
  verify_api "Suivi visible dans /mine" "$FAISEUR" /follow-up-requests/mine ".[] | select(.id==\"$FU_F_ID\") | .message" "E2E suivi faiseur $STAMP"; } || fail "POST /follow-up-requests (faiseur)" "$FU_F"

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 6 — MEMBRE : suivi OK + RBAC (403 attendu)"
echo "════════════════════════════════════════════════════════"

echo "▸ Créer une demande de suivi (MEMBRE)"
FU_M=$(api POST "$MEMBRE" /follow-up-requests "{\"type\":\"FAISEUR\",\"message\":\"E2E suivi membre $STAMP\"}")
FU_M_ID=$(echo "$FU_M" | jq -r '.id // empty')
[ -n "$FU_M_ID" ] && ok "POST /follow-up-requests (membre) → id=$FU_M_ID" || fail "POST /follow-up-requests (membre)" "$FU_M"

echo "▸ RBAC : MEMBRE ne doit PAS créer une âme (403 attendu)"
HTTP_M=$(curl -s -m 10 -o /dev/null -w '%{http_code}' -X POST "$BASE/souls" \
  -H "Authorization: Bearer $MEMBRE" -H "Content-Type: application/json" \
  -d "{\"nom\":\"E2EForbidden$STAMP\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"2026-09-08\"}")
if [ "$HTTP_M" = "403" ] || [ "$HTTP_M" = "401" ]; then ok "RBAC bloqué pour MEMBRE/souls (HTTP $HTTP_M)"; else fail "RBAC MEMBRE/souls non bloqué (HTTP $HTTP_M)" "attendu 403"; fi

echo "▸ RBAC : MEMBRE ne doit PAS créer un utilisateur (403 attendu)"
HTTP_M2=$(curl -s -m 10 -o /dev/null -w '%{http_code}' -X POST "$BASE/users" \
  -H "Authorization: Bearer $MEMBRE" -H "Content-Type: application/json" \
  -d "{\"email\":\"x$STAMP@x.com\",\"firstName\":\"X\",\"lastName\":\"Y\",\"password\":\"password123\",\"role\":\"MEMBRE\"}")
if [ "$HTTP_M2" = "403" ] || [ "$HTTP_M2" = "401" ]; then ok "RBAC bloqué pour MEMBRE/users (HTTP $HTTP_M2)"; else fail "RBAC MEMBRE/users non bloqué (HTTP $HTTP_M2)" "attendu 403"; fi

echo ""
echo "════════════════════════════════════════════════════════"
echo " PHASE 7 — PERSISTANCE GLOBALE (SQL direct)"
echo "════════════════════════════════════════════════════════"
verify_sql "Compte âmes E2E créées" "SELECT COUNT(*) FROM souls WHERE nom LIKE 'E2E%$STAMP%'" "4"
verify_sql "Compte utilisateurs E2E créés" "SELECT COUNT(*) FROM users WHERE email LIKE 'e2e.%$STAMP%'" "4"

echo ""
echo "════════════════════════════════════════════════════════"
echo " RÉSULTAT FINAL : $PASS_N réussis / $FAIL_N échoués"
echo "════════════════════════════════════════════════════════"
if [ $FAIL_N -gt 0 ]; then echo -e "Tests en échec :$FAILED_TESTS"; exit 1; fi
echo "🎉 TOUS LES FLUX DE CRÉATION FONCTIONNENT ET PERSISTENT."

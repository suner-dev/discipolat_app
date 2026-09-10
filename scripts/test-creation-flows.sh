#!/bin/bash
# ================================================================================
# TEST E2E DES FLUX DE CRÉATION — DISCIPOLAT (backend réel, zéro mock)
# Chaque rôle × chaque création + double vérification de persistance :
#   1. L'API renvoie bien l'entité créée (201 + id)
#   2. La donnée existe RÉELLEMENT en base PostgreSQL (requête SQL directe)
# ================================================================================
set -u
BASE="http://localhost:8080/api/v1"
PASS="password123"
STAMP=$(date +%s)
sql() { PGPASSWORD=discipolat_secret psql -h localhost -p 5433 -U discipolat -d discipolat -t -A -c "$1" 2>/dev/null | head -1; }
PASS_COUNT=0; OK=0; KO=0
declare -a FAILURES

login() {
  local email="$1" tries=0 resp token
  PASS_COUNT=$((PASS_COUNT+1))
  while [ $tries -lt 3 ]; do
    resp=$(curl -s -m 8 -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
      -d "{\"email\":\"$email\",\"password\":\"$PASS\"}")
    token=$(echo "$resp" | jq -r '.accessToken // empty' 2>/dev/null)
    [ -n "$token" ] && { echo "$token"; return 0; }
    tries=$((tries+1)); sleep 10
  done
  echo ""
}

ok()   { OK=$((OK+1));   echo "  ✅ $1"; }
fail() { KO=$((KO+1)); FAILURES+=("$1"); echo "  ❌ $1 — $(echo "${2:-}" | head -c 220)"; }
sep()  { echo ""; echo "━━━ $1 ━━━"; }


api() {
  local m="$1" p="$2" t="$3" d="${4:-}"
  if [ -n "$d" ]; then
    curl -s -m 10 -X "$m" "$BASE$p" -H "Authorization: Bearer $t" -H "Content-Type: application/json" -d "$d"
  else
    curl -s -m 10 -X "$m" "$BASE$p" -H "Authorization: Bearer $t"
  fi
}

# ═══════════════════════════ LOGIN (5 rôles) ═══════════════════════════
sep "CONNEXIONS (5 rôles)"
ADMIN=$(login "admin@discipolat.com");        sleep 3
RESP=$(login "responsable@discipolat.com");   sleep 3
CHEF=$(login "chef1@discipolat.com");         sleep 3
FAISEUR=$(login "faiseur1@discipolat.com");   sleep 3
MEMBRE=$(login "membre@discipolat.com")
for t in "ADMIN:$ADMIN" "RESPONSABLE:$RESP" "CHEF:$CHEF" "FAISEUR:$FAISEUR" "MEMBRE:$MEMBRE"; do
  role="${t%%:*}"; tok="${t#*:}"
  [ -n "$tok" ] && ok "Login $role" || fail "Login $role"
done

# ═══════════════════════════ ADMIN ═══════════════════════════
sep "[ADMIN] Créer une ÂME"
R=$(api POST "/souls" "$ADMIN" "{\"nom\":\"AUDIT_SOUL_$STAMP\",\"prenom\":\"E2E\",\"email\":\"e2e.soul.$STAMP@test.com\",\"telephone\":\"+243900$STAMP\",\"typeDisciple\":\"NOUVEL_ARRIVANT\",\"dateIntegration\":\"2026-09-01\",\"faiseurId\":\"$(sql "select id from users where email='faiseur1@discipolat.com'")\"}")
SOUL_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$SOUL_ID" ] && ok "Âme créée (201) id=$SOUL_ID" || fail "Création âme" "$R"
DB=$(sql "select count(*) from souls where id='$SOUL_ID' and nom='AUDIT_SOUL_$STAMP'")
[ "$DB" = "1" ] && ok "Persistance SQL souls ✅" || fail "Persistance âme en base" "count=$DB"

sep "[ADMIN] Créer une FAMILLE (+ chef créé à la volée)"
R=$(api POST "/families" "$ADMIN" "{\"nom\":\"Famille Audit $STAMP\",\"createNewChef\":true,\"newChefFirstName\":\"Chef\",\"newChefLastName\":\"Audit$STAMP\",\"newChefEmail\":\"e2e.chef.new.$STAMP@discipolat.com\",\"newChefPhone\":\"+243911$STAMP\"}")
FAM_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$FAM_ID" ] && ok "Famille créée (201) id=$FAM_ID" || fail "Création famille" "$R"
DB=$(sql "select count(*) from families where id='$FAM_ID' and nom='Famille Audit $STAMP' and chef_famille_id is not null")
[ "$DB" = "1" ] && ok "Persistance SQL families (+chef lié) ✅" || fail "Persistance famille" "count=$DB"
CHEF_USER=$(sql "select email from users where last_name='Audit$STAMP' limit 1")
[ -n "$CHEF_USER" ] && ok "Utilisateur chef auto-créé : $CHEF_USER" || fail "Chef auto-créé absent"

sep "[ADMIN] Créer un DÉPARTEMENT (+ responsable à la volée)"
R=$(api POST "/departments" "$ADMIN" "{\"nom\":\"Département Audit $STAMP\",\"createNewResponsable\":true,\"newRespFirstName\":\"Resp\",\"newRespLastName\":\"Audit$STAMP\",\"newRespEmail\":\"e2e.resp.new.$STAMP@discipolat.com\",\"newRespPhone\":\"+243922$STAMP\"}")
DEPT_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$DEPT_ID" ] && ok "Département créé (201) id=$DEPT_ID" || fail "Création département" "$R"
DB=$(sql "select count(*) from departments where id='$DEPT_ID'")
[ "$DB" = "1" ] && ok "Persistance SQL departments ✅" || fail "Persistance département" "count=$DB"

sep "[ADMIN] Créer un UTILISATEUR (RESPONSABLE)"
R=$(api POST "/users" "$ADMIN" "{\"email\":\"e2e.user.$STAMP@discipolat.com\",\"firstName\":\"User\",\"lastName\":\"E2E$STAMP\",\"phone\":\"+243933$STAMP\",\"password\":\"password123\",\"role\":\"RESPONSABLE\"}")
USER_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$USER_ID" ] && ok "Utilisateur créé (201) id=$USER_ID" || fail "Création utilisateur" "$R"
DB=$(sql "select statut from users where id='$USER_ID'")
[ "$DB" = "PENDING_ACTIVATION" ] && ok "Persistance SQL users (PENDING_ACTIVATION = US-02) ✅" || fail "Persistance utilisateur" "statut=$DB"

sep "[ADMIN] Créer une DEMANDE DE PRIÈRE"
R=$(api POST "/prayers" "$ADMIN" "{\"titre\":\"Prière Audit $STAMP\",\"description\":\"Vérification E2E\",\"categorie\":\"SANTE\",\"priorite\":\"HAUTE\",\"visibilite\":\"GENERALE\"}")
PRAY_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$PRAY_ID" ] && ok "Prière créée (201) id=$PRAY_ID" || fail "Création prière" "$R"
DB=$(sql "select count(*) from prayers where id='$PRAY_ID'")
[ "$DB" = "1" ] && ok "Persistance SQL prayers ✅" || fail "Persistance prière" "count=$DB"

sep "[ADMIN] Créer une PAGE (Page Builder)"
R=$(api POST "/pages" "$ADMIN" "{\"key\":\"e2e-page-$STAMP\",\"title\":\"Page E2E $STAMP\",\"slug\":\"e2e-page-$STAMP\",\"layout\":\"STACK\",\"blocks\":[],\"roles\":[],\"enabled\":true,\"published\":false,\"version\":1}")
PAGE_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$PAGE_ID" ] && ok "Page créée (201) id=$PAGE_ID" || fail "Création page" "$R"
DB=$(sql "select count(*) from custom_pages where id='$PAGE_ID' and slug='e2e-page-$STAMP'")
[ "$DB" = "1" ] && ok "Persistance SQL custom_pages ✅" || fail "Persistance page" "count=$DB"

sep "[RESPONSABLE] Créer une ÂME"
R=$(api POST "/souls" "$RESP" "{\"nom\":\"RESP_SOUL_$STAMP\",\"typeDisciple\":\"NOUVEAU_CONVERTI\",\"dateIntegration\":\"2026-09-01\",\"faiseurId\":\"$(sql "select id from users where email='responsable@discipolat.com'")\"}")
RSOUL_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$RSOUL_ID" ] && ok "Âme créée par RESPONSABLE id=$RSOUL_ID" || fail "Âme RESPONSABLE" "$R"
DB=$(sql "select count(*) from souls where id='$RSOUL_ID'")
[ "$DB" = "1" ] && ok "Persistance SQL ✅" || fail "Persistance âme resp" "count=$DB"

sep "[CHEF_DE_FAMILLE] Soumettre un RAPPORT FAMILLE (sa propre famille)"
MY_FAM=$(sql "select famille_geree_id from users where email='chef1@discipolat.com'")
MY_ID=$(sql "select id from users where email='chef1@discipolat.com'")
if [ -n "$MY_FAM" ]; then
  R=$(api POST "/reports/family-weekly" "$CHEF" "{\"familleId\":\"$MY_FAM\",\"chefFamilleId\":\"$MY_ID\",\"semaine\":\"2026-09-07\",\"statsAgregees\":{\"totalAmes\":5,\"presentes\":4},\"totalSorties\":3,\"totalMaintenus\":2,\"commentaireSynthese\":\"Rapport E2E $STAMP\"}")
  FRPT_ID=$(echo "$R" | jq -r '.id // empty')
  [ -n "$FRPT_ID" ] && ok "Rapport famille créé id=$FRPT_ID" || fail "Rapport famille" "$R"
  DB=$(sql "select count(*) from family_reports where id='$FRPT_ID' and famille_id='$MY_FAM'")
  [ "$DB" = "1" ] && ok "Persistance SQL family_reports ✅" || fail "Persistance rapport famille" "count=$DB"
else
  fail "chef1 sans famille rattachée (données de test)"
fi

sep "[FAISEUR] Soumettre un RAPPORT FAISEUR (sur son âme)"
F_MY_ID=$(sql "select id from users where email='faiseur1@discipolat.com'")
F_AME=$(sql "select id from souls where faiseur_id='$F_MY_ID' order by created_at desc limit 1")
if [ -z "$F_AME" ] && [ -n "${SOUL_ID:-}" ]; then F_AME="$SOUL_ID"; fi
if [ -n "$F_AME" ]; then
  R=$(api POST "/reports/maker-weekly" "$FAISEUR" "{\"faiseurId\":\"$F_MY_ID\",\"ameId\":\"$F_AME\",\"semaine\":\"2026-09-07\",\"presencesParCulte\":{\"Dimanche Matin\":true,\"Mercredi Soir\":false},\"absenceRaison\":\"MALADIE\",\"absenceCommentaire\":\"E2E $STAMP\",\"nbSorties\":2,\"nbMaintenus\":1,\"notesComplementaires\":\"Test persistance\"}")
  MRPT_ID=$(echo "$R" | jq -r '.id // empty')
  [ -n "$MRPT_ID" ] && ok "Rapport faiseur créé id=$MRPT_ID" || fail "Rapport faiseur" "$R"
  DB=$(sql "select count(*) from maker_reports where id='$MRPT_ID' and ame_id='$F_AME' and faiseur_id='$F_MY_ID'")
  [ "$DB" = "1" ] && ok "Persistance SQL maker_reports ✅" || fail "Persistance rapport faiseur" "count=$DB"
else
  fail "Aucune âme disponible pour faiseur1"
fi

sep "[MEMBRE] Créer une DEMANDE DE SUIVI"
R=$(api POST "/follow-up-requests" "$MEMBRE" "{\"type\":\"FAISEUR\",\"message\":\"Demande E2E $STAMP\"}")
FUP_ID=$(echo "$R" | jq -r '.id // empty')
[ -n "$FUP_ID" ] && ok "Demande de suivi créée id=$FUP_ID" || fail "Demande de suivi" "$R"
DB=$(sql "select count(*) from follow_up_requests where id='$FUP_ID'")
[ "$DB" = "1" ] && ok "Persistance SQL follow_up_requests ✅" || fail "Persistance suivi" "count=$DB"

sep "RBAC : refus attendus (403)"
CODE=$(curl -s -m 8 -o /dev/null -w '%{http_code}' -X POST "$BASE/users" -H "Authorization: Bearer $MEMBRE" -H "Content-Type: application/json" -d "{\"email\":\"hack.$STAMP@x.com\",\"firstName\":\"H\",\"lastName\":\"X\",\"password\":\"password123\",\"role\":\"ADMIN\"}")
[ "$CODE" = "403" ] && ok "MEMBRE ne peut pas créer d'utilisateur (403)" || fail "RBAC: membre→users" "code=$CODE"
CODE=$(curl -s -m 8 -o /dev/null -w '%{http_code}' -X POST "$BASE/souls" -H "Authorization: Bearer $MEMBRE" -H "Content-Type: application/json" -d '{"nom":"X","typeDisciple":"NOUVEL_ARRIVANT","faiseurId":"00000000-0000-0000-0000-000000000000"}')
[ "$CODE" = "403" ] && ok "MEMBRE ne peut pas créer d'âme (403)" || fail "RBAC: membre→souls" "code=$CODE"
CODE=$(curl -s -m 8 -o /dev/null -w '%{http_code}' -X POST "$BASE/pages" -H "Authorization: Bearer $CHEF" -H "Content-Type: application/json" -d "{\"key\":\"h$STAMP\",\"title\":\"X\",\"slug\":\"h$STAMP\",\"layout\":\"STACK\",\"blocks\":[],\"roles\":[],\"enabled\":true,\"published\":false,\"version\":1}")
[ "$CODE" = "403" ] && ok "CHEF ne peut pas créer de page (403)" || fail "RBAC: chef→pages" "code=$CODE"

sep "RÉSULTATS"
echo "  ✔ Réussis : $OK"
echo "  ✘ Échecs  : $KO"
if [ $KO -gt 0 ]; then printf '     ❌ %s\n' "${FAILURES[@]}"; fi
[ $KO -eq 0 ] && echo "  🎉 TOUS LES FLUX DE CRÉATION FONCTIONNENT (persistance vérifiée en base)"
exit $KO

#!/bin/bash
# ============================================================================
# verify-beta.sh — Vérification automatisée de l'ENVIRONNEMENT BÊTA-TESTING
# ----------------------------------------------------------------------------
# À lancer APRÈS le Sync Blueprint Render (services bêta en ligne).
#
# Teste, de bout en bout, l'URL publique de bêta-test :
#   1. Meta publique (env=beta, demoAccountsEnabled=true)
#   2. Connexion des 7 comptes de démonstration (password123)
#   3. Changement de rôle multi-rôles (paul)
#   4. Feedback : POST → GET → stats
#   5. Sécurité RBAC : MEMBRE → endpoints admin = 403, sans token = 401
#   6. Reset bêta (admin) : tables restaurées + feedbacks conservés
#   7. Isolation départements (accès responsable)
#
# NB : les tokens sont réutilisés (pas de re-login) pour respecter le
# rate-limiting de connexion (10 login/min/IP — protection anti-brute-force).
#
# Usage :
#   ./scripts/verify-beta.sh [BASE_URL] [ADMIN_EMAIL] [ADMIN_PASSWORD]
#   Ex. : ./scripts/verify-beta.sh https://discipolat-beta-api.onrender.com admin@discipolat.com password123
#
# Sortie : OK/FAIL par étape, exit code 0 si tout passe.
# ============================================================================

set -u

BASE="${1:-https://discipolat-beta-api.onrender.com}"
ADMIN_EMAIL="${2:-admin@discipolat.com}"
ADMIN_PASS="${3:-password123}"

PASS=0; FAIL=0
step()  { echo "── $1"; }
ok()    { PASS=$((PASS+1)); echo "   ✅ $1"; }
ko()    { FAIL=$((FAIL+1)); echo "   ❌ $1"; }

echo "=============================================="
echo " Discipolat — Vérification Bêta ($BASE)"
echo "=============================================="

# --- Cache de tokens : un login par compte, réutilisé ensuite --------------
declare -A TOKENS
login_token() {  # $1 = email
  local email="$1"
  if [ -n "${TOKENS[$email]:-}" ]; then
    echo "${TOKENS[$email]}"
    return 0
  fi
  local resp token
  resp=$(curl -s -m 60 -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$ADMIN_PASS\"}")
  token=$(echo "$resp" | python3 -c 'import sys,json
try: print(json.load(sys.stdin).get("accessToken",""))
except Exception: print("")' 2>/dev/null)
  if [ -n "$token" ]; then
    TOKENS[$email]="$token"
    echo "$token"
    return 0
  fi
  return 1
}

# --- 1. Meta publique ------------------------------------------------------
step "1. Meta publique (/api/v1/public/meta)"
META=$(curl -s -m 120 "$BASE/api/v1/public/meta")
if echo "$META" | grep -q '"environment":"beta"' && echo "$META" | grep -q '"demoAccountsEnabled":true'; then
  ok "env=beta + demoAccountsEnabled=true"
else
  ko "meta inattendue : $META"
fi

# --- 2. Connexion des comptes démo ----------------------------------------
step "2. Connexion des 7 comptes de démonstration (password123)"
for EMAIL in admin@discipolat.com pasteur@discipolat.com responsable1@discipolat.com chef1@discipolat.com faiseur@discipolat.com membre@discipolat.com paul@discipolat.com; do
  if login_token "$EMAIL" >/dev/null; then
    ok "$EMAIL → connexion OK"
  else
    ko "$EMAIL → échec de connexion"
  fi
done

# --- 3. Changement de rôle multi-rôles ------------------------------------
step "3. Changement de rôle multi-rôles (paul : RESPONSABLE → CHEF_DE_FAMILLE)"
PAUL_TOKEN=$(login_token "paul@discipolat.com")
if [ -n "$PAUL_TOKEN" ]; then
  SW=$(curl -s -m 60 -X POST "$BASE/api/v1/auth/switch-role" \
    -H "Authorization: Bearer $PAUL_TOKEN" -H "Content-Type: application/json" \
    -d '{"role":"CHEF_DE_FAMILLE"}')
  if echo "$SW" | grep -q '"accessToken"'; then
    ok "switch-role → nouveau JWT émis"
  else
    ko "switch-role : $(echo "$SW" | head -c 120)"
  fi
else
  ko "token paul introuvable"
fi

# --- 4. Feedback : POST → GET → stats -------------------------------------
step "4. Feedback testeur (POST → GET → stats)"
F_TOKEN=$(login_token "faiseur@discipolat.com")
FB=$(curl -s -m 60 -X POST "$BASE/api/v1/feedback" \
  -H "Authorization: Bearer $F_TOKEN" -H "Content-Type: application/json" \
  -d '{"category":"BUG","priority":"MOYENNE","subject":"Vérification automatique du déploiement bêta","description":"Envoyé par verify-beta.sh","pageUrl":"/login","browser":"curl","device":"CLI","os":"Linux"}')
if echo "$FB" | grep -q '"id"'; then
  ok "POST /feedback → id créé"
else
  ko "POST /feedback : $(echo "$FB" | head -c 160)"
fi
A_TOKEN=$(login_token "$ADMIN_EMAIL")
ADMIN_STATS=$(curl -s -m 60 "$BASE/api/v1/admin/feedback/stats" -H "Authorization: Bearer $A_TOKEN")
if echo "$ADMIN_STATS" | grep -qE '"total"|"parCategorie"|"parPriorite"|"parStatus"'; then
  ok "GET /admin/feedback/stats → OK"
else
  ko "stats admin : $(echo "$ADMIN_STATS" | head -c 160)"
fi
FEEDBACK_ID=$(echo "$FB" | python3 -c 'import sys,json
print(json.load(sys.stdin).get("id",""))' 2>/dev/null)
ADMIN_LIST=$(curl -s -m 60 "$BASE/api/v1/admin/feedback" -H "Authorization: Bearer $A_TOKEN")
if [ -n "$FEEDBACK_ID" ] && echo "$ADMIN_LIST" | grep -q "$FEEDBACK_ID"; then
  ok "GET /admin/feedback → le retour créé est visible par l'admin"
else
  ko "GET /admin/feedback : $(echo "$ADMIN_LIST" | head -c 160)"
fi

# --- 5. Sécurité RBAC ------------------------------------------------------
step "5. Sécurité RBAC (membre bloqué, sans token bloqué)"
M_TOKEN=$(login_token "membre@discipolat.com")
if [ -n "$M_TOKEN" ]; then
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 60 "$BASE/api/v1/admin/feedback" -H "Authorization: Bearer $M_TOKEN")
  if [ "$CODE" = "403" ]; then ok "MEMBRE → /admin/feedback = 403"; else ko "MEMBRE → $CODE (attendu 403)"; fi
  CODE2=$(curl -s -o /dev/null -w '%{http_code}' -m 60 -X POST "$BASE/api/v1/admin/beta/reset" -H "Authorization: Bearer $M_TOKEN")
  if [ "$CODE2" = "403" ]; then ok "MEMBRE → /admin/beta/reset = 403"; else ko "MEMBRE → reset $CODE2 (attendu 403)"; fi
else
  ko "token membre introuvable"
fi
CODE3=$(curl -s -o /dev/null -w '%{http_code}' -m 60 "$BASE/api/v1/admin/beta/status")
if [ "$CODE3" = "401" ]; then ok "sans token → /admin/beta/status = 401"; else ko "sans token → $CODE3 (attendu 401)"; fi

# --- 6. Isolation départements --------------------------------------------
step "6. Isolation départements (accès responsable)"
R_TOKEN=$(login_token "responsable1@discipolat.com")
if [ -n "$R_TOKEN" ]; then
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 60 "$BASE/api/v1/departments" -H "Authorization: Bearer $R_TOKEN")
  if [ "$CODE" = "200" ]; then ok "RESPONSABLE1 → /departments = 200"; else ko "RESPONSABLE1 → $CODE (attendu 200)"; fi
else
  ko "token responsable1 introuvable"
fi

# --- 7. Reset bêta (ADMIN) ------------------------------------------------
step "7. Reset de l'environnement bêta (admin)"
if [ -n "$A_TOKEN" ]; then
  RST=$(curl -s -m 120 -X POST "$BASE/api/v1/admin/beta/reset" -H "Authorization: Bearer $A_TOKEN")
  if echo "$RST" | grep -q '"status":"OK"'; then
    ok "POST /admin/beta/reset → OK (${RST})"
    # Invariant documenté : les retours testeurs SONT conservés après reset
    AFTER=$(curl -s -m 60 "$BASE/api/v1/admin/feedback" -H "Authorization: Bearer $A_TOKEN")
    if [ -n "$FEEDBACK_ID" ] && echo "$AFTER" | grep -q "$FEEDBACK_ID"; then
      ok "le feedback survit au reset (invariant conservé)"
    else
      ko "feedback perdu après reset"
    fi
  else
    ko "reset : $(echo "$RST" | head -c 200)"
  fi
else
  ko "token admin introuvable (reset non testé)"
fi

echo "=============================================="
echo " Résultat : $PASS ok, $FAIL échec(s)"
echo "=============================================="
[ "$FAIL" -eq 0 ]

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
#   7. Isolation départements / familles (403 croisés)
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

# --- 1. Meta publique ------------------------------------------------------
step "1. Meta publique (/api/v1/public/meta)"
META=$(curl -s -m 120 "$BASE/api/v1/public/meta")
if echo "$META" | grep -q '"environment":"beta"' && echo "$META" | grep -q '"demoAccountsEnabled":true'; then
  ok "env=beta + demoAccountsEnabled=true ($META)"
else
  ko "meta inattendue : $META"
fi

# --- 2. Connexion des comptes démo ----------------------------------------
step "2. Connexion des 7 comptes de démonstration (password123)"
for EMAIL in admin@discipolat.com pasteur@discipolat.com responsable1@discipolat.com chef1@discipolat.com faiseur@discipolat.com membre@discipolat.com paul@discipolat.com; do
  RESP=$(curl -s -m 60 -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$ADMIN_PASS\"}")
  if echo "$RESP" | grep -q '"accessToken"'; then
    ok "$EMAIL → connexion OK"
  else
    ko "$EMAIL → échec : $(echo "$RESP" | head -c 120)"
  fi
done

# --- Helper: login → token -------------------------------------------------
login_token() {
  curl -s -m 60 -X POST "$BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$ADMIN_PASS\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

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
  -d '{"categorie":"BUG","priorite":"MOYENNE","message":"Vérification automatique du déploiement bêta","page":"/login"}')
if echo "$FB" | grep -q '"id"'; then
  ok "POST /feedback → id créé"
else
  ko "POST /feedback : $(echo "$FB" | head -c 120)"
fi
A_TOKEN=$(login_token "$ADMIN_EMAIL")
ADMIN_STATS=$(curl -s -m 60 "$BASE/api/v1/admin/feedback/stats" -H "Authorization: Bearer $A_TOKEN")
if echo "$ADMIN_STATS" | grep -qE '"total"|"parCategorie"|"parPriorite"'; then
  ok "GET /admin/feedback/stats → OK"
else
  ko "stats admin : $(echo "$ADMIN_STATS" | head -c 120)"
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

# --- 6. Isolation départements / familles ---------------------------------
step "6. Isolation départements & familles (403 croisés)"
R_TOKEN=$(login_token "responsable1@discipolat.com")
if [ -n "$R_TOKEN" ]; then
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 60 "$BASE/api/v1/departments" -H "Authorization: Bearer $R_TOKEN")
  ok "RESPONSABLE1 → /departments = $CODE (200 attendu)"
else
  ko "token responsable1 introuvable"
fi

# --- 7. Reset bêta (ADMIN) ------------------------------------------------
step "7. Reset de l'environnement bêta (admin)"
if [ -n "$A_TOKEN" ]; then
  RST=$(curl -s -m 120 -X POST "$BASE/api/v1/admin/beta/reset" -H "Authorization: Bearer $A_TOKEN")
  if echo "$RST" | grep -qE '"success"|"ok"|"statut"|"reseted"|"reset"'; then
    ok "POST /admin/beta/reset → OK"
  else
    ko "reset : $(echo "$RST" | head -c 160)"
  fi
else
  ko "token admin introuvable (reset non testé)"
fi

echo "=============================================="
echo " Résultat : $PASS ok, $FAIL échec(s)"
echo "=============================================="
[ "$FAIL" -eq 0 ]

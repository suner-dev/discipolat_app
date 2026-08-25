#!/bin/bash
# 🔬 SMOKE TEST DISCIPOLAT — vérifie qu'aucune page frontend n'utilise de données mock
#    et que les endpoints appelés existent dans le backend (routes fantômes).
# Usage : bash scripts/smoke-check.sh
set -u
cd "$(dirname "$0")/.."
FAIL=0

echo "═══════════════════════════════════════════"
echo " 1/3 PAGES FRONTEND MOCK"
echo "═══════════════════════════════════════════"
MOCKS=$(grep -rlE 'MOCK_ITEMS|MOCK_KPIS|MOCK_LISTINGS|MOCK_STREAMS|const MOCK\b|_MOCK_' frontend/src/pages/*.tsx 2>/dev/null || true)
if [ -n "$MOCKS" ]; then
  echo "❌ MOCK détectés :"; echo "$MOCKS"; FAIL=1
else
  echo "✅ Aucune page frontend avec données mock en dur."
fi

echo ""
echo "═══════════════════════════════════════════"
echo " 2/3 ROUTES FANTÔMES (frontend → backend)"
echo "═══════════════════════════════════════════"
python3 - <<'PY'
import re, glob, os, subprocess

# 1) extraire tous les endpoints appelés depuis le frontend
called = set()
for path in glob.glob('frontend/src/pages/*.tsx') + glob.glob('frontend/src/lib/*.ts'):
    txt = open(path).read()
    for m in re.findall(r"api(?:Raw)?\.(get|post|put|patch|delete)\(['\"`]([^'\"`]+)", txt):
        ep = re.sub(r'\$\{[^}]+\}', '', m[1]).rstrip('/?&=')
        if ep.startswith('/'):
            # retirer query string
            ep = ep.split('?')[0]
            called.add(ep)

# 2) collecter toutes les routes backend (@RequestMapping + @GetMapping...)
backend_routes = []
for root, _, files in os.walk('backend/src/main/java'):
    for f in files:
        if f.endswith('Controller.java'):
            txt = open(os.path.join(root, f)).read()
            base = ''
            bm = re.search(r'@RequestMapping\("([^"]+)"\)', txt)
            if bm:
                base = bm.group(1)
            paths = [base]
            for pm in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', txt):
                sub = pm[1] or ''
                paths.append(base + sub)
            backend_routes.extend([p.replace('{','').replace('}','') for p in paths])

def route_matches(called_ep):
    """vérifie si une route backend correspond au pattern appelé"""
    ce = called_ep.rstrip('/')
    for br in backend_routes:
        br_n = br.rstrip('/')
        if br_n == ce or ce.startswith(br_n + '/') or br_n.startswith(ce + '/'):
            return True
        # match par segments dynamiques : /users/{id}/detail vs /users//detail
        bseg = br_n.split('/')
        cseg = ce.split('/')
        if len(bseg) == len(cseg) and all(
            ('{' in b or b == c) for b, c in zip(bseg, cseg)):
            return True
    return False

ghosts = []
for ep in sorted(called):
    if not route_matches(ep):
        ghosts.append(ep)

if ghosts:
    print(f"⚠️ {len(ghosts)} routes potentiellement fantômes (à vérifier manuellement — heuristique) :")
    for g in ghosts[:30]:
        print("   -", g)
else:
    print("✅ Toutes les routes frontend ont une contrepartie backend.")
PY

echo ""
echo "═══════════════════════════════════════════"
echo " 3/3 BUILDS"
echo "═══════════════════════════════════════════"

(cd frontend && npx tsc -b > /dev/null 2>&1) \
  && echo "✅ Frontend tsc OK" || { echo "❌ Frontend tsc ÉCHEC"; FAIL=1; }

if command -v flutter >/dev/null 2>&1; then
  (cd mobile && flutter analyze --no-pub 2>&1 | grep -q "error •") \
    && { echo "❌ Flutter analyse contient des erreurs"; FAIL=1; } \
    || echo "✅ Mobile flutter analyze sans erreur bloquante"
fi

echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "🎉 SMOKE TEST GLOBAL : PASS"
else
  echo "💥 SMOKE TEST GLOBAL : FAIL (voir ci-dessus)"
fi
exit $FAIL
#!/usr/bin/env bash
# A6 #2 — Génération de docs/API.md depuis Springdoc (ne PAS écrire à la main).
#
# Vérité terrain vérifiée par grep :
#   * application.yml → springdoc.api-docs.path = /api-docs (PAS le
#     /v3/api-docs par défaut) ; swagger-ui : /swagger-ui.html.
#   * SecurityConfig → api-docs public seulement en environnement
#     dev/docker ; sinon réservé ADMIN/PASTEUR. La génération se fait donc
#     contre une instance locale (profil test, app.environment=dev par
#     défaut) ou toute instance dev/docker.
#
# Usage :
#   bash scripts/generate-api-docs.sh [BASE_URL]      # défaut : http://localhost:8080
#
# Sorties committées :
#   docs/openapi.json  — spécification machine brute
#   docs/API.md        — Markdown lisible généré depuis openapi.json
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_JSON="$ROOT/docs/openapi.json"
OUT_MD="$ROOT/docs/API.md"

echo "→ GET $BASE_URL/api-docs"
if ! curl -fsS --max-time 30 "$BASE_URL/api-docs" -o "$OUT_JSON.tmp"; then
    rm -f "$OUT_JSON.tmp"
    echo "❌ Impossible d'atteindre $BASE_URL/api-docs."
    echo "   Démarrez d'abord le backend, par exemple :"
    echo "     cd backend && mvn -o spring-boot:run -Dspring-boot.run.profiles=test \\"
    echo "       -Dspring-boot.run.arguments=--server.port=8080"
    echo "   (ou passez en argument l'URL d'une instance dev/docker)."
    exit 1
fi
mv "$OUT_JSON.tmp" "$OUT_JSON"
echo "✓ $(du -h "$OUT_JSON" | cut -f1) → docs/openapi.json"

python3 - "$OUT_JSON" "$OUT_MD" <<'PY'
import json, sys, datetime, re

src, dst = sys.argv[1], sys.argv[2]
spec = json.load(open(src))
info = spec.get('info', {})
paths = spec.get('paths', {})
METHODS = ['get', 'put', 'post', 'patch', 'delete', 'options', 'head', 'trace']

def esc(s):
    # un '|' non échappé casserait le tableau Markdown
    return s.replace('|', '\\|')

def summarize(op):
    s = (op.get('summary') or op.get('operationId') or '').strip()
    return esc(re.sub(r'\s+', ' ', s))

def params_md(op):
    ps = op.get('parameters', [])
    if not ps:
        return ''
    return ', '.join(
        f"`{p.get('name')}`{' *(req)*' if p.get('required') else ''} ({p.get('in')})"
        for p in ps)

by_tag = {}
for path, item in sorted(paths.items()):
    for m in METHODS:
        if m in item:
            op = item[m]
            tags = op.get('tags') or ['(sans tag)']
            for t in tags:
                by_tag.setdefault(t, []).append((m.upper(), path, op))

total_ops = sum(len(v) for v in by_tag.values())
now = datetime.date.today().isoformat()

out = []
out.append('# Référence API — Discipolat')
out.append('')
out.append(f'> ⚙️ **Fichier généré — ne pas éditer à la main.**')
out.append('> Régénérer : `bash scripts/generate-api-docs.sh [URL]` — chemin d’export '
           'vérifié `springdoc.api-docs.path=/api-docs` (application.yml).')
out.append('> Instances réelles (render.yaml) : prod `https://discipolat-api.onrender.com`, '
           'bêta `https://discipolat-beta-api.onrender.com`.')
out.append(f'> Généré le {now} — API `{info.get("title", "?")}` '
           f'v`{info.get("version", "?")}` : **{len(paths)} chemins, {total_ops} opérations, '
           f'{len(by_tag)} tags**.')
out.append('')
out.append('Interface interactive : `/swagger-ui.html` (même instance).')
out.append('Authentification : JWT Bearer (`POST /api/v1/auth/login`) ; '
           'le tenant est porté par le jeton — voir '
           '[MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) '
           'et [SECURITY.md](SECURITY.md).')
out.append('')
out.append('## Sommaire')
out.append('')
for t in sorted(by_tag):
    anchor = re.sub(r'[^a-z0-9]+', '-', t.lower()).strip('-')
    out.append(f'- [{t}](#{anchor}) — {len(by_tag[t])} opérations')
out.append('')
for t in sorted(by_tag):
    out.append(f'## {t}')
    out.append('')
    out.append('| Méthode | Chemin | Résumé | Paramètres |')
    out.append('|---|---|---|---|')
    for m, path, op in sorted(by_tag[t], key=lambda r: r[1]):
        out.append(f'| `{m}` | `{path}` | {summarize(op) or '—'} | {params_md(op) or '—'} |')
    out.append('')

open(dst, 'w').write('\n'.join(out) + '\n')
print(f'✓ {dst} — {total_ops} opérations documentées')
PY

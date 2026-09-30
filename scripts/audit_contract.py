#!/usr/bin/env python3
"""Audit contract: routes backend ( parsing @RequestMapping de classe + mappings de méthodes )
vs appels clients mobile/frontend, avec fichier:ligne de l'appelant."""
import json, re, os, glob

# --- 1. routes backend réelles ---
routes = set()
for f in glob.glob('backend/src/main/java/**/*Controller.java', recursive=True):
    txt = open(f, encoding='utf-8', errors='ignore').read()
    m = re.search(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"', txt)
    base = m.group(1) if m else ''
    for mm in re.finditer(r'@(?:Get|Post|Put|Patch|Delete)Mapping\(\s*(?:value\s*=\s*)?"([^"]*)"', txt):
        p = mm.group(1)
        full = (base.rstrip('/') + ('/' + p.lstrip('/') if p else '')) or base
        routes.add(full)
    # méthodes sans chemin (@GetMapping sans parenthese)
    for mm in re.finditer(r'@(?:Get|Post|Put|Patch|Delete)Mapping\s*(?:\n|\r)\s*(?:@\w+[^\n]*\n\s*)*(?:public|private)', txt):
        pass
rxt = []
for p in routes:
    rxt.append(re.compile('^' + re.sub(r'\{[^}]+\}', '[^/]+', p) + '$'))

# openapi en complément (chemins fins non capturees par mon parseur)
try:
    for p in json.load(open('docs/openapi.json'))['paths'].keys():
        if not any(re.sub(r'\{[^}]+\}', 'ZZZ', p) == re.sub(r'\{[^}]+\}', 'ZZZ', q) for q in routes):
            rxt.append(re.compile('^' + re.sub(r'\{[^}]+\}', '[^/]+', p) + '$'))
except Exception:
    pass

def exists(cp):
    cp = cp.split('?')[0].rstrip('/')
    cands = [cp] if cp.startswith('/api') else ['/api/v1' + cp, cp]
    for c in cands:
        if any(r.match(c) for r in rxt):
            return True
    return False

CALL = re.compile(r"(?:_api|api|axios|client|http(?:Client)?|dio|restClient)\s*\.\s*(?:get|post|put|patch|delete)(?:<[^>]+>)?\s*\(\s*[`'\"](/[a-zA-Z][^`'\"]*)[`\"']")

def norm(p):
    p = re.sub(r'\$\{[^}]*\}', '{X}', p)
    p = re.sub(r'\$\{[^}]*', '{X}', p)   # interpolations tronquees
    p = re.sub(r'\$[a-zA-Z_][a-zA-Z0-9_\[\].]*', '{X}', p)
    p = re.sub(r"'.*", '', p)            # restoie apres une quote perdue
    p = p.replace('{}', '{X}')
    return p

def scan(roots, exts):
    bad = []
    seen = set()
    for root in roots:
        for dirpath, _, files in os.walk(root):
            for fn in files:
                if not fn.endswith(exts):
                    continue
                fp = os.path.join(dirpath, fn)
                txt = open(fp, encoding='utf-8', errors='ignore').read()
                for m in CALL.finditer(txt):
                    p = norm(m.group(1))
                    if '{X}' in p:
                        p2 = re.sub(r'\{X\}', 'SEG', p)
                        p3 = re.sub(r'\{X\}+', 'SEG', p)
                    else:
                        p2 = p3 = p
                    ok = exists(p2) or exists(p3) or exists(re.sub(r'/SEG/SEG', '/SEG/SEG', p2))
                    # tolerance: route existante dont le prefixe fixe couvre (client coupe avant /{id}/sub)
                    if not ok:
                        pre = p2.replace('/SEG', '').rstrip('/')
                        ok = any(r.match(p2) for r in rxt)
                        if not ok:
                            for r in rxt:
                                pass
                    if not ok:
                        key = (p, fp)
                        if key not in seen:
                            seen.add(key)
                            line = txt[:m.start()].count('\n') + 1
                            bad.append((p, f'{fp}:{line}'))
    return bad

for name, roots, exts in [('MOBILE', ['mobile/lib'], ('.dart',)), ('FRONTEND', ['frontend/src'], ('.ts', '.tsx'))]:
    bad = scan(roots, exts)
    paths_bad = sorted(set(p for p, _ in bad))
    print(f'== {name}: {len(bad)} sites, {len(paths_bad)} chemins distincts sans route')
    for p in paths_bad:
        locs = [l for pp, l in bad if pp == p][:2]
        print(f'   {p}   ({", ".join(locs)})')

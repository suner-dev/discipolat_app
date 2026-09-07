#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Backfill the 6 web dictionaries with the extracted French UI corpus.

For each corpus string (French, source-as-key):
  * en/pt/es/sw/ar: translate via (1) whole-phrase glossary, (2) per-word
    glossary composition, (3) pattern templates, (4) French fallback.
  * fr: the key maps to the French text itself.
Keys whose French value already exists in the dictionaries are skipped
(tText() resolves by value and must stay unambiguous).
"""
import json, re, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from l10n_gloss import GLOSSARIES

I18N = Path('frontend/src/i18n')
LANGS = ['fr', 'en', 'pt', 'es', 'sw', 'ar']
OTHER = ['en', 'pt', 'es', 'sw', 'ar']

corpus = json.load(open('/tmp/corpus_web.json'))

# Existing per-language dictionaries (key -> fr value) and fr value set.
existing = {}
for lang in LANGS:
    text = (I18N / f'{lang}.ts').read_text(encoding='utf-8')
    existing[lang] = dict(re.findall(r"^\s*'((?:[^'\\]|\\.)*)':\s*'((?:[^'\\]|\\.)*)',?\s*$", text, re.M))
    # strip escape sequences for comparison
fr_values = set(existing['fr'].values())

LOWER = {k.lower(): v for k, v in GLOSSARIES.items()}

TEMPLATES = {
    # generic pattern -> per language replacement (uses captured groups)
    'fr': [
        (r'^Erreur lors de la (création|suppression|mise à jour|récupération|transmission)( des .*)?$',
         {'en': r'Error \1\2', 'pt': r'Erro ao \1\2', 'es': r'Error al \1\2', 'sw': r'Hitilafu ya \1\2', 'ar': r'خطأ في \1\2'}),
    ],
}

def esc(s):
    return s.replace('\\', r'\\').replace("'", r"\'")

def word_translate(text, lang):
    gloss = GLOSSARIES[lang]
    low2 = {k.lower(): v for k, v in gloss.items()}
    words = text.split(' ')
    out = []
    used = False
    for w in words:
        arrow = ''
        core = w
        while core and core[0] in '←→':
            arrow += core[0]; core = core[1:]
        key = core.lower()
        if key in low2:
            out.append(arrow + low2[key]); used = True
        else:
            out.append(w)
    return ' '.join(out), used

def translate(s, lang):
    gloss = GLOSSARIES[lang]
    # 1) exact whole phrase
    if s in gloss:
        return gloss[s]
    if s.lower() in {k.lower() for k in gloss}:
        for k, v in gloss.items():
            if k.lower() == s.lower():
                return v
    # 2) word composition
    composed, used = word_translate(s, lang)
    if used:
        return composed
    # 3) fallback French
    return s

stats = {l: 0 for l in OTHER}
added = 0
skipped = 0
for s in sorted(corpus):
    if s in fr_values:
        skipped += 1
        continue
    fr_values.add(s)
    added += 1

# ---------- Build FR block and per-language blocks ----------
fr_entries = []
by_lang = {l: [] for l in OTHER}
fr_new = []
for s in sorted(corpus):
    if s in existing['fr'].values():
        continue
    fr_entries.append((esc(s), s))
    if s in existing['fr'].values():
        continue
    for lang in OTHER:
        tr = translate(s, lang)
        if tr != s:
            stats[lang] += 1
        by_lang[lang].append((esc(s), tr))
    fr_new.append(s)

def splice(lang, entries):
    path = I18N / f'{lang}.ts'
    text = path.read_text(encoding='utf-8')
    block = []
    for k, v in entries:
        block.append(f"  '{k}': '{esc(v)}',")
    new = '\n'.join(['\n  // === Auto-extracted web UI (source FR → keys) ==='] + block)
    marker = '\n};'
    idx = text.rindex(marker)
    text = text[:idx] + new + text[idx:]
    path.write_text(text, encoding='utf-8')

# Append fr entries first (fr values become keys)
splice('fr', fr_entries)
for lang in OTHER:
    splice(lang, by_lang[lang])

print(f"added keys: {added} (pre-skip) | fr entries written: {len(fr_entries)}")
for lang in OTHER:
    print(f"  {lang}: translated {stats[lang]}/{len(fr_entries)} ({100*stats[lang]//max(1,len(fr_entries))}%)")
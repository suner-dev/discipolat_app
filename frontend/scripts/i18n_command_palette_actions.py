#!/usr/bin/env python3
"""G5.1 — Ajoute les clés d'actions/voir-tout après 'commandPalette.trigger'."""
import io, re, os, json

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

ADDITIONS = {
    "fr": {"groupActions": "Actions", "actionNewSoul": "Nouvelle âme", "actionOpenSearch": "Recherche avancée", "seeAllResults": "Voir tous les résultats"},
    "en": {"groupActions": "Actions", "actionNewSoul": "New person", "actionOpenSearch": "Advanced search", "seeAllResults": "See all results"},
    "pt": {"groupActions": "Ações", "actionNewSoul": "Nova pessoa", "actionOpenSearch": "Pesquisa avançada", "seeAllResults": "Ver todos os resultados"},
    "es": {"groupActions": "Acciones", "actionNewSoul": "Nueva alma", "actionOpenSearch": "Búsqueda avanzada", "seeAllResults": "Ver todos los resultados"},
    "sw": {"groupActions": "Vitendo", "actionNewSoul": "Mtu mpya", "actionOpenSearch": "Tafuta kina", "seeAllResults": "Ona matokeo yote"},
    "ar": {"groupActions": "إجراءات", "actionNewSoul": "شخص جديد", "actionOpenSearch": "بحث متقدم", "seeAllResults": "عرض كل النتائج"},
}
ORDER = ["groupActions", "actionNewSoul", "actionOpenSearch", "seeAllResults"]

for loc, kv in ADDITIONS.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "commandPalette.seeAllResults" in content:
        print(f"{loc}: already present, skip"); continue
    block = "".join("  'commandPalette.%s': %s,\n" % (k, json.dumps(kv[k], ensure_ascii=False)) for k in ORDER)
    m = re.search(r"( *)'commandPalette\.trigger':\s*(['\"])(?:.*?)\2,\n", content)
    if not m:
        print(f"{loc}: anchor commandPalette.trigger not found!"); continue
    idx = m.end()
    new = content[:idx] + block + content[idx:]
    with io.open(path, "w", encoding="utf-8") as fh:
        fh.write(new)
    print(f"{loc}: inserted {len(ORDER)} keys")

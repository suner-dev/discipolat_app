#!/usr/bin/env python3
"""G5.1 — Insère les clés commandPalette.* après la ligne 'nav.logout' de chaque dictionnaire."""
import io, re, os

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

BLOCKS = {
    "fr": {
        "title": "Recherche globale",
        "placeholder": "Rechercher une personne, une page, une action…",
        "results": "Résultats",
        "noResults": "Aucun résultat",
        "startTyping": "Commencez à taper…",
        "groupPeople": "Personnes",
        "trigger": "Rechercher",
    },
    "en": {
        "title": "Global search",
        "placeholder": "Search a person, a page, an action…",
        "results": "Results",
        "noResults": "No results",
        "startTyping": "Start typing…",
        "groupPeople": "People",
        "trigger": "Search",
    },
    "pt": {
        "title": "Busca global",
        "placeholder": "Pesquisar pessoa, página ou ação…",
        "results": "Resultados",
        "noResults": "Nenhum resultado",
        "startTyping": "Comece a digitar…",
        "groupPeople": "Pessoas",
        "trigger": "Pesquisar",
    },
    "es": {
        "title": "Búsqueda global",
        "placeholder": "Buscar una persona, una página, una acción…",
        "results": "Resultados",
        "noResults": "Sin resultados",
        "startTyping": "Empieza a escribir…",
        "groupPeople": "Personas",
        "trigger": "Buscar",
    },
    "sw": {
        "title": "Tafuta kote",
        "placeholder": "Tafuta mtu, ukurasa, kitendo…",
        "results": "Matokeo",
        "noResults": "Hakuna matokeo",
        "startTyping": "Anza kuandika…",
        "groupPeople": "Watu",
        "trigger": "Tafuta",
    },
    "ar": {
        "title": "بحث شامل",
        "placeholder": "ابحث عن شخص أو صفحة أو إجراء…",
        "results": "النتائج",
        "noResults": "لا توجد نتائج",
        "startTyping": "ابدأ الكتابة…",
        "groupPeople": "أشخاص",
        "trigger": "بحث",
    },
}

ORDER = ["title", "placeholder", "results", "noResults", "startTyping", "groupPeople", "trigger"]

for loc, kv in BLOCKS.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "commandPalette.title" in content:
        print(f"{loc}: already present, skip")
        continue
    block = "".join(
        "  'commandPalette.%s': %s,\n" % (k, __import__("json").dumps(kv[k], ensure_ascii=False))
        for k in ORDER
    )
    # Insère après la ligne 'nav.logout': '...',
    m = re.search(r"( *)'nav\.logout':\s*(['\"])(?:.*?)\2,\n", content)
    if not m:
        print(f"{loc}: anchor 'nav.logout' not found!")
        continue
    idx = m.end()
    new = content[:idx] + "\n  // Command palette (G5.1)\n" + block + content[idx:]
    with io.open(path, "w", encoding="utf-8") as fh:
        fh.write(new)
    print(f"{loc}: inserted {len(ORDER)} keys")

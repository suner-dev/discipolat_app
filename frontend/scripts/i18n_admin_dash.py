#!/usr/bin/env python3
"""G5.5 (§58) — Clés admin.* pour la console tableau de bord tenant admin."""
import io, re, os, json

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

FR = {
    "admin.dashTitle": "Tableau de bord — {name}",
    "admin.loading": "Chargement…",
    "admin.statTotalUsers": "Total Utilisateurs",
    "admin.statActive": "actifs",
    "admin.statMembers": "Membres",
    "admin.statChurches": "Églises",
    "admin.statDepartments": "Départements",
    "admin.statSubChurches": "Sous-églises",
    "admin.statCampuses": "Campus",
    "admin.statGroups": "Groupes",
    "admin.statByRole": "Membres par rôle",
}
EN = {
    "admin.dashTitle": "Dashboard — {name}",
    "admin.loading": "Loading…",
    "admin.statTotalUsers": "Total users",
    "admin.statActive": "active",
    "admin.statMembers": "Members",
    "admin.statChurches": "Churches",
    "admin.statDepartments": "Departments",
    "admin.statSubChurches": "Sub-churches",
    "admin.statCampuses": "Campuses",
    "admin.statGroups": "Groups",
    "admin.statByRole": "Members by role",
}
PT = {
    "admin.dashTitle": "Painel — {name}",
    "admin.loading": "Carregando…",
    "admin.statTotalUsers": "Total de usuários",
    "admin.statActive": "ativos",
    "admin.statMembers": "Membros",
    "admin.statChurches": "Igrejas",
    "admin.statDepartments": "Departamentos",
    "admin.statSubChurches": "Sub-igrejas",
    "admin.statCampuses": "Campi",
    "admin.statGroups": "Grupos",
    "admin.statByRole": "Membros por função",
}
ES = {
    "admin.dashTitle": "Panel — {name}",
    "admin.loading": "Cargando…",
    "admin.statTotalUsers": "Usuarios totales",
    "admin.statActive": "activos",
    "admin.statMembers": "Miembros",
    "admin.statChurches": "Iglesias",
    "admin.statDepartments": "Departamentos",
    "admin.statSubChurches": "Sub-iglesias",
    "admin.statCampuses": "Campus",
    "admin.statGroups": "Grupos",
    "admin.statByRole": "Miembros por rol",
}
SW = {
    "admin.dashTitle": "Dashibodi — {name}",
    "admin.loading": "Inapakia…",
    "admin.statTotalUsers": "Jumla ya watumiaji",
    "admin.statActive": "hai",
    "admin.statMembers": "Wanachama",
    "admin.statChurches": "Makanisa",
    "admin.statDepartments": "Idiwani",
    "admin.statSubChurches": "Makanisa madogo",
    "admin.statCampuses": "Vikampusi",
    "admin.statGroups": "Vikundi",
    "admin.statByRole": "Wanachama kwa chezo",
}
AR = {
    "admin.dashTitle": "لوحة التحكم — {name}",
    "admin.loading": "جارٍ التحميل…",
    "admin.statTotalUsers": "إجمالي المستخدمين",
    "admin.statActive": "نشطون",
    "admin.statMembers": "الأعضاء",
    "admin.statChurches": "الكنائس",
    "admin.statDepartments": "الأقسام",
    "admin.statSubChurches": "الكنائس الفرعية",
    "admin.statCampuses": "الحرماء",
    "admin.statGroups": "المجموعات",
    "admin.statByRole": "الأعضاء حسب الدور",
}

ALL = {"fr": FR, "en": EN, "pt": PT, "es": ES, "sw": SW, "ar": AR}
ANCHOR = "admin.spacesEmpty"

for loc, kv in ALL.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "'admin.dashTitle'" in content:
        print(f"{loc}: already present, skip"); continue
    m = re.search(r"( *)'%s':\s*(['\"])(?:.*?)\2,\n" % re.escape(ANCHOR), content)
    if not m:
        print(f"{loc}: anchor {ANCHOR} not found!"); continue
    indent = m.group(1)
    block = "".join("%s'%s': %s,\n" % (indent, k, json.dumps(kv[k], ensure_ascii=False)) for k in FR.keys())
    idx = m.end()
    new = content[:idx] + block + content[idx:]
    with io.open(path, "w", encoding="utf-8") as fh:
        fh.write(new)
    print(f"{loc}: inserted {len(FR)} keys")

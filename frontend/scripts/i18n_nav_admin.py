#!/usr/bin/env python3
"""G5.5 (§58) — Clés nav.* pour la console tenant admin (Sidebar + CommandPalette)."""
import io, re, os, json

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

FR = {
    "nav.adminOrgConsole": "Console organisation",
    "nav.adminOrgMembers": "Membres de l'organisation",
    "nav.adminRoles": "Rôles & permissions",
    "nav.adminInvitations": "Invitations",
    "nav.adminSpaces": "Espaces de travail",
    "nav.tenantSettings": "Paramètres de l'église",
    "nav.tenantModules": "Modules de l'église",
}
EN = {
    "nav.adminOrgConsole": "Organization console",
    "nav.adminOrgMembers": "Organization members",
    "nav.adminRoles": "Roles & permissions",
    "nav.adminInvitations": "Invitations",
    "nav.adminSpaces": "Workspaces",
    "nav.tenantSettings": "Church settings",
    "nav.tenantModules": "Church modules",
}
PT = {
    "nav.adminOrgConsole": "Console da organização",
    "nav.adminOrgMembers": "Membros da organização",
    "nav.adminRoles": "Funções e permissões",
    "nav.adminInvitations": "Convites",
    "nav.adminSpaces": "Espaços de trabalho",
    "nav.tenantSettings": "Configurações da igreja",
    "nav.tenantModules": "Módulos da igreja",
}
ES = {
    "nav.adminOrgConsole": "Consola de organización",
    "nav.adminOrgMembers": "Miembros de la organización",
    "nav.adminRoles": "Roles y permisos",
    "nav.adminInvitations": "Invitaciones",
    "nav.adminSpaces": "Espacios de trabajo",
    "nav.tenantSettings": "Ajustes de la iglesia",
    "nav.tenantModules": "Módulos de la iglesia",
}
SW = {
    "nav.adminOrgConsole": "Konsoli ya shirika",
    "nav.adminOrgMembers": "Wanachama wa shirika",
    "nav.adminRoles": "Chezo na ruhusa",
    "nav.adminInvitations": "Mialiko",
    "nav.adminSpaces": "Nafasi za kazi",
    "nav.tenantSettings": "Mipangilio ya kanisa",
    "nav.tenantModules": "Moduli za kanisa",
}
AR = {
    "nav.adminOrgConsole": "وحدة تحكم المنظمة",
    "nav.adminOrgMembers": "أعضاء المنظمة",
    "nav.adminRoles": "الأدوار والأذونات",
    "nav.adminInvitations": "الدعوات",
    "nav.adminSpaces": "مساحات العمل",
    "nav.tenantSettings": "إعدادات الكنيسة",
    "nav.tenantModules": "وحدات الكنيسة",
}

ALL = {"fr": FR, "en": EN, "pt": PT, "es": ES, "sw": SW, "ar": AR}
ANCHOR = "nav.feedback"

for loc, kv in ALL.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "'nav.adminOrgConsole'" in content:
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

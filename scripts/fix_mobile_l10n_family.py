#!/usr/bin/env python3
"""Réparation HEAD — clés 'family space' manquantes dans app_localizations.dart.

family_space_screen.dart référence 9 getters inexistants (l10n.family,
familySpaceTitle, familyError, members, activitiesThisMonth, familyMembers,
recentActivity, noRecentActivity, activity) et le map FR était corrompu par
des entrées anglaises orphelines. Ajoute les clés dans les 6 locales + getters.
"""
import io, re

PATH = "/home/arise/discipolat/discipolat_app/mobile/lib/l10n/app_localizations.dart"

KEYS = ["familyError", "family", "familySpaceTitle", "members",
        "activitiesThisMonth", "familyMembers", "recentActivity",
        "noRecentActivity", "activity"]

LOCALES = ["fr", "en", "pt", "es", "sw", "ar"]
VALUES = {
    "fr": ["Erreur lors du chargement de l'espace famille.", "Famille", "Espace famille",
           "Membres", "Activités ce mois-ci", "Membres de la famille",
           "Activité récente", "Aucune activité récente", "Activité"],
    # _english contient déjà familyError/family/familySpaceTitle → 6 clés seulement
    "en": [None, None, None, "Members", "Activities this month", "Family members",
           "Recent activity", "No recent activity", "Activity"],
    "pt": ["Erro ao carregar o espaço familiar.", "Família", "Espaço familiar",
           "Membros", "Atividades deste mês", "Membros da família",
           "Atividade recente", "Nenhuma atividade recente", "Atividade"],
    "es": ["Error al cargar el espacio familiar.", "Familia", "Espacio familiar",
           "Miembros", "Actividades este mes", "Miembros de la familia",
           "Actividad reciente", "Sin actividad reciente", "Actividad"],
    "sw": ["Hitilafu kupakia eneo la familia.", "Familia", "Eneo la Familia",
           "Wanachama", "Shughuli mwezi huu", "Wanachama wa familia",
           "Shughuli ya hivi karibuni", "Hakuna shughuli za hivi karibuni", "Shughuli"],
    "ar": ["تعذر تحميل مساحة العائلة.", "العائلة", "مساحة العائلة", "الأعضاء",
           "أنشطة هذا الشهر", "أفراد العائلة", "نشاط حديث",
           "لا يوجد نشاط حديث", "نشاط"],
}


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")


with io.open(PATH, "r", encoding="utf-8") as fh:
    content = fh.read()

if "'familySpaceTitle':" in content.split("_english")[0]:
    pass  # déjà inséré côté FR ? on vérifie plus bas via le compte d'ancres

# 1) entrées de dictionnaires — après chaque 'voiceCmd6' (6 sections, ordre LOCALES)
anchors = list(re.finditer(r"( *)'voiceCmd6': .*,\n", content))
assert len(anchors) == 6, f"expected 6 anchors, got {len(anchors)}"
offset = 0
for m, loc in zip(anchors, LOCALES):
    indent = m.group(1)
    pairs = [(k, v) for k, v in zip(KEYS, VALUES[loc]) if v is not None]
    block = "".join("%s'%s': '%s',\n" % (indent, k, esc(v)) for k, v in pairs)
    idx = m.end() + offset
    content = content[:idx] + block + content[idx:]
    offset += len(block)
    print(f"map {loc}: +{len(pairs)}")

# 2) getters — après le getter familyMeetingEmpty (unique)
gm = re.search(r"( *)String get familyMeetingEmpty => translate\('familyMeetingEmpty'\);\n", content)
assert gm, "getter anchor not found"
indent = gm.group(1)
# famille de getters déjà présents ?
missing = [k for k in KEYS if not re.search(r"String get %s( |=>)" % re.escape(k), content)]
gblock = "".join("%sString get %s => translate('%s');\n" % (indent, k, k) for k in missing)
idx = gm.end()
content = content[:idx] + gblock + content[idx:]
print("getters: +%d %s" % (len(missing), missing))

with io.open(PATH, "w", encoding="utf-8") as fh:
    fh.write(content)
print("done")

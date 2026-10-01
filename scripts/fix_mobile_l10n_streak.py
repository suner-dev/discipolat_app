#!/usr/bin/env python3
"""Réparation HEAD — clés streak (fr/en seuls) complétées pt/es/sw/ar + getters.

streak_screen.dart réécrit proprement utilise streakError/streakTitle/
currentStreak/streakDay/streakDays/keepItUp : clés présentes uniquement dans
les maps fr et en, getters absents. Ajoute les 4 locales manquantes + getters.
"""
import io, re

PATH = "/home/arise/discipolat/discipolat_app/mobile/lib/l10n/app_localizations.dart"

KEYS = ["streakError", "streakTitle", "currentStreak", "streakDay", "streakDays", "keepItUp"]
VALUES = {
    "pt": ["Erro ao buscar dados de sequência.", "Estatísticas de sequência",
           "Sequência atual", "dia", "dias", "Continue assim"],
    "es": ["Error al obtener datos de la racha.", "Estadísticas de racha",
           "Racha actual", "día", "días", "¡Sigue así!"],
    "sw": ["Hitilafu kupata data ya mfululizo.", "Takwimu za mfululizo",
           "Mfululizo wa sasa", "siku", "siku", "Endelea hivyo"],
    "ar": ["خطأ في جلب بيانات السلسلة.", "إحصائيات السلسلة", "السلسلة الحالية",
           "يوم", "أيام", "واصل التقدم"],
}


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")


with io.open(PATH, "r", encoding="utf-8") as fh:
    content = fh.read()

if "String get streakTitle =>" in content:
    print("getters already present, skip")
    raise SystemExit

# 1) clés dans les maps pt/es/sw/ar — ancrage : après 'familyError' de chaque map
#    (inséré à l'étape précédente pour les 6 locales ; on cible les 4 cibles).
anchors = list(re.finditer(r"( *)'familyError': .*,\n", content))
assert len(anchors) == 6, f"expected 6 familyError anchors, got {len(anchors)}"
# ordre des maps : fr, en, pt, es, sw, ar → insérer seulement idx 2..5
offset = 0
for idx, loc in [(2, "pt"), (3, "es"), (4, "sw"), (5, "ar")]:
    m = anchors[idx]
    indent = m.group(1)
    block = "".join("%s'%s': '%s',\n" % (indent, k, esc(v))
                    for k, v in zip(KEYS, VALUES[loc]))
    pos = m.end() + offset
    content = content[:pos] + block + content[pos:]
    offset += len(block)
    print(f"map {loc}: +{len(KEYS)}")

# 2) getters — après le getter familyMeetingEmpty
gm = re.search(r"( *)String get familyMeetingEmpty => translate\('familyMeetingEmpty'\);\n", content)
assert gm, "getter anchor not found"
indent = gm.group(1)
gblock = "".join("%sString get %s => translate('%s');\n" % (indent, k, k) for k in KEYS)
content = content[:gm.end()] + gblock + content[gm.end():]
print(f"getters: +{len(KEYS)}")

with io.open(PATH, "w", encoding="utf-8") as fh:
    fh.write(content)
print("done")

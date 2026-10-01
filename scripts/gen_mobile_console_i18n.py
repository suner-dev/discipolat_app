#!/usr/bin/env python3
"""G5.5 §59 — Clés l10n mobile pour la console admin terrain (approbations + présence)."""
import io, re

PATH = "/home/arise/discipolat/discipolat_app/mobile/lib/l10n/app_localizations.dart"

KEYS = [
    ("navAdminConsole", "navAdminConsole"),
    ("adminConsoleTitle", "adminConsoleTitle"),
    ("adminConsoleTabApprovals", "adminConsoleTabApprovals"),
    ("adminConsoleTabAttendance", "adminConsoleTabAttendance"),
    ("adminConsoleNoApprovals", "adminConsoleNoApprovals"),
    ("adminConsoleApprove", "adminConsoleApprove"),
    ("adminConsoleReject", "adminConsoleReject"),
    ("adminConsoleCommentHint", "adminConsoleCommentHint"),
    ("adminConsoleDecisionDone", "adminConsoleDecisionDone"),
    ("adminConsoleDecisionError", "adminConsoleDecisionError"),
    ("adminConsolePickDepartment", "adminConsolePickDepartment"),
    ("adminConsolePickEvent", "adminConsolePickEvent"),
    ("adminConsoleNoEvents", "adminConsoleNoEvents"),
    ("adminConsoleMarkAll", "adminConsoleMarkAll"),
    ("adminConsoleLoadError", "adminConsoleLoadError"),
]

# Ordre des sections dans le fichier : fr, en, pt, es, sw, ar
LOCALES = ["fr", "en", "pt", "es", "sw", "ar"]
VALUES = {
    "fr": ["Console terrain", "Console admin terrain", "Approbations", "Présence",
           "Aucune approbation en attente.", "Approuver", "Rejeter", "Commentaire (optionnel)",
           "Décision enregistrée", "Échec de la décision", "Choisir un département",
           "Choisir un événement", "Aucun événement pour ce département.",
           "Tout marquer présent", "Erreur de chargement"],
    "en": ["Field console", "Field admin console", "Approvals", "Attendance",
           "No pending approval.", "Approve", "Reject", "Comment (optional)",
           "Decision recorded", "Decision failed", "Choose a department",
           "Choose an event", "No event for this department.",
           "Mark all present", "Load error"],
    "pt": ["Console de campo", "Console admin de campo", "Aprovações", "Presenças",
           "Nenhuma aprovação pendente.", "Aprovar", "Rejeitar", "Comentário (opcional)",
           "Decisão registada", "Falha na decisão", "Escolher um departamento",
           "Escolher um evento", "Nenhum evento neste departamento.",
           "Marcar todos presentes", "Erro de carregamento"],
    "es": ["Consola de campo", "Consola admin de campo", "Aprobaciones", "Asistencia",
           "Ninguna aprobación pendiente.", "Aprobar", "Rechazar", "Comentario (opcional)",
           "Decisión registrada", "Error en la decisión", "Elegir un departamento",
           "Elegir un evento", "Ningún evento en este departamento.",
           "Marcar todos presentes", "Error de carga"],
    "sw": ["Konsoli ya uwandani", "Konsoli ya msimamizi wa uwandani", "Idhini", "Mahudhurio",
           "Hakuna idhini inayosubiri.", "Idhinisha", "Kataa", "Maoni (hiari)",
           "Uamuzi umehifadhiwa", "Uamuzi umeshindikana", "Chagua idara",
           "Chagua tukio", "Hakuna tukio kwa idara hii.",
           "Wawache wote waliopo", "Hitilafu ya kupakia"],
    "ar": ["وحدة التحكم الميدانية", "وحدة تحكم المسؤول الميدانية", "الموافقات", "الحضور",
           "لا توجد موافقات معلقة.", "موافقة", "رفض", "تعليق (اختياري)",
           "تم تسجيل القرار", "فشل القرار", "اختر قسماً",
           "اختر حدثاً", "لا توجد أحداث لهذا القسم.",
           "وضع علامة حاضر للجميع", "خطأ في التحميل"],
}

def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")

with io.open(PATH, "r", encoding="utf-8") as fh:
    content = fh.read()

if "'navAdminConsole':" in content:
    print("already present, skip"); raise SystemExit

# 1) entrées de dictionnaires — après chaque 'adminRequestsEmpty' (6 sections, ordre LOCALES)
anchors = list(re.finditer(r"( *)'adminRequestsEmpty': .*,\n", content))
assert len(anchors) == 6, f"expected 6 anchors, got {len(anchors)}"
offset = 0
for m, loc in zip(anchors, LOCALES):
    indent = m.group(1)
    block = "".join("%s'%s': '%s',\n" % (indent, k, esc(v))
                    for (k, _), v in zip(KEYS, VALUES[loc]))
    idx = m.end() + offset
    content = content[:idx] + block + content[idx:]
    offset += len(block)
    print(f"map {loc}: +{len(KEYS)}")

# 2) getters — après le getter adminRequestsEmpty
gm = re.search(r"( *)String get adminRequestsEmpty => translate\('adminRequestsEmpty'\);\n", content)
assert gm, "getter anchor not found"
indent = gm.group(1)
gblock = "".join("%sString get %s => translate('%s');\n" % (indent, g, g) for g, _ in KEYS)
idx = gm.end()
content = content[:idx] + gblock + content[idx:]
print("getters: +%d" % len(KEYS))

with io.open(PATH, "w", encoding="utf-8") as fh:
    fh.write(content)
print("done")

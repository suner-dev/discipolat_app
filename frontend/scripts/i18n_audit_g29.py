#!/usr/bin/env python3
"""G5.5 (§58 journaux / §G2.9) — Clés audit.* : vérification chaîne + historique métier."""
import io, re, os, json

BASE = os.path.dirname(os.path.abspath(__file__)) + "/../src/i18n"

FR = {
    "audit.verifyChain": "Vérifier l'intégrité de la chaîne",
    "audit.chainOk": "Chaîne d'audit intègre — {n} événements vérifiés",
    "audit.chainBroken": "Intégrité compromise — rupture après {n} événements",
    "audit.chainUnverified": "Vérification impossible",
    "audit.history": "Historique",
    "audit.historyTitle": "Historique métier de l'entité",
    "audit.historyEmpty": "Aucun événement d'historique pour cet objet",
    "audit.actor": "Acteur",
    "audit.role": "Rôle",
}
EN = {
    "audit.verifyChain": "Verify chain integrity",
    "audit.chainOk": "Audit chain intact — {n} events verified",
    "audit.chainBroken": "Integrity compromised — chain broken after {n} events",
    "audit.chainUnverified": "Verification failed",
    "audit.history": "History",
    "audit.historyTitle": "Entity business history",
    "audit.historyEmpty": "No history events for this object",
    "audit.actor": "Actor",
    "audit.role": "Role",
}
PT = {
    "audit.verifyChain": "Verificar integridade da cadeia",
    "audit.chainOk": "Cadeia de auditoria íntegra — {n} eventos verificados",
    "audit.chainBroken": "Integridade comprometida — cadeia rompida após {n} eventos",
    "audit.chainUnverified": "Verificação impossível",
    "audit.history": "Histórico",
    "audit.historyTitle": "Histórico de negócio da entidade",
    "audit.historyEmpty": "Nenhum evento de histórico para este objeto",
    "audit.actor": "Ator",
    "audit.role": "Função",
}
ES = {
    "audit.verifyChain": "Verificar integridad de la cadena",
    "audit.chainOk": "Cadena de auditoría íntegra — {n} eventos verificados",
    "audit.chainBroken": "Integridad comprometida — cadena rota tras {n} eventos",
    "audit.chainUnverified": "Verificación imposible",
    "audit.history": "Historial",
    "audit.historyTitle": "Historial de negocio de la entidad",
    "audit.historyEmpty": "Ningún evento de historial para este objeto",
    "audit.actor": "Actor",
    "audit.role": "Rol",
}
SW = {
    "audit.verifyChain": "Thibitisha uimara wa mnyororo",
    "audit.chainOk": "Mnyororo wa ukaguzi imara — matukio {n} yamehakikiwa",
    "audit.chainBroken": "Uimara umevunjika — mnyororo ulikatika baada ya matukio {n}",
    "audit.chainUnverified": "Hakikisho haliwezekani",
    "audit.history": "Historia",
    "audit.historyTitle": "Historia ya shughuli ya kipengele",
    "audit.historyEmpty": "Hakuna matukio ya historia kwa kitu hiki",
    "audit.actor": "Mfanya",
    "audit.role": "Chezo",
}
AR = {
    "audit.verifyChain": "التحقق من سلامة السلسلة",
    "audit.chainOk": "سلسلة التدقيق سليمة — تم التحقق من {n} حدثًا",
    "audit.chainBroken": "السلامة مخترقة — انقطعت السلسلة بعد {n} حدثًا",
    "audit.chainUnverified": "التحقق غير ممكن",
    "audit.history": "السجل",
    "audit.historyTitle": "السجل المهني للكيان",
    "audit.historyEmpty": "لا توجد أحداث سجل لهذا الكائن",
    "audit.actor": "الفاعل",
    "audit.role": "الدور",
}

ALL = {"fr": FR, "en": EN, "pt": PT, "es": ES, "sw": SW, "ar": AR}
ANCHOR = "admin.statByRole"

for loc, kv in ALL.items():
    path = os.path.join(BASE, f"{loc}.ts")
    with io.open(path, "r", encoding="utf-8") as fh:
        content = fh.read()
    if "'audit.verifyChain'" in content:
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

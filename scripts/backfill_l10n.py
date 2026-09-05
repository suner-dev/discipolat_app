#!/usr/bin/env python3
"""Backfill the FR-only keys (nav entries, profile, feedback) into the
EN, PT, ES, SW and AR dictionaries of mobile/app_localizations.dart so all
six locales share the same key set."""
import re
from pathlib import Path

DART = Path("mobile/lib/l10n/app_localizations.dart")

# key -> {lang: translation}
ADD = {
    "feedbackSubtitle": {"en": "Bug, suggestion, issue…", "pt": "Erro, sugestão, problema…", "es": "Error, sugerencia, problema…", "sw": "Hitilafu, pendekezo, tatizo…", "ar": "خطأ، اقتراح، مشكلة…"},
    "feedbackTitle": {"en": "Feedback?", "pt": "Um feedback?", "es": "¿Un comentario?", "sw": "Maoni?", "ar": "ملاحظات؟"},
    "navActiveLabel": {"en": "ACTIVE", "pt": "ATIVO", "es": "ACTIVO", "sw": "Hai", "ar": "نشط"},
    "navAdminSection": {"en": "ADMINISTRATION", "pt": "ADMINISTRAÇÃO", "es": "ADMINISTRACIÓN", "sw": "USIMAMIZI", "ar": "الإدارة"},
    "navAppointments": {"en": "Appointments", "pt": "Compromissos", "es": "Citas", "sw": "Miwadi", "ar": "المواعيد"},
    "navAudit": {"en": "Audit", "pt": "Auditoria", "es": "Auditoría", "sw": "Ukaguzi", "ar": "التدقيق"},
    "navChangeRole": {"en": "Change role", "pt": "Trocar de papel", "es": "Cambiar de rol", "sw": "Badilisha jukumu", "ar": "تغيير الدور"},
    "navChefDashboard": {"en": "Family Head Dashboard", "pt": "Painel do Chefe de Família", "es": "Panel del Jefe de Familia", "sw": "Dashibodi ya Mkuu wa Familia", "ar": "لوحة رب الأسرة"},
    "navChurchBenchmark": {"en": "Churches benchmark", "pt": "Benchmark de igrejas", "es": "Comparativa de iglesias", "sw": "Ulinganisho wa makanisa", "ar": "مقارنة الكنائس"},
    "navChurchSettings": {"en": "Church settings", "pt": "Configurações da igreja", "es": "Ajustes de la iglesia", "sw": "Mipangilio ya kanisa", "ar": "إعدادات الكنيسة"},
    "navChurches": {"en": "Churches (tenants)", "pt": "Igrejas (organizações)", "es": "Iglesias (organizaciones)", "sw": "Makanisa (mashirika)", "ar": "الكنائس (المنظمات)"},
    "navCompliance": {"en": "GDPR compliance", "pt": "Conformidade GDPR", "es": "Cumplimiento RGPD", "sw": "Uzingatiaji wa GDPR", "ar": "الامتثال للائحة العامة لحماية البيانات"},
    "navCrmFaiseur": {"en": "Maker CRM", "pt": "CRM do Fazedor", "es": "CRM del Hacedor", "sw": "CRM ya Mtendaji", "ar": "إدارة علاقات الصنّاع"},
    "navCustomFields": {"en": "Custom fields", "pt": "Campos personalizados", "es": "Campos personalizados", "sw": "Sehemu maalum", "ar": "حقول مخصصة"},
    "navCustomPages": {"en": "Custom pages", "pt": "Páginas personalizadas", "es": "Páginas personalizadas", "sw": "Kurasa maalum", "ar": "صفحات مخصصة"},
    "navDataMigration": {"en": "Data migration", "pt": "Migração de dados", "es": "Migración de datos", "sw": "Uhamisho wa data", "ar": "ترحيل البيانات"},
    "navDictionaries": {"en": "Dictionaries", "pt": "Dicionários", "es": "Diccionarios", "sw": "Kamusi", "ar": "القواميس"},
    "navDiscipleshipPath": {"en": "Discipleship path", "pt": "Jornada de discipulado", "es": "Camino de discipulado", "sw": "Njia ya uanafunzi", "ar": "مسار التلمذة"},
    "navDocuments": {"en": "Documents", "pt": "Documentos", "es": "Documentos", "sw": "Nyaraka", "ar": "المستندات"},
    "navEncouragements": {"en": "Encouragements", "pt": "Incentivos", "es": "Ánimos", "sw": "Tia moyo", "ar": "التشجيع"},
    "navEvaluations": {"en": "Evaluations", "pt": "Avaliações", "es": "Evaluaciones", "sw": "Tathmini", "ar": "التقييمات"},
    "navFaceCheckin": {"en": "Face check-in", "pt": "Presença facial", "es": "Registro facial", "sw": "Kuingia kwa uso", "ar": "تسجيل الوجه"},
    "navFamilyReport": {"en": "Family report", "pt": "Relatório da família", "es": "Informe familiar", "sw": "Ripoti ya familia", "ar": "تقرير العائلة"},
    "navFollowUpRequests": {"en": "Follow-up requests", "pt": "Pedidos de acompanhamento", "es": "Solicitudes de seguimiento", "sw": "Maombi ya ufuatiliaji", "ar": "طلبات المتابعة"},
    "navGraceActions": {"en": "Thanksgiving actions", "pt": "Ações de graças", "es": "Acciones de gracias", "sw": "Matendo ya shukrani", "ar": "أعمال الشكر"},
    "navGrowthProjection": {"en": "Growth projection", "pt": "Projeção de crescimento", "es": "Proyección de crecimiento", "sw": "Makadirio ya ukuaji", "ar": "توقع النمو"},
    "navIntegrations": {"en": "Integrations", "pt": "Integrações", "es": "Integraciones", "sw": "Ushirikiano", "ar": "التكاملات"},
    "navInventory": {"en": "Inventory", "pt": "Inventário", "es": "Inventario", "sw": "Hesabu", "ar": "المخزون"},
    "navLoadPrediction": {"en": "Load prediction", "pt": "Previsão de carga", "es": "Predicción de carga", "sw": "Utabiri wa mzigo", "ar": "توقع العبء"},
    "navMakerReport": {"en": "Maker report", "pt": "Relatório do fazedor", "es": "Informe del hacedor", "sw": "Ripoti ya mtendaji", "ar": "تقرير الصانع"},
    "navMenus": {"en": "Platform menus", "pt": "Menus da plataforma", "es": "Menús de la plataforma", "sw": "Menus za jukwaa", "ar": "قوائم المنصة"},
    "navMessaging": {"en": "Messaging", "pt": "Mensagens", "es": "Mensajería", "sw": "Ujumbe", "ar": "المراسلة"},
    "navModeration": {"en": "Moderation", "pt": "Moderação", "es": "Moderación", "sw": "Udhibiti", "ar": "المراجعة"},
    "navModules": {"en": "Platform modules", "pt": "Módulos da plataforma", "es": "Módulos de la plataforma", "sw": "Moduli za jukwaa", "ar": "وحدات المنصة"},
    "navNeighborhoodHealth": {"en": "Neighborhood health", "pt": "Saúde dos bairros", "es": "Salud de los barrios", "sw": "Afya ya vitongoji", "ar": "صحة الأحياء"},
    "navObjectives": {"en": "Objectives", "pt": "Objetivos", "es": "Objetivos", "sw": "Malengo", "ar": "الأهداف"},
    "navParallelFollowups": {"en": "Parallel follow-ups", "pt": "Acompanhamentos paralelos", "es": "Seguimientos paralelos", "sw": "Ufuatiliaji sambamba", "ar": "متابعات متوازية"},
    "navPermissions": {"en": "Permissions", "pt": "Permissões", "es": "Permisos", "sw": "Ruhusa", "ar": "الصلاحيات"},
    "navQuest": {"en": "Quest (XP)", "pt": "Quest (XP)", "es": "Quest (XP)", "sw": "Quest (XP)", "ar": "المهمة (نقاط)"},
    "navRequests": {"en": "Requests", "pt": "Pedidos", "es": "Solicitudes", "sw": "Maombi", "ar": "الطلبات"},
    "navRespDashboard": {"en": "Manager Dashboard", "pt": "Painel do Responsável", "es": "Panel del Responsable", "sw": "Dashibodi ya Mwajibikaji", "ar": "لوحة المسؤول"},
    "navRewards": {"en": "Rewards", "pt": "Recompensas", "es": "Recompensas", "sw": "Zawadi", "ar": "المكافآت"},
    "navSabbath": {"en": "Sabbath Dashboard", "pt": "Painel do Sábado", "es": "Panel del Sábado", "sw": "Dashibodi ya Sabato", "ar": "لوحة السبت"},
    "navSecurity": {"en": "Security", "pt": "Segurança", "es": "Seguridad", "sw": "Usalama", "ar": "الأمان"},
    "navShepherdsPilot": {"en": "Pastor dashboard", "pt": "Pilotagem do Pastor", "es": "Pilotaje del Pastor", "sw": "Dashibodi ya Mchungaji", "ar": "لوحة القس"},
    "navTithesOfferings": {"en": "Tithes & offerings", "pt": "Dízimos e ofertas", "es": "Diezmos y ofrendas", "sw": "Zaka na sadaka", "ar": "العشور والتقدمات"},
    "navTontines": {"en": "Tontines", "pt": "Tontinas", "es": "Tontinas", "sw": "Tontine", "ar": "التونتين"},
    "navTransferWorkflow": {"en": "Transfer workflow", "pt": "Fluxo de transferência", "es": "Flujo de transferencia", "sw": "Mtiririko wa uhamisho", "ar": "سير عمل النقل"},
    "navTransfers": {"en": "Transfers", "pt": "Transferências", "es": "Transferencias", "sw": "Uhamisho", "ar": "عمليات النقل"},
    "navUsageAnalytics": {"en": "Usage analytics", "pt": "Analítica de uso", "es": "Analítica de uso", "sw": "Uchambuzi wa matumizi", "ar": "تحليلات الاستخدام"},
    "navVisits": {"en": "Visits", "pt": "Visitas", "es": "Visitas", "sw": "Ziara", "ar": "الزيارات"},
    "navVoiceAssistant": {"en": "Voice Assistant", "pt": "Assistente de Voz", "es": "Asistente de voz", "sw": "Msaidizi wa Sauti", "ar": "المساعد الصوتي"},
    "navVoiceReports": {"en": "Voice reports", "pt": "Relatórios de voz", "es": "Informes de voz", "sw": "Ripoti za sauti", "ar": "تقارير صوتية"},
    "navAiAssistant": {"en": "AI Assistant", "pt": "Assistente de IA", "es": "Asistente de IA", "sw": "Msaidizi wa AI", "ar": "المساعد الذكي"},
    "navAiPredictions": {"en": "AI Predictions", "pt": "Previsões de IA", "es": "Predicciones de IA", "sw": "Utabiri wa AI", "ar": "توقعات الذكاء الاصطناعي"},
    "navPropheticJournal": {"en": "Prophetic Journal", "pt": "Diário Profético", "es": "Diario Profético", "sw": "Shajara ya Kiunabii", "ar": "المذكرات النبوية"},
    "navWeeklyChallenges": {"en": "Weekly challenges", "pt": "Desafios semanais", "es": "Desafíos semanales", "sw": "Changamoto za wiki", "ar": "تحديات أسبوعية"},
    "navWhatsApp": {"en": "WhatsApp reminders", "pt": "Lembretes WhatsApp", "es": "Recordatorios WhatsApp", "sw": "Vikumbusho vya WhatsApp", "ar": "تذكيرات واتساب"},
    "profileDepartment": {"en": "Department", "pt": "Departamento", "es": "Departamento", "sw": "Idara", "ar": "القسم"},
    "profileEmail": {"en": "Email", "pt": "Email", "es": "Correo", "sw": "Barua pepe", "ar": "البريد"},
    "profileFamily": {"en": "Family", "pt": "Família", "es": "Familia", "sw": "Familia", "ar": "العائلة"},
    "profileLogout": {"en": "Sign out", "pt": "Sair", "es": "Cerrar sesión", "sw": "Toka", "ar": "تسجيل الخروج"},
    "profilePersonalInfo": {"en": "Personal information", "pt": "Informações pessoais", "es": "Información personal", "sw": "Taarifa za kibinafsi", "ar": "معلومات شخصية"},
    "profilePhone": {"en": "Phone", "pt": "Telefone", "es": "Teléfono", "sw": "Simu", "ar": "الهاتف"},
    "profilePresence": {"en": "Attendance", "pt": "Presença", "es": "Asistencia", "sw": "Mahudhurio", "ar": "الحضور"},
    "profileProgression": {"en": "Progression", "pt": "Progressão", "es": "Progresión", "sw": "Maendeleo", "ar": "التقدم"},
    "profileQuickActions": {"en": "Quick actions", "pt": "Ações rápidas", "es": "Acciones rápidas", "sw": "Hatua za haraka", "ar": "إجراءات سريعة"},
    "profileRegisteredOn": {"en": "Registered on", "pt": "Registrado em", "es": "Registrado el", "sw": "Imeandikishwa", "ar": "مسجل في"},
    "profileRole": {"en": "Role", "pt": "Papel", "es": "Rol", "sw": "Jukumu", "ar": "الدور"},
    "profileScore": {"en": "Score", "pt": "Pontuação", "es": "Puntuación", "sw": "Alama", "ar": "الدرجة"},
    "profileSpiritualInfo": {"en": "Spiritual information", "pt": "Informações espirituais", "es": "Información espiritual", "sw": "Taarifa za kiroho", "ar": "معلومات روحية"},
    "profileSpiritualScore": {"en": "Spiritual score", "pt": "Pontuação espiritual", "es": "Puntuación espiritual", "sw": "Alama za kiroho", "ar": "الدرجة الروحية"},
    "profileTitle": {"en": "Profile", "pt": "Perfil", "es": "Perfil", "sw": "Wasifu", "ar": "الملف الشخصي"},
    "rejectAction": {"en": "Reject", "pt": "Rejeitar", "es": "Rechazar", "sw": "Kataa", "ar": "رفض"},
    "sendError": {"en": "Send failed", "pt": "Falha ao enviar", "es": "Error al enviar", "sw": "Hitilafu ya kutuma", "ar": "فشل الإرسال"},
    # PT-only recurring keys (missing in PT but present in others)
    "recurringTitle": {"pt": "Doações recorrentes"},
    "recurringNew": {"pt": "Nova doação recorrente"},
    "recurringHide": {"pt": "Ocultar"},
    "recurringCreate": {"pt": "Criar doação recorrente"},
    "recurringFrequency": {"pt": "Frequência"},
    "recurringWeekly": {"pt": "Semanal"},
    "recurringBimonthly": {"pt": "Quinzenal"},
    "recurringMonthly": {"pt": "Mensal"},
    "recurringQuarterly": {"pt": "Trimestral"},
    "recurringYearly": {"pt": "Anual"},
    "recurringCreated": {"pt": "Doação recorrente criada com sucesso!"},
    "recurringCancelled": {"pt": "Doação recorrente cancelada."},
    "recurringActive": {"pt": "Ativo"},
    "recurringInactive": {"pt": "Inativo"},
    "recurringEmpty": {"pt": "Nenhuma doação recorrente. Crie uma para automatizar suas doações."},
    "recurringNextDate": {"pt": "Próxima data"},
    "recurringTotalDonated": {"pt": "Total doado"},
    "recurringDonations": {"pt": "doações"},
}

LANGS = {
    "en": "_english",
    "pt": "_portuguese",
    "es": "_spanish",
    "sw": "_swahili",
    "ar": "_arabic",
}


def main() -> int:
    src = DART.read_text(encoding="utf-8")
    inserted = 0
    for lang, block_name in LANGS.items():
        marker = f"  static const Map<String, String> {block_name} = {{"
        if marker not in src:
            print(f"WARN: {block_name} block not found, skipping")
            continue
        start = src.index(marker) + len(marker)
        end = src.index("\n  };", start)
        inner = src[start:end]
        keys_present = set(re.findall(r"^\s+'([^']+)':", inner, re.M))
        additions = []
        for key, by_lang in ADD.items():
            if key in keys_present:
                continue
            val = by_lang.get(lang)
            if val is None:
                continue
            val_esc = val.replace("\\", "\\\\").replace("'", "\\'")
            additions.append(f"    '{key}': '{val_esc}',")
        if not additions:
            continue
        chunk = "\n" + "\n".join(additions)
        new_inner = inner + chunk
        src = src[:start] + new_inner + src[end:]
        inserted += len(additions)
        print(f"  {block_name}: +{len(additions)} keys")
    DART.write_text(src, encoding="utf-8")
    print(f"Total keys added: {inserted}")
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
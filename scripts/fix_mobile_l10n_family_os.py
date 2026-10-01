#!/usr/bin/env python3
"""G4 — insère les clés i18n « Family OS + ministère pastoral » (mobile) dans
lib/l10n/app_localizations.dart : 6 maps (ordre fr,en,pt,es,sw,ar) ancrées sur
la ligne 'familySpaceTitle' de chaque locale + getters ancrés sur
`String get familySpaceTitle`. Idempotent : saute si la clé existe déjà."""
import re
import sys

PATH = 'lib/l10n/app_localizations.dart'

KEYS = {
    'familyOsTabOverview': {
        'fr': 'Aperçu', 'en': 'Overview', 'pt': 'Visão geral',
        'es': 'Resumen', 'sw': 'Muhtasari', 'ar': 'نظرة عامة',
    },
    'familyOsTabVisits': {
        'fr': 'Visites', 'en': 'Visits', 'pt': 'Visitas',
        'es': 'Visitas', 'sw': 'Tembelea', 'ar': 'الزيارات',
    },
    'familyOsTabMembers': {
        'fr': 'Membres', 'en': 'Members', 'pt': 'Membros',
        'es': 'Miembros', 'sw': 'Wanachama', 'ar': 'الأعضاء',
    },
    'familyOsTabJournal': {
        'fr': 'Journal', 'en': 'Journal', 'pt': 'Diário',
        'es': 'Registro', 'sw': 'Shajara', 'ar': 'السجل',
    },
    'familyOsNoFamily': {
        'fr': 'Aucune famille ne vous est associée. Contactez votre responsable.',
        'en': 'No family is assigned to you. Contact your leader.',
        'pt': 'Nenhuma família está associada a você. Contate seu responsável.',
        'es': 'Ninguna familia está asociada a usted. Contacte a su responsable.',
        'sw': 'Hakuna familia inayohusishwa nawe. Wasiliana na kiongozi wako.',
        'ar': 'لا توجد عائلة مرتبطة بحسابك. تواصل مع مسؤولك.',
    },
    'familyOsOverdueFollowUps': {
        'fr': 'Suivis en retard', 'en': 'Overdue follow-ups',
        'pt': 'Acompanhamentos atrasados', 'es': 'Seguimientos atrasados',
        'sw': 'Ufuatiliaji uliochelewa', 'ar': 'متأخرات المتابعة',
    },
    'familyOsUpcomingVisits': {
        'fr': 'Visites à venir', 'en': 'Upcoming visits',
        'pt': 'Próximas visitas', 'es': 'Próximas visitas',
        'sw': 'Tembelea zijazo', 'ar': 'الزيارات القادمة',
    },
    'familyOsFollowUpList': {
        'fr': 'Suivis en cours', 'en': 'Active follow-ups',
        'pt': 'Acompanhamentos em andamento', 'es': 'Seguimientos en curso',
        'sw': 'Ufuatiliaji unaoendelea', 'ar': 'المتابعة الجارية',
    },
    'familyOsNextAction': {
        'fr': 'Prochaine action', 'en': 'Next action',
        'pt': 'Próxima ação', 'es': 'Próxima acción',
        'sw': 'Hatua inayofuata', 'ar': 'الإجراء التالي',
    },
    'familyOsRecentReceptions': {
        'fr': 'Réceptions récentes', 'en': 'Recent receptions',
        'pt': 'Recepções recentes', 'es': 'Recepciones recientes',
        'sw': 'Karibisho za hivi karibuni', 'ar': 'الاستقبالات الأخيرة',
    },
    'familyOsNoVisits': {
        'fr': 'Aucune visite enregistrée', 'en': 'No visit recorded',
        'pt': 'Nenhuma visita registrada', 'es': 'Ninguna visita registrada',
        'sw': 'Hakuna tembelea iliyorekodiwa', 'ar': 'لا توجد زيارات مسجلة',
    },
    'familyOsMarkDone': {
        'fr': 'Marquer comme effectuée', 'en': 'Mark as done',
        'pt': 'Marcar como feita', 'es': 'Marcar como realizada',
        'sw': 'Weka kama imefanyika', 'ar': 'وضع كمُنجزة',
    },
    'familyOsVisitUpdated': {
        'fr': 'Visite mise à jour', 'en': 'Visit updated',
        'pt': 'Visita atualizada', 'es': 'Visita actualizada',
        'sw': 'Tembelea imesasishwa', 'ar': 'تم تحديث الزيارة',
    },
    'familyOsActionError': {
        'fr': 'Action échouée', 'en': 'Action failed',
        'pt': 'Ação falhou', 'es': 'Acción fallida',
        'sw': 'Kitendo kimeshindwa', 'ar': 'فشل الإجراء',
    },
    'familyOsNewVisit': {
        'fr': 'Nouvelle visite', 'en': 'New visit',
        'pt': 'Nova visita', 'es': 'Nueva visita',
        'sw': 'Tembelea mpya', 'ar': 'زيارة جديدة',
    },
    'familyOsVisitDate': {
        'fr': 'Date', 'en': 'Date', 'pt': 'Data',
        'es': 'Fecha', 'sw': 'Tarehe', 'ar': 'التاريخ',
    },
    'familyOsSubjectHint': {
        'fr': 'Sujet (optionnel)', 'en': 'Subject (optional)',
        'pt': 'Assunto (opcional)', 'es': 'Asunto (opcional)',
        'sw': 'Sujeti (hiari)', 'ar': 'الموضوع (اختياري)',
    },
    'familyOsVisitSaved': {
        'fr': 'Visite enregistrée', 'en': 'Visit saved',
        'pt': 'Visita salva', 'es': 'Visita guardada',
        'sw': 'Tembelea imehifadhiwa', 'ar': 'تم حفظ الزيارة',
    },
    'familyOsAddSoul': {
        'fr': 'Ajouter une âme', 'en': 'Add a soul',
        'pt': 'Adicionar uma alma', 'es': 'Añadir un alma',
        'sw': 'Ongeza nafsi', 'ar': 'إضافة نفس',
    },
    'familyOsScopeCampus': {
        'fr': 'Campus', 'en': 'Campus', 'pt': 'Campus',
        'es': 'Campus', 'sw': 'Kampasi', 'ar': 'الفرع',
    },
    'familyOsScopeChurch': {
        'fr': 'Église entière', 'en': 'Whole church',
        'pt': 'Igreja inteira', 'es': 'Iglesia entera',
        'sw': 'Kanisani nzima', 'ar': 'الكنيسة بأكملها',
    },
    'familyOsSoulSearchHint': {
        'fr': 'Rechercher un nom…', 'en': 'Search a name…',
        'pt': 'Buscar um nome…', 'es': 'Buscar un nombre…',
        'sw': 'Tafuta jina…', 'ar': 'ابحث عن اسم…',
    },
    'familyOsNoCandidates': {
        'fr': 'Aucune âme disponible trouvée',
        'en': 'No available soul found',
        'pt': 'Nenhuma alma disponível encontrada',
        'es': 'Ningún alma disponible encontrada',
        'sw': 'Hakuna nafsi anayepatikana',
        'ar': 'لا توجد أنفُس متاحة',
    },
    'familyOsNoFaiseur': {
        'fr': 'Sans faiseur attitré', 'en': 'No assigned worker',
        'pt': 'Sem obreiro designado', 'es': 'Sin obrero asignado',
        'sw': 'Bila mfanyiaji aliyeteuliwa', 'ar': 'بدون عامل مخصص',
    },
    'familyOsConfirmAdd': {
        'fr': 'Ajouter à la famille', 'en': 'Add to the family',
        'pt': 'Adicionar à família', 'es': 'Añadir a la familia',
        'sw': 'Ongeza kwenye familia', 'ar': 'إضافة إلى العائلة',
    },
    'familyOsSoulAdded': {
        'fr': 'Âme ajoutée à la famille', 'en': 'Soul added to the family',
        'pt': 'Alma adicionada à família', 'es': 'Alma añadida a la familia',
        'sw': 'Nafsi ameongezwa kwenye familia',
        'ar': 'تمت إضافة النفس إلى العائلة',
    },
    'familyOsNoMembers': {
        'fr': 'Aucun membre dans cette famille',
        'en': 'No members in this family',
        'pt': 'Nenhum membro nesta família',
        'es': 'Ningún miembro en esta familia',
        'sw': 'Hakuna wanachama katika familia hii',
        'ar': 'لا يوجد أعضاء في هذه العائلة',
    },
    'pastoralTitle': {
        'fr': 'Ministère pastoral', 'en': 'Pastoral ministry',
        'pt': 'Ministério pastoral', 'es': 'Ministerio pastoral',
        'sw': 'Huduma ya kichungaji', 'ar': 'الخدمة الراعوية',
    },
    'pastoralTabMandates': {
        'fr': 'Mandats', 'en': 'Mandates', 'pt': 'Mandatos',
        'es': 'Mandatos', 'sw': 'Majukumu', 'ar': 'التفويضات',
    },
    'pastoralTabTransfers': {
        'fr': 'Transferts', 'en': 'Transfers', 'pt': 'Transferências',
        'es': 'Traslados', 'sw': 'Uhamisho', 'ar': 'الانتقالات',
    },
    'pastoralTabHistory': {
        'fr': 'Historique', 'en': 'History', 'pt': 'Histórico',
        'es': 'Histórico', 'sw': 'Historia', 'ar': 'السجل',
    },
    'pastoralLoadError': {
        'fr': 'Erreur de chargement du ministère pastoral',
        'en': 'Error loading the pastoral ministry',
        'pt': 'Erro ao carregar o ministério pastoral',
        'es': 'Error al cargar el ministerio pastoral',
        'sw': 'Hitilafu kupakia huduma ya kichungaji',
        'ar': 'خطأ في تحميل الخدمة الراعوية',
    },
    'pastoralNoMandates': {
        'fr': 'Aucun mandat pastoral enregistré',
        'en': 'No pastoral mandate recorded',
        'pt': 'Nenhum mandato pastoral registrado',
        'es': 'Ningún mandato pastoral registrado',
        'sw': 'Hakuna jukumu la kichungaji lililosajiliwa',
        'ar': 'لا توجد تفويضات رعوية مسجلة',
    },
    'pastoralActive': {
        'fr': 'En cours', 'en': 'In progress', 'pt': 'Em andamento',
        'es': 'En curso', 'sw': 'Inaendelea', 'ar': 'جارية',
    },
    'pastoralEnded': {
        'fr': 'Terminés', 'en': 'Ended', 'pt': 'Encerrados',
        'es': 'Finalizados', 'sw': 'Yameisha', 'ar': 'منتهية',
    },
    'pastoralNoTransfers': {
        'fr': 'Aucun transfert pastoral', 'en': 'No pastoral transfer',
        'pt': 'Nenhuma transferência pastoral',
        'es': 'Ningún traslado pastoral',
        'sw': 'Hakuna uhamisho wa kichungaji',
        'ar': 'لا توجد انتقالات رعوية',
    },
    'pastoralSelectPastor': {
        'fr': 'Choisir un pasteur', 'en': 'Select a pastor',
        'pt': 'Escolher um pastor', 'es': 'Elegir un pastor',
        'sw': 'Chagua kichungaji', 'ar': 'اختر راعياً',
    },
    'pastoralNoHistory': {
        'fr': 'Aucun historique pour ce pasteur',
        'en': 'No history for this pastor',
        'pt': 'Nenhum histórico para este pastor',
        'es': 'Sin histórico para este pastor',
        'sw': 'Hakuna historia kwa kichungaji huyu',
        'ar': 'لا يوجد سجل لهذا الراعي',
    },
}

LOCALES = ['fr', 'en', 'pt', 'es', 'sw', 'ar']


def esc(v: str) -> str:
    return v.replace('\\', '\\\\').replace("'", "\\'")


def main():
    with open(PATH, encoding='utf-8') as f:
        lines = f.read().split('\n')
    text = '\n'.join(lines)

    idx = 0
    out = []
    inserted = 0
    for line in lines:
        out.append(line)
        if re.match(r"\s*'familySpaceTitle':", line) and idx < len(LOCALES):
            loc = LOCALES[idx]
            block = []
            for k, tr in KEYS.items():
                if f"'{k}':" in text:
                    continue
                block.append(f"    '{k}': '{esc(tr[loc])}',")
            if block:
                out.extend(block)
                inserted += len(block)
            idx += 1
    if inserted:
        text2 = '\n'.join(out)
        getters = '\n'.join(
            f"  String get {k} => translate('{k}');" for k in KEYS
        )
        text2 = text2.replace(
            "  String get familySpaceTitle => translate('familySpaceTitle');",
            "  String get familySpaceTitle => translate('familySpaceTitle');\n"
            + getters,
            1,
        )
        with open(PATH, 'w', encoding='utf-8') as f:
            f.write(text2)
    print(f'locale maps touched: {idx}, map lines inserted: {inserted}')
    final = open(PATH, encoding='utf-8').read()
    missing = [k for k in KEYS if f"String get {k} " not in final]
    if missing:
        print('MISSING getters:', missing)
        sys.exit(1)
    per_locale = final.count("'familyOsTabOverview':")
    print('map entries per locale:', per_locale)
    if per_locale != 6:
        sys.exit(2)
    print('OK')


if __name__ == '__main__':
    main()

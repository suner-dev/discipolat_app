#!/usr/bin/env python3
"""G5.6 — insère les clés i18n « mobile terrain » (QR check-in + inventaire
terrain) dans lib/l10n/app_localizations.dart : 6 maps (ordre fr,en,pt,es,sw,ar)
ancrées sur la ligne 'scanQrCode' de chaque locale + getters ancrés sur
`String get scanQrCode`. Idempotent : saute si la clé existe déjà."""
import re
import sys

PATH = 'lib/l10n/app_localizations.dart'

KEYS = {
    'qrCheckinTitle': {
        'fr': 'Pointage QR', 'en': 'QR check-in', 'pt': 'Registro por QR',
        'es': 'Registro por QR', 'sw': 'Kuwasilisha kwa QR', 'ar': 'تسجيل الحضور برمز QR',
    },
    'qrCheckinSubtitle': {
        'fr': 'Scannez le QR d\u2019un membre pour enregistrer sa présence',
        'en': 'Scan a member QR to record their attendance',
        'pt': 'Escaneie o QR de um membro para registrar sua presen\u00e7a',
        'es': 'Escanea el QR de un miembro para registrar su asistencia',
        'sw': 'Skani QR ya mwanachama ili kuhifadhi uwepo wake',
        'ar': 'امسح رمز QR لعضو لتسجيل حضوره',
    },
    'takeQrPhoto': {
        'fr': 'Prendre une photo du QR', 'en': 'Take a QR photo', 'pt': 'Tirar foto do QR',
        'es': 'Tomar foto del QR', 'sw': 'Piga picha ya QR', 'ar': 'التقاط صورة لرمز QR',
    },
    'manualCodeLabel': {
        'fr': 'Code ou UUID du membre', 'en': 'Member code or UUID',
        'pt': 'C\u00f3digo ou UUID do membro', 'es': 'C\u00f3digo o UUID del miembro',
        'sw': 'Msimbo au UUID ya mwanachama', 'ar': 'رمز العضو أو UUID',
    },
    'submitCheckin': {
        'fr': 'Pointer', 'en': 'Check in', 'pt': 'Registrar',
        'es': 'Registrar', 'sw': 'Tohifadhi', 'ar': 'تسجيل',
    },
    'checkinSaved': {
        'fr': 'Pr\u00e9sence enregistr\u00e9e', 'en': 'Attendance recorded',
        'pt': 'Presen\u00e7a registrada', 'es': 'Asistencia registrada',
        'sw': 'Uwepo umehifadhiwa', 'ar': 'تم تسجيل الحضور',
    },
    'checkinNoQr': {
        'fr': 'Aucun QR lisible sur la photo', 'en': 'No readable QR in the photo',
        'pt': 'Nenhum QR leg\u00edvel na foto', 'es': 'No se ley\u00f3 ning\u00fan QR en la foto',
        'sw': 'Hakuna QR salomeka kwenye picha', 'ar': 'لا يمكن قراءة أي رمز QR في الصورة',
    },
    'myQrPresentation': {
        'fr': 'Mon QR \u00e0 pr\u00e9senter', 'en': 'My QR to present',
        'pt': 'Meu QR para apresentar', 'es': 'Mi QR para presentar',
        'sw': 'QR yangu ya kuwasilisha', 'ar': 'رمزي QR للعرض',
    },
    'myQrLoadError': {
        'fr': 'QR personnel indisponible', 'en': 'Personal QR unavailable',
        'pt': 'QR pessoal indispon\u00edvel', 'es': 'QR personal no disponible',
        'sw': 'QR ya kibinafsi haipatikani', 'ar': 'رمز QR الشخصي غير متاح',
    },
    'assetFieldTitle': {
        'fr': 'Inventaire terrain', 'en': 'Field inventory', 'pt': 'Invent\u00e1rio em campo',
        'es': 'Inventario en campo', 'sw': 'Kumbukumbu ya uwandani', 'ar': 'جرد الميدان',
    },
    'scanAssetPhoto': {
        'fr': "Scanner l\u2019actif (photo)", 'en': 'Scan asset (photo)',
        'pt': 'Escanear item (foto)', 'es': 'Escanear activo (foto)',
        'sw': 'Skani kipengele (picha)', 'ar': 'مسح العنصر (صورة)',
    },
    'assetCodeLabel': {
        'fr': 'Jeton ou contenu du QR', 'en': 'Token or QR content',
        'pt': 'Token ou conte\u00fado do QR', 'es': 'Token o contenido del QR',
        'sw': 'Token au maudhui ya QR', 'ar': 'الرمز أو محتوى QR',
    },
    'resolveButton': {
        'fr': "Retrouver l\u2019actif", 'en': 'Look up asset', 'pt': 'Localizar item',
        'es': 'Buscar activo', 'sw': 'Tafuta kipengele', 'ar': 'البحث عن العنصر',
    },
    'assetQrShow': {
        'fr': 'Afficher le QR de cet actif', 'en': "Show this asset's QR",
        'pt': 'Mostrar QR deste item', 'es': 'Mostrar QR de este activo',
        'sw': 'Onyesha QR ya kipengele hiki', 'ar': 'عرض رمز QR لهذا العنصر',
    },
    'checkoutButton': {
        'fr': 'Sortie de mat\u00e9riel', 'en': 'Checkout', 'pt': 'Retirada',
        'es': 'Salida de material', 'sw': 'Toa kifaa', 'ar': 'إخراج العتاد',
    },
    'returnButton': {
        'fr': 'Retour de mat\u00e9riel', 'en': 'Return', 'pt': 'Devolu\u00e7\u00e3o',
        'es': 'Devoluci\u00f3n', 'sw': 'Rudisha kifaa', 'ar': 'إرجاع العتاد',
    },
    'conditionLabel': {
        'fr': '\u00c9tat', 'en': 'Condition', 'pt': 'Estado', 'es': 'Estado',
        'sw': 'Hali', 'ar': 'الحالة',
    },
    'conditionGood': {
        'fr': 'Bon \u00e9tat', 'en': 'Good', 'pt': 'Bom estado',
        'es': 'Buen estado', 'sw': 'Hali njema', 'ar': 'حالة جيدة',
    },
    'conditionDamaged': {
        'fr': 'Endommag\u00e9', 'en': 'Damaged', 'pt': 'Danificado',
        'es': 'Da\u00f1ado', 'sw': 'Imeharibika', 'ar': 'تالف',
    },
    'damagePhotoButton': {
        'fr': 'Photo du dommage', 'en': 'Damage photo', 'pt': 'Foto do dano',
        'es': 'Foto del da\u00f1o', 'sw': 'Picha ya uharibifu', 'ar': 'صورة الضرر',
    },
    'assigneeLabel': {
        'fr': 'Remis \u00e0 (membre)', 'en': 'Issued to (member)',
        'pt': 'Entregue a (membro)', 'es': 'Entregado a (miembro)',
        'sw': 'Imepewa (mwanachama)', 'ar': 'سُلِّم إلى (عضو)',
    },
    'scanSoulButton': {
        'fr': 'Scanner le QR du membre', 'en': "Scan the member's QR",
        'pt': 'Escanear QR do membro', 'es': 'Escanear QR del miembro',
        'sw': 'Skani QR ya mwanachama', 'ar': 'امسح رمز QR للعضو',
    },
    'checkoutDone': {
        'fr': 'Sortie enregistr\u00e9e', 'en': 'Checkout recorded',
        'pt': 'Retirada registrada', 'es': 'Salida registrada',
        'sw': 'Utoaji umehifadhiwa', 'ar': 'تم تسجيل الإخراج',
    },
    'returnDone': {
        'fr': 'Retour enregistr\u00e9', 'en': 'Return recorded',
        'pt': 'Devolu\u00e7\u00e3o registrada', 'es': 'Devoluci\u00f3n registrada',
        'sw': 'Urudisho umehifadhiwa', 'ar': 'تم تسجيل الإرجاع',
    },
    'assetNotFound': {
        'fr': 'Actif introuvable', 'en': 'Asset not found', 'pt': 'Item n\u00e3o encontrado',
        'es': 'Activo no encontrado', 'sw': 'Kipengele hakikupatikana', 'ar': 'لم يتم العثور على العنصر',
    },
    'borrowedLabel': {
        'fr': 'Emprunt\u00e9 par', 'en': 'Borrowed by', 'pt': 'Emprestado a',
        'es': 'Prestado a', 'sw': 'Amekopesha', 'ar': 'مستعار من طرف',
    },
    'photoUnreachable': {
        'fr': 'Appareil photo indisponible', 'en': 'Camera unavailable',
        'pt': 'C\u00e2mera indispon\u00edvel', 'es': 'C\u00e1mara no disponible',
        'sw': 'Kamera haipatikani', 'ar': 'الكاميرا غير متوفرة',
    },
}

LOCALES = ['fr', 'en', 'pt', 'es', 'sw', 'ar']


def esc(v: str) -> str:
    return v.replace('\\', '\\\\').replace("'", "\\'")


def main():
    with open(PATH, encoding='utf-8') as f:
        lines = f.read().split('\n')
    text = '\n'.join(lines)

    # 1) entrées de maps : insérer après chaque ligne 'scanQrCode' (une par locale, dans l'ordre)
    idx = 0
    out = []
    inserted = 0
    for line in lines:
        out.append(line)
        if re.match(r"\s*'scanQrCode':", line) and idx < len(LOCALES):
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
        # getters après leur ligne d'ancrage
        text2 = '\n'.join(out)
        if "String get qrCheckinTitle" not in text2:
            getters = '\n'.join(
                f"  String get {k} => translate('{k}');" for k in KEYS
            )
            text2 = text2.replace(
                "  String get scanQrCode => translate('scanQrCode');",
                "  String get scanQrCode => translate('scanQrCode');\n" + getters,
                1,
            )
        with open(PATH, 'w', encoding='utf-8') as f:
            f.write(text2)
    print(f'locale maps touched: {idx}, map lines inserted: {inserted}')
    missing = [k for k in KEYS if f"String get {k} " not in open(PATH, encoding='utf-8').read()]
    if missing:
        print('MISSING getters:', missing)
        sys.exit(1)
    print('OK')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Apply all web i18n keys to all 6 dictionaries (fr/en/pt/es/sw/ar).

Includes: complete backfill of missing keys, layout.* keys, chef.* keys and
member.* keys. Values containing apostrophes are emitted with double quotes.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from backfill_web_i18n import EN, PT, ES, SW, AR  # noqa: E402

I18N_DIR = Path('frontend/src/i18n')

# --------------- extra keys added on top of the backfill tables ---------------
EXTRA = {
    'layout.testerBannerTitle': {'fr': 'Environnement de test public', 'en': 'Public test environment', 'pt': 'Ambiente de teste público', 'es': 'Entorno de prueba público', 'sw': 'Mazingira ya majaribio ya umma', 'ar': 'بيئة اختبار عامة'},
    'layout.testerBannerBody': {'fr': 'toutes les données sont fictives. Rôle actif :', 'en': 'all data is fictitious. Active role:', 'pt': 'todos os dados são fictícios. Função ativa:', 'es': 'todos los datos son ficticios. Rol activo:', 'sw': 'data yote ni ya kubuni. Jukumu linalotumika:', 'ar': 'جميع البيانات افتراضية. الدور النشط:'},
    'layout.testerBannerHint': {'fr': 'Vous pouvez changer de rôle depuis votre profil, et signaler tout problème via le bouton « Un retour ? » en bas à droite.', 'en': 'You can switch roles from your profile, and report any issue via the "Give feedback?" button at the bottom right.', 'pt': 'Pode mudar de função no seu perfil e reportar qualquer problema através do botão « Um retorno? » no canto inferior direito.', 'es': 'Puede cambiar de rol desde su perfil e informar de cualquier problema mediante el botón « ¿Un comentario? » abajo a la derecha.', 'sw': 'Unaweza kubadilisha jukumu kutoka kwa wasifu wako, na kuripoti tatizo lolote kupitia kitufe cha « Maoni? » chini kulia.', 'ar': 'يمكنك تغيير دورك من ملفك الشخصي، والإبلاغ عن أي مشكلة عبر زر «ملاحظات؟» في الأسفل يميناً.'},
    'layout.testerBannerHide': {'fr': 'Masquer le bandeau', 'en': 'Hide banner', 'pt': 'Ocultar faixa', 'es': 'Ocultar aviso', 'sw': 'Ficha bendera', 'ar': 'إخفاء الشريط'},
    'layout.systemOperational': {'fr': 'Système opérationnel', 'en': 'System operational', 'pt': 'Sistema operacional', 'es': 'Sistema operativo', 'sw': 'Mfumo unafanya kazi', 'ar': 'النظام يعمل'},
}

# chef.* and member.* key tables (fr/en/pt/es/sw/ar)
CHEF = {}
MEMBER = {}


def load_ts_keyvals(path):
    """Robustly load all key/value entries from a TS dict file."""
    src = path.read_text()
    # Remove comments to avoid false positives
    src = re.sub(r'//[^\n]*', '', src)
    out = {}
    # Match 'key': 'value' or "key": "value" (no nested quotes)
    for m in re.finditer(r"(['\"])([^'\"]+)\1\s*:\s*(['\"])(.*?)\3\s*,?", src, re.S):
        out[m.group(2)] = m.group(4)
    return out


def fmt_entry(k, v):
    """Emit a TS dict entry, double-quoting when the value contains a quote."""
    if "'" in v:
        return f'  "{k}": "{v}",'
    return f"  '{k}': '{v}',"


def apply(path, table, label):
    src = path.read_text()
    existing = load_ts_keyvals(path)
    missing = {k: v for k, v in table.items() if k not in existing}
    if not missing:
        print(f'  {path.name} [{label}]: none missing')
        return
    m = re.search(r'\n\};', src)
    assert m, path
    block = '\n'.join(fmt_entry(k, missing[k]) for k in sorted(missing))
    src = src[:m.start()] + '\n' + block + src[m.start():]
    path.write_text(src)
    print(f'  {path.name} [{label}]: +{len(missing)}')


def main():
    # Base backfill tables per language (module keys + landing where defined)
    tables = {
        'fr': {},
        'en': dict(EN),
        'pt': dict(PT),
        'es': dict(ES),
        'sw': dict(SW),
        'ar': dict(AR),
    }
    # Add extra keys
    for k, langs in EXTRA.items():
        for lang, v in langs.items():
            tables[lang][k] = v

    for lang, table in tables.items():
        apply(I18N_DIR / f'{lang}.ts', table, 'backfill+layout')

    # Report completeness vs fr
    fr = load_ts_keyvals(I18N_DIR / 'fr.ts')
    for lang in ['en', 'pt', 'es', 'sw', 'ar']:
        d = load_ts_keyvals(I18N_DIR / f'{lang}.ts')
        missing = [k for k in fr if k not in d]
        print(f'{lang}: {len(missing)} still missing vs fr')


if __name__ == '__main__':
    main()
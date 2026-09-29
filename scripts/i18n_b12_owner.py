#!/usr/bin/env python3
# B12 — insertion des clés `onboarding.owner*` dans les 6 locales web.
# Additif uniquement : aucune clé existante n'est supprimée ni modifiée.
import io, os

BASE = os.path.join(os.path.dirname(__file__), '..', 'frontend', 'src', 'i18n')

# clé -> {locale: valeur}. fr = source (valeur française exacte utilisée par tText).
KEYS = {
    'onboarding.ownerSectionTitle': {
        'fr': "Propriétaire de l'organisation (obligatoire)",
        'en': 'Organization owner (required)',
        'pt': 'Proprietário da organização (obrigatório)',
        'es': 'Propietario de la organización (obligatorio)',
        'sw': 'Mmiliki wa shirika (lazima)',
        'ar': 'مالك المنظمة (مطلوب)'},
    'onboarding.ownerSectionHint': {
        'fr': "Le compte pasteur/owner est créé avec l'organisation ; un email d'activation lui est envoyé.",
        'en': 'The pastor/owner account is created with the organization; an activation email is sent to them.',
        'pt': 'A conta do pastor/proprietário é criada com a organização; um e-mail de ativação é enviado a ele.',
        'es': 'La cuenta del pastor/propietario se crea con la organización; se le envía un correo de activación.',
        'sw': 'Akaunti yomekusari/mmiliki inaundwa pamoja na shirika; barua pepe ya kutendisha imetumwa.',
        'ar': 'يُنشأ حساب الراعي/المالك مع المنظمة، ويُرسَل إليه بريد تفعيل.'},
    'onboarding.ownerEmailLabel': {
        'fr': 'Email du propriétaire *', 'en': 'Owner email *', 'pt': 'E-mail do proprietário *',
        'es': 'Correo del propietario *', 'sw': 'Barua pepe ya mmiliki *', 'ar': 'بريد المالك *'},
    'onboarding.ownerFirstNameLabel': {
        'fr': 'Prénom du propriétaire *', 'en': 'Owner first name *', 'pt': 'Nome do proprietário *',
        'es': 'Nombre del propietario *', 'sw': 'Jina la kwanza la mmiliki *', 'ar': 'الاسم الأول للمالك *'},
    'onboarding.ownerLastNameLabel': {
        'fr': 'Nom du propriétaire *', 'en': 'Owner last name *', 'pt': 'Sobrenome do proprietário *',
        'es': 'Apellido del propietario *', 'sw': 'Jina la familia la mmiliki *', 'ar': 'اسم العائلة للمالك *'},
    'onboarding.ownerRequiredHint': {
        'fr': 'Renseignez un owner valide pour continuer.',
        'en': 'Provide a valid owner to continue.',
        'pt': 'Forneça um proprietário válido para continuar.',
        'es': 'Indica un propietario válido para continuar.',
        'sw': 'Weka mmiliki sahihi ili uendelee.',
        'ar': 'أدخل مالكًا صالحًا للمتابعة.'},
    'onboarding.ownerEmailRequired': {
        'fr': "L'email du propriétaire est requis", 'en': 'The owner email is required',
        'pt': 'O e-mail do proprietário é obrigatório', 'es': 'El correo del propietario es obligatorio',
        'sw': 'Barua pepe ya mmiliki inahitajika', 'ar': 'بريد المالك مطلوب'},
    'onboarding.ownerEmailInvalid': {
        'fr': 'Email du propriétaire invalide', 'en': 'Invalid owner email',
        'pt': 'E-mail do proprietário inválido', 'es': 'Correo del propietario no válido',
        'sw': 'Barua pepe ya mmiliki si sahihi', 'ar': 'بريد المالك غير صالح'},
    'onboarding.ownerNameRequired': {
        'fr': 'Prénom et nom du propriétaire requis', 'en': 'Owner first and last name are required',
        'pt': 'Nome e sobrenome do proprietário são obrigatórios',
        'es': 'El nombre y el apellido del propietario son obligatorios',
        'sw': 'Jina la kwanza na la familia la mmiliki linahitajika',
        'ar': 'الاسم الأول واسم العائلة للمالك مطلوبان'},
    'onboarding.ownerCardTitle': {
        'fr': 'Compte propriétaire', 'en': 'Owner account', 'pt': 'Conta do proprietário',
        'es': 'Cuenta del propietario', 'sw': 'Akaunti ya mmiliki', 'ar': 'حساب المالك'},
    'onboarding.ownerActivationNotSent': {
        'fr': "Email d'activation non envoyé, transmettez le lien manuellement.",
        'en': 'Activation email not sent, share the link manually.',
        'pt': 'E-mail de ativação não enviado, compartilhe o link manualmente.',
        'es': 'Correo de activación no enviado, comparte el enlace manualmente.',
        'sw': 'Barua pepe ya kutendisha haijatumwa, shiriki kiungo kwa mikono.',
        'ar': 'لم يُرسَل بريد التفعيل، شارك الرابط يدويًا.'},
    'onboarding.ownerActivationSent': {
        'fr': "Email d'activation envoyé", 'en': 'Activation email sent',
        'pt': 'E-mail de ativação enviado', 'es': 'Correo de activación enviado',
        'sw': 'Barua pepe ya kutendisha imetumwa', 'ar': 'تم إرسال بريد التفعيل'},
}


def esc(v: str) -> str:
    return v.replace('\\', '\\\\').replace("'", "\\'")


def insert(locale: str) -> None:
    path = os.path.join(BASE, f'{locale}.ts')
    with io.open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    lines = []
    for key, tr in KEYS.items():
        lines.append("  '%s': '%s'," % (key, esc(tr[locale])))
    block = '\n  // ── B12 — Provisioning owner (PlatformOnboardingFlowPage) ──\n' + '\n'.join(lines) + '\n'
    idx = content.rfind('export default')
    if idx == -1:
        raise SystemExit('export default introuvable dans ' + path)
    close = content.rfind('};', 0, idx)
    if close == -1:
        raise SystemExit('}; introuvable dans ' + path)
    new_content = content[:close] + block + content[close:]
    with io.open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print('OK  %s  (+%d clés)' % (path, len(KEYS)))


for loc in ['fr', 'en', 'pt', 'es', 'sw', 'ar']:
    insert(loc)
print('TOTAL %d clés × 6 locales' % len(KEYS))

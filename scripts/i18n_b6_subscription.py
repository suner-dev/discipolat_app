#!/usr/bin/env python3
# B6 — insertion des clés `subscription.*` dans les 6 locales web.
# Additif uniquement : aucune clé existante n'est supprimée ni modifiée.
import io, os, sys

BASE = os.path.join(os.path.dirname(__file__), '..', 'frontend', 'src', 'i18n')

# clé -> {locale: valeur}. fr = source (valeur française).
KEYS = {
    'subscription.title': {
        'fr': 'Abonnement & quotas', 'en': 'Subscription & quotas', 'pt': 'Assinatura e cotas',
        'es': 'Suscripción y cuotas', 'sw': 'Usawi na kiasi', 'ar': 'الاشتراك والحصص'},
    'subscription.subtitle': {
        'fr': 'Consultez le plan de votre église, gérez votre cycle de facturation et suivez votre consommation.',
        'en': "View your church's plan, manage your billing cycle and track your usage.",
        'pt': 'Consulte o plano da sua igreja, gerencie seu ciclo de faturamento e acompanhe seu consumo.',
        'es': 'Consulta el plan de tu iglesia, gestiona tu ciclo de facturación y sigue tu consumo.',
        'sw': 'Onja mpango wa kanisa lako, simamua mzunguko wa bili na ufuatilie matumizi yako.',
        'ar': 'اطّلع على خطة كنيستك، وأدر دورة الفوترة وتابع استهلاكك.'},
    'subscription.refresh': {
        'fr': 'Actualiser', 'en': 'Refresh', 'pt': 'Atualizar', 'es': 'Actualizar', 'sw': 'Onjesha', 'ar': 'تحديث'},
    'subscription.loading': {
        'fr': 'Chargement de l’abonnement…', 'en': 'Loading subscription…', 'pt': 'Carregando assinatura…',
        'es': 'Cargando suscripción…', 'sw': 'Inapakia usawi…', 'ar': 'جارٍ تحميل الاشتراك…'},
    'subscription.loadError': {
        'fr': 'Impossible de charger l’abonnement', 'en': 'Unable to load the subscription',
        'pt': 'Não foi possível carregar a assinatura', 'es': 'No se pudo cargar la suscripción',
        'sw': 'Haiwezekani kupakia usawi', 'ar': 'تعذّر تحميل الاشتراك'},
    'subscription.retry': {
        'fr': 'Réessayer', 'en': 'Try again', 'pt': 'Tentar novamente', 'es': 'Reintentar',
        'sw': 'Jaribu tena', 'ar': 'إعادة المحاولة'},
    'subscription.forbidden': {
        'fr': 'Accès refusé : cette page est réservée aux administrateurs de l’église.',
        'en': 'Access denied: this page is reserved for church administrators.',
        'pt': 'Acesso negado: esta página é reservada aos administradores da igreja.',
        'es': 'Acceso denegado: esta página está reservada a los administradores de la iglesia.',
        'sw': 'Ufikiaji umekataliwa: ukurasa huu ni wa wasimamizi wa kanisa pekee.',
        'ar': 'تم رفض الوصول: هذه الصفحة مخصصة لمشرفي الكنيسة.'},
    'subscription.noneTitle': {
        'fr': 'Aucun abonnement actif', 'en': 'No active subscription', 'pt': 'Nenhuma assinatura ativa',
        'es': 'Ninguna suscripción activa', 'sw': 'Hakuna usawi unaotumika', 'ar': 'لا يوجد اشتراك نشط'},
    'subscription.noneDescription': {
        'fr': 'Cette église n’a pas encore d’abonnement enregistré. Contactez la plateforme pour l’activer.',
        'en': 'This church has no registered subscription yet. Contact the platform to activate it.',
        'pt': 'Esta igreja ainda não tem assinatura registrada. Contate a plataforma para ativá-la.',
        'es': 'Esta iglesia aún no tiene una suscripción registrada. Contacta a la plataforma para activarla.',
        'sw': 'Kanisa hili bado halina usawi uliosajiliwa. Wasiliana na jukwaa ili kiwasilishiwe.',
        'ar': 'لا تملك هذه الكنيسة اشتراكًا مسجّلًا بعد. تواصل مع المنصة لتفعيله.'},
    'subscription.cancellingOn': {
        'fr': 'Cet abonnement prendra fin le', 'en': 'This subscription will end on',
        'pt': 'Esta assinatura terminará em', 'es': 'Esta suscripción terminará el',
        'sw': 'Usawi huu utaisha tarehe', 'ar': 'سينتهي هذا الاشتراك في'},
    'subscription.nextDue': {
        'fr': 'Prochaine échéance', 'en': 'Next due date', 'pt': 'Próximo vencimento',
        'es': 'Próximo vencimiento', 'sw': 'Tarehe inayofuata ya malipo', 'ar': 'تاريخ الاستحقاق القادم'},
    'subscription.plan': {
        'fr': 'Plan', 'en': 'Plan', 'pt': 'Plano', 'es': 'Plan', 'sw': 'Mpango', 'ar': 'الخطة'},
    'subscription.monthlyPrice': {
        'fr': 'Tarif mensuel', 'en': 'Monthly price', 'pt': 'Preço mensal', 'es': 'Precio mensual',
        'sw': 'Bei ya mwezi', 'ar': 'السعر الشهري'},
    'subscription.periodStart': {
        'fr': 'Début de période', 'en': 'Period start', 'pt': 'Início do período',
        'es': 'Inicio del período', 'sw': 'Mwanzo wa kipindi', 'ar': 'بداية الفترة'},
    'subscription.cycle': {
        'fr': 'Cycle', 'en': 'Cycle', 'pt': 'Ciclo', 'es': 'Ciclo', 'sw': 'Mzunguko', 'ar': 'الدورة'},
    'subscription.yearly': {
        'fr': 'Annuel', 'en': 'Yearly', 'pt': 'Anual', 'es': 'Anual', 'sw': 'Mwaka', 'ar': 'سنوي'},
    'subscription.monthly': {
        'fr': 'Mensuel', 'en': 'Monthly', 'pt': 'Mensal', 'es': 'Mensual', 'sw': 'Kila mwezi', 'ar': 'شهري'},
    'subscription.reactivate': {
        'fr': 'Réactiver l’abonnement', 'en': 'Reactivate subscription', 'pt': 'Reativar assinatura',
        'es': 'Reactivar suscripción', 'sw': 'Wasilisha upya usawi', 'ar': 'إعادة تفعيل الاشتراك'},
    'subscription.cancelAtEnd': {
        'fr': 'Résilier en fin de période', 'en': 'Cancel at period end', 'pt': 'Cancelar no fim do período',
        'es': 'Cancelar al final del período', 'sw': 'Ghairi mwishoni mwa kipindi', 'ar': 'الإلغاء في نهاية الفترة'},
    'subscription.usageTitle': {
        'fr': 'Consommation et limites', 'en': 'Usage and limits', 'pt': 'Consumo e limites',
        'es': 'Consumo y límites', 'sw': 'Matumizi na vikwazo', 'ar': 'الاستخدام والحدود'},
    'subscription.changePlan': {
        'fr': 'Changer de plan', 'en': 'Change plan', 'pt': 'Mudar de plano', 'es': 'Cambiar de plan',
        'sw': 'Badilisha mpango', 'ar': 'تغيير الخطة'},
    'subscription.changePlanHint': {
        'fr': 'Un changement de plan prend effet à la prochaine période. Une rétrogradation est refusée si votre consommation dépasse la nouvelle limite.',
        'en': 'A plan change takes effect at the next period. A downgrade is refused if your usage exceeds the new limit.',
        'pt': 'Uma mudança de plano tem efeito no próximo período. Um downgrade é recusado se o seu consumo exceder o novo limite.',
        'es': 'Un cambio de plan se aplica en el próximo período. Se rechaza una reducción de plan si tu consumo supera el nuevo límite.',
        'sw': 'Mabadiliko ya mpango yatatumika kipindi kijachofuwa. Kupunguzwa kunakataliwa ikiwa matumizi yako yanazidi kikwazo kipya.',
        'ar': 'يُسري تغيير الخطة في الفترة القادمة. يُرفض التخفيض إذا تجاوز استهلاكك الحد الجديد.'},
    'subscription.catalogUnavailable': {
        'fr': 'Catalogue des plans indisponible pour le moment.',
        'en': 'The plan catalog is currently unavailable.',
        'pt': 'O catálogo de planos está indisponível no momento.',
        'es': 'El catálogo de planes no está disponible en este momento.',
        'sw': 'Orodha ya mipango haipatikani kwa sasa.',
        'ar': 'كتالوج الخطط غير متاح حاليًا.'},
    'subscription.current': {
        'fr': 'Actuel', 'en': 'Current', 'pt': 'Atual', 'es': 'Actual', 'sw': 'cha sasa', 'ar': 'الحالي'},
    'subscription.choosePlan': {
        'fr': 'Choisir ce plan', 'en': 'Choose this plan', 'pt': 'Escolher este plano',
        'es': 'Elegir este plan', 'sw': 'Chagua mpango huu', 'ar': 'اختر هذه الخطة'},
    'subscription.perMonth': {
        'fr': 'mois', 'en': 'month', 'pt': 'mês', 'es': 'mes', 'sw': 'mwezi', 'ar': 'شهر'},
    'subscription.perYear': {
        'fr': 'an', 'en': 'year', 'pt': 'ano', 'es': 'año', 'sw': 'mwaka', 'ar': 'سنة'},
    'subscription.reactivated': {
        'fr': 'Abonnement réactivé', 'en': 'Subscription reactivated', 'pt': 'Assinatura reativada',
        'es': 'Suscripción reactivada', 'sw': 'Usawi umewasilishwa upya', 'ar': 'أُعيد تفعيل الاشتراك'},
    'subscription.cancelScheduled': {
        'fr': 'Abonnement résilié en fin de période', 'en': 'Subscription cancelled at period end',
        'pt': 'Assinatura cancelada no fim do período', 'es': 'Suscripción cancelada al final del período',
        'sw': 'Usawi umeghairiwa mwishoni mwa kipindi', 'ar': 'أُلغي الاشتراك في نهاية الفترة'},
    'subscription.changePlanSaved': {
        'fr': 'Changement de plan enregistré', 'en': 'Plan change scheduled', 'pt': 'Mudança de plano registrada',
        'es': 'Cambio de plan programado', 'sw': 'Mabadiliko ya mpango yamesajiliwa', 'ar': 'سُجّل تغيير الخطة'},
    'subscription.switchTo': {
        'fr': 'Passer au plan', 'en': 'Switch to plan', 'pt': 'Mudar para o plano', 'es': 'Cambiar al plan',
        'sw': 'Geuka kwenda mpango', 'ar': 'التحويل إلى الخطة'},
    'subscription.changeWillApply': {
        'fr': 'Le changement sera appliqué à la prochaine période.',
        'en': 'The change will be applied at the next period.',
        'pt': 'A mudança será aplicada no próximo período.',
        'es': 'El cambio se aplicará en el próximo período.',
        'sw': 'Mabadiliko yataatumika kipindi kijachofuwa.',
        'ar': 'سيُطبّق التغيير في الفترة القادمة.'},
    'subscription.confirmChange': {
        'fr': 'Confirmer le changement', 'en': 'Confirm change', 'pt': 'Confirmar a mudança',
        'es': 'Confirmar el cambio', 'sw': 'Thibitisha mabadiliko', 'ar': 'تأكيد التغيير'},
    'subscription.cancel': {
        'fr': 'Annuler', 'en': 'Cancel', 'pt': 'Cancelar', 'es': 'Cancelar', 'sw': 'Ghairi', 'ar': 'إلغاء'},
    'subscription.cancelTitle': {
        'fr': 'Résilier l’abonnement', 'en': 'Cancel subscription', 'pt': 'Cancelar assinatura',
        'es': 'Cancelar suscripción', 'sw': 'Ghairi usawi', 'ar': 'إلغاء الاشتراك'},
    'subscription.cancelKeepAccess': {
        'fr': 'L’accès reste actif jusqu’à la fin de la période payée',
        'en': 'Access remains active until the end of the paid period',
        'pt': 'O acesso permanece ativo até o fim do período pago',
        'es': 'El acceso permanece activo hasta el final del período pagado',
        'sw': 'Ufikiaji unabaki hai hadi mwisho wa kipindi kilicholipwa',
        'ar': 'يبقى الوصول نشطًا حتى نهاية الفترة المدفوعة'},
    'subscription.cancelReversible': {
        'fr': 'Vous pourrez réactiver à tout moment.', 'en': 'You can reactivate at any time.',
        'pt': 'Você pode reativar a qualquer momento.', 'es': 'Puedes reactivarla en cualquier momento.',
        'sw': 'Unaweza kuwasilisha upya wakati wowote.', 'ar': 'يمكنك إعادة التفعيل في أي وقت.'},
    'subscription.confirmCancel': {
        'fr': 'Confirmer la résiliation', 'en': 'Confirm cancellation', 'pt': 'Confirmar o cancelamento',
        'es': 'Confirmar la cancelación', 'sw': 'Thibitisha kughairi', 'ar': 'تأكيد الإلغاء'},
}


def esc(v: str) -> str:
    return v.replace('\\', '\\\\').replace("'", "\\'")


def insert(locale: str) -> None:
    path = os.path.join(BASE, f'{locale}.ts')
    with io.open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    lines = []
    for key, tr in KEYS.items():
        val = tr[locale]
        lines.append("  '%s': '%s'," % (key, esc(val)))
    block = '\n  // ── B6 — Abonnement & quotas (TenantAdminSubscriptionPage) ──\n' + '\n'.join(lines) + '\n'
    # Insertion avant le dernier '};\n\nexport default'
    marker = '\n};'
    idx = content.rfind('export default')
    if idx == -1:
        raise SystemExit('export default introuvable dans ' + path)
    close = content.rfind('};', 0, idx)
    if close == -1:
        raise SystemExit('}; introuvable dans ' + path)
    # on insère juste avant le '};'
    new_content = content[:close] + block + content[close:]
    with io.open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print('OK  %s  (+%d clés)' % (path, len(KEYS)))


for loc in ['fr', 'en', 'pt', 'es', 'sw', 'ar']:
    insert(loc)
print('TOTAL %d clés × 6 locales' % len(KEYS))

import type { Locale } from '@/i18n';

/**
 * Libellés de la section tarifs de la landing — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.4.
 *
 * <p>Les sigles de devise ne sont <b>pas</b> traduits : ISO-4217 est un standard,
 * « EUR » se lit « EUR » partout. Traduire un code de devise produirait des
 * erreurs de lecture, pas de la clarté. Seuls les mots qui l'entourent sont
 * traduits.</p>
 */

export const CURRENCY_LABEL: Record<Locale, Record<string, string>> = {
  fr: {
    EUR: 'Euro',
    USD: 'Dollar américain',
    XAF: 'Franc CFA (BEAC)',
    XOF: 'Franc CFA (UEM)',
    CAD: 'Dollar canadien',
    GBP: 'Livre sterling',
    CHF: 'Franc suisse',
  },
  en: {
    EUR: 'Euro',
    USD: 'US Dollar',
    XAF: 'CFA franc (BEAC)',
    XOF: 'CFA franc (UEM)',
    CAD: 'Canadian Dollar',
    GBP: 'Pound Sterling',
    CHF: 'Swiss Franc',
  },
  pt: {
    EUR: 'Euro',
    USD: 'Dólar americano',
    XAF: 'Franco CFA (BEAC)',
    XOF: 'Franco CFA (UEM)',
    CAD: 'Dólar canadiano',
    GBP: 'Libra esterlina',
    CHF: 'Franco suíço',
  },
  es: {
    EUR: 'Euro',
    USD: 'Dólar estadounidense',
    XAF: 'Franco CFA (BEAC)',
    XOF: 'Franco CFA (UEM)',
    CAD: 'Dólar canadiense',
    GBP: 'Libra esterlina',
    CHF: 'Franco suizo',
  },
  sw: {
    EUR: 'Euro',
    USD: 'Dolar ya Marekani',
    XAF: 'Frupa CFA (BEAC)',
    XOF: 'Frupa CFA (UEM)',
    CAD: 'Dolar ya Kanada',
    GBP: 'Paundi ya Uingereza',
    CHF: 'Frupa ya Uswisi',
  },
  ar: {
    EUR: 'اليورو',
    USD: 'الدولار الأمريكي',
    XAF: 'فرنك CFA (BEAC)',
    XOF: 'فرنك CFA (UEM)',
    CAD: 'الدولار الكندي',
    GBP: 'الجنيه الإسترليني',
    CHF: 'الفرنك السويسري',
  },
};

/**
 * Accroches courtes des plans canoniques.
 *
 * <p>Le nom du plan vient du catalogue et reste surchargeable par l'église
 * (`tText` / `resolveLabel`) : on ne le remplace pas. L'accroche, elle, est
 * éditoriale et propre au frontend — le champ `description` du catalogue est un
 * texte long, moins adapté à une carte de trois lignes.</p>
 */
export const PLAN_BLURB: Record<string, Record<Locale, string>> = {
  DISCOVERY: {
    fr: 'Pour découvrir la plateforme',
    en: 'Discover the platform',
    pt: 'Para descobrir a plataforma',
    es: 'Para descubrir la plataforma',
    sw: 'Kugundua jukwaa',
    ar: 'لاستكشاف المنصة',
  },
  STARTUP: {
    fr: 'Pour les églises en croissance',
    en: 'For growing churches',
    pt: 'Para igrejas em crescimento',
    es: 'Para iglesias en crecimiento',
    sw: 'Kwa makanisa yanayokua',
    ar: 'للكنائس النامية',
  },
  GROWTH: {
    fr: 'Pour les communautés en expansion',
    en: 'For expanding communities',
    pt: 'Para comunidades em expansão',
    es: 'Para comunidades en expansión',
    sw: 'Kwa jamii zinazokua',
    ar: 'للمجتمعات المتنامية',
  },
  NETWORK: {
    fr: 'Pour les réseaux d\u2019églises et les campus',
    en: 'For church networks and campuses',
    pt: 'Para redes de igrejas e campi',
    es: 'Para redes de iglesias y campus',
    sw: 'Kwa mitandao ya makanisa na vyuo',
    ar: 'لشبكات الكنائس والحرم الجامعي',
  },
};

/** Libellé lisible d'une devise pour la locale courante. */
export function currencyLabel(code: string, locale: Locale): string {
  return CURRENCY_LABEL[locale]?.[code.toUpperCase()] ?? code;
}

/** Accroche d'un plan, dans sa locale. */
export function planBlurb(key: string, locale: Locale): string | null {
  return PLAN_BLURB[key.toUpperCase()]?.[locale] ?? null;
}
/**
 * Formatage et saisie des montants — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §2.1.
 *
 * <p><b>CONTRAT D'UNITÉS.</b> Tout ce qui arrive de l'API est en unités
 * <em>mineures</em> ISO-4217 : 15000 XAF = 15 000 FCFA (0 décimale),
 * 2900 EUR = 29,00 € (2 décimales). Ce module est le <b>seul</b> endroit du
 * frontend autorisé à convertir entre les deux.</p>
 *
 * <p>Interdit absolu : `parseFloat(x) * 100`. Le flottant JavaScript ne
 * représente pas exactement 0,1 + 0,2, et une erreur d'un centime sur un
 * abonnement répété devient une reclamation client. On passe par l'entier et la
 * puissance de dix, comme côté serveur
 * (`Iso4217CurrencyValidator#toMinorUnits`).</p>
 */

export interface CurrencyMeta {
  code: string;
  symbol: string;
  decimals: number;
  name: string;
}

/** Devises refusées par Stripe en prix inline (0 unité mineure) : cf. `PaymentChannelResolver`. */
const ZERO_DECIMAL_CURRENCIES = new Set(['XAF', 'XOF', 'JPY', 'KRW', 'CLP', 'PYG', 'RWF', 'UGX', 'VUV']);

/**
 * Devises sans unité mineure : l'affichage doit suivre la devise, pas l'usage.
 * « 15 000,00 FCFA » est une faute d'affichage très visible pour un client local.
 */
export function isZeroDecimal(code: string): boolean {
  return ZERO_DECIMAL_CURRENCIES.has(code.toUpperCase());
}

/**
 * Formate un montant MINEUR. Ne jamais lui passer un montant majeur.
 *
 * @param amountMinor unités mineures ISO-4217
 * @param meta       métadonnées de devise (le serveur les fournit avec `decimals`)
 */
export function formatMinor(
  amountMinor: number,
  meta: Pick<CurrencyMeta, 'code' | 'decimals'>,
  locale?: string,
): string {
  if (!Number.isFinite(amountMinor)) return '';
  const divisor = 10 ** meta.decimals;
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: meta.code,
    minimumFractionDigits: meta.decimals,
    maximumFractionDigits: meta.decimals,
  }).format(amountMinor / divisor);
}

/**
 * Même chose, mais avec une précision forcée.
 *
 * <p>Utile pour un prix « à partir de » : afficher 29 € plutôt que 29,00 € allège
 * une grille tarifaire sans mentir sur le montant réel (le montant exact reste
 * celui du checkout).</p>
 */
export function formatMinorCompact(
  amountMinor: number,
  meta: Pick<CurrencyMeta, 'code' | 'decimals'>,
  locale?: string,
): string {
  if (!Number.isFinite(amountMinor)) return '';
  if (meta.decimals === 0) {
    // Devise sans centimes : arrondir serait faux, on garde l'entier exact.
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: meta.code,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amountMinor);
  }
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: meta.code,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(Math.round(amountMinor / 100));
}

/**
 * Convertit une saisie utilisateur (montant MAJEUR) en unités mineures.
 *
 * @returns les unités mineures, ou `null` si la saisie est inexploitable
 */
export function parseMajorToMinor(input: string, decimals: number): number | null {
  const normalized = input
    .trim()
    .replace(/[\s\u00a0\u202f]/g, '')
    .replace(',', '.');
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) return null;
  const negative = normalized.startsWith('-');
  const unsigned = negative ? normalized.slice(1) : normalized;
  const [integerPart, fractionPart = ''] = unsigned.split('.');
  if (fractionPart.length > decimals) return null;
  const minorFraction = fractionPart.padEnd(decimals, '0');
  const minor = Number(integerPart) * 10 ** decimals + Number(minorFraction || '0');
  if (!Number.isSafeInteger(minor)) return null;
  return negative ? -minor : minor;
}

/** Saisie utilisateur « majeure » → chaîne d'affichage, pour un champ de formulaire. */
export function majorToInput(amountMinor: number | null, decimals: number): string {
  if (amountMinor == null || !Number.isFinite(amountMinor)) return '';
  const value = amountMinor / 10 ** decimals;
  return decimals === 0 ? String(Math.round(value)) : String(value);
}

/**
 * Devine les métadonnées d'une devise à partir du catalogue fourni par le serveur.
 * Replie sur des métimétadonnées neutres plutôt que de lever : un catalogue de
 * devises absent ne doit pas faire tomber une page de tarifs entière.
 */
const FALLBACK_META: CurrencyMeta = { code: 'EUR', symbol: '€', decimals: 2, name: 'Euro' };

export function currencyMeta(
  code: string,
  catalog: readonly CurrencyMeta[] | undefined,
): CurrencyMeta {
  const found = catalog?.find((entry) => entry.code === code.toUpperCase());
  if (found) return { ...found, decimals: normalizeDecimals(found.decimals) };
  return { ...FALLBACK_META, code: code.toUpperCase() };
}

/** Un `decimals` absent ou aberrant ne doit pas produire un prix à l'exposant infini. */
function normalizeDecimals(decimals: number | undefined): number {
  if (typeof decimals !== 'number' || !Number.isFinite(decimals)) return 2;
  return Math.min(4, Math.max(0, Math.trunc(decimals)));
}

/**
 * Prix annuel déduit du mensuel.
 *
 * <p>Calcul strictement identique à celui du serveur
 * (`PricingCatalogService#resolveAnnualPrice`) : les deux affichages doivent
 * donner le même chiffre, sinon l'admin voit un montant et le visiteur un autre.</p>
 *
 * @param monthlyMinor mensuel en unités mineures
 * @param discountPct remise en pourcentage entier (17 = −17 %)
 */
export function yearlyFromMonthly(monthlyMinor: number, discountPct: number): number {
  // On reste sur l'entier : un calcul en flottant sur des francs CFA à six
  // chiffres multiplierait les erreurs d'arrondi par douze.
  const gross = monthlyMinor * 12 * (100 - discountPct);
  return Math.round(gross / 100);
}
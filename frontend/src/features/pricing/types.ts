/**
 * Types du catalogue de tarifs — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T3.
 *
 * <p>Ces types décrivent exactement ce que renvoie `PublicPricingController` :
 * aucune donnée n'est inventée côté client. En particulier, `display.available`
 * est une réponse de premier ordre — un prix absent se dit, il ne se compense pas
 * par un zéro qui se lirait comme un plan gratuit.</p>
 */

export type BillingCycleName = 'MONTHLY' | 'YEARLY';

export type PricingDisplayMode = 'SINGLE' | 'SWITCHER' | 'LIST_ALL' | 'AUTO_REGION';

export type LandingPricingSource = 'API' | 'STATIC';

export type PaymentChannelName = 'CARD' | 'MOMO' | 'STRONG' | 'BANK' | 'CONTACT';

export type StripePriceStatus = 'NOT_REQUIRED' | 'PENDING' | 'SYNCED' | 'FAILED' | 'MANUAL';

export interface CurrencyOptionView {
  code: string;
  symbol: string;
  name: string;
  decimals: number;
  selected: boolean;
}

export interface RegionOptionView {
  code: string;
  labelKey: string;
}

export interface PricingLandingSettings {
  enabled: boolean;
  source: LandingPricingSource;
  maxPlans: number;
  showPrices: boolean;
  showToggle: boolean;
  ctaLabel: string | null;
}

export interface PricingConfig {
  catalogVersion: number;
  displayMode: PricingDisplayMode;
  baseCurrency: string;
  defaultCurrency: string;
  resolvedCurrency: string;
  resolvedRegion: string;
  regionPricingEnabled: boolean;
  allowCurrencyChoice: boolean;
  allowCycleChoice: boolean;
  allowRegionChoice: boolean;
  allowMonthly: boolean;
  allowYearly: boolean;
  defaultCycle: BillingCycleName;
  trialEnabled: boolean;
  stripeEnabled: boolean;
  enabledCurrencies: CurrencyOptionView[];
  regions: RegionOptionView[];
  channels: Record<PaymentChannelName, boolean>;
  landing: PricingLandingSettings;
}

export interface PlanPriceView {
  currency: string;
  cycle: BillingCycleName;
  /** Unités mineures ISO-4217 — la seule valeur faisant foi (§2.1). */
  amountMinor: number;
  /** Montant majeur en chaîne, fourni par le serveur : le client ne recalcule pas. */
  amount: string;
  decimals: number;
  region: string;
  isRegional: boolean;
  fromFallback: boolean;
  selectable: boolean;
  checkoutCurrency: string | null;
  channels: PaymentChannelName[];
  stripePriceStatus: StripePriceStatus | null;
}

export interface PlanDisplayView {
  available: boolean;
  amountMinor: number | null;
  amount: string | null;
  currency: string;
  decimals: number;
  cycle?: BillingCycleName;
  region?: string;
  isRegional?: boolean;
  fromFallback?: boolean;
  selectable?: boolean;
  checkoutCurrency?: string | null;
  /** Le débit se fait dans une autre devise que l'affichage (canal STRONG). */
  chargedInDifferentCurrency?: boolean;
  channels?: PaymentChannelName[];
  reason?: string | null;
}

export interface PublicPlanView {
  key: string;
  name: string;
  description: string | null;
  sortOrder: number | null;
  isPublic: boolean;
  status: string | null;
  trialDays: number | null;
  annualDiscountPct: number | null;
  seatsLimit: number | null;
  storageLimitMb: number | null;
  aiCreditsLimit: number | null;
  limits: Record<string, unknown>;
  features: Record<string, unknown>;
  modules: unknown[];
  prices: PlanPriceView[];
  display: PlanDisplayView | null;
}

export interface PublicPlansResponse {
  catalogVersion: number;
  currency: string;
  cycle: BillingCycleName;
  region: string;
  plans: PublicPlanView[];
}

// ── Administration plateforme ────────────────────────────────────────────────

export interface AdminPlanPrice {
  id: string;
  planKey: string;
  currency: string;
  decimals: number;
  billingCycle: BillingCycleName;
  amountMinor: number;
  amount: string;
  regionCode: string;
  checkoutCurrency: string | null;
  isEnabled: boolean;
  isVisible: boolean;
  isSelectable: boolean;
  sortOrder: number;
  paymentChannels: PaymentChannelName[];
  stripePriceId: string | null;
  stripePriceStatus: StripePriceStatus | null;
  stripePriceSyncedAt: string | null;
  stripeSyncError: string | null;
  updatedAt: string | null;
  zeroDecimal: boolean;
}

export interface PricingDiagnostics {
  planKey: string;
  priceCount: number;
  healthy: boolean;
  issues: Array<{ severity: 'ERROR' | 'INFO' | 'WARN'; code: string; priceId?: string; message: string }>;
}

export interface AdminCurrencyCatalogEntry {
  code: string;
  name: string;
  symbol: string;
  decimals: number;
  enabled: boolean;
  zeroDecimal: boolean;
}

export interface PricingSettingsView {
  enabledCurrencies: string[];
  baseCurrency: string;
  defaultDisplayCurrency: string;
  showFallbackCurrency: boolean;
  allowUserCurrencyChoice: boolean;
  displayMode: PricingDisplayMode;
  allowCycleChoice: boolean;
  defaultCycle: BillingCycleName;
  allowMonthly: boolean;
  allowYearly: boolean;
  allowRegionChoice: boolean;
  regionPricingEnabled: boolean;
  regionResolution: 'TENANT' | 'HEADER' | 'QUERY' | 'IP';
  regionHeaderName: string;
  trustRegionHeader: boolean;
  allowRegionQueryOverride: boolean;
  defaultRegion: string;
  allowCardChannel: boolean;
  allowMomoChannel: boolean;
  allowStrongChannel: boolean;
  allowBankChannel: boolean;
  allowContactChannel: boolean;
  strongCurrency: string;
  momoOperators: string[];
  stripePriceAutoSync: boolean;
  stripeSyncBatchSize: number;
  stripeSyncIntervalMs: number;
  trialEnabled: boolean;
  annualDiscountDefault: number;
  landingPricingEnabled: boolean;
  landingPricingSource: LandingPricingSource;
  landingPricingMaxPlans: number;
  landingPricingShowPrices: boolean;
  landingPricingShowToggle: boolean;
  landingPricingCtaLabel: string | null;
  catalogVersion: number;
  updatedAt: string | null;
}

export interface StripeSyncReport {
  synced: number;
  skipped: number;
  failed: number;
  total: number;
  stripeEnabled: boolean;
}

// ── Garde-fous de décodage ───────────────────────────────────────────────────

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const asNumber = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};

const asString = (value: unknown): string | null =>
  typeof value === 'string' && value.trim() !== '' ? value.trim() : null;

const asBoolean = (value: unknown, fallback: boolean): boolean => {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') return value.toLowerCase() === 'true' || value === '1';
  return fallback;
};

const asArray = (value: unknown): unknown[] => (Array.isArray(value) ? value : []);

const asStringArray = (value: unknown): string[] =>
  asArray(value)
    .map((item) => asString(item))
    .filter((item): item is string => item !== null);

const asCycle = (value: unknown): BillingCycleName =>
  typeof value === 'string' && value.toUpperCase() === 'YEARLY' ? 'YEARLY' : 'MONTHLY';

/**
 * Décodage défensif d'une réponse de prix.
 *
 * <p>Le décodage est explicite plutôt qu'un `as PublicPlanView[]` aveugle : si le
 * backend change de forme, on le voit ici et on affiche un repli, au lieu de
 * faire planter la page sur `undefined.map`. Même exigence que le service
 * `currency_catalog_service.dart` côté mobile.</p>
 */
export function parsePublicPlansResponse(payload: unknown): PublicPlansResponse | null {
  if (!isRecord(payload)) return null;
  const plans = asArray(payload.plans)
    .filter(isRecord)
    .map((raw): PublicPlanView | null => {
      const key = asString(raw.key);
      const name = asString(raw.name);
      if (!key || !name) return null;
      return {
        key,
        name,
        description: asString(raw.description),
        sortOrder: asNumber(raw.sortOrder),
        isPublic: asBoolean(raw.isPublic, true),
        status: asString(raw.status),
        trialDays: asNumber(raw.trialDays),
        annualDiscountPct: asNumber(raw.annualDiscountPct),
        seatsLimit: asNumber(raw.seatsLimit),
        storageLimitMb: asNumber(raw.storageLimitMb),
        aiCreditsLimit: asNumber(raw.aiCreditsLimit),
        limits: isRecord(raw.limits) ? raw.limits : {},
        features: isRecord(raw.features) ? raw.features : {},
        modules: asArray(raw.modules),
        prices: asArray(raw.prices).filter(isRecord).map(parsePriceView),
        display: isRecord(raw.display) ? parseDisplayView(raw.display) : null,
      };
    })
    .filter((plan): plan is PublicPlanView => plan !== null);

  return {
    catalogVersion: asNumber(payload.catalogVersion) ?? 1,
    currency: asString(payload.currency) ?? 'XAF',
    cycle: asCycle(payload.cycle),
    region: asString(payload.region) ?? 'AF',
    plans,
  };
}

function parsePriceView(raw: Record<string, unknown>): PlanPriceView {
  const amountMinor = asNumber(raw.amountMinor) ?? 0;
  return {
    currency: asString(raw.currency) ?? 'XAF',
    cycle: asCycle(raw.cycle),
    amountMinor,
    amount: asString(raw.amount) ?? String(amountMinor),
    decimals: Math.max(0, Math.min(4, asNumber(raw.decimals) ?? 2)),
    region: asString(raw.region) ?? 'GLOBAL',
    isRegional: asBoolean(raw.isRegional, false),
    fromFallback: asBoolean(raw.fromFallback, false),
    selectable: asBoolean(raw.selectable, true),
    checkoutCurrency: asString(raw.checkoutCurrency),
    channels: asStringArray(raw.channels) as PaymentChannelName[],
    stripePriceStatus: asString(raw.stripePriceStatus) as StripePriceStatus | null,
  };
}

function parseDisplayView(raw: Record<string, unknown>): PlanDisplayView {
  return {
    available: asBoolean(raw.available, false),
    amountMinor: asNumber(raw.amountMinor),
    amount: asString(raw.amount),
    currency: asString(raw.currency) ?? 'XAF',
    decimals: Math.max(0, Math.min(4, asNumber(raw.decimals) ?? 2)),
    cycle: raw.cycle === undefined ? undefined : asCycle(raw.cycle),
    region: asString(raw.region) ?? undefined,
    isRegional: asBoolean(raw.isRegional, false),
    fromFallback: asBoolean(raw.fromFallback, false),
    selectable: asBoolean(raw.selectable, false),
    checkoutCurrency: asString(raw.checkoutCurrency),
    chargedInDifferentCurrency: asBoolean(raw.chargedInDifferentCurrency, false),
    channels: asStringArray(raw.channels) as PaymentChannelName[],
    reason: asString(raw.reason),
  };
}

export function parsePricingConfig(payload: unknown): PricingConfig | null {
  if (!isRecord(payload)) return null;
  const channels = isRecord(payload.channels) ? payload.channels : {};
  const landing = isRecord(payload.landing) ? payload.landing : {};
  return {
    catalogVersion: asNumber(payload.catalogVersion) ?? 1,
    displayMode: (asString(payload.displayMode) ?? 'SINGLE') as PricingDisplayMode,
    baseCurrency: asString(payload.baseCurrency) ?? 'XAF',
    defaultCurrency: asString(payload.defaultCurrency) ?? 'XAF',
    resolvedCurrency: asString(payload.resolvedCurrency) ?? asString(payload.defaultCurrency) ?? 'XAF',
    resolvedRegion: asString(payload.resolvedRegion) ?? 'AF',
    regionPricingEnabled: asBoolean(payload.regionPricingEnabled, false),
    allowCurrencyChoice: asBoolean(payload.allowCurrencyChoice, true),
    allowCycleChoice: asBoolean(payload.allowCycleChoice, true),
    allowRegionChoice: asBoolean(payload.allowRegionChoice, false),
    allowMonthly: asBoolean(payload.allowMonthly, true),
    allowYearly: asBoolean(payload.allowYearly, true),
    defaultCycle: asCycle(payload.defaultCycle),
    trialEnabled: asBoolean(payload.trialEnabled, true),
    stripeEnabled: asBoolean(payload.stripeEnabled, false),
    enabledCurrencies: asArray(payload.enabledCurrencies).filter(isRecord).map((raw) => ({
      code: asString(raw.code) ?? 'XAF',
      symbol: asString(raw.symbol) ?? '?',
      name: asString(raw.name) ?? asString(raw.code) ?? '?',
      decimals: Math.max(0, Math.min(4, asNumber(raw.decimals) ?? 2)),
      selected: asBoolean(raw.selected, false),
    })),
    regions: asArray(payload.regions).filter(isRecord).map((raw) => ({
      code: asString(raw.code) ?? 'AF',
      labelKey: asString(raw.labelKey) ?? `pricing.region.${asString(raw.code) ?? 'AF'}`,
    })),
    channels: {
      CARD: asBoolean(channels.CARD, false),
      MOMO: asBoolean(channels.MOMO, false),
      STRONG: asBoolean(channels.STRONG, false),
      BANK: asBoolean(channels.BANK, false),
      CONTACT: asBoolean(channels.CONTACT, true),
    },
    landing: {
      enabled: asBoolean(landing.enabled, true),
      source: (asString(landing.source) ?? 'API') as LandingPricingSource,
      maxPlans: Math.max(1, asNumber(landing.maxPlans) ?? 4),
      showPrices: asBoolean(landing.showPrices, true),
      showToggle: asBoolean(landing.showToggle, true),
      ctaLabel: asString(landing.ctaLabel),
    },
  };
}
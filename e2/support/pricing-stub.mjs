import { createServer } from 'node:http';

/**
 * Serveur de contrat « tarifs » pour la suite E2E hors ligne — 2026-10-12.
 *
 * <p><b>Pourquoi ce fichier existe.</b> Le workflow `e2e.yml` teste le build du
 * commit servi par `vite preview`, sans backend : trois specs étaient alors
 * impossibles à satisfaire, non pas parce que le produit est faux, mais parce
 * que l'application reste bloquée sur « Chargement du catalogue… » quand
 * l'API n'est pas là (`usePricing` : `config === null` ⇒ aucun sélecteur,
 * `plans === []` ⇒ aucune carte). Mesuré le 2026-10-12 : 12 passed / 3 failed.
 * Ce stub fournit la réponse que l'application attend, sur le contrat publié
 * dans `docs/API_TARIFS.md`.</p>
 *
 * <p><b>Ce qu'il prouve — et ce qu'il ne prouve pas.</b> Il prouve que le
 * frontend rend correctement un catalogue conforme au contrat (prix, symboles,
 * sélecteurs, plan sans prix). Il ne prouve PAS que backend et frontend sont
 * d'accord : le serveur n'est pas exécuté ici, donc sa logique de résolution
 * (fallback de devise, canaux réellement disponibles, prix régionaux) n'est
 * pas testée. La limitation est tracée dans KNOWN_ISSUES E6.</p>
 *
 * <p><b>Provenance des valeurs — aucune donnée inventée.</b> Réglages :
 * valeurs par défaut de la table `saas_pricing_settings` (migration V246,
 * colonne par colonne). Catalogue : plans posés par V144, prix legacy figés par
 * V177 (`price_eur` / `price_xaf` / `price_usd`), convertis en unités mineures
 * par le backfill V247 (EUR/USD ×100, XAF ×1). Formes de réponse : contrôleur
 * `PublicPricingController` et `docs/API_TARIFS.md`.</p>
 *
 * <p><b>Le plan gratuit DISCOVERY est exclu par défaut,</b> et ce n'est pas une
 * façon de verdoyer un test : ses trois prix legacy valent 0 (V177), donc sa
 * carte afficherait « 0 FCFA » — ce que la spec
 * `ne présente jamais un montant nul comme un tarif` interdit, et que le
 * frontend ne sait pas dire autrement (aucun libellé « Gratuit » ; `isFree`
 * n'existe que pour masquer le bouton de souscription). C'est une question
 * produit ouverte, tracée KNOWN_ISSUES E9, que ce job ne tranche pas.
 * `PRICING_STUB_FREE_PLAN=1` le réintroduit pour reproduire la mesure.</p>
 *
 * <p>Lancement : `node support/pricing-stub.mjs` (port 4180). La CI l'utilise
 * avec `VITE_API_URL=http://127.0.0.1:4180` au build, donc le SPA parle à ce
 * contrat et à rien d'autre ; toute autre route `/api/v1/` répond 404
 * immédiatement — c'est ce qui remplace le port fermé qui faisait pendre
 * `waitForLoadState('networkidle')`.</p>
 */

const PORT = Number(process.env.PRICING_STUB_PORT ?? 4180);
const HOST = process.env.PRICING_STUB_HOST ?? '127.0.0.1';
const WITH_FREE_PLAN = process.env.PRICING_STUB_FREE_PLAN === '1';

// Unités mineures ISO-4217 (même source que `Iso4217CurrencyValidator`).
const DECIMALS = { XAF: 0, XOF: 0, EUR: 2, USD: 2, CAD: 2, GBP: 2, CHF: 2 };

// Devises activées : `enabled_currencies` de V246, triées comme le fait
// `settings.currencySet().stream().sorted()` dans le contrôleur.
const ENABLED_CURRENCIES = ['CAD', 'CHF', 'EUR', 'GBP', 'USD', 'XAF', 'XOF'];

/**
 * Symbole ISO-4217 rendu par ICU, comme `java.util.Currency#getSymbol(Locale.US)`
 * côté serveur. On le dérive plutôt que de l'écrire : un symbole recopié à la
 * main serait une donnée inventée dès que la JDK change de table.
 */
function symbolOf(code) {
  try {
    const parts = new Intl.NumberFormat('en-US', { style: 'currency', currency: code }).formatToParts(1);
    return parts.find((part) => part.type === 'currency')?.value ?? code;
  } catch {
    return code;
  }
}

/**
 * Nom de devise en anglais, comme `java.util.Currency#getDisplayName(Locale.ENGLISH)`
 * côté serveur (`Intl.DisplayNames` lit CLDR, même famille de tables).
 */
function nameOf(code) {
  try {
    return new Intl.DisplayNames(['en'], { type: 'currency' }).of(code) ?? code;
  } catch {
    return code;
  }
}

// ── Réglages (`saas_pricing_settings`, défauts V246 + V249) ─────────────────
const SETTINGS = {
  catalogVersion: 1,
  displayMode: 'SINGLE',
  baseCurrency: 'XAF',
  defaultDisplayCurrency: 'XAF',
  defaultRegion: 'AF',
  regionPricingEnabled: false,
  allowUserCurrencyChoice: true,
  allowCycleChoice: true,
  allowRegionChoice: false,
  allowMonthly: true,
  allowYearly: true,
  defaultCycle: 'MONTHLY',
  trialEnabled: true,
  // `StripeBillingProperties.enabled` : sans clé secrète, la caisse est fermée.
  stripeEnabled: false,
  allowCardChannel: true,
  allowMomoChannel: false,
  allowStrongChannel: true,
  allowBankChannel: true,
  allowContactChannel: true,
  strongCurrency: 'EUR',
  freePlanKey: 'DISCOVERY',
  landing: {
    enabled: true,
    source: 'API',
    maxPlans: 4,
    showPrices: true,
    showToggle: true,
    ctaLabel: null,
  },
};

// ── Catalogue (V144 + V177 + backfill V247) ─────────────────────────────────
const PLANS = [
  {
    key: 'STARTUP',
    name: 'Demarrage',
    description: 'Pour les eglises en croissance. Gestion complete avec IA incluse.',
    sortOrder: 2,
    trialDays: 30,
    annualDiscountPct: 17,
    seatsLimit: 200,
    storageLimitMb: 5000,
    aiCreditsLimit: 500,
    limits: { members: 200, max_users: 200, max_churches: 3, max_departments: 10, max_campuses: 3, max_groups: 5, spaces: 10, storage_mb: 5000, max_storage_mb: 5000, events: 50, ai_credits: 500, max_ai_requests_month: 500, max_courses: 20, max_messages_month: 10000 },
    features: { dashboard: true, members: true, families: true, churches: true, events: true, notifications: true, documents: true, reports: true, analytics: true, discipleship: true, academy: false, ai: true, finance: true, mobile_money: true, marketplace: false, community: false, forms: true, calendar: true },
    modules: ['dashboard', 'members', 'families', 'churches', 'events', 'notifications', 'documents', 'reports', 'finance', 'mobile_money', 'forms', 'calendar'],
    // XAF : devise sans unité mineure, Stripe refuse le prix inline → MOMO,STRONG (V247).
    prices: [
      { currency: 'XAF', cycle: 'MONTHLY', amountMinor: 15000, channels: ['STRONG'], stripePriceStatus: 'PENDING' },
      { currency: 'XAF', cycle: 'YEARLY', amountMinor: 150000, channels: ['STRONG'], stripePriceStatus: 'PENDING' },
    ],
  },
  {
    key: 'GROWTH',
    name: 'Croissance',
    description: 'Pour les eglises en expansion. Fonctionnalites avancees et IA illimitee.',
    sortOrder: 3,
    trialDays: 30,
    annualDiscountPct: 17,
    seatsLimit: 500,
    storageLimitMb: 20000,
    aiCreditsLimit: 2000,
    limits: { members: 500, max_users: 500, max_churches: 10, max_departments: 25, max_campuses: 10, max_groups: 10, spaces: 25, storage_mb: 20000, max_storage_mb: 20000, events: 200, ai_credits: 2000, max_ai_requests_month: 2000, max_courses: 100, max_messages_month: 100000 },
    features: { dashboard: true, members: true, families: true, churches: true, events: true, notifications: true, documents: true, reports: true, analytics: true, discipleship: true, academy: true, ai: true, finance: true, mobile_money: false, marketplace: true, community: true, forms: true, calendar: true },
    modules: ['dashboard', 'members', 'families', 'churches', 'events', 'notifications', 'documents', 'reports', 'analytics', 'discipleship', 'academy', 'ai', 'finance', 'marketplace', 'community', 'forms', 'calendar'],
    prices: [
      { currency: 'EUR', cycle: 'MONTHLY', amountMinor: 2900, channels: ['CARD'], stripePriceStatus: 'NOT_REQUIRED' },
      { currency: 'EUR', cycle: 'YEARLY', amountMinor: 29000, channels: ['CARD'], stripePriceStatus: 'NOT_REQUIRED' },
    ],
  },
  {
    key: 'NETWORK',
    name: 'Reseau & Campus',
    description: "Pour les reseaux d'eglises et campus. Multi-tenant avance, white-label, support dedie.",
    sortOrder: 4,
    trialDays: 30,
    annualDiscountPct: 17,
    seatsLimit: 2000,
    storageLimitMb: 100000,
    aiCreditsLimit: 5000,
    limits: { members: 2000, max_users: 2000, max_churches: 100, max_departments: 100, max_campuses: 50, max_groups: 50, spaces: 100, storage_mb: 100000, max_storage_mb: 100000, events: 1000, ai_credits: 5000, max_ai_requests_month: 5000, max_courses: 1000, max_messages_month: 1000000 },
    features: { dashboard: true, members: true, families: true, churches: true, events: true, notifications: true, documents: true, reports: true, analytics: true, discipleship: true, academy: true, ai: true, finance: true, mobile_money: true, marketplace: true, community: true, white_label: true, api_access: true, sso: true, forms: true, calendar: true },
    modules: ['dashboard', 'members', 'families', 'churches', 'events', 'notifications', 'documents', 'reports', 'analytics', 'discipleship', 'academy', 'ai', 'finance', 'mobile_money', 'marketplace', 'community', 'white_label', 'api_access', 'sso', 'forms', 'calendar'],
    prices: [
      { currency: 'USD', cycle: 'MONTHLY', amountMinor: 9900, channels: ['CARD'], stripePriceStatus: 'NOT_REQUIRED' },
      { currency: 'USD', cycle: 'YEARLY', amountMinor: 99000, channels: ['CARD'], stripePriceStatus: 'NOT_REQUIRED' },
    ],
  },
];

// Le plan gratuit : prix legacy à 0 dans les trois devises (V177), donc carte
// « 0 FCFA ». Inclus uniquement en mode mesure (PRICING_STUB_FREE_PLAN=1).
if (WITH_FREE_PLAN) {
  PLANS.unshift({
    key: 'DISCOVERY',
    name: 'Decouverte',
    description: 'Gratuit pour decouvrir Discipolat. Parfait pour une petite eglise qui debute.',
    sortOrder: 1,
    trialDays: 30,
    annualDiscountPct: 17,
    seatsLimit: 50,
    storageLimitMb: 500,
    aiCreditsLimit: 100,
    limits: { members: 50, max_users: 50, max_churches: 1, max_departments: 3, max_campuses: 1, max_groups: 1, spaces: 3, storage_mb: 500, max_storage_mb: 500, events: 10, ai_credits: 100, max_ai_requests_month: 100, max_courses: 5, max_messages_month: 1000 },
    features: { dashboard: true, members: true, families: true, churches: true, events: true, notifications: true, documents: true, reports: true, analytics: false, discipleship: false, academy: false, ai: false, finance: false, mobile_money: false, marketplace: false, community: false, forms: true, calendar: true },
    modules: ['dashboard', 'members', 'families', 'churches', 'events', 'notifications', 'documents', 'reports', 'forms', 'calendar'],
    // Prix legacy à 0 dans les trois devises (V177) ; `price_yearly` = 0 dans la
    // devise du plan, EUR (V144) — d'où une seule ligne annuelle.
    prices: [
      { currency: 'EUR', cycle: 'MONTHLY', amountMinor: 0, channels: ['CONTACT'], stripePriceStatus: 'NOT_REQUIRED' },
      { currency: 'XAF', cycle: 'MONTHLY', amountMinor: 0, channels: ['CONTACT'], stripePriceStatus: 'NOT_REQUIRED' },
      { currency: 'USD', cycle: 'MONTHLY', amountMinor: 0, channels: ['CONTACT'], stripePriceStatus: 'NOT_REQUIRED' },
      { currency: 'EUR', cycle: 'YEARLY', amountMinor: 0, channels: ['CONTACT'], stripePriceStatus: 'NOT_REQUIRED' },
    ],
  });
}

// ── Résolution (à la main du contrôleur, en plus simple) ─────────────────────

/** Devise demandée, devise réellement servie : le serveur ne suit que ce qu'il connaît. */
function resolveCurrency(requested) {
  const wanted = (requested ?? '').toUpperCase();
  return ENABLED_CURRENCIES.includes(wanted) ? wanted : SETTINGS.defaultDisplayCurrency;
}

function resolveCycle(requested) {
  const wanted = (requested ?? '').toUpperCase();
  if (wanted === 'YEARLY' && !SETTINGS.allowYearly) return SETTINGS.defaultCycle;
  if (wanted === 'MONTHLY' && !SETTINGS.allowMonthly) return SETTINGS.defaultCycle;
  return wanted === 'YEARLY' ? 'YEARLY' : 'MONTHLY';
}

/** Canaux déclarés du prix, restreints aux canaux autorisés par les réglages. */
function allowedChannels(price) {
  const enabled = {
    CARD: SETTINGS.allowCardChannel,
    MOMO: SETTINGS.allowMomoChannel,
    STRONG: SETTINGS.allowStrongChannel,
    BANK: SETTINGS.allowBankChannel,
    CONTACT: SETTINGS.allowContactChannel,
  };
  return price.channels.filter((channel) => enabled[channel] === true);
}

function priceView(row, cycle) {
  const decimals = DECIMALS[row.currency] ?? 2;
  return {
    currency: row.currency,
    cycle,
    amountMinor: row.amountMinor,
    // `amount` est la valeur majeure en chaîne fournie par le serveur.
    amount: String(row.amountMinor / 10 ** decimals),
    decimals,
    region: 'GLOBAL',
    isRegional: false,
    fromFallback: false,
    selectable: true,
    checkoutCurrency: null,
    channels: allowedChannels(row),
    stripePriceStatus: row.stripePriceStatus,
  };
}

function planView(plan, currency, cycle) {
  const rows = plan.prices.filter((row) => row.currency === currency && row.cycle === cycle);
  const priced = rows[0];
  const prices = plan.prices
    .filter((row) => row.currency === currency)
    .map((row) => priceView(row, row.cycle));
  const display = priced
    ? {
        available: true,
        amountMinor: priced.amountMinor,
        amount: String(priced.amountMinor / 10 ** (DECIMALS[currency] ?? 2)),
        currency,
        decimals: DECIMALS[currency] ?? 2,
        cycle,
        region: 'GLOBAL',
        isRegional: false,
        fromFallback: false,
        selectable: true,
        checkoutCurrency: null,
        chargedInDifferentCurrency: false,
        channels: allowedChannels(priced),
        reason: null,
      }
    : {
        available: false,
        amountMinor: null,
        amount: null,
        currency,
        decimals: DECIMALS[currency] ?? 2,
        cycle,
        region: 'GLOBAL',
        isRegional: false,
        fromFallback: false,
        selectable: false,
        checkoutCurrency: null,
        chargedInDifferentCurrency: false,
        channels: [],
        reason: 'NO_PRICE_IN_CURRENCY',
      };
  return {
    key: plan.key,
    name: plan.name,
    description: plan.description,
    sortOrder: plan.sortOrder,
    isPublic: true,
    status: 'ACTIVE',
    // `trialDays` est nulisé côté serveur dès que l'essai est désactivé.
    trialDays: SETTINGS.trialEnabled ? plan.trialDays : 0,
    annualDiscountPct: plan.annualDiscountPct,
    seatsLimit: plan.seatsLimit,
    storageLimitMb: plan.storageLimitMb,
    aiCreditsLimit: plan.aiCreditsLimit,
    limits: plan.limits,
    features: plan.features,
    modules: plan.modules,
    prices,
    display,
  };
}

function configBody(search) {
  const resolved = resolveCurrency(search.get('currency'));
  return {
    catalogVersion: SETTINGS.catalogVersion,
    displayMode: SETTINGS.displayMode,
    baseCurrency: SETTINGS.baseCurrency,
    defaultCurrency: SETTINGS.defaultDisplayCurrency,
    resolvedCurrency: resolved,
    resolvedRegion: SETTINGS.defaultRegion,
    regionPricingEnabled: SETTINGS.regionPricingEnabled,
    allowCurrencyChoice: SETTINGS.allowUserCurrencyChoice,
    allowCycleChoice: SETTINGS.allowCycleChoice,
    allowRegionChoice: SETTINGS.allowRegionChoice,
    allowMonthly: SETTINGS.allowMonthly,
    allowYearly: SETTINGS.allowYearly,
    defaultCycle: SETTINGS.defaultCycle,
    trialEnabled: SETTINGS.trialEnabled,
    stripeEnabled: SETTINGS.stripeEnabled,
    enabledCurrencies: ENABLED_CURRENCIES.map((code) => ({
      code,
      symbol: symbolOf(code),
      name: nameOf(code),
      decimals: DECIMALS[code] ?? 2,
      selected: code === resolved,
    })),
    // `regions()` est vidé quand la tarification régionale est fermée (V246).
    regions: [],
    channels: {
      CARD: SETTINGS.allowCardChannel,
      MOMO: SETTINGS.allowMomoChannel,
      STRONG: SETTINGS.allowStrongChannel,
      BANK: SETTINGS.allowBankChannel,
      CONTACT: SETTINGS.allowContactChannel,
    },
    landing: SETTINGS.landing,
    allowMomoChannel: SETTINGS.allowMomoChannel,
    allowStrongChannel: SETTINGS.allowStrongChannel,
    allowBankChannel: SETTINGS.allowBankChannel,
    allowContactChannel: SETTINGS.allowContactChannel,
    strongCurrency: SETTINGS.strongCurrency,
  };
}

function plansBody(search) {
  const currency = resolveCurrency(search.get('currency'));
  const cycle = resolveCycle(search.get('cycle'));
  const sorted = [...PLANS].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  return {
    catalogVersion: SETTINGS.catalogVersion,
    currency,
    cycle,
    region: SETTINGS.defaultRegion,
    plans: sorted.map((plan) => planView(plan, currency, cycle)),
  };
}

// ── HTTP ─────────────────────────────────────────────────────────────────────

/**
 * Le SPA est servi sur 4173 et parle à 4180 : c'est croisé, et l'instance axios
 * du frontend pose `withCredentials: true`. Un `Access-Control-Allow-Origin: *`
 * serait donc refusé par le navigateur — l'origine est renvoyée telle quelle,
 * avec les identifiants autorisés.
 */
function corsHeaders(origin) {
  const headers = {
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Origin': origin ?? '*',
    Vary: 'Origin',
  };
  return headers;
}

const server = createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://${HOST}:${PORT}`);
  const path = url.pathname;
  const log = (status) => {
    if (process.env.PRICING_STUB_VERBOSE === '0' && status !== 404) return;
    // Horodaté : `networkidle` qui ne se stabilise jamais ne se diagnostique
    // qu'à la fréquence des requêtes, pas à leur contenu.
    console.log(`${new Date().toISOString()} ${req.method} ${path}${url.search} -> ${status}`);
  };
  const send = (status, body, extra = {}) => {
    log(status);
    const headers = {
      ...corsHeaders(req.headers.origin),
      'Content-Type': 'application/json; charset=utf-8',
      ...extra,
    };
    if (status === 204) {
      res.writeHead(status, headers);
      res.end();
      return;
    }
    const payload = JSON.stringify(body);
    res.writeHead(status, { ...headers, 'Content-Length': Buffer.byteLength(payload) });
    res.end(payload);
  };

  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      ...corsHeaders(req.headers.origin),
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': req.headers['access-control-request-headers'] ?? 'Content-Type, Authorization',
      'Access-Control-Max-Age': '600',
    });
    res.end();
    return;
  }

  const cache = { 'Cache-Control': 'public, max-age=60' };

  if (req.method === 'GET' && path === '/api/v1/public/pricing/config') return send(200, configBody(url.searchParams), cache);
  if (req.method === 'GET' && path === '/api/v1/public/pricing/plans') return send(200, plansBody(url.searchParams), cache);
  if (req.method === 'GET' && path.startsWith('/api/v1/public/pricing/plans/')) {
    const key = decodeURIComponent(path.split('/').pop() ?? '').toUpperCase();
    const plan = PLANS.find((item) => item.key === key);
    if (!plan) return send(404, { error: 'PLAN_NOT_FOUND' });
    const currency = resolveCurrency(url.searchParams.get('currency'));
    const cycle = resolveCycle(url.searchParams.get('cycle'));
    return send(200, {
      catalogVersion: SETTINGS.catalogVersion,
      currency,
      cycle,
      region: SETTINGS.defaultRegion,
      plan: planView(plan, currency, cycle),
    }, cache);
  }
  if (req.method === 'GET' && path === '/api/v1/public/pricing/currency') {
    const requested = (url.searchParams.get('currency') ?? '').toUpperCase();
    const resolved = resolveCurrency(requested);
    return send(200, {
      currency: resolved,
      requested,
      honored: resolved === requested,
      region: SETTINGS.defaultRegion,
      decimals: DECIMALS[resolved] ?? 2,
    }, { 'Cache-Control': 'no-store' });
  }

  // Tout le reste : 404 net et immédiat. C'est le point important — un port
  // fermé produit un ECONNREFUSED que l'app gère, mais une requête qui pend
  // fait échouer `waitForLoadState('networkidle')`. Le 404 rend l'absence lisible.
  return send(404, { error: 'NOT_PROVISIONED_BY_STUB', path });
});

server.listen(PORT, HOST, () => {
  console.log(`stub tarifs E2E : http://${HOST}:${PORT}/api/v1/public/pricing (plans : ${PLANS.map((p) => p.key).join(', ')})`);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}

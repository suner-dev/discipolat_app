import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PricingPage from '@/pages/PricingPage';

/**
 * Page publique des tarifs — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T7.1.
 *
 * <p>Ces tests ne vérifient pas « la page s'affiche » mais trois décisions
 * produit :</p>
 * <ul>
 *   <li>un prix absent se dit (<em>tarif non communiqué</em>) et ne devient
 *       jamais un 0 qui se lirait comme un plan gratuit ;</li>
 *   <li>un sélecteur n'apparaît que si le super-admin l'autorise — son absence
 *       est un choix, pas un oubli ;</li>
 *   <li>les montants affichés viennent du serveur, en unités mineures, sans
 *       reconversion côté client.</li>
 * </ul>
 */

const { apiGet, getErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  getErrorMessage: vi.fn().mockReturnValue('Service indisponible'),
}));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet, post: vi.fn() },
  getErrorMessage,
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false }),
}));

const currencyOption = (code: string, decimals: number, symbol: string) => ({
  code,
  symbol,
  name: code,
  decimals,
  selected: code === 'XAF',
});

const defaultConfig = {
  catalogVersion: 3,
  displayMode: 'SWITCHER',
  baseCurrency: 'XAF',
  defaultCurrency: 'XAF',
  resolvedCurrency: 'XAF',
  resolvedRegion: 'AF',
  regionPricingEnabled: false,
  allowCurrencyChoice: true,
  allowCycleChoice: true,
  allowRegionChoice: false,
  allowMonthly: true,
  allowYearly: true,
  defaultCycle: 'MONTHLY',
  trialEnabled: true,
  stripeEnabled: false,
  enabledCurrencies: [currencyOption('XAF', 0, 'FCFA'), currencyOption('EUR', 2, '€')],
  regions: [],
  channels: { CARD: true, MOMO: false, STRONG: true, BANK: false, CONTACT: true },
  landing: { enabled: true, source: 'API', maxPlans: 4, showPrices: true, showToggle: true, ctaLabel: null },
};

const planView = (overrides: Record<string, unknown> = {}) => ({
  key: 'STARTUP',
  name: 'Démarrage',
  description: 'Pour les églises en croissance',
  sortOrder: 2,
  isPublic: true,
  status: 'ACTIVE',
  trialDays: 30,
  annualDiscountPct: 17,
  seatsLimit: 200,
  storageLimitMb: 5000,
  aiCreditsLimit: 500,
  limits: {},
  features: {},
  modules: ['members', 'finance'],
  prices: [
    {
      currency: 'XAF',
      cycle: 'MONTHLY',
      amountMinor: 15000,
      amount: '15000',
      decimals: 0,
      region: 'GLOBAL',
      isRegional: false,
      fromFallback: false,
      selectable: true,
      checkoutCurrency: null,
      channels: ['MOMO', 'STRONG'],
      stripePriceStatus: 'PENDING',
    },
  ],
  display: {
    available: true,
    amountMinor: 15000,
    amount: '15000',
    currency: 'XAF',
    decimals: 0,
    cycle: 'MONTHLY',
    region: 'GLOBAL',
    isRegional: false,
    fromFallback: false,
    selectable: true,
    checkoutCurrency: null,
    chargedInDifferentCurrency: false,
    channels: ['MOMO', 'STRONG'],
    reason: null,
  },
  ...overrides,
});

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PricingPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** Route les appels API par URL, comme le ferait le serveur réel. */
function routeApi(overrides: {
  config?: Record<string, unknown>;
  plans?: Record<string, unknown> | { reject: true };
  configReject?: boolean;
} = {}) {
  apiGet.mockImplementation((url: string) => {
    if (url.includes('/public/pricing/config')) {
      if (overrides.configReject) return Promise.reject(new Error('network'));
      return Promise.resolve({ data: overrides.config ?? defaultConfig });
    }
    if (url.includes('/public/pricing/plans')) {
      if (overrides.plans && 'reject' in overrides.plans) return Promise.reject(new Error('network'));
      return Promise.resolve({ data: overrides.plans ?? { currency: 'XAF', cycle: 'MONTHLY', region: 'AF', catalogVersion: 3, plans: [planView()] } });
    }
    return Promise.resolve({ data: {} });
  });
}

describe('PricingPage — catalogue public', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('discipolat-locale', 'fr');
    routeApi();
  });

  it('affiche le montant du serveur sans le reconvertir', async () => {
    renderPage();

    const heading = await screen.findByRole('heading', { name: 'Démarrage' });
    const card = heading.closest('article') as HTMLElement;
    // 15000 unités mineures XAF = « 15 000 FCFA ». Si la page affichait 150, le
    // contrat d'unités serait rompu côté client.
    expect(within(card).getByText(/15\s?000/)).toBeInTheDocument();
    expect(within(card).getByText(/FCFA/)).toBeInTheDocument();
    expect(within(card).getByText(/par mois/)).toBeInTheDocument();
  });

  it('propose le sélecteur de devise quand le super-admin l’autorise', async () => {
    renderPage();

    const select = await screen.findByLabelText(/devise/i);
    expect(select).toBeInTheDocument();
    expect(within(select as HTMLSelectElement).getAllByRole('option')).toHaveLength(2);
  });

  it('ne propose AUCUN sélecteur quand le super-admin refuse le choix', async () => {
    routeApi({
      config: { ...defaultConfig, allowCurrencyChoice: false, allowCycleChoice: false },
    });
    renderPage();

    await screen.findByRole('heading', { name: 'Démarrage' });
    expect(screen.queryByLabelText(/devise/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mensuel' })).not.toBeInTheDocument();
  });

  it('masque la bascule annuel quand le cycle annuel est interdit', async () => {
    routeApi({ config: { ...defaultConfig, allowYearly: false } });
    renderPage();

    await screen.findByRole('heading', { name: 'Démarrage' });
    expect(screen.getByRole('button', { name: 'Mensuel' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Annuel' })).not.toBeInTheDocument();
  });

  it('bascule en cycle annuel et redemande le catalogue', async () => {
    renderPage();

    await screen.findByRole('heading', { name: 'Démarrage' });
    await userEvent.click(screen.getByRole('button', { name: 'Annuel' }));

    await waitFor(() => {
      // Le cycle part dans les paramètres, pas dans le chemin : on inspecte
      // les deux arguments du mock plutôt que l'URL seule.
      const cycles = apiGet.mock.calls
        .filter((call) => String(call[0]).includes('/public/pricing/plans'))
        .map((call) => (call[1] as { params?: Record<string, string> } | undefined)?.params?.cycle);
      expect(cycles).toContain('YEARLY');
    });
  });

  it('dit « tarif non communiqué » plutôt qu’afficher 0 quand aucun prix ne s’applique', async () => {
    routeApi({
      plans: {
        currency: 'EUR',
        cycle: 'MONTHLY',
        region: 'AF',
        catalogVersion: 3,
        plans: [
          planView({
            key: 'NETWORK',
            name: 'Réseau & Campus',
            prices: [],
            display: {
              available: false,
              amountMinor: null,
              amount: null,
              currency: 'EUR',
              decimals: 2,
              cycle: 'MONTHLY',
              channels: [],
              selectable: false,
              reason: 'NO_PRICE_IN_CURRENCY',
            },
          }),
        ],
      },
    });
    renderPage();

    const card = (await screen.findByRole('heading', { name: 'Réseau & Campus' })).closest('article') as HTMLElement;
    expect(within(card).getByText(/tarif non communiqué/i)).toBeInTheDocument();
    // Le point critique : aucun zero ne doit se lire comme un plan gratuit.
    expect(within(card).queryByText(/^0([\s\u00a0]|\.00|,00)*€?$/)).not.toBeInTheDocument();
  });

  it('propose un canal alternatif quand le débit se fait dans une autre devise', async () => {
    routeApi({
      plans: {
        currency: 'XAF',
        cycle: 'MONTHLY',
        region: 'AF',
        catalogVersion: 3,
        plans: [
          planView({
            display: {
              available: true,
              amountMinor: 15000,
              amount: '15000',
              currency: 'XAF',
              decimals: 0,
              cycle: 'MONTHLY',
              checkoutCurrency: 'EUR',
              chargedInDifferentCurrency: true,
              selectable: true,
              channels: ['STRONG'],
            },
          }),
        ],
      },
    });
    renderPage();

    await screen.findByRole('heading', { name: 'Démarrage' });
    // Le débit en devise forte doit être annoncé : sinon la surprise arrive à
    // l'écran de carte, après le clic.
    expect(screen.getAllByText(/débité en/i).length).toBeGreaterThan(0);
  });

  it('affiche honnêtement l’indisponibilité du catalogue', async () => {
    routeApi({ configReject: true, plans: { reject: true } });
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    // Aucune carte ne doit sortir un montant d'un catalogue indisponible.
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('gère l’annulation du paiement sans casser la page', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 } } });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/pricing?checkout=cancelled']}>
          <PricingPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('status')).toHaveTextContent(/annulé/i);
  });
});
// B6 — Tests de la page « Abonnement & quotas » (constat F4).
//
// Verrouillent la consommation du contrat réel de `SubscriptionController`
// (/api/v1/admin/subscription) ET les 5 états (§5.0.2) + les issues métier :
//   GET  /current  -> { hasSubscription, subscription, quotas }
//   GET  /plans    -> [ { key, name, priceMonthly, priceYearly, currency } ]
//   POST /change-plan { planKey }   (rétrogradation refusée -> erreur affichée)
//   POST /cancel { atPeriodEnd }
//   POST /reactivate
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TenantAdminSubscriptionPage from '@/pages/TenantAdminSubscriptionPage';

// `vi.mock` est hoisté : les faux sont créés DANS la factory.
vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  return {
    default: { get, post },
    api: { get, post },
    getErrorMessage: () => 'Erreur test',
    __mocks: { get, post },
  };
});
type M = ReturnType<typeof vi.fn>;
const mocked = (await import('@/lib/api')) as unknown as { __mocks: { get: M; post: M } };
const { get, post } = mocked.__mocks;

// Toast espionné : les issues (succès/erreur) passent par lui, on vérifie les
// appels sans dépendre du rendu d'un <Toaster/>.
const toastSuccess = vi.fn();
const toastError = vi.fn();
vi.mock('react-hot-toast', () => ({
  default: { success: (...a: unknown[]) => toastSuccess(...a), error: (...a: unknown[]) => toastError(...a) },
}));

// Le panneau de quotas a son propre composant (réutilisé, non recréé). On le
// stub pour vérifier qu'il EST monté avec les metrics normalisées, sans tester
// son rendu interne (c'est le rôle de ses propres tests).
vi.mock('@/components/admin/QuotaUsageCards', () => ({
  QuotaUsageCards: ({ title, metrics }: { title?: string; metrics: unknown[] }) => (
    <div data-testid="quota-cards" data-count={metrics.length}>{title}</div>
  ),
}));

const END = new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString();
const START = new Date().toISOString();

const CURRENT_ACTIVE = {
  hasSubscription: true,
  subscription: {
    id: '33333333-3333-3333-3333-333333333333',
    planKey: 'GROWTH',
    status: 'ACTIVE',
    billingCycle: 'monthly',
    currentPeriodStart: START,
    currentPeriodEnd: END,
    cancelAtPeriodEnd: false,
    canceledAt: null,
    trialEndsAt: null,
    plan: { name: 'Growth', priceMonthly: 50000, priceYearly: 500000 },
  },
  quotas: { users: { used: 5, limit: 50 } },
};

const PLANS = [
  { key: 'DISCOVERY', name: 'Discovery', priceMonthly: 0, priceYearly: 0, currency: 'EUR', isActive: true },
  { key: 'GROWTH', name: 'Growth', priceMonthly: 50000, priceYearly: 500000, currency: 'XAF', isActive: true },
  { key: 'NETWORK', name: 'Network', priceMonthly: 120000, priceYearly: 1200000, currency: 'XAF', isActive: true },
];

function mockCurrent(payload: unknown) {
  get.mockImplementation((path: string) => {
    if (path === '/admin/subscription/plans') return Promise.resolve({ data: PLANS });
    if (path === '/admin/subscription/current') return Promise.resolve({ data: payload });
    return Promise.resolve({ data: {} });
  });
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } });
  return render(
    <QueryClientProvider client={qc}>
      <TenantAdminSubscriptionPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockCurrent(CURRENT_ACTIVE);
});

describe('B6 — les 5 états', () => {
  it('affiche un squelette accessible pendant le chargement', () => {
    get.mockImplementation(() => new Promise(() => undefined)); // jamais résolu
    renderPage();
    expect(screen.getByRole('status')).toBeTruthy();
  });

  it('affiche l\'abonnement courant (état succès)', async () => {
    renderPage();
    // Le nom du plan courant est présent deux fois (carte + pastille « Actuel »
    // dans la grille) : on accepte le pluriel plutôt que de supposer l'unicité.
    expect((await screen.findAllByText('Growth')).length).toBeGreaterThan(0);
    expect(screen.getByText('Actif')).toBeTruthy();
    // « Cycle : Mensuel » est un seul <p> à enfants text multiples : le libellé
    // est cherché par motif, pas en égalité stricte (le texte est composé).
    expect(screen.getByText(/Mensuel/)).toBeTruthy();
  });

  it('monte le panneau de quotas réutilisé avec les metrics normalisées', async () => {
    renderPage();
    const cards = await screen.findByTestId('quota-cards');
    // normalizeQuotaUsage transforme { users: { used, limit } } en une metric.
    expect(cards.getAttribute('data-count')).toBe('1');
  });

  it('affiche un état d\'erreur actionnable avec retry', async () => {
    get.mockRejectedValue(new Error('500'));
    renderPage();
    expect(await screen.findByText('Impossible de charger l’abonnement')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeTruthy();
  });

  it('affiche un état vide quand aucun abonnement n\'est actif', async () => {
    mockCurrent({ hasSubscription: false });
    renderPage();
    expect(await screen.findByText('Aucun abonnement actif')).toBeTruthy();
  });
});

describe('B6 — changement de plan', () => {
  it('propose les plans publiés et envoie le contrat exact', async () => {
    // Le serveur accepte le changement : la mutation doit résoudre pour que le
    // succès (toast + revalidation) soit observable.
    post.mockResolvedValue({ data: { subscriptionId: 'x', planKey: 'DISCOVERY', status: 'ACTIVE', message: 'ok' } });
    renderPage();
    await screen.findAllByText('Growth');
    // « Actuel » marque le plan déjà actif ; les autres offrent un bouton.
    expect(screen.getByText('Actuel')).toBeTruthy();
    // Deux plans non courants (Discovery, Network) → deux boutons ; on prend le
    // premier (l'ordre suit le catalogue renvoyé par le serveur).
    const choose = screen.getAllByRole('button', { name: 'Choisir ce plan' })[0];
    await userEvent.click(choose);
    // Confirmation demandée avant d\'appeler le serveur.
    const confirm = await screen.findByRole('button', { name: 'Confirmer le changement' });
    await userEvent.click(confirm);
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/subscription/change-plan', { planKey: 'DISCOVERY' }));
    expect(toastSuccess).toHaveBeenCalled();
  });

  it('affiche le motif du serveur quand la rétrogradation est refusée', async () => {
    post.mockRejectedValue(Object.assign(new Error('400'), { isAxiosError: true }));
    renderPage();
    await screen.findAllByText('Growth');
    await userEvent.click(screen.getAllByRole('button', { name: 'Choisir ce plan' })[0]);
    await userEvent.click(await screen.findByRole('button', { name: 'Confirmer le changement' }));
    await waitFor(() => expect(toastError).toHaveBeenCalled());
    // L\'appel a bien eu lieu : le refus est une donnée serveur, pas un bloc front.
    expect(post).toHaveBeenCalledWith('/admin/subscription/change-plan', { planKey: 'DISCOVERY' });
  });
});

describe('B6 — résiliation et réactivation', () => {
  it('résilie en fin de période (atPeriodEnd: true)', async () => {
    post.mockResolvedValue({ data: { message: 'ok' } });
    renderPage();
    await screen.findAllByText('Growth');
    await userEvent.click(screen.getByRole('button', { name: 'Résilier en fin de période' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Confirmer la résiliation' }));
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/subscription/cancel', { atPeriodEnd: true }));
  });

  it('propose la réactivation et l\'enregistre quand le plan est annulé', async () => {
    mockCurrent({
      ...CURRENT_ACTIVE,
      subscription: { ...CURRENT_ACTIVE.subscription, status: 'CANCELLED' },
    });
    post.mockResolvedValue({ data: { message: 'ok' } });
    renderPage();
    await screen.findAllByText('Growth');
    await userEvent.click(screen.getByRole('button', { name: 'Réactiver l’abonnement' }));
    await waitFor(() => expect(post).toHaveBeenCalledWith('/admin/subscription/reactivate'));
    expect(toastSuccess).toHaveBeenCalled();
  });

  it('signale une annulation déjà programmée par un bandeau', async () => {
    mockCurrent({
      ...CURRENT_ACTIVE,
      subscription: { ...CURRENT_ACTIVE.subscription, cancelAtPeriodEnd: true },
    });
    renderPage();
    expect(await screen.findByText(/Cet abonnement prendra fin/)).toBeTruthy();
    // En cas d\'annulation programmée, la sortie logique est la réactivation.
    expect(screen.getByRole('button', { name: 'Réactiver l’abonnement' })).toBeTruthy();
  });
});

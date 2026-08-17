import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import toast from 'react-hot-toast';
import FinancePage from '@/pages/FinancePage';
import { PlatformContext } from '@/contexts/PlatformContext';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

// Recharts : conteneurs simples pour le rendu jsdom.
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

const { apiGet, apiPost, apiPut, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage(disabledModules: string[] = []) {
  const moduleEnabled = (key: string) => !disabledModules.includes(key);
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformContext.Provider
        value={{
          menus: [], modules: [], isLoaded: true, moduleEnabled,
          canAccessPath: () => true, refetch: () => undefined,
        }}
      >
        <MemoryRouter initialEntries={['/finances']}>
          <FinancePage />
        </MemoryRouter>
      </PlatformContext.Provider>
    </QueryClientProvider>
  );
}

const TX_RECETTE = {
  id: 'tx-1', type: 'RECETTE', categorie: 'DIME', montant: 1500, description: 'Dîmes du dimanche',
  dateTransaction: '2026-08-17',
};
const TX_DEPENSE = {
  id: 'tx-2', type: 'DEPENSE', categorie: 'LOYER', montant: 600, description: 'Loyer du mois',
  dateTransaction: '2026-08-05',
};

describe('FinancePage — outil métier Finances', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url.startsWith('/finances/stats')) {
        return { data: {
          annee: 2026, totalRecettes: 1500, totalDepenses: 600, solde: 900, nbTransactions: 2,
          parMois: [{ mois: 'août', recettes: 1500, depenses: 600 }],
          recettesParCategorie: [{ categorie: 'DIME', total: 1500 }],
          depensesParCategorie: [{ categorie: 'LOYER', total: 600 }],
        } };
      }
      if (url.startsWith('/finances/budgets')) return { data: [] };
      if (url.startsWith('/finances/transactions')) return { data: [TX_RECETTE, TX_DEPENSE] };
      return { data: [] };
    });
    apiPost.mockResolvedValue({ data: {} });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche les KPIs, la liste des transactions et le graphique', async () => {
    renderPage();

    // Attendre que les requêtes (stats + transactions) soient résolues.
    await waitFor(() => {
      expect(screen.getAllByText('DIME').length).toBeGreaterThan(0);
    });
    // KPIs calculés sur les données réelles (Intl fr-FR → espaces fines U+202F).
    const kpi = (n: string) => screen.getAllByText((content) => content.includes(n) && content.includes('FCFA'));
    expect(kpi('500').length).toBeGreaterThan(0);
    expect(kpi('600').length).toBeGreaterThan(0);
    expect(kpi('900').length).toBeGreaterThan(0);
    // Transactions listées (LOYER apparaît aussi dans le filtre de catégories).
    expect(screen.getByText('Dîmes du dimanche')).toBeInTheDocument();
    expect(screen.getAllByText('LOYER').length).toBeGreaterThan(0);
    // Graphique présent.
    expect(screen.getByText(/Recettes \/ dépenses par mois/)).toBeInTheDocument();
  });

  it('crée une transaction via la modale (POST /finances/transactions)', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Finances')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouvelle transaction/i }));
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Nouvelle transaction' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('Catégorie'), { target: { value: 'Materiel' } });
    fireEvent.change(screen.getByLabelText('Montant'), { target: { value: '250.50' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Micro-casque' } });
    fireEvent.click(screen.getByRole('button', { name: /ajouter/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/finances/transactions', expect.objectContaining({
        type: 'RECETTE', categorie: 'MATERIEL', montant: 250.5, description: 'Micro-casque',
      }));
    });
    expect(toast.success).toHaveBeenCalledWith('Transaction enregistrée');
  });

  it('affiche l’état explicite quand le module est désactivé', async () => {
    renderPage(['FINANCES']);

    await waitFor(() => {
      expect(screen.getByText('Module Finances désactivé')).toBeInTheDocument();
    });
    // Aucun appel API finances (le garde-fou serveur renverrait 403 de toute façon).
    expect(apiGet).not.toHaveBeenCalled();
  });
});

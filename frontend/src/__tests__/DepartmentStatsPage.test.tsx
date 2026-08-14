import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentStatsPage from '@/pages/DepartmentStatsPage';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
  };
});

const { mockStats } = vi.hoisted(() => ({
  mockStats: {
    periode: { code: 'ANNEE', debut: '2025-09-01', fin: '2026-08-14' },
    effectif: { total: 2, actifs: 1, nouveaux30j: 0 },
    presence: { total: 2, presents: 1, absents: 1, taux: 50 },
    taches: { parStatut: { EN_COURS: 1 } },
    evolutionEffectif: [], evolutionPresence: [], evolutionTaches: [],
    disciplineParCategorie: {}, chargeParMembre: [],
    equipes: {}, affectations: {}, postesActifs: 0, evenements: [],
  },
}));

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockResolvedValue({ data: mockStats }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return {
    default: mockApiInstance,
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/departments/dept-1/stats']}>
        <DepartmentStatsPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DepartmentStatsPage — sélecteur de période', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('charge les statistiques par défaut (12 mois) et affiche la période', async () => {
    renderPage();
    expect(await screen.findByText('Statistiques du département')).toBeInTheDocument();
    expect(screen.getByText('Du 2025-09-01 au 2026-08-14')).toBeInTheDocument();
  });

  it('change de période et affiche les champs personnalisés', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    await screen.findByText('Statistiques du département');

    const select = screen.getByRole('combobox') as HTMLSelectElement;
    fireEvent.change(select, { target: { value: 'TRIMESTRE' } });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/departments/dept-1/stats', expect.objectContaining({
        params: { periode: 'TRIMESTRE' },
      }));
    });
    // Le chargement de la nouvelle période passe par un écran de chargement puis affiche les stats
    await screen.findByText('Statistiques du département');
    expect(screen.getByText('Du 2025-09-01 au 2026-08-14')).toBeInTheDocument();

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'PERSONNALISEE' } });
    await screen.findByText('Statistiques du département');
    expect(screen.getByText('Du')).toBeInTheDocument();
    expect(screen.getByText('Au')).toBeInTheDocument();
  });
});

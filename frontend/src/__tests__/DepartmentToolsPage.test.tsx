import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentToolsPage from '@/pages/DepartmentToolsPage';
import { PlatformContext } from '@/contexts/PlatformContext';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
  };
});

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/detail')) return Promise.resolve({ data: { id: 'dept-1', nom: 'Audiovisuel' } });
      if (url.includes('/reports/list')) return Promise.resolve({ data: [] });
      if (url.includes('/checklists')) return Promise.resolve({ data: [] });
      if (url.includes('/equipment')) return Promise.resolve({ data: [] });
      if (url.includes('/documents/stats')) return Promise.resolve({ data: { total: 0 } });
      if (url.includes('/documents')) return Promise.resolve({ data: [] });
      if (url.includes('/settings')) return Promise.resolve({ data: {} });
      return Promise.resolve({ data: [] });
    }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return {
    default: mockApiInstance,
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage(disabledModules: string[] = []) {
  const moduleEnabled = (key: string) => !disabledModules.includes(key);
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformContext.Provider
        value={{
          menus: [],
          modules: [],
          isLoaded: true,
          moduleEnabled,
          canAccessPath: () => true,
          refetch: () => undefined,
        }}
      >
        <MemoryRouter initialEntries={['/departments/dept-1/tools']}>
          <DepartmentToolsPage />
        </MemoryRouter>
      </PlatformContext.Provider>
    </QueryClientProvider>
  );
}

describe('DepartmentToolsPage — page Outils du département', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('affiche la page Outils avec les 5 onglets (parité mobile)', async () => {
    renderPage();

    expect(await screen.findByText('Outils & rapports')).toBeInTheDocument();
    expect(screen.getByText('Rapports')).toBeInTheDocument();
    expect(screen.getByText('Checklists')).toBeInTheDocument();
    expect(screen.getByText('Inventaire')).toBeInTheDocument();
    expect(screen.getByText('Documentation')).toBeInTheDocument();
    expect(screen.getByText('Paramètres')).toBeInTheDocument();
    // Onglet par défaut : les synthèses sauvegardées
    expect(await screen.findByText(/Synthèses sauvegardées/)).toBeInTheDocument();
  });

  it('affiche le formulaire de checklist dans l’onglet Checklists', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('Checklists'));

    expect(await screen.findByText('Nouvelle checklist')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Préparation du culte/)).toBeInTheDocument();
  });

  it('affiche le paramétrage des alertes dans l’onglet Paramètres', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('Paramètres'));

    expect(await screen.findByText(/Seuils des alertes intelligentes/)).toBeInTheDocument();
  });

  it('masque les onglets des sous-modules désactivés', async () => {
    renderPage(['DEPT_REPORTS', 'DEPT_INVENTORY', 'DEPT_CHECKLISTS']);

    expect(screen.queryByText('Rapports')).not.toBeInTheDocument();
    expect(screen.queryByText('Checklists')).not.toBeInTheDocument();
    expect(screen.queryByText('Inventaire')).not.toBeInTheDocument();
    expect(await screen.findByText('Documentation')).toBeInTheDocument();
  });
});

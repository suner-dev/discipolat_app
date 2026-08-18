import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentManagementPage from '@/pages/DepartmentManagementPage';
import { PlatformContext } from '@/contexts/PlatformContext';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
  };
});

vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn().mockReturnValue({
      user: { id: 'u1', role: 'RESPONSABLE', roles: ['RESPONSABLE'], activeRole: 'RESPONSABLE' },
      activeRole: 'RESPONSABLE',
      hasRole: vi.fn().mockReturnValue(true),
    }),
  };
});

const mockDept = { id: 'dept-1', nom: 'Département A', description: 'Le département test' };
const mockMembers = { content: [{ id: 'm1', nomComplet: 'Jean Dupont' }] };
const mockManagement = {
  teams: [{ id: 't1', nom: 'Louange', type: 'EQUIPE_PERMANENTE', statut: 'ACTIVE', nbMembres: 3 }],
  positions: [{ id: 'p1', nom: 'Coordinateur', statut: 'ACTIF', nbMembres: 1 }],
  assignments: [],
  taskStats: { enRetard: 1 },
  activity: [],
  org: { equipesActives: 1, postesActifs: 1, membresAffectes: 1 },
};
const mockSearchResults = {
  total: 2,
  membres: [{ id: 'm1', nomComplet: 'Jean Dupont', statut: 'ACTIF' }],
  equipes: [{ id: 't1', nom: 'Louange', nbMembres: 3 }],
  postes: [],
  taches: [],
  evenements: [],
};
const mockMembersMgmt = [
  { id: 'm1', soulId: 's1', nom: 'Jean Dupont', statut: 'ACTIF', email: 'jean@test.com' },
];
const mockSettings = {
  departmentId: 'dept-1', absenceSeuil: 2, absencePeriode: 3, inactiviteMois: 3, tacheRetardAlerte: true,
  eventRappelJours: 1,
};
const mockDocumentsList = [
  { id: 'd1', titre: "Procédure d'accueil", type: 'PROCEDURE', statut: 'ACTIF', createdAt: '2026-08-01T10:00:00' },
];
const mockDocumentStats = { total: 1, PROCEDURE: 1, GUIDE: 0, DOCUMENT: 0, FORMULAIRE: 0, COMPTE_RENDU: 0, RESSOURCE: 0 };

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/search')) return Promise.resolve({ data: mockSearchResults });
      if (url.includes('/detail')) return Promise.resolve({ data: mockDept });
      if (url.includes('/members/management')) return Promise.resolve({ data: mockMembersMgmt });
      if (url.includes('/members')) return Promise.resolve({ data: mockMembers });
      if (url.includes('/management')) return Promise.resolve({ data: mockManagement });
      if (url.includes('/settings')) return Promise.resolve({ data: mockSettings });
      if (url.includes('/documents/stats')) return Promise.resolve({ data: mockDocumentStats });
      if (url.includes('/documents')) return Promise.resolve({ data: mockDocumentsList });
      return Promise.resolve({ data: {} });
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
        <MemoryRouter initialEntries={['/departments/dept-1/manage']}>
          <DepartmentManagementPage />
        </MemoryRouter>
      </PlatformContext.Provider>
    </QueryClientProvider>
  );
}

describe('DepartmentManagementPage — recherche globale & onglets', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('affiche la recherche globale dès 2 caractères avec les résultats par catégorie', async () => {
    renderPage();
    const input = await screen.findByPlaceholderText(/Recherche rapide/);
    fireEvent.change(input, { target: { value: 'joh' } });

    await waitFor(() => expect(screen.getByText(/2 résultats/)).toBeInTheDocument());
    expect(screen.getAllByText('Jean Dupont').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Louange').length).toBeGreaterThan(0);
  });

  it('affiche et enregistre les seuils d\'alertes depuis l\'onglet Paramètres', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    fireEvent.click(await screen.findByText('Paramètres'));

    expect(await screen.findByText(/Seuils des alertes intelligentes/)).toBeInTheDocument();
    const inactivite = screen.getByLabelText('Mois sans présence (0–24)');
    fireEvent.change(inactivite, { target: { value: '6' } });

    fireEvent.click(screen.getByText("Enregistrer les paramètres"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/departments/dept-1/settings', expect.anything()));
    const payload = (api.put as any).mock.calls[0][1];
    expect(payload.inactiviteMois).toBe(6);
    expect(payload.absenceSeuil).toBe(2);
  });

  it('configure le rappel automatique des événements depuis l\'onglet Paramètres', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    fireEvent.click(await screen.findByText('Paramètres'));

    expect(await screen.findByText(/Rappel automatique des événements/)).toBeInTheDocument();
    const rappel = screen.getByLabelText(/Jours avant l'événement/);
    expect(rappel).toHaveValue(1);
    fireEvent.change(rappel, { target: { value: '7' } });

    fireEvent.click(screen.getByText("Enregistrer les paramètres"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/departments/dept-1/settings', expect.anything()));
    const payload = (api.put as any).mock.calls[0][1];
    expect(payload.eventRappelJours).toBe(7);
  });

  it('masque les onglets des sous-modules désactivés par l\'administrateur', async () => {
    renderPage(['DEPT_INVENTORY', 'DEPT_CHECKLISTS']);
    await screen.findByText('Gestion de Département A');

    expect(screen.queryByText('Inventaire')).not.toBeInTheDocument();
    expect(screen.queryByText('Checklists')).not.toBeInTheDocument();
    expect(screen.getByText('Documentation')).toBeInTheDocument();
    expect(screen.getByText('Organisation')).toBeInTheDocument();
  });

  it('gère la documentation du département (ajout + liste par type)', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    fireEvent.click(await screen.findByText('Documentation'));

    expect(await screen.findByText("Procédure d'accueil")).toBeInTheDocument();
    fireEvent.click(screen.getByText('Ajouter un document'));
    fireEvent.change(screen.getByLabelText('Titre *'), { target: { value: 'Guide son' } });
    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'GUIDE' } });
    fireEvent.click(screen.getByText('Ajouter'));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/departments/dept-1/documents', expect.anything()));
    const payload = (api.post as any).mock.calls[0][1];
    expect(payload.titre).toBe('Guide son');
    expect(payload.type).toBe('GUIDE');
  });

  it('affiche les onglets de gestion pour un Responsable', async () => {
    renderPage();
    await screen.findByText('Gestion de Département A');
    // Tab buttons specifically (not stat-card labels)
    expect(screen.getAllByText('Membres').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Équipes').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Tâches').length).toBeGreaterThan(0);
  });
});

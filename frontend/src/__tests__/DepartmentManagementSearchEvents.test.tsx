import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentManagementPage from '@/pages/DepartmentManagementPage';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
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
const mockEvents = {
  content: [
    { id: 'e1', titre: 'Convention départementale', typeEvenement: 'REUNION', statut: 'PLANIFIE', dateDebut: '2026-09-01T09:00:00' },
  ],
};

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/search')) return Promise.resolve({ data: mockSearchResults });
      if (url.includes('/detail')) return Promise.resolve({ data: mockDept });
      if (url.includes('/members')) return Promise.resolve({ data: mockMembers });
      if (url.includes('/management')) return Promise.resolve({ data: mockManagement });
      if (url.includes('/events/department/')) return Promise.resolve({ data: mockEvents });
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

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/departments/dept-1/manage']}>
        <DepartmentManagementPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DepartmentManagementPage — recherche globale & événements', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('affiche la recherche globale dès 2 caractères avec les résultats par catégorie', async () => {
    renderPage();
    const input = await screen.findByPlaceholderText(/Recherche rapide/);
    fireEvent.change(input, { target: { value: 'joh' } });

    await waitFor(() => expect(screen.getByText(/2 résultats/)).toBeInTheDocument());
    expect(screen.getByText('Jean Dupont')).toBeInTheDocument();
    expect(screen.getAllByText('Louange').length).toBeGreaterThan(0);
  });

  it('affiche les événements du département dans l’onglet Événements', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('Événements'));

    expect(await screen.findByText('Nouvel événement du département')).toBeInTheDocument();
    expect(await screen.findByText('Convention départementale')).toBeInTheDocument();
    expect(screen.getByText(/À venir \(1\)/)).toBeInTheDocument();
  });

  it('crée un événement de département via le formulaire', async () => {
    const { default: api } = await import('@/lib/api');
    const { container } = renderPage();
    fireEvent.click(await screen.findByText('Événements'));

    const titre = await screen.findByPlaceholderText(/Convention départementale 2026/);
    fireEvent.change(titre, { target: { value: 'Sortie d’évangélisation' } });
    const date = container.querySelector('input[type="datetime-local"]') as HTMLInputElement;
    fireEvent.change(date, { target: { value: '2026-09-15T08:00' } });

    fireEvent.click(screen.getByText("Créer l'événement"));
    await waitFor(() => expect(api.post).toHaveBeenCalled());
    const payload = (api.post as any).mock.calls[0][1];
    expect(payload.departmentId).toBe('dept-1');
    expect(payload.titre).toBe('Sortie d’évangélisation');
  });
});

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
const mockSettings = {
  departmentId: 'dept-1', absenceSeuil: 2, absencePeriode: 3, inactiviteMois: 3, tacheRetardAlerte: true,
};
const mockDocuments = {
  content: undefined,
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
      if (url.includes('/members')) return Promise.resolve({ data: mockMembers });
      if (url.includes('/management')) return Promise.resolve({ data: mockManagement });
      if (url.includes('/events/department/')) return Promise.resolve({ data: mockEvents });
      if (url.includes('/settings')) return Promise.resolve({ data: mockSettings });
      if (url.includes('/documents/stats')) return Promise.resolve({ data: mockDocumentStats });
      if (url.includes('/documents')) return Promise.resolve({ data: mockDocumentsList });
      return Promise.resolve({ data: mockDocuments });
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

  it('affiche et enregistre les seuils d\'alertes depuis l\'onglet Paramètres', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    fireEvent.click(await screen.findByText('Paramètres'));

    expect(await screen.findByText(/Seuils des alertes intelligentes/)).toBeInTheDocument();
    // La valeur chargée depuis le serveur (mois d'inactivité = 3)
    const inactivite = screen.getByLabelText('Mois sans présence (0–24)');
    fireEvent.change(inactivite, { target: { value: '6' } });

    fireEvent.click(screen.getByText("Enregistrer les paramètres"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith('/departments/dept-1/settings', expect.anything()));
    const payload = (api.put as any).mock.calls[0][1];
    expect(payload.inactiviteMois).toBe(6);
    expect(payload.absenceSeuil).toBe(2);
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

  it('crée une équipe temporaire liée à un événement du département', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    fireEvent.click(await screen.findByText('Nouvelle équipe'));

    const typeSelect = screen.getByLabelText('Type');
    fireEvent.change(typeSelect, { target: { value: 'EQUIPE_TEMPORAIRE' } });

    // Le sélecteur d'événement apparaît avec les événements du département
    const eventSelect = await screen.findByLabelText(/Événement lié/);
    await screen.findByRole('option', { name: 'Convention départementale' });
    fireEvent.change(eventSelect, { target: { value: 'e1' } });

    const nom = screen.getByLabelText(/Nom de l'équipe/);
    fireEvent.change(nom, { target: { value: 'Équipe logistique' } });
    fireEvent.click(screen.getByText("Créer l'équipe"));

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/departments/dept-1/teams', expect.anything()));
    const payload = (api.post as any).mock.calls[0][1];
    expect(payload.eventId).toBe('e1');
    expect(payload.type).toBe('EQUIPE_TEMPORAIRE');
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import MemberRequestsPage from '@/pages/MemberRequestsPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPatch } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPatch: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: vi.fn(),
    patch: apiPatch,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const mockUser = {
  id: 'user-1',
  firstName: 'Jean',
  lastName: 'Pasteur',
  email: 'jean@eglise.com',
  activeRole: 'PASTEUR',
  roles: ['ADMIN', 'PASTEUR'],
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: mockUser }),
}));

vi.mock('@/hooks/useDictionaries', () => ({
  useDictionaries: () => ({
    label: (_: string, code: string) => code,
    options: () => [],
    color: () => null,
  }),
}));

const REQUESTS = [
  {
    id: 'r-1', type: 'SUGGESTION', statut: 'OUVERT', message: 'Je propose un nouveau groupe de louange',
    auteurNom: 'Marie', departmentNom: 'Louange', familyNom: null, cible: 'PASTEUR',
    createdAt: '2026-08-15T10:00:00', dateTraitement: null, traiteParNom: null, reponse: null, piecesJointes: [],
  },
  {
    id: 'r-2', type: 'RENDEZ_VOUS', statut: 'EN_COURS', message: 'Je voudrais un rendez-vous avec le pasteur',
    auteurNom: 'Pierre', departmentNom: 'Accueil', familyNom: 'Famille Grâce', cible: 'PASTEUR',
    createdAt: '2026-08-14T14:30:00', dateTraitement: null, traiteParNom: null, reponse: null, piecesJointes: [],
  },
  {
    id: 'r-3', type: 'SIGNALEMENT', statut: 'RESOLU', message: 'Problème de son pendant le culte',
    auteurNom: 'Paul', departmentNom: 'Technique', familyNom: null, cible: 'RESPONSABLE',
    createdAt: '2026-08-10T09:00:00', dateTraitement: '2026-08-11T12:00:00', traiteParNom: 'Jean Pasteur', reponse: 'Corrigé', piecesJointes: [],
  },
];

const PRESENCES = [
  {
    id: 'p-1', userId: 'u-1', nomMembre: 'Marie Dupont', semaine: '2026-08-11',
    presences: { 'Culte dimanche': true, 'Réunion mardi': false }, notes: 'Bienvenue',
  },
  {
    id: 'p-2', userId: 'u-2', nomMembre: 'Pierre Martin', semaine: '2026-08-11',
    presences: { 'Culte dimanche': true, 'Réunion mardi': true }, notes: null,
  },
];

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemberRequestsPage />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  queryClient.clear();
  vi.clearAllMocks();
  apiGet.mockImplementation(async (url: string) => {
    if (url.includes('/members/requests/inbox')) return { data: REQUESTS };
    if (url.includes('/members/presences/recent')) return { data: PRESENCES };
    return { data: [] };
  });
  apiPatch.mockResolvedValue({ data: {} });
});

describe('MemberRequestsPage — demandes des membres', () => {
  it('affiche le titre avec le bon rôle', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Toute l'église/)).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: /Demandes/ })).toBeInTheDocument();
  });

  it('affiche les demandes avec les messages', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Je propose un nouveau groupe de louange')).toBeInTheDocument();
    });
    expect(screen.getByText('Je voudrais un rendez-vous avec le pasteur')).toBeInTheDocument();
    expect(screen.getByText('Problème de son pendant le culte')).toBeInTheDocument();
  });

  it('affiche les noms des auteurs', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Marie')).toBeInTheDocument();
    });
    expect(screen.getByText('Pierre')).toBeInTheDocument();
    expect(screen.getByText('Paul')).toBeInTheDocument();
  });

  it('filtre par recherche textuelle', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Je propose un nouveau groupe de louange')).toBeInTheDocument();
    });
    const searchInput = screen.getByPlaceholderText(/Rechercher par message/);
    fireEvent.change(searchInput, { target: { value: 'louange' } });
    await waitFor(() => {
      expect(screen.getByText('Je propose un nouveau groupe de louange')).toBeInTheDocument();
    });
    expect(screen.queryByText('Problème de son pendant le culte')).not.toBeInTheDocument();
  });

  it('switch vers l\'onglet présences', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Marie')).toBeInTheDocument();
    });

    const presencesTab = screen.getByText(/Présences/);
    fireEvent.click(presencesTab);

    await waitFor(() => {
      expect(screen.getByText('Marie Dupont')).toBeInTheDocument();
    });
    expect(screen.getByText('Pierre Martin')).toBeInTheDocument();
  });
});

describe('MemberRequestsPage — stats', () => {
  it('affiche les 5 cartes stats', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Total')).toBeInTheDocument();
    });
    // Use getAllByText for labels that appear in multiple places
    expect(screen.getAllByText('Ouvertes').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('En cours').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Suggestions').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Signalements').length).toBeGreaterThanOrEqual(1);
  });
});

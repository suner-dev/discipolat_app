import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentMemberDossierPage from '@/pages/DepartmentMemberDossierPage';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1', memberId: 'm1' }),
    useNavigate: () => vi.fn(),
  };
});

const mockDossier = {
  profil: { id: 'm1', nomComplet: 'Aya Kouassi', nom: 'Kouassi', prenom: 'Aya', statut: 'ACTIF', membreActif: true },
  presences: { tauxPresence: 80, presents: 4, absents: 1, total: 5, liste: [] },
};

const mockEventAttendance = {
  soulId: 'm1',
  total: 2,
  presents: 1,
  absents: 0,
  nonMarques: 1,
  events: [
    { eventId: 'e1', titre: 'Convention départementale', typeEvenement: 'CONFERENCE', dateDebut: '2026-09-01T09:00:00', statut: 'PLANIFIE', present: true },
    { eventId: 'e2', titre: 'Sortie évangélisation', typeEvenement: 'SORTIE', dateDebut: '2026-08-10T08:00:00', statut: 'TERMINE', present: null },
  ],
};

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/event-attendance')) return Promise.resolve({ data: mockEventAttendance });
      if (url.includes('/dossier')) return Promise.resolve({ data: mockDossier });
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
      <MemoryRouter initialEntries={['/departments/dept-1/members/m1']}>
        <DepartmentMemberDossierPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DepartmentMemberDossierPage — présence aux événements du département', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('affiche la présence du membre sur les événements du département dans l’onglet Présences', async () => {
    renderPage();
    expect(await screen.findByText('Aya Kouassi')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Présences'));

    expect(await screen.findByText('Présence aux événements du département')).toBeInTheDocument();
    expect(await screen.findByText('Convention départementale')).toBeInTheDocument();
    expect(screen.getByText('Sortie évangélisation')).toBeInTheDocument();
    // Convention pointée présente, sortie non pointée
    expect(screen.getByText('✓ Présent')).toBeInTheDocument();
    expect(screen.getByText('Non pointé')).toBeInTheDocument();
  });

  it('permet de pointer le membre présent/absent à un événement depuis le dossier', async () => {
    const { default: api } = await import('@/lib/api');
    renderPage();
    expect(await screen.findByText('Aya Kouassi')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Présences'));

    const markPresentButtons = await screen.findAllByTitle('Marquer présent');
    // La convention est déjà présente → bouton désactivé ; on marque la sortie
    fireEvent.click(markPresentButtons[1]);

    await waitFor(() => expect(api.put).toHaveBeenCalledWith(
      '/departments/dept-1/events/e2/attendance',
      { soulId: 'm1', present: true },
    ));
  });
});

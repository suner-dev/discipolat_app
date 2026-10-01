import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import api from '@/lib/api';
import FamilyOsPanel from '@/components/family/FamilyOsPanel';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

// -- Fixures (formes réelles de /families/{id}/os/*) --------------------------
const DASHBOARD = {
  familyId: 'f1',
  upcomingVisits: [{ id: 'v1', soulId: 's1', soulName: 'Marie K.', visitDate: '2026-09-22', visitType: 'VISITE', subject: null, status: 'PLANNED', nextActionDate: null }],
  followUps: [{ id: 'v2', soulId: 's2', soulName: 'Jean P.', visitDate: '2026-09-10', visitType: 'SUIVI', subject: null, status: 'PLANNED', nextActionDate: '2026-09-15' }],
  recentReceptions: [],
  upcomingMeetings: [],
  overdueFollowUps: 1,
};
const VISITS = [
  { id: 'v1', familyId: 'f1', soulId: 's1', faiseurId: 'u9', visitDate: '2026-09-22', visitType: 'VISITE', subject: 'Premier contact', report: null, decisions: null, nextActionDate: null, nextActionType: null, status: 'PLANNED' },
  { id: 'v2', familyId: 'f1', soulId: 's2', faiseurId: 'u9', visitDate: '2026-09-10', visitType: 'SUIVI', subject: null, report: null, decisions: null, nextActionDate: '2026-09-15', nextActionType: 'Rappeler', status: 'COMPLETED' },
];
const MEMBERS = [
  { id: 's1', prenom: 'Marie', nom: 'K.', typeDisciple: 'NOUVEAU_CONVERTI', faiseurId: 'u9' },
  { id: 's2', prenom: 'Jean', nom: 'P.', typeDisciple: 'NOUVEL_ARRIVANT', faiseurId: null },
];
const CANDIDATES = [
  { soulId: 's7', userId: 'u7', prenom: 'Esther', nom: 'M.' },
  { soulId: 's8', userId: null, prenom: 'Paul', nom: 'D.' },
];
const ACTIVITIES = {
  content: [{ id: 'a1', activityType: 'VISIT', title: 'Visite: Marie K.', description: 'Premier contact', activityDate: '2026-09-22', status: 'PLANNED' }],
  page: 0, size: 20, totalElements: 1, totalPages: 1,
};

function stubApi() {
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url === '/families/f1/os/dashboard') return Promise.resolve({ data: DASHBOARD });
    if (url === '/families/f1/os/visits') return Promise.resolve({ data: VISITS });
    if (url === '/families/f1/os/receptions') return Promise.resolve({ data: [] });
    if (url === '/families/f1/os/meetings') return Promise.resolve({ data: [] });
    if (url === '/families/f1/os/members') return Promise.resolve({ data: MEMBERS });
    if (url.startsWith('/families/f1/os/activities')) return Promise.resolve({ data: ACTIVITIES });
    if (url.startsWith('/families/f1/os/search-souls')) return Promise.resolve({ data: CANDIDATES });
    if (url.startsWith('/users?role=FAISEUR')) return Promise.resolve({ data: { content: [{ id: 'u9', firstName: 'Pierre', lastName: 'F.' }] } });
    return Promise.resolve({ data: null });
  });
  (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
  (api.put as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
}

function renderPanel(props: Partial<{ familyId: string; canWrite: boolean }> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <FamilyOsPanel familyId={props.familyId ?? 'f1'} canWrite={props.canWrite ?? true} />
    </QueryClientProvider>,
  );
}

const clickTabWait = (label: string) => {
  fireEvent.click(screen.getByText(label));
};

beforeEach(() => {
  vi.clearAllMocks();
  stubApi();
});

describe('FamilyOsPanel (G4.1/G4.2)', () => {
  it('renders real dashboard KPIs from /os/dashboard', async () => {
    renderPanel();
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/families/f1/os/dashboard');
      expect(screen.getByText('Suivis en retard')).toBeInTheDocument();
    });
    // Liste des âmes en suivi (soulName serveur, pas de mock local)
    expect(await screen.findByText('Jean P.')).toBeInTheDocument();
    expect(screen.getByText('Marie K.')).toBeInTheDocument();
  });

  it('visits tab lists visits from /os/visits and can complete one via PUT', async () => {
    renderPanel();
    await screen.findByText('Tableau de bord');
    clickTabWait('Visites');
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/families/f1/os/visits'));
    expect(await screen.findByText(/Premier contact/)).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('os-visit-complete-v1'));
    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith('/families/f1/os/visits/v1', { status: 'COMPLETED' });
    });
  });

  it('members tab shows attached souls and add-soul modal searches scoped candidates', async () => {
    renderPanel();
    await screen.findByText('Tableau de bord');
    clickTabWait('Membres');
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/families/f1/os/members'));
    expect(await screen.findByText('Marie K.')).toBeInTheDocument();

    fireEvent.click(await screen.findByTestId('os-create-btn'));
    // scope par défaut CAMPUS (recherche scopée §G4.2)
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(expect.stringContaining('/families/f1/os/search-souls?scope=CAMPUS'));
    });
    expect(await screen.findByText('Esther M.')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Esther M.'));
    fireEvent.click(screen.getByTestId('os-add-confirm'));
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/families/f1/os/members?soulId=s7');
    });
  });

  it('hides all create actions when canWrite=false (write gate)', async () => {
    renderPanel({ canWrite: false });
    await screen.findByText('Tableau de bord');
    clickTabWait('Visites');
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/families/f1/os/visits'));
    await screen.findByText(/Premier contact/);
    expect(screen.queryByTestId('os-create-btn')).not.toBeInTheDocument();
    // Pas de boutons de statut non plus (mandat en cours → actions d'écriture)
    expect(screen.queryByTestId('os-visit-complete-v1')).not.toBeInTheDocument();
  });

  it('journal tab consumes the family-scoped activities endpoint (leak fix G4.1)', async () => {
    renderPanel();
    await screen.findByText('Tableau de bord');
    clickTabWait('Journal');
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith(expect.stringContaining('/families/f1/os/activities?'));
    });
    expect(await screen.findByText('Visite: Marie K.')).toBeInTheDocument();
  });
});

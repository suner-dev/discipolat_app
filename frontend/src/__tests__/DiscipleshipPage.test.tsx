import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DiscipleshipPage from '@/pages/DiscipleshipPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const svc = vi.hoisted(() => ({
  listJourneys: vi.fn(),
  listStages: vi.fn(),
  listProgress: vi.fn(),
  listAssignments: vi.fn(),
  listMeetings: vi.fn(),
  getReport: vi.fn(),
  getTopMentors: vi.fn(),
  createJourney: vi.fn(),
  createProgress: vi.fn(),
  createAssignment: vi.fn(),
  scheduleMeeting: vi.fn(),
  endAssignment: vi.fn(),
  completeMeeting: vi.fn(),
}));

vi.mock('@/services/discipleshipService', () => svc);

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const JOURNEY = {
  id: 1, name: 'Fondations', description: 'Parcours de base', type: 'NEW_BELIEVER',
  totalStages: 4, startDate: null, endDate: null, isActive: true,
};
const PROGRESS = {
  id: 7, discipleId: 'd-uuid', discipleName: 'Paul', journeyId: 1, journeyName: 'Fondations',
  currentStageName: 'Stage 1', completedStages: 1, totalStages: 4,
  completedRequirements: 2, totalRequirements: 10, status: 'IN_PROGRESS',
};

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <DiscipleshipPage />
    </QueryClientProvider>
  );
}

describe('DiscipleshipPage — câblée sur discipleshipService (V233)', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    svc.listJourneys.mockResolvedValue([JOURNEY]);
    svc.listStages.mockResolvedValue([{ id: 11, name: 'Repentance', order: 1 }]);
    svc.listProgress.mockResolvedValue([PROGRESS]);
    svc.listAssignments.mockResolvedValue([]);
    svc.listMeetings.mockResolvedValue([]);
    svc.getReport.mockResolvedValue({ journeyId: 1, totalDisciples: 8, activeDisciples: 5, completedDisciples: 1, stalledDisciples: 2, averageCompletion: 42, totalMeetings: 20, completedMeetings: 17, generatedAt: '2026-10-01' });
    svc.getTopMentors.mockResolvedValue([{ mentorId: 'm-uuid', mentorName: 'Timothée', activeDisciples: 3 }]);
    svc.createJourney.mockResolvedValue(JOURNEY);
  });

  it('charge les parcours via listJourneys et affiche les vues serveur', async () => {
    renderPage();
    await waitFor(() => expect(svc.listJourneys).toHaveBeenCalled());
    expect(await screen.findByText('Fondations')).toBeInTheDocument();
    expect(screen.getByText(/NEW_BELIEVER/)).toBeInTheDocument();
  });

  it('affiche les étapes du parcours sélectionné via listStages', async () => {
    const { container } = renderPage();
    await screen.findByText('Fondations');
    fireEvent.click(screen.getByRole('button', { name: /étapes/i }));
    await waitFor(() => {
      expect(svc.listStages).toHaveBeenCalledWith(1);
      expect(screen.getByText(/Repentance/)).toBeInTheDocument();
    });
    expect(container).toBeTruthy();
  });

  it('crée un parcours avec uniquement les clés body vérifiées côté serveur', async () => {
    const { container } = renderPage();
    await screen.findByText('Fondations');
    fireEvent.click(screen.getByRole('button', { name: /nouveau/i }));
    const nameInput = container.querySelectorAll('input')[0];
    fireEvent.change(nameInput, { target: { value: 'Nouveau parcours' } });
    fireEvent.click(screen.getByRole('button', { name: /^créer$/i }));
    await waitFor(() => {
      expect(svc.createJourney).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Nouveau parcours', type: 'GROWTH', totalStages: 5,
      }));
    });
    expect(svc.createJourney.mock.calls[0][0]).not.toHaveProperty('id');
  });

  it('change d\'onglet : progressions listées via listProgress', async () => {
    renderPage();
    await screen.findByText('Fondations');
    fireEvent.click(screen.getByRole('button', { name: /^progress$/i }));
    await waitFor(() => {
      expect(svc.listProgress).toHaveBeenCalled();
      expect(screen.getByText('Paul')).toBeInTheDocument();
    });
  });

  it('onglet rapports : getReport + getTopMentors sur le parcours choisi', async () => {
    renderPage();
    await screen.findByText('Fondations');
    fireEvent.click(screen.getByRole('button', { name: /^reports$/i }));
    const select = document.querySelector('select') as HTMLSelectElement;
    fireEvent.change(select, { target: { value: '1' } });
    await waitFor(() => {
      expect(svc.getReport).toHaveBeenCalledWith(1);
      expect(svc.getTopMentors).toHaveBeenCalledWith(1);
      expect(screen.getByText(/Timothée/)).toBeInTheDocument();
    });
  });
});

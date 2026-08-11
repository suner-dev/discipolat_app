import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminFeedbackPage from '@/pages/AdminFeedbackPage';

const { apiGet, apiPatch, apiPost, apiPut, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPatch: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    patch: apiPatch,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

const FEEDBACKS = [
  {
    id: 'fb-1',
    category: 'BUG',
    priority: 'HAUTE',
    subject: 'Le rapport ne se soumet pas',
    description: 'Erreur 500 à la soumission du rapport du faiseur.',
    pageUrl: 'https://beta.example.com/reports/maker',
    browser: 'Chrome',
    device: 'Desktop',
    os: 'Windows',
    appVersion: '1.1.0',
    status: 'NOUVEAU',
    reporterEmail: 'faiseur@discipolat.com',
    createdAt: '2026-08-10T09:00:00Z',
    updatedAt: '2026-08-10T09:00:00Z',
  },
  {
    id: 'fb-2',
    category: 'SUGGESTION',
    priority: 'BASSE',
    subject: 'Ajouter un export Excel',
    description: 'Ce serait utile pour les rapports mensuels.',
    status: 'RESOLU',
    reporterEmail: 'pasteur@discipolat.com',
    createdAt: '2026-08-05T14:30:00Z',
    updatedAt: '2026-08-06T10:00:00Z',
  },
];

const STATS = { total: 2, nouveaux: 1, enCours: 0, resolus: 1, rejetes: 0, parCategorie: { BUG: 1, SUGGESTION: 1 } };

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminFeedbackPage />
    </QueryClientProvider>
  );
}

describe('AdminFeedbackPage', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/admin/feedback/stats') return { data: STATS };
      if (url === '/admin/feedback') return { data: FEEDBACKS };
      return { data: {} };
    });
    apiPatch.mockResolvedValue({ data: { ...FEEDBACKS[0], status: 'EN_COURS' } });
  });

  it('affiche les statistiques des retours', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Retours testeurs')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByText('2')).toBeInTheDocument());
    expect(screen.getByText('Nouveaux')).toBeInTheDocument();
    expect(screen.getByText('Résolus')).toBeInTheDocument();
  });

  it('liste les retours avec sujet, catégorie et émetteur', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Le rapport ne se soumet pas')).toBeInTheDocument();
    });
    expect(screen.getByText(/faiseur@discipolat.com/)).toBeInTheDocument();
    expect(screen.getByText('Ajouter un export Excel')).toBeInTheDocument();
  });

  it('change le statut d un retour via PATCH', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Le rapport ne se soumet pas')).toBeInTheDocument());

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: 'EN_COURS' } });

    await waitFor(() => {
      expect(apiPatch).toHaveBeenCalledWith('/admin/feedback/fb-1/status', { status: 'EN_COURS' });
    });
  });

  it('filtre par statut', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Le rapport ne se soumet pas')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /résolu/i }));

    await waitFor(() => {
      expect(screen.getByText('Ajouter un export Excel')).toBeInTheDocument();
    });
    expect(screen.queryByText('Le rapport ne se soumet pas')).not.toBeInTheDocument();
  });

  it('affiche un état vide quand il n y a aucun retour', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/admin/feedback/stats') return { data: { ...STATS, total: 0, nouveaux: 0, resolus: 0, parCategorie: {} } };
      return { data: [] };
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Aucun retour pour le moment/)).toBeInTheDocument();
    });
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SyncConflictsPage from '@/pages/SyncConflictsPage';

const { apiGet, apiPost } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const CONFLICTS = [
  {
    id: 'cf-1',
    clientUuid: 'op-123',
    entityType: 'MEMBER_PRESENCE',
    entityId: 'soul-9',
    fieldName: 'presences',
    clientValue: '{"present":false}',
    serverValue: '{"present":true}',
    clientOpAt: '2026-09-14T08:05:00',
    serverUpdatedAt: '2026-09-14T08:10:00',
    createdAt: '2026-09-14T08:12:00Z',
  },
];

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <SyncConflictsPage />
    </QueryClientProvider>,
  );
}

describe('SyncConflictsPage (§G5.7 — réconciliation LWW)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('liste les conflits ouverts avec les deux versions', async () => {
    apiGet.mockResolvedValue({ data: CONFLICTS });
    renderPage();
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith('/sync/conflicts'));
    expect(await screen.findByText(/MEMBER_PRESENCE/)).toBeInTheDocument();
    // pretty() pré-formatte le JSON (multiligne) : on teste le contenu sémantique.
    expect(screen.getByText(/"present": false/)).toBeInTheDocument();
    expect(screen.getByText(/"present": true/)).toBeInTheDocument();
  });

  it('valide la réconciliation par note documentée (POST réel)', async () => {
    apiGet.mockResolvedValue({ data: CONFLICTS });
    apiPost.mockResolvedValue({ data: { ...CONFLICTS[0], resolved: true } });
    renderPage();
    const textarea = await screen.findByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'Serveur conservé, mobile notifié' } });
    fireEvent.click(screen.getByText(/Valider la réconciliation/));
    await waitFor(() =>
      expect(apiPost).toHaveBeenCalledWith('/sync/conflicts/cf-1/resolve', {
        note: 'Serveur conservé, mobile notifié',
      }),
    );
  });

  it('état vide honnête quand la file est propre', async () => {
    apiGet.mockResolvedValue({ data: [] });
    renderPage();
    expect(
      await screen.findByText(/Aucun conflit en attente/),
    ).toBeInTheDocument();
    expect(apiPost).not.toHaveBeenCalled();
  });
});

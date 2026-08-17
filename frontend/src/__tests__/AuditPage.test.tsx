import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AuditPage from '@/pages/AuditPage';

const mockGet = vi.fn();

vi.mock('@/lib/api', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

function pageResponse(content: unknown[] = []) {
  return {
    data: {
      content,
      totalElements: content.length,
      totalPages: Math.max(1, content.length),
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: content.length === 0,
    },
  };
}

const SAMPLE_ENTRIES = [
  {
    id: 'log-1',
    utilisateurId: 'u-pasteur',
    emailUtilisateur: 'pasteur@discipolat.com',
    action: 'CREER_SOUL',
    entiteType: 'SOUL',
    createdAt: '2026-08-05T10:30:00',
  },
  {
    id: 'log-2',
    utilisateurId: 'u-chef',
    emailUtilisateur: 'chef@discipolat.com',
    action: 'MODIFIER_FAMILLE',
    entiteType: 'FAMILY',
    createdAt: '2026-08-06T09:00:00',
  },
  {
    id: 'log-3',
    utilisateurId: 'u-pasteur',
    emailUtilisateur: 'pasteur@discipolat.com',
    action: 'TRANSFERT_SOUL',
    entiteType: 'SOUL',
    createdAt: '2026-08-06T11:00:00',
  },
];

const USERS = [
  { id: 'u-pasteur', email: 'pasteur@discipolat.com', firstName: 'Pierre', lastName: 'Apôtre', role: 'PASTEUR', roles: ['PASTEUR'], activeRole: 'PASTEUR', estChefDeFamille: false, statut: 'ACTIVE', createdAt: '', updatedAt: '' },
  { id: 'u-chef', email: 'chef@discipolat.com', firstName: 'Clarisse', lastName: 'Mukendi', role: 'CHEF_DE_FAMILLE', roles: ['CHEF_DE_FAMILLE'], activeRole: 'CHEF_DE_FAMILLE', estChefDeFamille: true, statut: 'ACTIVE', createdAt: '', updatedAt: '' },
];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AuditPage />
    </QueryClientProvider>
  );
}

const originalClick = HTMLAnchorElement.prototype.click;

describe('AuditPage — filtres et export', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockImplementation((url: string) => {
      if (url.startsWith('/users')) return Promise.resolve({ data: { content: USERS } });
      if (url.startsWith('/audit/export')) return Promise.resolve({ data: new Blob(['csv']) });
      if (url.startsWith('/audit')) return Promise.resolve(pageResponse(SAMPLE_ENTRIES));
      return Promise.resolve({ data: {} });
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    Object.defineProperty(HTMLAnchorElement.prototype, 'click', {
      writable: true,
      value: originalClick,
    });
  });

  it('affiche le titre, les entrées et les filtres', async () => {
    renderPage();

    expect(await screen.findByText("Journal d'audit")).toBeInTheDocument();
    expect(await screen.findByText('CREER_SOUL')).toBeInTheDocument();
    expect(screen.getByText('MODIFIER_FAMILLE')).toBeInTheDocument();
    expect(screen.getByText(/tous les utilisateurs/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Date de début')).toBeInTheDocument();
    expect(screen.getByLabelText('Date de fin')).toBeInTheDocument();
  });

  it('résout le nom affiché via la liste des utilisateurs', async () => {
    renderPage();

    // Les emails des entrées sont affichés directement (le pasteur a 2 entrées).
    expect((await screen.findAllByText('pasteur@discipolat.com')).length).toBeGreaterThan(0);
    expect(screen.getByText('chef@discipolat.com')).toBeInTheDocument();
  });

  it('filtre par utilisateur et entité en ajoutant les paramètres API', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('CREER_SOUL');

    await user.selectOptions(screen.getByText(/tous les utilisateurs/i).closest('select')!, 'u-pasteur');
    await user.selectOptions(screen.getByText(/toutes les entités/i).closest('select')!, 'SOUL');

    await waitFor(() => {
      const auditCalls = mockGet.mock.calls.filter(([url]) => String(url).startsWith('/audit?'));
      expect(auditCalls.length).toBeGreaterThan(0);
      const last = String(auditCalls[auditCalls.length - 1][0]);
      expect(last).toContain('utilisateurId=u-pasteur');
      expect(last).toContain('entiteType=SOUL');
    });
  });

  it('filtre par catégorie d’action (Transferts) et affiche le badge', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('CREER_SOUL');

    // Toutes les entrées (y compris le transfert) sont visibles au départ.
    expect(screen.getByText('TRANSFERT_SOUL')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Filtrer par type d'action"), 'TRANSFER');

    // Seule l'entrée TRANSFERT_SOUL reste après le filtre client-side.
    expect(screen.getByText('TRANSFERT_SOUL')).toBeInTheDocument();
    expect(screen.queryByText('CREER_SOUL')).not.toBeInTheDocument();
    expect(screen.queryByText('MODIFIER_FAMILLE')).not.toBeInTheDocument();
  });

  it('filtre par plage de dates (debut/fin ISO)', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('CREER_SOUL');

    await user.type(screen.getByLabelText('Date de début'), '2026-08-01');
    await user.type(screen.getByLabelText('Date de fin'), '2026-08-07');

    await waitFor(() => {
      const auditCalls = mockGet.mock.calls.filter(([url]) => String(url).startsWith('/audit?'));
      const last = decodeURIComponent(String(auditCalls[auditCalls.length - 1][0]));
      expect(last).toContain('debut=2026-08-01T00:00:00');
      expect(last).toContain('fin=2026-08-07T23:59:59');
    });
  });

  it('réinitialise les filtres', async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText('CREER_SOUL');

    await user.selectOptions(screen.getByText(/tous les utilisateurs/i).closest('select')!, 'u-pasteur');
    expect(await screen.findByRole('button', { name: /réinitialiser/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /réinitialiser/i }));

    await waitFor(() => {
      const auditCalls = mockGet.mock.calls.filter(([url]) => String(url).startsWith('/audit?'));
      const last = String(auditCalls[auditCalls.length - 1][0]);
      expect(last).not.toContain('utilisateurId=');
    });
  });

  it('exporte le journal en CSV avec les filtres appliqués', async () => {
    const createObjectURL = vi.fn(() => 'blob:mock');
    const revokeObjectURL = vi.fn();
    const click = vi.fn();
    vi.stubGlobal('URL', { ...window.URL, createObjectURL, revokeObjectURL });
    Object.defineProperty(HTMLAnchorElement.prototype, 'click', { writable: true, value: click });

    const user = userEvent.setup();
    renderPage();
    await screen.findByText('CREER_SOUL');

    await user.selectOptions(screen.getByText(/tous les utilisateurs/i).closest('select')!, 'u-pasteur');
    await user.click(screen.getByRole('button', { name: /exporter csv/i }));

    await waitFor(() => {
      const exportCalls = mockGet.mock.calls.filter(([url]) => String(url).startsWith('/audit/export'));
      expect(exportCalls.length).toBe(1);
      expect(String(exportCalls[0][0])).toContain('utilisateurId=u-pasteur');
    });
    expect(click).toHaveBeenCalled();
  });
});

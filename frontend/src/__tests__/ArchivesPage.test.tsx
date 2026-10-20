import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ArchivesPage from '@/pages/ArchivesPage';

/**
 * V272 — contrat de lecture de l'écran « Archives » : la liste vient de
 * `GET /archives/search` (résumés paginés, jamais d'instantanés), les filtres
 * de `GET /archives/facets`, le détail de `GET /archives/{id}` et la
 * navigation de versions de `/search?eventId=` ; Échap ferme ; pagination
 * Précédent/Suivant. §C/§D du fichier `TODO_ARCHIVES_V272.md`.
 */

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

vi.mock('@/lib/api', () => ({
  default: apiMock,
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const SUMMARY_V2 = {
  id: 'a2', eventId: 'e1', eventTitle: 'Séminaire du 4 mars',
  eventStartAt: '2023-03-04T09:00:00Z', eventYear: 2023, eventMonth: 3,
  spaceId: null, version: 2, attendanceCount: 7,
  archivedAt: '2023-03-05T10:00:00Z', archivedBy: 'u1',
};
const SUMMARY_V1 = {
  ...SUMMARY_V2, id: 'a1', eventTitle: 'Veillée de prière',
  version: 1, attendanceCount: 5,
};

const DETAIL_V2 = {
  ...SUMMARY_V2,
  snapshotJson: {
    title: 'Séminaire du 4 mars', type: 'REUNION',
    startAt: '2023-03-04T09:00:00Z', endAt: '2023-03-04T17:00:00Z',
    description: 'Retraite trimestrielle', attendanceCount: 7,
    spaces: [{ spaceName: 'Sanctuaire', role: 'MAIN' }],
    teams: [{ name: 'Louange' }],
    tasks: [{ title: 'Sonoriser', status: 'DONE' }],
    schedule: [{ title: 'Ouverture', startAt: '2023-03-04T09:30:00Z' }],
    assets: [{ quantity: 2 }],
    dressCodes: [{ title: 'Tenue choeur', status: 'ARCHIVED', rules: [{ groupName: 'Choeur', description: 'Robe blanche' }] }],
  },
};
const DETAIL_V1 = { ...SUMMARY_V1, snapshotJson: { ...DETAIL_V2.snapshotJson, attendanceCount: 5 } };

const pageOf = (content: unknown[], page = 0, totalPages = 1, size = 12) => ({
  content, page, size, totalElements: content.length, totalPages,
});

function mockHappyPaths() {
  apiMock.get.mockImplementation((url: string) => {
    if (url.endsWith('/archives/facets')) {
      return Promise.resolve({ data: [{ eventYear: 2023, months: [3], archiveCount: 2 }] });
    }
    if (url.includes('/archives/search?eventId=') || url.includes('eventId=')) {
      return Promise.resolve({ data: pageOf([DETAIL_V2, DETAIL_V1], 0, 1, 50) });
    }
    if (url.includes('/archives/search')) {
      return Promise.resolve({ data: pageOf([SUMMARY_V2, SUMMARY_V1]) });
    }
    if (/\/archives\/a1$/.test(url)) return Promise.resolve({ data: DETAIL_V1 });
    if (/\/archives\/a2$/.test(url)) return Promise.resolve({ data: DETAIL_V2 });
    return Promise.resolve({ data: [] });
  });
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ArchivesPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  cleanup();
  vi.clearAllMocks();
  mockHappyPaths();
});

describe('ArchivesPage (V272)', () => {
  it('liste via /archives/search : résumés paginés, sans instantané téléchargé', async () => {
    renderPage();

    expect(await screen.findByText('Séminaire du 4 mars')).toBeInTheDocument();
    expect(screen.getByText('v2')).toBeInTheDocument();
    expect(screen.getByText(/7\s+présences/i)).toBeInTheDocument();
    // Le contrat anti-téléchargement intégral : la liste est passée par /search.
    expect(apiMock.get).toHaveBeenCalledWith(expect.stringContaining('/church-events/archives/search?'));
    expect(apiMock.get).toHaveBeenCalledWith('/church-events/archives/facets');
    // Aucun GET /archives (liste historique intégrale) au chargement.
    expect(apiMock.get).not.toHaveBeenCalledWith('/church-events/archives');
  });

  it('filtres alimentés par /facets : année servie, mois Intl (locale fr)', async () => {
    renderPage();

    const yearSelect = await screen.findByLabelText('Année');
    // L'option 2023 n'existe qu'une fois /facets chargée : on l'attend avant
    // de changer la valeur (sinon le <select> retombe sur l'option vide).
    await screen.findByRole('option', { name: '2023' });
    fireEvent.change(yearSelect, { target: { value: '2023' } });

    const monthSelect = screen.getByLabelText('Mois');
    await waitFor(() => expect(monthSelect).not.toBeDisabled());
    // Mars rendu par Intl.DateTimeFormat de la locale active, non codé en dur.
    expect(monthSelect.textContent).toContain('mars');

    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(expect.stringContaining('year=2023')),
    );
  });

  it('détail via GET /archives/{id} puis navigation entre versions (eventId)', async () => {
    renderPage();

    fireEvent.click(await screen.findByText('Séminaire du 4 mars'));

    // La fiche complète arrive par la ressource de détail — sections lisibles.
    expect(await screen.findByText('Retraite trimestrielle')).toBeInTheDocument();
    expect(screen.getByText('Tenue choeur')).toBeInTheDocument();
    expect(screen.getByText('Sanctuaire')).toBeInTheDocument();
    expect(apiMock.get).toHaveBeenCalledWith('/church-events/archives/a2');

    // Versions du même événement : /search?eventId= (résumés), puis clic v1.
    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(expect.stringContaining('eventId=e1')),
    );
    fireEvent.click(screen.getByLabelText('Version 1'));
    await waitFor(() => expect(apiMock.get).toHaveBeenCalledWith('/church-events/archives/a1'));
  });

  it('Échap ferme la fiche de détail', async () => {
    renderPage();

    fireEvent.click(await screen.findByText('Séminaire du 4 mars'));
    const dialog = await screen.findByRole('dialog');
    fireEvent.keyDown(dialog, { key: 'Escape' });

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('pagination Précédent/Suivant : page 2 requêtée auprès du serveur', async () => {
    apiMock.get.mockImplementation((url: string) => {
      if (url.endsWith('/archives/facets')) return Promise.resolve({ data: [] });
      if (url.includes('/archives/search')) {
        const onPage1 = url.includes('page=1');
        return Promise.resolve({
          data: { content: [onPage1 ? SUMMARY_V1 : SUMMARY_V2], page: onPage1 ? 1 : 0, size: 12, totalElements: 2, totalPages: 2 },
        });
      }
      return Promise.resolve({ data: null });
    });
    renderPage();

    await screen.findByText('Séminaire du 4 mars');
    fireEvent.click(screen.getByText('Suivant'));

    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(expect.stringContaining('page=1')),
    );
    expect(await screen.findByText('v1')).toBeInTheDocument();
  });

  it('état vide : aucune archive renvoyée par /search', async () => {
    apiMock.get.mockImplementation((url: string) => {
      if (url.endsWith('/archives/facets')) return Promise.resolve({ data: [] });
      if (url.includes('/archives/search')) return Promise.resolve({ data: pageOf([]) });
      return Promise.resolve({ data: null });
    });
    renderPage();

    expect(await screen.findByText(/Aucune archive pour cette période/)).toBeInTheDocument();
  });

  it('état d’erreur : /search échoue → message explicite, pas d’explosion', async () => {
    apiMock.get.mockImplementation((url: string) => {
      if (url.endsWith('/archives/facets')) return Promise.resolve({ data: [] });
      if (url.includes('/archives/search')) return Promise.reject(new Error('500'));
      return Promise.resolve({ data: null });
    });
    renderPage();

    expect(await screen.findByText('Impossible de charger les archives.')).toBeInTheDocument();
  });
});

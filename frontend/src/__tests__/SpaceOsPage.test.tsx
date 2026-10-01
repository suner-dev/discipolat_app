import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import api from '@/lib/api';
import SpaceOsPage from '@/pages/SpaceOsPage';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

let currentParams: Record<string, string> = {};
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => currentParams,
    Link: ({ to, children }: { to: string; children: React.ReactNode }) => (
      <a href={to}>{children}</a>
    ),
  };
});

// Deux espaces, même base de code — la seule différence vient du bootstrap.
const SPACE_A = {
  spaceId: 'sp-a', tenantId: 't1', organizationUnitId: null,
  spaceType: 'DEPARTMENT', templateCode: 'TPL_AUDIOVISUEL',
  name: 'Régie Audiovisuelle', code: 'DEP-RAV', icon: null, color: '#f59e0b',
  description: null, status: 'ACTIVE', visiblePeopleScope: 'CHURCH', entityType: 'DEPARTMENT',
  modules: [{ code: 'EVENEMENTS', enabled: true }, { code: 'EQUIPEMENTS', enabled: false }],
  statuses: [], customFields: [], widgets: [], widgetsLocked: false,
  permissions: { canCustomize: false, permissionKeys: [], roleKeys: ['FAISEUR'] },
  memberCount: 2,
  membersPreview: [
    { personId: 'p1', fullName: 'Marie Dupont', responsibility: null, membershipType: 'MEMBER', joinedAt: '2026-01-05T00:00:00Z' },
    { personId: 'p2', fullName: null, responsibility: 'Son', membershipType: 'MEMBER', joinedAt: null },
  ],
  uiConfig: {}, actions: {}, updatedAt: null,
};

const SPACE_B = {
  ...SPACE_A,
  spaceId: 'sp-b', name: 'Famille Bethel', code: 'FAM-BET', templateCode: null,
  spaceType: 'FAMILY',
  modules: [{ code: 'TACHES', enabled: true }],
  statuses: [
    { code: 'EN_ATTENTE', name: 'En attente', color: '#ef4444', initial: true, final: false, allowedTransitions: ['ACTIF'] },
    { code: 'ACTIF', name: 'Actif', color: '#22c55e', initial: false, final: true, allowedTransitions: [] },
  ],
  customFields: [
    { key: 'NB_ENFANTS', label: 'Nombre d\u2019enfants', type: 'NUMBER', required: true, options: [], placeholder: null, defaultValue: '0' },
  ],
  permissions: { canCustomize: true, permissionKeys: ['SPACE_UPDATE'], roleKeys: ['CHEF_DE_FAMILLE'] },
};

function stubBootstrap() {
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url === '/spaces/sp-a/bootstrap') return Promise.resolve({ data: SPACE_A });
    if (url === '/spaces/sp-b/bootstrap') return Promise.resolve({ data: SPACE_B });
    if (url.startsWith('/custom-fields/')) return Promise.resolve({ data: { NB_ENFANTS: '3' } });
    return Promise.resolve({ data: null });
  });
  (api.put as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
}

function renderPage(id: string) {
  currentParams = { id };
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <SpaceOsPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  stubBootstrap();
});

describe('SpaceOsPage — expérience générée (G5.3)', () => {
  it('space A (template audiovisuel, pas de statuts) : onglets générés = Vue ensemble + Membres seulement', async () => {
    renderPage('sp-a');
    await waitFor(() => expect(screen.getByText('Régie Audiovisuelle')).toBeInTheDocument());
    expect(screen.getByRole('tab', { name: /Vue d'ensemble/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Membres \(2\)/ })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /Statuts/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: /Champs/ })).not.toBeInTheDocument();
    // canCustomize=false côté serveur → aucun onglet Réglages rendu
    expect(screen.queryByRole('tab', { name: /Réglages/ })).not.toBeInTheDocument();
  });

  it('space B (statuts + champs customisés + canCustomize) : onglets supplémentaires générés', async () => {
    renderPage('sp-b');
    await waitFor(() => expect(screen.getByText('Famille Bethel')).toBeInTheDocument());
    expect(screen.getByRole('tab', { name: /Statuts/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Champs/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Réglages/ })).toBeInTheDocument();
  });

  it('module désactivé au niveau tenant = tuile informative, jamais un lien mort', async () => {
    renderPage('sp-a');
    const tile = await screen.findByText('Désactivé par la plateforme');
    const link = tile.closest('a');
    expect(link).toBeNull();
    // module activé avec page réelle → lien véritable /events
    expect(screen.getByText('Ouvrir').closest('a')).toHaveAttribute('href', '/events');
  });

  it('onglet Champs : lit et envoie les vraies valeurs G2.4 sur /custom-fields/{entityType}/{spaceId}', async () => {
    renderPage('sp-b');
    await waitFor(() => expect(screen.getByText('Famille Bethel')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: /Champs/ }));
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/custom-fields/DEPARTMENT/sp-b');
    });
    const input = await screen.findByLabelText(/Nombre/);
    expect(input).toHaveValue(3); // input type=number → valeur numérique
    fireEvent.change(input, { target: { value: '4' } });
    fireEvent.click(screen.getByRole('button', { name: /Enregistrer les valeurs/ }));
    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith('/custom-fields/DEPARTMENT/sp-b', {
        values: { NB_ENFANTS: '4' },
      });
    });
  });

  it('onglet Réglages (canCustomize serveur) : PUT partiel /spaces/{id} avec le nom', async () => {
    renderPage('sp-b');
    await waitFor(() => expect(screen.getByText('Famille Bethel')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('tab', { name: /Réglages/ }));
    const nameInput = await screen.findByLabelText(/Nom de l'espace/);
    fireEvent.change(nameInput, { target: { value: 'Famille Bethel rénovée' } });
    fireEvent.click(screen.getByRole('button', { name: /Enregistrer$/ }));
    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith('/spaces/sp-b', expect.objectContaining({
        name: 'Famille Bethel rénovée',
      }));
    });
  });
});

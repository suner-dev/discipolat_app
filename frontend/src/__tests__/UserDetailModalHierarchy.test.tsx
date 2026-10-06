import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UserDetailModal } from '@/components/users/UserDetailModal';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPut } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: apiPut,
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const PASTEUR_ID = 'aaaaaaaa-0000-0000-0000-000000000001';
const BERGER_ID = 'aaaaaaaa-0000-0000-0000-000000000002';
const MEMBRE_ID = 'aaaaaaaa-0000-0000-0000-000000000003';

function detail(overrides: Record<string, unknown> = {}) {
  return {
    id: MEMBRE_ID,
    firstName: 'Awa',
    lastName: 'Diallo',
    email: 'awa@eglise.org',
    role: 'MEMBRE',
    statut: 'ACTIVE',
    evaluations: {},
    ame: null,
    dossier: [],
    dossierDocuments: [],
    hierarchie: {
      rolePrincipal: 'MEMBRE',
      nombreBranches: 1,
      ascendants: [
        { id: PASTEUR_ID, nom: 'Jean Maka', via: 'ORGANISATION', noeud: 'Église mère', noeudType: 'ROOT_CHURCH' },
        { id: BERGER_ID, nom: 'Paul Beye', via: 'DECLARATIF', typeRelation: 'MENTOR', typeLabel: 'Mon mentor' },
      ],
      branches: [
        {
          noeud: { id: 'node-1', nom: 'Campus Nord', type: 'CAMPUS', level: 2 },
          origine: 'ASSIGNATION_V3',
          niveaux: 3,
          chaine: [
            { id: 'node-1', nom: 'Campus Nord', type: 'CAMPUS', level: 2, responsable: { id: BERGER_ID, nom: 'Paul Beye' } },
            { id: 'node-0', nom: 'Église mère', type: 'ROOT_CHURCH', level: 0, responsable: { id: PASTEUR_ID, nom: 'Jean Maka' } },
          ],
        },
      ],
      suivi: {
        faiseur: { id: PASTEUR_ID, nom: 'Jean Maka' },
        chefDeFamille: null,
        familleGeree: null,
        departementsDiriges: [{ id: 'd1', nom: 'Louange' }],
        noeudsDiriges: [],
        amesSuiviesTotal: 0,
      },
      resume: {
        branchesOrganisationnelles: 1,
        encadrantsDeclares: 1,
        membresRattaches: 1,
        origines: ['ASSIGNATION_V3', 'DECLARATIF'],
        hierarchieComplete: true,
      },
    },
    relations: {
      sortantes: [
        {
          id: 'rel-1', otherUserId: BERGER_ID, otherNom: 'Paul Beye',
          relationType: 'MENTOR', typeLabel: 'Mon mentor', statut: 'ACTIVE',
        },
      ],
      entrantes: [
        {
          id: 'rel-2', otherUserId: 'user-x', otherNom: 'Moussa Diop',
          relationType: 'MENTOR', typeLabel: 'Mon mentor', statut: 'ACTIVE',
        },
      ],
    },
    ...overrides,
  };
}

function renderModal(userId = MEMBRE_ID) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <UserDetailModal userId={userId} onClose={() => {}} />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  apiGet.mockImplementation(async (url: string) => {
    if (url.startsWith('/hierarchy/users/')) return { data: detail().hierarchie };
    if (url.startsWith('/relations/users/')) return { data: detail().relations };
    if (url.startsWith('/users/')) return { data: detail() };
    if (url.startsWith('/evaluations')) return { data: [] };
    return { data: [] };
  });
});

describe('UserDetailModal — hiérarchie et encadrement (V231)', () => {
  it('affiche les responsables unifiés (organisation ∪ déclaratif) comme personnes cliquables', async () => {
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());

    // Jean Maka apparaît plusieurs fois (ascendant, arbre, suivi) : on vérifie
    // que chaque occurrence est bien un bouton actionnable.
    expect(screen.getAllByRole('button', { name: /Jean Maka/ }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /Paul Beye/ }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /Église mère/ }).length).toBeGreaterThan(0);
  });

  it('rend l’ARBRE des branches avec le responsable de chaque niveau', async () => {
    const { container } = renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());

    // Racine de l'arbre = rôle « tree », branches = « treeitem ».
    expect(container.querySelector('[role="tree"]')).toBeInTheDocument();
    const items = container.querySelectorAll('[role="treeitem"]');
    expect(items.length).toBeGreaterThanOrEqual(3); // Campus Nord, Berger, Église mère

    // Le libellé de branche porte son origine (traçabilité de la donnée) :
    // « Campus Nord » apparaît en racine ET en maillon de la chaîne.
    expect(screen.getAllByText(/Campus Nord/).length).toBeGreaterThanOrEqual(2);
    // L'origine est désormais LOCALISÉE (plus le code technique brut).
    expect(screen.getByText(/assignation/i)).toBeInTheDocument();
  });

  it('navigue vers la fiche du responsable et revient en arrière (fil d’Ariane)', async () => {
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());

    // Au départ : aucun retour possible (une seule fiche consultée).
    expect(screen.queryByRole('button', { name: /^Retour$/ })).not.toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: /Paul Beye/ })[0]);

    // Le fil d'Ariane apparaît et la requête porte bien sur la nouvelle fiche.
    await waitFor(() => {
      expect(apiGet).toHaveBeenCalledWith(`/users/${BERGER_ID}/detail`);
    });
    const back = await screen.findByRole('button', { name: /^Retour$/ });
    expect(screen.getByRole('navigation', { name: /Hiérarchie consultée/ })).toBeInTheDocument();

    fireEvent.click(back);
    await waitFor(() => {
      expect(apiGet).toHaveBeenCalledWith(`/users/${MEMBRE_ID}/detail`);
    });
  });

  it('affiche l’encadrement pastoral (pasteur, départements dirigés)', async () => {
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());

    expect(screen.getByText('Encadrement pastoral')).toBeInTheDocument();
    expect(screen.getByText('Départements dirigés')).toBeInTheDocument();
    expect(screen.getByText('Louange')).toBeInTheDocument();
  });

  it('affiche les membres rattachés avec leur nombre', async () => {
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());
    expect(screen.getByText(/Membres rattachés/)).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Moussa Diop/ }).length).toBeGreaterThan(0);
  });

  it('hiérarchiePartielle=true → message explicite au lieu d’une carte vide', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url.startsWith('/users/')) {
        return { data: detail({ hierarchiePartielle: true }) };
      }
      if (url.startsWith('/hierarchy/users/')) return { data: null };
      if (url.startsWith('/relations/users/')) return { data: null };
      return { data: [] };
    });
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());
    expect(screen.getByText(/Hiérarchie indisponible/)).toBeInTheDocument();
  });

  it('aucune branche organisationnelle → message explicite (pas de vide muet)', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url.startsWith('/hierarchy/users/')) {
        const h = detail().hierarchie;
        h.branches = [];
        return { data: h };
      }
      if (url.startsWith('/relations/users/')) return { data: detail().relations };
      if (url.startsWith('/users/')) return { data: detail() };
      return { data: [] };
    });
    renderModal();
    await waitFor(() => expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument());
    expect(
      screen.getByText(/Aucune branche d'organisation rattachée/)
    ).toBeInTheDocument();
  });

  it('échec de chargement → message d’erreur, jamais une fiche vide', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url.startsWith('/users/')) throw new Error('boom');
      return { data: [] };
    });
    renderModal();
    await waitFor(
      () => {
        expect(screen.getByText('Impossible de charger cette fiche.')).toBeInTheDocument();
      },
      { timeout: 8000 }
    );
    expect(screen.queryByText(/Awa Diallo/)).not.toBeInTheDocument();
  });
});

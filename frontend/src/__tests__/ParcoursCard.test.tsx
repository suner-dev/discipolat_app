import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ParcoursCard } from '@/components/relations/ParcoursCard';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const MOCK_USER = { id: 'u-self', email: 'moi@eglise.org', role: 'MEMBRE' };
const OTHER_ID = 'aaaaaaaa-0000-0000-0000-000000000009';

vi.mock('@/contexts/AuthContext', () => ({
  useAuthOptional: () => ({ user: MOCK_USER }),
}));

const PARCOURS = {
  userId: 'u-self',
  nomComplet: 'Awa Diallo',
  depuis: '2020-01-01T00:00:00Z',
  ancienneteJours: 1000,
  totalEvenements: 4,
  evenements: [
    { horodatage: '2020-01-01T00:00:00Z', type: 'ADHESION', action: 'DEBUT', libelle: 'Adhésion · Église' },
    { horodatage: '2021-06-01T00:00:00Z', type: 'CAPACITE', action: 'DEBUT', libelle: 'Capacité · Berger · Campus Nord' },
    { horodatage: '2023-01-01T00:00:00Z', type: 'CAPACITE', action: 'FIN', libelle: 'Fin de capacité · Berger' },
    { horodatage: '2024-02-01T00:00:00Z', type: 'RATTACHEMENT', action: 'FIN', libelle: 'Rattachement retiré · Mon pasteur · Jean Maka' },
  ],
  resume: { parType: { ADHESION: 1, CAPACITE: 3, RATTACHEMENT: 1 }, transferts: 1, rattachementsRompus: 1 },
};

function renderCard(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('ParcoursCard — le récit temporel d\'un membre', () => {
  beforeEach(() => vi.clearAllMocks());

  it('sans userId : lit son propre parcours et rend les événements traduits', async () => {
    apiGet.mockImplementation(async (url: string) => ({ data: PARCOURS }));
    renderCard(<ParcoursCard />);

    await waitFor(() => expect(screen.getByText('Mon parcours pastoral')).toBeInTheDocument());
    expect(apiGet).toHaveBeenCalledWith('/hierarchy/me/parcours');
    expect(screen.getByText('Adhésion')).toBeInTheDocument();
    expect(screen.getAllByText('Capacité').length).toBe(2);
    // Dégradation : le récit complet est porté par la page — pas de doublon affiché.
    expect(screen.queryByText('Adhésion · Église')).not.toBeInTheDocument();
    // Les signaux faibles du résumé, uniquement quand le serveur en a trouvé.
    expect(screen.getByText(/1 transfert\(s\)/)).toBeInTheDocument();
    expect(screen.getByText(/1 rattachement\(s\) rompu\(s\)/)).toBeInTheDocument();
  });

  it('avec un tiers : endpoint users/{id}/parcours (jamais /me)', async () => {
    apiGet.mockImplementation(async () => ({ data: { ...PARCOURS, userId: OTHER_ID } }));
    renderCard(<ParcoursCard userId={OTHER_ID} />);

    await waitFor(() => expect(screen.getByText('Mon parcours pastoral')).toBeInTheDocument());
    expect(apiGet).toHaveBeenCalledWith(`/hierarchy/users/${OTHER_ID}/parcours`);
  });

  it('aucun récit (ou backend sans la brique) : rien s\'affiche, aucune erreur trompeuse', async () => {
    apiGet.mockImplementation(async () => ({ data: { evenements: [] } }));
    const { container } = renderCard(<ParcoursCard />);
    await waitFor(() => expect(apiGet).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();

    apiGet.mockImplementation(async () => { throw new Error('404'); });
    const r2 = renderCard(<ParcoursCard userId={OTHER_ID} />);
    await waitFor(() => expect(apiGet).toHaveBeenCalled());
    expect(r2.container).toBeEmptyDOMElement();
  });

  it('un événement sans date ne casse pas le rendu (tri nulls-last côté serveur)', async () => {
    apiGet.mockImplementation(async () => ({
      data: {
        ...PARCOURS,
        depuis: null,
        evenements: [{ horodatage: null, type: 'TRANSFERT', action: 'FIN', libelle: 'Transfert' }],
        resume: { parType: {}, transferts: 0, rattachementsRompus: 0 },
      },
    }));
    renderCard(<ParcoursCard />);
    await waitFor(() => expect(screen.getByText('Transfert')).toBeInTheDocument());
    expect(screen.queryByText(/transfert\(s\)/)).not.toBeInTheDocument();
  });
});

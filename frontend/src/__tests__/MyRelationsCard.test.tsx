import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MyRelationsCard } from '@/components/relations/MyRelationsCard';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPost, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: vi.fn(),
    patch: vi.fn(),
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const PASTEUR = {
  id: 'rel-1',
  fromUserId: 'moi',
  otherUserId: 'user-pasteur-1',
  otherNom: 'Jean Maka',
  relationType: 'PASTEUR',
  typeLabel: 'Mon pasteur',
  statut: 'ACTIVE',
  revocable: true,
};

const MEMBRE_RATTACHE = {
  id: 'rel-2',
  fromUserId: 'user-membre-2',
  otherUserId: 'user-membre-2',
  otherNom: 'Awa Diallo',
  relationType: 'MENTOR',
  typeLabel: 'Mon mentor',
  statut: 'ACTIVE',
  revocable: false,
};

function page(content: unknown[], totalElements: number, number = 0, size = 25) {
  return {
    data: {
      content,
      totalElements,
      totalPages: Math.max(1, Math.ceil(totalElements / size)),
      number,
      size,
      last: number + 1 >= Math.ceil(totalElements / size),
    },
  };
}

function renderCard(onOpenUser?: (id: string) => void) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MyRelationsCard onOpenUser={onOpenUser} />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  apiGet.mockImplementation(async (url: string) => {
    if (url === '/relations/types') {
      return { data: [{ code: 'PASTEUR', label: 'Mon pasteur' }, { code: 'MENTOR', label: 'Mon mentor' }] };
    }
    if (url === '/relations/me') {
      return { data: { sortantes: [PASTEUR], entrantes: [MEMBRE_RATTACHE] } };
    }
    if (url === '/relations/me/members') {
      return page([MEMBRE_RATTACHE], 1);
    }
    return { data: [] };
  });
});

describe('MyRelationsCard — « Mon encadrement » (V231)', () => {
  it('affiche les encadrants déclarés avec le libellé du type', async () => {
    renderCard();
    await waitFor(() => {
      expect(screen.getByText('Jean Maka')).toBeInTheDocument();
    });
    expect(screen.getByText('Mon pasteur')).toBeInTheDocument();
  });

  it('propose les types du paramétrage de l’église', async () => {
    renderCard();
    await waitFor(() => expect(screen.getByText('Jean Maka')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Ajouter/i }));
    const select = screen.getByLabelText('Type de relation') as HTMLSelectElement;
    expect(within(select).getByText('Mon mentor')).toBeInTheDocument();
  });

  it('« mes membres » : le nom est cliquable et ouvre la fiche du membre', async () => {
    const onOpenUser = vi.fn();
    renderCard(onOpenUser);

    const memberButton = await screen.findByRole('button', { name: /Awa Diallo/ });
    fireEvent.click(memberButton);

    expect(onOpenUser).toHaveBeenCalledWith('user-membre-2');
  });

  it('« mes membres » : le bouton « Détacher » n’apparaît que si le serveur autorise', async () => {
    renderCard(vi.fn());
    await screen.findByRole('button', { name: /Awa Diallo/ });

    // MEMBRE_RATTACHE a revocable=false → aucun bouton Détacher sur sa ligne.
    expect(screen.queryByRole('button', { name: 'Détacher' })).not.toBeInTheDocument();
    // …mais bien un « Retirer » sur la ligne sortante (revocable=true).
    expect(screen.getAllByRole('button', { name: 'Retirer' })).toHaveLength(1);
  });

  it('« mes membres » : un encadrant / modérateur peut détacher (revocable=true côté serveur)', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/relations/types') return { data: [{ code: 'PASTEUR', label: 'Mon pasteur' }] };
      if (url === '/relations/me') return { data: { sortantes: [PASTEUR], entrantes: [] } };
      if (url === '/relations/me/members') return page([{ ...MEMBRE_RATTACHE, revocable: true }], 1);
      return { data: [] };
    });
    renderCard(vi.fn());
    await screen.findByRole('button', { name: 'Détacher' });

    fireEvent.click(screen.getByRole('button', { name: 'Détacher' }));
    await waitFor(() => expect(apiDelete).toHaveBeenCalledWith('/relations/me/rel-2'));
  });

  it('« mes membres » : compte total et pagination quand le plafond est dépassé', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/relations/types') return { data: [{ code: 'PASTEUR', label: 'Mon pasteur' }] };
      if (url === '/relations/me') return { data: { sortantes: [PASTEUR], entrantes: [] } };
      if (url === '/relations/me/members') return page([MEMBRE_RATTACHE], 130, 0, 25);
      return { data: [] };
    });
    renderCard();

    await waitFor(() => expect(screen.getByText('130')).toBeInTheDocument());
    expect(screen.getByText(/Page 1 \/ 6/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Suivant/ }));
    await waitFor(() => {
      expect(apiGet).toHaveBeenCalledWith('/relations/me/members', {
        params: { page: 1, size: 25 },
      });
    });
  });

  it('retirer un encadrant appelle DELETE sur la bonne relation', async () => {
    renderCard();
    await screen.findByRole('button', { name: 'Retirer' });
    fireEvent.click(screen.getByRole('button', { name: 'Retirer' }));
    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/relations/me/rel-1');
    });
  });

  it('panne du serveur : message d’erreur + bouton Réessayer, jamais de vide silencieux', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/relations/me') throw new Error('network down');
      return { data: [] };
    });
    renderCard();

    // Le composant réessaie une fois (retry: 1) avant d'afficher l'erreur :
    // le délai de back-off (~1 s) dépasse le timeout par défaut de waitFor.
    await waitFor(
      () => {
        expect(screen.getByText(/Impossible de charger votre encadrement/)).toBeInTheDocument();
      },
      { timeout: 8000 }
    );
    expect(screen.getByRole('button', { name: /Réessayer/ })).toBeInTheDocument();
    expect(screen.queryByText('Mes encadrants')).not.toBeInTheDocument();
  });

  it('sans `onOpenUser`, le nom reste du texte simple (pas de bouton mort)', async () => {
    renderCard();
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    // Aucun bouton n'est proposé : on ne fabrique pas un contrôle inerte.
    expect(screen.queryByRole('button', { name: /Awa Diallo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Jean Maka/ })).not.toBeInTheDocument();
  });
});

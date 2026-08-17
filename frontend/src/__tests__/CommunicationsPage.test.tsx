import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import toast from 'react-hot-toast';
import CommunicationsPage from '@/pages/CommunicationsPage';
import { PlatformContext } from '@/contexts/PlatformContext';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

// Rôle actif mutable : permet de tester la vue gestion (ADMIN) et la vue lecture (MEMBRE).
// vi.hoisted : le factory de vi.mock est hoisté au-dessus des déclarations `let`.
const { activeRole } = vi.hoisted(() => ({ activeRole: { value: 'ADMIN' } }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    activeRole: activeRole.value,
    user: { id: 'u1', role: activeRole.value, roles: [activeRole.value] },
  }),
}));

const { apiGet, apiPost, apiPut, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage(disabledModules: string[] = []) {
  const moduleEnabled = (key: string) => !disabledModules.includes(key);
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformContext.Provider
        value={{
          menus: [], modules: [], isLoaded: true, moduleEnabled,
          canAccessPath: () => true, refetch: () => undefined,
        }}
      >
        <MemoryRouter initialEntries={['/communications']}>
          <CommunicationsPage />
        </MemoryRouter>
      </PlatformContext.Provider>
    </QueryClientProvider>
  );
}

const ANNONCE_TOUS = {
  id: 'comm-1', titre: 'Rentrée de septembre', contenu: 'La rentrée aura lieu le 7 septembre.',
  cible: 'TOUS', roles: [], statut: 'PUBLIEE', datePublication: '2026-08-17T10:00:00',
};
const ANNONCE_BROUILLON = {
  id: 'comm-2', titre: 'Conseil des responsables', contenu: 'Réunion du conseil jeudi.',
  cible: 'ROLE', roles: ['RESPONSABLE'], statut: 'BROUILLON',
};

describe('CommunicationsPage — outil métier Communication', () => {
  beforeEach(() => {
    activeRole.value = 'ADMIN';
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/communications') return { data: [ANNONCE_TOUS] };
      if (url === '/communications/admin') return { data: [ANNONCE_TOUS, ANNONCE_BROUILLON] };
      if (url.startsWith('/families')) return { data: [{ id: 'fam-1', nom: 'Famille Timothée' }] };
      if (url.startsWith('/departments')) return { data: [{ id: 'dept-1', nom: 'Jeunesse' }] };
      return { data: [] };
    });
    apiPost.mockResolvedValue({ data: { ...ANNONCE_BROUILLON, destinataires: 1 } });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche la gestion (ADMIN) et les annonces publiées pour l’utilisateur', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Gestion des annonces')).toBeInTheDocument();
    });
    // Brouillon dans la gestion (données chargées de façon asynchrone).
    await waitFor(() => {
      expect(screen.getByText('Conseil des responsables')).toBeInTheDocument();
    });
    expect(screen.getAllByText('Brouillon').length).toBeGreaterThan(0);
    // Annonce publiée visible pour l'utilisateur courant (gestion + fil publié).
    expect(screen.getAllByText('Rentrée de septembre').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/La rentrée aura lieu le 7 septembre/).length).toBeGreaterThan(0);
  });

  it('crée une annonce ciblée via la modale (POST /communications/admin)', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Gestion des annonces')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouvelle annonce/i }));
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Nouvelle annonce' })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('Titre'), { target: { value: 'Invitation culte' } });
    fireEvent.change(screen.getByLabelText('Contenu'), { target: { value: 'Culte spécial dimanche.' } });
    fireEvent.change(screen.getByLabelText('Cible de diffusion'), { target: { value: 'ROLE' } });
    // Sélection du rôle destinataire (badge cliquable).
    fireEvent.click(screen.getByRole('button', { name: 'MEMBRE' }));
    fireEvent.click(screen.getByRole('button', { name: /créer/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/communications/admin', expect.objectContaining({
        titre: 'Invitation culte',
        contenu: 'Culte spécial dimanche.',
        cible: 'ROLE',
        roles: ['MEMBRE'],
      }));
    });
    expect(toast.success).toHaveBeenCalledWith('Annonce créée');
  });

  it('publie une annonce et confirme la diffusion (POST …/publish)', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Conseil des responsables')).toBeInTheDocument();
    });

    // jsdom : window.confirm renvoie false par défaut → stub à true pour déclencher la mutation.
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fireEvent.click(screen.getByTitle('Publier et diffuser'));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/communications/admin/comm-2/publish');
    });
    expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('1 destinataire'));
  });

  it('affiche uniquement les annonces publiées pour un rôle en lecture seule', async () => {
    activeRole.value = 'MEMBRE';
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Rentrée de septembre')).toBeInTheDocument();
    });
    // Pas de gestion pour un MEMBRE.
    expect(screen.queryByText('Gestion des annonces')).not.toBeInTheDocument();
    expect(apiGet).not.toHaveBeenCalledWith('/communications/admin');
  });

  it('affiche l’état explicite quand le module est désactivé', async () => {
    renderPage(['COMMUNICATION']);

    await waitFor(() => {
      expect(screen.getByText('Communication désactivée')).toBeInTheDocument();
    });
    expect(apiGet).not.toHaveBeenCalled();
  });
});

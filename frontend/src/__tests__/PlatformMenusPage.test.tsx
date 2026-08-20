import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import PlatformMenusPage from '@/pages/PlatformMenusPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPut, apiPost, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  apiPost: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: apiPost,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const MENUS = [
  { id: 'menu-1', key: 'dashboard', label: 'Tableau de bord', href: '/dashboard', icon: 'LayoutDashboard', section: 'Pilotage', ordre: 0, roles: ['ADMIN', 'PASTEUR'], moduleKey: null, enabled: true },
  { id: 'menu-2', key: 'souls', label: 'Âmes', href: '/souls', icon: 'Heart', section: 'Pilotage', ordre: 1, roles: ['ADMIN', 'PASTEUR', 'FAISEUR'], moduleKey: null, enabled: true },
  { id: 'menu-3', key: 'transfers', label: 'Transferts', href: '/transfers', icon: 'Move', section: 'Vie de l\'église', ordre: 0, roles: ['ADMIN', 'PASTEUR'], moduleKey: 'TRANSFERTS', enabled: false },
];

const MODULES = [
  { key: 'TRANSFERTS', label: 'Transferts', description: '', icon: 'Move', section: 'Vie de l\'église', ordre: 1, enabled: true },
];

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformMenusPage />
    </QueryClientProvider>
  );
}

function changeInputByLabel(labelText: string, value: string) {
  const label = screen.getByText(labelText);
  const container = label.closest('div');
  const input = container?.querySelector('input, select, textarea') as HTMLInputElement | null;
  if (!input) throw new Error(`Input introuvable pour "${labelText}"`);
  fireEvent.change(input, { target: { value } });
}

describe('PlatformMenusPage — administration des menus', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/platform/admin/menus') return { data: MENUS };
      if (url === '/platform/modules') return { data: MODULES };
      return { data: [] };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche les menus groupés par section avec leurs rôles', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Menus')).toBeInTheDocument();
    });
    expect(screen.getByText('Pilotage')).toBeInTheDocument();
    expect(screen.getByText('Vie de l\'église')).toBeInTheDocument();
    // Libellés présents deux fois (aperçu en direct + sections) — vérifier la présence globale.
    expect(screen.getAllByText('Tableau de bord').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Âmes').length).toBeGreaterThan(0);
    // Badge de rôle visible sur une entrée.
    expect(screen.getByText('FAISEUR')).toBeInTheDocument();
    // Badge module visible sur le menu Transferts.
    expect(screen.getByText('TRANSFERTS')).toBeInTheDocument();
  });

  it('crée un nouveau menu via le formulaire modal', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Menus')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau menu/i }));
    // Le libellé « URL » n'existe que dans le modal ouvert.
    await waitFor(() => {
      expect(screen.getByText('URL')).toBeInTheDocument();
    });

    changeInputByLabel('Libellé', 'Calendrier');
    changeInputByLabel('URL', '/calendar');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/platform/menus', expect.objectContaining({
        label: 'Calendrier',
        href: '/calendar',
      }));
    });
  });

  it('active/désactive un menu via le toggle', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('Tableau de bord').length).toBeGreaterThan(0);
    });

    const toggle = screen.getByRole('switch', { name: /activer tableau de bord/i });
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/platform/menus/menu-1', expect.objectContaining({
        id: 'menu-1',
        enabled: false,
      }));
    });
  });

  it('réordonne les menus de la même section via POST /reorder', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('Âmes').length).toBeGreaterThan(0);
    });

    // Bouton « Monter » de la 2e entrée (Âmes) → échange avec Tableau de bord.
    const upButtons = screen.getAllByRole('button', { name: /monter/i });
    expect(upButtons.length).toBeGreaterThanOrEqual(2);
    fireEvent.click(upButtons[1]);

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/platform/menus/reorder', expect.arrayContaining([
        expect.objectContaining({ id: 'menu-1', ordre: 1 }),
        expect.objectContaining({ id: 'menu-2', ordre: 0 }),
      ]));
    });
  });

  it('supprime un menu avec confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Transferts')).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole('button', { name: /supprimer transferts/i });
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/platform/menus/menu-3');
    });
    vi.restoreAllMocks();
  });

  it('affiche une erreur quand la création du menu échoue', async () => {
    apiPost.mockRejectedValue(new Error('boom'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Menus')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau menu/i }));
    await waitFor(() => {
      expect(screen.getByText('URL')).toBeInTheDocument();
    });

    changeInputByLabel('Libellé', 'Calendrier');
    changeInputByLabel('URL', '/calendar');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
  });
});
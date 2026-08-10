import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import PlatformModulesPage from '@/pages/PlatformModulesPage';

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

const MODULES = [
  { key: 'DISCIPOLAT', label: 'Discipolat', description: 'Gestion du discipolat', icon: 'Heart', section: 'Pilotage', ordre: 1, enabled: true },
  { key: 'TRANSFERTS', label: 'Transferts', description: 'Transferts', icon: 'Move', section: 'Vie de l\'église', ordre: 2, enabled: false },
  { key: 'PRIERES', label: 'Prières', description: 'Sujets de prière', icon: 'Heart', section: 'Pilotage', ordre: 3, enabled: true },
];

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformModulesPage />
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

describe('PlatformModulesPage — administration des modules', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/platform/modules') return { data: MODULES };
      return { data: [] };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche les modules groupés par section avec leurs badges clés', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Modules')).toBeInTheDocument();
    });
    expect(screen.getByText('Pilotage')).toBeInTheDocument();
    expect(screen.getByText('Vie de l\'église')).toBeInTheDocument();
    // Libellé et badge clé unique (monospace) de chaque module.
    expect(screen.getByText('Discipolat')).toBeInTheDocument();
    expect(screen.getByText('DISCIPOLAT')).toBeInTheDocument();
    expect(screen.getByText('TRANSFERTS')).toBeInTheDocument();
    // Le module désactivé (Transferts) a son toggle éteint.
    expect(screen.getByRole('switch', { name: 'Activer Transferts' }))
      .toHaveAttribute('aria-checked', 'false');
  });

  it('affiche une erreur quand le toggle du module échoue', async () => {
    apiPut.mockRejectedValue(new Error('boom'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Discipolat')).toBeInTheDocument();
    });

    const toggles = screen.getAllByRole('switch', { name: /activer/i });
    fireEvent.click(toggles[0]);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
  });

  it('active/désactive un module via le toggle', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Discipolat')).toBeInTheDocument();
    });

    // Premier module de la liste (Discipolat, activé) → son toggle envoie enabled: false.
    const toggles = screen.getAllByRole('switch', { name: /activer/i });
    fireEvent.click(toggles[0]);

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/platform/modules/DISCIPOLAT', { enabled: false });
    });
  });

  it('crée un nouveau module via le formulaire modal', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Modules')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau module/i }));
    // Le libellé « Clé (unique) » n'existe que dans le modal ouvert.
    await waitFor(() => {
      expect(screen.getByText('Clé (unique)')).toBeInTheDocument();
    });

    changeInputByLabel('Clé (unique)', 'FORMATION');
    changeInputByLabel('Libellé', 'Formations');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/platform/modules', expect.objectContaining({
        key: 'FORMATION',
        label: 'Formations',
      }));
    });
  });

  it('supprime un module avec confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Discipolat')).toBeInTheDocument();
    });

    const deleteBtn = screen.getByRole('button', { name: /supprimer discipolat/i });
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/platform/modules/DISCIPOLAT');
    });
    vi.restoreAllMocks();
  });
});
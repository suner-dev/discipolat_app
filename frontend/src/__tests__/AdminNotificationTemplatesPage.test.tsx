import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import AdminNotificationTemplatesPage from '@/pages/AdminNotificationTemplatesPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPut, apiPost, apiPatch, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  apiPost: vi.fn(),
  apiPatch: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: apiPost,
    patch: apiPatch,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const CATALOG = [
  { event: 'INFORMATION', label: 'Information générale', defaultTitre: 'ℹ️ Information', defaultMessage: 'Information émise par la plateforme.', canauxSuggestes: ['IN_APP'], variables: ['{{type}}', '{{entiteType}}'] },
  { event: 'ALERTE_ABSENCE', label: "Alerte d'absence", defaultTitre: '⚠️ Absence constatée', defaultMessage: 'Un membre est sans contact.', canauxSuggestes: ['IN_APP', 'PUSH'], variables: ['{{type}}'] },
  { event: 'TRANSFERT_VALIDEE', label: 'Transfert — validé', defaultTitre: '🎉 Transfert validé', defaultMessage: 'Votre demande de transfert a été validée.', canauxSuggestes: ['IN_APP'], variables: ['{{type}}'] },
];

const TEMPLATES = [
  {
    id: 'tpl-1', event: 'INFORMATION', titre: 'ℹ️ Info personnalisée', message: 'Message personnalisé.',
    canaux: ['IN_APP', 'PUSH'], rolesDestinataires: ['PASTEUR', 'RESPONSABLE'], actif: true, updatedAt: '2026-08-18T10:00:00',
  },
  {
    id: 'tpl-2', event: 'ALERTE_ABSENCE', titre: '⚠️ Absence', message: 'Un membre est sans contact.',
    canaux: ['IN_APP'], rolesDestinataires: [], actif: false, updatedAt: '2026-08-18T09:00:00',
  },
];

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminNotificationTemplatesPage />
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
describe('AdminNotificationTemplatesPage — centre de configuration des notifications', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/admin/notifications/events') return { data: CATALOG };
      if (url === '/admin/notifications/templates') return { data: TEMPLATES };
      return { data: [] };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: {} });
    apiPatch.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche les modèles configurés avec libellé, titre, canaux et statut', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Info personnalisée/)).toBeInTheDocument();
    });
    // Libellé d'événement résolu depuis le catalogue + badge clé.
    expect(screen.getByText('Information générale')).toBeInTheDocument();
    expect(screen.getByText('INFORMATION')).toBeInTheDocument();
    // Canaux + rôles.
    expect(screen.getAllByText('IN_APP').length).toBeGreaterThan(0);
    expect(screen.getByText('PASTEUR, RESPONSABLE')).toBeInTheDocument();
    // Statut actif/inactif.
    expect(screen.getByText('Actif')).toBeInTheDocument();
    expect(screen.getByText('Inactif')).toBeInTheDocument();
  });

  it('affiche l’état vide avec CTA quand aucun modèle n’est configuré', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/admin/notifications/events') return { data: CATALOG };
      if (url === '/admin/notifications/templates') return { data: [] };
      return { data: [] };
    });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Aucun modèle configuré')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /créer un modèle/i })).toBeInTheDocument();
  });

  it('crée un modèle en pré-remplissant les textes suggérés du catalogue', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/admin/notifications/events') return { data: CATALOG };
      if (url === '/admin/notifications/templates') return { data: [] };
      return { data: [] };
    });
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /nouveau modèle/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /nouveau modèle/i }));

    // Sélection d'un événement → pré-remplissage du titre/message suggérés.
    const eventSelect = screen.getByLabelText('Événement') as HTMLSelectElement;
    fireEvent.change(eventSelect, { target: { value: 'TRANSFERT_VALIDEE' } });

    await waitFor(() => {
      expect(screen.getByDisplayValue(/Transfert validé/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/admin/notifications/templates', expect.objectContaining({
        event: 'TRANSFERT_VALIDEE',
        titre: '🎉 Transfert validé',
        canaux: ['IN_APP'],
        actif: true,
      }));
    });
  });

  it('active/désactive un modèle via le toggle (PATCH)', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Info personnalisée/)).toBeInTheDocument();
    });

    // Deuxième modèle (ALERTE_ABSENCE, inactif) → toggle → actif: true.
    const toggles = screen.getAllByRole('switch');
    fireEvent.click(toggles[1]);

    await waitFor(() => {
      expect(apiPatch).toHaveBeenCalledWith('/admin/notifications/templates/tpl-2/toggle', { actif: true });
    });
  });

  it('modifie un modèle existant (PUT)', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Info personnalisée/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /modifier information générale/i }));
    await waitFor(() => {
      expect(screen.getByText('Modifier le modèle')).toBeInTheDocument();
    });

    changeInputByLabel('Titre (avec variables {{...}})', 'ℹ️ Info retravaillée');
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/admin/notifications/templates/tpl-1', expect.objectContaining({
        event: 'INFORMATION',
        titre: 'ℹ️ Info retravaillée',
      }));
    });
  });

  it('supprime un modèle avec confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Info personnalisée/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /supprimer information générale/i }));

    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/admin/notifications/templates/tpl-1');
    });
    vi.restoreAllMocks();
  });

  it('masque les événements déjà configurés dans le modal de création', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/Info personnalisée/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau modèle/i }));

    const eventSelect = screen.getByLabelText('Événement') as HTMLSelectElement;
    const options = Array.from(eventSelect.options).map((o) => o.value);
    // INFORMATION et ALERTE_ABSENCE ont déjà un modèle → masqués.
    expect(options).not.toContain('INFORMATION');
    expect(options).not.toContain('ALERTE_ABSENCE');
    expect(options).toContain('TRANSFERT_VALIDEE');
  });
});


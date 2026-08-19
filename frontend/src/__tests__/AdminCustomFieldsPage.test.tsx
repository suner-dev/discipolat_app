import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import AdminCustomFieldsPage from '@/pages/AdminCustomFieldsPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
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
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const DEFS = [
  {
    id: 'cf-1', entiteType: 'SOUL', code: 'LANGUE', label: 'Langue parlée',
    type: 'TEXTE', obligatoire: true, ordre: 1, options: null,
    placeholder: 'Ex : Français', defaultValue: '', rolesLecture: [], rolesEcriture: [], actif: true,
  },
  {
    id: 'cf-2', entiteType: 'SOUL', code: 'TALENT', label: 'Talent',
    type: 'SELECTION', obligatoire: false, ordre: 2, options: ['Musique', 'Enseignement'],
    placeholder: '', defaultValue: '', rolesLecture: [], rolesEcriture: [], actif: true,
  },
  {
    id: 'cf-4', entiteType: 'SOUL', code: 'NOTE_HISTO', label: 'Note historique',
    type: 'TEXTE', obligatoire: false, ordre: 3, options: null,
    placeholder: '', defaultValue: '', rolesLecture: [], rolesEcriture: [], actif: false,
  },
];

const USER_DEFS = [
  {
    id: 'cf-3', entiteType: 'USER', code: 'TELEPHONE', label: 'Téléphone secondaire',
    type: 'TELEPHONE', obligatoire: false, ordre: 1, options: null,
    placeholder: '', defaultValue: '', rolesLecture: [], rolesEcriture: [], actif: true,
  },
];

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminCustomFieldsPage />
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

describe('AdminCustomFieldsPage — administration des champs personnalisés', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url.includes('/custom-fields/definitions/all')) {
        const et = new URL(url, 'http://test').searchParams.get('entiteType');
        if (et === 'USER') return { data: USER_DEFS };
        return { data: DEFS };
      }
      return { data: [] };
    });
    apiPost.mockResolvedValue({ data: {} });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche les onglets et les définitions de l\'entité active', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Champs personnalisés')).toBeInTheDocument();
    });
    expect(screen.getByText('Âmes')).toBeInTheDocument();
    expect(screen.getByText('Utilisateurs')).toBeInTheDocument();
    expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    expect(screen.getByText('Talent')).toBeInTheDocument();
    expect(screen.getByText('SELECTION')).toBeInTheDocument();
    expect(screen.getByText('* Obligatoire')).toBeInTheDocument();
  });

  it('change d\'onglet et recharge les définitions pour l\'entité sélectionnée', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Utilisateurs'));
    await waitFor(() => {
      expect(screen.getByText('Téléphone secondaire')).toBeInTheDocument();
    });
    expect(screen.queryByText('Langue parlée')).not.toBeInTheDocument();
  });

  it('crée un nouveau champ via le formulaire modal', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Champs personnalisés')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau champ/i }));
    await waitFor(() => {
      expect(screen.getByText('Code')).toBeInTheDocument();
    });

    changeInputByLabel('Code', 'OBSERVATIONS');
    changeInputByLabel('Libellé', 'Observations');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/custom-fields/definitions', expect.objectContaining({
        code: 'OBSERVATIONS',
        label: 'Observations',
        entiteType: 'SOUL',
      }));
    });
  });

  it('modifie un champ existant via le formulaire modal', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    const row = screen.getByText('Langue parlée').closest('.glass-card')!;
    const editBtn = row.querySelector('button[title="Modifier"]');
    expect(editBtn).toBeTruthy();
    fireEvent.click(editBtn!);

    await waitFor(() => {
      expect(screen.getByText('Modifier le champ')).toBeInTheDocument();
    });

    changeInputByLabel('Libellé', 'Langue maternelle');
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/custom-fields/definitions/cf-1', expect.objectContaining({
        code: 'LANGUE',
        label: 'Langue maternelle',
      }));
    });
  });

  it('supprime un champ avec confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    const row = screen.getByText('Langue parlée').closest('.glass-card')!;
    const deleteBtn = row.querySelector('button[title="Supprimer"]')!;
    expect(deleteBtn).toBeTruthy();
    fireEvent.click(deleteBtn);

    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/custom-fields/definitions/cf-1');
    });
    vi.restoreAllMocks();
  });

  it('affiche une erreur quand la création du champ échoue', async () => {
    apiPost.mockRejectedValue(new Error('boom'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Champs personnalisés')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouveau champ/i }));
    await waitFor(() => {
      expect(screen.getByText('Code')).toBeInTheDocument();
    });

    changeInputByLabel('Code', 'OBSERVATIONS');
    changeInputByLabel('Libellé', 'Observations');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
  });

  it('affiche l\'aperçu d\'un champ', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    // Click preview button on the first field
    const row = screen.getByText('Langue parlée').closest('.glass-card')!;
    const previewBtn = row.querySelector('button[title="Aperçu"]');
    expect(previewBtn).toBeTruthy();
    fireEvent.click(previewBtn!);

    await waitFor(() => {
      expect(screen.getByText(/Aperçu — Langue parlée/)).toBeInTheDocument();
    });
  });

  it('recherche un champ par nom', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/Rechercher un champ/);
    fireEvent.change(searchInput, { target: { value: 'Talent' } });

    await waitFor(() => {
      expect(screen.getByText('Talent')).toBeInTheDocument();
    });
    expect(screen.queryByText('Langue parlée')).not.toBeInTheDocument();
  });

  it('affiche les champs inactifs quand le filtre est activé', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Langue parlée')).toBeInTheDocument();
    });

    // By default, inactive fields should be hidden
    expect(screen.queryByText('Note historique')).not.toBeInTheDocument();

    // Click "Inclus inactifs" button
    fireEvent.click(screen.getByText('Inclus inactifs'));

    await waitFor(() => {
      expect(screen.getByText('Note historique')).toBeInTheDocument();
    });
  });

  it('affiche le bon compteur par entité', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/3 champ/)).toBeInTheDocument();
    });
  });
});

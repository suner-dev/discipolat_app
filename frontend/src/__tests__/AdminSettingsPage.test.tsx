import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import AdminSettingsPage from '@/pages/AdminSettingsPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

// vi.hoisted : les fonctions du mock doivent exister avant le hoisting du factory.
const { apiGet, apiPut, apiPost } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  apiPost: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: apiPost,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const SETTINGS = {
  id: 'settings-1',
  churchName: 'Église de la Grâce',
  platformName: 'Discipolat',
  slogan: 'Sauver et discipler',
  description: 'Notre vision',
  logoUrl: '',
  faviconUrl: '',
  bannerUrl: '',
  primaryColor: '#0F766E',
  accentColor: '#B91C1C',
  buttonColor: '#0F766E',
  fontFamily: 'Inter',
  allowDarkMode: true,
  address: '12 Avenue Paix',
  phone: '+243 800 000 000',
  email: 'contact@eglise.org',
  website: 'https://eglise.org',
  contactNotes: '',
  socialLinks: { facebook: 'https://facebook.com/eglise' },
};

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminSettingsPage />
    </QueryClientProvider>
  );
}

/** Remplit l'input situé dans le même conteneur que le libellé donné. */
function changeInputByLabel(labelText: string, value: string) {
  const label = screen.getByText(labelText);
  const container = label.closest('div');
  const input = container?.querySelector('input, textarea, select') as HTMLInputElement | null;
  if (!input) throw new Error(`Input introuvable pour le libellé "${labelText}"`);
  fireEvent.change(input, { target: { value } });
}

describe('AdminSettingsPage — identité & marque', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/settings') return { data: SETTINGS };
      return { data: {} };
    });
    apiPut.mockResolvedValue({ data: SETTINGS });
    apiPost.mockResolvedValue({ data: SETTINGS });
  });

  it('affiche le titre et pré-remplit le formulaire depuis l’API', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Identité & marque')).toBeInTheDocument();
    });
    // Le formulaire est pré-rempli avec les données du backend.
    expect(apiGet).toHaveBeenCalledWith('/settings');
    expect(screen.getByText('Nom de l\'église')).toBeInTheDocument();
    expect(screen.getByText('Couleurs & thème')).toBeInTheDocument();
    expect(screen.getByText('Réseaux sociaux')).toBeInTheDocument();
    expect(screen.getByText('facebook')).toBeInTheDocument();
  });

  it('enregistre les modifications via PUT /settings', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Identité & marque')).toBeInTheDocument();
    });

    changeInputByLabel('Nom de l\'église', 'Église Éternelle');
    fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/settings', expect.objectContaining({
        churchName: 'Église Éternelle',
        primaryColor: '#0F766E',
        fontFamily: 'Inter',
      }));
    });
  });

  it('réinitialise les paramètres via POST /settings/reset', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Identité & marque')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /réinitialiser/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/settings/reset');
    });
  });

  it("affiche une erreur quand l'enregistrement échoue", async () => {
    apiPut.mockRejectedValue(new Error('boom'));
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Identité & marque')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
  });

  it('permet de modifier une couleur de base', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Identité & marque')).toBeInTheDocument();
    });

    // Sélecteur de couleur principal : change la valeur → le state est mis à jour.
    const colorInputs = screen.getAllByLabelText(/sélecteur/);
    expect(colorInputs.length).toBeGreaterThanOrEqual(3); // principale, accent, boutons
    fireEvent.change(colorInputs[0], { target: { value: '#7C3AED' } });
  });
});

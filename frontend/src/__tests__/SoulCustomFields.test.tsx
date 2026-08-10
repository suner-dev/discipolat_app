import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import SoulCreatePage from '@/pages/SoulCreatePage';

// vi.hoisted : les fonctions du mock doivent exister avant le hoisting du factory.
const { apiGet, apiPost, apiPut } = vi.hoisted(() => ({
  apiGet: vi.fn((url: string) => {
    if (url.includes('/custom-fields/definitions')) {
      return Promise.resolve({
        data: [
          {
            id: 'cf-1',
            code: 'LANGUE',
            label: 'Langue',
            type: 'TEXTE',
            obligatoire: true,
            ordre: 1,
            defaultValue: '',
            options: null,
            placeholder: 'Ex : Français',
            rolesLecture: [],
            rolesEcriture: [],
            actif: true,
          },
          {
            id: 'cf-2',
            code: 'TALENT',
            label: 'Talent',
            type: 'SELECTION',
            obligatoire: false,
            ordre: 2,
            defaultValue: '',
            options: ['Musique', 'Enseignement', 'Service'],
            rolesLecture: [],
            rolesEcriture: [],
            actif: true,
          },
        ],
      });
    }
    if (url.includes('/users')) {
      return Promise.resolve({
        data: {
          content: [
            {
              id: 'faiseur-test',
              firstName: 'Fabrice',
              lastName: 'Faiseur',
              email: 'fabrice@test.com',
              role: 'FAISEUR',
              roles: ['FAISEUR'],
              activeRole: 'FAISEUR',
              estChefDeFamille: false,
              statut: 'ACTIVE',
              createdAt: '',
              updatedAt: '',
            },
          ],
        },
      });
    }
    return Promise.resolve({ data: { content: [] } });
  }),
  apiPost: vi.fn().mockResolvedValue({ data: { id: 'soul-1' } }),
  apiPut: vi.fn().mockResolvedValue({ data: {} }),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

/** Remplit l'input situé dans le même conteneur que le libellé donné. */
function changeInputByLabel(labelText: string, value: string) {
  const label = screen.getByText(labelText);
  const container = label.closest('div');
  const input = container?.querySelector('input');
  if (!input) throw new Error(`Input introuvable pour le libellé "${labelText}"`);
  fireEvent.change(input, { target: { value } });
}

function renderWithProviders() {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <SoulCreatePage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('SoulCreatePage — champs personnalisés', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('affiche la section des champs personnalisés depuis la configuration', async () => {
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText('Informations complémentaires')).toBeInTheDocument();
    });
    expect(screen.getByText('Langue')).toBeInTheDocument();
    expect(screen.getByText('Talent')).toBeInTheDocument();
    expect(screen.getByText('Musique')).toBeInTheDocument();
  });

  it('bloque la soumission si un champ personnalisé obligatoire est vide', async () => {
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText('Informations complémentaires')).toBeInTheDocument();
    });

    // Remplir les champs standards pour passer la validation zod.
    changeInputByLabel('Nom *', 'Kabila');
    // Sélectionner un faiseur (4e combobox de la page).
    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[3], { target: { value: 'faiseur-test' } });

    fireEvent.click(screen.getByRole('button', { name: /créer l'âme/i }));

    await waitFor(() => {
      expect(screen.getByText(/Champs obligatoires à renseigner : Langue/)).toBeInTheDocument();
    });
    expect(apiPost).not.toHaveBeenCalled();
  });

  it('sauvegarde les valeurs personnalisées après la création de la fiche', async () => {
    renderWithProviders();

    await waitFor(() => {
      expect(screen.getByText('Informations complémentaires')).toBeInTheDocument();
    });

    // Champs standards.
    changeInputByLabel('Nom *', 'Kabila');
    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[3], { target: { value: 'faiseur-test' } });

    // Champ personnalisé requis.
    fireEvent.change(screen.getByPlaceholderText('Ex : Français'), { target: { value: 'Français' } });

    fireEvent.click(screen.getByRole('button', { name: /créer l'âme/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/custom-fields/SOUL/soul-1', { 'cf-1': 'Français' });
    });
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProfilePage from '@/pages/ProfilePage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPost } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const mockUser = {
  id: 'user-rgpd-1',
  firstName: 'Marie',
  lastName: 'Dupont',
  email: 'marie@eglise.com',
  phone: '',
  dateNaissance: '',
  situationFamiliale: '',
  role: 'MEMBRE',
  roles: ['MEMBRE'],
  activeRole: 'MEMBRE',
  platformRoles: [],
  platformSuperAdmin: false,
  estChefDeFamille: false,
  statut: 'ACTIVE',
  twoFactorEnabled: false,
  createdAt: '2026-01-10T08:00:00',
  updatedAt: '2026-01-10T08:00:00',
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: mockUser, updateUser: vi.fn() }),
}));

vi.mock('@/hooks/useDictionaries', () => ({
  useDictionaries: () => ({
    label: (_: string, code: string) => code,
    options: () => [],
    color: () => null,
  }),
}));

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <ProfilePage />
    </QueryClientProvider>
  );
}

beforeEach(() => {
  queryClient.clear();
  vi.clearAllMocks();
  // jsdom n'implémente pas createObjectURL — stub minimal pour le téléchargement
  (URL as { createObjectURL: unknown }).createObjectURL = vi.fn(() => 'blob:mock');
  (URL as { revokeObjectURL: unknown }).revokeObjectURL = vi.fn();
  apiGet.mockResolvedValue({ data: { userId: mockUser.id, consents: [] } });
  apiPost.mockResolvedValue({ data: { id: 'gdpr-1', statut: 'PENDING' } });
});

describe('ProfilePage — self-service RGPD', () => {
  it('affiche la section « Mes données (RGPD) »', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Mes données (RGPD)')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Exporter mes données \(art\. 20\)/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Demander la suppression \(art\. 17\)/ })).toBeInTheDocument();
  });

  it('export de portabilité : GET /compliance/portability/<id-courant>', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Mes données (RGPD)')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /Exporter mes données/ }));
    await waitFor(() => {
      expect(apiGet).toHaveBeenCalledWith('/compliance/portability/user-rgpd-1');
    });
  });

  it('demande de suppression : POST /compliance/gdpr avec typeDemande=SUPPRESSION', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Mes données (RGPD)')).toBeInTheDocument();
    });
    const motifInput = screen.getByPlaceholderText(/je quitte cette communauté/i);
    fireEvent.change(motifInput, { target: { value: 'Départ de la ville' } });
    fireEvent.click(screen.getByRole('button', { name: /Demander la suppression/ }));
    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/compliance/gdpr', {
        typeDemande: 'SUPPRESSION',
        motif: 'Départ de la ville',
      });
    });
  });

  it('demande de suppression sans motif : motif par défaut non vide', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Mes données (RGPD)')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /Demander la suppression/ }));
    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/compliance/gdpr',
        expect.objectContaining({ typeDemande: 'SUPPRESSION', motif: 'Demande self-service' }));
    });
  });
});

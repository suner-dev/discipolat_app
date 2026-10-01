import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProfilePage from '@/pages/ProfilePage';

const { apiGet, apiPut } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('qrcode.react', () => ({
  QRCodeSVG: () => null,
}));

vi.mock('@/hooks/useDictionaries', () => ({
  useDictionaries: () => ({ options: () => [], label: () => '' }),
}));

const updateUserMock = vi.fn();
let authUser: Record<string, unknown> = {};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: authUser, updateUser: updateUserMock }),
}));

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <ProfilePage />
    </QueryClientProvider>,
  );
}

describe('ProfilePage — §G5.9 opt-in portail basse connexion', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiGet.mockResolvedValue({ data: {} });
  });

  it('affiche la section basse connexion avec le switch éteint sans opt-in', () => {
    authUser = { firstName: 'Anne', lastName: 'Nzé', email: 'a@b.c', phone: '+241000000', whatsappOptIn: false };
    renderPage();
    expect(screen.getByText('Portail basse connexion')).toBeInTheDocument();
    const sw = screen.getByRole('switch');
    expect(sw).toHaveAttribute('aria-checked', 'false');
    expect(sw).toBeEnabled();
  });

  it('activer le switch envoie PUT /users/me { whatsappOptIn: true }', async () => {
    authUser = { firstName: 'Anne', lastName: 'Nzé', email: 'a@b.c', phone: '+241000000', whatsappOptIn: false };
    apiPut.mockResolvedValue({ data: { whatsappOptIn: true } });
    renderPage();
    fireEvent.click(screen.getByRole('switch'));
    await waitFor(() =>
      expect(apiPut).toHaveBeenCalledWith('/users/me', { whatsappOptIn: true }),
    );
    expect(updateUserMock).toHaveBeenCalledWith({ whatsappOptIn: true });
  });

  it('sans téléphone, le switch est désactivé (jamais d\'opt-in injoignable)', () => {
    authUser = { firstName: 'Anne', lastName: 'Nzé', email: 'a@b.c', phone: '', whatsappOptIn: false };
    renderPage();
    expect(screen.getByRole('switch')).toBeDisabled();
    expect(screen.getByText(/numéro de téléphone/)).toBeInTheDocument();
  });

  it('le switch reflète un opt-in déjà actif', () => {
    authUser = { firstName: 'Anne', lastName: 'Nzé', email: 'a@b.c', phone: '+241000000', whatsappOptIn: true };
    renderPage();
    expect(screen.getByRole('switch')).toHaveAttribute('aria-checked', 'true');
  });
});

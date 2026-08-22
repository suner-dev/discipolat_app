import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ResetPasswordPage from '@/pages/ResetPasswordPage';

const { apiPost, apiGetErrorMessage } = vi.hoisted(() => ({
  apiPost: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: apiPost,
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: apiGetErrorMessage,
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function LoginStub() {
  return <div>Page de connexion (stub)</div>;
}

function ForgotStub() {
  return <div>Page mot de passe oublié (stub)</div>;
}

function renderPage(token: string | null = 'reset-token-123') {
  const initial = token ? `/reset-password?token=${token}` : '/reset-password';
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initial]}>
        <Routes>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/login" element={<LoginStub />} />
          <Route path="/forgot-password" element={<ForgotStub />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

async function fillPasswords(password = 'Password123', confirm = 'Password123') {
  const user = userEvent.setup();
  await user.type(screen.getByPlaceholderText('Minimum 8 caractères'), password);
  await user.type(screen.getByPlaceholderText('Confirmez le mot de passe'), confirm);
  await user.click(screen.getByRole('button', { name: /réinitialiser le mot de passe/i }));
}

describe('ResetPasswordPage — nouveau mot de passe', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiPost.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('sans token dans l URL → affiche "Lien invalide" avec un lien vers /forgot-password', () => {
    renderPage(null);
    expect(screen.getByText('Lien invalide')).toBeInTheDocument();
    expect(screen.getByText(/invalide ou a expiré/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /renvoyer un lien/i })).toHaveAttribute('href', '/forgot-password');
  });

  it('avec token → affiche le formulaire du nouveau mot de passe', () => {
    renderPage('token-abc');
    // Le titre (heading) est distinct du label du champ « Nouveau mot de passe »
    expect(screen.getByRole('heading', { name: 'Nouveau mot de passe' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Minimum 8 caractères')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Confirmez le mot de passe')).toBeInTheDocument();
  });

  it('mot de passe trop court → erreur de validation sans appel API', async () => {
    renderPage('token-abc');
    await fillPasswords('Court1', 'Court1');

    await waitFor(() => {
      expect(screen.getByText('Le mot de passe doit contenir au moins 8 caractères')).toBeInTheDocument();
    });
    expect(apiPost).not.toHaveBeenCalled();
  });

  it('mot de passe sans majuscule ni chiffre → erreur de validation', async () => {
    renderPage('token-abc');
    await fillPasswords('motdepasse', 'motdepasse');

    await waitFor(() => {
      expect(screen.getByText(
        'Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre'
      )).toBeInTheDocument();
    });
    expect(apiPost).not.toHaveBeenCalled();
  });

  it('confirmation différente → erreur de validation', async () => {
    renderPage('token-abc');
    await fillPasswords('Password123', 'Password124');

    await waitFor(() => {
      expect(screen.getByText('Les mots de passe ne correspondent pas')).toBeInTheDocument();
    });
    expect(apiPost).not.toHaveBeenCalled();
  });

  it('mot de passe valide → POST /auth/reset-password avec token et newPassword puis succès', async () => {
    renderPage('token-abc');
    await fillPasswords();

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/reset-password', {
        token: 'token-abc',
        newPassword: 'Password123',
      });
    });
    await waitFor(() => {
      expect(screen.getByText('Mot de passe réinitialisé !')).toBeInTheDocument();
    });
  });

  it('le bouton "Se connecter" du succès navigue vers /login', async () => {
    renderPage('token-abc');
    await fillPasswords();

    await waitFor(() => {
      expect(screen.getByText('Mot de passe réinitialisé !')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Se connecter' }));

    await waitFor(() => {
      expect(screen.getByText('Page de connexion (stub)')).toBeInTheDocument();
    });
  });

  it('affiche le message derreur quand la réinitialisation échoue', async () => {
    apiPost.mockRejectedValueOnce(new Error('token expired'));
    apiGetErrorMessage.mockReturnValue('Ce lien a expiré');
    renderPage('token-abc');
    await fillPasswords();

    await waitFor(() => {
      expect(screen.getByText('Ce lien a expiré')).toBeInTheDocument();
    });
    expect(screen.queryByText('Mot de passe réinitialisé !')).not.toBeInTheDocument();
  });
});

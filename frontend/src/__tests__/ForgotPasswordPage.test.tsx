import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ForgotPasswordPage from '@/pages/ForgotPasswordPage';

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

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('ForgotPasswordPage — mot de passe oublié', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiPost.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('affiche le formulaire : titre, champ email et bouton denvoi', () => {
    renderPage();
    expect(screen.getByText('Mot de passe oublié')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('vous@email.com')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /envoyer le lien/i })).toBeInTheDocument();
  });

  it('le bouton denvoi est désactivé tant que lemail est vide', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /envoyer le lien/i })).toBeDisabled();
  });

  it('envoie POST /auth/forgot-password avec lemail saisi puis affiche le succès', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('vous@email.com'), 'pasteur@discipolat.com');
    await user.click(screen.getByRole('button', { name: /envoyer le lien/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/forgot-password', { email: 'pasteur@discipolat.com' });
    });
    await waitFor(() => {
      expect(screen.getByText('Email envoyé !')).toBeInTheDocument();
    });
    // « 30 minutes » est dans un <strong>, le texte est découpé entre éléments
    expect(screen.getByText(/30 minutes/)).toBeInTheDocument();
  });

  it('affiche le message derreur quand la demande échoue', async () => {
    apiPost.mockRejectedValueOnce(new Error('smtp down'));
    apiGetErrorMessage.mockReturnValue("Impossible d'envoyer l'email");
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('vous@email.com'), 'pasteur@discipolat.com');
    await user.click(screen.getByRole('button', { name: /envoyer le lien/i }));

    await waitFor(() => {
      expect(screen.getByText("Impossible d'envoyer l'email")).toBeInTheDocument();
    });
    expect(screen.queryByText('Email envoyé !')).not.toBeInTheDocument();
  });

  it('le lien "Retour à la connexion" pointe vers /login', () => {
    renderPage();
    expect(screen.getByRole('link', { name: /retour à la connexion/i })).toHaveAttribute('href', '/login');
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import LoginPage from '@/pages/LoginPage';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderWithProviders() {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('LoginPage', () => {
  it('renders login form with title', () => {
    renderWithProviders();
    expect(screen.getByText('Connexion')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
  });

  it('renders email and password inputs', () => {
    renderWithProviders();
    expect(screen.getByPlaceholderText('vous@email.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
  });

  it('shows demo account hint', () => {
    renderWithProviders();
    expect(screen.getByText(/pasteur@discipolat.com/i)).toBeInTheDocument();
  });
});

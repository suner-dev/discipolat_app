import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import SoulCreatePage from '@/pages/SoulCreatePage';

// Mock API calls
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { content: [] } }),
    post: vi.fn().mockResolvedValue({ data: {} }),
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
          <SoulCreatePage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('SoulCreatePage', () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it('renders the form title', () => {
    renderWithProviders();
    expect(screen.getByText('Nouvelle âme')).toBeInTheDocument();
    expect(screen.getByText('Identité')).toBeInTheDocument();
    expect(screen.getByText('Information de disciple')).toBeInTheDocument();
  });

  it('renders required fields with labels', () => {
    renderWithProviders();
    expect(screen.getByText('Nom *')).toBeInTheDocument();
    expect(screen.getByText('Type *')).toBeInTheDocument();
    expect(screen.getByText("Date d'intégration *")).toBeInTheDocument();
    expect(screen.getByText('Faiseur assigné *')).toBeInTheDocument();
  });

  it('renders submit and cancel buttons', () => {
    renderWithProviders();
    expect(screen.getByRole('button', { name: /créer l'âme/i })).toBeInTheDocument();
    expect(screen.getByText('Annuler')).toBeInTheDocument();
  });

  it('shows validation errors for required fields on submit', async () => {
    renderWithProviders();
    
    const submitBtn = screen.getByRole('button', { name: /créer l'âme/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Nom requis')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it('shows date de conversion field when type is NOUVEAU_CONVERTI', async () => {
    renderWithProviders();
    
    // Select NOUVEAU_CONVERTI from the type dropdown
    const selects = screen.getAllByRole('combobox');
    const typeSelect = selects[0];
    fireEvent.change(typeSelect, { target: { value: 'NOUVEAU_CONVERTI' } });

    await waitFor(() => {
      expect(screen.getByText('Date de conversion')).toBeInTheDocument();
    });
  });
});

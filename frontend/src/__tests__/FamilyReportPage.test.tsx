import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import FamilyReportPage from '@/pages/FamilyReportPage';

const { apiMock } = vi.hoisted(() => ({
  apiMock: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

vi.mock('@/lib/api', () => ({
  default: apiMock,
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const FILES = {
  content: [
    { id: 'f1', nom: 'Compte rendu', typeFichier: 'application/pdf', chemin: 'https://x/f1.pdf', taille: 1024, categorie: 'COMPTE_RENDU' },
  ],
};

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderWithProviders() {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <FamilyReportPage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

function setChefUser() {
  localStorage.setItem('user', JSON.stringify({
    id: 'u1',
    email: 'chef@discipolat.test',
    role: 'CHEF_DE_FAMILLE',
    roles: ['CHEF_DE_FAMILLE'],
    activeRole: 'CHEF_DE_FAMILLE',
    estChefDeFamille: true,
    firstName: 'Chef',
    lastName: 'Test',
    statut: 'ACTIVE',
  }));
}

describe('FamilyReportPage — pièces jointes', () => {
  beforeEach(() => {
    localStorage.clear();
    setChefUser();
    queryClient.clear();
    vi.clearAllMocks();
    apiMock.get.mockImplementation((url: string) => {
      if (url.startsWith('/families')) {
        return Promise.resolve({ data: { content: [{ id: 'fam-1', nom: 'Famille A' }] } });
      }
      if (url.startsWith('/reports/family-weekly/')) {
        return Promise.resolve({
          data: [{
            id: 'fr1',
            familleId: 'fam-1',
            chefFamilleId: 'u1',
            semaine: '2026-08-07',
            statutValidation: 'BROUILLON',
            commentaireSynthese: '',
            piecesJointes: [{ id: 'att-1', fileId: 'f1', nom: 'Compte rendu', url: 'https://x/f1.pdf' }],
          }],
        });
      }
      if (url.startsWith('/reports/maker-weekly')) {
        return Promise.resolve({ data: { content: [] } });
      }
      if (url.startsWith('/files')) return Promise.resolve({ data: FILES });
      return Promise.resolve({ data: {} });
    });
    apiMock.post.mockResolvedValue({ data: {} });
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('affiche le sélecteur de pièces jointes après sélection d’une famille', async () => {
    renderWithProviders();
    await waitFor(() => expect(screen.getByText('Famille A')).toBeInTheDocument());

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'fam-1' } });

    await waitFor(() => expect(screen.getByText('Pièces jointes')).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /joindre des documents/i })).toBeInTheDocument();
  });

  it('affiche les pièces jointes en liens cliquables quand le rapport est soumis', async () => {
    apiMock.get.mockImplementation((url: string) => {
      if (url.startsWith('/families')) {
        return Promise.resolve({ data: { content: [{ id: 'fam-1', nom: 'Famille A' }] } });
      }
      if (url.startsWith('/reports/family-weekly/')) {
        return Promise.resolve({
          data: [{
            id: 'fr1',
            familleId: 'fam-1',
            chefFamilleId: 'u1',
            semaine: '2026-08-07',
            statutValidation: 'SOUMIS',
            commentaireSynthese: '',
            piecesJointes: [{ id: 'att-1', fileId: 'f1', nom: 'Compte rendu', url: 'https://x/f1.pdf' }],
          }],
        });
      }
      if (url.startsWith('/reports/maker-weekly')) {
        return Promise.resolve({ data: { content: [] } });
      }
      if (url.startsWith('/files')) return Promise.resolve({ data: FILES });
      return Promise.resolve({ data: {} });
    });

    renderWithProviders();
    await waitFor(() => expect(screen.getByText('Famille A')).toBeInTheDocument());

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'fam-1' } });

    // Rapport SOUMIS → lien cliquable en lecture seule, pas de sélecteur.
    await waitFor(() => {
      const link = screen.getByRole('link', { name: 'Compte rendu' });
      expect(link).toHaveAttribute('href', 'https://x/f1.pdf');
    });
    expect(screen.queryByRole('button', { name: /joindre des documents/i })).not.toBeInTheDocument();
  });

  it('pré-charge les pièces existantes du rapport et les renvoie à la soumission', async () => {
    renderWithProviders();
    await waitFor(() => expect(screen.getByText('Famille A')).toBeInTheDocument());

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'fam-1' } });

    // La pièce jointe du rapport backend (piecesJointes → fichierIds) est pré-remplie.
    await waitFor(() => expect(screen.getByText('Compte rendu')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /soumettre le rapport de famille/i }));

    await waitFor(() => {
      expect(apiMock.post).toHaveBeenCalledWith(
        '/reports/family-weekly',
        expect.objectContaining({ familleId: 'fam-1', fichierIds: ['f1'] })
      );
    });
  });
});

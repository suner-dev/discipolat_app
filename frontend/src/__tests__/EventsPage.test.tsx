import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import EventsPage from '@/pages/EventsPage';

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
    { id: 'f2', nom: 'Photo culte', typeFichier: 'image/jpeg', chemin: 'https://x/f2.jpg', taille: 2048, categorie: 'PHOTO' },
  ],
};

const EMPTY_PAGE = { content: [], totalPages: 0, number: 0, first: true, last: true, totalElements: 0 };

const EVENT_WITH_PIECES = {
  id: 'evt-1',
  titre: 'Retraite',
  description: '',
  typeEvenement: 'RETRAITE',
  dateDebut: '2026-08-10T09:00',
  dateFin: undefined,
  lieu: '',
  limitePlaces: undefined,
  nbInscrits: 0,
  statut: 'PLANIFIE',
  dateCreation: '2026-08-07T00:00',
  updatedAt: '2026-08-07T00:00',
  organisateurId: 'u1',
  piecesJointes: [{ id: 'att-1', fileId: 'f1', nom: 'Compte rendu', url: 'https://x/f1.pdf' }],
};

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderWithProviders() {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <EventsPage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

function setPasteurUser() {
  localStorage.setItem('user', JSON.stringify({
    id: 'u1',
    email: 'pasteur@discipolat.test',
    role: 'PASTEUR',
    roles: ['PASTEUR'],
    activeRole: 'PASTEUR',
    estChefDeFamille: false,
    firstName: 'Pasteur',
    lastName: 'Test',
    statut: 'ACTIVE',
  }));
}

describe('EventsPage — pièces jointes', () => {
  beforeEach(() => {
    localStorage.clear();
    setPasteurUser();
    queryClient.clear();
    vi.clearAllMocks();
    apiMock.get.mockImplementation((url: string) => {
      if (url.startsWith('/events?')) return Promise.resolve({ data: EMPTY_PAGE });
      if (url.startsWith('/files')) return Promise.resolve({ data: FILES });
      if (url.startsWith('/users')) return Promise.resolve({ data: { content: [] } });
      return Promise.resolve({ data: {} });
    });
    apiMock.post.mockResolvedValue({ data: {} });
    apiMock.put.mockResolvedValue({ data: {} });
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('affiche la section « Pièces jointes » dans le formulaire de création', async () => {
    renderWithProviders();
    await waitFor(() => expect(screen.getByText('Événements')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /nouvel événement/i }));

    expect(screen.getByText('Pièces jointes')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /joindre des documents/i })).toBeInTheDocument();
  });

  it('crée un événement en envoyant les fichierIds sélectionnés', async () => {
    renderWithProviders();
    await waitFor(() => expect(screen.getByText('Événements')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /nouvel événement/i }));

    // Ouvre le sélecteur et coche les deux documents.
    fireEvent.click(screen.getByRole('button', { name: /joindre des documents/i }));
    await waitFor(() => expect(screen.getByText('Compte rendu')).toBeInTheDocument());
    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);
    fireEvent.click(checkboxes[1]);

    fireEvent.change(screen.getByPlaceholderText('Ex: Retraite de prière'), { target: { value: 'Retraite de prière' } });

    fireEvent.click(screen.getByRole('button', { name: /^créer$/i }));

    await waitFor(() => {
      expect(apiMock.post).toHaveBeenCalledWith(
        '/events',
        expect.objectContaining({ titre: 'Retraite de prière', fichierIds: ['f1', 'f2'] })
      );
    });
  });

  function mockEventWithPieces() {
    apiMock.get.mockImplementation((url: string) => {
      if (url.startsWith('/events?')) {
        return Promise.resolve({ data: { content: [EVENT_WITH_PIECES], totalPages: 1, number: 0, first: true, last: true, totalElements: 1 } });
      }
      if (url.startsWith('/files')) return Promise.resolve({ data: FILES });
      if (url.startsWith('/users')) return Promise.resolve({ data: { content: [] } });
      return Promise.resolve({ data: {} });
    });
  }

  it('affiche les pièces jointes des événements avec des liens cliquables', async () => {
    mockEventWithPieces();

    renderWithProviders();
    await waitFor(() => expect(screen.getByRole('button', { name: /modifier/i })).toBeInTheDocument());

    const link = screen.getByRole('link', { name: 'Compte rendu' });
    expect(link).toHaveAttribute('href', 'https://x/f1.pdf');
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('pré-remplit les pièces jointes existantes à l’édition et les renvoie au PUT', async () => {
    mockEventWithPieces();

    renderWithProviders();
    // « Retraite » apparaît dans le titre ET le badge de type → on attend le bouton d'édition.
    await waitFor(() => expect(screen.getByRole('button', { name: /modifier/i })).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /modifier/i }));

    // Le document apparaît dans la colonne Pièces ET pré-rempli dans le picker (2 occurrences).
    await waitFor(() => expect(screen.getAllByText('Compte rendu').length).toBeGreaterThan(0));

    fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }));

    await waitFor(() => {
      expect(apiMock.put).toHaveBeenCalledWith(
        '/events/evt-1',
        expect.objectContaining({ titre: 'Retraite', fichierIds: ['f1'] })
      );
    });
  });
});

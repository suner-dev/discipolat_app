import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import PeopleDirectoryPage from '@/pages/PeopleDirectoryPage';

// -- Mocks --------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

// -- Fixures (forme réelle de PageResponse<Person> / GET /spaces) --------------
const PERSON = {
  id: 'per-1', firstName: 'Jean', lastName: 'Nouveau', fullName: 'Jean Nouveau',
  emailNormalized: 'nouveau@test.com', phoneNormalized: '+33123456789', status: 'ACTIVE',
};
const PAGE = {
  content: [PERSON], page: 0, size: 20, totalElements: 1, totalPages: 1,
};
const SPACES = [{ id: 'sp-1', name: 'Département Louange' }];

function mockUser(role: string) {
  (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({ user: { role } });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockUser('RESPONSABLE');
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url.startsWith('/people')) return Promise.resolve({ data: PAGE });
    if (url === '/spaces') return Promise.resolve({ data: SPACES });
    return Promise.resolve({ data: null });
  });
  (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ data: {} });
});

function renderPage() {
  return render(
    <MemoryRouter>
      <PeopleDirectoryPage />
    </MemoryRouter>,
  );
}

describe('PeopleDirectoryPage (§G3.1/§G6.4)', () => {
  it('charge le répertoire depuis /people et affiche les fiches', async () => {
    renderPage();
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(expect.stringContaining('/people')));
    expect(await screen.findByText('Jean Nouveau')).toBeInTheDocument();
    expect(screen.getByText('nouveau@test.com')).toBeInTheDocument();
  });

  it('applique le filtre SANS ESPACE sur l’API réelle (sansSpace=true)', async () => {
    renderPage();
    const checkbox = await screen.findByRole('checkbox', { name: /sans espace/i });
    fireEvent.click(checkbox);
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(expect.stringContaining('withoutSpace=true')),
    );
  });

  it('affecte une personne à un espace via POST /people/{id}/spaces?spaceId=', async () => {
    renderPage();
    fireEvent.click(await screen.findByText('Affecter'));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/spaces'));
    const select = await screen.findByRole('combobox');
    fireEvent.change(select, { target: { value: 'sp-1' } });
    fireEvent.click(screen.getByText('Confirmer'));
    await waitFor(() =>
      expect(api.post).toHaveBeenCalledWith(
        '/people/per-1/spaces',
        null,
        expect.objectContaining({ params: { spaceId: 'sp-1' } }),
      ),
    );
  });

  it('réserve l’enrôlement manuel aux ADMIN/PASTEUR', async () => {
    renderPage();
    await screen.findByText('Jean Nouveau');
    expect(screen.queryByText('Nouvelle fiche')).not.toBeInTheDocument();
    mockUser('PASTEUR');
    renderPage();
    expect(await screen.findByText('Nouvelle fiche')).toBeInTheDocument();
  });
});

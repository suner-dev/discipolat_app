// PORT Develop1 — suite ajoutee SANS remplacer la suite de main du meme nom.
// Test Develop1 porte a cote de celui de main : il porte la palette
// // §G5.1 de Develop1 (@/components/common/CommandPalette, async + useCommandPaletteOpen),
// // tandis que CommandPalette.test.tsx de main reste vert sur la palette vivante de main.
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import CommandPalette from '@/components/common/CommandPalette';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return { ...actual, useAuth: vi.fn() };
});

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => navigateMock };
});

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

// -- Helpers -----------------------------------------------------------------
function mockAuth(activeRole: string) {
  (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated: true,
    isLoading: false,
    user: { id: 'u1', email: 'a@b.c', role: activeRole, activeRole, roles: [activeRole], firstName: 'A', lastName: 'B' },
    activeRole,
  });
}

function renderPalette(props: Partial<React.ComponentProps<typeof CommandPalette>> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onClose = vi.fn();
  const utils = render(
    <QueryClientProvider client={queryClient}>
      <CommandPalette open onClose={onClose} {...props} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose };
}

const PEOPLE = [
  { id: 'p1', nomComplet: 'Marie Dupont', email: 'marie@eglise.org' },
  { id: 'p2', nomComplet: 'Jean Martin', email: 'jean@eglise.org' },
];

beforeEach(() => {
  vi.clearAllMocks();
  (api.get as ReturnType<typeof vi.fn>).mockResolvedValue({ data: PEOPLE });
});

describe('CommandPalette (G5.1)', () => {
  it('renders the combobox input and default nav commands when opened', () => {
    mockAuth('PASTEUR');
    renderPalette();

    const input = screen.getByRole('combobox');
    expect(input).toBeInTheDocument();
    // A few role-allowed pages are surfaced as commands (translated labels).
    expect(screen.getAllByText('Tableau de bord').length).toBeGreaterThanOrEqual(1);
  });

  it('queries /search/autocomplete once the query reaches 2 chars and lists people', async () => {
    mockAuth('FAISEUR');
    renderPalette();

    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'ma' } });

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/search/autocomplete', { params: { q: 'ma', limit: 8 } }));
    await waitFor(() => expect(screen.getByText('Marie Dupont')).toBeInTheDocument());
    expect(screen.getByText('Jean Martin')).toBeInTheDocument();
  });

  it('does not hit the autocomplete API below 2 characters', () => {
    mockAuth('FAISEUR');
    renderPalette();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'm' } });
    expect(api.get).not.toHaveBeenCalled();
  });

  it('navigates to the person sheet on Enter for the active row', async () => {
    mockAuth('FAISEUR');
    renderPalette();
    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'ma' } });
    await waitFor(() => expect(screen.getByText('Marie Dupont')).toBeInTheDocument());

    // First result is the active option → Enter selects it.
    fireEvent.keyDown(input, { key: 'Enter' });
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/souls/p1'));
  });

  it('moves the active option with ArrowDown/ArrowUp (aria-selected)', async () => {
    mockAuth('FAISEUR');
    renderPalette();
    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'ma' } });
    await waitFor(() => expect(screen.getByText('Marie Dupont')).toBeInTheDocument());

    const listbox = screen.getByRole('listbox');
    const selectedId = () => listbox.querySelector('[aria-selected="true"]')?.id;
    const first = selectedId();
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    expect(selectedId()).not.toBe(first);
    fireEvent.keyDown(input, { key: 'ArrowUp' });
    expect(selectedId()).toBe(first);
  });

  it('closes on Escape', () => {
    mockAuth('FAISEUR');
    const { onClose } = renderPalette();
    fireEvent.keyDown(screen.getByRole('combobox'), { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });

  it('hides admin-only commands for a FAISEUR (role parity, no dead buttons)', () => {
    mockAuth('FAISEUR');
    renderPalette();
    // "Permissions" is ADMIN/PASTEUR-only → must not appear as a command.
    expect(screen.queryByText('Permissions')).not.toBeInTheDocument();
  });

  it('surfaces real quick actions the role is allowed to run', async () => {
    mockAuth('FAISEUR');
    renderPalette();
    const newSoul = await screen.findByText('Nouvelle âme');
    expect(newSoul).toBeInTheDocument();
    fireEvent.click(newSoul);
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/souls/new'));
  });

  it('offers a "see all results" shortcut that prefills /search?q=', async () => {
    mockAuth('FAISEUR');
    renderPalette();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'ma' } });
    const seeAll = await screen.findByText('Voir tous les résultats');
    fireEvent.click(seeAll);
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/search?q=ma'));
  });
});

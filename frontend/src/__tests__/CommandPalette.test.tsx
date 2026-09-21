import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import CommandPalette, { COMMAND_ITEMS } from '@/components/CommandPalette';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';

// jsdom n'implémente pas scrollIntoView (utilisé par l'auto-scroll de la palette)
beforeAll(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

// --- Mocks ---

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn() },
}));

vi.mock('@/hooks/useTheme', () => ({
  useTheme: () => ({ darkMode: false, toggleTheme: vi.fn() }),
}));

const mockUseAuth = vi.mocked(useAuth);
const mockApiGet = vi.mocked(api.get);

function LocationProbe() {
  const loc = useLocation();
  return <div data-testid="loc">{loc.pathname}</div>;
}

function setup(role: string | null) {
  mockUseAuth.mockReturnValue({
    activeRole: role as never,
    user: role ? ({ role } as never) : null,
  } as never);
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <LocationProbe />
      <CommandPalette open onClose={vi.fn()} />
    </MemoryRouter>,
  );
}

describe('CommandPalette — filtrage par rôle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche tous les items ADMIN pour un ADMIN', () => {
    setup('ADMIN');
    expect(screen.getByText('Journal d\u2019audit')).toBeTruthy();
    expect(screen.getByText('Paramètres')).toBeTruthy();
    expect(screen.getAllByRole('option').length).toBe(COMMAND_ITEMS.length);
  });

  it('masque les items admin pour un FAISEUR', () => {
    setup('FAISEUR');
    expect(screen.queryByText('Paramètres')).toBeNull();
    expect(screen.queryByText('Journal d\u2019audit')).toBeNull();
    // L'âme est visible pour un FAISEUR
    expect(screen.getByText('Âmes')).toBeTruthy();
  });

  it('masque les finances pour un RESPONSABLE', () => {
    setup('RESPONSABLE');
    expect(screen.queryByText('Finances')).toBeNull();
    // mais l'inventaire (ADMIN/RESPONSABLE) est visible
    expect(screen.getByText('Inventaire')).toBeTruthy();
  });

  it('filtre par requête parmi les items autorisés', () => {
    setup('ADMIN');
    const input = screen.getByLabelText('Recherche') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'fin' } });
    expect(screen.getByText('Finances')).toBeTruthy();
    expect(screen.queryByText('Inventaire')).toBeNull();
  });

  it('expose les metadata de rôle sur les items', () => {
    const finances = COMMAND_ITEMS.find((i) => i.id === 'mgmt-finances');
    expect(finances?.roles).toContain('ADMIN');
    const dashboard = COMMAND_ITEMS.find((i) => i.id === 'nav-dashboard');
    expect(dashboard?.roles).toBeUndefined();
  });
});

describe('CommandPalette — recherche globale temps réel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockApiGet.mockResolvedValue({ data: [] });
  });

  it('appelle l\u2019autocomplete après le debounce et affiche la section Personnes', async () => {
    mockApiGet.mockResolvedValue({
      data: [{ type: 'AME', id: 'abc-1', nomComplet: 'Jean Kouassi' }],
    });
    setup('ADMIN');
    const input = screen.getByLabelText('Recherche') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'kouas' } });
    await waitFor(
      () => expect(mockApiGet).toHaveBeenCalledWith(
        '/search/autocomplete',
        expect.objectContaining({ params: { q: 'kouas', limit: 6 } }),
      ),
      { timeout: 2000 },
    );
    await waitFor(() => expect(screen.getByText('Jean Kouassi')).toBeTruthy());
  });

  it('ne déclenche pas d\u2019appel API pour une requête < 2 caractères', async () => {
    setup('ADMIN');
    const input = screen.getByLabelText('Recherche') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'j' } });
    await new Promise((r) => setTimeout(r, 400));
    expect(mockApiGet).not.toHaveBeenCalled();
  });

  it('navigue vers la fiche âme avec Entrée (premier résultat = Personnes)', async () => {
    mockApiGet.mockResolvedValue({
      data: [{ type: 'AME', id: 'soul-9', nomComplet: 'Marie Ndiaye' }],
    });
    setup('ADMIN');
    const input = screen.getByLabelText('Recherche') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'mari' } });
    await waitFor(() => expect(screen.getByText('Marie Ndiaye')).toBeTruthy(), { timeout: 2000 });
    fireEvent.keyDown(window, { key: 'Enter' });
    await waitFor(() => expect(screen.getByTestId('loc').textContent).toBe('/souls/soul-9'));
  });

  it('n\u2019affiche pas d\u2019erreur si l\u2019API échoue', async () => {
    mockApiGet.mockRejectedValue(new Error('network down'));
    setup('ADMIN');
    const input = screen.getByLabelText('Recherche') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'tab' } });
    await waitFor(() => expect(screen.getByText('Tableau de bord')).toBeTruthy(), { timeout: 2000 });
    // La palette reste fonctionnelle avec les items statiques filtrés
    expect(screen.getAllByRole('option').length).toBeGreaterThan(0);
  });
});


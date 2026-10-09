import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { I18nProvider } from '@/i18n';

// Mock du client API : le picker parle aux deux endpoints NEUFS du T1.1.
const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));
vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: () => 'Erreur test',
}));

import ChurchPicker from '@/components/auth/ChurchPicker';

function renderPicker(props = {}) {
  return render(
    <MemoryRouter>
      <I18nProvider>
        <ChurchPicker {...(props as any)} />
      </I18nProvider>
    </MemoryRouter>,
  );
}

/** Fait avancer le debounce (300 ms) puis laisse la promesse se résoudre. */
async function flushDebounce() {
  await act(async () => {
    vi.advanceTimersByTime(300);
  });
  await act(async () => {
    await Promise.resolve();
  });
}

beforeEach(() => {
  vi.useFakeTimers();
  apiGet.mockReset();
  localStorage.clear();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("LOT 1 §GLISE-D'ABORD (T1.2) — ChurchPicker", () => {
  it('ne questionne pas le serveur en dessous de 2 caractères', async () => {
    renderPicker();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'e' } });
    await flushDebounce();
    expect(apiGet).not.toHaveBeenCalled();
  });

  it('interroge /public/churches/suggest (debouncée) et affiche les suggestions', async () => {
    apiGet.mockResolvedValue({
      data: { total: 2, items: [{ name: 'Emmanuel Worship', slug: 'emmanuel', city: 'Douala' }, { name: 'Emmanuel Zenith', slug: 'emmanuel-zenith' }] },
    });
    renderPicker();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'emman' } });
    await flushDebounce();

    expect(apiGet).toHaveBeenCalledWith('/public/churches/suggest', { params: { q: 'emman' } });
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    const options = screen.getAllByRole('option');
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveTextContent('Emmanuel Worship');
  });

  it('a11y : le combobox expose aria-expanded qui suit l ouverture de la liste', async () => {
    apiGet.mockResolvedValue({ data: { total: 1, items: [{ name: 'Bethel', slug: 'bethel' }] } });
    renderPicker();
    const combo = screen.getByRole('combobox');
    expect(combo).toHaveAttribute('aria-expanded', 'false');

    fireEvent.change(combo, { target: { value: 'bet' } });
    await flushDebounce();
    expect(combo).toHaveAttribute('aria-expanded', 'true');
  });

  it('sélection → onSelect reçoit { name, slug } (le parent écrira ?tenant=, ?church= : A3)', async () => {
    const onSelect = vi.fn();
    apiGet.mockResolvedValue({ data: { total: 1, items: [{ name: 'Bethel', slug: 'bethel', city: 'Paris' }] } });
    renderPicker({ onSelect });
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'beth' } });
    await flushDebounce();

    fireEvent.click(screen.getByRole('option'));
    expect(onSelect).toHaveBeenCalledWith({ name: 'Bethel', slug: 'bethel' });
    expect(screen.getByText(/Église trouvée/)).toBeInTheDocument();
  });

  it('aucune suggestion → état notFound + CTA vers les pages déjà existantes', async () => {
    apiGet.mockResolvedValue({ data: { total: 0, items: [] } });
    renderPicker();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'zzzz' } });
    await flushDebounce();

    expect(screen.getByText(/n.existe pas ou n.est pas publi/)).toBeInTheDocument();
    // Les CTA pointent vers des pages QUI EXISTENT DÉJÀ (A1/A3), pas de route neuve.
    expect(screen.getByRole('link', { name: /code d.entrée/ })).toHaveAttribute('href', '/join');
    expect(screen.getByRole('link', { name: /lien d.invitation/ })).toHaveAttribute('href', '/accept-invitation');
    expect(screen.getByRole('link', { name: /Créer mon .glise/ })).toHaveAttribute('href', '/register?mode=church');
  });

  it('Entrée sans suggestion active confirme le nom exact via /exists', async () => {
    const onSelect = vi.fn();
    apiGet
      .mockResolvedValueOnce({ data: { total: 0, items: [] } }) // suggest vide
      .mockResolvedValueOnce({ data: { found: true, slug: 'grace', name: 'Église de la Grâce' } }); // exists
    renderPicker({ onSelect });
    const combo = screen.getByRole('combobox');
    fireEvent.change(combo, { target: { value: 'Église de la Grâce' } });
    await flushDebounce();
    // suggest a répondu « rien » → état notFound ; l'utilisateur appuie Entrée :
    fireEvent.keyDown(combo, { key: 'Enter' });
    await act(async () => { await Promise.resolve(); });

    expect(apiGet).toHaveBeenCalledWith('/public/churches/exists', { params: { q: 'Église de la Grâce' } });
    expect(onSelect).toHaveBeenCalledWith({ name: 'Église de la Grâce', slug: 'grace' });
  });

  it('erreur réseau → état erreur non-bloquant (le picker reste un raccourci)', async () => {
    apiGet.mockRejectedValue(new Error('boom'));
    renderPicker();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'emme' } });
    await flushDebounce();
    // Pas de crash, un message discret est affiché.
    expect(screen.getByTestId('church-picker')).toBeInTheDocument();
    expect(screen.getByText('Erreur test')).toBeInTheDocument();
  });

  it('Échap referme la liste sans la vider', async () => {
    apiGet.mockResolvedValue({ data: { total: 1, items: [{ name: 'Bethel', slug: 'bethel' }] } });
    renderPicker();
    const combo = screen.getByRole('combobox');
    fireEvent.change(combo, { target: { value: 'beth' } });
    await flushDebounce();
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    fireEvent.keyDown(combo, { key: 'Escape' });
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect((combo as HTMLInputElement).value).toBe('beth');
  });

  it('NON-RÉGRESSION : jamais d identifiant interne ni de PII dans l affichage', async () => {
    // Le serveur ne renvoie que name/slug/city/country ; on vérifie que le picker
    // n invente aucun champ et ne rend que le nom + localité.
    apiGet.mockResolvedValue({ data: { total: 1, items: [{ name: 'Bethel', slug: 'bethel', city: 'Paris', country: 'FR' }] } });
    renderPicker();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'beth' } });
    await flushDebounce();
    const option = screen.getByRole('option');
    expect(option.textContent).toContain('Bethel');
    expect(option.textContent).toContain('Paris, FR');
    expect(document.body.textContent).not.toContain('tenant');
  });
});

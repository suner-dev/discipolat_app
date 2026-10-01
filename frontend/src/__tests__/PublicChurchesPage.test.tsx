import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PublicChurchesPage from '@/pages/PublicChurchesPage';

/**
 * G6.9 — Annuaire public « Eglises sur Discipolat ».
 *
 * Ce test cible l'implémentation réellement livrée sur main : la page charge
 * l'annuaire en `fetch` direct sur `${API_BASE}/api/v1/public/churches` et lit
 * `{ content, total }` (pas via le client axios). On stubbe donc `global.fetch`
 * pour vérifier (1) le rendu des églises opt-in et (2) l'état vide explicite.
 */
describe('PublicChurchesPage — annuaire public (§G6.9)', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('affiche les églises opt-in renvoyées par /public/churches', async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        content: [
          { slug: 'eglise-central', name: 'Église Central', slogan: 'Bienvenue', city: 'Paris', country: 'FR' },
          { slug: 'grace', name: 'Grace Chapel', city: 'Lyon' },
        ],
        total: 2,
      }),
    });
    render(
      <MemoryRouter>
        <PublicChurchesPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    const calledUrl = String((global.fetch as ReturnType<typeof vi.fn>).mock.calls[0][0]);
    expect(calledUrl).toContain('/public/churches');
    expect(await screen.findByText('Église Central')).toBeInTheDocument();
    expect(screen.getByText('Grace Chapel')).toBeInTheDocument();
    // main joint ville · pays avec « ', ' » (ex. « Paris, FR »).
    expect(screen.getByText(/Paris, FR/)).toBeInTheDocument();
  });

  it('rend un état vide explicite quand aucune église ne participe', async () => {
    (global.fetch as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ content: [], total: 0 }),
    });
    render(
      <MemoryRouter>
        <PublicChurchesPage />
      </MemoryRouter>,
    );
    expect(await screen.findByText(/Aucune eglise/i)).toBeInTheDocument();
  });
});

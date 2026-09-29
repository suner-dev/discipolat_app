// B2 — Tests de la bannière d'onboarding.
// Exigences du plan : affichée si completed=false, masquée si completed=true,
// lien correct, erreur API -> masquée, dismissible pour la session.

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OnboardingBanner from '@/components/onboarding/OnboardingBanner';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  return { default: { get, post: vi.fn() } };
});
import api from '@/lib/api';
const mocked = api as unknown as { get: ReturnType<typeof vi.fn> };

const status = (completed: boolean) => ({
  completed,
  completedAt: completed ? '2026-09-28T10:00:00Z' : null,
  completedBy: null,
  totalSteps: 7,
  completedSteps: completed ? 7 : 2,
  skippedSteps: 0,
  percentage: completed ? 100 : 29,
});

function renderBanner() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter><OnboardingBanner /></MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  sessionStorage.clear();
});
afterEach(() => vi.restoreAllMocks());

describe('B2 — visibilité', () => {
  it('affiche la bannière si completed=false', async () => {
    mocked.get.mockResolvedValue({ data: status(false) });
    renderBanner();
    await waitFor(() => expect(screen.getByText(/Configuration incomplète/)).toBeTruthy());
  });

  it('masque la bannière si completed=true', async () => {
    mocked.get.mockResolvedValue({ data: status(true) });
    const { container } = renderBanner();
    await waitFor(() => expect(mocked.get).toHaveBeenCalled());
    expect(container.querySelector('[role="status"]')).toBeNull();
  });

  it('MASQUE la bannière si l\u2019API échoue (silence, pas de boucle)', async () => {
    mocked.get.mockRejectedValue(new Error('réseau'));
    const { container } = renderBanner();
    await waitFor(() => expect(mocked.get).toHaveBeenCalled());
    expect(container.querySelector('[role="status"]')).toBeNull();
    // Aucune relance en boucle : retry:0 => un seul appel.
    expect(mocked.get).toHaveBeenCalledTimes(1);
  });
});

describe('B2 — contenu et lien', () => {
  it('le lien pointe vers /onboarding-wizard', async () => {
    mocked.get.mockResolvedValue({ data: status(false) });
    renderBanner();
    await waitFor(() => screen.getByText(/Configuration incomplète/));
    const link = screen.getByRole('link');
    expect(link.getAttribute('href')).toBe('/onboarding-wizard');
  });

  it('la bannière est annoncée poliment (role=status, aria-live)', async () => {
    mocked.get.mockResolvedValue({ data: status(false) });
    renderBanner();
    await waitFor(() => expect(screen.getByText(/Configuration incomplète/)).toBeTruthy());
    const el = screen.getByRole('status');
    expect(el.getAttribute('aria-live')).toBe('polite');
  });
});

describe('B2 — dismissible pour la session', () => {
  it('le bouton de fermeture masque la bannière', async () => {
    const user = userEvent.setup();
    mocked.get.mockResolvedValue({ data: status(false) });
    renderBanner();
    await waitFor(() => screen.getByText(/Configuration incomplète/));
    await user.click(screen.getByRole('button', { name: /Masquer cette bannière/ }));
    expect(screen.queryByText(/Configuration incomplète/)).toBeNull();
  });

  it('la dismissal survit au re-rendout (sessionStorage)', async () => {
    const user = userEvent.setup();
    mocked.get.mockResolvedValue({ data: status(false) });
    const first = renderBanner();
    await waitFor(() => screen.getByText(/Configuration incomplète/));
    await user.click(screen.getByRole('button', { name: /Masquer cette bannière/ }));
    expect(sessionStorage.getItem('discipolat:onboarding-banner-dismissed')).toBe('1');
    first.unmount();

    // Nouveau rendu : la bannière doit rester masquée.
    renderBanner();
    await waitFor(() => expect(mocked.get).toHaveBeenCalled());
    expect(screen.queryByText(/Configuration incomplète/)).toBeNull();
  });
});

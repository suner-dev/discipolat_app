import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from '@/pages/RegisterPage';
import api from '@/lib/api';

/* ============================================================================
 * SPEC_ONBOARDING_FLOWS §4.1 — ÉCRAN FONDATEUR.
 *
 * Garde-fou contre une RÉGRESSION SILENCIEUSE : le backend renvoie bien
 * `church.slug` (AuthController:152), mais si le front le jette, le fondateur
 * ne dispose plus que du code à dicter. Le lien /j/<slug> est pourtant la
 * porte la plus fluide du flow (clic, zéro saisie) — c'est un CONTRAT.
 *
 * Ce test verrouille LES DEUX portes : le code ET le lien.
 * ========================================================================== */

vi.mock('@/lib/api', () => {
  const post = vi.fn();
  return {
    default: {
      get: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn(),
      delete: vi.fn(),
      post,
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
      defaults: { headers: { common: {} } },
    },
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

// Pas de mock d'@/i18n : le vrai module est utilisé, comme dans le reste de
// la suite. Seul @/lib/api est simulé (c'est la réponse qu'on contrôle).
const mockedApi = api as unknown as { post: ReturnType<typeof vi.fn> };

function renderRegister() {
  return render(
    <MemoryRouter initialEntries={['/register?mode=church']}>
      <RegisterPage />
    </MemoryRouter>,
  );
}

/** Remplit le formulaire fondateur et le soumet (3 consentements requis). */
async function submitFounderForm() {
  const set = (id: string, value: string) => {
    const el = document.getElementById(id) as HTMLInputElement | null;
    if (el) fireEvent.change(el, { target: { value } });
  };

  set('churchName', 'Eglise de la Grace');
  set('firstName', 'Jean');
  set('lastName', 'Kouassi');
  set('email', 'jean@exemple.com');
  set('password', 'MotDePasse123!');
  set('confirmPassword', 'MotDePasse123!');

  // Les 3 consentements RGPD ne sont JAMAIS pré-cochés (art. 7) : on les
  // coche explicitement, comme le fait un utilisateur réel.
  for (const box of Array.from(
    document.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'),
  )) {
    fireEvent.click(box);
  }

  const submit = document.querySelector('button[type="submit"]') as HTMLButtonElement | null;
  fireEvent.click(submit as HTMLButtonElement);
}

describe('RegisterPage — écran fondateur (code + lien)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockedApi.post.mockResolvedValue({
      data: {
        session: { accessToken: 'tok-access', refreshToken: 'tok-refresh' },
        church: { name: 'Eglise de la Grace', slug: 'eglise-de-la-grace', joinCode: 'GRACE-7K2X' },
      },
    });
  });

  it('envoie createChurch=true puis affiche CODE et LIEN au fondateur', async () => {
    renderRegister();
    await submitFounderForm();

    // 1. Le backend a bien reçu la demande de création.
    await waitFor(() => expect(mockedApi.post).toHaveBeenCalled(), { timeout: 10000 });
    const payload = mockedApi.post.mock.calls[0][1] as Record<string, unknown>;
    expect(payload.createChurch).toBe(true);
    expect(payload.churchName).toBe('Eglise de la Grace');

    // 2. Le CODE est affiché…
    await waitFor(() => expect(screen.getByText('GRACE-7K2X')).toBeInTheDocument(), { timeout: 10000 });

    // 3. …ET le lien d'invitation cliquable /j/<slug> existe aussi.
    // Sans ce test, on pourrait « simplifier » l'écran en ne montrant que le
    // code : le build passe, et le fondateur perd sa porte d'entrée la plus fluide.
    const link = await screen.findByRole('link', { name: /\/j\/eglise-de-la-grace/ }, { timeout: 10000 });
    expect(link).toHaveAttribute('href', '/j/eglise-de-la-grace');
  });

  it('propose un partage WhatsApp contenant le lien ET le code', async () => {
    renderRegister();
    await submitFounderForm();

    const share = await screen.findByRole('link', { name: /Partager/ }, { timeout: 10000 });
    const href = share.getAttribute('href') ?? '';
    expect(href).toContain('wa.me');
    // Le message partagé porte les DEUX portes (lien + code).
    const text = decodeURIComponent(href);
    expect(text).toContain('/j/eglise-de-la-grace');
    expect(text).toContain('GRACE-7K2X');
  });
});
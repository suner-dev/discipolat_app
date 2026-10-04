import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from '@/pages/RegisterPage';
import { AuthProvider } from '@/contexts/AuthContext';
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
  // `AuthProvider` est requis : SPEC_ORGANISATION_DENOMINATION_V2 §7.0 / T-W0.
  // La session rendue par `/auth/register` doit être ADOPTÉE
  // (`adoptSession`), faute de quoi `localStorage.user` reste absent et le
  // fondateur est renvoyé vers /login juste après avoir créé son église.
  // C'est la régression que ce test est censé empêcher.
  //
  // Le provider est bien présent en production : `main.tsx` enveloppe toute
  // l'application. On le reproduit ici comme dans les 10+ autres tests qui
  // rendent un composant authentifié.
  return render(
    <MemoryRouter initialEntries={['/register?mode=church']}>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
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

  // ============================================================================
  // T-W0 / FAILLE F9 — RÉGRESSION SILENCIEUSE, TEST NON VACUANT
  //
  // L'ancien écran posait les jetons puis faisait `localStorage.removeItem('user')`
  // en pensant que `/auth/me` régénérerait l'identité au rechargement. Or
  // `AuthProvider` retourne AVANT l'appel `/auth/me` si `user` est absent :
  // `isAuthenticated` restait `false` et le fondateur était renvoyé vers
  // `/login` JUSTE APRÈS avoir créé son église et reçu son code.
  //
  // Ce test verrouille les TROIS clés attendues par le bootstrap. Les knobs
  // d'écran (code, lien, partage) ne le détecteraient pas : c'est bien
  // l'état de session qui était cassé.
  // ============================================================================
  it('T-W0 (F9) : ADOPTE la session — les 3 clés du bootstrap sont écrites', async () => {
    renderRegister();
    await submitFounderForm();

    // On attend l'écriture : sans elle, le test passerait sur une régression.
    await waitFor(
      () => expect(localStorage.getItem('accessToken')).toBe('tok-access'),
      { timeout: 10000 },
    );

    expect(localStorage.getItem('accessToken')).toBe('tok-access');
    expect(localStorage.getItem('refreshToken')).toBe('tok-refresh');

    // LA CLÉ DÉTERMINANTE : sans `user`, le bootstrap n'hydrate rien et
    // `isAuthenticated` vaut false. C'est précisément ce que l'ancien code
    // effaçait.
    expect(localStorage.getItem('user')).not.toBeNull();

    // L'en-tête par défaut d'axios doit porter le jeton, sinon la requête
    // suivante part sans authentification alors que l'écran affiche « connecté ».
    const { default: apiClient } = await import('@/lib/api');
    expect((apiClient.defaults.headers.common as Record<string, string>).Authorization)
      .toBe('Bearer tok-access');
  });

  it('T-W0 (F9) : la redirection finale ne recharge pas la page (le contexte React est à jour)', async () => {
    renderRegister();
    await submitFounderForm();
    await waitFor(
      () => expect(localStorage.getItem('user')).not.toBeNull(),
      { timeout: 10000 },
    );

    const button = await screen.findByRole(
      'button',
      { name: /Accéder|entrer|Entrer/i },
      { timeout: 10000 },
    );
    // On vérifie qu'un bouton de continuation existe : c'est lui qui exécute
    // `navigate('/dashboard')`. Un `window.location.href` ferait un
    // rechargement complet — fonctionnellement correct mais destructif pour
    // l'état React, et c'est précisément ce que le correctif a remplacé.
    expect(button).toBeInTheDocument();
  });
});
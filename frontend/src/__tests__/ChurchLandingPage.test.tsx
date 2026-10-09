import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { applyScopedBranding } from '@/lib/branding';

// jsdom ne fournit pas scrollIntoView/etc. ; rien de critique ici.
import ChurchLandingPage from '@/pages/ChurchLandingPage';

function mockFetch(status: number, body: unknown) {
  const fn = vi.fn().mockResolvedValue({
    status,
    ok: status >= 200 && status < 300,
    json: async () => body,
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}

function renderAt(slug: string) {
  return render(
    <MemoryRouter initialEntries={[`/e/${slug}`]}>
      <Routes>
        <Route path="/e/:slug" element={<ChurchLandingPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

const PROJECTION = {
  slug: 'bethel',
  name: 'Église Bethel',
  landingEnabled: true,
  slogan: 'Une famille pour toujours',
  description: 'Bienvenue chez nous.',
  logoUrl: 'https://logo.example/bethel.png',
  city: 'Douala',
  country: 'CM',
  branding: { primaryColor: '#ff0055', accentColor: '#00ff88', primaryFont: 'Poppins' },
};

beforeEach(() => {
  document.title = 'Plateforme';
  // Enlève un éventuel <meta name=robots> résiduel d'un autre test.
  document.head.querySelector('meta[name="robots"]')?.remove();
  document.documentElement.removeAttribute('style');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  document.head.querySelector('meta[name="robots"]')?.remove();
  document.head.querySelectorAll('meta[property^="og:"]').forEach((m) => m.remove());
});

describe('LOT 2 §GLISE-D\'ABORD (T2.4) — ChurchLandingPage /e/:slug', () => {
  it('rend la projection publique (nom, slogan, logo, localité)', async () => {
    mockFetch(200, PROJECTION);
    renderAt('bethel');

    expect(await screen.findByText('Église Bethel')).toBeInTheDocument();
    expect(screen.getByText('Une famille pour toujours')).toBeInTheDocument();
    expect(screen.getByText(/Douala, CM/)).toBeInTheDocument();
    expect(screen.getByAltText('Église Bethel')).toHaveAttribute('src', 'https://logo.example/bethel.png');
    // Le CTA réutilise la CONVENTION existante /register?tenant= (A1/A3), rien de neuf.
    expect(screen.getByRole('link', { name: /Rejoindre cette .glise/ })).toHaveAttribute(
      'href',
      '/register?tenant=bethel',
    );
  });

  it('pose noindex dès l\'origine (R2 : pas d\'indexation surprise d\'une page cultuelle)', async () => {
    mockFetch(200, PROJECTION);
    renderAt('bethel');
    await screen.findByText('Église Bethel');

    const robots = document.head.querySelector<HTMLMetaElement>('meta[name="robots"]');
    expect(robots).not.toBeNull();
    expect(robots!.content).toContain('noindex');
    // og:title suit le nom de l'église.
    const ogTitle = document.head.querySelector<HTMLMetaElement>('meta[property="og:title"]');
    expect(ogTitle?.getAttribute('content')).toBe('Église Bethel');
  });

  it('R4 — la palette est SCOPÉE au conteneur : aucune fuite sur :root (marque plateforme)', async () => {
    mockFetch(200, PROJECTION);
    const { container } = renderAt('bethel');
    await screen.findByText('Église Bethel');

    const landingRoot = container.querySelector('[data-testid="church-landing"]') as HTMLElement;
    // La teinte de l'église est posée sur le conteneur…
    expect(landingRoot.style.getPropertyValue('--color-primary-500')).not.toBe('');
    // …mais JAMAIS sur documentElement (:root) — la plateforme garde sa marque.
    expect(document.documentElement.style.getPropertyValue('--color-primary-500')).toBe('');
  });

  it('restaure le titre de la plateforme au démontage (aucune trace entre écrans)', async () => {
    mockFetch(200, PROJECTION);
    const view = renderAt('bethel');
    await screen.findByText('Église Bethel');
    expect(document.title).toBe('Église Bethel');

    view.unmount();
    expect(document.title).toBe('Plateforme');
    expect(document.head.querySelector('meta[name="robots"]')).toBeNull();
  });

  it('404 (R3) — message UNIQUE, sans révéler slug inconnu vs non listée vs landing éteinte', async () => {
    mockFetch(404, {});
    renderAt('nimportequoi');

    expect(await screen.findByText('Page indisponible')).toBeInTheDocument();
    expect(screen.getByText(/n'existe pas, n'est pas publiée, ou a été désactivée/)).toBeInTheDocument();
    // Aucun élément d'oracle : pas de nom, pas de champ serveur.
    expect(screen.queryByText('Église Bethel')).not.toBeInTheDocument();
  });

  it('erreur réseau / quota → état indisponible non-bloquant', async () => {
    mockFetch(429, {});
    renderAt('bethel');
    expect(await screen.findByText('Indisponible temporairement')).toBeInTheDocument();
  });
});

describe('LOT 2 §GLISE-D\'ABORD (T2.4, A6) — applyScopedBranding (unité)', () => {
  it('pose les variables sur le nœud et les retire au cleanup, sans toucher :root', () => {
    const node = document.createElement('div');
    document.body.appendChild(node);
    document.documentElement.removeAttribute('style');

    const cleanupFn = applyScopedBranding(node, { primaryColor: '#123456' });
    expect(node.style.getPropertyValue('--color-primary-500')).not.toBe('');
    expect(node.style.getPropertyValue('--font-sans')).not.toBe('');
    expect(document.documentElement.style.getPropertyValue('--color-primary-500')).toBe('');

    cleanupFn();
    expect(node.style.getPropertyValue('--color-primary-500')).toBe('');
    node.remove();
  });
});

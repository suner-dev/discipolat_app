// FE-5 — Annuaire scopé par entité (FE-3) : `/users?nodeId=<nœud>&scope=`.
//
// Ce que la mission demande est un LIEN navigable : depuis une entité, voir ses
// membres. L'écran choisi est l'annuaire, qui existait déjà sans paramètre de
// nœud — d'où deux obligations vérifiées ici :
//  • sous `?nodeId=`, c'est le roster du nœud qui répond (portée DIRECT ou
//    SUBTREE), l'annuaire entier n'est PAS re-interrogé (20 comptes hors sujet,
//    et un appel de plus au serveur) ;
//  • SANS paramètre, la page est strictement inchangée — la règle absolue du
//    plan est « additif uniquement ».
//
// La portée reçue du client est tolérée mais bornée côté client comme côté
// serveur : une valeur inconnue retombe sur SUBTREE, jamais sur « tout ».

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import UsersPage from '@/pages/UsersPage';
import { I18nProvider } from '@/i18n';
import type { Locale } from '@/i18n';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet, post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur test'),
}));

vi.mock('@/contexts/TenantContext', async () => {
  const actual = await vi.importActual<typeof import('@/contexts/TenantContext')>('@/contexts/TenantContext');
  return {
    ...actual,
    useTenant: () => ({ currentTenant: { id: 'tenant-1', name: 'Église Grace' } }),
    // Hors contexte tenant initialisé, `useServedAccess` retombe sur le prédicat
    // legacy (rôles ADMIN/PASTEUR) — celui de la page telle qu'elle est gardée.
    useTenantOptional: () => null,
  };
});

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'u-admin', role: 'ADMIN', activeRole: 'ADMIN' }, activeRole: 'ADMIN' }),
}));

// La fiche membre est éprouvée ailleurs (UserDetailModalHierarchy) ; ici on
// vérifie seulement que le roster mène bien à elle.
vi.mock('@/components/users/UserDetailModal', () => ({
  UserDetailModal: ({ userId }: { userId: string }) => <div data-testid="user-detail">{userId}</div>,
}));

vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

const NODE_ID = 'c0000000-0000-0000-0000-000000000003';
const ROSTER_URL = `/tenant/organization/nodes/${NODE_ID}/members`;

const DIRECTORY_PAGE = {
  content: [
    {
      id: 'u-direct', firstName: 'Awa', lastName: 'Diallo', email: 'awa@eglise.org',
      role: 'MEMBRE', statut: 'ACTIVE', activeRole: 'MEMBRE', phone: null, photoUrl: null,
      roles: [], createdAt: '2026-01-01T00:00:00Z',
    },
  ],
  page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true,
};

const ROSTER = {
  nodeId: NODE_ID, nodeName: 'Cellule Nord', nodeType: 'GROUP', scope: 'SUBTREE',
  content: [
    {
      userId: 'u-direct', firstName: 'Awa', lastName: 'Diallo', email: 'awa@eglise.org',
      statut: 'ACTIVE', attachedNodeId: NODE_ID, attachedNodeName: 'Cellule Nord',
      attachedNodeType: 'GROUP', direct: true, origine: 'ADHESION_NOEUD', roles: ['Berger'],
    },
  ],
  page: 0, size: 20, totalElements: 1, totalPages: 1, memberCountDirect: 1, memberCountSousArbre: 1,
};

const rosterCalls = () => apiGet.mock.calls.filter(([url]) => String(url).endsWith('/members'));
const directoryCalls = () =>
  apiGet.mock.calls.filter(([url]) => String(url).startsWith('/users?size=')).length;

function renderAt(entry: string, locale: Locale = 'fr') {
  window.localStorage.setItem('discipolat-locale', locale);
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <I18nProvider>
        <MemoryRouter initialEntries={[entry]}>
          <Routes>
            <Route path="/users" element={<UsersPage />} />
          </Routes>
        </MemoryRouter>
      </I18nProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  apiGet.mockImplementation((url: string) => {
    const u = String(url);
    if (u.endsWith('/members')) return Promise.resolve({ data: ROSTER });
    if (u.startsWith('/users?size=')) return Promise.resolve({ data: DIRECTORY_PAGE });
    if (u === '/dictionaries') return Promise.resolve({ data: {} });
    if (u.startsWith('/families')) return Promise.resolve({ data: { content: [] } });
    return Promise.resolve({ data: [] });
  });
});

afterEach(() => {
  window.localStorage.setItem('discipolat-locale', 'fr');
});

describe('UsersPage scopé par entité (?nodeId=)', () => {
  it('rend le roster du nœud et renonce à l’annuaire complet', async () => {
    renderAt(`/users?nodeId=${NODE_ID}&scope=SUBTREE`);
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());

    expect(rosterCalls()).toHaveLength(1);
    expect(apiGet).toHaveBeenCalledWith(ROSTER_URL, { params: { scope: 'SUBTREE', page: 0, size: 20 } });
    expect(directoryCalls()).toBe(0);
    expect(screen.queryByText('Gestion des comptes, rôles et sécurité')).not.toBeInTheDocument();
  });

  it('respecte la portée DIRECT demandée dans l’URL', async () => {
    renderAt(`/users?nodeId=${NODE_ID}&scope=DIRECT`);
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(ROSTER_URL, {
      params: { scope: 'DIRECT', page: 0, size: 20 },
    }));
  });

  it('sans scope dans l’URL, c’est le sous-arbre qui est demandé (défaut le plus utile)', async () => {
    renderAt(`/users?nodeId=${NODE_ID}`);
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(ROSTER_URL, {
      params: { scope: 'SUBTREE', page: 0, size: 20 },
    }));
  });

  it('une portée inventée retombe sur SUBTREE, jamais sur « tout voir »', async () => {
    renderAt(`/users?nodeId=${NODE_ID}&scope=ALL_TENANTS; DROP TABLE users`);
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(ROSTER_URL, {
      params: { scope: 'SUBTREE', page: 0, size: 20 },
    }));
    // Une valeur non reconnue n’est pas recopiée telle quelle au serveur.
    for (const [, opts] of apiGet.mock.calls.filter(([u]) => String(u).endsWith('/members'))) {
      expect((opts as { params: { scope: string } }).params.scope).toMatch(/^(DIRECT|SUBTREE)$/);
    }
  });

  it('offre le retour à l’annuaire complet et la fiche membre (pas d’impasse)', async () => {
    renderAt(`/users?nodeId=${NODE_ID}&scope=SUBTREE`);
    const back = await screen.findByRole('link', { name: /Retour à/ });
    expect(back).toHaveAttribute('href', '/users');

    fireEvent.click(await screen.findByText('Awa Diallo'));
    expect(await screen.findByTestId('user-detail')).toHaveTextContent('u-direct');
  });

  it('l’écran scopé est intégralement traduit (aucune clé brute)', async () => {
    renderAt(`/users?nodeId=${NODE_ID}&scope=SUBTREE`, 'ar');
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(screen.getByText('أعضاء الجهة')).toBeInTheDocument();
    expect(document.body.textContent).not.toMatch(/orgV3\.[a-zA-Z]/);
  });
});

describe('UsersPage sans paramètre — comportement strictement inchangé', () => {
  it('interroge l’annuaire et ne touche pas au roster', async () => {
    renderAt('/users');
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());

    expect(apiGet).toHaveBeenCalledWith('/users?size=20&page=0');
    expect(rosterCalls()).toHaveLength(0);
    expect(screen.getByText('Gestion des comptes, rôles et sécurité')).toBeInTheDocument();
    expect(screen.getByText('Nouvel utilisateur')).toBeInTheDocument();
  });

  it('un nodeId vide (« ?nodeId= ») n’est pas une entité ciblée : annuaire complet', async () => {
    renderAt('/users?nodeId=');
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith('/users?size=20&page=0'));
    expect(rosterCalls()).toHaveLength(0);
  });
});

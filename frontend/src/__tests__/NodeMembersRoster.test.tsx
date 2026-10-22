// FE-5 — Roster « membres d'une entité » (mission hiérarchie ecclésiastique).
//
// Trois choses sont verrouillées ici, et rien d'autre :
//  1. le HOOK `useNodeMembers` (contrat d'appel de GET
//     /tenant/organization/nodes/{id}/members : scope, q omis si vide, page/taille,
//     et silence total tant que nodeId n'est pas posé) ;
//  2. la VUE partagée `NodeRoster`, consommée par l'onglet « Membres » de la fiche
//     nœud ET par l'annuaire scopé `/users?nodeId=` — rendu paginé, DIRECT vs
//     « via … », compteurs, et surtout DÉGRADATION EXPLICITE (une erreur de
//     lecture n'est jamais rendue comme « aucun membre ») ;
//  3. la PARITÉ i18n des clés `orgV3.members.*` / `orgV3.node.tab.members` sur les
//     six locales, rendues par le vrai I18nProvider : un texte qui retombe sur sa
//     clé brute (rosette « orgV3.members… ») est une faute, pas une traduction.
//
// La fiche membre (`UserDetailModal`) est remplacée par un bouchon : ce test prouve
// que le roster Y MÈNE (transmission du userId), pas le contenu de la fiche, déjà
// couvert par `UserDetailModalHierarchy.test.tsx`.

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { ReactNode } from 'react';
import NodeRoster from '@/components/organization/NodeRoster';
import OrganizationNodeDetailPage from '@/pages/admin/OrganizationNodeDetailPage';
import { useNodeMembers, type NodeMembersPage } from '@/hooks/useOrganizationV3';
import { I18nProvider } from '@/i18n';
import type { Locale } from '@/i18n';
import fr from '@/i18n/fr';
import en from '@/i18n/en';
import pt from '@/i18n/pt';
import es from '@/i18n/es';
import sw from '@/i18n/sw';
import ar from '@/i18n/ar';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet, post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur test'),
}));

// Le hook lit le tenant pour sa clé de cache ; le roster, lui, n'envoie JAMAIS de
// tenantId dans la requête (le serveur le déduit du contexte).
vi.mock('@/contexts/TenantContext', () => ({
  useTenant: () => ({ currentTenant: { id: 'tenant-1', name: 'Église Grace' } }),
}));

vi.mock('@/components/users/UserDetailModal', () => ({
  UserDetailModal: ({ userId }: { userId: string }) => <div data-testid="user-detail">{userId}</div>,
}));

const NODE_ID = 'c0000000-0000-0000-0000-000000000003';

function page(overrides: Partial<NodeMembersPage> = {}): NodeMembersPage {
  return {
    nodeId: NODE_ID,
    nodeName: 'Cellule Nord',
    nodeType: 'GROUP',
    scope: 'SUBTREE',
    content: [
      {
        userId: 'u-direct', firstName: 'Awa', lastName: 'Diallo', email: 'awa@eglise.org',
        statut: 'ACTIVE', attachedNodeId: NODE_ID, attachedNodeName: 'Cellule Nord',
        attachedNodeType: 'GROUP', direct: true, origine: 'ADHESION_NOEUD', roles: ['Berger'],
      },
      {
        userId: 'u-indirect', firstName: 'Moussa', lastName: 'Diop', email: 'moussa@eglise.org',
        statut: 'ACTIVE', attachedNodeId: 'c-sous', attachedNodeName: 'Groupe Bethel',
        attachedNodeType: 'DEPARTMENT', direct: false, origine: 'ASSIGNATION_V3', roles: ['Chantre'],
      },
    ],
    page: 0,
    size: 20,
    totalElements: 2,
    totalPages: 1,
    memberCountDirect: 1,
    memberCountSousArbre: 2,
    ...overrides,
  };
}

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

/** Rend la vue avec le VRAI provider i18n (la parité se prouve à l'écran). */
function renderRoster(props: Parameters<typeof NodeRoster>[0], locale: Locale = 'fr') {
  window.localStorage.setItem('discipolat-locale', locale);
  const qc = makeClient();
  return render(
    <QueryClientProvider client={qc}>
      <I18nProvider>
        <NodeRoster {...props} />
      </I18nProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  apiGet.mockResolvedValue({ data: page() });
});

// `tText` lit une variable de module (`activeLocale`) réassignée par I18nProvider :
// sans reset, les assertions en français liraient l'arabe posé par un cas précédent.
afterEach(() => {
  window.localStorage.setItem('discipolat-locale', 'fr');
});

describe('useNodeMembers — contrat avec le backend', () => {
  it('interroge le roster du nœud, avec les paramètres serveur', async () => {
    const qc = makeClient();
    const { result } = renderHook(() => useNodeMembers(NODE_ID), {
      wrapper: ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={qc}>{children}</QueryClientProvider>
      ),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(apiGet).toHaveBeenCalledWith(`/tenant/organization/nodes/${NODE_ID}/members`, {
      params: { scope: 'SUBTREE', page: 0, size: 20 },
    });
    expect(result.current.data?.content).toHaveLength(2);
  });

  it('ne part pas sans nœud (l’onglet « Membres » et l’annuaire scopé sont optionnels)', async () => {
    const qc = makeClient();
    const { result } = renderHook(() => useNodeMembers(null), {
      wrapper: ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={qc}>{children}</QueryClientProvider>
      ),
    });
    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(apiGet).not.toHaveBeenCalled();
  });

  it('omit le filtre q quand il est vide ou en blanc, et trim ce qui reste', async () => {
    const qc = makeClient();
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    );
    renderHook(() => useNodeMembers(NODE_ID, { q: '   ' }), { wrapper });
    await waitFor(() => expect(apiGet).toHaveBeenCalled());
    const paramsBlank = (apiGet.mock.calls[0][1] as { params: Record<string, unknown> }).params;
    expect(paramsBlank).not.toHaveProperty('q');

    apiGet.mockClear();
    renderHook(() => useNodeMembers(NODE_ID, { q: ' diallo ' }), { wrapper });
    await waitFor(() => expect(apiGet).toHaveBeenCalled());
    const paramsTrim = (apiGet.mock.calls[0][1] as { params: Record<string, unknown> }).params;
    expect(paramsTrim).toMatchObject({ q: 'diallo' });
  });

  it('clave le cache par (portée, filtre, page, taille) — deux portées ne partagent pas une réponse', async () => {
    const qc = makeClient();
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    );
    const { rerender } = renderHook(
      ({ scope }: { scope: 'DIRECT' | 'SUBTREE' }) => useNodeMembers(NODE_ID, { scope }),
      { wrapper, initialProps: { scope: 'SUBTREE' } as { scope: 'DIRECT' | 'SUBTREE' } },
    );
    await waitFor(() => expect(apiGet).toHaveBeenCalledTimes(1));
    // Même nœud, autre portée → autre requête (pas une réécriture de cache).
    rerender({ scope: 'DIRECT' });
    await waitFor(() => expect(apiGet).toHaveBeenCalledTimes(2));
  });
});

describe('NodeRoster — la vue des membres d’une entité', () => {
  it('rend les noms, les rôles et les compteurs de l’entité consultée', async () => {
    renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(screen.getByText('Moussa Diop')).toBeInTheDocument();
    expect(screen.getByText('Membres de Cellule Nord')).toBeInTheDocument();
    expect(screen.getByText(/Directs: 1/)).toBeInTheDocument();
    expect(screen.getByText(/Total sous-arbre: 2/)).toBeInTheDocument();
    expect(screen.getByText('Berger')).toBeInTheDocument();
  });

  it('distingue le rattachement direct du rattachement par sous-structure', async () => {
    renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    // Direct → le libellé de portée sur SA ligne ; indirect → « via » + le nom du
    // nœud porteur. Le libellé de portée est aussi le bouton de filtre : on le
    // vérifie par rôle, pas par texte global (sinon l'assertion ne veut rien dire).
    expect(screen.getByText('Awa Diallo').closest('li')).toHaveTextContent('Rattachés ici');
    expect(screen.getByRole('button', { name: 'Rattachés ici' })).toBeInTheDocument();
    expect(screen.getByText('Moussa Diop').closest('li')).toHaveTextContent('via: Groupe Bethel');
  });

  it('ne montre la pagination que lorsqu’il y a plusieurs pages, et change de page', async () => {
    const unique = renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Suivant' })).not.toBeInTheDocument();
    unique.unmount();

    apiGet.mockResolvedValue({ data: page({ totalPages: 3, totalElements: 45 }) });
    renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(screen.getByText('1 / 3')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Suivant' }));
    await waitFor(() =>
      expect(apiGet).toHaveBeenLastCalledWith(`/tenant/organization/nodes/${NODE_ID}/members`, {
        params: { scope: 'SUBTREE', page: 1, size: 20 },
      }),
    );
  });

  it('sans donnée → un vide explicite ; en échec → un message d’erreur, jamais un faux vide', async () => {
    apiGet.mockResolvedValue({ data: page({ content: [], totalElements: 0, totalPages: 0, memberCountSousArbre: 0 }) });
    const { unmount } = renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Aucun membre pour cette sélection.')).toBeInTheDocument());
    expect(screen.queryByText(/Information non renseignée/)).not.toBeInTheDocument();
    unmount();

    apiGet.mockRejectedValue(new Error('500'));
    renderRoster({ nodeId: NODE_ID });
    await waitFor(() =>
      expect(screen.getByText("Le roster n'a pas pu être chargé. Information non renseignée.")).toBeInTheDocument(),
    );
    expect(screen.queryByText('Aucun membre pour cette sélection.')).not.toBeInTheDocument();
  });

  it('le filtre ramène à la page 0 (une recherche ne peut pas porter sur une page fantôme)', async () => {
    apiGet.mockResolvedValue({ data: page({ totalPages: 3 }) });
    renderRoster({ nodeId: NODE_ID });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Suivant' }));
    await waitFor(() => expect(apiGet).toHaveBeenLastCalledWith(expect.any(String), {
      params: { scope: 'SUBTREE', page: 1, size: 20 },
    }));

    fireEvent.change(screen.getByLabelText('Rechercher un membre (nom, e-mail)'), {
      target: { value: 'Diallo' },
    });
    await waitFor(() => expect(apiGet).toHaveBeenLastCalledWith(expect.any(String), {
      params: { scope: 'SUBTREE', q: 'Diallo', page: 0, size: 20 },
    }));
  });

  it('changer de portée interroge le serveur sur la portée (le tri se fait côté serveur)', async () => {
    renderRoster({ nodeId: NODE_ID, initialScope: 'DIRECT' });
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(apiGet).toHaveBeenCalledWith(expect.any(String), {
      params: { scope: 'DIRECT', page: 0, size: 20 },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Avec sous-structures' }));
    await waitFor(() => expect(apiGet).toHaveBeenLastCalledWith(expect.any(String), {
      params: { scope: 'SUBTREE', page: 0, size: 20 },
    }));
    // La lecture précédente reste affichée pendant le nouveau chargement : le
    // champ de recherche ne disparaît pas sous les yeux de l'utilisateur.
    expect(screen.getByLabelText('Rechercher un membre (nom, e-mail)')).toBeInTheDocument();
  });

  it('mène à la fiche membre complète (le roster est une entrée, pas un cul-de-sac)', async () => {
    renderRoster({ nodeId: NODE_ID });
    fireEvent.click(await screen.findByText('Awa Diallo'));
    expect(await screen.findByTestId('user-detail')).toHaveTextContent('u-direct');
  });

  it('ne rend rien sans entité ciblée (pas de roster « de toute l’église » implicite)', () => {
    const { container } = renderRoster({ nodeId: null });
    expect(container).toBeEmptyDOMElement();
    expect(apiGet).not.toHaveBeenCalled();
  });

  it('six locales, zéro clé brute à l’écran', async () => {
    for (const locale of ['fr', 'en', 'pt', 'es', 'sw', 'ar'] as Locale[]) {
      const { unmount } = renderRoster({ nodeId: NODE_ID }, locale);
      // Les noms ne se traduisent pas : ils attestent que la vue est rendue.
      await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
      const text = document.body.textContent ?? '';
      expect(text, `locale ${locale} : une clé i18n est sortie à l’écran`).not.toMatch(/orgV3\.[a-zA-Z]/);
      expect(text.trim().length).toBeGreaterThan(0);
      unmount();
      apiGet.mockClear();
    }
  });

  it('traduit réellement (la même vue en anglais et en arabe)', async () => {
    window.localStorage.setItem('discipolat-locale', 'en');
    const qc = makeClient();
    const { unmount } = render(
      <QueryClientProvider client={qc}>
        <I18nProvider>
          <NodeRoster nodeId={NODE_ID} />
        </I18nProvider>
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.getByText('Moussa Diop')).toBeInTheDocument());
    expect(screen.getByText(/Direct:/)).toBeInTheDocument();
    unmount();

    window.localStorage.setItem('discipolat-locale', 'ar');
    const qc2 = makeClient();
    render(
      <QueryClientProvider client={qc2}>
        <I18nProvider>
          <NodeRoster nodeId={NODE_ID} />
        </I18nProvider>
      </QueryClientProvider>,
    );
    await waitFor(() => expect(screen.getByText('Moussa Diop')).toBeInTheDocument());
    expect(document.documentElement.dir).toBe('rtl');
  });
});

describe('FE-2 — onglet « Membres » de la fiche nœud', () => {
  /** Fiche nœud telle que la monte App.tsx : `/tenant/organization/nodes/:nodeId`. */
  function renderFiche() {
    window.localStorage.setItem('discipolat-locale', 'fr');
    const qc = makeClient();
    return render(
      <QueryClientProvider client={qc}>
        <I18nProvider>
          <MemoryRouter initialEntries={[`/tenant/organization/nodes/${NODE_ID}`]}>
            <Routes>
              <Route path="/tenant/organization/nodes/:nodeId" element={<OrganizationNodeDetailPage />} />
            </Routes>
          </MemoryRouter>
        </I18nProvider>
      </QueryClientProvider>,
    );
  }

  it('l’onglet existe, est atteignable, et n’appelle le roster qu’une fois ouvert', async () => {
    apiGet.mockImplementation((url: string) => {
      const u = String(url);
      if (u.endsWith('/members')) return Promise.resolve({ data: page() });
      if (u === '/tenant/organization/tree') {
        return Promise.resolve({ data: [{ id: NODE_ID, name: 'Cellule Nord', type: 'GROUP', parentId: null }] });
      }
      return Promise.resolve({ data: [] });
    });

    renderFiche();
    // Onglet par défaut = Identité : le roster n’est PAS interrogé (pas d’appel
    // large sur un écran qui n’a rien à voir avec les membres).
    await waitFor(() => expect(screen.getByText('Identité du nœud')).toBeInTheDocument());
    expect(apiGet).not.toHaveBeenCalledWith(expect.stringContaining('/members'), expect.anything());

    fireEvent.click(screen.getByRole('button', { name: 'Membres' }));
    await waitFor(() => expect(screen.getByText('Awa Diallo')).toBeInTheDocument());
    expect(apiGet).toHaveBeenCalledWith(`/tenant/organization/nodes/${NODE_ID}/members`, {
      params: { scope: 'SUBTREE', page: 0, size: 20 },
    });
    // Prolongement de la navigation : l’onglet mène à l’annuaire scopé.
    expect(screen.getByRole('link', { name: /Ouvrir dans/ })).toHaveAttribute(
      'href',
      `/users?nodeId=${NODE_ID}&scope=SUBTREE`,
    );
  });
});

describe('FE-4 — parité stricte des clés du roster sur 6 locales', () => {
  const KEYS = [
    'orgV3.node.tab.members',
    'orgV3.members.title',
    'orgV3.members.titleOf',
    'orgV3.members.search',
    'orgV3.members.empty',
    'orgV3.members.error',
    'orgV3.members.via',
    'orgV3.members.countDirect',
    'orgV3.members.countSousArbre',
    'orgV3.members.scope.DIRECT',
    'orgV3.members.scope.SUBTREE',
    'orgV3.members.prev',
    'orgV3.members.next',
    'orgV3.members.openDirectory',
    'orgV3.members.back',
    'orgV3.members.link',
  ];

  it('toutes les clés existent dans les six dictionnaires, avec une valeur non vide', () => {
    for (const d of [fr, en, pt, es, sw, ar]) {
      for (const k of KEYS) {
        const v = (d as Record<string, string>)[k];
        expect(v, `clé ${k} absente du dictionnaire`).toBeTruthy();
        expect(v.trim().length, `clé ${k} vide`).toBeGreaterThan(0);
      }
    }
  });

  it('aucune valeur n’est laissée à la clé brute (traduction ≠ identité)', () => {
    for (const d of [en, pt, es, sw, ar] as Record<string, string>[]) {
      for (const k of KEYS) expect(d[k], `clé ${k} non traduite`).not.toBe(k);
    }
  });

  it('le libellé d’onglet existe partout — un onglet sans clé s’affiche en clé brute', () => {
    expect((fr as Record<string, string>)['orgV3.node.tab.members']).toBe('Membres');
    expect((en as Record<string, string>)['orgV3.node.tab.members']).toBe('Members');
  });
});

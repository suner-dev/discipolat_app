// PARAMÉTRISATION PAR ÉGLISE (Phase 9) — verrous de l'éditeur de rôles.
//
// Trois propriétés sont verrouillées ici, chacune avec son CONTRÔLE NÉGATIF
// (une implémentation qui régresse doit rougir) :
// 1. la check-list des permissions est branchée sur le catalogue CANONIQUE
//    (`/admin/roles/permissions`, table `permissions`) : l'ancien
//    `/permissions/catalog` (`permission_catalog`, legacy) contient des clés que
//    le backend écartait en silence — « Enregistrer » renvoyait 200 sans effet ;
// 2. une ligne GLOBALE n'est pas éditable telle quelle : le serveur refuse
//    (CANNOT_MODIFY_SYSTEM_ROLE), l'UI doit donc proposer « Personnaliser pour mon
//    église » (`POST /admin/roles/{id}/local-copy`) et désactiver l'éditeur ;
// 3. une surcouche locale qui double une clé globale peut être « Réinitialisée »
//    (`DELETE /admin/roles/{id}/local-copy`) — mais un rôle créé par l'église,
//    lui, n'a pas de catalogue global à retrouver.
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TenantAdminRolesPage from '@/pages/TenantAdminRolesPage';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  const put = vi.fn();
  const del = vi.fn();
  return {
    default: { get, post, put, delete: del },
    api: { get, post, put, delete: del },
    getErrorMessage: () => 'Erreur test',
    __mocks: { get, post, put, del },
  };
});

const mocked = (await import('@/lib/api')) as unknown as {
  __mocks: { get: M; post: M; put: M; del: M };
};
type M = ReturnType<typeof vi.fn>;
const { get, post, put, del } = mocked.__mocks;

// Permissions réelles de l'éditeur, pilotées par test : `hasPermission` du
// TenantContext est lu par la page pour désactiver ce que le serveur refuserait.
const authz = vi.hoisted(() => ({ roles: {} as Record<string, boolean> }));

vi.mock('@/contexts/TenantContext', () => ({
  useTenant: () => ({
    currentTenant: { id: '11111111-1111-1111-1111-111111111111', name: 'Église test' },
    hasPermission: (key: string) => authz.roles[key] !== false,
  }),
}));

const GLOBAL_ADMIN = {
  id: 'aaaaaaaa-0000-0000-0000-000000000001',
  key: 'ADMIN',
  label: 'Administrateur',
  priority: 800,
  isSystem: true,
  permissions: ['USER_READ'],
  local: false,
  effectiveForTenant: true,
  customizable: true,
  customized: false,
};

const GLOBAL_PASTEUR = {
  id: 'aaaaaaaa-0000-0000-0000-000000000002',
  key: 'PASTEUR',
  label: 'Pasteur (catalogue global)',
  priority: 600,
  isSystem: true,
  permissions: ['SOULS_EXECUTE'],
  local: false,
  // La surcouche locale prime : la ligne globale ne décide plus rien ici.
  effectiveForTenant: false,
  customizable: true,
  customized: true,
};

const LOCAL_PASTEUR = {
  id: 'bbbbbbbb-0000-0000-0000-000000000002',
  key: 'PASTEUR',
  label: 'Pasteur (personnalisé ici)',
  priority: 600,
  isSystem: false,
  permissions: ['SOULS_EXECUTE'],
  local: true,
  effectiveForTenant: true,
  customizable: false,
  customized: false,
};

const LOCAL_TRESORIER = {
  id: 'bbbbbbbb-0000-0000-0000-000000000003',
  key: 'TRESORIER',
  label: 'Trésorier',
  priority: 50,
  isSystem: false,
  permissions: [],
  local: true,
  effectiveForTenant: true,
  customizable: false,
  customized: false,
};

/** Catalogue canonique : deux clés, regroupées par `category` (et non par le
 *  champ `module` du legacy). */
const CANONICAL = [
  { key: 'SOULS_EXECUTE', label: 'Âmes — exécuter', category: 'SOULS' },
  { key: 'USER_READ', label: 'Utilisateurs — lire', category: 'USERS' },
];

function stubRoles(roles: unknown[]) {
  get.mockImplementation((path: string) => {
    if (path === '/admin/roles') return Promise.resolve({ data: roles });
    if (path === '/admin/roles/permissions') return Promise.resolve({ data: CANONICAL });
    return Promise.resolve({ data: [] });
  });
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <TenantAdminRolesPage />
    </QueryClientProvider>,
  );
}

/** Carte identifiée par son libellé (unique dans le jeu de tests : deux lignes
 *  peuvent porter la même clé, la globale et sa surcouche). Asynchrone : la liste
 *  arrive d'un GET simulé, la lire trop tôt serait un faux négatif. */
async function cardOf(label: string): Promise<HTMLElement> {
  const node = await screen.findByText(label);
  const card = node.closest('div.bg-white');
  if (!(card instanceof HTMLElement)) throw new Error(`carte introuvable pour ${label}`);
  return card;
}

beforeEach(() => {
  vi.clearAllMocks();
  authz.roles = {};
  post.mockResolvedValue({ data: LOCAL_PASTEUR });
  put.mockResolvedValue({ data: {} });
  del.mockResolvedValue({ data: GLOBAL_PASTEUR });
  // jsdom ne fait pas de boîtes de dialogue : sans stub, confirm() renvoie faux
  // et la porte de sortie de l'UI parait cassée alors qu'elle est testée.
  vi.spyOn(window, 'confirm').mockReturnValue(true);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('Phase 9 — source des permissions présentée à l’éditeur', () => {
  it('ouvre la check-list sur le catalogue canonique, plus sur le legacy', async () => {
    stubRoles([LOCAL_TRESORIER]);
    renderPage();

    await userEvent.click(
      within(await cardOf('Trésorier')).getByRole('button', { name: /Éditer les permissions/ }),
    );

    await waitFor(() => expect(get).toHaveBeenCalledWith('/admin/roles/permissions'));
    // CONTRÔLE NÉGATIF : tant que la page lit `/permissions/catalog`, les clés
    // fantômes reviennent et l'enregistrement redevient un no-op silencieux.
    expect(get.mock.calls.some((c) => c[0] === '/permissions/catalog')).toBe(false);

    // Les clés canoniques sont bien groupées par `category`.
    expect(await screen.findByText('SOULS')).toBeTruthy();
    expect(screen.getByText('USERS')).toBeTruthy();
  });

  it('envoie la matrice complète au contrat du backend (permissionKeys)', async () => {
    stubRoles([LOCAL_TRESORIER]);
    renderPage();

    await userEvent.click(
      within(await cardOf('Trésorier')).getByRole('button', { name: /Éditer les permissions/ }),
    );
    await userEvent.click(await screen.findByRole('checkbox', { name: /SOULS_EXECUTE/ }));
    await userEvent.click(screen.getByRole('button', { name: /Enregistrer/ }));

    await waitFor(() => expect(put).toHaveBeenCalledTimes(1));
    expect(put.mock.calls[0][0]).toBe(
      `/admin/roles/${LOCAL_TRESORIER.id}/permissions`,
    );
    expect(put.mock.calls[0][1]).toEqual({ permissionKeys: ['SOULS_EXECUTE'] });
  });
});

describe('Phase 9 — personnaliser un rôle global', () => {
  it('refuse d’éditer la ligne globale et propose la surcouche', async () => {
    stubRoles([GLOBAL_ADMIN]);
    renderPage();

    const card = await cardOf('Administrateur');
    const permBtn = within(card).getByRole('button', { name: /Éditer les permissions/ });
    // CONTRÔLE NÉGATIF : un bouton actif ici renverrait 400 CANNOT_MODIFY_SYSTEM_ROLE.
    expect(permBtn.getAttribute('disabled')).toBeDefined();
    // L'intitulé technique du garde est conservé : l'admin sait quoi réclamer.
    expect(within(card).getByRole('button', { name: /Personnaliser pour mon église/ })).toBeTruthy();

    await userEvent.click(within(card).getByRole('button', { name: /Personnaliser pour mon église/ }));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0][0]).toBe(`/admin/roles/${GLOBAL_ADMIN.id}/local-copy`);
    // La surcouche renvoyée s'ouvre directement : un clic de plus serait un piège.
    await waitFor(() => expect(get).toHaveBeenCalledWith('/admin/roles/permissions'));
  });

  it('désactive la personnalisation sans ROLE_UPDATE', async () => {
    authz.roles = { ROLE_UPDATE: false };
    stubRoles([GLOBAL_ADMIN]);
    renderPage();

    const btn = within(await cardOf('Administrateur')).getByRole('button', { name: /Personnaliser pour mon église/ });
    expect(btn.getAttribute('disabled')).toBeDefined();
    expect(btn.getAttribute('title')).toBe('ROLE_UPDATE');
  });

  it('ouvre l’éditeur sur la surcouche quand la clé globale est déjà doublée', async () => {
    stubRoles([GLOBAL_PASTEUR, LOCAL_PASTEUR]);
    renderPage();

    // La carte globale n'est plus qu'un modèle : son bouton pointe sur la ligne
    // locale (id different), c'est la seule vraiment exercée par @authz.can.
    const globalCard = await cardOf('Pasteur (catalogue global)');
    await userEvent.click(
      within(globalCard).getByRole('button', { name: /Éditer les permissions/ }),
    );

    await waitFor(() => expect(screen.getByPlaceholderText(/Rechercher/)).toBeTruthy());
    const save = screen.getByRole('button', { name: /Enregistrer/ });
    await userEvent.click(save);

    await waitFor(() => expect(put).toHaveBeenCalledTimes(1));
    expect(put.mock.calls[0][0]).toBe(`/admin/roles/${LOCAL_PASTEUR.id}/permissions`);
    // La ligne globale ne reçoit jamais d'écriture.
    expect(put.mock.calls[0][0]).not.toContain(GLOBAL_PASTEUR.id);
  });
});

describe('Phase 9 — réinitialiser une surcouche', () => {
  it('propose la réinitialisation seulement là où un catalogue global existe', async () => {
    stubRoles([GLOBAL_PASTEUR, LOCAL_PASTEUR, LOCAL_TRESORIER]);
    renderPage();

    // Badge de surcouche : la carte locale est bien identifiée comme telle.
    expect(
      within(await cardOf('Pasteur (personnalisé ici)')).getByText('Personnalisé pour cette église'),
    ).toBeTruthy();
    // Un rôle créé par l'église n'a rien à « réinitialiser » : le bouton
    // apparaîtrait comme une porte vers un rôle inexistant.
    expect(screen.getAllByRole('button', { name: /Réinitialiser/ })).toHaveLength(1);

    await userEvent.click(screen.getByRole('button', { name: /Réinitialiser/ }));
    await waitFor(() => expect(del).toHaveBeenCalledWith(
      `/admin/roles/${LOCAL_PASTEUR.id}/local-copy`,
    ));
  });

  it('ne réinitialise pas sans confirmation de l’agent', async () => {
    stubRoles([GLOBAL_PASTEUR, LOCAL_PASTEUR]);
    renderPage();
    await screen.findByText('Pasteur (personnalisé ici)');
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    await userEvent.click(screen.getByRole('button', { name: /Réinitialiser/ }));

    expect(del).not.toHaveBeenCalled();
  });
});

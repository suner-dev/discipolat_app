// B4 — Tests de la page d'administration des invitations (constat F3).
//
// Chaque cas verrouille un point du contrat réel du backend
// (platform/api/InvitationController) :
// - GET  /admin/invitations?page&size&status&q  -> PageResponse
// - POST /admin/invitations                     -> 201 { invitationLink, emailSent,
//                                                    requiresTenantSwitch } ou
//                                                  201 { invitedUserId }
// - POST /admin/invitations/{id}/resend          -> { invitationLink, emailSent }
// - DELETE /admin/invitations/{id}

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TenantAdminInvitationsPage from '@/pages/TenantAdminInvitationsPage';
import { FALLBACK_ROLES } from '@/hooks/useInvitations';

// `vi.mock` est hoisté au sommet du fichier : les faux doivent être créés
// DANS la factory, sinon ils ne sont pas initialisés au moment du mock.
vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  const del = vi.fn();
  return {
    default: { get, post, delete: del },
    api: { get, post, delete: del },
    getErrorMessage: () => 'Erreur test',
    __mocks: { get, post, del },
  };
});

const mocked = (await import('@/lib/api')) as unknown as {
  __mocks: { get: M; post: M; del: M };
};
type M = ReturnType<typeof vi.fn>;
const { get, post, del } = mocked.__mocks;

vi.mock('@/contexts/TenantContext', () => ({
  useTenant: () => ({
    currentTenant: { id: '11111111-1111-1111-1111-111111111111', name: 'Église test' },
    hasPermission: () => true,
  }),
}));

const PAGE = (content: unknown[], page = 0, totalPages = 1) => ({
  content,
  page,
  size: 20,
  totalElements: content.length,
  totalPages,
});

const FUTURE = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString();
const PAST = new Date(Date.now() - 24 * 3600 * 1000).toISOString();

const INVITATION = {
  id: '22222222-2222-2222-2222-222222222222',
  email: 'pasteur@exemple.com',
  role: 'PASTEUR',
  status: 'PENDING',
  scopeType: 'TENANT',
  createdAt: new Date().toISOString(),
  expiresAt: FUTURE,
  tenantName: 'Église test',
};

function renderPage() {
  // `retryDelay: 0` : la politique du hook reste respectée (un 5xx est
  // rejoué une fois), mais le test n'attend pas le délai exponentiel.
  const qc = new QueryClient({ defaultOptions: { queries: { retryDelay: 0 } } });
  return render(
    <QueryClientProvider client={qc}>
      <TenantAdminInvitationsPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  get.mockResolvedValue({ data: PAGE([INVITATION]) });
  // L'arborescence est un chargement annexe : par défaut elle répond vite.
  get.mockImplementation((path: string) => {
    if (path === '/admin/org/tree') {
      return Promise.resolve({ data: { root: null, nodes: [], childrenByParent: {} } });
    }
    if (path === '/admin/roles/overview') {
      // Repli : les rôles système seedés par V135.
      return Promise.resolve({
        data: FALLBACK_ROLES.map((r) => ({ ...r, system: true, priority: 100 })),
      });
    }
    return Promise.resolve({ data: PAGE([INVITATION]) });
  });
});

describe('B4 — liste des invitations', () => {
  it('consomme la pagination du backend (consommation de A10)', async () => {
    renderPage();

    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    const call = get.mock.calls.find((c) => c[0] === '/admin/invitations');
    // Sans `page`, le backend renvoie la liste complète : l'UI doit demander
    // la page explicite pour bénéficier du tri/filtrage serveur.
    expect(call?.[1]?.params).toMatchObject({ page: 0, size: 20 });
  });

  it('affiche un état vide avec une action, pas un tableau nu', async () => {
    get.mockImplementation((path: string) =>
      path === '/admin/org/tree'
        ? Promise.resolve({ data: { root: null, nodes: [], childrenByParent: {} } })
        : Promise.resolve({ data: PAGE([]) }),
    );

    renderPage();

    expect(await screen.findByText('Aucune invitation')).toBeTruthy();
    expect(screen.getAllByRole('button', { name: 'Nouvelle invitation' }).length).toBeGreaterThan(0);
  });

  it('affiche un état d\'erreur actionnable avec retry', async () => {
    get.mockImplementation((path: string) => {
      if (path === '/admin/org/tree' || path === '/admin/roles/overview') {
        return Promise.resolve({ data: path === '/admin/org/tree' ? { root: null, nodes: [], childrenByParent: {} } : [] });
      }
      return Promise.reject(new Error('500'));
    });

    renderPage();

    expect(await screen.findByText('Chargement impossible')).toBeTruthy();
    const retry = screen.getByRole('button', { name: 'Réessayer' });
    // Le retry est proposé : un écran d'erreur sans sortie est un écran cassé.
    expect(retry).toBeTruthy();
  });

  it('filtre par statut en transmettant le paramètre', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());

    await userEvent.selectOptions(screen.getByLabelText('Statut'), 'ACCEPTED');

    await waitFor(() => {
      const call = get.mock.calls.filter((c) => c[0] === '/admin/invitations').pop();
      expect(call?.[1]?.params).toMatchObject({ status: 'ACCEPTED', page: 0 });
    });
  });

  it('marque « Expirée » une invitation en attente dont la date est passée', async () => {
    get.mockImplementation((path: string) => {
      if (path === '/admin/org/tree') {
        return Promise.resolve({ data: { root: null, nodes: [], childrenByParent: {} } });
      }
      return Promise.resolve({ data: PAGE([{ ...INVITATION, expiresAt: PAST }]) });
    });

    renderPage();

    // Le backend ne rebadge pas la ligne : sans ce traitement, l'admin voit
    // « en attente » sur une invitation morte et la renvoie sans succès.
    expect(await screen.findByText('Expirée')).toBeTruthy();
  });
});

describe('B4 — création', () => {
  it('envoie le contrat exact et montre le lien à copier', async () => {
    post.mockResolvedValue({
      data: {
        success: true,
        invitationId: INVITATION.id,
        email: INVITATION.email,
        role: 'PASTEUR',
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=abc123',
        emailSent: true,
        requiresTenantSwitch: false,
      },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);

    await userEvent.type(await screen.findByLabelText(/Email/), 'nouveau@exemple.com');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'FAMILY_LEADER');
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    // Le corps ne contient QUE les clés attendues par le backend.
    expect(post.mock.calls[0][0]).toBe('/admin/invitations');
    expect(post.mock.calls[0][1]).toEqual({
      email: 'nouveau@exemple.com',
      role: 'FAMILY_LEADER',
      scopeType: 'TENANT',
    });

    // Le lien est affiché : c'est le seul endroit où il existe en clair.
    const link = await screen.findByDisplayValue(/accept-invitation\?token=abc123/);
    expect(link).toBeTruthy();
  });

  it('copie le lien dans le presse-papiers', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    post.mockResolvedValue({
      data: {
        success: true,
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=abc123',
        emailSent: true,
      },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);
    await userEvent.type(await screen.findByLabelText(/Email/), 'a@b.fr');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'MEMBER');
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    const copyBtn = await screen.findByRole('button', { name: 'Copier le lien' });
    await userEvent.click(copyBtn);

    await waitFor(() => expect(writeText).toHaveBeenCalledWith(expect.stringContaining('abc123')));
    expect(await screen.findByRole('button', { name: 'Lien copié' })).toBeTruthy();
  });

  it('annonce explicitement quand l\'email n\'a pas été envoyé (SMTP absent)', async () => {
    post.mockResolvedValue({
      data: {
        success: true,
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=zzz',
        emailSent: false,
      },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);
    await userEvent.type(await screen.findByLabelText(/Email/), 'a@b.fr');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'MEMBER');
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    expect(await screen.findByText(/SMTP indisponible/)).toBeTruthy();
    // Le lien reste disponible : c'est la seule façon de l'envoyer.
    expect(screen.getByDisplayValue(/token=zzz/)).toBeTruthy();
  });

  it('signale le changement d\'église requis (requiresTenantSwitch)', async () => {
    post.mockResolvedValue({
      data: {
        success: true,
        invitationLink: 'https://app.discipolat.com/accept-invitation?token=ccc',
        emailSent: true,
        requiresTenantSwitch: true,
      },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);
    await userEvent.type(await screen.findByLabelText(/Email/), 'a@b.fr');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'MEMBER');
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    expect(await screen.findByText(/appartient déjà à une autre église/)).toBeTruthy();
  });

  it('propose les rôles RÉELLEMENT assignables (consommation de /admin/roles/overview)', async () => {
    // L'écran ne doit plus proposer une liste inventée : le backend résout le
    // rôle dans la table `roles`, donc une clé absente = invitation refusée.
    get.mockImplementation((path: string) => {
      if (path === '/admin/org/tree') {
        return Promise.resolve({ data: { root: null, nodes: [], childrenByParent: {} } });
      }
      if (path === '/admin/roles/overview') {
        return Promise.resolve({
          data: [
            { id: 'aaaaaaaa-0000-0000-0000-000000000001', key: 'MEMBER', label: 'Membre', system: true, priority: 100 },
            { id: 'aaaaaaaa-0000-0000-0000-000000000002', key: 'PASTORS_ITINERANTS', label: 'Pasteurs itinérants', system: false, priority: 120 },
          ],
        });
      }
      return Promise.resolve({ data: PAGE([INVITATION]) });
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);

    const options = await screen.findAllByRole('option');
    const values = options.map((o) => (o as HTMLOptionElement).value).filter(Boolean);
    // Rôle custom du tenant : impossible avec une liste en dur.
    expect(values).toContain('PASTORS_ITINERANTS');
    // Et les anciennes clés françaises invalides ont disparu.
    expect(values).not.toContain('RESPONSABLE');
    expect(values).not.toContain('MEMBRE');
  });

  it('retombe sur les rôles système seedés (V135) si l\'API des rôles échoue', async () => {
    get.mockImplementation((path: string) => {
      if (path === '/admin/org/tree') {
        return Promise.resolve({ data: { root: null, nodes: [], childrenByParent: {} } });
      }
      if (path === '/admin/roles/overview') {
        return Promise.reject(new Error('500'));
      }
      return Promise.resolve({ data: PAGE([INVITATION]) });
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);

    const values = (await screen.findAllByRole('option'))
      .map((o) => (o as HTMLOptionElement).value)
      .filter(Boolean);
    // Les replis sont des clés RÉELLEMENT seedées, pas des traductions.
    expect(values).toEqual(expect.arrayContaining(FALLBACK_ROLES.map((r) => r.key)));
  });

  it('propose la portée ORGANIZATION avec un nœud de l\'arborescence réelle', async () => {
    get.mockImplementation((path: string) => {
      if (path === '/admin/org/tree') {
        return Promise.resolve({
          data: {
            root: { id: '33333333-3333-3333-3333-333333333333', name: 'Église racine', type: 'CHURCH' },
            nodes: [{ id: '44444444-4444-4444-4444-444444444444', name: 'Département Nord', type: 'DEPARTMENT' }],
            childrenByParent: {},
          },
        });
      }
      return Promise.resolve({ data: PAGE([INVITATION]) });
    });
    post.mockResolvedValue({ data: { success: true, invitationLink: 'x', emailSent: true } });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);
    await userEvent.type(await screen.findByLabelText(/Email/), 'a@b.fr');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'MEMBER');
    await userEvent.click(screen.getByRole('radio', { name: /Nœud d’organisation/ }));

    // Le nœud vient de GET /admin/org/tree, pas d'une liste en dur.
    await userEvent.selectOptions(
      (await screen.findByLabelText(/^Nœud \*/)) as HTMLSelectElement,
      '44444444-4444-4444-4444-444444444444',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    await waitFor(() => expect(post).toHaveBeenCalled());
    expect(post.mock.calls[0][1]).toEqual({
      email: 'a@b.fr',
      role: 'MEMBER',
      scopeType: 'ORGANIZATION',
      organizationNodeId: '44444444-4444-4444-4444-444444444444',
    });
  });

  it('traite le cas « membre ajouté directement » (compte déjà connu)', async () => {
    post.mockResolvedValue({
      data: {
        success: true,
        invitedUserId: '55555555-5555-5555-5555-555555555555',
        crossTenantIdentity: false,
        requiresTenantSwitch: false,
        message: 'Utilisateur ajouté directement (compte existant)',
      },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getAllByRole('button', { name: 'Nouvelle invitation' })[0]);
    await userEvent.type(await screen.findByLabelText(/Email/), 'existant@exemple.com');
    await userEvent.selectOptions(screen.getByLabelText(/Rôle/), 'MEMBER');
    await userEvent.click(screen.getByRole('button', { name: 'Envoyer l’invitation' }));

    // Il n'y a pas de lien à envoyer : le dire, plutôt qu'afficher une
    // modale de lien vide.
    expect(await screen.findByText(/ajouté directement/i)).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Copier le lien' })).toBeNull();
  });
});

describe('B4 — renvoi et annulation', () => {
  it('renvoie une invitation en attente et confirme l\'envoi', async () => {
    post.mockResolvedValue({
      data: { success: true, invitationLink: 'https://x/y', emailSent: true, expiresAt: FUTURE },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getByRole('button', { name: 'Renvoyer' }));

    await waitFor(() => expect(post).toHaveBeenCalledWith(`/admin/invitations/${INVITATION.id}/resend`));
  });

  it('ne prétend pas avoir renvoyé quand l\'email n\'est pas parti', async () => {
    post.mockResolvedValue({
      data: { success: true, invitationLink: 'https://x/y', emailSent: false, expiresAt: FUTURE },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getByRole('button', { name: 'Renvoyer' }));

    // Le plan l'exige : avertissement explicite + le lien proposé à la main.
    expect(await screen.findByText(/SMTP indisponible/)).toBeTruthy();
    expect(screen.getByDisplayValue('https://x/y')).toBeTruthy();
  });

  it('demande confirmation avant d\'annuler (pas de confirm() natif)', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('pasteur@exemple.com')).toBeTruthy());
    await userEvent.click(screen.getByRole('button', { name: 'Annuler l’invitation' }));

    // Le ConfirmDialog du design system affiche la question ET porte le
    // bouton de confirmation.
    expect(await screen.findByText('Annuler cette invitation ?')).toBeTruthy();
    // Rien n'est envoyé tant que l'admin n'a pas confirmé.
    expect(del).not.toHaveBeenCalled();

    const dialog = screen.getByText('Annuler cette invitation ?').closest('.modal-content');
    expect(dialog).toBeTruthy();
    const confirmButton = within(dialog as HTMLElement).getByRole('button', {
      name: 'Annuler l’invitation',
    });
    await userEvent.click(confirmButton);
    await waitFor(() => expect(del).toHaveBeenCalledWith(`/admin/invitations/${INVITATION.id}`));
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MetaProvider } from '@/contexts/MetaContext';
import TenantJoinManagementPage from '@/pages/TenantJoinManagementPage';

/**
 * L'écran d'administration ne doit pas annoncer une réussite que le serveur
 * n'a pas	eue.
 *
 * <p>Avant, l'approbation affichait « Demande approuvée. » dans TOUS les cas :
 * quand l'invitation était partie, quand elle était partie dans le vide, et —
 * avant le correctif du JoinMode — quand aucun membre n'avait été créé du
 * tout. Le responsable pouvait croire avoir accueilli quelqu'un alors que la
 * personne n'avait jamais rien reçu, et n'avait aucun moyen de le savoir.
 *
 * <p>Ces tests verrouillent les trois issues du serveur
 * (`JOINED` / `INVITATION_SENT` + email parti / + email NON parti) et la
 * visibilité des adhésions approuvées, sans lesquelles un lien expiré reste
 * introuvable.
 */

const { apiGet, apiPost, apiPatch, apiGetErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPatch: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    patch: apiPatch,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: apiGetErrorMessage,
}));

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

const PENDING = {
  id: 'req-1',
  code: 'BETHEL-7K2M',
  email: 'marie@example.com',
  userId: '',
  status: 'PENDING',
  createdAt: '2026-10-01T10:00:00Z',
  invitationId: '',
  invitationSentAt: '',
  resendable: false,
};

const APPROVED_LINK_OK = {
  id: 'req-2',
  code: 'BETHEL-7K2M',
  email: 'Jean@example.com',
  userId: '',
  status: 'APPROVED',
  createdAt: '2026-10-02T10:00:00Z',
  invitationId: 'inv-2',
  invitationSentAt: '2026-10-02T10:05:00Z',
  resendable: true,
};

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/tenant/join-management']}>
        <MetaProvider>
          <Routes>
            <Route path="/tenant/join-management" element={<TenantJoinManagementPage />} />
          </Routes>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('TenantJoinManagementPage — honnêteté de l’approbation et relance', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();

    apiGet.mockImplementation(async (url: string) => {
      if (url === '/tenant/join-codes') return { data: [] };
      if (url === '/tenant/join-codes/sub-churches') return { data: [] };
      if (url === '/tenant/join-requests') return { data: [] };
      return { data: [] };
    });
    apiPatch.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  /** Programme les deux listes (en attente / approuvées) et le POST. */
  function givenLists(
    pending: unknown[],
    approved: unknown[],
    approveResponse: Record<string, unknown>,
  ) {
    apiGet.mockImplementation(async (url: string, config?: { params?: { status?: string } }) => {
      if (url === '/tenant/join-codes/sub-churches') return { data: [] };
      if (url === '/tenant/join-codes') return { data: [] };
      if (url === '/tenant/join-requests') {
        return config?.params?.status === 'APPROVED'
          ? { data: approved }
          : { data: pending };
      }
      return { data: [] };
    });
    apiPost.mockResolvedValue({ data: approveResponse });
  }

  it('approuver une demande SANS compte : « invitation envoyée », pas « approuvé »', async () => {
    givenLists([PENDING], [], {
      status: 'INVITATION_SENT',
      tenantName: 'Église Bethel',
      applicantEmail: 'marie@example.com',
      emailSent: true,
      invitationExpiresAt: '2026-10-09T10:05:00Z',
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Approuver' }));

    expect(await screen.findByText(/Invitation envoyée/)).toBeInTheDocument();
    // L'email apparaît deux fois : dans la ligne de la demande et dans le
    // message — le responsable sait donc de QUI il parle.
    expect(screen.getAllByText(/marie@example\.com/).length).toBeGreaterThanOrEqual(2);
    // Le message doit dire ce qu'il RESTE à faire, sinon le responsable
    // croira l'adhésion accomplie.
    expect(screen.getByText(/doit encore ouvrir son lien/)).toBeInTheDocument();
    expect(screen.queryByText(/sera examinée par un Super Admin/)).not.toBeInTheDocument();
  });

  it('email parti dans le vide : avertissement explicite, PAS de succès plein', async () => {
    givenLists([PENDING], [], {
      status: 'INVITATION_SENT',
      tenantName: 'Église Bethel',
      applicantEmail: 'marie@example.com',
      emailSent: false,
      invitationExpiresAt: '',
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Approuver' }));

    expect(await screen.findByText(/n’a pas pu être envoyé/)).toBeInTheDocument();
    // Le mot « envoyée » seul serait un mensonge.
    expect(screen.queryByText(/^Invitation envoyée/)).not.toBeInTheDocument();
  });

  it('adhésion avec compte : le rattachement est réel, le message le dit', async () => {
    givenLists([PENDING], [], {
      status: 'JOINED',
      tenantName: 'Église Bethel',
      applicantEmail: '',
      emailSent: false,
      invitationExpiresAt: '',
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Approuver' }));

    expect(await screen.findByText(/membre est rattaché/i)).toBeInTheDocument();
  });

  it('les adhésions approuvées sont listées, avec la date d’envoi du lien', async () => {
    givenLists([], [APPROVED_LINK_OK], {});
    renderPage();

    const section = await screen.findByTestId('join-approved');
    expect(section).toHaveTextContent('Jean@example.com');
    expect(section).toHaveTextContent(/lien envoyé le/);
  });

  it('« Renvoyer le lien » n’apparaît QUE sur une demande relançable (règle du serveur)', async () => {
    givenLists([], [
      APPROVED_LINK_OK,
      { ...APPROVED_LINK_OK, id: 'req-3', email: 'pas-relançable@example.com', resendable: false },
    ], {});
    renderPage();

    const section = await screen.findByTestId('join-approved');
    const boutons = section.querySelectorAll('button');
    expect(boutons).toHaveLength(1);
    expect(section).toHaveTextContent('Renvoyer le lien');
    expect(section).toHaveTextContent('Déjà effectif');
  });

  it('relancer appelle la route dédiée et rend compte du nouvel envoi', async () => {
    givenLists([], [APPROVED_LINK_OK], {
      status: 'INVITATION_SENT',
      tenantName: 'Église Bethel',
      applicantEmail: 'Jean@example.com',
      emailSent: true,
      invitationExpiresAt: '2026-10-20T10:05:00Z',
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: /Renvoyer le lien/ }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/tenant/join-requests/req-2/resend-invitation');
    });
    expect(await screen.findByText(/Invitation envoyée/)).toBeInTheDocument();
  });

  it('un lien JAMAIS envoyé est signalé comme tel — pas présenté comme récent', async () => {
    givenLists([], [
      { ...APPROVED_LINK_OK, id: 'req-4', email: 'jamais@example.com', invitationSentAt: '' },
    ], {});
    renderPage();

    const section = await screen.findByTestId('join-approved');
    expect(section).toHaveTextContent('lien jamais envoyé');
  });

  it('l’écran interroge bien la liste APPROVED, et pas seulement les PENDING', async () => {
    givenLists([PENDING], [APPROVED_LINK_OK], {});
    renderPage();

    await waitFor(() => {
      expect(apiGet).toHaveBeenCalledWith('/tenant/join-requests', {
        params: { status: 'APPROVED' },
      });
    });
    expect(apiGet).toHaveBeenCalledWith('/tenant/join-requests');
  });
});

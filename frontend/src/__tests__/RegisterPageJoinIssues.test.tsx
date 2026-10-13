import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MetaProvider } from '@/contexts/MetaContext';
import RegisterPage from '@/pages/RegisterPage';

/**
 * Les TROIS issues de l'inscription, et le fait qu'elles ne se confondent plus.
 *
 * <p>Avant le correctif, le bandeau de confirmation n'en connaissait que deux :
 * « demande à un Super Admin » et « demande à un responsable d'église ».
 * L'inscription `?tenant=<slug>` d'une église en adhésion OUVERTE — où le
 * serveur crée un compte MEMBRE et envoie un mail d'activation — tombait dans
 * la première et affichait donc : « Votre demande d'église sera examinée par un
 * Super Admin avant toute création de compte ». Deux affirmations fausses :
 * aucun Super Admin n'est concerné, et le compte existait bel et bien.
 *
 * <p>Le serveur distingue désormais les trois cas par des champs explicites
 * (`status`, `role`, `churchName`) ; ce test verrouille que l'écran les rend
 * justes.
 */

const { apiPost, apiGet, apiPut, apiGetErrorMessage } = vi.hoisted(() => ({
  apiPost: vi.fn(),
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    post: apiPost,
    get: apiGet,
    put: apiPut,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: apiGetErrorMessage,
}));

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function LoginStub() {
  return <div>Connexion (stub)</div>;
}

function renderRegister(entry = '/register') {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <MetaProvider>
          <AuthProvider>
            <Routes>
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/login" element={<LoginStub />} />
            </Routes>
          </AuthProvider>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

/** Remplit et soumet le formulaire d'inscription (3 consentements obligatoires). */
async function submitRegister(user: ReturnType<typeof userEvent.setup>) {
  // Les libellés du formulaire se recoupent (« Nom »tombe aussi sur « Nom de l'église »,
  // les deux mots de passe partagent le même placeholder) : on cible par id.
  const field = (id: string) => document.getElementById(id) as HTMLInputElement;
  await user.type(field('email'), 'marie@example.com');
  await user.type(field('password'), 'motdepasse123');
  await user.type(field('confirmPassword'), 'motdepasse123');
  await user.type(field('firstName'), 'Marie');
  await user.type(field('lastName'), 'Kouassi');
  // Uniquement en mode « créer mon église » : le nom est alors obligatoire.
  const churchName = document.getElementById('churchName') as HTMLInputElement | null;
  if (churchName) await user.type(churchName, 'Ma nouvelle église');
  const boxes = screen.getAllByRole('checkbox');
  for (const box of boxes) await user.click(box);
  await user.click(screen.getByRole('button', { name: /s.?inscrire|créer mon compte/i }));
}

describe("RegisterPage — les trois issues de l'inscription", () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockResolvedValue({ data: { versions: {}, current: {} } });
    apiPut.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('église en APPROVAL : « Demande transmise », aucun compte annoncé', async () => {
    apiPost.mockResolvedValue({
      data: {
        status: 'PENDING_APPROVAL',
        churchName: 'Église Bethel de Cocody',
        message: "Demande enregistrée. Un responsable de l'église vous contactera.",
      },
    });
    const user = userEvent.setup();
    renderRegister('/register?tenant=bethel-cocody');
    await submitRegister(user);

    expect(await screen.findByText('Demande transmise')).toBeInTheDocument();
    expect(screen.getByText(/Un responsable vous contactera après validation/)).toBeInTheDocument();
    // Le mensonge « aucun compte avant approbation d'un Super Admin » ne doit
    // jamais apparaître ici.
    expect(screen.queryByText(/Super Admin/)).not.toBeInTheDocument();
  });

  it('église en adhésion ouverte : « Compte créé » + email d’activation (pas de Super Admin)', async () => {
    apiPost.mockResolvedValue({
      data: {
        message: 'Account created. Check your email to activate it.',
        role: 'MEMBRE',
        churchName: 'Église Bethel de Cocody',
      },
    });
    const user = userEvent.setup();
    renderRegister('/register?tenant=bethel-cocody');
    await submitRegister(user);

    expect(await screen.findByText('Compte créé')).toBeInTheDocument();
    expect(screen.getByText(/Vérifiez votre email pour activer votre compte et rejoindre Église Bethel de Cocody/))
      .toBeInTheDocument();
    // C'est le cœur du correctif : aucun Super Admin n'intervient ici.
    expect(screen.queryByText(/Super Admin/)).not.toBeInTheDocument();
  });

  it('demande d’organisation simple : le message Super Admin reste exact', async () => {
    apiPost.mockResolvedValue({
      data: {
        message: "Demande d'église reçue. Elle sera examinée par un Super Admin.",
        status: 'PENDING_APPROVAL',
      },
    });
    const user = userEvent.setup();
    renderRegister('/register');
    await submitRegister(user);

    expect(await screen.findByText(/sera examinée par un Super Admin avant toute création de compte/))
      .toBeInTheDocument();
  });

  it('inscription par code : message inchangé', async () => {
    apiPost.mockResolvedValue({
      data: {
        message: 'Account created. Check your email to activate it.',
        role: 'MEMBRE',
        churchName: 'Église Bethel de Cocody',
      },
    });
    const user = userEvent.setup();
    renderRegister('/register?joinCode=BETHEL-7K2M&church=%C3%89glise%20Bethel');
    await submitRegister(user);

    // Le code passe déjà par la même réponse que l'église ouverte : le nom de
    // l'église est donc nommé, ce qui est plus précis qu'avant sans rien changer
    // au contrat du serveur.
    expect(await screen.findByText(/Vérifiez votre email pour activer votre compte/)).toBeInTheDocument();
  });

  it('« Créer mon église » reste le parcours fondateur avec session immédiate', async () => {
    apiPost.mockResolvedValue({
      data: {
        session: { accessToken: 'a1', refreshToken: 'r1', userId: 'u1', roles: ['TENANT_OWNER'] },
        church: { name: 'Ma nouvelle église', slug: 'ma-nouvelle-eglise', joinCode: 'MA-NOUV-7K2M' },
        grantedPlan: 'DISCOVERY',
        pendingPlan: null,
      },
    });
    const user = userEvent.setup();
    renderRegister('/register?mode=church');
    await submitRegister(user);

    await waitFor(() => {
      expect(screen.getByText(/Votre église est prête/)).toBeInTheDocument();
    });
    expect(apiPost).toHaveBeenCalledWith('/auth/register', expect.objectContaining({ createChurch: true }));
  });
});

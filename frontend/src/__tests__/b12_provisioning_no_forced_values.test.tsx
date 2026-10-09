// B12 — Provisioning web : aucune valeur géo/plan FORCÉE dans le payload.
//
// Régression du constat : `PlatformOnboardingFlowPage` pré-remplissait
// `plan:'free'`, `country:'CM'`, `currency:'XAF'`, `timezone:'Africa/Douala'`
// et les étalait dans le POST. Un super-admin provisionnant une église
// européenne obtenait un tenant camerounais en XOF sans l'avoir demandé.
// Le mobile applique déjà la règle opposée (§G-B.5) ; ce test verrouille
// l'alignement du web sur ce comportement.
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PlatformOnboardingFlowPage from '@/pages/PlatformOnboardingFlowPage';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  return { default: { get, post }, api: { get, post }, getErrorMessage: () => 'Erreur test', __mocks: { get, post } };
});
const mocked = (await import('@/lib/api')) as unknown as {
  __mocks: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };
};
const { get, post } = mocked.__mocks;

vi.mock('react-hot-toast', () => ({ default: { success: vi.fn(), error: vi.fn() } }));

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <PlatformOnboardingFlowPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  get.mockResolvedValue({ data: [] }); // aucun plan renvoyé par le serveur
  // Le contrôleur renvoie `{ tenant, church, department, family, owner }` : sans
  // cette forme, le parcours s'arrête avant le POST.
  post.mockResolvedValue({
    data: {
      tenant: { id: 't', name: 'Église Bethel', slug: 'eglise-bethel', status: 'ACTIVE' },
      church: { id: 'c' },
      department: { id: 'd' },
      family: { id: 'f' },
      owner: { userId: 'o', email: 'pasteur@eglise.com', activationEmailSent: true },
    },
  });
});

// ── B12 — aucune donnée géo/plan FORCÉE dans le payload de provisioning ──────
describe('B12 PlatformOnboardingFlow : aucune valeur forcée', () => {
  it("le payload ne contient ni plan ni géo quand l'admin n'a rien choisi", async () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } });
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <PlatformOnboardingFlowPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Le formulaire ne doit afficher AUCUNE valeur pré-remplie : ni devise, ni
    // pays, ni fuseau, ni plan.
    expect(screen.queryByDisplayValue('CM')).toBeNull();
    expect(screen.queryByDisplayValue('XAF')).toBeNull();
    expect(screen.queryByDisplayValue('Africa/Douala')).toBeNull();
    expect(screen.queryByDisplayValue('free')).toBeNull();

    // Parcours complet jusqu'au POST.
    await userEvent.type(
      screen.getByPlaceholderText('Ex. : Église Évangélique Bethel'),
      'Église Bethel',
    );
    await userEvent.type(
      screen.getByPlaceholderText('pasteur@eglise.com'),
      'pasteur@eglise.com',
    );
    await userEvent.type(screen.getByLabelText('Prénom du propriétaire *'), 'Jean');
    await userEvent.type(screen.getByLabelText('Nom du propriétaire *'), 'Dupont');
    await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
    await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
    await userEvent.type(screen.getByPlaceholderText('Ex. : Accueil & Louange'), 'Accueil');
    await userEvent.type(screen.getByPlaceholderText('Prénom *'), 'Paul');
    await userEvent.type(screen.getByPlaceholderText('Nom *'), 'Mbarga');
    await userEvent.type(screen.getByPlaceholderText('Email *'), 'paul@eglise.com');
    await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
    await userEvent.type(screen.getByPlaceholderText('Prénom *'), 'Anne');
    await userEvent.type(screen.getByPlaceholderText('Nom *'), 'Ngo');
    await userEvent.type(screen.getByPlaceholderText('Email *'), 'anne@eglise.com');
    await userEvent.click(
      screen.getByRole('button', { name: /Provisionner l'organisation/ }),
    );

    await waitFor(() => expect(post).toHaveBeenCalled());
    // Le 2e argument EST le corps : `api.post(url, body)`, pas `{ data }`.
    const body = post.mock.calls[0][1] as Record<string, unknown>;

    // C'est le cœur du correctif : ces quatre clés doivent être ABSENTES, pas
    // présentes-vides et surtout pas valuées ('free'/'CM'/'XAF'/Africa/Douala).
    expect(body.plan).toBeUndefined();
    expect(body.country).toBeUndefined();
    expect(body.currency).toBeUndefined();
    expect(body.timezone).toBeUndefined();

    // Le reste du contrat est intact.
    expect(body.name).toBe('Église Bethel');
    expect(body.ownerEmail).toBe('pasteur@eglise.com');
    expect(body.churchName).toBeTruthy();
  });
});

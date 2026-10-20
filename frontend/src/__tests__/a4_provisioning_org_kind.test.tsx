// A4 (contrat §3.5) — nature de l'organisation au provisioning web.
//
// Le choix « Type d'organisation » voyage jusqu'au POST
// /platform/admin/provisioning, mais SEULEMENT si l'utilisateur a choisi
// explicitement autre chose que CHURCH (même règle B12 que plan/géo :
// on n'envoie pas une valeur que l'admin n'a pas affirmée ; le serveur
// applique CHURCH par défaut). Ce test verrouille les deux branches.
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

beforeEach(() => {
  vi.clearAllMocks();
  get.mockResolvedValue({ data: [] });
  post.mockResolvedValue({
    data: {
      tenant: { id: 't', name: 'Bethel', slug: 'bethel', status: 'ACTIVE' },
      church: { id: 'c' },
      department: { id: 'd' },
      family: { id: 'f' },
      owner: { userId: 'o', email: 'pasteur@eglise.com', activationEmailSent: true },
    },
  });
});

/// Parcourt les 4 étapes jusqu'au POST. `kind` optionnel : si fourni, le
/// sélecteur « Type d'organisation » est manipulé sur l'étape 1 (choix
/// explicite), sinon il reste à sa valeur initiale CHURCH (aucun choix).
async function runFlow(opts: { kind?: string } = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } });
  render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <PlatformOnboardingFlowPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );

  await userEvent.type(
    screen.getByPlaceholderText('Ex. : Église Évangélique Bethel'),
    'Église Bethel',
  );
  if (opts.kind) {
    // Le select porte en valeur affichée le libellé de l'option retenue ;
    // seule cette liste d'options contient « Église isolée ».
    const kindSelect = screen.getByDisplayValue('Église isolée');
    await userEvent.selectOptions(kindSelect, opts.kind);
  }
  await userEvent.type(screen.getByPlaceholderText('pasteur@eglise.com'), 'pasteur@eglise.com');
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
  await userEvent.click(screen.getByRole('button', { name: /Provisionner l'organisation/ }));

  await waitFor(() => expect(post).toHaveBeenCalled());
  // Le 2e argument EST le corps : `api.post(url, body)`, pas `{ data }`.
  return post.mock.calls[0][1] as Record<string, unknown>;
}

describe('A4 — nature de l\'organisation (contrat §3.5)', () => {
  it("omet kind quand l'admin garde le choix par défaut (CHURCH serveur)", async () => {
    const body = await runFlow();
    // La clé doit être ABSENTE (pas 'CHURCH' plaqué sans choix explicite),
    // cohérent avec la règle B12 sur plan/géo.
    expect(body.kind).toBeUndefined();
    expect(Object.values(body)).not.toContain('CHURCH');
  });

  it("transmet kind quand l'admin choisit explicitement Dénomination", async () => {
    const body = await runFlow({ kind: 'DENOMINATION' });
    expect(body.kind).toBe('DENOMINATION');
    // Le reste du contrat est intact.
    expect(body.name).toBe('Église Bethel');
    expect(body.ownerEmail).toBe('pasteur@eglise.com');
  });
});

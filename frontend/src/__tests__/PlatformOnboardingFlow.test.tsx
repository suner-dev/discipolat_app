// B12 — Provisioning web : champs owner obligatoires (contrat §3.5).
//
// Verrouille l'ajout de l'owner à l'étape « Organisation » de
// PlatformOnboardingFlowPage et sa conformité au contrat réel
// `POST /platform/admin/provisioning` :
//   { ownerEmail, ownerFirstName, ownerLastName } dans le corps
//   réponse { owner: { userId, email, activationEmailSent } }
// Cas imposés : validation owner (3 cas), payload conforme,
// récapitulatif avec activationEmailSent = false (avertissement).
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PlatformOnboardingFlowPage from '@/pages/PlatformOnboardingFlowPage';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  return {
    default: { get, post },
    api: { get, post },
    getErrorMessage: () => 'Erreur test',
    __mocks: { get, post },
  };
});
type M = ReturnType<typeof vi.fn>;
const mocked = (await import('@/lib/api')) as unknown as { __mocks: { get: M; post: M } };
const { get, post } = mocked.__mocks;

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

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

// Remplit l'étape « Organisation » jusqu'à ce que l'owner soit valide.
async function fillOrgAndOwner() {
  await userEvent.type(screen.getByPlaceholderText('Ex. : Église Évangélique Bethel'), 'Église Bethel');
  await userEvent.type(screen.getByPlaceholderText('pasteur@eglise.com'), 'pasteur@eglise.com');
  await userEvent.type(screen.getByLabelText('Prénom du propriétaire *'), 'Jean');
  await userEvent.type(screen.getByLabelText('Nom du propriétaire *'), 'Dupont');
}

// Enchaîne les étapes 1→4 jusqu'au POST de provisionnement.
async function driveToProvisioning() {
  await fillOrgAndOwner();
  // Étape 0 → 1 (l'église est pré-remplie automatiquement).
  await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
  // Étape 1 → 2.
  await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
  // Étape 2 : département + nouveau responsable (mode « new » par défaut).
  await userEvent.type(screen.getByPlaceholderText('Ex. : Accueil & Louange'), 'Accueil');
  await userEvent.type(screen.getByPlaceholderText('Prénom *'), 'Paul');
  await userEvent.type(screen.getByPlaceholderText('Nom *'), 'Mbarga');
  await userEvent.type(screen.getByPlaceholderText('Email *'), 'paul@eglise.com');
  await userEvent.click(screen.getByRole('button', { name: 'Continuer' }));
  // Étape 3 : famille (nom auto-rempli) + nouveau chef.
  await userEvent.type(screen.getByPlaceholderText('Prénom *'), 'Anne');
  await userEvent.type(screen.getByPlaceholderText('Nom *'), 'Ngo');
  await userEvent.type(screen.getByPlaceholderText('Email *'), 'anne@eglise.com');
  await userEvent.click(screen.getByRole('button', { name: /Provisionner l'organisation/ }));
}

beforeEach(() => {
  vi.clearAllMocks();
  get.mockResolvedValue({ data: [] }); // catalogue de plans vide → repli interne
});

describe('B12 — validation de l\'owner (étape Organisation)', () => {
  it('bloque « Continuer » quand l\'email du propriétaire est absent', () => {
    renderPage();
    const continueBtn = screen.getByRole('button', { name: 'Continuer' }) as HTMLButtonElement;
    expect(continueBtn.disabled).toBe(true);
  });

  it('bloque « Continuer » quand l\'email du propriétaire est invalide', async () => {
    renderPage();
    await userEvent.type(screen.getByPlaceholderText('pasteur@eglise.com'), 'pas-un-email');
    await userEvent.type(screen.getByLabelText('Prénom du propriétaire *'), 'Jean');
    await userEvent.type(screen.getByLabelText('Nom du propriétaire *'), 'Dupont');
    const continueBtn = screen.getByRole('button', { name: 'Continuer' }) as HTMLButtonElement;
    expect(continueBtn.disabled).toBe(true);
  });

  it('bloque « Continuer » quand le nom du propriétaire manque malgré un email valide', async () => {
    renderPage();
    await userEvent.type(screen.getByPlaceholderText('pasteur@eglise.com'), 'pasteur@eglise.com');
    await userEvent.type(screen.getByLabelText('Prénom du propriétaire *'), 'Jean');
    const continueBtn = screen.getByRole('button', { name: 'Continuer' }) as HTMLButtonElement;
    expect(continueBtn.disabled).toBe(true);
  });

  it('débloque « Continuer » dès que l\'owner est complet et valide', async () => {
    renderPage();
    await fillOrgAndOwner();
    const continueBtn = screen.getByRole('button', { name: 'Continuer' }) as HTMLButtonElement;
    expect(continueBtn.disabled).toBe(false);
  });
});

describe('B12 — payload conforme au contrat §3.5', () => {
  it('envoie ownerEmail / ownerFirstName / ownerLastName au provisioning', async () => {
    post.mockResolvedValue({
      data: {
        tenant: { id: 't', name: 'Église Bethel', slug: 'eglise-bethel', plan: 'free', status: 'ACTIVE' },
        church: { id: 'c' }, department: { id: 'd' }, family: { id: 'f' },
        owner: { userId: 'o', email: 'pasteur@eglise.com', activationEmailSent: true },
      },
    });
    renderPage();
    await driveToProvisioning();

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      '/platform/admin/provisioning',
      expect.objectContaining({
        ownerEmail: 'pasteur@eglise.com',
        ownerFirstName: 'Jean',
        ownerLastName: 'Dupont',
      }),
    ));
  });
});

describe('B12 — récapitulatif et email d\'activation', () => {
  it('affiche l\'email owner et avertit si activationEmailSent = false', async () => {
    post.mockResolvedValue({
      data: {
        tenant: { id: 't', name: 'Église Bethel', slug: 'eglise-bethel', plan: 'free', status: 'ACTIVE' },
        church: { id: 'c' }, department: { id: 'd' }, family: { id: 'f' },
        owner: { userId: 'o', email: 'pasteur@eglise.com', activationEmailSent: false },
      },
    });
    renderPage();
    await driveToProvisioning();

    // Récapitulatif atteint : l'email de l'owner est affiché…
    expect(await screen.findByText('pasteur@eglise.com')).toBeTruthy();
    // …et l\'avertissement d\'email non envoyé aussi (constat B12).
    expect(
      await screen.findByText('Email d\'activation non envoyé, transmettez le lien manuellement.'),
    ).toBeTruthy();
  });

  it('confirme l\'envoi quand activationEmailSent = true', async () => {
    post.mockResolvedValue({
      data: {
        tenant: { id: 't', name: 'Église Bethel', slug: 'eglise-bethel', plan: 'free', status: 'ACTIVE' },
        church: { id: 'c' }, department: { id: 'd' }, family: { id: 'f' },
        owner: { userId: 'o', email: 'pasteur@eglise.com', activationEmailSent: true },
      },
    });
    renderPage();
    await driveToProvisioning();

    expect(await screen.findByText('Email d\'activation envoyé')).toBeTruthy();
  });
});

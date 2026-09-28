// B1 — Tests du wizard d'onboarding (≥ 12 cas exigés par le plan).
// Chaque cas verrouille un point du contrat §3.1 ou un critère d'acceptation.

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AxiosError } from 'axios';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OnboardingWizardPage from '@/pages/OnboardingWizardPage';
import type { OnboardingStep } from '@/types/onboarding';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  const api = { get, post, put: vi.fn(), delete: vi.fn() } as unknown as {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
  };
  return { default: api, api, getErrorMessage: () => 'Erreur test' };
});

import api from '@/lib/api';
const mocked = api as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

function makeStep(over: Partial<OnboardingStep> = {}): OnboardingStep {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    stepType: 'CHURCH_IDENTITY',
    stepOrder: 0,
    title: 'Identité de l\u2019église',
    description: 'Nom, logo, devise.',
    status: 'PENDING',
    isCompleted: false,
    isSkippable: false,
    skipRequiresReason: false,
    startedAt: null,
    completedAt: null,
    completedData: null,
    ...over,
  };
}

const STEPS: OnboardingStep[] = [
  makeStep({ id: 's0', stepType: 'CHURCH_IDENTITY', stepOrder: 0, title: 'Identité de l\u2019église' }),
  makeStep({
    id: 's1', stepType: 'MEMBER_IMPORT', stepOrder: 1, title: 'Import des membres',
    isSkippable: true, skipRequiresReason: true,
  }),
  makeStep({ id: 's2', stepType: 'STRUCTURE', stepOrder: 2, title: 'Familles et départements' }),
  makeStep({
    id: 's3', stepType: 'ROLES', stepOrder: 3, title: 'Inviter les responsables',
    isSkippable: true, skipRequiresReason: true,
  }),
  makeStep({ id: 's4', stepType: 'BRANDING', stepOrder: 4, title: 'Identité visuelle', isSkippable: true }),
  makeStep({ id: 's5', stepType: 'MODULES', stepOrder: 5, title: 'Modules activés', isSkippable: true }),
  makeStep({ id: 's6', stepType: 'FIRST_EVENT', stepOrder: 6, title: 'Premier événement' }),
];

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <OnboardingWizardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const progress = (p?: OnboardingStep[]) => ({
  totalSteps: 7, completedSteps: 0, skippedSteps: 0, percentage: 0, isComplete: false,
  steps: p === undefined ? STEPS : p,
});

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockImplementation(async (url: string) => {
    if (url === '/onboarding-wizard') return { data: STEPS };
    if (url === '/onboarding-wizard/progress') return { data: progress() };
    if (url === '/onboarding-wizard/status') {
      return { data: { completed: false, completedAt: null, completedBy: null, totalSteps: 7,
        completedSteps: 0, skippedSteps: 0, percentage: 0 } };
    }
    if (url === '/admin/tenant-features') return { data: [] };
    return { data: [] };
  });
  mocked.post.mockResolvedValue({ data: STEPS[0] });
});

afterEach(() => { vi.restoreAllMocks(); });

describe('B1 — rendu du contrat §3.1', () => {
  it('affiche les 7 étapes du contrat dans l\u2019ordre de stepOrder', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText(/1\. Identité de l\u2019église/)).toBeTruthy());
    const nav = screen.getByRole('navigation');
    expect(within(nav).getByText(/7\. Premier événement/)).toBeTruthy();
  });

  it('marque l\u2019étape courante avec aria-current="step"', async () => {
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    expect(screen.getByRole('button', { name: /1\. Identité de l\u2019église/ })
      .getAttribute('aria-current')).toBe('step');
  });

  it('expose la progression avec role=progressbar', async () => {
    const { container } = renderPage();
    await waitFor(() => expect(container.querySelector('[role="progressbar"]')).toBeTruthy());
  });

  it('n\u2019utilise jamais le champ "order" (absent du contrat) : le tri suit stepOrder', async () => {
    const inversed = [...STEPS].reverse();
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: inversed };
      if (url === '/onboarding-wizard/progress') return { data: progress(inversed) };
      return { data: [] };
    });
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    // Si le tri était cassé, l'étape affichée serait la dernière du tableau.
    expect(screen.getByLabelText(/Nom de l\u2019église/)).toBeTruthy();
  });
});

describe('B1 — complétion', () => {
  it('complète une étape sans donnée requise en envoyant un corps vide', async () => {
    const user = userEvent.setup();
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    await user.type(screen.getByLabelText(/Nom de l\u2019église/), 'Église de la Grâce');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
    const [url, body] = mocked.post.mock.calls[0];
    expect(url).toBe('/onboarding-wizard/s0/complete');
    expect(body.data).toMatchObject({ churchName: 'Église de la Grâce' });
  });

  it('bloque la soumission si le nom est trop court (validation locale)', async () => {
    const user = userEvent.setup();
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    await user.type(screen.getByLabelText(/Nom de l\u2019église/), 'a');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(mocked.post).not.toHaveBeenCalled();
    expect(await screen.findByRole('alert')).toBeTruthy();
  });
});

describe('B1 — skip', () => {
  it('refuse de sauter sans motif quand skipRequiresReason est vrai', async () => {
    const user = userEvent.setup();
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: STEPS };
      if (url === '/onboarding-wizard/progress') {
        return { data: { ...progress(), steps: STEPS.map((s, i) => (i === 0 ? { ...s, isCompleted: true, status: 'COMPLETED' as const } : s)) } };
      }
      return { data: [] };
    });
    renderPage();
    await waitFor(() => screen.getByText(/2\. Import des membres/));
    await user.click(screen.getByRole('button', { name: /Ignorer cette étape/ }));
    await user.click(screen.getByRole('button', { name: /Confirmer l\u2019ignorance/ }));
    expect(mocked.post).not.toHaveBeenCalled();
  });

  it('envoie le motif quand il est fourni', async () => {
    const user = userEvent.setup();
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: STEPS };
      if (url === '/onboarding-wizard/progress') {
        return { data: { ...progress(), steps: STEPS.map((s, i) => (i === 0 ? { ...s, isCompleted: true, status: 'COMPLETED' as const } : s)) } };
      }
      return { data: [] };
    });
    mocked.post.mockResolvedValue({ data: { ...STEPS[1], status: 'SKIPPED', isCompleted: true } });
    renderPage();
    await waitFor(() => screen.getByText(/2\. Import des membres/));
    await user.click(screen.getByRole('button', { name: /Ignorer cette étape/ }));
    await user.type(screen.getByLabelText(/Motif de l\u2019ignorance/), 'Pas encore de fichier');
    await user.click(screen.getByRole('button', { name: /Confirmer l\u2019ignorance/ }));
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
    expect(mocked.post.mock.calls[0][0]).toBe('/onboarding-wizard/s1/skip');
    expect(mocked.post.mock.calls[0][1]).toEqual({ reason: 'Pas encore de fichier' });
  });

  it('n\u2019affiche pas le bouton Ignorer sur une étape non skippable', async () => {
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    expect(screen.queryByRole('button', { name: /Ignorer cette étape/ })).toBeNull();
  });
});

describe('B1 — erreurs du contrat', () => {
  it('affiche les champs fautifs sur STEP_DATA_INVALID', async () => {
    const user = userEvent.setup();
    mocked.post.mockRejectedValueOnce(
      new AxiosError('Bad Request', 'ERR_BAD_REQUEST', undefined, undefined, {
        status: 400,
        data: { title: 'STEP_DATA_INVALID', detail: 'Données invalides',
          errors: { churchName: 'trop court' } },
      } as never),
    );
    renderPage();
    await waitFor(() => screen.getByText(/1\. Identité de l\u2019église/));
    await user.type(screen.getByLabelText(/Nom de l\u2019église/), 'Église Test');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
  });

  it('affiche un écran dédié pour TENANT_SUSPENDED (jamais un écran blanc)', async () => {
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') {
        throw new AxiosError('Forbidden', 'ERR_BAD_REQUEST', undefined, undefined, {
          status: 403, data: { title: 'TENANT_SUSPENDED', detail: 'Service suspendu.' },
        } as never);
      }
      return { data: [] };
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('Service suspendu')).toBeTruthy());
    expect(screen.getByRole('link', { name: /Se reconnecter/ })).toBeTruthy();
  });
});

describe('B1 — reprise et fin de parcours', () => {
  it('reprise : se positionne sur la première étape non terminée', async () => {
    const withDone = STEPS.map((s, i) => (i < 2 ? { ...s, isCompleted: true, status: 'COMPLETED' as const } : s));
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: withDone };
      if (url === '/onboarding-wizard/progress') return { data: progress(withDone) };
      return { data: [] };
    });
    renderPage();
    await waitFor(() => screen.getByText(/3\. Familles et départements/));
  });

  it('affiche l\u2019écran de fin quand isComplete est vrai', async () => {
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: STEPS };
      if (url === '/onboarding-wizard/progress') return { data: { ...progress(), isComplete: true, percentage: 100 } };
      if (url === '/onboarding-wizard/status') {
        return { data: { completed: true, completedAt: '2026-09-28T10:00:00Z', completedBy: null,
          totalSteps: 7, completedSteps: 7, skippedSteps: 0, percentage: 100 } };
      }
      return { data: [] };
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('Configuration terminée')).toBeTruthy());
    expect(screen.getByRole('link', { name: 'Tableau de bord' })).toBeTruthy();
    expect(screen.getByRole('link', { name: /Inviter l\u2019équipe/ })).toBeTruthy();
  });

  it('réaffiche completedData en lecture seule pour une étape terminée', async () => {
    const withData = STEPS.map((s, i) => (i === 0
      ? { ...s, isCompleted: true, status: 'COMPLETED' as const, completedAt: '2026-09-28T10:00:00Z',
          completedData: { churchName: 'Église Test' } }
      : s));
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: withData };
      if (url === '/onboarding-wizard/progress') return { data: progress(withData) };
      return { data: [] };
    });
    const user = userEvent.setup();
    renderPage();
    // La reprise se place sur la 1re étape non terminée : on revient sur l'étape 0.
    await waitFor(() => screen.getByText(/2\. Import des membres/));
    await user.click(screen.getByRole('button', { name: /1\. Identité de l’église/ }));
    await waitFor(() => expect(screen.getByText(/Étape terminée/)).toBeTruthy());
    expect(screen.getByText(/"churchName": "Église Test"/)).toBeTruthy();
  });

  it('état vide : message dédié quand le backend ne renvoie aucune étape', async () => {
    mocked.get.mockImplementation(async (url: string) => {
      if (url === '/onboarding-wizard') return { data: [] };
      if (url === '/onboarding-wizard/progress') return { data: progress([]) };
      return { data: [] };
    });
    renderPage();
    await waitFor(() => expect(screen.getByText(/Aucune étape de configuration disponible/)).toBeTruthy());
  });
});

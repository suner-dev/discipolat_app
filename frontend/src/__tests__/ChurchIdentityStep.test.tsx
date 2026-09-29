// B1 + G1 — Consommation du catalogue de devises du backend dans l'étape
// « Identité de l'église ».
//
// Ce test verrouille le CONTRAT avec l'API livrée par le backend
// (GET /api/v1/platform/currencies, réponse { standard, count, currencies[] })
// et les 3 états non-négociables : catalogue chargé, chargement, repli.

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ChurchIdentityStep from '@/components/onboarding/steps/ChurchIdentityStep';
import type { OnboardingStep } from '@/types/onboarding';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const api = { get, post: vi.fn(), put: vi.fn(), delete: vi.fn() } as unknown as {
    get: ReturnType<typeof vi.fn>;
  };
  return { default: api, api, getErrorMessage: () => 'Erreur test' };
});

import api from '@/lib/api';
const mockedGet = (api as unknown as { get: ReturnType<typeof vi.fn> }).get;

const STEP: OnboardingStep = {
  id: 's0',
  stepType: 'CHURCH_IDENTITY',
  stepOrder: 0,
  title: 'Identité',
  description: '',
  status: 'PENDING',
  isCompleted: false,
  isSkippable: false,
  skipRequiresReason: false,
  startedAt: null,
  completedAt: null,
  completedData: null,
};

const CATALOG = {
  standard: 'ISO-4217',
  count: 2,
  currencies: [
    { code: 'XAF', name: 'Franc CFA (BEAC)', symbol: 'FCFA', decimals: 0 },
    { code: 'EUR', name: 'Euro', symbol: '€', decimals: 2 },
  ],
};

const suggestions = (listId: string): string[] =>
  Array.from(document.querySelectorAll(`#${listId} option`)).map((o) =>
    (o as HTMLOptionElement).value,
  );

function renderStep() {
  const onSubmit = vi.fn();
  // Le catalogue passe par TanStack Query : sans provider, le composant ne peut
  // pas s'exécuter. `retry: false` pour qu'un rejet ne soit pas masqué par un retry.
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={qc}>
      <ChurchIdentityStep step={STEP} disabled={false} onSubmit={onSubmit} />
    </QueryClientProvider>,
  );
  return { onSubmit };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('B1-G1 — catalogue de devises', () => {
  it('charge GET /platform/currencies et propose les codes ISO', async () => {
    mockedGet.mockResolvedValue({ data: CATALOG });

    renderStep();

    // Un seul appel, sur le bon chemin, et la réponse est consommée telle quelle.
    await waitFor(() => expect(mockedGet).toHaveBeenCalledWith('/platform/currencies'));
    await waitFor(() => expect(suggestions('currency-suggestions')).toContain('XAF'));
    const values = suggestions('currency-suggestions');
    expect(values).toEqual(expect.arrayContaining(['XAF', 'EUR']));
    // Le libellé de l'option porte le symbole et le nom : ce n'est pas une
    // liste de codes nus.
    const fcfa = document.querySelector('#currency-suggestions option[value="XAF"]');
    expect(fcfa?.textContent).toContain('FCFA');
    // Absence de repli : l'interface annonce le catalogue officiel.
    expect(
      await screen.findByText(/liste officielle des devises/i),
    ).toBeTruthy();
  });

  it('annonce le chargement au lieu d\'afficher une liste vide', async () => {
    let resolve: (v: unknown) => void = () => {};
    mockedGet.mockReturnValue(new Promise((r) => { resolve = r; }));

    renderStep();

    expect(await screen.findByText(/chargement de la liste des devises/i)).toBeTruthy();
    resolve({ data: CATALOG });
    await waitFor(() => expect(screen.queryByText(/chargement de la liste/i)).toBeNull());
  });

  it('replie sur une liste minimale et l\'annonce si l\'API échoue', async () => {
    mockedGet.mockRejectedValue(new Error('500'));

    renderStep();

    // L'utilisateur n'est JAMAIS bloqué : la saisie reste possible…
    const input = screen.getByLabelText(/devise/i) as HTMLInputElement;
    expect(input).toBeTruthy();
    expect(input.disabled).toBe(false);
    // …et il est prévenu que la liste est une approximation.
    expect(
      await screen.findByText(/liste des devises indisponible/i),
    ).toBeTruthy();
    await waitFor(() => expect(suggestions('currency-suggestions')).toContain('USD'));
    expect(suggestions('currency-suggestions')).toEqual(
      expect.arrayContaining(['EUR', 'XAF', 'USD']),
    );
  });

  it('envoie la devise choisie, au format exact attendu par le backend', async () => {
    mockedGet.mockResolvedValue({ data: CATALOG });
    const { onSubmit } = renderStep();

    await waitFor(() => expect(suggestions('currency-suggestions')).toContain('XAF'));
    await userEvent.type(screen.getByLabelText(/nom de l/i), 'Église de Douala');
    await userEvent.type(screen.getByLabelText(/devise/i), 'XAF');
    await userEvent.type(screen.getByLabelText(/fuseau horaire/i), 'Africa/Douala');
    await userEvent.click(screen.getByRole('button', { name: /enregistrer/i }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        churchName: 'Église de Douala',
        currency: 'XAF',
        timezone: 'Africa/Douala',
      }),
    );
  });

  it('propose les fuseaux IANA de la plateforme', () => {
    mockedGet.mockResolvedValue({ data: CATALOG });
    renderStep();

    const zoneOptions = suggestions('timezone-suggestions');
    const tz = Intl.supportedValuesOf('timeZone');
    // Le runtime de test Node expose la liste complète ; l'interface doit en
    // faire une suggestion, pas un champ libre déguisé.
    expect(zoneOptions).toEqual(expect.arrayContaining([tz[0], 'Africa/Douala']));
  });
});

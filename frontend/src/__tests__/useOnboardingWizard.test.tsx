// B1 — Tests du hook useOnboardingWizard.
// Objectif imposé par le plan : prouver l'invalidation de cache après chaque
// mutation, et le non-retry des erreurs 4xx (non transitoires).

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { AxiosError } from 'axios';

vi.mock('@/lib/api', () => {
  const get = vi.fn();
  const post = vi.fn();
  return { default: { get, post } };
});

import api from '@/lib/api';
import {
  ONBOARDING_KEYS,
  useCompleteStep,
  useOnboardingSteps,
  useSkipStep,
} from '@/hooks/useOnboardingWizard';

const mocked = api as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

const STEPS = [
  { id: 's0', stepType: 'CHURCH_IDENTITY', stepOrder: 0, title: 'Identité', description: null,
    status: 'PENDING', isCompleted: false, isSkippable: false, skipRequiresReason: false,
    startedAt: null, completedAt: null, completedData: null },
];

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

function wrapperFor(qc: QueryClient) {
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mocked.get.mockResolvedValue({ data: STEPS });
  mocked.post.mockResolvedValue({ data: STEPS[0] });
});
afterEach(() => vi.restoreAllMocks());

describe('useOnboardingWizard — lectures', () => {
  it('useOnboardingSteps interroge le bon endpoint', async () => {
    const qc = makeClient();
    renderHook(() => useOnboardingSteps(), { wrapper: wrapperFor(qc) });
    await waitFor(() => expect(mocked.get).toHaveBeenCalledWith('/onboarding-wizard'));
    expect(qc.getQueryData(ONBOARDING_KEYS.steps)).toHaveLength(1);
  });
});

describe('useOnboardingWizard — invalidation de cache', () => {
  it('useCompleteStep invalide steps, progress ET status', async () => {
    const qc = makeClient();
    // On pré-peuple les 3 caches pour prouve qu'ils sont invalidés.
    qc.setQueryData(ONBOARDING_KEYS.steps, STEPS);
    qc.setQueryData(ONBOARDING_KEYS.progress, { totalSteps: 7, percentage: 0 });
    qc.setQueryData(ONBOARDING_KEYS.status, { completed: false });

    const invalidated: string[][] = [];
    const spy = vi.spyOn(qc, 'invalidateQueries').mockImplementation(((q?: unknown) => {
      const key = (q as { queryKey?: string[] } | undefined)?.queryKey ?? [];
      invalidated.push(key);
      return Promise.resolve();
    }) as unknown as typeof qc.invalidateQueries);

    const { result } = renderHook(() => useCompleteStep(), { wrapper: wrapperFor(qc) });
    result.current.mutate({ stepId: 's0', body: { data: { churchName: 'X' } } });
    await waitFor(() => expect(spy).toHaveBeenCalled());

    const keys = invalidated.map((k) => k[0]);
    expect(keys).toContain('onboarding-wizard');
    expect(keys).toContain('onboarding-status');
  });

  it('useSkipStep invalide également le cache', async () => {
    const qc = makeClient();
    const spy = vi.spyOn(qc, 'invalidateQueries').mockResolvedValue(undefined);
    const { result } = renderHook(() => useSkipStep(), { wrapper: wrapperFor(qc) });
    result.current.mutate({ stepId: 's0', body: { reason: 'plus tard' } });
    await waitFor(() => expect(spy).toHaveBeenCalled());
    const keys = spy.mock.calls.map((c) => (c[0] as unknown as { queryKey: string[] }).queryKey[0]);
    expect(keys).toContain('onboarding-wizard');
  });
});

describe('useOnboardingWizard — contrat HTTP', () => {
  it('complete envoie {} quand aucun data n\u2019est fourni (D7)', async () => {
    const qc = makeClient();
    const { result } = renderHook(() => useCompleteStep(), { wrapper: wrapperFor(qc) });
    result.current.mutate({ stepId: 's0' });
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
    expect(mocked.post).toHaveBeenCalledWith('/onboarding-wizard/s0/complete', {});
  });

  it('skip envoie le motif quand il est fourni', async () => {
    const qc = makeClient();
    const { result } = renderHook(() => useSkipStep(), { wrapper: wrapperFor(qc) });
    result.current.mutate({ stepId: 's0', body: { reason: 'plus tard' } });
    await waitFor(() => expect(mocked.post).toHaveBeenCalled());
    expect(mocked.post).toHaveBeenCalledWith('/onboarding-wizard/s0/skip', { reason: 'plus tard' });
  });
});

describe('useOnboardingWizard — erreurs', () => {
  it('ne rejoue PAS une erreur 4xx (TENANT_SUSPENDED n\u2019est pas transitoire)', async () => {
    mocked.get.mockRejectedValue(new AxiosError('Forbidden', undefined, undefined, undefined, {
      status: 403, data: { title: 'TENANT_SUSPENDED' },
    } as never));

    const qc = makeClient();
    const { result } = renderHook(() => useOnboardingSteps(), { wrapper: wrapperFor(qc) });
    await waitFor(() => expect(result.current.isError).toBe(true), { timeout: 4000 });
    // 1 seul appel : pas de replay.
    expect(mocked.get).toHaveBeenCalledTimes(1);
  });

  it('rejoue une erreur réseau (5xx / pas de réponse)', async () => {
    mocked.get.mockRejectedValue(new AxiosError('Server', undefined, undefined, undefined, {
      status: 500, data: {},
    } as never));

    const qc = makeClient();
    const { result } = renderHook(() => useOnboardingSteps(), { wrapper: wrapperFor(qc) });
    await waitFor(() => expect(result.current.isError).toBe(true), { timeout: 6000 });
    // 2 appels : l'initial + 1 retry.
    expect(mocked.get.mock.calls.length).toBeGreaterThan(1);
  });
});

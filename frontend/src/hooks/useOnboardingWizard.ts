// B1 — Accès au wizard d'onboarding (contrat §3.1).
//
// Règles : aucun champ hors contrat, aucun endpoint inventé (§3), et
// invalidation systématique du cache pour que la bannière (B2) et la page
// restent cohérentes après chaque mutation.

import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import api from '@/lib/api';
import type {
  CompleteStepBody,
  OnboardingProgress,
  OnboardingStatus,
  OnboardingStep,
  SkipStepBody,
} from '@/types/onboarding';

export const ONBOARDING_KEYS = {
  all: ['onboarding-wizard'] as const,
  steps: ['onboarding-wizard', 'steps'] as const,
  progress: ['onboarding-wizard', 'progress'] as const,
  status: ['onboarding-status'] as const,
};

function invalidate(qc: ReturnType<typeof useQueryClient>) {
  // Le préfixe 'onboarding-wizard' couvre steps + progress.
  qc.invalidateQueries({ queryKey: ONBOARDING_KEYS.all });
  qc.invalidateQueries({ queryKey: ONBOARDING_KEYS.status });
}

/**
 * Ne rejoue PAS une erreur 4xx : un 403 TENANT_SUSPENDED ou un 404 n'est pas
 * transitoire, et réessayer retarderait l'affichage de l'écran d'erreur
 * (~1 s de délai par tentative) sans jamais changer le résultat.
 */
function shouldRetry(failureCount: number, error: unknown): boolean {
  const status = (error as AxiosError | undefined)?.response?.status;
  if (status !== undefined && status >= 400 && status < 500) return false;
  return failureCount < 1;
}

export function useOnboardingSteps() {
  return useQuery({
    queryKey: ONBOARDING_KEYS.steps,
    queryFn: async () => (await api.get<OnboardingStep[]>('/onboarding-wizard')).data,
    // Le backend initialise les étapes au premier GET : une donnée vide est
    // un état transitoire légitime, pas une erreur.
    retry: shouldRetry,
  });
}

export function useOnboardingProgress() {
  return useQuery({
    queryKey: ONBOARDING_KEYS.progress,
    queryFn: async () => (await api.get<OnboardingProgress>('/onboarding-wizard/progress')).data,
    placeholderData: keepPreviousData,
  });
}

export function useOnboardingStatus() {
  return useQuery({
    queryKey: ONBOARDING_KEYS.status,
    queryFn: async () => (await api.get<OnboardingStatus>('/onboarding-wizard/status')).data,
    // D8 : la bannière ne doit jamais provoquer de boucle ni bloquer l'app.
    retry: 0,
    staleTime: 30_000,
  });
}

export function useInitializeOnboarding() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => (await api.post<OnboardingStep[]>('/onboarding-wizard/initialize')).data,
    onSuccess: () => invalidate(qc),
  });
}

export function useStartStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (stepId: string) =>
      (await api.post<OnboardingStep>(`/onboarding-wizard/${stepId}/start`)).data,
    onSuccess: () => invalidate(qc),
  });
}

/**
 * D7 : le corps est facultatif. Pour une étape sans donnée requise, on peut
 * n'envoyer aucun corps du tout — l'envoi de `{}` reste valide.
 */
export function useCompleteStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ stepId, body }: { stepId: string; body?: CompleteStepBody }) =>
      (
        await api.post<OnboardingStep>(`/onboarding-wizard/${stepId}/complete`, body ?? {})
      ).data,
    onSuccess: () => invalidate(qc),
  });
}

export function useSkipStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ stepId, body }: { stepId: string; body?: SkipStepBody }) =>
      (await api.post<OnboardingStep>(`/onboarding-wizard/${stepId}/skip`, body ?? {})).data,
    onSuccess: () => invalidate(qc),
  });
}

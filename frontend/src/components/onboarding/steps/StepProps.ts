// Contrat commun à tous les formulaires d'étape du wizard (B1).
import type { OnboardingStep } from '@/types/onboarding';

export interface StepProps {
  step: OnboardingStep;
  disabled: boolean;
  /** Soumet un data conforme à la colonne « data acceptée » du contrat §3.1. */
  onSubmit: (data: Record<string, unknown>) => void;
}

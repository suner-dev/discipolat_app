// B1 — Contrat de données du wizard d'onboarding.
// Source de vérité : PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md §3.1.
// Les noms de champs sont FIGÉS (R2) : ne jamais les renommer.

/** Les 7 étapes canoniques, dans l'ordre du backend (OnboardingWizardStep.StepType). */
export type OnboardingStepType =
  | 'CHURCH_IDENTITY'
  | 'MEMBER_IMPORT'
  | 'STRUCTURE'
  | 'ROLES'
  | 'BRANDING'
  | 'MODULES'
  | 'FIRST_EVENT';

export const ONBOARDING_STEP_TYPES: OnboardingStepType[] = [
  'CHURCH_IDENTITY',
  'MEMBER_IMPORT',
  'STRUCTURE',
  'ROLES',
  'BRANDING',
  'MODULES',
  'FIRST_EVENT',
];

export type OnboardingStepStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED';

/** Codes métier du contrat §3.1 — utilisés pour l'affichage sélectif des erreurs. */
export type OnboardingErrorCode =
  | 'STEP_NOT_FOUND'
  | 'STEP_DATA_INVALID'
  | 'STEP_ORDER_VIOLATION'
  | 'STEP_ALREADY_COMPLETED'
  | 'STEP_NOT_SKIPPABLE'
  | 'STEP_SKIP_REASON_REQUIRED'
  | 'STEP_PRECONDITION_FAILED'
  | 'TENANT_SUSPENDED'
  | 'TENANT_CANCELLED'
  | 'TENANT_STATUS_UNAVAILABLE';

export interface OnboardingStep {
  id: string;
  stepType: OnboardingStepType;
  stepOrder: number;
  /** Titre FR fourni par l'API (D5). Le web peut le remplacer par sa clé i18n. */
  title: string;
  description: string | null;
  status: OnboardingStepStatus;
  isCompleted: boolean;
  isSkippable: boolean;
  skipRequiresReason: boolean;
  startedAt: string | null;
  completedAt: string | null;
  /** Objet JSON (désérialisé côté serveur), jamais une chaîne brute. */
  completedData: Record<string, unknown> | null;
}

export interface OnboardingProgress {
  totalSteps: number;
  completedSteps: number;
  skippedSteps: number;
  percentage: number;
  isComplete: boolean;
  steps: OnboardingStep[];
}

export interface OnboardingStatus {
  completed: boolean;
  completedAt: string | null;
  completedBy: string | null;
  totalSteps: number;
  completedSteps: number;
  skippedSteps: number;
  percentage: number;
}

// ── Données d'étape (contrat §3.1, colonne « data acceptée ») ───────────────

export interface ChurchIdentityData {
  churchName: string;
  businessName?: string;
  city?: string;
  phone?: string;
  email?: string;
  timezone?: string;
  currency?: string;
}

export interface MemberImportData {
  importedCount: number;
}

export interface StructureData {
  departments?: string[];
  families?: string[];
}

export interface RoleInvitation {
  email: string;
  role: string;
}

export interface RolesData {
  invitations?: RoleInvitation[];
}

export interface BrandingData {
  primaryColor?: string;
  logoUrl?: string;
  allowDarkMode?: boolean;
}

export interface ModulesData {
  modules: string[];
}

export interface FirstEventData {
  title: string;
  /** ISO-8601, doit être dans le futur. */
  startAt: string;
  location?: string;
}

/** Corps envoyé à POST /onboarding-wizard/{id}/complete — toujours facultatif côté serveur. */
export interface CompleteStepBody {
  data?: Record<string, unknown>;
}

export interface SkipStepBody {
  reason?: string;
}

/** Erreur RFC 7807 renvoyée par GlobalExceptionHandler (title = code métier). */
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  /** Présent pour STEP_DATA_INVALID : liste des champs fautifs. */
  errors?: Record<string, string>;
}

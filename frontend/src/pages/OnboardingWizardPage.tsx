// B1 — Wizard d'onboarding : orchestration des 7 étapes (contrat §3.1).
//
// Réécriture complète : l'ancienne version typait l'étape avec `order` (champ
// absent du contrat) et ne gérait ni chargement, ni vide, ni erreur, ni reprise.
//
// §5.0 : les 5 états (chargement / vide / erreur / succès / hors-ligne) sont
// tous traités, et l'avancement vient de GET /progress (source de vérité).

import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Rocket, CheckCircle2, AlertTriangle, LogOut } from 'lucide-react';
import { AxiosError } from 'axios';
import toast from 'react-hot-toast';
import { useI18n, tText } from '@/i18n';
import { getErrorMessage } from '@/lib/api';
import {
  useCompleteStep,
  useOnboardingProgress,
  useOnboardingStatus,
  useOnboardingSteps,
  useSkipStep,
  useStartStep,
} from '@/hooks/useOnboardingWizard';
import type { OnboardingStepType, ProblemDetail } from '@/types/onboarding';
import OnboardingStepper from '@/components/onboarding/OnboardingStepper';
import { SkeletonCard } from '@/components/ui/UXComponents';
import ChurchIdentityStep from '@/components/onboarding/steps/ChurchIdentityStep';
import MemberImportStep from '@/components/onboarding/steps/MemberImportStep';
import StructureStep from '@/components/onboarding/steps/StructureStep';
import RolesStep from '@/components/onboarding/steps/RolesStep';
import BrandingStep from '@/components/onboarding/steps/BrandingStep';
import ModulesStep from '@/components/onboarding/steps/ModulesStep';
import FirstEventStep from '@/components/onboarding/steps/FirstEventStep';
import type { StepProps } from '@/components/onboarding/steps/StepProps';

function renderStep(props: StepProps, type: OnboardingStepType) {
  switch (type) {
    case 'CHURCH_IDENTITY': return <ChurchIdentityStep {...props} />;
    case 'MEMBER_IMPORT': return <MemberImportStep {...props} />;
    case 'STRUCTURE': return <StructureStep {...props} />;
    case 'ROLES': return <RolesStep {...props} />;
    case 'BRANDING': return <BrandingStep {...props} />;
    case 'MODULES': return <ModulesStep {...props} />;
    case 'FIRST_EVENT': return <FirstEventStep {...props} />;
  }
}

function problemOf(err: unknown): ProblemDetail | null {
  if (err instanceof AxiosError) {
    const d = err.response?.data as ProblemDetail | undefined;
    return d && typeof d === 'object' ? d : null;
  }
  return null;
}

export default function OnboardingWizardPage() {
  const { t } = useI18n();
  const stepsQ = useOnboardingSteps();
  const progressQ = useOnboardingProgress();
  const statusQ = useOnboardingStatus();
  const start = useStartStep();
  const complete = useCompleteStep();
  const skip = useSkipStep();

  const [index, setIndex] = useState(0);
  const [skipping, setSkipping] = useState(false);
  const [reason, setReason] = useState('');

  const steps = useMemo(() => {
    const fromProgress = progressQ.data?.steps;
    const source = fromProgress?.length ? fromProgress : stepsQ.data;
    return source ? [...source].sort((a, b) => a.stepOrder - b.stepOrder) : [];
  }, [progressQ.data?.steps, stepsQ.data]);

  // Reprise : on se positionne sur la première étape non terminée.
  useEffect(() => {
    if (steps.length === 0) return;
    const firstOpen = steps.findIndex((s) => !s.isCompleted);
    if (firstOpen >= 0) setIndex(firstOpen);
  }, [steps.length]); // eslint-disable-line react-hooks/exhaustive-deps

  const current = steps[index];
  const isBusy = start.isPending || complete.isPending || skip.isPending;

  // Écrans de statut.
  if (stepsQ.isLoading) {
    return (
      <div className="page-container" aria-busy="true">
        <div className="space-y-4"><SkeletonCard /><SkeletonCard /></div>
      </div>
    );
  }

  if (stepsQ.isError) {
    const p = problemOf(stepsQ.error);
    // TENANT_SUSPENDED : message dédié, jamais un écran blanc.
    if (p?.title === 'TENANT_SUSPENDED' || p?.title === 'TENANT_CANCELLED') {
      return (
        <div className="page-container">
          <div className="glass-card p-8 text-center border-red-500/40">
            <AlertTriangle className="w-10 h-10 mx-auto text-amber-500 mb-3" aria-hidden="true" />
            <h1 className="text-lg font-bold mb-2">{t('Service suspendu')}</h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              {p.detail ?? tText('Le service de cette église est suspendu. Contactez le support Discipolat.')}
            </p>
            <Link to="/login" className="btn-primary btn-sm inline-flex items-center gap-2 min-h-[44px]">
              <LogOut className="w-4 h-4" aria-hidden="true" /> {t('Se reconnecter')}
            </Link>
          </div>
        </div>
      );
    }
    return (
      <div className="page-container">
        <div className="glass-card p-6 border-red-500/40" role="alert">
          <p className="text-sm text-red-500 mb-3">{getErrorMessage(stepsQ.error)}</p>
          <button onClick={() => stepsQ.refetch()} className="btn-sm underline min-h-[44px]">
            {tText('Réessayer')}
          </button>
        </div>
      </div>
    );
  }

  if (steps.length === 0) {
    return (
      <div className="page-container">
        <div className="glass-card p-10 text-center text-gray-500">
          {tText('Aucune étape de configuration disponible.')}
        </div>
      </div>
    );
  }

  const handleComplete = (data: Record<string, unknown>) => {
    if (!current) return;
    // D7 : corps facultatif. MEMBER_IMPORT/ROLES exigent des données, les
    // autres étapes acceptent un corps vide.
    const needsData = current.stepType === 'MEMBER_IMPORT' || current.stepType === 'ROLES'
      || current.stepType === 'STRUCTURE' || current.stepType === 'FIRST_EVENT';
    complete.mutate(
      { stepId: current.id, body: needsData ? { data } : (Object.keys(data).length ? { data } : {}) },
      {
        onSuccess: (updated) => {
          toast.success(tText('Étape enregistrée.'));
          const next = steps.findIndex((s) => s.stepOrder > updated.stepOrder && !s.isCompleted);
          setIndex(next >= 0 ? next : Math.min(index + 1, steps.length - 1));
          setSkipping(false);
        },
        onError: (err) => {
          const p = problemOf(err);
          if (p?.title === 'STEP_DATA_INVALID') {
            const details = p.errors
              ? Object.entries(p.errors).map(([f, m]) => `${f} : ${m}`).join(' — ')
              : (p.detail ?? '');
            toast.error(`${p.title}${details ? ` — ${details}` : ''}`);
          } else if (p?.title === 'TENANT_SUSPENDED') {
            toast.error(p.detail ?? tText('Le service de cette église est suspendu.'));
          } else {
            toast.error(p ? `${p.title} — ${p.detail ?? ''}`.trim() : getErrorMessage(err));
          }
        },
      },
    );
  };

  const handleSkip = () => {
    if (!current) return;
    if (current.skipRequiresReason && reason.trim().length === 0) {
      toast.error(tText('Un motif est obligatoire pour ignorer cette étape.'));
      return;
    }
    skip.mutate(
      { stepId: current.id, body: reason.trim() ? { reason: reason.trim() } : undefined },
      {
        onSuccess: () => {
          toast.success(tText('Étape ignorée.'));
          setReason(''); setSkipping(false);
        },
        onError: (err) => {
          const p = problemOf(err);
          toast.error(p ? `${p.title} — ${p.detail ?? ''}`.trim() : getErrorMessage(err));
        },
      },
    );
  };

  const percentage = progressQ.data?.percentage ?? 0;
  const isComplete = progressQ.data?.isComplete ?? false;

  if (isComplete || statusQ.data?.completed) {
    return (
      <div className="page-container">
        <div className="glass-card p-10 text-center">
          <CheckCircle2 className="w-12 h-12 mx-auto text-emerald-500 mb-4" aria-hidden="true" />
          <h1 className="text-xl font-bold mb-2">{t('Configuration terminée')}</h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-6">
            {tText('Votre église est prête. Vous pouvez encore ajuster les réglages à tout moment.')}
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link to="/dashboard" className="btn-primary btn-sm min-h-[44px] px-4 inline-flex items-center">
              {t('Tableau de bord')}
            </Link>
            <Link to="/users" className="btn-sm rounded-lg border border-gray-300 dark:border-gray-700 px-4 min-h-[44px] inline-flex items-center">
              {t('Inviter l\u2019équipe')}
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white shadow-lg">
          <Rocket className="w-6 h-6" aria-hidden="true" />
        </div>
        <div>
          <h1 className="page-title">{tText('Assistant de configuration')}</h1>
          <p className="page-subtitle">{tText('Complétez les étapes pour démarrer sur la plateforme')}</p>
        </div>
      </div>

      <div className="glass-card p-4 mb-6">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-gray-500">{t('Progression')}</span>
          <span className="text-sm font-medium text-gray-800 dark:text-gray-200">
            {progressQ.data
              ? `${progressQ.data.completedSteps + progressQ.data.skippedSteps}/${progressQ.data.totalSteps} (${percentage}%)`
              : `—`}
          </span>
        </div>
        <div
          className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"
          role="progressbar" aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100}
          aria-label={t('Progression de la configuration')}
        >
          <div
            className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full transition-[width] duration-300"
            style={{ width: `${percentage}%` }}
          />
        </div>
      </div>

      <OnboardingStepper steps={steps} current={index} onStepClick={setIndex} />

      {current && (
        <div className="max-w-3xl">
          <h2 className="text-lg font-semibold mb-1">{t(current.title)}</h2>
          {current.description && (
            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">{current.description}</p>
          )}

          {current.isCompleted ? (
            <div className="glass-card p-5">
              <p className="text-sm font-medium text-emerald-600 dark:text-emerald-400 mb-1">
                {current.status === 'SKIPPED' ? t('Étape ignorée') : t('Étape terminée')}
                {current.completedAt &&
                  ` — ${new Date(current.completedAt).toLocaleDateString(undefined, { dateStyle: 'long' })}`}
              </p>
              {current.completedData && Object.keys(current.completedData).length > 0 && (
                <pre className="mt-3 text-xs bg-black/5 dark:bg-white/5 rounded p-3 overflow-x-auto">
                  {JSON.stringify(current.completedData, null, 2)}
                </pre>
              )}
            </div>
          ) : (
            <>
              {renderStep({ step: current, disabled: isBusy, onSubmit: handleComplete }, current.stepType)}

              {current.isSkippable && (
                <div className="mt-4">
                  {!skipping ? (
                    <button
                      onClick={() => setSkipping(true)}
                      disabled={isBusy}
                      className="btn-sm underline min-h-[44px]"
                    >
                      {tText('Ignorer cette étape')}
                    </button>
                  ) : (
                    <div className="glass-card p-4 space-y-3">
                      <label htmlFor="skipReason" className="block text-sm font-medium">
                        {t('Motif de l\u2019ignorance')}
                        {current.skipRequiresReason && <span className="text-red-400"> *</span>}
                      </label>
                      <textarea
                        id="skipReason" value={reason} onChange={(e) => setReason(e.target.value)}
                        disabled={isBusy} rows={2}
                        required={current.skipRequiresReason}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2"
                      />
                      <div className="flex gap-2">
                        <button
                          onClick={handleSkip} disabled={isBusy}
                          className="btn-primary btn-sm min-h-[44px]"
                        >
                          {tText('Confirmer l\u2019ignorance')}
                        </button>
                        <button
                          onClick={() => { setSkipping(false); setReason(''); }}
                          disabled={isBusy} className="btn-sm underline min-h-[44px]"
                        >
                          {t('Annuler')}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}

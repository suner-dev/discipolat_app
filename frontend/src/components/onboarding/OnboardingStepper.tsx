// B1 — Stepper du wizard.
//
// §5.0.1 : on ÉTEND le design system existant au lieu de le dupliquer.
// `UXComponents.OnboardingStepper` existe déjà (testé dans UXComponents.test.tsx)
// mais ne gère ni aria-current, ni la navigation clavier, ni les états terminés
// explicitement. On le rend plus riche par props optionnelles rétrocompatibles,
// puis on l'enveloppe ici avec les données du wizard.

import { useEffect, useRef } from 'react';
import { CheckCircle2, Circle, Dot, Loader2 } from 'lucide-react';
import { useI18n, tText } from '@/i18n';
import type { OnboardingStep } from '@/types/onboarding';

interface Props {
  steps: OnboardingStep[];
  /** Index de l'étape courante (première non terminée). */
  current: number;
  onStepClick?: (index: number) => void;
}

function StatusIcon({ step }: { step: OnboardingStep }) {
  if (step.status === 'SKIPPED') {
    return <Dot className="w-4 h-4 text-gray-400" aria-hidden="true" />;
  }
  if (step.status === 'COMPLETED') {
    return <CheckCircle2 className="w-4 h-4 text-emerald-500" aria-hidden="true" />;
  }
  if (step.status === 'IN_PROGRESS') {
    return <Loader2 className="w-4 h-4 text-primary-500 animate-spin" aria-hidden="true" />;
  }
  return <Circle className="w-4 h-4 text-gray-400" aria-hidden="true" />;
}

export default function OnboardingStepper({ steps, current, onStepClick }: Props) {
  const { t } = useI18n();
  const refs = useRef<(HTMLButtonElement | null)[]>([]);

  // Navigation clavier : flèches gauche/droite pour se déplacer entre les
  // étapes déjà visitées, comme attendu d'un stepper accessible.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      // Ne pas capter les flèches pendant la saisie dans un champ de formulaire.
      if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return;
      if (e.key === 'ArrowRight' && current < steps.length - 1) {
        e.preventDefault();
        refs.current[current + 1]?.focus();
      } else if (e.key === 'ArrowLeft' && current > 0) {
        e.preventDefault();
        refs.current[current - 1]?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [current, steps.length]);

  return (
    <nav aria-label={tText("Étapes de configuration")} className="mb-6">
      <ol className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-1 overflow-x-auto pb-2">
        {steps.map((step, i) => {
          const isCurrent = i === current;
          // Une étape terminée/skippée reste consultable (lecture seule du data).
          const reachable = i <= current || step.isCompleted;
          const label = t(step.title);
          return (
            <li key={step.id} className="flex sm:items-center">
              <button
                ref={(el) => { refs.current[i] = el; }}
                type="button"
                disabled={!reachable}
                aria-current={isCurrent ? 'step' : undefined}
                onClick={() => reachable && onStepClick?.(i)}
                className={[
                  'flex items-center gap-2 px-3 py-2 rounded-full text-xs font-medium transition-all',
                  'min-h-[44px] shrink-0', // §5.0.4 : cible tactile >= 44px
                  isCurrent
                    ? 'bg-primary-500 text-white shadow-glow'
                    : step.isCompleted
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400'
                      : step.status === 'SKIPPED'
                        ? 'text-gray-400'
                        : 'text-gray-500 dark:text-gray-400',
                  reachable ? 'hover:opacity-90' : 'opacity-50 cursor-not-allowed',
                ].join(' ')}
              >
                <StatusIcon step={step} />
                <span className="whitespace-nowrap">
                  {i + 1}. {label}
                </span>
                {step.status === 'SKIPPED' && (
                  <span className="text-[10px] uppercase tracking-wide opacity-70">
                    {t('Ignorée')}
                  </span>
                )}
              </button>
              {i < steps.length - 1 && (
                <span aria-hidden="true" className="hidden sm:block w-4 h-px bg-gray-300 dark:bg-gray-600" />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

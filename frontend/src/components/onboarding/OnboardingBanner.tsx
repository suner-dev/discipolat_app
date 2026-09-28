// B2 — Bannière d'onboarding post-connexion.
//
// DÉCISION D8 (non négociable) : la bannière est **informative**, jamais
// bloquante. Aucun `navigate()` automatique n'est déclenché ici.
//
// Silence par conception (cf. plan B2) :
//   - si GET /onboarding-wizard/status échoue  -> bannière masquée, aucun retry,
//     aucune boucle (une bannière qui hurle sur un réseau instable est pire
//     qu'une bannière absente) ;
//   - l'utilisateur peut la dismisser pour la session courante (sessionStorage,
//     donc réellement "pour la session" et pas seulement jusqu'au rechargement).

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Rocket, X } from 'lucide-react';
import { useI18n, tText } from '@/i18n';
import { useOnboardingStatus } from '@/hooks/useOnboardingWizard';

const DISMISS_KEY = 'discipolat:onboarding-banner-dismissed';

export default function OnboardingBanner() {
  const { t } = useI18n();
  // Le hook a déjà retry: 0 et staleTime : ne pas redemander inutilement.
  const { data, isError } = useOnboardingStatus();
  const [dismissed, setDismissed] = useState<boolean>(() => {
    try {
      return sessionStorage.getItem(DISMISS_KEY) === '1';
    } catch {
      return false; // navigation privée / storage indisponible
    }
  });

  const dismiss = () => {
    setDismissed(true);
    try {
      sessionStorage.setItem(DISMISS_KEY, '1');
    } catch {
      /* stockage indisponible : l'état React suffit pour la session courante */
    }
  };

  if (isError) return null;
  if (!data || data.completed) return null;
  if (dismissed) return null;

  const remaining = data.totalSteps - (data.completedSteps + data.skippedSteps);

  return (
    <div className="mx-4 sm:mx-6 lg:mx-8 mt-4 animate-slide-up">
      <div
        className="flex items-center gap-3 px-4 py-2.5 rounded-xl border border-emerald-500/30 bg-gradient-to-r from-emerald-600/15 via-emerald-500/10 to-transparent"
        role="status"
        aria-live="polite"
      >
        <Rocket className="w-4 h-4 text-emerald-500 flex-shrink-0" aria-hidden="true" />
        <p className="text-xs text-emerald-700 dark:text-emerald-300 flex-1">
          <span className="font-semibold">{tText('Configuration incomplète')}</span>
          {' — '}
          {/* Chaînes FIXES, jamais interpolées : `tText` indexe par valeur exacte
              (§5.0.5). Interpoler le nombre produirait une chaîne absente de
              fr.ts et donc non traduite dans les 5 autres locales. Le nombre
              exact est déjà visible dans la barre de progression du wizard. */}
          {remaining > 0
            ? t('onboarding.banner.remainingSteps')
            : tText('Dernière étape en cours : finalisez la configuration.')}
        </p>
        <Link
          to="/onboarding-wizard"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 text-white text-xs font-medium hover:bg-emerald-700 flex-shrink-0 min-h-[44px]"
        >
          {tText('Continuer')}
        </Link>
        <button
          type="button"
          onClick={dismiss}
          aria-label={tText('Masquer cette bannière')}
          className="flex-shrink-0 p-2 rounded-lg hover:bg-black/5 dark:hover:bg-white/10 min-h-[44px] min-w-[44px] flex items-center justify-center"
        >
          <X className="w-3.5 h-3.5" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}

import { useEffect, useSyncExternalStore, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n';
import {
  isFeatureEnabled,
  resolveFeature,
  resolveFeatureLabel,
  resolveLabel,
  setUiCustomization,
  subscribeUiCustomization,
  type UiFeatureSetting,
} from './uiCustomization';

/**
 * LOT 2 §LB — accès React au paramétrage fin de l'interface.
 *
 * <p>Le magasin lui-même est module-level (voir {@link ./uiCustomization}) pour
 * que la fonction {@code t()} puisse le consulter sans être un composant. Ces
 * hooks ne font que l'abonner à React.
 */

/**
 * Charge les surcharges de l'église et les installe dans le magasin.
 *
 * <p>À monter une seule fois, haut dans l'arbre (fait dans `MainLayout`).
 * Tant que la requête n'a pas répondu, le magasin reste vide et
 * l'application se comporte exactement comme avant : aucun risque de casser
 * l'affichage si l'API est lente ou indisponible.
 */
export function useUiCustomizationLoader(enabled = true): void {
  const { user } = useAuth();
  const { locale } = useI18n();

  const { data } = useQuery({
    queryKey: ['ui-customization', user?.id ?? null, locale],
    queryFn: async () => {
      const res = await api.get('/tenant/customization', { params: { locale } });
      return {
        labels: (res.data?.labels ?? {}) as Record<string, string>,
        features: (res.data?.features ?? {}) as Record<string, UiFeatureSetting>,
      };
    },
    // Le paramétrage bouge rarement : 10 min évitent un aller-retour à chaque
    // changement de langue tout en gardant un délai de rafraîchissement court.
    staleTime: 10 * 60 * 1000,
    retry: 1,
    enabled: enabled && !!user,
  });

  useEffect(() => {
    if (data) setUiCustomization(data);
  }, [data]);
}

/** Force le re-rendu d'un composant lorsque les surcharges changent. */
function useCustomizationVersion(): number {
  return useSyncExternalStore(
    subscribeUiCustomization,
    () => 1,
    () => 1,
  );
}

/** Surcharge d'un libellé, ou `undefined` si l'administration n'a rien changé. */
export function useUiLabel(key: string): string | undefined {
  useCustomizationVersion();
  return resolveLabel(key);
}

/**
 * La fonctionnalité est-elle active sur cet écran ?
 *
 * <p>Défaut <b>ouvert</b> : sans réglage, elle est active. L'administrateur
 * écrit donc « je désactive ce bouton » — et non « j'active 200 boutons ».
 */
export function useFeatureEnabled(pageKey: string, featureKey: string): boolean {
  useCustomizationVersion();
  return isFeatureEnabled(pageKey, featureKey);
}

/** Libellé personnalisé d'un bouton, ou `undefined`. */
export function useFeatureLabel(pageKey: string, featureKey: string): string | undefined {
  useCustomizationVersion();
  return resolveFeatureLabel(pageKey, featureKey);
}

interface FeatureGateProps {
  pageKey: string;
  featureKey: string;
  children: ReactNode;
  /** Rendu quand la fonctionnalité est retirée (par défaut : rien). */
  fallback?: ReactNode;
}

/**
 * Rend ses enfants seulement si la fonctionnalité est active sur l'écran.
 *
 * <p>C'est la brique qui permet de « choisir les fonctionnalités à ajouter sur
 * telle page ou en enlever » sans écrire de code par la suite.
 */
export function FeatureGate({ pageKey, featureKey, children, fallback = null }: FeatureGateProps) {
  const enabled = useFeatureEnabled(pageKey, featureKey);
  return <>{enabled ? children : fallback}</>;
}

export { resolveFeature, resolveFeatureLabel, resolveLabel };
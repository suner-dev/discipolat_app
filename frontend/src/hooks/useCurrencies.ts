// Consommation du catalogue de devises livré par le backend
// (GET /api/v1/platform/currencies, ISO-4217).
//
// Pourquoi un schéma zod ici : c'est la **frontière réseau**. Le reste de la
// base de code attend des types, pas des `any` : si le backend change la forme
// de la réponse, l'échec doit être **visible et testable**, pas se manifester
// par un `undefined` rendu dans un sélecteur trois écrans plus loin.
//
// D12 : aucune valeur n'est **imposée**. En cas d'indisponibilité du catalogue,
// on retombe sur une liste minimale documentée et l'interface l'annonce — la
// saisie reste libre, donc l'utilisateur n'est jamais bloqué.

import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import api from '@/lib/api';

const CurrencySchema = z.object({
  code: z.string().length(3),
  name: z.string().min(1),
  symbol: z.string().min(1),
  decimals: z.number().int().min(0).max(4),
});

const CurrencyCatalogSchema = z.object({
  standard: z.string(),
  count: z.number(),
  currencies: z.array(CurrencySchema),
});

export type Currency = z.infer<typeof CurrencySchema>;

/**
 * Repli documentaire : les trois devises que le produit sait afficher et
 * formater (format XOF à 0 décimale inclus). Volontairement minimal — c'est
 * un filet de sécurité, pas une liste de référence.
 */
export const FALLBACK_CURRENCIES: readonly Currency[] = [
  { code: 'EUR', name: 'Euro', symbol: '€', decimals: 2 },
  { code: 'XAF', name: 'Franc CFA (BEAC)', symbol: 'FCFA', decimals: 0 },
  { code: 'USD', name: 'Dollar des États-Unis', symbol: '$', decimals: 2 },
];

export const CURRENCIES_QUERY_KEY = ['platform', 'currencies'] as const;

export function useCurrencies() {
  const query = useQuery({
    queryKey: CURRENCIES_QUERY_KEY,
    queryFn: async () => {
      const response = await api.get('/platform/currencies');
      // Parse = la réponse est typée par le schéma, pas par un `as`.
      return CurrencyCatalogSchema.parse(response.data).currencies;
    },
    // Catalogue statique : inutile de le re-demander à chaque écran.
    staleTime: 60 * 60 * 1000,
    // `retry` n'est PAS forcé ici : la politique globale de l'application
    // (main.tsx) et celle du test restent respectées. Un hook qui écrase le
    // `retry` du client rend le comportement non testable et non pilotable.
  });

  return {
    currencies: query.data ?? FALLBACK_CURRENCIES,
    /** Le catalogue du backend est-il réellement chargé ? */
    isCatalog: query.isSuccess,
    isLoading: query.isLoading,
    error: query.error,
  };
}

/**
 * Fuseaux horaires IANA, sans dépendance : la plateforme connaît sa propre
 * liste. Repli sur une chaîne vide si l'exécution ne fournit pas
 * `Intl.supportedValuesOf` (anciens moteurs) — la saisie reste possible.
 */
export function listTimeZones(): readonly string[] {
  if (typeof Intl === 'undefined' || typeof Intl.supportedValuesOf !== 'function') {
    return [];
  }
  try {
    return Intl.supportedValuesOf('timeZone');
  } catch {
    return [];
  }
}

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  parsePricingConfig,
  parsePublicPlansResponse,
  type BillingCycleName,
  type CurrencyOptionView,
  type PlanPriceView,
  type PricingConfig,
  type PublicPlanView,
  type PublicPlansResponse,
} from './types';
import { currencyMeta, yearlyFromMonthly } from '@/lib/money';

const CURRENCY_STORAGE_KEY = 'discipolat-display-currency';
const CYCLE_STORAGE_KEY = 'discipolat-display-cycle';

/**
 * Catalogue de tarifs côté client — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T5.2 / T7.2.
 *
 * <p>Principe : le serveur décide de tout (quelles devises existent, si
 * l'utilisateur peut choisir, quels canaux sont ouverts), le client ne fait que
 * refléter et laisser agir dans les limites reçues. Aucun interrupteur
 * d'affichage n'est codé en dur ici.</p>
 */
export interface UsePricingResult {
  config: PricingConfig | null;
  plans: PublicPlanView[];
  currency: string;
  cycle: BillingCycleName;
  region: string | null;
  /** Devises réellement proposées (jamais la liste ISO complète). */
  currencies: CurrencyOptionView[];
  /** Métadonnées de la devise affichée, pour `formatMinor`. */
  activeMeta: ReturnType<typeof currencyMeta>;
  loading: boolean;
  error: string | null;
  setCurrency: (code: string) => void;
  setCycle: (cycle: BillingCycleName) => void;
  setRegion: (code: string) => void;
}

function readStoredCurrency(): string | null {
  try {
    return localStorage.getItem(CURRENCY_STORAGE_KEY);
  } catch {
    return null;
  }
}

function readStoredCycle(): BillingCycleName | null {
  try {
    const stored = localStorage.getItem(CYCLE_STORAGE_KEY);
    return stored === 'YEARLY' || stored === 'MONTHLY' ? stored : null;
  } catch {
    return null;
  }
}

export function usePricing(initial?: { currency?: string; cycle?: string }): UsePricingResult {
  const [config, setConfig] = useState<PricingConfig | null>(null);
  const [catalog, setCatalog] = useState<PublicPlansResponse | null>(null);
  const [currency, setCurrencyState] = useState<string>(() => initial?.currency ?? readStoredCurrency() ?? '');
  const [cycle, setCycleState] = useState<BillingCycleName>(() =>
    (initial?.cycle ?? readStoredCycle()) === 'YEARLY' ? 'YEARLY' : 'MONTHLY',
  );
  const [region, setRegion] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // ── Réglages publics ───────────────────────────────────────────────────────
  useEffect(() => {
    let active = true;
    const params = currency ? { currency } : {};
    api
      .get<unknown>('/public/pricing/config', { params })
      .then(({ data }) => {
        if (!active) return;
        const parsed = parsePricingConfig(data);
        if (!parsed) return;
        setConfig(parsed);
        setError(null);
        // Premier chargement : on aligne la devise sur ce que le serveur retient.
        // Ensuite la sélection de l'utilisateur prime — jamais l'inverse.
        setCurrencyState((current) => current || parsed.resolvedCurrency);
      })
      .catch(() => {
        if (active) setError('pricing.loadError');
      });
    return () => {
      active = false;
    };
  }, [currency]);

  const activeCurrency = currency || config?.resolvedCurrency || config?.defaultCurrency || 'XAF';
  const activeCycle: BillingCycleName =
    cycle === 'YEARLY' && config?.allowYearly === false ? 'MONTHLY' : cycle;

  // ── Catalogue ─────────────────────────────────────────────────────────────
  const query = useQuery({
    queryKey: ['public', 'pricing', activeCurrency, activeCycle, region],
    queryFn: async (): Promise<PublicPlansResponse | null> => {
      const { data } = await api.get<unknown>('/public/pricing/plans', {
        params: { currency: activeCurrency, cycle: activeCycle, ...(region ? { region } : {}) },
      });
      return parsePublicPlansResponse(data);
    },
    // Le catalogue bouge rarement ; 60 s évite un aller-retour à chaque rendu
    // sans servir un tarif qu'une heure après une modification.
    staleTime: 60_000,
    enabled: Boolean(activeCurrency),
    retry: 1,
  });

  useEffect(() => {
    if (query.data) setCatalog(query.data);
  }, [query.data]);

  const setCurrency = useCallback((code: string) => {
    setCurrencyState(code.toUpperCase());
    try {
      localStorage.setItem(CURRENCY_STORAGE_KEY, code.toUpperCase());
    } catch {
      /* navigation privée : la sélection ne survivra pas, sans plus grave conséquence */
    }
  }, []);

  const setCycle = useCallback((next: BillingCycleName) => {
    setCycleState(next);
    try {
      localStorage.setItem(CYCLE_STORAGE_KEY, next);
    } catch {
      /* idem */
    }
  }, []);

  const currencies = useMemo(() => {
    if (config?.enabledCurrencies?.length) return config.enabledCurrencies;
    return [{ code: activeCurrency, symbol: '?', name: activeCurrency, decimals: 2, selected: true }];
  }, [config, activeCurrency]);

  const activeMeta = useMemo(
    () => currencyMeta(activeCurrency, currencies),
    [activeCurrency, currencies],
  );

  return {
    config,
    plans: catalog?.plans ?? [],
    currency: activeCurrency,
    cycle: activeCycle,
    region,
    currencies,
    activeMeta,
    loading: query.isLoading,
    error: error ?? (query.isError ? 'pricing.loadError' : null),
    setCurrency,
    setCycle,
    setRegion,
  };
}

/**
 * Prix annuel d'un plan : le prix saisi s'il existe, sinon dérivé du mensuel.
 *
 * <p>On distingue les deux cas par `computed`, pour que l'interface puisse
 * l'annoncer discrètement plutôt que de présenter un chiffre calculé comme
 * s'il avait été decided. Le serveur fait le même calcul — celui du client ne
 * sert qu'à comparer deux affichages côte à côte.</p>
 */
export function resolveYearlyPrice(
  plan: PublicPlanView,
  activeCurrency: string,
): { price: PlanPriceView; computed: boolean } | null {
  const explicit = plan.prices.find(
    (price) => price.cycle === 'YEARLY' && price.currency === activeCurrency,
  );
  if (explicit) return { price: explicit, computed: false };

  const monthly = plan.prices.find(
    (price) => price.cycle === 'MONTHLY' && price.currency === activeCurrency,
  );
  if (!monthly) return null;

  const discount = plan.annualDiscountPct ?? 0;
  const amountMinor = yearlyFromMonthly(monthly.amountMinor, discount);
  return {
    price: { ...monthly, cycle: 'YEARLY', amountMinor, amount: String(amountMinor) },
    computed: true,
  };
}

/** Le cycle mensuel est-il proposé ? */
export function offersMonthlyCycle(config: PricingConfig | null): boolean {
  return config?.allowMonthly !== false;
}
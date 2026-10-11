import { useCallback, useEffect, useState } from 'react';
import { Pencil, Power, RefreshCw } from 'lucide-react';
import api from '@/lib/api';
import PlanPricesPanel from '@/features/pricing/PlanPricesPanel';
import PricingSettingsPanel from '@/features/pricing/PricingSettingsPanel';

/**
 * Plans SaaS — gestion des offres et de leurs tarifs
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL T5.2).
 *
 * <p>Les champs d'origine (clé, nom, description, activation) sont inchangés :
 * le parcours de création et de bascule d'un plan continue de fonctionner
 * exactement comme avant. Ce qui change, c'est l'<b>éditeur de prix</b>, qui
 * remplace les deux champs « prix mensuel / prix annuel » codés en dur — ces
 * champs affichaient « FCFA » quelle que soit la devise.</p>
 *
 * <p>La page ne dit plus « 15 000 FCFA/mois » : elle dit le prix, dans la devise
 * du prix, avec le symbole de cette devise.</p>
 */
interface PlanSummary {
  key: string;
  name: string;
  description: string | null;
  isActive: boolean;
  sortOrder?: number;
  trialDays?: number;
  annualDiscountPct?: number;
}

type Tab = 'plans' | 'pricing';

export default function PlatformPlansPage() {
  const [tab, setTab] = useState<Tab>('plans');
  const [plans, setPlans] = useState<PlanSummary[]>([]);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<PlanSummary[]>('/platform/admin/plans');
      const list = response.data ?? [];
      setPlans(list);
      setSelectedKey((current) => current ?? list[0]?.key ?? null);
    } catch {
      setError('Impossible de charger les plans.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const toggle = async (plan: PlanSummary) => {
    try {
      await api.post('/platform/admin/plans', { key: plan.key, isActive: !plan.isActive });
      await load();
    } catch {
      setError('Modification impossible.');
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold">Plans SaaS</h1>
            <p className="text-sm text-slate-500">Gérer les offres, leurs limites et leurs tarifs.</p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className="inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm hover:bg-slate-50"
              onClick={() => void load()}
              disabled={loading}
            >
              <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Actualiser
            </button>
          </div>
        </div>

        {/* Deux espaces distincts : les OFFRES d'un côté, la PARAMÉTRATION
            tarifaire de l'autre. Les mélanger rendait impossible de répondre à
            « combien coûte GROWTH en GBP ? » sans perdre le contexte. */}
        <div className="inline-flex rounded-lg border border-slate-300 bg-white p-1 text-sm">
          <TabButton active={tab === 'plans'} onClick={() => setTab('plans')}>
            Plans
          </TabButton>
          <TabButton active={tab === 'pricing'} onClick={() => setTab('pricing')}>
            Tarification & devises
          </TabButton>
        </div>

        {error && <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

        {tab === 'pricing' ? (
          <PricingSettingsPanel />
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {plans.map((plan) => (
                <div
                  key={plan.key}
                  className={`rounded-xl border bg-white p-5 shadow-sm ${
                    selectedKey === plan.key ? 'border-indigo-400 ring-2 ring-indigo-100' : 'border-slate-200'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <button
                      type="button"
                      className="text-left"
                      onClick={() => setSelectedKey(plan.key)}
                      aria-pressed={selectedKey === plan.key}
                    >
                      <h2 className="font-semibold">{plan.name}</h2>
                      <span className="text-xs uppercase text-slate-400">{plan.key}</span>
                    </button>
                    <div className="flex items-center gap-2">
                      <span
                        className={`rounded-full px-2 py-1 text-xs ${
                          plan.isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {plan.isActive ? 'Actif' : 'Inactif'}
                      </span>
                      <button
                        type="button"
                        className="rounded-md p-2 text-slate-600 hover:bg-slate-100"
                        onClick={() => void toggle(plan)}
                        aria-label={`${plan.isActive ? 'Désactiver' : 'Activer'} ${plan.name}`}
                      >
                        <Power className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                  <p className="mt-3 min-h-10 text-sm text-slate-600">
                    {plan.description || 'Aucune description'}
                  </p>
                  {plan.trialDays ? (
                    <p className="mt-2 text-xs text-slate-500">
                      Essai {plan.trialDays} jours · remise annuelle {plan.annualDiscountPct ?? 0} %
                    </p>
                  ) : null}
                  <button
                    type="button"
                    onClick={() => setSelectedKey(plan.key)}
                    className="mt-3 inline-flex items-center text-xs font-medium text-indigo-600 hover:text-indigo-700"
                  >
                    <Pencil className="mr-1 h-3.5 w-3.5" /> Gérer les tarifs de ce plan
                  </button>
                </div>
              ))}
            </div>

            {selectedKey ? <PlanPricesPanel planKey={selectedKey} /> : null}
          </>
        )}
      </div>
    </main>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-md px-4 py-1.5 font-medium transition-colors ${
        active ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
      }`}
    >
      {children}
    </button>
  );
}
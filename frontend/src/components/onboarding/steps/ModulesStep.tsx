// B1 — Étape 5 : modules activés.
// La liste provient de GET /admin/tenant-features (endpoint vérifié, lecture seule :
// l'activation est faite par l'action serveur du wizard, cf. A3).
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { useI18n, tText } from '@/i18n';
import type { ModulesData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

interface TenantFeature {
  key: string;
  name?: string;
  enabled?: boolean;
}

export default function ModulesStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<ModulesData>;
  const [selected, setSelected] = useState<string[]>(prior.modules ?? []);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['tenant-features'],
    queryFn: async () => (await api.get<TenantFeature[]>('/admin/tenant-features')).data,
    retry: 0,
  });

  // État vide : la liste peut légitimement être vide (aucun module exposé).
  const features = data ?? [];

  const toggle = (key: string) =>
    setSelected((s) => (s.includes(key) ? s.filter((k) => k !== key) : [...s, key]));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selected.length < 1 || selected.length > 50) {
      setError(tText('Sélectionnez entre 1 et 50 modules.'));
      return;
    }
    setError(null);
    onSubmit({ modules: selected });
  };

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4">
      {isLoading && (
        <div className="space-y-2" aria-busy="true" aria-label={tText('Chargement')}>
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-11 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
          ))}
        </div>
      )}
      {isError && (
        <div role="alert" className="text-sm text-red-500 flex items-center gap-3 flex-wrap">
          <span>{tText('Impossible de charger la liste des modules.')}</span>
          <button type="button" onClick={() => refetch()} className="btn-sm underline min-h-[44px]">
            {tText('Réessayer')}
          </button>
        </div>
      )}
      {!isLoading && !isError && features.length === 0 && (
        <p className="text-sm text-gray-500">{tText('Aucun module disponible.')}</p>
      )}
      {!isLoading && !isError && features.length > 0 && (
        <fieldset className="space-y-1" disabled={disabled}>
          <legend className="sr-only">{t('Modules disponibles')}</legend>
          {features.map((f) => (
            <label
              key={f.key}
              className="flex items-center gap-2 min-h-[44px] px-2 rounded cursor-pointer hover:bg-black/5 dark:hover:bg-white/5"
            >
              <input
                type="checkbox" checked={selected.includes(f.key)} onChange={() => toggle(f.key)}
                disabled={disabled} className="w-5 h-5"
              />
              <span className="text-sm">{f.name ?? f.key}</span>
            </label>
          ))}
        </fieldset>
      )}
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}

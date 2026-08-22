import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { Loader2, GitBranch, TrendingUp, Users, Sparkles, ArrowRight } from 'lucide-react';

interface SimulationRequest {
  faiseurMultiplier: number;
  retentionGainPercent: number;
  pipelineBoost: number;
  months: number;
}

interface TwinSnapshot {
  totalSouls: number;
  faiseurs: number;
  leaders: number;
  ratioLeadersPerFaiseur: number;
  avgRisk: number;
}

interface SimulationResult {
  snapshot: TwinSnapshot;
  projectedSouls: number;
  projectedFaiseurs: number;
  projectedLeaders: number;
  projectedRatio: number;
  deltaSouls: number;
  deltaRatio: string;
}

/** Jumeau Numérique de l'Église — simulateur « what-if » de croissance. */
export default function DigitalTwinPage() {
  const [form, setForm] = useState({
    faiseurMultiplier: '1.5',
    retentionGainPercent: '10',
    pipelineBoost: '20',
    months: '12',
  });

  const simMutation = useMutation({
    mutationFn: async (payload?: Partial<SimulationRequest>) =>
      (
        await api.post<SimulationResult>('/twin/simulate', {
          faiseurMultiplier: Number(payload?.faiseurMultiplier ?? form.faiseurMultiplier),
          retentionGainPercent: Number(payload?.retentionGainPercent ?? form.retentionGainPercent),
          pipelineBoost: Number(payload?.pipelineBoost ?? form.pipelineBoost),
          months: Number(payload?.months ?? form.months),
        })
      ).data,
  });

  const result = simMutation.data;

  const ScenarioButton = ({ label, payload }: { label: string; payload: Partial<SimulationRequest> }) => (
    <button
      onClick={() => {
        if (payload.months) setForm({ ...form, months: String(payload.months) });
        if (payload.retentionGainPercent !== undefined)
          setForm({ ...form, retentionGainPercent: String(payload.retentionGainPercent) });
        if (payload.faiseurMultiplier !== undefined)
          setForm({ ...form, faiseurMultiplier: String(payload.faiseurMultiplier) });
        if (payload.pipelineBoost !== undefined)
          setForm({ ...form, pipelineBoost: String(payload.pipelineBoost) });
        simMutation.mutate(payload);
      }}
      className="btn-sm px-3 py-2 rounded-lg glass-card text-xs hover:shadow-md"
    >
      {label}
    </button>
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-slate-600 to-slate-800 text-white shadow-lg">
          <GitBranch className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Jumeau Numérique de l'Église</h1>
          <p className="page-subtitle">Simulez des stratégies avant d'investir — projection sur N mois</p>
        </div>
      </div>

      {/* Scénarios rapides */}
      <div className="flex flex-wrap gap-2 mb-6">
        <ScenarioButton label={`Stagnation (${form.months || 12} mois)`} payload={{}} />
        <ScenarioButton label="Croissance douce" payload={{ faiseurMultiplier: 1.3, retentionGainPercent: 5, pipelineBoost: 10 }} />
        <ScenarioButton label="Réveil des faiseurs" payload={{ faiseurMultiplier: 2, retentionGainPercent: 0, pipelineBoost: 0 }} />
        <ScenarioButton label="Réveil + rétention" payload={{ faiseurMultiplier: 1.8, retentionGainPercent: 15, pipelineBoost: 15 }} />
        <ScenarioButton label="Réveil spirituel" payload={{ faiseurMultiplier: 2.5, retentionGainPercent: 20, pipelineBoost: 40 }} />
      </div>

      {/* Formulaire personnalisé */}
      <div className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-4 animate-slide-up">
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            Multiplicateur faiseurs ×
          </label>
          <input type="number" step="0.1" min="0" max="5" className="input w-full"
            value={form.faiseurMultiplier}
            onChange={(e) => setForm({ ...form, faiseurMultiplier: e.target.value })} />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            Gain rétention (%)
          </label>
          <input type="number" min="0" max="50" className="input w-full"
            value={form.retentionGainPercent}
            onChange={(e) => setForm({ ...form, retentionGainPercent: e.target.value })} />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            Boost pipeline évangélisation (%)
          </label>
          <input type="number" min="0" max="200" className="input w-full"
            value={form.pipelineBoost}
            onChange={(e) => setForm({ ...form, pipelineBoost: e.target.value })} />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            Horizon (mois)
          </label>
          <div className="flex gap-2">
            <input type="number" min="1" max="36" className="input w-full"
              value={form.months}
              onChange={(e) => setForm({ ...form, months: e.target.value })} />
            <button onClick={() => simMutation.mutate(undefined)} disabled={simMutation.isPending}
              className="btn-primary btn-sm whitespace-nowrap flex items-center gap-1">
              {simMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
              Simuler
            </button>
          </div>
        </div>
      </div>

      {/* Résultats */}
      {result && !simMutation.isPending && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6 animate-slide-up">
            <div className="stat-card bg-gradient-to-br from-blue-500 to-indigo-600">
              <TrendingUp className="w-5 h-5 opacity-80" />
              <p className="stat-value">{result.projectedSouls.toLocaleString('fr-FR')}</p>
              <p className="text-xs opacity-80">
                Âmes projetées ({result.deltaSouls >= 0 ? '+' : ''}
                {Math.round(result.deltaSouls)})
              </p>
            </div>
            <div className="stat-card bg-gradient-to-br from-emerald-500 to-teal-600">
              <Sparkles className="w-5 h-5 opacity-80" />
              <p className="stat-value">{Math.round(result.projectedFaiseurs)}</p>
              <p className="text-xs opacity-80">Faiseurs projetés</p>
            </div>
            <div className="stat-card bg-gradient-to-br from-purple-500 to-violet-600">
              <Users className="w-5 h-5 opacity-80" />
              <p className="stat-value">{Math.round(result.projectedLeaders)}</p>
              <p className="text-xs opacity-80">Leaders projetés</p>
            </div>
            <div className="stat-card bg-gradient-to-br from-amber-500 to-orange-600">
              <GitBranch className="w-5 h-5 opacity-80" />
              <p className="stat-value">{result.projectedRatio.toFixed(1)}</p>
              <p className="text-xs opacity-80">
                Ratio leaders/faiseurs ({result.deltaRatio})
              </p>
            </div>
          </div>

          {/* Comparaison actuel → projeté */}
          <div className="glass-card p-6 animate-slide-up">
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">
              Situation actuelle → projection à {form.months} mois
            </h2>
            <div className="space-y-3">
              {[
                { label: 'Âmes', current: result.snapshot.totalSouls, projected: Math.round(result.projectedSouls), unit: '' },
                { label: 'Faiseurs', current: result.snapshot.faiseurs, projected: Math.round(result.projectedFaiseurs), unit: '' },
                { label: 'Leaders', current: result.snapshot.leaders, projected: Math.round(result.projectedLeaders), unit: '' },
              ].map((row) => {
                const max = Math.max(row.current, row.projected, 1);
                return (
                  <div key={row.label}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-700 dark:text-gray-300">{row.label}</span>
                      <span className="font-medium">
                        {row.current} <ArrowRight className="inline w-3 h-3 mx-1 text-primary-500" />{' '}
                        {row.projected}
                        {row.projected > row.current && (
                          <span className="ml-1 text-green-600 dark:text-green-400">
                            (+{Math.round(((row.projected - row.current) / Math.max(row.current, 1)) * 100)}%)
                          </span>
                        )}
                      </span>
                    </div>
                    <div className="relative w-full h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                      <div className="absolute h-full bg-gray-300 dark:bg-gray-600 rounded-full"
                        style={{ width: `${(row.current / max) * 100}%` }} />
                      <div className="absolute h-full bg-gradient-to-r from-primary-500 to-primary-400 rounded-full"
                        style={{ width: `${(row.projected / max) * 100}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-4">
              Modèle : croissance composée mensuelle — faiseurs = moteurs d'évangélisation, leaders plafonnés au ratio
              1 leader / 8 faiseurs. Score santé actuel : {Math.round(100 - result.snapshot.avgRisk * 10)}/100.
            </p>
          </div>
        </>
      )}

      {!result && !simMutation.isPending && (
        <div className="glass-card p-12 text-center animate-slide-up">
          <GitBranch className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400">
            Lancez une simulation pour voir la trajectoire projetée de votre église.
          </p>
        </div>
      )}
    </div>
  );
}

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { AlertTriangle, Loader2, Filter } from 'lucide-react';

interface RiskPrediction {
  id: string;
  soulId: string;
  soulName?: string;
  type: string;
  level: string;
  score: number;
  reason: string;
  createdAt: string;
}

const RISK_COLORS: Record<string, string> = {
  HIGH: 'text-red-400 bg-red-500/20',
  MEDIUM: 'text-yellow-400 bg-yellow-500/20',
  LOW: 'text-green-400 bg-green-500/20',
};

const TYPES = ['ABSENCE', 'DISENGAGEMENT', 'SPIRITUAL_DECLINE', 'FAMILY_ISSUE', 'FINANCIAL', 'HEALTH'];

export default function PredictionsRiskPage() {
  const [type, setType] = useState<string>('');

  const { data: allRisks = [], isLoading } = useQuery({
    queryKey: ['ai-predictions-risks'],
    queryFn: async () => (await api.get('/ai-predictions/risks')).data as RiskPrediction[],
  });

  const { data: typeRisks = [], isLoading: loadingType } = useQuery({
    queryKey: ['ai-predictions-type', type],
    queryFn: async () => (await api.get(`/ai-predictions/type/${type}`)).data as RiskPrediction[],
    enabled: !!type,
  });

  const risks = type ? typeRisks : allRisks;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-red-500 to-orange-600 text-white shadow-lg">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Prédictions de risques</h1>
          <p className="page-subtitle">Analyse prédictive des risques pastoraux</p>
        </div>
      </div>

      <div className="glass-card p-4 mb-6">
        <div className="flex items-center gap-3 flex-wrap">
          <Filter className="w-4 h-4 text-gray-400" />
          <button onClick={() => setType('')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${!type ? 'bg-primary-500 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>
            Tous
          </button>
          {TYPES.map((t) => (
            <button key={t} onClick={() => setType(t)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${type === t ? 'bg-primary-500 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>
              {t.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: risks.length, color: 'text-orange-600' },
          { label: 'Élevé', value: risks.filter((r) => r.level === 'HIGH').length, color: 'text-red-600' },
          { label: 'Moyen', value: risks.filter((r) => r.level === 'MEDIUM').length, color: 'text-yellow-600' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <p className={`stat-value ${s.color}`}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      {(isLoading || loadingType) ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : risks.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune prédiction de risque</div>
      ) : (
        <div className="space-y-3">
          {risks.map((r) => (
            <div key={r.id} className={`glass-card p-5 ${r.level === 'HIGH' ? 'border-l-[3px] border-l-red-500' : ''}`}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${RISK_COLORS[r.level] ?? 'text-gray-400 bg-gray-500/20'}`}>{r.level}</span>
                    <span className="text-xs text-gray-400">{r.type.replace(/_/g, ' ')}</span>
                    <span className="text-xs text-gray-400">Score: {r.score}%</span>
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.soulName ?? `Membre #${r.soulId.slice(0, 8)}`}</p>
                  <p className="text-xs text-gray-500 mt-1">{r.reason}</p>
                  <p className="text-[11px] text-gray-500 mt-1">{new Date(r.createdAt).toLocaleDateString('fr-FR')}</p>
                </div>
                <div className="w-16 h-16 relative ml-4">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                    <circle cx="18" cy="18" r="15.5" fill="none" className="stroke-gray-200 dark:stroke-gray-700" strokeWidth="3" />
                    <circle cx="18" cy="18" r="15.5" fill="none" stroke={r.level === 'HIGH' ? '#ef4444' : r.level === 'MEDIUM' ? '#eab308' : '#22c55e'}
                      strokeWidth="3" strokeDasharray={`${r.score} 100`} strokeLinecap="round" />
                  </svg>
                  <span className="absolute inset-0 flex items-center justify-center text-[10px] font-bold text-gray-800 dark:text-gray-200">{r.score}%</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

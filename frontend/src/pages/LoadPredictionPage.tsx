import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Gauge, AlertTriangle, CalendarClock } from 'lucide-react';

interface WeekPred { semaine: number; debut: string; fin: string; evenementsPlanifies: number; joursDePics: string[]; chargeEstimee: number; niveau: string; recommandation: string; }
interface Prediction { baselineChargeHebdo: number; moyenneInscriptionsHebdo: number; moyenneRapportsHebdo: number; joursForts: string[]; semainesCritiques: number; semaines: WeekPred[]; }

const LEVEL_STYLE: Record<string, string> = {
  CRITIQUE: 'text-red-400 bg-red-500/20 border-red-500/30',
  ELEVE: 'text-orange-400 bg-orange-500/20 border-orange-500/30',
  NORMAL: 'text-blue-400 bg-blue-500/20 border-blue-500/30',
  FAIBLE: 'text-gray-400 bg-gray-500/20 border-gray-500/30',
};

/** P3 #102 — Prédiction de charge (pics d'activité) sur 8 semaines. */
export default function LoadPredictionPage() {
  const { data, isLoading } = useQuery({ queryKey: ['load-prediction'], queryFn: async () => (await api.get('/load-prediction')).data as Prediction });

  if (isLoading) return <div className="p-6 text-gray-400">Chargement de la prédiction de charge…</div>;
  if (!data) return <div className="p-6 text-gray-400">Prédiction indisponible.</div>;

  const maxLoad = Math.max(...data.semaines.map((w) => w.chargeEstimee), 1);

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Gauge className="text-cyan-400" /> Prédiction de charge — 8 prochaines semaines</h1>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Kpi label="Charge de base hebdo" value={data.baselineChargeHebdo} />
        <Kpi label="Inscriptions / semaine" value={data.moyenneInscriptionsHebdo} />
        <Kpi label="Rapports / semaine" value={data.moyenneRapportsHebdo} />
        <div className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10">
          <p className="text-xs text-gray-400">Semaines critiques</p>
          <p className={`text-2xl font-bold flex items-center gap-1 ${data.semainesCritiques > 0 ? 'text-red-400' : 'text-green-400'}`}>
            {data.semainesCritiques > 0 && <AlertTriangle className="w-5 h-5" />} {data.semainesCritiques}
          </p>
        </div>
      </div>
      <p className="text-sm text-gray-400 flex items-center gap-2"><CalendarClock className="w-4 h-4 text-cyan-400" /> Jours forts historiques : <span className="text-white font-medium">{data.joursForts.join(', ')}</span></p>
      <div className="space-y-3">
        {data.semaines.map((w) => (
          <div key={w.semaine} className={`bg-white/5 backdrop-blur rounded-2xl p-4 border ${LEVEL_STYLE[w.niveau] ?? LEVEL_STYLE.NORMAL}`}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-white font-medium">Semaine {w.semaine} <span className="text-gray-500 text-xs">({w.debut} → {w.fin})</span></span>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${LEVEL_STYLE[w.niveau] ?? ''}`}>{w.niveau}</span>
            </div>
            <div className="w-full h-2 bg-black/40 rounded-full overflow-hidden mb-2">
              <div className={`h-full rounded-full ${w.niveau === 'CRITIQUE' ? 'bg-red-500' : w.niveau === 'ELEVE' ? 'bg-orange-500' : w.niveau === 'FAIBLE' ? 'bg-gray-600' : 'bg-cyan-500'}`} style={{ width: `${Math.round((w.chargeEstimee / maxLoad) * 100)}%` }} />
            </div>
            <div className="flex items-center justify-between text-xs text-gray-400">
              <span>{w.evenementsPlanifies} événement(s) planifié(s){w.joursDePics.length > 0 ? ` • pics : ${w.joursDePics.join(', ')}` : ''}</span>
              <span className="text-white font-semibold">charge {w.chargeEstimee}</span>
            </div>
            <p className="text-xs text-gray-400 mt-1 italic">{w.recommandation}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10">
      <p className="text-xs text-gray-400">{label}</p>
      <p className="text-2xl font-bold text-white">{value}</p>
    </div>
  );
}

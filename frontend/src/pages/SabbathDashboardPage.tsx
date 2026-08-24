import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Church, Compass } from 'lucide-react';

interface Axis { axe: string; score: number; niveau: string; }
interface Dashboard { totalAmes: number; amesActives: number; tauxActivite: number; faiseursActifs: number; ratioFaiseurs: number; famillesARisque: number; maturiteGlobale: number; saisonSpirituelle: string; orientationPastorale: string; axes: Axis[]; }

const NIV_STYLE: Record<string, string> = {
  MATURE: 'bg-green-500',
  EN_CROISSANCE: 'bg-yellow-500',
  EMBRYONNAIRE: 'bg-red-500',
};

/** P3 #106 — Tableau de bord sabbatique : état spirituel consolidé sur 12 axes de maturité. */
export default function SabbathDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ['sabbath-dashboard'], queryFn: async () => (await api.get('/sabbath-dashboard')).data as Dashboard });
  if (isLoading || !data) return <div className="p-6 text-gray-400">Chargement du tableau sabbatique…</div>;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Church className="text-violet-400" /> Tableau de bord sabbatique</h1>

      <div className="bg-gradient-to-r from-violet-600/20 to-indigo-600/20 backdrop-blur rounded-2xl p-5 border border-violet-500/30">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs text-violet-300/80">Maturité spirituelle globale</p>
            <p className="text-4xl font-bold text-white">{data.maturiteGlobale}<span className="text-lg text-gray-400">/100</span></p>
          </div>
          <div className="text-right max-w-md">
            <p className="text-white font-semibold">{data.saisonSpirituelle}</p>
            <p className="text-sm text-gray-300 mt-1 flex items-start gap-2"><Compass className="w-4 h-4 mt-0.5 shrink-0 text-violet-300" /> {data.orientationPastorale}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <Kpi label="Âmes totales" value={data.totalAmes} />
        <Kpi label="Âmes actives" value={data.amesActives} />
        <Kpi label="Taux d'activité" value={`${Math.round(data.tauxActivite)}%`} />
        <Kpi label="Faiseurs actifs" value={data.faiseursActifs} />
        <Kpi label="Familles à risque" value={data.famillesARisque} />
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-4">Les 12 axes de maturité</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-3">
          {data.axes.map((a) => (
            <div key={a.axe}>
              <div className="flex items-center justify-between text-sm mb-1">
                <span className="text-gray-300">{a.axe}</span>
                <span className="text-white font-medium">{a.score}</span>
              </div>
              <div className="w-full h-2 bg-black/40 rounded-full overflow-hidden">
                <div className={`h-full rounded-full ${NIV_STYLE[a.niveau] ?? 'bg-gray-600'}`} style={{ width: `${a.score}%` }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10">
      <p className="text-xs text-gray-400">{label}</p>
      <p className="text-2xl font-bold text-white">{value}</p>
    </div>
  );
}

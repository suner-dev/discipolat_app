import { formatEnum } from '@/lib/labels';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { MapPin, HeartPulse, Users } from 'lucide-react';

interface Zone { zone: string; total: number; couverts: number; actifs: number; contactsRecents: number; tauxCouverture: number; healthScore: number; status: string; actionRecommandee?: string; latitude?: number; longitude?: number; }
interface HealthData { zones: Zone[]; zonesFaibles?: number; genereLe?: string; }

const STATUS_STYLE: Record<string, { bar: string; badge: string }> = {
  BONNE: { bar: 'bg-green-500', badge: 'text-green-400 bg-green-500/20' },
  MOYENNE: { bar: 'bg-yellow-500', badge: 'text-yellow-400 bg-yellow-500/20' },
  FAIBLE: { bar: 'bg-red-500', badge: 'text-red-400 bg-red-500/20' },
};

/** P3 #104 — Analyse de santé spirituelle par quartier (heatmap + zones de couverture faible). */
export default function NeighborhoodHealthPage() {
  const { data, isLoading } = useQuery({ queryKey: ['neighborhood-health'], queryFn: async () => (await api.get('/neighborhood-health')).data as HealthData });
  if (isLoading) return <div className="p-6 text-gray-400">Chargement de la santé par quartier…</div>;
  const zones = data?.zones ?? [];
  const faibles = zones.filter((z) => z.status === 'FAIBLE').length;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><HeartPulse className="text-rose-400" /> Santé spirituelle par quartier</h1>
      <p className="text-sm text-gray-400 flex items-center gap-2">
        <MapPin className="w-4 h-4 text-rose-400" /> {zones.length} zone(s) analysée(s)
        {faibles > 0 && <span className="text-red-400 font-medium">— {faibles} zone(s) en couverture faible nécessitent une intervention pastorale</span>}
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {zones.map((z) => {
          const st = STATUS_STYLE[z.status] ?? STATUS_STYLE.MOYENNE;
          return (
            <div key={z.zone} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-rose-500/30 transition">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-white font-semibold">{z.zone}</h3>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${st.badge}`}>{formatEnum(z.status)}</span>
              </div>
              <div className="w-full h-2 bg-black/40 rounded-full overflow-hidden mb-3">
                <div className={`h-full rounded-full ${st.bar}`} style={{ width: `${z.healthScore}%` }} />
              </div>
              <div className="grid grid-cols-2 gap-y-1 text-xs text-gray-400">
                <span className="flex items-center gap-1"><Users className="w-3 h-3" /> {z.total} âmes</span>
                <span>{z.couverts} couvertes ({Math.round((z.tauxCouverture ?? 0) * 100)}%)</span>
                <span>{z.actifs} actives</span>
                <span>{z.contactsRecents} contacts &lt; 30j</span>
              </div>
              {z.latitude != null && z.longitude != null && <p className="text-[10px] text-gray-600 mt-2">📍 {z.latitude.toFixed(4)}, {z.longitude.toFixed(4)}</p>}
              {z.actionRecommandee && <p className="text-xs text-rose-300 mt-2 italic">{z.actionRecommandee}</p>}
            </div>
          );
        })}
      </div>
      {zones.length === 0 && <p className="text-sm text-gray-500">Aucune donnée de zone. Renseignez le champ « zone » des âmes pour activer l'analyse géographique.</p>}
    </div>
  );
}

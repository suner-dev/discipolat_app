import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Activity, MousePointerClick, Users, Timer, Smartphone, Monitor, Tablet } from 'lucide-react';

interface Summary { periodeJours: number; totalEvenements: number; pagesVues: number; utilisateursUniques: number; dureeMoyenneSec: number; topPages: Array<{ page?: string; vues?: number; [k: string]: unknown }>; vuesParJour: Record<string, number> | Array<{ jour?: string; vues?: number }>; parAppareil: Record<string, number>; topFunnels?: Array<Record<string, unknown>>; }

const DEVICE_ICON: Record<string, typeof Monitor> = { MOBILE: Smartphone, DESKTOP: Monitor, TABLET: Tablet };

/** P3 #109 — Analytics d'usage self-hosted : pages vues, appareils, funnels. */
export default function AdminUsageAnalyticsPage() {
  const [days, setDays] = useState(30);
  const { data, isLoading } = useQuery({
    queryKey: ['usage-analytics', days],
    queryFn: async () => (await api.get('/usage-analytics/summary', { params: { days } })).data as Summary,
  });

  if (isLoading || !data) return <div className="p-6 text-gray-400">Chargement des analytics d'usage…</div>;

  const daily = Array.isArray(data.vuesParJour)
    ? data.vuesParJour.map((d) => [String(d.jour ?? ''), Number(d.vues ?? 0)] as const)
    : Object.entries(data.vuesParJour ?? {});
  const maxDay = Math.max(...daily.map(([, v]) => v), 1);
  const totalDevices = Object.values(data.parAppareil ?? {}).reduce((s, v) => s + Number(v), 0) || 1;

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Activity className="text-lime-400" /> Analytics d'usage</h1>
        <select value={days} onChange={(e) => setDays(Number(e.target.value))} className="bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200">
          {[7, 14, 30, 90].map((d) => <option key={d} value={d}>{d} derniers jours</option>)}
        </select>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Kpi icon={MousePointerClick} label="Pages vues" value={data.pagesVues} />
        <Kpi icon={Users} label="Utilisateurs uniques" value={data.utilisateursUniques} />
        <Kpi icon={Timer} label="Durée moyenne" value={`${data.dureeMoyenneSec}s`} />
        <Kpi icon={Activity} label="Événements totaux" value={data.totalEvenements} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Panel title="Top pages visitées">
          {(data.topPages ?? []).map((p, i) => (
            <Row key={i} left={String(p.page ?? '—')} right={`${p.vues ?? '—'} vues`} />
          ))}
          {(data.topPages ?? []).length === 0 && <Empty />}
        </Panel>
        <Panel title="Répartition par appareil">
          {Object.entries(data.parAppareil ?? {}).map(([dev, count]) => {
            const Icon = DEVICE_ICON[dev] ?? Monitor;
            return (
              <div key={dev} className="flex items-center gap-3 py-1.5">
                <Icon className="w-4 h-4 text-gray-400" />
                <span className="text-sm text-gray-300 w-24">{dev}</span>
                <div className="flex-1 h-2 bg-black/40 rounded-full overflow-hidden"><div className="h-full bg-lime-500 rounded-full" style={{ width: `${Math.round((Number(count) / totalDevices) * 100)}%` }} /></div>
                <span className="text-xs text-gray-400">{Math.round((Number(count) / totalDevices) * 100)}%</span>
              </div>
            );
          })}
        </Panel>
      </div>

      <Panel title="Vues par jour">
        <div className="flex items-end gap-1 h-32">
          {daily.map(([jour, vues]) => (
            <div key={jour} className="flex-1 flex flex-col items-center justify-end h-full group" title={`${jour} : ${vues} vues`}>
              <div className="w-full bg-gradient-to-t from-lime-600 to-lime-400 rounded-t group-hover:from-lime-500 transition-all" style={{ height: `${(vues / maxDay) * 100}%` }} />
              <span className="text-[9px] text-gray-500 mt-1 rotate-45 origin-top-left hidden md:block">{jour?.slice(5)}</span>
            </div>
          ))}
        </div>
        {daily.length === 0 && <Empty />}
      </Panel>
    </div>
  );
}

function Kpi({ icon: Icon, label, value }: { icon: typeof Activity; label: string; value: string | number }) {
  return (<div className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10"><p className="text-xs text-gray-400 flex items-center gap-1"><Icon className="w-3 h-3" /> {label}</p><p className="text-2xl font-bold text-white">{value}</p></div>);
}
function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (<div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10"><h2 className="text-white font-semibold mb-3">{title}</h2>{children}</div>);
}
function Row({ left, right }: { left: string; right: string }) {
  return (<div className="flex items-center justify-between text-sm bg-black/20 rounded-lg px-3 py-2 mb-1"><span className="text-gray-300 truncate max-w-[60%]">{left}</span><span className="text-white font-medium">{right}</span></div>);
}
function Empty() { return <p className="text-sm text-gray-500">Aucune donnée sur la période.</p>; }

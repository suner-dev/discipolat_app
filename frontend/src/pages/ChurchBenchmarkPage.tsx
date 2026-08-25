import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { BarChart3, Plus, Loader2, Trash2, Users2 } from 'lucide-react';

interface Comparison { id: string; nomEglise: string; effectif: number; tauxPresence: number; tauxConversion: number; tauxRetention: number; scoreSpirituelMoyen: number; generositeMoyenne: number; nbDepartements: number; nbFamilles: number; categorie?: string; pays?: string; denomination?: string; }

const CATEGORIES = ['PETITE', 'MOYENNE', 'GRANDE'] as const;

/** P3 #107 — Benchmark anonyme inter-églises amélioré : comparaison par taille/pays/dénomination + clustering. */
export default function ChurchBenchmarkPage() {
  const qc = useQueryClient();
  const [category, setCategory] = useState<string>('');
  const listQ = useQuery({ queryKey: ['church-comparisons'], queryFn: async () => (await api.get('/church-comparisons')).data as Comparison[] });
  const clustersQ = useQuery({ queryKey: ['church-clusters'], queryFn: async () => (await api.get('/church-comparisons/clusters')).data });
  const byCatQ = useQuery({
    queryKey: ['church-benchmark', category], enabled: !!category,
    queryFn: async () => (await api.get(`/church-comparisons/by-category/${category}`)).data,
  });

  const create = useMutation({
    mutationFn: async () => (await api.post('/church-comparisons', { nomEglise: 'Mon église', effectif: 150, categorie: category || 'MOYENNE' })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['church-comparisons'] }),
  });
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/church-comparisons/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['church-comparisons'] }),
  });

  const churches = listQ.data ?? [];
  const avg = (f: (c: Comparison) => number) => churches.length ? Math.round(churches.reduce((s, c) => s + f(c), 0) / churches.length * 10) / 10 : 0;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><BarChart3 className="text-sky-400" /> Benchmark inter-églises</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Kpi label="Églises comparées" value={churches.length} />
        <Kpi label="Présence moy." value={`${avg((c) => c.tauxPresence)}%`} />
        <Kpi label="Rétention moy." value={`${avg((c) => c.tauxRetention)}%`} />
        <Kpi label="Score spirituel moy." value={avg((c) => c.scoreSpirituelMoyen)} />
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <div className="flex flex-wrap items-center gap-3 mb-4">
          <select value={category} onChange={(e) => setCategory(e.target.value)} className="bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200">
            <option value="">Comparer par catégorie…</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <button onClick={() => create.mutate()} disabled={create.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-sky-600 to-blue-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
            {create.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Ajouter mon église au benchmark
          </button>
        </div>
                {byCatQ.data != null && (
          <div className="text-xs text-sky-300 mb-3 flex flex-wrap gap-x-4 gap-y-1">
            {(() => {
              const d = byCatQ.data as { rang?: number; total?: number; effectifMoyen?: number } | null;
              if (!d || typeof d !== 'object') return null;
              return (
                <>
                  {d.rang != null && <span>🏆 Rang : <strong>{d.rang}</strong>{d.total != null ? ` / ${d.total}` : ''}</span>}
                  {d.effectifMoyen != null && <span>Effectif moyen : <strong>{Math.round(d.effectifMoyen)}</strong></span>}
                  {d.rang == null && d.total != null && <span>Catégorie de <strong>{d.total}</strong> église(s)</span>}
                </>
              );
            })()}
          </div>
        )}
        <div className="space-y-2">
          {churches.map((c) => (
            <div key={c.id} className="flex items-center justify-between bg-black/20 rounded-xl px-4 py-3 text-sm">
              <div>
                <span className="text-white font-medium">{c.nomEglise}</span>
                <span className="text-gray-500 ml-2 flex items-center gap-1"><Users2 className="w-3 h-3" /> {c.effectif}{c.pays ? ` • ${c.pays}` : ''}{c.denomination ? ` • ${c.denomination}` : ''}</span>
              </div>
              <div className="flex items-center gap-4 text-gray-300">
                <span>Présence {Math.round(c.tauxPresence)}%</span>
                <span>Rétention {Math.round(c.tauxRetention)}%</span>
                <button onClick={() => remove.mutate(c.id)} aria-label="Retirer du benchmark" className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
              </div>
            </div>
          ))}
        </div>
      </div>

            {clustersQ.data != null && (
        <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
          <h2 className="text-white font-semibold mb-3">Clusters d'églises similaires</h2>
          {Array.isArray(clustersQ.data) && clustersQ.data.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {(clustersQ.data as Array<Record<string, unknown>>).map((cl, idx) => (
                <div key={idx} className="bg-black/20 rounded-xl p-3 text-sm">
                  <p className="text-white font-medium">{(cl.nom ?? cl.nomCluster ?? `Cluster ${idx + 1}`) as string}</p>
                  <p className="text-gray-400 mt-1">
                    {(cl.nbEglises as number | undefined) != null && <span>{String(cl.nbEglises)} église(s)</span>}
                    {(cl.effectifMoyen as number | undefined) != null && <span> · Effectif moyen {Math.round(cl.effectifMoyen as number)}</span>}
                    {(cl.tauxPresenceMoyen as number | undefined) != null && <span> · Présence {Math.round(cl.tauxPresenceMoyen as number)}%</span>}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-gray-400">Aucun cluster disponible pour le moment.</p>
          )}
        </div>
      )}
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (<div className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10"><p className="text-xs text-gray-400">{label}</p><p className="text-2xl font-bold text-white">{value}</p></div>);
}

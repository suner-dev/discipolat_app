import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { TrendingUp, Sparkles, Loader2, Trash2 } from 'lucide-react';

interface Projection { id: string; typeProjection: string; nom: string; effectifActuel: number; effectifProjete: number; tauxCroissanceAnnuel: number; moisProjection: number; recommandations?: string; }
interface Prophecy { croissanceAnnuellePct?: number; effectifProjete12Mois?: number; message?: string; besoinsLeaders?: number; [k: string]: unknown; }

/** P3 #103 — Prophétie de croissance : modèle prédictif sur données réelles + simulateur. */
export default function GrowthProjectionPage() {
  const qc = useQueryClient();
  const listQ = useQuery({ queryKey: ['growth-projections'], queryFn: async () => (await api.get('/growth-projections')).data as Projection[] });
  const prophecyQ = useQuery({ queryKey: ['growth-prophecy'], queryFn: async () => (await api.get('/growth-projections/prophecy')).data as Prophecy });

  const [form, setForm] = useState({ nom: 'Mon église', typeProjection: 'EGLISE', effectifActuel: 150, tauxCroissanceAnnuel: 12, moisProjection: 12 });

  const simulate = useMutation({
    mutationFn: async () => (await api.post('/growth-projections/simulate', form)).data as Projection,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['growth-projections'] }),
  });

  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/growth-projections/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['growth-projections'] }),
  });

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><TrendingUp className="text-emerald-400" /> Prophétie de croissance</h1>

      {prophecyQ.data && (
        <div className="bg-gradient-to-r from-emerald-600/20 to-teal-600/20 backdrop-blur rounded-2xl p-5 border border-emerald-500/30">
          <h2 className="text-white font-semibold flex items-center gap-2 mb-2"><Sparkles className="w-4 h-4 text-emerald-300" /> Analyse prédictive (données réelles)</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Stat label="Croissance annuelle projetée" value={`${prophecyQ.data.croissanceAnnuellePct ?? '—'} %`} />
            <Stat label="Effectif dans 12 mois" value={String(prophecyQ.data.effectifProjete12Mois ?? '—')} />
            <Stat label="Nouveaux leaders nécessaires" value={String(prophecyQ.data.besoinsLeaders ?? '—')} />
          </div>
          {prophecyQ.data.message && <p className="text-sm text-emerald-200 mt-3 italic">{prophecyQ.data.message}</p>}
        </div>
      )}

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Simulateur de croissance</h2>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mb-4">
          <Field label="Nom" value={form.nom} onChange={(v) => setForm({ ...form, nom: v })} />
          <div>
            <label className="block text-xs text-gray-400 mb-1">Type</label>
            <select value={form.typeProjection} onChange={(e) => setForm({ ...form, typeProjection: e.target.value })} className="w-full bg-black/30 border border-white/10 rounded-lg px-2 py-2 text-sm text-gray-200">
              {['EGLISE', 'FAMILLE', 'DEPARTEMENT'].map((x) => <option key={x}>{x}</option>)}
            </select>
          </div>
          <NumField label="Effectif actuel" value={form.effectifActuel} onChange={(v) => setForm({ ...form, effectifActuel: v })} />
          <NumField label="Taux annuel (%)" value={form.tauxCroissanceAnnuel} onChange={(v) => setForm({ ...form, tauxCroissanceAnnuel: v })} />
          <NumField label="Mois" value={form.moisProjection} onChange={(v) => setForm({ ...form, moisProjection: v })} />
        </div>
        <button onClick={() => simulate.mutate()} disabled={simulate.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
          {simulate.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />} Simuler
        </button>
        {simulate.data && (
          <p className="mt-3 text-sm text-emerald-300">Résultat : {simulate.data.effectifActuel} → <span className="font-bold">{simulate.data.effectifProjete}</span> membres dans {simulate.data.moisProjection} mois ({simulate.data.tauxCroissanceAnnuel}%/an).</p>
        )}
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Projections enregistrées</h2>
        {(listQ.data ?? []).length === 0 ? <p className="text-sm text-gray-500">Aucune projection enregistrée.</p> : (
          <div className="space-y-2">
            {(listQ.data ?? []).map((p) => (
              <div key={p.id} className="flex items-center justify-between bg-black/20 rounded-xl px-4 py-3 text-sm">
                <div><span className="text-white font-medium">{p.nom}</span><span className="text-gray-500 ml-2">{p.typeProjection}</span></div>
                <div className="flex items-center gap-4">
                  <span className="text-gray-300">{p.effectifActuel} → <span className="text-emerald-400 font-bold">{p.effectifProjete}</span></span>
                  <button onClick={() => remove.mutate(p.id)} aria-label="Supprimer la projection" className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (<div><p className="text-xs text-emerald-300/80">{label}</p><p className="text-2xl font-bold text-white">{value}</p></div>);
}
function Field({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (<div><label className="block text-xs text-gray-400 mb-1">{label}</label><input value={value} onChange={(e) => onChange(e.target.value)} className="w-full bg-black/30 border border-white/10 rounded-lg px-2 py-2 text-sm text-gray-200" /></div>);
}
function NumField({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (<div><label className="block text-xs text-gray-400 mb-1">{label}</label><input type="number" value={value} onChange={(e) => onChange(Number(e.target.value))} className="w-full bg-black/30 border border-white/10 rounded-lg px-2 py-2 text-sm text-gray-200" /></div>);
}

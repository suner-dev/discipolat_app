import { formatEnum } from '@/lib/labels';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Database, Upload, Play, XCircle, CheckCircle, Loader2, FileSpreadsheet } from 'lucide-react';

interface MigrationJob { id: string; targetType: string; status: string; totalRows?: number; importedRows?: number; }
interface Analysis { suggestions?: Array<{ sourceColumn?: string; targetField?: string; confidence?: number }>; summary?: string; }

const TARGET_TYPES = ['SOULS', 'MEMBERS', 'FAMILIES', 'EVENTS'] as const;

/** P3 #101 — Assistant de migration de données (Excel/CSV → Discipolat). */
export default function DataMigrationPage() {
  const qc = useQueryClient();
  const [csvText, setCsvText] = useState('');
  const [targetType, setTargetType] = useState<string>('SOULS');

  const jobsQ = useQuery({ queryKey: ['data-migration'], queryFn: async () => (await api.get('/data-migration')).data as MigrationJob[] });
  const analysis = qc.getQueryData(['dm-analysis']) as Analysis | undefined;

  const analyze = useMutation({
    mutationFn: async () => {
      const lines = csvText.trim().split('\n').filter(Boolean);
      if (lines.length < 1) throw new Error('Collez au moins une ligne CSV.');
      const parse = (l: string) => l.split(/[,;\t]/).map((s) => s.trim().replace(/^"|"$/g, ''));
      const headers = parse(lines[0]);
      const sampleRows = lines.slice(1, 6).map((l) => { const c = parse(l); const r: Record<string, string> = {}; headers.forEach((h, i) => (r[h] = c[i] ?? '')); return r; });
      return (await api.post('/data-migration/analyze', { targetType, headers, sampleRows })).data;
    },
    onSuccess: (d) => qc.setQueryData(['dm-analysis'], d),
    onError: (e: Error) => alert(e.message),
  });

  const execute = useMutation({
    mutationFn: async () => {
      const job = (await api.post<MigrationJob>('/data-migration', { targetType, status: 'PENDING' })).data;
      return api.post(`/data-migration/${job.id}/execute`, {
        fieldMapping: JSON.stringify(analysis?.suggestions ?? []),
        totalRows: Math.max(csvText.trim().split('\n').length - 1, 0), importedRows: 0,
      });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['data-migration'] }),
  });

  const cancel = useMutation({
    mutationFn: async (id: string) => api.post(`/data-migration/${id}/cancel`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['data-migration'] }),
  });

  const badge = (s: string) => ['COMPLETED', 'TERMINE'].includes(s) ? 'text-green-400 bg-green-500/20' : ['FAILED', 'CANCELLED'].includes(s) ? 'text-red-400 bg-red-500/20' : 'text-yellow-400 bg-yellow-500/20';

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Database className="text-purple-400" /> Assistant de migration de données</h1>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
          <h2 className="text-white font-semibold flex items-center gap-2 mb-3"><FileSpreadsheet className="w-4 h-4 text-purple-400" /> 1. Collez vos données (CSV / Excel)</h2>
          <textarea value={csvText} onChange={(e) => setCsvText(e.target.value)}
            placeholder={'Nom,Telephone,Quartier\nJean Kouassi,+22507000000,Cocody'}
            className="w-full h-40 bg-black/30 border border-white/10 rounded-xl p-3 text-sm text-gray-200 font-mono focus:outline-none focus:border-purple-500/50" />
          <div className="flex items-center gap-3 mt-3">
            <select value={targetType} onChange={(e) => setTargetType(e.target.value)} className="bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200">
              {TARGET_TYPES.map((x) => <option key={x} value={x}>Cible : {x}</option>)}
            </select>
            <button onClick={() => analyze.mutate()} disabled={analyze.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
              {analyze.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Upload className="w-4 h-4" />} Analyser le mapping
            </button>
          </div>
        </div>
        <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
          <h2 className="text-white font-semibold mb-3">2. Mapping intelligent suggéré</h2>
          {!analysis ? <p className="text-sm text-gray-500">Lancez l'analyse pour détecter automatiquement la correspondance des champs.</p> : (
            <>
              {analysis.summary && <p className="text-xs text-gray-400 mb-3">{analysis.summary}</p>}
              <div className="space-y-2 max-h-56 overflow-auto">
                {(analysis.suggestions ?? []).map((s, i) => (
                  <div key={i} className="flex items-center justify-between text-sm bg-black/20 rounded-lg px-3 py-2">
                    <span className="text-gray-300">{s.sourceColumn}</span><span className="text-purple-400">→</span>
                    <span className="text-white font-medium">{s.targetField}</span>
                    <span className={`ml-2 text-xs px-2 py-0.5 rounded-full ${(s.confidence ?? 0) >= 0.8 ? 'text-green-400 bg-green-500/20' : 'text-yellow-400 bg-yellow-500/20'}`}>{Math.round((s.confidence ?? 0) * 100)}%</span>
                  </div>
                ))}
              </div>
              <button onClick={() => execute.mutate()} disabled={execute.isPending} className="mt-4 flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-green-600 to-emerald-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
                {execute.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />} Lancer la migration
              </button>
            </>
          )}
        </div>
      </div>
      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Historique des migrations</h2>
        {(jobsQ.data ?? []).length === 0 ? <p className="text-sm text-gray-500">Aucune migration enregistrée.</p> : (
          <div className="space-y-2">
            {(jobsQ.data ?? []).map((j) => (
              <div key={j.id} className="flex items-center justify-between bg-black/20 rounded-xl px-4 py-3 text-sm">
                <div><span className="text-white font-medium">{j.targetType}</span><span className="text-gray-500 ml-2">{j.importedRows ?? 0}/{j.totalRows ?? 0} lignes</span></div>
                <div className="flex items-center gap-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs ${badge(j.status)}`}>{formatEnum(j.status)}</span>
                  {['PENDING', 'RUNNING'].includes(j.status) && <button onClick={() => cancel.mutate(j.id)} aria-label="Annuler la migration" className="text-red-400 hover:text-red-300"><XCircle className="w-4 h-4" /></button>}
                  {['COMPLETED', 'TERMINE'].includes(j.status) && <CheckCircle className="w-4 h-4 text-green-400" />}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

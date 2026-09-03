import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Mic, Search, UserRound, Smile, Frown, Meh, Heart, Loader2, FileText, ChevronDown, ClipboardList } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface VoiceReport {
  id: string;
  authorId: string;
  authorName?: string;
  transcription: string;
  analysis: string | null;
  createdAt: string;
}

interface Analysis {
  personnes: string[];
  humeur: string;
  besoinPriere: boolean;
  actions: string[];
}

interface ActionItem {
  id: string;
  reportId: string;
  action: string;
  personne?: string;
  statut?: string;
}

const MOOD_STYLES: Record<string, { icon: typeof Smile; cls: string; label: string }> = {
  JOYEUX: { icon: Smile, cls: 'text-green-500', label: 'Joyeux' },
  TRISTE: { icon: Frown, cls: 'text-blue-500', label: 'Triste' },
  NEUTRE: { icon: Meh, cls: 'text-gray-400', label: 'Neutre' },
};

type ReportTab = 'all' | 'mine';

export default function VoiceReportsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [tab, setTab] = useState<ReportTab>('all');
  const [openStructured, setOpenStructured] = useState<string | null>(null);
  const [structured, setStructured] = useState<Record<string, string> | null>(null);
  const [structuredLoading, setStructuredLoading] = useState<string | null>(null);

  const reportsQuery = useQuery({
    queryKey: ['voice-reports', tab],
    queryFn: async () => (await api.get<VoiceReport[]>(tab === 'mine' ? '/voice-reports/mine' : '/voice-reports')).data,
  });

  const actionItemsQuery = useQuery({
    queryKey: ['voice-reports-action-items'],
    queryFn: async () => (await api.get<ActionItem[]>('/voice-reports/action-items')).data,
  });

  const parseAnalysis = (raw: string | null): Analysis | null => {
    if (!raw) return null;
    try { return JSON.parse(raw) as Analysis; } catch { return null; }
  };

  const loadStructured = async (id: string) => {
    if (openStructured === id) { setOpenStructured(null); setStructured(null); return; }
    setStructuredLoading(id);
    try {
      const res = await api.get(`/voice-reports/${id}/structured`);
      setStructured(res.data ?? {});
      setOpenStructured(id);
    } catch { toast.error('Impossible de générer le rapport structuré'); }
    finally { setStructuredLoading(null); }
  };

  const filtered = (reportsQuery.data ?? []).filter(r => !search || r.transcription.toLowerCase().includes(search.toLowerCase()));

  if (reportsQuery.isLoading) return <SkeletonLoader lines={4} variant="card" />;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-pink-500 to-rose-600 text-white shadow-lg">
          <Mic className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Rapports Vocaux</h1>
          <p className="page-subtitle">Dictés sur le terrain, transcrits et analysés par l'IA</p>
        </div>
        <div className="ml-auto flex items-center gap-3">
          <div className="flex rounded-xl bg-white/5 border border-white/10 p-1">
            <button onClick={() => setTab('all')}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${tab === 'all' ? 'bg-pink-500 text-white' : 'text-gray-400 hover:text-white'}`}>
              Tous
            </button>
            <button onClick={() => setTab('mine')}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${tab === 'mine' ? 'bg-pink-500 text-white' : 'text-gray-400 hover:text-white'}`}>
              Mes rapports
            </button>
          </div>
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input placeholder="Rechercher…" className="input pl-9 w-48" value={search} onChange={e => setSearch(e.target.value)} />
          </div>
        </div>
      </div>

      {actionItemsQuery.data && actionItemsQuery.data.length > 0 && (
        <div className="glass-card p-5 mb-6">
          <h2 className="flex items-center gap-2 font-semibold mb-3">
            <ClipboardList className="w-5 h-5 text-amber-500" /> Actions à suivre
          </h2>
          <ul className="space-y-2">
            {actionItemsQuery.data.map(a => (
              <li key={a.id} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                {a.personne ? <strong>{a.personne} :</strong> : null} {a.action}
                {a.statut && <span className="badge badge-ghost text-xs">{a.statut}</span>}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {filtered.map(r => {
          const a = parseAnalysis(r.analysis);
          const mood = a ? MOOD_STYLES[a.humeur] ?? MOOD_STYLES.NEUTRE : MOOD_STYLES.NEUTRE;
          return (
            <div key={r.id} className="glass-card p-5 animate-slide-up">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <UserRound className="w-4 h-4" />
                  {r.authorName ? `${r.authorName} — ` : ''}{new Date(r.createdAt).toLocaleString('fr-FR')}
                </div>
                <span className={`flex items-center gap-1 text-xs font-medium ${mood.cls}`}>
                  <mood.icon className="w-4 h-4" /> {mood.label}
                </span>
              </div>
              <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed border-l-2 border-pink-300 dark:border-pink-700 pl-3">
                « {r.transcription} »
              </p>
              {a && (
                <div className="mt-4 space-y-2">
                  {a.personnes.length > 0 && (
                    <div className="flex flex-wrap gap-1.5">
                      {a.personnes.map(p => <span key={p} className="badge badge-info">{p}</span>)}
                    </div>
                  )}
                  {a.actions.length > 0 && (
                    <ul className="text-xs text-gray-500 space-y-1">
                      {a.actions.map((act, i) => <li key={i}>• {act}</li>)}
                    </ul>
                  )}
                  {a.besoinPriere && (
                    <span className="badge badge-warning inline-flex items-center gap-1">
                      <Heart className="w-3 h-3" /> Besoin de prière
                    </span>
                  )}
                </div>
              )}
              <div className="mt-4 border-t border-white/10 pt-3">
                <button onClick={() => loadStructured(r.id)}
                  className="flex items-center gap-2 text-xs font-medium text-pink-500 hover:text-pink-600 transition">
                  <FileText className="w-4 h-4" />
                  {structuredLoading === r.id ? 'Génération…' : 'Rapport structuré'}
                  <ChevronDown className={`w-3 h-3 transition ${openStructured === r.id ? 'rotate-180' : ''}`} />
                </button>
                {openStructured === r.id && structured && (
                  <pre className="mt-3 text-xs text-gray-600 dark:text-gray-300 bg-black/5 dark:bg-white/5 rounded-lg p-3 whitespace-pre-wrap max-h-64 overflow-y-auto">
                    {JSON.stringify(structured, null, 2)}
                  </pre>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {!reportsQuery.isLoading && filtered.length === 0 && (
        <EmptyState icon={<Mic className="w-8 h-8 text-gray-400" />}
          title="Aucun rapport vocal"
          message="Les rapports dictés depuis l'application mobile apparaîtront ici" />
      )}
    </div>
  );
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { Loader2, Eye, Plus, Link2, Moon, Sun, Flame, CloudRainWind, HeartHandshake, Sparkles, Lightbulb, Globe2, Lock } from 'lucide-react';

interface PropheticEntry {
  id: string;
  authorId: string;
  type: EntryType;
  title: string;
  content: string;
  tags: string | null;
  scope: string | null;
  isPublic: boolean;
  createdAt: string;
}

type EntryType = 'VISION' | 'REVE' | 'PROPHETIE' | 'CONVICTION' | 'AVERTISMENT' | 'ENCOURAGEMENT' | 'REVELATION';

const TYPE_META: Record<EntryType, { icon: typeof Eye; label: string; cls: string }> = {
  VISION: { icon: Eye, label: 'Vision', cls: 'from-indigo-500 to-violet-600' },
  REVE: { icon: Moon, label: 'Rêve', cls: 'from-slate-500 to-slate-700' },
  PROPHETIE: { icon: Sparkles, label: 'Prophétie', cls: 'from-amber-500 to-orange-600' },
  CONVICTION: { icon: Flame, label: 'Conviction', cls: 'from-red-500 to-rose-600' },
  AVERTISMENT: { icon: CloudRainWind, label: 'Avertissement', cls: 'from-gray-500 to-gray-700' },
  ENCOURAGEMENT: { icon: HeartHandshake, label: 'Encouragement', cls: 'from-emerald-500 to-teal-600' },
  REVELATION: { icon: Lightbulb, label: 'Révélation', cls: 'from-yellow-400 to-amber-500' },
};

/** Journal Prophétique — visions, rêves & révélations avec veille de corrélation IA. */
export default function PropheticJournalPage() {
  const [tab, setTab] = useState<'mine' | 'public'>('mine');
  const [showForm, setShowForm] = useState(false);
  const [correlating, setCorrelating] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const [form, setForm] = useState({
    type: 'VISION' as EntryType,
    title: '',
    content: '',
    tags: '',
    isPublic: false,
  });

  const entriesQuery = useQuery({
    queryKey: ['prophetic', tab],
    queryFn: async () =>
      (await api.get<PropheticEntry[]>(tab === 'mine' ? '/prophetic/mine' : '/prophetic/public')).data,
  });

  const correlatedQuery = useQuery({
    enabled: correlating !== null,
    queryKey: ['prophetic-correlated', correlating],
    queryFn: async () =>
      (await api.get<PropheticEntry[]>(`/prophetic/${correlating}/correlated`)).data,
  });

  const createMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post<PropheticEntry>('/prophetic', {
          type: form.type,
          title: form.title,
          content: form.content,
          tags: form.tags || null,
          isPublic: form.isPublic,
        })
      ).data,
    onSuccess: () => {
      setForm({ type: 'VISION', title: '', content: '', tags: '', isPublic: false });
      setShowForm(false);
      queryClient.invalidateQueries({ queryKey: ['prophetic'] });
    },
  });

  const statsQuery = useQuery({
    queryKey: ['prophetic-stats'],
    queryFn: async () => (await api.get<Record<string, number>>('/prophetic/stats')).data,
  });

  const parseTags = (raw: string | null): string[] => {
    if (!raw) return [];
    return raw.split(/[,;]+/).map((t) => t.trim()).filter(Boolean);
  };

  const entryCard = (e: PropheticEntry, showCorrelation: boolean) => {
    const meta = TYPE_META[e.type] ?? TYPE_META.REVELATION;
    const Icon = meta.icon;
    return (
      <div key={e.id} className="glass-card p-5 animate-slide-up">
        <div className="flex items-start gap-3">
          <div className={`p-2 rounded-lg bg-gradient-to-br ${meta.cls} text-white shadow shrink-0`}>
            <Icon className="w-4 h-4" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">{e.title}</h3>
              <span className="text-[10px] uppercase tracking-wide px-2 py-0.5 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-300">
                {meta.label}
              </span>
              {e.isPublic ? (
                <span title="Partagé"><Globe2 className="w-3.5 h-3.5 text-emerald-500" /></span>
              ) : (
                <span title="Privé"><Lock className="w-3.5 h-3.5 text-gray-400" /></span>
              )}
            </div>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap line-clamp-4">
              {e.content}
            </p>
            <div className="mt-2 flex items-center gap-2 flex-wrap">
              {parseTags(e.tags).map((t) => (
                <span key={t} className="text-[11px] px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300">
                  #{t}
                </span>
              ))}
              <span className="ml-auto text-[11px] text-gray-400">
                {new Date(e.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
              </span>
            </div>

            {showCorrelation && (
              <>
                <button
                  onClick={() => setCorrelating(correlating === e.id ? null : e.id)}
                  className="btn-sm mt-3 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg glass-card text-xs hover:shadow-md"
                >
                  <Link2 className="w-3.5 h-3.5" />
                  Veille de corrélation
                </button>
                {correlating === e.id && (
                  <div className="mt-3 border-l-2 border-primary-400 pl-3 space-y-2">
                    {correlatedQuery.isLoading && <Loader2 className="w-4 h-4 animate-spin text-primary-500" />}
                    {correlatedQuery.data && correlatedQuery.data.length === 0 && (
                      <p className="text-xs text-gray-500">Aucune entrée corrélée pour l'instant.</p>
                    )}
                    {(correlatedQuery.data ?? []).map((c) => {
                      const cm = TYPE_META[c.type] ?? TYPE_META.REVELATION;
                      return (
                        <p key={c.id} className="text-xs text-gray-600 dark:text-gray-400">
                          <span className="font-medium text-gray-800 dark:text-gray-200">{cm.label} — {c.title}</span>{' '}
                          ({new Date(c.createdAt).toLocaleDateString('fr-FR')})
                        </p>
                      );
                    })}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-violet-500 to-indigo-700 text-white shadow-lg">
          <Eye className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Journal Prophétique</h1>
          <p className="page-subtitle">Visions, rêves & révélations — corrélés automatiquement par IA</p>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1">
          {showForm ? <Loader2 className="w-4 h-4 animate-spin hidden" /> : <Plus className="w-4 h-4" />}
          Nouvelle entrée
        </button>
      </div>

      {/* Statistiques */}
      {statsQuery.data && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {[
            { key: 'totalEntries', label: 'Entrées totales', cls: 'from-violet-500 to-indigo-600' },
            { key: 'publicEntries', label: 'Partagées', cls: 'from-emerald-500 to-teal-600' },
            { key: 'visions', label: 'Visions', cls: 'from-indigo-500 to-violet-600' },
            { key: 'reves', label: 'Rêves', cls: 'from-slate-500 to-slate-700' },
          ].map(({ key, label, cls }) => (
            <div key={key} className={`stat-card bg-gradient-to-br ${cls}`}>
              <Eye className="w-5 h-5 opacity-80" />
              <p className="stat-value">{(statsQuery.data?.[key] ?? 0).toLocaleString('fr-FR')}</p>
              <p className="text-xs opacity-80">{label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Formulaire de création */}
      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4 animate-slide-up">
          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Type</label>
              <select className="input w-full" value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value as EntryType })}>
                {(Object.keys(TYPE_META) as EntryType[]).map((t) => (
                  <option key={t} value={t}>{TYPE_META[t].label}</option>
                ))}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Titre</label>
              <input className="input w-full" placeholder="Ce que j'ai reçu…" value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Contenu</label>
            <textarea className="input w-full min-h-[110px]" placeholder="Décrivez la vision, le rêve ou la révélation…"
              value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
          </div>
          <div className="flex gap-4 items-end flex-wrap">
            <div className="flex-1 min-w-[180px]">
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                Tags (séparés par des virgules)
              </label>
              <input className="input w-full" placeholder="famille, décrochage, jeunesse"
                value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })} />
            </div>
            <label className="inline-flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300 pb-2">
              <input type="checkbox" checked={form.isPublic}
                onChange={(e) => setForm({ ...form, isPublic: e.target.checked })} />
              Partager avec l'église
            </label>
            <button
              onClick={() => createMutation.mutate()}
              disabled={!form.title.trim() || !form.content.trim() || createMutation.isPending}
              className="btn-primary btn-sm inline-flex items-center gap-1 whitespace-nowrap">
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              Enregistrer
            </button>
          </div>
        </div>
      )}

      {/* Onglets */}
      <div className="flex gap-2 mb-6">
        <button onClick={() => setTab('mine')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'mine' ? 'bg-gradient-to-r from-violet-500 to-indigo-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Mes entrées
        </button>
        <button onClick={() => setTab('public')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'public' ? 'bg-gradient-to-r from-violet-500 to-indigo-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Partagées par l'église
        </button>
      </div>

      {/* Liste */}
      {entriesQuery.isLoading && <Loader2 className="w-8 h-8 animate-spin text-primary-500 mx-auto mt-20" />}
      {entriesQuery.data && entriesQuery.data.length === 0 && (
        <div className="glass-card p-12 text-center animate-slide-up">
          <Eye className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400">
            {tab === 'mine'
              ? "Aucune entrée pour l'instant — notez vos visions et rêves dès qu'ils arrivent."
              : "Aucune révélation partagée par l'église pour le moment."}
          </p>
        </div>
      )}
      <div className="space-y-4">
        {(entriesQuery.data ?? []).map((e) => entryCard(e, true))}
      </div>
    </div>
  );
}

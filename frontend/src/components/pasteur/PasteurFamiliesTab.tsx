import { useState, useCallback, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Family, PageResponse } from '@/types';
import {
  Users, Plus, Search, Eye, Edit3, Trash2, ArrowLeft,
  Loader2, AlertTriangle, Shield, History, ChevronRight,
  BarChart3, Filter, RotateCcw, Archive,
  Bot, Send, Sparkles, X, MessageSquare, Copy, Check,
} from 'lucide-react';
import toast from 'react-hot-toast';

type ViewMode = 'liste' | 'detail' | 'create' | 'edit' | 'archive';

interface AiChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

const FAMILY_SUGGESTIONS = [
  'Quelles familles sont à risque ?',
  'Résumé des présences cette semaine',
  'Quels nouveaux convertis suivre en priorité ?',
  'Génère un rapport pastoral pour les familles',
];

export default function PasteurFamiliesTab() {
  const queryClient = useQueryClient();
  const [aiOpen, setAiOpen] = useState(false);
  const [aiInput, setAiInput] = useState('');
  const [aiMessages, setAiMessages] = useState<AiChatMessage[]>([]);
  const aiMessagesEndRef = useRef<HTMLDivElement>(null);
  const [aiCopiedId, setAiCopiedId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [riskFilter, setRiskFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedFamily, setSelectedFamily] = useState<Family | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [form, setForm] = useState({ nom: '', chefFamilleId: '' });
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['families', 'pasteur', page, search, riskFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (riskFilter) params.set('niveauRisque', riskFilter);
      const res = await api.get(`/families?${params}`);
      return res.data as PageResponse<Family>;
    },
    enabled: view === 'liste',
  });

  const { data: users } = useQuery({
    queryKey: ['users', 'chefs'],
    queryFn: async () => {
      const res = await api.get('/users?size=200');
      return res.data.content as { id: string; firstName: string; lastName: string }[];
    },
    enabled: view === 'create' || view === 'edit',
  });

  const { data: familyDetail } = useQuery({
    queryKey: ['families', selectedFamily?.id],
    queryFn: async () => {
      const res = await api.get(`/families/${selectedFamily!.id}`);
      return res.data;
    },
    enabled: !!selectedFamily && view === 'detail',
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/families/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['families'] }); toast.success('Famille supprimée'); setShowDeleteConfirm(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof form) => { await api.post('/families', { ...data, chefFamilleId: data.chefFamilleId || undefined }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['families'] }); toast.success('Famille créée'); setView('liste'); setForm({ nom: '', chefFamilleId: '' }); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof form }) => { await api.put(`/families/${id}`, { ...data, chefFamilleId: data.chefFamilleId || undefined }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['families'] }); toast.success('Famille mise à jour'); setView('liste'); setEditingId(null); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const riskColor = (risk?: string) => {
    if (risk === 'A_RISQUE') return 'text-red-500 bg-red-50 dark:bg-red-900/20';
    if (risk === 'SOUS_SURVEILLANCE') return 'text-amber-500 bg-amber-50 dark:bg-amber-900/20';
    return 'text-green-500 bg-green-50 dark:bg-green-900/20';
  };

  const riskLabel = (risk?: string) => {
    const m: Record<string, string> = { A_RISQUE: 'À risque', SOUS_SURVEILLANCE: 'Sous surveillance', NORMAL: 'Normal' };
    return m[risk || 'NORMAL'] || risk || 'Normal';
  };

  const handleSubmit = useCallback(() => {
    if (!form.nom.trim()) { toast.error('Le nom est obligatoire'); return; }
    if (editingId) { updateMutation.mutate({ id: editingId, data: form }); }
    else { createMutation.mutate(form); }
  }, [form, editingId, createMutation, updateMutation]);

  const aiMutation = useMutation({
    mutationFn: async (content: string) => {
      const userMsg: AiChatMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        content,
        timestamp: new Date().toISOString(),
      };
      setAiMessages((prev) => [...prev, userMsg]);
      try {
        const res = await api.post('/ai/chat', { message: content });
        return res.data as { reply: string; sources?: string[] };
      } catch {
        return { reply: 'Désolé, je ne peux pas répondre pour le moment. Vérifiez que l\'assistant IA est configuré.', sources: [] };
      }
    },
    onSuccess: (data) => {
      const assistantMsg: AiChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: data.reply,
        timestamp: new Date().toISOString(),
      };
      setAiMessages((prev) => [...prev, assistantMsg]);
    },
    onError: () => {
      toast.error('Erreur de communication avec l\'assistant IA');
    },
  });

  const handleAiSend = useCallback(() => {
    if (!aiInput.trim() || aiMutation.isPending) return;
    aiMutation.mutate(aiInput.trim());
    setAiInput('');
  }, [aiInput, aiMutation]);

  const handleAiKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleAiSend();
    }
  };

  const handleAiCopy = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setAiCopiedId(id);
    setTimeout(() => setAiCopiedId(null), 2000);
  };

  useEffect(() => {
    aiMessagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [aiMessages]);

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedFamily) {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedFamily(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{selectedFamily.nom}</h2>
              <p className="text-sm text-gray-500">Chef: {selectedFamily.chefFamilleNom || '—'}</p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => { setForm({ nom: selectedFamily.nom, chefFamilleId: selectedFamily.chefFamilleId }); setEditingId(selectedFamily.id); setView('edit'); }} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
              <Link to={`/families/${selectedFamily.id}`} className="btn-primary btn-sm"><Eye className="w-4 h-4" /> Vue complète</Link>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Statut</p>
              <p className="font-semibold text-sm">{selectedFamily.statut}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Niveau de risque</p>
              <p className={`font-semibold text-sm px-2 py-0.5 rounded-full inline-block ${riskColor(selectedFamily.niveauRisque)}`}>
                {riskLabel(selectedFamily.niveauRisque)}
              </p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Créée le</p>
              <p className="font-semibold text-sm">{new Date(selectedFamily.createdAt).toLocaleDateString('fr-FR')}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Chef adjoint</p>
              <p className="font-semibold text-sm">{selectedFamily.chefAdjointNom || '—'}</p>
            </div>
          </div>
          {familyDetail?.souls && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Âmes de la famille ({familyDetail.souls.length})</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {familyDetail.souls.map((s: any) => (
                  <Link key={s.id} to={`/souls/${s.id}`} className="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/30">
                    <div className="w-6 h-6 rounded-full bg-rose-100 dark:bg-rose-900/30 flex items-center justify-center text-[9px] font-bold text-rose-600">{(s.prenom || s.nom || '?').charAt(0)}</div>
                    <span className="text-xs font-medium">{s.prenom} {s.nom}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
          <div className="flex gap-2">
            <Link to={`/families/${selectedFamily.id}`} className="btn-secondary btn-sm"><BarChart3 className="w-4 h-4" /> Performance</Link>
            <Link to={`/families/compare`} className="btn-secondary btn-sm"><BarChart3 className="w-4 h-4" /> Comparer</Link>
          </div>
        </div>
      </div>
    );
  }

  // === VUE CREATE / EDIT ===
  if (view === 'create' || view === 'edit') {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setEditingId(null); setForm({ nom: '', chefFamilleId: '' }); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{editingId ? 'Modifier la famille' : 'Nouvelle famille'}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom de la famille *</label>
              <input className="input" value={form.nom} onChange={e => setForm({ ...form, nom: e.target.value })} placeholder="Ex: Famille Mukendi" />
            </div>
            <div>
              <label className="label">Chef de famille</label>
              <select className="input" value={form.chefFamilleId} onChange={e => setForm({ ...form, chefFamilleId: e.target.value })}>
                <option value="">Sélectionner...</option>
                {(users || []).map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button onClick={() => { setView('liste'); setEditingId(null); }} className="btn-secondary">Annuler</button>
            <button onClick={handleSubmit} disabled={createMutation.isPending || updateMutation.isPending} className="btn-primary">
              {(createMutation.isPending || updateMutation.isPending) && <Loader2 className="w-4 h-4 animate-spin" />}
              {editingId ? 'Enregistrer' : 'Créer'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // === VUE LISTE ===
  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Users className="w-5 h-5 text-primary-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Familles</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements} résultats)</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/families/compare" className="btn-secondary btn-sm"><BarChart3 className="w-4 h-4" /> Comparer</Link>
          <button onClick={() => { setForm({ nom: '', chefFamilleId: '' }); setEditingId(null); setView('create'); }} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouvelle famille
          </button>
        </div>
      </div>

      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher une famille..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Risque</span>
              <select value={riskFilter} onChange={e => { setRiskFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous</option>
                <option value="A_RISQUE">À risque</option>
                <option value="SOUS_SURVEILLANCE">Sous surveillance</option>
                <option value="NORMAL">Normal</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Famille</th>
                  <th>Chef</th>
                  <th>Statut</th>
                  <th>Risque</th>
                  <th>Créée le</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content || []).map(fam => (
                  <tr key={fam.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                    <td>
                      <button onClick={() => { setSelectedFamily(fam); setView('detail'); }} className="text-primary-600 hover:text-primary-700 font-medium hover:underline">{fam.nom}</button>
                    </td>
                    <td className="text-sm text-gray-500">{fam.chefFamilleNom || '—'}</td>
                    <td><span className="badge-info">{fam.statut}</span></td>
                    <td><span className={`badge text-[10px] ${riskColor(fam.niveauRisque)}`}>{riskLabel(fam.niveauRisque)}</span></td>
                    <td className="text-sm text-gray-500">{new Date(fam.createdAt).toLocaleDateString('fr-FR')}</td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setSelectedFamily(fam); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Voir"><Eye className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => { setForm({ nom: fam.nom, chefFamilleId: fam.chefFamilleId }); setEditingId(fam.id); setView('edit'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Modifier"><Edit3 className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => setShowDeleteConfirm(fam.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer"><Trash2 className="w-3.5 h-3.5 text-red-400" /></button>
                      </div>
                    </td>
                  </tr>
                ))}
                {(data?.content || []).length === 0 && <tr><td colSpan={6} className="py-12 text-center text-gray-400">Aucune famille trouvée</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number * data.size + 1} à {Math.min((data.number + 1) * data.size, data.totalElements)} sur {data.totalElements}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-sm w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-2">Supprimer cette famille ?</h3>
            <p className="text-sm text-gray-500 mb-4">Les âmes ne seront pas supprimées.</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary">Annuler</button>
              <button onClick={() => deleteMutation.mutate(showDeleteConfirm)} className="btn-primary bg-red-600 hover:bg-red-700"><Trash2 className="w-4 h-4" /> Supprimer</button>
            </div>
          </div>
        </div>
      )}

      {/* === COPILOT IA FLOTTANT === */}
      {aiOpen && (
        <div className="fixed bottom-20 right-4 z-40 w-[380px] max-h-[500px] rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 flex flex-col animate-slide-up overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200/50 dark:border-gray-700/50 bg-gradient-to-r from-violet-500/10 to-purple-500/10 dark:from-violet-500/5 dark:to-purple-500/5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white flex items-center justify-center">
                <Bot className="w-4 h-4" />
              </div>
              <div>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">Copilot Familles</p>
                <p className="text-[10px] text-gray-400">IA locale • Aucune donnée externe</p>
              </div>
            </div>
            <button onClick={() => setAiOpen(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
              <X className="w-4 h-4 text-gray-500" />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3 min-h-[200px]">
            {aiMessages.length === 0 && (
              <div className="text-center py-4">
                <Sparkles className="w-8 h-8 text-violet-400 mx-auto mb-2" />
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Posez une question sur vos familles</p>
                <p className="text-xs text-gray-400 mb-3">L'IA connaît les données de votre église</p>
                <div className="space-y-1.5">
                  {FAMILY_SUGGESTIONS.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => { setAiInput(s); }}
                      className="w-full text-left p-2 rounded-lg border border-gray-200/50 dark:border-gray-700/50 hover:border-violet-300 dark:hover:border-violet-600 hover:bg-violet-50/50 dark:hover:bg-violet-900/10 transition-all text-xs text-gray-600 dark:text-gray-400"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {aiMessages.map((msg) => (
              <div key={msg.id} className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                {msg.role === 'assistant' && (
                  <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                    <Bot className="w-3 h-3 text-white" />
                  </div>
                )}
                <div className={`max-w-[80%] p-3 rounded-xl text-sm ${
                  msg.role === 'user'
                    ? 'bg-primary-600 text-white rounded-br-md'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-md'
                }`}>
                  <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                  <div className="flex items-center gap-2 mt-1.5">
                    <span className="text-[9px] opacity-50">
                      {new Date(msg.timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                    {msg.role === 'assistant' && (
                      <button onClick={() => handleAiCopy(msg.id, msg.content)} className="opacity-50 hover:opacity-100">
                        {aiCopiedId === msg.id ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {aiMutation.isPending && (
              <div className="flex gap-2">
                <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
                  <Bot className="w-3 h-3 text-white" />
                </div>
                <div className="bg-gray-100 dark:bg-gray-800 rounded-xl rounded-bl-md p-3">
                  <div className="flex items-center gap-2 text-xs text-gray-500">
                    <Loader2 className="w-3 h-3 animate-spin" />
                    L'IA réfléchit...
                  </div>
                </div>
              </div>
            )}

            <div ref={aiMessagesEndRef} />
          </div>

          {/* Input */}
          <div className="border-t border-gray-200/50 dark:border-gray-700/50 px-4 py-3">
            <div className="flex gap-2 items-end">
              <textarea
                value={aiInput}
                onChange={(e) => setAiInput(e.target.value)}
                onKeyDown={handleAiKeyDown}
                placeholder="Question sur les familles..."
                className="flex-1 resize-none rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-violet-500 focus:border-transparent placeholder:text-gray-400 dark:placeholder:text-gray-500"
                rows={1}
                disabled={aiMutation.isPending}
              />
              <button
                onClick={handleAiSend}
                disabled={!aiInput.trim() || aiMutation.isPending}
                className="rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white p-2.5 hover:from-violet-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              >
                {aiMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Floating AI button */}
      <button
        onClick={() => setAiOpen(!aiOpen)}
        className={`fixed bottom-4 right-4 z-40 w-14 h-14 rounded-full shadow-lg flex items-center justify-center transition-all ${
          aiOpen
            ? 'bg-gray-800 dark:bg-gray-700 text-white rotate-0'
            : 'bg-gradient-to-br from-violet-500 to-purple-600 text-white hover:from-violet-600 hover:to-purple-700 hover:scale-105'
        }`}
        title="Copilot IA Familles"
      >
        {aiOpen ? <X className="w-5 h-5" /> : <Sparkles className="w-5 h-5" />}
      </button>
    </div>
  );
}

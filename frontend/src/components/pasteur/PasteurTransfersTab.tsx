import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  ArrowLeftRight, Plus, Search, Eye, Filter, CheckCircle, XCircle,
  Clock, Loader2, AlertTriangle, ChevronRight, X, ArrowLeft,
  History, MessageSquare, Send, Archive, Ban, CheckSquare, Square,
  ChevronDown, ChevronUp,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';

interface Transfer {
  id: string; type: string; statut: string; personneNom?: string; cible?: string;
  demandeurNom?: string; dateSoumission?: string; priorite?: string;
  typeTransfert?: string; motif?: string; raison?: string;
  departementSourceNom?: string; departementCibleNom?: string;
  familleSourceNom?: string; familleCibleNom?: string;
  createdAt: string; updatedAt?: string;
}

interface TransferDetail extends Transfer {
  decisions?: { id: string; decisioneurNom?: string; decision: string; commentaire?: string; dateDecision: string }[];
  historique?: { action: string; utilisateurNom?: string; date: string; details?: string }[];
}

interface DecisionForm { decision: string; commentaire: string; }

export default function PasteurTransfersTab() {
  const { locale } = useI18n();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [prioriteFilter, setPrioriteFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [showDetail, setShowDetail] = useState<TransferDetail | null>(null);
  const [showDecision, setShowDecision] = useState<Transfer | null>(null);
  const [decisionForm, setDecisionForm] = useState<DecisionForm>({ decision: 'APPROBATION', commentaire: '' });
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [selectAll, setSelectAll] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['transfers', 'pasteur', page, search, statutFilter, typeFilter, prioriteFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/transfers?${params}`);
      let result = res.data as PageResponse<Transfer>;
      // Client-side additional filters
      if (typeFilter && result.content) result = { ...result, content: result.content.filter((t: any) => t.type?.includes(typeFilter)) };
      if (prioriteFilter && result.content) result = { ...result, content: result.content.filter((t: any) => t.priorite === prioriteFilter) };
      return result;
    },
  });

  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ['transfers', showDetail?.id],
    queryFn: async () => {
      const [detail, history, decisions] = await Promise.all([
        api.get(`/transfers/${showDetail!.id}`),
        api.get(`/transfers/${showDetail!.id}/history`).catch(() => ({ data: [] })),
        api.get(`/transfers/${showDetail!.id}/decisions`).catch(() => ({ data: [] })),
      ]);
      return { ...detail.data, historique: history.data, decisions: decisions.data } as TransferDetail;
    },
    enabled: !!showDetail?.id,
  });

  // Mutations for workflow
  const decideMutation = useMutation({
    mutationFn: async ({ id, decision, commentaire }: { id: string; decision: string; commentaire: string }) => {
      await api.post(`/transfers/${id}/decide`, { decision, commentaire });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['transfers'] }); toast.success('Décision enregistrée'); setShowDecision(null); setShowDetail(null); },
    onError: () => toast.error('Erreur lors de la décision'),
  });

  const cancelMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/transfers/${id}/cancel`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['transfers'] }); toast.success('Transfert annulé'); setShowDetail(null); },
    onError: () => toast.error('Erreur lors de l\'annulation'),
  });

  const archiveMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/transfers/${id}/archive`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['transfers'] }); toast.success('Transfert archivé'); setShowDetail(null); },
    onError: () => toast.error('Erreur lors de l\'archivage'),
  });

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => { const next = new Set(prev); next.has(id) ? next.delete(id) : next.add(id); return next; });
  };
  const toggleSelectAll = () => {
    if (selectAll) { setSelectedIds(new Set()); } else { setSelectedIds(new Set((data?.content || []).filter((t: any) => t.statut === 'EN_ATTENTE_VALIDATION').map((t: any) => t.id))); }
    setSelectAll(!selectAll);
  };

  const statutBadge = (s: string) => {
    const map: Record<string, { cls: string; icon: any; label: string }> = {
      BROUILLON: { cls: 'badge-gray', icon: null, label: 'Brouillon' },
      SOUMIS: { cls: 'badge-info', icon: Send, label: 'Soumis' },
      EN_ATTENTE_VALIDATION: { cls: 'badge-warning', icon: Clock, label: 'En attente' },
      VALIDATION_PARTIELLE: { cls: 'badge-warning', icon: Clock, label: 'Validation partielle' },
      VALIDE: { cls: 'badge-success', icon: CheckCircle, label: 'Validé' },
      EXECUTE: { cls: 'badge-success', icon: CheckCircle, label: 'Exécuté' },
      REFUSE: { cls: 'badge-error', icon: XCircle, label: 'Refusé' },
      ANNULE: { cls: 'badge-gray', icon: Ban, label: 'Annulé' },
      ARCHIVE: { cls: 'badge-info', icon: Archive, label: 'Archivé' },
    };
    const m = map[s] || { cls: 'badge-info', icon: null, label: s };
    const Icon = m.icon;
    return <span className={`badge text-[10px] ${m.cls}`}>{Icon && <Icon className="w-3 h-3 inline mr-1" />}{m.label}</span>;
  };

  const decisionColor = (d: string) => {
    if (d === 'APPROBATION') return 'text-green-600 bg-green-50 dark:bg-green-900/20';
    if (d === 'REFUS') return 'text-red-600 bg-red-50 dark:bg-red-900/20';
    if (d === 'DEMANDE_INFORMATIONS') return 'text-amber-600 bg-amber-50 dark:bg-amber-900/20';
    return 'text-gray-600 bg-gray-50 dark:bg-gray-800/50';
  };

  const typeLabel = (t?: string) => {
    if (!t) return '—';
    const m: Record<string, string> = {
      SOUL_TRANSFERT: 'Transfert âme', FAISEUR_TRANSFERT: 'Transfert faiseur',
      CHEF_FAMILLE_TRANSFERT: 'Transfert chef', DEPARTEMENT_TRANSFERT: 'Transfert département',
      MEMBRE_DEPARTEMENT_TRANSFERT: 'Transfert membre',
    };
    return m[t] || t.replace(/_/g, ' ');
  };

  // === VUE DÉTAIL ===
  if (showDetail) {
    const t = detailData || showDetail;
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setShowDetail(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
                {t.personneNom || '—'} → {t.cible || '—'}
              </h2>
              <p className="text-sm text-gray-500">{typeLabel(t.type || t.typeTransfert)}</p>
            </div>
            <div className="flex gap-2 flex-wrap">
              {statutBadge(t.statut)}
              {t.priorite === 'HAUTE' && <span className="text-[10px] bg-red-100 dark:bg-red-900/30 text-red-600 px-2 py-0.5 rounded-full font-semibold uppercase">Priorité</span>}
            </div>
          </div>

          {/* Infos */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Demandeur</p>
              <p className="font-semibold text-sm">{t.demandeurNom || '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Date soumission</p>
              <p className="font-semibold text-sm">{t.dateSoumission ? new Date(t.dateSoumission).toLocaleDateString(locale) : '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Source</p>
              <p className="font-semibold text-sm">{t.departementSourceNom || t.familleSourceNom || '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Cible</p>
              <p className="font-semibold text-sm">{t.departementCibleNom || t.familleCibleNom || t.cible || '—'}</p>
            </div>
          </div>

          {t.motif && (
            <div className="p-4 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 mb-4">
              <p className="text-xs font-medium text-amber-600 mb-1">Motif</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{t.motif}</p>
            </div>
          )}
          {t.raison && (
            <div className="p-4 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/30 mb-4">
              <p className="text-xs font-medium text-blue-600 mb-1">Raison</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{t.raison}</p>
            </div>
          )}

          {/* Décisions */}
          {t.decisions && t.decisions.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2"><MessageSquare className="w-4 h-4" /> Décisions ({t.decisions.length})</h3>
              <div className="space-y-2">
                {t.decisions.map((d: any) => (
                  <div key={d.id} className={`p-3 rounded-xl border ${decisionColor(d.decision)}`}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-semibold">{d.decision?.replace(/_/g, ' ')}</span>
                      <span className="text-[10px] text-gray-400">{d.dateDecision ? new Date(d.dateDecision).toLocaleDateString(locale, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'}</span>
                    </div>
                    <p className="text-xs">Par {d.decisioneurNom || '—'}</p>
                    {d.commentaire && <p className="text-xs mt-1 opacity-80">{d.commentaire}</p>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Historique */}
          {t.historique && t.historique.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2"><History className="w-4 h-4" /> Historique ({t.historique.length})</h3>
              <div className="space-y-1">
                {t.historique.map((h: any, i: number) => (
                  <div key={i} className="flex items-start gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                    <div className="w-2 h-2 rounded-full bg-primary-400 mt-1.5 flex-shrink-0" />
                    <div className="min-w-0">
                      <p className="text-xs text-gray-900 dark:text-gray-100"><span className="font-medium">{h.utilisateurNom || 'Système'}</span> — {h.action?.replace(/_/g, ' ')}</p>
                      {h.details && <p className="text-[10px] text-gray-400">{h.details}</p>}
                      <p className="text-[10px] text-gray-400">{h.date ? new Date(h.date).toLocaleDateString(locale, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-2 pt-4 border-t border-gray-200 dark:border-gray-700">
            {t.statut === 'EN_ATTENTE_VALIDATION' && (
              <>
                <button onClick={() => setShowDecision({ ...t, id: t.id } as any)} className="btn-primary btn-sm bg-emerald-600 hover:bg-emerald-700"><CheckCircle className="w-4 h-4" /> Décider</button>
                <button onClick={() => cancelMutation.mutate(t.id)} disabled={cancelMutation.isPending} className="btn-secondary btn-sm text-red-600"><Ban className="w-4 h-4" /> Annuler</button>
              </>
            )}
            {(t.statut === 'EXECUTE' || t.statut === 'REFUSE') && (
              <button onClick={() => archiveMutation.mutate(t.id)} disabled={archiveMutation.isPending} className="btn-secondary btn-sm"><Archive className="w-4 h-4" /> Archiver</button>
            )}
            <Link to={`/transfers/${t.id}`} className="btn-secondary btn-sm ml-auto"><Eye className="w-4 h-4" /> Vue complète</Link>
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
          <ArrowLeftRight className="w-5 h-5 text-violet-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Transferts</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          {selectedIds.size > 0 && (
            <span className="text-xs text-primary-600 font-medium flex items-center gap-1">
              <CheckSquare className="w-4 h-4" /> {selectedIds.size} sélectionné(s)
            </span>
          )}
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/transfers/new" className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouveau transfert</Link>
          <Link to="/admin/transfers" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Admin workflows</Link>
        </div>
      </div>

      {/* Search + Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un transfert..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous statuts</option>
              <option value="SOUMIS">Soumis</option>
              <option value="EN_ATTENTE_VALIDATION">En attente</option>
              <option value="VALIDE">Validé</option>
              <option value="EXECUTE">Exécuté</option>
              <option value="REFUSE">Refusé</option>
              <option value="ANNULE">Annulé</option>
              <option value="ARCHIVE">Archivé</option>
            </select>
            <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous types</option>
              <option value="SOUL">Transfert âme</option>
              <option value="FAISEUR">Transfert faiseur</option>
              <option value="CHEF_FAMILLE">Transfert chef</option>
              <option value="DEPARTEMENT">Transfert département</option>
              <option value="MEMBRE_DEPARTEMENT">Transfert membre</option>
            </select>
            <select value={prioriteFilter} onChange={e => { setPrioriteFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Toutes priorités</option>
              <option value="HAUTE">Haute</option>
              <option value="MOYENNE">Moyenne</option>
              <option value="BASSE">Basse</option>
            </select>
          </div>
        )}
      </div>

      {/* Bulk select for pending transfers */}
      {statutFilter === 'EN_ATTENTE_VALIDATION' && (data?.content || []).length > 0 && (
        <div className="flex items-center gap-3 mb-3 px-2">
          <button onClick={toggleSelectAll} className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700">
            {selectAll ? <CheckSquare className="w-4 h-4 text-primary-500" /> : <Square className="w-4 h-4" />}
            {selectAll ? 'Tout désélectionner' : 'Tout sélectionner'}
          </button>
          {selectedIds.size > 0 && <span className="text-xs text-primary-600 font-medium">{selectedIds.size} sélectionné(s)</span>}
        </div>
      )}

      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-2">
          {(data?.content || []).map(t => (
            <div key={t.id} className={`glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors cursor-pointer ${selectedIds.has(t.id) ? 'ring-2 ring-primary-400' : ''}`}
              onClick={() => setShowDetail(t)}>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  {t.statut === 'EN_ATTENTE_VALIDATION' && (
                    <button onClick={(e) => { e.stopPropagation(); toggleSelect(t.id); }} className="flex-shrink-0">
                      {selectedIds.has(t.id) ? <CheckSquare className="w-4 h-4 text-primary-500" /> : <Square className="w-4 h-4 text-gray-300" />}
                    </button>
                  )}
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
                    t.statut === 'EXECUTE' ? 'bg-green-50 dark:bg-green-900/20' :
                    t.statut === 'REFUSE' ? 'bg-red-50 dark:bg-red-900/20' :
                    t.statut === 'EN_ATTENTE_VALIDATION' ? 'bg-amber-50 dark:bg-amber-900/20' :
                    'bg-blue-50 dark:bg-blue-900/20'
                  }`}>
                    <ArrowLeftRight className={`w-5 h-5 ${
                      t.statut === 'EXECUTE' ? 'text-green-500' :
                      t.statut === 'REFUSE' ? 'text-red-500' :
                      t.statut === 'EN_ATTENTE_VALIDATION' ? 'text-amber-500' : 'text-blue-500'
                    }`} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {t.personneNom || '—'} → {t.cible || '—'}
                    </p>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5 flex-wrap">
                      <span className="badge-info text-[9px]">{typeLabel(t.type || t.typeTransfert)}</span>
                      {t.demandeurNom && <span>Par {t.demandeurNom}</span>}
                      {t.dateSoumission && <span>{new Date(t.dateSoumission).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {statutBadge(t.statut)}
                  {t.priorite === 'HAUTE' && <span className="text-[8px] bg-red-100 dark:bg-red-900/30 text-red-600 px-1.5 py-0.5 rounded-full font-semibold uppercase">Priorité</span>}
                  <ChevronRight className="w-4 h-4 text-gray-400" />
                </div>
              </div>
            </div>
          ))}
          {(data?.content || []).length === 0 && <div className="glass-card p-14 text-center"><ArrowLeftRight className="w-10 h-10 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">Aucun transfert</p></div>}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Decision modal */}
      {showDecision && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDecision(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Décision — {showDecision.personneNom}</h3>
              <button onClick={() => setShowDecision(null)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="label">Décision *</label>
                <select className="input" value={decisionForm.decision} onChange={e => setDecisionForm({ ...decisionForm, decision: e.target.value })}>
                  <option value="APPROBATION">✅ Approbation</option>
                  <option value="REFUS">❌ Refus</option>
                  <option value="DEMANDE_INFORMATIONS">❓ Demande d'informations</option>
                  <option value="RENOI_CORRECTION">🔄 Renvoi pour correction</option>
                </select>
              </div>
              <div>
                <label className="label">Commentaire</label>
                <textarea className="input" rows={3} value={decisionForm.commentaire} onChange={e => setDecisionForm({ ...decisionForm, commentaire: e.target.value })} placeholder="Justification..." />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <button onClick={() => setShowDecision(null)} className="btn-secondary">Annuler</button>
              <button onClick={() => { if (!decisionForm.commentaire.trim()) { toast.error('Ajoutez un commentaire'); return; } decideMutation.mutate({ id: showDecision.id, ...decisionForm }); }} disabled={decideMutation.isPending} className="btn-primary">
                {decideMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />} Envoyer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

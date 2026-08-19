import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Soul, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Activity, Plus, Loader2, X, UserPlus, Calendar, Search,
  Filter, Eye, CheckCircle2, Clock, RotateCcw, ChevronDown, ChevronUp,
  FileText, MessageSquare, BarChart3, RefreshCw,
} from 'lucide-react';
import toast from 'react-hot-toast';
import DataTable from '@/components/shared/DataTable';

/** Repli (dictionnaire indisponible) */
const RAISON_FALLBACK: Record<string, string> = {
  TRANSFERT_EN_COURS: 'Transfert en cours',
  RENFORT: 'Renfort',
  VISITE: 'Visite',
  REPRISE_CONTACT: 'Reprise de contact',
  AUTRE: 'Autre',
};

interface ParallelFollowup {
  id: string;
  ameId: string;
  ameNom?: string;
  initiateurId: string;
  initiateurNom?: string;
  raison: string;
  raisonDetail?: string;
  statut: 'EN_COURS' | 'CLOTURE';
  dateDebut: string;
  dateCloture?: string;
  motifCloture?: string;
  createdAt: string;
}

const RAISON_ICONS: Record<string, typeof Activity> = {
  TRANSFERT_EN_COURS: RotateCcw,
  RENFORT: UserPlus,
  VISITE: Calendar,
  REPRISE_CONTACT: MessageSquare,
  AUTRE: FileText,
};

const RAISON_COLORS: Record<string, string> = {
  TRANSFERT_EN_COURS: 'bg-blue-100/80 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border-blue-200/60 dark:border-blue-700/40',
  RENFORT: 'bg-emerald-100/80 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 border-emerald-200/60 dark:border-emerald-700/40',
  VISITE: 'bg-teal-100/80 dark:bg-teal-900/30 text-teal-700 dark:text-teal-300 border-teal-200/60 dark:border-teal-700/40',
  REPRISE_CONTACT: 'bg-purple-100/80 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 border-purple-200/60 dark:border-purple-700/40',
  AUTRE: 'bg-gray-100/80 dark:bg-gray-800/40 text-gray-600 dark:text-gray-300 border-gray-200/60 dark:border-gray-700/40',
};

export default function ParallelFollowupsPage() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [raisonFilter, setRaisonFilter] = useState('');
  const [statutFilter, setStatutFilter] = useState<'' | 'EN_COURS' | 'CLOTURE'>('');
  const [showModal, setShowModal] = useState(false);
  const [detailItem, setDetailItem] = useState<ParallelFollowup | null>(null);
  const [formData, setFormData] = useState({
    ameId: '',
    initiateurId: '',
    raison: 'AUTRE' as const,
    raisonDetail: '',
  });
  const [clotureForm, setClotureForm] = useState({ motifCloture: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['parallel-followups', page, raisonFilter, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (raisonFilter) params.set('raison', raisonFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/parallel-followups?${params}`);
      return res.data as PageResponse<ParallelFollowup>;
    },
  });

  const { data: souls } = useQuery({
    queryKey: ['souls', 'all'],
    queryFn: async () => {
      const res = await api.get('/souls?size=200');
      return res.data.content as Soul[];
    },
  });

  const raisonEntries = useMemo(() => {
    const configured = dictionaries.options('FOLLOWUP_RAISON');
    return configured.length > 0
      ? configured.map((e) => ({ code: e.code, label: e.label }))
      : Object.entries(RAISON_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const raisonLabel = (code: string) =>
    dictionaries.label('FOLLOWUP_RAISON', code) || RAISON_FALLBACK[code] || code;

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['parallel-followups'] });
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/parallel-followups', formData);
    },
    onSuccess: () => {
      invalidate();
      toast.success('Suivi parallèle créé avec succès');
      setShowModal(false);
      setFormData({ ameId: '', initiateurId: '', raison: 'AUTRE', raisonDetail: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const clotureMutation = useMutation({
    mutationFn: async ({ id, motifCloture }: { id: string; motifCloture?: string }) => {
      await api.patch(`/parallel-followups/${id}/cloture`, { motifCloture });
    },
    onSuccess: () => {
      invalidate();
      toast.success('Suivi clôturé');
      setDetailItem(null);
      setClotureForm({ motifCloture: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Client-side search filter
  const filteredContent = useMemo(() => {
    if (!data?.content) return [];
    if (!searchTerm.trim()) return data.content;
    const q = searchTerm.toLowerCase();
    return data.content.filter((f) =>
      f.ameNom?.toLowerCase().includes(q) ||
      f.initiateurNom?.toLowerCase().includes(q) ||
      f.raison?.toLowerCase().includes(q) ||
      f.raisonDetail?.toLowerCase().includes(q)
    );
  }, [data, searchTerm]);

  // Stats
  const stats = useMemo(() => {
    if (!data?.content) return { total: 0, enCours: 0, cloture: 0, byRaison: {} as Record<string, number> };
    const items = data.content;
    const byRaison: Record<string, number> = {};
    items.forEach((f) => { byRaison[f.raison] = (byRaison[f.raison] || 0) + 1; });
    return {
      total: data.totalElements || items.length,
      enCours: items.filter((f) => f.statut === 'EN_COURS').length,
      cloture: items.filter((f) => f.statut === 'CLOTURE').length,
      byRaison,
    };
  }, [data]);

  const toggleStatutFilter = (s: 'EN_COURS' | 'CLOTURE') => {
    setStatutFilter(statutFilter === s ? '' : s);
    setPage(0);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Activity className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Suivis parallèles</h1>
          </div>
          <p className="page-subtitle">
            Accompagnements hors périmètre formel — {stats.total} suivi(s) au total
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => setShowModal(true)} className="btn-primary btn-sm animate-scale-in">
            <Plus className="w-4 h-4" /> Nouveau suivi
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total', value: stats.total, icon: Activity, color: 'from-primary-500 to-primary-600', active: !statutFilter && !raisonFilter },
          { label: 'En cours', value: stats.enCours, icon: Clock, color: 'from-amber-500 to-orange-500', active: statutFilter === 'EN_COURS' },
          { label: 'Clôturés', value: stats.cloture, icon: CheckCircle2, color: 'from-emerald-500 to-green-500', active: statutFilter === 'CLOTURE' },
          { label: 'Types actifs', value: Object.keys(stats.byRaison).length, icon: BarChart3, color: 'from-blue-500 to-indigo-500', active: false },
        ].map((s, i) => (
          <div key={s.label} className={`stat-card animate-slide-up ${s.active ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 60}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{s.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                <s.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '180ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher par âme, initiateur, raison..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9"
            />
          </div>
          <div className="flex items-center gap-1.5">
            <Filter className="w-4 h-4 text-gray-400" />
            <select
              value={raisonFilter}
              onChange={(e) => { setRaisonFilter(e.target.value); setPage(0); }}
              className="input !w-auto"
            >
              <option value="">Toutes les raisons</option>
              {raisonEntries.map((o) => (
                <option key={o.code} value={o.code}>{o.label}</option>
              ))}
            </select>
          </div>
          <div className="flex gap-1.5">
            <button
              onClick={() => toggleStatutFilter('EN_COURS')}
              className={`btn-sm ${statutFilter === 'EN_COURS' ? 'btn-primary' : 'btn-secondary'}`}
            >
              <Clock className="w-3.5 h-3.5" /> En cours ({stats.enCours})
            </button>
            <button
              onClick={() => toggleStatutFilter('CLOTURE')}
              className={`btn-sm ${statutFilter === 'CLOTURE' ? 'btn-primary' : 'btn-secondary'}`}
            >
              <CheckCircle2 className="w-3.5 h-3.5" /> Clôturés ({stats.cloture})
            </button>
          </div>
        </div>
      </div>

      {/* Raison distribution */}
      {Object.keys(stats.byRaison).length > 0 && (
        <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '220ms' }}>
          <div className="flex items-center gap-2 mb-3">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Répartition par raison</h3>
          </div>
          <div className="flex flex-wrap gap-2">
            {Object.entries(stats.byRaison)
              .sort(([, a], [, b]) => b - a)
              .map(([raison, count]) => {
                const RIcon = RAISON_ICONS[raison] || FileText;
                return (
                  <button
                    key={raison}
                    onClick={() => { setRaisonFilter(raisonFilter === raison ? '' : raison); setPage(0); }}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-medium border transition-all ${
                      raisonFilter === raison
                        ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-1 ring-primary-500/30'
                        : RAISON_COLORS[raison] || RAISON_COLORS.AUTRE
                    }`}
                  >
                    <RIcon className="w-3.5 h-3.5" />
                    {raisonLabel(raison)} ({count})
                  </button>
                );
              })}
          </div>
        </div>
      )}

      {/* Table */}
      <DataTable<ParallelFollowup>
        columns={[
          {
            header: 'Âme',
            cell: (f) => (
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <span className="text-xs font-bold text-primary-600 dark:text-primary-400">
                    {f.ameNom?.split(' ').map(p => p?.[0]).join('').slice(0, 2).toUpperCase() || '?'}
                  </span>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{f.ameNom || f.ameId.slice(0, 8)}</p>
                  <p className="text-[10px] text-gray-400">Initié par {f.initiateurNom || '—'}</p>
                </div>
              </div>
            ),
          },
          {
            header: 'Raison',
            cell: (f) => {
              const RIcon = RAISON_ICONS[f.raison] || FileText;
              return (
                <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium border ${RAISON_COLORS[f.raison] || RAISON_COLORS.AUTRE}`}>
                  <RIcon className="w-3 h-3" />
                  {raisonLabel(f.raison)}
                </span>
              );
            },
          },
          {
            header: 'Statut',
            cell: (f) => (
              <span className={`inline-flex items-center gap-1.5 ${f.statut === 'EN_COURS' ? 'badge-warning' : 'badge-success'}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${f.statut === 'EN_COURS' ? 'bg-amber-500 animate-pulse-soft' : 'bg-green-500'}`} />
                {f.statut === 'EN_COURS' ? 'En cours' : 'Clôturé'}
              </span>
            ),
          },
          {
            header: 'Début',
            cell: (f) => (
              <span className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-1">
                <Calendar className="w-3 h-3" />
                {new Date(f.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
              </span>
            ),
          },
          {
            header: '',
            cell: (f) => (
              <button
                onClick={(e) => { e.stopPropagation(); setDetailItem(f); }}
                className="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors"
                title="Voir le détail"
              >
                <Eye className="w-4 h-4" />
              </button>
            ),
          },
        ]}
        data={filteredContent}
        isLoading={isLoading}
        emptyMessage="Aucun suivi parallèle"
        emptyIcon={<Activity className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages} · {data.totalElements} suivi(s)</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Create modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white">
                  <UserPlus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Nouveau suivi parallèle</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Accompagnement hors périmètre formel</p>
                </div>
              </div>
              <button onClick={() => setShowModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="modal-body space-y-4">
              <div>
                <label className="label">Âme concernée *</label>
                <select className="input" value={formData.ameId} onChange={(e) => setFormData({ ...formData, ameId: e.target.value })}>
                  <option value="">Sélectionner une âme...</option>
                  {souls?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.prenom ? `${s.prenom} ${s.nom}` : s.nom} ({s.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Converti' : 'Arrivant'})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Raison</label>
                <select className="input" value={formData.raison} onChange={(e) => setFormData({ ...formData, raison: e.target.value as any })}>
                  {raisonEntries.map((o) => (
                    <option key={o.code} value={o.code}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Détail (optionnel)</label>
                <textarea className="input" rows={3} value={formData.raisonDetail} onChange={(e) => setFormData({ ...formData, raisonDetail: e.target.value })} placeholder="Informations complémentaires..." />
              </div>
            </div>

            <div className="modal-footer">
              <button onClick={() => setShowModal(false)} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={!formData.ameId || createMutation.isPending} className="btn-primary btn-sm">
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                Créer le suivi
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Detail modal */}
      {detailItem && (
        <div className="modal-overlay" onClick={() => setDetailItem(null)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center text-white shadow-sm">
                  <Activity className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Détail du suivi</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{detailItem.ameNom || detailItem.ameId}</p>
                </div>
              </div>
              <button onClick={() => setDetailItem(null)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Raison</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-0.5">{raisonLabel(detailItem.raison)}</p>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Statut</p>
                  <div className="mt-0.5">
                    <span className={`badge text-xs ${detailItem.statut === 'EN_COURS' ? 'badge-warning' : 'badge-success'}`}>
                      {detailItem.statut === 'EN_COURS' ? 'En cours' : 'Clôturé'}
                    </span>
                  </div>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Initiateur</p>
                  <p className="text-sm text-gray-900 dark:text-gray-100 mt-0.5">{detailItem.initiateurNom || '—'}</p>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Date de début</p>
                  <p className="text-sm text-gray-900 dark:text-gray-100 mt-0.5">
                    {new Date(detailItem.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                  </p>
                </div>
              </div>
              {detailItem.raisonDetail && (
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Détail</p>
                  <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{detailItem.raisonDetail}</p>
                </div>
              )}
              {detailItem.statut === 'CLOTURE' && (
                <>
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                    <p className="text-[10px] text-gray-400 uppercase font-semibold">Date de clôture</p>
                    <p className="text-sm text-gray-900 dark:text-gray-100 mt-0.5">
                      {detailItem.dateCloture ? new Date(detailItem.dateCloture).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) : '—'}
                    </p>
                  </div>
                  {detailItem.motifCloture && (
                    <div className="p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-900/10 border border-emerald-200/40 dark:border-emerald-800/20">
                      <p className="text-[10px] text-emerald-600 dark:text-emerald-400 uppercase font-semibold mb-1">Motif de clôture</p>
                      <p className="text-sm text-emerald-700 dark:text-emerald-300">{detailItem.motifCloture}</p>
                    </div>
                  )}
                </>
              )}

              {detailItem.statut === 'EN_COURS' && (
                <div className="p-4 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/40 dark:border-amber-800/20">
                  <p className="text-xs font-semibold text-amber-700 dark:text-amber-300 mb-2">Clôturer ce suivi</p>
                  <textarea
                    className="input text-sm"
                    rows={2}
                    placeholder="Motif de clôture (optionnel)"
                    value={clotureForm.motifCloture}
                    onChange={(e) => setClotureForm({ motifCloture: e.target.value })}
                  />
                  <button
                    onClick={() => clotureMutation.mutate({ id: detailItem.id, motifCloture: clotureForm.motifCloture || undefined })}
                    disabled={clotureMutation.isPending}
                    className="btn-primary btn-sm mt-2"
                  >
                    {clotureMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                    Clôturer le suivi
                  </button>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDetailItem(null)}>Fermer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

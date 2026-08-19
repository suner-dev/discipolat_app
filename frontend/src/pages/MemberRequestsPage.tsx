import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import type {
  MemberRequest,
  MemberRequestStatus,
  MemberPresence,
  UpdateMemberRequestStatus,
} from '@/types';
import {
  Inbox, MessageSquare, CalendarCheck, CheckCircle2,
  XCircle, PlayCircle, Clock, ChevronRight, User, Building2, Users,
  Search, Filter, Eye, X, Loader2, AlertTriangle, TrendingUp,
  Lightbulb, ShieldAlert, Send,
} from 'lucide-react';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import { useDictionaries } from '@/hooks/useDictionaries';

/** Repli */
const TYPE_FALLBACK: Record<string, { label: string; icon: typeof Lightbulb; color: string }> = {
  SUGGESTION: { label: 'Suggestion', icon: Lightbulb, color: 'from-blue-500 to-indigo-500' },
  RENDEZ_VOUS: { label: 'Rendez-vous', icon: Clock, color: 'from-teal-500 to-emerald-500' },
  SIGNALEMENT: { label: 'Signalement', icon: ShieldAlert, color: 'from-red-500 to-rose-500' },
};

const CIBLE_FALLBACK: Record<string, string> = {
  PASTEUR: 'Pasteur',
  RESPONSABLE: 'Responsable',
  CHEF_DE_FAMILLE: 'Chef de famille',
};

const STATUS_BADGES: Record<MemberRequestStatus, string> = {
  OUVERT: 'badge-warning',
  EN_COURS: 'badge-info',
  RESOLU: 'badge-success',
  REJETE: 'badge-error',
};

const STATUS_FALLBACK: Record<MemberRequestStatus, string> = {
  OUVERT: 'Ouvert',
  EN_COURS: 'En cours',
  RESOLU: 'Résolu',
  REJETE: 'Rejeté',
};

const STATUS_ICONS: Record<MemberRequestStatus, typeof PlayCircle> = {
  OUVERT: PlayCircle,
  EN_COURS: Clock,
  RESOLU: CheckCircle2,
  REJETE: XCircle,
};

const tauxPresence = (p: MemberPresence) => {
  const entries = Object.entries(p.presences || {});
  if (entries.length === 0) return 0;
  return Math.round((entries.filter(([, v]) => v).length * 100) / entries.length);
};

export default function MemberRequestsPage() {
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'demandes' | 'presences'>('demandes');
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [detailItem, setDetailItem] = useState<MemberRequest | null>(null);
  const [replyText, setReplyText] = useState<Record<string, string>>({});

  const { data: requests = [], isLoading } = useQuery({
    queryKey: ['members', 'requests', 'inbox'],
    queryFn: async () => (await api.get('/members/requests/inbox')).data as MemberRequest[],
  });

  const { data: presences = [], isLoading: presencesLoading } = useQuery({
    queryKey: ['members', 'presences', 'recent'],
    queryFn: async () => (await api.get('/members/presences/recent')).data as MemberPresence[],
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: UpdateMemberRequestStatus }) => {
      const res = await api.patch(`/members/requests/${id}/status`, payload);
      return res.data as MemberRequest;
    },
    onSuccess: () => {
      toast.success('Demande mise à jour ✅');
      queryClient.invalidateQueries({ queryKey: ['members', 'requests', 'inbox'] });
      setDetailItem(null);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateStatus = (req: MemberRequest, statut: MemberRequestStatus) => {
    statusMutation.mutate({
      id: req.id,
      payload: { statut, reponse: replyText[req.id]?.trim() || undefined },
    });
  };

  // Filtered requests
  const filteredRequests = useMemo(() => {
    let items = requests;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter((r) =>
        r.message?.toLowerCase().includes(q) ||
        r.auteurNom?.toLowerCase().includes(q) ||
        r.departmentNom?.toLowerCase().includes(q) ||
        r.familyNom?.toLowerCase().includes(q)
      );
    }
    if (typeFilter) items = items.filter((r) => r.type === typeFilter);
    if (statusFilter) items = items.filter((r) => r.statut === statusFilter);
    return items;
  }, [requests, searchTerm, typeFilter, statusFilter]);

  const openRequests = requests.filter((r) => r.statut === 'OUVERT' || r.statut === 'EN_COURS');
  const isPasteurOrAdmin = user?.activeRole === 'PASTEUR' || user?.activeRole === 'ADMIN';

  // Stats
  const stats = useMemo(() => ({
    total: requests.length,
    ouvert: requests.filter((r) => r.statut === 'OUVERT').length,
    enCours: requests.filter((r) => r.statut === 'EN_COURS').length,
    resolu: requests.filter((r) => r.statut === 'RESOLU').length,
    rejete: requests.filter((r) => r.statut === 'REJETE').length,
    byType: {
      SUGGESTION: requests.filter((r) => r.type === 'SUGGESTION').length,
      RENDEZ_VOUS: requests.filter((r) => r.type === 'RENDEZ_VOUS').length,
      SIGNALEMENT: requests.filter((r) => r.type === 'SIGNALEMENT').length,
    },
  }), [requests]);

  // Presences
  const latestByMember = new Map<string, MemberPresence>();
  for (const p of presences) {
    if (!latestByMember.has(p.userId)) latestByMember.set(p.userId, p);
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Inbox className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              {isPasteurOrAdmin ? 'Toute l\'église' : user?.activeRole === 'RESPONSABLE' ? 'Mes départements' : 'Ma famille'}
            </span>
          </div>
          <h1 className="page-title">
            Demandes <span className="text-gradient font-display">des membres</span>
          </h1>
          <p className="page-subtitle">
            Suggestions, rendez-vous et signalements — {requests.length} demande(s) au total
            {openRequests.length > 0 && (
              <span className="ml-2 badge badge-warning text-xs">{openRequests.length} à traiter</span>
            )}
          </p>
        </div>
      </div>

      {/* View tabs */}
      <div className="flex items-center gap-2 mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
        {[
          { key: 'demandes' as const, label: 'Demandes', icon: MessageSquare, count: requests.length },
          { key: 'presences' as const, label: 'Présences', icon: CalendarCheck, count: latestByMember.size },
        ].map((t) => {
          const Icon = t.icon;
          const isActive = tab === t.key;
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all whitespace-nowrap ${
                isActive
                  ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800/50'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              {t.label}
              <span className={`ml-1 px-1.5 py-0.5 rounded-full text-[9px] font-bold ${isActive ? 'bg-primary-500/20' : 'bg-gray-200 dark:bg-gray-700'}`}>
                {t.count}
              </span>
            </button>
          );
        })}
      </div>

      {tab === 'demandes' && (
        <>
          {/* Stats cards */}
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 mb-6">
            {[
              { label: 'Total', value: stats.total, icon: Inbox, color: 'from-primary-500 to-primary-600', filterVal: '', filterType: 'status' },
              { label: 'Ouvertes', value: stats.ouvert, icon: PlayCircle, color: 'from-amber-500 to-orange-500', filterVal: 'OUVERT', filterType: 'status' },
              { label: 'En cours', value: stats.enCours, icon: Clock, color: 'from-blue-500 to-indigo-500', filterVal: 'EN_COURS', filterType: 'status' },
              { label: 'Suggestions', value: stats.byType.SUGGESTION, icon: Lightbulb, color: 'from-teal-500 to-emerald-500', filterVal: 'SUGGESTION', filterType: 'type' },
              { label: 'Signalements', value: stats.byType.SIGNALEMENT, icon: ShieldAlert, color: 'from-red-500 to-rose-500', filterVal: 'SIGNALEMENT', filterType: 'type' },
            ].map((s, i) => (
              <button
                key={s.label}
                type="button"
                onClick={() => {
                  if (s.filterType === 'status') { setStatusFilter(statusFilter === s.filterVal ? '' : s.filterVal); }
                  else { setTypeFilter(typeFilter === s.filterVal ? '' : s.filterVal); }
                }}
                className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${
                  (s.filterType === 'status' && statusFilter === s.filterVal) || (s.filterType === 'type' && typeFilter === s.filterVal)
                    ? 'ring-2 ring-primary-500/50' : ''
                }`}
                style={{ animationDelay: `${i * 50}ms` }}
              >
                <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{s.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white shadow-sm`}>
                    <s.icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <p className="stat-value text-xl">{s.value}</p>
              </button>
            ))}
          </div>

          {/* Search & filters */}
          <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Rechercher par message, auteur, département..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="input pl-9"
                />
              </div>
              <div className="flex items-center gap-1.5">
                <Filter className="w-4 h-4 text-gray-400" />
                <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="input !w-auto">
                  <option value="">Tous les types</option>
                  <option value="SUGGESTION">💡 Suggestion</option>
                  <option value="RENDEZ_VOUS">📅 Rendez-vous</option>
                  <option value="SIGNALEMENT">⚠️ Signalement</option>
                </select>
                <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="input !w-auto">
                  <option value="">Tous les statuts</option>
                  <option value="OUVERT">Ouvert</option>
                  <option value="EN_COURS">En cours</option>
                  <option value="RESOLU">Résolu</option>
                  <option value="REJETE">Rejeté</option>
                </select>
              </div>
            </div>
          </div>

          {/* Request list */}
          {isLoading ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="glass-card p-6 animate-fade-in">
                  <div className="skeleton h-4 w-40 mb-3 rounded" />
                  <div className="skeleton h-4 w-full mb-2 rounded" />
                  <div className="skeleton h-4 w-2/3 rounded" />
                </div>
              ))}
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="glass-card p-10 text-center animate-scale-in">
              <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/20 w-fit mx-auto mb-4">
                <Inbox className="w-6 h-6 text-green-500" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                {requests.length === 0 ? 'Aucune demande' : 'Aucun résultat'}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {requests.length === 0
                  ? 'Les membres n\'ont pas encore envoyé de demande dans votre périmètre.'
                  : 'Essayez de modifier les filtres.'}
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {filteredRequests.map((r, i) => {
                const typeInfo = TYPE_FALLBACK[r.type] || TYPE_FALLBACK.SUGGESTION;
                const TypeIcon = typeInfo.icon;
                const StatusIcon = STATUS_ICONS[r.statut];
                return (
                  <div
                    key={r.id}
                    className="glass-card p-5 animate-slide-up flex flex-col hover:shadow-md transition-all cursor-pointer"
                    style={{ animationDelay: `${i * 40}ms` }}
                    onClick={() => setDetailItem(r)}
                  >
                    <div className="flex items-center justify-between gap-2 flex-wrap mb-3">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-medium border bg-gradient-to-r ${typeInfo.color} text-white`}>
                          <TypeIcon className="w-3 h-3" />
                          {typeInfo.label}
                        </span>
                        <span className={`badge text-[10px] ${STATUS_BADGES[r.statut]}`}>
                          {STATUS_FALLBACK[r.statut]}
                        </span>
                      </div>
                      <span className="text-[10px] text-gray-400">
                        {new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>

                    <p className="text-sm text-gray-700 dark:text-gray-200 leading-relaxed flex-1 line-clamp-3">{r.message}</p>

                    {r.piecesJointes && r.piecesJointes.length > 0 && (
                      <div className="mt-2.5">
                        <AttachmentLinks pieces={r.piecesJointes} />
                      </div>
                    )}

                    <div className="flex items-center gap-4 mt-3 text-xs text-gray-500 dark:text-gray-400 flex-wrap">
                      <span className="inline-flex items-center gap-1.5">
                        <User className="w-3.5 h-3.5" /> {r.auteurNom || 'Membre'}
                      </span>
                      {r.departmentNom && (
                        <span className="inline-flex items-center gap-1.5">
                          <Building2 className="w-3.5 h-3.5" /> {r.departmentNom}
                        </span>
                      )}
                      {r.familyNom && (
                        <span className="inline-flex items-center gap-1.5">
                          <Users className="w-3.5 h-3.5" /> {r.familyNom}
                        </span>
                      )}
                    </div>

                    {r.reponse && (
                      <div className="mt-3 p-2.5 rounded-lg bg-emerald-50/70 dark:bg-emerald-900/10 border border-emerald-200/50 dark:border-emerald-800/30 text-sm text-emerald-800 dark:text-emerald-300">
                        <strong>Réponse :</strong> {r.reponse}
                        {r.traiteParNom && <span className="block text-[11px] opacity-70 mt-0.5">— {r.traiteParNom}</span>}
                      </div>
                    )}

                    {r.statut !== 'RESOLU' && r.statut !== 'REJETE' && (
                      <div className="mt-3 pt-3 border-t border-white/20 dark:border-white/[0.06] flex gap-2" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => updateStatus(r, 'RESOLU')} disabled={statusMutation.isPending} className="btn-primary btn-xs flex-1">
                          {statusMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                          Résoudre
                        </button>
                        <button onClick={() => updateStatus(r, 'EN_COURS')} disabled={statusMutation.isPending} className="btn-secondary btn-xs">
                          <PlayCircle className="w-3.5 h-3.5" /> Prendre en charge
                        </button>
                      </div>
                    )}

                    {(r.statut === 'RESOLU' || r.statut === 'REJETE') && (
                      <div className="mt-3 flex items-center gap-1.5 text-[11px] text-gray-400">
                        <StatusIcon className="w-3.5 h-3.5" />
                        Traité {r.dateTraitement ? `le ${new Date(r.dateTraitement).toLocaleDateString('fr-FR')}` : ''}
                        {r.traiteParNom && <> par {r.traiteParNom}</>}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {tab === 'presences' && (
        <>
          {presencesLoading ? (
            <div className="glass-card p-6 animate-fade-in">
              <div className="skeleton h-4 w-40 mb-3 rounded" />
              <div className="skeleton h-4 w-full rounded" />
            </div>
          ) : latestByMember.size === 0 ? (
            <div className="glass-card p-10 text-center animate-scale-in">
              <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/20 w-fit mx-auto mb-4">
                <CalendarCheck className="w-6 h-6 text-green-500" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune présence saisie</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Les membres de votre périmètre n'ont pas encore renseigné leur présence hebdomadaire.
              </p>
            </div>
          ) : (
            <div className="glass-card p-6 animate-slide-up overflow-x-auto">
              <table className="table w-full min-w-[560px]">
                <thead>
                  <tr>
                    <th>Membre</th>
                    <th>Semaine</th>
                    <th>Présence</th>
                    <th>Taux</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {[...latestByMember.values()].map((p) => {
                    const t = tauxPresence(p);
                    return (
                      <tr key={p.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                        <td className="font-medium text-gray-900 dark:text-gray-100">{p.nomMembre || 'Membre'}</td>
                        <td className="text-sm text-gray-500">
                          {new Date(p.semaine + 'T00:00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })}
                        </td>
                        <td className="text-sm">
                          <div className="flex flex-wrap gap-1 max-w-[220px]">
                            {Object.entries(p.presences || {}).map(([prog, present]) => (
                              <span
                                key={prog}
                                title={prog}
                                className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[10px] border ${
                                  present
                                    ? 'bg-green-50 dark:bg-green-900/20 border-green-200/60 dark:border-green-800/40 text-green-700 dark:text-green-400'
                                    : 'bg-red-50 dark:bg-red-900/20 border-red-200/60 dark:border-red-800/40 text-red-600 dark:text-red-400'
                                }`}
                              >
                                {present ? '✓' : '✗'} {prog}
                              </span>
                            ))}
                          </div>
                        </td>
                        <td>
                          <span className={`badge text-xs ${t >= 70 ? 'badge-success' : t >= 40 ? 'badge-warning' : 'badge-error'}`}>
                            {t}%
                          </span>
                        </td>
                        <td className="text-xs text-gray-400 max-w-[160px] truncate">{p.notes || '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <p className="flex items-center gap-1.5 text-[11px] text-gray-400 mt-3">
                <ChevronRight className="w-3 h-3" /> Semaine la plus récente par membre · les données se mettent à jour dès la saisie du membre
              </p>
            </div>
          )}
        </>
      )}

      {/* Detail Modal */}
      {detailItem && (
        <div className="modal-overlay" onClick={() => setDetailItem(null)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-white shadow-sm bg-gradient-to-br ${(TYPE_FALLBACK[detailItem.type] || TYPE_FALLBACK.SUGGESTION).color}`}>
                  {(() => { const TI = (TYPE_FALLBACK[detailItem.type] || TYPE_FALLBACK.SUGGESTION).icon; return <TI className="w-5 h-5" />; })()}
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{(TYPE_FALLBACK[detailItem.type] || TYPE_FALLBACK.SUGGESTION).label}</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {new Date(detailItem.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              </div>
              <button onClick={() => setDetailItem(null)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Statut</p>
                  <div className="mt-0.5">
                    <span className={`badge text-xs ${STATUS_BADGES[detailItem.statut]}`}>
                      {STATUS_FALLBACK[detailItem.statut]}
                    </span>
                  </div>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Cible</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-0.5">
                    {CIBLE_FALLBACK[detailItem.cible] || detailItem.cible}
                  </p>
                </div>
              </div>
              <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Message</p>
                <p className="text-sm text-gray-700 dark:text-gray-200 leading-relaxed">{detailItem.message}</p>
              </div>
              {detailItem.piecesJointes && detailItem.piecesJointes.length > 0 && (
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Pièces jointes</p>
                  <AttachmentLinks pieces={detailItem.piecesJointes} />
                </div>
              )}
              <div className="grid grid-cols-3 gap-3">
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                  <User className="w-4 h-4 text-gray-400 mx-auto mb-1" />
                  <p className="text-[10px] text-gray-400">Auteur</p>
                  <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{detailItem.auteurNom || '—'}</p>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                  <Building2 className="w-4 h-4 text-gray-400 mx-auto mb-1" />
                  <p className="text-[10px] text-gray-400">Département</p>
                  <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{detailItem.departmentNom || '—'}</p>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                  <Users className="w-4 h-4 text-gray-400 mx-auto mb-1" />
                  <p className="text-[10px] text-gray-400">Famille</p>
                  <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{detailItem.familyNom || '—'}</p>
                </div>
              </div>
              {detailItem.reponse && (
                <div className="p-3 rounded-xl bg-emerald-50/70 dark:bg-emerald-900/10 border border-emerald-200/50">
                  <p className="text-[10px] text-emerald-600 dark:text-emerald-400 uppercase font-semibold mb-1">Réponse</p>
                  <p className="text-sm text-emerald-700 dark:text-emerald-300">{detailItem.reponse}</p>
                  {detailItem.traiteParNom && <p className="text-[10px] text-emerald-500 mt-1">— {detailItem.traiteParNom}</p>}
                </div>
              )}
              {/* Quick actions in detail */}
              {detailItem.statut !== 'RESOLU' && detailItem.statut !== 'REJETE' && (
                <div className="p-4 rounded-xl bg-primary-50/50 dark:bg-primary-900/10 border border-primary-200/40">
                  <textarea
                    className="input !py-2 text-sm mb-2"
                    rows={2}
                    placeholder="Votre réponse (facultatif)"
                    value={replyText[detailItem.id] || ''}
                    onChange={(e) => setReplyText((s) => ({ ...s, [detailItem.id]: e.target.value }))}
                  />
                  <div className="flex flex-wrap gap-2">
                    <button onClick={() => updateStatus(detailItem, 'RESOLU')} disabled={statusMutation.isPending} className="btn-primary btn-xs flex-1">
                      {statusMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                      Résoudre
                    </button>
                    <button onClick={() => updateStatus(detailItem, 'EN_COURS')} disabled={statusMutation.isPending} className="btn-secondary btn-xs">
                      <PlayCircle className="w-3.5 h-3.5" /> En cours
                    </button>
                    <button onClick={() => updateStatus(detailItem, 'REJETE')} disabled={statusMutation.isPending} className="btn-danger btn-xs">
                      <XCircle className="w-3.5 h-3.5" /> Rejeter
                    </button>
                  </div>
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

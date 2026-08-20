import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Shield, Download, Trash2, Search, Loader2, RefreshCw,
  Clock, CheckCircle, XCircle, Filter, X, User, FileText,
  ChevronDown, AlertTriangle, Eye,
} from 'lucide-react';
import type { User as UserType } from '@/types';

interface GdprRequest {
  id: string;
  type: 'EXPORT' | 'DELETE';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';
  userId: string;
  userName: string;
  userEmail: string;
  requestedBy: string;
  requestedByName: string;
  reason?: string;
  processedBy?: string;
  processedByName?: string;
  createdAt: string;
  processedAt?: string;
  downloadUrl?: string;
}

const TYPE_OPTIONS = [
  { value: 'EXPORT' as const, label: 'Export de données', icon: Download, color: 'text-blue-500', bg: 'from-blue-500 to-indigo-600' },
  { value: 'DELETE' as const, label: 'Suppression', icon: Trash2, color: 'text-red-500', bg: 'from-red-500 to-rose-600' },
];

const STATUS_OPTIONS = [
  { value: 'PENDING' as const, label: 'En attente', color: 'badge-warning', dot: 'bg-amber-500' },
  { value: 'PROCESSING' as const, label: 'En cours', color: 'badge-info', dot: 'bg-blue-500' },
  { value: 'COMPLETED' as const, label: 'Terminé', color: 'badge-success', dot: 'bg-green-500' },
  { value: 'REJECTED' as const, label: 'Rejeté', color: 'badge-error', dot: 'bg-red-500' },
];

export default function AdminGdprPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [modalOpen, setModalOpen] = useState(false);
  const [detailRequest, setDetailRequest] = useState<GdprRequest | null>(null);
  const [form, setForm] = useState<{ type: 'EXPORT' | 'DELETE'; userSearch: string; reason: string; selectedUser: UserType | null }>({
    type: 'EXPORT',
    userSearch: '',
    reason: '',
    selectedUser: null,
  });
  const [userSearchResults, setUserSearchResults] = useState<UserType[]>([]);
  const [searchingUsers, setSearchingUsers] = useState(false);

  const { data: requests = [], isLoading, refetch } = useQuery({
    queryKey: ['admin', 'gdpr-requests'],
    queryFn: async () => {
      const res = await api.get('/gdpr/requests');
      return res.data as GdprRequest[];
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'gdpr-requests'] });
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!form.selectedUser) throw new Error('Aucun utilisateur sélectionné');
      const endpoint = form.type === 'EXPORT'
        ? '/gdpr/export'
        : '/gdpr/delete';
      await api.post(endpoint, {
        userId: form.selectedUser.id,
        reason: form.reason || undefined,
      });
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setForm({ type: 'EXPORT', userSearch: '', reason: '', selectedUser: null });
      toast.success('Demande GDPR créée');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const processMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/gdpr/requests/${id}/process`);
    },
    onSuccess: () => {
      invalidate();
      toast.success('Demande traitée');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const rejectMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/gdpr/requests/${id}/reject`);
    },
    onSuccess: () => {
      invalidate();
      toast.success('Demande rejetée');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const searchUsers = async (query: string) => {
    if (query.length < 2) {
      setUserSearchResults([]);
      return;
    }
    setSearchingUsers(true);
    try {
      const res = await api.get(`/users/search?q=${encodeURIComponent(query)}`);
      setUserSearchResults((res.data as UserType[]).slice(0, 8));
    } catch {
      setUserSearchResults([]);
    } finally {
      setSearchingUsers(false);
    }
  };

  const filtered = requests.filter((r) => {
    if (statusFilter && r.status !== statusFilter) return false;
    if (typeFilter && r.type !== typeFilter) return false;
    return true;
  });

  const statsByStatus = STATUS_OPTIONS.map((s) => ({
    ...s,
    count: requests.filter((r) => r.status === s.value).length,
  }));

  const getTypeInfo = (type: string) => TYPE_OPTIONS.find((t) => t.value === type) || TYPE_OPTIONS[0];
  const getStatusInfo = (status: string) => STATUS_OPTIONS.find((s) => s.value === status) || STATUS_OPTIONS[0];

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-6xl">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Shield className="w-5 h-5 text-primary-500" />
            RGPD (GDPR)
          </h1>
          <p className="page-subtitle">
            Gestion des demandes de protection des données — exports et suppressions conformes au RGPD.
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button className="btn-primary btn-sm" onClick={() => setModalOpen(true)}>
            <FileText className="w-4 h-4" /> Nouvelle demande
          </button>
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {statsByStatus.map((s, i) => (
          <button
            key={s.value}
            type="button"
            onClick={() => setStatusFilter(statusFilter === s.value ? '' : s.value)}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statusFilter === s.value ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 60}ms` }}
          >
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{statusFilter === s.value ? `${s.label} (filtré)` : s.label}</span>
              <span className={`w-3 h-3 rounded-full ${s.dot} shadow-[0_0_6px_rgba(0,0,0,0.15)]`} />
            </div>
            <p className="stat-value text-2xl">{s.count}</p>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {s.count === 1 ? 'demande' : 'demandes'}
            </p>
          </button>
        ))}
      </div>

      {/* Filter bar */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '240ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-semibold text-gray-500">Filtrer :</span>
          </div>
          <select
            className="input w-auto text-sm"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <option value="">Tous les types</option>
            {TYPE_OPTIONS.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
          <select
            className="input w-auto text-sm"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">Tous les statuts</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
          {(statusFilter || typeFilter) && (
            <button
              onClick={() => { setStatusFilter(''); setTypeFilter(''); }}
              className="btn-ghost btn-sm"
            >
              <X className="w-3.5 h-3.5" /> Réinitialiser
            </button>
          )}
        </div>
      </div>

      {/* Request list */}
      {filtered.length === 0 ? (
        <div className="glass-card p-10 text-center animate-scale-in">
          <Shield className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">
            {requests.length === 0 ? 'Aucune demande GDPR.' : 'Aucune demande ne correspond aux filtres.'}
          </p>
          {requests.length === 0 && (
            <button className="text-primary-500 hover:underline text-sm mt-2" onClick={() => setModalOpen(true)}>
              Créer la première demande
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((r, i) => {
            const typeInfo = getTypeInfo(r.type);
            const statusInfo = getStatusInfo(r.status);
            const TypeIcon = typeInfo.icon;
            return (
              <div
                key={r.id}
                className="glass-card px-5 py-4 flex items-center gap-4 animate-slide-up hover:shadow-md transition-shadow"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <div className={`w-11 h-11 rounded-xl bg-gradient-to-br ${typeInfo.bg} flex items-center justify-center text-white shadow-sm flex-shrink-0`}>
                  <TypeIcon className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{r.userName}</span>
                    <span className={`badge text-[10px] ${statusInfo.color}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${statusInfo.dot}`} />
                      {statusInfo.label}
                    </span>
                    <span className={`badge text-[10px] ${typeInfo.color === 'text-red-500' ? 'badge-error' : 'badge-info'}`}>
                      {typeInfo.label}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                    <span className="flex items-center gap-1">
                      <User className="w-3 h-3" /> {r.userEmail}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
                    </span>
                    {r.requestedByName && (
                      <span className="text-gray-500">par {r.requestedByName}</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    className="btn-icon text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                    onClick={() => setDetailRequest(r)}
                    title="Voir le détail"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  {r.status === 'PENDING' && (
                    <>
                      <button
                        className="btn-icon text-gray-400 hover:text-green-500 hover:bg-green-50 dark:hover:bg-green-900/20"
                        onClick={() => processMutation.mutate(r.id)}
                        disabled={processMutation.isPending}
                        title="Traiter"
                      >
                        {processMutation.isPending ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <CheckCircle className="w-4 h-4" />
                        )}
                      </button>
                      <button
                        className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
                        onClick={() => rejectMutation.mutate(r.id)}
                        disabled={rejectMutation.isPending}
                        title="Rejeter"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-sm">
                  <Shield className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                    Nouvelle demande GDPR
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Créez une demande d'export ou de suppression de données
                  </p>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              {/* Type selector */}
              <div>
                <label className="label">Type de demande</label>
                <div className="grid grid-cols-2 gap-3">
                  {TYPE_OPTIONS.map((t) => (
                    <button
                      key={t.value}
                      type="button"
                      onClick={() => setForm({ ...form, type: t.value })}
                      className={`p-3 rounded-xl border text-center transition-all ${
                        form.type === t.value
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/20'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                      }`}
                    >
                      <t.icon className={`w-5 h-5 mx-auto mb-1 ${t.color}`} />
                      <p className="text-xs font-semibold text-gray-900 dark:text-gray-100">{t.label}</p>
                    </button>
                  ))}
                </div>
              </div>

              {form.type === 'DELETE' && (
                <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/50 dark:border-amber-800/30">
                  <p className="text-xs text-amber-700 dark:text-amber-400 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4" />
                    La suppression est irréversible. Toutes les données de l'utilisateur seront définitivement effacées.
                  </p>
                </div>
              )}

              {/* User search */}
              <div>
                <label className="label">Utilisateur</label>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="text"
                    className="input pl-9"
                    placeholder="Rechercher par nom ou email..."
                    value={form.selectedUser ? `${form.selectedUser.firstName} ${form.selectedUser.lastName} (${form.selectedUser.email})` : form.userSearch}
                    onChange={(e) => {
                      const val = e.target.value;
                      setForm({ ...form, userSearch: val, selectedUser: null });
                      searchUsers(val);
                    }}
                    disabled={!!form.selectedUser}
                  />
                  {form.selectedUser && (
                    <button
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      onClick={() => setForm({ ...form, selectedUser: null, userSearch: '' })}
                    >
                      <X className="w-4 h-4" />
                    </button>
                  )}
                </div>
                {searchingUsers && (
                  <div className="flex items-center gap-2 mt-2 text-xs text-gray-400">
                    <Loader2 className="w-3 h-3 animate-spin" /> Recherche...
                  </div>
                )}
                {userSearchResults.length > 0 && !form.selectedUser && (
                  <div className="mt-2 border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden max-h-40 overflow-y-auto">
                    {userSearchResults.map((u) => (
                      <button
                        key={u.id}
                        type="button"
                        className="w-full px-3 py-2 flex items-center gap-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors text-left border-b border-gray-100 dark:border-gray-800 last:border-0"
                        onClick={() => {
                          setForm({ ...form, selectedUser: u, userSearch: '' });
                          setUserSearchResults([]);
                        }}
                      >
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                          {u.firstName?.[0]}{u.lastName?.[0]}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{u.firstName} {u.lastName}</p>
                          <p className="text-[10px] text-gray-400 truncate">{u.email}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Reason */}
              <div>
                <label className="label">Motif (optionnel)</label>
                <textarea
                  className="input min-h-[80px]"
                  placeholder="Raison de la demande..."
                  value={form.reason}
                  onChange={(e) => setForm({ ...form, reason: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => createMutation.mutate()}
                disabled={createMutation.isPending || !form.selectedUser}
              >
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileText className="w-4 h-4" />}
                Créer la demande
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailRequest && (
        <div className="modal-overlay" onClick={() => setDetailRequest(null)}>
          <div className="modal-content max-w-xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${getTypeInfo(detailRequest.type).bg} flex items-center justify-center text-white shadow-sm`}>
                  {(() => { const I = getTypeInfo(detailRequest.type).icon; return <I className="w-6 h-6" />; })()}
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{detailRequest.userName}</h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className={`badge text-[10px] ${getStatusInfo(detailRequest.status).color}`}>
                      {getStatusInfo(detailRequest.status).label}
                    </span>
                    <span className="badge text-[10px]">{getTypeInfo(detailRequest.type).label}</span>
                  </div>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setDetailRequest(null)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <User className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">Email</span>
                  </div>
                  <p className="text-sm text-gray-900 dark:text-gray-100">{detailRequest.userEmail}</p>
                </div>
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <Clock className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">Demandé le</span>
                  </div>
                  <p className="text-sm text-gray-900 dark:text-gray-100">
                    {new Date(detailRequest.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              </div>

              {detailRequest.reason && (
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Motif</p>
                  <p className="text-sm text-gray-700 dark:text-gray-300">{detailRequest.reason}</p>
                </div>
              )}

              {detailRequest.processedAt && (
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                    <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Traité par</p>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{detailRequest.processedByName}</p>
                  </div>
                  <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                    <p className="text-[10px] text-gray-400 uppercase font-semibold mb-1">Traité le</p>
                    <p className="text-sm text-gray-900 dark:text-gray-100">
                      {new Date(detailRequest.processedAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                    </p>
                  </div>
                </div>
              )}

              {detailRequest.downloadUrl && (
                <div className="p-4 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30">
                  <a
                    href={detailRequest.downloadUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 text-sm text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    <Download className="w-4 h-4" />
                    Télécharger l'export de données
                  </a>
                </div>
              )}

              {detailRequest.status === 'PENDING' && (
                <div className="flex items-center gap-2 pt-2">
                  <button
                    className="btn-primary btn-sm"
                    onClick={() => { processMutation.mutate(detailRequest.id); setDetailRequest(null); }}
                    disabled={processMutation.isPending}
                  >
                    <CheckCircle className="w-4 h-4" /> Traiter
                  </button>
                  <button
                    className="btn-danger btn-sm"
                    onClick={() => { rejectMutation.mutate(detailRequest.id); setDetailRequest(null); }}
                    disabled={rejectMutation.isPending}
                  >
                    <XCircle className="w-4 h-4" /> Rejeter
                  </button>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDetailRequest(null)}>Fermer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

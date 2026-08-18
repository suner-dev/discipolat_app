import { useEffect, useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Alert, PageResponse, Soul, Department, Family, User } from '@/types';
import {
  Bell,
  BellRing,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Loader2,
  Filter,
  Shield,
  ShieldCheck,
  Sparkles,
  Plus,
  X,
  Send,
  Building2,
  Users,
  User as UserIcon,
  Church,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const TYPE_FALLBACK: Record<string, string> = {
  ABSENCE_48H: '🚨 Absence 48h',
  ABSENCE_3_SEMAINES: '🚨 Décrochage 3 semaines',
  RAPPORT_NON_SOUMIS: '📋 Rapport non soumis',
  RAPPORT_FAMILLE_NON_SOUMIS: '📊 Rapport famille',
  ALERTE_ABSENCE: '🚨 Alerte absence',
  MANUEL: '📢',
};

const CIBLE_FALLBACK = ['PERSONNE', 'DEPARTEMENT', 'FAMILLE', 'GROUPE', 'EGLISE'];
const PRIORITE_FALLBACK = [
  { value: 'BASSE', label: 'Basse' },
  { value: 'MOYENNE', label: 'Moyenne' },
  { value: 'HAUTE', label: 'Haute' },
  { value: 'URGENTE', label: 'Urgente' },
];
const STATUT_FALLBACK = [
  { value: 'ACTIVE', label: 'Actives uniquement' },
  { value: 'TRAITEE', label: 'Traitées uniquement' },
  { value: 'RESOLUE', label: 'Résolues uniquement' },
];

export default function AlertsPage() {
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [alertForm, setAlertForm] = useState({
    typeAlerteManuel: '', titre: '', message: '', cible: 'PERSONNE',
    priorite: 'MOYENNE', ameId: '', familleId: '', departmentId: '', faiseurId: '',
  });
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkMode, setBulkMode] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['alerts', page, filter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (filter) params.set('statut', filter);
      const res = await api.get(`/alerts?${params}`);
      return res.data as PageResponse<Alert>;
    },
  });

  const resolveMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/alerts/${id}/resolve`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      toast.success('Alerte résolue avec succès');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const resolveBatchMutation = useMutation({
    mutationFn: async (ids: string[]) => {
      await api.post('/alerts/resolve-batch', ids);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      setSelectedIds(new Set());
      setBulkMode(false);
      toast.success('Alertes résolues avec succès');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Data for targeting
  const { data: souls } = useQuery({
    queryKey: ['alerts', 'souls'],
    queryFn: async () => {
      const res = await api.get('/souls?size=100');
      return res.data.content as Soul[];
    },
    enabled: showCreate && alertForm.cible === 'PERSONNE',
  });
  const { data: families } = useQuery({
    queryKey: ['alerts', 'families'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
    enabled: showCreate && alertForm.cible === 'FAMILLE',
  });
  const { data: departments } = useQuery({
    queryKey: ['alerts', 'departments'],
    queryFn: async () => {
      const res = await api.get('/departments?size=100');
      return res.data.content as Department[];
    },
    enabled: showCreate && alertForm.cible === 'DEPARTEMENT',
  });
  const { data: users } = useQuery({
    queryKey: ['alerts', 'users'],
    queryFn: async () => {
      const res = await api.get('/users?size=100');
      return res.data.content as User[];
    },
    enabled: showCreate && alertForm.cible === 'GROUPE',
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof alertForm) => {
      await api.post('/alerts', {
        typeAlerteManuel: data.typeAlerteManuel,
        titre: data.titre,
        message: data.message,
        cible: data.cible,
        priorite: data.priorite,
        ameId: data.ameId || undefined,
        familleId: data.familleId || undefined,
        departmentId: data.departmentId || undefined,
        faiseurId: data.faiseurId || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      toast.success('Alerte créée avec succès');
      setShowCreate(false);
      setAlertForm({ typeAlerteManuel: '', titre: '', message: '', cible: 'PERSONNE', priorite: 'MOYENNE', ameId: '', familleId: '', departmentId: '', faiseurId: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const getCibleIcon = (cible: string) => {
    switch (cible) {
      case 'PERSONNE': return <UserIcon className="w-3.5 h-3.5" />;
      case 'DEPARTEMENT': return <Building2 className="w-3.5 h-3.5" />;
      case 'FAMILLE': return <Users className="w-3.5 h-3.5" />;
      case 'EGLISE': return <Church className="w-3.5 h-3.5" />;
      default: return <Bell className="w-3.5 h-3.5" />;
    }
  };

  /** Libellé du type d'alerte (dictionnaire ALERT_TYPE, repli local sinon). */
  const typeLabel = (alert: Alert) => {
    if (alert.typeAlerte === 'MANUEL') {
      return `📢 ${alert.titre || dictionaries.label('ALERT_TYPE', 'MANUEL') || TYPE_FALLBACK.MANUEL || 'Alerte manuelle'}`;
    }
    return dictionaries.label('ALERT_TYPE', alert.typeAlerte)
      || TYPE_FALLBACK[alert.typeAlerte]
      || alert.typeAlerte;
  };

  /** Badge du type d'alerte : couleur du dictionnaire si disponible. */
  const typeBadgeClass = (type: string) => {
    if (dictionaries.color('ALERT_TYPE', type)) return undefined;
    return type === 'ABSENCE_48H' || type === 'ABSENCE_3_SEMAINES' || type === 'ALERTE_ABSENCE'
      ? 'badge-danger'
      : type === 'MANUEL' ? 'badge-primary' : 'badge-warning';
  };

  /** Cibles configurables (dictionnaire ALERT_CIBLE) — repli sinon. */
  const cibleOptions = useMemo(() => {
    const configured = dictionaries.options('ALERT_CIBLE');
    return configured.length > 0 ? configured.map((e) => e.code) : CIBLE_FALLBACK;
  }, [dictionaries]);

  /** Priorités configurables (dictionnaire ALERT_PRIORITE) — repli sinon. */
  const prioriteOptions = useMemo(() => {
    const configured = dictionaries.selectOptions('ALERT_PRIORITE');
    return configured.length > 0 ? configured : PRIORITE_FALLBACK;
  }, [dictionaries]);

  /** Filtres de statut configurables (dictionnaire ALERT_STATUS) — repli sinon. */
  const statutFilters = useMemo(() => {
    const configured = dictionaries.options('ALERT_STATUS');
    if (configured.length > 0) {
      return configured.map((e) => ({ value: e.code, label: `${e.label} uniquement` }));
    }
    return STATUT_FALLBACK;
  }, [dictionaries]);

  // Si l'admin a désactivé la cible/priorité par défaut, bascule sur la première option disponible.
  useEffect(() => {
    if (!showCreate) return;
    if (cibleOptions.length > 0 && !cibleOptions.includes(alertForm.cible)) {
      setAlertForm((f) => ({ ...f, cible: cibleOptions[0] }));
    }
    if (prioriteOptions.length > 0 && !prioriteOptions.some((o) => o.value === alertForm.priorite)) {
      setAlertForm((f) => ({ ...f, priorite: prioriteOptions[0].value }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showCreate, cibleOptions, prioriteOptions]);

  const activeAlerts = data?.content.filter(a => a.statut === 'ACTIVE') || [];
  const resolvedAlerts = data?.content.filter(a => a.statut === 'RESOLUE') || [];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BellRing className="w-5 h-5 text-amber-500" />
            <h1 className="page-title">Alertes</h1>
          </div>
          <p className="page-subtitle">
            {activeAlerts.length > 0
              ? `${activeAlerts.length} alerte${activeAlerts.length > 1 ? 's' : ''} active${activeAlerts.length > 1 ? 's' : ''} nécessitant votre attention`
              : 'Toutes les alertes sont traitées'}
          </p>
        </div>
        <div className="flex items-center gap-2 animate-fade-in">
          <Filter className="w-4 h-4 text-gray-400" />
          <select
            value={filter}
            onChange={(e) => { setFilter(e.target.value); setPage(0); }}
            className="input w-auto"
          >
            <option value="">Toutes les alertes</option>
            {statutFilters.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
          <button
            onClick={() => { setBulkMode(!bulkMode); setSelectedIds(new Set()); }}
            className={`btn-sm ${bulkMode ? 'btn-primary' : 'btn-secondary'}`}
          >
            {bulkMode ? 'Annuler sélection' : 'Sélectionner'}
          </button>
          {bulkMode && selectedIds.size > 0 && (
            <button
              onClick={() => resolveBatchMutation.mutate(Array.from(selectedIds))}
              disabled={resolveBatchMutation.isPending}
              className="btn-glow btn-sm"
            >
              {resolveBatchMutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <CheckCircle2 className="w-4 h-4" />
              )}
              Résoudre ({selectedIds.size})
            </button>
          )}
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" />
            Nouvelle alerte
          </button>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8 animate-slide-up">
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-red-100 dark:bg-red-900/30">
              <BellRing className="w-5 h-5 text-red-600 dark:text-red-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{activeAlerts.length}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Alertes actives</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-green-100 dark:bg-green-900/30">
              <ShieldCheck className="w-5 h-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{resolvedAlerts.length}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Alertes résolues</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-100 dark:bg-blue-900/30">
              <Shield className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{data?.totalElements || 0}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Total</p>
            </div>
          </div>
        </div>
      </div>

      {/* Alerts list */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="glass-card p-4 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="flex items-center gap-4">
                <div className="skeleton w-10 h-10 rounded-xl" />
                <div className="flex-1">
                  <div className="skeleton h-4 w-48 mb-2" />
                  <div className="skeleton h-3 w-32" />
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : data?.content.length === 0 ? (
        <div className="glass-card p-14 text-center animate-scale-in">
          <div className="inline-flex p-4 rounded-2xl bg-green-100 dark:bg-green-900/20 mb-4">
            <Bell className="w-10 h-10 text-green-500" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune alerte</h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">Toutes les alertes ont été traitées</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data?.content.map((alert, i) => (
            <div
              key={alert.id}
              className={`glass-card p-4 animate-slide-up hover-lift ${
                alert.statut === 'ACTIVE'
                  ? 'border-l-4 border-l-red-500'
                  : 'border-l-4 border-l-green-500 opacity-70'
              }`}
              style={{ animationDelay: `${i * 40}ms` }}
            >
              <div className="flex items-start gap-4">
                {/* Checkbox (bulk mode) */}
                {bulkMode && (
                  <input
                    type="checkbox"
                    checked={selectedIds.has(alert.id)}
                    onChange={(e) => {
                      const next = new Set(selectedIds);
                      if (e.target.checked) next.add(alert.id); else next.delete(alert.id);
                      setSelectedIds(next);
                    }}
                    className="mt-3 w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                  />
                )}

                {/* Icon */}
                <div className={`p-2.5 rounded-xl ${
                  alert.statut === 'ACTIVE'
                    ? 'bg-red-100 dark:bg-red-900/30'
                    : 'bg-green-100 dark:bg-green-900/30'
                }`}>
                  {alert.statut === 'ACTIVE' ? (
                    <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400" />
                  ) : (
                    <CheckCircle2 className="w-5 h-5 text-green-600 dark:text-green-400" />
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`badge text-[10px] ${typeBadgeClass(alert.typeAlerte) || 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'}`}
                      style={dictionaries.color('ALERT_TYPE', alert.typeAlerte)
                        ? { backgroundColor: `${dictionaries.color('ALERT_TYPE', alert.typeAlerte)}22`, color: dictionaries.color('ALERT_TYPE', alert.typeAlerte) }
                        : undefined}
                    >
                      {typeLabel(alert)}
                    </span>
                    <span className="inline-flex items-center gap-1 badge text-[10px] badge-gray">
                      {getCibleIcon(alert.cible)} {dictionaries.label('ALERT_CIBLE', alert.cible) || alert.cible}
                    </span>
                    <span
                      className="badge text-[10px] bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300"
                      style={dictionaries.color('ALERT_PRIORITE', alert.priorite)
                        ? { backgroundColor: `${dictionaries.color('ALERT_PRIORITE', alert.priorite)}22`, color: dictionaries.color('ALERT_PRIORITE', alert.priorite) }
                        : undefined}
                    >
                      {dictionaries.label('ALERT_PRIORITE', alert.priorite) || alert.priorite || 'MOYENNE'}
                    </span>
                    <span
                      className="badge text-[10px] bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300"
                      style={dictionaries.color('ALERT_STATUS', alert.statut)
                        ? { backgroundColor: `${dictionaries.color('ALERT_STATUS', alert.statut)}22`, color: dictionaries.color('ALERT_STATUS', alert.statut) }
                        : undefined}
                    >
                      {dictionaries.label('ALERT_STATUS', alert.statut)
                        || (alert.statut === 'ACTIVE' ? 'Active' : alert.statut === 'TRAITEE' ? 'Traitée' : 'Résolue')}
                    </span>
                  </div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {alert.message}
                  </p>
                  <div className="flex items-center gap-2 mt-1.5">
                    <Clock className="w-3 h-3 text-gray-400" />
                    <span className="text-xs text-gray-400">
                      {new Date(alert.dateDeclenchement).toLocaleDateString('fr-FR', {
                        day: 'numeric', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                      })}
                    </span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex-shrink-0">
                  {alert.statut === 'ACTIVE' && (
                    <button
                      onClick={() => resolveMutation.mutate(alert.id)}
                      disabled={resolveMutation.isPending}
                      className="btn-glow btn-sm"
                    >
                      {resolveMutation.isPending ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <CheckCircle2 className="w-4 h-4" />
                      )}
                      Résoudre
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 animate-fade-in">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Page {data.number + 1} / {data.totalPages}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={data.first}
              className="btn-secondary btn-sm"
            >
              ← Précédent
            </button>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={data.last}
              className="btn-primary btn-sm"
            >
              Suivant →
            </button>
          </div>
        </div>
      )}

      {/* Create alert modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowCreate(false)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg animate-slide-up max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-5 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500 to-orange-500 flex items-center justify-center text-white">
                  <BellRing className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Nouvelle alerte</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Alerte, rappel, urgence ou convocation</p>
                </div>
              </div>
              <button onClick={() => setShowCreate(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700">
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate(alertForm);
              }}
              className="p-5 space-y-4"
            >
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="label">Type *</label>
                  <input
                    className="input"
                    value={alertForm.typeAlerteManuel}
                    onChange={(e) => setAlertForm({ ...alertForm, typeAlerteManuel: e.target.value })}
                    placeholder="Ex: Convocation, Rappel..."
                    required
                  />
                </div>
                <div>
                  <label className="label">Priorité</label>
                  <select
                    className="input"
                    value={alertForm.priorite}
                    onChange={(e) => setAlertForm({ ...alertForm, priorite: e.target.value })}
                  >
                    {prioriteOptions.map((o) => (
                      <option key={o.value} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="label">Titre *</label>
                <input
                  className="input"
                  value={alertForm.titre}
                  onChange={(e) => setAlertForm({ ...alertForm, titre: e.target.value })}
                  placeholder="Titre de l'alerte"
                  required
                />
              </div>
              <div>
                <label className="label">Message *</label>
                <textarea
                  className="input"
                  rows={3}
                  value={alertForm.message}
                  onChange={(e) => setAlertForm({ ...alertForm, message: e.target.value })}
                  placeholder="Détail de l'alerte..."
                  required
                />
              </div>
              <div>
                <label className="label">Cible</label>
                <div className="grid grid-cols-5 gap-1.5">
                  {cibleOptions.map((c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setAlertForm({ ...alertForm, cible: c, ameId: '', familleId: '', departmentId: '', faiseurId: '' })}
                      className={`p-2 rounded-lg text-[10px] font-medium transition-all border-2 flex flex-col items-center gap-1
                        ${alertForm.cible === c
                          ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                          : 'border-gray-200 dark:border-gray-700 text-gray-500'}`}
                    >
                      {getCibleIcon(c)}
                      {dictionaries.label('ALERT_CIBLE', c) || c}
                    </button>
                  ))}
                </div>
              </div>
              {alertForm.cible === 'PERSONNE' && (
                <select className="input" value={alertForm.ameId}
                  onChange={(e) => setAlertForm({ ...alertForm, ameId: e.target.value })}>
                  <option value="">Sélectionner une personne...</option>
                  {souls?.map((s) => (
                    <option key={s.id} value={s.id}>{s.prenom} {s.nom}</option>
                  ))}
                </select>
              )}
              {alertForm.cible === 'DEPARTEMENT' && (
                <select className="input" value={alertForm.departmentId}
                  onChange={(e) => setAlertForm({ ...alertForm, departmentId: e.target.value })}>
                  <option value="">Sélectionner un département...</option>
                  {departments?.map((d) => (
                    <option key={d.id} value={d.id}>{d.nom}</option>
                  ))}
                </select>
              )}
              {alertForm.cible === 'FAMILLE' && (
                <select className="input" value={alertForm.familleId}
                  onChange={(e) => setAlertForm({ ...alertForm, familleId: e.target.value })}>
                  <option value="">Sélectionner une famille...</option>
                  {families?.map((f) => (
                    <option key={f.id} value={f.id}>{f.nom}</option>
                  ))}
                </select>
              )}
              {alertForm.cible === 'GROUPE' && (
                <select className="input" value={alertForm.faiseurId}
                  onChange={(e) => setAlertForm({ ...alertForm, faiseurId: e.target.value })}>
                  <option value="">Sélectionner un faiseur / groupe...</option>
                  {users?.map((u) => (
                    <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</option>
                  ))}
                </select>
              )}
              {alertForm.cible === 'EGLISE' && (
                <p className="text-sm text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/50 p-3 rounded-xl">
                  <Church className="w-4 h-4 inline mr-1 -mt-0.5 text-primary-500" />
                  Cette alerte sera visible par toute l'église.
                </p>
              )}
              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button type="button" onClick={() => setShowCreate(false)} className="btn-secondary">Annuler</button>
                <button type="submit" disabled={createMutation.isPending} className="btn-primary">
                  {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                  Créer l'alerte
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

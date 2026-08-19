import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Notification, PageResponse } from '@/types';
import {
  Bell, BellRing, CheckCheck, Mail, Smartphone, Globe, Loader2, Clock,
  Inbox, Sparkles, ChevronRight, Filter, X, Search, BarChart3,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis */
const TYPE_FALLBACK: Record<string, string> = {
  RAPPORT_NON_SOUMIS: 'Rapport non soumis', ABSENCE_48H: 'Absence 48h',
  RAPPORT_FAMILLE_NON_SOUMIS: 'Rapport famille non soumis', ALERTE_ABSENCE: 'Alerte absence',
  INFORMATION: 'Information', PRIERE_EXAUCEE: 'Prières exaucées',
  TRANSFERT_DEMANDE: 'Demande de transfert', TRANSFERT_VALIDATION: 'Validation de transfert',
  TRANSFERT_VALIDEE: 'Transfert validé', TRANSFERT_REFUSEE: 'Transfert refusé',
  TRANSFERT_INFOS_DEMANDEES: 'Informations demandées', TRANSFERT_CORRECTION: 'Correction demandée',
  TRANSFERT_EXECUTEE: 'Transfert exécuté', TRANSFERT_ANNULEE: 'Transfert annulé',
  TRANSFERT_DELAI_DEPASSE: 'Délai dépassé',
};

const CANAL_FALLBACK: Record<string, string> = { IN_APP: 'Dans l\'application', EMAIL: 'E-mail', PUSH: 'Push' };

function canalIcon(canal: string) {
  switch (canal) {
    case 'EMAIL': return <Mail className="w-3 h-3" />;
    case 'PUSH': return <Smartphone className="w-3 h-3" />;
    default: return <Globe className="w-3 h-3" />;
  }
}

export default function NotificationsPage() {
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [typeFilter, setTypeFilter] = useState('');
  const [canalFilter, setCanalFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', 'list', page, unreadOnly],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page), unreadOnly: String(unreadOnly) });
      const res = await api.get(`/notifications?${params}`);
      return res.data as PageResponse<Notification>;
    },
  });

  const { data: unreadCount } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: async () => (await api.get('/notifications/unread-count')).data as number,
  });

  const invalidate = () => { queryClient.invalidateQueries({ queryKey: ['notifications'] }); };

  const readMutation = useMutation({
    mutationFn: async (id: string) => { await api.patch(`/notifications/${id}/read`); },
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const markAllMutation = useMutation({
    mutationFn: async () => { await api.post('/notifications/mark-all-read'); },
    onSuccess: () => { invalidate(); toast.success('Toutes les notifications ont été marquées comme lues'); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const notifications = data?.content || [];
  const unread = notifications.filter((n) => !n.lu).length;
  const globalUnread = unreadCount ?? 0;

  // Client-side filtering
  const filtered = useMemo(() => {
    let items = notifications;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter(n => n.titre?.toLowerCase().includes(q) || n.message?.toLowerCase().includes(q));
    }
    if (typeFilter) items = items.filter(n => n.type === typeFilter);
    if (canalFilter) items = items.filter(n => n.canal === canalFilter);
    return items;
  }, [notifications, searchTerm, typeFilter, canalFilter]);

  // Stats
  const stats = useMemo(() => {
    const types = new Map<string, number>();
    const canaux = new Map<string, number>();
    notifications.forEach(n => { types.set(n.type, (types.get(n.type) || 0) + 1); canaux.set(n.canal, (canaux.get(n.canal) || 0) + 1); });
    return { total: data?.totalElements ?? notifications.length, unread: globalUnread, types: Object.fromEntries(types), canaux: Object.fromEntries(canaux) };
  }, [notifications, data, globalUnread]);

  const typeEntries = useMemo(() => Object.entries(stats.types).sort(([, a], [, b]) => b - a).slice(0, 6), [stats.types]);
  const hasActiveFilters = Boolean(typeFilter || canalFilter || searchTerm);

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BellRing className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Notifications</h1>
          </div>
          <p className="page-subtitle">
            {globalUnread > 0
              ? `${globalUnread} notification${globalUnread > 1 ? 's' : ''} non lue${globalUnread > 1 ? 's' : ''}`
              : 'Tout est à jour'} — {stats.total} notification(s) au total
          </p>
        </div>
        <div className="flex items-center gap-2 animate-fade-in">
          <button onClick={() => { setUnreadOnly(!unreadOnly); setPage(0); }}
            className={`btn-sm ${unreadOnly ? 'btn-primary' : 'btn-ghost'}`}>
            <Bell className="w-4 h-4" /> Non lues
          </button>
          <button onClick={() => markAllMutation.mutate()} disabled={markAllMutation.isPending || globalUnread === 0}
            className="btn-secondary btn-sm">
            {markAllMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCheck className="w-4 h-4" />}
            Tout marquer comme lu
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total', value: stats.total, icon: Bell, color: 'from-primary-500 to-primary-600' },
          { label: 'Non lues', value: stats.unread, icon: BellRing, color: 'from-amber-500 to-orange-500' },
          { label: 'Types actifs', value: Object.keys(stats.types).length, icon: BarChart3, color: 'from-blue-500 to-indigo-500' },
          { label: 'Canaux', value: Object.keys(stats.canaux).length, icon: Mail, color: 'from-emerald-500 to-green-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
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

      {/* Search & filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher une notification..."
              value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9" />
          </div>
          <div className="flex items-center gap-1.5">
            <Filter className="w-4 h-4 text-gray-400" />
            <select value={typeFilter} onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
              className="input !w-auto text-xs">
              <option value="">Tous les types</option>
              {typeEntries.map(([type, count]) => (
                <option key={type} value={type}>{TYPE_FALLBACK[type] || type} ({count})</option>
              ))}
            </select>
            <select value={canalFilter} onChange={(e) => { setCanalFilter(e.target.value); setPage(0); }}
              className="input !w-auto text-xs">
              <option value="">Tous les canaux</option>
              <option value="IN_APP">📱 Dans l'application</option>
              <option value="EMAIL">✉️ E-mail</option>
              <option value="PUSH">🔔 Push</option>
            </select>
          </div>
          {hasActiveFilters && (
            <button onClick={() => { setSearchTerm(''); setTypeFilter(''); setCanalFilter(''); setPage(0); }}
              className="btn-ghost btn-sm">
              <X className="w-3.5 h-3.5" /> Réinitialiser
            </button>
          )}
        </div>
      </div>

      {/* Explanation */}
      <div className="glass-card p-4 mb-6 text-sm text-gray-500 dark:text-gray-400 flex items-start gap-3 animate-slide-up" style={{ animationDelay: '180ms' }}>
        <Sparkles className="w-4 h-4 text-primary-500 mt-0.5 flex-shrink-0" />
        <p>
          Les libellés et couleurs de chaque type de notification sont
          <strong className="text-gray-700 dark:text-gray-200"> configurables</strong> depuis{' '}
          <span className="font-mono text-[11px]">Administration → Dictionnaires</span> — adaptez-les au
          vocabulaire de votre église.
        </p>
      </div>

      {/* List */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="glass-card p-4 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="skeleton h-4 w-56 mb-2" />
              <div className="skeleton h-3 w-40" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-14 text-center animate-scale-in">
          <div className="inline-flex p-4 rounded-2xl bg-gray-100 dark:bg-gray-800 mb-4">
            <Inbox className="w-10 h-10 text-gray-400" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
            {notifications.length === 0 ? 'Aucune notification' : 'Aucun résultat'}
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {notifications.length === 0
              ? 'Les événements de la plateforme apparaîtront ici.'
              : 'Essayez de modifier les filtres.'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((n, i) => (
            <button
              key={n.id}
              onClick={() => { if (!n.lu) readMutation.mutate(n.id); }}
              className={`w-full text-left glass-card p-4 animate-slide-up hover-lift transition-all ${
                n.lu ? 'opacity-75' : 'border-l-4 border-l-primary-500'
              }`}
              style={{ animationDelay: `${i * 30}ms` }}
            >
              <div className="flex items-start gap-4">
                <div className={`p-2.5 rounded-xl flex-shrink-0 ${
                  n.lu ? 'bg-gray-100 dark:bg-gray-800 text-gray-400' : 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
                }`}>
                  {n.lu ? <Bell className="w-5 h-5" /> : <BellRing className="w-5 h-5 animate-pulse-soft" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1">
                    <span className="badge text-[10px] bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300"
                      style={dictionaries.color('NOTIFICATION_TYPE', n.type) ? { backgroundColor: `${dictionaries.color('NOTIFICATION_TYPE', n.type)}22`, color: dictionaries.color('NOTIFICATION_TYPE', n.type) } : undefined}>
                      {dictionaries.label('NOTIFICATION_TYPE', n.type) || TYPE_FALLBACK[n.type] || n.type}
                    </span>
                    <span className="inline-flex items-center gap-1 badge text-[10px] badge-gray">
                      {canalIcon(n.canal)} {dictionaries.label('NOTIFICATION_CANAL', n.canal) || CANAL_FALLBACK[n.canal] || n.canal}
                    </span>
                    {!n.lu && <span className="badge text-[10px] badge-primary">Nouveau</span>}
                  </div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{n.titre}</p>
                  {n.message && <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-2">{n.message}</p>}
                  <div className="flex items-center gap-2 mt-1.5">
                    <Clock className="w-3 h-3 text-gray-400" />
                    <span className="text-xs text-gray-400">
                      {new Date(n.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600 flex-shrink-0 mt-2" />
              </div>
            </button>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 animate-fade-in">
          <p className="text-sm text-gray-500 dark:text-gray-400">Page {data.number + 1} / {data.totalPages} · {data.totalElements} notification(s)</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}
    </div>
  );
}

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Notification, PageResponse } from '@/types';
import {
  Bell, BellRing, CheckCheck, Mail, Smartphone, Globe, Loader2, Clock,
  Inbox, Sparkles, ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const TYPE_FALLBACK: Record<string, string> = {
  RAPPORT_NON_SOUMIS: 'Rapport non soumis',
  ABSENCE_48H: 'Absence 48h',
  RAPPORT_FAMILLE_NON_SOUMIS: 'Rapport famille non soumis',
  ALERTE_ABSENCE: 'Alerte absence',
  INFORMATION: 'Information',
  PRIERE_EXAUCEE: 'Prières exaucées',
  TRANSFERT_DEMANDE: 'Demande de transfert',
  TRANSFERT_VALIDATION: 'Validation de transfert',
  TRANSFERT_VALIDEE: 'Transfert validé',
  TRANSFERT_REFUSEE: 'Transfert refusé',
  TRANSFERT_INFOS_DEMANDEES: 'Informations demandées',
  TRANSFERT_CORRECTION: 'Correction demandée',
  TRANSFERT_EXECUTEE: 'Transfert exécuté',
  TRANSFERT_ANNULEE: 'Transfert annulé',
  TRANSFERT_DELAI_DEPASSE: 'Délai dépassé',
};

const CANAL_FALLBACK: Record<string, string> = {
  IN_APP: 'Dans l’application',
  EMAIL: 'E-mail',
  PUSH: 'Push',
};

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

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', 'list', page, unreadOnly],
    queryFn: async () => {
      const res = await api.get(`/notifications?size=20&page=${page}&unreadOnly=${unreadOnly}`);
      return res.data as PageResponse<Notification>;
    },
  });

  // Compteur global de non-lues (toutes pages) pour l'action « tout marquer comme lu ».
  const { data: unreadCount } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: async () => (await api.get('/notifications/unread-count')).data as number,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
  };

  const readMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/notifications/${id}/read`);
    },
    onSuccess: invalidate,
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const markAllMutation = useMutation({
    mutationFn: async () => {
      await api.post('/notifications/mark-all-read');
    },
    onSuccess: () => {
      invalidate();
      toast.success('Toutes les notifications ont été marquées comme lues');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const notifications = data?.content || [];
  const unread = notifications.filter((n) => !n.lu).length;
  const globalUnread = unreadCount ?? 0;

  return (
    <div className="page-container max-w-3xl">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BellRing className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Notifications</h1>
          </div>
          <p className="page-subtitle">
            {unread > 0
              ? `${unread} notification${unread > 1 ? 's' : ''} non lue${unread > 1 ? 's' : ''}`
              : 'Tout est à jour'}
          </p>
        </div>
        <div className="flex items-center gap-2 animate-fade-in">
          <button
            onClick={() => { setUnreadOnly(!unreadOnly); setPage(0); }}
            className={`btn-sm ${unreadOnly ? 'btn-primary' : 'btn-ghost'}`}
          >
            <Bell className="w-4 h-4" /> Non lues
          </button>
          <button
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending || globalUnread === 0}
            className="btn-secondary btn-sm"
          >
            {markAllMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCheck className="w-4 h-4" />}
            Tout marquer comme lu
          </button>
        </div>
      </div>

      {/* Explication */}
      <div className="glass-card p-4 mb-6 text-sm text-gray-500 dark:text-gray-400 flex items-start gap-3 animate-slide-up">
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
      ) : notifications.length === 0 ? (
        <div className="glass-card p-14 text-center animate-scale-in">
          <div className="inline-flex p-4 rounded-2xl bg-gray-100 dark:bg-gray-800 mb-4">
            <Inbox className="w-10 h-10 text-gray-400" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
            {unreadOnly ? 'Aucune notification non lue' : 'Aucune notification'}
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Les événements de la plateforme (rapports, absences, transferts, prières…) apparaîtront ici.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((n, i) => (
            <button
              key={n.id}
              onClick={() => { if (!n.lu) readMutation.mutate(n.id); }}
              className={`w-full text-left glass-card p-4 animate-slide-up hover-lift transition-all ${
                n.lu ? 'opacity-75' : 'border-l-4 border-l-primary-500'
              }`}
              style={{ animationDelay: `${i * 40}ms` }}
            >
              <div className="flex items-start gap-4">
                {/* Icon */}
                <div className={`p-2.5 rounded-xl flex-shrink-0 ${
                  n.lu
                    ? 'bg-gray-100 dark:bg-gray-800 text-gray-400'
                    : 'bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
                }`}>
                  {n.lu ? <Bell className="w-5 h-5" /> : <BellRing className="w-5 h-5 animate-pulse-soft" />}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1">
                    <span
                      className="badge text-[10px] bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300"
                      style={dictionaries.color('NOTIFICATION_TYPE', n.type)
                        ? { backgroundColor: `${dictionaries.color('NOTIFICATION_TYPE', n.type)}22`, color: dictionaries.color('NOTIFICATION_TYPE', n.type) }
                        : undefined}
                    >
                      {dictionaries.label('NOTIFICATION_TYPE', n.type) || TYPE_FALLBACK[n.type] || n.type}
                    </span>
                    <span className="inline-flex items-center gap-1 badge text-[10px] badge-gray">
                      {canalIcon(n.canal)}
                      {dictionaries.label('NOTIFICATION_CANAL', n.canal) || CANAL_FALLBACK[n.canal] || n.canal}
                    </span>
                    {!n.lu && <span className="badge text-[10px] badge-primary">Nouveau</span>}
                  </div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{n.titre}</p>
                  {n.message && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-2">{n.message}</p>
                  )}
                  <div className="flex items-center gap-2 mt-1.5">
                    <Clock className="w-3 h-3 text-gray-400" />
                    <span className="text-xs text-gray-400">
                      {new Date(n.createdAt).toLocaleDateString('fr-FR', {
                        day: 'numeric', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                      })}
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
    </div>
  );
}

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Inbox, Clock, CheckCircle2, XCircle, MessageSquareText, Loader2,
  Bug, Lightbulb, RefreshCw, Smartphone, Monitor, Globe, AlertTriangle,
  FlaskConical, RotateCcw, ShieldOff,
} from 'lucide-react';
import type { Feedback, FeedbackStats, FeedbackStatus } from '@/types';

const CATEGORY_LABELS: Record<string, string> = {
  BUG: 'Bug',
  UX: 'UX',
  SUGGESTION: 'Suggestion',
  FONCTIONNALITE_MANQUANTE: 'Fonctionnalité manquante',
  PERFORMANCE: 'Performance',
  TRADUCTION: 'Traduction',
  AFFICHAGE: 'Affichage',
  AUTRE: 'Autre',
};

const STATUS_LABELS: Record<FeedbackStatus, string> = {
  NOUVEAU: 'Nouveau',
  EN_COURS: 'En cours',
  RESOLU: 'Résolu',
  REJETE: 'Rejeté',
};

const STATUS_STYLES: Record<FeedbackStatus, string> = {
  NOUVEAU: 'bg-blue-500/10 border-blue-500/20 text-blue-500',
  EN_COURS: 'bg-amber-500/10 border-amber-500/20 text-amber-500',
  RESOLU: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-500',
  REJETE: 'bg-gray-500/10 border-gray-500/20 text-gray-400',
};

const PRIORITY_STYLES: Record<string, string> = {
  BASSE: 'text-gray-400',
  MOYENNE: 'text-sky-400',
  HAUTE: 'text-amber-400',
  CRITIQUE: 'text-red-400',
};

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  } catch {
    return iso;
  }
}

function StatCard({ icon: Icon, label, value, gradient }: {
  icon: React.ElementType; label: string; value: number; gradient: string;
}) {
  return (
    <div className="glass-card p-4 sm:p-5 flex items-center gap-3.5">
      <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center shadow-md flex-shrink-0`}>
        <Icon className="w-5 h-5 text-white" />
      </div>
      <div className="min-w-0">
        <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono leading-none">{value}</p>
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 truncate">{label}</p>
      </div>
    </div>
  );
}

export default function AdminFeedbackPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<'TOUS' | FeedbackStatus>('TOUS');
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);

  // État de l'environnement bêta (profil beta : reset activé)
  const { data: betaStatus } = useQuery({
    queryKey: ['admin-beta', 'status'],
    queryFn: async () => {
      const res = await api.get('/admin/beta/status');
      return res.data as { environment: string; resetEnabled: boolean };
    },
    retry: false,
  });

  const resetMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post('/admin/beta/reset');
      return res.data as { status: string; environment: string; truncatedTables: number; resetAt: string };
    },
    onSuccess: (data) => {
      setResetConfirmOpen(false);
      queryClient.invalidateQueries({ queryKey: ['admin-beta'] });
      queryClient.invalidateQueries({ queryKey: ['admin-feedback'] });
      toast.success(`Environnement bêta réinitialisé (${data.truncatedTables} tables restaurées)`);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const { data: feedbacks = [], isLoading } = useQuery({
    queryKey: ['admin-feedback'],
    queryFn: async () => {
      const res = await api.get('/admin/feedback');
      return res.data as Feedback[];
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['admin-feedback', 'stats'],
    queryFn: async () => {
      const res = await api.get('/admin/feedback/stats');
      return res.data as FeedbackStats;
    },
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: FeedbackStatus }) => {
      const res = await api.patch(`/admin/feedback/${id}/status`, { status });
      return res.data as Feedback;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-feedback'] });
      queryClient.invalidateQueries({ queryKey: ['admin-feedback', 'stats'] });
      toast.success('Statut mis à jour');
    },
    onError: () => toast.error('Erreur lors de la mise à jour du statut'),
  });

  const filtered = useMemo(
    () => (statusFilter === 'TOUS' ? feedbacks : feedbacks.filter((f) => f.status === statusFilter)),
    [feedbacks, statusFilter]
  );

  const categoryBreakdown = useMemo(() => {
    if (!stats?.parCategorie) return [];
    const total = stats.parCategorie;
    const entries = Object.entries(total).sort((a, b) => b[1] - a[1]);
    const max = entries[0]?.[1] || 1;
    return entries.map(([cat, count]) => ({ cat, count, pct: Math.round((count / max) * 100) }));
  }, [stats]);

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title">Retours testeurs</h1>
          <p className="page-subtitle">
            Bugs, suggestions et retours UX envoyés via le widget de feedback. Gérez leur statut ici.
          </p>
        </div>
        <div className="page-header-actions">
          <button
            className="btn-ghost btn-sm"
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['admin-feedback'] });
              toast.success('Liste actualisée');
            }}
          >
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5 mb-6">
        <StatCard icon={Inbox} label="Total" value={stats?.total ?? 0} gradient="from-primary-500 to-primary-700" />
        <StatCard icon={MessageSquareText} label="Nouveaux" value={stats?.nouveaux ?? 0} gradient="from-sky-500 to-blue-600" />
        <StatCard icon={Clock} label="En cours" value={stats?.enCours ?? 0} gradient="from-amber-500 to-orange-600" />
        <StatCard icon={CheckCircle2} label="Résolus" value={stats?.resolus ?? 0} gradient="from-emerald-500 to-teal-600" />
        <StatCard icon={XCircle} label="Rejetés" value={stats?.rejetes ?? 0} gradient="from-gray-500 to-slate-600" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Liste */}
        <div className="lg:col-span-2 space-y-3">
          {/* Filtre par statut */}
          <div className="flex flex-wrap items-center gap-2">
            {(['TOUS', 'NOUVEAU', 'EN_COURS', 'RESOLU', 'REJETE'] as const).map((s) => (
              <button
                key={s}
                onClick={() => setStatusFilter(s)}
                className={`px-3 py-1.5 rounded-xl text-xs font-medium transition-all duration-200 border
                  ${statusFilter === s
                    ? 'bg-primary-500/10 border-primary-500/30 text-primary-600 dark:text-primary-400'
                    : 'border-gray-200/70 dark:border-gray-700/50 text-gray-500 dark:text-gray-400 hover:border-gray-300 dark:hover:border-gray-600'}`}
              >
                {s === 'TOUS' ? 'Tous' : STATUS_LABELS[s]}
                {s !== 'TOUS' && (
                  <span className="ml-1.5 text-[10px] opacity-70">
                    {stats ? (s === 'NOUVEAU' ? stats.nouveaux : s === 'EN_COURS' ? stats.enCours : s === 'RESOLU' ? stats.resolus : stats.rejetes) : ''}
                  </span>
                )}
              </button>
            ))}
          </div>

          {isLoading ? (
            <div className="min-h-[40vh] flex items-center justify-center">
              <div className="spinner h-8 w-8" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="glass-card p-10 text-center">
              <div className="w-14 h-14 mx-auto mb-4 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center">
                <Inbox className="w-6 h-6 text-gray-400" />
              </div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Aucun retour {statusFilter !== 'TOUS' ? 'avec ce statut' : 'pour le moment'}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Les retours des testeurs apparaîtront ici dès qu'ils seront envoyés.
              </p>
            </div>
          ) : (
            filtered.map((f) => (
              <div key={f.id} className="glass-card p-4 sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="flex items-start gap-3 min-w-0 flex-1">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0
                      ${f.category === 'BUG' ? 'bg-red-500/10 text-red-400' : 'bg-primary-500/10 text-primary-400'}`}>
                      {f.category === 'BUG' ? <Bug className="w-4.5 h-4.5" /> : <Lightbulb className="w-4.5 h-4.5" />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">{f.subject}</p>
                        <span className={`text-[10px] font-bold uppercase ${PRIORITY_STYLES[f.priority] || ''}`}>
                          {f.priority}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">
                        {f.description || '—'}
                      </p>
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2.5 text-[11px] text-gray-400 dark:text-gray-500">
                        <span className="inline-flex items-center gap-1">
                          {CATEGORY_LABELS[f.category] || f.category}
                        </span>
                        {f.reporterEmail && <span>par {f.reporterEmail}</span>}
                        <span>{formatDate(f.createdAt)}</span>
                        {f.appVersion && <span>v{f.appVersion}</span>}
                      </div>
                      {(f.pageUrl || f.browser || f.os || f.device) && (
                        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1.5 text-[11px] text-gray-400 dark:text-gray-600">
                          {f.device && <span className="inline-flex items-center gap-1">{f.device === 'Mobile' ? <Smartphone className="w-3 h-3" /> : <Monitor className="w-3 h-3" />}{f.device}</span>}
                          {f.browser && <span className="inline-flex items-center gap-1"><Globe className="w-3 h-3" />{f.browser}</span>}
                          {f.os && <span>{f.os}</span>}
                          {f.pageUrl && <span className="truncate max-w-[240px] font-mono">{f.pageUrl}</span>}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Statut */}
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${STATUS_STYLES[f.status] || ''}`}>
                      {STATUS_LABELS[f.status] || f.status}
                    </span>
                    <select
                      className="input !py-1.5 !w-auto text-xs"
                      value={f.status}
                      disabled={statusMutation.isPending}
                      onChange={(e) => statusMutation.mutate({ id: f.id, status: e.target.value as FeedbackStatus })}
                      aria-label={`Changer le statut de « ${f.subject} »`}
                    >
                      {(Object.keys(STATUS_LABELS) as FeedbackStatus[]).map((s) => (
                        <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Répartition par catégorie */}
        <div className="space-y-3">
          <div className="glass-card p-5">
            <div className="flex items-center gap-2 mb-4 text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">
              <AlertTriangle className="w-4 h-4" /> Répartition par catégorie
            </div>
            {categoryBreakdown.length === 0 ? (
              <p className="text-xs text-gray-400 dark:text-gray-500">Aucune donnée pour le moment.</p>
            ) : (
              <div className="space-y-3">
                {categoryBreakdown.map(({ cat, count, pct }) => (
                  <div key={cat}>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-gray-600 dark:text-gray-300">{CATEGORY_LABELS[cat] || cat}</span>
                      <span className="font-mono text-gray-400">{count}</span>
                    </div>
                    <div className="h-1.5 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-primary-500 to-gold-500 transition-all duration-500"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Environnement de test — reset bêta (profil beta uniquement) */}
          <div className="glass-card p-5">
            <div className="flex items-center gap-2 mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">
              <FlaskConical className="w-4 h-4" /> Environnement de test
            </div>
            {betaStatus ? (
              <>
                <div className="flex items-center justify-between text-xs mb-3">
                  <span className="text-gray-500 dark:text-gray-400">Environnement</span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border
                    ${betaStatus.environment === 'beta' ? 'bg-amber-500/10 border-amber-500/30 text-amber-500' : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-500'}`}>
                    {betaStatus.environment}
                  </span>
                </div>
                {betaStatus.resetEnabled ? (
                  <>
                    <p className="text-xs text-gray-500 dark:text-gray-400 leading-relaxed mb-3">
                      Réinitialisez les données de démonstration : les modifications des testeurs sont
                      supprimées et le jeu de données fictives est restauré.
                    </p>
                    <button
                      className="btn-ghost btn-sm w-full !border-red-500/30 !text-red-500 hover:!bg-red-500/10"
                      disabled={resetMutation.isPending}
                      onClick={() => setResetConfirmOpen(true)}
                    >
                      {resetMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <RotateCcw className="w-4 h-4" />}
                      Réinitialiser l'environnement
                    </button>
                  </>
                ) : (
                  <p className="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
                    <ShieldOff className="w-3.5 h-3.5" />
                    Réinitialisation désactivée sur cet environnement (production protégée).
                  </p>
                )}
              </>
            ) : (
              <p className="text-xs text-gray-400 dark:text-gray-500">
                État de l'environnement indisponible.
              </p>
            )}
          </div>

          {/* Modale de confirmation du reset */}
          {resetConfirmOpen && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in">
              <div className="absolute inset-0 bg-black/50 dark:bg-black/60 backdrop-blur-sm" onClick={() => setResetConfirmOpen(false)} />
              <div className="relative w-full max-w-md glass-strong rounded-3xl shadow-glass-lg border border-white/20 dark:border-white/[0.06] p-6 animate-scale-in">
                <div className="w-12 h-12 rounded-2xl bg-red-500/10 flex items-center justify-center mb-4">
                  <RotateCcw className="w-5 h-5 text-red-400" />
                </div>
                <h3 className="text-base font-bold text-gray-900 dark:text-white font-display mb-1.5">
                  Réinitialiser l'environnement de test ?
                </h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed mb-5">
                  Toutes les modifications des testeurs seront supprimées et les données de démonstration
                  seront restaurées. Cette action est irréversible. Vous serez déconnecté : reconnectez-vous
                  avec un compte de démonstration.
                </p>
                <div className="flex items-center justify-end gap-2">
                  <button className="btn-ghost btn-sm" onClick={() => setResetConfirmOpen(false)}>Annuler</button>
                  <button
                    className="btn-primary btn-sm !bg-red-600 hover:!bg-red-500 border-red-600"
                    disabled={resetMutation.isPending}
                    onClick={() => resetMutation.mutate()}
                  >
                    {resetMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <RotateCcw className="w-4 h-4" />}
                    Réinitialiser
                  </button>
                </div>
              </div>
            </div>
          )}

          <div className="glass-card p-5">
            <div className="flex items-center gap-2 mb-2 text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500">
              <MessageSquareText className="w-4 h-4" /> Conseil
            </div>
            <p className="text-xs text-gray-500 dark:text-gray-400 leading-relaxed">
              Passez chaque retour en <span className="text-gray-700 dark:text-gray-200 font-medium">En cours</span> dès
              qu'il est pris en charge, puis <span className="text-gray-700 dark:text-gray-200 font-medium">Résolu</span>
              une fois corrigé. Un retour <span className="text-gray-700 dark:text-gray-200 font-medium">Rejeté</span> reste
              consultable dans les statistiques.
            </p>
            {statusMutation.isPending && (
              <div className="flex items-center gap-2 mt-3 text-xs text-gray-400">
                <Loader2 className="w-3.5 h-3.5 animate-spin" /> Mise à jour…
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

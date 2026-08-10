import { useState } from 'react';
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
} from 'lucide-react';
import AttachmentLinks from '@/components/shared/AttachmentLinks';

const TYPE_LABELS: Record<string, string> = {
  SUGGESTION: '💡 Suggestion',
  RENDEZ_VOUS: '📅 Rendez-vous',
  SIGNALEMENT: '⚠️ Signalement',
};

const CIBLE_LABELS: Record<string, string> = {
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

const STATUS_LABELS: Record<MemberRequestStatus, string> = {
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
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'demandes' | 'presences'>('demandes');
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
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateStatus = (req: MemberRequest, statut: MemberRequestStatus) => {
    statusMutation.mutate({
      id: req.id,
      payload: { statut, reponse: replyText[req.id]?.trim() || undefined },
    });
  };

  const openRequests = requests.filter((r) => r.statut === 'OUVERT' || r.statut === 'EN_COURS');
  const isPasteurOrAdmin = user?.activeRole === 'PASTEUR' || user?.activeRole === 'ADMIN';

  // Regrouper les présences : une entrée par membre (semaine la plus récente)
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
              {isPasteurOrAdmin
                ? 'Toute l\'église'
                : user?.activeRole === 'RESPONSABLE'
                  ? 'Mes départements'
                  : 'Ma famille'}
            </span>
          </div>
          <h1 className="page-title">
            Demandes <span className="text-gradient font-display">des membres</span>
          </h1>
          <p className="page-subtitle">
            Suggestions, rendez-vous et signalements adressés à vous
            {openRequests.length > 0 && (
              <span className="ml-2 badge badge-warning text-xs">{openRequests.length} à traiter</span>
            )}
          </p>
        </div>
      </div>

      {/* Onglets */}
      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setTab('demandes')}
          className={`btn-sm ${tab === 'demandes' ? 'btn-primary' : 'btn-secondary'}`}
        >
          <MessageSquare className="w-4 h-4" /> Demandes ({requests.length})
        </button>
        <button
          onClick={() => setTab('presences')}
          className={`btn-sm ${tab === 'presences' ? 'btn-primary' : 'btn-secondary'}`}
        >
          <CalendarCheck className="w-4 h-4" /> Présences des membres ({latestByMember.size})
        </button>
      </div>

      {tab === 'demandes' && (
        <>
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
          ) : requests.length === 0 ? (
            <div className="glass-card p-10 text-center animate-scale-in">
              <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/20 w-fit mx-auto mb-4">
                <Inbox className="w-6 h-6 text-green-500" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune demande</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Les membres n'ont pas encore envoyé de suggestion, de rendez-vous ou de signalement dans votre périmètre.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {requests.map((r, i) => {
                const StatusIcon = STATUS_ICONS[r.statut];
                return (
                  <div
                    key={r.id}
                    className="glass-card p-5 animate-slide-up flex flex-col"
                    style={{ animationDelay: `${i * 60}ms` }}
                  >
                    <div className="flex items-center justify-between gap-2 flex-wrap mb-3">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="badge text-[10px] badge-info">{TYPE_LABELS[r.type]}</span>
                        <span className="badge text-[10px] badge-warning">{CIBLE_LABELS[r.cible]}</span>
                        <span className={`badge text-[10px] ${STATUS_BADGES[r.statut]}`}>
                          {STATUS_LABELS[r.statut]}
                        </span>
                      </div>
                      <span className="text-[10px] text-gray-400">
                        {new Date(r.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>

                    <p className="text-sm text-gray-700 dark:text-gray-200 leading-relaxed flex-1">{r.message}</p>

                    {r.piecesJointes && r.piecesJointes.length > 0 && (
                      <div className="mt-2.5">
                        <AttachmentLinks pieces={r.piecesJointes} />
                      </div>
                    )}

                    {/* Auteur + portée */}
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

                    {/* Actions de traitement */}
                    {r.statut !== 'RESOLU' && r.statut !== 'REJETE' && (
                      <div className="mt-4 pt-3 border-t border-white/20 dark:border-white/[0.06]">
                        <textarea
                          className="input !py-2 text-sm"
                          rows={2}
                          placeholder="Votre réponse (facultatif)"
                          value={replyText[r.id] || ''}
                          onChange={(e) => setReplyText((s) => ({ ...s, [r.id]: e.target.value }))}
                        />
                        <div className="flex flex-wrap gap-2 mt-2">
                          <button
                            onClick={() => updateStatus(r, 'EN_COURS')}
                            disabled={statusMutation.isPending}
                            className="btn-secondary btn-xs"
                          >
                            <PlayCircle className="w-3.5 h-3.5" /> Prendre en charge
                          </button>
                          <button
                            onClick={() => updateStatus(r, 'RESOLU')}
                            disabled={statusMutation.isPending}
                            className="btn-primary btn-xs"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" /> Résoudre
                          </button>
                          <button
                            onClick={() => updateStatus(r, 'REJETE')}
                            disabled={statusMutation.isPending}
                            className="btn-danger btn-xs"
                          >
                            <XCircle className="w-3.5 h-3.5" /> Rejeter
                          </button>
                        </div>
                      </div>
                    )}

                    {r.statut === 'RESOLU' || r.statut === 'REJETE' ? (
                      <div className="mt-3 flex items-center gap-1.5 text-[11px] text-gray-400">
                        <StatusIcon className="w-3.5 h-3.5" />
                        Traité le {r.dateTraitement ? new Date(r.dateTraitement).toLocaleDateString('fr-FR') : '—'}
                        {r.traiteParNom && <> par {r.traiteParNom}</>}
                      </div>
                    ) : null}
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
    </div>
  );
}

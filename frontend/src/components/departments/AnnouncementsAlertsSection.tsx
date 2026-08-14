import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Megaphone, Bell, Loader2, Plus, Send, Trash2, AlertTriangle, Clock,
  UserPlus, Target, CheckCircle2,
} from 'lucide-react';

const CIBLE_LABELS: Record<string, string> = {
  TOUS: 'Tout le département',
  EQUIPE: 'Une équipe',
  POSTE: 'Un poste',
};

const ALERT_LABELS: Record<string, string> = {
  ABSENCE_REPETEE: 'Absences répétées',
  TACHE_EN_RETARD: 'Tâche en retard',
};

export default function AnnouncementsAlertsSection({ deptId }: { deptId: string }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [message, setMessage] = useState('');
  const [cible, setCible] = useState('TOUS');
  const [teamId, setTeamId] = useState('');
  const [positionId, setPositionId] = useState('');

  const { data: announcements = [] } = useQuery({
    queryKey: ['department', deptId, 'announcements'],
    queryFn: async () => (await api.get(`/departments/${deptId}/announcements`)).data as any[],
    enabled: !!deptId,
  });

  const { data: alerts = [] } = useQuery({
    queryKey: ['department', deptId, 'alerts', 'smart'],
    queryFn: async () => (await api.get(`/departments/${deptId}/alerts/smart`)).data as any[],
    enabled: !!deptId,
  });

  const { data: overview } = useQuery({
    queryKey: ['department', deptId, 'management'],
    queryFn: async () => (await api.get(`/departments/${deptId}/management`)).data as any,
    enabled: !!deptId,
  });
  const teams: any[] = overview?.teams ?? [];
  const positions: any[] = overview?.positions ?? [];

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'announcements'] });
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'alerts', 'smart'] });
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(`/departments/${deptId}/announcements`, {
        titre: titre.trim(), message: message.trim(), cible,
        teamId: cible === 'EQUIPE' && teamId ? teamId : null,
        positionId: cible === 'POSTE' && positionId ? positionId : null,
      });
      return res.data;
    },
    onSuccess: () => {
      toast.success('Annonce publiée ✅');
      setTitre(''); setMessage(''); setCible('TOUS'); setTeamId(''); setPositionId('');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (announcementId: string) => api.delete(`/departments/${deptId}/announcements/${announcementId}`),
    onSuccess: () => {
      toast.success('Annonce supprimée');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
      {/* Annonces */}
      <div className="glass-card p-5">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <Megaphone className="w-5 h-5 text-amber-500" /> Annonces du département ({announcements.length})
        </h2>

        <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="sm:col-span-2">
              <label className="label">Titre *</label>
              <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)} placeholder="Ex : Réunion d'équipe samedi…" />
            </div>
            <div className="sm:col-span-2">
              <label className="label">Message *</label>
              <textarea className="input" rows={2} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Contenu de l'annonce…" />
            </div>
            <div>
              <label className="label">Cible</label>
              <select className="input" value={cible} onChange={(e) => setCible(e.target.value)}>
                {Object.entries(CIBLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            {cible === 'EQUIPE' && (
              <div>
                <label className="label">Équipe *</label>
                <select className="input" value={teamId} onChange={(e) => setTeamId(e.target.value)}>
                  <option value="">— Choisir —</option>
                  {teams.filter((t) => t.statut === 'ACTIVE').map((t) => (
                    <option key={t.id} value={t.id}>{t.nom}</option>
                  ))}
                </select>
              </div>
            )}
            {cible === 'POSTE' && (
              <div>
                <label className="label">Poste *</label>
                <select className="input" value={positionId} onChange={(e) => setPositionId(e.target.value)}>
                  <option value="">— Choisir —</option>
                  {positions.filter((p) => p.statut === 'ACTIVE').map((p) => (
                    <option key={p.id} value={p.id}>{p.nom}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
          <button
            onClick={() => createMutation.mutate()}
            disabled={!titre.trim() || !message.trim() || createMutation.isPending || (cible === 'EQUIPE' && !teamId) || (cible === 'POSTE' && !positionId)}
            className="btn-primary btn-sm mt-3 cursor-pointer"
          >
            {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />} Publier
          </button>
        </div>

        {announcements.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">Aucune annonce — publiez la première</p>
        ) : (
          <div className="space-y-2 max-h-80 overflow-y-auto">
            {announcements.map((a) => (
              <div key={a.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40 border-l-[3px] border-l-amber-500">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 flex-wrap">
                      {a.titre}
                      <span className="badge text-[9px] badge-info">{CIBLE_LABELS[a.cible] || a.cible}</span>
                    </p>
                    <p className="text-xs text-gray-600 dark:text-gray-300 mt-0.5">{a.message}</p>
                    <p className="text-[10px] text-gray-400 mt-1">
                      {a.auteurNom || ''} · {new Date(a.createdAt).toLocaleDateString('fr-FR')}
                      {a.teamNom ? ` · ${a.teamNom}` : ''}{a.positionNom ? ` · ${a.positionNom}` : ''}
                    </p>
                  </div>
                  <button
                    onClick={() => { if (confirm(`Supprimer l'annonce « ${a.titre} » ?`)) deleteMutation.mutate(a.id); }}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer shrink-0"
                    title="Supprimer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Alertes intelligentes */}
      <div className="glass-card p-5">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
          <Bell className="w-5 h-5 text-amber-500" /> Alertes intelligentes ({alerts.length})
        </h2>
        <p className="text-xs text-gray-400 mb-3">
          Détection automatique : absences répétées et tâches en retard. Ces alertes sont aussi visibles dans les dossiers membres.
        </p>
        {alerts.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-6">
            <CheckCircle2 className="w-8 h-8 mx-auto mb-2 text-emerald-500" />
            Aucune alerte en cours — tout est sous contrôle
          </p>
        ) : (
          <div className="space-y-2 max-h-80 overflow-y-auto">
            {alerts.map((a) => (
              <div key={a.id} className={`p-3 rounded-xl border ${
                a.priorite === 'HAUTE'
                  ? 'bg-red-50 dark:bg-red-900/20 border-red-200/50 dark:border-red-500/20'
                  : 'bg-amber-50 dark:bg-amber-900/20 border-amber-200/50 dark:border-amber-500/20'
              }`}>
                <div className="flex items-start gap-2">
                  {a.priorite === 'HAUTE'
                    ? <AlertTriangle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
                    : <Clock className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />}
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 flex-wrap">
                      {a.titre}
                      <span className="badge text-[9px] badge-danger">{ALERT_LABELS[a.typeAlerte] || a.typeAlerte.replace(/_/g, ' ')}</span>
                    </p>
                    <p className="text-xs text-gray-600 dark:text-gray-300 mt-0.5">{a.message}</p>
                    {a.ameNom && (
                      <a
                        href={`#/departments/${deptId}/members/${a.ameId}`}
                        className="inline-flex items-center gap-1 text-[10px] font-medium text-primary-600 mt-1 hover:underline"
                      >
                        <UserPlus className="w-3 h-3" /> Voir le dossier de {a.ameNom}
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

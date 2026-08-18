import { useState } from 'react';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { Users2, UserPlus, X } from 'lucide-react';
import type { Assignment, Team, Position } from './types';
import { ROLE_LABELS } from './types';

export function AssignmentsTab({ assignments, teams, positions, members, deptId, onChanged }: {
  assignments: Assignment[]; teams: Team[]; positions: Position[]; members: any[]; deptId: string; onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const [memberId, setMemberId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [positionId, setPositionId] = useState('');
  const [role, setRole] = useState('MEMBRE');
  const [dateDebut, setDateDebut] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/assignments`, {
        memberId, teamId: teamId || null, positionId: positionId || null, role, dateDebut: dateDebut || null,
      });
    },
    onSuccess: () => { toast.success('Membre affecté ✅'); setMemberId(''); setTeamId(''); setPositionId(''); setRole('MEMBRE'); setDateDebut(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const endMutation = useMutation({
    mutationFn: async (assignmentId: string) => api.delete(`/departments/${deptId}/assignments/${assignmentId}`),
    onSuccess: () => { toast.success('Affectation terminée'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const activeAssignments = assignments.filter((a) => a.actif);
  const inactiveAssignments = assignments.filter((a) => !a.actif);

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <Users2 className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Affectations membres → équipes / postes</h3>
        <span className="badge text-[10px] badge-info">{activeAssignments.length} actives</span>
      </div>

      <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <div>
            <label className="label">Membre *</label>
            <select className="input" value={memberId} onChange={(e) => setMemberId(e.target.value)}>
              <option value="">— Choisir —</option>
              {members.map((m) => <option key={m.id} value={m.id}>{m.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Équipe *</label>
            <select className="input" value={teamId} onChange={(e) => setTeamId(e.target.value)}>
              <option value="">— Choisir —</option>
              {teams.filter((t) => t.statut === 'ACTIVE').map((t) => <option key={t.id} value={t.id}>{t.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Poste</label>
            <select className="input" value={positionId} onChange={(e) => setPositionId(e.target.value)}>
              <option value="">— Aucun —</option>
              {positions.filter((p) => p.statut === 'ACTIVE').map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Rôle</label>
            <select className="input" value={role} onChange={(e) => setRole(e.target.value)}>
              {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Début</label>
            <input type="date" className="input" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </div>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!memberId || !teamId || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
          <UserPlus className="w-4 h-4" /> Affecter
        </button>
      </div>

      {activeAssignments.length === 0 ? (
        <div className="text-center py-8">
          <Users2 className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucune affectation active</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Membre</th><th>Équipe</th><th>Poste</th><th>Rôle</th><th>Début</th><th>Fin</th><th></th>
              </tr>
            </thead>
            <tbody>
              {activeAssignments.map((a) => (
                <tr key={a.id}>
                  <td className="font-medium text-gray-900 dark:text-gray-100">{a.memberNom || a.memberId.slice(0, 8)}</td>
                  <td>{a.teamNom ? <span className="badge text-[10px] badge-info">{a.teamNom}</span> : '—'}</td>
                  <td className="text-sm">{a.positionNom || '—'}</td>
                  <td><span className={`badge text-[10px] ${a.role === 'CHEF' ? 'badge-warning' : a.role === 'ADJOINT' ? 'badge-info' : 'badge-gray'}`}>{ROLE_LABELS[a.role] || a.role}</span></td>
                  <td className="text-sm">{a.dateDebut || '—'}</td>
                  <td className="text-sm">{a.dateFin || '—'}</td>
                  <td>
                    <button onClick={() => endMutation.mutate(a.id)} title="Mettre fin" className="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-500/10 transition-all cursor-pointer">
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {inactiveAssignments.length > 0 && (
        <div className="mt-4">
          <p className="text-xs font-medium text-gray-400 mb-2 uppercase tracking-wider">Historique ({inactiveAssignments.length})</p>
          <div className="space-y-1">
            {inactiveAssignments.map((a) => (
              <div key={a.id} className="flex items-center gap-2 text-xs text-gray-400 px-2 py-1">
                <X className="w-3 h-3" />
                {a.memberNom} → {a.teamNom || a.positionNom || '—'} · terminé le {a.dateFin || '—'}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ============================================================
// TÂCHES
// ============================================================


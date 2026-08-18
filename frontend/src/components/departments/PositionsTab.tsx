import { useState } from 'react';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { Briefcase, Plus, Archive } from 'lucide-react';
import type { Position } from './types';

export function PositionsTab({ positions, deptId, onChanged }: { positions: Position[]; deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [nom, setNom] = useState('');
  const [description, setDescription] = useState('');
  const [competences, setCompetences] = useState('');

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post(`/departments/${deptId}/positions`, { nom, description: description || null, competencesRequises: competences || null });
    },
    onSuccess: () => { toast.success('Poste créé ✅'); setNom(''); setDescription(''); setCompetences(''); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
  const archiveMutation = useMutation({
    mutationFn: async (positionId: string) => api.delete(`/departments/${deptId}/positions/${positionId}`),
    onSuccess: () => { toast.success('Poste archivé'); queryClient.invalidateQueries({ queryKey: ['department'] }); onChanged(); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-4">
        <Briefcase className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Postes du département</h3>
      </div>
      <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="label">Nom du poste *</label>
            <input className="input" value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Ex : Technicien son, Vidéaste…" />
          </div>
          <div>
            <label className="label">Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Rôle, responsabilités…" />
          </div>
          <div>
            <label className="label">Compétences requises</label>
            <input className="input" value={competences} onChange={(e) => setCompetences(e.target.value)} placeholder="Ex : mixage, éclairage…" />
          </div>
        </div>
        <button onClick={() => createMutation.mutate()} disabled={!nom.trim() || createMutation.isPending} className="btn-primary btn-sm mt-3 cursor-pointer">
          <Plus className="w-4 h-4" /> Créer le poste
        </button>
      </div>
      {positions.length === 0 ? (
        <div className="text-center py-8">
          <Briefcase className="w-10 h-10 text-gray-300 mx-auto mb-2" />
          <p className="text-sm text-gray-400">Aucun poste défini</p>
        </div>
      ) : (
        <div className="space-y-2">
          {positions.map((p) => (
            <div key={p.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 dark:bg-gray-800/40">
              <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300">
                <Briefcase className="w-4 h-4" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{p.nom}</span>
                  <span className="badge text-[9px] badge-gray">{p.nbMembres} membre{p.nbMembres > 1 ? 's' : ''}</span>
                </div>
                {(p.description || p.competencesRequises) && (
                  <p className="text-[10px] text-gray-400 truncate max-w-xl">{p.description}{p.competencesRequises ? ` · ${p.competencesRequises}` : ''}</p>
                )}
              </div>
              {p.statut !== 'ARCHIVED' && (
                <button onClick={() => archiveMutation.mutate(p.id)} title="Archiver" className="p-1.5 rounded-lg text-gray-400 hover:text-amber-600 hover:bg-amber-500/10 transition-all cursor-pointer">
                  <Archive className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ============================================================
// AFFECTATIONS
// ============================================================

